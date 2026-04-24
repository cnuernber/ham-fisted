(ns ham-fisted.fjp
  "Support for java.util.concurrent.ForkJoinPool-specific operations such as managed block and task fork/join.
  Additionally supports concept of 'exception-safe' via wrapping executing code and unwrapper result post
  execution in order avoid wrapping exceptions and breaking calling code that may be expecting specific
  exception types.

  Some of the api's fall back to regular executor service code when called.

  Example:

```clojure
(defn split-parallel-reduce
  \"Perform a parallel reduction of a spliterator using the provided ExecutorService\"
  [executor-service split ideal-split init-fn rfn merge-fn]
  (let [n-elems (proto/estimate-count split)
        pool (or executor-service (ForkJoinPool/commonPool))]
    (if (or (<= n-elems (long ideal-split)) (= n-elems Long/MAX_VALUE))
      (split-reduce rfn (init-fn) split)
      (if-let [[lhs rhs] (proto/split split)]
        (let [lt (fjp/safe-fork-task pool (split-parallel-reduce pool lhs ideal-split init-fn rfn merge-fn))
              rt (fjp/safe-fork-task pool (split-parallel-reduce pool rhs ideal-split init-fn rfn merge-fn))]
          (merge-fn (fjp/managed-block-unwrap lt) (fjp/managed-block-unwrap rt)))))))
```"
  (:require [ham-fisted.language :refer [cond not]]
            [ham-fisted.protocols :as proto]
            [ham-fisted.defprotocol :refer [extend-type extend-protocol]])
  (:import [java.util.concurrent ForkJoinPool ForkJoinTask ForkJoinPool$ManagedBlocker RecursiveTask Future
            ExecutorService]
           [clojure.lang IDeref]
           [ham_fisted FJTask])
  (:refer-clojure :exclude [cond not extend-type extend-protocol]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn common-pool "Returns  (ForkJoinPool/commonPool)"
  ^ForkJoinPool [] (ForkJoinPool/commonPool))
(defn common-pool-parallelism "Integer parallelism assigned to the common pool "
  ^long [] (ForkJoinPool/getCommonPoolParallelism))
(defn in-fork-join-pool? "Returns true if this task is executing in a fork join pool thread"
  [] (ForkJoinTask/inForkJoinPool))

(extend-protocol proto/ManagedBlocker
  ForkJoinTask
  (managed-blocker [m] (reify ForkJoinPool$ManagedBlocker
                         (block [_] (.quietlyComplete m) true)
                         (isReleasable [_] (.isDone m))
                         clojure.lang.IDeref
                         (deref [_] (.get m))))
  Future
  (managed-blocker [m] (reify ForkJoinPool$ManagedBlocker
                         (block [_] (.get m) true)
                         (isReleasable [_] (.isDone m))
                         clojure.lang.IDeref
                         (deref [_] (.get m))))
  clojure.lang.IPending
  (managed-blocker [m] (reify ForkJoinPool$ManagedBlocker
                         (block [_] (deref m) true)
                         (isReleasable [_] (realized? m))
                         clojure.lang.IDeref
                         (deref [_] (deref m))))
  ForkJoinPool$ManagedBlocker
  (managed-blocker [m] m))

(defn managed-block
  "Block on a delay or future using the fjp system's managed blocking facility.  Safe to call all the time
  whether the current system is in a forkjoinpool task or not."
  ([dly]
   (if (instance? ForkJoinTask dly)
     (.join ^ForkJoinTask dly)
     (let [dly (proto/managed-blocker dly)]
       (ForkJoinPool/managedBlock dly)
       @dly)))
  ([finished? wait-till-finished get-value]
   (managed-block (reify
                    ForkJoinPool$ManagedBlocker
                    (block [this] (wait-till-finished))
                    (isReleasable [this] (finished?))
                    clojure.lang.IDeref
                    (deref [this] (get-value))))))

(defn task "Create a task from a clojure IFn or something that implements IDeref"
  ^FJTask [f] (FJTask. f))

(defn fork-task
  "Begin a separate execution for f.  If already in a fork join pool fork the task else
  submit f to passed in pool."
  [pool f] (if (in-fork-join-pool?)
             (let [t (task f)] (.fork t) t)
             (.submit ^ExecutorService pool ^Callable f)))

(defmacro exception-safe
  "Wrap code in an exception-safe wrapper - returns a map with either
  `:ham-fisted.fjp/result` or `:ham-fisted.fjp.error`."
  [& code]
  `(let [~(with-meta 'ffn {:tag 'Callable})
         (^:once fn* []
          (try {:ham-fisted.fjp/result (do ~@code)}
               (catch Throwable e# {:ham-fisted.fjp/error e#})))]
     ~'ffn))

(defmacro safe-fork-task
  "Called from within an executing task, fork a executing some code and wrapping it in [[exception-safe]]
  then calling [[fork-task]]"
  [pool & code]
  `(->> (do ~@code)
        (exception-safe)
        (fork-task ~pool)))

(defn join
  "join a previously forked task returning the result"
  [^ForkJoinTask t] (.join t))

(defn compute
  "compute a task in current thread - returns result"
  [t]
  (if (instance? FJTask t)
    (.deref (.-c ^FJTask t))
    (throw (RuntimeException. "Only recursive tasks (or tasks create via \"task\" can be computed"))))

(defn unsafe-common-pool
  "Run a callable on the common pool.  If the callable throws you will get a wrapped exception thrown
  which may confuse calling code - specifically code that relies on exact exception types or ex-info."
  [code]
  (-> (.submit (ForkJoinPool/commonPool) ^Callable code)
      (deref)))

(defn unwrap-safe
  "Unwrap result created via executing code wrapped in [[exception-safe]].  Throws original exception if found."
  [m]
  (let [rv (get m ::result ::failure)]
    (if (identical? rv ::failure)
      (throw (get m ::error))
      rv)))

(defn safe-common-pool
  "Run safe code - see [[exception-safe]] unwrapping the result and re-throwing the wrapped exception.  This allows
  systems based on typed exceptions to pass error info."
  [safe-code]
  (unwrap-safe (unsafe-common-pool safe-code)))

(defmacro on-cp
  "Run arbitrary code on the common-pool.  Make sure any blocking operations are wrapped in [[managed-block]]."
  [& code]
  `(safe-common-pool (exception-safe ~@code)))

(defn managed-block-unwrap "managed block then safe unwrap the exception-safe result"
  [dly] (managed-block dly) (unwrap-safe @dly))

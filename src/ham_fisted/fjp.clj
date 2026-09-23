(ns ham-fisted.fjp
  "Support for java.util.concurrent.ForkJoinPool-specific operations such as managed block and task fork/join.
  Additionally supports concept of 'exception-safe' via wrapping executing code and unwrapper result post
  execution in order avoid wrapping exceptions and breaking calling code that may be expecting specific
  exception types.

  Some of the api's fall back to regular executor service code when called.

  One thing to note is there is no support for bound-fn in this codebase so it is on
  the user to use bound-fn-like constructs if support for dynamic variables is required
  in their code.

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

(defn make-blocker
  "Make a `ForkJoinPool$ManagedBlocker` managed blocker from a set of functions.
  * `finished?` - returns truthy if the op has finished
  * `wait-till-finished?` - blocks until finished.  finished? is checked and wait-till-finished is
    called again if finished? returns falsy.  This is so that wait-till-finished can be easily
  bound to LockSupport/park.
  * `get-value` - Return the value - this will be called once finished? has returned true."
  (^ForkJoinPool$ManagedBlocker [finished? wait-till-finished get-value]
   (reify ForkJoinPool$ManagedBlocker
     (block [_]
       (try
         (while (not (finished?))
           (wait-till-finished))
         (catch InterruptedException _
           (.interrupt (Thread/currentThread))))
       true)
     (isReleasable [_] (boolean (finished?)))
     clojure.lang.IDeref
     (deref [_] (get-value))))
  (^ForkJoinPool$ManagedBlocker [finished? wait-till-finished]
   (make-blocker finished? wait-till-finished wait-till-finished)))

(extend-protocol proto/ManagedBlocker
  Future
  (managed-blocker [m]
    (make-blocker #(.isDone m) #(.get m)))
  clojure.lang.IPending
  (managed-blocker [m]
    (make-blocker realized? deref))
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
   (managed-block (make-blocker finished? wait-till-finished get-value))))

(defmacro exception-safe
  "Wrap code in an exception-safe wrapper - returns a map with either
  `:ham-fisted.fjp/result` or `:ham-fisted.fjp.error`."
  [& code]
  `(let [~(with-meta 'ffn {:tag 'Callable})
         (^:once fn* []
          (try {:ham-fisted.fjp/result (do ~@code)}
               (catch Throwable e# {:ham-fisted.fjp/error e#})))]
     ~'ffn))

(defn ^:no-doc task "Create a task from a clojure IFn or something that implements IDeref"
  ^FJTask [f] (FJTask. f))

(defn ^:no-doc fork-task
  "Begin a separate execution for f.  If already in a fork join pool fork the task else
  submit f to passed in pool."
  [pool f] (if (in-fork-join-pool?)
             (let [t (task f)] (.fork t) t)
             (.submit ^ExecutorService pool ^Callable f)))

(defmacro safe-fork-task
  "Called from within an executing task, fork a executing some code and wrapping it in [[exception-safe]]
  then calling [[fork-task]]"
  [pool & code]
  `(->> (do ~@code)
        (exception-safe)
        (fork-task ~pool)))

(defn unwrap-safe
  "Unwrap result created via executing code wrapped in [[exception-safe]].  Throws original exception if found."
  [m]
  (let [rv (get m ::result ::failure)]
    (if (identical? rv ::failure)
      (throw (get m ::error))
      rv)))

(defn managed-block-unwrap "managed block then safe unwrap the exception-safe result"
  [dly] (managed-block dly) (unwrap-safe @dly))

(defmacro on-cp
  "Run arbitrary code on the common-pool.  Make sure any blocking operations are wrapped in [[managed-block]]."
  [& code]
  `(->> (exception-safe ~@code)
        (.submit (common-pool))
        (managed-block-unrwap)))


(def ^{:dynamic true
       :doc "User-bindable cpu pool to allow custom forkjoinpools"}
  *cpu-pool* (common-pool))

(defn cpu-pool "Get the currently bound cpu pool as a forkjoinpool" ^ForkJoinPool [] *cpu-pool*)

(defmacro on-cpu-pool
  "Run code on the cpu pool.  Code on run the cpu pool must use [managed-block] as opposed to
  deref"
  [& code]
  `(->> (exception-safe ~@code)
        (.submit (cpu-pool))
        (managed-block-unrwap)))

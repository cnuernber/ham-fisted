(ns ham-fisted.fjp
  (:import [java.util.concurrent ForkJoinPool ForkJoinTask ForkJoinPool$ManagedBlocker RecursiveTask]
           [clojure.lang IDeref]
           [ham_fisted FJTask]))

(set! *warn-on-reflection* true)

(defn managed-block
  "Block on a delay or future using the fjp system's managed blocking facility.  Safe to call all the time
  whether the current system is blocking or not."
  ([dly]
   (let [df #(deref dly)]
     (managed-block  #(realized? dly) df df)))
  ([finished? wait-till-finished get-value]
   (ForkJoinPool/managedBlock
    (reify
      ForkJoinPool$ManagedBlocker
      (block [this] (wait-till-finished))
      (isReleasable [this] (finished?))
      clojure.lang.IDeref
      (deref [this] (get-value))))))

(defn task
  "Create a task from a clojure IFn or something that implements IDeref"
  [f]
  (FJTask. f))

(defn fork
  "submit a task for execution - call from within a running task"
  [^ForkJoinTask t] (.fork t))

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

(defmacro exception-safe
  "Wrap code in an exception-safe wrapper - returns a map with either 
  `:ham-fisted.fjp/result` or `:ham-fisted.fjp.error`."
  [& code]
  `(^:once fn* []
    (try {:ham-fisted.fjp/result (do ~@code)}
         (catch Throwable e# {:ham-fisted.fjp/error e#}))))

(defn unwrap-safe
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

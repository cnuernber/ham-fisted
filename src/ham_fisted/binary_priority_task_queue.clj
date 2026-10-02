(ns ham-fisted.binary-priority-task-queue
  (:require [ham-fisted.defprotocol :refer [extend-type]]
            [ham-fisted.protocols :as hamf-proto])
  (:import [ham_fisted BinaryPriorityFutureTask CooperativeTPE CooperativeTPE$PooledThread]
           [java.util ArrayDeque]
           [java.util.concurrent BlockingQueue LinkedBlockingQueue TimeUnit ThreadPoolExecutor ThreadFactory Executors ExecutorService Callable]
           [java.util.concurrent.locks ReentrantLock])
  (:refer-clojure :exclude [extend-type]))

(set! *warn-on-reflection* true)

(defn future-task [c is-high?] (BinaryPriorityFutureTask. c is-high?))
(extend-type BinaryPriorityFutureTask
  hamf-proto/BinaryPriority
  (is-high-priority? [m] (.isHighPriority m)))

(defn is-in-binary-priority-task? [] (BinaryPriorityFutureTask/isInBinaryPriorityTask))

(defn binary-priority-blocking-queue
  "Unbounded blocking queue where high priority tasks (see [[ham-fisted.protocols/is-high-priority?]])
  are always taken before low priority tasks."
  ^BlockingQueue []
  (let [lowq (ArrayDeque.)
        highq (ArrayDeque.)
        lock (ReentrantLock.)
        cc (.newCondition lock)
        ;;All of these must be called with lock held.
        poll-one (fn [] (or (.poll highq) (.poll lowq)))
        n-items (fn ^long [] (+ (.size highq) (.size lowq)))]
    (reify
      BlockingQueue
      (offer [_ task]
        (.lock lock)
        (try
          (if (hamf-proto/is-high-priority? task)
            (.add highq task)
            (.add lowq task))
          (.signal cc)
          true
          (finally (.unlock lock))))
      (offer [this task _timeout _time-unit] (.offer this task))
      (add [this task] (.offer this task))
      (put [this task] (.offer this task) nil)
      (poll [_]
        (.lock lock)
        (try (poll-one) (finally (.unlock lock))))
      (poll [_ timeout time-unit]
        (let [deadline (+ (System/nanoTime) (.toNanos ^TimeUnit time-unit timeout))]
          (.lock lock)
          (try
            ;;Checking and waiting under the lock means an offer cannot slip in between.
            (loop []
              (or (poll-one)
                  (let [remaining (- deadline (System/nanoTime))]
                    (when (pos? remaining)
                      (.awaitNanos cc remaining)
                      (recur)))))
            (finally (.unlock lock)))))
      (take [_]
        (.lock lock)
        (try
          (loop []
            (or (poll-one)
                (do (.await cc) (recur))))
          (finally (.unlock lock))))
      (peek [_]
        (.lock lock)
        (try (or (.peek highq) (.peek lowq)) (finally (.unlock lock))))
      (isEmpty [_]
        (.lock lock)
        (try (== 0 (n-items)) (finally (.unlock lock))))
      (size [_]
        (.lock lock)
        (try (unchecked-int (n-items)) (finally (.unlock lock))))
      (remainingCapacity [_] Integer/MAX_VALUE)
      (remove [_ t]
        (.lock lock)
        (try (boolean (or (.remove highq t) (.remove lowq t))) (finally (.unlock lock))))
      (contains [_ t]
        (.lock lock)
        (try (boolean (or (.contains highq t) (.contains lowq t))) (finally (.unlock lock))))
      (drainTo [this c] (.drainTo this c Integer/MAX_VALUE))
      (drainTo [_ c max-elems]
        (.lock lock)
        (try
          (loop [n 0]
            (if-let [t (when (< n (long max-elems)) (poll-one))]
              (do (.add ^java.util.Collection c t) (recur (unchecked-inc n)))
              (unchecked-int n)))
          (finally (.unlock lock))))
      (clear [_]
        (.lock lock)
        (try (.clear highq) (.clear lowq) (finally (.unlock lock))))
      (^objects toArray [_]
        (.lock lock)
        (let [^objects rv (try (into-array Object (concat highq lowq)) (finally (.unlock lock)))]
          rv))
      (^objects toArray [this ^"[Ljava.lang.Object;" ary]
        (let [^objects data (.toArray this)
              ^objects rv (if (<= (alength data) (alength ary))
                            (do (System/arraycopy data 0 ary 0 (alength data))
                                (when (< (alength data) (alength ary))
                                  (aset ary (alength data) nil))
                                ary)
                            (java.util.Arrays/copyOf data (alength data) (.getClass ^Object ary)))]
          rv))
      ;;Snapshot iterator - removal through it is not supported.
      (iterator [this] (.iterator (java.util.Arrays/asList (.toArray this))))
      clojure.lang.IDeref
      (deref [_]
        (.lock lock)
        (try {:lowq-size (.size lowq)
              :highq-size (.size highq)}
             (finally (.unlock lock)))))))

(comment
  (defrecord HR [] hamf-proto/BinaryPriority (is-high-priority? [_] true))
  (def qq (binary-priority-blocking-queue))
  (.offer qq (map->HR {:a 1}))
  (.offer qq {:b 1})


  (def ee (ThreadPoolExecutor. 10 10 0 TimeUnit/MILLISECONDS (binary-priority-blocking-queue)))

  (let [lowl (atom 0)
        highl (atom 0)
        n-reps 2000
        start (System/nanoTime)
        cur (fn [] (* 1e-6 (- (System/nanoTime) start)))]
    (dotimes [idx n-reps]
      (.execute ee (future-task (fn []
                                  (Thread/sleep 2)
                                  (when (= (swap! highl (fnil inc 0)) n-reps)
                                    (println "high priority" {:low @lowl :high @highl} (is-in-binary-priority-task?) (cur))))
                                true))
      (.execute ee (future-task (fn []
                                  (Thread/sleep 1)
                                  (when (= (swap! lowl (fnil inc 0)) n-reps)
                                    (println "low priority" {:low @lowl :high @highl} (is-in-binary-priority-task?) (cur))))
                                false)))
    (Thread/sleep 5000)
    (println {:low @lowl :high @highl}))
  :-)


(defn cooperative-thread-pool-executor
  ^ThreadPoolExecutor [n-threads thread-factory queue handler]
  (ham_fisted.CooperativeTPE. n-threads 0 thread-factory queue (or handler (java.util.concurrent.ThreadPoolExecutor$AbortPolicy.))))

(definterface PrioritySubmit
  (submitCallablePriority [task high-priority?]))

(defmacro with-high-priority
  [& code]
  `(let [old-p# (boolean (.get BinaryPriorityFutureTask/isHighPriorityVar))]
     (try (.set BinaryPriorityFutureTask/isHighPriorityVar true)
          ~@code
          (finally (.set BinaryPriorityFutureTask/isHighPriorityVar old-p#)))))

(defn is-current-thread-high-priority? [] (.get BinaryPriorityFutureTask/isHighPriorityVar))

(defn binary-priority-executor
  ^ExecutorService [& {:keys [n-threads thread-name-prefix rejected-execution-handler thread-factory daemon-threads?]
                       :or {daemon-threads? true}
                       :as _options}]
  (let [queue (binary-priority-blocking-queue)
        n-threads (long (or n-threads (Math/max 1 (dec (.availableProcessors (Runtime/getRuntime))))))
        thread-pool* (volatile! nil)
        thread-factory (or thread-factory
                           (let [thread-name-prefix (or thread-name-prefix "binary-priority-thread")
                                 idx (volatile! 0)]
                             (reify ThreadFactory
                               (newThread [_ runnable]
                                 (doto (CooperativeTPE$PooledThread. runnable @thread-pool* (str thread-name-prefix (vswap! idx inc)))
                                   (.setDaemon daemon-threads?))))))
        thread-pool (cooperative-thread-pool-executor n-threads thread-factory queue rejected-execution-handler)]
    (vreset! thread-pool* thread-pool)
    (reify
      ham_fisted.ExecutorService
      (submitCallable [_ task]
        (let [fv (future-task task (boolean (.get BinaryPriorityFutureTask/isHighPriorityVar)))]
          (.execute thread-pool fv)
          fv))
      (submitRunnable [this task]
        (.submitCallable this (Executors/callable ^Runnable task)))
      (submitRunnable [this task result]
        (.submitCallable this (Executors/callable ^Runnable task result)))
      (execute [this task] (.submitRunnable this task) nil)
      (shutdown [_] (.shutdown thread-pool))
      (shutdownNow [_] (.shutdownNow thread-pool))
      (isShutdown [_] (.isShutdown thread-pool))
      (isTerminated [_] (.isTerminated thread-pool))
      (awaitTermination [_ timeout unit] (.awaitTermination thread-pool timeout unit))
      (invokeAll [_ tasks] (.invokeAll thread-pool tasks))
      (invokeAll [_ tasks timeout unit] (.invokeAll thread-pool tasks timeout unit))
      (invokeAny [_ tasks] (.invokeAny thread-pool tasks))
      (invokeAny [_ tasks timeout unit] (.invokeAny thread-pool tasks timeout unit))
      PrioritySubmit
      (submitCallablePriority [_ task high-priority?]
        (let [fv (future-task task high-priority?)]
          (.execute thread-pool fv)
          fv))
      clojure.lang.IDeref
      (deref [_] (merge {:thread-pool thread-pool} @queue)))))

(defn managed-block [blocker]
  (CooperativeTPE/managedBlock blocker))

(comment
  (defonce ee (binary-priority-executor))
  (let []
    (time
     (->> (range 10000)
          (mapv (fn [_] (.submit ee ^Callable (fn []
                                                (managed-block (delay (Thread/sleep 200)))
                                                (loop [idx 0
                                                       sum 0]
                                                  (if (< idx 1000000)
                                                    (recur (inc idx) (+ sum idx))
                                                    sum))))))
          (mapv deref)))
    :ok)
  (managed-block (delay (Thread/sleep 200)))
  :-)

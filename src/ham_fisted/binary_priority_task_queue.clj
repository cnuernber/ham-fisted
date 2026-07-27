(ns ham-fisted.binary-priority-task-queue
  (:require [ham-fisted.defprotocol :refer [extend-type]]
            [ham-fisted.protocols :as hamf-proto])
  (:import [ham_fisted BinaryPriorityFutureTask CooperativeTPE CooperativeTPE$PooledThread]
           [java.util.concurrent BlockingQueue LinkedBlockingQueue TimeUnit ThreadPoolExecutor ThreadFactory Executors ExecutorService]
           [java.util.concurrent.locks ReentrantLock])
  (:refer-clojure :exclude [extend-type]))

(set! *warn-on-reflection* true)

(defn future-task [c is-high?] (BinaryPriorityFutureTask. c is-high?))
(extend-type BinaryPriorityFutureTask
  hamf-proto/BinaryPriority
  (is-high-priority? [m] (.isHighPriority m)))

(defn is-in-binary-priority-task? [] (BinaryPriorityFutureTask/isInBinaryPriorityTask))

(defn binary-priority-blocking-queue ^BlockingQueue []
  (let [lowq (LinkedBlockingQueue.)
        highq (LinkedBlockingQueue.)
        lock (ReentrantLock.)
        cc (.newCondition lock)]
    (reify
      BlockingQueue
      (offer [_ task]
        (.lock lock)
        (try
          (let [rv
                (if (hamf-proto/is-high-priority? task)
                  (.offer highq task)
                  (.offer lowq task))]
            (.signal cc)
            rv)
          (finally (.unlock lock))))
      (isEmpty [_] (boolean (and (.isEmpty highq) (.isEmpty lowq))))
      (poll [_ timeout time-unit]
        (if-let [ht (.poll highq 0 TimeUnit/MILLISECONDS)]
          ht
          (if-let [lt (.poll lowq 0 TimeUnit/MILLISECONDS)]
            lt
            (do
              (.lock lock)
              (try
                (.await cc timeout time-unit)
                (finally (.unlock lock)))
              (or (.poll highq 0 TimeUnit/MILLISECONDS) (.poll lowq 0 TimeUnit/MILLISECONDS))))))
      (take [this] (.poll this Integer/MAX_VALUE TimeUnit/MILLISECONDS))
      (size [_] (+ (.size highq) (.size lowq)))
      (remove [_ t] (throw (RuntimeException. "Unimplemented")))
      clojure.lang.IDeref
      (deref [_] {:lowq-size (.size lowq)
                  :highq-size (.size highq)}))))

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

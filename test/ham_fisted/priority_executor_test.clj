(ns ham-fisted.priority-executor-test
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.binary-priority-task-queue :as bptq]
            [ham-fisted.protocols :as hamf-proto])
  (:import [java.util ArrayList]
           [java.util.concurrent TimeUnit Callable]))


(defrecord ^:private High [v] hamf-proto/BinaryPriority (is-high-priority? [_] true))


(deftest priority-queue-order-and-methods
  (let [q (bptq/binary-priority-blocking-queue)]
    (.offer q :low1)
    (.offer q (->High 1))
    (.offer q :low2)
    (is (= 3 (.size q)))
    (is (= (->High 1) (.peek q)))
    (is (= (->High 1) (.poll q 0 TimeUnit/MILLISECONDS)))
    (is (.contains q :low2))
    (let [drained (ArrayList.)]
      (is (= 2 (.drainTo q drained)))
      (is (= [:low1 :low2] (vec drained))))
    (is (.isEmpty q))
    (is (nil? (.poll q 1 TimeUnit/MILLISECONDS)))))


(deftest priority-queue-wakes-waiting-poll
  (let [q (bptq/binary-priority-blocking-queue)
        f (future (.poll q 5000 TimeUnit/MILLISECONDS))]
    (Thread/sleep 50)
    (.offer q :x)
    (is (= :x (deref f 1000 :timeout)))))


(deftest priority-executor-service-methods
  (let [e (bptq/binary-priority-executor {:n-threads 2})
        p (promise)]
    (is (nil? @(.submit e ^Runnable (fn [] nil))))
    (is (= :r @(.submit e ^Runnable (fn [] nil) :r)))
    (is (= 3 @(.submit e ^Callable (fn [] 3))))
    (.execute e ^Runnable (fn [] (deliver p 1)))
    (is (= 1 (deref p 5000 :timeout)))
    (is (= [1 2] (mapv deref (.invokeAll e [(fn [] 1) (fn [] 2)]))))
    (.shutdown e)
    (is (.awaitTermination e 5 TimeUnit/SECONDS))
    (is (.isShutdown e))))

(ns ham-fisted.fjp-test
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.fjp :as fjp]
            [ham-fisted.protocols :as proto]
            [ham-fisted.function :as hamf-fn]
            [ham-fisted.reduce :as hamf-rf]
            [ham-fisted.language :refer [cond not]]
            [ham-fisted.spliterator :as spliterator]
            [clojure.test :refer [deftest is]])
  (:import [java.util List Spliterator Spliterator$OfDouble Spliterator$OfLong]
           [java.util.function Consumer DoubleConsumer LongConsumer]
           [clojure.lang IDeref IFn$LLL IFn$DDD]
           [ham_fisted Consumers$IDerefLongConsumer Consumers$IDerefDoubleConsumer])
  (:refer-clojure :exclude [cond not]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn lp ^long [^long a ^long b] (+ a b))
(defn dp ^double [^double a ^double b] (+ a b))

(defn sum-reducer []
  (reify
    proto/Reducer
    (->init-val-fn [_] #(long-array 1))
    (->rfn [_] (fn [^longs lv ^long v] (let [_ (aset lv 0 (+ (aget lv 0) v))] lv)))
    proto/ParallelReducer
    (->merge-fn [_] (fn [^longs lv ^longs rv]
                      (let [_ (aset lv 0 (+ (aget lv 0) (aget rv 0)))]
                        lv)))
    proto/Finalize
    (finalize [this lv] (aget ^longs lv 0))))

(deftest parallel-summation
  (let [data (hamf/range 1000000)
        total (hamf/lsum data)]
    (is (= total
           (spliterator/split-parallel-reduce
            (fjp/common-pool) (proto/->spliterator data) 1000 (constantly 0) lp lp)))
    (is (= total
           (spliterator/split-parallel-reduce
            clojure.lang.Agent/soloExecutor (proto/->spliterator data) 1000 (constantly 0) lp lp)))
    (is (= total (hamf-rf/preduce (constantly 0) lp lp (proto/->spliterator data))))
    (is (= total (hamf-rf/preduce-reducer (sum-reducer) (proto/->spliterator data))))))

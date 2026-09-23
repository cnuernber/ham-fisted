(ns ham-fisted.fjp-test
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.fjp :as fjp]
            [ham-fisted.lazy-noncaching :as lznc]
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


(deftest split-reduce-respects-reduced
  (let [olo (reify ham_fisted.IFnDef$OLO
              (invokePrim [_ acc v] (if (== v 3) (reduced (conj acc v)) (conj acc v))))
        odo (reify ham_fisted.IFnDef$ODO
              (invokePrim [_ acc v] (if (== v 3.0) (reduced (conj acc v)) (conj acc v))))
        obj (fn [acc v] (if (== (long v) 3) (reduced (conj acc v)) (conj acc v)))]
    (is (= [1 2 3] (spliterator/split-reduce olo [] (long-array [1 2 3 4 5]))))
    (is (= [1.0 2.0 3.0] (spliterator/split-reduce odo [] (double-array [1 2 3 4 5]))))
    (is (= [1 2 3] (spliterator/split-reduce obj [] (vec (range 1 6)))))
    ;;no reduced - full reduction
    (is (= [1 2] (spliterator/split-reduce olo [] (long-array [1 2]))))
    (is (= [1.0 2.0] (spliterator/split-reduce odo [] (double-array [1 2]))))
    ;;typed take transducer is OLO when downstream rfn is OLO
    (is (= [1 2] (spliterator/split-reduce ((lznc/take 2) olo) [] (long-array [1 2 5 6]))))))


(deftest on-pool-macros
  (is (= 3 (fjp/on-cp (+ 1 2))))
  (is (= 3 (fjp/on-cpu-pool (+ 1 2))))
  (is (thrown-with-msg? Exception #"boom" (fjp/on-cp (throw (Exception. "boom"))))))


(comment
  (def data (hamf/range 100000000))
  (def vdata (into (hamf/immut-list) data))
  (def vvdata (into [] data))
  (= (spliterator/sum-fast vdata)
     (spliterator/psum vdata))

  (spliterator/split-to-max-size vdata 10 spliterator/elements)
  (spliterator/elements (.spliterator vdata 25 35))
  (require '[clj-async-profiler.core :as prof])
  (crit/quick-bench (spliterator/sum-fast vdata))
  (crit/quick-bench (spliterator/sum-fast vvdata))
  (prof/serve-ui 8080)

  (require '[criterium.core :as crit])
  (crit/quick-bench (spliterator/psum vdata))
  (crit/quick-bench (spliterator/psum vvdata))
  
  )

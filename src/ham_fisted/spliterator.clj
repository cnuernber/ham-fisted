(ns ham-fisted.spliterator
  (:require [ham-fisted.protocols :as proto]
            [ham-fisted.fjp :as fjp]
            [ham-fisted.defprotocol :refer [extend-type]]
            [ham-fisted.language :as hamf-language])
  (:import [java.util Spliterator Spliterator$OfDouble Spliterator$OfLong]
           [java.util.function Consumer]
           [java.util.concurrent ForkJoinPool]
           [clojure.lang IFn$DDD IFn$LLL]
           [ham_fisted Consumers$IDerefLongConsumer Consumers$IDerefDoubleConsumer Consumers$IDerefConsumer
            ParallelOptions Casts])
  (:refer-clojure :exclude [extend-type]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(extend-type nil
  proto/EstimateCount
  (estimate-count [m] 0)
  proto/Split
  (split [m] nil))

(extend-type Object
  proto/EstimateCount
  (estimate-count [m] (proto/count m))
  proto/Split
  (split [m] nil))

(defmacro deref-long-consumer [varname accept-code deref-code]
  `(reify Consumers$IDerefLongConsumer
     (acceptLong [_ ~varname] (let [l# ~accept-code]))
     (deref [_] ~deref-code)))

(defmacro deref-double-consumer [varname accept-code deref-code]
  `(reify Consumers$IDerefDoubleConsumer
     (acceptDouble [_ ~varname] (let [l# ~accept-code]))
     (deref [_] ~deref-code)))

(defmacro deref-consumer [varname accept-code deref-code]
  `(reify Consumers$IDerefConsumer
     (accept [_ ~varname] ~accept-code)
     (deref [_] ~deref-code)))

(defn split-reduce [rfn acc split]
  (cond
    (instance? IFn$LLL rfn)
    (let [dd (long-array [(Casts/longCast acc)])
          cc (deref-long-consumer ll (aset dd 0 (.invokePrim ^IFn$LLL rfn (aget dd 0) ll)) (aget dd 0))]
      (.forEachRemaining ^Spliterator split cc)
      @cc)
    (instance? IFn$DDD rfn)
    (let [dd (double-array [(Casts/doubleCast acc)])
          cc (deref-double-consumer ll (aset dd 0 (.invokePrim ^IFn$DDD rfn (aget dd 0) ll)) (aget dd 0))]
      (.forEachRemaining ^Spliterator split cc)
      @cc)
    :else
    (let [dd (hamf-language/obj-ary acc)
          cc (deref-consumer ll (aset dd 0 (rfn (aget dd 0) )) (aget dd 0))]
      (loop []
        (let [c? (.tryAdvance ^Spliterator split cc)]
          (when c?
            (let [vv @cc]
              (if (reduced? vv)
                @vv
                (recur))))))
      @cc)))

(clojure.core/extend Spliterator
    clojure.core.protocols/CollReduce
    {:coll-reduce split-reduce})

(defn- println-ret [v] (println v) v)

(extend-type Spliterator
  proto/EstimateCount
  (estimate-count [s] (.estimateSize s))
  proto/Split
  (split [s] (when-let [ss (.trySplit s)] [s ss])))

(defn split-parallel-reduce [pool split ideal-split init-fn rfn merge-fn]
  (let [n-elems (proto/estimate-count split)
        pool (or pool (ForkJoinPool/commonPool))]
    (if (or (<= n-elems (long ideal-split)) (= n-elems Long/MAX_VALUE))
      (split-reduce rfn (init-fn) split)
      (if-let [[lhs rhs] (proto/split split)]
        (let [lt (fjp/safe-fork-task pool (split-parallel-reduce pool lhs ideal-split init-fn rfn merge-fn))
              rt (fjp/safe-fork-task pool (split-parallel-reduce pool rhs ideal-split init-fn rfn merge-fn))]
          (merge-fn (fjp/managed-block-unwrap lt) (fjp/managed-block-unwrap rt)))))))

(extend-type Spliterator
  proto/ParallelReduction
  (preduce [coll init-fn rfn merge-fn options]
    (split-parallel-reduce (.-pool options) coll (.-minN options) init-fn rfn merge-fn)))

(extend-type java.util.Collection
  proto/ToSpliterator
  (->spliterator [c] (.spliterator c)))

(ns ham-fisted.spliterator
  "Support for spliterator reduction and parallel reduction."
  (:require [ham-fisted.protocols :as proto]
            [ham-fisted.fjp :as fjp]
            [ham-fisted.defprotocol :refer [extend-type extend]]
            [ham-fisted.language :as hamf-language])
  (:import [java.util Spliterator Spliterator$OfDouble Spliterator$OfLong List RandomAccess ArrayList]
           [java.util.function Consumer]
           [java.util.concurrent ForkJoinPool]
           [clojure.lang IFn$DDD IFn$LLL IFn$OLO IFn$ODO]
           [ham_fisted Consumers$IDerefLongConsumer Consumers$IDerefDoubleConsumer Consumers$IDerefConsumer
            ParallelOptions Casts ArrayLists])
  (:refer-clojure :exclude [extend-type extend]))

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


(run! (fn [[dt-name ary-cls]]
        (extend ary-cls
          proto/ToSpliterator
          {:->spliterator
           (case dt-name
             :boolean (fn [a] (.spliterator (ArrayLists/toList ^booleans a)))
             :byte (fn [a] (.spliterator (ArrayLists/toList ^bytes a)))
             :short (fn [a] (.spliterator (ArrayLists/toList ^shorts a)))
             :char (fn [a] (.spliterator (ArrayLists/toList ^chars a)))
             :int (fn [a] (.spliterator (ArrayLists/toList ^ints a)))
             :long (fn [a] (.spliterator (ArrayLists/toList ^longs a)))
             :float (fn [a] (.spliterator (ArrayLists/toList ^floats a)))
             :double (fn [a] (.spliterator (ArrayLists/toList ^doubles a)))
             (fn [a] (.spliterator (ArrayLists/toList ^objects a))))}))
      hamf-language/array-classes)

(deftype ^:private DerefLongConsumer [^{:unsynchronized-mutable true
                                        :tag long} v
                                      ^IFn$LLL rfn]
  Consumers$IDerefLongConsumer
  (acceptLong [this d] (set! v (.invokePrim rfn v d)))
  (deref [this] v))

(defmacro deref-long-consumer
  "Create a LongConsumer with support for IDeref"
  [varname accept-code deref-code]
  `(reify Consumers$IDerefLongConsumer
     (acceptLong [_ ~varname] (let [l# ~accept-code]))
     (deref [_] ~deref-code)))

(deftype DerefDoubleConsumer [^{:unsynchronized-mutable true
                                :tag double} v
                              ^IFn$DDD rfn]
  Consumers$IDerefDoubleConsumer
  (acceptDouble [this d] (set! v (.invokePrim rfn v d)))
  (deref [this] v))

(defmacro deref-double-consumer
  "Create a DoubleConsumer with support for IDeref"
  [varname accept-code deref-code]
  `(reify Consumers$IDerefDoubleConsumer
     (acceptDouble [_ ~varname] (let [l# ~accept-code]))
     (deref [_] ~deref-code)))

(defmacro deref-consumer
  "Create a Consumer with support for IDeref"
  [varname accept-code deref-code]
  `(reify Consumers$IDerefConsumer
     (accept [_ ~varname] ~accept-code)
     (deref [_] ~deref-code)))

(defn ->spliterator ^Spliterator [ii] (if (instance? Spliterator ii) ii (proto/->spliterator ii)))

(defn split-reduce "Reduce over a spliterator.  Special support exists for IFn$LLL and IFn$DDD"
  ([rfn split]
   (let [acc-ary (object-array 1)
         cc (deref-consumer ll (aset acc-ary 0 ll) (aget acc-ary 0))
         split (->spliterator split)]
     (if (.tryAdvance split cc)
       (split-reduce rfn (aget acc-ary 0) split)
       (rfn))))
  ([rfn acc split]
   (cond
     (instance? IFn$LLL rfn)
     (let [cc (DerefLongConsumer. (Casts/longCast acc) rfn)]
       (.forEachRemaining (->spliterator split) cc)
       @cc)
     (instance? IFn$OLO rfn)
     (let [dd (hamf-language/obj-ary acc)
           cc (deref-long-consumer ll (aset dd 0 (.invokePrim ^IFn$OLO rfn (aget dd 0) ll)) (aget dd 0))]
       (.forEachRemaining (->spliterator split) cc)
       @cc)
     (instance? IFn$DDD rfn)
     (let [cc (DerefDoubleConsumer. (Casts/doubleCast acc) rfn)]
       (.forEachRemaining (->spliterator split) cc)
       @cc)
     (instance? IFn$ODO rfn)
     (let [dd (hamf-language/obj-ary acc)
           cc (deref-double-consumer ll (aset dd 0 (.invokePrim ^IFn$ODO rfn (aget dd 0) ll)) (aget dd 0))]
       (.forEachRemaining (->spliterator split) cc)
       @cc)
     :else
     (let [dd (hamf-language/obj-ary acc)
           cc (deref-consumer ll (aset dd 0 (rfn (aget dd 0) ll)) (aget dd 0))
           split (->spliterator split)]
       (loop []
         (let [c? (.tryAdvance split cc)]
           (if c?
             (let [vv @cc]
               (if (reduced? vv)
                 @vv
                 (recur)))
             (unreduced @cc))))))))

(clojure.core/extend Spliterator
  clojure.core.protocols/CollReduce
  {:coll-reduce (fn reduce-spliterator
                  ([coll rfn acc] (split-reduce rfn acc coll))
                  ([coll rfn] (split-reduce rfn coll)))})

(defn- println-ret [v] (println v) v)

(extend-type Spliterator
  proto/ToSpliterator
  (->spliterator [s] s)
  proto/EstimateCount
  (estimate-count [s] (.estimateSize s))
  proto/Split
  (split [s] (when-let [ss (.trySplit s)] [s ss])))

(extend-type java.util.RandomAccess
  proto/Split
  (split [s] (let [^List s s
                   ne (.size s)]
               (when (> ne 2)
                 (let [n (quot ne 2)]
                   [(.subList s 0 n)
                    (.subList s n ne)])))))

(defn split-parallel-reduce
  "Perform a parallel reduction of a spliterator using the provided ExecutorService"
  [executor-service split ideal-split init-fn rfn merge-fn]
  (let [split (->spliterator split)
        n-elems (.estimateSize split)
        pool (or executor-service (ForkJoinPool/commonPool))]
    (if (or (<= n-elems (long ideal-split)) (= n-elems Long/MAX_VALUE))
      (split-reduce rfn (init-fn) split)
      (if-let [rhs (.trySplit split)]
        (let [lt (fjp/safe-fork-task pool (split-parallel-reduce pool split ideal-split init-fn rfn merge-fn))
              rt (fjp/safe-fork-task pool (split-parallel-reduce pool rhs ideal-split init-fn rfn merge-fn))]
          (merge-fn (fjp/managed-block-unwrap lt) (fjp/managed-block-unwrap rt)))
        (split-reduce rfn (init-fn) split)))))

(extend-type Spliterator
  proto/ParallelReduction
  (preduce [coll init-fn rfn merge-fn options]
    (split-parallel-reduce (.-pool options) coll (.-minN options) init-fn rfn merge-fn)))

(extend-type java.util.Collection
  proto/ToSpliterator
  (->spliterator [c] (.spliterator c)))

(defn sum-fast
  "spliterator based serial summation"
  ^double [vv] (let [cc (ham_fisted.Sum$SimpleSum.)]
                 (.forEachRemaining (->spliterator vv) cc)
                 @cc))

(defn- dp ^double [^double a ^double b] (+ a b))

(defn psum "spliterator based parallel summation - does not account for Double/NaN"
  ^double [vv] (split-parallel-reduce nil (->spliterator vv) 10000 (constantly 0) dp dp))

(defn elements "Return all the elements referenced by this spliterator as a persistent list"
  [data] (let [split (->spliterator data)
               rv (ham_fisted.ArrayLists$ObjectArrayList.)
               cc (reify Consumer
                    (accept [this v] (.add rv v)))]
           (.forEachRemaining split cc)
           (persistent! rv)))

(defn split-to-max-size [split ^long max-size op]
  (let [split (->spliterator split)
        n-elems (.estimateSize ^Spliterator split)]
    (if (or (<= n-elems max-size) (== n-elems Long/MAX_VALUE))
      [(op split)]
      (let [rv (doto (ArrayList. ) (.add split))]
        (loop [idx 0]
          (if (== idx (.size rv))
            rv
            (let [ll (.get rv idx)]
              (if (<= (.estimateSize ^Spliterator ll) max-size)
                (do
                  (.set rv idx (op ll))
                  (recur (inc idx)))
                (if-let [rhs (.trySplit ^Spliterator ll)]
                  (do (.add rv (inc idx) rhs)
                      (recur idx))
                  (do
                    (.set rv idx (op ll))
                    (recur (inc idx))))))))))))

(comment


  (defn ^:no-doc ms-parallel-reduce
    "Perform a parallel reduction of a spliterator using the provided ExecutorService"
    [executor-service split ideal-split init-fn rfn merge-fn]
    (let [split (->spliterator split)
          n-elems (.estimateSize split)
          pool (or executor-service (ForkJoinPool/commonPool))
          ideal-split (long ideal-split)]
      (if (or (<= n-elems ideal-split) (= n-elems Long/MAX_VALUE))
        (split-reduce rfn (init-fn) split)
        (let [data (split-to-max-size
                    split ideal-split
                    #(fjp/safe-fork-task pool (split-parallel-reduce pool % ideal-split init-fn rfn merge-fn)))
              merge-fn #(reduce merge-fn (lznc/map fjp/managed-block-unwrap %))]
          (loop [merge-data data]
            (if (>= (count merge-data) ideal-split)
              (recur (split-to-max-size
                      merge-data ideal-split
                      #(fjp/safe-fork-task pool (merge-fn %))))
              (merge-fn merge-data)))))))
  )

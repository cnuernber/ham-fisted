(ns ham-fisted.set
  (:require [ham-fisted.protocols :as hamf-proto]
            [ham-fisted.api :as api]
            [ham-fisted.impl]
            [clojure.set :as cset])
  (:import [ham_fisted HashSet PersistentHashSet Ranges$LongRange IMutList]
           [java.util BitSet Set Map Collection]
           [java.util.concurrent ConcurrentHashMap]
           [clojure.lang APersistentSet])
  (:refer-clojure :exclude [set set?]))


(set! *warn-on-reflection* true)


(declare unique bitset)


(defn- set-add-all
  [^Set s data]
  (api/reduce (fn [^Set acc v]
                 (.add acc v)
                 acc)
               s
               data))

(defn mut-set
  "Return a mutable set."
  (^Set [] (HashSet.))
  (^Set [data]
   (set-add-all (HashSet.) data)))


(defn set
  "Return an immutable set"
  (^Set [] (PersistentHashSet.))
  (^Set [data]
   (if (instance? PersistentHashSet data)
     data
     (-> (mut-set data)
         (persistent!)))))


(defn set?
  [data]
  (when data (hamf-proto/set? data)))


(defn java-hashset
  "Return a java hashset"
  (^Set [] (java.util.HashSet.))
  (^Set [data] (if (instance? java.util.HashSet data)
                 data
                 (set-add-all (java.util.HashSet.) data))))


(defn java-concurrent-hashset
  "Create a concurrent hashset."
  (^Set [] (ConcurrentHashMap/newKeySet))
  (^Set [data]
   (if (instance? Set data)
     data
     (unique {:set-constructor (constantly (java-concurrent-hashset))} data))))


(def ^{:private true
       :tag 'long} unsigned-int-max (Integer/toUnsignedLong (int -1)))

(defn- unsigned-int->host
  ^long [^long v]
  (when (or (< v 0)
            (> v unsigned-int-max))
    (throw (RuntimeException. (str "Value out of range for unsigned integer: " v))))
  (unchecked-int v))

(defn- reduce->range
  [data]
  (unique {:set-constructor bitset} data))

(defn bitset
  "Create a java.util.Bitset.  The two argument version assumes you are passing in the
  start, end of a monotonically incrementing range."
  (^BitSet [] (BitSet.))
  (^BitSet [data]
   (cond
     (instance? BitSet data)
     data
     (instance? Ranges$LongRange data)
     (let [^Ranges$LongRange data data
           rstart (.-start data)
           rend (.-end data)
           step (.-step data)]
       (if (== 1 step)
         (doto (BitSet.)
           (.set (unchecked-int rstart) (unchecked-int rend)))
         (reduce->range data)))
     :else
     (reduce->range data)))
  (^BitSet [^long start ^long end]
   (doto (BitSet.)
     (.set (unchecked-int start) (unchecked-int end)))))



(extend-protocol hamf-proto/SetOps
  Object
  (set? [l] (instance? Set l))
  (union [l r] (api/union l r))
  (difference [l r] (api/difference l r))
  (intersection [l r] (api/intersection l r))
  (xor [l r]
    (api/difference (api/union l r)
                    (api/intersection l r)))
  (contains-fn [l]
    #(.contains ^Set l %))
  (cardinality [l] (.size ^Set l))
  BitSet
  (set? [l] true)
  (union [l r]
    (let [^BitSet l (.clone l)]
      (.or ^BitSet (.clone l) (bitset r))
      l))
  (difference [l r]
    (let [^BitSet l (.clone l)]
      (.andNot l (bitset r))
      l))
  (intersection [l r]
    (let [^BitSet l (.clone l)]
      (.and l (bitset r))
      l))
  (xor [l r]
    (let [^BitSet l (.clone l)]
      (.xor l (bitset r))
      l))
  (contains-fn [l]
    (api/long-predicate v (.get l (unchecked-int v))))
  (cardinality [l] (.cardinality l)))


(extend-protocol hamf-proto/BulkSetOps
  Object
  (reduce-union [l data]
    (reduce hamf-proto/union l data))
  (reduce-intersection [l data]
    (reduce hamf-proto/intersection l data)))


(extend-protocol hamf-proto/PAdd
  BitSet
  (add-fn [c] (api/long-accumulator b v (.set ^BitSet b (unchecked-int v)) b)))


(extend-protocol hamf-proto/BitSet
  Object
  (bitset? [item] false)
  BitSet
  (bitset? [item] true)
  (contains-range? [item sidx eidx]
    (api/reduce (api/long-accumulator
                 acc v
                 (if (and (>= v 0) (.get item (unchecked-int v)))
                   true
                   (reduced false)))
                true
                (api/range sidx eidx)))
  (intersects-range? [item ^long sidx ^long eidx]
    (api/reduce (api/long-accumulator
                 acc v
                 (if (and (>= v 0) (.get item (unchecked-int v)))
                   (reduced true)
                   false))
                false
                (api/range sidx eidx)))
  (min-set-value [item]
    (.nextSetBit item 0))
  (max-set-value [item]
    (.previousSetBit item Integer/MAX_VALUE)))


(defn union
  "set union"
  [l r]
  (if l
    (hamf-proto/union l r)
    r))


(defn reduce-union
  "Reduce a number of objects into one object via union"
  ([l] l)
  ([l & data]
   (hamf-proto/reduce-union l data)))


(defn intersection
  "set intersection"
  [l r]
  (if l
    (hamf-proto/intersection l r)
    r))


(defn reduce-intersection
  ([l] l)
  ([l & data]
   (hamf-proto/reduce-intersection l data)))


(defn difference
  "set difference"
  [l r]
  (when l
    (hamf-proto/difference l r)))


(defn xor
  "set xor - difference of intersection from union"
  [l r]
  (if l
    (hamf-proto/xor l r)
    r))


(defn map-invert
  "invert a map such that the keys are the vals and the vals are the keys"
  [m]
  (when m
    (-> (api/reduce (fn [^Map acc e]
                      (.put acc (val e) (key e))
                      acc)
                    (api/mut-map)
                    m)
        (persistent!))))


(defn contains-fn
  "Return an IFn that returns efficiently returns true if the set contains
  a given element."
  [s]
  (hamf-proto/contains-fn s))


(defn cardinality
  "Return the cardinality (size) of a given set."
  ^long [s]
  (hamf-proto/cardinality s))


(defn bitset?
  "Return true if this is a bitset"
  [s]
  (hamf-proto/bitset? s))


(defn contains-range?
  "bitset-specific query that returns true if the set contains all the integers from
  sidx to eidx non-inclusive."
  [s ^long sidx ^long eidx]
  (hamf-proto/contains-range? s sidx eidx))


(defn intersects-range?
  "bitset-specific query that returns true if the set contains any the integers from
  sidx to eidx non-inclusive."
  [s ^long sidx ^long eidx]
  (hamf-proto/intersects-range? s sidx eidx))


(defn min-set-value
  "Given a bitset, return the minimum set value.  Errors if the bitset is empty."
  [s] (hamf-proto/min-set-value s))


(defn max-set-value
  "Given a bitset, return the maximum set value.  Errors if the bitset is empty."
  [s] (hamf-proto/max-set-value s))


(defn ->integer-random-access
  "Given a set (or bitset), return a efficient, sorted random access structure.  This assumes
  the set contains integers."
  [s]
  (let [ne (cardinality s)
        rv
        (if (== 0 ne)
          (api/range 0)
          (if (bitset? s)
            (let [mins (long (min-set-value s))
                  maxs (long (max-set-value s))]
              (if (== ne (- (unchecked-inc maxs) mins))
                (api/range mins (unchecked-inc maxs))
                (if (< maxs Integer/MAX_VALUE)
                  (api/ivec s)
                  (api/lvec s))))
            (let [^IMutList l (api/lvec s)]
              ;;Sorting means downstream access is in memory order.
              (.sort l nil)
              l)))]
    (if (== 0 ne)
      rv
      ;;Keeping track of the min, max values allows downstream functions to choose
      ;;perhaps different pathways.
      (vary-meta rv assoc :min (rv 0) :max (rv -1)))))


(defn unique-reducer
  "Create a parallel reducer that creates a set.

  Options:

  * `:set-constructor` construct something that implements [[ham-fisted.protocols/add-fn]]
  and [[ham-fisted.protocols/union]]
  * `:add-fn` Pass in user-defined add function as opposed to using protocol lookup to
    find it."
  [options]
  (let [ctor (get options :set-constructor api/mut-set)
        add-fn (or (get options :add-fn)
                   (hamf-proto/add-fn (ctor)))]
    (reify
      hamf-proto/Reducer
      (->init-val-fn [r] ctor)
      (->rfn [r] add-fn)
      (finalize [r v] (api/persistent! v))
      hamf-proto/ParallelReducer
      (->merge-fn [r] union))))


(defn unique
  "Create a set of unique items.  Parallelized and non-lazy.

  See options for [[unique-reducer]] and [[ham-fisted.api/preduce-reducer]]."
  ([options data]
   (api/preduce-reducer (unique-reducer options) (merge {:min-n 1000} options) data))
  ([data] (unique nil data)))

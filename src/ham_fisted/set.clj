(ns ham-fisted.set
  (:require [ham-fisted.protocols :as hamf-proto]
            [ham-fisted.api :as api]
            [ham-fisted.impl]
            [clojure.set :as cset])
  (:import [ham_fisted HashSet PersistentHashSet Ranges$LongRange]
           [java.util BitSet Set Map]
           [clojure.lang APersistentSet])
  (:refer-clojure :exclude [set]))


(defn mut-set
  (^Set [] (HashSet.))
  (^Set [data]
   (api/reduce (fn [^Set acc v]
                 (.add acc v)
                 acc)
               (HashSet.)
               data)))


(defn set
  (^Set [] (PersistentHashSet.))
  (^Set [data]
   (if (instance? PersistentHashSet data)
     data
     (-> (mut-set data)
         (persistent!)))))


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
  (api/reduce (api/long-accumulator
               acc v
               (.set ^BitSet acc (unchecked-int v))
               acc)
              (BitSet.)
              data))

(defn bitset
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
     (reduce->range data))))



(extend-protocol hamf-proto/SetOps
  Object
  (union [l r] (api/union l r))
  (difference [l r] (api/difference l r))
  (intersection [l r] (api/intersection l r))
  (xor [l r]
    (api/difference (api/union l r)
                    (api/intersection l r)))
  BitSet
  (union [l r]
    (let [^BitSet l (.clone l)]
      (.or (.clone l) (bitset r))
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
      l)))



(defn union
  "set union"
  [l r]
  (if l
    (hamf-proto/union l r)
    r))


(defn intersection
  "set intersection"
  [l r]
  (if l
    (hamf-proto/intersection l r)
    r))


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

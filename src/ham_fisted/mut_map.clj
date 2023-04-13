(ns ham-fisted.mut-map
  "Functions for working with java's mutable map interface"
  (:require [ham-fisted.function :as hamf-fn])
  (:import [java.util Map Set Collection]))


(defn compute!
  "Compute a new value in a map derived from an existing value.  bfn gets passed k, v where k
  may be nil.  If the function returns nil the corresponding key is removed from the map.

  See [Map.compute](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#compute-K-java.util.function.BiFunction-)

  An example `bfn` for counting occurrences would be `#(if % (inc (long %)) 1)`."
  [m k bfn]
  (.compute ^Map m k (hamf-fn/->bi-function bfn)))


(defn compute-if-present!
  "Compute a new value if the value already exists and is non-nil in the hashmap.  Must use
  mutable maps.  bfn gets passed k, v where v is non-nil.

  See [Map.computeIfPresent](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#computeIfPresent-K-java.util.function.BiFunction-)"
  [m k bfn]
  (.computeIfPresent ^Map m k (hamf-fn/->bi-function bfn)))


(defn compute-if-absent!
  "Compute a value if absent from the map.  Useful for memoize-type operations.  Must use
  mutable maps.  bfn gets passed k.

  See [map.computeIfAbsent](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#computeIfAbsent-K-java.util.function.Function-)"
  [m k bfn]
  (.computeIfAbsent ^Map m k (hamf-fn/->function bfn)))


(defn keyset
  "Return the keyset of the map.  This may not be in the same order as (keys m) or (vals
  m).  For hamf maps, this has the same ordering as (keys m).  For both hamf and java
  hashmaps, the returned implementation of java.util.Set has both more utility and better
  performance than (keys m)."
  ^Set [^Map m] (.keySet m))


(defn values
  "Return the values collection of the map.  This may not be in the same order as (keys m)
  or (vals m).  For hamf hashmaps, this does have the same order as (vals m)."
  ^Collection [^Map m] (.values m))

(ns ham-fisted.api
  "Mutable and immutable pathways based on bitmap trie-based hashmaps.  Mutable pathways
  implement the java.util.Map or Set interfaces respectively including the update-in-place
  pathways such as compute or computeIfPresent.

  All mutable maps or sets can be turned into their immutable counterparts via the Clojure
  `persistent!` call.  This allows you to work in a mutable space for performance
  (or ease of use) and switch to an immutable pathway when finished.  Please note that once
  you call persistent! you should never change the mutable map or set again as this will
  change the immutable view of the data. All immutable datastructures support conversion to
  transient via the `transient` call.

  Very fast versions of union, difference and intersection are provided for maps and sets
  with the map version of union and difference requiring an extra argument,
  a java.util.BiFunction or an IFn taking 2 arguments to merge the left and right sides into
  the final map.  These versions are faster than what is possible given any other map
  implementation on the JVM.  These boolean operators work across the map and set classes.

  Additionally a fast value update pathway is provided so you can update all the values in a
  given map quickly as well as a new map primitive - map-map - that allows you to transform
  a given map into a new map quickly by mapping across all the entries.

  Unlike the standard Java objects, under no circumstances is mutation-via-iterator supported."
  (:require [ham-fisted.iterator :as iterator])
  (:import [ham_fisted HashMap PersistentHashMap HashSet PersistentHashSet
            BitmapTrieCommon$HashProvider BitmapTrieCommon BitmapTrieCommon$MapSet]
           [clojure.lang ITransientAssociative2 ITransientCollection Indexed]
           [java.util Map Map$Entry List RandomAccess Set Collection]
           [java.util.function Function BiFunction BiConsumer Consumer]
           [java.util.concurrent ForkJoinPool ExecutorService])
  (:refer-clojure :exclude [assoc! conj! frequencies merge merge-with]))

(set! *warn-on-reflection* true)
(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider based on Clojure's hasheq and equiv pathways - the same pathways
that clojure's persistent datastructures use.  This is the default hash provider is somewhat
(<2x) slower than the [[equal-hash-provider]]."}
  equiv-hash-provider BitmapTrieCommon/equivHashProvider)
(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider based on Object.hashCode and Object.equals - this is the same
pathway that java.util.HashMap uses and is the overall the fastest hash provider.  Hash-based
datastructures based on this hash provider will be faster to create and access but will not
use the hasheq pathway.  This is fine for integer keys, strings, keywords, and symbols, but
differs for objects such as doubles, floats, and BigDecimals.  This is the default
hash provider."}
  equal-hash-provider BitmapTrieCommon/equalHashProvider)


(defn- options->provider
  ^BitmapTrieCommon$HashProvider [options]
  (get options :hash-provider equal-hash-provider))


(def empty-map (PersistentHashMap. (options->provider nil)))


(def ^:private objary-cls (Class/forName "[Ljava.lang.Object;"))

(defn- ->obj-ary
  ^"[Ljava.lang.Object;" [data]
  (if (instance? objary-cls data)
    data
    (object-array data)))


(defn mut-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  operations such as put!, remove!, compute-at! and comput-if-absent!.  You can create
  a persistent hashmap via the clojure `persistent!` call.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the equal-hash-provider."
  (^HashMap [] (HashMap. (options->provider nil)))
  (^HashMap [data]
   (into (HashMap. (options->provider nil)) data))
  (^HashMap [options data]
   (into (mut-map options) data)))


(defn immut-map
  "Create an immutable map.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the equal-hash-provider."
  (^PersistentHashMap [] empty-map)
  (^PersistentHashMap [data]
   (into (PersistentHashMap. (options->provider nil)) data))
  (^PersistentHashMap [options data]
   (into (PersistentHashMap. (options->provider options)) data)))


(defn java-hashmap
  "Create a java hashmap which is still the fastest possible way to solve a few problems."
  (^java.util.HashMap [] (java.util.HashMap.))
  (^java.util.HashMap [data]
   (if (instance? Map data)
     (java.util.HashMap. ^Map data)
     (let [retval (java.util.HashMap.)]
       (doseq [item data]
         (cond
           (instance? Indexed item)
           (.put retval (.nth ^Indexed item 0) (.nth ^Indexed item 1))
           (instance? Map$Entry item)
           (.put retval (.getKey ^Map$Entry item) (.getValue ^Map$Entry item))
           :else
           (throw (Exception. "Unrecognized map entry item type:" item))))
       retval))))


(defn mut-set
  "Create a mutable hashset based on the bitmap trie. You can create a persistent hashset via
  the clojure `persistent!` call.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the equal-hash-provider."
  (^HashSet [] (HashSet. (options->provider nil)))
  (^HashSet [data] (into (HashSet. (options->provider nil)) data))
  (^HashSet [options data] (into (HashSet. (options->provider options)) data)))


(defn immut-set
  "

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the equal-hash-provider."
  (^PersistentHashSet [] (PersistentHashSet. (options->provider nil)))
  (^PersistentHashSet [data] (into (PersistentHashSet. (options->provider nil)) data))
  (^PersistentHashSet [options data] (into (PersistentHashSet. (options->provider options)) data)))


(defn java-hashset
  "Create a java hashset which is still the fastest possible way to solve a few problems."
  (^java.util.HashSet [] (java.util.HashSet.))
  (^java.util.HashSet [data]
   (if (instance? Set data)
     (java.util.HashSet. ^Set data)
     (let [retval (java.util.HashSet.)]
       (doseq [item data]
         (.add retval item))
       retval))))


(defn assoc!
  "assoc! that works on transient collections, implementations of java.util.Map and
  RandomAccess java.util.List implementations.  Be sure to keep track of return value
  as some implementations return a different return value than the first argument."
  [obj k v]
  (cond
    (instance? ITransientAssociative2 obj)
    (.assoc ^ITransientAssociative2 obj k v)
    (instance? Map obj)
    (do (.put ^Map obj k v) obj)
    (instance? RandomAccess obj)
    (do (.set ^List obj k v) obj)
    :else
    (throw (Exception. "Item cannot be assoc!'d"))))


(defn conj!
  "conj! that works on transient collections, implementations of java.util.Set and
  RandomAccess java.util.List implementations.  Be sure to keep track of return value
  as some implementations return a different return value than the first argument."
  [obj val]
  (cond
    (instance? ITransientCollection obj)
    (.conj ^ITransientCollection obj val)
    (instance? Set obj)
    (do (.add ^Set obj val) obj)
    :else
    (throw (Exception. "Item cannot be conj!'d"))))


(defn ->bi-function
  "Convert an object to a java.util.BiFunction.  Object can either already be a bi-function or
  cljfn will be invoked with 2 arguments."
  ^BiFunction [cljfn]
  (if (instance? BiFunction cljfn)
    cljfn
    (reify BiFunction (apply [this a b] (cljfn a b)))))


(defn ->function
  "Convert an object to a java.util.BiFunction.  Object can either already be a bi-function or
  cljfn will be invoked with 2 arguments."
  ^Function [cljfn]
  (if (instance? Function cljfn)
    cljfn
    (reify Function (apply [this a] (cljfn a)))))


(defn compute!
  "Compute a new value in a map derived from an existing value.  bfn gets passed k, v where k
  may be nil.  If the function returns nil the corresponding key is removed from the map.

  See [Map.compute](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#compute-K-java.util.function.BiFunction-)

  An example `bfn` for counting occurances would be `#(if % (inc (long %)) 1)`."
  [m k bfn]
  (.compute ^Map m k (->bi-function bfn)))


(defn compute-if-present!
  "Compute a new value if the value already exists and is non-nil in the hashmap.  Must use
  mutable maps.  bfn gets passed k, v where v is non-nil.

  See [Map.computeIfPresent](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#computeIfPresent-K-java.util.function.BiFunction-)"
  [m k bfn]
  (.computeIfPresent ^Map m k (->bi-function bfn)))


(defn compute-if-absent!
  "Compute a value if absent from the map.  Useful for memoize-type operations.  Must use
  mutable maps.  bfn gets passed k.

  See [map.computeIfAbsent](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#computeIfAbsent-K-java.util.function.Function-)"
  [m k bfn]
  (.computeIfAbsent ^Map m k (->function bfn)))


(defn- map-set?
  [item]
  (instance? BitmapTrieCommon$MapSet item))


(defn- as-map-set
  ^BitmapTrieCommon$MapSet [item] item)


(defn- ->set
  ^Set [item]
  (cond
    (instance? Set item)
    item
    (instance? Map item)
    (.keySet ^Map item)
    :else
    (immut-set item)))


(defn map-union
  "Take the union of two maps returning a new map.  bfn is a function that takes 2 arguments,
  map1-val and map2-val and returns a new value.  Has fallback if map1 and map2 aren't backed
  by bitmap tries.

   * `bfn` - A function taking two arguments and returning one.  `+` is a fine choice.
   * `map1` - the lhs of the union.
   * `map2` - the rhs of the union.

  Returns a persistent map."
  [bfn map1 map2]
  (let [bfn (->bi-function bfn)]
    (if (and (map-set? map1) (map-set? map2))
      (.union (as-map-set map1) (as-map-set map2) bfn)
      (let [retval (HashMap. equal-hash-provider)]
        (.putAll retval map1)
        (.forEach ^Map map2 (reify BiConsumer
                              (accept [this k v]
                                (.merge retval k v bfn))))
        (persistent! retval)))))


(defn- set-map-union-bfn
  ^BiFunction [s1 s2]
  (cond
    (instance? Set s1)
    HashSet/setValueMapper
    ;; This ordering allows union to be used in place of merge for maps.
    (instance? Map s2)
    BitmapTrieCommon/rhsWins
    (instance? Map s1)
    BitmapTrieCommon/lhsWins
    :else
    HashSet/setValueMapper))


(defn union
  [s1 s2]
  (cond
    (and (map-set? s1) (map-set? s2))
    (.union (as-map-set s1) (as-map-set s2) (set-map-union-bfn s1 s2))
    (and (instance? Map s1) (instance? Map s2))
    (map-union BitmapTrieCommon/rhsWins s1 s2)
    :else
    (let [retval (HashSet. equal-hash-provider)]
      (.addAll retval s1)
      (.forEach ^Collection s2 (reify Consumer
                                 (accept [this v]
                                   (.add retval v))))
      (persistent! retval))))


(defn map-union-java-hashmap
  "Take the union of two maps returning a new map.  See documentation for [map-union].
  Returns a java.util.HashMap."
  ^java.util.HashMap [bfn ^Map lhs ^Map rhs]
  (let [bfn (->bi-function bfn)
        retval (java.util.HashMap. (.size lhs))]
    (.putAll retval lhs)
    (.forEach rhs (reify BiConsumer
                    (accept [this k v]
                      (.merge retval k v bfn))))
    retval))


(defn union-reduce-maps
  "Do an efficient union reduction across many maps using bfn to update values.  See
  documentation for [map-union].  If any of the input maps are not implementations provided
  by this library this falls backs to `(reduce (partial union-maps bfn) maps)`.

  This operator is an example of how to write a parallelized map reduction but it itself
  is only faster when you have many large maps to union into a final result.  `map-union`
  is generally faster in normal use cases.

  Options:

  * `:force-serial?` - Force the use of a serial reduction algorithm.
  * `:executor-service` - Use this executor service.  Falls back to ForkJoinPool/commonPool
     when not provided.
  * `:parallelism` - Use this many threads, must be a number from [1-1024] and defaults to
     ForkJoinPool/getCommonPoolParallelism."
  ([bfn maps options]
   (let [bfn (->bi-function bfn)]
     (if (every? map-set? maps)
       (if (get options :force-serial?)
         (PersistentHashMap/unionReduce bfn maps)
         (let [^ExecutorService executor (get options :executor-service
                                              (ForkJoinPool/commonPool))
               parallelism (long (get options :parallelism
                                      (ForkJoinPool/getCommonPoolParallelism)))]
           (PersistentHashMap/parallelUnionReduce bfn maps executor parallelism)))
       (reduce #(map-union bfn %1 %2) maps))))
  ([bfn maps]
   (union-reduce-maps bfn maps nil)))


(defn union-reduce-java-hashmap
  "Do an efficient union of many maps into a single java.util.HashMap."
  (^java.util.HashMap [bfn maps options]
   (let [maps (seq maps)]
     (if (nil? maps)
       nil
       (let [bfn (->bi-function bfn)
             retval (java.util.HashMap. ^Map (first maps))
             maps (rest maps)
             bic (reify BiConsumer
                   (accept [this k v]
                     (.merge retval k v bfn)))
             iter (.iterator ^Iterable maps)]
         (loop [c (.hasNext iter)]
           (if c
             (do
               (.forEach ^Map (.next iter) bic)
               (recur (.hasNext iter)))
             retval))))))
  (^java.util.HashMap [bfn maps]
   (union-reduce-java-hashmap bfn maps nil)))



(defn difference
  "Take the difference of two maps (or sets) returning a new map.  Return value is a map1
  (or set1) without the keys present in map2."
  [map1 map2]
  (if (and (map-set? map1) (map-set? map2))
    (.difference (as-map-set map1) (as-map-set map2))
    (if (instance? Map map1)
      (let [retval (HashMap. equal-hash-provider)
            rhs (->set map2)]
        (.forEach ^Map map1 (reify BiConsumer
                              (accept [this k v]
                                (when-not (.contains rhs k)
                                  (.put retval k v)))))
        (persistent! retval))
      (let [retval (HashSet. equal-hash-provider)
            rhs (->set map2)]
        (.forEach (->set map1)
                  (reify Consumer
                    (accept [this v]
                      (when-not (.contains rhs v)
                        (.add retval v)))))
        (persistent! retval)))))


(defn map-intersection
  "Intersect the keyspace of map1 and map2 returning a new map.  Each value is the result
  of bfn applied to the map1-value and map2-value, respectively."
  [bfn map1 map2]
  (let [bfn (->bi-function bfn)]
    (if (and (map-set? map1) (map-set? map2))
      (.intersection (as-map-set map1) (as-map-set map2) bfn)
      (let [retval (HashMap. equal-hash-provider)]
        (.forEach ^Map map1 (reify BiConsumer
                              (accept [this k v]
                                (let [vv (.getOrDefault ^Map map2 k ::failure)]
                                  (when-not (identical? vv ::failure)
                                    (.put retval k (.apply bfn v vv)))))))
        (persistent! retval)))))


(defn intersection
  "Intersect the keyspace of map1 and map2 returning a new map.  Each value is the result
  of bfn applied to the map1-value and map2-value, respectively.  When both are maps
  the keys are unioned and the values are the rhs values."
  [s1 s2]
  (cond
    (and (map-set? s1) (map-set? s2))
    (.intersection (as-map-set s1) (as-map-set s2)
                   (set-map-union-bfn s1 s2))
    (and (instance? Map s1) (instance? Map s2))
    (map-intersection BitmapTrieCommon/rhsWins s1 s2)
    :else
    (let [retval (HashSet. equal-hash-provider)
          s2 (->set s2)]
      (.forEach (->set s1)
                (reify Consumer
                  (accept [this v]
                    (when (.contains s2 v)
                      (.add retval v)))))
      (persistent! retval))))


(defn update-values
  "Immutably update all values in the map returning a new map.  bfn takes k,v and returns
  a new v. Returns new persistent map."
  [map bfn]
  (let [bfn (->bi-function bfn)]
    (if (map-set? map)
      (.immutUpdateValues (as-map-set map) bfn)
      (let [retval (HashMap. equal-hash-provider)]
        (.forEach ^Map map
                  (reify BiConsumer
                    (accept [this k v]
                      (.put retval k (.apply bfn k v)))))
        (persistent! retval)))))


(defn map-map
  "Clojure's missing piece :-).  Map over the data in src-map which must be a map or sequence
  of pairs using map-fn.  map-fn must return nil or a new key-value pair. Finally remove
  nil pairs, and return a new map.  If map-fn returns the same [k v] pair the later pair
  will overwrite the earlier pair.

  Logically the same as:

  ```clojure
  (->> src-map (map map-fn) (remove nil?) (into {}))
  ```"
  [map-fn src-map]
  (let [pair-seq (if (instance? Map src-map)
                   (.entrySet ^Map src-map)
                   src-map)
        pair-iter (.iterator ^Iterable pair-seq)
        retval (HashMap. equal-hash-provider)]
    (loop [c (.hasNext pair-iter)]
      (when c
        (let [^Map$Entry entry (.next pair-iter)
              ;;Normalize map entries so this works with java hashmaps.
              ^Indexed entry (if (instance? Indexed entry)
                               entry
                               [(.getKey entry) (.getValue entry)])
              result (map-fn entry)]
          (when result
            (.put retval (result 0) (result 1)))
          (recur (.hasNext pair-iter)))))
    (persistent! retval)))


(defn frequencies
  "Faster (9X or so) implementation of clojure.core/frequencies."
  [coll]
  (persistent!
   (reduce (fn [counts x]
             (compute! counts x BitmapTrieCommon/incBiFn)
             counts)
           (mut-map) coll)))

(defn merge
  "Merge 2 maps with the rhs values winning any intersecting keys.  Uses map-union under
  the with `BitmapTrieCommon/rhsWins`."
  ([] nil)
  ([m1] m1)
  ([m1 m2] (map-union BitmapTrieCommon/rhsWins m1 m2))
  ([m1 m2 & args]
   ;;I didn't use union-reduce here because it is only faster when you have large maps.
   ;;Else union is just fine.
   (reduce #(map-union BitmapTrieCommon/rhsWins %1 %2)
           (map-union BitmapTrieCommon/rhsWins m1 m2)
           args)))


(defn merge-with
  "Merge (union) any number of maps using `f` as the merge operator.  `f` gets passed two
  arguments, lhs-val and rhs-val and must return a new value.
  Returns a new persistent map."
  ([f] nil)
  ([f m1] m1)
  ([f m1 m2] (map-union f m1 m2))
  ([f m1 m2 & args]
   (let [f (->bi-function f)]
     (reduce #(map-union f %1 %2)
             (map-union f m1 m2)
             args))))

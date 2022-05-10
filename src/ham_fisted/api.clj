(ns ham-fisted.api
  (:import [ham_fisted HashMap PersistentHashMap BitmapTrie TransientHashMap
            BitmapTrieCommon$HashProvider BitmapTrieCommon
            BitmapTrieCommon$MapSet BitmapTrie]
           [clojure.lang ITransientAssociative2 ITransientMap Indexed MapEntry]
           [java.util Map Map$Entry List RandomAccess]
           [java.util.function Function BiFunction BiConsumer]
           [java.util.concurrent ForkJoinPool ExecutorService])
  (:refer-clojure :exclude [assoc!]))

(set! *warn-on-reflection* true)

(def empty-map PersistentHashMap/EMPTY)
(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider based on Clojure's hasheq and equiv pathways - the same pathways
that clojure's persistent datastructures use.  This is the default hash provider is somewhat
(<2x) slower than the [[equal-hash-provider]]."}
  equiv-hash-provider PersistentHashMap/equivHashProvider)
(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider based on Object.hashCode and Object.equals - this is the same
pathway that java.util.HashMap uses and is the fastest hash provider.  Hash-based
datastructures based on this hash provider will be faster to create and access but will not
use the hasheq pathway.  This is fine for integer keys, strings, keywords, and symbols, but
differs for objects such as doubles, floats, and BigDecimals."}
  equal-hash-provider BitmapTrieCommon/hashcodeProvider)



(def ^:private objary-cls (Class/forName "[Ljava.lang.Object;"))

(defn- ->obj-ary
  ^"[Ljava.lang.Object;" [data]
  (if (instance? objary-cls data)
    data
    (object-array data)))


(defn mut-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  operations such as put!, remove!, compute-at! and comput-if-absent!."
  (^HashMap [] (HashMap.))
  (^HashMap [k v]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k v)
     hm))
  (^HashMap [k1 v1 k2 v2]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k1 v1)
     (.put hm k2 v2)
     hm))
  (^HashMap [k1 v1 k2 v2 k3 v3]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k1 v1)
     (.put hm k2 v2)
     (.put hm k3 v3)
     hm))
  (^HashMap [k1 v1 k2 v2 k3 v3 k4 v4]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k1 v1)
     (.put hm k2 v2)
     (.put hm k3 v3)
     (.put hm k4 v4)
     hm))
  (^HashMap [k1 v1 k2 v2 k3 v3 k4 v4 & args]
   (HashMap. PersistentHashMap/equivHashProvider
             false
             (object-array (concat [k1 k1 k2 v2 k3 v3 k4 v4] args)))))


(defn mut-mapv
  "Create a mutable implementation of java.util.HashMap optionally providing a
  hashcode provider.

  Options:

  * `:hash-provider` - Either [[equiv-hash-provider]] or [[equal-hash-provider]].  See related
  documentation."
  (^HashMap [args options]
   (HashMap. (get options :hash-provider PersistentHashMap/equivHashProvider)
             false
             (->obj-ary args)))
  (^HashMap [args]
   (mut-mapv args nil)))


(defn immut-map
  "Create an immutable map.  For small maps (<= 4 keys) using Clojure's hash-map pathway is
  the fastest - this pathway is guaranteed to produce an ham-fisted persistent hashmap."
  (^PersistentHashMap [] empty-map)
  (^PersistentHashMap [k v]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k v)
     (persistent! hm)))
  (^PersistentHashMap [k1 v1 k2 v2]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k1 v1)
     (.put hm k2 v2)
     (persistent! hm)))
  (^PersistentHashMap [k1 v1 k2 v2 k3 v3]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k1 v1)
     (.put hm k2 v2)
     (.put hm k3 v3)
     (persistent! hm)))
  (^PersistentHashMap [k1 v1 k2 v2 k3 v3 k4 v4]
   (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
     (.put hm k1 v1)
     (.put hm k2 v2)
     (.put hm k3 v3)
     (.put hm k4 v4)
     (persistent! hm)))
  (^PersistentHashMap [k1 v1 k2 v2 k3 v3 k4 v4 & args]
   (PersistentHashMap. (object-array (concat [k1 k1 k2 v2 k3 v3 k4 v4] args)))))


(defn immut-mapv
  "Create an immutable map from args which must have length divisible by 2.

    Options:

  * `:hash-provider` - Either [[equiv-hash-provider]] or [[equal-hash-provider]].  See related
  documentation, defaults to `equiv-hash-provider`."
  (^PersistentHashMap [args options]
   (PersistentHashMap. (get options :hash-provider equiv-hash-provider)
                       false
                       (->obj-ary args)))
  (^PersistentHashMap [args]
   (immut-mapv args)))


(defn persistent-hash-map
  "Create an empty persistent hash map optionally overriding the hash provider.

  Options:

  * `:hash-provider` - Either [[equiv-hash-provider]] or [[equal-hash-provider]].  See related
  documentation, defaults to `equiv-hash-provider`."
  (^PersistentHashMap
   [options]
   (-> (HashMap. ^BitmapTrieCommon$HashProvider (get options :hash-provider equiv-hash-provider))
       (.persistent)))
  (^PersistentHashMap [] empty-map))


(defn hamf-hash-map
  "Create an bitmap trie hash map optionally overriding the hash provider.

  Options:

  * `:hash-provider` - Either [[equiv-hash-provider]] or [[equal-hash-provider]].  See related
  documentation, defaults to `equiv-hash-provider`."
  (^HashMap [options] (HashMap. ^BitmapTrieCommon$HashProvider (get options :hash-provider equiv-hash-provider)))
  (^HashMap [] (HashMap. equiv-hash-provider)))


(defn java-hash-map
  (^java.util.HashMap [] (java.util.HashMap.))
  (^java.util.HashMap [k1 v1]
   (doto (java.util.HashMap.)
     (.put k1 v1)))
  (^java.util.HashMap [k1 v1 k2 v2]
   (doto (java.util.HashMap.)
     (.put k1 v1)
     (.put k2 v2)))
  (^java.util.HashMap [k1 v1 k2 v2 k3 v3]
   (doto (java.util.HashMap.)
     (.put k1 v1)
     (.put k2 v2)
     (.put k3 v3)))

  (^java.util.HashMap [k1 v1 k2 v2 k3 v3 k4 v4]
   (doto (java.util.HashMap.)
     (.put k1 v1)
     (.put k2 v2)
     (.put k3 v3)
     (.put k4 v4)))

  (^java.util.HashMap [k1 v1 k2 v2 k3 v3 k4 v4 & args]
   (when-not (== 0 (rem (count args) 2))
     (throw (Exception. "Map arguments must be evenly divisible by 2")))
   (let [hm (doto (java.util.HashMap.)
              (.put k1 v1)
              (.put k2 v2)
              (.put k3 v3)
              (.put k4 v4))]
     (doseq [[k v] (partition 2 args)]
       (.put hm k v))
     hm)))


(defn assoc!
  [obj k v]
  (cond
    (instance? ITransientAssociative2 obj)
    (.assoc ^ITransientAssociative2 obj k v)
    (instance? Map obj)
    (do (.put ^Map obj k v) obj)
    (instance? RandomAccess obj)
    (.set ^List obj k v)))


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
      (let [retval (HashMap. equiv-hash-provider)]
        (.putAll retval map1)
        (.forEach ^Map map2 (reify BiConsumer
                              (accept [this k v]
                                (.merge retval k v bfn))))
        (persistent! retval)))))


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


(defn map-difference
  "Take the difference of two maps returning a new map.  Return value is a map1 without the
  keys present in map2."
  [map1 map2]
  (if (and (map-set? map1) (map-set? map2))
    (.difference (as-map-set map1) (as-map-set map2))
    (let [retval (HashMap. equiv-hash-provider)]
      (.forEach ^Map map1 (reify BiConsumer
                            (accept [this k v]
                              (when-not (.containsKey ^Map map2 k)
                                (.put retval k v)))))
      (persistent! retval))))


(defn map-intersection
  "Intersect the keyspace of map1 and map2 returning a new map.  Each value is the result
  of bfn applied to the map1-value and map2-value, respectively."
  [bfn map1 map2]
  (let [bfn (->bi-function bfn)]
    (if (and (map-set? map1) (map-set? map2))
      (.intersection (as-map-set map1) (as-map-set map2) bfn)
      (let [retval (HashMap. equiv-hash-provider)]
        (.forEach ^Map map1 (reify BiConsumer
                              (accept [this k v]
                                (let [vv (.getOrDefault ^Map map2 k ::failure)]
                                  (when-not (identical? vv ::failure)
                                    (.put retval k (.apply bfn v vv)))))))
        (persistent! retval)))))


(defn update-values
  "Immutably update all values in the map returning a new map.  bfn takes k,v and returns
  a new v. Returns new persistent map."
  [map bfn]
  (let [bfn (->bi-function bfn)]
    (if (map-set? map)
      (.immutUpdateValues (as-map-set map) bfn)
      (let [retval (HashMap. equiv-hash-provider)]
        (.forEach ^Map map
                  (reify BiConsumer
                    (accept [this k v]
                      (.put retval k (.apply bfn k v)))))
        (persistent! retval)))))


(defn map-map
  "Clojure's missing piece :-).  Given a map or sequence of pairs map using map-fn which must
  return a new pair.  Removing nil pairs, and return a new map.  If map-fn returns the same
  [k v] pair the later pair will overwrite the earlier pair.

  Logically the same as:

  ```clojure
  (->> src-map (map map-fn) (remove nil?) (into {}))
  ```"
  [map-fn src-map]
  (let [pair-seq (if (instance? Map src-map)
                   (.entrySet ^Map src-map)
                   src-map)
        pair-iter (.iterator ^Iterable pair-seq)
        retval (HashMap. equiv-hash-provider)]
    (loop [c (.hasNext pair-iter)]
      (when c
        (let [^Map$Entry entry (.next pair-iter)
              ;;Normalize map entries so this works with java hashmaps.
              entry (if (instance? Indexed entry)
                      entry
                      [(.getKey entry) (.getValue entry)])
              result (map-fn entry)]
          (when result
            (.put retval (result 0) (result 1)))
          (recur (.hasNext pair-iter)))))
    (persistent! retval)))

(ns ham-fisted.api
  "Fast mutable and immutable associative data structures based on bitmap trie
  hashmaps. Mutable pathways implement the `java.util.Map` or `Set` interfaces
  including in-place update features such as compute or computeIfPresent.

  Mutable maps or sets can be turned into their immutable counterparts via the
  Clojure `persistent!` call. This allows working in a mutable space for
  convenience and performance then switching to an immutable pathway when
  necessary. Note: after `persistent!` one should never backdoor mutate map or
  set again as this will break the contract of immutability Immutable
  data structures also support conversion to transient via `transient`.

  Map keysets (`.keySet`) are full `PersistentHashSet`s of keys.

  Maps and sets support metadata but setting the metadata on mutable objects
  returns a new mutable object that shares the backing store leading to possible
  issues. Metadata is transferred to the persistent versions of the
  mutable/transient objects upon `persistent!`.

  Very fast versions of union, difference and intersection are provided for maps
  and sets with the map version of union and difference requiring an extra
  argument, a `java.util.BiFunction` or an `IFn` taking 2 arguments to merge the
  left and right sides into the final map. These implementations of union,
  difference, and intersection are the fastest implementation of these
  operations we know of on the JVM.

  Additionally a fast value update pathway is provided, enabling quickly
  updating all the values in a given map. Additionally, a new map primitive
  - [[mapmap]] - allows transforming a given map into a new map quickly by
  mapping across all the entries.

  Unlike the standard Java objects, mutation-via-iterator is not supported."
  (:require [ham-fisted.iterator :as iterator])
  (:import [ham_fisted HashMap PersistentHashMap HashSet PersistentHashSet
            BitmapTrieCommon$HashProvider BitmapTrieCommon BitmapTrieCommon$MapSet
            BitmapTrieCommon$Box PersistentArrayMap ObjArray]
           [clojure.lang ITransientAssociative2 ITransientCollection Indexed
            IEditableCollection RT IPersistentMap Associative Util IFn ArraySeq]
           [java.util Map Map$Entry List RandomAccess Set Collection ArrayList]
           [java.util.function Function BiFunction BiConsumer Consumer]
           [java.util.concurrent ForkJoinPool ExecutorService Callable Future
            ConcurrentHashMap])
  (:refer-clojure :exclude [assoc! conj! frequencies merge merge-with memoize
                            into assoc-in get-in update assoc update-in hash-map]))


(set! *warn-on-reflection* true)


(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider based on Clojure's hasheq and equiv pathways - the
  same algorithm that Clojure's persistent data structures use. This hash
  provider is somewhat (<2x) slower than the [[equal-hash-provider]]."}
  equiv-hash-provider BitmapTrieCommon/equivHashProvider)
(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider based on Object.hashCode and Object.equals - this is
the same pathway that `java.util.HashMap` uses and is the overall the fastest
hash provider.  Hash-based data structures based on this hash provider will be
faster to create and access but will not use the hasheq pathway. This is fine
for integer keys, strings, keywords, and symbols, but differs for objects such
as doubles, floats, and BigDecimals. This is the default hash provider."}
equal-hash-provider BitmapTrieCommon/equalHashProvider)


(defn- options->provider
  ^BitmapTrieCommon$HashProvider [options]
  (get options :hash-provider equal-hash-provider))


(def ^{:tag PersistentArrayMap} empty-map PersistentArrayMap/EMPTY)
(def ^{:tag PersistentHashSet} empty-set (PersistentHashSet. (options->provider nil)))


(declare assoc! conj!)


(defn- empty-map?
  [m]
  (or (nil? m)
      (and (instance? Map m)
           (== 0 (.size ^Map m)))))

(defn assoc
  "Drop in faster or equivalent replacement for clojure.core/assoc especially for
  small numbers of keyval pairs."
  ([m a b]
   (if (empty-map? m)
     (PersistentArrayMap. equal-hash-provider a b (meta m))
     (.assoc ^Associative m a b)))
  ([m a b c d]
   (if (and (empty-map? m) (PersistentArrayMap/different equal-hash-provider a c))
     (PersistentArrayMap. equal-hash-provider a b c d (meta m))
     (-> (assoc! (transient (or m PersistentArrayMap/EMPTY)) a b)
         (assoc! c d)
         (persistent!))))
  ([m a b c d e f]
   (if (and (empty-map? m) (PersistentArrayMap/different equal-hash-provider a c e))
     (PersistentArrayMap. equal-hash-provider a b c d e f (meta m))
     (-> (transient (or m PersistentArrayMap/EMPTY))
         (assoc! a b)
         (assoc! c d)
         (assoc! e f)
         (persistent!))))
  ([m a b c d e f g h]
   (if (and (empty-map? m) (PersistentArrayMap/different equal-hash-provider a c e g))
     (PersistentArrayMap. equal-hash-provider a b c d e f g h (meta m))
     (-> (transient (or m (PersistentArrayMap/EMPTY)))
         (assoc! a b)
         (assoc! c d)
         (assoc! e f)
         (assoc! g h)
         (persistent!))))
  ([m a b c d e f g h & args]
   (when-not (== 0 (rem (count args) 2))
     (throw (Exception. "Assoc takes an odd number of arguments.")))
   (if (empty-map? m)
     (let [m (HashMap. equal-hash-provider
                       ^IPersistentMap (meta m)
                       (+ 4 (quot (count args) 2)))]
       (.put m a b)
       (.put m c d)
       (.put m e f)
       (.put m g h)
       (.putAll m (iterator/array-seq-ary args))
       (persistent! m))
     (loop [m (-> (transient m)
                  (assoc! a b)
                  (assoc! c d)
                  (assoc! e f)
                  (assoc! g h))
            args args]
       (if args
         (let [k (RT/first args)
               args (RT/next args)
               v (RT/first args)]
           (recur (assoc! m k v) (RT/next args)))
         (persistent! m))))))


(def ^:private obj-ary-cls (Class/forName "[Ljava.lang.Object;"))

(defn- ->obj-ary
  ^objects [data]
  (cond
    (instance? obj-ary-cls data)
    data
    (instance? Collection data)
    (.toArray ^Collection data)
    :else
    (object-array data)))


(defn obj-ary
  "As quickly as possible, produce an object array from these inputs.  Very fast for arities
  <= 6."
  (^objects [] (object-array 0))
  (^objects [v0] (ObjArray/create v0))
  (^objects [v0 v1] (ObjArray/create v0 v1))
  (^objects [v0 v1 v2] (ObjArray/create v0 v1 v2))
  (^objects [v0 v1 v2 v3] (ObjArray/create v0 v1 v2 v3))
  (^objects [v0 v1 v2 v3 v4] (ObjArray/create v0 v1 v2 v3 v4))
  (^objects [v0 v1 v2 v3 v4 v5] (ObjArray/create v0 v1 v2 v3 v4 v5))
  (^objects [v0 v1 v2 v3 v4 v5 & args]
   (ObjArray/create v0 v1 v2 v3 v4 v5 (iterator/array-seq-ary args))))


(defn into
  "Like clojure.core/into, but also designed to handle editable collections,
  transients, and base java.util.Map, List and Set containers."
  ([container data]
   (cond
     (instance? IEditableCollection container)
     (-> (reduce conj! (transient container) data)
         (persistent!))
     (instance? ITransientCollection container)
     (reduce conj! container data)
     (instance? Map container)
     (if (instance? Map data)
       (do (.putAll ^Map container ^Map data) container)
       (reduce conj! container data))
     (instance? Collection container)
     (if (instance? Collection data)
       (do (.addAll ^Collection container ^Collection data) container)
       (reduce conj! container data))
     :else
     (throw (Exception. (str "Unable to ascertain container type: " (type container))))))
  ([container xform data]
   (into container (eduction xform data))))


(defn mut-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  operations such as put!, remove!, compute-at! and compute-if-absent!.  You can create
  a persistent hashmap via the clojure `persistent!` call.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[equal-hash-provider]]."
  (^HashMap [] (HashMap. (options->provider nil)))
  (^HashMap [data]
   (into (HashMap. (options->provider nil)) data))
  (^HashMap [options data]
   (into (mut-map options) data)))


(defn immut-map
  "Create an immutable map.  This object supports conversion to a transient map via
  Clojure's `transient` function.  Duplicate keys are treated as if by assoc.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[equal-hash-provider]].

  Examples:

  ```clojure
ham-fisted.api> (immut-map (obj-ary :a 1 :b 2 :c 3 :d 4))
{:a 1, :b 2, :c 3, :d 4}
ham-fisted.api> (type *1)
ham_fisted.PersistentArrayMap
ham-fisted.api> (immut-map (obj-ary :a 1 :b 2 :c 3 :d 4 :e 5))
{:d 4, :b 2, :c 3, :a 1, :e 5}
ham-fisted.api> (type *1)
ham_fisted.PersistentHashMap
ham-fisted.api> (immut-map [[:a 1][:b 2][:c 3][:d 4][:e 5]])
{:d 4, :b 2, :c 3, :a 1, :e 5}
ham-fisted.api> (type *1)
ham_fisted.PersistentHashMap
```"
  (^PersistentHashMap [] empty-map)
  (^PersistentHashMap [data]
   (immut-map nil data))
  (^PersistentHashMap [options data]
   (if (instance? obj-ary-cls data)
     (PersistentHashMap/create (options->provider options) false ^objects data)
     (into (PersistentHashMap. (options->provider options)) data))))


(defn hash-map
  "Drop-in replacement to Clojure's hash-map function."
  ([] empty-map)
  ([a b] (PersistentArrayMap. equal-hash-provider a b nil))
  ([a b c d]
   (if (PersistentArrayMap/different equal-hash-provider a c)
     (PersistentArrayMap. equal-hash-provider a b c d nil)
     (hash-map c d)))
  ([a b c d e f]
   (if (PersistentArrayMap/different equal-hash-provider a c e)
     (PersistentArrayMap. equal-hash-provider a b c d e f nil)
     (PersistentHashMap/create equal-hash-provider false (obj-ary a b c d e f))))
  ([a b c d e f g h]
   (if (PersistentArrayMap/different equal-hash-provider a c e g)
     (PersistentArrayMap. equal-hash-provider a b c d e f g h nil)
     (PersistentHashMap/create equal-hash-provider false (obj-ary a b c d e f g h))))
  ([a b c d e f g h & args]
   (PersistentHashMap/create equal-hash-provider false (obj-ary a b c d e f g h args))))


(defn java-hashmap
  "Create a java.util.HashMap.  Duplicate keys are treated as if map was created by assoc."
  (^java.util.HashMap [] (java.util.HashMap.))
  (^java.util.HashMap [data]
   (if (instance? Map data)
     (java.util.HashMap. ^Map data)
     (let [retval (java.util.HashMap.)]
       (iterator/doiter
        item data
        (cond
          (instance? Indexed item)
          (.put retval (.nth ^Indexed item 0) (.nth ^Indexed item 1))
          (instance? Map$Entry item)
          (.put retval (.getKey ^Map$Entry item) (.getValue ^Map$Entry item))
          :else
          (throw (Exception. "Unrecognized map entry item type:" item))))
       retval))))


(defn java-concurrent-hashmap
  "Create a java concurrent hashmap which is still the fastest possible way to solve a
  few concurrent problems."
  (^ConcurrentHashMap [] (ConcurrentHashMap.))
  (^ConcurrentHashMap [data] (into (ConcurrentHashMap.) data)))


(defn mut-set
  "Create a mutable hashset based on the bitmap trie. You can create a persistent hashset via
  the clojure `persistent!` call.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[equal-hash-provider]]."
  (^HashSet [] (HashSet. (options->provider nil)))
  (^HashSet [data] (into (HashSet. (options->provider nil)) data))
  (^HashSet [options data] (into (HashSet. (options->provider options)) data)))


(defn immut-set
  "Create an immutable hashset based on the bitmap trie.  This object supports conversion
  to transients via `transient`.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[equal-hash-provider]]."
  (^PersistentHashSet [] empty-set)
  (^PersistentHashSet [data] (into (PersistentHashSet. (options->provider nil)) data))
  (^PersistentHashSet [options data] (into (PersistentHashSet. (options->provider options)) data)))


(defn java-hashset
  "Create a java hashset which is still the fastest possible way to solve a few problems."
  (^java.util.HashSet [] (java.util.HashSet.))
  (^java.util.HashSet [data] (into (java.util.HashSet.) data)))


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
    (do (.set ^List obj (int k) v) obj)
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
    (instance? List obj)
    (do (.add ^List obj val) obj)
    :else
    (throw (Exception. "Item cannot be conj!'d"))))


(defn ->bi-function
  "Convert an object to a java.util.BiFunction. Object can either already be a
  bi-function or an IFn to be invoked with 2 arguments."
  ^BiFunction [cljfn]
  (if (instance? BiFunction cljfn)
    cljfn
    (reify BiFunction (apply [this a b] (cljfn a b)))))


(defn ->function
  "Convert an object to a java Function. Object can either already be a
  Function or an IFn to be invoked."
  ^Function [cljfn]
  (if (instance? Function cljfn)
    cljfn
    (reify Function (apply [this a] (cljfn a)))))


(defn compute!
  "Compute a new value in a map derived from an existing value.  bfn gets passed k, v where k
  may be nil.  If the function returns nil the corresponding key is removed from the map.

  See [Map.compute](https://docs.oracle.com/javase/8/docs/api/java/util/Map.html#compute-K-java.util.function.BiFunction-)

  An example `bfn` for counting occurrences would be `#(if % (inc (long %)) 1)`."
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


(defn clear!
  "Mutably clear a map, set, list or implementation of java.util.Collection."
  [map-or-coll]
  (cond
    (instance? Map map-or-coll)
    (.clear ^Map map-or-coll)
    (instance? Collection map-or-coll)
    (.clear ^Collection map-or-coll))
  map-or-coll)


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


(defn- ->collection
  ^Collection [item]
  (cond
    (instance? Collection item)
    item
    (instance? Map item)
    (.entrySet ^Map item)
    :else
    (vec item)))


(defn map-union
  "Take the union of two maps returning a new map.  bfn is a function that takes 2 arguments,
  map1-val and map2-val and returns a new value.  Has fallback if map1 and map2 aren't backed
  by bitmap tries.

   * `bfn` - A function taking two arguments and returning one.  `+` is a fine choice.
   * `map1` - the lhs of the union.
   * `map2` - the rhs of the union.

  Returns a persistent map."
  [bfn map1 map2]
  (cond
    (nil? map1) map2
    (nil? map2) map1
    :else
    (let [bfn (->bi-function bfn)]
      (if (and (map-set? map1) (map-set? map2))
        (.union (as-map-set map1) (as-map-set map2) bfn)
        (let [retval (mut-map map1)]
          (.forEach ^Map map2 (reify BiConsumer
                                (accept [this k v]
                                  (.merge retval k v bfn))))
          (persistent! retval))))))


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
  "Union of two sets or two maps.  When two maps are provided the right hand side
  wins in the case of an intersection.

  Result is either a set or a map, depending on if s1 is a set or map."
  [s1 s2]
  (cond
    (nil? s1) s2
    (nil? s2) s1
    (and (map-set? s1) (map-set? s2))
    (.union (as-map-set s1) (as-map-set s2) (set-map-union-bfn s1 s2))
    (and (instance? Map s1) (instance? Map s2))
    (map-union BitmapTrieCommon/rhsWins s1 s2)
    :else
    (let [retval (mut-set s1)]
      (.forEach ^Collection s2 (reify Consumer
                                 (accept [this v]
                                   (.add retval v))))
      (persistent! retval))))


(defn map-union-java-hashmap
  "Take the union of two maps returning a new map.  See documentation for [map-union].
  Returns a java.util.HashMap."
  ^java.util.HashMap [bfn ^Map lhs ^Map rhs]
  (let [bfn (->bi-function bfn)
        retval (java-hashmap lhs)]
    (.forEach rhs (reify BiConsumer
                    (accept [this k v]
                      (.merge retval k v bfn))))
    retval))


(defn union-reduce-maps
  "Do an efficient union reduction across many maps using bfn to update values.  See
  documentation for [map-union].  If any of the input maps are not implementations provided
  by this library this falls backs to `(reduce (partial union-maps bfn) maps)`.

  This operator is an example of how to write a parallelized map reduction but it itself
  is only faster when you have many large maps to union into a final result.  [map-union]
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
             retval (java-hashmap (first maps))
             maps (rest maps)
             bic (reify BiConsumer
                   (accept [this k v]
                     (.merge retval k v bfn)))]
         (reduce #(do (.forEach ^Map %2 bic)
                      retval)
                 retval
                 maps)))))
  (^java.util.HashMap [bfn maps]
   (union-reduce-java-hashmap bfn maps nil)))


(defn difference
  "Take the difference of two maps (or sets) returning a new map.  Return value is a map1
  (or set1) without the keys present in map2."
  [map1 map2]
  (if (nil? map2)
    map1
    (if (and (map-set? map1) (map-set? map2))
      (.difference (as-map-set map1) (as-map-set map2))
      (if (instance? Map map1)
        (let [retval (mut-map)
              rhs (->set map2)]
          (.forEach ^Map map1 (reify BiConsumer
                                (accept [this k v]
                                  (when-not (.contains rhs k)
                                    (.put retval k v)))))
          (persistent! retval))
        (let [retval (mut-set)
              rhs (->set map2)]
          (.forEach (->collection map1)
                    (reify Consumer
                      (accept [this v]
                        (when-not (.contains rhs v)
                          (.add retval v)))))
          (persistent! retval))))))


(defn map-intersection
  "Intersect the keyspace of map1 and map2 returning a new map.  Each value is the result
  of bfn applied to the map1-value and map2-value, respectively.  See documentation for
  [[map-union]].

  Clojure's `merge` functionality can be duplicate via:

  ```clojure
  (map-intersection (fn [lhs rhs] rhs) map1 map2)
  ```"
  [bfn map1 map2]
  (if (or (nil? map2) (nil? map2))
    empty-map
    (let [bfn (->bi-function bfn)]
      (if (and (map-set? map1) (map-set? map2))
        (.intersection (as-map-set map1) (as-map-set map2) bfn)
        (let [retval (mut-map)]
          (.forEach ^Map map1 (reify BiConsumer
                                (accept [this k v]
                                  (let [vv (.getOrDefault ^Map map2 k ::failure)]
                                    (when-not (identical? vv ::failure)
                                      (.put retval k (.apply bfn v vv)))))))
          (persistent! retval))))))


(defn intersection
  "Intersect the keyspace of set1 and set2 returning a new set.  Also works if s1 is a
  map and s2 is a set - the map is trimmed to the intersecting keyspace of s1 and s2."
  [s1 s2]
  (cond
    (or (nil? s1) (nil? s2)) empty-set
    (and (map-set? s1) (map-set? s2))
    (.intersection (as-map-set s1) (as-map-set s2)
                   (set-map-union-bfn s1 s2))
    (and (instance? Map s1) (instance? Map s2))
    (map-intersection BitmapTrieCommon/rhsWins s1 s2)
    :else
    (let [retval (mut-set)
          s2 (->set s2)]
      (.forEach (->set s1)
                (reify Consumer
                  (accept [this v]
                    (when (.contains s2 v)
                      (.add retval v)))))
      (persistent! retval))))


(defn update-values
  "Immutably update all values in the map returning a new map.  bfn takes 2 arguments,
  k,v and returns a new v. Returns new persistent map."
  [map bfn]
  (let [bfn (->bi-function bfn)]
    (if (map-set? map)
      (.immutUpdateValues (as-map-set map) bfn)
      (let [retval (mut-map)]
        (.forEach ^Map map
                  (reify BiConsumer
                    (accept [this k v]
                      (.put retval k (.apply bfn k v)))))
        (persistent! retval)))))


(defn mapmap
  "Clojure's missing piece. Map over the data in src-map, which must be a map or
  sequence of pairs, using map-fn. map-fn must return either a new key-value
  pair or nil. Then, remove nil pairs, and return a new map. If map-fn returns
  more than one pair with the same key later pair will overwrite the earlier
  pair.

  Logically the same as:

  ```clojure
  (->> (map map-fn src-map) (remove nil?) (into {}))
  ```"
  [map-fn src-map]
  (let [pair-seq (if (instance? Map src-map)
                   (.entrySet ^Map src-map)
                   src-map)
        retval (HashMap. equal-hash-provider)]
    (iterator/doiter
     entry pair-seq
     (let [;;Normalize map entries so this works with java hashmaps.
           ^Indexed entry (if (instance? Indexed entry)
                            entry
                            [(.getKey ^Map$Entry entry)
                             (.getValue ^Map$Entry entry)])
           ^Indexed result (map-fn entry)]
          (when result
            (.put retval (.nth result 0) (.nth result 1)))))
    (persistent! retval)))


(defn- pfor
  [n-elems body-fn]
  (let [parallelism (ForkJoinPool/getCommonPoolParallelism)
        pool (ForkJoinPool/commonPool)
        n-elems (long n-elems)
        gsize (quot n-elems parallelism)
        leftover (rem n-elems parallelism)]
    (->> (range parallelism)
         (mapv (fn [^long pidx]
                 (let [start-idx (long (+ (* pidx gsize)
                                          (min pidx leftover)))
                       end-idx (long (+ (+ start-idx gsize)
                                        (long (if (< pidx leftover)
                                                1 0))))]
                   (.submit pool ^Callable #(body-fn start-idx end-idx)))))
         (eduction (map #(.get ^Future %))))))


(defn ^:no-doc pfrequencies
  "Parallelized frequencies. Can be faster for large random-access containers."
  [tdata]
  (let [n-elems (count tdata)]
    (->> (pfor n-elems
               (fn [^long start-idx ^long end-idx]
                 (let [gsize (- end-idx start-idx)
                       hm (mut-map)]
                   (dotimes [idx gsize]
                     (.compute hm (tdata (+ idx start-idx)) BitmapTrieCommon/incBiFn))
                   hm)))
         (reduce #(map-union BitmapTrieCommon/sumBiFn %1 %2)))))


(defn frequencies
  "Faster (9X or so), implementation of clojure.core/frequencies."
  [coll]
  (persistent!
   (reduce (fn [counts x]
             (compute! counts x BitmapTrieCommon/incBiFn)
             counts)
           (mut-map) coll)))


(defn ^:no-doc pfrequencies-current-hashmap
  "Parallelized frequencies.  Used for testing concurrentCompute functionality -
  tdata must be random access and countable."
  [tdata]
  (let [n-elems (count tdata)
        hm (java.util.concurrent.ConcurrentHashMap.)]
    (pfor n-elems
          (fn [^long start-idx ^long end-idx]
            (let [gsize (- end-idx start-idx)]
              (dotimes [idx gsize]
                (.compute hm (tdata (+ idx start-idx))
                          BitmapTrieCommon/incBiFn)))))))

(defn merge
  "Merge 2 maps with the rhs values winning any intersecting keys.  Uses map-union
  with `BitmapTrieCommon/rhsWins`.

  Returns a new persistent map."
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


(defn memoize
  "Efficient thread-safe version of clojure.core/memoize.  Unlike
  clojure.core/memoize this version guarantees memo-fn will be called exactly
  once per argument vector even in high-contention environments.

  Also see [[clear-memoized-fn!]] to mutably clear the backing store."
  [memo-fn]
  (let [hm (ConcurrentHashMap.)
        compute-fn (reify Function
                     (apply [this argv]
                       ;;wrapping in a vector to handle null case
                       (BitmapTrieCommon$Box.
                        (case (count argv)
                          0 (memo-fn)
                          1 (memo-fn (argv 0))
                          2 (memo-fn (argv 0) (argv 1))
                          3 (memo-fn (argv 0) (argv 1) (argv 2))
                          4 (memo-fn (argv 0) (argv 1) (argv 2) (argv 3))
                          (apply memo-fn argv)))))]
    (vary-meta
     (fn
       ([] (.obj ^BitmapTrieCommon$Box (.computeIfAbsent hm [] compute-fn)))
       ([v0] (.obj ^BitmapTrieCommon$Box (.computeIfAbsent hm [v0] compute-fn)))
       ([v0 v1] (.obj ^BitmapTrieCommon$Box (.computeIfAbsent hm [v0 v1] compute-fn)))
       ([v0 v1 v2] (.obj ^BitmapTrieCommon$Box (.computeIfAbsent hm [v0 v1 v2] compute-fn)))
       ([v0 v1 v2 v3]
        (.obj ^BitmapTrieCommon$Box (.computeIfAbsent hm [v0 v1 v2 v3] compute-fn)))
       ([v0 v1 v2 v3 & args]
        (.obj ^BitmapTrieCommon$Box (.computeIfAbsent hm (into [v0 v1 v2 v3] args) compute-fn))))
     assoc :cache hm)))


(defn clear-memoized-fn!
  "Clear a memoized function backing store."
  [memoize-fn]
  (if-let [map (get (meta memoize-fn) :cache)]
    (clear! map)
    (throw (Exception. (str "Arg is not a memoized fn - " memoize-fn))))
  memoize-fn)


(defn map-factory
  "Create a factory to quickly produce maps with a fixed set of keys but arbitrary
  values.  Returned IFn is same arity as the number of keys passed in and the values
  will be set as values of the hashmap.  The factory produces PersistentHashMaps."
  ([] (constantly empty-map))
  ([k0] (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0))]
          (fn [v0] (.apply mf (obj-ary v0)))))
  ([k0 k1] (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0 k1))]
             (fn [v0 v1] (.apply mf (obj-ary v0 v1)))))
  ([k0 k1 k2] (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0 k1 k2))]
                (fn [v0 v1 v2] (.apply mf (obj-ary v0 v1 v2)))))
  ([k0 k1 k2 k3] (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0 k1 k2 k3))]
                   (fn [v0 v1 v2 v3] (.apply mf (obj-ary v0 v1 v2 v3)))))
  ([k0 k1 k2 k3 k4] (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0 k1 k2 k3 k4))]
                      (fn [v0 v1 v2 v3 v4] (.apply mf (obj-ary v0 v1 v2 v3 v4)))))
  ([k0 k1 k2 k3 k4 k5] (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0 k1 k2 k3 k4 k5))]
                         (fn [v0 v1 v2 v3 v4 v5] (.apply mf (obj-ary v0 v1 v2 v3 v4 v5)))))
  ([k0 k1 k2 k3 k4 k5 & args]
   (let [mf (PersistentHashMap/makeFactory equal-hash-provider (obj-ary k0 k1 k2 k3 k4 k5 args))]
     (fn [v0 v1 v2 v3 v4 v5 & args] (.apply mf (obj-ary v0 v1 v2 v3 v4 v5 args))))))


(defn map-factoryv
  "Create a factory to quickly produce maps with a fixed set of keys but arbitrary
  values.  This version takes a vector or sequence of keys and returns and IFn that
  takes a vector, object-array, or sequence of values.  The most efficient pathway will be
  if values are already in an object array.

  The factory produces PersistentHashMaps."
  [keys]
  (let [mf (PersistentHashMap/makeFactory equal-hash-provider (->obj-ary keys))]
    (fn [vals] (.apply mf (->obj-ary vals)))))

(defn- assoc-inv
  [m ks ^long ksoff v]
  (let [ksc (unchecked-subtract (count ks) ksoff)]
    (case ksc
      0 (assoc m nil v)
      1 (assoc m (ks ksoff) v)
      2 (let [k0 (ks ksoff)
              k1 (ks (unchecked-add ksoff 1))]
          (assoc m k0 (assoc (get m k0) k1 v)))
      3 (let [k0 (ks ksoff)
              k1 (ks (unchecked-add ksoff 1))
              k2 (ks (unchecked-add ksoff 2))
              m1 (get m k0)]
          (->> (assoc (get m1 k1) k2 v)
               (assoc m1 k1)
               (assoc m k0)))
      (assoc m (ks ksoff) (assoc-inv (get m (ks ksoff)) ks (unchecked-inc ksoff) v)))))


(defmacro assoc-in
  "Assoc-in - more efficient replacement if ks is a known compile time constant
  or a vector."
  [m ks v]
  (if (vector? ks)
    (if (== 0 (count ks))
      `(assoc ~m nil ~v)
      (let [nargs (count ks)
            nnargs (dec nargs)]
        `(let [~@(->> (range nargs)
                      (mapcat (fn [argidx]
                                [(symbol (str "k" argidx)) (ks argidx)])))
               ~@(->> (range nargs)
                      (mapcat (fn [argidx]
                                [(symbol (str "m" argidx))
                                 (if (== 0 argidx)
                                   `~m
                                   `(get ~(symbol (str "m" (dec argidx)))
                                         ~(symbol (str "k" (dec argidx)))))])))]
           ~(->> (range nargs)
                 (reduce (fn [retstmt argidx]
                           (let [ridx (- nnargs (long argidx))]
                             `(assoc ~(symbol (str "m" ridx))
                                     ~(symbol (str "k" ridx))
                                     ~retstmt)))
                         v)))))
    `(assoc-inv ~m (if (vector? ~ks) ~ks (vec ~ks)) 0 ~v)))


(defn- get-inv
  [m ks ^long ksoff def-val]
  (if (nil? m)
    def-val
    (let [ksc (unchecked-subtract (count ks) ksoff)]
      (case ksc
        0 (throw (Exception. "Empty key vector provided to get-in"))
        1 (get m (ks ksoff) def-val)
        2 (-> (get m (ks ksoff))
              (get (ks (unchecked-add ksoff 1)) def-val))
        3 (-> (get m (ks ksoff))
              (get (ks (unchecked-add ksoff 1)))
              (get (ks (unchecked-add ksoff 2)) def-val))
        (get-inv (get m (ks ksoff)) ks (unchecked-inc ksoff) def-val)))))


(defmacro get-in
  "get-in drop-in more efficient replacement if ks is a vector especially if ks
  is known at compile time."
  ([m ks default-value]
   (if (vector? ks)
     (let [nargs (count ks)
           nnargs (dec nargs)]
       `~(->> (range nargs)
              (reduce (fn [curget ^long argidx]
                        (if (== argidx nnargs)
                          `(get ~curget
                                ~(ks argidx)
                                ~default-value)
                          `(get ~curget ~(ks argidx))))
                      m)))
     `(get-inv ~m (if (vector? ~ks) ~ks (vec ~ks)) 0 ~default-value)))
  ([m ks]
   `(get-in m ks nil)))


(defn- single-arg-fn
  ([f] (reify
         Function
         (apply [this v] (f v))
         IFn
         (invoke [this v] (f v))))
  ([f a]
   (reify
     Function
     (apply [this v] (f v a))
     IFn
     (invoke [this v] (f v a))))

  ([f a b]
   (reify
     Function
     (apply [this v] (f v a b))
     IFn
     (invoke [this v] (f v a b))))

  ([f a b c]
   (reify
     Function
     (apply [this v] (f v a b c))
     IFn
     (invoke [this v] (f v a b c))))


  ([f a b c d]
   (reify
     Function
     (apply [this v] (f v a b c d))
     IFn
     (invoke [this v] (f v a b c d))))

  ([f a b c d e]
   (reify
     Function
     (apply [this v] (f v a b c d e))
     IFn
     (invoke [this v] (f v a b c d e))))

  ([f a b c d e f]
   (reify
     Function
     (apply [this v] (f v a b c d e f))
     IFn
     (invoke [this v] (f v a b c d e f))))

  ([f a b c d e f args]
   (reify
     Function
     (apply [this v] (apply f v a b c d e f args))
     IFn
     (invoke [this v] (apply f v a b c d e f args)))))


(defn update
  "Slightly faster version of clojure.core/update when you have persistent maps from this
  library."
  ([m k f]
   (cond
     (empty-map? m)
     (PersistentArrayMap. equal-hash-provider k (f nil) (meta m))
     (map-set? m)
     (.immutUpdateValue (as-map-set m) k (->function f))
     :else
     (clojure.core/update m k f)))
  ([m k f a]
   (update m k (single-arg-fn f a)))
  ([m k f a b]
   (update m k (single-arg-fn f a b)))
  ([m k f a b c]
   (update m k (single-arg-fn f a b c)))
  ([m k f a b c d]
   (update m k (single-arg-fn f a b c d)))
  ([m k f a b c d e]
   (update m k (single-arg-fn f a b c d e)))
  ([m k f a b c d e f]
   (update m k (single-arg-fn f a b c d e f)))
  ([m k f a b c d e f & args]
   (update m k (single-arg-fn f a b c d e f args))))


(defn- update-inv
  [m ks ^long ksoff f]
  (let [nks (unchecked-subtract (count ks) ksoff)]
    (case nks
      0 (update m nil f)
      1 (update m (ks ksoff) f)
      2 (let [k0 (ks ksoff)
              k1 (ks (unchecked-add ksoff 1))
              m1 (get m k0)]
          (-> (update m1 k1 f)
              (assoc m k0)))
      3 (let [k0 (ks ksoff)
              k1 (ks (unchecked-add ksoff 1))
              k2 (ks (unchecked-add ksoff 2))
              m1 (get m k0)
              m2 (get m1 k1)]
          (->> (update m2 k2 f)
               (assoc m1 k1)
               (assoc m k0)))
      (update-inv (get m (ks ksoff)) ks (unchecked-inc ksoff) f))))


(defn update-in
  ([m ks f]
   (update-inv m (if (vector? ks) ks (vec ks)) 0 f))
  ([m ks f a]
   (update-in m ks (single-arg-fn f a)))
  ([m ks f a b]
   (update-in m ks (single-arg-fn f a b)))
  ([m ks f a b c]
   (update-in m ks (single-arg-fn f a b c)))
  ([m ks f a b c d]
   (update-in m ks (single-arg-fn f a b c d)))
  ([m ks f a b c d e]
   (update-in m ks (single-arg-fn f a b c d e)))
  ([m ks f a b c d e f]
   (update-in m ks (single-arg-fn f a b c d e f)))
  ([m ks f a b c d e f & args]
   (update-in m ks (single-arg-fn f a b c d e f args))))

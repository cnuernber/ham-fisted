(ns ham-fisted.api
  "Fast mutable and immutable associative data structures based on bitmap trie
  hashmaps. Mutable pathways implement the `java.util.Map` or `Set` interfaces
  including in-place update features such as compute or computeIfPresent.

  Mutable maps or sets can be turned into their immutable counterparts via the
  Clojure `persistent!` call. This allows working in a mutable space for
  convenience and performance then switching to an immutable pathway when
  necessary. Note: after `persistent!` one should never backdoor mutate map or
  set again as this will break the contract of immutability.  Immutable
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
  (:require [ham-fisted.iterator :as iterator]
            [ham-fisted.lazy-noncaching
             :refer [map concat filter repeatedly]
             :as lznc]
            [ham-fisted.lazy-caching :as lzc]
            [com.climate.claypoole :as claypoole])
  (:import [ham_fisted HashMap PersistentHashMap HashSet PersistentHashSet
            BitmapTrieCommon$HashProvider BitmapTrieCommon BitmapTrieCommon$MapSet
            BitmapTrieCommon$Box PersistentArrayMap ObjArray ImmutValues
            MutList ImmutList StringCollection ArrayImmutList ArrayLists
            ImmutSort IMutList ArrayLists$SummationConsumer Ranges$LongRange
            Ranges$DoubleRange IFnDef Transformables$MapIterable
            Transformables$FilterIterable Transformables$CatIterable
            Transformables$MapList Transformables$IMapable Transformables
            ReindexList ConstList ArrayLists$ObjectArrayList Transformables$SingleMapList
            ArrayLists$IntArrayList ArrayLists$LongArrayList ArrayLists$DoubleArrayList
            ReverseList TypedList DoubleMutList LongMutList ArrayLists$ReductionConsumer]
           [clojure.lang ITransientAssociative2 ITransientCollection Indexed
            IEditableCollection RT IPersistentMap Associative Util IFn ArraySeq
            Reversible IReduce IReduceInit IFn$DD IFn$DL IFn$DO IFn$LD IFn$LL IFn$LO
            IFn$OD IFn$OL IObj]
           [java.util Map Map$Entry List RandomAccess Set Collection ArrayList Arrays
            Comparator Random]
           [java.lang.reflect Array]
           [java.util.function Function BiFunction BiConsumer Consumer
            DoubleBinaryOperator LongBinaryOperator LongFunction IntFunction]
           [java.util.concurrent ForkJoinPool ExecutorService Callable Future
            ConcurrentHashMap ForkJoinTask]
           [it.unimi.dsi.fastutil.ints IntComparator IntArrays]
           [it.unimi.dsi.fastutil.longs LongComparator]
           [it.unimi.dsi.fastutil.floats FloatComparator]
           [it.unimi.dsi.fastutil.doubles DoubleComparator]
           [it.unimi.dsi.fastutil.objects ObjectArrays]
           [com.google.common.cache Cache CacheBuilder CacheLoader LoadingCache CacheStats]
           [com.google.common.collect MinMaxPriorityQueue]
           [java.time Duration])
  (:refer-clojure :exclude [assoc! conj! frequencies merge merge-with memoize
                            into assoc-in get-in update assoc update-in hash-map
                            group-by subvec group-by mapv vec vector object-array
                            sort int-array long-array double-array float-array
                            range map concat filter filterv first last pmap take take-last drop
                            drop-last sort-by repeat repeatedly shuffle into-array
                            empty? reverse]))


(set! *warn-on-reflection* true)

(declare assoc! conj! vec mapv vector object-array range first take drop into-array shuffle
         object-array-list fast-reduce int-array-list long-array-list double-array-list
         int-array argsort)


(defn ->collection
  "Ensure item is an implementation of java.util.Collection."
  ^Collection [item]
  (lznc/->collection item))


(defn ->random-access
  "Ensure item is derived from java.util.List and java.util.RandomAccess and
  thus supports constant time random addressing."
  ^List [item]
  (lznc/->random-access item))


(defn ->reducible
  "Ensure item either implements IReduceInit or java.util.Collection.  For arrays
  this will return an object that has a much more efficient reduction pathway
  than the base Clojure reducer."
  [item]
  (lznc/->reducible item))


(defn reindex
  "Permut coll by the given indexes.  Result is random-access and the same length as
  the index collection.  Indexes are expected to be in the range of [0->count(coll))."
  [coll indexes]
  (lznc/reindex (->random-access coll) (int-array indexes)))


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

(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Hash provider opportunistically using IHashEq pathway when provided else
falling back to mixhash(obj.hashCode).  For equality strictly uses Util.equiv as equality
has not shown up to be a profiler bottleneck while generating mumur3 compatible hashes has in
some cases (integers).  This hash provider provides a middle ground offering more performance
for simple datatypes but still using the more robust equiv pathways for more complex datatypes.
This is currently the default hash provider for the library."}
  hybrid-hash-provider BitmapTrieCommon/hybridHashProvider)


(def ^{:tag BitmapTrieCommon$HashProvider
       :doc "Default hash provider - currently set to the hybrid hash provider."}
  default-hash-provider BitmapTrieCommon/defaultHashProvider)



(defn- options->provider
  ^BitmapTrieCommon$HashProvider [options]
  (get options :hash-provider default-hash-provider))


(def ^{:tag PersistentArrayMap
       :doc "Constant persistent empty map"} empty-map PersistentArrayMap/EMPTY)
(def ^{:tag PersistentHashSet
       :doc "Constant persistent empty set"} empty-set (PersistentHashSet. (options->provider nil)))
(def ^{:tag ArrayImmutList
       :doc "Constant persistent empty vec"} empty-vec ArrayImmutList/EMPTY)


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
     (PersistentArrayMap. default-hash-provider a b (meta m))
     (.assoc ^Associative m a b)))
  ([m a b c d]
   (if (and (empty-map? m) (PersistentArrayMap/different default-hash-provider a c))
     (PersistentArrayMap. default-hash-provider a b c d (meta m))
     (-> (assoc! (transient (or m PersistentArrayMap/EMPTY)) a b)
         (assoc! c d)
         (persistent!))))
  ([m a b c d e f]
   (if (and (empty-map? m) (PersistentArrayMap/different default-hash-provider a c e))
     (PersistentArrayMap. default-hash-provider a b c d e f (meta m))
     (-> (transient (or m PersistentArrayMap/EMPTY))
         (assoc! a b)
         (assoc! c d)
         (assoc! e f)
         (persistent!))))
  ([m a b c d e f g h]
   (if (and (empty-map? m) (PersistentArrayMap/different default-hash-provider a c e g))
     (PersistentArrayMap. default-hash-provider a b c d e f g h (meta m))
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
     (let [m (HashMap. default-hash-provider
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


(def ^:private empty-objs (clojure.core/object-array 0))


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
  (^objects [v0 v1 v2 v3 v4 v5 v6] (ObjArray/create v0 v1 v2 v3 v4 v5 v6))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9) v10)
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12 v13] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12 v13))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12 v13 v14] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12 v13 v14))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12 v13 v14 v15] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12 v13 v14 v15)))


(defn into
  "Like clojure.core/into, but also designed to handle editable collections,
  transients, and base java.util.Map, List and Set containers."
  ([container data]
   (cond
     (instance? IEditableCollection container)
     (-> (fast-reduce conj! (transient container) data)
         (persistent!))
     (instance? ITransientCollection container)
     (reduce conj! container data)
     (instance? Map container)
     (if (instance? Map data)
       (do (.putAll ^Map container ^Map data) container)
       (reduce conj! container data))
     (instance? Collection container)
     (cond
       (instance? Collection data)
       (do (.addAll ^Collection container ^Collection data) container)
       (instance? CharSequence data)
       (do (.addAll ^Collection container (StringCollection. data)) container)
       :else
       (reduce conj! container data))
     :else
     (throw (Exception. (str "Unable to ascertain container type: " (type container))))))
  ([container xform data]
   (into container (eduction xform data))))


(defn reduce-put-map
  "Perform a reduction to put values as if by assoc into a mutable map."
  ^Map [^Map m data]
  (fast-reduce (fn [^Map m d]
                 (cond
                   (instance? Map$Entry d)
                   (.put m (.getKey ^Map$Entry d) (.getValue ^Map$Entry d))
                   (instance? Indexed d)
                   (.put m (.nth ^Indexed d 0) (.nth ^Indexed d 1))
                   :else
                   (throw (Exception. "Unrecognized map input")))
                 m)
               m
               data))


(defn mut-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  operations such as put!, remove!, compute-at! and compute-if-absent!.  You can create
  a persistent hashmap via the clojure `persistent!` call.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^HashMap [] (HashMap. (options->provider nil)))
  (^HashMap [data] (mut-map nil data))
  (^HashMap [options data]
   (if (instance? obj-ary-cls data)
     (HashMap. (options->provider options) true ^objects data)
     (reduce-put-map (HashMap. (options->provider options)) data))))


(defn ^:no-doc map-data->obj-ary
  ^objects [data]
  (if (instance? obj-ary-cls data)
    data
    (-> (fast-reduce (fn [^List l d]
                       (cond
                         (instance? Map$Entry d)
                         (do (.add l (.getKey ^Map$Entry d))
                             (.add l (.getValue ^Map$Entry d)))
                         (instance? Indexed d)
                         (do (.add l (.nth ^Indexed d 0))
                             (.add l (.nth ^Indexed d 1))))
                       l)
                     (object-array-list)
                     data)
          (object-array))))

(defn ^:no-doc immut-map-via-obj-ary
  [options data]
  (PersistentHashMap/create (options->provider options) false (map-data->obj-ary data)))


(defn constant-countable?
  "Return true if data has a constant time count."
  [data]
  (when-not (nil? data)
    (or (instance? RandomAccess data)
        (instance? Set data)
        (instance? Map data)
        (.isArray (.getClass ^Object data)))))


(defn constant-count
  "Constant time count.  Returns nil if input doesn't have a constant time count."
  [data]
  (if (nil? data)
    0
    (cond
      (instance? RandomAccess data) (.size ^List data)
      (instance? Map data) (.size ^Map data)
      (instance? Set data) (.size ^Set data)
      (.isArray (.getClass ^Object data)) (Array/getLength data))))


(defn immut-map
  "Create an immutable map.  This object supports conversion to a transient map via
  Clojure's `transient` function.  Duplicate keys are treated as if by assoc.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  If you know you will have consistently more key/val pairs than 8 you should just
  use `(persistent! (mut-map data))` as that avoids the transition from an arraymap
  to a persistent hashmap.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]].

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
     (persistent! (mut-map options data)))))


(defn hash-map
  "Drop-in replacement to Clojure's hash-map function."
  ([] empty-map)
  ([a b] (PersistentArrayMap. default-hash-provider a b nil))
  ([a b c d]
   (if (PersistentArrayMap/different default-hash-provider a c)
     (PersistentArrayMap. default-hash-provider a b c d nil)
     (hash-map c d)))
  ([a b c d e f]
   (if (PersistentArrayMap/different default-hash-provider a c e)
     (PersistentArrayMap. default-hash-provider a b c d e f nil)
     (PersistentHashMap/create default-hash-provider false (obj-ary a b c d e f))))
  ([a b c d e f g h]
   (if (PersistentArrayMap/different default-hash-provider a c e g)
     (PersistentArrayMap. default-hash-provider a b c d e f g h nil)
     (PersistentHashMap/create default-hash-provider false (obj-ary a b c d e f g h))))
  ([a b c d e f g h i j]
   (PersistentHashMap/create default-hash-provider false (obj-ary a b c d e f g h i j)))
  ([a b c d e f g h i j k l]
   (PersistentHashMap/create default-hash-provider false (obj-ary a b c d e f g h i j k l)))
  ([a b c d e f g h i j k l m n]
   (PersistentHashMap/create default-hash-provider false (obj-ary a b c d e f g h i j k l m n)))
  ([a b c d e f g h i j k l m n o p]
   (PersistentHashMap/create default-hash-provider false (obj-ary a b c d e f g h i j k l m n o p)))
  ([a b c d e f g h i j k l m n o p & args]
   (PersistentHashMap/create default-hash-provider false (ObjArray/createv a b c d e f g h i j k l m n o p (object-array args)))))


(defn java-hashmap
  "Create a java.util.HashMap.  Duplicate keys are treated as if map was created by assoc."
  (^java.util.HashMap [] (java.util.HashMap.))
  (^java.util.HashMap [data]
   (if (instance? Map data)
     (java.util.HashMap. ^Map data)
     (reduce-put-map (java.util.HashMap.) data))))


(defn java-concurrent-hashmap
  "Create a java concurrent hashmap which is still the fastest possible way to solve a
  few concurrent problems."
  (^ConcurrentHashMap [] (ConcurrentHashMap.))
  (^ConcurrentHashMap [data] (reduce-put-map (ConcurrentHashMap.) data)))


(defn mut-set
  "Create a mutable hashset based on the bitmap trie. You can create a persistent hashset via
  the clojure `persistent!` call.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^HashSet [] (HashSet. (options->provider nil)))
  (^HashSet [data] (into (HashSet. (options->provider nil)) data))
  (^HashSet [options data] (into (HashSet. (options->provider options)) data)))


(defn immut-set
  "Create an immutable hashset based on the bitmap trie.  This object supports conversion
  to transients via `transient`.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^PersistentHashSet [] empty-set)
  (^PersistentHashSet [data] (into (PersistentHashSet. (options->provider nil)) data))
  (^PersistentHashSet [options data] (into (PersistentHashSet. (options->provider options)) data)))


(defn java-hashset
  "Create a java hashset which is still the fastest possible way to solve a few problems."
  (^java.util.HashSet [] (java.util.HashSet.))
  (^java.util.HashSet [data] (into (java.util.HashSet.) data)))


(defn mut-list
  "Create a mutable java list that is in-place convertible to a persistent list"
  (^MutList [] (MutList.))
  (^MutList [data]
   (cond
     (nil? data) (MutList.)
     (instance? obj-ary-cls data)
     (MutList/create false nil ^objects data)
     (or (instance? IReduceInit data) (instance? Collection data))
     (doto (MutList/create true (meta data) (object-array data)))
     (string? data)
     (doto (MutList.) (.addAll (StringCollection. data)))
     (.isArray (.getClass ^Object data))
     (MutList/create true nil (.toArray (ArrayLists/toList data)))
     :else
     (into (MutList.) data))))


(defn immut-list
  "Create a persistent list.  Object arrays will be treated as if this new object owns them."
  (^ImmutList [] empty-vec)
  (^ImmutList [data]
   (cond
     (instance? obj-ary-cls data)
     (ArrayImmutList/create true nil data)
     (or (instance? IReduceInit data) (instance? Collection data))
     (ArrayImmutList/create true (meta data) (object-array data))
     :else
     (persistent! (mut-list data)))))


(defn array-list
  "Create an implementation of java.util.ArrayList."
  (^ArrayList [data] (doto (ArrayList.) (.addAll (->collection data))))
  (^ArrayList [] (ArrayList.)))


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


(defmacro bi-function
  "Create an implementation of java.util.function.BiFunction"
  [arg1 arg2 code]
  `(reify BiFunction
     (apply [this ~arg1 ~arg2]
       ~code)))


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
    (.clear ^Collection map-or-coll)
    (instance? LoadingCache map-or-coll)
    (.invalidateAll ^LoadingCache map-or-coll)
    :else
    (throw (Exception. (str "Unrecognized type for clear!: " map-or-coll))))
  map-or-coll)


(defn- map-set?
  [item]
  (instance? BitmapTrieCommon$MapSet item))

(defn- as-map-set
  ^BitmapTrieCommon$MapSet [item] item)

(defn- immut-vals?
  [item]
  (instance? ImmutValues item))

(defn- as-immut-vals
  ^ImmutValues [item] item)

(defn- ->set
  ^Set [item]
  (cond
    (instance? Set item)
    item
    (instance? Map item)
    (.keySet ^Map item)
    :else
    (immut-set item)))


(defn map-keyset
  "Return the keyset of the map.  This may not be in the same order as (keys m) or (vals
  m).  For hamf maps, this has the same ordering as (keys m).  For both hamf and java
  hashmaps, the returned implementation of java.util.Set both more utility and better
  performance than (keys m)."
  ^Set [^Map m] (.keySet m))


(defn map-values
  "Return the values collection of the map.  This may not be in the same order as (keys m)
  or (vals m).  For hamf hashmaps, this does have the same order as (vals m).  For both
  hamf hashmaps and java hashmaps, this has better performance for reductions especially
  using `fast-reduce` than (vals m)."
  ^Collection [^Map m] (.values m))


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
  by this library this falls backs to `(reduce (partial union-maps bfn) maps)`."
  ([bfn maps]
   (let [bfn (->bi-function bfn)]
     (if (every? map-set? maps)
       (PersistentHashMap/unionReduce bfn maps)
       (reduce #(map-union bfn %1 %2) maps)))))


(defn union-reduce-java-hashmap
  "Do an efficient union of many maps into a single java.util.HashMap."
  (^java.util.HashMap [bfn maps options]
   (let [maps (->reducible maps)]
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
  k,v and returns a new v. Returns new persistent map.
  If passed a vector, k is the index and v is the value.  Will return a new vector.
  else map is assumed to be convertible to a sequence and this pathway works the same
  as map-indexed."
  [map bfn]
  (let [bfn (->bi-function bfn)]
    (cond
      (immut-vals? map)
      (.immutUpdateValues (as-immut-vals map) bfn)
      (instance? Map map)
      (let [retval (mut-map)]
        (.forEach ^Map map
                  (reify BiConsumer
                    (accept [this k v]
                      (.put retval k (.apply bfn k v)))))
        (persistent! retval))
      (instance? RandomAccess map)
      (let [^List map map
            retval (MutList.)]
        (dotimes [idx (.size map)]
          (.add retval (.apply bfn idx (.get map idx))))
        (persistent! retval))
      :else
      (map-indexed #(.apply bfn %1 %2) map))))


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
  (-> (fast-reduce (fn [^Map m entry]
                     (let [^Indexed result (map-fn entry)]
                       (when result
                         (.put m (.nth result 0) (.nth result 1)))
                       m))
                   (mut-map)
                   src-map)
      (persistent!)))


(defn in-fork-join-task?
  "True if you are currently running in a fork-join task"
  []
  (ForkJoinTask/inForkJoinPool))


(defn pgroups
  "Run y groups across n-elems.   Y is common pool parallelism.

  body-fn gets passed two longs, startidx and endidx.

  Returns a sequence of the results of body-fn applied to each group of indexes.

  Options:

  * `:pgroup-min` - when provided n-elems must be more than this value for the computation
    to be parallelized.
  * `:batch-size` - max batch size.  Defaults to 64000."
  ([n-elems body-fn options]
   (let [n-elems (long n-elems)
         parallelism (ForkJoinPool/getCommonPoolParallelism)]
     (if (or (in-fork-join-task?)
             (< n-elems (max parallelism (long (get options :pgroup-min 0)))))
       [(body-fn 0 n-elems)]
       (let [pool (ForkJoinPool/commonPool)
             n-elems (long n-elems)
             max-batch-size (long (get options :batch-size 64000))
             gsize (min max-batch-size (quot n-elems parallelism))
             ngroups (quot n-elems gsize)
             leftover (rem n-elems gsize)]
         (->> (range ngroups)
              (mapv (fn [^long gidx]
                      (let [start-idx (long (+ (* gidx gsize)
                                               (min gidx leftover)))
                            end-idx (min n-elems
                                         (long (+ (+ start-idx gsize)
                                                  (long (if (< gidx leftover)
                                                          1 0)))))]
                        (.submit pool ^Callable #(body-fn start-idx end-idx)))))
              (map #(.get ^Future %)))))))
  ([n-elems body-fn]
   (pgroups n-elems body-fn nil)))


(defn pmap
  "pmap using the commonPool.  This is useful for interacting with other primitives, namely
  [[pgroups]] which are also based on this pool.  This is a change from Clojure's base
  pmap in that it uses the ForkJoinPool/commonPool for parallelism as opposed to the
  agent pool - this makes it compose with pgroups and dtype-next's parallelism system."
  [map-fn & sequences]
  (if (in-fork-join-task?)
    (apply map map-fn sequences)
    (apply claypoole/pmap (ForkJoinPool/commonPool) map-fn sequences)))


(defn upmap
  "Unordered pmap using the commonPool.  This is useful for interacting with other primitives,
  namely [[pgroups]] which are also based on this pool.
  Like pmap this uses the commonPool so it composes with this api's pmap, pgroups, and
  dtype-next's parallelism primitives *but* it does not impose an ordering constraint on the
  results and thus may be significantly faster in some cases."
  [map-fn & sequences]
  (if (in-fork-join-task?)
    (apply map map-fn sequences)
    (apply claypoole/upmap (ForkJoinPool/commonPool) map-fn sequences)))


(defn ^:no-doc pfrequencies
  "Parallelized frequencies. Can be faster for large random-access containers."
  [tdata]
  (let [n-elems (count tdata)]
    (->> (pgroups n-elems
               (fn [^long start-idx ^long end-idx]
                 (let [gsize (- end-idx start-idx)
                       hm (mut-map)]
                   (dotimes [idx gsize]
                     (.compute hm (tdata (+ idx start-idx)) BitmapTrieCommon/incBiFn))
                   hm)))
         (reduce #(map-union BitmapTrieCommon/sumBiFn %1 %2)))))


(defn frequencies
  "Faster implementation of clojure.core/frequencies."
  [coll]
  (-> (reduce (fn [counts x]
                (compute! counts x BitmapTrieCommon/incBiFn)
                counts)
              (mut-map)
              coll)
      (persistent!)))


(defn ^:no-doc pfrequencies-current-hashmap
  "Parallelized frequencies.  Used for testing concurrentCompute functionality -
  tdata must be random access and countable."
  [tdata]
  (let [n-elems (count tdata)
        hm (java.util.concurrent.ConcurrentHashMap.)]
    (pgroups n-elems
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
     (union-reduce-maps f (apply lznc/concat (vector (map-union f m1 m2)) args)))))


(defn memoize
  "Efficient thread-safe version of clojure.core/memoize.

  Also see [[clear-memoized-fn!]] to mutably clear the backing store.

  Options.

  * `:write-ttl-ms` - Time that values should remain in the cache after write in milliseconds.
  * `:access-ttl-ms` - Time that values should remain in the cache after access in milliseconds.
  * `:soft-values?` - When true, the cache will store [SoftReferences](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/SoftReference.html) to the data.
  * `:weak-values?` - When true, the cache will store [WeakReferences](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html) to the data.
  * `:max-size` - When set, the cache will behave like an LRU cache.
  * `:record-stats?` - When true, the LoadingCache will record access statistics.  You can
     get those via the undocumented function memo-stats."
  ([memo-fn]
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
  ([memo-fn {:keys [write-ttl-ms
                    access-ttl-ms
                    soft-values?
                    weak-values?
                    max-size
                    record-stats?]}]
   (let [^CacheBuilder new-builder
         (cond-> (CacheBuilder/newBuilder)
           access-ttl-ms
           (.expireAfterAccess (Duration/ofMillis access-ttl-ms))
           write-ttl-ms
           (.expireAfterWrite (Duration/ofMillis write-ttl-ms))
           soft-values?
           (.softValues)
           weak-values?
           (.weakValues)
           max-size
           (.maximumSize (long max-size))
           record-stats?
           (.recordStats))
         ^LoadingCache cache
         (.build new-builder
                 (proxy [CacheLoader] []
                   (load [args]
                     (BitmapTrieCommon$Box. (apply memo-fn args)))))]
     (-> (fn [& args]
           (.obj ^BitmapTrieCommon$Box (.get cache args)))
         (with-meta {:cache cache})))))


(defn clear-memoized-fn!
  "Clear a memoized function backing store."
  [memoize-fn]
  (if-let [map (get (meta memoize-fn) :cache)]
    (clear! map)
    (throw (Exception. (str "Arg is not a memoized fn - " memoize-fn))))
  memoize-fn)


(defn ^:no-doc memo-stats
  "Return the statistics from a google guava cache.  In order for a memoized function
  to produce these the :record-stats? option must be true."
  [memoize-fn]
  (when-let [cache (:cache (meta memoize-fn))]
    (when (instance? Cache cache)
      (let [^Cache cache cache
            ^CacheStats cache-map (.stats cache)]
        {:hit-count (.hitCount cache-map)
         :hit-rate (.hitRate cache-map)
         :miss-count (.missCount cache-map)
         :miss-rate (.missRate cache-map)
         :load-success-count (.loadSuccessCount cache-map)
         :load-exception-count (.loadExceptionCount cache-map)
         :average-load-penalty-nanos (.averageLoadPenalty cache-map)
         :total-load-time-nanos (.totalLoadTime cache-map)
         :eviction-count (.evictionCount cache-map)}))))


(defn map-factory
  "Create a factory to quickly produce maps with a fixed set of keys but arbitrary
  values.  This version takes a vector or sequence of keys and returns and IFn that
  takes a vector, object-array, or sequence of values.  The most efficient pathway will be
  if values are already in an object array.

  The factory produces PersistentHashMaps."
  [keys]
  (let [mf (PersistentHashMap/makeFactory default-hash-provider (object-array keys))]
    (fn [vals] (.apply mf (object-array vals)))))


(defn ^:no-doc assoc-inf
  "Associates a value in a nested associative structure, where ks is a
  sequence of keys and v is the new value and returns a new nested structure.
  If any levels do not exist, hash-maps will be created."
  {:added "1.0"
   :static true}
  [m [k & ks] v]
  (if ks
    (assoc m k (assoc-inf (get m k) ks v))
    (assoc m k v)))


(defmacro assoc-in
  "Assoc-in - more efficient replacement if ks is a known compile time constant
  or a vector.  See the caveats in the README before using this exact function."
  [m ks v]
  (if (vector? ks)
    (if (== 0 (count ks))
      `(assoc ~m nil ~v)
      (let [nargs (count ks)
            nnargs (dec nargs)]
        `(let [~@(->> (range (dec nargs))
                      (mapcat (fn [argidx]
                                (let [argidx (inc argidx)]
                                  [(symbol (str "m" argidx))
                                   (if (== 0 argidx)
                                     `~m
                                     `(get ~(if (= 1 argidx)
                                              m
                                              (symbol (str "m" (dec argidx))))
                                           ~(ks (dec argidx))))]))))]
           ~(->> (range nargs)
                 (reduce (fn [retstmt argidx]
                           (let [ridx (- nnargs (long argidx))]
                             `(assoc ~(if (= 0 ridx)
                                        m
                                        (symbol (str "m" ridx)))
                                     ~(ks ridx)
                                     ~retstmt)))
                         v)))))
    `(assoc-inf ~m ~ks ~v)))


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
     `(clojure.core/get-in ~m ~ks ~default-value)))
  ([m ks]
   `(get-in ~m ~ks nil)))


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
  "Version of update that produces maps from this library."
  {:static true}
  ([m k f]
   (assoc m k (f (get m k))))
  ([m k f x]
   (assoc m k (f (get m k) x)))
  ([m k f x y]
   (assoc m k (f (get m k) x y)))
  ([m k f x y z]
   (assoc m k (f (get m k) x y z)))
  ([m k f x y z & more]
   (assoc m k (apply f (get m k) x y z more))))


(defn ^:no-doc update-inf
  "'Updates' a value in a nested associative structure, where ks is a
  sequence of keys and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested structure.  If any levels do not exist, hash-maps will be
  created."
  {:added "1.0"
   :static true}
  ([m ks f & args]
     (let [up (fn up [m ks f args]
                (let [[k & ks] ks]
                  (if ks
                    (assoc m k (up (get m k) ks f args))
                    (assoc m k (apply f (get m k) args)))))]
       (up m ks f args))))


(defmacro update-in
  "An attempt at a slightly more efficient version of update-in.

  See the caveats in the readme - measure carefully before using this."
  [m ks f & args]
  (cond
    (nil? m)
    `(assoc-in ~m ~ks (~f nil ~@args))
    (vector? ks)
    (let [countk (count ks)]
      (case countk
        0 `(update ~m nil ~f ~@args)
        1 `(update ~m ~(ks 0) ~f ~@args)
        `(let [~@(->> (range 1 countk)
                      (mapcat (fn [argidx]
                                (let [didix (dec argidx)]
                                  [(symbol (str "m" argidx))
                                   `(get ~(if (= 0 didix) m (symbol (str "m" didix)))
                                         ~(ks didix))]))))]
           ~(reduce (fn [expr argidx]
                      `(assoc ~(if (= 0 argidx) m (symbol (str "m" argidx)))
                              ~(ks argidx)
                              ~expr))
                    `(update ~(symbol (str "m" (dec countk)))
                             ~(ks (dec countk))
                             ~f ~@args)
                    (range (- countk 2) -1 -1)))))
    :else
    `(update-inf ~m ~ks ~f ~@args)))


(defn subvec
  "More general version of subvec.  Works for any java list implementation
  including persistent vectors and any array."
  ([m sidx eidx]
   (let [^List m (if (instance? List m)
                   m
                   (->random-access m))]
     (.subList m sidx eidx)))
  ([m sidx] (subvec m sidx (count m))))


(defn group-by
  "Group items in collection by the grouping function f.  Returns persistent map of
  keys to persistent vectors."
  [f coll]
  (let [retval (mut-map)
        compute-fn (reify Function
                     (apply [this k]
                       (object-array-list)))
        coll (->reducible coll)]
    (fast-reduce (fn [retval v]
                   (.add ^List (compute-if-absent! retval (f v) compute-fn) v)
                   retval)
                 retval
                 coll)
    (update-values retval (reify BiFunction
                            (apply [this k v]
                              (immut-list v))))))


(defn group-by-reduce
  "Group by key. Apply the reduce-fn the existing value and on each successive value.
  finalize-fn is called during finalization with the sole argument of the existing value.
  If at any point result is reduced? group-by continues but further reductions for that
  specific value cease.  In this case finalize-fn isn't called but the result is simply
  deref'd during finalization.

  This type of reduction can be both faster and more importantly use
  less memory than a reduction of the forms:

  ```clojure
  (->> group-by map into)` or `(->> group-by mapmap)
  ```

```clojure
ham-fisted.api> (group-by-reduce #(rem (unchecked-long %1) 7) + (range 100))
  {0 735, 1 750, 2 665, 3 679, 4 693, 5 707, 6 721}
ham-fisted.api> (group-by-reduce #(rem (unchecked-long %1) 7) max (shuffle (range 100)))
  {0 98, 1 99, 2 93, 3 94, 4 95, 5 96, 6 97}

ham-fisted.api> ;;Reduce to map of first value found
ham-fisted.api> (group-by-reduce #(rem (unchecked-long %1) 7) (fn [l r] l) (range 100))
{0 0, 1 1, 2 2, 3 3, 4 4, 5 5, 6 6}
ham-fisted.api> ;;Reduce to map of last value found
ham-fisted.api> (group-by-reduce #(rem (unchecked-long %1) 7) (fn [l r] r) (range 100))
{0 98, 1 99, 2 93, 3 94, 4 95, 5 96, 6 97}
```"
  ([key-fn reduce-fn finalize-fn coll]
   (let [retval (mut-map)
         box-fn (reify Function
                  (apply [this k]
                    (BitmapTrieCommon$Box.)))
         update-fn (reify BiFunction
                     (apply [this oldv newv]
                       (if (reduced? oldv)
                         oldv
                         (reduce-fn oldv newv))))
         finalize-fn (or finalize-fn identity)]
     (fast-reduce (fn [retval v]
                    (let [b (compute-if-absent! retval (key-fn v) box-fn)]
                      (.inplaceUpdate ^BitmapTrieCommon$Box b update-fn v))
                    retval)
                  retval coll)
     (.replaceAll retval (bi-function
                          k b
                          (let [^BitmapTrieCommon$Box b b
                                v (.obj b)]
                            (if (reduced? v)
                              (deref v)
                              (finalize-fn v)))))
     (persistent! retval)))
  ([key-fn reduce-fn coll]
   (group-by-reduce key-fn reduce-fn nil coll)))


(defn mapv
  "Produce a persistent vector from a collection."
  ([map-fn coll]
   (immut-list (map map-fn coll)))
  ([map-fn c1 c2]
   (immut-list (map map-fn c1 c2)))
  ([map-fn c1 c2 c3]
   (immut-list (map map-fn c1 c2 c3)))
  ([map-fn c1 c2 c3 & args]
   (immut-list (apply map map-fn c1 c2 c3 args))))


(defn filterv
  "Filter a collection into a vector."
  [pred coll]
  (immut-list (filter pred coll)))


(defn vec
  "Produce a persistent vector.  Optimized pathways exist for object arrays and
  java List implementations."
  ([data]
   (if (vector? data)
     (if (instance? IObj data)
       (with-meta data nil)
       data)
     (immut-list data)))
  ([] (immut-list)))


(defn vector
  ([] empty-vec)
  ([a] (ArrayImmutList/create true nil (ObjArray/create a)))
  ([a b] (ArrayImmutList/create true nil (ObjArray/create a b)))
  ([a b c] (ArrayImmutList/create true nil (ObjArray/create a b c)))
  ([a b c d] (ArrayImmutList/create true nil (ObjArray/create a b c d)))
  ([a b c d e] (ArrayImmutList/create true nil (ObjArray/create a b c d e)))
  ([a b c d e f] (ArrayImmutList/create true nil (ObjArray/create a b c d e f)))
  ([a b c d e f g] (ArrayImmutList/create true nil (ObjArray/create a b c d e f g)))
  ([a b c d e f g h] (ArrayImmutList/create true nil (ObjArray/create a b c d e f g h)))
  ([a b c d e f g h i] (ArrayImmutList/create true nil (ObjArray/create a b c d e f g h i)))
  ([a b c d e f g h i j] (ArrayImmutList/create true nil (ObjArray/create a b c d e f g h i j)))
  ([a b c d e f g h i j k] (ArrayImmutList/create true nil (ObjArray/create a b c d e f g h i j k)))
  ([a b c d e f g h i j k & args] (ImmutList/create true nil (apply obj-ary a b c d e f g h i j k args))))


(defn splice
  "Splice v2 into v1 at idx.  Returns a persistent vector."
  [v1 idx v2]
  (let [retval (mut-list)
        v1 (->collection v1)]
    (.addAll retval (subvec v1 0 idx))
    (.addAll retval (->collection v2))
    (.addAll retval (subvec v1 idx))
    (persistent! retval)))


(defn empty?
  [coll]
  (if coll
    (.isEmpty (->collection coll))
    true))


(defn- concat-reducible
  ([^IMutList retval v1 v2]
   (let [retval (mut-list)]
     (.addAllReducible retval (->reducible v1))
     (.addAllReducible retval (->reducible v2))
     retval))
  ([^IMutList retval v1 v2 args]
   (when-not (nil? v1) (.addAllReducible retval (->reducible v1)))
   (when-not (nil? v2) (.addAllReducible retval (->reducible v2)))
   (fast-reduce (fn [data c]
                  (when-not (nil? c) (.addAllReducible retval (->reducible c)))
                  retval)
                retval
                args)))


(defn concatv
  "non-lazily concat a set of items returning a persistent vector.  "
  ([] empty-vec)
  ([v1] (vec v1))
  ([v1 v2]
   (cond
     (nil? v1) (vec v2)
     (nil? v2) (vec v1)
     :else
     (-> (concat-reducible (mut-list) v1 v2)
         (persistent!))))
  ([v1 v2 & args]
   (-> (concat-reducible (mut-list) v1 v2 args)
       (persistent!))))


(defn concata
  "non-lazily concat a set of items returning an object array.  This always returns an
  object array an may return an empty array whereas concat may return nil."
  (^objects [] (object-array nil))
  (^objects [v1] (object-array v1))
  (^objects [v1 v2]
   (cond
     (nil? v1) (object-array v2)
     (nil? v2) (object-array v1)
     :else
     (-> (concat-reducible (ArrayLists$ObjectArrayList.) v1 v2)
         (object-array))))
  (^objects [v1 v2 & args]
   (-> (concat-reducible (ArrayLists$ObjectArrayList.) v1 v2 args)
       (object-array))))


(defn object-array
  "Faster version of object-array for java collections and strings."
  ^objects [item] (lznc/object-array item))


(defn into-array
  "Faster version of clojure.core/into-array."
  ([aseq] (lznc/into-array aseq))
  ([ary-type aseq] (lznc/into-array ary-type aseq))
  ([ary-type mapfn aseq] (lznc/into-array ary-type mapfn aseq)))


(defn- ->comparator
  ^java.util.Comparator [comp]
  (or comp compare))


(defmacro make-long-comparator
  "Make a comparator that gets passed two long arguments."
  [lhsvar rhsvar & code]
  (let [lhsvar (with-meta lhsvar {:tag 'long})
        rhsvar (with-meta rhsvar {:tag 'long})
        compsym (with-meta 'compare {:tag 'int})]
    `(reify
       LongComparator
       (~compsym [this# ~lhsvar ~rhsvar]
        ~@code)
       IFnDef
       (invoke [this# l# r#]
         (.compare this# l# r#)))))


(defmacro make-double-comparator
  "Make a comparator that gets passed two double arguments."
  [lhsvar rhsvar & code]
  (let [lhsvar (with-meta lhsvar {:tag 'double})
        rhsvar (with-meta rhsvar {:tag 'double})
        compsym (with-meta 'compare {:tag 'int})]
    `(reify
       DoubleComparator
       (~compsym [this# ~lhsvar ~rhsvar]
        ~@code)
       IFnDef
       (invoke [this# l# r#]
         (.compare this# l# r#)))))


(defmacro make-comparator
  "Make a java comparator."
  [lhsvar rhsvar code]
  `(reify
     Comparator
     (compare [this# ~lhsvar ~rhsvar]
      ~@code)
     IFnDef
     (invoke [this# l# r#]
       (.compare this# l# r#))))


(def ^{:doc "A reverse comparator that sorts in descending order" }
  rcomp
  (reify
    Comparator
    (^int compare [this ^Object l ^Object r]
      (.compareTo ^Comparable r l))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (Double/compare r l))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare r l))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(defn sorta
  "Sort returning an object array."
  (^objects [coll] (sorta nil coll))
  (^objects [comp coll]
   (let [a (object-array coll)]
     (if (< (alength a) 1000)
       (if comp
         (Arrays/sort a (->comparator comp))
         (Arrays/sort a))
       (if comp
         (ObjectArrays/parallelQuickSort a (->comparator comp))
         (ObjectArrays/parallelQuickSort a)))
     a)))


(defn sort
  "Exact replica of clojure.core/sort but instead of wrapping the final object array in a seq
  which loses the fact the result is countable and random access.  Faster implementations
  are provided when the input is an integer, long, or double array."
  ([coll] (sort nil coll))
  ([comp coll]
   (let [coll (->collection coll)]
     (if (instance? ImmutSort coll)
       (if (nil? comp)
         (.immutSort ^ImmutSort coll)
         (.immutSort ^ImmutSort coll (->comparator comp)))
       (let [a (sorta comp coll)]
         (ArrayLists/toList a 0 (alength a) ^IPersistentMap (meta coll)))))))


(defn type-single-arg-ifn
  "Categorize the return type of a single argument ifn.  May be :float64, :int64, or :object."
  [ifn]
  (cond
    (or (instance? IFn$DD ifn)
        (instance? IFn$LD ifn)
        (instance? IFn$OD ifn))
    :float64
    (or (instance? IFn$DL ifn)
        (instance? IFn$LL ifn)
        (instance? IFn$OL ifn))
    :int64
    :else
    :object))


(defn sort-by
  "Sort a collection by keyfn.  Typehinting the return value of keyfn will somewhat increase
  the speed of the sort :-)."
  ([keyfn coll]
   (sort-by keyfn nil coll))
  ([keyfn comp coll]
   (let [coll (->random-access coll)
         ^IMutList data (map keyfn coll)
         data (case (type-single-arg-ifn keyfn)
                :float64
                (.toDoubleArray data)
                :int64
                (.toLongArray data)
                data)
         indexes (argsort data)]
     (reindex coll indexes))))


(defn shuffle
  "shuffle values returning random access container.

  Options:

  * `:seed` - If instance of java.util.Random, use this.  If integer, use as seed.
  If not provided a new instance of java.util.Random is created."
  (^List [coll] (lznc/shuffle coll nil))
  (^List [coll opts] (lznc/shuffle coll opts)))


(defn iarange
  "Return an integer array holding the values of the range.  Use `->collection` to get a
  list implementation wrapping for generic access."
  (^ints [end]
   (iarange 0 end 1))
  (^ints [start end]
   (iarange start end 1))
  (^ints [start end step]
   (ArrayLists/iarange start end step)))

(defn larange
  "Return a long array holding values of the range.  Use `->collection` get a list
  implementation for generic access."
  (^longs [end]
   (larange 0 end 1))
  (^longs [start end]
   (larange start end 1))
  (^longs [start end step]
   (ArrayLists/larange start end step)))

(defn darange
  "Return a double array holding the values of the range.  Useing `->collection` to get
  an implementation of java.util.List that supports the normal Clojure interfaces."
  (^doubles [end]
   (darange 0 end 1))
  (^doubles [start end]
   (darange start end 1))
  (^doubles [start end step]
   (ArrayLists/darange start end step)))


(defn- floating?
  [item]
  (or (double? item) (float? item)))


(defn range
  "When given arguments returns a range that implements random access java list
  interfaces so nth, reverse and friends are efficient."
  ([] (clojure.core/range))
  ([end] (range 0 end 1))
  ([start end] (range start end 1))
  ([start end step]
   (if (and (integer? start) (integer? end) (integer? step))
     (Ranges$LongRange. start end step nil)
     (Ranges$DoubleRange. start end step nil))))


(defn argsort
  "Sort a collection of data returning an array of indexes.  The collection must be
  random access and the return value is an integer array of indexes which will read the
  input data in sorted order.  Faster implementations are provided when the collection
  is an integer, long, or double array.  See also [[reindex]].

  Note this sort is not nan, null aware.  dtype-next provides a somewhat slower version
  in the argops namespace which will correctly handle null and nan values."
  ([comp coll]
   (let [^List coll (if (instance? RandomAccess coll)
                      coll
                      (let [coll (->collection coll)]
                        (if (instance? RandomAccess coll)
                          coll
                          (object-array-list coll))))]
     (->
      (if (instance? IMutList coll)
        (.sortIndirect ^IMutList coll (when comp (->comparator comp)))
        (let [idata (iarange (.size coll))
              idx-comp (ArrayLists/intIndexComparator coll comp)]
          (IntArrays/parallelQuickSort idata ^IntComparator idx-comp)
          idata))
      (->collection))))
  ([coll]
   (argsort nil coll)))


(defn int-array
  "Create an integer array from a number of some data"
  ^ints [data]
  (if (number? data)
    (clojure.core/int-array data)
    (let [data (->reducible data)]
      (if (instance? IMutList data)
        (.toIntArray ^IMutList data)
        (.toIntArray ^IMutList (int-array-list data))))))


(defn long-array
  "Create a long array from a number of some data"
  ^longs [data]
  (if (number? data)
    (clojure.core/long-array data)
    (let [data (->reducible data)]
      (if (instance? IMutList data)
        (.toLongArray ^IMutList data)
        (.toLongArray ^IMutList (long-array-list data))))))


(defn float-array
  "Create a float array from a number of some data"
  ^floats [data]
  (if (number? data)
    (clojure.core/float-array data)
    (let [data (->reducible data)]
      (if (instance? IMutList data)
        (.toFloatArray ^IMutList data)
        (clojure.core/float-array data)))))


(defn double-array
  "Create a double array from a number of some data"
  ^doubles [data]
  (if (number? data)
    (clojure.core/double-array data)
    (let [data (->reducible data)]
      (if (instance? IMutList data)
        (.toDoubleArray ^IMutList data)
        (.toDoubleArray ^IMutList (double-array-list data))))))


(defn object-array-list
  "An array list that is as fast as java.util.ArrayList for add,get, etc but includes
  many accelerated operations such as fill and an accelerated addAll when the src data
  is an object array based list."
  (^IMutList [] (ArrayLists$ObjectArrayList.))
  (^IMutList [cap-or-data]
   (if (number? cap-or-data)
     (ArrayLists$ObjectArrayList. (int cap-or-data))
     (doto (ArrayLists$ObjectArrayList.)
       (.addAllReducible (->reducible cap-or-data))))))


(defn int-array-list
  "An array list that is as fast as java.util.ArrayList for add,get, etc but includes
  many accelerated operations such as fill and an accelerated addAll when the src data
  is an array list."
  (^IMutList [] (ArrayLists$IntArrayList.))
  (^IMutList [cap-or-data]
   (if (number? cap-or-data)
     (ArrayLists$IntArrayList. (int cap-or-data))
     (doto (ArrayLists$IntArrayList.)
       (.addAllReducible (->reducible cap-or-data))))))


(defn long-array-list
  "An array list that is as fast as java.util.ArrayList for add,get, etc but includes
  many accelerated operations such as fill and an accelerated addAll when the src data
  is an array list."
  (^IMutList [] (ArrayLists$LongArrayList.))
  (^IMutList [cap-or-data]
   (if (number? cap-or-data)
     (ArrayLists$LongArrayList. (int cap-or-data))
     (doto (ArrayLists$LongArrayList.)
       (.addAllReducible (->reducible cap-or-data))))))


(defn double-array-list
  "An array list that is as fast as java.util.ArrayList for add,get, etc but includes
  many accelerated operations such as fill and an accelerated addAll when the src data
  is an array list."
  (^IMutList [] (ArrayLists$DoubleArrayList.))
  (^IMutList [cap-or-data]
   (if (number? cap-or-data)
     (ArrayLists$LongArrayList. (int cap-or-data))
     (doto (ArrayLists$LongArrayList.)
       (.addAllReducible (->reducible cap-or-data))))))



(defmacro double-binary-operator
  "Create a binary operator that is specialized for double values.  Useful to speed up
  operations such as sorting or summation."
  [lvar rvar & code]
  `(reify
     DoubleBinaryOperator
     (applyAsDouble [this# ~lvar ~rvar]
       ~@code)
     IFnDef
     (invoke [this# l# r#]
       (.applyAsDouble this# l# r#))))


(defmacro long-binary-operator
  "Create a binary operator that is specialized for long values.  Useful to speed up
  operations such as sorting or summation."
  [lvar rvar & code]
  `(reify
     LongBinaryOperator
     (applyAsLong [this ~lvar ~rvar]
       ~@code)
     IFnDef
     (invoke [this# l# r#]
       (.applyAsLong this# l# r#))))


(defn sum
  "Fast simple double summation.  Does not do any summation compensation but does
  parallelize the summation for random access containers leading to some additional
  numeric stability."
  ^double [coll]
  (let [coll (->reducible coll)]
    (if (instance? IMutList coll)
      (let [^IMutList coll coll
            op (double-binary-operator l r (unchecked-add l r))]
        (if (< (.size coll) 10000)
            (.doubleReduction coll op 0.0)
            (->> (pgroups (.size coll)
                          (fn [^long sidx ^long eidx]
                            (.doubleReduction ^IMutList (.subList coll sidx eidx)
                                              op
                                           0.0)))
                 (reduce +))))
      (double (fast-reduce + 0.0 coll)))))


(defn mean
  "Return the mean of the collection.  Returns double/NaN for empty collections."
  ^double [coll]
  (let [coll (->collection coll)]
    (/ (sum coll)
       (.size coll))))


(defn first
  "Get the first item of a collection."
  [coll]
  (if (nil? coll)
    nil
    (let [coll (->reducible coll)]
      (if (instance? RandomAccess coll)
        (when-not (.isEmpty ^List coll)
          (.get ^List coll 0))
        (clojure.core/first coll)))))


(defn last
  "Get the last item in the collection.  Constant time for
  random access lists."
  [coll]
  (if (nil? coll)
    nil
    (let [coll (->reducible coll)]
      (cond
        (instance? RandomAccess coll)
        (let [^List coll coll
              sz (.size coll)]
          (when-not (== 0 sz)
            (.get coll (unchecked-dec sz))))
        (instance? Reversible coll)
        (RT/first (.rseq ^Reversible coll))
        :else
        (clojure.core/last coll)))))


(defn reverse
  "Reverse a collection or sequence.  Constant time reverse is provided
  for any random access list."
  [coll]
  (let [coll (->reducible coll)]
    (cond
      (instance? IMutList coll)
      (.reverse ^IMutList coll)
      (instance? RandomAccess coll)
      (ReverseList/create coll (meta coll))
      :else
      (clojure.core/reverse coll))))


(defn take
  "Take the first N values from a collection.  If the input is
  random access, the result will be random access."
  [n coll]
  (when coll
    (let [coll (->reducible coll)]
      (if (instance? RandomAccess coll)
        (.subList ^List coll 0 (min (long n) (.size ^List coll)))
        (clojure.core/take n coll)))))


(defn take-last
  "Take the last N values of the collection.  If the input is random-access,
  the result will be random-access."
  [n coll]
  (when coll
    (let [coll (->reducible coll)]
      (if (instance? RandomAccess coll)
        (let [ne (.size ^List coll)
              n (long n)]
          (.subList ^List coll (- ne n 1) ne))
        (clojure.core/take-last n coll)))))


(defn take-min
  "Take the min n values of a collection.  This is not an order-preserving operation."
  ([n comp values]
   (let [comp (->comparator comp)
         queue (-> (MinMaxPriorityQueue/orderedBy comp)
                   (.maximumSize (int n))
                   (.create))
         values (->reducible values)]
     (fast-reduce (fn [q obj]
                    (.add queue obj)
                    queue)
                  queue
                  values)
     (->collection (object-array queue))))
  ([n values]
   (take-min n nil values)))


(defn drop
  "Drop the first N items of the collection.  If item is random access, the return
  value is random-access."
  [n coll]
  (when coll
    (let [coll (->reducible coll)]
      (if (instance? RandomAccess coll)
        (subvec coll (min (long n) (dec (.size ^List coll))))
        (clojure.core/drop n coll)))))


(defn drop-min
  "Drop the min n values of a collection.  This is not an order-preserving operation."
  ([n comp values]
   (let [values (->random-access values)
         tn (- (count values) (long n))]
     (if (<= tn 0)
       empty-vec
       (take-min tn (.reversed (->comparator comp)) values))))
  ([n values]
   (drop-min n nil values)))


(defn drop-last
  "Drop the last N values from a collection.  IF the input is random access,
  the result will be random access."
  [n coll]
  (when coll
    (let [coll (->reducible coll)]
      (if (instance? RandomAccess coll)
        (let [ne (.size ^List coll)
              n (long n)]
          (.subList ^List coll 0 (- ne n)))
        (clojure.core/take-last n coll)))))


(defn repeat
  "When called with no arguments, produce an infinite sequence of v.
  When called with 2 arguments, produce a random access list that produces v at each
  index."
  ([v] (clojure.core/repeat v))
  (^List [n v] (ConstList/create n v nil)))


(defn iter-reduce
  "Faster reduce for things like arraylists or hashmap entrysets that implement Iterable
  but not IReduceInit."
  ([rfn init iter] (Transformables/iterReduce iter init rfn))
  ([rfn iter] (Transformables/iterReduce iter rfn)))


(defn fast-reduce
  "Version of reduce that is a bit faster for things that aren't sequences and do not
  implement IReduceInit."
  ([rfn init iter]
   (cond
     (instance? IReduceInit iter)
     (.reduce ^IReduceInit iter rfn init)
     (instance? Map iter)
     (iter-reduce rfn init (.entrySet ^Map iter))
     :else
     (iter-reduce rfn init (->collection iter))))
  ([rfn iter]
   (cond
     (instance? IReduce iter)
     (.reduce ^IReduce iter rfn)
     (instance? Map iter)
     (iter-reduce rfn (.entrySet ^Map iter))
     :else
     (iter-reduce rfn (->collection iter)))))


(comment

  (require '[criterium.core :as crit])
  (def data (double-array (shuffle (range 10000))))
  (crit/quick-bench (argsort (double-comparator l r (Double/compare r l)) data))
  ;; 861.1 us
  (crit/quick-bench (sort data))
  ;; 749.7 us
  (crit/quick-bench (reduce + data))
  ;; 309 us
  (crit/quick-bench (reduce (fn [^double l ^double r]
                              (unchecked-add l r)) data))
  ;; 271 us
  (crit/quick-bench (sum data))
  (crit/quick-bench (dfn/sum-fast data))
  ;; 10 us
  (require '[tech.v3.datatype.argops :as argops])
  (require '[tech.v3.datatype.functional :as dfn])
  (import '[ham_fisted ArrayLists$ReductionConsumer])
  (crit/quick-bench (let [dc (ArrayLists$ReductionConsumer. + 0)]
                      (.forEach (->collection data) dc)
                      (.value dc)))

  (crit/quick-bench (->> (clojure.core/range 20000)
                         (clojure.core/filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;369us

  (crit/quick-bench (->> (range 20000)
                         (filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;183us

  (crit/quick-bench (->> (range 20000)
                         (lzc/filter #(== 0 (rem (long %) 3)))
                         (sum)))

  (crit/quick-bench (->> (clojure.core/range 20000)
                         (filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;261us

  (crit/quick-bench (->> (clojure.core/range 20000)
                         (clojure.core/map #(* (long %) 2))
                         (clojure.core/filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;688us
  (crit/quick-bench (->> (clojure.core/range 20000)
                         (eduction
                          (comp
                           (clojure.core/map #(* (long %) 2))
                           (clojure.core/filter #(== 0 (rem (long %) 3)))))
                         (sum)))
  ;;575us
  (crit/quick-bench (->> (range 20000)
                         (map #(* (long %) 2))
                         (filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;259us
  (crit/quick-bench (->> (clojure.core/range 20000)
                         (map #(* (long %) 2))
                         (filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;421us

  (crit/quick-bench (->> (clojure.core/range 20000)
                         (clojure.core/map #(* (long %) 2))
                         (clojure.core/map #(+ 1 (long %)))
                         (clojure.core/filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;1.0ms
  (crit/quick-bench (->> (range 20000)
                         (eduction
                          (comp
                           (clojure.core/map #(* (long %) 2))
                           (clojure.core/map #(+ 1 (long %)))
                           (clojure.core/filter #(== 0 (rem (long %) 3)))))
                         (sum)))
  ;;887us
  (crit/quick-bench (->> (range 20000)
                         (map #(* (long %) 2))
                         (map #(+ 1 (long %)))
                         (filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;422us
  (crit/quick-bench (->> (clojure.core/range 20000)
                         (map #(* (long %) 2))
                         (map #(+ 1 (long %)))
                         (filter #(== 0 (rem (long %) 3)))
                         (sum)))
  ;;643us


  (crit/quick-bench (->> (reduce clojure.core/concat (repeat 200 (range 100)))
                         (sum)))
  ;;151ms

  (crit/quick-bench (->> (apply clojure.core/concat (repeat 200 (range 100)))
                         (sum)))
  ;;2.44ms

  (crit/quick-bench (->> (reduce concat (repeat 200 (range 100)))
                         (sum)))
  ;;737us
  (crit/quick-bench (->> (apply concat (repeat 200 (range 100)))
                         (sum)))
  ;;263us

  )

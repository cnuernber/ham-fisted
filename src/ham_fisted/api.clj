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
            [ham-fisted.function :refer [bi-function ->bi-function function bi-consumer obj->long]
             :as hamf-fn]
            [ham-fisted.lazy-noncaching
             :refer [map concat filter repeatedly]
             :as lznc]
            [ham-fisted.lazy-caching :as lzc]
            [ham-fisted.alists :as alists]
            [ham-fisted.impl :as impl]
            [ham-fisted.reduce :refer [reduce-reducer preduce options->parallel-options preduce-reducer
                                       long-accumulator double-accumulator double-consumer-accumulator
                                       long-consumer-accumulator]
             :as hamf-rf]
            [ham-fisted.protocols :as protocols])
  (:import [ham_fisted UnsharedHashMap UnsharedLongHashMap UnsharedHashSet
            PersistentHashSet PersistentHashMap PersistentLongHashMap
            ArrayLists$ArrayOwner
            HashProvider MapSetOps SetOps ObjArray UpdateValues
            MutList ImmutList StringCollection ArrayImmutList ArrayLists
            ImmutSort IMutList Ranges$LongRange ArrayHelpers
            Ranges$DoubleRange IFnDef Transformables$MapIterable
            Transformables$FilterIterable Transformables$CatIterable
            Transformables$MapList Transformables$IMapable Transformables
            ReindexList ConstList ArrayLists$ObjectArrayList Transformables$SingleMapList
            ArrayLists$IntArrayList ArrayLists$LongArrayList ArrayLists$DoubleArrayList
            ReverseList TypedList DoubleMutList LongMutList
            Consumers Sum Sum$SimpleSum Casts Reducible IndexedDoubleConsumer
            IndexedLongConsumer IndexedConsumer ITypedReduce ParallelOptions Reductions
            IFnDef$LO IFnDef$LL IFnDef$DO IFnDef$DD IFnDef$DDD
            IFnDef$LLL ParallelOptions$CatParallelism IFnDef$OO IFnDef$OOO IFnDef$ODO
            IFnDef$OLO IFnDef$OD IFnDef$OL IFnDef$LD IFnDef$DL IFnDef$OLOO IFnDef$OLDO
            IFnDef$OLLO IFnDef$LongPredicate IFnDef$DoublePredicate IFnDef$Predicate
            Consumers$IncConsumer Reductions$IndexedDoubleAccum Reductions$IndexedLongAccum
            Reductions$IndexedAccum MutableMap IAMapEntry MapForward TypedNth]
           [ham_fisted.alists ByteArrayList ShortArrayList CharArrayList FloatArrayList
            BooleanArrayList]
           [clojure.lang ITransientAssociative2 ITransientCollection Indexed
            IEditableCollection RT IPersistentMap Associative Util IFn ArraySeq
            Reversible IReduce IReduceInit IFn$DD IFn$DL IFn$DO IFn$LD IFn$LL IFn$LO
            IFn$OD IFn$OL IFn$OLO IFn$ODO IObj Util IReduceInit Seqable IteratorSeq
            ITransientMap Counted Box]
           [java.util Map Map$Entry List RandomAccess Set Collection ArrayList Arrays
            Comparator Random Collections Iterator PriorityQueue LinkedHashMap]
           [java.lang.reflect Array]
           [java.util.function Function BiFunction BiConsumer Consumer
            DoubleBinaryOperator LongBinaryOperator LongFunction IntFunction
            DoubleConsumer DoublePredicate DoubleUnaryOperator LongPredicate
            LongUnaryOperator LongConsumer Predicate UnaryOperator]
           [java.util.concurrent ForkJoinPool ExecutorService Callable Future
            ConcurrentHashMap ForkJoinTask ArrayBlockingQueue]
           [it.unimi.dsi.fastutil.ints IntComparator IntArrays]
           [it.unimi.dsi.fastutil.longs LongComparator]
           [it.unimi.dsi.fastutil.floats FloatComparator]
           [it.unimi.dsi.fastutil.doubles DoubleComparator DoubleArrays]
           [it.unimi.dsi.fastutil.objects ObjectArrays]
           [com.github.benmanes.caffeine.cache Caffeine LoadingCache CacheLoader Cache
            RemovalCause]
           [com.github.benmanes.caffeine.cache.stats CacheStats]
           [java.time Duration]
           [java.util.logging Logger]
           [java.util.stream IntStream DoubleStream])
  (:refer-clojure :exclude [assoc! conj! frequencies merge merge-with memoize
                            into hash-map
                            group-by subvec group-by mapv vec vector object-array
                            sort int-array long-array double-array float-array
                            range map concat filter filterv first last pmap take take-last drop
                            drop-last sort-by repeat repeatedly shuffle into-array
                            empty? reverse byte-array short-array char-array boolean-array
                            keys vals persistent! rest transient update-vals]))

(comment
  (require '[clj-java-decompiler.core :refer [disassemble]])
  )


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(declare assoc! conj! vec mapv vector object-array range first take drop into-array shuffle
         object-array-list int-array-list long-array-list double-array-list
         int-array argsort byte-array short-array char-array boolean-array repeat
         persistent! rest immut-map keys vals group-by-reduce
         reindex group-by-consumer
         merge constant-count mutable-map?
         transient update-vals apply-concat)


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



(defn- check-deprecated-provider
  [options]
  (when (:hash-provider options)
    (.warning (Logger/getGlobal) "Hash providers have been deprecated")))


(def ^{:tag PersistentHashMap
       :doc "Constant persistent empty map"} empty-map PersistentHashMap/EMPTY)
(def ^{:tag PersistentHashSet
       :doc "Constant persistent empty set"} empty-set PersistentHashSet/EMPTY)
(def ^{:tag ArrayImmutList
       :doc "Constant persistent empty vec"} empty-vec ArrayImmutList/EMPTY)


(defn- empty-map?
  [m]
  (or (nil? m)
      (and (instance? Map m)
           (== 0 (.size ^Map m)))))



(def ^:private empty-objs (clojure.core/object-array 0))


(defn obj-ary
  "As quickly as possible, produce an object array from these inputs.  Very fast for arities
  <= 16."
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
     (-> (reduce conj! (transient container) data)
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


(defonce ^:private obj-ary-cls (type (clojure.core/object-array 0)))


(defn mut-map-rf
  ([cons-fn] (mut-map-rf cons-fn nil))
  ([cons-fn finalize-fn]
   (fn
     ([] (cons-fn))
     ([acc] (if finalize-fn (finalize-fn acc) acc))
     ([^Map m d]
      (cond
        (instance? Map$Entry d)
        (.put m (.getKey ^Map$Entry d) (.getValue ^Map$Entry d))
        (instance? Indexed d)
        (.put m (.nth ^Indexed d 0) (.nth ^Indexed d 1))
        :else
        (throw (Exception. "Unrecognized map input")))
      m))))


(defn transient-map-rf
  ([cons-fn] (transient-map-rf cons-fn nil))
  ([cons-fn finalize-fn]
   (fn
     ([] (cons-fn))
     ([acc] (if finalize-fn (finalize-fn acc) acc))
     ([^ITransientCollection m d] (.conj m d)))))


(defn- tduce
  [xform rf data]
  (if xform
    (transduce xform rf data)
    (rf (reduce rf (rf) data))))


(defn mut-hashtable-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  the various members of the java.util.Map interface such as put, compute, computeIfAbsent,
  replaceAll and merge.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned."
  (^UnsharedHashMap [] (UnsharedHashMap. nil))
  (^UnsharedHashMap [data] (mut-hashtable-map nil nil data))
  (^UnsharedHashMap [xform data] (mut-hashtable-map xform nil data))
  (^UnsharedHashMap [xform options data]
   (cond
     (number? data)
     (UnsharedHashMap. nil (int data))
     (nil? xform)
     (cond
       (instance? obj-ary-cls data)
       (UnsharedHashMap/create data)
       (instance? Map data)
       (doto (UnsharedHashMap. nil (.size ^Map data))
         (.putAll data))
       :else
       (into (UnsharedHashMap. nil) data))
     :else
     (into (UnsharedHashMap. nil) xform data))))


(defn mut-long-hashtable-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  the various members of the java.util.Map interface such as put, compute, computeIfAbsent,
  replaceAll and merge.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned."
  (^UnsharedLongHashMap [] (UnsharedLongHashMap. nil))
  (^UnsharedLongHashMap [data] (mut-long-hashtable-map nil nil data))
  (^UnsharedLongHashMap [xform data] (mut-long-hashtable-map xform nil data))
  (^UnsharedLongHashMap [xform options data]
   (cond
     (number? data)
     (UnsharedLongHashMap. nil (int data))
     (nil? xform)
     (cond
       (instance? obj-ary-cls data)
       (UnsharedLongHashMap/create data)
       (instance? Map data)
       (doto (UnsharedLongHashMap. nil (.size ^Map data))
         (.putAll data))
       :else
       (into (UnsharedLongHashMap. nil) data))
     :else
     (into (UnsharedLongHashMap. nil) xform data))))


(defn mut-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  the various members of the java.util.Map interface such as put, compute, computeIfAbsent,
  replaceAll and merge.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned."
  (^UnsharedHashMap [] (mut-hashtable-map))
  (^UnsharedHashMap [data] (mut-hashtable-map data))
  (^UnsharedHashMap [xform data] (mut-hashtable-map xform data))
  (^UnsharedHashMap [xform options data] (mut-hashtable-map xform options data)))


(defn mut-long-map
  "Create a mutable implementation of java.util.Map specialized to long keys.  This object
  efficiently implements ITransient map so you can use assoc! and persistent! on it but you can additionally use
  the various members of the java.util.Map interface such as put, compute, computeIfAbsent,
  replaceAll and merge.  Attempting to store any non-numeric value will result in an exception.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned."
  (^UnsharedLongHashMap [] (mut-long-hashtable-map))
  (^UnsharedLongHashMap [data] (mut-long-hashtable-map data))
  (^UnsharedLongHashMap [xform data] (mut-long-hashtable-map xform data))
  (^UnsharedLongHashMap [xform options data] (mut-long-hashtable-map xform options data)))


(defn constant-countable?
  "Return true if data has a constant time count."
  [data]
  (lznc/constant-countable? data))


(defn constant-count
  "Constant time count.  Returns nil if input doesn't have a constant time count."
  [data]
  (lznc/constant-count data))


(defn immut-map
  "Create an immutable map.  This object supports conversion to a transient map via
  Clojure's `transient` function.  Duplicate keys are treated as if by assoc.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  If you know you will have consistently more key/val pairs than 8 you should just
  use `(persistent! (mut-map data))` as that avoids the transition from an arraymap
  to a persistent hashmap.

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
   (-> (mut-map options data)
       (persistent!))))


(defn hash-map
  "Drop-in replacement to Clojure's hash-map function."
  ([] empty-map)
  ([a b]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b))))
  ([a b c d]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d))))
  ([a b c d e f]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d)
                  (.put e f))))
  ([a b c d e f g h]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d)
                  (.put e f)
                  (.put g h))))
  ([a b c d e f g h i j]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d)
                  (.put e f)
                  (.put g h)
                  (.put i j))))
  ([a b c d e f g h i j k l]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d)
                  (.put e f)
                  (.put g h)
                  (.put i j)
                  (.put k l))))
  ([a b c d e f g h i j k l m n]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d)
                  (.put e f)
                  (.put g h)
                  (.put i j)
                  (.put k l)
                  (.put m n))))
  ([a b c d e f g h i j k l m n o p]
   (persistent! (doto (mut-hashtable-map)
                  (.put a b)
                  (.put c d)
                  (.put e f)
                  (.put g h)
                  (.put i j)
                  (.put k l)
                  (.put m n)
                  (.put o p))))
  ([a b c d e f g h i j k l m n o p & args]
   (persistent! (UnsharedHashMap/create
                 (ObjArray/createv a b c d e f g h i j k l m n o p (object-array args))))))


(defn java-hashmap
  "Create a java.util.HashMap.  Duplicate keys are treated as if map was created by assoc."
  (^java.util.HashMap [] (java.util.HashMap.))
  (^java.util.HashMap [data] (java-hashmap nil nil data))
  (^java.util.HashMap [xform data] (java-hashmap xform nil data))
  (^java.util.HashMap [xform options data]
   (cond
     (number? data)
     (java.util.HashMap. (int data))
     (and (nil? xform) (instance? Map data))
     (if (instance? java.util.HashMap data)
       (.clone ^java.util.HashMap data)
       (java.util.HashMap. ^Map data))
     :else
     (tduce xform
            (mut-map-rf #(java.util.HashMap. (long (get options :init-size 0))))
            data))))


(defn java-linked-hashmap
  "Linked hash maps perform identically or very nearly so to java.util.HashMaps
  but they retain the order of insertion and modification."
  (^java.util.LinkedHashMap [] (java.util.LinkedHashMap.))
  (^java.util.LinkedHashMap [data]
   (cond
     (instance? java.util.LinkedHashMap data) data
     (number? data) (java.util.LinkedHashMap. (int data))
     (instance? java.util.LinkedHashMap data)
     (.clone ^java.util.LinkedHashMap data)
     (instance? Map data)
     (java.util.LinkedHashMap. ^Map data)
     :else
     (tduce nil (mut-map-rf #(java.util.LinkedHashMap.)) data))))


(defn linked-hashmap
  "Linked hash map using clojure's equiv pathways.  At this time the node link order reflects
  insertion order.  Modification and access do not affect the node link order."
  (^ham_fisted.LinkedHashMap [] (ham_fisted.LinkedHashMap.))
  (^ham_fisted.LinkedHashMap [data]
   (let [rv (ham_fisted.LinkedHashMap.)]
     (if (instance? Map data)
       (do (.putAll rv data) rv)
       (tduce nil (mut-map-rf (constantly rv)) data)))))


(defn java-concurrent-hashmap
  "Create a java concurrent hashmap which is still the fastest possible way to solve a
  few concurrent problems."
  (^ConcurrentHashMap [] (ConcurrentHashMap.))
  (^ConcurrentHashMap [data]
   (cond
     (instance? ConcurrentHashMap data) data
     (number? data) (ConcurrentHashMap. (int data))
     (instance? Map data)
     (ConcurrentHashMap. ^Map data)
     :else
     (tduce nil (mut-map-rf #(ConcurrentHashMap.)) data))))


(defn mut-set
  "Create a mutable hashset based on the hashtable. You can create a persistent hashset via
  the clojure `persistent!` call.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^UnsharedHashSet [] (UnsharedHashSet. nil))
  (^UnsharedHashSet [data] (into (UnsharedHashSet. nil) data))
  (^UnsharedHashSet [options data] (into (UnsharedHashSet. nil) data)))


(defn immut-set
  "Create an immutable hashset based on a hash table.  This object supports conversion
  to transients via `transient`.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^PersistentHashSet [] empty-set)
  (^PersistentHashSet [data] (into empty-set data))
  (^PersistentHashSet [options data] (into empty-set data)))


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
     (doto (MutList.) (.addAllReducible data))
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
   (if (instance? obj-ary-cls data)
     (ArrayImmutList/create true nil data)
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
    (instance? Collection obj)
    (do (.add ^Collection obj val) obj)
    (instance? Map obj)
    (do (cond
          (instance? Indexed val)
          (let [^Indexed val val]
            (.put ^Map obj (.nth val 0) (.nth val 1))
            obj)
          (instance? Map$Entry val)
          (let [^Map$Entry val val]
            (.put ^Map obj (.getKey val) (.getValue val))
            obj)
          :else
          (throw (RuntimeException. (str "Cannot conj " val " to a map.")))))
    :else
    (throw (Exception. "Item cannot be conj!'d"))))


(defn add-all!
  "Add all items from l2 to l1.  l1 is expected to be a java.util.List implementation.
  Returns l1."
  [l1 l2]
  (if (instance? IMutList l1)
    (.addAllReducible ^IMutList l1 l2)
    (.addAll (->collection l1) l2))
  l1)



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


(defn ^:no-doc map-set?
  [item]
  (instance? MapSetOps item))

(defn ^:no-doc as-map-set
  ^MapSetOps [item] item)

(defn- ->set
  ^Set [item]
  (cond
    (instance? Set item)
    item
    (instance? Map item)
    (.keySet ^Map item)
    :else
    (immut-set item)))


(defn keys
  "Return the keys of a map.  This version allows parallel reduction operations on
  the returned sequence."
  [m]
  (map key m))


(defn vals
  "Return the values of a map.  This version allows parallel reduction operations on
  the returned sequence.  Returned sequence is in same order as `(keys m)`."
  [m]
  (map val m))

(defmacro make-map-entry
  "Create a dynamic implementation of clojure's IMapEntry class."
  [k-code v-code]
  `(reify IAMapEntry
     (getKey [this#] ~k-code)
     (getValue [this#] ~v-code)))


(defn map-union
  "Take the union of two maps returning a new map.  bfn is a function that takes 2 arguments,
  map1-val and map2-val and returns a new value.  Has fallback if map1 and map2 aren't backed
  by bitmap tries.

   * `bfn` - A function taking two arguments and returning one.  `+` is a fine choice.
   * `map1` - the lhs of the union.
   * `map2` - the rhs of the union.

  Returns a persistent map if input is a persistent map else if map1 is a mutable map
  map1 is returned with overlapping entries merged.  In this way you can pass in a
  normal java hashmap, a linked java hashmap, or a persistent map and get back a result that
  matches the input.

  If map1 and map2 are the same returns map1."
  [bfn map1 map2]
  (cond
    (nil? map1) map2
    (nil? map2) map1
    (identical? map1 map2) map1
    :else
    (let [bfn (->bi-function bfn)]
      (if (map-set? map1)
        (.union (as-map-set map1) map2 bfn)
        (let [map1 (if (mutable-map? map1)
                     map1
                     (mut-map map1))]
          (reduce (fn [acc kv]
                    (.merge ^Map map1 (key kv) (val kv) bfn))
                  nil
                  map2)
          map1)))))


(defn map-union-java-hashmap
  "Take the union of two maps returning a new map.  See documentation for [map-union].
  Returns a java.util.HashMap."
  ^java.util.HashMap [bfn ^Map lhs ^Map rhs]
  (map-union bfn (java-hashmap lhs) rhs))

(def ^:no-doc rhs-wins (hamf-fn/bi-function l r r))


(defn ^:no-doc map-set-union-fallback
  [m m2]
  (-> (reduce (fn [^ITransientMap l e]
                (.conj l e))
              (mut-hashtable-map m)
              m2)
      (persistent!)))


(defn ^:no-doc set-union-fallback
  [m m2]
  (-> (reduce (fn [^UnsharedHashSet l e]
                (.conj l e))
              (mut-set m)
              m2)
      (persistent!)))


(defn mutable-map?
  [m]
  (or (instance? MutableMap m)
      (and (instance? Map m)
           (not (instance? IPersistentMap m))
           (not (instance? ITransientMap m)))))


(defn union
  "Union of two sets or two maps.  When two maps are provided the right hand side
  wins in the case of an intersection - same as merge.

  Result is either a set or a map, depending on if s1 is a set or map."
  [s1 s2]
  (cond
    (nil? s1) s2
    (nil? s2) s1
    (identical? s1 s2) s1
    (and (instance? Map s1) (instance? Map s2))
    (cond
      (mutable-map? s1)
      (do (.putAll ^Map s1 s2) s1)
      (map-set? s1)
      (.union (as-map-set s1) ^Map s2 rhs-wins)
      :else
      (persistent!
       (reduce-kv (fn [acc k v]
                 (assoc! acc k v))
               (transient s1)
               s2)))
    (instance? SetOps s1)
    (.union ^SetOps s1 s2)
    :else
    (set-union-fallback s1 s2)))


(defn union-reduce-maps
  "Do an efficient union reduction across many maps using bfn to update values.
  If the first map is mutable the union is done mutably into the first map and it is
  returned."
  ([bfn maps]
   (let [bfn (->bi-function bfn)]
     (-> (reduce #(map-union bfn %1 %2) maps)
         (persistent!)))))


(defn ^:no-doc union-reduce-java-hashmap
  "Do an efficient union of many maps into a single java.util.HashMap."
  (^java.util.HashMap [bfn maps options]
   (let [maps (->reducible maps)]
     (if (nil? maps)
       nil
       (let [bfn (->bi-function bfn)]
         (reduce (fn [acc v]
                        (map-union bfn acc v))
                      (java-hashmap (first maps))
                      (rest maps))))))
  (^java.util.HashMap [bfn maps]
   (union-reduce-java-hashmap bfn maps nil)))


(defn difference
  "Take the difference of two maps (or sets) returning a new map.  Return value is a map1
  (or set1) without the keys present in map2."
  [map1 map2]
  (cond
    (or (nil? map1) (nil? map2))
    map1
    (map-set? map1)
    (if (instance? Map map2)
      (.difference (as-map-set map1) ^Map map2)
      (.difference (as-map-set map1) (->set map2)))
    (instance? SetOps map1)
    (.difference ^SetOps map1 (->set map2))
    (instance? Set map1)
    (let [map2 (->set map2)]
      (-> (reduce (fn [^Set acc v]
                    (when-not (.contains map2 v)
                      (.add acc v))
                    acc)
                  (mut-set)
                  map1)
          (persistent!)))
    (instance? Map map1)
    (let [map2 (->set map2)]
      (-> (reduce (fn [^Map acc kv]
                    (when-not (.contains map2 (key kv))
                      (.remove acc (key kv)))
                    acc)
                  (mut-map)
                  map1)
          (persistent!)))))


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
      (if (map-set? map1)
        (if (instance? Map map2)
          (.intersection (as-map-set map1) ^Map map2 bfn)
          (.intersection (as-map-set map1) (->set map2)))
        (let [retval (mut-map)]
          (.forEach ^Map map1 (reify BiConsumer
                                (accept [this k v]
                                  (let [vv (.getOrDefault ^Map map2 k ::failure)]
                                    (when-not (identical? vv ::failure)
                                      (.put ^Map retval k (.apply bfn v vv)))))))
          (persistent! retval))))))


(defn intersection
  "Intersect the keyspace of set1 and set2 returning a new set.  Also works if s1 is a
  map and s2 is a set - the map is trimmed to the intersecting keyspace of s1 and s2."
  [s1 s2]
  (cond
    (or (nil? s1) (nil? s2)) empty-set
    (instance? SetOps s1)
    (.intersection ^SetOps s1 (if (instance? Map s2)
                                (.keySet ^Map s2)
                                (->set s2)))
    (instance? Map s1)
    (map-intersection rhs-wins s1 s2)
    :else
    (let [retval (mut-set)
          [s1 s2] (if (< (count s1) (count s2))
                    [s1 s2]
                    [s2 s1])
          s2 (->set s2)]
      (-> (reduce (fn [rv v]
                    (when (.contains s2 v)
                      (.add ^Set rv v))
                    rv)
                  (mut-set)
                  s1)
          (persistent!)))))


(defn update-values
  "Immutably (or mutably) update all values in the map returning a new map.
  bfn takes 2 arguments, k,v and returns a new v. Returning nil removes the key from the map.
  When passed a vector the keys are indexes and no nil-removal is done."
  [map bfn]
  (let [bfn (->bi-function bfn)]
    (cond
      (instance? UpdateValues map)
      (.updateValues ^UpdateValues map bfn)
      (or (instance? ITransientMap map) (instance? IPersistentMap map))
      (-> (reduce (fn [^Map acc kv]
                    (when-let [v (.apply bfn (key kv) (val kv))]
                      (.put acc (key kv) v))
                    acc)
                  (mut-map)
                  map)
          (persistent!))
      (instance? Map map)
      (let [iter (.iterator (.entrySet ^Map map))]
        (loop [continue? (.hasNext iter)]
          (if continue?
            (let [kv (.next iter)]
              (if-let [v (.apply bfn (key kv) (val kv))]
                (.setValue ^Map$Entry kv v)
                (.remove iter))
              (recur (.hasNext iter)))
            map)))
      (instance? RandomAccess map)
      (mut-list (lznc/map-indexed #(.apply bfn %1 %2) map))
      :else
      (map-indexed #(.apply bfn %1 %2) map))))


(defn update-vals
  [data f]
  (update-values data (bi-function k v (f v))))


(defn mapmap
  "Clojure's missing piece. Map over the data in src-map, which must be a map or
  sequence of pairs, using map-fn. map-fn must return either a new key-value
  pair or nil. Then, remove nil pairs, and return a new map. If map-fn returns
  more than one pair with the same key later pair will overwrite the earlier
  pair.

  Logically the same as:

  ```clojure
  (into {} (comp (map map-fn) (remove nil?)) src-map)
  ```"
  [map-fn src-map]
  (-> (reduce (fn [^Map m entry]
                (let [^Indexed result (map-fn entry)]
                  (when result
                    (.put m (.nth result 0) (.nth result 1)))
                  m))
              (mut-map nil {:init-size (or (constant-count src-map) 16)} nil)
              src-map)
      (persistent!)))


(defn in-fork-join-task?
  "True if you are currently running in a fork-join task"
  []
  (ForkJoinTask/inForkJoinPool))


(def ^:private default-pgroup-opts (options->parallel-options {:ordered? true}))


(defn pgroups
  "Run y index groups across n-elems.   Y is common pool parallelism.

  body-fn gets passed two longs, startidx and endidx.

  Returns a sequence of the results of body-fn applied to each group of indexes.

  Before using this primitive please see if [[ham-fisted.reduce/preduce]] will work.

  You *must* wrap this in something that realizes the results if you need the parallelization
  to finish by a particular point in the program - `(dorun (hamf/pgroups ...))`.

  Options:

  * `:pgroup-min` - when provided n-elems must be more than this value for the computation
    to be parallelized.
  * `:batch-size` - max batch size.  Defaults to 64000."
  ([n-elems body-fn options]
   (impl/pgroups n-elems body-fn (options->parallel-options (assoc options :ordered? true))))
  ([n-elems body-fn]
   (impl/pgroups n-elems body-fn default-pgroup-opts)))


(def ^:private default-upgroup-opts (options->parallel-options {:ordered? false}))


(defn upgroups
  "Run y index groups across n-elems.   Y is common pool parallelism.

  body-fn gets passed two longs, startidx and endidx.

  Returns a sequence of the results of body-fn applied to each group of indexes.

  Before using this primitive please see if [[ham-fisted.reduce/preduce]] will work.

  You *must* wrap this in something that realizes the results if you need the parallelization
  to finish by a particular point in the program - `(dorun (hamf/upgroups ...))`.

  Options:

  * `:pgroup-min` - when provided n-elems must be more than this value for the computation
    to be parallelized.
  * `:batch-size` - max batch size.  Defaults to 64000."
  ([n-elems body-fn options]
   (impl/pgroups n-elems body-fn (options->parallel-options (assoc options :ordered? false))))
  ([n-elems body-fn]
   (impl/pgroups n-elems body-fn default-upgroup-opts)))


(defn pmap
  "pmap using the commonPool.  This is useful for interacting with other primitives, namely
  [[pgroups]] which are also based on this pool.  This is a change from Clojure's base
  pmap in that it uses the ForkJoinPool/commonPool for parallelism as opposed to the
  agent pool - this makes it compose with pgroups and dtype-next's parallelism system.

    Before using this primitive please see if [[ham-fisted.reduce/preduce]] will work.

  Is guaranteed to *not* trigger the need for `shutdown-agents`."
  [map-fn & sequences]
  (impl/pmap (ParallelOptions. 0 64000 true) map-fn sequences))


(defn pmap-opts
  "[[pmap]] but takes an extra option map as the *first* argument.  This is useful if you,
   for instance, want to control exactly the parallel options arguments such as
  `:n-lookahead`.  See docs for [[ham-fisted.reduce/options->parallel-options]]."
  [opts map-fn & sequences]
  (impl/pmap (hamf-rf/options->parallel-options opts) map-fn sequences))


(defn upmap
  "Unordered pmap using the commonPool.  This is useful for interacting with other
  primitives, namely [[pgroups]] which are also based on this pool.

  Before using this primitive please see if [[ham-fisted.reduce/preduce]] will work.

  Like pmap this uses the commonPool so it composes with this api's pmap, pgroups, and
  dtype-next's parallelism primitives *but* it does not impose an ordering constraint on the
  results and thus may be significantly faster in some cases."
  [map-fn & sequences]
  (impl/pmap (ParallelOptions. 0 64000 false) map-fn sequences))


(defn persistent!
  "If object is an ITransientCollection, call clojure.core/persistent!.  Else return
  collection."
  [v]
  (if (instance? ITransientCollection v)
    (clojure.core/persistent! v)
    v))


(defn transient
  [v]
  (if (instance? IEditableCollection v)
    (clojure.core/transient v)
    v))


(defn mut-map-union!
  "Very fast union that may simply update lhs and return it.  Both lhs and rhs *must* be
  mutable maps.  See docs for [[map-union]]."
  [merge-bifn ^Map l ^Map r]
  (cond
    (identical? l r) l
    (map-set? l) (.union ^MapSetOps l r (->bi-function merge-bifn))
    :else
    (let [merge-bifn (->bi-function merge-bifn)]
      (reduce (fn [acc kv]
                (.merge ^Map acc (key kv) (val kv) merge-bifn)
                acc)
              l r))))

(defn freq-reducer
  "Return a hamf parallel reducer that performs a frequencies operation."
  ([options]
   (let [map-fn (get options :map-fn mut-map)
         cfn (function _v (Consumers$IncConsumer.))
         sk? (get options :skip-finalize?)
         fin-bfn (when-not sk? (bi-function k v (deref v)))]
     (reify
       protocols/Reducer
       (->init-val-fn [this] mut-map)
       (->rfn [this] (fn [acc v]
                       (.inc ^Consumers$IncConsumer (.computeIfAbsent ^Map acc v cfn))
                       acc))
       protocols/ParallelReducer
       (->merge-fn [this] hamf-rf/reducible-merge)
       protocols/Finalize
       (finalize [this v]
         (if sk?
           v
           (update-values v fin-bfn))))))
  ([] (freq-reducer nil)))

(def ^:private nil-freq-reducer (freq-reducer nil))
(def ^:private freq-parallel-opts (options->parallel-options {:min-n 1000
                                                              :ordered? true}))

(defn frequencies
  "Faster implementation of clojure.core/frequencies."
  ([coll] (frequencies nil coll))
  ([options coll]
   (preduce-reducer (if options (freq-reducer options) nil-freq-reducer)
                    (if options options freq-parallel-opts)
                    coll)))


(defn ^:no-doc inc-consumer
  "Return a consumer which increments a long counter.  Consumer ignores
  its input.  Deref the consumer to get the value of the counter."
  ^Consumer [] #(Consumers$IncConsumer.))


(def ^{:doc "A hamf reducer that works with inc-consumers"} inc-consumer-reducer
  (reify
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/Reducer
    (->init-val-fn [this] #(Consumers$IncConsumer.))
    (->rfn [this] hamf-rf/consumer-accumulator)
    protocols/ParallelReducer
    (->merge-fn [this] hamf-rf/reducible-merge)))


(defn merge
  "Merge 2 maps with the rhs values winning any intersecting keys.  Uses map-union
  with `BitmapTrieCommon/rhsWins`.

  Returns a new persistent map."
  ([] nil)
  ([m1] m1)
  ([m1 m2] (union m1 m2))
  ([m1 m2 & args]
   ;;union on mutable maps can just be putAll.
   (reduce #(union %1 %2)
           (union m1 m2)
           args)))


(defn merge-with
  "Merge (union) any number of maps using `f` as the merge operator.  `f` gets passed two
  arguments, lhs-val and rhs-val and must return a new value.

  Returns a new persistent map."
  ([f] nil)
  ([f m1] m1)
  ([f m1 m2] (map-union f m1 m2))
  ([f m1 m2 & args]
   (union-reduce-maps f (apply-concat [(map-union f m1 m2)] args))))


(defn memoize
  "Efficient thread-safe version of clojure.core/memoize.

  Also see [[clear-memoized-fn!]] [[evict-memoized-call]] and [[memoize-cache-as-map]] to
  mutably clear the backing store, manually evict a value, and get a java.util.Map view of
  the cache backing store.


```clojure
ham-fisted.api> (def m (memoize (fn [& args] (println \"fn called - \" args) args)
                                {:write-ttl-ms 1000 :eviction-fn (fn [args rv cause]
                                                                   (println \"evicted - \" args rv cause))}))
#'ham-fisted.api/m
ham-fisted.api> (m 3)
fn called -  (3)
(3)
ham-fisted.api> (m 4)
fn called -  (4)
(4)evicted -  [3] (3) :expired
ham-fisted.api> (dotimes [idx 4] (do (m 3) (evict-memoized-call m [3])))
fn called -  (3)
fn called -  (3)
fn called -  (3)
fn called -  (3)
nil
ham-fisted.api> (dotimes [idx 4] (do (m 3) #_(evict-memoized-call m [3])))
fn called -  (3)
nil
```

  Options:

  * `:write-ttl-ms` - Time that values should remain in the cache after write in milliseconds.
  * `:access-ttl-ms` - Time that values should remain in the cache after access in milliseconds.
  * `:soft-values?` - When true, the cache will store [SoftReferences](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/SoftReference.html) to the data.
  * `:weak-values?` - When true, the cache will store [WeakReferences](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html) to the data.
  * `:max-size` - When set, the cache will behave like an LRU cache.
  * `:record-stats?` - When true, the LoadingCache will record access statistics.  You can
     get those via the undocumented function memo-stats.
  * `:eviction-fn - Function that receives 3 arguments, [args v cause], when a value is
     evicted.  Causes the keywords `:collected :expired :explicit :replaced and :size`.  See
     [caffeine documentation](https://www.javadoc.io/static/com.github.ben-manes.caffeine/caffeine/2.9.3/com/github/benmanes/caffeine/cache/RemovalCause.html) for cause definitions."
  ([memo-fn] (memoize memo-fn nil))
  ([memo-fn {:keys [write-ttl-ms
                    access-ttl-ms
                    soft-values?
                    weak-values?
                    max-size
                    record-stats?
                    eviction-fn]}]
   (let [^Caffeine new-builder
         (cond-> (Caffeine/newBuilder)
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
           (.recordStats)
           eviction-fn
           (.evictionListener (reify com.github.benmanes.caffeine.cache.RemovalListener
                                (onRemoval [this args v cause]
                                  (eviction-fn args (.-val ^Box v)
                                               (condp identical? cause
                                                 RemovalCause/COLLECTED :collected
                                                 RemovalCause/EXPIRED :expired
                                                 RemovalCause/EXPLICIT :explicit
                                                 RemovalCause/REPLACED :replaced
                                                 RemovalCause/SIZE :size
                                                 (keyword (.toLowerCase (str cause)))))))))
         ^LoadingCache cache
         (.build new-builder
                 (proxy [CacheLoader] []
                   (load [args]
                     (Box.
                      (case (count args)
                        0 (memo-fn)
                        1 (memo-fn (args 0))
                        2 (memo-fn (args 0) (args 1))
                        3 (memo-fn (args 0) (args 1) (args 2))
                        (.applyTo ^IFn memo-fn (seq args)))))))]
     (-> (fn
           ([] (.val ^Box (.get cache [])))
           ([a] (.val ^Box (.get cache [a])))
           ([a b] (.val ^Box (.get cache [a b])))
           ([a b c] (.val ^Box (.get cache [a b c])))
           ([a b c & args] (let [^IMutList obj-ary (mut-list)]
                             (.add obj-ary a)
                             (.add obj-ary b)
                             (.add obj-ary c)
                             (.addAllReducible obj-ary args)
                             (.val ^Box (.get cache (persistent! obj-ary))))))
         (with-meta {:cache cache})))))


(defn clear-memoized-fn!
  "Clear a memoized function backing store."
  [memoized-fn]
  (if-let [map (get (meta memoized-fn) :cache)]
    (clear! map)
    (throw (Exception. (str "Arg is not a memoized fn - " memoized-fn))))
  memoized-fn)


(defn memoize-cache-as-map
  "Return the memoize backing store as an implementation of java.util.Map."
  ^Map [memoized-fn]
  (when-let [^Cache cache (get (meta memoized-fn) :cache)]
    (.asMap cache)))


(defn evict-memoized-call
  [memo-fn fn-args]
  (when-let [cache (memoize-cache-as-map memo-fn)]
    (.remove cache (vec fn-args))))


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
         :average-load-penalty-nanos (.averageLoadPenalty cache-map)
         :total-load-time-nanos (.totalLoadTime cache-map)
         :eviction-count (.evictionCount cache-map)}))))



(defn subvec
  "More general version of subvec.  Works for any java list implementation
  including persistent vectors and any array."
  ([m sidx eidx]
   (let [^List m (if (instance? List m)
                   m
                   (->random-access m))]
     (.subList m sidx eidx)))
  ([m sidx] (subvec m sidx (count m))))


(defn group-by-reduce
  "Group by key. Apply the reduce-fn with the new value an the return of init-val-fn.
  Merged maps due to multithreading will be merged with merge-fn in a similar way
  of [[preduce]].

  This type of reduction can be both faster and more importantly use
  less memory than a reduction of the forms:

```clojure
  (->> group-by map into)
  ;; or
  (->> group-by mapmap)
```

  Options (which are passed to [[preduce]]):

  * `:map-fn` Function which takes no arguments and must return an instance of
    java.util.Map that supports `computeIfAbsent`.  Some examples:
    - `(constantly (java.util.concurrent.ConcurrentHashMap. ...))`  Very fast update
       especially in the case where the keyspace is large.
    - `mut-map` - Fast merge, fast update, in-place immutable conversion via `persistent!`.
    - `java-hashmap` - fast merge, fast update, just a simple java.util.HashMap-based reduction.
    - `#(LinkedHashMap.)` - When used with options {:ordered? true} the result keys will be
       in order *and* the result values will be reduced in order."
  ([key-fn init-val-fn rfn merge-fn options coll]
   (let [has-map-fn? (get :map-fn options)
         map-fn (get options :map-fn mut-map)
         merge-bifn (->bi-function merge-fn)
         rfn (cond
               (or (= identity key-fn) (nil? key-fn))
               (let [bifn (bi-function k acc (rfn (or acc (init-val-fn)) k))]
                 (fn [^Map l v]
                   (.compute l v bifn)
                   l))
               ;;These formulations can trigger more efficient primitive reductions when,
               ;;for instance, you are reducing over a stream of integer indexes.
               (and (instance? IFn$LO key-fn) (instance? IFn$OLO rfn))
               (long-accumulator
                l v
                 (.compute ^Map l (.invokePrim ^IFn$LO key-fn v)
                           (bi-function
                            k acc (.invokePrim ^IFn$OLO rfn (or acc (init-val-fn)) v)))
                 l)
               (and (instance? IFn$DO key-fn) (instance? IFn$ODO rfn))
               (double-accumulator
                 l v
                 (.compute ^Map l (.invokePrim ^IFn$DO key-fn v)
                           (bi-function
                            k acc (.invokePrim ^IFn$ODO rfn (or acc (init-val-fn)) v)))
                 l)
               :else
               (fn [^Map l v]
                 ;;It annoys the hell out of me that I have to create a new
                 ;;bifunction here but there is no threadsafe way to pass in the
                 ;;new value to the reducer otherwise.
                 (.compute l (key-fn v) (bi-function k acc (rfn (or acc (init-val-fn)) v)))
                 l))]
     (cond-> (preduce map-fn rfn #(mut-map-union! merge-bifn %1 %2)
                      (merge {:min-n 1000} options)
                      coll)
       ;;In the case where no map-fn was passed in we return a persistent hash map.
       (not has-map-fn?)
       (persistent!))))
  ([key-fn init-val-fn rfn merge-fn coll]
   (group-by-reduce key-fn init-val-fn rfn merge-fn nil coll)))


(defn group-by-reducer
  "Perform a group-by-reduce passing in a reducer.  Same options as group-by-reduce.

  Options:

  * `:skip-finalize?` - skip finalization step."
  ([key-fn reducer coll]
   (group-by-reducer key-fn reducer nil coll))
  ([key-fn reducer options coll]
   (let [finalizer (if (:skip-finalize? options)
                     identity
                     #(update-values % (bi-function k v (protocols/finalize reducer v))))]
     (-> (group-by-reduce key-fn
                          (protocols/->init-val-fn reducer)
                          (protocols/->rfn reducer)
                          (protocols/->merge-fn reducer)
                          options coll)
         (finalizer)))))


(defn group-by-consumer
  "Perform a group-by-reduce passing in a reducer.  Same options as group-by-reduce -
  This uses a slightly different pathway - computeIfAbsent - in order to preserve order.
  In this case the return value of the reduce fn is ignored.  This allows things like
  the linked hash map to preserve initial order of keys.  It map also be slightly
  more efficient because the map itself does not need to check the return value
  of rfn - something that the `.compute` primitive *does* need to do.

  Options:

  * `:skip-finalize?` - skip finalization step."
  ([key-fn reducer coll]
   (group-by-reducer key-fn reducer nil coll))
  ([key-fn reducer options coll]
   (let [finalizer (when-not (:skip-finalize? options)
                     #(update-values % (bi-function k v (protocols/finalize reducer v))))
         has-map-fn? (get :map-fn options)
         map-fn (get options :map-fn mut-map)
         merge-fn (protocols/->merge-fn reducer)
         merge-bifn (->bi-function merge-fn)
         init-fn (protocols/->init-val-fn reducer)
         afn (function k (init-fn))
         rfn (protocols/->rfn reducer)
         rfn (cond
                 (or (= identity key-fn) (nil? key-fn))
                 (fn [^Map l v]
                   (-> (.computeIfAbsent l v afn)
                       (rfn v))
                   l)
                 ;;These formulations can trigger more efficient primitive reductions when,
                 ;;for instance, you are reducing over a stream of integer indexes.
                 (and (instance? IFn$LO key-fn) (instance? IFn$OLO rfn))
                 (long-accumulator
                  l v
                  (let [acc (.computeIfAbsent ^Map l (.invokePrim ^IFn$LO key-fn v) afn)]
                    (.invokePrim ^IFn$OLO rfn acc v))
                  l)
                 (and (instance? IFn$DO key-fn) (instance? IFn$ODO rfn))
                 (double-accumulator
                  l v
                  (let [acc (.computeIfAbsent ^Map l (.invokePrim ^IFn$DO key-fn v) afn)]
                    (.invokePrim ^IFn$ODO rfn acc v))
                  l)
                 :else
                 (fn [^Map l v]
                   (-> (.computeIfAbsent l (key-fn v) afn)
                       (rfn v))
                   l))]
     (let [fin-map (preduce map-fn rfn #(mut-map-union! merge-bifn %1 %2)
                            (merge {:min-n 1000} options)
                            coll)]
       (cond
         finalizer
         (finalizer fin-map)
         (not has-map-fn?)
         (persistent! fin-map)
         :else
         fin-map)))))



(defn group-by
  "Group items in collection by the grouping function f.  Returns persistent map of
  keys to persistent vectors.

  Options are same as [[group-by-reduce]] but this reductions defaults to an
  ordered reduction."
  ([f options coll]
   (group-by-reduce f object-array-list conj! add-all!
                    (merge {:ordered? true :min-n 1000} options) coll))
  ([f coll]
   (group-by f nil coll)))


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
  (^IMutList [^IMutList retval v1 v2]
   (let [retval (mut-list)]
     (.addAllReducible retval (->reducible v1))
     (.addAllReducible retval (->reducible v2))
     retval))
  (^IMutList [^IMutList retval v1 v2 args]
   (when-not (nil? v1) (.addAllReducible retval (->reducible v1)))
   (when-not (nil? v2) (.addAllReducible retval (->reducible v2)))
   (reduce (fn [data c]
                  (when-not (nil? c) (.addAllReducible retval (->reducible c)))
                  retval)
                retval
                args)))

(defn apply-concatv
  [data]
  (reduce (fn [^IMutList v data]
            (when data
              (.addAllReducible v data))
            v)
          (mut-list)
          data))

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
         (.toArray))))
  (^objects [v1 v2 & args]
   (-> (concat-reducible (ArrayLists$ObjectArrayList.) v1 v2 args)
       (.toArray))))


(defn apply-concat
  "Faster lazy noncaching version of (apply concat)"
  [args]
  (if args
    (lznc/apply-concat args)
    '()))


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


(defn sorta
  "Sort returning an object array."
  (^objects [coll] (sorta hamf-fn/comp-nan-last coll))
  (^objects [comp coll]
   (let [coll (->reducible coll)]
     (if (instance? IMutList coll)
       (-> (.immutSort ^IMutList coll comp)
           (.toArray))
       (let [a (object-array coll)]
         (if (< (alength a) 1000)
           (if comp
             (Arrays/sort a (->comparator comp))
             (Arrays/sort a))
           (if comp
             (ObjectArrays/parallelQuickSort a (->comparator comp))
             (ObjectArrays/parallelQuickSort a)))
         a)))))


(defn sort
  "Exact replica of clojure.core/sort but instead of wrapping the final object array in a seq
  which loses the fact the result is countable and random access.  Faster implementations
  are provided when the input is an integer, long, or double array.

  The default comparison is nan-last meaning null-last if the input is an undefined
  container and nan-last if the input is a double or float specific container."
  ([coll] (sort hamf-fn/comp-nan-last coll))
  ([comp coll]
   (let [coll (->collection coll)]
     (if (instance? ImmutSort coll)
       (if (nil? comp)
         (.immutSort ^ImmutSort coll)
         (.immutSort ^ImmutSort coll (->comparator comp)))
       (let [a (sorta comp coll)]
         (ArrayLists/toList a 0 (alength a) ^IPersistentMap (meta coll)))))))


(defn sort-by
  "Sort a collection by keyfn.  Typehinting the return value of keyfn will somewhat increase
  the speed of the sort :-)."
  ([keyfn coll]
   (sort-by keyfn nil coll))
  ([keyfn comp coll]
   (let [coll (->random-access coll)
         data (map keyfn coll)
         ;;Arraylists are faster to create because they do not have to be sized exactly
         ;;to the collection.  They have very fast addAllReducible pathways that specialize
         ;;for constant sized containers.
         data (case (lznc/type-single-arg-ifn keyfn)
                :float64
                (double-array-list data)
                :int64
                (long-array-list data)
                (object-array-list data))
         indexes (argsort comp data)]
     (reindex coll indexes))))


(defn shuffle
  "shuffle values returning random access container.  If you are calling this repeatedly
   on the same collection you should call [[->random-access]] on the collection *before*
   you start as shuffle internally only works on random access collections.

  Options:

  * `:seed` - If instance of java.util.Random, use this.  If integer, use as seed.
  If not provided a new instance of java.util.Random is created."
  (^List [coll] (lznc/shuffle coll nil))
  (^List [coll opts] (lznc/shuffle coll opts)))


(defn binary-search
  "Binary search.  Coll must be a sorted random access container.
  comp must be an implementation of java.lang.Comparator.  If you know your container's
  type, such as a double array, then comp should be a fastutil DoubleComparator.


  The most efficient method will be to convert coll to random access using
  ->random-access, so for a pure double array it is slightly better to call
  ->random-access outside this function before the function call.

  This search defaults to the slower java.util.Collections search using
  clojure's built in `compare` - reason being that that allows you to search
  for a double number in a vector of only longs.  If you want an accelerated search
  you can explicitly pass in a nil comparator *but* you need to make sure that
  you are searching for the rough datatype in the data - e.g. long in a byte array
  or a double in a double for float array.  Searching for doubles in integer arrays
  with an accelerated search will probably result in confusing results.

```clojure
ham-fisted.api> (def data (->random-access (double-array (range 10))))
#'ham-fisted.api/data
ham-fisted.api> (binary-search data 0)
0
ham-fisted.api> (binary-search data -1)
0
ham-fisted.api> (binary-search data 1)
1
ham-fisted.api> (binary-search data 1.1)
2
ham-fisted.api> (binary-search data 10)
10
ham-fisted.api> (binary-search data 11)
10
ham-fisted.api> ;;be wary of datatype conversions in typed containers
ham-fisted.api> (def data (->random-access (int-array (range 10))))
#'ham-fisted.api/data
ham-fisted.api> (binary-search data 1)
1
ham-fisted.api> (binary-search data 1.1)
  2
ham-fisted.api> ;;accelerated search - flattens input to container datatype
ham-fisted.api> (binary-search data 1.1 nil)
1
```"
  (^long [coll v] (binary-search coll v compare))
  (^long [coll v comp]
   (let [comp (when comp (->comparator comp))
         coll (->random-access coll)]
     (if (instance? IMutList coll)
       (if comp
         (.binarySearch ^IMutList coll v comp)
         (.binarySearch ^IMutList coll v))
       (let [rv (if comp
                  (Collections/binarySearch coll v comp)
                  ;;This corrects for things like searching 50.1 in a list that has longs
                  (Collections/binarySearch coll v compare))]
         (if (< rv 0)
           (- -1 rv)
           rv))))))


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
  "Return a double array holding the values of the range.  Use `wrap-array` to get
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
  is an integer, long, or double array.  See also [[reindex]]."
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
   (argsort hamf-fn/comp-nan-last coll)))


(defn ^:no-doc do-make-array
  [clj-ary-fn ary-ra-fn ary-list-fn data]
  (if (number? data)
     (clj-ary-fn data)
     (let [data (->reducible data)]
       (if-let [c (constant-count data)]
         (let [retval (clj-ary-fn c)]
           (.fillRangeReducible ^IMutList (ary-ra-fn retval) 0 data)
           retval)
         (.toNativeArray ^IMutList (ary-list-fn data))))))


(defn byte-array-list
  (^IMutList [] (ByteArrayList. (clojure.core/byte-array 4) 0 nil))
  (^IMutList [data]
   (if (number? data)
     (ByteArrayList. (clojure.core/byte-array data) 0 nil)
     (let [^IMutList retval (if (instance? RandomAccess data)
                              (byte-array-list (.size ^List data))
                              (byte-array-list))]
       (.addAllReducible retval data)
       retval))))


(defn byte-array
  (^bytes [] (byte-array 0))
  (^bytes [data] (do-make-array clojure.core/byte-array
                                #(ArrayLists/toList ^bytes %)
                                byte-array-list data)))


(defn short-array-list
  (^IMutList [] (ShortArrayList. (clojure.core/short-array 4) 0 nil))
  (^IMutList [data]
   (if (number? data)
     (ShortArrayList. (clojure.core/short-array data) 0 nil)
     (let [^IMutList retval (if (instance? RandomAccess data)
                              (short-array-list (.size ^List data))
                              (short-array-list))]
       (.addAllReducible retval data)
       retval))))


(defn short-array
  (^shorts [] (short-array 0))
  (^shorts [data] (do-make-array clojure.core/short-array
                                 #(ArrayLists/toList ^shorts %)
                                 short-array-list data)))


(defn char-array-list
  (^IMutList [] (CharArrayList. (clojure.core/char-array 4) 0 nil))
  (^IMutList [data]
   (if (number? data)
     (CharArrayList. (clojure.core/char-array data) 0 nil)
     (let [^IMutList retval (if (instance? RandomAccess data)
                              (char-array-list (.size ^List data))
                              (char-array-list))]
       (.addAllReducible retval data)
       retval))))


(defn char-array
  (^chars [] (char-array 0))
  (^chars [data] (do-make-array clojure.core/char-array
                                #(ArrayLists/toList ^chars %)
                                char-array-list data)))


(defn boolean-array-list
  (^IMutList [] (BooleanArrayList. (clojure.core/boolean-array 4) 0 nil))
  (^IMutList [data]
   (if (number? data)
     (BooleanArrayList. (clojure.core/boolean-array data) 0 nil)
     (let [^IMutList retval (if (instance? RandomAccess data)
                              (boolean-array-list (.size ^List data))
                              (boolean-array-list))]
       (.addAllReducible retval data)
       retval))))


(defn boolean-array
  (^booleans [] (boolean-array 0))
  (^booleans [data] (do-make-array clojure.core/boolean-array
                                   #(ArrayLists/toList ^booleans %)
                                   boolean-array-list data)))


(defmacro ^:no-doc impl-array-macro
  [data ctor elem-cast vecfn]
  (cond
    (number? data)
    `(~ctor ~data)
    ;;16 chosen arbitrarily
    (and (vector? data) (< (count data) 16))
    `(let [~'ary (~ctor (unchecked-int ~(count data)))]
       (do
         ~@(->> (range (count data))
                (map (fn [^long idx]
                       `(ArrayHelpers/aset ~'ary (unchecked-int ~idx) (~elem-cast ~(data idx))))))
         ~'ary))
    :else
    `(~vecfn ~data)))


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


(def ^:private int-ary-cls (Class/forName "[I"))


(defn ^:no-doc int-array-v
  ^ints [data]
  (cond
    (instance? int-ary-cls data)
    data
    (instance? IMutList data)
    (.toIntArray ^IMutList data)
    :else
    (do-make-array #(ArrayLists/intArray %) #(ArrayLists/toList ^ints %)
                   int-array-list data)))



(defmacro int-array
  ([] `(ArrayLists/intArray 0))
  ([data]
   `(impl-array-macro ~data ArrayLists/intArray Casts/longCast int-array-v)))


(defn reindex
  "Permut coll by the given indexes.  Result is random-access and the same length as
  the index collection.  Indexes are expected to be in the range of [0->count(coll))."
  [coll indexes]
  (lznc/reindex (->random-access coll) (int-array indexes)))


(defmacro ivec
  "Create a persistent-vector-compatible list backed by an int array."
  ([] `(ArrayLists/toList (int-array)))
  ([data] `(ArrayLists/toList (int-array ~data))))


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

(def ^:no-doc long-array-cls (Class/forName "[J"))

(defn ^:no-doc long-array-v
  ^longs [data]
  (cond
    (instance? long-array-cls data)
    data
    (instance? IMutList data)
    (.toLongArray ^IMutList data)
    :else
    (do-make-array #(ArrayLists/longArray %) #(ArrayLists/toList ^longs %)
                   long-array-list data)))


(defmacro long-array
  ([] `(ArrayLists/longArray 0))
  ([data]
   `(impl-array-macro ~data ArrayLists/longArray Casts/longCast long-array-v)))


(defmacro lvec
  "Create a persistent-vector-compatible list backed by a long array."
  ([] `(ArrayLists/toList (long-array)))
  ([data] `(ArrayLists/toList (long-array ~data))))


(defn float-array-list
  (^IMutList [] (FloatArrayList. (clojure.core/float-array 4) 0 nil))
  (^IMutList [data]
   (if (number? data)
     (FloatArrayList. (clojure.core/float-array data) 0 nil)
     (let [^IMutList retval (if (instance? RandomAccess data)
                              (float-array-list (.size ^List data))
                              (float-array-list))]
       (.addAllReducible retval data)
       retval))))

(defn ^:no-doc float-array-v
  ^floats [data]
  (if (instance? IMutList data)
    (.toFloatArray ^IMutList data)
    (do-make-array #(ArrayLists/floatArray %) #(ArrayLists/toList ^floats %)
                   float-array-list data)))


(defmacro float-array
  ([] `(ArrayLists/floatArray 0))
  ([data]
   `(impl-array-macro ~data ArrayLists/floatArray Casts/doubleCast float-array-v)))


(defmacro fvec
  "Create a persistent-vector-compatible list backed by a float array."
  ([] `(ArrayLists/toList (float-array)))
  ([data] `(ArrayLists/toList (float-array ~data))))


(defn double-array-list
  "An array list that is as fast as java.util.ArrayList for add,get, etc but includes
  many accelerated operations such as fill and an accelerated addAll when the src data
  is an array list."
  (^IMutList [] (ArrayLists$DoubleArrayList.))
  (^IMutList [cap-or-data]
   (if (number? cap-or-data)
     (ArrayLists$DoubleArrayList. (int cap-or-data))
     (doto (ArrayLists$DoubleArrayList.)
       (.addAllReducible (->reducible cap-or-data))))))


(def dbl-ary-cls (Class/forName "[D"))


(defn ^:no-doc double-array-v
  ^doubles [data]
  (cond
    (instance? dbl-ary-cls data)
    data
    (instance? IMutList data)
    (.toDoubleArray ^IMutList data)
    :else
    (do-make-array #(ArrayLists/doubleArray %) #(ArrayLists/toList ^doubles %)
                   double-array-list data)))



(defmacro double-array
  ([] `(ArrayLists/doubleArray 0))
  ([data]
   `(impl-array-macro ~data ArrayLists/doubleArray Casts/doubleCast double-array-v)))



(defmacro dvec
  "Create a persistent-vector-compatible list backed by a double array."
  ([] `(ArrayLists/toList (double-array)))
  ([data] `(ArrayLists/toList (double-array ~data))))



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



(defn ^:no-doc ovec-v
  ^ArrayImmutList [data]
  (if (instance? obj-ary-cls data)
    (ArrayImmutList. ^objects data 0 (alength ^objects data) nil)
    (if-let [c (constant-count data)]
      (-> (doto (ArrayLists/toList (ArrayLists/objectArray (int c)))
            (.fillRangeReducible 0 data))
          (persistent!))
      (-> (doto (object-array-list)
            (.addAllReducible data))
          (persistent!)))))


(defmacro ovec
  "Return an immutable persistent vector like object backed by a single object array."
  ([] `ArrayImmutList/EMPTY)
  ([data]
   (cond
     (number? data)
     `(ArrayImmutList. (ArrayLists/objectArray ~data) 0 ~data nil)
     (vector? data)
     `(ArrayImmutList. (obj-ary ~@data) 0 ~(count data) ~(meta data))
     :else
     `(ovec-v ~data))))


(defmacro dnth
  "nth operation returning a primitive double.  Efficient when obj is a double array."
  [obj idx]
  `(TypedNth/dnth ~obj ~idx))


(defmacro lnth
  "nth operation returning a primitive long.  Efficient when obj is a long array."
  [obj idx]
  `(TypedNth/lnth ~obj ~idx))


(defmacro fnth
  "nth operation returning a primitive float.  Efficient when obj is a float array."
  [obj idx]
  `(TypedNth/fnth ~obj ~idx))


(defmacro inth
  "nth operation returning a primitive int.  Efficient when obj is an int array."
  [obj idx]
  `(TypedNth/inth ~obj ~idx))


(defn mapv
  "Produce a persistent vector from a collection."
  ([map-fn coll]
   (if-let [c (constant-count coll)]
     (let [c (int c)
           rv (ArrayLists/objectArray c)]
       (reduce (hamf-rf/indexed-accum
                acc idx v
                (ArrayHelpers/aset rv idx (map-fn v)))
               nil
               coll)
       (ArrayImmutList. rv 0 c nil))
     (let [rv (ArrayLists$ObjectArrayList. (ArrayLists/objectArray 8) 0 nil)
           _ (reduce (fn [acc v] (.conj rv (map-fn v))) rv coll)]
       (persistent! rv))))
  ([map-fn c1 c2]
   (ovec (map map-fn c1 c2)))
  ([map-fn c1 c2 c3]
   (ovec (map map-fn c1 c2 c3)))
  ([map-fn c1 c2 c3 & args]
   (ovec (apply map map-fn c1 c2 c3 args))))


(defn filterv
  "Filter a collection into a vector."
  [pred coll]
  (ovec (filter pred coll)))


(defn sum-fast
  "Fast simple double summation.  Does not do any nan checking or summation
  compensation."
  ^double [coll]
  ;;Using raw reduce call as opposed to reduce-reducer to avoid protocol dispatch for small N
  @(reduce double-consumer-accumulator (Sum$SimpleSum.) coll))



(defn ^:no-doc apply-nan-strategy
  [options coll]
  (case (get options :nan-strategy :remove)
    :remove (filter (hamf-fn/double-predicate v (not (Double/isNaN v))) coll)
    :keep coll
    :exception (map (hamf-fn/double-unary-operator v
                                                   (when (Double/isNaN v)
                                                     (throw (Exception. "Nan detected")))
                                                   v)
                    coll)))


(defn sum-stable-nelems
  "Stable sum returning map of {:sum :n-elems}. See options for [[sum]]."
  ([coll] (sum-stable-nelems nil coll))
  ([options coll]
   (->> (->reducible coll)
        (apply-nan-strategy options)
        (preduce-reducer (Sum.) options))))


(defn sum
  "Very stable high performance summation.  Uses both threading and kahans compensated
  summation.

  Options:

  * `nan-strategy` - defaults to `:remove`.  Options are `:keep`, `:remove` and
  `:exception`."
  (^double [coll] (sum nil coll))
  (^double [options coll]
   (get (sum-stable-nelems options coll) :sum)))


(defn mean
  "Return the mean of the collection.  Returns double/NaN for empty collections.
  See options for [[sum]]."
  (^double [coll] (mean nil coll))
  (^double [options coll]
   (let [vals (sum-stable-nelems options coll)]
     (/ (double (vals :sum))
        (long (vals :n-elems))))))


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


(defn- key? [e] (when e (key e)))


(deftype ^:private MaxKeyReducer [^{:unsynchronized-mutable true
                                    :tag long} data
                                  ^:unsynchronized-mutable value
                                  ^IFn$OL mapper]
  Consumer
  (accept [this v]
    (let [mval (.invokePrim mapper v)]
      (when (>= mval data)
        (set! data mval)
        (set! value v))))
  clojure.lang.IDeref
  (deref [this] value)
  Reducible
  (reduce [this o]
    (cond
      (== data Long/MIN_VALUE)
      o
      (== (.-data ^MaxKeyReducer o) Long/MIN_VALUE)
      this
      :else
      (do
        (.accept this (deref o))
        this))))

(defn- ensure-obj->long ^IFn$OL [f] (if (instance? IFn$OL f) f (obj->long v (f v))))

(defn mmax-key
  "Faster and nil-safe version of #(apply max-key %1 %2)"
  [f data]
  (let [f (ensure-obj->long f)]
    @(reduce hamf-rf/consumer-accumulator (MaxKeyReducer. Long/MIN_VALUE nil f) data)))

(deftype ^:private MinKeyReducer [^{:unsynchronized-mutable true
                                    :tag long} data
                                  ^:unsynchronized-mutable value
                                  ^IFn$OL mapper]
  Consumer
  (accept [this v]
    (let [mval (.invokePrim mapper v)]
      (when (<= mval data)
        (set! data mval)
        (set! value v))))
  clojure.lang.IDeref
  (deref [this] value)
  Reducible
  (reduce [this o]
    (cond
      (== data Long/MAX_VALUE)
      o
      (== (.-data ^MinKeyReducer o) Long/MAX_VALUE)
      this
      :else
      (do
        (.accept this (deref o))
        this))))


(defn mmin-key
  "Faster and nil-safe version of #(apply min-key %1 %2)"
  [f data]
  (let [f (ensure-obj->long f)]
    @(reduce hamf-rf/consumer-accumulator (MinKeyReducer. Long/MAX_VALUE nil f) data)))


(deftype ^:private MaxIdx [^{:unsynchronized-mutable true
                             :tag long} data
                           ^{:unsynchronized-mutable true
                             :tag long} idx
                           ^{:unsynchronized-mutable true
                             :tag long} value
                           ^IFn$OL mapper]
  Consumer
  (accept [this v]
    (let [mval (.invokePrim mapper v)]
      (when (>= mval data)
        (set! data mval)
        (set! value idx))
      (set! idx (unchecked-inc idx))))
  clojure.lang.IDeref
  (deref [this] value))


(defn mmax-idx
  "Like [[mmin-key]] but returns the max index.  F should be a function from obj->long."
  ^long [f data]
  @(reduce hamf-rf/consumer-accumulator (MaxIdx. Long/MIN_VALUE 0 -1 (ensure-obj->long f))
           data))


(deftype ^:private MinIdx [^{:unsynchronized-mutable true
                             :tag long} data
                           ^{:unsynchronized-mutable true
                             :tag long} idx
                           ^{:unsynchronized-mutable true
                             :tag long} value
                           ^IFn$OL mapper]
  Consumer
  (accept [this v]
    (let [mval (.invokePrim mapper v)]
      (when (<= mval data)
        (set! data mval)
        (set! value idx))
      (set! idx (unchecked-inc idx))))
  clojure.lang.IDeref
  (deref [this] value))


(defn mmin-idx
  "Like [[mmin-key]] but returns the min index.  F should be a function from obj->long."
  ^long [f data]
  @(reduce hamf-rf/consumer-accumulator (MinIdx. Long/MAX_VALUE 0 -1 (ensure-obj->long f))
           data))


(defn intersect-sets
  "Given a sequence of sets, efficiently perform the intersection of them.  This algorithm is usually faster and has a more stable
   runtime than (reduce clojure.set/intersection sets) which degrades depending on the order of the sets and the pairwise
   intersection of the initial sets."
  [sets]
  (let [sets (vec sets)
        ns (count sets)]
    (case ns
      0 #{}
      1 (sets 0)
      (let [min-idx (mmin-idx (fn ^long [arg] (.size ^Set arg)) sets)]
        (-> (reduce (fn [^Set rv ^long idx]
                      (cond
                        (.isEmpty rv) (reduced rv)
                        (== idx min-idx) rv
                        :else (intersection rv (sets idx))))
                    (transient (sets min-idx))
                    (range ns))
            (persistent!))))))



(defn mode
  "Return the most common occurance in the data."
  [data]
  (->> (frequencies {:map-fn java-hashmap} data)
       (mmax-key val)
       (key?)))


(defn rest
  "Version of rest that does uses subvec if collection is random access.  This preserves the
  ability to reduce in parallel over the collection."
  [coll]
  (cond
    (nil? coll) nil
    (instance? RandomAccess coll)
    (if (pos? (count coll))
      (subvec coll 1)
      [])
    :else (clojure.core/rest coll)))


(defn reverse
  "Reverse a collection or sequence.  Constant time reverse is provided
  for any random access list."
  [coll]
  (if (instance? Comparator coll)
    (.reversed ^Comparator coll)
    (let [coll (->reducible coll)]
      (cond
        (instance? IMutList coll)
        (.reverse ^IMutList coll)
        (instance? RandomAccess coll)
        (ReverseList/create coll (meta coll))
        :else
        (clojure.core/reverse coll)))))


(defn take
  "Take the first N values from a collection.  If the input is
  random access, the result will be random access."
  ([n] (clojure.core/take n))
  ([n coll]
   (when coll
     (let [coll (->reducible coll)]
       (if (instance? RandomAccess coll)
         (.subList ^List coll 0 (min (long n) (.size ^List coll)))
         (clojure.core/take n coll))))))


(defn take-last
  "Take the last N values of the collection.  If the input is random-access,
  the result will be random-access."
  ([n] (clojure.core/take-last n))
  ([n coll]
    (when coll
      (let [coll (->reducible coll)]
        (if (instance? RandomAccess coll)
          (let [ne (.size ^List coll)
                n (long n)]
            (.subList ^List coll (- ne n 1) ne))
          (clojure.core/take-last n coll))))))


(defn- priority-queue-rf
  [^long n comp]
  (let [comp (when comp (.reversed (->comparator comp)))]
    (fn
      ([] (if comp
            (PriorityQueue. n comp)
            (PriorityQueue. n)))
      ([acc] (->random-access (.toArray ^Collection acc)))
      ([acc v]
       (let [^PriorityQueue acc acc]
         (.offer acc v)
         (when (> (.size acc) n) (.poll acc)))
       acc))))


(defn take-min
  "Take the min n values of a collection.  This is not an order-preserving operation."
  ([n comp values]
   (reduce-reducer (priority-queue-rf n comp) values))
  ([n values]
   (take-min n < values)))


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
  ([n] (clojure.core/drop-last n))
  ([n coll]
   (when coll
     (let [coll (->reducible coll)]
       (if (instance? RandomAccess coll)
         (let [ne (.size ^List coll)
               n (min (long n) ne)]
           (.subList ^List coll 0 (- ne n)))
         (clojure.core/take-last n coll))))))


(defn repeat
  "When called with no arguments, produce an infinite sequence of v.
  When called with 2 arguments, produce a random access list that produces v at each
  index."
  ([v] (clojure.core/repeat v))
  (^List [n v] (ConstList/create n v nil)))


(defn ^:no-doc double-eq
  [lhs rhs]
  (if (number? lhs)
    (let [lhs (double lhs)
          rhs (double rhs)]
      (cond
        (and (Double/isNaN lhs)
             (Double/isNaN rhs))
        true
        (or (Double/isNaN lhs)
            (Double/isNaN rhs))
        false
        :else
        (== lhs rhs)))
    (and (== (count lhs) (count rhs))
         (every? identity (map double-eq lhs rhs)))))


(defmacro reduced->
  "Helper macro to implement reduce chains checking for if the accumulator
  is reduced before calling the next expression in data.

```clojure
(defrecord YMC [year-month ^long count]
  clojure.lang.IReduceInit
  (reduce [this rfn init]
    (let [init (reduced-> rfn init
                   (clojure.lang.MapEntry/create :year-month year-month)
                   (clojure.lang.MapEntry/create :count count))]
      (if (and __extmap (not (reduced? init)))
        (reduce rfn init __extmap)
        init))))
```"
  [rfn acc & data]
  (reduce (fn [expr next-val]
            `(let [val# ~expr]
               (if (reduced? val#)
                 val#
                 (~rfn val# ~next-val))))
          acc
          data))


(defmacro custom-ireduce
  "Custom implementation of IReduceInit and nothing else.  This can be the most efficient
  way to pass data to other interfaces.  Also see [[custom-counted-ireduce]] if the object
  should also implement ICounted.  See [[reduced->]] for implementation helper."
  [rfn acc & code]
  `(reify ITypedReduce
     (reduce [this# ~rfn ~acc]
       ~@code)))


(defmacro custom-counted-ireduce
  "Custom implementation of IReduceInit and nothing else.  This can be the most efficient
  way to pass data to other interfaces.  Also see custom-ireduce if the object
  does not need to be counted and see [[reduced->]] for implementation helper."
  [n-elems rfn acc & code]
  `(reify
     Counted
     (count [this] (unchecked-int ~n-elems))
     ITypedReduce
     (reduce [this# ~rfn ~acc]
       ~@code)))


(defn wrap-array
  "Wrap an array with an implementation of IMutList"
  ^IMutList [ary] (alists/wrap-array ary))


(defn wrap-array-growable
  "Wrap an array with an implementation of IMutList that supports add and addAllReducible.
  'ptr is the numeric put ptr, defaults to the array length.  Pass in zero for a preallocated
  but empty growable wrapper."
  (^IMutList [ary ptr]
   (alists/wrap-array-growable ary ptr))
  (^IMutList [ary]
   (alists/wrap-array-growable ary)))


(defn inc-consumer
  "Return a consumer that simply increments a long.  See java/ham_fisted/Consumers.java for definition."
  (^Consumers$IncConsumer [] (Consumers$IncConsumer.))
  (^Consumers$IncConsumer [^long init-value] (Consumers$IncConsumer. init-value)))

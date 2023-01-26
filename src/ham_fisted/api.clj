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
            [ham-fisted.alists :as alists]
            [ham-fisted.impl :as impl]
            [ham-fisted.protocols :as protocols])
  (:import [ham_fisted MutArrayMap MutHashTable LongMutHashTable MutBitmapTrie HashSet
            PersistentHashSet HashTable LongHashTable ArrayLists$ArrayOwner
            BitmapTrieCommon$HashProvider BitmapTrieCommon BitmapTrieCommon$MapSet
            BitmapTrieCommon$Box ObjArray ImmutValues UpdateValues
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
            Reductions$IndexedAccum MutHashTable ImmutHashTable MutableMap
            ImmutArrayMap MapForward MapBase]
           [ham_fisted.alists ByteArrayList ShortArrayList CharArrayList FloatArrayList
            BooleanArrayList]
           [clojure.lang ITransientAssociative2 ITransientCollection Indexed
            IEditableCollection RT IPersistentMap Associative Util IFn ArraySeq
            Reversible IReduce IReduceInit IFn$DD IFn$DL IFn$DO IFn$LD IFn$LL IFn$LO
            IFn$OD IFn$OL IFn$OLO IFn$ODO IObj Util IReduceInit Seqable IteratorSeq
            ITransientMap Counted]
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
           [java.util.stream IntStream DoubleStream])
  (:refer-clojure :exclude [assoc! conj! frequencies merge merge-with memoize
                            into assoc-in get-in update assoc update-in hash-map
                            group-by subvec group-by mapv vec vector object-array
                            sort int-array long-array double-array float-array
                            range map concat filter filterv first last pmap take take-last drop
                            drop-last sort-by repeat repeatedly shuffle into-array
                            empty? reverse byte-array short-array char-array boolean-array
                            keys vals persistent! rest]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(declare assoc! conj! vec mapv vector object-array range first take drop into-array shuffle
         object-array-list int-array-list long-array-list double-array-list
         int-array argsort byte-array short-array char-array boolean-array repeat
         persistent! rest immut-map keys vals group-by-reduce consumer-accumulator
         reducible-merge reindex long-consumer-reducer group-by-consumer
         double-consumer-reducer consumer-reducer merge constant-count mutable-map?)


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
as doubles, floats, and BigDecimals. This was the default hash provider."}
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


(def ^{:tag ImmutArrayMap
       :doc "Constant persistent empty map"} empty-map (.persistent (MutArrayMap. default-hash-provider)))
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
     (with-meta (.persistent (MutArrayMap/createKV default-hash-provider a b))
       (meta m))
     (.assoc ^Associative m a b)))
  ([m a b c d]
   (if (empty-map? m)
     (with-meta (.persistent (MutArrayMap/createKV default-hash-provider a b c d))
       (meta m))
     (-> (assoc! (transient (or m empty-map)) a b)
         (assoc! c d)
         (persistent!))))
  ([m a b c d e f]
   (if (empty-map? m)
     (with-meta (.persistent (MutArrayMap/createKV  default-hash-provider a b c d e f))
       (meta m))
     (-> (transient (or m empty-map))
         (assoc! a b)
         (assoc! c d)
         (assoc! e f)
         (persistent!))))
  ([m a b c d e f g h]
   (if (empty-map? m)
     (with-meta (.persistent (MutArrayMap/createKV default-hash-provider a b c d e f g h))
       (meta m))
     (-> (transient (or m empty-map))
         (assoc! a b)
         (assoc! c d)
         (assoc! e f)
         (assoc! g h)
         (persistent!))))
  ([m a b c d e f g h & args]
   (when-not (== 0 (rem (count args) 2))
     (throw (Exception. "Assoc takes an odd number of arguments.")))
   (if (empty-map? m)
     (let [m (MutHashTable. default-hash-provider
                            ^IPersistentMap (meta m)
                            (+ 4 (quot (count args) 2)))]
       (.put m a b)
       (.put m c d)
       (.put m e f)
       (.put m g h)
       (loop [args args]
         (if args
           (let [k (RT/first args)
                 args (RT/next args)
                 v (RT/first args)]
             (.put m k v)
             (recur (RT/next args)))
           (persistent! m))))
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


(defn- unpack-reduced
  [item]
  (if (reduced? item)
    (deref item)
    item))


(defn ^:no-doc options->parallel-options
  [options]
  (let [^Map options (or options {})
        ^ForkJoinPool pool (.getOrDefault options :pool (ForkJoinPool/commonPool))]
    (ParallelOptions. (.getOrDefault options :min-n 1000)
                      (.getOrDefault options :max-batch-size 64000)
                      (boolean (.getOrDefault options :ordered? true))
                      pool
                      (.getOrDefault options :parallelism (.getParallelism pool))
                      (case (.getOrDefault options :cat-parallelism :seq-wise)
                        :seq-wise ParallelOptions$CatParallelism/SEQWISE
                        :elem-wise ParallelOptions$CatParallelism/ELEMWISE)
                      (.getOrDefault options :put-timeout-ms 5000)
                      (.getOrDefault options :unmerged-result? false))))


(defn preduce
  "Parallelized reduction.  Currently coll must either be random access or a lznc map/filter
  chain based on one or more random access entities, hashmaps and sets from this library or
  any java.util set, hashmap or concurrent versions of these.  If input cannot be
  parallelized this lowers to a normal serial reduction.

  * `init-val-fn` - Potentially called in reduction threads to produce each initial value.
  * `rfn` - normal clojure reduction function.  Typehinting the second argument to double
     or long will sometimes produce a faster reduction.
  * `merge-fn` - Merge two reduction results into one.

  Options:
  * `:pool` - The fork-join pool to use.  Defaults to common pool which assumes reduction is
     cpu-bound.
  * `:parallelism` - What parallelism to use - defaults to pool's `getParallelism` method.
  * `:max-batch-size` - Rough maximum batch size for indexed or grouped reductions.  This
     can both even out batch times and ensure you don't get into safepoint trouble with
     jdk-8.
  * `:min-n` - minimum number of elements before initiating a parallelized reduction -
     Defaults to 1000 but you should customize this particular to your specific reduction.
  * `:ordered?` - True if results should be in order.  Unordered results sometimes are
    slightly faster but again you should test for your specific situation..
  * `:cat-parallelism` - Either `:seq-wise` or `:elem-wise`, defaults to `:seq-wise`.
     Test for your specific situation, this really is data-dependent. This contols how a
     concat primitive parallelizes the reduction across its contains.  Elemwise means each
     container's reduction is individually parallelized while seqwise indicates to do a
     pmap style initial reduction across containers then merge the results.
  * `:put-timeout-ms` - Number of milliseconds to wait for queue space before throwing
     an exception in unordered reductions.  Defaults to 50000.
  * `:unmerged-result?` - Defaults to false.  When true, the sequence of results
     be returned directly without any merge steps in a lazy-noncaching container.  Beware
     the noncaching aspect -- repeatedly evaluating this result may kick off the parallelized
     reduction multiple times.  To ensure caching if unsure call `seq` on the result ...)."
  ([init-val-fn rfn merge-fn coll] (preduce init-val-fn rfn merge-fn nil coll))
  ([init-val-fn rfn merge-fn options coll]
   (unpack-reduced
    (Reductions/parallelReduction init-val-fn rfn merge-fn (->reducible coll)
                                  (options->parallel-options options)))))


(defn preduce-reducer
  "Given an instance of [[ham-fisted.protocols/ParallelReducer]], perform a parallel
  reduction.

  In the case where the result is requested unmerged then finalize will
  be called on each result in a lazy noncaching way.  In this case you can use a
  non-parallelized reducer and simply get a sequence of results as opposed to one.

  * reducer - instance of ParallelReducer
  * options - Same options as preduce.
  * coll - something potentially with a parallelizable reduction.

  See options for [[preduce]].

  Additional Options:

  `:skip-finalize?` - when true, the reducer's finalize method is not called on the result."
  ([reducer options coll]
   (let [retval (preduce (protocols/->init-val-fn reducer)
                      (protocols/->rfn reducer)
                      (protocols/->merge-fn reducer)
                      options
                      coll)]
     (if (get options :skip-finalize?)
       retval
       (if (get options :unmerged-result?)
         (lznc/map #(protocols/finalize reducer %) retval)
         (protocols/finalize reducer retval)))))
  ([reducer coll]
   (preduce-reducer reducer nil coll)))


(defmacro double-accumulator
  "Type-hinted double reduction accumulator.
  consumer:

```clojure
  ham-fisted.api> (reduce (double-accumulator acc v (+ (double acc) v))
                             0.0
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  [accvar varvar & code]
  `(reify IFnDef$ODO
     (invokePrim [this ~accvar ~varvar]
       ~@code)))


(defmacro long-accumulator
  "Type-hinted double reduction accumulator.
  consumer:

```clojure
  ham-fisted.api> (reduce (double-accumulator acc v (+ (double acc) v))
                             0.0
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  [accvar varvar & code]
  `(reify IFnDef$OLO
     (invokePrim [this ~accvar ~varvar]
       ~@code)))


(defn compose-reducers
  "Given a map or sequence of reducers return a new reducer that produces a map or
  vector of results.

  If data is a sequence then context is guaranteed to be an object array.

  Options:

  * `:rfn-datatype` - One of nil, :int64, or :float64.  This indicates that the rfn's
  should all be uniform as accepting longs, doubles, or generically objects.  Defaults
  to nil."
  ([reducers] (compose-reducers nil reducers))
  ([options reducers]
   (if (instance? Map reducers)
     (let [reducer (compose-reducers (vals reducers))]
       (reify
         protocols/Reducer
         (->init-val-fn [_] (protocols/->init-val-fn reducer))
         (->rfn [_] (protocols/->rfn reducer))
         protocols/Finalize
         (finalize [_ v] (immut-map (map vector (keys reducers)
                                         (protocols/finalize reducer v))))
         protocols/ParallelReducer
         (->merge-fn [_] (protocols/->merge-fn reducer))))
     (let [init-fns (mapv protocols/->init-val-fn reducers)
           rfn-dt (get options :rfn-datatype)
           ^objects rfns (object-array
                          (case rfn-dt
                            :int64
                            (->> (map protocols/->rfn reducers)
                                 (map #(Transformables/toLongReductionFn %)))
                            :float64
                            (->> (map protocols/->rfn reducers)
                                 (map #(Transformables/toDoubleReductionFn %)))
                            ;;else branch
                            (map protocols/->rfn reducers)))
           ^objects mergefns (object-array (map protocols/->merge-fn reducers))
           n-vals (count rfns)
           init-fn-map (map #(%) init-fns)]
       (reify
         protocols/Reducer
         (->init-val-fn [_] (fn compose-init [] (object-array init-fn-map)))
         (->rfn [_]
           (case rfn-dt
             :int64 (Reductions/longCompose n-vals rfns)
             :float64 (Reductions/doubleCompose n-vals rfns)
             (Reductions/objCompose n-vals rfns)))
         protocols/Finalize
         (finalize [_ v] (mapv #(protocols/finalize %1 %2) reducers v))
         protocols/ParallelReducer
         (->merge-fn [_] (Reductions/mergeCompose n-vals, mergefns)))))))


(defn preduce-reducers
  "Given a map or sequence of [[ham-fisted.protocols/ParallelReducer]], produce a map or
  sequence of reduced values. Reduces over input coll once in parallel if coll is large
  enough.  See options for [[preduce]].

```clojure
ham-fisted.api> (preduce-reducers {:sum (Sum.) :mult *} (range 20))
{:mult 0, :sum #<Sum@5082c3b7: {:sum 190.0, :n-elems 20}>}
```"
  ([reducers options coll]
   (preduce-reducer (compose-reducers reducers) options coll))
  ([reducers coll] (preduce-reducers reducers nil coll)))


(defn reducer-xform->reducer
  "Given a reducer and a transducer xform produce a new reducer which will apply
  the transducer pipeline before is reduction function.

```clojure
ham-fisted.api> (reduce-reducer (reducer-xform->reducer (Sum.) (clojure.core/filter even?))
                                (range 1000))
#<Sum@479456: {:sum 249500.0, :n-elems 500}>
```
  !! - If you use a stateful transducer here then you must *not* use the reducer in a
  parallelized reduction."
  [reducer xform]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)
        xfn (xform (fn
                     ([] (init-val-fn))
                     ([v] (protocols/finalize reducer v))
                     ([acc v] (rfn acc v))))]
    (reify
      protocols/Reducer
      (->init-val-fn [this] init-val-fn)
      (->rfn [this] xfn)
      protocols/Finalize
      (finalize [this v] (xfn v))
      protocols/ParallelReducer
      (->merge-fn [this] (protocols/->merge-fn reducer)))))


(defn reducer->rf
  "Given a reducer, return a transduce-compatible rf -

```clojure
ham-fisted.api> (transduce (clojure.core/map #(+ % 2)) (reducer->rf (Sum.)) (range 200))
{:sum 20300.0, :n-elems 200}
```"
  [reducer]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)]
    (fn
      ([] (init-val-fn))
      ([acc v] (rfn acc v))
      ([v] (protocols/finalize reducer v)))))


(defn reducer->completef
  "Return fold-compatible pair of [reducef, completef] given a parallel reducer.
  Note that folded reducers are not finalized as of this time:

```clojure
ham-fisted.api> (def data (vec (range 200000)))
#'ham-fisted.api/data
ham-fisted.api> (r/fold (reducer->completef (Sum.)) (reducer->rfn (Sum.)) data)
#<Sum@858c206: {:sum 1.99999E10, :n-elems 200000}>
```"
  [reducer]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)
        merge-fn (protocols/->merge-fn reducer)]
    (fn
      ([] (init-val-fn))
      ([l r] (merge-fn l r))
      ([v] (protocols/finalize reducer v)))))


(defn reducer-with-finalize
  [reducer fin-fn]
  (reify
    protocols/Reducer
    (->init-val-fn [r] (protocols/->init-val-fn reducer))
    (->rfn [r] (protocols/->rfn reducer))
    protocols/Finalize
    (finalize [r v] (fin-fn v))
    protocols/ParallelReducer
    (->merge-fn [r] (protocols/->merge-fn reducer))))


(defn reduce-reducer
  "Serially reduce a reducer.

```clojure
ham-fisted.api> (reduce-reducer (Sum.) (range 1000))
#<Sum@afbedb: {:sum 499500.0, :n-elems 1000}>
```"
  [reducer coll]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)]
    (->> (reduce rfn (init-val-fn) coll)
         (protocols/finalize reducer))))


(defn reduce-reducers
  "Serially reduce a map or sequence of reducers into a map or sequence of results.

```clojure
ham-fisted.api> (reduce-reducers {:a (Sum.) :b *} (range 1 21))
{:b 2432902008176640000, :a #<Sum@6bcebeb1: {:sum 210.0, :n-elems 20}>}
```"
  [reducers coll]
  (reduce-reducer (compose-reducers reducers) coll))


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
    (reduce-reducer rf data)))


(defn mut-hashtable-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  the various members of the java.util.Map interface such as put, compute, computeIfAbsent,
  replaceAll and merge.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^MutHashTable [] (MutHashTable. default-hash-provider))
  (^MutHashTable [data] (mut-hashtable-map nil nil data))
  (^MutHashTable [xform data] (mut-hashtable-map xform nil data))
  (^MutHashTable [xform options data]
   (cond
     (number? data)
     (MutHashTable. (options->provider options) nil (int data))
     (and (nil? xform) (instance? obj-ary-cls data))
     (MutHashTable/create (options->provider options) true ^objects data)
     :else
     (tduce xform (transient-map-rf #(MutHashTable. (options->provider nil)
                                                    nil
                                                    (get options :init-size 0)))
            data))))


(defn mut-long-hashtable-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  the various members of the java.util.Map interface such as put, compute, computeIfAbsent,
  replaceAll and merge.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^LongMutHashTable [] (LongMutHashTable.))
  (^LongMutHashTable [data] (mut-long-hashtable-map nil nil data))
  (^LongMutHashTable [xform data] (mut-long-hashtable-map xform nil data))
  (^LongMutHashTable [xform options data]
   (cond
     (number? data)
     (LongMutHashTable. nil (int data))
     (and (nil? xform) (instance? obj-ary-cls data))
     (LongMutHashTable/create true ^objects data)
     :else
     (tduce xform (transient-map-rf #(LongMutHashTable.)) data))))


(defn mut-trie-map
  "Create a mutable implementation of java.util.Map.  This object efficiently implements
  ITransient map so you can use assoc! and persistent! on it but you can additionally use
  operations such as put!, remove!, compute-at! and compute-if-absent!.  You can create
  a persistent hashmap via the clojure `persistent!` call.

  If data is an object array it is treated as a flat key-value list which is distinctly
  different than how conj! treats object arrays.  You have been warned.

  Options:

  * `:hash-provider` - An implementation of `BitmapTrieCommon$HashProvider`.  Defaults to
  the [[default-hash-provider]]."
  (^MutBitmapTrie [] (MutBitmapTrie. (options->provider nil)))
  (^MutBitmapTrie [data] (mut-trie-map nil nil data))
  (^MutBitmapTrie [xform data] (mut-trie-map xform nil data))
  (^MutBitmapTrie [xform options data]
   (cond
     (number? data)
     (MutBitmapTrie. (options->provider nil))
     (and (nil? xform) (instance? obj-ary-cls data))
     (MutBitmapTrie/create (options->provider options) true ^objects data)
     :else
     (tduce xform
            (transient-map-rf #(MutBitmapTrie. (options->provider nil)))
            data))))


(defn mut-map
  (^Map [] (mut-hashtable-map))
  (^Map [data] (mut-hashtable-map data))
  (^Map [xform data] (mut-hashtable-map xform data))
  (^Map [xform options data] (mut-hashtable-map xform options data)))


(defn ^:no-doc map-data->obj-ary
  ^objects [data]
  (if (instance? obj-ary-cls data)
    data
    (-> (reduce (fn [^List l d]
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
  (MutArrayMap/create (options->provider options) false (map-data->obj-ary data)))


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
  (^ImmutHashTable [] empty-map)
  (^ImmutHashTable [data]
   (immut-map nil data))
  (^ImmutHashTable [options data]
   (-> (mut-map options data)
       (persistent!))))


(defn hash-map
  "Drop-in replacement to Clojure's hash-map function."
  ([] empty-map)
  ([a b]
   (persistent! (MutArrayMap/createKV default-hash-provider a b)))
  ([a b c d]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d)))
  ([a b c d e f]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d e f)))
  ([a b c d e f g h]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d e f g h)))
  ([a b c d e f g h i j]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d e f g h i j)))
  ([a b c d e f g h i j k l]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d e f g h i j k l)))
  ([a b c d e f g h i j k l m n]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d e f g h i j k l m n)))
  ([a b c d e f g h i j k l m n o p]
   (persistent! (MutArrayMap/createKV default-hash-provider a b c d e f g h i j k l m n o p)))
  ([a b c d e f g h i j k l m n o p & args]
   (persistent! (MutArrayMap/create default-hash-provider true
                                    (ObjArray/createv a b c d e f g h i j k l m n o p (object-array args))))))


(defn java-hashmap
  "Create a java.util.HashMap.  Duplicate keys are treated as if map was created by assoc."
  (^java.util.HashMap [] (java.util.HashMap.))
  (^java.util.HashMap [data] (java-hashmap nil nil data))
  (^java.util.HashMap [xform data] (java-hashmap xform nil data))
  (^java.util.HashMap [xform options data]
   (if (number? data)
     (java.util.HashMap. (int data))
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
     (instance? Map data)
     (java.util.LinkedHashMap. ^Map data)
     :else
     (tduce nil (mut-map-rf #(java.util.LinkedHashMap.)) data))))


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
  (^HashSet [] (HashSet. (options->provider nil)))
  (^HashSet [data] (into (HashSet. (options->provider nil)) data))
  (^HashSet [options data] (into (HashSet. (options->provider options)) data)))


(defn immut-set
  "Create an immutable hashset based on a hash table.  This object supports conversion
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
    (instance? Set obj)
    (do (.add ^Set obj val) obj)
    (instance? List obj)
    (do (.add ^List obj val) obj)
    :else
    (throw (Exception. "Item cannot be conj!'d"))))


(defn add-all!
  "Add all items from l2 to l1.  l1 is expected to be a java.util.List implementation.
  Returns l1."
  [l1 l2]
  (if (instance? IMutList l1)
    (.addAllReducible ^IMutList l1 l2)
    (.addAll ^List l1 l2))
  l1)


(defmacro bi-function
  "Create an implementation of java.util.function.BiFunction."
  [arg1 arg2 & code]
  `(reify IFnDef$OOO (invoke [this# ~arg1 ~arg2] ~@code)))


(defmacro bi-consumer
  [arg1 arg2 & code]
  `(reify BiConsumer
     (accept [this ~arg1 ~arg2]
       ~@code)))


(defn ->bi-function
  "Convert an object to a java.util.BiFunction. Object can either already be a
  bi-function or an IFn to be invoked with 2 arguments."
  ^BiFunction [cljfn]
  (if (instance? BiFunction cljfn)
    cljfn
    (bi-function a b (cljfn a b))))



(defmacro function
  "Create a java.util.function.Function"
  [arg & code]
  `(reify IFnDef$OO (invoke [this# ~arg] ~@code)))


(defmacro obj->long
  "Create a function that converts objects to longs"
  ([]
   `(reify IFnDef$OL
        (invokePrim [this v#]
          (Casts/longCast v#))))
  ([varname & code]
   `(reify IFnDef$OL
      (invokePrim [this ~varname]
        (Casts/longCast ~@code)))))



(defmacro obj->double
  "Create a function that converts objects to doubles"
  ([]
   `(reify IFnDef$OD
      (invokePrim [this v#]
        (Casts/doubleCast v#))))
  ([varname & code]
   `(reify IFnDef$OD
      (invokePrim [this ~varname]
        (Casts/doubleCast ~@code)))))


(defmacro long->double
  "Create a function that receives a long and returns a double"
  [varname & code]
  `(reify IFnDef$LD
     (invokePrim [this ~varname] ~@code)))


(defmacro double->long
  "Create a function that receives a double and returns a long"
  [varname & code]
  `(reify IFnDef$DL
     (invokePrim [this ~varname] ~@code)))


(defmacro long->obj
  "Create a function that receives a primitive long and returns an object."
  [varname & code]
  `(reify IFnDef$LO
     (invokePrim [this ~varname] ~@code)))


(defmacro double->obj
  [varname & code]
  `(reify IFnDef$DO
     (invokePrim [this ~varname] ~@code)))


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
  hashmaps, the returned implementation of java.util.Set has both more utility and better
  performance than (keys m)."
  ^Set [^Map m] (.keySet m))


(defn map-values
  "Return the values collection of the map.  This may not be in the same order as (keys m)
  or (vals m).  For hamf hashmaps, this does have the same order as (vals m)."
  ^Collection [^Map m] (.values m))


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
  matches the input."
  [bfn map1 map2]
  (cond
    (nil? map1) map2
    (nil? map2) map1
    (identical? map1 map2) map1
    :else
    (let [bfn (->bi-function bfn)]
      (if (and (map-set? map1) (map-set? map2))
        (.union (as-map-set map1) (as-map-set map2) bfn)
        (let [[map1 map2] (if (< (count map1) (count map2))
                            [map2 map1]
                            [map1 map2])
              map1 (if (mutable-map? map1)
                     map1
                     (mut-map map1))]
          (.forEach ^Map map2 (reify BiConsumer
                                (accept [this k v]
                                  (.merge ^Map map1 k v bfn))))
          map1)))))


(defn map-union-java-hashmap
  "Take the union of two maps returning a new map.  See documentation for [map-union].
  Returns a java.util.HashMap."
  ^java.util.HashMap [bfn ^Map lhs ^Map rhs]
  (map-union bfn (java-hashmap lhs) rhs))


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
    (identical? s1 s2) s1
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


(defn mutable-map?
  [m]
  (or (instance? MutableMap m)
      (and (not (instance? IPersistentMap m))
           (not (instance? ITransientMap)))))


(defn union-reduce-maps
  "Do an efficient union reduction across many maps using bfn to update values.
  If the first map is mutable the union is done mutably into the first map and it is
  returned."
  ([bfn maps]
   (let [bfn (->bi-function bfn)]
     (-> (reduce #(map-union bfn %1 %2) maps)
         (persistent!)))))


(defn union-reduce-java-hashmap
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
                                    (.put ^Map retval k v)))))
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
                                      (.put ^Map retval k (.apply bfn v vv)))))))
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
  "Immutably (or mutablye) update all values in the map returning a new map.
  bfn takes 2 arguments, k,v and returns a new v. Returns a new persistent map if input
  is persistent else an updated mutable map.
  If passed a vector, k is the index and v is the value.  Will return a new vector.
  else map is assumed to be convertible to a sequence and this pathway works the same
  as map-indexed."
  [map bfn]
  (let [bfn (->bi-function bfn)]
    (cond
      (instance? UpdateValues map)
      (.updateValues ^UpdateValues map bfn)
      (immut-vals? map)
      (.immutUpdateValues (as-immut-vals map) bfn)
      (instance? IPersistentMap map)
      (-> (reduce (fn [^Map acc kv]
                    (.put acc (key kv) (.apply bfn (key kv) (val kv))))
                  (mut-map)
                   map)
          (persistent!))
      (instance? Map map)
      (do
        ;;Mutable preduce - this is faster than replaceAll for larger maps and same
        ;;speed for most other maps.
        (preduce
         (constantly nil)
         ;;We do not care about the accumulator at all - we mutably replace
         ;;the value of the map entry
         (fn [acc ^Map$Entry e]
           (.setValue e (.apply bfn (.getKey e) (.getValue e))))
         (constantly nil)
         map)
        map)
      (instance? RandomAccess map)
      (mut-list (lznc/map-indexed #(.apply bfn %1 %2) map))
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


(defn pgroups
  "Run y groups across n-elems.   Y is common pool parallelism.

  body-fn gets passed two longs, startidx and endidx.

  Returns a sequence of the results of body-fn applied to each group of indexes.

  Before using this primitive please see if [[preduce]] will work.

  Options:

  * `:pgroup-min` - when provided n-elems must be more than this value for the computation
    to be parallelized.
  * `:batch-size` - max batch size.  Defaults to 64000."
  ([n-elems body-fn options]
   (impl/pgroups n-elems body-fn (options->parallel-options (assoc options :ordered? true))))
  ([n-elems body-fn]
   (pgroups n-elems body-fn nil)))


(defn upgroups
  "Run y groups across n-elems.   Y is common pool parallelism.

  body-fn gets passed two longs, startidx and endidx.

  Returns a sequence of the results of body-fn applied to each group of indexes.

  Before using this primitive please see if [[preduce]] will work.

  Options:

  * `:pgroup-min` - when provided n-elems must be more than this value for the computation
    to be parallelized.
  * `:batch-size` - max batch size.  Defaults to 64000."
  ([n-elems body-fn options]
   (impl/pgroups n-elems body-fn (options->parallel-options (assoc options :ordered? false))))
  ([n-elems body-fn]
   (upgroups n-elems body-fn nil)))


(defn pmap
  "pmap using the commonPool.  This is useful for interacting with other primitives, namely
  [[pgroups]] which are also based on this pool.  This is a change from Clojure's base
  pmap in that it uses the ForkJoinPool/commonPool for parallelism as opposed to the
  agent pool - this makes it compose with pgroups and dtype-next's parallelism system.

    Before using this primitive please see if [[preduce]] will work.

  Is guaranteed to *not* trigger the need for `shutdown-agents`."
  [map-fn & sequences]
  (impl/pmap (ParallelOptions. 0 64000 true) map-fn sequences))


(defn upmap
  "Unordered pmap using the commonPool.  This is useful for interacting with other
  primitives, namely [[pgroups]] which are also based on this pool.

  Before using this primitive please see if [[preduce]] will work.

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


(defn ^:no-doc frequencies-gbr-inc
  "Faster implementation of clojure.core/frequencies."
  [coll]
  (group-by-reduce identity (constantly 0)
                   (fn [acc v]
                     (unchecked-inc (long acc)))
                   +
                   coll))


(defn ^:no-doc frequencies-gbr-consumer
  "Faster implementation of clojure.core/frequencies."
  [coll]
  (-> (group-by-reduce identity #(Consumers$IncConsumer.)
                       consumer-accumulator
                       reducible-merge
                       coll)
      (update-values (bi-function k v (deref v)))))


(defn mut-map-union!
  "Very fast union that may simply update lhs and return it.  Both lhs and rhs *must* be
  mutable maps.  See docs for [[map-union]]."
  [merge-bifn ^Map l ^Map r]
  (cond
    (identical? l r) l
    (map-set? l) (map-union merge-bifn l r)
    :else
    (let [[^Map minm ^Map maxm] (if (< (.size l) (.size r))
                                  [l r]
                                  [r l])
          merge-bifn (->bi-function merge-bifn)]
      (.forEach minm (bi-consumer
                      k v
                      (.merge maxm k v merge-bifn)))
      maxm)))


(defn frequencies
  "Faster implementation of clojure.core/frequencies."
  ([coll] (frequencies nil coll))
  ([options coll]
   (-> (preduce (get options :map-fn mut-map)
                (fn [^Map l v]
                  (.compute l v BitmapTrieCommon/incBiFn)
                  l)
                #(mut-map-union! BitmapTrieCommon/incBiFn %1 %2)
                (merge {:min-n 1000 :ordered? true} options)
                coll)
       (persistent!))))


(defn ^:no-doc inc-consumer
  "Return a consumer which increments a long counter.  Consumer ignores
  its input.  Deref the consumer to get the value of the counter."
  ^Consumer [] #(Consumers$IncConsumer.))


(def ^:no-doc inc-consumer-reducer
  (reify
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/Reducer
    (->init-val-fn [this] #(Consumers$IncConsumer.))
    (->rfn [this] consumer-accumulator)
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))



(defn ^:no-doc frequencies-gbc
  ([options coll]
   (group-by-consumer nil inc-consumer-reducer
                      (merge {:map-fn #(MapForward. (LinkedHashMap.) nil)
                              :ordered? true}
                             options)
                      coll))
  ([coll] (frequencies-gbc nil coll)))


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
                                  (eviction-fn args (.-obj ^BitmapTrieCommon$Box v)
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
                     (BitmapTrieCommon$Box. (apply memo-fn args)))))]
     (-> (fn
           ([] (.obj ^BitmapTrieCommon$Box (.get cache [])))
           ([a] (.obj ^BitmapTrieCommon$Box (.get cache [a])))
           ([a b] (.obj ^BitmapTrieCommon$Box (.get cache [a b])))
           ([a b c] (.obj ^BitmapTrieCommon$Box (.get cache [a b c])))
           ([a b c & args] (let [^IMutList obj-ary (mut-list)]
                             (.add obj-ary a)
                             (.add obj-ary b)
                             (.add obj-ary c)
                             (.addAllReducible obj-ary args)
                             (.obj ^BitmapTrieCommon$Box (.get cache (persistent! obj-ary))))))
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
                      (mapcat (fn [^long argidx]
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
                      (mapcat (fn [^long argidx]
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
       in order *and* the result values will be reduced in order.

  Beware that nil keys are not allowed in any java.util-based map."
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
  ([^IMutList retval v1 v2]
   (let [retval (mut-list)]
     (.addAllReducible retval (->reducible v1))
     (.addAllReducible retval (->reducible v2))
     retval))
  ([^IMutList retval v1 v2 args]
   (when-not (nil? v1) (.addAllReducible retval (->reducible v1)))
   (when-not (nil? v2) (.addAllReducible retval (->reducible v2)))
   (reduce (fn [data c]
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
  [lhsvar rhsvar & code]
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
      (Util/compare r l))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (Double/compare r l))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare r l))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(def ^{:doc "A comparator that sorts null, NAN first, natural order"}
  comp-nan-first
  (reify
    Comparator
    (^int compare [this ^Object l ^Object r]
     (cond
       (nil? l) -1
       (nil? r) 1
       :else (Util/compare l r)))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (cond
       (Double/isNaN l) -1
       (Double/isNaN r) 1
       :else
       (Double/compare l r)))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare l r))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(def ^{:doc "A comparator that sorts null, NAN last, natural order"}
  comp-nan-last
  (reify
    Comparator
    (^int compare [this ^Object l ^Object r]
     (cond
       (nil? l) 1
       (nil? r) -1
       :else (clojure.lang.Util/compare l r)))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (cond
       (Double/isNaN l) 1
       (Double/isNaN r) -1
       :else
       (Double/compare l r)))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare l r))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(defn sorta
  "Sort returning an object array."
  (^objects [coll] (sorta comp-nan-last coll))
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
  ([coll] (sort comp-nan-last coll))
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
         data (map keyfn coll)
         ;;Arraylists are faster to create because they do not have to be sized exactly
         ;;to the collection.  They have very fast addAllReducible pathways that specialize
         ;;for constant sized containers.
         data (case (type-single-arg-ifn keyfn)
                :float64
                (double-array-list data)
                :int64
                (long-array-list data)
                (object-array-list data))
         indexes (argsort comp data)]
     (reindex coll indexes))))


(defn shuffle
  "shuffle values returning random access container.

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
   (argsort comp-nan-last coll)))


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
    `(let [~'ary (~ctor ~(count data))]
       (do
         ~@(->> (range (count data))
                (map (fn [^long idx]
                       `(ArrayHelpers/aset ~'ary ~idx (~elem-cast ~(data idx))))))
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


(defn ^:no-doc int-array-v
  ^ints [data]
  (if (instance? IMutList data)
    (.toIntArray ^IMutList data)
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

(defn ^:no-doc long-array-v
  ^longs [data]
  (if (instance? IMutList data)
    (.toLongArray ^IMutList data)
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


(defn ^:no-doc double-array-v
  ^doubles [data]
  (if (instance? IMutList data)
    (.toDoubleArray ^IMutList data)
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


(defmacro indexed-accum
  "Create an indexed accumulator that recieves and additional long index
  during a reduction:

```clojure
ham-fisted.api> (reduce (indexed-accum
                         acc idx v (conj acc [idx v]))
                        []
                        (range 5))
[[0 0] [1 1] [2 2] [3 3] [4 4]]
```"
  [accvar idxvar varvar & code]
  `(Reductions$IndexedAccum.
    (reify IFnDef$OLOO
      (invokePrim [this# ~accvar ~idxvar ~varvar]
        ~@code))))



(defn ^:no-doc ovec-v
  ^ArrayImmutList [data]
  (if (instance? obj-ary-cls data)
    (ArrayImmutList. ^objects data 0 (alength ^objects data) nil)
    (if-let [c (constant-count data)]
      (let [rv (ArrayLists/objectArray (int c))]
        (.fillRangeReducible (ArrayLists/toList rv) 0 data)
        (ArrayImmutList. rv 0 c (meta data)))
      (let [ol (object-array-list data)
            as (.getArraySection ^ArrayLists$ArrayOwner ol)
            ^objects ary (.-array as)]
        (ArrayImmutList. ary 0 (.size as) (meta data))))))


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


(defn mapv
  "Produce a persistent vector from a collection."
  ([map-fn coll]
   (if-let [c (constant-count coll)]
     (let [c (int c)
           rv (ArrayLists/objectArray c)]
       (reduce (indexed-accum
                acc idx v
                (ArrayHelpers/aset rv idx (map-fn v)))
               nil
               coll)
       (ArrayImmutList. rv 0 c nil))
     (let [rv (ArrayLists$ObjectArrayList. (ArrayLists/objectArray 8) 0 nil)
           _ (reduce (fn [acc v] (.add rv (map-fn v))) nil coll)
           as (.getArraySection ^ArrayLists$ArrayOwner rv)]
       (ArrayImmutList. (.-array as) 0 (.size as) nil))))
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


(defmacro double-binary-operator
  "Create a binary operator that is specialized for double values.  Useful to speed up
  operations such as sorting or summation."
  [lvar rvar & code]
  `(reify
     DoubleBinaryOperator
     (applyAsDouble [this# ~lvar ~rvar]
       ~@code)
     IFnDef$DDD
     (invokePrim [this# l# r#]
       (.applyAsDouble this# l# r#))))


(defmacro long-binary-operator
  "Create a binary operator that is specialized for long values.  Useful to speed up
  operations such as sorting or summation."
  [lvar rvar & code]
  `(reify
     LongBinaryOperator
     (applyAsLong [this ~lvar ~rvar]
       ~@code)
     IFnDef$LLL
     (invokePrim [this# l# r#]
       (.applyAsLong this# l# r#))))


(defn ^:no-doc reduce-reducibles
  [reducibles]
  (let [^Reducible r (first reducibles)]
    (when-not (instance? Reducible r)
      (throw (Exception. (str "Sequence does not contain reducibles: " (type (first r))))))
    (.reduce r (rest reducibles))))


(def double-consumer-accumulator
  "Converts from a double consumer to a double reduction accumulator that returns the
  consumer:

```clojure
ham-fisted.api> (reduce double-consumer-accumulator
                             (Sum$SimpleSum.)
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  (reify IFnDef$ODO
    (invokePrim [this dc v]
      (.accept ^DoubleConsumer dc v)
      dc)))


(def long-consumer-accumulator
  "Converts from a long consumer to a long reduction accumulator that returns the
  consumer:

```clojure
ham-fisted.api> (reduce double-consumer-accumulator
                             (Sum$SimpleSum.)
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  (reify IFnDef$OLO
    (invokePrim [this dc v]
      (.accept ^LongConsumer dc v)
      dc)))


(defn consumer-accumulator
  "Generic reduction function using a consumer"
  [^Consumer c v]
  (.accept c v)
  c)


(defmacro indexed-double-accum
  "Create an indexed double accumulator that recieves and additional long index
  during a reduction:

```clojure
ham-fisted.api> (reduce (indexed-double-accum
                         acc idx v (conj acc [idx v]))
                        []
                        (range 5))
[[0 0.0] [1 1.0] [2 2.0] [3 3.0] [4 4.0]]
```"
  [accvar idxvar varvar & code]
  `(Reductions$IndexedDoubleAccum.
    (reify IFnDef$OLDO
      (invokePrim [this# ~accvar ~idxvar ~varvar]
        ~@code))))


(defmacro indexed-long-accum
  "Create an indexed long accumulator that recieves and additional long index
  during a reduction:

```clojure
ham-fisted.api> (reduce (indexed-long-accum
                         acc idx v (conj acc [idx v]))
                        []
                        (range 5))
[[0 0] [1 1] [2 2] [3 3] [4 4]]
```"
  [accvar idxvar varvar & code]
  `(Reductions$IndexedLongAccum.
    (reify IFnDef$OLLO
      (invokePrim [this# ~accvar ~idxvar ~varvar]
        ~@code))))


(defn ->consumer
  "Return an instance of a consumer, double consumer, or long consumer."
  [cfn]
  (cond
    (or (instance? Consumer cfn)
        (instance? DoubleConsumer cfn)
        (instance? LongConsumer cfn))
    cfn
    (instance? IFn$DO cfn)
    (reify DoubleConsumer (accept [this v] (.invokePrim ^IFn$DO cfn v)))
    (instance? IFn$LO cfn)
    (reify LongConsumer (accept [this v] (.invokePrim ^IFn$LO cfn v)))
    :else
    (reify Consumer (accept [this v] (cfn v)))))


(defn consume!
  "Consumer a collection.  This is simply a reduction where the return value
  is ignored.

  Returns the consumer."
  [consumer coll]
  (let [c (->consumer consumer)]
    (cond
      (instance? DoubleConsumer c)
      (reduce double-consumer-accumulator c coll)
      (instance? LongConsumer c)
      (reduce long-consumer-accumulator c coll)
      :else
      (reduce consumer-accumulator c coll))
    consumer))


(defn double-consumer-preducer
  "Return a preducer for a double consumer.

  Consumer must implement java.util.function.DoubleConsumer,
  ham_fisted.Reducible and clojure.lang.IDeref.

```clojure
user> (require '[ham-fisted.api :as hamf])
nil
user> (import '[java.util.function DoubleConsumer])
java.util.function.DoubleConsumer
user> (import [ham_fisted Reducible])
ham_fisted.Reducible
user> (import '[clojure.lang IDeref])
clojure.lang.IDeref
user> (deftype MeanR [^{:unsynchronized-mutable true :tag 'double} sum
                      ^{:unsynchronized-mutable true :tag 'long} n-elems]
        DoubleConsumer
        (accept [this v] (set! sum (+ sum v)) (set! n-elems (unchecked-inc n-elems)))
        Reducible
        (reduce [this o]
          (set! sum (+ sum (.-sum ^MeanR o)))
          (set! n-elems (+ n-elems (.-n-elems ^MeanR o)))
          this)
        IDeref (deref [this] (/ sum n-elems)))
user.MeanR
user> (hamf/declare-double-consumer-preducer! MeanR (MeanR. 0 0))
nil
  user> (hamf/preduce-reducer (double-consumer-preducer #(MeanR. 0 0)) (hamf/range 200000))
99999.5
```"
  [constructor]
  (reify
    protocols/Reducer
    (->init-val-fn [r] constructor)
    (->rfn [r] double-consumer-accumulator)
    protocols/Finalize
    (finalize [r v] @v)
    protocols/ParallelReducer
    (->merge-fn [r] reducible-merge)))


(defn consumer-preducer
  "Bind a consumer as a parallel reducer.

  Consumer must implement java.util.function.Consumer,
  ham_fisted.Reducible and clojure.lang.IDeref.

  Returns instance of type bound.

  See documentation for [[declare-double-consumer-preducer!]].
```"
  [constructor]
  (reify
    protocols/Reducer
    (->init-val-fn [r] constructor)
    (->rfn [r] consumer-accumulator)
    protocols/Finalize
    (finalize [r v] @v)
    protocols/ParallelReducer
    (->merge-fn [r] reducible-merge)))


(defn reducible-merge
  "Parallel reduction merge function that expects both sides to be an instances of
  Reducible"
  [^Reducible lhs rhs]
  (.reduce lhs rhs))


(defn sum-fast
  "Fast simple serial double summation.  Does not do any nan checking or summation
  compensation."
  ^double [coll]
  ;;Using raw reduce call as opposed to reduce-reducer to avoid protocol dispatch.
  @(reduce double-consumer-accumulator (Sum$SimpleSum.) coll))


(defmacro double-predicate
  "Create an implementation of java.util.Function.DoublePredicate"
  [varname & code]
  `(reify
     IFnDef$DoublePredicate
     (test [this ~varname]
       ~@code)))


(defmacro double-unary-operator
  "Create an implementation of java.util.function.DoubleUnaryOperator"
  [varname & code]
  `(reify
     IFnDef$DD
     (invokePrim [this# ~varname]
       ~@code)))


(defmacro long-predicate
  "Create an implementation of java.util.Function.LongPredicate"
  [varname & code]
  `(reify
     IFnDef$LongPredicate
     (test [this ~varname]
       ~@code)))


(defn ->long-predicate
  ^LongPredicate [f]
  (if (instance? LongPredicate f)
    f
    (long-predicate ll (boolean (f ll)))))


(defmacro long-unary-operator
  "Create an implementation of java.util.function.LongUnaryOperator"
  [varname & code]
  `(reify
     IFnDef$LL
     (invokePrim [this# ~varname]
       ~@code)))


(defmacro predicate
  "Create an implementation of java.util.Function.Predicate"
  [varname & code]
  `(reify
     IFnDef$Predicate
     (test [this ~varname]
       ~@code)))


(defmacro unary-operator
  "Create an implementation of java.util.function.UnaryOperator"
  [varname & code]
  `(function ~varname ~@code))


(defmacro binary-operator
  "Create an implementation of java.util.function.BinaryOperator"
  [arg1 arg2 & code]
  `(bi-function ~arg1 ~arg2 ~@code))


(defmacro double-consumer
  "Create an instance of a java.util.function.DoubleConsumer"
  [varname & code]
  `(reify
     DoubleConsumer
     (accept [this# ~varname]
       ~@code)
     IFnDef$DO
     (invokePrim [this# v#] (.accept this# v#))))


(defmacro long-consumer
  "Create an instance of a java.util.function.LongConsumer"
  [varname & code]
  `(reify
     LongConsumer
     (accept [this# ~varname]
       ~@code)
     IFnDef$LO
     (invokePrim [this# v#] (.accept this# v#))))


(defmacro consumer
  "Create an instance of a java.util.function.Consumer"
  [varname & code]
  `(reify Consumer
     (accept [this# ~varname]
       ~@code)
     IFnDef$OO
     (invoke [this# arg#] (.accept this# arg#))))


(defn bind-double-consumer-reducer!
  "Bind a classtype as a double consumer parallel reducer - the consumer must implement
  DoubleConsumer, ham_fisted.Reducible, and IDeref."
  ([cls-type ctor]
   (extend cls-type
     protocols/Reducer
     {:->init-val-fn (fn [r] ctor)
      :->rfn (fn [r] double-consumer-accumulator)
      :finalize (fn [r b] (deref b))}
     protocols/ParallelReducer
     {:->merge-fn (fn [r] reducible-merge)}))
  ([ctor]
   (bind-double-consumer-reducer! (type (ctor)) ctor)))


(bind-double-consumer-reducer! #(Sum.))
(bind-double-consumer-reducer! #(Sum$SimpleSum.))


(defn double-consumer-reducer
  "Make a parallel double consumer reducer given a function that takes no arguments and is
  guaranteed to produce a double consumer which also implements Reducible and IDeref"
  [ctor]
  (reify
    protocols/Reducer
    (->init-val-fn [this] ctor)
    (->rfn [this] double-consumer-accumulator)
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))


(defn long-consumer-reducer
  "Make a parallel double consumer reducer given a function that takes no arguments and is
  guaranteed to produce a double consumer which also implements Reducible and IDeref"
  [ctor]
  (reify
    protocols/Reducer
    (->init-val-fn [this] ctor)
    (->rfn [this] long-consumer-accumulator)
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))


(defn consumer-reducer
  "Make a parallel double consumer reducer given a function that takes no arguments and is
  guaranteed to produce a double consumer which also implements Reducible and IDeref"
  [ctor]
  (reify
    protocols/Reducer
    (->init-val-fn [this] ctor)
    (->rfn [this] consumer-accumulator)
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))


(defn ^:no-doc apply-nan-strategy
  [options coll]
  (case (get options :nan-strategy :remove)
    :remove (filter (double-predicate v (not (Double/isNaN v))) coll)
    :keep coll
    :exception (map (double-unary-operator v
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
    (preduce-reducer (consumer-reducer #(MaxKeyReducer. Long/MIN_VALUE nil f)) data)))

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
    (preduce-reducer (consumer-reducer #(MinKeyReducer. Long/MAX_VALUE nil f)) data)))


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


(defn priority-queue-rf
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
  [n coll]
  (when coll
    (let [coll (->reducible coll)]
      (if (instance? RandomAccess coll)
        (let [ne (.size ^List coll)
              n (min (long n) ne)]
          (.subList ^List coll 0 (- ne n)))
        (clojure.core/take-last n coll)))))


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

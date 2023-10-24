(ns ham-fisted.lazy-noncaching
  (:require [ham-fisted.iterator :as iterator]
            [ham-fisted.alists :as alists]
            [ham-fisted.protocols :as protocols]
            [ham-fisted.print :as pp])
  (:import [ham_fisted Transformables$MapIterable Transformables$FilterIterable
            Transformables$CatIterable Transformables$MapList Transformables$IMapable
            Transformables$SingleMapList Transformables StringCollection ArrayLists
            ArrayImmutList ArrayLists$ObjectArrayList IMutList TypedList LongMutList
            DoubleMutList ReindexList Transformables$IndexedMapper
            IFnDef$OLO IFnDef$ODO Reductions Reductions$IndexedAccum
            IFnDef$OLOO ArrayHelpers ITypedReduce PartitionByInner Casts
            IMutList]
           [java.lang.reflect Array]
           [it.unimi.dsi.fastutil.ints IntArrays]
           [java.util RandomAccess Collection Map List Random Set Iterator]
           [clojure.lang RT IPersistentMap IReduceInit IReduce PersistentList
            IFn$OLO IFn$ODO IFn$DD IFn$LD IFn$OD
            IFn$DL IFn$LL IFn$OL IFn$D IFn$L Counted IDeref Seqable IObj])
  (:refer-clojure :exclude [map concat filter repeatedly into-array shuffle object-array
                            remove map-indexed partition-by partition-all]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(def ^{:tag ArrayImmutList} empty-vec ArrayImmutList/EMPTY)

(declare concat map-reducible)

(defn ->collection
  "Ensure an item implements java.util.Collection.  This is inherently true for seqs and any
  implementation of java.util.List but not true for object arrays.  For maps this returns
  the entry set."
  ^Collection [item]
  (cond
    (nil? item) empty-vec
    (instance? Collection item)
    item
    :else
    (protocols/->collection item)))


(defn ->reducible
  [item]
  (if (or (instance? IReduceInit item)
          (instance? IReduce item)
          (instance? Iterable item)
          (protocols/reducible? item))
    item
    (->collection item)))


(def ^:private obj-ary-cls (Class/forName "[Ljava.lang.Object;"))


(defn object-array
  "Faster version of object-array for eductions, java collections and strings."
  ^objects [item]
  (let [item (if (instance? Map item) (.entrySet ^Map item) item)]
    (cond
      (or (nil? item) (number? item))
      (clojure.core/object-array item)
      (instance? obj-ary-cls item)
      item
      ;;Results of eduction aren't collections but do implement IReduceInit
      (instance? IReduceInit item)
      (if (or (instance? RandomAccess item) (instance? Counted item))
        (let [item-size (if (instance? RandomAccess item) (.size ^List item) (count item))
              retval (clojure.core/object-array item-size)]
          (reduce (Reductions$IndexedAccum.
                   (reify IFnDef$OLOO
                     (invokePrim [this acc idx v]
                       (ArrayHelpers/aset ^objects acc (unchecked-int idx) v)
                       acc)))
                  retval
                  item))
        (let [retval (ArrayLists$ObjectArrayList.)]
          (.addAllReducible retval item)
          (.toArray retval)))
      (instance? Collection item)
      (.toArray ^Collection item)
      (instance? String item)
      (.toArray (StringCollection. item))
      (.isArray (.getClass ^Object item))
      (.toArray (ArrayLists/toList item))
      (instance? Iterable item)
      (let [alist (ArrayLists$ObjectArrayList.)]
        (.addAllReducible alist item)
        (.toArray alist))
      :else
      (throw (Exception. (str "Unable to coerce item of type: " (type item)
                              " to an object array"))))))


(defn as-random-acces
  "If item implements RandomAccess, return List interface."
  ^List [item]
  (when (instance? RandomAccess item) item))


(defn ->random-access
  ^List [item]
  (if (instance? RandomAccess item)
    item
    (let [c (->collection item)]
      (if (instance? RandomAccess c)
        c
        (->collection (object-array c))))))

(defn constant-countable?
  [data]
  (or (nil? data)
      (instance? RandomAccess data)
      (instance? Counted data)
      (instance? Set data)
      (instance? Map data)
      (.isArray (.getClass ^Object data))))


(defn constant-count
  "Constant time count.  Returns nil if input doesn't have a constant time count."
  [data]
  (cond
    (nil? data) 0
    (instance? RandomAccess data) (.size ^List data)
    (instance? Counted data) (.count ^Counted data)
    (instance? Map data) (.size ^Map data)
    (instance? Set data) (.size ^Set data)
    (.isArray (.getClass ^Object data)) (Array/getLength data)))


(defn into-array
  ([aseq] (into-array (if-let [item (first aseq)] (.getClass ^Object item) Object) aseq))
  ([ary-type aseq]
   (let [^Class ary-type (or ary-type Object)
         aseq (->reducible aseq)]
     (if-let [c (constant-count aseq)]
       (let [rv (Array/newInstance ary-type (int c))]
         (.fillRangeReducible ^IMutList (alists/wrap-array rv) 0 aseq)
         rv)
       (let [^IMutList al (alists/wrap-array-growable (Array/newInstance ary-type 4) 0)]
         (.addAllReducible al aseq)
         (.toNativeArray al)))))
  ([ary-type mapfn aseq]
   (if mapfn
     (into-array ary-type (map-reducible mapfn aseq))
     (into-array ary-type aseq))))


(defn map
  ([f]
   (fn [rf]
     (let [rf (Transformables/typedMapReducer rf f)]
       (cond
         (instance? IFn$OLO rf)
         (reify IFnDef$OLO
           (invoke [this] (rf))
           (invoke [this result] (rf result))
           (invokePrim [this acc v] (.invokePrim ^IFn$OLO rf acc v))
           (applyTo [this args]
             (rf (first args) (apply f (rest args)))))
         (instance? IFn$ODO rf)
         (reify IFnDef$ODO
           (invoke [this] (rf))
           (invoke [this result] (rf result))
           (invokePrim [this acc v] (.invokePrim ^IFn$ODO rf acc v))
           (applyTo [this args]
             (rf (first args) (apply f (rest args)))))
         :else
         (fn
           ([] (rf))
           ([result] (rf result))
           ([result input]
            (rf result input))
           ([result input & inputs]
            (apply rf result input inputs)))))))
  ([f arg]
   (cond
     (nil? arg) PersistentList/EMPTY
     (instance? Transformables$IMapable arg)
     (.map ^Transformables$IMapable arg f)
     (instance? RandomAccess arg)
     (Transformables$SingleMapList. f nil arg)
     :else
     (Transformables$MapIterable/createSingle f nil arg)))
  ([f arg & args]
   (let [args (concat [arg] args)]
     (if (every? #(instance? RandomAccess %) args)
       (Transformables$MapList/create f nil (into-array List args))
       (Transformables$MapIterable. f nil (object-array args))))))


(pp/implement-tostring-print Transformables$SingleMapList)
(pp/implement-tostring-print Transformables$MapIterable)
(pp/implement-tostring-print Transformables$MapList)


(defn map-indexed
  [map-fn coll]
  (cond
    (nil? coll)
    coll
    (instance? RandomAccess coll)
    (let [^List coll coll]
      (reify
        IMutList
        (size [this] (.size coll))
        (get [this idx] (map-fn idx (.get coll idx)))
        (subList [this sidx eidx]
          (map-indexed map-fn (.subList coll sidx eidx)))
        (reduce [this rfn acc]
          (reduce (Reductions$IndexedAccum.
                   (reify IFnDef$OLOO
                     (invokePrim [this acc idx v]
                       (rfn acc (map-fn idx v)))))
                  acc coll))
        Transformables$IMapable
        (map [this mfn] (map-indexed (fn [idx v]
                                       (-> (map-fn idx v)
                                           (mfn)))
                                     coll))))
    :else
    (Transformables$IndexedMapper. map-fn (protocols/->iterable coll) nil)))


(pp/implement-tostring-print Transformables$IndexedMapper)


(defn map-reducible
  "Map a function over r - r need only be reducible.  Returned value does not implement
  seq but is countable when r is countable countable."
  [f r]
  (if-let [c (constant-count r)]
    (reify
      Counted
      (count [this] c)
      IReduceInit
      (reduce [this rfn acc]
        (Reductions/serialReduction (Transformables/typedMapReducer rfn f) acc r)))
    (reify
      IReduceInit
      (reduce [this rfn acc]
        (Reductions/serialReduction (Transformables/typedMapReducer rfn f) acc r)))))


(defn concat
  ([] PersistentList/EMPTY)
  ([a] (if a a PersistentList/EMPTY))
  ([a & args]
   (if (instance? Transformables$IMapable a)
    (.cat ^Transformables$IMapable a args)
    (Transformables$CatIterable. (cons a args)))))


(pp/implement-tostring-print Transformables$CatIterable)


(defn apply-concat
  ([] PersistentList/EMPTY)
  ([data]
   (Transformables$CatIterable. data)))


(defn filter
  ([pred]
   (fn [rf]
     (Transformables$FilterIterable/typedReducer rf pred)))
  ([pred coll]
   (cond
     (nil? coll) PersistentList/EMPTY
     (instance? Transformables$IMapable coll)
     (.filter ^Transformables$IMapable coll pred)
     :else
     (Transformables$FilterIterable. pred nil coll))))


(pp/implement-tostring-print Transformables$FilterIterable)


(defn remove
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns logical false. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([pred coll]
   (filter (complement pred) coll))
  ([pred] (filter (complement pred))))


(defmacro make-readonly-list
  "Implement a readonly list.  If cls-type-kwd is provided it must be, at compile time,
  either :int64, :float64 or :object and the getLong, getDouble or get interface methods
  will be filled in, respectively.  In those cases read-code must return the appropriate
  type."
  ([n idxvar read-code]
   `(make-readonly-list :object ~n ~idxvar ~read-code))
  ([cls-type-kwd n idxvar read-code]
   `(let [~'nElems (int ~n)]
      ~(case cls-type-kwd
         :int64
         `(reify
            TypedList
            (containedType [this#] Long/TYPE)
            LongMutList
            (size [this#] ~'nElems)
            (getLong [this# ~idxvar] ~read-code))
         :float64
         `(reify
            TypedList
            (containedType [this#] Double/TYPE)
            DoubleMutList
            (size [this#] ~'nElems)
            (getDouble [this# ~idxvar] ~read-code))
         :object
         `(reify IMutList
            (size [this#] ~'nElems)
            (get [this# ~idxvar] ~read-code))))))


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


(defn type-zero-arg-ifn
  "Categorize the return type of a single argument ifn.  May be :float64, :int64, or :object."
  [ifn]
  (cond
    (instance? IFn$D ifn)
    :float64
    (instance? IFn$L ifn)
    :int64
    :else
    :object))


(defn repeatedly
  "When called with one argument, produce infinite list of calls to v.
  When called with two arguments, produce a random access list of length n of calls to v."
  ([f] (clojure.core/repeatedly f))
  (^IMutList [n f]
   (let [n (int n)]
     (case (type-zero-arg-ifn f)
       :int64
       (reify TypedList
         (containedType [this] Long/TYPE)
         LongMutList
         (size [this] (unchecked-int n))
         (getLong [this idx] (Casts/longCast (f))))
       :float64
       (reify TypedList
         (containedType [this] Double/TYPE)
         DoubleMutList
         (size [this] (unchecked-int n))
         (getDouble [this idx] (Casts/doubleCast (f))))
       (reify IMutList
         (size [this] (int n))
         (get [this idx] (f)))))))


(defn ^:no-doc contained-type
  [coll]
  (when (instance? TypedList coll)
    (.containedType ^TypedList coll)))


(defn- int-primitive?
  [cls]
  (or (identical? Byte/TYPE cls)
      (identical? Short/TYPE cls)
      (identical? Integer/TYPE cls)
      (identical? Long/TYPE cls)))


(defn- double-primitive?
  [cls]
  (or (identical? Float/TYPE cls)
      (identical? Double/TYPE cls)))



(defn shift
  "Shift a collection forward or backward repeating either the first or the last entries.
  Returns a random access list with the same elements as coll.

  Example:

```clojure
ham-fisted.api> (shift 2 (range 10))
[0 0 0 1 2 3 4 5 6 7]
ham-fisted.api> (shift -2 (range 10))
[2 3 4 5 6 7 8 9 9 9]
```"
  [n coll]
  (let [n (long n)
        coll (->random-access coll)
        n-elems (.size coll)
        ne (dec n-elems)
        ctype (contained-type coll)
        ^IMutList ml coll]
    (cond
      (int-primitive? ctype)
      (make-readonly-list :int64 n-elems idx (.getLong ml (min ne (max 0 (- idx n)))))
      (double-primitive? ctype)
      (make-readonly-list :float64 n-elems idx (.getDouble ml (min ne (max 0 (- idx n)))))
      :else
      (make-readonly-list n-elems idx (.get coll (min ne (max 0 (- idx n))))))))


(defn seed->random
  ^Random [seed]
  (cond
    (instance? Random seed) seed
    (number? seed) (Random. (int seed))
    (nil? seed) (Random.)
    :else
    (throw (Exception. (str "Invalid seed type: " seed)))))


(def ^:private int-ary-cls (Class/forName "[I"))


(defn reindex
  "Permut coll by the given indexes.  Result is random-access and the same length as
  the index collection.  Indexes are expected to be in the range of [0->count(coll))."
  [coll indexes]
  (let [^ints indexes (if (instance? int-ary-cls indexes)
                        indexes
                        (int-array indexes))
        ^List coll (if (instance? RandomAccess coll)
                     coll
                     (->random-access coll))]
    (if (instance? IMutList coll)
      (.reindex ^IMutList coll indexes)
      (ReindexList/create indexes coll (meta coll)))))


(defn shuffle
  "shuffle values returning random access container.

  Options:

  * `:seed` - If instance of java.util.Random, use this.  If integer, use as seed.
  If not provided a new instance of java.util.Random is created."
  (^List [coll] (shuffle coll nil))
  (^List [coll opts]
   (let [coll (->random-access coll)
         random (seed->random (get opts :seed))]
     (if (instance? IMutList coll)
       (.immutShuffle ^IMutList coll random)
       (reindex coll (IntArrays/shuffle (ArrayLists/iarange 0 (.size coll) 1) random))))))


(deftype ^:private PartitionOuterIter [^Iterator iter
                                       ignore-leftover?
                                       f
                                       ^:unsynchronized-mutable last-iter]
  Iterator
  (hasNext [this] (if last-iter
                    (do (when (and (not ignore-leftover?)
                                   (.hasNext ^Iterator last-iter))
                          (throw (RuntimeException. "Sub-collection was not completely iterated through")))
                        (boolean @last-iter))
                    (.hasNext iter)))
  (next [this]
    (if last-iter
      (let [piter-data @last-iter
            v (piter-data 0)
            fv (piter-data 1)
            rv (PartitionByInner. iter f v)]
        (set! last-iter rv)
        rv)
      (let [v (.next iter)
            fv (f v)
            rv (PartitionByInner. iter f v)]
        (set! last-iter rv)
        rv))))


(deftype ^:private PartitionBy [f coll ignore-leftover? m
                                ^{:unsynchronized-mutable true
                                  :tag long} _hasheq]
  ITypedReduce
  (reduce [this rfn acc]
    (let [citer (.iterator ^Iterable (protocols/->iterable coll))]
      (if (.hasNext citer)
        (loop [acc acc
               v (.next citer)
               fv (f v)]
          (let [
                piter (PartitionByInner. citer f v)
                ;;piter (PartitionInnerIter. citer f fv true v fv)
                acc (rfn acc piter)
                _ (when (and (not ignore-leftover?)
                             (.hasNext piter))
                    (throw (RuntimeException. "Sub-collection was not entirely consumed.")))
                piter-data @piter]
            (if (reduced? acc)
              @acc
              (if piter-data
                (recur acc (piter-data 0) (piter-data 1))
                acc))))
        acc)))
  Iterable
  (iterator [this] (PartitionOuterIter. (.iterator ^Iterable (protocols/->iterable coll))
                                        ignore-leftover?
                                        f
                                        nil))
  Seqable
  (seq [this] (clojure.core/map vec (clojure.lang.IteratorSeq/create (.iterator this))))
  clojure.lang.IHashEq
  (hasheq [this]
    (when (== _hasheq 0)
      (set! _hasheq (long (hash (seq this)))))
    _hasheq)
  clojure.lang.IPersistentCollection
  (count [this] (count (seq this)))
  (cons [this o] (cons (seq this) o))
  (empty [this] PersistentList/EMPTY)
  (equiv [this o]
    (if (identical? this o)
      true
      (if (instance? clojure.lang.IPersistentCollection o)
        (clojure.lang.Util/pcequiv (seq this) o)
        false)))
  IObj
  (meta [this] m)
  (withMeta [this mm] (PartitionBy. f coll ignore-leftover? mm 0))
  Object
  (toString [this] (.toString ^Object (map vec this)))
  (hashCode [this] (.hasheq this))
  (equals [this o] (.equiv this o)))



(defn partition-by
  "Lazy noncaching version of partition-by.  For reducing partitions into a singular value please see
  [[apply-concat]].  Return value most efficiently implements reduce with a slightly less efficient
  implementation of Iterable.

  Unlike clojure.core/partition-by this does not store intermediate elements nor does it build
  up intermediate containers.  This makes it somewhat faster in most contexts.

  Each sub-collection must be iterated through entirely before the next method of the parent iterator
  else the result will not be correct.

  Options:

  * `:ignore-leftover?` - When true leftover items in the previous iteration do not cause an exception.
  Defaults to false.


```clojure
user> ;;incorrect - inner items not iterated and non-caching!
user> (into [] (lznc/partition-by identity [1 1 1 2 2 2 3 3 3]))
Execution error at ham_fisted.lazy_noncaching.PartitionBy/reduce (lazy_noncaching.clj:514).
Sub-collection was not entirely consumed.

user> ;;correct - transducing form of into calls vec on each sub-collection
user> ;;thus iterating through it entirely.
user> (into [] (map vec) (lznc/partition-by identity [1 1 1 2 2 2 3 3 3]))
[[1 1 1] [2 2 2] [3 3 3]]

user> (def init-data (vec (lznc/apply-concat (lznc/map #(repeat 100 %) (range 1000)))))
#'user/init-data
user> (crit/quick-bench (mapv hamf/sum-fast (lznc/partition-by identity init-data)))
             Execution time mean : 366.915796 µs
  ...
nil
user> (crit/quick-bench (mapv hamf/sum-fast (clojure.core/partition-by identity init-data)))
             Execution time mean : 6.699424 ms
  ...
nil
user> (crit/quick-bench (into [] (comp (clojure.core/partition-by identity)
                                       (map hamf/sum-fast)) init-data))
             Execution time mean : 1.705864 ms
  ...
```"
  ([f] (clojure.core/partition-by f))
  ([f coll] (partition-by f nil coll))
  ([f options coll]
   (if (empty? coll)
     PersistentList/EMPTY
     (PartitionBy. f coll (boolean (get options :ignore-leftover?)) nil 0))))

(pp/implement-tostring-print PartitionBy)


(deftype ^:private PartitionAllInner [^{:unsynchronized-mutable true
                                        :tag long} n
                                      ^long step
                                      ^Iterator iter
                                      m]
  ITypedReduce
  (reduce [this rfn acc]
    (if-not (.hasNext this)
      acc
      (let [ss (unchecked-dec step)]
        (loop [nn n
               continue? true]
          (if (and (> nn 0) continue?)
            (let [acc (rfn acc (.next iter))]
              (if (reduced? acc)
                (do
                  (set! n (unchecked-dec nn))
                  (deref acc))
                (do
                  (dotimes [s ss] (when (.hasNext iter) (.next iter)))
                  (recur (unchecked-dec nn) (.hasNext iter)))))
            (do
              (set! n nn)
              acc))))))
  Iterator
  (hasNext [this] (and (> n 0) (.hasNext iter)))
  (next [this]
    (when-not (.hasNext iter)
      (throw (java.util.NoSuchElementException.)))
    (let [nval (.next iter)]
      (dotimes [s (unchecked-dec step)] (when (.hasNext iter) (.next iter)))
      (set! n (unchecked-dec n))
      nval))
  IObj
  (meta [this] m)
  (withMeta [this mm] (PartitionAllInner. n step iter mm)))

(deftype ^:private PartitionAllSingle [^{:unsynchronized-mutable true
                                         :tag long} n
                                       ^Iterator iter
                                       m]
  ITypedReduce
  (reduce [this rfn acc]
    (if-not (.hasNext this)
      acc
      (loop [nn n
             continue? true]
        (if (and (> nn 0) continue?)
          (let [acc (rfn acc (.next iter))]
            (if (reduced? acc)
              (do
                (set! n (unchecked-dec nn))
                (deref acc))
              (do
                (recur (unchecked-dec nn) (.hasNext iter)))))
          (do
            (set! n nn)
            acc)))))
  Iterator
  (hasNext [this] (and (> n 0) (.hasNext iter)))
  (next [this]
    (when-not (.hasNext iter)
      (throw (java.util.NoSuchElementException.)))
    (let [nval (.next iter)]
      (set! n (unchecked-dec n))
      nval))
  IObj
  (meta [this] m)
  (withMeta [this mm] (PartitionAllSingle. n iter mm)))

(defn ^:no-doc partition-all-inner
  ^Iterator [^long n ^long step iter]
  (if (== step 1)
    (PartitionAllSingle. n iter nil)
    (PartitionAllInner. n step iter nil)))

(deftype ^:private PartitionAllOuter [^Iterator iter
                                      n
                                      step
                                      ignore-leftover?
                                      ^:unsynchronized-mutable last-iter]
  Iterator
  (hasNext [this]
    (when (and (not ignore-leftover?) last-iter (.hasNext ^Iterator last-iter))
      (throw (RuntimeException. "Sub-collection not completely iterated")))
    (.hasNext ^Iterator iter))
  (next [this]
    (let [rv (partition-all-inner n step iter)]
      (set! last-iter rv)
      rv)))


(deftype ^:private PartitionAll [^long n ^long step ^Iterable coll
                                 ignore-leftover? m]
  IObj
  (meta [this] m)
  (withMeta [this mm] (PartitionAll. n step coll ignore-leftover? mm))
  ITypedReduce
  (reduce [this rfn acc]
    (let [iter (.iterator coll)]
      (if-not (.hasNext iter)
        acc
        (loop []
          (let [^Iterator sub-iter (partition-all-inner n step iter)
                acc (rfn acc sub-iter)]
            (if (reduced? acc)
              @acc
              (do
                (when (and (not ignore-leftover?) (.hasNext sub-iter))
                  (throw (RuntimeException. "Sub-collection not completely consumed")))
                (if (.hasNext iter)
                  (recur)
                  acc))))))))
  Iterable
  (iterator [this] (PartitionAllOuter. (.iterator coll) n step ignore-leftover? nil)))


(defn partition-all
  "Lazy noncaching version of partition-all.  When input is random access returns random access result."
  ([n] (clojure.core/partition-all n))
  ([n coll] (partition-all n 1 coll))
  ([^long n ^long step coll]
   (if (empty? coll)
     '()
     (let [ns (* n step)]
       (if-let [coll (as-random-access coll)]
         (let [n-elems (.size coll)
               n-batches (quot (+ n-elems (dec ns)) ns)]
           (if (== 1 step)
             (reify IMutList
               (size [this] (unchecked-int n-batches))
               (get [this outer]
                 (when-not (and (>= outer 0) (< outer n-batches))
                   (throw (IndexOutOfBoundsException.)))
                 (let [sidx (* outer ns)
                       eidx (min n-elems (+ sidx n))]
                   (.subList coll sidx eidx))))
             (reify IMutList
               (size [this] (unchecked-int n-batches))
               (get [this outer]
                 (when-not (and (>= outer 0) (< outer n-batches))
                   (throw (IndexOutOfBoundsException.)))
                 (let [batch-start (* outer ns)
                       batch-n (long (min n (quot (- n-elems batch-start) step)))]
                   (reify IMutList
                     (size [this] (unchecked-int batch-n))
                     (get [this inner]
                       (.get coll (+ batch-start (* inner step))))))))))
         (PartitionAll. n step (protocols/->iterable coll) false nil))))))

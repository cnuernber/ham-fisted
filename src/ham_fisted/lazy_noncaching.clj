(ns ham-fisted.lazy-noncaching
  "Lazy, noncaching implementation of many clojure.core functions.  There are several benefits of carefully
   constructed lazy noncaching versions:

   1. No locking - better multithreading/green thread performance.
   2. Higher performance generally.
   3. More datatype flexibility - if map is passed a single randomly addressible or generically
   parallelizable container the result is still randomly addressible or generically perallelizable.
   For instance (map key {:a 1 :b 2}) returns in the generic case something that can still be parallelizable
   as the entry set of a map implements spliterator."
  (:require [ham-fisted.iterator :as iterator]
            [ham-fisted.alists :as alists]
            [ham-fisted.protocols :as protocols]
            [ham-fisted.defprotocol :as hamf-defproto]
            [ham-fisted.function :as hamf-fn]
            [ham-fisted.print :as pp]
            [ham-fisted.language :as hamf-language]
            [ham-fisted.iterator :as hamf-iter]
            [ham-fisted.datatypes])
  (:import [ham_fisted Transformables$IMapable Transformables$IterableSeq
            Transformables StringCollection ArrayLists
            ArrayImmutList ArrayLists$ObjectArrayList IMutList TypedList LongMutList
            DoubleMutList ReindexList MapFn ForkJoinPatterns ParallelOptions
            IFnDef$OLO IFnDef$ODO IFnDef$LO IFnDef$DO IFnDef$LongPredicate
            IFnDef$DoublePredicate IFnDef$Predicate Reductions Reductions$IndexedAccum
            IFnDef$OLOO ArrayHelpers ITypedReduce PartitionByInner Casts
            IMutList LazyChunkedSeq ParallelOptions$CatParallelism MutTreeList IFnDef
            LongAccum]
           [java.util.function LongPredicate DoublePredicate Predicate]
           [java.lang.reflect Array]
           [it.unimi.dsi.fastutil.ints IntArrays]
           [java.util.concurrent.atomic AtomicLong]
           [java.util RandomAccess Collection Map List Random Set Iterator Map$Entry ArrayList Comparator]
           [clojure.lang RT IPersistentMap IReduceInit IReduce PersistentList
            IFn$OLO IFn$ODO IFn$DD IFn$LD IFn$OD IFn ArraySeq
            IFn$DL IFn$LL IFn$OL IFn$D IFn$L IFn$LO IFn$DO Counted IDeref Seqable IObj
            ]
           [java.util NoSuchElementException Arrays])
  (:refer-clojure :exclude [map concat filter repeatedly into-array shuffle object-array
                            remove map-indexed partition-by partition-all every?
                            complement cond drop take count]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(def ^{:tag ArrayImmutList} empty-vec ArrayImmutList/EMPTY)

(declare concat map-reducible every?)

(defmacro cond
  "See documentation for [[ham-fisted.api/cond]]"
  [& clauses]
  `(hamf-language/cond ~@clauses))

(defn count
  ^long [m]
  (protocols/count m))

(hamf-defproto/extend Map$Entry protocols/Counted {:count 2})
(hamf-defproto/extend nil protocols/Counted {:count 0})

(defmacro countable-arrays
  []
  `(do
     ~@(->> hamf-language/array-classes
            (mapcat (fn [kv]
                      (let [alen-name (symbol (str (name (key kv)) "-alength"))]
                        `[(defn ~alen-name
                            ~(with-meta [(with-meta 'm {:tag (.getName ^Class (val kv))})]
                               {:tag 'long})
                            (alength ~'m))
                          (hamf-defproto/extend ~(val kv) protocols/Counted {:count ~alen-name})]))))))

(countable-arrays)

(hamf-defproto/extend-protocol protocols/Counted
  clojure.lang.Counted (count [s] (.count s))
  java.util.RandomAccess (count [s] (.size ^java.util.Collection s))
  CharSequence (count [s] (.length s))
  Map (count [s] (.size s))
  Object (count [s] (let [lc (LongAccum. 0)]
                      (reduce (fn [^LongAccum lc _v]
                                (.accept lc 1)
                                lc)
                              lc
                              s)
                      (long (.deref lc)))))


;;------------------------------------------------------------------------------
;; Reduction function composition.  Fusing map and filter into the reduction function
;; is how the lazy containers below get their reduction performance - these preserve
;; primitive pathways whenever the map fn/predicate and the rfn allow it.

(defmacro ^:private rf-reify
  "reify iface with the init and completion arities delegating to rfn."
  [iface rfn & methods]
  `(reify ~iface
     (invoke [_#] (~rfn))
     (invoke [_# r#] (~rfn r#))
     ~@methods))

(defn typed-map-reducer
  "Return a reduction fn that applies map-fn to each input before calling rfn."
  ^IFn [rfn mfn]
  (let [^IFn rfn rfn]
    (cond
      (instance? IFn$LL mfn)
      (let [rr (Transformables/toLongReductionFn rfn)]
        (rf-reify IFnDef$OLO rfn (invokePrim [_ acc v] (.invokePrim rr acc (.invokePrim ^IFn$LL mfn v)))))
      (instance? IFn$LD mfn)
      (let [rr (Transformables/toDoubleReductionFn rfn)]
        (rf-reify IFnDef$OLO rfn (invokePrim [_ acc v] (.invokePrim rr acc (.invokePrim ^IFn$LD mfn v)))))
      (instance? IFn$DD mfn)
      (let [rr (Transformables/toDoubleReductionFn rfn)]
        (rf-reify IFnDef$ODO rfn (invokePrim [_ acc v] (.invokePrim rr acc (.invokePrim ^IFn$DD mfn v)))))
      (instance? IFn$DL mfn)
      (let [rr (Transformables/toLongReductionFn rfn)]
        (rf-reify IFnDef$ODO rfn (invokePrim [_ acc v] (.invokePrim rr acc (.invokePrim ^IFn$DL mfn v)))))
      (instance? IFn$OL mfn)
      (let [rr (Transformables/toLongReductionFn rfn)]
        (rf-reify IFnDef rfn (invoke [_ acc v] (.invokePrim rr acc (.invokePrim ^IFn$OL mfn v)))))
      (instance? IFn$OD mfn)
      (let [rr (Transformables/toDoubleReductionFn rfn)]
        (rf-reify IFnDef rfn (invoke [_ acc v] (.invokePrim rr acc (.invokePrim ^IFn$OD mfn v)))))
      :else
      (let [^IFn mfn mfn]
        (rf-reify IFnDef rfn
                  (invoke [_ acc v] (rfn acc (mfn v)))
                  (applyTo [_ args] (rfn (first args) (.applyTo mfn (next args)))))))))

(defn typed-filter-reducer
  "Return a reduction fn that only calls rfn for inputs where pred is truthy."
  ^IFn [rfn pred]
  (let [^IFn rfn rfn]
    (cond
      (instance? LongPredicate pred)
      (let [rr (Transformables/toLongReductionFn rfn)]
        (rf-reify IFnDef$OLO rfn (invokePrim [_ acc v] (if (.test ^LongPredicate pred v) (.invokePrim rr acc v) acc))))
      (instance? IFn$LO pred)
      (let [rr (Transformables/toLongReductionFn rfn)]
        (rf-reify IFnDef$OLO rfn (invokePrim [_ acc v] (if (Transformables/truthy (.invokePrim ^IFn$LO pred v))
                                                         (.invokePrim rr acc v) acc))))
      (instance? DoublePredicate pred)
      (let [rr (Transformables/toDoubleReductionFn rfn)]
        (rf-reify IFnDef$ODO rfn (invokePrim [_ acc v] (if (.test ^DoublePredicate pred v) (.invokePrim rr acc v) acc))))
      (instance? IFn$DO pred)
      (let [rr (Transformables/toDoubleReductionFn rfn)]
        (rf-reify IFnDef$ODO rfn (invokePrim [_ acc v] (if (Transformables/truthy (.invokePrim ^IFn$DO pred v))
                                                         (.invokePrim rr acc v) acc))))
      (instance? Predicate pred)
      (rf-reify IFnDef rfn (invoke [_ acc v] (if (.test ^Predicate pred v) (rfn acc v) acc)))
      :else
      (let [^IFn pred pred]
        (rf-reify IFnDef rfn (invoke [_ acc v] (if (Transformables/truthy (pred v)) (rfn acc v) acc)))))))

(defn- and-preds
  "Compose two predicates preserving primitive pathways."
  [src dst]
  (cond
    (and (instance? LongPredicate src) (instance? LongPredicate dst))
    (reify IFnDef$LongPredicate
      (test [_ v] (if (.test ^LongPredicate src v) (.test ^LongPredicate dst v) false)))
    (and (instance? IFn$LO src) (instance? IFn$LO dst))
    (reify IFnDef$LO
      (invokePrim [_ v] (and (Transformables/truthy (.invokePrim ^IFn$LO src v))
                             (Transformables/truthy (.invokePrim ^IFn$LO dst v)))))
    (and (instance? DoublePredicate src) (instance? DoublePredicate dst))
    (reify IFnDef$DoublePredicate
      (test [_ v] (if (.test ^DoublePredicate src v) (.test ^DoublePredicate dst v) false)))
    (and (instance? IFn$DO src) (instance? IFn$DO dst))
    (reify IFnDef$DO
      (invokePrim [_ v] (and (Transformables/truthy (.invokePrim ^IFn$DO src v))
                             (Transformables/truthy (.invokePrim ^IFn$DO dst v)))))
    (and (instance? Predicate src) (instance? Predicate dst))
    (reify IFnDef$Predicate
      (test [_ v] (if (.test ^Predicate src v) (.test ^Predicate dst v) false)))
    :else
    (let [^IFn src src ^IFn dst dst]
      (fn [v] (and (Transformables/truthy (src v)) (Transformables/truthy (dst v)))))))

(defn- cat-reducer
  "Wrap rfn such that reduced values escape the container-level reduction so the outer
  concatenation also stops."
  ^IFn [rfn]
  (cond
    (instance? IFn$OLO rfn)
    (reify IFnDef$OLO (invokePrim [_ acc v] (let [acc (.invokePrim ^IFn$OLO rfn acc v)]
                                              (if (reduced? acc) (reduced acc) acc))))
    (instance? IFn$ODO rfn)
    (reify IFnDef$ODO (invokePrim [_ acc v] (let [acc (.invokePrim ^IFn$ODO rfn acc v)]
                                              (if (reduced? acc) (reduced acc) acc))))
    :else
    (let [^IFn rfn rfn]
      (reify IFnDef (invoke [_ acc v] (let [acc (rfn acc v)]
                                        (if (reduced? acc) (reduced acc) acc)))))))

;;------------------------------------------------------------------------------
;; Lazy noncaching containers

(defn- src-iter ^Iterator [src] (.iterator (Transformables/toIterable src)))

(defn- invoke-n
  "Invoke f with the arguments in args."
  [^IFn f ^objects args]
  (case (alength args)
    3 (f (aget args 0) (aget args 1) (aget args 2))
    4 (f (aget args 0) (aget args 1) (aget args 2) (aget args 3))
    (.applyTo f (ArraySeq/create args))))

(defn- iter-syms [k] (vec (clojure.core/repeatedly k #(with-meta (gensym "iter") {:tag 'java.util.Iterator}))))

(defn- bind-iters [syms iters]
  (vec (mapcat (fn [s idx] [s `(aget ~iters ~idx)]) syms (range))))

(defmacro ^:private zip-reduce
  "Allocation-free reduction of (f (.next i0) ... (.next ik-1)) over k iterators."
  [f rfn acc iters k]
  (let [its (iter-syms k)]
    `(let ~(bind-iters its iters)
       (loop [acc# ~acc]
         (if (and ~@(clojure.core/map (fn [s] `(.hasNext ~s)) its))
           (let [acc# (~rfn acc# (~f ~@(clojure.core/map (fn [s] `(.next ~s)) its)))]
             (if (reduced? acc#) (deref acc#) (recur acc#)))
           acc#)))))

(defmacro ^:private zip-iter
  "Allocation-free iterator of (f (.next i0) ... (.next ik-1)) over k iterators."
  [f iters k]
  (let [its (iter-syms k)]
    `(let ~(bind-iters its iters)
       (reify Iterator
         (hasNext [_#] (and ~@(clojure.core/map (fn [s] `(.hasNext ~s)) its)))
         (next [_#] (~f ~@(clojure.core/map (fn [s] `(.next ~s)) its)))))))

(definterface IFusedReduce
  (fusedSource [] "The source the reduction actually runs over.")
  (^clojure.lang.IFn fuseRfn [^clojure.lang.IFn rfn] "rfn with this container's transformation (and that of its fusable sources) applied."))

(defn- fused-source [src] (if (instance? IFusedReduce src) (.fusedSource ^IFusedReduce src) src))
(defn- fuse-rfn ^IFn [src rfn] (if (instance? IFusedReduce src) (.fuseRfn ^IFusedReduce src rfn) rfn))

(deftype ^:private FlatIter [^Iterator outer ^{:unsynchronized-mutable true :tag Iterator} inner]
  Iterator
  (hasNext [this]
    (loop []
      (cond
        (and inner (.hasNext inner)) true
        (.hasNext outer) (do (set! inner (when-let [c (.next outer)] (src-iter c)))
                             (recur))
        :else false)))
  (next [this]
    (if (.hasNext this)
      (.next inner)
      (throw (NoSuchElementException.)))))

(defn- flat-iter
  "Iterator over the elements of each iterable in the object array iterables."
  ^Iterator [^objects iterables] (FlatIter. (.iterator (ArrayLists/toList iterables)) nil))

(deftype ^:private FilterIter [^Iterator iter pred ^:unsynchronized-mutable nxt]
  Iterator
  (hasNext [this]
    (if (identical? nxt ::none)
      (loop []
        (if (.hasNext iter)
          (let [v (.next iter)]
            (if (Transformables/truthy (pred v))
              (do (set! nxt v) true)
              (recur)))
          false))
      true))
  (next [this]
    (if (.hasNext this)
      (let [v nxt] (set! nxt ::none) v)
      (throw (NoSuchElementException.)))))

(defmacro ^:private defseqtype
  "deftype with toString, equals and hashCode defined in terms of the clojure sequence
  interfaces."
  [nm fields & body]
  `(do
     (deftype ~nm ~fields
       ~@body
       Object
       (toString [this#] (Transformables/sequenceToString this#))
       (equals [this# o#] (.equiv this# o#))
       (hashCode [this#] (.hasheq this#)))
     (pp/implement-tostring-print ~nm)))

(defseqtype MapIterable [^IFn f m src]
  IFusedReduce
  (fusedSource [this] (fused-source src))
  (fuseRfn [this rfn] (fuse-rfn src (typed-map-reducer rfn f)))
  Transformables$IterableSeq
  (iterator [this]
    (let [it (src-iter src)]
      (reify Iterator
        (hasNext [_] (.hasNext it))
        (next [_] (f (.next it))))))
  (reduce [this rfn acc] (Reductions/serialReduction (.fuseRfn this rfn) acc (.fusedSource this)))
  (parallelReduction [this init-fn rfn merge-fn options]
    (Reductions/parallelReduction init-fn (.fuseRfn this rfn) merge-fn (.fusedSource this) options))
  (map [this nf] (MapIterable. (MapFn/create f nf) m src))
  (meta [this] m)
  (withMeta [this mm] (MapIterable. f mm src)))

;;map over two or more sources
(defseqtype MultiMapIterable [^IFn f m ^objects srcs]
  Transformables$IterableSeq
  (iterator [this]
    (let [iters (clojure.core/object-array (clojure.core/map src-iter srcs))
          n (alength iters)]
      (case n
        2 (zip-iter f iters 2)
        3 (zip-iter f iters 3)
        4 (zip-iter f iters 4)
        (reify Iterator
          (hasNext [_] (loop [idx 0]
                         (if (< idx n)
                           (if (.hasNext ^Iterator (aget iters idx)) (recur (unchecked-inc idx)) false)
                           true)))
          (next [_] (let [args (clojure.core/object-array n)]
                      (dotimes [idx n] (aset args idx (.next ^Iterator (aget iters idx))))
                      (invoke-n f args)))))))
  (reduce [this rfn acc]
    (let [^IFn rfn rfn
          iters (clojure.core/object-array (clojure.core/map src-iter srcs))]
      (case (alength iters)
        2 (zip-reduce f rfn acc iters 2)
        3 (zip-reduce f rfn acc iters 3)
        4 (zip-reduce f rfn acc iters 4)
        (Reductions/iterReduce this acc rfn))))
  (map [this nf] (MultiMapIterable. (MapFn/create f nf) m srcs))
  (meta [this] m)
  (withMeta [this mm] (MultiMapIterable. f mm srcs)))

(defseqtype FilterIterable [pred m src]
  IFusedReduce
  (fusedSource [this] (fused-source src))
  (fuseRfn [this rfn] (fuse-rfn src (typed-filter-reducer rfn pred)))
  Transformables$IterableSeq
  (iterator [this] (FilterIter. (src-iter src) pred ::none))
  (reduce [this rfn acc] (Reductions/serialReduction (.fuseRfn this rfn) acc (.fusedSource this)))
  (parallelReduction [this init-fn rfn merge-fn options]
    (Reductions/parallelReduction init-fn (.fuseRfn this rfn) merge-fn (.fusedSource this) options))
  (filter [this p] (FilterIterable. (and-preds pred p) m src))
  (meta [this] m)
  (withMeta [this mm] (FilterIterable. pred mm src)))

;;data is an array of iterables of containers
(defseqtype CatIterable [m ^objects data ^ParallelOptions$CatParallelism parallelism]
  Transformables$IterableSeq
  (iterator [this] (FlatIter. (flat-iter data) nil))
  (reduce [this rfn acc]
    (let [rf (cat-reducer rfn)
          containers (flat-iter data)]
      (loop [acc acc]
        (if (and (not (reduced? acc)) (.hasNext containers))
          (recur (Reductions/serialReduction rf acc (.next containers)))
          (Reductions/unreduce acc)))))
  (parallelReduction [this init-fn rfn merge-fn options]
    (let [^ParallelOptions options options
          rf (cat-reducer rfn)
          containers (reify Iterable (iterator [_] (flat-iter data)))]
      (Reductions/unreduce
       (if (identical? ParallelOptions$CatParallelism/SEQWISE (or parallelism (.-catParallelism options)))
         (let [partial (ForkJoinPatterns/pmap options #(Reductions/serialReduction rf (init-fn) %)
                                              (ArrayLists/toList (clojure.core/object-array [containers])))]
           (if (.-unmergedResult options)
             partial
             (Reductions/serialReduction merge-fn (init-fn) partial)))
         (let [mapped (MapIterable. #(Reductions/parallelReduction init-fn rf merge-fn % options)
                                    nil containers)]
           (if (.-unmergedResult options)
             (CatIterable. nil (clojure.core/object-array [mapped]) nil)
             (Reductions/iterableMerge options merge-fn mapped)))))))
  (cat [this iters]
    (let [n (alength data)
          nd (Arrays/copyOf data (unchecked-inc n))]
      (aset nd n iters)
      (CatIterable. m nd nil)))
  (meta [this] m)
  (withMeta [this mm] (CatIterable. mm data parallelism)))

(hamf-defproto/extend-protocol protocols/Counted
  CatIterable
  (count [s] (let [containers (flat-iter (.-data s))]
               (loop [n 0]
                 (if (.hasNext containers)
                   (recur (+ n (count (.next containers))))
                   n)))))

(defseqtype SingleMapList [^IFn f m ^List l]
  IFusedReduce
  (fusedSource [this] (fused-source l))
  (fuseRfn [this rfn] (fuse-rfn l (typed-map-reducer rfn f)))
  IMutList
  (size [this] (.size l))
  (get [this idx] (f (.get l idx)))
  (subList [this sidx eidx] (SingleMapList. f m (.subList l sidx eidx)))
  (reduce [this rfn acc] (Reductions/serialReduction (.fuseRfn this rfn) acc (.fusedSource this)))
  (parallelReduction [this init-fn rfn merge-fn options]
    (Reductions/parallelReduction init-fn (.fuseRfn this rfn) merge-fn (.fusedSource this) options))
  (meta [this] m)
  (withMeta [this mm] (SingleMapList. f mm l))
  Transformables$IMapable
  (map [this nf] (SingleMapList. (MapFn/create f nf) m l)))

;;Two or more lists.  Parallel reduction is provided by IMutList.
(defseqtype MapList [^IFn f m ^objects lists ^long n-elems]
  IMutList
  (size [this] (unchecked-int n-elems))
  (get [this idx]
    (case (alength lists)
      2 (f (.get ^List (aget lists 0) idx) (.get ^List (aget lists 1) idx))
      3 (f (.get ^List (aget lists 0) idx) (.get ^List (aget lists 1) idx) (.get ^List (aget lists 2) idx))
      (let [n (alength lists)
            args (clojure.core/object-array n)]
        (dotimes [aidx n] (aset args aidx (.get ^List (aget lists aidx) idx)))
        (invoke-n f args))))
  ;;Own reduction loop so the per-element call sites have a type profile specific to this class
  ;;as opposed to the IMutList default reduce shared by every list implementation.
  (reduce [this rfn acc]
    (let [^IFn rfn rfn
          n (unchecked-int n-elems)]
      (loop [idx 0 acc acc]
        (if (< idx n)
          (let [acc (rfn acc (.get this (unchecked-int idx)))]
            (if (reduced? acc) (deref acc) (recur (unchecked-inc idx) acc)))
          acc))))
  (subList [this sidx eidx]
    (MapList. f m (clojure.core/object-array (clojure.core/map #(.subList ^List % sidx eidx) lists))
              (- eidx sidx)))
  (meta [this] m)
  (withMeta [this mm] (MapList. f mm lists n-elems))
  Transformables$IMapable
  (map [this nf] (MapList. (MapFn/create f nf) m lists n-elems)))

(defn- map-list
  [f ^objects lists]
  (if (== 1 (alength lists))
    (SingleMapList. f nil (aget lists 0))
    (MapList. f nil lists (loop [idx 0 ne Long/MAX_VALUE]
                            (if (< idx (alength lists))
                              (recur (unchecked-inc idx) (min ne (.size ^List (aget lists idx))))
                              ne)))))

(defseqtype IndexedMapper [^IFn f src m]
  Transformables$IterableSeq
  (iterator [this] (.iterator ^Iterable (.deref this)))
  (reduce [this rfn acc] (.reduce ^IReduceInit (.deref this) rfn acc))
  (size [this] (Transformables/iterCount (src-iter src)))
  (map [this nf] (IndexedMapper. (fn [idx v] (nf (f idx v))) src m))
  (meta [this] m)
  (withMeta [this mm] (IndexedMapper. f src mm))
  IDeref
  ;;a fresh mapper with its own index counter
  (deref [this] (let [cnt (long-array 1)]
                  (MapIterable. (fn [v] (let [idx (aget cnt 0)]
                                          (aset cnt 0 (unchecked-inc idx))
                                          (f idx v)))
                                nil src))))

(defn ^:no-doc map-iterable
  "Default implementation of IMapable/map"
  [f coll] (MapIterable. f (meta coll) coll))

(defn ^:no-doc filter-iterable
  "Default implementation of IMapable/filter"
  [pred coll] (FilterIterable. pred (meta coll) coll))

(defn ^:no-doc cat-iterable
  "Default implementation of IMapable/cat"
  [coll iters]
  (CatIterable. (meta coll) (clojure.core/object-array [(ArrayLists/toList (clojure.core/object-array [coll])) iters])
                nil))

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


(defn ->iterable
  ^Iterable [a]
  (if (instance? Iterable a) a
      (protocols/->iterable a)))


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

(def ^:no-doc long-array-cls (Class/forName "[J"))
(def ^:no-doc double-array-cls (Class/forName "[D"))
(def ^:no-doc obj-array-cls (Class/forName "[Ljava.lang.Object;"))


(defn as-random-access
  "If item implements RandomAccess, return List interface."
  ^List [item]
  (cond (instance? RandomAccess item) item
        (instance? long-array-cls item) (ArrayLists/toList ^longs item)
        (instance? double-array-cls item) (ArrayLists/toList ^doubles item)
        (instance? obj-array-cls item) (ArrayLists/toList ^objects item)))


(defn ->random-access
  ^List [item]
  (if (instance? RandomAccess item)
    item
    (let [c (->collection item)]
      (if (instance? RandomAccess c)
        c
        (-> (doto (MutTreeList.)
              (.addAllReducible c))
            (persistent!))))))

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
     (let [rf (typed-map-reducer rf f)]
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
     (SingleMapList. f nil arg)
     :else
     (MapIterable. f nil arg)))
  ([f arg & args]
   (let [args (clojure.core/object-array (cons arg args))]
     (if (clojure.core/every? #(instance? RandomAccess %) args)
       (map-list f args)
       (MultiMapIterable. f nil args)))))




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
    (IndexedMapper. map-fn (->iterable coll) nil)))




(defn map-reducible
  "Map a function over r - r need only be reducible.  Returned value does not implement
  seq but is countable when r is countable."
  [f r]
  (if-let [c (constant-count r)]
    (reify
      Counted
      (count [this] c)
      IReduceInit
      (reduce [this rfn acc]
        (Reductions/serialReduction (typed-map-reducer rfn f) acc r)))
    (reify
      IReduceInit
      (reduce [this rfn acc]
        (Reductions/serialReduction (typed-map-reducer rfn f) acc r)))))


(defn tuple-map
  "Lazy nonaching map but f simply gets a single random-access list of arguments.
  The argument list may be mutably updated between calls."
  ([f c1]
   (let [rdc (fn [rfn acc] (reduce (fn [acc v] (rfn acc (f [v]))) acc c1))]
     (if-let [c1 (as-random-access c1)]
       (reify IMutList
         (size [this] (.size c1))
         (get [this idx] (f [(.get c1 idx)]))
         (subList [this sidx eidx]
           (tuple-map f (.subList c1 sidx eidx)))
         (reduce [this rfn acc]
           (rdc rfn acc)))
       (reify
         Iterable
         (iterator [this]
           (let [citer (.iterator (->iterable c1))]
             (reify Iterator
               (hasNext [this] (.hasNext citer))
               (next [this] (f [(.next citer)])))))
         Seqable
         (seq [this] (LazyChunkedSeq/chunkIteratorSeq (.iterator this)))
         ITypedReduce
         (reduce [this rfn acc]
           (rdc rfn acc))))))
  ([f c1 c2]
   (let [c1 (->iterable c1)
         c2 (->iterable c2)]
     (reify
       Iterable
       (iterator [this]
         (let [c1-iter (.iterator c1)
               c2-iter (.iterator c2)]
           (reify Iterator
             (hasNext [this] (and (.hasNext c1-iter)
                                  (.hasNext c2-iter)))
             (next [this]
               (f [(.next c1-iter) (.next c2-iter)])))))
       Seqable
       (seq [this] (LazyChunkedSeq/chunkIteratorSeq (.iterator this)))
       ITypedReduce
       (reduce [this rfn acc]
         (Reductions/iterReduce this acc rfn)))))
  ([f c1 c2 & cs]
   (let [cs (doto (ArrayLists$ObjectArrayList.)
              (.add c1)
              (.add c2)
              (.addAll cs))
         nargs (.size cs)
         next-fn (fn next-fn [iters ^objects args]
                   (loop [idx 0]
                     (if (< idx nargs)
                       (let [^Iterator iter (iters idx)]
                         (if (.hasNext iter)
                           (do
                             (ArrayHelpers/aset args (unchecked-int idx) (.next iter))
                             (recur (unchecked-inc idx)))
                           false))
                       true)))
         rdc (fn [rfn acc]
               (let [iters (mapv #(.iterator (->iterable %)) cs)]
                 (loop [acc acc
                        args (ArrayLists/objectArray nargs)
                        next? (next-fn iters args)]
                   (if next?
                     (let [acc (rfn acc (f (ArrayLists/toList ^objects args)))]
                       (if (reduced? acc)
                         (deref acc)
                         (let [args (ArrayLists/objectArray nargs)]
                           (recur acc args (next-fn iters args)))))
                     acc))))]
     (reify
       Iterable
       (iterator [this]
         (let [args (ArrayLists/objectArray nargs)
               argvec (ArrayLists/toList args)
               iters (mapv #(.iterator (->iterable %)) cs)]
           (reify
             Iterator
             (hasNext [this] (clojure.core/every? #(.hasNext ^Iterator %) iters))
             (next [this]
               (when-not (next-fn iters args)
                 (throw (java.util.NoSuchElementException.)))
               (f argvec)))))
       Seqable
       (seq [this] (LazyChunkedSeq/chunkIteratorSeq (.iterator this)))
       ITypedReduce
       (reduce [this rfn acc]
         (rdc rfn acc))))))


(defn apply-concat
  "A more efficient form of (apply concat ...) that doesn't force data to be a clojure seq.
  See [[concat-opts]] for opts definition."
  ([] PersistentList/EMPTY)
  ([data]
   (CatIterable. nil (clojure.core/object-array [data]) nil))
  ([opts data]
   (CatIterable. nil (clojure.core/object-array [data])
                 (condp identical? (get opts :cat-parallelism)
                   :seq-wise ParallelOptions$CatParallelism/SEQWISE
                   :elem-wise ParallelOptions$CatParallelism/ELEMWISE
                   nil nil))))

(defn concat
  ([] PersistentList/EMPTY)
  ([a] (if a a PersistentList/EMPTY))
  ([a & args]
   (if (instance? Transformables$IMapable a)
     (.cat ^Transformables$IMapable a args)
     (apply-concat (cons a args)))))


(defn concat-opts
  "Concat where the first argument is an options map.  This variation allows you to set the `:cat-parallelism`
  as you may have an idea the best way to parallelism this concatenation at time of the concatenation creation.

  Options:

  `:cat-parallelism` - Set the type of parallelism - either `:elem-wise` or `:seq-wise`  - this overrides
   settings later passed into calls such as [[reduce.preduce]] - see [[reduce/options->parallel-options]]
   for definition."
  ([opts a] (if a a PersistentList/EMPTY))
  ([opts a & args]
   (if (instance? Transformables$IMapable a)
     (.cat ^Transformables$IMapable a args)
     (apply-concat opts (cons a args)))))


(defn filter
  ([pred]
   (fn [rf]
     (typed-filter-reducer rf pred)))
  ([pred coll]
   (cond
     (nil? coll) PersistentList/EMPTY
     (instance? Transformables$IMapable coll)
     (.filter ^Transformables$IMapable coll pred)
     :else
     (FilterIterable. pred nil coll))))




(defn complement
  "Like clojure core complement but avoids var lookup on 'not'"
  [f]
  (fn
    ([] (Transformables/not (f)))
    ([x] (Transformables/not (f x)))
    ([x y] (Transformables/not (f x y)))
    ([x y & zs] (Transformables/not (apply f x y zs)))))


(defn remove
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns logical false. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([pred coll]
   (filter (complement pred) coll))
  ([pred] (filter (complement pred))))

(declare drop take)

(deftype ^:private DropIterable [^long n data]
  clojure.lang.Sequential
  Iterable
  (iterator [this]
    (let [src-iter (.iterator (->iterable data))]
      (dotimes [idx n]
        (when (.hasNext src-iter)
          (.next src-iter)))
      src-iter))
  ITypedReduce
  (reduce [this rfn acc]
    (let [rfn ((drop n) rfn)]
      (reduce rfn acc data)))
  Object
  (toString [this] (Transformables/sequenceToString this)))

(pp/implement-tostring-print DropIterable)

(defmacro define-drop-tducer
  [nm iface rf-tag]
  (let [invoke-nm (if (= iface 'IFnDef)
                    'invoke
                    'invokePrim)]
    `(deftype ~(with-meta nm {:private true})
         [~(with-meta 'n {:unsynchronized-mutable true
                          :tag 'long})
          ~'rf]
       ~iface (~invoke-nm [this# ~'result ~'input]
               (let [~'nn (max -1 (dec ~'n))]
                 (set! ~'n ~'nn)
                 (if (neg? ~'nn)
                   (~(symbol (str "." (name invoke-nm))) ~(with-meta 'rf {:tag rf-tag}) ~'result ~'input)
                   ~'result)))
       (invoke [this#] (~'rf))
       (invoke [this# result#] (~'rf result#))
       clojure.lang.Fn)))

(define-drop-tducer DropLongTducer IFnDef$OLO clojure.lang.IFn$OLO)
(define-drop-tducer DropDoubleTducer IFnDef$ODO clojure.lang.IFn$ODO)
(define-drop-tducer DropTducer IFnDef clojure.lang.IFn)

(defn drop
  ([n]
   (fn [rf]
     (cond
       (instance? clojure.lang.IFn$OLO rf)
       (DropLongTducer. n rf)
       (instance? clojure.lang.IFn$ODO rf)
       (DropDoubleTducer. n rf)
       :else
       (DropTducer. n rf))))
  ([^long n data]
   (if(nil? data)
     '()
     (if-let [l (as-random-access data)]
       (let [sl (.size l)]
         (if (< sl n)
           '[]
           (.subList l n sl)))
       (DropIterable. n data)))))

(defmacro define-take-tducer
  [nm invoke-nm iface rf-tag]
  `(deftype ~(with-meta nm {:private true})
       [~(with-meta 'n {:unsynchronized-mutable true
                        :tag 'long})
        ~'rf]
     ~iface
     (~invoke-nm [this# ~'acc ~'v]
      (set! ~'n (max -1 (dec ~'n)))
      (if (neg? ~'n)
        ;;n started at <= 0 - terminate the reduction immediately
        (ensure-reduced ~'acc)
        (let [~'acc (~(symbol (str "." (name invoke-nm))) ~(with-meta 'rf {:tag rf-tag})
                     ~'acc ~'v)]
          (if (zero? ~'n)
            (ensure-reduced ~'acc)
            ~'acc))))
     (invoke [this#] (~'rf))
     (invoke [this# acc#] (~'rf acc#))
     clojure.lang.Fn))

(define-take-tducer TakeTducer invoke IFnDef clojure.lang.IFn)
(define-take-tducer TakeLongTducer invokePrim IFnDef$OLO clojure.lang.IFn$OLO)
(define-take-tducer TakeDoubleTducer invokePrim IFnDef$ODO clojure.lang.IFn$ODO)

(deftype TakeIterator [^{:unsynchronized-mutable true
                         :tag long} n
                       ^Iterator data]
  Iterator
  (hasNext [this] (and (pos? n) (.hasNext data)))
  (next [this]
    (set! n (dec n))
    (if (.hasNext data)
      (.next data)
      (throw (java.util.NoSuchElementException. "Iter out of range")))))

(deftype TakeIterable [n data]
  clojure.lang.Sequential
  Iterable
  (iterator [this]
    (TakeIterator. n (.iterator (->iterable data))))
  ITypedReduce
  (reduce [this rfn acc]
    (reduce ((take n) rfn) acc data))
  Object
  (toString [this] (Transformables/sequenceToString this)))

(pp/implement-tostring-print TakeIterable)

(defn take
  ([n]
   (fn [rf]
     (cond
       (instance? clojure.lang.IFn$OLO rf)
       (TakeLongTducer. n rf)
       (instance? clojure.lang.IFn$ODO rf)
       (TakeDoubleTducer. n rf)
       :else
       (TakeTducer. n rf))))
  ([^long n data]
   (if (nil? data)
     '()
     (if-let [l (as-random-access data)]
       (.subList l 0 (max 0 (min n (.size l))))
       (if (<= n 0)
         '()
         (TakeIterable. n data))))))

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
  (protocols/simplified-returned-datatype ifn))


(defn type-zero-arg-ifn
  "Categorize the return type of a single argument ifn.  May be :float64, :int64, or :object."
  [ifn]
  (protocols/simplified-returned-datatype ifn))

(defn repeatedly
  "When called with one argument, produce infinite list of calls to v.
  When called with two arguments, produce a non-caching random access list of length n of calls to v."
  ([f]
   (reify Iterable
     (iterator [this]
       (reify java.util.Iterator
         (hasNext [this] true)
         (next [this] (f))))))
  (^IMutList [n f]
   (let [n (int n)]
     (case (protocols/simplified-returned-datatype f)
       :int64
       (reify TypedList
         (containedType [this] Long/TYPE)
         LongMutList
         (size [this] (unchecked-int n))
         (getLong [this idx] (.invokePrim ^IFn$L f)))
       :float64
       (reify TypedList
         (containedType [this] Double/TYPE)
         DoubleMutList
         (size [this] (unchecked-int n))
         (getDouble [this idx] (.invokePrim ^IFn$D f)))
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
                                       binary-predicate
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
            rv (PartitionByInner. iter f v binary-predicate)]
        (set! last-iter rv)
        rv)
      (let [v (.next iter)
            fv (f v)
            rv (PartitionByInner. iter f v binary-predicate)]
        (set! last-iter rv)
        rv))))


(deftype ^:private PartitionBy [f coll ignore-leftover? m
                                binary-predicate
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
                piter (PartitionByInner. citer f v binary-predicate)
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
                                        binary-predicate
                                        nil))
  Seqable
  (seq [this]
    (let [ii (clojure.lang.IteratorSeq/create (.iterator this))]
      (when ii
        (clojure.core/map vec (clojure.lang.IteratorSeq/create (.iterator this))))))
  clojure.lang.Sequential
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
  (withMeta [this mm] (PartitionBy. f coll ignore-leftover? mm binary-predicate 0))
  Object
  (toString [this] (.toString ^Object (map vec this)))
  (hashCode [this] (.hasheq this))
  (equals [this o] (.equiv this o)))


(pp/implement-tostring-print PartitionBy)


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
  * `:binary-predicate` - When provided, use this for equality semantics.  Defaults to equiv semantics
     but in a numeric context it may be useful to have `(== ##NaN ##Nan)`.


```clojure
user> ;;incorrect - inner items not iterated and non-caching!
user> (into [] (lznc/partition-by identity [1 1 1 2 2 2 3 3 3]))
Execution error at ham_fisted.lazy_noncaching.PartitionBy/reduce (lazy_noncaching.clj:514).
Sub-collection was not entirely consumed.

user> ;;correct - transducing form of into calls vec on each sub-collection
user> ;;thus iterating through it entirely.
user> (into [] (map vec) (lznc/partition-by identity [1 1 1 2 2 2 3 3 3]))
[[1 1 1] [2 2 2] [3 3 3]]
user> ;;filter,collect NaN out of sequence
user> (lznc/map hamf/vec (lznc/partition-by identity {:binary-predicate (hamf-fn/binary-predicate
                                                                         x y (let [x (double x)
                                                                                   y (double y)]
                                                                               (cond
                                                                                 (Double/isNaN x)
                                                                                 (if (Double/isNaN y)
                                                                                   true
                                                                                   false)
                                                                                 (Double/isNaN y) false
                                                                                 :else true))) }
                                            [1 2 3 ##NaN ##NaN 3 4 5]))
([1 2 3] [NaN NaN] [3 4 5])

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
   (PartitionBy. f coll (boolean (get options :ignore-leftover?)) nil
                 (hamf-fn/binary-predicate-or-null (get options :binary-predicate))
                 0)))

(defn partition-by-comparator
  "Partition by a comparator.  Sub partitions must be fully consumed before parent is called.  An optional
  timeout can be used if sub partitions are being consumed in a future - the parent iteration will block
  until the sub partition is fully consumed.

  Options:
  * `:timeout-ms` - Timeout in milliseconds - if the sub partition isn't consumed by this time an exception
  is thrown."
  ([cmp coll] (partition-by-comparator cmp nil coll))
  ([^Comparator cmp options coll]
   (let [tms (long (get options :timeout-ms 1))
         update (fn [{:keys [rest*] :as ctx}]
                  (let [iter (hamf-iter/->iterator (let [ii (deref rest* tms ::timeout)]
                                                     (when (identical? ii ::timeout)
                                                       (throw (ex-info "Sub partition not fully consumed"
                                                                       {:timeout-ms tms})))
                                                     ii))]
                    (when (hamf-iter/has-next? iter)
                      (let [next-v (hamf-iter/next iter)
                            pred #(== 0 (.compare cmp next-v %))]
                        (hamf-iter/iter-take-while pred (hamf-iter/iter-cons next-v iter))))))]
     (hamf-iter/seq-once-iterable
      :data
      #(update {:rest* (deliver (promise) (hamf-iter/->iterator coll))})
      update
      :data))))

(defn partition-by-cost
  "Partition a sequence by integer cost.  Will produce partitions of average cost '(quot max-cost 2)
  with some partitions being larger or smaller if the input sequence ends.  This function not very lazy
  with each new partition greedily calculated."
  [cost-fn ^long max-cost coll]
  (let [^clojure.lang.IFn$OL cost-fn (if (instance? clojure.lang.IFn$OL cost-fn)
                                       cost-fn
                                       (fn ^long [o] (long (cost-fn o))))
        next-partition (ArrayList.)
        half-max (quot max-cost 2)
        update (fn [{:keys [iter half-info cur-cost] :as ctx}]
                 (loop [cur-cost (long (or cur-cost 0))
                        half-info half-info]
                   (if (hamf-iter/has-next? iter)
                     (let [next-e (.next ^Iterator iter)
                           cur-cost (+ cur-cost (.invokePrim cost-fn next-e))
                           over-half? (>= cur-cost half-max)]
                       (.add next-partition next-e)
                       (if (< cur-cost max-cost)
                         (recur cur-cost (if (and (nil? half-info) over-half?)
                                           [cur-cost (.size next-partition)]
                                           half-info))
                         (let [[half-cost half-idx] half-info
                               next-cost (- cur-cost (long (or half-cost 0)))
                               split-idx (long (or half-idx (.size next-partition)))
                               subl (.subList next-partition 0 split-idx)
                               ctx (assoc ctx
                                          :rows (vec subl)
                                          :half-cost next-cost
                                          :cur-cost next-cost
                                          :half-idx (- (.size next-partition) split-idx))]
                           (.clear subl)
                           ctx)))
                     (when-not (.isEmpty next-partition)
                       (let [rv {:rows (vec next-partition)}]
                         (.clear next-partition)
                         rv)))))]
    (hamf-iter/seq-once-iterable :rows #(update {:iter (hamf-iter/->iterator coll)}) update :rows)))

(defn partition-all
  "Lazy noncaching version of partition-all.  When input is random access returns random access result.

  If input is not random access then similar to [[partition-by]] each sub-collection must be entirely
  iterated through before requesting the next sub-collection.

```clojure
user> (crit/quick-bench (mapv hamf/sum-fast (lznc/partition-all 100 (range 100000))))
             Execution time mean : 335.821098 µs
nil
user> (crit/quick-bench (mapv hamf/sum-fast (partition-all 100 (range 100000))))
             Execution time mean : 6.831242 ms
nil
user> (crit/quick-bench (into [] (comp (partition-all 100)
                                       (map hamf/sum-fast))
                              (range 100000)))
             Execution time mean : 1.645954 ms
nil
```"
  ([n] (clojure.core/partition-all n))
  ([n coll] (partition-all n n coll))
  ([^long n ^long step coll]
   (if (empty? coll)
     '()
     (let [ns step]
       (if-let [coll (as-random-access coll)]
         (let [n-elems (.size coll)
               n-batches (quot (+ n-elems (dec ns)) ns)]
           (reify IMutList
             (size [this] (unchecked-int n-batches))
             (get [this outer]
               (when-not (and (>= outer 0) (< outer n-batches))
                 (throw (IndexOutOfBoundsException.)))
               (let [sidx (* outer ns)
                     eidx (min n-elems (+ sidx n))]
                 (.subList coll sidx eidx)))))
         (if (== n step)
           (let [iter (hamf-iter/->iterator coll)
                 update (fn [sub-iter]
                          (when (hamf-iter/has-next? sub-iter)
                            (throw (RuntimeException. "Sub iterator has more elements left")))
                          (when (hamf-iter/has-next? iter)
                            (hamf-iter/iter-take n iter)))]
             (-> (hamf-iter/once-iterable
                  identity
                  #(update nil)
                  update
                  hamf-iter/wrap-iter)
                 (hamf-iter/seq-iterable)))
           ;;No fastpath here because if step isn't n then that implies caching in the sequence
           (clojure.core/partition-all n step coll)))))))


(defn every?
  "Faster (in most circumstances) implementation of clojure.core/every?.  This can be much faster in the case
  of primitive arrays of values.  Type-hinted functions are best if coll is primitive array - see example.

```clojure
user> (type data)
[J
user> (count data)
100
user> (def vdata (vec data))
#'user/vdata
user> (crit/quick-bench (every? (fn [^long v] (> v 80)) data))
             Execution time mean : 40.248868 ns
nil
user> (crit/quick-bench (lznc/every? (fn [^long v] (> v 80)) data))
             Execution time mean : 7.601190 ns
nil
user> (crit/quick-bench (every? (fn [^long v] (< v 80)) vdata))
             Execution time mean : 1.269582 µs
nil
user> (crit/quick-bench (lznc/every? (fn [^long v] (< v 80)) vdata))
             Execution time mean : 211.645613 ns
nil
user>
```"
  [pred coll]
  (if-let [coll (as-random-access coll)]
    (let [ne (.size coll)
          pred-type (if (instance? IMutList coll)
                      (cond (instance? IFn$LO pred) :int64
                            (instance? IFn$DO pred) :float64
                            :else :object)
                      :object)]
      (case pred-type
        :int64
        (loop [idx 0]
          (if (< idx ne)
            (if (.invokePrim ^IFn$LO pred (.getLong ^IMutList coll (unchecked-int idx)))
              (recur (unchecked-inc idx))
              false)
            true))
        :float64
        (loop [idx 0]
          (if (< idx ne)
            (if (.invokePrim ^IFn$DO pred (.getDouble ^IMutList coll (unchecked-int idx)))
              (recur (unchecked-inc idx))
              false)
            true))
        (loop [idx 0]
          (if (< idx ne)
            (if (pred (.get coll (unchecked-int idx)))
              (recur (unchecked-inc idx))
              false)
            true))))
    (cond
      (instance? IFn$LO pred)
      (reduce (fn [acc ^long v]
                (if-not (.invokePrim ^IFn$LO pred v)
                  (reduced false)
                  true))
              true
              coll)
      (instance? IFn$DO pred)
      (reduce (fn [acc ^double v]
                (if-not (.invokePrim ^IFn$DO pred v)
                  (reduced false)
                  true))
              true
              coll)
      :else
      (reduce (fn [acc v]
                (if-not (pred v)
                  (reduced false)
                  true))
              true
              coll))))



(defn cartesian-map
  "Create a new sequence that is the cartesian join of the input sequence passed through f.
  Unlike map, f is passed the arguments as a single persistent vector.  This is to enable much
  higher efficiency in the higher-arity applications.  For tight numeric loops, see [[ham-fisted.hlet/let]].

  The argument vector is mutably updated between function calls so you can't cache it.  Use `(into [] args)`
  or some variation thereof to cache the arguments as is.

```clojure
user> (hamf/sum-fast (lznc/cartesian-map
                      #(h/let [[a b c d](lng-fns %)]
                         (-> (+ a b) (+ c) (+ d)))
                      [1 2 3]
                      [4 5 6]
                      [7 8 9]
                      [10 11 12 13 14]))
3645.0
```"
  ([f] '())
  ([f a] (map #(f [%]) a))
  ([f a b]
   (let [reducer (fn [rfn acc]
                   (let [values (ArrayLists/objectArray (unchecked-int 2))
                         val-seq (ArrayLists/toList values)
                         inner-reducer (fn [acc bb]
                                         (ArrayHelpers/aset values 1 bb)
                                         (rfn acc (f val-seq)))
                         outer-reducer (if (instance? IReduceInit b)
                                         (fn [acc aa]
                                           (ArrayHelpers/aset values 0 aa)
                                           ;;In very tight loops the reduce dispatch can take some time.
                                           (.reduce ^IReduceInit b inner-reducer acc))
                                         (fn [acc aa]
                                           (ArrayHelpers/aset values 0 aa)
                                           (reduce inner-reducer acc b)))]
                     (reduce outer-reducer acc a)))]
     (reify
       Iterable
       (iterator [this]
         (let [a (->iterable a)
               b (->iterable b)
               ia (.iterator a)
               ib (clojure.lang.Box. (.iterator b))
               a-v? (clojure.lang.Box. (.hasNext ia))
               values (ArrayLists/objectArray (unchecked-int 2))
               val-seq (ArrayLists/toList values)]
           (when (.-val a-v?)
             (aset values (unchecked-int 0) (.next ia)))
           (reify Iterator
             (hasNext [this] (and (.-val a-v?)
                                  (.hasNext ^Iterator (.-val ib))))
             (next [this]
               (let [^Iterator iib (.-val ib)
                     _ (aset values (unchecked-int 1) (.next iib))
                     rv (f val-seq)]
                 (when-not (.hasNext iib)
                   (set! (.-val a-v?) (.hasNext ia))
                   (when (.-val a-v?)
                     (aset values (unchecked-int 0) (.next ia))
                     (set! (.-val ib) (.iterator b))))
                 rv)))))
       Seqable
       (seq [this] (LazyChunkedSeq/chunkIteratorSeq (.iterator this)))
       ITypedReduce
       (reduce [this rfn acc]
         (reducer rfn acc)))))
  ([f a b & args]
   (let [args (vec (concat [a b] args))
         nargs (count args)
         dnargs (dec nargs)]
     (reify
       Iterable
       (iterator [this]
         (let [iterables (mapv ->iterable args)
               iterators (ArrayLists/toList (.toArray ^Collection (map #(.iterator ^Iterable %) iterables)))
               values-valid? (clojure.lang.Box. false)
               values (ArrayLists/objectArray (unchecked-int nargs))
               val-seq (ArrayLists/toList values)
               lidx
               (long (loop [idx 0]
                       (if (and (< idx dnargs) (.hasNext ^Iterator (iterators idx)))
                         (do
                           (aset values idx (.next ^Iterator (iterators idx)))
                           (recur (unchecked-inc idx)))
                         idx)))]
           (set! (.-val values-valid?) (== lidx dnargs))
           (reify
             Iterator
             (hasNext [this] (and (.-val values-valid?)
                                  (.hasNext ^Iterator (iterators dnargs))))
             (next [this]
               (let [last-iter ^Iterator (iterators dnargs)
                     _ (aset values dnargs (.next last-iter))
                     rv (f val-seq)]
                 (when-not (.hasNext last-iter)
                   ;;Find the first iterator seaching backward that does have a valid next item
                   (let [nidx (long (loop [idx 1]
                                      (if (< idx nargs)
                                        (let [ridx (- dnargs idx)
                                              ^Iterator iter (iterators ridx)]
                                          (if (.hasNext iter)
                                            ridx
                                            (recur (unchecked-inc idx))))
                                        -1)))]
                     (if (not (== nidx -1))
                       (do
                         (aset values nidx (.next ^Iterator (iterators nidx)))
                         (loop [idx (unchecked-inc nidx)]
                           (when (< idx nargs)
                             (let [iter (.iterator ^Iterable (iterables idx))]
                               (.set iterators idx iter)
                               (when (< idx dnargs)
                                 (aset values idx (.next iter)))
                               (recur (unchecked-inc idx))))))
                       ;;If there are no more valid iterators
                       (set! (.-val values-valid?) false))))
                 rv)))))
       Seqable
       (seq [this] (LazyChunkedSeq/chunkIteratorSeq (.iterator this)))
       ITypedReduce
       (reduce [this rfn acc]
         (let [values (ArrayLists/objectArray (unchecked-int nargs))
               val-seq (ArrayLists/toList values)
               ;;We could cache the reducer but it wouldn't help in most cases as people aren't going to cache the
               ;;cartesian map object.
               reducer (reduce (fn [rrfn ^long idx]
                                 (let [ridx (- dnargs idx)
                                       reduce-target (args ridx)
                                       inner-reducer (if (== idx 0)
                                                       (fn final-reducer [acc v]
                                                         (ArrayHelpers/aset values (unchecked-int ridx) v)
                                                         (rfn acc (f val-seq)))
                                                       (fn intermediate-reducer [acc v]
                                                         (ArrayHelpers/aset values (unchecked-int ridx) v)
                                                         (rrfn acc)))]
                                   (if (instance? IReduceInit reduce-target)
                                     #(.reduce ^IReduceInit reduce-target inner-reducer %)
                                     #(reduce inner-reducer % reduce-target))))
                               nil
                               (range nargs))]
           (reducer acc)))))))

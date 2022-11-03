(ns ham-fisted.lazy-noncaching
  (:require [ham-fisted.iterator :as iterator])
  (:import [ham_fisted Transformables$MapIterable Transformables$FilterIterable
            Transformables$CatIterable Transformables$MapList Transformables$IMapable
            Transformables$SingleMapList Transformables StringCollection ArrayLists
            ArrayImmutList ArrayLists$ObjectArrayList IMutList TypedList LongMutList
            DoubleMutList ReindexList Transformables$IndexedMapper]
           [java.lang.reflect Array]
           [it.unimi.dsi.fastutil.ints IntArrays]
           [java.util RandomAccess Collection Map List Random]
           [clojure.lang RT IPersistentMap IReduceInit IReduce PersistentList])
  (:refer-clojure :exclude [map concat filter repeatedly into-array shuffle object-array
                            remove map-indexed]))


(def ^{:tag ArrayImmutList} empty-vec ArrayImmutList/EMPTY)

(declare concat)

(defn ->collection
  "Ensure an item implements java.util.Collection.  This is inherently true for seqs and any
  implementation of java.util.List but not true for object arrays.  For maps this returns
  the entry set."
  ^Collection [item]
  (cond
    (nil? item) empty-vec
    (instance? Collection item)
    item
    (instance? Map item)
    (.entrySet ^Map item)
    (.isArray (.getClass ^Object item))
    (ArrayLists/toList item)
    (instance? String item)
    (StringCollection. item)
    :else
    (RT/seq item)))


(defn ->reducible
  [item]
  (if (or (instance? IReduceInit item)
          (instance? IReduce item)
          (instance? Iterable item))
    item
    (->collection item)))


(def ^:private obj-ary-cls (Class/forName "[Ljava.lang.Object;"))


(defn object-array
  "Faster version of object-array for eductions, java collections and strings."
  ^objects [item]
  (cond
    (or (nil? item) (number? item))
    (clojure.core/object-array item)
    (instance? obj-ary-cls item)
    item
    (instance? Collection item)
    (.toArray ^Collection item)
    ;;Results of eduction aren't collections but do implement IReduceInit
    (instance? IReduceInit item)
    (let [l (reduce (fn [l v]
                      (.add ^List l v)
                      l)
                    (ArrayLists$ObjectArrayList.)
                    item)]
      (.toArray ^List l))
    (instance? Map item)
    (.toArray (.entrySet ^Map item))
    (instance? String item)
    (.toArray (StringCollection. item))
    (.isArray (.getClass ^Object item))
    (.toArray (ArrayLists/toList item))
    (instance? Iterable item)
    (let [alist (ArrayLists$ObjectArrayList.)]
      (iterator/doiter
       v item
       (.add alist item))
      (.toArray alist))
    :else
    (throw (Exception. (str "Unable to coerce item of type: " (type item)
                            " to an object array")))))


(defn ->random-access
  ^List [item]
  (let [c (->collection item)]
    (if (instance? RandomAccess c)
      c
      (->collection (object-array c)))))


(defn into-array
  ([aseq] (into-array (if-let [item (first aseq)] (.getClass ^Object item) Object) aseq))
  ([ary-type aseq]
   (let [^Class ary-type (or ary-type Object)
         aseq (->collection aseq)
         ^List aseq (if (instance? RandomAccess aseq)
                      aseq
                      (doto (ArrayLists$ObjectArrayList.)
                        (.addAll (->collection aseq))))]
     (if (.isAssignableFrom Object ary-type)
       (.toArray aseq (Array/newInstance ary-type 0))
       (let [retval (Array/newInstance ary-type (.size aseq))
             ^IMutList ml (ArrayLists/toList retval)]
         (.fillRange ml 0 aseq)
         retval))))
  ([ary-type mapfn aseq]
   (let [ary-type (or ary-type Object)]
     (if mapfn
       (into-array ary-type (if (instance? RandomAccess aseq)
                              (Transformables$SingleMapList. mapfn nil aseq)
                              (doto (ArrayLists$ObjectArrayList.)
                                (.addAll (Transformables$MapIterable/createSingle
                                          mapfn nil
                                          (->collection aseq))))))
       (into-array ary-type aseq)))))


(defn map
  ([f arg]
   (cond
     (nil? arg) PersistentList/EMPTY
     (instance? Transformables$IMapable arg)
     (.map ^Transformables$IMapable arg f)
     (instance? RandomAccess arg)
     (Transformables$MapList/create f nil (into-array List (vector arg)))
     (.isArray (.getClass ^Object arg))
     (Transformables$MapList/create f nil (into-array List (vector (ArrayLists/toList arg))))
     :else
     (Transformables$MapIterable. f nil (into-array Iterable (vector (->collection arg))))))
  ([f arg & args]
   (let [args (concat [arg] args)]
     (if (every? #(instance? RandomAccess %) args)
       (Transformables$MapList/create f nil (into-array List args))
       (Transformables$MapIterable. f nil (into-array Iterable ->collection args))))))


(defn map-indexed
  [map-fn coll]
  (if (instance? RandomAccess coll)
    (let [^List coll coll]
      (reify
        IMutList
        (size [this] (.size coll))
        (get [this idx] (map-fn idx (.get coll idx)))
        (subList [this sidx eidx]
          (map-indexed map-fn (.subList coll sidx eidx)))
        Transformables$IMapable
        (map [this mfn] (map-indexed (fn [idx v]
                                       (-> (map-fn idx v)
                                           (mfn)))
                                     coll))))
    (Transformables$IndexedMapper. map-fn coll nil)))


(defn concat
  ([] PersistentList/EMPTY)
  ([a] (if a a PersistentList/EMPTY))
  ([a & args]
   (if (instance? Transformables$IMapable a)
    (.cat ^Transformables$IMapable a args)
    (Transformables$CatIterable. (cons a args)))))


(defn filter
  [pred coll]
  (cond
    (nil? coll) PersistentList/EMPTY
    (instance? Transformables$IMapable coll)
    (.filter ^Transformables$IMapable coll pred)
    :else
    (Transformables$FilterIterable. pred nil (Transformables/toIterable coll))))


(defn remove
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns logical false. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  [pred coll]
  (filter (complement pred) coll))


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


(defn repeatedly
  "When called with one argument, produce infinite list of calls to v.
  When called with two arguments, produce a random access list of length n of calls to v."
  ([f] (clojure.core/repeatedly f))
  (^List [n f] (repeatedly n f nil))
  (^List [n f opts] (make-readonly-list n idx (f))))


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

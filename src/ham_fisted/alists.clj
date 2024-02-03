(ns ham-fisted.alists
  "Generic primitive array backed array-lists.  The pure clojure implementations are a bit
  slower than the java ones but *far* less code so these are used for the
  less-frequently-used primive datatypes - byte, short, char, and float."
  (:require [ham-fisted.iterator :as iterator]
            [ham-fisted.protocols :as protocols])
  (:import [ham_fisted ArrayLists ArrayLists$ILongArrayList ArrayLists$IDoubleArrayList
            Transformables ArrayHelpers Casts IMutList IFnDef$OLO IFnDef$ODO
            ArrayLists$ObjectArrayList ArrayLists$ObjectArraySubList
            ArrayLists$ByteArraySubList ArrayLists$ShortArraySubList ArrayLists$CharArraySubList
            ArrayLists$IntArraySubList ArrayLists$IntArrayList
            ArrayLists$LongArraySubList ArrayLists$LongArrayList
            ArrayLists$FloatArraySubList ArrayLists$BooleanArraySubList
            ArrayLists$DoubleArraySubList ArrayLists$DoubleArrayList
            ArrayLists$IArrayList ChunkedList Reductions]
           [clojure.lang IPersistentMap IReduceInit RT]
           [java.util Arrays RandomAccess List]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(defmacro implement-tostring-print
  [tname]
  (require '[ham-fisted.print])
  `(ham-fisted.print/implement-tostring-print ~tname))


(defn- add-long-reduce
  [list c]
  (Reductions/serialReduction
   (reify IFnDef$OLO
     (invokePrim [this acc v]
       (.addLong ^IMutList acc v)
       acc))
   list
   c))

(defn- add-double-reduce
  [list c]
  (Reductions/serialReduction
   (reify IFnDef$ODO
     (invokePrim [this acc v]
       (.addDouble ^IMutList acc v)
       acc))
   list
   c))

(defmacro make-prim-array-list
  [lname ary-tag iface getname setname addname set-cast-fn get-cast-fn obj-cast-fn
   add-all-reduce]
  `(deftype ~lname [~(with-meta 'data {:unsynchronized-mutable true
                                       :tag ary-tag})
                    ~(with-meta 'n-elems {:unsynchronized-mutable true
                                          :tag 'long})
                    ~(with-meta 'm {:tag 'IPersistentMap})]
     ~'Object
     (hashCode [this#] (.hasheq this#))
     (equals [this# other#] (.equiv this# other#))
     (toString [this#] (Transformables/sequenceToString this#))
     ~iface
     (meta [this#] ~'m)
     (withMeta [this# newm#] (with-meta (.subList this# 0 ~'n-elems) newm#))
     (cloneList [this#] (~(symbol (str (name lname) ".")) (.copyOf this# ~'n-elems)
                         ~'n-elems ~'m))
     (clone [this#] (.cloneList this#))
     (size [this#] (unchecked-int ~'n-elems))
     (~getname [this# idx#] (~get-cast-fn (aget ~'data (ArrayLists/checkIndex idx# ~'n-elems))))
     (get [this# idx#] (aget ~'data (ArrayLists/checkIndex idx# ~'n-elems)))
     (~setname [this# idx# v#] (ArrayHelpers/aset ~'data (ArrayLists/checkIndex idx# ~'n-elems) (~set-cast-fn v#)))
     (set [this# idx# val#]
       (let [idx# (ArrayLists/checkIndex idx# ~'n-elems)
             rv# (aget ~'data idx#)]
         (ArrayHelpers/aset ~'data idx# (~set-cast-fn val#))
         rv#))
     (subList [this# sidx# eidx#]
       (ChunkedList/sublistCheck sidx# eidx# ~'n-elems)
       (ArrayLists/toList ~'data sidx# eidx# ~'m))
     (ensureCapacity [this# newlen#]
       (when (> newlen# (alength ~'data))
         (set! ~'data (.copyOf this# (ArrayLists/newArrayLen newlen#))))
       ~'data)
     (~addname [this# v#]
       (let [curlen# ~'n-elems
             newlen# (unchecked-inc ~'n-elems)
             ~(with-meta 'b {:tag ary-tag}) (.ensureCapacity this# newlen#)]
         (ArrayHelpers/aset ~'b curlen# (~set-cast-fn v#))
         (set! ~'n-elems newlen#)))
     (add [this# idx# obj#]
       (ArrayLists/checkIndex idx# ~'n-elems)
       (if (== idx# ~'n-elems)
         (.add this# obj#)
         (let [bval# (~set-cast-fn (~obj-cast-fn obj#))
               curlen# ~'n-elems
               newlen# (unchecked-inc curlen#)
               ~(with-meta 'd {:tag ary-tag}) (.ensureCapacity this# newlen#)]
           (System/arraycopy ~'d idx# ~'d (unchecked-inc idx#) (- curlen# idx#))
           (ArrayHelpers/aset ~'d idx# bval#)
           (set! ~'n-elems newlen#))))
     (addAllReducible [this# c#]
       (let [sz# (.size this#)]
         (if (instance? RandomAccess c#)
           (do
             (when-not (== 0 (.size ^List c#))
               (let [~(with-meta 'c {:tag 'List}) c#
                     curlen# ~'n-elems
                     newlen# (+ curlen# (.size ~'c))]
                 (.ensureCapacity this# newlen#)
                 (set! ~'n-elems newlen#)
                 (.fillRangeReducible this# curlen# ~'c))))
           (~add-all-reduce this# c#))
         (not (== sz# ~'n-elems))))
     (fillRangeReducible [this# sidx# c#]
       (.fillRangeReducible (.subList this# 0 (.size this#)) sidx# c#))
     (removeRange [this# sidx# eidx#]
       (ArrayLists/checkIndexRange ~'n-elems (long sidx#) (long eidx#))
       (System/arraycopy ~'data sidx# ~'data eidx# (- ~'n-elems eidx#))
       (set! ~'n-elems (- ~'n-elems (- eidx# sidx#))))
     (sort [~'this c#] (.sort ~(with-meta '(.subList this 0 n-elems)  {:tag 'IMutList}) c#))
     (sortIndirect [~'this c#] (.sortIndirect ~(with-meta '(.subList this 0 n-elems) {:tag 'IMutList}) c#))
     (shuffle [~'this r#] (.shuffle ~(with-meta '(.subList this 0 n-elems) {:tag 'IMutList}) r#))
     (reduce [this# rfn# init#]
       (reduce rfn# init# (.subList this# 0 ~'n-elems)))
     (binarySearch [~'this v# c#] (.binarySearch ~(with-meta '(.subList this 0 n-elems) {:tag 'IMutList}) v# c#))
     (fill [this# sidx# eidx# v#]
       (ArrayLists/checkIndexRange ~'n-elems (long sidx#) (long eidx#))
       (Arrays/fill ~'data sidx# eidx# (~set-cast-fn (~obj-cast-fn v#))))
     (copyOfRange [this# sidx# eidx#]
       (Arrays/copyOfRange ~'data sidx# eidx#))
     (copyOf [this# len#]
       (Arrays/copyOf ~'data len#))
     (getArraySection [this#]
       (ham_fisted.ArraySection. ~'data 0 ~'n-elems))))


(def array-lists
  [(make-prim-array-list ByteArrayList bytes ArrayLists$ILongArrayList getLong setLong addLong
                         RT/byteCast unchecked-long Casts/longCast add-long-reduce)
   (make-prim-array-list ShortArrayList shorts ArrayLists$ILongArrayList getLong setLong addLong
                         RT/shortCast unchecked-long Casts/longCast add-long-reduce)
   (make-prim-array-list CharArrayList chars ArrayLists$ILongArrayList getLong setLong addLong
                         Casts/charCast Casts/charLongCast Casts/charLongCast add-long-reduce)
   (make-prim-array-list FloatArrayList floats ArrayLists$IDoubleArrayList getDouble setDouble
                         addDouble float unchecked-double Casts/doubleCast add-double-reduce)


   (deftype BooleanArrayList [^{:unsynchronized-mutable true
                                :tag booleans} data
                              ^{:unsynchronized-mutable true
                                :tag long} n-elems
                              ^IPersistentMap m]
     Object
     (hashCode [this] (.hasheq this))
     (equals [this other] (.equiv this other))
     (toString [this] (Transformables/sequenceToString this))
     ArrayLists$IArrayList
     (cloneList [this] (BooleanArrayList. (.copyOf this n-elems) n-elems m))
     (meta [this] m)
     (withMeta [this newm] (with-meta (.subList this 0 n-elems) newm))
     (size [this] (unchecked-int n-elems))
     (get [this idx] (aget data (ArrayLists/checkIndex idx n-elems)))
     (set [this idx v]
       (let [retval (.get this idx)]
         (ArrayHelpers/aset data (ArrayLists/checkIndex idx n-elems)
                            (Casts/booleanCast v))
         retval))
     (subList [this sidx eidx]
       (ChunkedList/sublistCheck sidx eidx n-elems)
       (ArrayLists/toList data sidx eidx m))
     (ensureCapacity [this newlen]
       (when (> newlen (alength data))
         (set! data ^booleans (.copyOf this (ArrayLists/newArrayLen newlen))))
       data)
     (add [this v]
       (let [curlen n-elems
             newlen (unchecked-inc n-elems)
             ^booleans b (.ensureCapacity this newlen)]
         (ArrayHelpers/aset b curlen (Casts/booleanCast v))
         (set! n-elems newlen)
         true))
     (add [this idx obj]
       (if (== idx n-elems)
         (.add this obj)
         (do
           (ArrayLists/checkIndex idx n-elems)
           (let [bval (Casts/booleanCast obj)
                 curlen n-elems
                 newlen (unchecked-inc curlen)
                 ^booleans d (.ensureCapacity this newlen)]
             (System/arraycopy d idx d (unchecked-inc idx) (- curlen idx))
             (ArrayHelpers/aset d idx bval)
             (set! n-elems newlen)))))
     (addAllReducible [this c]
       (let [sz (.size this)]
         (if (instance? RandomAccess c)
           (let [^List c c
                 curlen n-elems
                 newlen (+ curlen (.size c))]
             (.ensureCapacity this newlen)
             (set! n-elems newlen)
             (.fillRangeReducible this curlen c))
           (Reductions/serialReduction (fn [acc v]
                                         (.add ^List acc v)
                                         v)
                                       this
                                       c))
         (not (== sz n-elems))))
     (fillRangeReducible [this sidx c]
       (.fillRangeReducible (.subList this 0 (.size this)) sidx c))
     (removeRange [this sidx eidx]
       (ArrayLists/checkIndexRange n-elems sidx eidx)
       (System/arraycopy data sidx data eidx (- n-elems eidx))
       (set! n-elems (- n-elems (- eidx sidx))))
     (sort [this c] (.sort (.subList this 0 n-elems) c))
     (sortIndirect [this c] (.sortIndirect ^IMutList (.subList this 0 n-elems) c))
     (shuffle [this r] (.shuffle ^IMutList (.subList this 0 n-elems) r))
     (binarySearch [this v c] (.binarySearch ^IMutList (.subList this 0 n-elems) v c))
     (fill [this sidx eidx v]
       (ArrayLists/checkIndexRange n-elems (long sidx) (long eidx))
       (Arrays/fill data sidx eidx (Casts/booleanCast v)))
     (copyOfRange [this sidx eidx]
       (Arrays/copyOfRange data sidx eidx))
     (copyOf [this len]
       (Arrays/copyOf data len))
     (getArraySection [this]
       (ham_fisted.ArraySection. data 0 n-elems)))])


(implement-tostring-print ByteArrayList)
(implement-tostring-print ShortArrayList)
(implement-tostring-print CharArrayList)
(implement-tostring-print FloatArrayList)
(implement-tostring-print BooleanArrayList)


(defn- ladd
  [^IMutList m v]
  (.add m v)
  m)

(defn- lmerge
  [^IMutList l v]
  (.addAllReducible l v)
  l)


#_(extend-protocol protocols/Reducer
  ArrayLists$ObjectArrayList
  (->init-val-fn [item] #(ArrayLists$ObjectArrayList.))
  (->rfn [item] ladd)
  ArrayLists$IntArrayList
  (->init-val-fn [item] #(ArrayLists$IntArrayList.))
  (->rfn [item] ladd)
  ArrayLists$LongArrayList
  (->init-val-fn [item] #(ArrayLists$LongArrayList.))
  (->rfn [item] ladd)
  ArrayLists$DoubleArrayList
  (->init-val-fn [item] #(ArrayLists$DoubleArrayList.))
  (->rfn [item] ladd))


#_(extend-protocol protocols/ParallelReducer
  ArrayLists$ObjectArrayList
  (->merge-fn [l] lmerge)
  ArrayLists$IntArrayList
  (->merge-fn [l] lmerge)
  ArrayLists$LongArrayList
  (->merge-fn [l] lmerge)
  ArrayLists$DoubleArrayList
  (->merge-fn [l] lmerge))



(defmacro extend-array-types
  []
  `(do
     ~@(->>
        {'(Class/forName "[Z") ['bytes 'BooleanArrayList.]
         '(Class/forName "[B") ['bytes 'ByteArrayList.]
         '(Class/forName "[S") ['shorts 'ShortArrayList.]
         '(Class/forName "[C") ['chars 'CharArrayList.]
         '(Class/forName "[I") ['ints 'ArrayLists$IntArrayList.]
         '(Class/forName "[J") ['longs 'ArrayLists$LongArrayList.]
         '(Class/forName "[F") ['floats 'FloatArrayList.]
         '(Class/forName "[D") ['doubles 'ArrayLists$DoubleArrayList.]
         '(Class/forName "[Ljava.lang.Object;") ['objects 'ArrayLists$ObjectArrayList.]}
        (map (fn [[cls-type [hint growable-cons]]]
               `(extend ~cls-type
                  protocols/WrapArray
                  {:wrap-array (fn [~(with-meta (symbol "ary") {:tag hint})]
                                 (ArrayLists/toList ~'ary))
                   :wrap-array-growable (fn [~(with-meta (symbol "ary") {:tag hint})
                                             ~(with-meta (symbol "ptr") {:tag 'long})]
                                          (~growable-cons ~'ary ~'ptr nil))}))))))


(extend-array-types)

(def ^:private obj-ary-cls (Class/forName "[Ljava.lang.Object;"))

(defn wrap-array
  "Wrap an array with an implementation of IMutList"
  ^IMutList [ary]
  (if (instance? obj-ary-cls ary)
    (ArrayLists/toList ^objects ary)
    (protocols/wrap-array ary)))


(defn wrap-array-growable
  "Wrap an array with an implementation of IMutList that supports add and addAllReducible.
  'ptr is the numeric put ptr, defaults to the array length.  Pass in zero for a preallocated
  but empty growable wrapper."
  (^IMutList [ary ptr]
   (if (instance? obj-ary-cls ary)
     (ArrayLists$ObjectArrayList. ary ptr nil)
     (protocols/wrap-array-growable ary ptr)))
  (^IMutList [ary] (wrap-array-growable ary (java.lang.reflect.Array/getLength ary))))

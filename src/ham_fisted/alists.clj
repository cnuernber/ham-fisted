(ns ham-fisted.alists
  "Generic primitive array backed array-lists.  The pure clojure implementations are a bit
  slower than the java ones but *far* less code so these are used for the
  less-frequently-used primive datatypes - byte, short, char, and float."
  (:require [ham-fisted.iterator :as iterator]
            [ham-fisted.print :as pp])
  (:import [ham_fisted ArrayLists ArrayLists$ILongArrayList ArrayLists$IDoubleArrayList
            ArrayLists$IBooleanArrayList Transformables ArrayHelpers Casts IMutList
            ArraySection
            ArrayLists$ObjectArrayList ArrayLists$ObjectArraySubList
            ArrayLists$ByteArraySubList ArrayLists$ShortArraySubList ArrayLists$CharArraySubList
            ArrayLists$IntArraySubList ArrayLists$IntArrayList
            ArrayLists$LongArraySubList ArrayLists$LongArrayList
            ArrayLists$FloatArraySubList ArrayLists$BooleanArraySubList
            ArrayLists$DoubleArraySubList ArrayLists$DoubleArrayList]
           [clojure.lang IPersistentMap IReduceInit]
           [java.util Arrays RandomAccess List]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(pp/implement-tostring-print ArraySection)

(defmacro make-prim-array-list
  [lname ary-tag iface getname setname addname set-cast-fn get-cast-fn obj-cast-fn]
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
     (size [this#] (unchecked-int ~'n-elems))
     (~getname [this# idx#] (~get-cast-fn (aget ~'data (ArrayLists/checkIndex idx# ~'n-elems))))
     (get [this# idx#] (aget ~'data (ArrayLists/checkIndex idx# ~'n-elems)))
     (~setname [this# idx# v#] (ArrayHelpers/aset ~'data (ArrayLists/checkIndex idx# ~'n-elems) (~set-cast-fn v#)))
     (subList [this# sidx# eidx#]
       (ArrayLists/checkIndexRange ~'n-elems sidx# eidx#)
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
             (let [~(with-meta 'c {:tag 'List}) c#
                   curlen# ~'n-elems
                   newlen# (+ curlen# (.size ~'c))]
               (.ensureCapacity this# newlen#)
               (set! ~'n-elems newlen#)
               (.fillRange this# curlen# ~'c)))
           (Transformables/longReduce (fn [lhs# ^long rhs#]
                                        (.addLong ^IMutList lhs# rhs#)
                                        lhs#)
                                      this#
                                      c#))
         (not (== sz# ~'n-elems))))
     (removeRange [this# sidx# eidx#]
       (ArrayLists/checkIndexRange ~'n-elems sidx# eidx#)
       (System/arraycopy ~'data sidx# ~'data eidx# (- ~'n-elems eidx#))
       (set! ~'n-elems (- ~'n-elems (- eidx# sidx#))))
     (sort [~'this c#] (.sort ~(with-meta '(.subList this 0 n-elems)  {:tag 'IMutList}) c#))
     (sortIndirect [~'this c#] (.sortIndirect ~(with-meta '(.subList this 0 n-elems) {:tag 'IMutList}) c#))
     (shuffle [~'this r#] (.shuffle ~(with-meta '(.subList this 0 n-elems) {:tag 'IMutList}) r#))
     (binarySearch [~'this v# c#] (.binarySearch ~(with-meta '(.subList this 0 n-elems) {:tag 'IMutList}) v# c#))
     (fill [this# sidx# eidx# v#]
       (ArrayLists/checkIndexRange ~'n-elems sidx# eidx#)
       (Arrays/fill ~'data sidx# eidx# (~set-cast-fn (~obj-cast-fn v#))))
     (copyOfRange [this# sidx# eidx#]
       (Arrays/copyOfRange ~'data sidx# eidx#))
     (copyOf [this# len#]
       (Arrays/copyOf ~'data len#))
     (getArraySection [this#]
       (ArraySection. ~'data 0 ~'n-elems))))


(make-prim-array-list ByteArrayList bytes ArrayLists$ILongArrayList getLong setLong addLong
                      byte unchecked-long Casts/longCast)

(make-prim-array-list ShortArrayList shorts ArrayLists$ILongArrayList getLong setLong addLong
                      short unchecked-long Casts/longCast)
(make-prim-array-list CharArrayList chars ArrayLists$ILongArrayList getLong setLong addLong
                      char Casts/longCast Casts/longCast)
(make-prim-array-list FloatArrayList floats ArrayLists$IDoubleArrayList getDouble setDouble
                      addDouble float unchecked-double Casts/doubleCast)

(make-prim-array-list BooleanArrayList booleans ArrayLists$IBooleanArrayList getBoolean
                      setBoolean addBoolean Casts/booleanCast Casts/booleanCast
                      Casts/booleanCast)


(pp/implement-tostring-print ShortArrayList)
(pp/implement-tostring-print CharArrayList)
(pp/implement-tostring-print FloatArrayList)
(pp/implement-tostring-print BooleanArrayList)

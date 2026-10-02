(ns ham-fisted.array-list-test
  (:require [clojure.test :refer [deftest is testing]]
            [ham-fisted.api :as hamf])
  (:import [java.util List]
           [java.util.function Predicate Consumer]
           [ham_fisted ArrayLists MutTreeList]))


(def growable-lists
  {:object hamf/object-array-list
   :int hamf/int-array-list
   :long hamf/long-array-list
   :double hamf/double-array-list
   :byte hamf/byte-array-list
   :short hamf/short-array-list
   :float hamf/float-array-list})


(defn- ->longs [l] (mapv long l))


(deftest growable-list-remove
  (doseq [[k ctor] growable-lists]
    (testing (name k)
      (let [^List l (ctor (range 6))]
        (.remove l (int 0))
        (is (= [1 2 3 4 5] (->longs l))))
      (let [^List l (ctor (range 6))]
        (.removeRange ^ham_fisted.IMutList l 1 3)
        (is (= [0 3 4 5] (->longs l))))
      (let [^List l (ctor (range 6))]
        (.removeIf l (reify Predicate (test [_ v] (odd? (long v)))))
        (is (= [0 2 4] (->longs l))))
      (let [^List l (ctor (range 3))]
        (.add l (int 3) 3)
        (.add l (int 0) 9)
        (is (= [9 0 1 2 3] (->longs l)))))))


(deftest boolean-list-remove
  (let [^List l (hamf/boolean-array-list [true false false true])]
    (.removeRange ^ham_fisted.IMutList l 1 3)
    (is (= [true true] (vec l)))
    (.add l (int 2) false)
    (is (= [true true false] (vec l)))))


(deftest list-iterator-remove-and-add
  (let [^List l (hamf/object-array-list [1 1 2 3 3])
        iter (.listIterator l)]
    (while (.hasNext iter)
      (when (odd? (.next iter)) (.remove iter)))
    (is (= [2] (vec l))))
  (let [^List l (hamf/object-array-list [1 3])
        iter (.listIterator l)]
    (.next iter)
    (.add iter 2)
    (is (= 3 (.next iter)))
    (is (= [1 2 3] (vec l)))))


(deftest add-range
  (let [l (hamf/int-array-list (range 4))]
    (.addRange ^ham_fisted.ArrayLists$IntArrayList l 1 3 9)
    (is (= [0 9 9 1 2 3] (->longs l))))
  (let [l (hamf/long-array-list (range 4))]
    (.addRange ^ham_fisted.ArrayLists$LongArrayList l 4 6 9)
    (is (= [0 1 2 3 9 9] (->longs l)))))


(deftest growable-list-meta
  (doseq [ctor [hamf/object-array-list hamf/int-array-list hamf/long-array-list
                hamf/double-array-list]]
    (is (= {:a 1} (meta (.cloneList ^ham_fisted.IMutList (with-meta (ctor [1 2]) {:a 1})))))))


(deftest sublist-with-meta-and-move
  (let [s (.subList ^List (ArrayLists/toList (byte-array (range 10))) 2 5)]
    (is (= [2 3 4] (->longs (with-meta s {:a 1})))))
  (let [data (long-array (range 10))
        s (.subList ^List (ArrayLists/toList data) 4 8)]
    (.move ^ham_fisted.ArrayLists$ArrayOwner s 0 1 2)
    (is (= [4 4 5 7] (->longs s)))
    (is (= [0 1 2 3] (->longs (take 4 data))))))


(deftest sublist-sort-indirect
  (doseq [data [(int-array [9 8 7 3 1 2]) (long-array [9 8 7 3 1 2])
                (double-array [9 8 7 3 1 2])]]
    (let [s (.subList ^List (ArrayLists/toList data) 3 6)]
      (is (= [1 2 0] (vec (.sortIndirect ^ham_fisted.IMutList s nil)))))))


(deftest unsafe-immut
  (is (= [1 2] (.unsafeImmut ^ham_fisted.ArrayLists$IArrayList (hamf/object-array-list [1 2]))))
  (is (= [1 2] (mapv long (.unsafeImmut ^ham_fisted.ArrayLists$ILongArrayList
                                        (hamf/long-array-list [1 2]))))))


(deftest const-list-bounds
  (let [l (hamf/repeat 3 :a)]
    (is (thrown? IndexOutOfBoundsException (nth l 10)))
    (is (= :nf (nth l 10 :nf)))
    (.sort ^List l nil)
    (is (= [:a :a :a] (vec l))))
  (is (thrown? IndexOutOfBoundsException (nth (hamf/repeat 3 1) 5)))
  (is (thrown? IndexOutOfBoundsException (nth (hamf/repeat 3 1.0) 5))))


(deftest reindex-sublist-bounds
  (is (thrown? IndexOutOfBoundsException
               (.subList ^List (hamf/reindex [1 2 3] (int-array [2 1 0])) 0 5))))


(deftest unsupported-insert-throws
  (let [m (MutTreeList.)]
    (.add m 1)
    (.add m (int 1) 2)
    (is (= [1 2] (vec m)))
    (is (thrown? UnsupportedOperationException (.add m (int 0) :x)))
    (is (thrown? UnsupportedOperationException (.remove m (int 0))))))


(deftest spliterator-split-after-advance
  (let [s (.spliterator ^List (hamf/object-array-list (range 10)))
        seen (java.util.ArrayList.)
        c (reify Consumer (accept [_ v] (.add seen v)))]
    (dotimes [_ 6] (.tryAdvance s c))
    (is (= 4 (.estimateSize s)))
    (when-let [r (.trySplit s)] (.forEachRemaining r c))
    (.forEachRemaining s c)
    (is (= (range 10) (sort seen)))))

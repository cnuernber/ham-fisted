(ns ham-fisted.java-misc-test
  (:require [clojure.test :refer [deftest is testing]]
            [ham-fisted.api :as hamf])
  (:import [ham_fisted StringCollection ParallelOptions ParallelOptions$CatParallelism CljHash
            MapFn MergeIterator Reductions IFnDef$L BlockSplitBloomFilter Sum]
           [java.util.concurrent ForkJoinPool]
           [java.util.function DoubleSupplier LongSupplier]))


(deftest string-collection-reduce
  (is (= [\a \b \c] (reduce conj [] (StringCollection. "abc"))))
  (is (= \c (reduce (fn [_ v] v) (StringCollection. "abc")))))


(deftest set-bulk-ops
  (let [s (hamf/mut-set [1 2 3])]
    (is (.containsAll s [1 2]))
    (is (not (.containsAll s [1 4])))
    (is (true? (.addAll s [4])))
    (is (false? (.addAll s [4])))))


(deftest parallel-options-min-n
  (let [o (.minN (ParallelOptions. 1 10 true (ForkJoinPool/commonPool) 4
                                   ParallelOptions$CatParallelism/SEQWISE 100 true 5)
                 3)]
    (is (= 3 (.minN o)))
    (is (.unmergedResult o))
    (is (= 5 (.nLookahead o)))))


(deftest map-equiv-nil-values
  (is (not (CljHash/mapEquiv {:a nil} {:b nil})))
  (is (CljHash/mapEquiv {:a nil} {:a nil}))
  (is (not= (hamf/immut-map {:a nil}) {:b nil}))
  (is (= (hamf/immut-map {:a nil}) {:a nil})))


(deftest entry-set-contains
  (let [es (.entrySet (hamf/mut-map {:a 1 :b nil}))]
    (is (.contains es (clojure.lang.MapEntry. :a 1)))
    (is (not (.contains es (clojure.lang.MapEntry. :a 2))))
    (is (.contains es (clojure.lang.MapEntry. :b nil)))
    (is (not (.contains es (clojure.lang.MapEntry. :c nil))))))


(deftest map-fn-apply
  (is (= 4 (apply (MapFn/create + inc) [1 2])))
  (is (= 1 (apply (MapFn/create + inc) []))))


(deftest ranges-match-clojure
  (doseq [[s e st] [[0 10 3] [10 0 -3] [10 0 1] [0 10 1] [0 0 1] [5 5 -1] [-7 7 4]]]
    (is (= (range s e st) (vec (hamf/range s e st))) (str [s e st])))
  (doseq [[s e st] [[0.0 1.0 0.3] [1.0 0.0 -0.25] [1.0 0.0 0.25] [0.0 2.0 0.5]]]
    (is (= (count (range s e st)) (count (hamf/range s e st))) (str [s e st])))
  ;;Elements are start + idx*step so this is exactly 10 - clojure.core/range accumulates
  ;;rounding error by repeated addition and yields 11.
  (is (= 10 (count (hamf/range 0.0 1.0 0.1))))
  (let [r (hamf/range 0 100 7)]
    (is (= (subvec (vec (range 0 100 7)) 3 9) (vec (.subList ^java.util.List r 3 9)))))
  (let [r (hamf/range 0.0 10.0 0.1)]
    (is (= 30 (count (.subList ^java.util.List r 10 40))))))


(deftest merge-iterators-are-stable
  (let [cmp (fn [a b] (compare (first a) (first b)))
        run (fn [n] (vec (iterator-seq
                          (MergeIterator/createMergeIterator
                           (mapv (fn [i] (.iterator [[1 i] [2 i]])) (range n))
                           cmp))))]
    (doseq [n [2 3 9]]
      (is (= (concat (map #(vector 1 %) (range n)) (map #(vector 2 %) (range n))) (run n))
          (str n " iterators")))))


(deftest reductions-edge-cases
  (is (= 0 (Reductions/iterReduce [] +)))
  (is (nil? (Reductions/reduceReducibles []))))


(deftest ifndef-long-supplier
  (let [f (reify IFnDef$L (invokePrim [_] 5))]
    (is (= 5 (.getAsLong ^LongSupplier f)))
    (is (= 5.0 (.getAsDouble ^DoubleSupplier f)))))


(deftest bloom-filter
  (is (zero? (mod (BlockSplitBloomFilter/optimalNumOfBits 10 0.01) 256)))
  (let [bf (BlockSplitBloomFilter. 4096)
        hashes (vec (repeatedly 500 #(.nextLong (java.util.concurrent.ThreadLocalRandom/current))))]
    (doseq [h hashes] (.insertHash bf h))
    ;;Concurrent lookups must never produce false negatives.
    (is (every? true? (apply concat (pmap (fn [_] (mapv #(.findHash bf %) hashes)) (range 8)))))))


(deftest sum-compensation
  (let [s (Sum.)]
    (doseq [v (cons 1.0 (repeat 1000 1e-16))] (.accept s (double v)))
    (is (= (+ 1.0 1e-13) (:sum @s)))))

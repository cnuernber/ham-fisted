(ns ham-fisted.api-test
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.api :as hamf]
            [ham-fisted.lazy-noncaching :as lznc])
  (:import [java.util BitSet]))



(deftest parallism-primitives-pass-errors
  (is (thrown? Exception (count (hamf/upmap
                                 (fn [^long idx]
                                   (when (== idx 77) (throw (Exception. "Error!!"))) idx)
                                 (range 100)))))
  (is (thrown? Exception (count (hamf/pmap (fn [^long idx]
                                             (when (== idx 77) (throw (Exception. "Error!!"))) idx)
                                           (range 100)))))
  (is (thrown? Exception (hamf/upgroups (fn [^long sidx ^long eidx]
                                          (when (>= sidx 70)
                                            (throw (Exception. "Error!!"))) sidx))))
  (is (thrown? Exception (hamf/pgroups (fn [^long sidx ^long eidx]
                                         (when (>= sidx 70)
                                           (throw (Exception. "Error!!"))) sidx)))))


(deftest group-by-nil
  (is (= {} (hamf/group-by :a nil)))
  (is (= {} (hamf/group-by-reduce :a + + + nil)))
  (is (= {} (hamf/group-by :a {})))
  (is (= {} (hamf/group-by-reduce :a + + + {})))
  )


(deftest map-filter-concat-nil
  (is (= (map + nil) (lznc/map + nil)))
  (is (= (filter + nil) (lznc/filter + nil)))
  (is (= (concat) (lznc/concat)))
  (is (= (concat nil) (lznc/concat nil))))


(deftest conj-with-friends
  (is (= (conj (map inc (list 1 2 3 4)) 4)
         (conj (lznc/map inc (list 1 2 3 4)) 4)))
  (is (= (conj (filter even? (list 1 2 3 4)) 4)
         (conj (lznc/filter even? (list 1 2 3 4)) 4)))
  (is (= (conj (concat [1 2 3] [4 5 6]) 4)
         (conj (lznc/concat [1 2 3] [4 5 6]) 4)))
  (is (= (conj (map-indexed + [1 2 3]) 4)
         (conj (lznc/map-indexed + (apply list [1 2 3])) 4)))
  (is (= (conj (map-indexed + nil) 4)
         (conj (lznc/map-indexed + nil) 4))))


(deftest empty-seq-preduce
  (is (== 0.0 (hamf/sum (list))))
  (is (== 19900.0 (hamf/sum (range 200))))
  (is (= 1 (hamf/preduce (constantly 1) + + nil)))
  (is (= 1 (hamf/preduce (constantly 1) + + (list)))))


(deftest group-by-reduce-large-n
  (is (= 113 (count (hamf/group-by #(rem (unchecked-long %1) 113) (range 10000)))))
  (is (= 337 (count (hamf/group-by-reduce #(rem (unchecked-long %1) 337)
                                          +
                                          +
                                          +
                                          (range 10000))))))


(deftest compare-seq-with-nonseq
  (is (not (= (hamf/vec (range 10)) :a))))


(deftest BitsetSet
  (let [bs (doto (BitSet.)
             (.set 1)
             (.set 10))]
    (is (= [1 10] (hamf/->random-access (hamf/int-array bs))))
    (is (= [1.0 10.0] (->> bs
                           (lznc/map (hamf/long-to-double-function v (double v)))
                           (hamf/vec))))
    (is (not (nil? (hamf/->collection bs))))))


(deftest char-array-reduction
  (let [cv (hamf/char-array [20 30])]
    (is (= [(char 20) (char 30)]
           (reduce conj [] cv))))
  (let [cv (hamf/char-array "hey")]
    (is (= [\h \e \y] (reduce conj [] cv)))))


(deftest java-maps-are-iterable
  (is (not (nil? (hamf/->collection (hamf/java-hashmap {:a 1 :b 2}))))))


(deftest set-add-long-val
  (let [alist (hamf/long-array-list)]
    (.add alist Long/MAX_VALUE)
    (is (= [Long/MAX_VALUE] alist))
    (.set alist 0 Long/MAX_VALUE)
    (is (= [Long/MAX_VALUE] alist))
    (is (= [Long/MAX_VALUE] (vec alist)))
    (let [sl (.subList alist 0 1)]
      (is (= [Long/MAX_VALUE] sl))
      (is (= [Long/MAX_VALUE] (vec sl))))))

(deftest tostring-empty-range
  (is (= "[]" (.toString (hamf/range 0))))
  (is (= "[]" (.toString (hamf/range 0.0)))))

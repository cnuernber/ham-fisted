(ns ham-fisted.api-test
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.api :as hamf]
            [ham-fisted.lazy-noncaching :as lznc]))



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
  (is (= {} (hamf/group-by-reduce :a + nil nil)))
  (is (= {} (hamf/group-by :a {})))
  (is (= {} (hamf/group-by-reduce :a + nil {})))
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
         (conj (lznc/concat [1 2 3] [4 5 6]) 4))))

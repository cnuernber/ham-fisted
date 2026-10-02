(ns ham-fisted.clj-collections-test
  (:require [clojure.test :refer [deftest is testing]]
            [ham-fisted.api :as hamf]
            [ham-fisted.language :as lang]
            [ham-fisted.set :as hset]
            [ham-fisted.impl :as impl]
            [ham-fisted.iterator :as hiter]
            [ham-fisted.reduce :as hamf-rf]
            [ham-fisted.lazy-caching :as lzc]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.alists :as alists])
  (:import [java.util BitSet]
           [ham_fisted Sum]))


(deftest obj-ary-all-arities
  (doseq [n (range 17)]
    (is (= (range n) (vec (apply lang/obj-ary (range n)))) (str n " args")))
  (is (= (range 12) (apply hamf/vector (range 12))))
  (is (= (range 12) (hamf/ovec [0 1 2 3 4 5 6 7 8 9 10 11]))))


(deftest take-drop-edges
  (is (= [] (vec (hamf/take -1 [1 2]))))
  (is (= [1 2] (vec (hamf/drop -1 [1 2]))))
  (is (= [1 2] (vec (lznc/drop -1 [1 2]))))
  (is (= [2 3] (vec (hamf/take-last 2 [1 2 3]))))
  (is (= [1 2 3] (vec (hamf/take-last 5 [1 2 3]))))
  (is (= [1 2] (vec (hamf/drop-last 1 (seq [1 2 3])))))
  (is (= [1 2] (vec (hamf/drop-last 1 [1 2 3]))))
  (is (= [1 2 3] (vec (hamf/drop-last -1 [1 2 3])))))


(deftest map-api-edges
  (is (= {:a 1} (hamf/immut-map {} [[:a 1]])))
  (is (= {} (hamf/map-intersection + nil {:a 1})))
  (is (= {:a 2} (hamf/map-intersection + {:a 1} {:a 1}))))


(deftest group-by-consumer-arities
  (let [data (range 20)]
    (is (= (hamf/group-by-consumer even? (hamf-rf/consumer-reducer #(Sum.)) data)
           (hamf/group-by-consumer even? (hamf-rf/consumer-reducer #(Sum.)) nil data))))
  (let [m (hamf/group-by-reduce even? (constantly 0) + + {:map-fn hamf/java-hashmap} (range 10))]
    (is (instance? java.util.HashMap m))
    (is (= {true 20 false 25} (into {} m)))))


(deftest bitset-ops
  (is (= #{1 5} (set (hamf/->collection (hset/union (doto (BitSet.) (.set 1))
                                                     (doto (BitSet.) (.set 5)))))))
  (is (= :x (reduce (fn [_ _] (reduced :x)) nil (doto (BitSet.) (.set 3) (.set 4)))))
  (is (= :x (reduce (fn [_ _] (reduced :x)) (doto (BitSet.) (.set 3) (.set 4)))))
  (let [c (hamf/->collection (BitSet.))]
    (is (true? (.add ^java.util.Collection c 3)))
    (is (false? (.add ^java.util.Collection c 3)))))


(deftest java-map-coll-reduce
  (is (get (:impls clojure.core.protocols/CollReduce) java.util.HashMap))
  (is (= 3 (reduce (fn [acc [_ v]] (+ acc v)) 0 (doto (java.util.HashMap.) (.put :a 1) (.put :b 2))))))


(deftest boolean-arrays
  (is (= [false false] (vec (alists/wrap-array (boolean-array 2)))))
  (is (= [true false] (vec (hamf/into-array Boolean/TYPE [true false])))))


(deftest iterator-edges
  (is (= [2 3] (vec (iterator-seq (hiter/->iterator (next (seq (object-array [1 2 3]))))))))
  (is (= (hiter/seq-iterable []) '())))


(deftest lazy-caching-retained-args
  (is (= [[1 3 5 7 9] [2 4 6 8 10]]
         (mapv vec (lzc/map (fn [& xs] xs) [1 2] [3 4] [5 6] [7 8] [9 10])))))


(deftest reduce-helpers
  (is (= 0.0 (:sum @(hamf-rf/reduce-reducibles [(Sum.) (Sum.)]))))
  (is (= [[0 10]] (vec (impl/pgroups 10 (fn [s e] [s e])))))
  (is (= 0 (hamf-rf/preduce (constantly 0) + + {:min-n 0} (hamf/range 0)))))

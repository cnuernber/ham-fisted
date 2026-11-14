(ns ham-fisted.lznc-tree-seq-test
  (:require [clojure.test :refer [deftest is testing]]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.api :as hamf])
  (:import [java.util Iterator NoSuchElementException]))

(def ^:private trees
  [[1 [2 [3 4]] 5]
   []
   [[] [[]] [[[]]]]
   [1 nil [nil 2]]
   {:a 1 :b {:c 2 :d {:e 3}}}])

(defn- parity-cases []
  (for [t trees
        [bp ch] [[sequential? seq] [coll? seq] [map? vals]]]
    [t bp ch]))

(deftest parity-with-clojure-core
  (doseq [[t bp ch] (parity-cases)]
    (let [expected (vec (clojure.core/tree-seq bp ch t))
          ts (lznc/tree-seq bp ch t)]
      (testing (pr-str t)
        (is (= expected (vec ts)) "iterator")
        (is (= expected (reduce conj [] ts)) "reduce")
        (is (= expected (into [] (map identity) ts)) "transduce")
        (is (= expected (hamf/vec ts)))
        (is (= (seq expected) (seq ts)))
        (is (= expected ts) "seq equality")))))

(deftest leaf-root
  (is (= [:x] (vec (lznc/tree-seq coll? seq :x))))
  (is (= [nil] (vec (lznc/tree-seq coll? seq nil)))))

(deftest nil-children
  (is (= [{:k nil}] (vec (lznc/tree-seq map? :k {:k nil}))))
  (is (= [{:k nil}] (reduce conj [] (lznc/tree-seq map? :k {:k nil})))))

(deftest early-termination
  (let [calls (atom 0)
        ts (lznc/tree-seq sequential? (fn [n] (swap! calls inc) (seq n))
                          [1 [2 [3 [4 [5]]]]])]
    (is (= [[1 [2 [3 [4 [5]]]]] 1 [2 [3 [4 [5]]]]]
           (reduce (fn [acc v] (if (= 3 (count acc)) (reduced acc) (conj acc v))) [] ts)))
    ;;Only the root and [2 ...] had their children realized.
    (is (= 2 @calls))
    (is (= 3 (reduce (fn [_ v] (if (= v 3) (reduced v) nil)) nil ts)))
    (is (= 2 (first (lznc/filter #(and (number? %) (even? %)) ts))))))

(deftest non-caching
  (let [calls (atom 0)
        ts (lznc/tree-seq sequential? (fn [n] (swap! calls inc) (seq n)) [1 [2]])]
    (dorun (seq ts))
    (reduce conj [] ts)
    (is (= 4 @calls) "each walk calls children again")))

(deftest composes-with-lznc
  (let [t [1 [2 [3 4]] 5]]
    (is (= [2 4 6 8 10]
           (vec (lznc/map #(* 2 %) (lznc/filter number? (lznc/tree-seq sequential? seq t))))))
    (is (= 15 (reduce + 0 (lznc/filter number? (lznc/tree-seq sequential? seq t)))))))

(deftest deep-tree-is-stack-safe
  (let [depth 100000
        tree (reduce (fn [acc i] [i acc]) [] (range depth))
        ts (lznc/tree-seq vector? seq tree)
        n-nums (+ depth 0)]
    (is (= n-nums (count (vec (lznc/filter number? ts)))))
    (is (= n-nums (reduce (fn [acc v] (if (number? v) (inc acc) acc)) 0 ts)))))

(deftest iterator-contract
  (let [^Iterator it (.iterator ^Iterable (lznc/tree-seq sequential? seq [1]))]
    (is (.hasNext it))
    (is (.hasNext it) "hasNext is idempotent")
    (is (= [1] (.next it)))
    (is (= 1 (.next it)))
    (is (not (.hasNext it)))
    (is (thrown? NoSuchElementException (.next it)))))

(deftest metadata
  (let [ts (with-meta (lznc/tree-seq sequential? seq [1]) {:a 1})]
    (is (= {:a 1} (meta ts)))
    (is (= [[1] 1] (vec ts)))))

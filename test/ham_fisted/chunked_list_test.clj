(ns ham-fisted.chunked-list-test
  (:require [clojure.test :refer [deftest is testing]])
  (:import [java.util List]
           [java.util.function BiFunction]
           [ham_fisted MutList ImmutList]))


(defn- mut-list ^MutList [data]
  (let [m (MutList.)]
    (doseq [d data] (.add m d))
    m))


(defn- immut-list [n] (ImmutList/create true nil (object-array (range n))))


(deftest mut-list-removal
  (let [m (mut-list (range 10))]
    (.pop m)
    (is (= (range 9) (vec m)))
    (.remove m (int 0))
    (is (= (range 1 9) (vec m)))
    (.removeRange m 2 4)
    (is (= [1 2 5 6 7 8] (vec m))))
  (let [m (mut-list (range 100))]
    (.removeRange m 10 50)
    (is (= (concat (range 10) (range 50 100)) (vec m))))
  (let [m (mut-list (range 3))]
    (dotimes [_ 3] (.pop m))
    (is (= [] (vec m)))))


(deftest mut-list-fill-and-nth
  (let [m (mut-list (range 10))]
    (.fillRange m (int 8) ^List [:a :b])
    (is (= [0 1 2 3 4 5 6 7 :a :b] (vec m))))
  (let [s (.subList (mut-list (range 10)) 2 4)]
    (is (= :nf (nth s 5 :nf)))
    (is (= 3 (nth s -1 :nf)))))


(deftest mut-list-after-persistent
  (let [m (mut-list (range 3))
        p (persistent! m)]
    (is (thrown? IllegalAccessError (.add m 4)))
    (is (thrown? IllegalAccessError (.set m (int 0) :x)))
    (is (= [0 1 2] p)))
  (let [m (mut-list (range 3))]
    (.updateValues m (reify BiFunction (apply [_ k v] v)))
    (.add m 3)
    (is (= [0 1 2 3] (vec m)))))


(deftest immut-list-lookups
  (let [l (immut-list 40)
        s (.subList l 5 30)]
    (is (= :nf (nth l 100 :nf)))
    (is (= 1 (.indexOf s 6)))
    (is (= 1 (.lastIndexOf s 6)))
    (is (= -1 (.indexOf s 0)))
    (is (.containsAll s [6 7 29]))
    (is (not (.containsAll s [6 30])))
    (is (= :x (reduce-kv (fn [_ _ _] (reduced :x)) nil l)))
    (is (not (.equiv l (seq (range 20)))))
    (is (.equiv l (seq (range 40))))
    (is (not (.equiv l (seq (range 41)))))))


(deftest immut-list-clone-boundaries
  (doseq [n [32 64 96]]
    (let [l (immut-list n)]
      (is (= (range n) (vec (.updateValues l (reify BiFunction (apply [_ k v] v))))))
      (is (= (range 1 n) (vec (.updateValues (.subList l 1 n)
                                             (reify BiFunction (apply [_ k v] v))))))
      (is (= (range n) (vec (persistent! (transient l))))))))


(deftest immut-list-transient-growth
  (let [t (transient (immut-list 64))]
    (dotimes [i 40] (conj! t (+ 64 i)))
    (let [p (persistent! t)]
      (is (= (range 104) (vec p)))
      (is (thrown? IllegalAccessError (conj! t 1))))))


(deftest immut-list-pop
  (let [l (immut-list 40)]
    (is (= (range 39) (vec (pop l))))
    (is (= (concat (range 39) [:a]) (vec (conj (pop l) :a))))
    (is (= (range 33) (vec (reduce (fn [l _] (pop l)) l (range 7)))))))

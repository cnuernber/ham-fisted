(ns ham-fisted.vec-like-test
  (:require [ham-fisted.api :as hamf]
            [clojure.test :refer [deftest is] :as test])
  (:import [ham_fisted TreeList IMutList MutTreeList]
           [java.util List]))

(defn sublist-tumbler
  [^List data]
  (let [ne (count data)
        idx0 (rand-int ne)
        idx1 (rand-int ne)
        eidx (max idx0 idx1)
        sidx (min idx0 idx1)
        ss (.subList data sidx eidx)
        answer (into [] (->> (drop sidx data) (take (- eidx sidx))))
        result (try (into [] ss) (catch Exception e (println e)))]
    (when-not (= answer result)
      (throw (ex-info "sublist failed:" {:data data
                                         :answer answer
                                         :result result
                                         :sidx sidx
                                         :eidx eidx})))))

(deftest sublist-test
  (let [tr (reduce conj (TreeList.) (range 1000000))]
    (dotimes [idx 50] (sublist-tumbler tr))
    (is (= (count tr) 1000000))))

(deftest transient-set-test
  (doseq [n [100 2000 40000]]
    (let [m (MutTreeList/create false nil (object-array (range n)))
          idxs (filterv #(< % n) [0 31 32 1023 1024 (quot n 2) (dec n)])]
      (doseq [i idxs] (.set m (int i) :x))
      (is (= (reduce #(assoc %1 %2 :x) (vec (range n)) idxs) (vec m))))))

(deftest transient-pop-test
  (let [m (MutTreeList/create false nil (object-array (range 33)))]
    (.pop m) (.pop m)
    (is (= (vec (range 31)) (vec m))))
  (let [m (MutTreeList/create false nil (object-array (range 33000)))]
    (dotimes [_ 32000] (.pop m))
    (.add m :a)
    (is (= (conj (vec (range 1000)) :a) (vec m))))
  (let [m (MutTreeList/create false nil (object-array (range 1100)))]
    (dotimes [_ 1100] (.pop m))
    (dotimes [i 2000] (.add m i))
    (is (= (vec (range 2000)) (vec m)))))

(deftest transient-model-test
  (let [rng (java.util.Random. 42)]
    (loop [iter 0
           m (transient (TreeList.))
           v []]
      (if (< iter 60000)
        (let [op (.nextInt rng 10)
              n (count v)]
          (cond
            (or (< op 6) (zero? n)) (recur (inc iter) (conj! m iter) (conj v iter))
            (< op 8) (recur (inc iter) (pop! m) (pop v))
            :else (let [i (.nextInt rng n)]
                    (recur (inc iter) (assoc! m i :x) (assoc v i :x)))))
        (is (= v (vec (persistent! m))))))))

(deftest transient-does-not-edit-source-test
  (let [t (reduce conj (TreeList.) (range 64))
        m (transient t)]
    (dotimes [i 40] (conj! m i))
    (.set ^MutTreeList m 0 :x)
    (.set ^MutTreeList m 33 :y)
    (is (= (vec (range 64)) (vec t)))
    (is (= [:x :y] [(nth m 0) (nth m 33)]))))

(deftest transient-after-persistent-test
  (let [m (MutTreeList/create false nil (object-array (range 64)))
        p (persistent! m)]
    (is (thrown? IllegalAccessError (.set m 40 :x)))
    (is (thrown? IllegalAccessError (.add m :x)))
    (is (thrown? IllegalAccessError (.pop m)))
    (is (= (vec (range 64)) (vec p)))))

(deftest transient-sublist-is-view-test
  (let [m (MutTreeList/create false nil (object-array (range 100)))
        s (.subList m 10 50)]
    (.set m 10 :x)
    (is (= :x (.get s 0)))
    (is (not (instance? TreeList s)))))

(deftest persistent-pop-test
  (let [t (nth (iterate pop (reduce conj (TreeList.) (range 1088))) 33)]
    (is (= (vec (range 1055)) (vec t))))
  (let [t (reduce (fn [t _] (pop t)) (reduce conj (TreeList.) (range 33000)) (range 32000))]
    (is (= (conj (vec (range 1000)) :a) (vec (conj t :a))))))

(deftest persistent-meta-test
  (is (= {:a 1} (meta (pop (with-meta (reduce conj (TreeList.) (range 1)) {:a 1})))))
  (is (= {:a 1} (meta (empty (with-meta (reduce conj (TreeList.) (range 3)) {:a 1}))))))

(deftest reduce-reduced-test
  (let [rfn (fn [_ v] (if (= v 3) (reduced :done) v))]
    (is (= :done (reduce rfn nil (reduce conj (TreeList.) (range 10)))))
    (is (= :done (reduce rfn nil (reduce conj (TreeList.) (range 100)))))))

(deftest sublist-ops-test
  (let [t (reduce conj (TreeList.) (range 100))
        s (.subList t 5 50)]
    (is (instance? ham_fisted.TreeListBase$SubList s))
    (is (= (assoc (subvec (vec (range 100)) 5 50) 0 :x 44 :y)
           (vec (-> s (assoc 0 :x) (assoc 44 :y)))))
    (is (= (conj (subvec (vec (range 100)) 5 50) :z) (vec (assoc s 45 :z))))
    (is (thrown? IndexOutOfBoundsException (assoc s 46 :z)))
    (is (= {:a 1} (meta (with-meta s {:a 1}))))
    (is (= (range 5 50) (vec s)))))

(defn add-all-reducible
  ^IMutList [^IMutList l data]
  (.addAllReducible l data)
  l)

(comment
  (def tr (reduce conj (TreeList.) (range 35)))

  (require '[criterium.core :as crit])
  (def rr (into [] (range 1000000)))
  (crit/quick-bench (reduce conj (ham_fisted.TreeList.) rr))
  (crit/quick-bench (reduce conj [] rr))
  (crit/quick-bench (hamf/object-array (into [] rr)))
  (crit/quick-bench (add-all-reducible (hamf/object-array-list) rr))
  (crit/quick-bench (hamf/object-array (add-all-reducible (ham_fisted.MutTreeList.) rr)))

  (def tr (reduce conj (ham_fisted.TreeList.) rr))
  (def pv (reduce conj [] rr))
  (def tr (reduce conj (ham_fisted.TreeList.) (range 32768)))

  (defn verify-structure
    [^TreeList tt]

    )
  (do
    (def tr (reduce conj (TreeList.) (range 1000000)))
    (when-let [ee (try (dotimes [idx 50] (sublist-tumbler tr))
                       (catch Throwable e e))]
      (let []
        (def exd (ex-data ee))
        (def sidx 763476)
        (def eidx 877568)
        (def tt (.subList tr sidx eidx))
        (def arrays (vec (iterator-seq (.arrayIterator (.data tt) (.offset tt) (+ (.offset tt) (.size tt))))))))
    )

  (do
    (require '[clj-async-profiler.core :as prof])
    (prof/serve-ui 8080))
  )

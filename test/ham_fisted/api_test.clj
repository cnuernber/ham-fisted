(ns ham-fisted.api-test
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.api :as hamf]
            [ham-fisted.reduce :as hamf-rf]
            [ham-fisted.function :as hamf-fn]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.set :as hamf-set])
  (:import [java.util BitSet]
           [java.util.function Consumer DoubleConsumer LongConsumer]
           [clojure.lang IDeref]
           [ham_fisted Reducible]))



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
  (is (= 1 (hamf-rf/preduce (constantly 1) + + nil)))
  (is (= 1 (hamf-rf/preduce (constantly 1) + + (list)))))


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
                           (lznc/map (hamf-fn/long->double v (double v)))
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


(deftest map-filter-concat-reduced
  (let [rfn (hamf-rf/long-accumulator acc v (if (< v 4) acc (reduced v)))]
    (is (= 4 (reduce rfn 0 (lznc/map (hamf-fn/long-unary-operator v (inc v)) (hamf/range 20)))))
    (is (= 4 (reduce rfn 0 (lznc/filter even? (hamf/range 20)))))
    (is (= 4 (reduce rfn 0 (lznc/concat  (hamf/range 20) (hamf/range 20 50)))))
    ))


(deftest into-array-nil
  (is (== 0 (count (hamf/into-array Double #(double %) nil)))))


(deftest mmax-key-clojure-map
  (is (= 3 (key (hamf/mmax-key val (frequencies (map #(rem % 7) (hamf/range 10000))))))))


(deftest reduce-empty-range
  (is (= 0 (reduce (fn [acc v] (inc acc)) 0 (hamf/range 0)))))


(deftest assoc-basic-fail
  (let [m1 (hamf/immut-map {:age 2, :sex 1, "salary (binned)" 4, :job 0, :salary 3})
        m2 (assoc m1 :tech.v3.dataset.reductions/_tmp_col 5)]
    (is (= (set (keys m1)) #{:age :sex "salary (binned)" :job :salary}))))

(deftest set-api-compat
  (is (= #{:a} (disj (hamf-set/difference (hamf/immut-set [:a :b :c])
                                          (hamf/immut-set [:c :d :e]))
                     :b)))
  (is (= #{} (hamf/intersection (hamf/immut-set #{:a :b}) #{}))))


(deftest test-partition-by
  (is (= [[1 1 1] [2 2 2] [3 3 3]]
         (vec (map vec (lznc/partition-by identity [1 1 1 2 2 2 3 3 3])))))
  (is (= [[1 1 1] [2 2 2] [3 3 3]]
         (hamf/mapv vec (lznc/partition-by identity [1 1 1 2 2 2 3 3 3]))))
  (is (= (clojure.core/partition-by identity [])
         (lznc/partition-by identity [])))
  (is (= (clojure.core/partition-by identity nil)
         (lznc/partition-by identity nil)))
  ;;Ensure we catch incorrect usage when possible
  (is (thrown? RuntimeException (into [] (lznc/partition-by identity [1 1 1 2 2 2 3 3 3])))))


(deftype ^:private LongAccum [^:unsynchronized-mutable v]
  LongConsumer
  (accept [this val] (set! v (+ val v)))
  Reducible
  (reduce [this o] (+ v @o))
  IDeref
  (deref [this] v))


(deftype ^:private DoubleAccum [^:unsynchronized-mutable v]
  DoubleConsumer
  (accept [this val] (set! v (+ val v)))
  Reducible
  (reduce [this o] (+ v @o))
  IDeref
  (deref [this] v))


(deftype ^:private Accum [^:unsynchronized-mutable v]
  Consumer
  (accept [this val] (set! v (+ val v)))
  Reducible
  (reduce [this o] (+ v @o))
  IDeref
  (deref [this] v))


(defn filter-sum-reducer
  [type]
  (case type
    :int64 (hamf-rf/long-consumer-reducer #(LongAccum. 0))
    :float64 (hamf-rf/double-consumer-reducer #(DoubleAccum. 0))
    (hamf-rf/consumer-reducer #(Accum. 0.0))))


(deftest compose-reducers
  (is (= {:a 49995000}
         (hamf-rf/preduce-reducers {:a (filter-sum-reducer :int64)}
                                   {:rfn-datatype :int64}
                                   (range 10000))))
  (is (= {:a 49995000.0}
         (hamf-rf/preduce-reducers {:a (filter-sum-reducer :float64)}
                                   {:rfn-datatype :float64}
                                   (range 10000))))
  (is (= {:a 49995000.0}
         (hamf-rf/preduce-reducers {:a (filter-sum-reducer nil)}
                                   (range 10000)))))


(deftest test-partition-all
  (is (= [[0 1 2] [3 4 5] [6 7 8] [9]]
         (vec (map vec (lznc/partition-all 3 (range 10))))))
  ;;Ensure we catch incorrect usage when possible
  (is (thrown? RuntimeException (into [] (lznc/partition-all 3 (range 10))))))

(ns ham-fisted.persistent-vector-test
  (:require [clojure.test :refer [deftest is testing are] :as test]
            [clj-memory-meter.core :as mm]
            [ham-fisted.api :as hamf]
            [ham-fisted.lazy-noncaching :as lznc])
  (:import [ham_fisted MutList ImmutList MutTreeList TreeList]
           [java.util List ArrayList Collections]))


(deftest vec-nil
  (is (= (clojure.core/vec nil) (hamf/vec nil))))


(def vec-fns
  {:api-vec {:convert-fn identity :vec-fn hamf/vec}
   :api-vec-sublist {:convert-fn identity
                     :vec-fn (fn
                               ([]
                                (hamf/subvec (hamf/vec [1 2 4]) 3))
                               ([data]
                                (hamf/subvec (hamf/vec (lznc/concat [1 2 4] data)) 3)))}
   :immut-vec {:convert-fn identity :vec-fn (comp persistent! hamf/mut-list)}
   :immut-vec-sublist {:convert-fn identity
                       :vec-fn (fn
                                 ([]
                                  (hamf/subvec (persistent!
                                               (hamf/mut-list (range 100)))
                                              100))
                                 ([data]
                                  (hamf/subvec (persistent!
                                               (hamf/mut-list
                                                (lznc/concat (range 100) data)))
                                              100)))}
   :api-mut-list  {:convert-fn identity :vec-fn hamf/mut-list}
   :api-mut-sublist  {:convert-fn identity
                      :vec-fn (fn
                                ([]
                                 (hamf/subvec
                                  (hamf/mut-list
                                   (range 100))
                                  100))
                                ([data]
                                 (hamf/subvec
                                  (hamf/mut-list
                                   (lznc/concat (range 100) data))
                                  100)))}
   :byte-vec {:convert-fn unchecked-byte :vec-fn (comp hamf/->random-access hamf/byte-array)}
   :byte-vec-list {:convert-fn unchecked-byte :vec-fn (comp hamf/->random-access hamf/byte-array-list)}
   :short-vec {:convert-fn identity :vec-fn (comp hamf/->random-access hamf/short-array)}
   :short-vec-list {:convert-fn identity :vec-fn (comp hamf/->random-access hamf/short-array-list)}
   :char-vec {:convert-fn char :vec-fn
              (fn ([] (hamf/->random-access (hamf/char-array)))
                ([data] (hamf/->random-access (hamf/char-array (hamf/mapv char data)))))}
   :char-vec-list {:convert-fn char :vec-fn
                   (fn ([] (hamf/->random-access (hamf/char-array-list)))
                     ([data] (hamf/->random-access (hamf/char-array-list (hamf/mapv char data)))))}
   :int-vec {:convert-fn identity :vec-fn (fn ([] (hamf/ivec)) ([data] (hamf/ivec data)))}
   :int-list-vec {:convert-fn identity :vec-fn hamf/int-array-list}
   :long-vec {:convert-fn identity :vec-fn (fn ([] (hamf/lvec)) ([data] (hamf/lvec data)))}
   :long-list-vec {:convert-fn identity :vec-fn hamf/long-array-list}
   :float-vec {:convert-fn float :vec-fn (fn ([] (hamf/fvec)) ([data] (hamf/fvec data)))}
   :float-list-vec {:convert-fn float :vec-fn (comp hamf/->random-access hamf/float-array-list)}
   :double-vec {:convert-fn double :vec-fn (fn ([] (hamf/dvec)) ([data] (hamf/dvec data)))}
   :double-list-vec {:convert-fn double :vec-fn hamf/double-array-list}
   })


(defn test-reversed-vec-fn
  [k {:keys [convert-fn vec-fn]}]
  (let [r (range 6)
        v (vec-fn r)
        reversed (.rseq v)]
    (testing "RSeq methods"
      (is (= (hamf/mapv convert-fn [5 4 3 2 1 0]) reversed) (str k " " v " " reversed))
      (is (= (convert-fn 5) (.first reversed)) k)
      (is (= (hamf/mapv convert-fn [4 3 2 1 0]) (.next reversed)) k)
      (is (= (hamf/mapv convert-fn [3 2 1 0]) (.. reversed next next)) k)
      (is (= 6 (.count reversed)) k))
    (testing "clojure calling through"
      (is (= (convert-fn 5) (first reversed)) k)
      (is (= (convert-fn 5) (nth reversed 0))) k)
    (testing "empty reverses to nil"
      (is (nil? (.. v empty rseq))))))


(deftest test-reversed-vec
  (doseq [[k v] vec-fns]
    (test-reversed-vec-fn k v)))


(defn all-add
  ([convert-fn a b]
   (convert-fn (+ (long a) (long b))))
  ([convert-fn a b c]
   (convert-fn (+ (long a) (long b) (long c)))))


(deftest test-subvector-reduce
  (doseq [[k v] vec-fns]
    (let [{:keys [convert-fn vec-fn]} v]
      (is (= (convert-fn 60)
             (let [prim-vec (vec-fn (lznc/map convert-fn (hamf/range 1000)))]
               (convert-fn (reduce (partial all-add convert-fn)
                                   (hamf/subvec prim-vec 10 15)))))
          k)
      (is (= (convert-fn 60)
             (let [prim-vec (hamf/vec (range 1000))]
               (reduce (partial all-add convert-fn)
                       (hamf/subvec prim-vec 10 15))))
          k))))


(deftest test-vec-associative
  (doseq [[k v] vec-fns]
    (let [{:keys [convert-fn vec-fn]} v]
      (let [empty-v (vec-fn)
            v       (vec-fn (range 1 6))]
        (testing "Associative.containsKey"
          (are [x] (.containsKey v x)
            0 1 2 3 4)
          (are [x] (not (.containsKey v x))
            -1 -100 nil [] "" #"" #{} 5 100)
          (are [x] (not (.containsKey empty-v x))
            0 1))
        (testing "contains?"
          (are [x] (contains? v x)
            0 2 4)
          (are [x] (not (contains? v x))
            -1 -100 nil "" 5 100)
          (are [x] (not (contains? empty-v x))
            0 1))
        (testing "Associative.entryAt"
          (are [idx val] (= (clojure.lang.MapEntry. idx (convert-fn val))
                            (.entryAt v idx))
            0 1
            2 3
            4 5)
          (are [idx] (nil? (.entryAt v idx))
            -5 -1 5 10 nil "")
          (are [idx] (nil? (.entryAt empty-v idx))
            0 1))))))


(defn =vec
  [expected v] (and (vector? v) (= expected v)))


(deftest test-mapv
  (are [r c1] (=vec r (hamf/mapv + c1))
    [1 2 3] [1 2 3])
  (are [r c1 c2] (=vec r (hamf/mapv + c1 c2))
    [2 3 4] [1 2 3] (repeat 1))
  (are [r c1 c2 c3] (=vec r (hamf/mapv + c1 c2 c3))
    [3 4 5] [1 2 3] (repeat 1) (repeat 1))
  (are [r c1 c2 c3 c4] (=vec r (hamf/mapv + c1 c2 c3 c4))
    [4 5 6] [1 2 3] [1 1 1] [1 1 1] [1 1 1]))

(deftest test-filterv
  (are [r c1] (=vec r (hamf/filterv even? c1))
    (hamf/vector) (hamf/vector 1 3 5)
    (hamf/vector 2 4) (hamf/vector 1 2 3 4 5)))

(deftest test-subvec
  (doseq [[k v] vec-fns]
    (let [{:keys [convert-fn vec-fn]} v]
      (let [v1 (vec-fn (hamf/range 100))
            v2 (hamf/subvec v1 50 57)]
        ;;nth, IFn interfaces allow negative (from the end) indexing
        (is (thrown? IndexOutOfBoundsException (.get v2 -1)))
        (is (thrown? IndexOutOfBoundsException (v2 7)) k)
        (is (= (v1 50) (v2 0)))
        (is (= (v1 56) (v2 6))))
      (let [v1 (vec-fn (hamf/range 10))
            v2 (hamf/subvec v1 2 7)]
        ;;nth, IFn interfaces allow negative (from the end) indexing
        (is (thrown? IndexOutOfBoundsException (.get v2 -1)))
        (is (thrown? IndexOutOfBoundsException (v2 7)) k)
        (is (= (v1 2) (v2 0)))
        (is (= (v1 6) (v2 4)))))))


(deftest test-vec
  (is (= [1 2] (hamf/vec (first {1 2}))))
  (is (= [0 1 2 3] (hamf/vec [0 1 2 3])))
  (is (= [0 1 2 3] (hamf/vec (list 0 1 2 3))))
  (is (= [[1 2] [3 4]] (vec (sorted-map 1 2 3 4))))
  (is (= [0 1 2 3] (hamf/vec (range 4))))
  (is (= [\a \b \c \d] (hamf/vec "abcd")))
  (is (= [0 1 2 3] (hamf/vec (object-array (range 4)))))
  (is (= [1 2 3 4] (hamf/vec (eduction (map inc) (range 4)))))
  (is (= [0 1 2 3] (hamf/vec (reify clojure.lang.IReduceInit
                              (reduce [_ f start]
                                (reduce f start (range 4))))))))

(deftest test-vector-eqv-to-non-counted-types
  (is (not= (range) (hamf/vector 0 1 2)))
  (is (not= (hamf/vector 0 1 2) (range)))
  (is (not= (hamf/vec (range 100)) (range)))
  (is (= (hamf/vec (range 100)) (hamf/range 100)))
  (is (= (hamf/vector 0 1 2) (take 3 (range))))
  (is (= (hamf/vector 0 1 2) (new java.util.ArrayList [0 1 2])))
  (is (not= (hamf/vector 1 2) (take 1 (cycle [1 2]))))
  (is (= (hamf/vector 1 2 3 nil 4 5 6 nil) (eduction cat [[1 2 3 nil] [4 5 6 nil]]))))


(deftest test-reduce-kv-vectors
  (is (= 25 (reduce-kv + 10 (hamf/vector 2 4 6))))
  (is (= 25 (reduce-kv + 10 (hamf/subvec (hamf/vector 0 2 4 6) 1))))
  (doseq [[k v] vec-fns]
    (when-not (#{:byte-vec :byte-vec-list} k)
      (let [{:keys [convert-fn vec-fn]} v]
        (is (= 9811 (long (reduce-kv (partial all-add convert-fn)
                                     10 (vec-fn (hamf/range 1 100)))))
            k)
        (is (= 9811 (long (reduce-kv (partial all-add convert-fn)
                                     10 (hamf/subvec (vec-fn (hamf/range 100)) 1))))
            k)))))


(deftest test-reduced?-in-container
  (doseq [[k v] vec-fns]
    (when-not (#{:byte-vec :byte-vec-list} k)
      (let [{:keys [convert-fn vec-fn]} v]
        (is (= 4 (reduce (fn [acc v]
                           (let [v (long v)]
                             (if (< v 4)
                               v
                               (reduced v))))
                         0 (vec-fn (hamf/range 1 100))))
            k)))))


(deftest test-reduce-kv-array
  (is (= 25 (reduce-kv + 10 (hamf/->random-access (hamf/int-array [2 4 6])))))
  (is (= 25 (reduce-kv + 10 (hamf/subvec (hamf/->random-access (hamf/int-array [0 2 4 6])) 1)))))


(deftest test-reduce-vectors
  (is (= 22 (reduce + 10 (hamf/vector 2 4 6))))
  (is (= 22 (reduce + 10 (hamf/subvec (hamf/vector 0 2 4 6) 1))))
  (is (= 4960 (reduce + 10 (hamf/vec (hamf/range 1 100)))))
  (is (= 4960 (reduce + 10 (hamf/subvec (hamf/vec (hamf/range 100)) 1)))))


(deftest test-reduce-arrays
  (is (= 22 (reduce + 10 (hamf/int-array [2 4 6]))))
  (is (= 22 (reduce + 10 (hamf/subvec (hamf/int-array [0 2 4 6]) 1)))))


(deftest concatv-special-cases
  (is (= (reduce + 0 (hamf/concatv [] (list 1 2 3) nil nil
                                  (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                                  (hamf/vec (hamf/range 50))))
         (reduce + 0 (concat [] (list 1 2 3) nil nil
                             (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                             (hamf/vec (hamf/range 50))))))
  (is (= (reduce + 0 (lznc/concat [] (list 1 2 3) nil nil
                                  (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                                  (hamf/vec (hamf/range 50))))
         (reduce + 0 (concat [] (list 1 2 3) nil nil
                             (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                             (hamf/vec (hamf/range 50))))))
  (is (= (hamf/concatv [] (list 1 2 3) nil nil
                      (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                      (hamf/vec (hamf/range 50)))
         (concat [] (list 1 2 3) nil nil
                 (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                 (hamf/vec (hamf/range 50)))))
  (is (= (lznc/concat [] (list 1 2 3) nil nil
                      (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                      (hamf/vec (hamf/range 50)))
         (concat [] (list 1 2 3) nil nil
                 (clojure.core/vector 1 2 3 4 5) (hamf/array-list [1 2 3 4])
                 (hamf/vec (hamf/range 50)))))
  (is (= (hamf/vec (hamf/concata [] (list 1 2 3) nil nil
                               (clojure.core/vector 1 2 3 4 5)
                               (hamf/object-array-list [1 2 3 4])
                               (hamf/vec (hamf/range 50))))
         (concat [] (list 1 2 3) nil nil
                 (clojure.core/vector 1 2 3 4 5) (hamf/object-array-list [1 2 3 4])
                 (hamf/vec (hamf/range 50))))))

(deftest tree-list-creation
  (let [data (hamf/object-array (hamf/range 100))
        vdata (clojure.core/vec data)]
    (is (= vdata (ham_fisted.TreeList/create true nil data)))
    (is (= vdata (ham_fisted.MutTreeList/create true nil data)))))


(deftest binary-search
  (let [data (hamf/shuffle (hamf/range 100))]
    (doseq [[k {:keys [convert-fn vec-fn]}] vec-fns]
      (let [init-data (vec-fn data)
            ;;make sure sort always works
            newvdata (hamf/sort init-data)
            ;;transform back
            vdata (vec-fn newvdata)
            subv (hamf/subvec vdata 50)]
        ;;Ensure that non-accelerated sort results are identical to
        ;;accelerated sort results
        (is (= vdata (hamf/sort compare init-data)) k)
        (is (= 50 (hamf/binary-search vdata (convert-fn 50))) k)
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 51 (hamf/binary-search vdata 50.1 compare)) k))
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 0 (hamf/binary-search vdata -1)) k))
        (is (= 100 (hamf/binary-search vdata (convert-fn 120))) k)
        (is (= 0 (hamf/binary-search subv (convert-fn 50))) k)
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 1 (hamf/binary-search subv 50.1 compare)) k))
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 0 (hamf/binary-search subv -1)) k))
        (is (= 50 (hamf/binary-search subv (convert-fn 120))) k)))))


(deftest boolean-arrays
  (is (== 2.0 (hamf/sum (hamf/boolean-array [true false true false]))))
  (is (= [true false true false] (hamf/->random-access (hamf/boolean-array [1 0 1 0])))))


(deftest float-regression
  (is (= 2 (count (hamf/float-array (lznc/filter (fn [^double v]
                                                  (not (Double/isNaN v)))
                                                [1 ##NaN 2]))))))

(deftest sublists-are-smaller-test
  (let [t (into (TreeList.) (range (* 1000 1000)))]
    (println (* 2 (mm/measure (hamf/subvec t 100000 200000) :bytes true))
             (* 1 (mm/measure t :bytes true)))
    (is (< (* 2 (mm/measure (hamf/subvec t 100000 200000) :bytes true))
           (* 1 (mm/measure t :bytes true))))))


(comment
  (def vec-fn (get-in vec-fns [:api-mut-sublist :vec-fn]))
  (def data (hamf/shuffle (hamf/range 100)))
  (def vv (vec-fn data))

  (def m (doto (MutList.) (.addAll (range 36))))
  (def m ImmutList/EMPTY)

  (def data (doto (ArrayList.)
              (.addAll (range 1000))))

  (crit/quick-bench (doto (ArrayList.)
                      (.addAll data)))
  ;; Evaluation count : 907674 in 6 samples of 151279 calls.
  ;;            Execution time mean : 662.299153 ns
  ;;   Execution time std-deviation : 4.369292 ns
  ;;  Execution time lower quantile : 653.753118 ns ( 2.5%)
  ;;  Execution time upper quantile : 665.603474 ns (97.5%)
  ;;                  Overhead used : 1.966980 ns


  (crit/quick-bench (doto (MutList.) (.addAll data)))
  ;; Evaluation count : 395256 in 6 samples of 65876 calls.
  ;;            Execution time mean : 1.517687 µs
  ;;   Execution time std-deviation : 0.809702 ns
  ;;  Execution time lower quantile : 1.516407 µs ( 2.5%)
  ;;  Execution time upper quantile : 1.518463 µs (97.5%)
  ;;                  Overhead used : 1.969574 ns
  (crit/quick-bench (hamf/mut-list data)) ;;same

  (def vdata (vec data))
  (crit/quick-bench (doto (MutList.) (.addAll vdata)))

  (def m (hamf/mut-list data))

  (defn index-test
    [^List m]
    (let [nelems (.size m)]
      (crit/quick-bench (dotimes [idx nelems]
                          (.get m idx)))))
  (index-test data) ;; 770ns
  (index-test m) ;; 1.7us
  (index-test vdata) ;;5.3us


  (crit/quick-bench (.toArray data)) ;;1.08us
  (crit/quick-bench (.toArray m)) ;;2.4us
  (crit/quick-bench (.toArray vdata)) ;;6.2us

  (def adata (.toArray data))
  (crit/quick-bench (hamf/mut-list adata))
  ;; Execution time mean : 775.067887 ns
  ;; Execution time std-deviation : 2.413493 ns
  ;; Execution time lower quantile : 770.746577 ns ( 2.5%)
  ;; Execution time upper quantile : 777.223862 ns (97.5%)
  ;; Overhead used : 1.965715 ns

  (crit/quick-bench (hamf/into [] data))
  ;; Evaluation count : 31608 in 6 samples of 5268 calls.
  ;;            Execution time mean : 19.045789 µs
  ;;   Execution time std-deviation : 28.268048 ns
  ;;  Execution time lower quantile : 19.001188 µs ( 2.5%)
  ;;  Execution time upper quantile : 19.073174 µs (97.5%)
  ;;                  Overhead used : 1.965715 ns

  (crit/quick-bench (hamf/into hamf/empty-vec adata))
  ;; Evaluation count : 31608 in 6 samples of 5268 calls.
  ;;            Execution time mean : 19.045789 µs
  ;;   Execution time std-deviation : 28.268048 ns
  ;;  Execution time lower quantile : 19.001188 µs ( 2.5%)
  ;;  Execution time upper quantile : 19.073174 µs (97.5%)
  ;;                  Overhead used : 1.965715 ns

  )

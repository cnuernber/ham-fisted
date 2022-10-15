(ns ham-fisted.persistent-vector-test
  (:require [ham-fisted.api :as api]
            [ham-fisted.lazy-noncaching :as lznc]
            [clojure.test :refer [deftest is testing are] :as test])
  (:import [ham_fisted MutList ImmutList]
           [java.util List ArrayList Collections]))


(deftest vec-nil
  (is (= (clojure.core/vec nil) (api/vec nil))))


(def vec-fns
  {:api-vec {:convert-fn identity :vec-fn api/vec}
   :api-vec-sublist {:convert-fn identity
                     :vec-fn (fn
                               ([]
                                (api/subvec (api/vec [1 2 4]) 3))
                               ([data]
                                  (api/subvec (api/vec (lznc/concat [1 2 4] data)) 3)))}
   :immut-vec {:convert-fn identity :vec-fn (comp persistent! api/mut-list)}
   :immut-vec-sublist {:convert-fn identity
                       :vec-fn (fn
                                 ([]
                                  (api/subvec (persistent!
                                               (api/mut-list (range 100)))
                                              100))
                                 ([data]
                                  (api/subvec (persistent!
                                               (api/mut-list
                                                (lznc/concat (range 100) data)))
                                              100)))}
   :api-mut-list  {:convert-fn identity :vec-fn api/mut-list}
   :api-mut-sublist  {:convert-fn identity
                      :vec-fn (fn
                                ([]
                                 (api/subvec
                                  (api/mut-list
                                   (range 100))
                                  100))
                                ([data]
                                 (api/subvec
                                  (api/mut-list
                                   (lznc/concat (range 100) data))
                                  100)))}
   :byte-vec {:convert-fn unchecked-byte :vec-fn (comp api/->random-access api/byte-array)}
   :byte-vec-list {:convert-fn unchecked-byte :vec-fn (comp api/->random-access api/byte-array-list)}
   :short-vec {:convert-fn identity :vec-fn (comp api/->random-access api/short-array)}
   :short-vec-list {:convert-fn identity :vec-fn (comp api/->random-access api/short-array-list)}
   :char-vec {:convert-fn char :vec-fn
              (fn ([] (api/->random-access (api/char-array)))
                ([data] (api/->random-access (api/char-array (api/mapv char data)))))}
   :char-vec-list {:convert-fn char :vec-fn
                   (fn ([] (api/->random-access (api/char-array-list)))
                     ([data] (api/->random-access (api/char-array-list (api/mapv char data)))))}
   :int-vec {:convert-fn identity :vec-fn (comp api/->random-access api/int-array)}
   :int-list-vec {:convert-fn identity :vec-fn api/int-array-list}
   :long-vec {:convert-fn identity :vec-fn (comp api/->random-access api/long-array)}
   :long-list-vec {:convert-fn identity :vec-fn api/long-array-list}
   :float-vec {:convert-fn float :vec-fn (comp api/->random-access api/float-array)}
   :float-list-vec {:convert-fn float :vec-fn (comp api/->random-access api/float-array-list)}
   :double-vec {:convert-fn double :vec-fn (comp api/->random-access api/double-array)}
   :double-list-vec {:convert-fn double :vec-fn api/double-array-list}
   })


(defn test-reversed-vec-fn
  [{:keys [convert-fn vec-fn]}]
  (let [r (range 6)
        v (vec-fn r)
        reversed (.rseq v)]
    (testing "RSeq methods"
      (is (= (api/mapv convert-fn [5 4 3 2 1 0]) reversed))
      (is (= (convert-fn 5) (.first reversed)))
      (is (= (api/mapv convert-fn [4 3 2 1 0]) (.next reversed)))
      (is (= (api/mapv convert-fn [3 2 1 0]) (.. reversed next next)))
      (is (= 6 (.count reversed))))
    (testing "clojure calling through"
      (is (= (convert-fn 5) (first reversed)))
      (is (= (convert-fn 5) (nth reversed 0))))
    (testing "empty reverses to nil"
      (is (nil? (.. v empty rseq))))))


(deftest test-reversed-vec
  (doseq [[k v] vec-fns]
    (test-reversed-vec-fn v)))


(defn all-add
  ([convert-fn a b]
   (convert-fn (+ (long a) (long b))))
  ([convert-fn a b c]
   (convert-fn (+ (long a) (long b) (long c)))))


(deftest test-subvector-reduce
  (doseq [[k v] vec-fns]
    (let [{:keys [convert-fn vec-fn]} v]
      (is (= (convert-fn 60)
             (let [prim-vec (vec-fn (lznc/map convert-fn (api/range 1000)))]
               (convert-fn (reduce (partial all-add convert-fn)
                                   (api/subvec prim-vec 10 15))))))
      (is (= (convert-fn 60)
             (let [prim-vec (api/vec (range 1000))]
               (reduce (partial all-add convert-fn)
                       (api/subvec prim-vec 10 15))))))))


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
  (are [r c1] (=vec r (api/mapv + c1))
    [1 2 3] [1 2 3])
  (are [r c1 c2] (=vec r (api/mapv + c1 c2))
    [2 3 4] [1 2 3] (repeat 1))
  (are [r c1 c2 c3] (=vec r (api/mapv + c1 c2 c3))
    [3 4 5] [1 2 3] (repeat 1) (repeat 1))
  (are [r c1 c2 c3 c4] (=vec r (api/mapv + c1 c2 c3 c4))
    [4 5 6] [1 2 3] [1 1 1] [1 1 1] [1 1 1]))

(deftest test-filterv
  (are [r c1] (=vec r (api/filterv even? c1))
    (api/vector) (api/vector 1 3 5)
    (api/vector 2 4) (api/vector 1 2 3 4 5)))

(deftest test-subvec
  (doseq [[k v] vec-fns]
    (let [{:keys [convert-fn vec-fn]} v]
      (let [v1 (vec-fn (api/range 100))
            v2 (api/subvec v1 50 57)]
        ;;nth, IFn interfaces allow negative (from the end) indexing
        (is (thrown? IndexOutOfBoundsException (.get v2 -1)))
        (is (thrown? IndexOutOfBoundsException (v2 7)) k)
        (is (= (v1 50) (v2 0)))
        (is (= (v1 56) (v2 6))))
      (let [v1 (vec-fn (api/range 10))
            v2 (api/subvec v1 2 7)]
        ;;nth, IFn interfaces allow negative (from the end) indexing
        (is (thrown? IndexOutOfBoundsException (.get v2 -1)))
        (is (thrown? IndexOutOfBoundsException (v2 7)) k)
        (is (= (v1 2) (v2 0)))
        (is (= (v1 6) (v2 4)))))))


(deftest test-vec
  (is (= [1 2] (api/vec (first {1 2}))))
  (is (= [0 1 2 3] (api/vec [0 1 2 3])))
  (is (= [0 1 2 3] (api/vec (list 0 1 2 3))))
  (is (= [[1 2] [3 4]] (vec (sorted-map 1 2 3 4))))
  (is (= [0 1 2 3] (api/vec (range 4))))
  (is (= [\a \b \c \d] (api/vec "abcd")))
  (is (= [0 1 2 3] (api/vec (object-array (range 4)))))
  (is (= [1 2 3 4] (api/vec (eduction (map inc) (range 4)))))
  (is (= [0 1 2 3] (api/vec (reify clojure.lang.IReduceInit
                              (reduce [_ f start]
                                (reduce f start (range 4))))))))

(deftest test-vector-eqv-to-non-counted-types
  (is (not= (range) (api/vector 0 1 2)))
  (is (not= (api/vector 0 1 2) (range)))
  (is (not= (api/vec (range 100)) (range)))
  (is (= (api/vec (range 100)) (api/range 100)))
  (is (= (api/vector 0 1 2) (take 3 (range))))
  (is (= (api/vector 0 1 2) (new java.util.ArrayList [0 1 2])))
  (is (not= (api/vector 1 2) (take 1 (cycle [1 2]))))
  (is (= (api/vector 1 2 3 nil 4 5 6 nil) (eduction cat [[1 2 3 nil] [4 5 6 nil]]))))


(deftest test-reduce-kv-vectors
  (is (= 25 (reduce-kv + 10 (api/vector 2 4 6))))
  (is (= 25 (reduce-kv + 10 (api/subvec (api/vector 0 2 4 6) 1))))
  (doseq [[k v] vec-fns]
    (when-not (#{:byte-vec :byte-vec-list} k)
      (let [{:keys [convert-fn vec-fn]} v]
        (is (= 9811 (long (reduce-kv (partial all-add convert-fn)
                                     10 (vec-fn (api/range 1 100)))))
            k)
        (is (= 9811 (long (reduce-kv (partial all-add convert-fn)
                                     10 (api/subvec (vec-fn (api/range 100)) 1))))
            k)))))


(deftest test-reduce-kv-array
  (is (= 25 (reduce-kv + 10 (api/->random-access (api/int-array [2 4 6])))))
  (is (= 25 (reduce-kv + 10 (api/subvec (api/->random-access (api/int-array [0 2 4 6])) 1)))))


(deftest test-reduce-vectors
  (is (= 22 (reduce + 10 (api/vector 2 4 6))))
  (is (= 22 (reduce + 10 (api/subvec (api/vector 0 2 4 6) 1))))
  (is (= 4960 (reduce + 10 (api/vec (api/range 1 100)))))
  (is (= 4960 (reduce + 10 (api/subvec (api/vec (api/range 100)) 1)))))


(deftest test-reduce-arrays
  (is (= 22 (api/fast-reduce + 10 (api/int-array [2 4 6]))))
  (is (= 22 (api/fast-reduce + 10 (api/subvec (api/int-array [0 2 4 6]) 1)))))


(deftest concatv-special-cases
  (is (= (reduce + 0 (api/concatv [] (list 1 2 3) nil nil
                                  (clojure.core/vector 1 2 3 4 5) (api/array-list [1 2 3 4])
                                  (api/vec (api/range 50))))
         (reduce + 0 (concat [] (list 1 2 3) nil nil
                             (clojure.core/vector 1 2 3 4 5) (api/array-list [1 2 3 4])
                             (api/vec (api/range 50))))))
  (is (= (api/concatv [] (list 1 2 3) nil nil
                      (clojure.core/vector 1 2 3 4 5) (api/array-list [1 2 3 4])
                      (api/vec (api/range 50)))
         (concat [] (list 1 2 3) nil nil
                 (clojure.core/vector 1 2 3 4 5) (api/array-list [1 2 3 4])
                 (api/vec (api/range 50)))))
  (is (= (api/vec (api/concata [] (list 1 2 3) nil nil
                               (clojure.core/vector 1 2 3 4 5)
                               (api/object-array-list [1 2 3 4])
                               (api/vec (api/range 50))))
         (concat [] (list 1 2 3) nil nil
                 (clojure.core/vector 1 2 3 4 5) (api/object-array-list [1 2 3 4])
                 (api/vec (api/range 50))))))


(deftest binary-search
  (let [data (api/shuffle (api/range 100))]
    (doseq [[k {:keys [convert-fn vec-fn]}] vec-fns]
      (let [init-data (vec-fn data)
            ;;make sure sort always works
            newvdata (api/sort init-data)
            ;;transform back
            vdata (vec-fn newvdata)
            subv (api/subvec vdata 50)]
        ;;Ensure that non-accelerated sort results are identical to
        ;;accelerated sort results
        (is (= vdata (api/sort compare init-data)))
        (is (= 50 (api/binary-search vdata (convert-fn 50))) k)
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 51 (api/binary-search vdata 50.1 compare)) k))
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 0 (api/binary-search vdata -1)) k))
        (is (= 100 (api/binary-search vdata (convert-fn 120))) k)
        (is (= 0 (api/binary-search subv (convert-fn 50))) k)
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 1 (api/binary-search subv 50.1 compare)) k))
        (when-not (#{:char-vec :char-vec-list} k)
          (is (= 0 (api/binary-search subv -1)) k))
        (is (= 50 (api/binary-search subv (convert-fn 120))) k)))))


(deftest boolean-arrays
  (is (== 2.0 (api/sum (api/boolean-array [true false true false]))))
  (is (= [true false true false] (api/->random-access (api/boolean-array [1 0 1 0])))))


(deftest float-regression
  (is (= 2 (count (api/float-array (lznc/filter (fn [^double v]
                                                  (not (Double/isNaN v)))
                                                [1 ##NaN 2]))))))


(comment
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
  (crit/quick-bench (api/mut-list data)) ;;same

  (def vdata (vec data))
  (crit/quick-bench (doto (MutList.) (.addAll vdata)))

  (def m (api/mut-list data))

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
  (crit/quick-bench (api/mut-list adata))
  ;; Execution time mean : 775.067887 ns
  ;; Execution time std-deviation : 2.413493 ns
  ;; Execution time lower quantile : 770.746577 ns ( 2.5%)
  ;; Execution time upper quantile : 777.223862 ns (97.5%)
  ;; Overhead used : 1.965715 ns

  (crit/quick-bench (api/into [] data))
  ;; Evaluation count : 31608 in 6 samples of 5268 calls.
  ;;            Execution time mean : 19.045789 µs
  ;;   Execution time std-deviation : 28.268048 ns
  ;;  Execution time lower quantile : 19.001188 µs ( 2.5%)
  ;;  Execution time upper quantile : 19.073174 µs (97.5%)
  ;;                  Overhead used : 1.965715 ns

  (crit/quick-bench (api/into api/empty-vec adata))
  ;; Evaluation count : 31608 in 6 samples of 5268 calls.
  ;;            Execution time mean : 19.045789 µs
  ;;   Execution time std-deviation : 28.268048 ns
  ;;  Execution time lower quantile : 19.001188 µs ( 2.5%)
  ;;  Execution time upper quantile : 19.073174 µs (97.5%)
  ;;                  Overhead used : 1.965715 ns

  )

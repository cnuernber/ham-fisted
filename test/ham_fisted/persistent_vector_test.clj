(ns ham-fisted.persistent-vector-test
  (:require [criterium.core :as crit]
            [ham-fisted.api :as api])
  (:import [ham_fisted MutList ImmutList]
           [java.util List ArrayList Collections]))
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

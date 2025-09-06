(ns ham-fisted.vec-like
  (:require [ham-fisted.api :as hamf]
            [clojure.test :refer [deftest is] :as test])
  (:import [ham_fisted TreeList]
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
  (let [tr (reduce conj (TreeList.) (range 32768))]
    (dotimes [idx 50] (sublist-tumbler tr))
    (is (= (count tr) 32768))))


(comment
  (def tr (reduce conj (TreeList.) (range 35)))
  
  (require '[criterium.core :as crit])
  (def rr (range 1000000))
  (crit/quick-bench (reduce conj (ham_fisted.TreeList.) rr))
  (crit/quick-bench (reduce conj [] rr))
  (crit/quick-bench (into [] rr))
  (crit/quick-bench (hamf/object-array-list rr))
  
  (def tr (reduce conj (ham_fisted.TreeList.) rr))
  (def pv (reduce conj [] rr))
  (def tr (reduce conj (ham_fisted.TreeList.) (range 32768)))


  (do
    (def tr (reduce conj (TreeList.) (range 32768)))
    (when-let [ee (try (dotimes [idx 50] (sublist-tumbler tr))
                       (catch Throwable e e))]
      (let [exd (ex-data ee)]
        (def sidx (:sidx exd))
        (def eidx (:eidx exd))
        (def answer (:answer exd))
        (def result (:result exd))
        (def tt (.subList tr sidx eidx))
        (def arrays (vec (iterator-seq (.arrayIterator (.data tt) (.offset tt) (+ (.offset tt) (.size tt))))))))
    )

  (do 
    (require '[clj-async-profiler.core :as prof])
    (prof/profile {:interval 10000} (dotimes [idx 10] (reduce conj (ham_fisted.TreeList.) (range 1000000))))
    (prof/serve-ui 8080))
  )

(ns ham-fisted.vec-like-test
  (:require [ham-fisted.api :as hamf]
            [clojure.test :refer [deftest is] :as test])
  (:import [ham_fisted TreeList IMutList Iter MutTreeList]
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

(defn add-all-reducible
  ^IMutList [^IMutList l data]
  (.addAllReducible l data)
  l)

(defn ->iter
  [data]
  (when data 
    (if (instance? Iter data)
      data
      (Iter/fromIterator (.iterator ^Iterable data)))))

(defn cons-all
  ^TreeList [^TreeList l data]
  (.consAll l (->iter data)))

(deftype RangeIter [^long n
                    ^{:unsynchronized-mutable true
                      :tag long} idx]
  Iter
  (get [this] (Long/valueOf idx))
  (next [this]
    (set! idx (inc idx))
    (when (< idx n)
      this)))


(comment
  (def tr (reduce conj (TreeList.) (range 35)))
  
  (require '[criterium.core :as crit])
  (def rr (into [] (range 1000000)))
  (crit/quick-bench (reduce conj (ham_fisted.TreeList.) rr))
  (crit/quick-bench (reduce conj [] rr))
  (crit/quick-bench (hamf/object-array (into [] rr)))
  (crit/quick-bench (add-all-reducible (hamf/object-array-list) rr))
  (crit/quick-bench (hamf/object-array (add-all-reducible (ham_fisted.MutTreeList.) rr)))
  (crit/quick-bench (cons-all (ham_fisted.TreeList.) rr))
  (crit/quick-bench (cons-all (ham_fisted.TreeList.) (RangeIter. (count rr) 0)))
  (crit/quick-bench (add-all-reducible (ham_fisted.BatchedList.) rr))
  
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
    (prof/profile {:interval 10000} (dotimes [idx 50] (add-all-reducible (ham_fisted.BatchedList.) rr)))
    (prof/serve-ui 8080))
  )

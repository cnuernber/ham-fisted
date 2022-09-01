(ns process-results
  (:require [perftest :as perftest]
            [tech.v3.dataset :as ds]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io File]))



(defn list-results
  []
  (->> (File. "results/")
       (.list)
       (remove #(= % ".keepme"))
       (map #(str "results/" %))))


(defn result-file->dataset
  [fname]
  (let [data (->> (slurp fname)
                  (edn/read-string))
        ds (-> data
               :dataset
               (ds/->dataset)
               (ds/select-columns perftest/column-order)
               ;;Always print entire dataset
               (ds/head 10000))]
    (vary-meta ds merge (-> (dissoc data :dataset)
                            (assoc :name fname)))))

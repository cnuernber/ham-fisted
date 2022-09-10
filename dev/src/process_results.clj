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

(def numeric-columns (drop 2 perftest/column-order))


(defn round-columns
  [ds cnames ndigits]
  (let [ndigits (double (Math/pow 10.0 ndigits))]
    (reduce (fn [ds cname]
              (ds/column-map ds cname (fn [val]
                                        (when val
                                          (-> (* (double val) ndigits)
                                              (Math/round)
                                              (/ ndigits))))
                             [cname]))
            ds
            cnames)))


(defn result-file->dataset
  [fname]
  (let [data (->> (slurp fname)
                  (edn/read-string))
        ds (-> data
               :dataset
               (ds/->dataset)
               (ds/select-columns perftest/column-order)
               (round-columns numeric-columns 3)
               ;;Always print entire dataset
               (ds/head 10000))]
    (vary-meta ds merge (-> (dissoc data :dataset)
                            (assoc :name fname)))))

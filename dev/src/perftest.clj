(ns perftest
  (:require [ham-fisted.api :as api]
            [ham-fisted.lazy-noncaching :as lzc]
            [ham-fisted.benchmark :as bench]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp])
  (:import [java.util HashMap ArrayList Map List])
  (:gen-class))

(defn hashmap-perftest
  [data]
  (let [n-elems (count data)
        cons-hm (fn [] (let [hm (HashMap.)]
                         (reduce (fn [hm [k v]]
                                   (.put ^Map hm k v)
                                   hm)
                                 hm
                                 data)))]
    {:construction {:clj (bench/benchmark-us (into {} data))
                    :hamf (bench/benchmark-us (api/immut-map data))
                    :java (bench/benchmark-us (cons-hm))}
     :access (let [method #(reduce (fn [m [k v]] (get m k) m)
                                   %
                                   data)
                   clj-d (into {} data)
                   hm-d (api/immut-map data)
                   jv-d (cons-hm)]
               {:clj (bench/benchmark-us (method clj-d))
                :hamf (bench/benchmark-us (method hm-d))
                :java (bench/benchmark-us (method jv-d))})
     :reduce (let [method #(reduce (fn [sum [k v]] (+ sum v))
                                      0
                                      %)
                   clj-d (into {} data)
                   hm-d (api/immut-map data)
                   jv-d (cons-hm)]
                  {:clj (bench/benchmark-us (method clj-d))
                   :hamf (bench/benchmark-us (method hm-d))
                   :java (bench/benchmark-us (method jv-d))})}))

(defn map-data
  [^long n-elems]
  (lzc/->random-access
   (object-array
    (lzc/map
     #(api/vector % %)
     (lzc/repeatedly 1000 #(rand-int 100000))))))


(defn general-hashmap
  []
  (log/info "hashmap perftest")
  {:hashmap-1000 (hashmap-perftest (map-data 1000))
   :hashmap-10 (hashmap-perftest (map-data 10))})


(defn vec-perftest
  [data]
  (let [odata (api/object-array data)
        n-elems (count data)]
    {:construction {:clj (bench/benchmark-us (vec data))
                    :hamf (bench/benchmark-us (api/vec data))
                    :java (bench/benchmark-us (doto (ArrayList.) (.addAll data)))}
     :cons-obj-ary {:clj (bench/benchmark-us (vec odata))
                    :hamf (bench/benchmark-us (api/immut-list odata))
                    :java (bench/benchmark-us (doto (ArrayList.) (.addAll data)))}
     :access (let [method (fn [vdata]
                            (dotimes [idx n-elems]
                              (.get ^List vdata idx)))
                   clj-d (vec data)
                   hm-d (api/vec data)
                   jv-d (doto (ArrayList.) (.addAll data))]
               {:clj (bench/benchmark-us (method clj-d))
                :hamf (bench/benchmark-us (method hm-d))
                :jv-d (bench/benchmark-us (method jv-d))})
     :reduce (let [method (fn [vdata]
                            (reduce + 0 vdata))
                   clj-d (vec data)
                   hm-d (api/vec data)
                   jv-d (doto (ArrayList.) (.addAll data))]
               {:clj (bench/benchmark-us (method clj-d))
                :hamf (bench/benchmark-us (method hm-d))
                :jv-d (bench/benchmark-us (method jv-d))})
     :to-array (let [clj-d (vec data)
                     hm-d (api/vec data)
                     jv-d (doto (ArrayList.) (.addAll data))]
                 {:clj (bench/benchmark-us (.toArray ^List clj-d))
                  :hamf (bench/benchmark-us (.toArray ^List hm-d))
                  :jv-d (bench/benchmark-us (.toArray ^List jv-d))})}))


(defn vec-data
  [^long n-elems]
  (doto (ArrayList.)
    (.addAll (range n-elems))))


(defn general-persistent-vector
  []
  (log/info "persistent vector perftest")
  {:persistent-vec-1000 (vec-perftest (vec-data 1000))
   :persistent-vec-10000 (vec-perftest (vec-data 10000))
   :persistent-vec-10 (vec-perftest (vec-data 10))})


(defn -main
  [& args]
  (let [perf-data (merge (general-hashmap)
                         (general-persistent-vector))
        vs (System/getProperty "java.version")
        perfs (with-out-str (pp/pprint perf-data))]
    (println "Perf data for "vs "\n" perfs)
    (spit (str "results/jdk-" vs ".edn") perfs))
  (log/info "End - perftest"))

(ns perftest
  (:require [ham-fisted.api :as api]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.benchmark :as bench]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp])
  (:import [java.util HashMap ArrayList Map List]
           [java.util.function BiFunction])
  (:gen-class))


(defn clj-hashmap ^Map [data] (into {} data))
(defn hamf-hashmap ^Map [data] (api/immut-map data))
(defn java-hashmap ^Map [data] (reduce (fn [hm [k v]] (api/assoc! hm k v))
                                       (HashMap.)
                                       data))

(defn hashmap-perftest
  [data]
  {:construction {:clj (bench/benchmark-us (clj-hashmap data))
                  :hamf (bench/benchmark-us (hamf-hashmap data))
                  :java (bench/benchmark-us (java-hashmap data))}
   :access (let [method #(api/fast-reduce (fn [m [k v]] (get m k) m)
                                          %
                                          data)
                 clj-d (clj-hashmap data)
                 hm-d (hamf-hashmap data)
                 jv-d (java-hashmap data)]
             {:clj (bench/benchmark-us (method clj-d))
              :hamf (bench/benchmark-us (method hm-d))
              :java (bench/benchmark-us (method jv-d))})
   :reduce (let [method #(api/fast-reduce (fn [sum [k v]] (+ sum v))
                                          0
                                          %)
                 clj-d (clj-hashmap data)
                 hm-d (hamf-hashmap data)
                 jv-d (java-hashmap data)]
             {:clj (bench/benchmark-us (method clj-d))
              :hamf (bench/benchmark-us (method hm-d))
              :java (bench/benchmark-us (method jv-d))})})

(defn map-data
  [^long n-elems]
  (lznc/->random-access
   (object-array
    (lznc/map
     #(api/vector % %)
     (lznc/repeatedly 1000 #(rand-int 100000))))))


(defn general-hashmap
  []
  (log/info "hashmap perftest")
  {:hashmap-10000 (hashmap-perftest (map-data 10000))
   :hashmap-10 (hashmap-perftest (map-data 10))})





(def union-data
  {:java-hashmap {:construct-fn java-hashmap
                  :merge-fn api/map-union-java-hashmap
                  :reduce-fn api/union-reduce-java-hashmap}
   :hamf-hashmap {:construct-fn hamf-hashmap
                  :merge-fn api/map-union
                  :reduce-fn #(api/union-reduce-maps %1 %2)}

   :clj-hashmap (let [make-merge-fn (fn [bifn]
                                      (fn [lhs rhs]
                                        (.apply ^BiFunction bifn lhs rhs)))]
                  {:construct-fn clj-hashmap
                   :merge-fn #(merge-with (make-merge-fn %1) %2 %3)
                   :reduce-fn #(apply merge-with (make-merge-fn %1) %2)})
   })


(defn benchmark-union
  [^long n-elems mapname]
  (let [{:keys [construct-fn merge-fn]} (union-data mapname)
        hn-elems (quot n-elems 2)
        src-data (map-data n-elems)
        lhs (api/array-list (take hn-elems src-data))
        rhs (api/array-list (drop hn-elems src-data))
        bfn (api/->bi-function +)
        lhs-m (construct-fn lhs)
        rhs-m (construct-fn rhs)
        merged-m (merge-fn bfn lhs-m rhs-m)]
    {:union-disj-μs (:mean-μs (bench/benchmark-us (merge-fn bfn lhs-m rhs-m)))
     :union-μs (:mean-μs (bench/benchmark-us (merge-fn bfn lhs-m merged-m)))
     :name mapname}))


(defn benchmark-union-reduce
  [^long n-elems mapname]
  (let [{:keys [construct-fn reduce-fn]} (union-data mapname)
        hn-elems (quot n-elems 2)
        src-data (map-data n-elems)
        lhs (api/array-list (take hn-elems src-data))
        rhs (api/array-list (drop hn-elems src-data))
        bfn (reify BiFunction
              (apply [this a b]
                (unchecked-add (unchecked-long a)
                               (unchecked-long b))))
        lhs-m (construct-fn lhs)
        rhs-m (construct-fn rhs)
        map-seq (api/vec (interleave (repeat 10 lhs-m) (repeat 10 rhs-m)))]
    {:union-μs (:mean-μs (bench/benchmark-us (reduce-fn bfn map-seq)))
     :name mapname}))


(defn union-perftest
  []
  (log/info "union perftest")
  (let [keys [:java-hashmap :clj-hashmap :hamf-hashmap]]
    {:union-10 (->> keys
                    (map (fn [k] [k (benchmark-union 10 k)]))
                    (into {}))
     :union-10000 (->> keys
                       (map (fn [k] [k (benchmark-union 10000 k)]))
                       (into {}))
     :union-reduce-10 (->> keys
                           (map (fn [k] [k (benchmark-union-reduce 10 k)]))
                           (into {}))
     :union-reduce-10000 (->> keys
                              (map (fn [k] [k (benchmark-union-reduce 10000 k)]))
                              (into {}))}))


(defn vec-perftest
  [data]
  (let [odata (api/object-array data)
        n-elems (count data)]
    {:construction {:clj (bench/benchmark-us (vec data))
                    :hamf (bench/benchmark-us (api/vec data))
                    :java (bench/benchmark-us (api/array-list data))}
     :cons-obj-ary {:clj (bench/benchmark-us (vec odata))
                    :hamf (bench/benchmark-us (api/immut-list odata))
                    :java (bench/benchmark-us (api/array-list odata))}
     :access (let [method (fn [vdata]
                            (dotimes [idx n-elems]
                              (.get ^List vdata idx)))
                   clj-d (vec data)
                   hm-d (api/vec data)
                   jv-d (api/array-list data)]
               {:clj (bench/benchmark-us (method clj-d))
                :hamf (bench/benchmark-us (method hm-d))
                :jv-d (bench/benchmark-us (method jv-d))})
     :reduce (let [method (fn [vdata]
                            (api/fast-reduce + 0 vdata))
                   clj-d (vec data)
                   hm-d (api/vec data)
                   jv-d (api/array-list data)]
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
    (.addAll (api/range n-elems))))


(defn general-persistent-vector
  []
  (log/info "persistent vector perftest")
  {:persistent-vec-1000 (vec-perftest (vec-data 1000))
   :persistent-vec-10000 (vec-perftest (vec-data 10000))
   :persistent-vec-10 (vec-perftest (vec-data 10))})


(defn sequence-summation
  []
  (log/info "sequence perftest")
  {:sequence-summation
   {:map-filter-clj (bench/benchmark-us (->> (clojure.core/range 20000)
                                             (clojure.core/map #(* (long %) 2))
                                             (clojure.core/map #(+ 1 (long %)))
                                             (clojure.core/filter #(== 0 (rem (long %) 3)))
                                             (api/sum)))
    :map-filter-eduction (bench/benchmark-us (->> (api/range 20000)
                                                  (eduction
                                                   (comp
                                                    (clojure.core/map #(* (long %) 2))
                                                    (clojure.core/map #(+ 1 (long %)))
                                                    (clojure.core/filter #(== 0 (rem (long %) 3)))))
                                                  (api/sum)))
    :map-filter-hamf (bench/benchmark-us (->> (api/range 20000)
                                              (lznc/map #(* (long %) 2))
                                              (lznc/map #(+ 1 (long %)))
                                              (lznc/filter #(== 0 (rem (long %) 3)))
                                              (api/sum)))}})


(defn object-array-perftest
  []
  (log/info "obj array perftest")
  (let [init-seq (->> (api/range 20000)
                      (lznc/map #(* (long %) 2))
                      (lznc/map #(+ 1 (long %)))
                      (lznc/filter #(== 0 (rem (long %) 3))))]
    {:obj-array
     {:clj-obj-ary (bench/benchmark-us (object-array init-seq))
      :hamf-obj-ary (bench/benchmark-us (api/object-array init-seq))}}))


(defn shuffle-perftest
  []
  (log/info "shuffle")
  (let [init-data (api/range 10000)]
    {:shuffle
     {:clj-shuffle (bench/benchmark-us (shuffle init-data))
      :hamf-shuffle (bench/benchmark-us (api/shuffle init-data))}}))


(defn sort-perftest
  []
  (log/info "sort")
  (let [init-data (api/shuffle (api/range 10000))]
    {:sort
     {:clj-sort (bench/benchmark-us (sort init-data))
      :hamf-sort (bench/benchmark-us (api/sort init-data))}}))


(defn object-list-perftest
  []
  (log/info "object list")
  (let [init-data (->> (range 20000)
                       (lznc/map #(* (long %) 2))
                       (lznc/map #(+ 1 (long %)))
                       (lznc/filter #(== 0 (rem (long %) 3))))]
    {:object-list
     {:java-array-list (bench/benchmark-us (doto (ArrayList.)
                                             (.addAll init-data)))
      :hamf-alist (bench/benchmark-us (doto (api/object-array-list)
                                        (.addAll init-data)))}}))


(defn -main
  [& args]
  (let [perf-data (merge (general-hashmap)
                         (general-persistent-vector)
                         (union-perftest)
                         (sequence-summation)
                         (object-array-perftest)
                         (shuffle-perftest)
                         (object-list-perftest)
                         (sort-perftest))
        vs (System/getProperty "java.version")
        perfs (with-out-str (pp/pprint perf-data))]
    (println "Perf data for "vs "\n" perfs)
    (spit (str "results/jdk-" vs ".edn") perfs))
  (log/info "End - perftest"))

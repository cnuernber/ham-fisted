(ns perftest
  (:require [ham-fisted.api :as api]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.benchmark :as bench]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.java.shell :as sh]
            [clojure.string :as str])
  (:import [java.util HashMap ArrayList Map List]
           [java.util.function BiFunction])
  (:gen-class))


(defn clj-hashmap ^Map [data] (into {} data))
(defn hamf-hashmap ^Map [data] (api/immut-map data))
(defn java-hashmap ^Map [data] (reduce (fn [hm [k v]] (api/assoc! hm k v))
                                       (HashMap.)
                                       data))


(defn map-data
  [^long n-elems]
  (lznc/->random-access
   (object-array
    (lznc/map
     #(api/vector % %)
     (lznc/repeatedly n-elems #(rand-int 100000))))))


(defn hashmap-perftest
  [data]
  (let [n-elems (count data)]
    [{:clj (bench/benchmark-us (clj-hashmap data))
      :hamf (bench/benchmark-us (hamf-hashmap data))
      :java (bench/benchmark-us (java-hashmap data))
      :test :hashmap-construction
      :n-elems n-elems}
     (let [method #(api/fast-reduce (fn [m [k v]] (get m k) m)
                                    %
                                    data)
           clj-d (clj-hashmap data)
           hm-d (hamf-hashmap data)
           jv-d (java-hashmap data)]
       {:clj (bench/benchmark-us (method clj-d))
        :hamf (bench/benchmark-us (method hm-d))
        :java (bench/benchmark-us (method jv-d))
        :test :hashmap-access
        :n-elems n-elems})
     ;;Fast reduce here avoids paying as much of a tax for reduce via iterator as
     ;;clojure.core/reduce.
     (let [method #(api/fast-reduce (fn [sum [k v]] (+ sum v))
                                    0
                                    %)
           clj-d (clj-hashmap data)
           hm-d (hamf-hashmap data)
           jv-d (java-hashmap data)]
       {:clj (bench/benchmark-us (method clj-d))
        :hamf (bench/benchmark-us (method hm-d))
        :java (bench/benchmark-us (method jv-d))
        :test :hashmap-reduce
        :n-elems n-elems})]))


(defn general-hashmap
  []
  (log/info "hashmap perftest")
  (concat (hashmap-perftest (map-data 10000))
          (hashmap-perftest (map-data 10))))


(def union-data
  {:java {:construct-fn java-hashmap
          :merge-fn api/map-union-java-hashmap
          :reduce-fn api/union-reduce-java-hashmap}
   :hamf {:construct-fn hamf-hashmap
          :merge-fn api/map-union
          :reduce-fn #(api/union-reduce-maps %1 %2)}

   :clj (let [make-merge-fn (fn [bifn]
                              (fn [lhs rhs]
                                (.apply ^BiFunction bifn lhs rhs)))]
          {:construct-fn clj-hashmap
           :merge-fn #(merge-with (make-merge-fn %1) %2 %3)
           :reduce-fn #(apply merge-with (make-merge-fn %1) %2)})})


(defn benchmark-union
  [^long n-elems]
  (->>
   (let [hn-elems (quot n-elems 2)
         src-data (map-data n-elems)
         lhs (api/array-list (take hn-elems src-data))
         rhs (api/array-list (drop hn-elems src-data))
         bfn (api/->bi-function +)]
     (for [testname [:union-disj :union]]
       (apply merge {:test testname
                     :n-elems n-elems}
              (for [sysname [:clj :java :hamf]]
                (let [{:keys [construct-fn merge-fn]} (union-data sysname)
                      lhs-m (construct-fn lhs)
                      rhs-m (construct-fn rhs)]
                  {sysname (bench/benchmark-us (merge-fn bfn lhs-m rhs-m))})))))))


(defn benchmark-union-reduce
  [^long n-elems]
  (let [hn-elems (quot n-elems 2)
        src-data (map-data n-elems)
        lhs (api/array-list (take hn-elems src-data))
        rhs (api/array-list (drop hn-elems src-data))
        bfn (reify BiFunction
              (apply [this a b]
                (unchecked-add (unchecked-long a)
                               (unchecked-long b))))]
    [(apply merge
            {:test :union-reduce
             :n-elems n-elems}
            (for [sysname [:clj :java :hamf]]
              (let [{:keys [construct-fn reduce-fn]} (union-data sysname)
                    lhs-m (construct-fn lhs)
                    rhs-m (construct-fn rhs)
                    map-seq (api/vec (interleave (repeat 10 lhs-m) (repeat 10 rhs-m)))]
                {sysname (bench/benchmark-us (reduce-fn bfn map-seq))})))]))


(defn union-perftest
  []
  (log/info "union perftest")
  (concat
   (benchmark-union 10)
   (benchmark-union 10000)
   (benchmark-union-reduce 10)
   (benchmark-union-reduce 10000)))


(defn vec-data
  [^long n-elems]
  (doto (ArrayList.)
    (.addAll (api/range n-elems))))


(defn vec-perftest
  [data]
  (let [odata (api/object-array data)
        n-elems (count data)]
    [{:clj (bench/benchmark-us (vec data))
      :hamf (bench/benchmark-us (api/vec data))
      :java (bench/benchmark-us (api/array-list data))
      :n-elems n-elems
      :test :vector-construction}
     {:clj (bench/benchmark-us (vec odata))
      :hamf (bench/benchmark-us (api/vec odata))
      :java (bench/benchmark-us (api/array-list odata))
      :n-elems n-elems
      :test :vector-cons-obj-array}
     (let [method (fn [vdata]
                    (dotimes [idx 10000]
                      (.get ^List vdata (unchecked-int (rem idx n-elems)))))
           clj-d (vec data)
           hm-d (api/vec data)
           jv-d (api/array-list data)]
       {:clj (bench/benchmark-us (method clj-d))
        :hamf (bench/benchmark-us (method hm-d))
        :java (bench/benchmark-us (method jv-d))
        :n-elems n-elems
        :test :vector-access})
     (let [method (fn [vdata]
                    (api/fast-reduce + 0 vdata))
           clj-d (vec data)
           hm-d (api/vec data)
           jv-d (api/array-list data)]
       {:clj (bench/benchmark-us (method clj-d))
        :hamf (bench/benchmark-us (method hm-d))
        :java (bench/benchmark-us (method jv-d))
        :n-elems n-elems
        :test :vector-reduce})
     (let [clj-d (vec data)
           hm-d (api/vec data)
           jv-d (doto (ArrayList.) (.addAll data))]
       {:clj (bench/benchmark-us (.toArray ^List clj-d))
        :hamf (bench/benchmark-us (.toArray ^List hm-d))
        :java (bench/benchmark-us (.toArray ^List jv-d))
        :n-elems n-elems
        :test :vector-to-array})]))


(defn general-persistent-vector
  []
  (log/info "persistent vector perftest")
  (concat (vec-perftest (vec-data 10))
          (vec-perftest (vec-data 10000))))


(defn sequence-summation
  []
  (log/info "sequence perftest")
  [{:n-elems 20000
    :test :sequence-summation
    :clj (bench/benchmark-us (->> (clojure.core/range 20000)
                                  (clojure.core/map #(* (long %) 2))
                                  (clojure.core/map #(+ 1 (long %)))
                                  (clojure.core/filter #(== 0 (rem (long %) 3)))
                                  (api/sum)))
    :eduction (bench/benchmark-us (->> (api/range 20000)
                                       (eduction
                                        (comp
                                         (clojure.core/map #(* (long %) 2))
                                         (clojure.core/map #(+ 1 (long %)))
                                         (clojure.core/filter #(== 0 (rem (long %) 3)))))
                                       (api/sum)))
    :hamf (bench/benchmark-us (->> (api/range 20000)
                                   (lznc/map #(* (long %) 2))
                                   (lznc/map #(+ 1 (long %)))
                                   (lznc/filter #(== 0 (rem (long %) 3)))
                                   (api/sum)))}])


(defn object-array-perftest
  []
  (log/info "obj array perftest")
  (let [init-seq (->> (api/range 20000)
                      (lznc/map #(* (long %) 2))
                      (lznc/map #(+ 1 (long %)))
                      (lznc/filter #(== 0 (rem (long %) 3))))]
    [{:n-elems 20000
      :test :object-array
      :clj (bench/benchmark-us (object-array init-seq))
      :hamf (bench/benchmark-us (api/object-array init-seq))}]))


(defn shuffle-perftest
  []
  (log/info "shuffle")
  (let [init-data (api/range 10000)]
    ;;Test is heavily gated on java.util.Random implementation
    [{:n-elems 10000
      :test :shuffle
      :clj (bench/benchmark-us (shuffle init-data))
      :hamf (bench/benchmark-us (api/shuffle init-data))}]))


(defn sort-perftest
  []
  (log/info "sort")
  (let [init-data (api/shuffle (api/range 10000))]
    [{:test :sort
      :n-elems 10000
      :clj (bench/benchmark-us (sort init-data))
      :hamf (bench/benchmark-us (api/sort init-data))}]))


(defn frequencies-perftest
  []
  (let [n-elems 10000
        tdata (mapv #(rem (unchecked-long %) 7) (range n-elems))]
    [{:n-elems n-elems
      :test :frequencies
      :clj (bench/benchmark-us (frequencies tdata))
      :hamf (bench/benchmark-us (api/frequencies tdata))}]))


(defn object-list-perftest
  []
  (log/info "object list")
  (let [n-elems 20000
        init-data (->> (range n-elems)
                       (lznc/map #(* (long %) 2))
                       (lznc/map #(+ 1 (long %)))
                       (lznc/filter #(== 0 (rem (long %) 3))))]
    [{:test :object-list
      :n-elems n-elems
      :java (bench/benchmark-us (doto (ArrayList.)
                                  (.addAll init-data)))
      :hamf (bench/benchmark-us (doto (api/object-array-list)
                                  (.addAll init-data)))}]))


(defn group-by-perftest
  []
  (let [n-elems 10000]
    [{:n-elems n-elems
      :test :group-by
      :clj (bench/benchmark-us (clojure.core/group-by #(rem (unchecked-long %1) 7)
                                                      (api/range n-elems)))
      :hamf (bench/benchmark-us (api/group-by #(rem (unchecked-long %1) 7)
                                              (api/range n-elems)))}]))


(defn group-by-reduce-perftest
  []
  (let [n-elems 10000]
    [{:n-elems n-elems
      :test :group-by-reduce
      :clj (bench/benchmark-us (->> (clojure.core/group-by #(rem (unchecked-long %1) 7)
                                                           (api/range n-elems))
                                    (map (fn [[k v]]
                                           [k (first v)]))
                                    (into {})))
      :hamf (bench/benchmark-us (api/group-by-reduce #(rem (unchecked-long %1) 7)
                                                     (fn ([l r] l) ([l] l))
                                                     (api/range n-elems)))}]))


(defn update-values-perftest
  []
  (let [n-elems 1000
        data (lznc/map #(api/vector % %) (range n-elems))
        clj-map (into {} data)
        hamf-map (api/immut-map data)]
    [{:n-elems n-elems
      :test :update-values
      :clj (bench/benchmark-us (-> (reduce (fn [m [k v]]
                                             (assoc! m k (unchecked-inc v)))
                                           (transient {})
                                           clj-map)
                                   (persistent!)))
      :hamf (bench/benchmark-us (api/update-values hamf-map
                                                   (api/bi-function k v (unchecked-inc v))))}]))

(defn machine-name
  []
  (.getHostName (java.net.InetAddress/getLocalHost)))


(defn git-sha
  []
  (-> (sh/sh "git" "rev-parse" "--short" "HEAD")
      :out
      (str/trim)))


(defn data->dataset
  [profile-data]
  (->> profile-data
       (mapv (fn [data-map]
               (api/mapmap (fn [[k v]]
                             [k (if (map? v)
                                  (v :mean-μs)
                                  v)])
                           data-map)))))


(defn normalize-rows
  [dataset]
  (let [system-keys #{:clj :hamf :java :eduction}]
    (->> dataset
         (mapv (fn [row]
                 (let [norm-factor (double (get row :clj (get row :java)))]
                   (api/assoc
                    (api/mapmap (fn [[k v]]
                                  [k (if (system-keys k)
                                       (/ v norm-factor)
                                       v)]) row)
                    :norm-factor-μs norm-factor)))))))


(defn profile
  []
  (api/concatv (general-hashmap)
               (general-persistent-vector)
               (union-perftest)
               (sequence-summation)
               (object-array-perftest)
               (shuffle-perftest)
               (object-list-perftest)
               (sort-perftest)
               (frequencies-perftest)
               (group-by-perftest)
               (group-by-reduce-perftest)
               (update-values-perftest)))


(defn process-dataset
  [dataset]
  (->> dataset
       (data->dataset)
       (normalize-rows)
       (sort-by :test)
       (vec)))


(def column-order [:test :n-elems :java :clj :eduction :hamf :norm-factor-μs])


(defn print-dataset
  [dataset]
  (pp/print-table column-order dataset))


(defn -main
  [& args]
  (let [perf-data (process-dataset (profile))
        vs (System/getProperty "java.version")
        mn (machine-name)
        gs (git-sha)
        fname (str "results/" gs "-" mn "-jdk-" vs ".edn")]
    (print-dataset perf-data)
    (println "Results stored to:" fname)
    (spit fname (with-out-str (pp/pprint {:machine-name mn :git-sha gs :jdk-version vs :dataset perf-data})))))

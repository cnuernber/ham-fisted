(ns perftest
  (:require [ham-fisted.api :as api]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.benchmark :as bench]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as dfn]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [criterium.core :as crit])
  (:import [java.util HashMap ArrayList Map List Map$Entry]
           [java.util.function BiFunction]
           [ham_fisted IMutList]
           [clojure.lang PersistentHashMap])
  (:gen-class))


(defn clj-hashmap ^Map [data] (into {} data))
(defn hamf-hashmap ^Map [data] (api/immut-map data))
(defn java-hashmap ^Map [data] (api/java-hashmap data))


(defn map-data
  [^long n-elems]
  (lznc/->random-access
   (lznc/object-array
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
     (let [method #(api/fast-reduce (fn [m tuple] (get m (nth tuple 1)) m)
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
     ;;Additionally, destructuring the java hashmap's key-value pair costs and exorbitant
     ;;amount.
     (let [method #(api/fast-reduce (fn [^long sum ^long v] (+ sum v))
                                    0
                                    (api/map-values %))
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


;;This test is designed to test deserialization performance of various map sizes.
;;assuming the best way to serialize map data is a flat object array of
;;key-value data and the best way to create this is reduce which is profiled above.
(defn hashmap-construction-obj-ary
  [^long n-elems]
  (log/info "hashmap obj array construction")
  (let [map-data (-> (reduce (fn [l kv]
                               (.add ^List l (nth kv 0))
                               (.add ^List l (nth kv 1))
                               l)
                             (api/object-array-list (* n-elems 2))
                             (map-data n-elems))
                     (api/object-array))]
    [{:n-elems n-elems
      :test :hashmap-cons-obj-ary
      :clj (bench/benchmark-us (PersistentHashMap/create map-data))
      :hamf (bench/benchmark-us (api/immut-map map-data))}]))


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
  ;;This is definitely 'cheating' in some dimensions but if we know the end result
  ;;is a double summation then if we typehint our entire pathway with doubles that will
  ;;allow the entire pathway to avoid boxing.  The predicate's return value must be
  ;;an object, however, not a long or a double so there is some minimal boxing.
  (let [mfn1 (fn ^double [^double v] (* v 2))
        mfn2 (fn ^double [^double v] (+ v 1))
        pred (fn [^double v] (== 0 (rem (long v) 3)))]
    (for [n-elems [100 1000 100000]]
      [{:n-elems n-elems
        :test :sequence-summation
        :clj (bench/benchmark-us (->> (clojure.core/range n-elems)
                                      (clojure.core/map mfn1)
                                      (clojure.core/map mfn2)
                                      (clojure.core/filter pred)
                                      (api/sum)))
        :eduction (bench/benchmark-us (->> (api/range n-elems)
                                           (eduction
                                            (comp
                                             (clojure.core/map mfn1)
                                             (clojure.core/map mfn2)
                                             (clojure.core/filter pred)))
                                           (api/sum)))
        :hamf (bench/benchmark-us (->> (api/range n-elems)
                                       (lznc/map mfn1)
                                       (lznc/map mfn2)
                                       (lznc/filter pred)
                                       (api/sum)))}])))


(defn stream-summation
  []
  (for [n-elems [10 10000 10000000]]
    [{:n-elems n-elems
      :test :stream-summation
      :hamf (bench/benchmark-us (api/sum-stable-nelems (api/range n-elems)))
      :java-stream (bench/benchmark-us (api/sum-stable-nelems-stream (api/range n-elems)))}]))


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
      ;;The default api sort sorts nil first.  This is more correct but an unfair comparison.
      :hamf (bench/benchmark-us (api/sort nil init-data))}]))


(defn sort-double-array
  []
  (log/info "sort doubles")
  (let [init-data (api/double-array (api/shuffle (api/range 10000)))]
    [{:test :sort-doubles
      :n-elems 10000
      :clj (bench/benchmark-us (sort init-data))
      ;;Fair comparison means we avoid nan-first behavior
      :hamf (bench/benchmark-us (api/sort nil init-data))}]))


(defn sort-int-array
  []
  (log/info "sort ints")
  (let [init-data (api/int-array (api/shuffle (api/range 10000)))]
    [{:test :sort-ints
      :n-elems 10000
      :clj (bench/benchmark-us (sort init-data))
      :hamf (bench/benchmark-us (api/sort nil init-data))}]))


(defn frequencies-perftest
  []
  (log/info "frequencies")
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
        init-data (->> (api/range n-elems)
                       (lznc/map #(* (long %) 2))
                       (lznc/map #(+ 1 (long %)))
                       (lznc/filter #(== 0 (rem (long %) 3))))]
    [{:test :object-list-red-1
      :n-elems n-elems
      :java (bench/benchmark-us (doto (ArrayList.)
                                  (.addAll init-data)))
      :hamf (bench/benchmark-us (api/object-array-list init-data))}]))




(defn int-list-perftest
  []
  (log/info "int list")
  (let [n-elems 20000
        init-data (->> (api/range n-elems)
                       (lznc/map (fn ^long [^long v] (* v 2)))
                       (lznc/map (fn ^long [^long v] (+ v 1)))
                       (lznc/filter (fn [^long v] (== 0 (rem v 3)))))]
    [{:test :int-list
      :n-elems n-elems
      :java (bench/benchmark-us (doto (ArrayList.)
                                  (.addAll init-data)))
      :hamf (bench/benchmark-us (api/int-array-list init-data))
      :dtype (bench/benchmark-us (doto (dtype/make-list :int32)
                                   (.addAll init-data)))}]))


(defn group-by-perftest
  []
  (log/info "group-by")
  (let [n-elems 10000]
    [{:n-elems n-elems
      :test :group-by
      :clj (bench/benchmark-us (clojure.core/group-by #(rem (unchecked-long %1) 7)
                                                      (api/range n-elems)))
      :hamf (bench/benchmark-us (api/group-by #(rem (unchecked-long %1) 7)
                                              (api/range n-elems)))}]))


(defn group-by-reduce-perftest
  []
  (log/info "group-by-reduce")
  ;;Choose a large-ish prime.  This emphasizes not only an efficient map datastructure but
  ;;also the much faster finalization step.
  (let [n-elems 10000]
    [{:n-elems n-elems
      :test :group-by-reduce
      :clj (bench/benchmark-us (->> (clojure.core/group-by #(rem (unchecked-long %1) 337)
                                                           (api/range n-elems))
                                    (map (fn [[k v]]
                                           [k (first v)]))
                                    (into {})))
      :hamf (bench/benchmark-us (api/group-by-reduce #(rem (unchecked-long %1) 337)
                                                     (fn [l r] l)
                                                     (api/range n-elems)))}]))


(defn update-values-perftest
  []
  (log/info "update-values")
  (let [n-elems 1000
        data (lznc/map #(api/vector % %) (range n-elems))
        clj-map (into {} data)
        hamf-map (api/immut-map data)]
    [{:n-elems n-elems
      :test :update-values
      :clj (bench/benchmark-us (-> (reduce (fn [m e]
                                             (assoc! m (key e) (unchecked-inc (val e))))
                                           (transient {})
                                           clj-map)
                                   (persistent!)))
      :hamf (bench/benchmark-us (api/update-values
                                 hamf-map
                                 (api/bi-function k v (unchecked-inc v))))}]))


(defn mapmap-perftest
  []
  (log/info "mapmap")
  (let [n-elems 1000
        data (lznc/map #(api/vector % %) (range n-elems))
        clj-map (into {} data)
        hamf-map (api/immut-map data)
        ;;Destructuring is by far the most expensive part of this operation
        ;;so we avoid it to get a better measure of the time.
        map-fn (fn [e] [(key e) (unchecked-inc (val e))])]
    [{:n-elems n-elems
      :test :mapmap
      :clj (bench/benchmark-us (into {} (comp (map map-fn) (remove nil?)) clj-map))
      :hamf (bench/benchmark-us (api/mapmap map-fn hamf-map))}]))


(defn assoc-in-perftest
  []
  (log/info "assoc-in")
  [{:n-elems 5
    :test :assoc-in-nil
    :clj (bench/benchmark-us (assoc-in nil [:a :b :c :d :e] 1))
    :hamf (bench/benchmark-us (api/assoc-in nil [:a :b :c :d :e] 1))}
   (let [data {:a {:b {:c {:d {}}}}}]
     {:n-elems 5
      :test :assoc-in
      :clj (bench/benchmark-us (assoc-in data [:a :b :c :d :e] 1))
      :hamf (bench/benchmark-us (api/assoc-in data [:a :b :c :d :e] 1))})])


(defn update-in-perftest
  []
  (log/info "update-in")
  (let [updater (fn [v] (unchecked-inc (or v 1)))]
    [{:n-elems 5
      :test :update-in-nil
      :clj (bench/benchmark-us (update-in nil [:a :b :c :d :e] updater))
      :hamf (bench/benchmark-us (api/update-in nil [:a :b :c :d :e] updater))}
     (let [data (api/assoc-in nil [:a :b :c :d :e] 2)]
       {:n-elems 5
        :test :update-in
        :clj (bench/benchmark-us (update-in data [:a :b :c :d :e] updater))
        :hamf (bench/benchmark-us (api/update-in data [:a :b :c :d :e] updater))})]))


(defn get-in-perftest
  []
  (log/info "get-in")
  (let [data {:a 1 :b 2 :c 3 :d 4 :e 5}]
    [{:n-elems 5
      :test :get-in
      :clj (bench/benchmark-us (get-in data [:a :b :c :d :e]))
      :hamf (bench/benchmark-us (api/get-in data [:a :b :c :d :e]))}]))


(defn concatv-perftest
  []
  (log/info "concatv")
  (let [data [[] (list 1 2 3) nil nil
              (clojure.core/vector 1 2 3 4 5) (api/array-list [1 2 3 4])
              (api/vec (api/range 50))]]
    [{:n-elems 100
      :test :concatv
      :clj (bench/benchmark-us (clojure.core/vec (apply concat data)))
      :hamf (bench/benchmark-us (apply api/concatv data))}]))


(defn stable-sum-perftest
  []
  (log/info "stable summation - dtype(clj) vs. hamf")
  (for [n-elems [100 1000 1000000]]
    (let [n-elems (long n-elems)
          data (api/double-array (range n-elems))]
      {:n-elems n-elems
       :test :stable-summation
       :hamf (bench/benchmark-us (api/sum data))
       :clj (bench/benchmark-us (dfn/sum data))})))


(defn unstable-sum-perftest
  []
  (log/info "unstable summation - dtype(clj) vs. hamf")
  (for [n-elems [100 1000 1000000]]
    (let [n-elems (long n-elems)
          data (api/double-array (range n-elems))]
      {:n-elems n-elems
       :test :unstable-summation
       :hamf (bench/benchmark-us (api/sum-fast data))
       :clj (bench/benchmark-us (dfn/sum-fast data))})))


(defn pmap-perftest
  []
  (log/info "pmap perftest")
  (for [n-elems [100 1000]]
    (let [n-elems (long n-elems)]
      {:n-elems n-elems
       :test :pmap
       :clj (bench/benchmark-us (count (clojure.core/pmap inc (api/range n-elems))))
       :hamf (bench/benchmark-us (count (api/pmap inc (api/range n-elems))))})))


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
  (api/concatv
   ;; (general-hashmap)
   ;; (hashmap-construction-obj-ary 4)
   ;; (hashmap-construction-obj-ary 10)
   ;; (hashmap-construction-obj-ary 1000)
   ;; (general-persistent-vector)
   ;; (union-perftest)
   ;; (sequence-summation)
   ;; (object-array-perftest)
   ;; (shuffle-perftest)
   ;; (object-list-perftest)
   ;; (int-list-perftest)
   ;; (sort-perftest)
   ;; (sort-double-array)
   ;; (sort-int-array)
   ;; (frequencies-perftest)
   ;; (group-by-perftest)
   ;; (group-by-reduce-perftest)
   ;; (update-values-perftest)
   ;; (mapmap-perftest)
   ;; (assoc-in-perftest)
   ;; (update-in-perftest)
   ;; (get-in-perftest)
   ;; (concatv-perftest)
   ;;technically a dtype-next vs. hamf test
   (stable-sum-perftest)
   (unstable-sum-perftest)
   ))



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
  ;;shutdown test
  (println "summation" (api/sum (api/pmap inc (api/range 1000))))
  #_(let [perf-data (process-dataset (profile))
        vs (System/getProperty "java.version")
        mn (machine-name)
        gs (git-sha)
        fname (str "results/" gs "-" mn "-jdk-" vs ".edn")]
    (print-dataset perf-data)
    (println "Results stored to:" fname)
    ;;(spit fname (with-out-str (pp/pprint {:machine-name mn :git-sha gs :jdk-version vs :dataset perf-data})))
    )
  (println "exiting"))

(ns perftest
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.benchmark :refer [benchmark-us] :as bench]
            [clojure.data.int-map :as i]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as dfn]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [criterium.core :as crit]
            [clj-memory-meter.core :as mm])
  (:import [java.util HashMap ArrayList Map List Map$Entry]
           [java.util.function BiFunction]
           [ham_fisted IMutList Sum$SimpleSum Sum BitmapTrieCommon Consumers$IncConsumer
            BitmapTrieCommon$MapSet ImmutValues]
           [clojure.lang PersistentHashMap])
  (:gen-class))



(defn long-map-data
  [^long n-elems]
  (lznc/->random-access
   (lznc/object-array
    (lznc/map
     #(hamf/vector % %)
     (lznc/repeatedly n-elems #(long (rand-int 100000)))))))


(defn kwd-map-data
  [^long n-elems]
  (lznc/->random-access
   (lznc/object-array
    (lznc/map
     #(hamf/vector (keyword (str %)) %)
     (lznc/repeatedly n-elems #(rand-int 100000))))))


(def map-non-numeric-constructors
  {:clj #(into {} %)
   :hamf-trie hamf/mut-trie-map
   :hamf-hashmap hamf/mut-map
   :java hamf/java-hashmap})


(def map-numeric-constructors
  {:hamf-long-map hamf/mut-long-hashtable-map
   :clj-int-map #(into (i/int-map) %)})

(defn map-constructors
  [numeric?]
  (merge map-non-numeric-constructors
         (when numeric?
           map-numeric-constructors)))


(defn initial-maps
  [mapsc data]
  (hamf/mapmap #(vector (key %) ((val %) data)) mapsc))


(defn hashmap-perftest
  [data]
  (let [numeric? (number? (ffirst data))
        n-elems (count data)
        _ (log/info (str "Running map benchmark on " (if numeric?
                                                       "numeric "
                                                       "non-numeric ")
                         "data with n=" n-elems))
        map-constructors (map-constructors numeric?)
        map-data (initial-maps map-constructors data)]
    [(merge (hamf/mapmap (fn [entry]
                           [(key entry) (benchmark-us ((val entry) data))])
                         map-constructors)
            {:n-elems n-elems :test :hashmap-construction :numeric? numeric?} )
     (merge (hamf/mapmap #(vector (key %)
                                  (benchmark-us
                                   (reduce (fn [acc data]
                                             (.get ^Map acc (data 0)) acc)
                                           (val %)
                                           data)))
                         map-data)
            {:n-elems n-elems :test :hashmap-access :numeric? numeric?})
     (merge (hamf/mapmap #(vector (key %)
                                  (benchmark-us
                                   (fn [acc map]
                                     (reduce (fn [kv] kv)
                                             nil
                                             (val %)))))
                         map-data)
            {:n-elems n-elems :test :hashmap-reduction :numeric? numeric?})
     (merge (hamf/mapmap #(vector (key %) (mm/measure (val %) :bytes true))
                         map-data)
            {:n-elems n-elems :test :hashmap-bytes  :numeric? numeric?})]))


(defn- spit-data
  [testname data]
  (let [fname (str "results/" testname ".edn")]
    (io/make-parents fname)
    (spit fname (pr-str data))))


(defn general-hashmap
  []
  (->> (for [n-elems [4 10 100 1000 10000 1000000
                      ]
             numeric? [true false
                       ]]
         (hashmap-perftest (if numeric?
                             (long-map-data n-elems)
                             (kwd-map-data n-elems))))
       (lznc/apply-concat)
       (vec)
       (spit-data "general-hashmap")))


(defn random-updates
  []
  (->> (for [n-elems [ 4 10
                      100
                       1000 10000 1000000
                      ]
             numeric? [true false
                       ]
             ]
         (do
           (log/info (str "random-update benchmark on " (if numeric?
                                                          "numeric "
                                                          "non-numeric ")
                          "data with n=" n-elems))
           (let [cycle-size 1000
                 data (vec (take (max cycle-size n-elems) (cycle (repeatedly (min n-elems cycle-size) #(long (rand-int 1000))))))
                 data (if numeric? data (map (comp keyword str) data))
                 init-maps (initial-maps (map-constructors numeric?) nil)]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (val kv)
                                         immut-path? (instance? clojure.lang.IPersistentMap m)]
                                     [(key kv)
                                      (if immut-path?
                                        (benchmark-us (reduce (fn [acc v]
                                                                (assoc! acc v (unchecked-inc (get acc v 0))))
                                                              (transient m)
                                                              data))
                                        (let [cfn (hamf/function _v (Consumers$IncConsumer.))]
                                          (benchmark-us (reduce (fn [^Map acc v]
                                                                  (.inc ^Consumers$IncConsumer
                                                                        (.computeIfAbsent acc v cfn))
                                                                  acc)
                                                                m
                                                                data))))]))
                                 init-maps)
                    {:n-elems n-elems :numeric? numeric? :test :random-update}))))
       (vec)
       (spit-data "random-update")))


(defn union-overlapping
  []
  (->> (for [n-elems [4 10
                      100
                      1000 10000 1000000
                      ]
             numeric? [true false
                       ]
             ]
         (do
           (log/info (str "union-overlapping benchmark on " (if numeric?
                                                              "numeric "
                                                              "non-numeric ")
                          "data with n=" n-elems))
           (let [data (if numeric? (long-map-data n-elems) (kwd-map-data n-elems))
                 constructors (map-constructors numeric?)
                 init-maps (initial-maps constructors data)
                 merge-bfn (hamf/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (val kv)]
                                     [(key kv)
                                      (cond
                                        (instance? clojure.lang.IPersistentMap m)
                                        (benchmark-us (merge-with merge-bfn m m))
                                        (instance? BitmapTrieCommon$MapSet m)
                                        (benchmark-us (.union ^BitmapTrieCommon$MapSet m m merge-bfn))
                                        :else
                                        (let [map-c (get constructors (key kv))]
                                          (benchmark-us (reduce (fn [^Map acc kv]
                                                                  (.merge acc (key kv) (val kv) merge-bfn)
                                                                  acc)
                                                                (map-c m)
                                                                m))))]))
                                 init-maps)
                    {:n-elems n-elems :numeric? numeric? :test :union-overlapping}))))
       (vec)
       (spit-data "union-overlapping")
       ))


(defn union-disj
  []
  (->> (for [n-elems [4 10
                      100
                      1000 10000 1000000
                      ]
             numeric? [true false
                       ]
             ]
         (do
           (log/info (str "union-disj benchmark on " (if numeric?
                                                              "numeric "
                                                              "non-numeric ")
                          "data with n=" n-elems))
           (let [data (if numeric? (long-map-data n-elems) (kwd-map-data n-elems))
                 lhs-data (vec (take (quot n-elems 2) data))
                 rhs-data (vec (drop (- n-elems (count lhs-data)) data))
                 constructors (map-constructors numeric?)
                 lhs-maps (initial-maps constructors lhs-data)
                 rhs-maps (initial-maps constructors rhs-data)
                 merge-bfn (hamf/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [lhs (val kv)
                                         rhs (rhs-maps (key kv))]
                                     [(key kv)
                                      (cond
                                        (instance? clojure.lang.IPersistentMap lhs)
                                        (benchmark-us (merge-with merge-bfn lhs rhs))
                                        (instance? BitmapTrieCommon$MapSet lhs)
                                        (benchmark-us (.union ^BitmapTrieCommon$MapSet lhs rhs merge-bfn))
                                        :else
                                        (let [map-c (get constructors (key kv))]
                                          (benchmark-us (reduce (fn [^Map acc kv]
                                                                  (.merge acc (key kv) (val kv) merge-bfn)
                                                                  acc)
                                                                (map-c lhs)
                                                                rhs))))]))
                                 lhs-maps)
                    {:n-elems n-elems :numeric? numeric? :test :union-disj}))))
       (vec)
       (spit-data "union-disj")
       ))


(defn union-reduce
  []
  (->> (for [n-elems [ 4 10
                      100
                       1000 10000 1000000
                      ]
             numeric? [true false
                       ]
             ]
         (do
           (log/info (str "union-reduce benchmark on " (if numeric?
                                                              "numeric "
                                                              "non-numeric ")
                          "data with n=" n-elems))
           (let [data (if numeric? (long-map-data n-elems) (kwd-map-data n-elems))
                 constructors (map-constructors numeric?)
                 init-maps (initial-maps constructors data)
                 merge-bfn (hamf/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (val kv)
                                         mseq (vec (repeat 16 m))]
                                     [(key kv)
                                      (cond
                                        (instance? clojure.lang.IPersistentMap m)
                                        (benchmark-us (reduce #(merge-with merge-bfn %1 %2)
                                                              m
                                                              mseq))
                                        (instance? BitmapTrieCommon$MapSet m)
                                        (benchmark-us (reduce #(.union ^BitmapTrieCommon$MapSet %1 %2 merge-bfn)
                                                              m
                                                              mseq))
                                        :else
                                        (let [map-c (get constructors (key kv))]
                                          (benchmark-us (reduce (fn [^Map acc m]
                                                                  (reduce (fn [acc kv]
                                                                            (.merge acc (key kv) (val kv) merge-bfn)
                                                                            acc)
                                                                          acc
                                                                          m))
                                                                (map-c m)
                                                                mseq))))]))
                                 init-maps)
                    {:n-elems n-elems :numeric? numeric? :test :union-reduce}))))
       (vec)
       (spit-data "union-reduce")))


(defn update-values
  []
  (->> (for [n-elems [ 4 10
                      100
                       1000 10000 1000000
                      ]
             numeric? [true false
                       ]
             ]
         (do
           (log/info (str "update-values benchmark on " (if numeric?
                                                          "numeric "
                                                          "non-numeric ")
                          "data with n=" n-elems))
           (let [data (if numeric? (long-map-data n-elems) (kwd-map-data n-elems))
                 constructors (map-constructors numeric?)
                 init-maps (initial-maps constructors data)
                 update-bfn (hamf/bi-function k v (unchecked-inc v))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (val kv)]
                                     [(key kv)
                                      (cond
                                        ;;for whatever reason update-vals isn't found during uberjar build
                                        (instance? clojure.lang.IPersistentMap m)
                                        (benchmark-us (update-vals m unchecked-inc))
                                        (instance? ImmutValues m)
                                        (benchmark-us (.immutUpdateValues ^ImmutValues m update-bfn))
                                        :else
                                        (benchmark-us (.replaceAll ^Map ((constructors (key kv)) m) update-bfn)))]))
                                 init-maps)
                    {:n-elems n-elems :numeric? numeric? :test :update-values}))))
       (vec)
       (spit-data "update-values")))




;;this test is designed to test deserialization performance of various map sizes.
;;assuming the best way to serialize map data is a flat object array of
;;key-value data and the best way to create this is reduce which is profiled above.
;; (defn hashmap-construction-obj-ary
;;   [^long n-elems]
;;   (log/info "hashmap obj array construction")
;;   (let [map-data (-> (reduce (fn [l kv]
;;                                (.add ^List l (nth kv 0))
;;                                (.add ^List l (nth kv 1))
;;                                l)
;;                              (api/object-array-list (* n-elems 2))
;;                              (map-data n-elems))
;;                      (hamf/object-array))]
;;     [{:n-elems n-elems
;;       :test :hashmap-cons-obj-ary
;;       :clj (bench/benchmark-us (PersistentHashMap/create map-data))
;;       :hamf (bench/benchmark-us (api/immut-map map-data))}]))


;; (def union-data
;;   {:java {:construct-fn java-hashmap
;;           :merge-fn api/map-union-java-hashmap
;;           :reduce-fn api/union-reduce-java-hashmap}
;;    :hamf {:construct-fn hamf-hashmap
;;           :merge-fn api/map-union
;;           :reduce-fn #(api/union-reduce-maps %1 %2)}

;;    :clj (let [make-merge-fn (fn [bifn]
;;                               (fn [lhs rhs]
;;                                 (.apply ^BiFunction bifn lhs rhs)))]
;;           {:construct-fn clj-hashmap
;;            :merge-fn #(merge-with (make-merge-fn %1) %2 %3)
;;            :reduce-fn #(apply merge-with (make-merge-fn %1) %2)})})


;; (defn benchmark-union
;;   [^long n-elems]
;;   (->>
;;    (let [hn-elems (quot n-elems 2)
;;          src-data (map-data n-elems)
;;          lhs (api/array-list (take hn-elems src-data))
;;          rhs (api/array-list (drop hn-elems src-data))
;;          bfn (api/->bi-function +)]
;;      (for [testname [:union-disj :union]]
;;        (apply merge {:test testname
;;                      :n-elems n-elems}
;;               (for [sysname [:clj :java :hamf]]
;;                 (let [{:keys [construct-fn merge-fn]} (union-data sysname)
;;                       lhs-m (construct-fn lhs)
;;                       rhs-m (construct-fn rhs)]
;;                   {sysname (bench/benchmark-us (merge-fn bfn lhs-m rhs-m))})))))))


;; (defn benchmark-union-reduce
;;   [^long n-elems]
;;   (let [hn-elems (quot n-elems 2)
;;         src-data (map-data n-elems)
;;         lhs (api/array-list (take hn-elems src-data))
;;         rhs (api/array-list (drop hn-elems src-data))
;;         bfn (reify BiFunction
;;               (apply [this a b]
;;                 (unchecked-add (unchecked-long a)
;;                                (unchecked-long b))))]
;;     [(apply merge
;;             {:test :union-reduce
;;              :n-elems n-elems}
;;             (for [sysname [:clj :java :hamf]]
;;               (let [{:keys [construct-fn reduce-fn]} (union-data sysname)
;;                     lhs-m (construct-fn lhs)
;;                     rhs-m (construct-fn rhs)
;;                     map-seq (api/vec (interleave (repeat 10 lhs-m) (repeat 10 rhs-m)))]
;;                 {sysname (bench/benchmark-us (reduce-fn bfn map-seq))})))]))


;; (defn union-perftest
;;   []
;;   (log/info "union perftest")
;;   (concat
;;    (benchmark-union 10)
;;    (benchmark-union 10000)
;;    (benchmark-union-reduce 10)
;;    (benchmark-union-reduce 10000)))


;; (defn vec-data
;;   [^long n-elems]
;;   (doto (ArrayList.)
;;     (.addAll (api/range n-elems))))


;; (defn vec-perftest
;;   [data]
;;   (let [odata (api/object-array data)
;;         n-elems (count data)]
;;     [{:clj (bench/benchmark-us (vec data))
;;       :hamf (bench/benchmark-us (api/vec data))
;;       :java (bench/benchmark-us (api/array-list data))
;;       :n-elems n-elems
;;       :test :vector-construction}
;;      {:clj (bench/benchmark-us (vec odata))
;;       :hamf (bench/benchmark-us (api/vec odata))
;;       :java (bench/benchmark-us (api/array-list odata))
;;       :n-elems n-elems
;;       :test :vector-cons-obj-array}
;;      (let [method (fn [vdata]
;;                     (dotimes [idx 10000]
;;                       (.get ^List vdata (unchecked-int (rem idx n-elems)))))
;;            clj-d (vec data)
;;            hm-d (api/vec data)
;;            jv-d (api/array-list data)]
;;        {:clj (bench/benchmark-us (method clj-d))
;;         :hamf (bench/benchmark-us (method hm-d))
;;         :java (bench/benchmark-us (method jv-d))
;;         :n-elems n-elems
;;         :test :vector-access})
;;      (let [method (fn [vdata]
;;                     (api/reduce + 0 vdata))
;;            clj-d (vec data)
;;            hm-d (api/vec data)
;;            jv-d (api/array-list data)]
;;        {:clj (bench/benchmark-us (method clj-d))
;;         :hamf (bench/benchmark-us (method hm-d))
;;         :java (bench/benchmark-us (method jv-d))
;;         :n-elems n-elems
;;         :test :vector-reduce})
;;      (let [clj-d (vec data)
;;            hm-d (api/vec data)
;;            jv-d (doto (ArrayList.) (.addAll data))]
;;        {:clj (bench/benchmark-us (.toArray ^List clj-d))
;;         :hamf (bench/benchmark-us (.toArray ^List hm-d))
;;         :java (bench/benchmark-us (.toArray ^List jv-d))
;;         :n-elems n-elems
;;         :test :vector-to-array})]))


;; (defn general-persistent-vector
;;   []
;;   (log/info "persistent vector perftest")
;;   (concat (vec-perftest (vec-data 10))
;;           (vec-perftest (vec-data 10000))))


;; (defn sequence-summation
;;   []
;;   (log/info "sequence perftest")
;;   ;;This is definitely 'cheating' in some dimensions but if we know the end result
;;   ;;is a double summation then if we typehint our entire pathway with doubles that will
;;   ;;allow the entire pathway to avoid boxing.  The predicate's return value must be
;;   ;;an object, however, not a long or a double so there is some minimal boxing.
;;   (let [mfn1 (api/long-unary-operator v (* v 2))
;;         mfn2 (api/long-unary-operator v (+ v 1))
;;         pred (api/long-predicate v (== 0 (rem v 3)))]
;;     (for [n-elems [10 100 1000 100000]]
;;       {:n-elems n-elems
;;        :test :sequence-summation
;;        :clj (bench/benchmark-us (->> (clojure.core/range n-elems)
;;                                      (clojure.core/map mfn1)
;;                                      (clojure.core/map mfn2)
;;                                      (clojure.core/filter pred)
;;                                      (api/sum)))
;;        :hamf (bench/benchmark-us (->> (api/range n-elems)
;;                                       (lznc/map mfn1)
;;                                       (lznc/map mfn2)
;;                                       (lznc/filter pred)
;;                                       (api/sum)))
;;        :eduction (bench/benchmark-us (->> (api/range n-elems)
;;                                           (eduction
;;                                            (comp
;;                                             (clojure.core/map mfn1)
;;                                             (clojure.core/map mfn2)
;;                                             (clojure.core/filter pred)))
;;                                           (api/sum)))})))


;; (defn typed-vs-untyped-serial-reduction
;;   []
;;   (log/info "typed vs untyped reduction")
;;   (let [mfn1 (api/long-unary-operator v (* v 2))
;;         mfn2 (api/long-unary-operator v (+ v 1))
;;         pred (api/long-predicate v (== 0 (rem v 3)))
;;         ut1 (fn [v] (* (long v) 2))
;;         ut2 (fn [v] (+ (long v) 1))
;;         utp (fn [v] (== 0 (rem (long v) 3)))]
;;     (for [n-elems [10000 1000000]]
;;       {:n-elems n-elems
;;        :test :typed-vs-untyped-serial
;;        :untyped (bench/benchmark-us (->> (api/range n-elems)
;;                                          (lznc/map ut1)
;;                                          (lznc/map ut2)
;;                                          (lznc/filter utp)
;;                                          (api/reduce-reducer (Sum$SimpleSum.))))
;;        :typed (bench/benchmark-us (->> (api/range n-elems)
;;                                        (lznc/map mfn1)
;;                                        (lznc/map mfn2)
;;                                        (lznc/filter pred)
;;                                        (api/reduce-reducer (Sum$SimpleSum.))))})))


;; (defn typed-vs-untyped-parallel-reduction
;;   []
;;   (log/info "typed vs untyped reduction")
;;   (let [mfn1 (api/long-unary-operator v (* v 2))
;;         mfn2 (api/long-unary-operator v (+ v 1))
;;         pred (api/long-predicate v (== 0 (rem v 3)))
;;         ut1 (fn [v] (* (long v) 2))
;;         ut2 (fn [v] (+ (long v) 1))
;;         utp (fn [v] (== 0 (rem (long v) 3)))]
;;     (for [n-elems [10000 1000000]]
;;       {:n-elems n-elems
;;        :test :typed-vs-untyped-parallel
;;        :untyped (bench/benchmark-us (->> (api/range n-elems)
;;                                          (lznc/map ut1)
;;                                          (lznc/map ut2)
;;                                          (lznc/filter utp)
;;                                          (api/preduce-reducer (ham_fisted.Sum$SimpleSum.))))
;;        :typed (bench/benchmark-us (->> (api/range n-elems)
;;                                        (lznc/map mfn1)
;;                                        (lznc/map mfn2)
;;                                        (lznc/filter pred)
;;                                        (api/preduce-reducer (ham_fisted.Sum$SimpleSum.))))})))


;; (defn typed-consumer-vs-boxed
;;   []
;;   (log/info "typed vs untyped reduction")
;;   (let [mfn1 (fn ^long [^long v] (* v 2))
;;         mfn2 (fn ^long [^long v] (+ v 1))
;;         pred (fn [^long v] (== 0 (rem v 3)))
;;         ut1 (fn [v] (* (long v) 2))
;;         ut2 (fn [v] (+ (long v) 1))
;;         utp (fn [v] (== 0 (rem (long v) 3)))]
;;     (for [n-elems [10000 1000000]]
;;       {:n-elems n-elems
;;        :test :consumer-vs-boxed
;;        :boxed (bench/benchmark-us (->> (api/range n-elems)
;;                                        (lznc/map mfn1)
;;                                        (lznc/map mfn2)
;;                                        (lznc/filter pred)
;;                                        (api/reduce (api/double-accumulator
;;                                                          acc v (+ (double acc) v))
;;                                                         0.0)))
;;        :typed-consumer (bench/benchmark-us (->> (api/range n-elems)
;;                                                 (lznc/map mfn1)
;;                                                 (lznc/map mfn2)
;;                                                 (lznc/filter pred)
;;                                                 (api/reduce-reducer (ham_fisted.Sum$SimpleSum.))))})))


;; (defn map-indexed-summation
;;   []
;;   (for [n-elems [10 100 1000 100000]]
;;     {:n-elems n-elems
;;      :test :map-indexed-summation
;;      :clj (bench/benchmark-us (api/sum (map-indexed + (api/range n-elems))))
;;      :hamf (bench/benchmark-us (api/sum (lznc/map-indexed + (api/range n-elems))))}))


;; (defn object-array-perftest
;;   []
;;   (log/info "obj array perftest")
;;   (let [init-seq (->> (api/range 20000)
;;                       (lznc/map #(* (long %) 2))
;;                       (lznc/map #(+ 1 (long %)))
;;                       (lznc/filter #(== 0 (rem (long %) 3))))]
;;     [{:n-elems 20000
;;       :test :object-array
;;       :clj (bench/benchmark-us (object-array init-seq))
;;       :hamf (bench/benchmark-us (api/object-array init-seq))}]))


;; (defn shuffle-perftest
;;   []
;;   (log/info "shuffle")
;;   (let [init-data (api/range 10000)]
;;     ;;Test is heavily gated on java.util.Random implementation
;;     [{:n-elems 10000
;;       :test :shuffle
;;       :clj (bench/benchmark-us (shuffle init-data))
;;       :hamf (bench/benchmark-us (api/shuffle init-data))}]))


;; (defn sort-perftest
;;   []
;;   (log/info "sort")
;;   (let [init-data (api/shuffle (api/range 10000))]
;;     [{:test :sort
;;       :n-elems 10000
;;       :clj (bench/benchmark-us (sort init-data))
;;       ;;The default api sort sorts nil first.  This is more correct but an unfair comparison.
;;       :hamf (bench/benchmark-us (api/sort nil init-data))}]))


;; (defn alloc-small-array
;;   []
;;   [{:test :alloc-small-double-array
;;     :n-elems 5
;;     :clj (bench/benchmark-us (double-array [1 2 3 4 5]))
;;     :hamf (bench/benchmark-us (api/double-array [1 2 3 4 5]))}
;;    {:test :alloc-small-double-array
;;     :n-elems 10
;;     :clj (bench/benchmark-us (double-array [1 2 3 4 5 6 7 8 9 10]))
;;     :hamf (bench/benchmark-us (api/double-array [1 2 3 4 5 6 7 8 9 10]))}
;;    {:test :alloc-small-double-array
;;     :n-elems 100
;;     :clj (bench/benchmark-us (double-array (range 100)))
;;     :hamf (bench/benchmark-us (api/double-array (api/range 100)))}
;;    (let [v (vec (api/range 1000))]
;;      {:test :alloc-small-double-array
;;       :n-elems 1000
;;       :clj (bench/benchmark-us (double-array v))
;;       :hamf (bench/benchmark-us (api/double-array v))})])


;; (defn sort-double-array
;;   []
;;   (log/info "sort doubles")
;;   (let [init-data (api/double-array (api/shuffle (api/range 10000)))]
;;     [{:test :sort-doubles
;;       :n-elems 10000
;;       :clj (bench/benchmark-us (sort init-data))
;;       ;;Fair comparison means we avoid nan-first behavior
;;       :hamf (bench/benchmark-us (api/sort nil init-data))}]))


;; (defn sort-int-array
;;   []
;;   (log/info "sort ints")
;;   (let [init-data (api/int-array (api/shuffle (api/range 10000)))]
;;     [{:test :sort-ints
;;       :n-elems 10000
;;       :clj (bench/benchmark-us (sort init-data))
;;       :hamf (bench/benchmark-us (api/sort nil init-data))}]))


;; (defn frequencies-perftest
;;   []
;;   (log/info "frequencies")
;;   (for [n-elems [10 1000 10000 100000]]
;;     (let [tdata (lznc/map #(rem (unchecked-long %) 7) (api/range n-elems))]
;;       {:n-elems n-elems
;;        :test :frequencies
;;        :gbr-inc (bench/benchmark-us (api/frequencies-gbr-inc tdata))
;;        :gbr-consumer (bench/benchmark-us (api/frequencies-gbr-consumer tdata))
;;        :bespoke (bench/benchmark-us (api/frequencies tdata))
;;        :clj (bench/benchmark-us (frequencies tdata))})))


;; (defn serial-verse-parallel-sum
;;   []
;;   (log/info "parallel serial sum")
;;   (for [n-elems [10000 100000 10000000]]
;;     {:n-elems n-elems
;;      :test :serial-parallel-sum
;;      :serial (bench/benchmark-us (api/reduce-reducer (Sum.)
;;                                                      (api/range n-elems)))
;;      :parallel (bench/benchmark-us (api/preduce-reducer (Sum.)
;;                                                         {:min-n 1000} (api/range n-elems)))}))


;; (defn object-list-perftest
;;   []
;;   (log/info "object list")
;;   (let [n-elems 20000
;;         init-data (->> (api/range n-elems)
;;                        (lznc/map #(* (long %) 2))
;;                        (lznc/map #(+ 1 (long %)))
;;                        (lznc/filter #(== 0 (rem (long %) 3))))]
;;     [{:test :object-list-red-1
;;       :n-elems n-elems
;;       :java (bench/benchmark-us (doto (ArrayList.)
;;                                   (.addAll init-data)))
;;       :hamf (bench/benchmark-us (api/object-array-list init-data))}]))




;; (defn int-list-perftest
;;   []
;;   (log/info "int list")
;;   (let [n-elems 20000
;;         init-data (->> (api/range n-elems)
;;                        (lznc/map (fn ^long [^long v] (* v 2)))
;;                        (lznc/map (fn ^long [^long v] (+ v 1)))
;;                        (lznc/filter (fn [^long v] (== 0 (rem v 3)))))]
;;     [{:test :int-list
;;       :n-elems n-elems
;;       :java (bench/benchmark-us (doto (ArrayList.)
;;                                   (.addAll init-data)))
;;       :hamf (bench/benchmark-us (api/int-array-list init-data))
;;       :dtype (bench/benchmark-us (doto (dtype/make-list :int32)
;;                                    (.addAll init-data)))}]))


;; (defn group-by-perftest
;;   []
;;   (log/info "group-by")
;;   (for [n-elems [100 10000]]
;;     {:n-elems n-elems
;;      :test :group-by
;;      :clj (bench/benchmark-us (clojure.core/group-by #(rem (unchecked-long %1) 31)
;;                                                      (api/range n-elems)))
;;      :hamf (bench/benchmark-us (api/group-by #(rem (unchecked-long %1) 31)
;;                                              (api/range n-elems)))}))


;; (defn group-by-reduce-perftest
;;   []
;;   (log/info "group-by-reduce")
;;   ;;Choose a large-ish prime.  This emphasizes not only an efficient map datastructure but
;;   ;;also the much faster finalization step.
;;   (let [n-elems 100000]
;;     [{:n-elems n-elems
;;       :test :group-by-reduce
;;       :clj (bench/benchmark-us (->> (clojure.core/group-by #(rem (unchecked-long %1) 337)
;;                                                            (api/range n-elems))
;;                                     (map (fn [[k v]]
;;                                            [k (reduce + 0 v)]))
;;                                     (into {})))
;;       :hamf (bench/benchmark-us (api/group-by-reduce #(rem (unchecked-long %1) 337)
;;                                                      +
;;                                                      +
;;                                                      +
;;                                                      (api/range n-elems)))}]))


;; (defn update-values-perftest
;;   []
;;   (log/info "update-values")
;;   (let [n-elems 1000
;;         data (lznc/map #(api/vector % %) (range n-elems))
;;         clj-map (into {} data)
;;         hamf-map (api/immut-map data)]
;;     [{:n-elems n-elems
;;       :test :update-values
;;       :clj (bench/benchmark-us (-> (reduce (fn [m e]
;;                                              (assoc! m (key e) (unchecked-inc (val e))))
;;                                            (transient {})
;;                                            clj-map)
;;                                    (persistent!)))
;;       :hamf (bench/benchmark-us (api/update-values
;;                                  hamf-map
;;                                  (api/bi-function k v (unchecked-inc v))))}]))


;; (defn mapmap-perftest
;;   []
;;   (log/info "mapmap")
;;   (let [n-elems 1000
;;         data (lznc/map #(api/vector % %) (range n-elems))
;;         clj-map (into {} data)
;;         hamf-map (api/immut-map data)
;;         ;;Destructuring is by far the most expensive part of this operation
;;         ;;so we avoid it to get a better measure of the time.
;;         map-fn (fn [e] [(key e) (unchecked-inc (val e))])]
;;     [{:n-elems n-elems
;;       :test :mapmap
;;       :clj (bench/benchmark-us (into {} (comp (map map-fn) (remove nil?)) clj-map))
;;       :hamf (bench/benchmark-us (api/mapmap map-fn hamf-map))}]))


;; (defn assoc-in-perftest
;;   []
;;   (log/info "assoc-in")
;;   [{:n-elems 5
;;     :test :assoc-in-nil
;;     :clj (bench/benchmark-us (assoc-in nil [:a :b :c :d :e] 1))
;;     :hamf (bench/benchmark-us (api/assoc-in nil [:a :b :c :d :e] 1))}
;;    (let [data {:a {:b {:c {:d {}}}}}]
;;      {:n-elems 5
;;       :test :assoc-in
;;       :clj (bench/benchmark-us (assoc-in data [:a :b :c :d :e] 1))
;;       :hamf (bench/benchmark-us (api/assoc-in data [:a :b :c :d :e] 1))})])


;; (defn update-in-perftest
;;   []
;;   (log/info "update-in")
;;   (let [updater (fn [v] (unchecked-inc (or v 1)))]
;;     [{:n-elems 5
;;       :test :update-in-nil
;;       :clj (bench/benchmark-us (update-in nil [:a :b :c :d :e] updater))
;;       :hamf (bench/benchmark-us (api/update-in nil [:a :b :c :d :e] updater))}
;;      (let [data (api/assoc-in nil [:a :b :c :d :e] 2)]
;;        {:n-elems 5
;;         :test :update-in
;;         :clj (bench/benchmark-us (update-in data [:a :b :c :d :e] updater))
;;         :hamf (bench/benchmark-us (api/update-in data [:a :b :c :d :e] updater))})]))


;; (defn get-in-perftest
;;   []
;;   (log/info "get-in")
;;   (let [data {:a 1 :b 2 :c 3 :d 4 :e 5}]
;;     [{:n-elems 5
;;       :test :get-in
;;       :clj (bench/benchmark-us (get-in data [:a :b :c :d :e]))
;;       :hamf (bench/benchmark-us (api/get-in data [:a :b :c :d :e]))}]))


;; (defn concatv-perftest
;;   []
;;   (log/info "concatv")
;;   (let [data [[] (list 1 2 3) nil nil
;;               (clojure.core/vector 1 2 3 4 5) (api/array-list [1 2 3 4])
;;               (api/vec (api/range 50))]]
;;     [{:n-elems 100
;;       :test :concatv
;;       :clj (bench/benchmark-us (clojure.core/vec (apply concat data)))
;;       :hamf (bench/benchmark-us (apply api/concatv data))}]))


;; (defn stable-sum-perftest
;;   []
;;   (log/info "stable summation - dtype(clj) vs. hamf")
;;   (for [n-elems [10 100 1000 1000000]]
;;     (let [n-elems (long n-elems)
;;           data (api/double-array (range n-elems))]
;;       {:n-elems n-elems
;;        :test :stable-summation
;;        :hamf (bench/benchmark-us (api/sum data))
;;        :clj (bench/benchmark-us (dfn/sum data))})))


;; (defn unstable-sum-perftest
;;   []
;;   (log/info "unstable summation - dtype(clj) vs. hamf")
;;   (for [n-elems [100 1000 1000000]]
;;     (let [n-elems (long n-elems)
;;           data (api/double-array (range n-elems))]
;;       {:n-elems n-elems
;;        :test :unstable-summation
;;        :hamf (bench/benchmark-us (api/sum-fast data))
;;        :clj (bench/benchmark-us (dfn/sum-fast data))})))


;; (defn pmap-perftest
;;   []
;;   (log/info "pmap perftest")
;;   (for [n-elems [100 1000]]
;;     (let [n-elems (long n-elems)]
;;       {:n-elems n-elems
;;        :test :pmap
;;        :clj (bench/benchmark-us (count (clojure.core/pmap inc (api/range n-elems))))
;;        :hamf (bench/benchmark-us (count (api/pmap inc (api/range n-elems))))})))


;; (defn machine-name
;;   []
;;   (.getHostName (java.net.InetAddress/getLocalHost)))


;; (defn git-sha
;;   []
;;   (-> (sh/sh "git" "rev-parse" "--short" "HEAD")
;;       :out
;;       (str/trim)))


;; (defn data->dataset
;;   [profile-data]
;;   (->> profile-data
;;        (mapv (fn [data-map]
;;                (api/mapmap (fn [[k v]]
;;                              [k (if (map? v)
;;                                   (v :mean-μs)
;;                                   v)])
;;                            data-map)))))


;; (defn normalize-rows
;;   [dataset]
;;   (let [system-keys #{:clj :hamf :java :eduction}]
;;     (->> dataset
;;          (mapv (fn [row]
;;                  (let [norm-factor (double (get row :clj (get row :java)))]
;;                    (api/assoc
;;                     (api/mapmap (fn [[k v]]
;;                                   [k (if (system-keys k)
;;                                        (/ v norm-factor)
;;                                        v)]) row)
;;                     :norm-factor-μs norm-factor)))))))


;; (defn profile
;;   []
;;   (api/concatv
;;    ;; (general-hashmap)
;;    ;; (hashmap-construction-obj-ary 4)
;;    ;; (hashmap-construction-obj-ary 10)
;;    ;; (hashmap-construction-obj-ary 1000)
;;    ;; (general-persistent-vector)
;;    ;; (union-perftest)
;;    ;; (sequence-summation)
;;    ;; (object-array-perftest)
;;    ;; (shuffle-perftest)
;;    ;; (object-list-perftest)
;;    ;; (int-list-perftest)
;;    ;; (sort-perftest)
;;    ;; (sort-double-array)
;;    ;; (sort-int-array)
;;    ;; (frequencies-perftest)
;;    ;; (group-by-perftest)
;;    ;; (group-by-reduce-perftest)
;;    ;; (update-values-perftest)
;;    ;; (mapmap-perftest)
;;    ;; (assoc-in-perftest)
;;    ;; (update-in-perftest)
;;    ;; (get-in-perftest)
;;    ;; (concatv-perftest)
;;    ;;technically a dtype-next vs. hamf test
;;    (stable-sum-perftest)
;;    (unstable-sum-perftest)
;;    ))



;; (defn process-dataset
;;   [dataset]
;;   (->> dataset
;;        (data->dataset)
;;        (normalize-rows)
;;        (sort-by :test)
;;        (vec)))


;; (def column-order [:test :n-elems :java :clj :eduction :hamf :norm-factor-μs])


;; (defn print-dataset
;;   [dataset]
;;   (pp/print-table column-order dataset))


(defn -main
  [& args]
  ;;shutdown test
  (union-overlapping)
  (union-disj)
  (union-reduce)
  (update-values)
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

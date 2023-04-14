(ns perftest
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.function :as hamf-fn]
            [ham-fisted.reduce :as hamf-rf]
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
  (:import [java.util HashMap ArrayList Map List Map$Entry ArrayList]
           [java.util.function BiFunction]
           [ham_fisted IMutList Sum$SimpleSum Sum BitmapTrieCommon Consumers$IncConsumer
            BitmapTrieCommon$MapSet ImmutValues]
           [clojure.lang PersistentHashMap])
  (:gen-class))


(set! *unchecked-math* :warn-on-boxed)



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
  (let [data (vec data)
        fname (str "results/" testname ".edn")]
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
                 n-elems (long n-elems)
                 data (vec (take (max cycle-size n-elems) (cycle (repeatedly (min n-elems cycle-size) #(long (rand-int 1000))))))
                 data (if numeric? data (map (comp keyword str) data))
                 init-maps (initial-maps (map-constructors numeric?) nil)]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (val kv)
                                         immut-path? (instance? clojure.lang.IPersistentMap m)]
                                     [(key kv)
                                      (if immut-path?
                                        (benchmark-us (reduce (fn [acc v]
                                                                (assoc! acc v (unchecked-inc (long (get acc v 0)))))
                                                              (transient m)
                                                              data))
                                        (let [cfn (hamf-fn/function _v (Consumers$IncConsumer.))]
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
                 merge-bfn (hamf-fn/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (hamf/persistent! (val kv))]
                                     [(key kv)
                                      (cond
                                        (instance? clojure.lang.IPersistentMap m)
                                        (benchmark-us (merge-with merge-bfn m m))
                                        (instance? BitmapTrieCommon$MapSet m)
                                        (benchmark-us (.union ^BitmapTrieCommon$MapSet m m merge-bfn))
                                        :else
                                        (let [map-c (get constructors (key kv))]
                                          (benchmark-us (hamf/map-union merge-bfn (map-c m) m))))]))
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
           (let [n-elems (long n-elems)
                 data (if numeric? (long-map-data n-elems) (kwd-map-data n-elems))
                 lhs-data (vec (take (quot n-elems 2) data))
                 rhs-data (vec (drop (- n-elems (count lhs-data)) data))
                 constructors (map-constructors numeric?)
                 lhs-maps (initial-maps constructors lhs-data)
                 rhs-maps (initial-maps constructors rhs-data)
                 merge-bfn (hamf-fn/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   ;;Make sure we can't edit lhs
                                   (let [lhs (hamf/persistent! (val kv))
                                         rhs (rhs-maps (key kv))]
                                     [(key kv)
                                      (cond
                                        (instance? clojure.lang.IPersistentMap lhs)
                                        (benchmark-us (merge-with merge-bfn lhs rhs))
                                        (instance? BitmapTrieCommon$MapSet lhs)
                                        (benchmark-us (.union ^BitmapTrieCommon$MapSet (hamf/transient lhs) rhs merge-bfn))
                                        :else
                                        (let [map-c (get constructors (key kv))]
                                          (benchmark-us (hamf/map-union merge-bfn (map-c lhs ) rhs))))]))
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
                 merge-bfn (hamf-fn/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (hamf/persistent! (val kv))
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
                                          (benchmark-us (reduce #(hamf/map-union merge-bfn %1 %2)
                                                                (map-c m)
                                                                mseq))))]))
                                 init-maps)
                    {:n-elems n-elems :numeric? numeric? :test :union-reduce}))))
       (vec)
       (spit-data "union-reduce")))


(defn union-reduce-transient
  "Comparing reducing into a single transient container vs. doing a shallow clone every union call."
  []
  (->> (for [n-elems [ 4 10
                      100
                       1000 10000 1000000
                      ]
             numeric? [true
                       ]
             ]
         (do
           (log/info (str "union-reduce-transient benchmark on " (if numeric?
                                                                   "numeric "
                                                                   "non-numeric ")
                          "data with n=" n-elems))
           (let [data (if numeric? (long-map-data n-elems) (kwd-map-data n-elems))
                 constructors (map-constructors numeric?)
                 hashmap (hamf/mut-map data)
                 long-hashmap (hamf/mut-map data)
                 init-maps {:hamf-hashmap (persistent! hashmap)
                            :hamf-long-map (persistent! long-hashmap)
                            :hamf-trans-hashmap (with-meta (persistent! hashmap) {:transient? true})
                            :hamf-trans-long-map (with-meta (persistent! long-hashmap) {:transient? true})}
                 merge-bfn (hamf-fn/bi-function a b (+ (long a) (long b)))]
             (merge (hamf/mapmap (fn [kv]
                                   (let [m (val kv)
                                         mseq (vec (repeat 16 m))]
                                     [(key kv)
                                      (benchmark-us (reduce #(.union (hamf/as-map-set %1) %2 merge-bfn)
                                                            (if (:transient? (meta m))
                                                              (transient m)
                                                              m)
                                                            mseq))]))
                                 init-maps)
                    {:n-elems n-elems :numeric? numeric? :test :union-reduce-transient}))))
       (vec)
       (spit-data "union-reduce-transient")))


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
                 update-bfn (hamf-fn/bi-function k v (unchecked-inc (long v)))]
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


(deftype LongAccum [^{:unsynchronized-mutable true
                      :tag long} val]
  clojure.lang.IDeref
  (deref [this] val)
  java.util.function.LongConsumer
  (accept [this v] (set! val (+ v val))))


(defn typed-reductions
  []
  (->>
   (for [n-elems [ 4 10
                  100
                  1000 10000 1000000                  ]]
     (do
       (log/info (str "typed reduction benchmark on " (if true
                                                        "numeric "
                                                        "non-numeric ")
                      "data with n=" n-elems))
       (merge
        {:clj (benchmark-us (transduce (comp (map #(+ (long %) 10)) (filter #(== 0 (rem (long %) 2))))
                                       + 0 (hamf/range n-elems)))
         :clj-typed (benchmark-us (transduce (comp (map (hamf-fn/long-unary-operator a (+ a 10)))
                                                   (filter (hamf-fn/long-predicate a (== 0 (rem a 2)))))
                                             (reify ham_fisted.IFnDef$LLL
                                               (invokePrim [this a b] (+ a b))
                                               (invoke [this a] a)) 0
                                             (hamf/range n-elems)))
         :hamf-partial (benchmark-us (->> (hamf/range n-elems)
                                          (lznc/map (hamf-fn/long-unary-operator a (+ a 10)))
                                          (lznc/filter (hamf-fn/long-predicate a(== 0 (rem a 2))))
                                          (reduce (hamf-fn/long-binary-operator a b (+ a b)) 0)))
         :hamf-deftype-consumer (benchmark-us (->> (hamf/range n-elems)
                                                   (lznc/map (hamf-fn/long-unary-operator a (+ a 10)))
                                                   (lznc/filter (hamf-fn/long-predicate a(== 0 (rem a 2))))
                                                   (reduce hamf-rf/long-consumer-accumulator (LongAccum. 0))))
         :hamf-java-consumer (benchmark-us (->> (hamf/range n-elems)
                                                (lznc/map (hamf-fn/long-unary-operator a (+ a 10)))
                                                (lznc/filter (hamf-fn/long-predicate a(== 0 (rem a 2))))
                                                (reduce hamf-rf/long-consumer-accumulator (ham_fisted.LongAccum. 0))))}
        {:n-elems n-elems :numeric? true :test :typed-reductions})))
   (vec)
   (spit-data "typed-reductions-intel")))


(defn typed-parallel-reductions
  []
  (->>
   (for [n-elems [1000 1000000
                  100000000
                  ]]
     (do
       (log/info (str "parallel reduction benchmark on " (if true
                                                        "numeric "
                                                        "non-numeric ")
                      "data with n=" n-elems))
       (let [ops (hamf-rf/options->parallel-options {:min-n 10})]
         (merge
          {:clj (benchmark-us (transduce (comp (map #(+ (long %) 10)) (filter #(== 0 (rem (long %) 2))))
                                         + 0 (hamf/range n-elems)))
           :hamf-untyped (benchmark-us (->> (hamf/range n-elems)
                                            (lznc/map #(+ (long %) 10))
                                            (lznc/filter #(== 0 (rem (long %) 2)))
                                            (hamf-rf/preduce (constantly 0)
                                                          #(+ (long %1) (long %2))
                                                          #(+ (long %1) (long %2))
                                                          ops)))
           :hamf-typed (benchmark-us (->> (hamf/range n-elems)
                                          (lznc/map (hamf-fn/long-unary-operator a (+ a 10)))
                                          (lznc/filter (hamf-fn/long-predicate a(== 0 (rem a 2))))
                                          (hamf-rf/preduce (constantly 0)
                                                        (hamf-fn/long-binary-operator a b (+ a b))
                                                        (hamf-fn/long-binary-operator a b (+ a b))
                                                        ops)))
           :hamf-consumer (benchmark-us (->> (hamf/range n-elems)
                                             (lznc/map (hamf-fn/long-unary-operator a (+ a 10)))
                                             (lznc/filter (hamf-fn/long-predicate a(== 0 (rem a 2))))
                                             (hamf-rf/preduce #(ham_fisted.LongAccum. 0)
                                                           hamf-rf/long-consumer-accumulator
                                                           hamf-rf/reducible-merge
                                                           ops)))}
          {:n-elems n-elems :numeric? true :test :typed-parallel-reductions}))))
   (vec)
   (spit-data "typed-parallel-reductions")
   ))


(def persistent-vector-constructors
  {:clj vec
   :hamf hamf/immut-list
   :hamf-objary #(hamf/immut-list (hamf/object-array %))
   :java #(doto (ArrayList.)
            (.addAll (hamf/->random-access %)))})


(defn persistent-vector-perftest
  []
  (->> (for [n-elems [4 10
                      100
                      1000 10000 100000]]
         (do
           (log/info (str "persistent vector perftest with n= " n-elems))
           [(merge (hamf/mapmap (fn [kv]
                                  [(key kv)
                                   (benchmark-us ((val kv) (range n-elems)))])
                                persistent-vector-constructors)
                   {:n-elems n-elems :numeric? true :test :vector-construction})
            (merge (hamf/mapmap (fn [kv]
                                  (let [data ((val kv) (range n-elems))]
                                    [(key kv)
                                     (benchmark-us (dotimes [idx n-elems]
                                                     (.get ^List data idx)))]))
                                persistent-vector-constructors)
                   {:n-elems n-elems :numeric? true :test :vector-access})
            (merge (hamf/mapmap (fn [kv]
                                  (let [data ((val kv) (range n-elems))]
                                    [(key kv)
                                     (benchmark-us (.toArray ^List data))]))
                                persistent-vector-constructors)
                   {:n-elems n-elems :numeric? true :test :to-object-array})
            (merge (hamf/mapmap (fn [kv]
                                  (let [data (double-array (range n-elems))]
                                    [(key kv)
                                     (benchmark-us ((val kv) data))]))
                                persistent-vector-constructors)
                   {:n-elems n-elems :numeric? true :test :from-double-array})
            ]))
       (lznc/apply-concat)
       (vec)
       (spit-data "persistent-vector")))


(defn sort-by-perftest
  []
  (->>
   (for [n-elems [4 10
                  100
                  1000 10000 100000
                  ]]
     (do
       (log/info (str "sort-by perftest with n= " n-elems))
       (let [data (mapv (fn [idx]
                          {:a 1
                           :b idx})
                        (shuffle (range n-elems)))]
         {:clj (benchmark-us (sort-by :b data))
          :hamf (benchmark-us (hamf/sort-by :b data))
          :hamf-typed (benchmark-us (hamf/sort-by (hamf-fn/obj->long d (long (d :b))) data))
          :n-elems n-elems
          :test :sort-by
          :numeric? true})))
   (vec)
   (spit-data "sort-by")))




(defn -main
  [& args]
  ;;shutdown test
  (sort-by-perftest)
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

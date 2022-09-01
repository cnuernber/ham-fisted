(ns ham-fisted.hash-map-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [ham-fisted.api :as api]
            [criterium.core :as crit])
  (:import [ham_fisted HashMap PersistentHashMap BitmapTrie TransientHashMap
            BitmapTrieCommon]
           [java.util ArrayList Collections Map Collection]
           [java.util.function BiFunction BiConsumer]
           [java.util.concurrent ForkJoinPool Future Callable]))

(defonce orig api/empty-map)


(deftest simple-assoc
  (let [orig PersistentHashMap/EMPTY]
    (is (= 0 (count orig)))
    (is (= {:a :b} (assoc orig :a :b)))
    (is (= 1 (count (assoc orig :a :b))))
    (is (= {} (-> (assoc orig :a :b)
                  (dissoc :a))))
    (is (= 0 (-> (assoc orig :a :b)
                 (dissoc :a)
                 (count)))))

  (let [nilmap (assoc orig nil :b)]
    (is (= {nil :b} nilmap))
    (is (= nilmap {nil :b}))
    (is (= {nil :b :a :b} (assoc nilmap :a :b)))
    (is (= (assoc nilmap :a :b) {nil :b :a :b}))
    (is (= 1 (count nilmap)))
    (is (= 1 (count (dissoc nilmap :a))))
    (is (= 2 (count (assoc nilmap :a :b))))
    (is (= 0 (count (dissoc nilmap nil))))
    (is (= #{nil :a} (set (keys (assoc nilmap :a :b)))))))


(defonce test-data* (atom {}))


(deftest random-assoc-dissoc
  (let [n-elems 100
        n-take (quot n-elems 10)
        n-left (- n-elems n-take)
        data (shuffle (range n-elems))
        dissoc-vals (take n-take data)
        data (set data)
        dissoc-data (set/difference data (set dissoc-vals))]
    (reset! test-data* {:data data
                        :dissoc-vals dissoc-vals
                        :dissoc-data dissoc-data})
    (testing "immutable"
      (let [alldata (reduce #(assoc %1 %2 %2)
                            orig
                            data)
            disdata (reduce #(dissoc %1 %2) alldata dissoc-vals)]
        (is (= n-left (count disdata)))
        (is (= n-elems (count alldata)))
        (is (= dissoc-data (set (keys disdata))))
        (is (= data (set (keys alldata))))))
    (testing "transient1"
      (let [alldata (-> (reduce #(assoc! %1 %2 %2)
                                (TransientHashMap.)
                                data)
                        (persistent!))
            disdata (-> (reduce #(dissoc! %1 %2)
                                (transient alldata)
                                dissoc-vals)
                        (persistent!))]
        (is (= n-left (count disdata)))
        (is (= n-elems (count alldata)))
        (is (= dissoc-data (set (keys disdata))))
        (is (= data (set (keys alldata))))))
    (testing "transient"
      (let [alldata (-> (reduce #(assoc! %1 %2 %2)
                                (transient orig)
                                data)
                        (persistent!))
            disdata (-> (reduce #(dissoc! %1 %2)
                                (transient alldata)
                                dissoc-vals)
                        (persistent!))]
        (is (= n-left (count disdata)))
        (is (= n-elems (count alldata)))
        (is (= dissoc-data (set (keys disdata))))
        (is (= data (set (keys alldata))))))
    (testing "mutable"
      (let [alldata (HashMap.)
            _ (doseq [item data]
                (.put alldata item item))
            disdata (.clone alldata)
            _ (is (= n-elems (count disdata)))
            _ (doseq [item dissoc-vals]
                (.remove disdata item))]
        (is (= n-left (count disdata)))
        (is (= n-elems (count alldata)))
        (is (= dissoc-data (set (keys disdata))))
        (is (= data (set (keys alldata))))
        ))))


(deftest split-iterator-test
  (let [hm (HashMap.)
        _ (dotimes [idx 2]
            (.put hm idx idx))]
    (is (= 2 (->> (map iterator-seq (.splitKeys hm 8))
                  (map count)
                  (reduce +)))))
  (let [hm (HashMap.)
        _ (dotimes [idx 10]
            (.put hm idx idx))]
    (is (= 10 (->> (map iterator-seq (.splitKeys hm 8))
                  (map count)
                  (reduce +)))))
  ;; Force splitting in between nodes.
  (let [hm (HashMap.)
        _ (dotimes [idx 60]
            (.put hm idx idx))]
    (is (= 60 (->> (map iterator-seq (.splitKeys hm 7))
                   (map count)
                   (reduce +)))))
  (let [hm (HashMap.)
        _ (dotimes [idx 100]
            (.put hm idx idx))]
    (is (= 100 (->> (map iterator-seq (.splitKeys hm 8))
                    (map count)
                    (reduce +)))))
  (let [hm (HashMap.)
        _ (dotimes [idx 1000]
            (.put hm idx idx))]
    (is (= 1000 (->> (map iterator-seq (.splitKeys hm 8))
                     (map count)
                     (reduce +)))))
  (let [hm (HashMap.)
        _ (dotimes [idx 1000]
            (.put hm idx idx))
        _ (.put hm nil :a)]
    (is (= 1001 (->> (map iterator-seq (.splitKeys hm 8))
                     (map count)
                     (reduce +)))))
  (let [hm (HashMap.)
        _ (dotimes [idx 1000]
            (.put hm idx idx))
        _ (.put hm nil :a)]
    ;;Ensure we get split buckets at the root level and ensure we only
    (is (= 1001 (->> (map iterator-seq (.splitKeys hm 13))
                     (map count)
                     (reduce +))))))



(defmacro benchmark-us
  [op]
  `(let [bdata# (crit/quick-benchmark ~op nil)]
     {:mean-μs (* (double (first (:mean bdata#))) 1e6)
      :variance-μs (* (double (first (:variance bdata#))) 1e6)}))


(defn ->collection
  ^Collection [data]
  (if (instance? Collection data)
    data
    (seq data)))


(defn ->array-list
  ^ArrayList [data]
  (if (instance? ArrayList data)
    data
    (let [retval (ArrayList.)]
      (.addAll retval (->collection data))
      retval)))


(defn time-dataset
  [data constructor]
  (let [dlist (doto (ArrayList.)
                (.addAll ^Collection (seq data)))
        _ (Collections/shuffle dlist)
        ctime (benchmark-us (constructor dlist))
        ^Map ds (constructor dlist)
        nelems (.size dlist)
        atime (benchmark-us
               (dotimes [idx nelems]
                 (.get ds (.get dlist idx))))
        itime (benchmark-us
               (let [iter (.iterator (.entrySet ds))]
                 (loop [c? (.hasNext iter)]
                   (when c?
                     (.next iter)
                     (recur (.hasNext iter))))))]
    {:construct-μs (:mean-μs ctime)
     :access-μs (:mean-μs atime)
     :iterate-μs (:mean-μs itime)}))


(defn java-hashmap
  [^ArrayList data]
  (let [hm (java.util.HashMap.)
        nelems (.size data)]
    (dotimes [idx nelems]
      (.put hm (.get data idx) idx))
    hm))


(defn hamf-hashmap
  [^ArrayList data]
  (let [hm (HashMap.)
        nelems (.size data)]
    (dotimes [idx nelems]
      (.put hm (.get data idx) idx))
    hm))


(defn hamf-equiv-hashmap
  [^ArrayList data]
  (let [hm (HashMap. api/equiv-hash-provider)
        nelems (.size data)]
    (dotimes [idx nelems]
      (.put hm (.get data idx) idx))
    hm))


(defn clj-transient
  [^ArrayList data]
  (let [nelems (.size data)]
    (loop [hm (transient {})
           idx 0]
      (if (< idx nelems)
        (recur (assoc! hm (.get data idx) idx) (unchecked-inc idx))
        (persistent! hm)))))


(defn hamf-transient
  [^ArrayList data]
  (let [nelems (.size data)]
    (loop [hm (transient PersistentHashMap/EMPTY)
           idx 0]
      (if (< idx nelems)
        (recur (assoc! hm (.get data idx) idx) (unchecked-inc idx))
        (persistent! hm)))))


(def datastructures
  [[:java-hashmap java-hashmap]
   [:hamf-hashmap hamf-hashmap]
   [:hamf-equiv-hashmap hamf-equiv-hashmap]
   [:clj-transient clj-transient]
   [:hamf-transient hamf-transient]
   ])


(defn compare-datastructures
  [data]
  (let [data (->array-list data)
        ^Map hm (hamf-equiv-hashmap data)
        ^Map thm (hamf-transient data)
        nelems (.size data)]
    (println "~~~HASHMAP~~~")
    (.printNodes hm)
    (crit/quick-bench (dotimes [idx nelems]
                        (.get hm (.get data idx))))
    (println "~~~Transient~~~~")
    (.printNodes thm)
    (crit/quick-bench (dotimes [idx nelems]
                        (.get thm (.get data idx))))
    ))


(defn profile-datastructures
  [data]
  (let [data (->array-list data)]
    (->> (shuffle datastructures)
         (map (fn [[ds-name ctor]]
                (-> (time-dataset data ctor)
                    (assoc :ds-name ds-name))))
         (sort-by :construct-μs))))


(deftest union-test
  (let [n-elems 100
        hn-elems (quot n-elems 2)
        src-data (repeatedly n-elems #(rand-int 100000000))
        lhs (->array-list (take hn-elems src-data))
        rhs (->array-list (drop hn-elems src-data))
        ^PersistentHashMap hm1 (hamf-transient lhs)
        bfn (reify BiFunction (apply [this a b] (+ a b)))
        hm2 (hamf-transient rhs)
        un1 (.union hm1 hm2 bfn)
        un2 (.union un1 hm2 bfn)
        single-sum (reduce + (range hn-elems))]
    (is (= (count un1) n-elems))
    (is (= (set src-data) (set (keys un1))))
    (is (= (count un2) n-elems))
    (is (= (* 2 single-sum) (reduce + (vals un1))))
    (is (= (* 3 single-sum) (reduce + (vals un2))))))


(deftest difference-test
  (let [n-elems 100
        hn-elems (quot n-elems 2)
        src-data (repeatedly n-elems #(rand-int 100000000))
        lhs (->array-list (take hn-elems src-data))
        rhs (->array-list (drop hn-elems src-data))
        llhs (->array-list (take (quot hn-elems 2) src-data))
        ^PersistentHashMap hm1 (hamf-transient lhs)
        hm2 (hamf-transient rhs)
        df1 (.difference hm1 hm2)
        df2 (.difference hm1 (hamf-transient llhs))]
    (is (= hn-elems (count df1)))
    (is (= (quot hn-elems 2) (count df2)))
    (is (= (set/difference (set lhs) (set llhs)) (set (keys df2))))))


(deftest intersection-test
  (let [n-elems 100
        hn-elems (quot n-elems 2)
        hhn-elems (quot hn-elems 2)
        src-data (repeatedly n-elems #(rand-int 100000000))
        lhs (->array-list (take hn-elems src-data))
        rhs (->array-list (drop hn-elems src-data))
        llhs (->array-list (take hhn-elems src-data))
        bfn (reify BiFunction (apply [this a b] (+ a b)))
        ^PersistentHashMap hm1 (hamf-transient lhs)
        hhm1 (hamf-transient llhs)
        hm2 (hamf-transient rhs)
        df1 (.intersection hm1 hm2 bfn)
        df2 (.intersection hm1 hhm1 bfn)
        hhn-sum (reduce + (range hhn-elems))]
    (is (= 0 (count df1)))
    (is (= (quot hn-elems 2) (count df2)))
    (is (= (set/intersection (set lhs) (set llhs)) (set (keys df2))))
    (is (= (* 2 hhn-sum) (reduce + (vals df2))))))


(def union-data
  {:java-hashmap {:construct-fn java-hashmap
                  :merge-fn api/map-union-java-hashmap
                  :reduce-fn api/union-reduce-java-hashmap}
   ;;jdk-8 - 1000
   ;; {:union-disj-μs 20.413211065573773,
   ;;  :union-μs 41.34100116886689,
   ;;  :name :java-hashmap}
   ;; 100000
   ;; {:union-disj-μs 5050.580285714286,
   ;;  :union-μs 8292.417935897436,
   ;;  :name :java-hashmap}
   :hamf-hashmap {:construct-fn hamf-hashmap
                  :merge-fn api/map-union
                  :reduce-fn #(api/union-reduce-maps %1 %2)}

   :clj-transient (let [make-merge-fn (fn [bifn]
                                        (fn [lhs rhs]
                                          (.apply ^BiFunction bifn lhs rhs)))]
                    {:construct-fn clj-transient
                     :merge-fn #(merge-with (make-merge-fn %1) %2 %3)
                     :reduce-fn #(apply merge-with (make-merge-fn %1) %2)})
   ;;jdk-8 - 1000
   ;; {:union-disj-μs 11.491328581829329,
   ;;  :union-μs 30.876507346896837,
   ;;  :name :hamf-hashmap}
   ;; 100000
   ;; {:union-disj-μs 2461.248479674797,
   ;;  :union-μs 5106.405916666667,
   ;;  :name :hamf-hashmap}
   })


(defn benchmark-union
  [^long n-elems mapname]
  (let [{:keys [construct-fn merge-fn]} (union-data mapname)
        hn-elems (quot n-elems 2)
        src-data (repeatedly n-elems #(rand-int 100000000))
        lhs (->array-list (take hn-elems src-data))
        rhs (->array-list (drop hn-elems src-data))
        bfn (api/->bi-function +)
        lhs-m (construct-fn lhs)
        rhs-m (construct-fn rhs)
        merged-m (merge-fn bfn lhs-m rhs-m)]
    {:union-disj-μs (:mean-μs (benchmark-us (merge-fn bfn lhs-m rhs-m)))
     :union-μs (:mean-μs (benchmark-us (merge-fn bfn lhs-m merged-m)))
     :name mapname}))


(defn benchmark-reduce-union
  [^long n-elems mapname]
  (let [{:keys [construct-fn reduce-fn]} (union-data mapname)
        hn-elems (quot n-elems 2)
        src-data (repeatedly n-elems #(rand-int 100000000))
        lhs (->array-list (take hn-elems src-data))
        rhs (->array-list (drop hn-elems src-data))
        bfn (reify BiFunction
              (apply [this a b]
                (unchecked-add (unchecked-long a)
                               (unchecked-long b))))
        lhs-m (construct-fn lhs)
        rhs-m (construct-fn rhs)
        map-seq (vec (interleave (repeat 10 lhs-m) (repeat 10 rhs-m)))]
    {:union-μs (:mean-μs (benchmark-us (reduce-fn bfn map-seq)))
     :name mapname}))


(defn benchmark-split-map-reduction
  [^long n-elems]
  (let [^PersistentHashMap src-map (hamf-transient (->array-list (repeatedly n-elems #(rand-int 10000000))))]
    (benchmark-us
     #(let [map-ary (.splitMaps src-map 10)
            incrementor (reify BiFunction (apply [this lhs rhs]
                                            (throw (Exception. "Never get here"))
                                            (unchecked-add (unchecked-long lhs)
                                                           (unchecked-long rhs))))]
        (reduce (api/map-union incrementor %1 %2) map-ary)))))


(deftest hashcode-equal-hashmap
  (is (= (.hasheq (api/mut-map [[:a 1] [:b 2]])) (.hasheq {:a 1 :b 2})))
  (is (= (.hasheq (api/mut-map [[:a 1] [:b 2] [nil 3]])) (.hasheq {:a 1 :b 2 nil 3})))
  (is (= (.hasheq (api/mut-map [[:a 1] [:b 2]])) (.hasheq (api/immut-map {:a 1 :b 2}))))
  (is (not= (.hasheq (api/mut-map [[:a 1] [:b 3]])) (.hasheq {:a 1 :b 2})))
  (is (.equals (api/immut-map [[:a 1] [:b 2]]) {:a 1 :b 2}))
  (is (.equals (api/immut-map [[:a 1] [:b 2]]) (api/mut-map [[:a 1] [:b 2]])))
  (is (not (.equals (api/immut-map [[:a 1] [:b 2]]) nil)))
  (is (= (.hasheq (api/mut-set [:a :b])) (.hasheq #{:a :b})))
  (is (not= (.hasheq (api/mut-set [:a :b :c])) (.hasheq #{:a :b})))
  (is (.equals (api/immut-set [:a :b :c]) #{:a :b :c}))
  (is (.equals (api/immut-set [:a :b :c]) (api/mut-set #{:a :b :c})))
  (is (not (.equals (api/immut-set [:a :b :c]) nil))))


(deftest concurrentCompute
  ;;Testing high contention first
  (let [n-elems 10000
        tdata (mapv #(rem (unchecked-long %) 7) (range n-elems))
        answer (api/frequencies tdata)
        panswer (api/pfrequencies tdata)]
    (is (.equals answer panswer))))




(comment

  (do
    (def hm (HashMap.))
    (def orig PersistentHashMap/EMPTY)
    )

  (dotimes [idx 100]
    (.put hm idx idx))

  (map iterator-seq (.splitKeys hm 8))

  (dotimes [idx 10000]
    (.put hm idx idx))

  (crit/quick-bench (let [hm (HashMap.)]
                      (dotimes [idx 2]
                        (.put hm idx idx))))
  ;;34ns
  ;;jdk-17 - 24ns
  (crit/quick-bench (let [hm (HashMap.)]
                      (dotimes [idx 2]
                        (.put hm idx idx))
                      (dotimes [idx 2]
                        (.get hm idx))))

  (crit/quick-bench (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
                      (dotimes [idx 2]
                        (.put hm idx idx))))
  ;;70ns - twice as fast as clojure's transient for small case.  hasheq is the long
  ;;poll in the tent.
  ;;jdk-17 - 61ns

  (crit/quick-bench (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
                      (dotimes [idx 2]
                        (.put hm idx idx))
                      (dotimes [idx 2]
                        (.get hm idx))))

  (crit/quick-bench (let [hm (java.util.HashMap.)]
                      (dotimes [idx 2]
                        (.put hm idx idx))))
  ;;36ns
  ;;jdk-17 31ns

  (crit/quick-bench (loop [idx 0
                           hm (transient {})]
                      (if (< idx 2)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;113ns
  ;;jdk-17 - 136ns

  (crit/quick-bench
   (let [phm (loop [idx 0
                    hm (transient {})]
               (if (< idx 2)
                 (recur (unchecked-inc idx)
                        (assoc! hm idx idx))
                 (persistent! hm)))]
     (loop [idx 0]
       (when (< idx 2)
         (get phm idx)
         (recur (unchecked-inc idx))))))

  (crit/quick-bench (loop [idx 0
                           hm (transient PersistentHashMap/EMPTY)]
                      (if (< idx 2)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;147ns - arrayhashmap is faster for this case but still a lot slower than
  ;;using hashmap representation.
  ;;jdk-17 - 137ns

  (crit/quick-bench (persistent! (transient PersistentHashMap/EMPTY)))



  (crit/quick-bench (let [hm (HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;21us
  ;;jdk-17 - 21us
  (crit/quick-bench (let [hm (HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))
                      (dotimes [idx 1000]
                        (.get hm idx))))
  ;;35us

  (crit/quick-bench (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;41us
  ;;jdk-17 - 39us

  (crit/quick-bench (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))
                      (dotimes [idx 1000]
                        (.get hm idx))))
  ;; 75us

  (crit/quick-bench (let [hm (java.util.HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;17us
  ;;jdk-17 - 21us
  (crit/quick-bench (let [hm (java.util.HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))
                      (dotimes [idx 1000]
                        (.get hm idx))))
  ;;20us

  (crit/quick-bench (loop [idx 0
                           hm (transient {})]
                      (if (< idx 1000)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;112us
  ;;123us
  (crit/quick-bench
   (let [phm (loop [idx 0
                    hm (transient {})]
               (if (< idx 1000)
                 (recur (unchecked-inc idx)
                        (assoc! hm idx idx))
                 (persistent! hm)))]
     (loop [idx 0]
       (when (< idx 1000)
         (get phm idx)
         (recur (unchecked-inc idx))))))
  ;;168us

  (crit/quick-bench (loop [idx 0
                           hm (transient PersistentHashMap/EMPTY)]
                      (if (< idx 1000)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;47us
  ;;jdk-17 50us

  (crit/quick-bench
   (let [phm (loop [idx 0
                    hm (transient PersistentHashMap/EMPTY)]
               (if (< idx 1000)
                 (recur (unchecked-inc idx)
                        (assoc! hm idx idx))
                 (persistent! hm)))]
     (loop [idx 0]
       (when (< idx 1000)
         (get phm idx)
         (recur (unchecked-inc idx))))))
  ;; 83us

  ;;Useful to profile small things sometimes.
  (dotimes [idx 100000000]
    (loop [idx 0
           hm (transient {})]
      (if (< idx 20)
        (recur (unchecked-inc idx)
               (assoc! hm idx idx))
        (persistent! hm))))


  (def hm (let [hm (HashMap.)
                _ (dotimes [idx 60]
                    (.put hm idx idx))]
            hm))
  (let [nhm (HashBase.)]
    (.keyspaceSplit hm 146 291 false nhm)
    (.printNodes nhm))

  (let [hm (HashMap.)
        _ (dotimes [idx 60]
            (.put hm idx idx))]
    (map iterator-seq (.splitKeys hm 7)))

  (def small-int-profile (profile-datastructures (repeatedly 2 #(rand-int 100000))))
  ;;jdk-8
  ;;({:construct-μs 0.04298386356008815,
  ;;  :access-μs 0.040345482495456206,
  ;;  :iterate-μs 0.05087595836156305,
  ;;  :ds-name :hamf-hashmap}
  ;; {:construct-μs 0.044846695459721565,
  ;;  :access-μs 0.02446975706544951,
  ;;  :iterate-μs 0.04827733417909167,
  ;;  :ds-name :java-hashmap}
  ;; {:construct-μs 0.10289826952319454,
  ;;  :access-μs 0.08631483123589048,
  ;;  :iterate-μs 0.05130890786182448,
  ;;  :ds-name :hamf-equiv-hashmap}
  ;; {:construct-μs 0.14209144548280342,
  ;;  :access-μs 0.08244324142027323,
  ;;  :iterate-μs 0.04715165522789527,
  ;;  :ds-name :clj-transient}
  ;; {:construct-μs 0.16351660268608148,
  ;;  :access-μs 0.09678529907937389,
  ;;  :iterate-μs 0.05798288225070914,
  ;;  :ds-name :hamf-transient})

  (def int-profile (profile-datastructures (repeatedly 1000 #(rand-int 100000))))
  ;; jdk-8
  ;; ({:construct-μs 23.099907694058732,
  ;;   :access-μs 11.601162415160529,
  ;;   :iterate-μs 8.334938366460666,
  ;;   :ds-name :java-hashmap}
  ;;  {:construct-μs 34.1529201495073,
  ;;   :access-μs 12.681086848216172,
  ;;   :iterate-μs 14.304917063893303,
  ;;   :ds-name :hamf-hashmap}
  ;;  {:construct-μs 63.69087151015229,
  ;;   :access-μs 48.95698405726371,
  ;;   :iterate-μs 17.277700236284005,
  ;;   :ds-name :hamf-equiv-hashmap}
  ;;  {:construct-μs 66.86508244206775,
  ;;   :access-μs 50.38417851142474,
  ;;   :iterate-μs 21.130427287409592,
  ;;   :ds-name :hamf-transient}
  ;;  {:construct-μs 110.31809377289377,
  ;;   :access-μs 44.23049499782514,
  ;;   :iterate-μs 35.53727620563613,
  ;;   :ds-name :clj-transient})
  ;; jdk-17
  ;; ({:construct-μs 31.915435858163935,
  ;;   :access-μs 12.193707814495532,
  ;;   :iterate-μs 7.316710778209167,
  ;;   :ds-name :java-hashmap}
  ;;  {:construct-μs 45.07551537356322,
  ;;   :access-μs 21.033255470685386,
  ;;   :iterate-μs 22.979641088509506,
  ;;   :ds-name :hamf-hashmap}
  ;;  {:construct-μs 73.53580794223828,
  ;;   :access-μs 49.47928398962892,
  ;;   :iterate-μs 22.8927910698984,
  ;;   :ds-name :hamf-equiv-hashmap}
  ;;  {:construct-μs 77.98202366716494,
  ;;   :access-μs 52.42915830218901,
  ;;   :iterate-μs 20.852072754333744,
  ;;   :ds-name :hamf-transient}
  ;;  {:construct-μs 134.15703174603175,
  ;;   :access-μs 47.73247937254903,
  ;;   :iterate-μs 37.22873836295491,
  ;;   :ds-name :clj-transient})

  (def big-int-profile (profile-datastructures (repeatedly 100000 #(rand-int 100000))))
  ;;jdk-8
  ;; ({:construct-μs 4818.585603174603,
  ;;   :access-μs 3115.862890625,
  ;;   :iterate-μs 903.9770223214286,
  ;;   :ds-name :java-hashmap}
  ;;  {:construct-μs 8781.270125000001,
  ;;   :access-μs 8000.021961538461,
  ;;   :iterate-μs 1914.1505256410255,
  ;;   :ds-name :hamf-hashmap}
  ;;  {:construct-μs 12504.3571875,
  ;;   :access-μs 11433.55188888889,
  ;;   :iterate-μs 2047.054111111111,
  ;;   :ds-name :hamf-equiv-hashmap}
  ;;  {:construct-μs 12913.919562500001,
  ;;   :access-μs 12543.427979166667,
  ;;   :iterate-μs 2056.0890714285715,
  ;;   :ds-name :hamf-transient}
  ;;  {:construct-μs 13137.229833333335,
  ;;   :access-μs 10124.450916666668,
  ;;   :iterate-μs 3152.3616718750004,
  ;;   :ds-name :clj-transient})

  (def double-profile (profile-datastructures (repeatedly 1000 #(rand 100000))))
  (def str-profile (profile-datastructures (map str (repeatedly 1000 #(rand-int 100000)))))
  ;;jdk-8
  ;;({:construct-μs 23.930453143122246,
  ;;  :access-μs 13.627138225718937,
  ;;  :ds-name :java-hashmap}
  ;; {:construct-μs 43.07852368684701,
  ;;  :access-μs 32.3816567357513,
  ;;  :ds-name :hamf-hashmap}
  ;; {:construct-μs 68.1719815066939,
  ;;  :access-μs 58.411238927738935,
  ;;  :ds-name :hamf-equiv-hashmap}
  ;; {:construct-μs 73.45427547307133,
  ;;  :access-μs 66.72418882978724,
  ;;  :ds-name :hamf-transient}
  ;; {:construct-μs 128.5458824027073,
  ;;  :access-μs 53.36163036127426,
  ;;  :ds-name :clj-transient})
  (def kw-profile (profile-datastructures (map (comp keyword str)
                                               (repeatedly 1000 #(rand-int 100000)))))
  ;;jdk-8
  ;; ({:construct-μs 37.88887029341394,
  ;;   :access-μs 18.842061234058516,
  ;;   :iterate-μs 12.900347590258127,
  ;;   :ds-name :java-hashmap}
  ;;  {:construct-μs 40.78816281271129,
  ;;   :access-μs 31.311148279404215,
  ;;   :iterate-μs 26.028699949122363,
  ;;   :ds-name :hamf-equiv-hashmap}
  ;;  {:construct-μs 44.76396859578287,
  ;;   :access-μs 37.260138704523115,
  ;;   :iterate-μs 25.54532113194623,
  ;;   :ds-name :hamf-transient}
  ;;  {:construct-μs 48.99040834149526,
  ;;   :access-μs 36.94391981651376,
  ;;   :iterate-μs 26.269819758868937,
  ;;   :ds-name :hamf-hashmap}
  ;;  {:construct-μs 98.01407147466925,
  ;;   :access-μs 33.85242917783735,
  ;;   :iterate-μs 37.85340298225746,
  ;;   :ds-name :clj-transient})

  )

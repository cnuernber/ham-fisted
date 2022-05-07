(ns ham-fisted.hash-map-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set]
            [criterium.core :as crit])
  (:import [ham_fisted HashMap PersistentHashMap BitmapTrie]
           [java.util ArrayList Collections Map Collection]
           [java.util.function BiFunction]))

(defonce orig PersistentHashMap/EMPTY)


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
    (is (= {nil :b :a :b} (assoc nilmap :a :b)))
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
                 (.get ds (.get dlist idx))))]
    {:construct-μs (:mean-μs ctime)
     :access-μs (:mean-μs atime)}))


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
  (let [hm (HashMap. PersistentHashMap/equivHashProvider)
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
  (let [n-elems 1000
        hn-elems (quot 1000 2)
        src-data (repeatedly n-elems #(rand-int 100000000))
        lhs (->array-list (take hn-elems src-data))
        rhs (->array-list (drop hn-elems src-data))
        ^PersistentHashMap hm1 (hamf-transient lhs)
        bfn (reify BiFunction (apply [this a b] (+ a b)))
        hm2 (hamf-transient rhs)
        un1 (.union hm1 hm2 bfn)
        un2 (.union un1 hm2 bfn)
        single-sum (reduce + (range hn-elems))]
    (is (= (count un1) 1000))
    (is (= (set src-data) (set (keys un1))))
    (is (= (count un2) 1000))
    (is (= (* 2 single-sum) (reduce + (vals un1))))
    (is (= (* 3 single-sum) (reduce + (vals un2))))))


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


  (def int-profile (profile-datastructures (repeatedly 1000 #(rand-int 100000))))
  ;; jdk-8
  ;;  ({:construct-μs 24.788405885214008,
  ;;  :access-μs 13.965560035219426,
  ;;  :ds-name :java-hashmap}
  ;; {:construct-μs 37.363700646042986,
  ;;  :access-μs 27.359574198159564,
  ;;  :ds-name :hamf-hashmap}
  ;; {:construct-μs 66.22582915842672,
  ;;  :access-μs 46.921109418931586,
  ;;  :ds-name :hamf-equiv-hashmap}
  ;; {:construct-μs 68.11221358447489,
  ;;  :access-μs 54.25726681049983,
  ;;  :ds-name :hamf-transient}
  ;; {:construct-μs 113.06234441939121,
  ;;  :access-μs 52.703857229753226,
  ;;  :ds-name :clj-transient})
  (def double-profile (profile-datastructures (repeatedly 1000 #(rand-int 100000))))
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
 ;;  ({:construct-μs 29.58943302505967,
 ;;  :access-μs 19.902508271217474,
 ;;  :ds-name :java-hashmap}
 ;; {:construct-μs 38.35744891443167,
 ;;  :access-μs 30.663697570397485,
 ;;  :ds-name :hamf-equiv-hashmap}
 ;; {:construct-μs 49.081114095052094,
 ;;  :access-μs 40.52704244139046,
 ;;  :ds-name :hamf-transient}
 ;; {:construct-μs 52.87004256437204,
 ;;  :access-μs 40.30576300499933,
 ;;  :ds-name :hamf-hashmap}
 ;; {:construct-μs 99.67729208250168,
 ;;  :access-μs 33.36973040195426,
 ;;  :ds-name :clj-transient})

  )

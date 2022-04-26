(ns ham-fisted.hash-map-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as set])
  (:import [ham_fisted HashMap PersistentHashMap HashBase]))

(defonce orig PersistentHashMap/EMPTY)


(deftest simple-assoc
  (let [orig PersistentHashMap/EMPTY]
    (is (= {:a :b} (assoc orig :a :b)))
    (is (= {} (-> (assoc orig :a :b)
                  (dissoc :a)))))

  (let [nilmap (assoc orig nil :b)]
    (is (= {nil :b} nilmap))
    (is (= {nil :b :a :b} (assoc nilmap :a :b)))
    (is (= 1 (count nilmap)))
    (is (= 1 (count (dissoc nilmap :a))))
    (is (= 2 (count (assoc nilmap :a :b))))
    (is (= 0 (count (dissoc nilmap nil))))
    (is (= #{nil :a} (set (keys (assoc nilmap :a :b)))))))


(deftest random-assoc-dissoc
  (let [data (shuffle (range 1000))
        dissoc-vals (take 100 data)
        data (set data)
        dissoc-data (set/difference data (set dissoc-vals))]
    (testing "immutable"
      (let [alldata (reduce #(assoc %1 %2 %2)
                            orig
                            data)
            disdata (reduce #(dissoc %1 %2) alldata dissoc-vals)]
        (is (= 900 (count disdata)))
        (is (= dissoc-data (set (keys disdata))))
        (is (= 1000 (count alldata)))
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
        (is (= 900 (count disdata)))
        (is (= 1000 (count alldata)))))
    (testing "mutable"
      (let [alldata (HashMap.)
            _ (doseq [item data]
                (.put alldata item item))
            disdata (.clone alldata)
            _ (doseq [item dissoc-vals]
                (.remove disdata item))]
        (is (= 900 (count disdata)))
        (is (= 1000 (count alldata)))))))


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
                     (reduce +))))))

(comment

  (do
    (require '[criterium.core :as crit])
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
  (crit/quick-bench (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
                      (dotimes [idx 2]
                        (.put hm idx idx))))
  ;;70ns - twice as fast as clojure's transient for small case.  hasheq is the long
  ;;poll in the tent.
  ;;jdk-17 - 61ns

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
  (crit/quick-bench (let [hm (HashMap. PersistentHashMap/equivHashProvider)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;41us
  ;;jdk-17 - 39us

  (crit/quick-bench (let [hm (java.util.HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;17us
  ;;jdk-17 - 21us
  (crit/quick-bench (loop [idx 0
                           hm (transient {})]
                      (if (< idx 1000)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;112us
  ;;123us
  (crit/quick-bench (loop [idx 0
                           hm (transient PersistentHashMap/EMPTY)]
                      (if (< idx 1000)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;47us
  ;;jdk-17 50us



  ;;Useful to profile small things sometimes.
  (dotimes [idx 100000000]
    (loop [idx 0
           hm (transient {})]
      (if (< idx 20)
        (recur (unchecked-inc idx)
               (assoc! hm idx idx))
        (persistent! hm))))

  )

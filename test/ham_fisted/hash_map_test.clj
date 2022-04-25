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

(comment

  (do
    (require '[criterium.core :as crit])
    (def hm (HashMap.))
    (def orig PersistentHashMap/EMPTY)
    )

  (dotimes [idx 10]
    (.put hm idx idx))

  (dotimes [idx 10000]
    (.put hm idx idx))

  (crit/quick-bench (let [hm (HashMap.)]
                      (dotimes [idx 2]
                        (.put hm idx idx))))
  ;;34ns

  (crit/quick-bench (let [hm (java.util.HashMap.)]
                      (dotimes [idx 2]
                        (.put hm idx idx))))
  ;;36ns

  (crit/quick-bench (loop [idx 0
                           hm (transient {})]
                      (if (< idx 2)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;113ns

  (loop [idx 0
         hm (transient PersistentHashMap/EMPTY)]
    (if (< idx 2)
      (recur (unchecked-inc idx)
             (assoc! hm idx idx))
      (persistent! hm)))

  (crit/quick-bench (loop [idx 0
                           hm (transient PersistentHashMap/EMPTY)]
                      (if (< idx 2)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))

  (crit/quick-bench (.shallowClone (.unsafeGetHashBase PersistentHashMap/EMPTY)))



  (crit/quick-bench (let [hm (HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;21us

  (crit/quick-bench (let [hm (java.util.HashMap.)]
                      (dotimes [idx 1000]
                        (.put hm idx idx))))
  ;;17us
  (crit/quick-bench (loop [idx 0
                           hm (transient {})]
                      (if (< idx 1000)
                        (recur (unchecked-inc idx)
                               (assoc! hm idx idx))
                        (persistent! hm))))
  ;;112us

  (dotimes [idx 10000]
    (let [hm (HashMap.)]
      (dotimes [idx 100000]
        (.put hm idx idx))
      hm))


  (dotimes [idx 100000000]
    (loop [idx 0
           hm (transient PersistentHashMap/EMPTY)]
      (if (< idx 2)
        (recur (unchecked-inc idx)
               (assoc! hm idx idx))
        (persistent! hm))))

  )

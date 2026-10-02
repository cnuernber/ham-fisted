(ns ham-fisted.bloom-filter-test
  (:require [ham-fisted.bloom-filter :as hamf-bf]
            [clojure.test :refer [deftest is]])
  (:import [java.util UUID]))



(deftest uuid-test
  (let [M (long 1e6)
        uuids (vec (repeatedly M #(UUID/randomUUID)))
        bf (hamf-bf/bloom-filter M 0.01)
        pred (hamf-bf/make-obj-predicate bf)]
    (reduce hamf-bf/insert-hash! bf (map hamf-bf/hash-obj uuids))
    (is (pred (uuids 0)))
    (let [false-pos (long (reduce (fn [eax _]
                                    (if (pred (UUID/randomUUID))
                                      (inc eax)
                                      eax))
                                  0
                                  (range M)))
          true-pos (long (reduce (fn [eax u]
                                   (if (pred u)
                                     (inc eax)
                                     eax))
                                 0
                                 uuids))]
      (is (< false-pos 2000))
      (is (== M true-pos)))))


(deftest small-integer-types
  (let [bf (hamf-bf/bloom-filter 100 0.01)]
    (doseq [v [(int 5) (short 6) (byte 7) 8]]
      (hamf-bf/insert-obj bf v))
    (is (every? #(hamf-bf/contains? bf %) [(int 5) (short 6) (byte 7) 8 5 6 7]))))


(deftest uuid-predicate-is-threadsafe
  (let [uuids (vec (repeatedly 2000 #(UUID/randomUUID)))
        bf (hamf-bf/add-uuids! (hamf-bf/bloom-filter 2000 0.01) uuids)
        pred (hamf-bf/make-uuid-pred bf)]
    (is (every? true? (apply concat (pmap (fn [_] (mapv pred uuids)) (range 8)))))
    (is (every? #(hamf-bf/contains? bf %) uuids))))

(ns ham-fisted.parallel-test
  (:require [ham-fisted.api :as api]
            [ham-fisted.reduce :as hamf-rf]
            [ham-fisted.mut-map :as hamf-map]
            [ham-fisted.lazy-noncaching :as lznc]
            [clojure.test :refer [deftest is]])
  (:import [ham_fisted Sum]))


(deftest spliterator-preduce
  (let [data (api/immut-map (lznc/map #(api/vector % %) (range 10000)))
        sum-data (fn [opts m]
                   (-> (hamf-rf/preduce #(Sum.) hamf-rf/double-consumer-accumulator
                                        (fn [^Sum l ^Sum r] (.merge l r) l)
                                        opts
                                        m)
                       (deref)
                       :sum
                       (long)))
        opts {:min-n 5 :ordered? true}]
    (is (= 49995000 (sum-data opts (api/keys data))))
    (is (= 49995000 (sum-data opts (api/vals data))))
    (is (= 49995000 (sum-data (assoc opts :ordered? false) (api/keys data))))
    (is (= 49995000 (sum-data (assoc opts :ordered? false) (api/vals data))))
    (is (= 49995000 (sum-data (assoc opts :ordered? false) (hamf-map/keyset data))))
    (is (= 49995000 (sum-data (assoc opts :ordered? false) (hamf-map/values data))))
    (let [small (api/immut-map (lznc/map #(api/vector % %) (range 8)))]
      (is (= 28 (sum-data opts (api/keys small))))
      (is (= 28 (sum-data opts (api/vals small)))))))



;;There was an issue with small upmaps hanging
(deftest small-upmap
  (is (= (api/range 5)
         (api/sort (api/upmap #(+ % 1) (range -1 4))))))

(deftest small-pmap
  (is (= (api/range 10)
         (api/pmap #(+ % 1) (range -1 9)))))

;;This caused a hang before wrapping queue io with boxes.
(deftest upmap-nil-return
  (is (every? nil? (api/upgroups 10000
                                 (fn [^long sidx ^long eidx]
                                   nil)))))

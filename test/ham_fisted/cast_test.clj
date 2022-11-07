(ns ham-fisted.cast-test
  (:require [ham-fisted.api :as api]
            [ham-fisted.lazy-noncaching :as lznc]
            [clojure.test :refer [deftest is]]))


(deftest basic-casts
  (is (thrown? Exception (api/long-array [0.0 ##NaN 0.0])))
  (is (api/double-eq [0.0 ##NaN 0.0] (api/double-array [0 nil 0])))
  (is (thrown? Exception (api/long-array (lznc/map (fn ^double [^double v]
                                                     (+ v 2.0))
                                                   [0.0 ##NaN 0.0]))))
  (is (thrown? Exception (api/double-array (->> [0.0 ##NaN 0.0]
                                                (lznc/map (fn ^double [^double v]
                                                            (+ v 2.0)))
                                                (lznc/filter (fn [^long v]
                                                               (not (== 0 (rem v 3)))))))))
  (is (thrown? Exception (vec (lznc/map (fn ^long [^long v]
                                          (+ v 2))
                                        [0.0 ##NaN 0.0]))))
  (is (thrown? Exception (vec (->> [0.0 ##NaN 0.0]
                                   (lznc/map (fn ^double [^double v]
                                               (+ v 2.0)))
                                   (lznc/filter (fn [^long v]
                                                  (not (== 0 (rem v 3)))))))))
  (is (= [false false false] (api/->random-access (api/boolean-array [nil ##NaN nil]))))
  (is (= [false false] (api/->random-access (api/boolean-array [nil nil])))))

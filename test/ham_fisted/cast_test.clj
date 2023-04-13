(ns ham-fisted.cast-test
  (:require [ham-fisted.api :as api]
            [ham-fisted.function :as hamf-fn]
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
                                                (lznc/filter (hamf-fn/long-predicate
                                                              v (not (== 0 (rem v 3)))))))))
  (is (thrown? Exception (vec (lznc/map (hamf-fn/long-unary-operator v (+ v 2))
                                        [0.0 ##NaN 0.0]))))
  (is (thrown? Exception (vec (->> [0.0 ##NaN 0.0]
                                   (hamf-fn/double-unary-operator v (+ v 2.0))
                                   (lznc/filter (hamf-fn/long-predicate v (not (== 0 (rem v 3)))))))))
  (is (= [false false false] (api/->random-access (api/boolean-array [nil ##NaN nil]))))
  (is (= [false false] (api/->random-access (api/boolean-array [nil nil])))))

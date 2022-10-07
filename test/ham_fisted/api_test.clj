(ns ham-fisted.api-test
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.api :as hamf]))



(deftest parallism-primitives-pass-errors
  (is (thrown? Exception (count (hamf/upmap
                                 (fn [^long idx]
                                   (when (== idx 77) (throw (Exception. "Error!!"))) idx)
                                 (range 100)))))
  (is (thrown? Exception (count (hamf/pmap (fn [^long idx]
                                             (when (== idx 77) (throw (Exception. "Error!!"))) idx)
                                           (range 100)))))
  (is (thrown? Exception (hamf/upgroups (fn [^long sidx ^long eidx]
                                          (when (>= sidx 70)
                                            (throw (Exception. "Error!!"))) sidx))))
  (is (thrown? Exception (hamf/pgroups (fn [^long sidx ^long eidx]
                                         (when (>= sidx 70)
                                           (throw (Exception. "Error!!"))) sidx)))))

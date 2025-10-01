(ns ham-fisted.defprotocol-test.other-test
  (:refer-clojure :exclude [defprotocol extend-type extend extend-protocol satisfies? extends?])
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.defprotocol :refer [defprotocol extend-type extend extend-protocol satisfies? extends?]]
            [ham-fisted.defprotocol-test])
  (:refer-clojure :exclude [defprotocol extend-type extend extend-protocol satisfies? extends?]))

(defn cf [val]
  (let [aseq (ham-fisted.defprotocol-test/f val)]
    (count aseq)))
(extend-protocol ham-fisted.defprotocol-test/P String
                 (f [s] (seq s)))
(deftest test-resolve-type-hints-in-protocol-methods
  (is (= 4 (cf "test"))))

(ns ham-fisted.defprotocol-test.examples
  (:refer-clojure :exclude [defprotocol])
  (:require [ham-fisted.defprotocol :refer [defprotocol]]))

(defprotocol ExampleProtocol
  "example protocol used by clojure tests"

  (foo [a] "method with one arg")
  (bar [a b] "method with two args")
  (^String baz [a] [a b] "method with multiple arities")
  (with-quux [a] "method name with a hyphen"))

(defprotocol MarkerProtocol
  "a protocol with no methods")

(defprotocol MarkerProtocol2)

(definterface ExampleInterface
  (hinted [^int i])
  (hinted [^String s]))

(defprotocol LongsHintedProto
  (^longs longs-hinted [_]))

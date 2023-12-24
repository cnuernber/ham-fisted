(ns ham-fisted.defprotocol-test.more-examples
  (:require [ham-fisted.defprotocol :as hamf-defp]))

(hamf-defp/defprotocol SimpleProtocol
  "example protocol used by clojure tests. Note that
   foo collides with examples/ExampleProtocol."

  (foo [a] ""))

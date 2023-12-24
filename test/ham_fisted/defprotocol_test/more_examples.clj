(ns ham-fisted.protocols-test.more-examples)

(defprotocol SimpleProtocol
  "example protocol used by clojure tests. Note that
   foo collides with examples/ExampleProtocol."

  (foo [a] ""))

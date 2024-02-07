(ns ham-fisted.print)


(defmacro implement-tostring-print
  "Implement tostring printing for a particular type name."
  [typename]
  `(.addMethod ~(with-meta 'print-method {:tag 'clojure.lang.MultiFn}) ~typename
               ham_fisted.Reductions/ToStringPrint))

(ns ham-fisted.print)


(defmacro implement-tostring-print
  "Implement tostring printing for a particular type name."
  [typename]
  `(defmethod print-method ~typename
     [buf# w#]
     (.write ^java.io.Writer w# (.toString ^Object buf#))))

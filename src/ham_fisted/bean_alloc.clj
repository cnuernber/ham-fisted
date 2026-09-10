(ns ham-fisted.bean-alloc
  "Measure JVM allocation by wrapping forms, analogous to clojure.core/time."
  (:import (com.sun.management ThreadMXBean)
           (java.lang.management ManagementFactory)))

(defn total-allocated-bytes
  "Sum of bytes allocated across all threads (live and terminated) since the
   JVM started. Backed by com.sun.management.ThreadMXBean — HotSpot/OpenJDK only."
  ^long []
  (.getTotalThreadAllocatedBytes
   ^ThreadMXBean (ManagementFactory/getThreadMXBean)))

(defmacro report
  "Like clojure.core/time, but for allocation: evaluates body, prints the
   number of bytes allocated across all threads during evaluation, and returns
   the body's value."
  [& body]
  `(let [start# (total-allocated-bytes)
         ret# (do ~@body)]
     (prn (str "Allocation: " (- (total-allocated-bytes) start#) " bytes"))
     ret#))

(defmacro measure
  "Evaluates body and returns {:value <body-value> :bytes <allocated>}.
   Use when you want to assert on or aggregate the measurement."
  [& body]
  `(let [start# (total-allocated-bytes)
         ret# (do ~@body)]
     {:value ret#
      :bytes (- (total-allocated-bytes) start#)}))

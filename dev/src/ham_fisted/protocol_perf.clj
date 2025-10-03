(ns ham-fisted.protocol-perf
  (:require [ham-fisted.defprotocol :as hamf-defproto]
            [ham-fisted.protocols :as hamf-proto]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.api :as hamf]
            [ham-fisted.reduce :as hamf-rf])
  (:import [java.util LongSummaryStatistics]
           [java.util.function LongConsumer])
  (:gen-class))
(set! *unchecked-math* :warn-on-boxed)
(set! *warn-on-reflection* true)

(hamf-defproto/defprotocol HamfMemsize
  (^long hamf-memsize [m]))

(hamf-defproto/extend Double HamfMemsize {:hamf-memsize 24})
(hamf-defproto/extend Long HamfMemsize {:hamf-memsize 24})
(hamf-defproto/extend clojure.lang.Keyword HamfMemsize {:hamf-memsize 48})

(hamf-defproto/extend-protocol HamfMemsize
  String
  (hamf-memsize [s] (+ 24 (.length ^String s)))
  java.util.Collection
  (hamf-memsize [c] (hamf/lsum (lznc/map (fn ^long [d] (+ 24 (hamf-memsize d))) c)))
  java.util.Map
  (hamf-memsize [c] (hamf/lsum (lznc/map (fn ^long [kv]
                                           (+ 36 (+ (hamf-memsize (key kv)) (hamf-memsize (val kv)))))
                                         c))))

(clojure.core/defprotocol CoreMemsize
  (core-memsize [m]))

(clojure.core/extend-protocol CoreMemsize
  Double (core-memsize [d] 24)
  Long (core-memsize [l] 24)
  clojure.lang.Keyword (core-memsize [k] 48)
  String (core-memsize [s] (+ 24 (.length ^String s)))
  java.util.Collection
  (core-memsize [c]
    (hamf/lsum (lznc/map (fn ^long [d] (+ 24 (long (core-memsize d)))) c))
    #_(reduce
       (fn [s v] (+ s 24 (core-memsize v)))
       0
       c))
  java.util.Map
  (core-memsize [m]
    (hamf/lsum (lznc/map (fn ^long [kv]
                           (+ 36 (+ (long (core-memsize (key kv)))
                                    (long (core-memsize (val kv))))))
                         m))
    #_(reduce
       (fn [s [k v]] (+ s 36 (core-memsize k) (core-memsize v)))
       0
       m)))

(def test-datastructure
  {:a "hello"
   :b 24
   :c (into [] (repeat 1000 (rand)))
   :d (into [] (repeat 1000 1))})

(defn test-callit
  ^long [v vv]
  (if (instance? v Number)
    (ham_fisted.Casts/longCast v)
    (.invokePrim ^clojure.lang.IFn$OL v vv)))


(def measure-data (into [] (repeat 10000 test-datastructure)))

(defn multithread-test
  [measure-fn]
  (hamf/lsum (hamf/pmap measure-fn measure-data)))

(defn -main
  [& args]
  (println "Core protocols")
  (dotimes [idx 10]
    (time (multithread-test core-memsize)))

  (println "hamf protocols")
  (dotimes [idx 10]
    (time (multithread-test hamf-memsize)))
  :ok)

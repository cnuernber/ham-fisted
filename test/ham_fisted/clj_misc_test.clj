(ns ham-fisted.clj-misc-test
  (:require [clojure.test :refer [deftest is]]
            [ham-fisted.api :as hamf]
            [ham-fisted.function :as hamf-fn]
            [ham-fisted.process :as proc])
  (:import [java.util Comparator]
           [it.unimi.dsi.fastutil.doubles DoubleComparator]
           [clojure.lang IFn$OL IFn$OD IFn$DO]
           [java.util.function DoubleConsumer]))


(deftest function-macros
  (is (= "1.5" (.invokePrim ^IFn$DO (hamf-fn/double->obj v (str v)) 1.5)))
  (let [acc (atom 0.0)
        c (hamf-fn/double-consumer v (swap! acc + v))]
    (.accept ^DoubleConsumer c 2.0)
    (is (= 2.0 @acc)))
  (is (= 6 (.invokePrim ^IFn$OL (hamf-fn/obj->long v (inc 1) (+ v 1)) 5)))
  (is (= 6.0 (.invokePrim ^IFn$OD (hamf-fn/obj->double v (inc 1) (+ v 1)) 5))))


(deftest nan-comparators-are-consistent
  (doseq [c [hamf-fn/comp-nan-first hamf-fn/comp-nan-last]]
    (is (zero? (.compare ^Comparator c nil nil)))
    (is (zero? (.compare ^DoubleComparator c Double/NaN Double/NaN)))
    (is (= (- (.compare ^Comparator c nil 1)) (.compare ^Comparator c 1 nil))))
  (is (= (concat (repeat 3 nil) (range 5))
         (vec (hamf/sort hamf-fn/comp-nan-first [nil 3 nil 0 4 nil 1 2])))))


(deftest stream-strings-utf8
  (let [s (apply str (repeat 50 "é∑"))
        input (java.io.ByteArrayInputStream. (.getBytes s "UTF-8"))]
    (is (= s (apply str (proc/stream->strings input 3 java.nio.charset.StandardCharsets/UTF_8))))))

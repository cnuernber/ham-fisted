(ns ham-fisted.hlet-test
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.primitive-invoke :as pi]
            [ham-fisted.hlet :as h]
            [clojure.test :refer [deftest is]]))


(set! *unchecked-math* :warn-on-boxed)



(deftest basic-hlet
  (h/let [[x y] (dbls (if true
                        (hamf/double-array [1 2])
                        (hamf/double-array [3 4])))
          look-fn (pi/->ddd (fn ^double [^double a ^double b] (+ a b)))
          ;;test singular case
          sx (dbls (rand-int 100))]
    (is (= 3.0 (pi/ddd look-fn x y)))))


(comment
  (require '[clj-java-decompiler.core :refer [disassemble]])

  (disassemble (h/let [[x y] (dbls (if true
                                     (hamf/double-array [1 2])
                                     (hamf/double-array [3 4])))
                       look-fn (pi/->ddd (fn ^double [^double a ^double b] (+ a b)))]
                 (look-fn x y)
                 (pi/ddd look-fn x y)))

  )

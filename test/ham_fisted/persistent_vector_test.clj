(ns ham-fisted.persistent-vector-test
  (:require [criterium.core :as crit])
  (:import [ham_fisted MutList]
           [java.util List ArrayList]))
(comment
  (def m (doto (MutList.) (.addAll (range 36))))
  )

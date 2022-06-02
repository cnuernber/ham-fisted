(ns ham-fisted.persistent-vector-test
  (:require [criterium.core :as crit]
            [ham-fisted.api :as api])
  (:import [ham_fisted MutList ImmutList]
           [java.util List ArrayList Collections]))
(comment
  (def m (doto (MutList.) (.addAll (range 36))))
  (def m ImmutList/EMPTY)
  )

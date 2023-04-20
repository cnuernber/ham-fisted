(ns ham-fisted.api-j9
  (:require [ham-fisted.api :as api])
  (:import [ham_fisted ArrayImmutList9]))


(defn immut-list
  [data]
  (if (instance? api/obj-ary-cls data)
    (ArrayImmutList9. data 0 (alength ^objects data) nil)
    (api/immut-list data)))

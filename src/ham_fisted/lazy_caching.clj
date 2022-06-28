(ns ham-fisted.lazy-caching
  (:require [ham-fisted.lazy-noncaching :as lzn])
  (:import [java.util RandomAccess]
           [ham_fisted Transformables$CachingIterable Transformables$CachingList])
  (:refer-clojure :exclude [map filter concat repeatedly]))


(defn cached
  [item]
  (let [item (lzn/->collection item)]
    (if (instance? RandomAccess item)
      (Transformables$CachingList. item nil)
      (Transformables$CachingIterable. item nil))))


(defn map
  ([f arg]
   (-> (lzn/map f arg)
       (cached)))
  ([f arg & args]
   (-> (apply lzn/map f arg args)
       (cached))))


(defn filter
  [pred coll]
  (-> (lzn/filter pred coll)
      (cached)))


(defn concat
  ([] nil)
  ([a] a)
  ([a b] (-> (lzn/concat a b)
             (cached)))
  ([a b & args]
   (-> (apply lzn/concat a b args)
       (cached))))


(defn repeatedly
  ([f] (clojure.core/repeatedly f))
  ([n f] (-> (lzn/repeatedly n f)
             (cached))))

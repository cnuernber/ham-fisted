(ns ham-fisted.lazy-caching
  (:require [ham-fisted.lazy-noncaching :as lznc])
  (:import [java.util RandomAccess List Iterator]
           [ham_fisted Transformables$CachingIterable Transformables$CachingList
            ArrayLists ArrayHelpers ArrayLists$ObjectArrayList]
           [clojure.lang ISeq ArraySeq IFn])
  (:refer-clojure :exclude [map filter concat repeatedly]))

(defn- seq-map-recur
  ([f ^ISeq c1 ^ISeq c2]
   (when (and c1 c2)
     (cons (f (.first c1) (.first c2))
           (lazy-seq (seq-map-recur f (.next c1) (.next c2))))))
  ([f ^ISeq c1 ^ISeq c2 ^ISeq c3]
   (when (and c1 c2 c3)
     (cons (f (.first c1) (.first c2) (.first c3))
           (lazy-seq (seq-map-recur f (.next c1) (.next c2) (.next c3))))))
  ([f ^ISeq c1 ^ISeq c2 ^ISeq c3 ^ISeq c4]
   (when (and c1 c2 c3 c4)
     (cons (f (.first c1) (.first c2) (.first c3) (.first c4))
           (lazy-seq (seq-map-recur f (.next c1) (.next c2) (.next c3) (.next c4))))))
  ([f ^List cs]
   (let [ncs (.size cs)
         args (ArrayLists/objectArray ncs)         
         next? (loop [idx 0 next? true]
                 (if (< idx ncs)
                   (let [^ISeq c (.get cs (unchecked-int idx))
                         _ (ArrayHelpers/aset args (unchecked-int idx) (.first c))
                         c (.next c)]               
                     (.set cs (unchecked-int idx) c)
                     (recur (unchecked-inc idx) (and next? c)))
                   next?))]
     (cons (.applyTo ^IFn f (ArraySeq/create args))
           (when next? 
             (lazy-seq (seq-map-recur f cs)))))))

(defn- seq-map
  ([f arg] (clojure.core/map f arg))
  ([f c1 c2]
   (let [^ISeq c1 (seq c1)
         ^ISeq c2 (seq c2)]
     (if (and c1 c2)
       (seq-map-recur f c1 c2)
       '())))
  ([f c1 c2 c3]
   (let [^ISeq c1 (seq c1)
         ^ISeq c2 (seq c2)
         ^ISeq c3 (seq c3)]
     (if (and c1 c2 c3)
       (seq-map-recur f c1 c2 c3)
       '())))
  ([f c1 c2 c3 c4]
   (let [^ISeq c1 (seq c1)
         ^ISeq c2 (seq c2)
         ^ISeq c3 (seq c3)
         ^ISeq c4 (seq c4)]
     (if (and c1 c2 c3 c4)
       (seq-map-recur f c1 c2 c3 c4)
       '())))
  ([f c1 c2 c3 c4 args]
   (let [cs (ArrayLists$ObjectArrayList.)
         c1 (seq c1)
         c2 (seq c2)
         c3 (seq c3)
         c4 (seq c4)]
     (if (and c1 c2 c3 c4)
       (do 
         (.add cs c1)
         (.add cs c2)
         (.add cs c3)
         (.add cs c4)
         (let [all-valid? 
               (reduce (fn [acc v]
                         (if-let [s (seq v)]
                           (do (.add cs s)
                               acc)
                           (reduced false)))
                       true
                       args)]
           (if all-valid?
             (seq-map-recur f cs)
             '())))
       '()))))


(defn map
  ([f arg] (clojure.core/map f arg))
  ([f c1 c2] (->> (lznc/map f c1 c2) (seq)))
  ([f c1 c2 c3] (seq-map f c1 c2 c3))
  ([f c1 c2 c3 c4] (seq-map f c1 c2 c3 c4))
  ([f c1 c2 c3 c4 & args] (seq-map f c1 c2 c3 c4 args)))


(defn- seq-tuple-map
  ([f ^ISeq c1 ^ISeq c2]
   (let [args (ArrayLists/objectArray 2)
         _ (ArrayHelpers/aset args (unchecked-int 0) (.first c1))
         _ (ArrayHelpers/aset args (unchecked-int 1) (.first c2))
         c1 (.next c1)
         c2 (.next c2)]
     (cons (f (ArrayLists/toList args))
           (when (and c1 c2)
             (lazy-seq (seq-tuple-map f c1 c2))))))
  ([f ^List cs]
   (let [n-args (.size cs)
         args (ArrayLists/objectArray n-args)
         next? (loop [idx 0
                      next? true]
                 (if (< idx n-args)
                   (let [^ISeq c (.get cs (unchecked-int idx))
                         _ (ArrayHelpers/aset args (unchecked-int idx) (.first c))
                         c (.next c)]
                     (.set cs (unchecked-int idx) c)
                     (recur (unchecked-inc idx) (and next? c)))
                   next?))]
     (cons (f (ArrayLists/toList args))
           (when next?
             (lazy-seq (seq-tuple-map f cs)))))))


(defn tuple-map
  "f always receives a single tuple argument.  This is *far* faster for larger argument lists."
  ([f arg] (clojure.core/map #(f [%]) arg))
  ([f c1 c2]
   (let [c1 (seq c1)
         c2 (seq c2)]
     (if (and c1 c2)
       (seq-tuple-map f c1 c2)
       '())))
  ([f c1 c2 & args]
   (let [args (doto  (ArrayLists$ObjectArrayList.)
                (.add (seq c1))
                (.add (seq c2))
                (.addAll (lznc/map seq args)))]
     (if (lznc/every? identity args)
       (seq-tuple-map f args)
       '()))))



(defn filter
  [pred coll]
  (-> (lznc/filter pred coll)
      (seq)))


(defn concat
  ([] nil)
  ([a] a)
  ([a b] (->> (lznc/concat a b)
              (seq)))
  ([a b & args]
   (-> (apply lznc/concat a b args)
       (seq))))


(defn repeatedly
  ([f] (clojure.core/repeatedly f))
  ([n f] (-> (lznc/repeatedly n f)
             (seq))))

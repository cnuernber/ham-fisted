(ns ham-fisted.lockless-caching
  (:require [ham-fisted.print :refer [implement-tostring-print]]
            [ham-fisted.iterator :as hamf-iter]
            [ham-fisted.language :as hamf-lang])
  (:import (java.util.concurrent.atomic AtomicReference)
           (clojure.lang ISeq Seqable)
           [java.util List]
           (java.lang.invoke MethodHandles VarHandle)
           [ham_fisted ITypedReduce Transformables])
  (:refer-clojure :exclude [lazy-seq lazy-cons map filter remove concat]))

(set! *warn-on-reflection* true)

(defn core-lazy-range [^long n]
  (when (pos? n)
    (cons n (clojure.core/lazy-seq (core-lazy-range (dec n))))))

;; Helper constructor macro
(defmacro lazy-cons [head & tail-thunk]
  `(ham_fisted.LockFreeLazyCons. ~head (fn [] ~@tail-thunk)))

(defn cons-lazy-range [^long n]
  (when (pos? n)
    (lazy-cons n (cons-lazy-range (dec n)))))

(implement-tostring-print ham_fisted.LockFreeLazyCons)

(defn iter-range [^long n]
  (-> (hamf-iter/iterable
       #(pos? (aget ^longs % 0))
       #(long-array [n])
       #(do (aset ^longs % 0 (dec (aget ^longs % 0))) %)
       #(aget ^longs % 0))
      (hamf-iter/seq-iterable)))

(deftype LazySeq [thunk]
  clojure.lang.Seqable
  (seq [_m] (thunk))
  Object
  (toString [_m] (Transformables/sequenceToString (.seq _m))))

(defmacro lazy-seq [thunk]
  `(LazySeq. (fn [] ~thunk)))

(implement-tostring-print LazySeq)

(defn- map*
  ([f ^ISeq a]
   (lazy-cons
       (f (.first a))
       (when-let [aa (.next a)]
         (map* f aa))))
  ([f ^ISeq a ^ISeq b]
   (lazy-cons (f (.first a) (.first b))
     (let [a (.next a) b (.next b)]
       (when (and a b)
         (map* f a b)))))
  ([f ^ISeq a ^ISeq b ^ISeq c]
   (lazy-cons (f (.first a) (.first b) (.first c))
     (let [a (.next a) b (.next b) c (.next c)]
       (when (and a b c)
         (map* f a b c))))))

(defn- early-out-nil [f args]
  (let [na (count args)]
    (loop [rv (transient [])
           idx 0]
      (if (== idx na)
        (persistent! rv)
        (when-let [rr (f (.get ^List args idx))]
          (recur (conj! rv rr) (unchecked-inc idx)))))))

(defn- map** [f args]
  (lazy-cons (apply f (map* #(.first ^ISeq %) args))
    (when-let [args (early-out-nil #(.next ^ISeq %) args)]
      (map** f args))))

(defn map
  ([f a]
   (lazy-seq (when-let [a (seq a)]
               (map* f a))))
  ([f a b]
   (lazy-seq (let [a (seq a) b (seq b)]
               (when (and a b)
                 (map* f a b)))))
  ([f a b c]
   (lazy-seq (let [a (seq a) b (seq b) c (seq c)]
               (when (and a b c)
                 (map* f a b c)))))
  ([f a b c & args]
   (lazy-seq
    (when-let [args (early-out-nil seq (into [a b c] args))]
      (map** f args)))))

(defn filter* [pred ^ISeq a]
  (when (pred (.first a))
    (lazy-cons a
      (when-let [a (.next a)]
        (filter* pred a)))))

(defn filter
  [pred a]
  (lazy-seq (when-let [^ISeq a (seq a)]
              (filter* pred a))))

(defn remove [pred a] (filter (hamf-lang/complement pred) a))

(defn- concat*
  ([^ISeq a b]
   (lazy-cons (.first a)
     (if-let [a (.next a)]
       (concat* a b)
       (when-let [b (seq b)] b))))
  ([^ISeq a b c]
   (lazy-cons (.first a)
     (if-let [a (.next a)]
       (concat* a b c)
       (if-let [b (seq b)]
         (concat* b c)
         (when-let [c (seq c)]
           c))))))

(defn- concat** [^ISeq a ^ISeq args]
  (lazy-cons (.first a)
             (if-let [a (.next a)]
               (concat** a args)
               (loop [a (seq (.first args))
                      args (.next args)]
                 (if args
                   (if a
                     (concat** a args)
                     (recur (seq (.first args)) (.next args)))
                   a)))))

(defn concat
  ([a] a)
  ([a b]
   (lazy-seq
    (let [a (seq a)]
      (hamf-lang/cond
        (and a b)
        (concat* a b)
        a a
        :else
        (when-let [b (seq b)]
          b)))))
  ([a b c]
   (lazy-seq
    (let [a (seq a)]
      (if a
        (concat* a b c)
        (if-let [b (seq b)]
          (concat* b c)
          (when-let [c (seq c)]
            c))))))
  ;;args in this case may be infinite
  ([a b c & args]
   (lazy-seq
    (loop [^ISeq args (seq (concat [b c] args))
           a (seq a)]
      (hamf-lang/cond (and a args)
        (concat** a args)
        a a
        :else
        (let [^ISeq args (.next args)]
          (recur args (when args (seq (.first args))))))))))

(comment
  (require '[criterium.core :as crit])
  (require '[clj-memory-meter.core :as mm])
  (require '[ham-fisted.api :as hamf])
  (crit/quick-bench (into [] (core-lazy-range 10000)))
  ;;1.38ms
  (crit/quick-bench (into [] (cons-lazy-range 10000)))
  ;;1.2ms
  (crit/quick-bench (into [] (iter-range 10000)))
  ;;425us
  (crit/quick-bench (into [] (hamf/range 10000)))
  ;;323us

  ;;static size is 240000 bytes (10000 * 24 bytes)
  (mm/measure (let [s (core-lazy-range 10000)]
                (into [] s)
                s))
  ;;859KB
  
  (mm/measure (let [s (cons-lazy-range 10000)]
                (into [] s)
                s))
  ;;468KB
  
  (require '[ham-fisted.bean-alloc :as ba])

  
  (ba/measure (dotimes [idx 100] (into [] (core-lazy-range 10000))))
  ;;{:value nil, :bytes 165154024}
  (ba/measure (dotimes [idx 100] (into [] (cons-lazy-range 10000))))
  ;;{:value nil, :bytes 77156424}
  (ba/measure (dotimes [idx 100] (into [] (iter-range 10000))))
  ;;{:value nil, :bytes 29183624}
  (ba/measure (dotimes [idx 100] (into [] (hamf/range 10000))))
  ;;{:value nil, :bytes 29157304}
  )

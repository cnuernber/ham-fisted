(ns ham-fisted.lockless-caching
  (:require [ham-fisted.print :refer [implement-tostring-print]])
  (:import (java.util.concurrent.atomic AtomicReference)
           (clojure.lang ISeq Seqable IPersistentCollection Counted)
           [ham_fisted ITypedReduce])
  (:refer-clojure :exclude [lazy-seq]))

(set! *warn-on-reflection* true)

;; Marker representation for state tracking
(deftype Pending [thunk])
(deftype Evaluating [owner-thread])
(deftype Realized [val tail])

(definterface IRealizable (realize []))

(deftype LockFreeLazySeq [^AtomicReference state]
  ISeq
  (first [this]
    (.realize this)
    (let [^Realized r (.get state)]
      (.-val r)))

  (next [this]
    (.more this))

  (more [this]
    (.realize this)
    (let [^Realized r (.get state)
          tail (.-tail r)]
      (if (nil? tail)
        nil
        (clojure.lang.RT/seq tail))))

  (cons [this o]
    (clojure.lang.Cons. o this))

  (empty [this]
    clojure.lang.PersistentList/EMPTY)

  (equiv [this o]
    (if (instance? ISeq o)
      (loop [s1 this
             s2 (seq o)]
        (cond
          (and (nil? s1) (nil? s2)) true
          (or (nil? s1) (nil? s2)) false
          (= (first s1) (first s2)) (recur (next s1) (next s2))
          :else false))
      false))

  Seqable
  (seq [this]
    (.realize this)
    (let [^Realized r (.get state)]
      (if (and (nil? (.-val r)) (nil? (.-tail r)))
        nil
        this)))

  Object
  (toString [this]
    (let [st (.get state)]
      (if (instance? Realized st)
        (str "(" (.-val ^Realized st) " ...) ")
        "(...)")))

  ITypedReduce
  (reduce [this rfn acc]
    (loop [^LockFreeLazySeq l this
           acc acc]
      (.realize l)
      (let [^Realized r (.get ^AtomicReference (.-state l))]
        (if (and r (or (.-val r) (.-tail r)))
          (let [acc (rfn acc (.-val r))]
            (if (reduced? acc)
              acc
              (let [ll (.-tail r)]
                (if (instance? LockFreeLazySeq ll)
                  (recur ll acc)
                  (reduce rfn acc ll)))))
          acc))))

  IRealizable
  ;; Internal realization logic
  (realize [this]
    (loop []
      (let [current (.get state)]
        (cond
         ;; Already realized -> done
          (instance? Realized current)
          nil

         ;; Pending -> Try to claim evaluation rights via CAS
          (instance? Pending current)
          (let [evaluating-state (Evaluating. (Thread/currentThread))]
            (if (.compareAndSet state current evaluating-state)
             ;; Evaluation claimed successfully
              (try
                (let [thunk (.-thunk ^Pending current)
                      res (thunk)
                      ^ISeq s (clojure.lang.RT/seq res)
                      realized-state (if (nil? s)
                                       (Realized. nil nil)
                                       (Realized. (.first s) (rest s)))]
                  (.set state realized-state))
                (catch Throwable e
                 ;; Reset to original pending state on failure so another thread can retry
                  (.set state current)
                  (throw e)))
             ;; CAS failed, another thread touched state; retry loop
              (recur)))

         ;; Evaluating -> Spin wait / yield until evaluating thread finishes
          (instance? Evaluating current)
          (do
            (Thread/yield)
            (recur)))))))

(implement-tostring-print LockFreeLazySeq)

;; Macro helper mirroring `lazy-seq`
(defmacro lazy-seq [& body]
  `(LockFreeLazySeq. (AtomicReference. (Pending. (fn [] ~@body)))))

(defn lazy-range [^long n]
  (when (pos? n)
    (cons n (lazy-seq (lazy-range (dec n))))))

(defn core-lazy-range [^long n]
  (when (pos? n)
    (cons n (clojure.core/lazy-seq (core-lazy-range (dec n))))))

(comment

  
  (println )
  )

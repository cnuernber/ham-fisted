(ns ham-fisted.hlet
  "Extensible let to allow efficient typed destructuring.

  Registered Extensions:

  `dbls` and `lngs` will most efficiently destructure java primitive arrays and fall back to casting the result
  of clojure.lang.RT/nth if input is not a double or long array.

  `dlb-fns` and `lng-fns` call the object's IFn interface with no interface checking.  This will *not* work
   with a raw array but is the fastest way - faster than RT/nth - to get data out of a persistent-vector or map
   like object.

  `obj-fns` - fast IFn-based destructuring to objects - does not work with object arrays.  Often much faster
   than RT/nth.

  This can significantly reduce boxing in tight loops without needing to result in really verbose pathways.

```clojure
user> (h/let [[a b] (dbls [1 2])] (+ a b))
  3.0
user> (hamf/sum-fast (lznc/cartesian-map
                      #(h/let [[a b c d](lng-fns %)]
                         (-> (+ a b) (+ c) (+ d)))
                      [1 2 3]
                      [4 5 6]
                      [7 8 9]
                      [10 11 12 13 14]))
3645.0
```
  See also [[ham-fisted.primitive-invoke]], [[ham-fisted.api/dnth]] [[ham-fisted.api/lnth]]."
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.reduce :as hamf-rf])
  (:import [java.util List]
           [ham_fisted Casts])
  (:refer-clojure :exclude [let]))


(def ^{:private true
       :tag java.util.Map} extension-table (hamf/java-concurrent-hashmap))


(defn extend-let
  "Code gets a tuple of [lhs rhs] must return
  a flattened sequence of left and right hand sides.
  This uses a special symbol that will look like a function call on the
  right hand side as the dispatch mechanism.

  See source code of this file for example extensions."
  [sym-name code]
  (.put extension-table sym-name code))


(defn- rhs-code
  [r]
  (when (list? r)
    (clojure.core/let [s (first r)]
      (when (symbol? s)
        (.get extension-table s)))))


(defn- add-all!
  [a bs]
  (.addAll ^List a bs)
  a)

(defn- add!
  ([a b]
   (.add ^List a b)
   a)
  ([a b c]
   (.add ^List a b)
   (.add ^List a c)
   a))


(defn- process-bindings
  [bindings]
  (->
   (reduce (fn [acc pair]
             (if-let [code (rhs-code (pair 1))]
               (clojure.core/let [new-pairs (code pair)]
                 (when-not (== 0 (rem (count new-pairs) 2))
                   (throw (RuntimeException. (str "Code for symbol " (first (pair 1)) " returned uneven number of results"))))
                 (add-all! acc new-pairs))
               (add-all! acc pair)))
           (hamf/mut-list)
           (lznc/partition-all 2 (vec bindings)))
   (persistent!)))


(defmacro let
  "Extensible let intended to allow typed destructuring of arbitrary datatypes such as primitive arrays
  or point types.  Falls back to normal let after extension process.  Several extensions are registered by default -
  * `dbls` and `lngs` which destructure into primitive doubles and primitive longs, respectively.
  * `dlb-fns` and `lng-fns` which destructure into primitive doubls and longs but use the often faster IFn overloads
     to get the data - avoiding RT.nth calls.
  * `obj-fns` which destructure into objects using the IFn interface.

```clojure
user> (h/let [[x y] (dbls (hamf/double-array [1 2]))]
        (+ x y))
3.0
```
  "
  [bindings & body]
  (when-not (== 0 (rem (count bindings) 2))
    (throw (RuntimeException. "Bindings must be divisible by 2")))
  `(clojure.core/let ~(process-bindings bindings)
     ~@body))

(defn ^:no-doc typed-destructure
  [code scalar-fn nth-fn]
  (clojure.core/let [lvec (code 0)
                     rdata (second (code 1))]
    (if (vector? lvec)
      (let [rtemp (if (symbol? rdata)
                    rdata
                    (gensym "__tmp"))]
        (-> (reduce (hamf-rf/indexed-accum
                     acc idx lv-entry
                     (nth-fn rtemp acc idx lv-entry))
                    (hamf/mut-list (if (identical? rtemp rdata)
                                     nil
                                     [rtemp rdata]))
                    lvec)
            (persistent!)))
      (scalar-fn lvec rdata))))


(defn ^:no-doc typed-nth-destructure
  [nth-symbol scalar-cast code]
  (typed-destructure code
                     (fn [lvec rdata]
                       [lvec `(~scalar-cast ~rdata)])
                     (fn [rtemp acc ^long idx lv-entry]
                       (add! acc lv-entry `(~nth-symbol ~rtemp ~idx)))))

(extend-let 'dbls #(typed-nth-destructure 'ham-fisted.api/dnth 'ham_fisted.Casts/doubleCast %))
(extend-let 'lngs #(typed-nth-destructure 'ham-fisted.api/lnth 'ham_fisted.Casts/longCast %))


(defn ^:no-doc typed-fn-destructure
  [scalar-cast code]
  (typed-destructure code
                     (fn [lvec rdata]
                       [lvec `(~scalar-cast ~rdata)])
                     (fn [rtemp acc ^long idx lv-entry]
                       (add! acc lv-entry `(~scalar-cast (~rtemp ~idx))))))


(extend-let 'lng-fns #(typed-fn-destructure 'ham_fisted.Casts/longCast %))
(extend-let 'dbl-fns #(typed-fn-destructure 'ham_fisted.Casts/doubleCast %))
(extend-let 'obj-fns #(typed-destructure %
                                         (fn [lvec rdata]
                                           [lvec `~rdata])
                                         (fn [rtemp acc ^long idx lv-entry]
                                           (add! acc lv-entry `(~rtemp ~idx)))))


(defn let-extension-names
  "Return the current extension names."
  []
  (keys extension-table))

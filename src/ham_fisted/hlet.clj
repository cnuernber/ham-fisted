(ns ham-fisted.hlet
  "Extensible let to allow efficient typed destructuring.  Two extensions are registered - dlbs and lngs which
  do an efficient typed nth operation resulting in primitive longs and doubles respectively.

  dlbs and lngs will most efficiently destructure java primitive arrays and fall back to casting the result
  of clojure.lang.RT/nth if input is not a double or long array.

  This can significantly reduce boxing in tight loops without needing to result in really verbose pathways.

```clojure
user> (hlet [[a b] (dbls [1 2])] (+ a b))
3.0
```
  See also [[ham-fisted.primitive-invoke]], [[ham-fisted.api/dnth]] [[ham-fisted.api/lnth]]."
  (:require [ham-fisted.api :as hamf]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.reduce :as hamf-rf])
  (:import [java.util List]))


(def ^{:private true
       :tag java.util.Map} extension-table (hamf/java-concurrent-hashmap))


(defn extend-hlet
  "Code must take two arguments, left and right hand sides and return
  a flattened sequence of left and right hand sides.
  This uses a special symbol that will look like a function call on the
  right hand side.

  See source code of this file for example extensions."
  [sym-name code]
  (.put extension-table sym-name code))


(defn- rhs-code
  [r]
  (when (list? r)
    (let [s (first r)]
      (when (symbol? s)
        (.get extension-table s)))))


(defn- add-all!
  [a bs]
  (.addAll ^List a bs)
  a)


(defn- process-bindings
  [bindings]
  (->
   (reduce (fn [acc pair]
             (if-let [code (rhs-code (pair 1))]
               (let [new-pairs (code pair)]
                 (when-not (== 0 (rem (count new-pairs) 2))
                   (throw (RuntimeException. (str "Code for symbol " (first (pair 1)) " returned uneven number of results"))))
                 (add-all!  acc new-pairs))
               (add-all! acc pair)))
           (hamf/mut-list)
           (lznc/partition-all 2 (vec bindings)))
   (persistent!)))


(defmacro hlet
  "Extensible let intended to allow typed destructuring of arbitrary datatypes such as primitive vectors
  or point types.  Falls back to normal let after extension process."
  [bindings & body]
  (when-not (== 0 (rem (count bindings) 2))
    (throw (RuntimeException. "Bindings must be divisible by 2")))
  `(let ~(process-bindings bindings)
     ~@body))


(defn ^:no-doc typed-nth-destructure
  [nth-symbol code]
  (let [lvec (code 0)
        rdata (second (code 1))]
    (if (vector? lvec)
      (let [rtemp (if (symbol? rdata)
                    rdata
                    (gensym "__dbls"))]
        (-> (reduce (hamf-rf/indexed-accum
                     acc idx lv-entry
                     (add-all! acc [lv-entry `(~nth-symbol ~rtemp ~idx)]))
                    (hamf/mut-list (if (identical? rtemp rdata)
                                     nil
                                     [rtemp rdata]))
                    lvec)
            (persistent!)))
      [lvec '(double rdata)])))


(extend-hlet
 'dbls
 #(typed-nth-destructure 'ham-fisted.api/dnth %))

(extend-hlet
 'lngs
 #(typed-nth-destructure 'ham-fisted.api/lnth %))


(defn hlet-extension-names
  "Return the current extension names."
  []
  (keys extension-table))

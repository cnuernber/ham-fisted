(ns ham-fisted.language
  (:import [ham_fisted Transformables]
           [ham_fisted ObjArray])
  (:refer-clojure :exclude [cond constantly not]))

(defmacro cond
  "See documentation for [[ham-fisted.api/cond]]"
  [& clauses]
  (let [clauses (vec clauses)
        nc (count clauses)
        constant-clause? #(or (identical? % true)
                              (keyword? %))
        odd-clauses? (odd? nc)
        else? (or odd-clauses? (and (> nc 2)
                                    (constant-clause? (nth clauses (- nc 2)))))]
    (if-not else?
      `(clojure.core/cond ~@clauses)
      (let [[clauses else-branch] (if odd-clauses?
                                    [(subvec clauses 0 (dec nc)) (last clauses)]
                                    [(subvec clauses 0 (- nc 2)) (last clauses)])
            pred-true-branch (reverse (partition 2 clauses))]
        (reduce (fn [stmts [pred true-branch]]
                  `(if ~pred ~true-branch ~stmts))
                else-branch
                pred-true-branch)))))

(def array-classes {:byte (.getClass ^Object (clojure.core/byte-array 0))
                    :short (.getClass ^Object (clojure.core/short-array 0))
                    :char (.getClass ^Object (clojure.core/char-array 0))
                    :int (.getClass ^Object (clojure.core/int-array 0))
                    :long (.getClass ^Object (clojure.core/long-array 0))
                    :float (.getClass ^Object (clojure.core/float-array 0))
                    :double (.getClass ^Object (clojure.core/double-array 0))
                    :boolean (.getClass ^Object (clojure.core/boolean-array 0))
                    :object (.getClass ^Object (clojure.core/object-array 0))})

(defn constantly
  [x]
  (fn constantly-fn
    ([] x)
    ([a] x)
    ([a b] x)
    ([a b c] x)
    ([a b c d] x)
    ([a b c d e] x)
    ([a b c d e & args] x)))

(defn not
  "Returns boolean opposite of passed in value"
  {:inline (fn [x] `(Transformables/not ~x))
   :inline-arities #{1}}
  [a]
  (Transformables/not a))

(def ^:private empty-objs (clojure.core/object-array 0))


(defn obj-ary
  "As quickly as possible, produce an object array from these inputs.  Very fast for arities
  <= 16."
  (^objects [] empty-objs)
  (^objects [v0] (ObjArray/create v0))
  (^objects [v0 v1] (ObjArray/create v0 v1))
  (^objects [v0 v1 v2] (ObjArray/create v0 v1 v2))
  (^objects [v0 v1 v2 v3] (ObjArray/create v0 v1 v2 v3))
  (^objects [v0 v1 v2 v3 v4] (ObjArray/create v0 v1 v2 v3 v4))
  (^objects [v0 v1 v2 v3 v4 v5] (ObjArray/create v0 v1 v2 v3 v4 v5))
  (^objects [v0 v1 v2 v3 v4 v5 v6] (ObjArray/create v0 v1 v2 v3 v4 v5 v6))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9) v10)
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12 v13] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12 v13))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12 v13 v14] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12 v13 v14))
  (^objects [v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v10 v11 v12 v13 v14 v15] (ObjArray/create v0 v1 v2 v3 v4 v5 v6 v7 v8 v9 v0 v11 v12 v13 v14 v15)))

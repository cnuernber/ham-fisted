(ns ham-fisted.language
  (:refer-clojure :exclude [cond]))

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
                    :object (.getClass ^Object (clojure.core/object-array 0))})

(ns ham-fisted.function
  "Helpers for working with [java.util.function](https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html)
  package objects."
  (:import [ham_fisted IFnDef$ODO IFnDef$OOO IFnDef$OO IFnDef$OL IFnDef$LongPredicate
            IFnDef$DoublePredicate IFnDef$DD IFnDef$LL IFnDef IFnDef$LD IFnDef$DL IFnDef$OD
            IFnDef$LO Casts IFnDef$Predicate IFnDef$LLL IFnDef$DDD]
           [java.util.function BiFunction BiConsumer Function DoublePredicate LongPredicate Predicate
            Consumer LongConsumer DoubleConsumer LongBinaryOperator DoubleBinaryOperator
            BiPredicate]
           [java.util Comparator]
           [clojure.lang Util]
           [it.unimi.dsi.fastutil.longs LongComparator]
           [it.unimi.dsi.fastutil.doubles DoubleComparator]))


(defmacro bi-function
  "Create an implementation of java.util.function.BiFunction."
  [arg1 arg2 & code]
  `(reify IFnDef$OOO (invoke [this# ~arg1 ~arg2] ~@code)))


(defmacro bi-consumer
  [arg1 arg2 & code]
  `(reify BiConsumer
     (accept [this# ~arg1 ~arg2]
       ~@code)))


(defn ->bi-function
  "Convert an object to a java.util.BiFunction. Object can either already be a
  bi-function or an IFn to be invoked with 2 arguments."
  ^BiFunction [cljfn]
  (if (instance? BiFunction cljfn)
    cljfn
    (bi-function a b (cljfn a b))))



(defmacro function
  "Create a java.util.function.Function"
  [arg & code]
  `(reify IFnDef$OO (invoke [this# ~arg] ~@code)))


(defmacro obj->long
  "Create a function that converts objects to longs"
  ([]
   `(reify IFnDef$OL
        (invokePrim [this# v#]
          (Casts/longCast v#))))
  ([varname & code]
   `(reify IFnDef$OL
      (invokePrim [this ~varname]
        (Casts/longCast ~@code)))))


(defmacro obj->double
  "Create a function that converts objects to doubles"
  ([]
   `(reify IFnDef$OD
      (invokePrim [this# v#]
        (Casts/doubleCast v#))))
  ([varname & code]
   `(reify IFnDef$OD
      (invokePrim [this# ~varname]
        (Casts/doubleCast ~@code)))))


(defmacro long->double
  "Create a function that receives a long and returns a double"
  [varname & code]
  `(reify IFnDef$LD
     (invokePrim [this# ~varname] ~@code)))


(defmacro double->long
  "Create a function that receives a double and returns a long"
  [varname & code]
  `(reify IFnDef$DL
     (invokePrim [this# ~varname] ~@code)))


(defmacro long->obj
  "Create a function that receives a primitive long and returns an object."
  [varname & code]
  `(reify IFnDef$LO
     (invokePrim [this# ~varname] ~@code)))


(defmacro double->obj
  [varname & code]
  `(reify IFnDef$DO
     (invokePrim [this# ~varname] ~@code)))


(defn ->function
  "Convert an object to a java Function. Object can either already be a
  Function or an IFn to be invoked."
  ^Function [cljfn]
  (if (instance? Function cljfn)
    cljfn
    (reify IFnDef$OO (invoke [this a] (cljfn a)))))


(defmacro double-predicate
  "Create an implementation of java.util.Function.DoublePredicate"
  [varname & code]
  `(reify
     IFnDef$DoublePredicate
     (test [this# ~varname]
       ~@code)))


(defmacro double-unary-operator
  "Create an implementation of java.util.function.DoubleUnaryOperator"
  [varname & code]
  `(reify
     IFnDef$DD
     (invokePrim [this# ~varname]
       ~@code)))


(defmacro long-predicate
  "Create an implementation of java.util.Function.LongPredicate"
  [varname & code]
  `(reify
     IFnDef$LongPredicate
     (test [this# ~varname]
       ~@code)))


(defn ->long-predicate
  ^LongPredicate [f]
  (if (instance? LongPredicate f)
    f
    (long-predicate ll (boolean (f ll)))))


(defmacro long-unary-operator
  "Create an implementation of java.util.function.LongUnaryOperator"
  [varname & code]
  `(reify
     IFnDef$LL
     (invokePrim [this# ~varname]
       ~@code)))


(defmacro predicate
  "Create an implementation of java.util.Function.Predicate"
  [varname & code]
  `(reify
     IFnDef$Predicate
     (test [this# ~varname]
       ~@code)))


(defmacro unary-operator
  "Create an implementation of java.util.function.UnaryOperator"
  [varname & code]
  `(function ~varname ~@code))


(defmacro binary-operator
  "Create an implementation of java.util.function.BinaryOperator"
  [arg1 arg2 & code]
  `(bi-function ~arg1 ~arg2 ~@code))


(defmacro double-consumer
  "Create an instance of a java.util.function.DoubleConsumer"
  [varname & code]
  `(reify
     DoubleConsumer
     (accept [this# ~varname]
       ~@code)
     IFnDef$DO
     (invokePrim [this# v#] (.accept this# v#))))


(defmacro long-consumer
  "Create an instance of a java.util.function.LongConsumer"
  [varname & code]
  `(reify
     LongConsumer
     (accept [this# ~varname]
       ~@code)
     IFnDef$LO
     (invokePrim [this# v#] (.accept this# v#))))


(defmacro consumer
  "Create an instance of a java.util.function.Consumer"
  [varname & code]
  `(reify Consumer
     (accept [this# ~varname]
       ~@code)
     IFnDef$OO
     (invoke [this# arg#] (.accept this# arg#))))


(defmacro make-long-comparator
  "Make a comparator that gets passed two long arguments."
  [lhsvar rhsvar & code]
  (let [lhsvar (with-meta lhsvar {:tag 'long})
        rhsvar (with-meta rhsvar {:tag 'long})
        compsym (with-meta 'compare {:tag 'int})]
    `(reify
       LongComparator
       (~compsym [this# ~lhsvar ~rhsvar]
        ~@code)
       IFnDef
       (invoke [this# l# r#]
         (.compare this# l# r#)))))


(defmacro make-double-comparator
  "Make a comparator that gets passed two double arguments."
  [lhsvar rhsvar & code]
  (let [lhsvar (with-meta lhsvar {:tag 'double})
        rhsvar (with-meta rhsvar {:tag 'double})
        compsym (with-meta 'compare {:tag 'int})]
    `(reify
       DoubleComparator
       (~compsym [this# ~lhsvar ~rhsvar]
        ~@code)
       IFnDef
       (invoke [this# l# r#]
         (.compare this# l# r#)))))


(defmacro make-comparator
  "Make a java comparator."
  [lhsvar rhsvar & code]
  `(reify
     Comparator
     (compare [this# ~lhsvar ~rhsvar]
      ~@code)
     IFnDef
     (invoke [this# l# r#]
       (.compare this# l# r#))))




(def ^{:doc "A reverse comparator that sorts in descending order" }
  rcomp
  (reify
    Comparator
    (^int compare [this ^Object l ^Object r]
      (Util/compare r l))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (Double/compare r l))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare r l))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(def ^{:doc "A comparator that sorts null, NAN first, natural order"}
  comp-nan-first
  (reify
    Comparator
    (^int compare [this ^Object l ^Object r]
     (cond
       (nil? l) -1
       (nil? r) 1
       :else (Util/compare l r)))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (cond
       (Double/isNaN l) -1
       (Double/isNaN r) 1
       :else
       (Double/compare l r)))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare l r))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(def ^{:doc "A comparator that sorts null, NAN last, natural order"}
  comp-nan-last
  (reify
    Comparator
    (^int compare [this ^Object l ^Object r]
     (cond
       (nil? l) 1
       (nil? r) -1
       :else (clojure.lang.Util/compare l r)))
    DoubleComparator
    (^int compare [this ^double l ^double r]
     (cond
       (Double/isNaN l) 1
       (Double/isNaN r) -1
       :else
       (Double/compare l r)))
    LongComparator
    (^int compare [this ^long l ^long r]
     (Long/compare l r))
    IFnDef
    (invoke [this l r]
      (.compare this l r))))


(defmacro double-binary-operator
  "Create a binary operator that is specialized for double values.  Useful to speed up
  operations such as sorting or summation."
  [lvar rvar & code]
  `(reify
     DoubleBinaryOperator
     (applyAsDouble [this# ~lvar ~rvar]
       ~@code)
     IFnDef$DDD
     (invokePrim [this# l# r#]
       (.applyAsDouble this# l# r#))))


(defmacro long-binary-operator
  "Create a binary operator that is specialized for long values.  Useful to speed up
  operations such as sorting or summation."
  [lvar rvar & code]
  `(reify
     LongBinaryOperator
     (applyAsLong [this# ~lvar ~rvar]
       ~@code)
     IFnDef$LLL
     (invokePrim [this# l# r#]
       (.applyAsLong this# l# r#))))


(defmacro binary-predicate
  "Create a java.util.function.BiPredicate"
  [xvar yvar code]
  `(reify
     BiPredicate
     (test [this# ~xvar ~yvar] (boolean ~code))
     IFnDef
     (invoke [this# ~xvar ~yvar] (boolean ~code))))


(defn binary-predicate-or-null
  "If f is null, return null.  Else return f as a java.util.function.BiPredicate."
  ^BiPredicate [f]
  (when f
    (if (instance? BiPredicate f)
      f
      (binary-predicate x y (f x y)))))

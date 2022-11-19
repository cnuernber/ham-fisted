(ns ham-fisted.protocols
  (:import [clojure.lang IFn]
           [java.util.function DoubleConsumer]
           [ham_fisted Sum Sum$SimpleSum Reducible IFnDef$ODO ParallelOptions
            Reductions])
  (:refer-clojure :exclude [reduce]))


(defprotocol ToIterable
  (convertible-to-iterable? [item])
  (->iterable [item]))


(defprotocol ToCollection
  (convertible-to-collection? [item])
  (->collection [item]))


(defprotocol Reduction
  "Reduce a collection to an accumulator."
  (reducible? [coll])
  (reduce [coll rfn acc]))


(defprotocol ParallelReduction
  "Protocol to define a parallel reduction in a collection-specific pathway.  Specializations
  are in impl as that is where the parallelization routines are found."
  (preduce [coll init-val-fn rfn merge-fn ^ParallelOptions options]
    "Container-specific parallelized reduction.  Reductions must respect the pool passed in via
the options."))


(defprotocol Reducer
  "Reducer is the basic reduction abstraction as a single object."
    (->init-val-fn [item]
    "Returns the initial values for a parallel reduction.  This function
takes no arguments and returns the initial accumulator.")
    (->rfn [item]
      "Returns the reduction function for a parallel reduction. This function takes
two arguments, the accumulator and a value from the collection and returns a new
or modified accumulator.")
    (finalize [item v]
    "A finalize function called on the result of the reduction after it is
reduced but before it is returned to the user.  Returning v is a reasonable default."))


(defprotocol ParallelReducer
  "Parallel reducers are simple a single object that you can pass into preduce as
  opposed to 3 separate functions."
  (->merge-fn [item]
    "Returns the merge function for a parallel reduction.  This function takes
two accumulators  and returns a or modified accumulator."))


(def ^:no-doc double-consumer-accumulator
  (reify IFnDef$ODO
    (invokePrim [f acc v]
      (.accept ^DoubleConsumer acc v)
      acc)))

(defn- reducible-merge
  [^Reducible lhs rhs]
  (.reduce lhs rhs))


(extend-protocol Reducer
  IFn
  (->init-val-fn [this] this)
  (->rfn [this] this)
  (finalize [this v] (this v)) ;;single-arity overload
  Sum
  (->init-val-fn [s] #(Sum.))
  (->rfn [s] double-consumer-accumulator)
  (finalize [s v] (deref v))
  Sum$SimpleSum
  (->init-val-fn [s] #(Sum$SimpleSum.))
  (->rfn [s] double-consumer-accumulator)
  (finalize [s v] (deref v)))


(extend-protocol ParallelReducer
  IFn
  (->merge-fn [this] this)
  Sum
  (->merge-fn [s] reducible-merge)
  Sum$SimpleSum
  (->merge-fn [s] reducible-merge))


(defprotocol SetOps
  "Simple protocol for set operations to make them uniformly extensible to new objects."
  (union [l r])
  (difference [l r])
  (intersection [l r])
  (xor [l r]))

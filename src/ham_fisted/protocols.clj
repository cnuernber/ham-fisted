(ns ham-fisted.protocols
  (:import [clojure.lang IFn]
           [java.util.function DoubleConsumer]
           [ham_fisted Sum Sum$SimpleSum Reducible IFnDef$ODO]))




(defprotocol ParallelReducer
  "Parallel reducers are simple a single object that you can pass into preduce as
  opposed to 3 separate functions."
  (->init-val-fn [item]
    "Returns the initial values for a parallel reduction.  This function
takes no arguments and returns the initial reduction value.")
  (->rfn [item]
    "Returns the reduction function for a parallel reduction. This function takes
two arguments, the initial value and a value from the collection and returns a new
initial value.")
  (->merge-fn [item]
    "Returns the merge function for a parallel reduction.  This function takes
two initial values and returns a new initial value."))


(def ^:no-doc double-consumer-accumulator
  (reify IFnDef$ODO
    (invokePrim [f acc v]
      (.accept ^DoubleConsumer acc v)
      acc)))

(defn- reducible-merge
  [^Reducible lhs rhs]
  (.reduce lhs rhs))


(extend-protocol ParallelReducer
  IFn
  (->init-val-fn [this] this)
  (->rfn [this] this)
  (->merge-fn [this] this)
  Sum
  (->init-val-fn [s] #(Sum.))
  (->rfn [s] double-consumer-accumulator)
  (->merge-fn [s] reducible-merge)
  Sum$SimpleSum
  (->init-val-fn [s] #(Sum$SimpleSum.))
  (->rfn [s] double-consumer-accumulator)
  (->merge-fn [s] reducible-merge))

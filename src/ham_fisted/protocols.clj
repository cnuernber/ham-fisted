(ns ham-fisted.protocols
  (:require [clojure.core.protocols :as cl-proto])
  (:import [clojure.lang IFn IReduceInit IDeref]
           [java.util.function DoubleConsumer]
           [java.util Map]
           [ham_fisted Sum Sum$SimpleSum Reducible IFnDef$ODO ParallelOptions
            Reductions IMutList])
  (:refer-clojure :exclude [reduce set?]))


(defprotocol ToIterable
  (convertible-to-iterable? [item])
  (->iterable [item]))


(defprotocol ToCollection
  (convertible-to-collection? [item])
  (->collection [item]))


(defprotocol Reduction
  "Faster check than satisfies? to see if something is reducible"
  (reducible? [coll]))


(extend-protocol Reduction
  nil
  (reducible? [this] true)
  Object
  (reducible? [this]
    (or (instance? IReduceInit this)
        (instance? Iterable this)
        (instance? Map this)
        ;;This check is dog slow
        (satisfies? cl-proto/CollReduce this))))


(defprotocol ParallelReduction
  "Protocol to define a parallel reduction in a collection-specific pathway.  Specializations
  are in impl as that is where the parallelization routines are found."
  (preduce [coll init-val-fn rfn merge-fn ^ParallelOptions options]
    "Container-specific parallelized reduction.  Reductions must respect the pool passed in via
the options."))


(defprotocol Finalize
  "Generic protocol for things that finalize results of reductions.  Defaults to deref of
  instance of IDeref or identity."
  (finalize [this val]))


(extend-protocol Finalize
  Object
  (finalize [this val]
    (if (instance? IDeref val)
      (.deref ^IDeref val)
      val))
  ;;clojure rfn equivalence
  IFn
  (finalize [this val]
    (this val)))


(defprotocol Reducer
  "Reducer is the basic reduction abstraction as a single object."
    (->init-val-fn [item]
    "Returns the initial values for a parallel reduction.  This function
takes no arguments and returns the initial accumulator.")
    (->rfn [item]
      "Returns the reduction function for a parallel reduction. This function takes
two arguments, the accumulator and a value from the collection and returns a new
or modified accumulator."))


(extend-protocol Reducer
  IFn
  (->init-val-fn [this] this)
  (->rfn [this] this))


(defprotocol ParallelReducer
  "Parallel reducers are simple a single object that you can pass into preduce as
  opposed to 3 separate functions."
  (->merge-fn [item]
    "Returns the merge function for a parallel reduction.  This function takes
two accumulators  and returns a or modified accumulator."))


(extend-protocol ParallelReducer
  IFn
  (->merge-fn [this] this))


(extend-protocol ParallelReducer
  Object
  (->merge-fn [this]
    (fn [_l _r] (throw (RuntimeException. (str "Object does not implement merge: "
                                               (type this)))))))


(def ^:no-doc double-consumer-accumulator
  (reify IFnDef$ODO
    (invokePrim [f acc v]
      (.accept ^DoubleConsumer acc v)
      acc)))

(defn- reducible-merge
  [^Reducible lhs rhs]
  (.reduce lhs rhs))


(defprotocol PAdd
  "Define a function to mutably add items to a collection.  This function must return
  the collection -- it must be useable in a reduction."
  (add-fn [l]))


(defprotocol SetOps
  "Simple protocol for set operations to make them uniformly extensible to new objects."
  (set? [l])
  (union [l r])
  (difference [l r])
  (intersection [l r])
  (xor [l r])
  (contains-fn [item]
    "Return an efficient function for deciding if this set contains a single item.")
  (^long cardinality [item]
   "Some sets don't work with clojure's count function."))


(defprotocol BulkSetOps
  (reduce-union [l data])
  (reduce-intersection [l data]))


(defprotocol BitSet
  "Protocol for efficiently dealing with bitsets"
  (bitset? [item])
  (contains-range? [item sidx eidx])
  (intersects-range? [item sidx eidx])
  (min-set-value [item])
  (max-set-value [item]))


(defprotocol WrapArray
  (^IMutList wrap-array [ary])
  (^IMutList wrap-array-growable [ary ptr]))


(defprotocol SerializeObjBytes
  (serialize->bytes [o]))

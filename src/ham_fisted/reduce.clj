(ns ham-fisted.reduce
  "Protocol-based parallel reduction architecture and helper functions."
  (:require [ham-fisted.protocols :as protocols]
            [ham-fisted.defprotocol :refer [extend extend-type extend-protocol]]
            [ham-fisted.lazy-noncaching :refer [map] :as lznc]
            [ham-fisted.function :refer [bi-function]])
  (:import [ham_fisted ParallelOptions ParallelOptions$CatParallelism Reductions
            Transformables Reducible IFnDef$OOO IFnDef$OLOO
            IFnDef$ODO IFnDef$OLO IFnDef$DDD IFnDef$LLL Sum Sum$SimpleSum Reductions$IndexedAccum
            Reductions$IndexedLongAccum Reductions$IndexedDoubleAccum IFnDef$OLLO IFnDef$OLDO]
           [clojure.lang IFn$DO IFn$LO IFn$OLO IFn$DDD IFn$LLL]
           [java.util Map]
           [java.util.function DoubleConsumer LongConsumer Consumer]
           [java.util.concurrent Executor ForkJoinPool])
  (:refer-clojure :exclude [map extend extend-type extend-protocol]))

(set! *warn-on-reflection* true)


(defn- unpack-reduced
  [item]
  (if (reduced? item)
    (deref item)
    item))


(defn options->parallel-options
  "Convert an options map to a parallel options object.

  Options:

  * `:pool` - supply the forkjoinpool to use.
  * `:max-batch-size` - Defaults to 64000, used for index-based  parallel pathways to control
     the number size of each parallelized batch.
  * `:ordered?` - When true process inputs and provide results in order.
  * `:parallelism` - The amount of parallelism to expect.  Defaults to the number of threads
     in the fork-join pool provided.
  * `:cat-parallelism` - Either `:seq-wise` or `:elem-wise` - when parallelizing over a
     concatenation of containers either parallelize each container meaning call preduce on each
     container using many threads per container or use one thread per container - `seq-wise`.  Defaults
     to `seq-wise` as this doesn't require each container itself to support parallelization but relies on
     the sequence of containers to be long enough to saturate the processor.  Can also be set at time of
     container construction - see [[lazy-noncaching/concat-opts]].
  * `:put-timeout-ms` - The time to wait to put data into the queue.  This is a safety mechanism
    so that if the processing system fails we don't just keep putting things into a queue.
  * `:unmerged-result?` - Use with care.  For parallel reductions do not perform the merge step
    but instead return the sequence of partially reduced results.
  * `:n-lookahead` - How for to look ahead for pmap and upmap to add new jobs to the queue.  Defaults
    to `(* 2 parallelism)."
  ^ParallelOptions [options]
  (cond
    (instance? ParallelOptions options)
    options
    (nil? options)
    (ParallelOptions.)
    :else
    (let [^Map options (or options {})
          ^Executor pool (.getOrDefault options :pool (ForkJoinPool/commonPool))]
      (ParallelOptions. (.getOrDefault options :min-n 1000)
                        (.getOrDefault options :max-batch-size 64000)
                        (boolean (.getOrDefault options :ordered? true))
                        pool
                        (.getOrDefault options :parallelism
                                       (if (instance? ForkJoinPool pool)
                                         (.getParallelism ^ForkJoinPool pool)
                                         1))
                        (case (.getOrDefault options :cat-parallelism :seq-wise)
                          :seq-wise ParallelOptions$CatParallelism/SEQWISE
                          :elem-wise ParallelOptions$CatParallelism/ELEMWISE)
                        (.getOrDefault options :put-timeout-ms 5000)
                        (.getOrDefault options :unmerged-result? false)
                        (.getOrDefault options :n-lookahead -1)))))


(defn preduce
  "Parallelized reduction.  Currently coll must either be random access or a lznc map/filter
  chain based on one or more random access entities, hashmaps and sets from this library or
  any java.util set, hashmap or concurrent versions of these.  If input cannot be
  parallelized this lowers to a normal serial reduction.

  For potentially small-n invocations providing the parallel options explicitly will improve
  performance surprisingly - converting the options map to the parallel options object
  takes a bit of time.

  * `init-val-fn` - Potentially called in reduction threads to produce each initial value.
  * `rfn` - normal clojure reduction function.  Typehinting the second argument to double
     or long will sometimes produce a faster reduction.
  * `merge-fn` - Merge two reduction results into one.

  Options:
  * `:pool` - The fork-join pool to use.  Defaults to common pool which assumes reduction is
     cpu-bound.
  * `:parallelism` - What parallelism to use - defaults to pool's `getParallelism` method.
  * `:max-batch-size` - Rough maximum batch size for indexed or grouped reductions.  This
     can both even out batch times and ensure you don't get into safepoint trouble with
     jdk-8.
  * `:min-n` - minimum number of elements before initiating a parallelized reduction -
     Defaults to 1000 but you should customize this particular to your specific reduction.
  * `:ordered?` - True if results should be in order.  Unordered results sometimes are
    slightly faster but again you should test for your specific situation..
  * `:cat-parallelism` - Either `:seq-wise` or `:elem-wise`, defaults to `:seq-wise`.
     Test for your specific situation, this really is data-dependent. This contols how a
     concat primitive parallelizes the reduction across its contains.  Elemwise means each
     container's reduction is individually parallelized while seqwise indicates to do a
     pmap style initial reduction across containers then merge the results.
  * `:put-timeout-ms` - Number of milliseconds to wait for queue space before throwing
     an exception in unordered reductions.  Defaults to 50000.
  * `:unmerged-result?` - Defaults to false.  When true, the sequence of results
     be returned directly without any merge steps in a lazy-noncaching container.  Beware
     the noncaching aspect -- repeatedly evaluating this result may kick off the parallelized
     reduction multiple times.  To ensure caching if unsure call `seq` on the result."
  ([init-val-fn rfn merge-fn coll] (preduce init-val-fn rfn merge-fn nil coll))
  ([init-val-fn rfn merge-fn options coll]
   (unpack-reduced
    (Reductions/parallelReduction init-val-fn rfn merge-fn (lznc/->reducible coll)
                                  (options->parallel-options options)))))


(defn preduce-reducer
  "Given an instance of [[ham-fisted.protocols/ParallelReducer]], perform a parallel
  reduction.

  In the case where the result is requested unmerged then finalize will
  be called on each result in a lazy noncaching way.  In this case you can use a
  non-parallelized reducer and simply get a sequence of results as opposed to one.

  * reducer - instance of ParallelReducer
  * options - Same options as preduce.
  * coll - something potentially with a parallelizable reduction.

  See options for [[ham-fisted.reduce/preduce]].

  Additional Options:

  * `:skip-finalize?` - when true, the reducer's finalize method is not called on the result."
  ([reducer options coll]
   (let [retval (preduce (protocols/->init-val-fn reducer)
                         (protocols/->rfn reducer)
                         (protocols/->merge-fn reducer)
                         options
                         coll)]
     (if (get options :skip-finalize?)
       retval
       (if (get options :unmerged-result?)
         (lznc/map #(protocols/finalize reducer %) retval)
         (protocols/finalize reducer retval)))))
  ([reducer coll]
   (preduce-reducer reducer nil coll)))


(defmacro double-accumulator
  "Type-hinted double reduction accumulator.
  consumer:

```clojure
  ham-fisted.api> (reduce (double-accumulator acc v (+ (double acc) v))
                             0.0
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  [accvar varvar & code]
  `(reify IFnDef$ODO
     (invokePrim [this# ~accvar ~varvar]
       ~@code)))


(defmacro long-accumulator
  "Type-hinted double reduction accumulator.
  consumer:

```clojure
  ham-fisted.api> (reduce (double-accumulator acc v (+ (double acc) v))
                             0.0
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  [accvar varvar & code]
  `(reify IFnDef$OLO
     (invokePrim [this# ~accvar ~varvar]
       ~@code)))

(defn immut-map-kv
  ([keyfn valfn data]
   (-> (reduce (fn [^Map m v]
                 (.put m (keyfn v) (valfn v))
                 m)
               (ham_fisted.UnsharedHashMap. nil)
               data)
       (persistent!)))
  ([ks vs]
   (let [rv (ham_fisted.UnsharedHashMap. nil)
         ki (.iterator (Transformables/toIterable ks))
         vi (.iterator (Transformables/toIterable vs))]
     (while (and (.hasNext ki) (.hasNext vi))
       (.put rv (.next ki) (.next vi)))
     (persistent! rv))))


(defn compose-reducers
  "Given a map or sequence of reducers return a new reducer that produces a map or
  vector of results.

  If data is a sequence then context is guaranteed to be an object array.

  Options:

  * `:rfn-datatype` - One of nil, :int64, or :float64.  This indicates that the rfn's
  should all be uniform as accepting longs, doubles, or generically objects.  Defaults
  to nil."
  ([reducers] (compose-reducers nil reducers))
  ([options reducers]
   (if (instance? Map reducers)
     (let [reducer (compose-reducers (vals reducers))]
       (reify
         protocols/Reducer
         (->init-val-fn [_] (protocols/->init-val-fn reducer))
         (->rfn [_] (protocols/->rfn reducer))
         protocols/Finalize
         (finalize [_ v] (immut-map-kv (keys reducers) (protocols/finalize reducer v)))
         protocols/ParallelReducer
         (->merge-fn [_] (protocols/->merge-fn reducer))))
     (let [init-fns (.toArray ^java.util.Collection (lznc/map protocols/->init-val-fn reducers))
           rfn-dt (get options :rfn-datatype)
           ^objects rfns (object-array
                          (case rfn-dt
                            :int64
                            (->> (map protocols/->rfn reducers)
                                 (map #(Transformables/toLongReductionFn %)))
                            :float64
                            (->> (map protocols/->rfn reducers)
                                 (map #(Transformables/toDoubleReductionFn %)))
                            ;;else branch
                            (map protocols/->rfn reducers)))
           ^objects mergefns (object-array (map protocols/->merge-fn reducers))
           n-vals (count rfns)
           n-init (count init-fns)]
       (reify
         protocols/Reducer
         (->init-val-fn [_] (fn compose-init []
                              (let [rv (ham_fisted.ArrayLists/objectArray n-init)]
                                (dotimes [idx n-init]
                                  (aset rv idx ((aget init-fns idx))))
                                rv)))
         (->rfn [_]
           (case rfn-dt
             :int64 (Reductions/longCompose n-vals rfns)
             :float64 (Reductions/doubleCompose n-vals rfns)
             (Reductions/objCompose n-vals rfns)))
         protocols/Finalize
         (finalize [_ v] (mapv #(protocols/finalize %1 %2) reducers v))
         protocols/ParallelReducer
         (->merge-fn [_] (Reductions/mergeCompose n-vals, mergefns)))))))


(defn preduce-reducers
  "Given a map or sequence of [[ham-fisted.protocols/ParallelReducer]], produce a map or
  sequence of reduced values. Reduces over input coll once in parallel if coll is large
  enough.  See options for [[ham-fisted.reduce/preduce]].

```clojure
ham-fisted.api> (preduce-reducers {:sum (Sum.) :mult *} (range 20))
{:mult 0, :sum #<Sum@5082c3b7: {:sum 190.0, :n-elems 20}>}
```"
  ([reducers options coll]
   (preduce-reducer (compose-reducers reducers) options coll))
  ([reducers coll] (preduce-reducers reducers nil coll)))


(defn reducer-xform->reducer
  "Given a reducer and a transducer xform produce a new reducer which will apply
  the transducer pipeline before is reduction function.

```clojure
ham-fisted.api> (reduce-reducer (reducer-xform->reducer (Sum.) (clojure.core/filter even?))
                                (range 1000))
#<Sum@479456: {:sum 249500.0, :n-elems 500}>
```
  !! - If you use a stateful transducer here then you must *not* use the reducer in a
  parallelized reduction."
  [reducer xform]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)
        xfn (xform (fn
                     ([] (init-val-fn))
                     ([v] (protocols/finalize reducer v))
                     ([acc v] (rfn acc v))))]
    (reify
      protocols/Reducer
      (->init-val-fn [this] init-val-fn)
      (->rfn [this] xfn)
      protocols/Finalize
      (finalize [this v] (xfn v))
      protocols/ParallelReducer
      (->merge-fn [this] (protocols/->merge-fn reducer)))))


(defn reducer->rf
  "Given a reducer, return a transduce-compatible rf -

```clojure
ham-fisted.api> (transduce (clojure.core/map #(+ % 2)) (reducer->rf (Sum.)) (range 200))
{:sum 20300.0, :n-elems 200}
```"
  [reducer]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)]
    (fn
      ([] (init-val-fn))
      ([acc v] (rfn acc v))
      ([v] (protocols/finalize reducer v)))))


(defn reducer->completef
  "Return fold-compatible pair of [reducef, completef] given a parallel reducer.
  Note that folded reducers are not finalized as of this time:

```clojure
ham-fisted.api> (def data (vec (range 200000)))
#'ham-fisted.api/data
ham-fisted.api> (r/fold (reducer->completef (Sum.)) (reducer->rfn (Sum.)) data)
#<Sum@858c206: {:sum 1.99999E10, :n-elems 200000}>
```"
  [reducer]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)
        merge-fn (protocols/->merge-fn reducer)]
    (fn
      ([] (init-val-fn))
      ([l r] (merge-fn l r))
      ([v] (protocols/finalize reducer v)))))


(defn reducer-with-finalize
  [reducer fin-fn]
  (reify
    protocols/Reducer
    (->init-val-fn [r] (protocols/->init-val-fn reducer))
    (->rfn [r] (protocols/->rfn reducer))
    protocols/Finalize
    (finalize [r v] (fin-fn v))
    protocols/ParallelReducer
    (->merge-fn [r] (protocols/->merge-fn reducer))))


(defn reduce-reducer
  "Serially reduce a reducer.

```clojure
ham-fisted.api> (reduce-reducer (Sum.) (range 1000))
#<Sum@afbedb: {:sum 499500.0, :n-elems 1000}>
```"
  [reducer coll]
  (let [rfn (protocols/->rfn reducer)
        init-val-fn (protocols/->init-val-fn reducer)]
    (->> (reduce rfn (init-val-fn) coll)
         (protocols/finalize reducer))))


(defn reduce-reducers
  "Serially reduce a map or sequence of reducers into a map or sequence of results.

```clojure
ham-fisted.api> (reduce-reducers {:a (Sum.) :b *} (range 1 21))
{:b 2432902008176640000, :a #<Sum@6bcebeb1: {:sum 210.0, :n-elems 20}>}
```"
  [reducers coll]
  (reduce-reducer (compose-reducers reducers) coll))


(defn ^:no-doc reduce-reducibles
  [reducibles]
  (let [^Reducible r (first reducibles)]
    (when-not (instance? Reducible r)
      (throw (Exception. (str "Sequence does not contain reducibles: " (type (first r))))))
    (.reduce r (rest reducibles))))


(def double-consumer-accumulator
  "Converts from a double consumer to a double reduction accumulator that returns the
  consumer:

```clojure
ham-fisted.api> (reduce double-consumer-accumulator
                             (Sum$SimpleSum.)
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```" ham_fisted.ConsumerAccumulators$DoubleConsumerAccumulator/INST)


(def long-consumer-accumulator
  "Converts from a long consumer to a long reduction accumulator that returns the
  consumer:

```clojure
ham-fisted.api> (reduce double-consumer-accumulator
                             (Sum$SimpleSum.)
                             (range 1000))
#<SimpleSum@2fbcf20: 499500.0>
ham-fisted.api> @*1
499500.0
```"
  ham_fisted.ConsumerAccumulators$LongConsumerAccumulator/INST)


(def consumer-accumulator
  "Generic reduction function using a consumer"
  ham_fisted.ConsumerAccumulators$ConsumerAccumulator/INST)


(def ^{:doc "Parallel reduction merge function that expects both sides to be an instances of
  Reducible"} reducible-merge
  (bi-function lhs rhs (.reduce ^Reducible lhs rhs)))

(defmacro indexed-accum
  "Create an indexed accumulator that recieves and additional long index
  during a reduction:

```clojure
ham-fisted.api> (reduce (indexed-accum
                         acc idx v (conj acc [idx v]))
                        []
                        (range 5))
[[0 0] [1 1] [2 2] [3 3] [4 4]]
```"
  [accvar idxvar varvar & code]
  `(Reductions$IndexedAccum.
    (reify IFnDef$OLOO
      (invokePrim [this# ~accvar ~idxvar ~varvar]
        ~@code))))


(defmacro indexed-double-accum
  "Create an indexed double accumulator that recieves and additional long index
  during a reduction:

```clojure
ham-fisted.api> (reduce (indexed-double-accum
                         acc idx v (conj acc [idx v]))
                        []
                        (range 5))
[[0 0.0] [1 1.0] [2 2.0] [3 3.0] [4 4.0]]
```"
  [accvar idxvar varvar & code]
  `(Reductions$IndexedDoubleAccum.
    (reify IFnDef$OLDO
      (invokePrim [this# ~accvar ~idxvar ~varvar]
        ~@code))))


(defmacro indexed-long-accum
  "Create an indexed long accumulator that recieves and additional long index
  during a reduction:

```clojure
ham-fisted.api> (reduce (indexed-long-accum
                         acc idx v (conj acc [idx v]))
                        []
                        (range 5))
[[0 0] [1 1] [2 2] [3 3] [4 4]]
```"
  [accvar idxvar varvar & code]
  `(Reductions$IndexedLongAccum.
    (reify IFnDef$OLLO
      (invokePrim [this# ~accvar ~idxvar ~varvar]
        ~@code))))


(defn ->consumer
  "Return an instance of a consumer, double consumer, or long consumer."
  [cfn]
  (cond
    (or (instance? Consumer cfn)
        (instance? DoubleConsumer cfn)
        (instance? LongConsumer cfn))
    cfn
    (instance? IFn$DO cfn)
    (reify DoubleConsumer (accept [this v] (.invokePrim ^IFn$DO cfn v)))
    (instance? IFn$LO cfn)
    (reify LongConsumer (accept [this v] (.invokePrim ^IFn$LO cfn v)))
    :else
    (reify Consumer (accept [this v] (cfn v)))))


(defn consume!
  "Consumer a collection.  This is simply a reduction where the return value
  is ignored.

  Returns the consumer."
  [consumer coll]
  (let [c (->consumer consumer)]
    (cond
      (instance? DoubleConsumer c)
      (reduce double-consumer-accumulator c coll)
      (instance? LongConsumer c)
      (reduce long-consumer-accumulator c coll)
      :else
      (reduce consumer-accumulator c coll))
    consumer))


(defn double-consumer-preducer
  "Return a preducer for a double consumer.

  Consumer must implement java.util.function.DoubleConsumer,
  ham_fisted.Reducible and clojure.lang.IDeref.

```clojure
user> (require '[ham-fisted.api :as hamf])
nil
user> (import '[java.util.function DoubleConsumer])
java.util.function.DoubleConsumer
user> (import [ham_fisted Reducible])
ham_fisted.Reducible
user> (import '[clojure.lang IDeref])
clojure.lang.IDeref
user> (deftype MeanR [^{:unsynchronized-mutable true :tag 'double} sum
                      ^{:unsynchronized-mutable true :tag 'long} n-elems]
        DoubleConsumer
        (accept [this v] (set! sum (+ sum v)) (set! n-elems (unchecked-inc n-elems)))
        Reducible
        (reduce [this o]
          (set! sum (+ sum (.-sum ^MeanR o)))
          (set! n-elems (+ n-elems (.-n-elems ^MeanR o)))
          this)
        IDeref (deref [this] (/ sum n-elems)))
user.MeanR
user> (hamf/declare-double-consumer-preducer! MeanR (MeanR. 0 0))
nil
  user> (hamf/preduce-reducer (double-consumer-preducer #(MeanR. 0 0)) (hamf/range 200000))
99999.5
```"
  [constructor]
  (reify
    protocols/Reducer
    (->init-val-fn [r] constructor)
    (->rfn [r] double-consumer-accumulator)
    protocols/Finalize
    (finalize [r v] @v)
    protocols/ParallelReducer
    (->merge-fn [r] reducible-merge)))


(deftype DDDReducer [^{:unsynchronized-mutable true
                       :tag double} acc
                     ^{:tag IFn$DDD} rfn
                     merge-fn
                     ^{:unsynchronized-mutable true
                       :tag clojure.lang.Box} merged]

  DoubleConsumer
  (accept [this v] (set! acc (.invokePrim rfn acc v)))
  Reducible
  (reduce [this other]
    (if merged
      (set! (.-val merged) (merge-fn (.-val merged) @other))
      (set! merged (clojure.lang.Box. (merge-fn acc @other))))
    this)
  clojure.lang.IDeref
  (deref [this] (if merged (.-val merged) acc)))


(deftype LLLReducer [^{:unsynchronized-mutable true
                       :tag long} acc
                     ^{:tag IFn$LLL} rfn
                     merge-fn
                     ^{:unsynchronized-mutable true
                       :tag clojure.lang.Box} merged]

  LongConsumer
  (accept [this v] (set! acc (.invokePrim rfn acc v)))
  Reducible
  (reduce [this other]
    (if merged
      (set! (.-val merged) (merge-fn (.-val merged) @other))
      (set! merged (clojure.lang.Box. (merge-fn acc @other))))
    this)
  clojure.lang.IDeref
  (deref [this] (if merged (.-val merged) acc)))


(defn parallel-reducer
  "Implement a parallel reducer by explicitly passing in the various required functions.

  * 'init-fn' - Takes no argumenst and returns a new accumulation target.
  * 'rfn' - clojure rf function - takes two arguments, the accumulation target and a new value
     and produces a new accumulation target.
  * 'merge-fn' - Given two accumulation targets returns a new combined accumulation target.
  * 'fin-fn' - optional - Given an accumulation target returns the desired final type.

```clojure
user> (hamf-rf/preduce-reducer
       (hamf-rf/parallel-reducer
        hamf/mut-set
        #(do (.add ^java.util.Set %1 %2) %1)
        hamf/union
        hamf/sort)
       (lznc/map (fn ^long [^long v] (rem v 13)) (hamf/range 1000000)))
[0 1 2 3 4 5 6 7 8 9 10 11 12]
```
"
  ([init-fn rfn merge-fn fin-fn]
   (cond
     (instance? IFn$DDD rfn)
     (reify protocols/Reducer
       (->init-val-fn [this] #(DDDReducer. (double (init-fn)) rfn merge-fn nil))
       (->rfn [r] double-consumer-accumulator)
       protocols/ParallelReducer
       (->merge-fn [r] reducible-merge)
       protocols/Finalize
       (finalize [r v] (fin-fn @v)))
     (instance? IFn$LLL rfn)
     (reify protocols/Reducer
       (->init-val-fn [this] #(LLLReducer. (long (init-fn)) rfn merge-fn nil))
       (->rfn [r] long-consumer-accumulator)
       protocols/ParallelReducer
       (->merge-fn [r] reducible-merge)
       protocols/Finalize
       (finalize [r v] (fin-fn @v)))
     :else
     (reify
       protocols/Reducer
       (->init-val-fn [this] init-fn)
       (->rfn [r] rfn)
       protocols/ParallelReducer
       (->merge-fn [r] merge-fn)
       protocols/Finalize
       (finalize [r v] (fin-fn v)))))
  ([init-fn rfn merge-fn]
   (parallel-reducer init-fn rfn merge-fn identity)))


(defn consumer-preducer
  "Bind a consumer as a parallel reducer.

  Consumer must implement java.util.function.Consumer,
  ham_fisted.Reducible and clojure.lang.IDeref.

  Returns instance of type bound.

  See documentation for [[declare-double-consumer-preducer!]].
```"
  [constructor]
  (reify
    protocols/Reducer
    (->init-val-fn [r] constructor)
    (->rfn [r] consumer-accumulator)
    protocols/Finalize
    (finalize [r v] @v)
    protocols/ParallelReducer
    (->merge-fn [r] reducible-merge)))



(defn bind-double-consumer-reducer!
  "Bind a classtype as a double consumer parallel reducer - the consumer must implement
  DoubleConsumer, ham_fisted.Reducible, and IDeref."
  ([cls-type ctor]
   (extend cls-type
     protocols/Reducer
     {:->init-val-fn (fn [r] ctor)
      :->rfn (fn [r] double-consumer-accumulator)}
     protocols/ParallelReducer
     {:->merge-fn (fn [r] reducible-merge)}))
  ([ctor]
   (bind-double-consumer-reducer! (type (ctor)) ctor)))


(bind-double-consumer-reducer! #(Sum.))
(bind-double-consumer-reducer! #(Sum$SimpleSum.))


(defn double-consumer-reducer
  "Make a parallel double consumer reducer given a function that takes no arguments and is
  guaranteed to produce a double consumer which also implements Reducible and IDeref"
  [ctor]
  (reify
    protocols/Reducer
    (->init-val-fn [this] ctor)
    (->rfn [this] double-consumer-accumulator)
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))


(defn long-consumer-reducer
  "Make a parallel double consumer reducer given a function that takes no arguments and is
  guaranteed to produce a double consumer which also implements Reducible and IDeref"
  [ctor]
  (reify
    protocols/Reducer
    (->init-val-fn [this] ctor)
    (->rfn [this] long-consumer-accumulator)
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))


(defn consumer-reducer
  "Make a parallel double consumer reducer given a function that takes no arguments and is
  guaranteed to produce a double consumer which also implements Reducible and IDeref"
  [ctor]
  (reify
    protocols/Reducer
    (->init-val-fn [this] ctor)
    (->rfn [this] consumer-accumulator)
    protocols/Finalize
    (finalize [this v] (deref v))
    protocols/ParallelReducer
    (->merge-fn [this] reducible-merge)))

# Reductions

The ham-fisted project extends the concept of Clojure's `reduce` in a few ways,
taking influence from java streams and Clojure transducers.  The most important
way is a formal definition of a parallel reduction (analogous to `pmap` for `map`).


Most interesting is the 3 argument form of `(reduce rfn init coll)`.  Problems
exist with the 2 argument form `(reduce rfn coll)` as the reduction function -
`rfn`'s leftmost argument is sometimes a value from the collection and at other
times an accumulated value.  Some reductions have the property that the
accumulator is in the set of objects in the collection (such as numeric `+`),
these reductions are not the most general. They are a special case of a
reduction where the accumulator may be a different type entirely than the values
in the collection.


## Parallelizable Containers

Efficient parallel reductions depend on parallelizable containers.

Java has three types of containers that operate efficiently in parallel.

1) Finite random access containers (ex: an array)
2) Containers that can provide spliterators (ex: hashtable)
3) A concatenation of containers suitable for parallel computation over the parts

These three types of containers we can parallelize; random access containers,
maps and sets (or more generally anything with a correct spliterator
implementation), and concatenations of sub-containers each of which may not
itself have a parallelizable reduction.


## Parallelized Reduction

A parallelized reduction works by splitting up elements of the data source.
Many reduction contexts operate simultaneous each of which will perform a serial
reduction.  A separate step merges the results back together.  This may be
thought of as the "true" map-reduce, but either way it may be useful to compare
a parallelized reduction in detail to a serial reduction.


To perform a parallel reduction, four things must be provided:

* `init-val-fn` - a function to produce initial accumulators for each reduction context
* `rfn` - a function that takes an accumulator and a value and updates the accumulator ---  This
  is the typical reduction function passed as the first argument to Clojure's `reduce`
* `merge-fn` - a function that takes two accumulators and merges them to produces one
  result accumulator.
* `coll` - a collection of items to reduce to a single output


Here are the function signatures (Keep in mind ... `preduce`:`reduce` :: `pmap`:`map`):

```clojure
(defn preduce [init-val-fn rfn merge-fn coll] ...)
```

Notably Java streams have a 'collect' method that takes the same four arguments
where the collection is the `this` object:

```java
interface Stream<E> {
<R> R collect(Supplier<R> supplier,
              BiConsumer<R,? super T> accumulator,
              BiConsumer<R,R> combiner);
}
```


The parallelizable reduction operates as a a serial reduction if the init-val-fn
is called exactly once with no arguments and the entire collection is passed
along with rfn to reduce:

```clojure
(reduce rfn (init-val-fn) coll)
```

From there `preduce` essentially switches on the type of coll and performs one
of four distinct types of reductions:

 * serial
 * parallel over and index space
 * parallel over and spliterator space
 * parallel over sub-containers


## Map, Filter, Concat Chains


It is common in functional programming to implement data transformations as
chains of `map`, `filter`, and `concat` operations.  Analyzing sequences of
these operations is insight with regards to reduction in general and
parallelization of reductions.


The first insight is found in the Clojure transducer pathways and involves collapsing
the reduction function when possible for map and filter applications.  Let's start with
a reduction of the form `(->> coll (map x) (filter y) (reduce ...))`.

The filter operator can specialize its reduce implementation by producing a new reduction
function and reducing over its source collection:

```java
public Object reduce(IFn rfn, Object init) {
	return source.reduce(new IFn() {
	  public Object invoke(Object acc, Object v) {
	    if(pred.test(v))
		  return rfn.invoke(acc, v);
	    else
		  return acc;
	  }
	}, init);
}
```

This results in 'collapsing' the reduction allowing the source to perform the
iteration across its elements and simply dynamically creating a slightly more
complex reduction function, `rfn`.  A similar pathway exists for `map` as we can
always delegate up the chain making a slightly more complex reduction function
as long as we are reducing over a single source of data.  This optimization
leads to many fewer function calls and intermediate collections when compared
with naive implementations of `map` and `filter`. Clojure's transducers do this
automatically.

Collapsing the reduction also allows us to parallelize reductions like the initial one
stated before as if the filter object has a parallelReduction method that does an identical
collapsing pathway then if the source is parallelizable then the reduction itself can
still parallelize:

```java
public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn) {
  return source.parallelReduction(initValFn, new IFn() {...}, mergeFn);
}
```

If the source collection itself allows for parallel reduction, then it's
possible to achieve similar 'collapsing' in `preduce`.  Clojure's transducers do
not have this particular optimization for parallel reduction, but Java streams
do.


Also worth noting, these optimizations are only available if we use the 4
argument form of reduce *and* if we assume that `map`, `filter`, and `concat` are lazy
and non-caching.


With those assumptions in place it is possible to parallelize a reduction over
the entries, keys or values of map using simple primitive composition:

```clojure
user> (require '[ham-fisted.api :as hamf])
nil
user> (require '[ham-fisted.lazy-noncaching :as lznc])
nil
user> (def data (hamf/immut-map (lznc/map #(vector % %) (range 20000))))
#'user/data
user> (type data)
ham_fisted.PersistentHashMap
user> (hamf/preduce + + + (lznc/map key data))
199990000
```

## Stream-based Reductions

Java streams have a notion of parallel reduction built-in. Their design suffers
from two flaws, one minor and one major.

The first minor flaw is that you can ask a stream for a parallel version of
itself and it will give you one if possible else return a copy of itself.
Unfortunately this only works on the first stream in a pipeline so for instance:

```java
  coll.stream().map().filter().parallel().collect();
```

yields a serial reduction while:

```java
  coll.stream().parallel().map().filter().collect();
```

yields a parallel reduction.

This is unfortunate because it means you must go back in time to get a parallel
version of the stream if you want to perform a parallel collection; something
that may or may not be easily done at the point in time when you decide you do
in fact want to parallel reduction (especially in library code).

The second and more major flaw is that stream-based parallelization does not
allow the user to pass in their own fork-join pool at any point. This limits use
to the built in pool where it's pad form to park threads or do blocking
operations.


## reducers.clj And Parallel Folds

Clojure has an alpha namespace that provides a parallel reduction, [reducers.clj](https://github.com/clojure/clojure/blob/master/src/clj/clojure/core/reducers.clj).  The signature
for this method is:

```clojure
(defn fold
  "Reduces a collection using a (potentially parallel) reduce-combine
  strategy. The collection is partitioned into groups of approximately
  n (default 512), each of which is reduced with reducef (with a seed
  value obtained by calling (combinef) with no arguments). The results
  of these reductions are then reduced with combinef (default
  reducef). combinef must be associative, and, when called with no
  arguments, (combinef) must produce its identity element. These
  operations may be performed in parallel, but the results will
  preserve order."
  {:added "1.5"}
  ([reducef coll] (fold reducef reducef coll))
  ([combinef reducef coll] (fold 512 combinef reducef coll))
  ([n combinef reducef coll]
     (coll-fold coll n combinef reducef)))
```

In this case we use overloads of `combinef` or `reducef` to provider the initial accumulator
(called the identity element), the rfn, finalization and the merge function.  `combinef` called
with no arguments provides each thread context's accumulator and called with two arguments
performs a merge of two accumulators.  `reducef` called with 2 arguments provides
the reduction from a value into the accumulator and when called with one argument
finalizes both the potentially stateful reducing function and finalizes the
accumulator.  It prescribes the parallelization system but users can override a protocol
to do it themselves.

This the same major drawback as the java stream system, namely users cannot provide
their own pool for parallelization.

An interesting decision was made here as to whether one can actually parallelize the
reduction or not.  Transducers, the elements providing `reducef`, may be stateful
such as `(take 15)`.  One interesting difference is that state is done with a closure in
the reduction function as opposed to providing a custom accumulator that wraps the user's
accumulator but tracks state.

One aspect we haven't discussed but that is also handled here in an interesting
manner is that whether a reduction can be parallelized or not is a function both
of the container *and* of the reducer.  `reducers.clj` does a sort of
double-dispatch where the transducer may choose to implement the parallel
reduction, called `coll-fold` or not and is queried first and if it allows
parallel reduction then the collection itself is dispatched.  Overall this is a
great, safe choice because it disallows completely parallel dispatch if the
transducer or the collection do not support it.


## Parallel Reducers

If we combine all three functions: `init-val-fn`, `rfn`, and `merge-fn` into one object
then we get a ParallelReducer, defined in protocols.clj.  This protocol allows the
user to pass a single object into a parallelized reduction as opposed to three functions
which is useful when we want to have many reducers reduce over a single source of data.
A `finalize` method is added in order to allow compositions of reducers, and to allow
reducers to hide state and information from end users:

```clojure
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
two initial values and returns a new initial value.")
  (finalize [item v]
    "A finalize function called on the result of the reduction after it is
reduced but before it is returned to the user.  Identity is a reasonable default."))
```

There are defaults to the reducer protocol for an IFn which assumes it can be
called with no arguments for a initial value and two arguments for both
reduction and merge.  This works for things like `+` and `*`.  Additionally
there are implementations provided for the ham_fisted Sum (Kahans compensated)
and SimpleSum
[DoubleConsumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/DoubleConsumer.html)
classes.


With the three functions bundled into one logical protocol or object it is easy then
to create complex (aggregate) and efficient parallelized reductions:

```clojure
user> (require '[ham-fisted.api :as hamf])
nil
user> (hamf/preduce-reducers {:sum + :product *} (range 1 20))
{:product 121645100408832000, :sum 190}
user>
```

This goes over the data in parallel, exactly once.


## Consumers, Transducers, and `rfn` Chains

If we look at the reduction in terms of a push model as opposed to a pull model
where the stream will push data into a consumer then we can implement similar
chains or map and filter.  These are based on creating a new consumer that takes
the older consumer and the filter predicate or mapping function.  In this way
one can implement a pipeline on the input stream, or perhaps diverging pipelines
on each reduction function in a multiple reducer scenario.  Since the init and
merge functions operate in accumulator space, which remains unchanged, one can
develop up increasingly sophisticated reduction functions and still perform a
parallelized reduction.  Naturally, everything is composed in reverse (push
instead of pull), which is the reason that `comp` works in reverse when working
with transducers.

In fact, given that the covers are pulled back on composing reduction functions,
the definition of the single argument `clojure.core/filter` becomes more clear:

```clojure
(defn filter
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns logical true. pred must be free of side-effects.
  Returns a transducer when no collection is provided."
  {:added "1.0"
   :static true}
  ([pred]
    (fn [rf]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
           (if (pred input)
             (rf result input)
             result)))))))
```

It returns a function that, when given a reduction function, returns a new reduction
function that when called in the two argument form is identical to the result above
(although expressed in pure Clojure as opposed to Java).


Starting with the concept that a reduction begins at the collection, flows
downward through the pipeline and bottoms out at the reducer then the
lazy-noncaching namespace and Java streams implement parallelization flowing
from the container downward. Separately consumer chains and transducers
implement the pipeline flowing up from the reducer itself.  Thus building the
pipeline either downward from the source or upward from the final reduction
produces subtly different properties.  Regardless, every system must disable
parallelization where it will cause an incorrect answer (to ensure correctness)
- such as in a stateful transducer.


Broadly speaking, however, it can be faster to enable full parallelization and
filter invalid results than it is to force an early serialization our problem
and thus lose lots of our parallelization potential.  When concerned with
performance, attempt to move transformations as much as possible into a
parallelizable domain.


For the `take-n` use case specifically mentioned above and potentially for others we
can parallelize the reduction and do the take-n both in the parallelized phase and
in the merge phase assuming we are using an ordered parallelization, so that doesn't
itself necessarily force a serialized reduction but there are of course
transformations and reductions that do.  There are intermediate points however that are
perhaps somewhat wasteful in terms of cpu load but do allow for more parallelization - a
tradeoff that is sometimes worth it. Generically speaking we can visualize this sort
of a tradeoff as triangle of three points where one point is data locality, one
point parallelism, and one point redundancy.  Specifically if we are willing to
trade some cpu efficiency for some redundancy, for instance, then we often get more
parallelization.  Likewise if we are willing to save/load data from 'far' away from
the CPU, then we can cut down on redundancy but at the cost of locality.  For more
on this line of thinking please take a moment and read at least some of Jonathan Ragan-Kelly's
[excellent PhD thesis](http://people.csail.mit.edu/jrk/jrkthesis.pdf) - a better explanation
of the above line of reasoning begins on page 20.


## Primitive Typed Serial Reductions

This comes last for a good reason :-) - it doesn't make a huge difference in performance
but it should be noted allowing objects to implemented typed reductions:

```java
default Object doubleReduction(IFn.ODO op, Object init);
default Object longReduction(IFn.OLO op, Object init)
```

where the next incoming value is a primitive object but the accumulator is still
a generic object allows us to use things like `DoubleConsumers` and
`LongConsumers` and avoid boxing the stream.  Furthermore if the aforementioned
`map` and `filter` primitives are careful about their rfn composition we can
maintain a completely primitive typed pipeline through an entire processing
chain.


## One Final Note About Performance

Collapsing reductions brings the source iteration pathway closer to the final
reduction pathway in terms of machine stack space which allows HotSpot to
optimize the entire chain more readily.  Regardless of how good HotSpot gets,
however, parallelizing will nearly always result in a larger win but both work
together to enable peak performance on the JVM given arbitrary partially typed
compositions of sequences and reductions.

When increasing the data size yet again, one can of course use the same design
to distribute the computations to different machines.  As some people have
figured out, however, simply implementing the transformations you need
efficiently reduces or completely eliminates the need to distribute computation
in the first place leading to a simpler, easier to test and more robust system.

Ideally we can make achieving great performance for various algorithms clear and easy and
thus avoid myriad of issues regarding distributing computing in the first place.

* The first rule of distributed systems is to avoid distributing your computation in the first place - [1](https://bravenewgeek.com/service-disoriented-architecture/), [2](https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing).
* The first law of distributed objects is to avoid using distributed objects. [3](https://martinfowler.com/bliki/FirstLaw.html).

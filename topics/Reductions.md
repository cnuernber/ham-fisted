# Reductions

In ham-fisted I have attempted to extend the concept of reduction in Clojure a few
ways taking influence from java streams and Clojure transducers.  The most important
way is a formal definition of a parallel reduction.


We are only interested in the 4 argument form of reductions `(reduce rfn init coll)`.
It turns out there are type issues with the 3 argument form `(reduce rfn coll)` as the
reduction function - `rfn`'s leftmost argument could either be a value
from the collection *or* an accumulated value.  While there are reductions where the
accumulator is in the set of objects in the collection such as numeric `+`, these are
not the most general and are a special case of a reduction where the accumulator may
be a different type entirely than the values in the collection.


## Parallelizable Containers


In Java there are three types of containers that we can efficiently parallelize.
The first two are finite contains that are either randomly addressable or have a
spliterator design where we can divide the elements roughly but perhaps not
precisely in half.  A array is an example of a randomly addressable container
while a hashtable is an example of a container that allows us to split our iterator
ending up with 2 iterators and dividing the elements roughly in half.


The third type of container is a concatenation many other containers
where we can parallelize reduction across each sub-container in one of two ways, either
by in parallel reducing each sub-container or by performing a parallelized reduction
across each sub-container container.


Regardless, we end up with a few types of containers we can parallelize; random access
containers, maps and sets or more generally anything with a correct spliterator
implementation, and concatenations of sub-containers each of which may not itself
have a parallelizable reduction.


## Parallelized Reduction

A parallelized reduction must generally have a way of splitting up elements of the data
source.  Then it will create many reduction contexts each of which will perform a
serial reduction.  Finally it will need to merge the results back together.  I think this
is perhaps a clearer definition of map-reduce than map-reduce but either way I think it
is useful to compare a parallelized reduction in detail to a serial reduction.


So our parallelizable reduction entry point must have at least four things from the user:

* `init-val-fn` - A function to produce initial accumulators for each reduction context.
* `rfn` - a function that takes an accumulator and a value and updates the accumulator.  This
  is the typical reduction operator used for clojure's `reduce` function.
* `merge-fn` - a function that takes two accumulators and merges them to produces one
  result accumulator.


Here are the function signatures:

```clojure
(defn preduce [init-val-fn rfn merge-fn coll] ...)
```

We should note that the java stream 'collect' method takes the same four arguments where
the collection is the `this` object:

```java
interface Stream<E> {
<R> R collect(Supplier<R> supplier,
              BiConsumer<R,? super T> accumulator,
              BiConsumer<R,R> combiner);
}
```


The parallelizable reduction breaks down in a serial reduction when the init-val-fn is called
once with no arguments and the entire collection is passed along with rfn to reduce:

```clojure
(reduce rfn (init-val-fn) coll)
```

From there we can imagine `preduce` switching on the type of coll and performing one of four
distinct types of reductions:

 * serial
 * parallelizing over and index space.
 * parallelizing over and spliterator space.
 * parallelizing over sub-containers.


## Map, Filter, Concat Chains


It is common in functional programming or perhaps ubiquitous to implement your data
transformations as chains of map, filter, and concat operations.  Analyzing sequences
of these operations yields a few insights with regards to reduction in general
and parallelization of reductions.


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
complex reduction function, `rfn`.  A similar pathway exists for map as we can always delegate
up the chain making a slightly more complex reduction function as long as we are reducing
over a single source of data.  This same optimization is done automatically Clojure's
transducer implementations.

Collapsing the reduction also allows us to parallelize reductions like the initial one
stated before as if the filter object has a parallelReduction method that does an identical
collapsing pathway then if the source is parallelizable then the reduction itself can
still parallelize:

```java
public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn) {
  return source.parallelReduction(initValFn, new IFn() {...}, mergeFn);
}
```

In this way we can parallelize reductions over map,filter chains assuming the source data
itself allows for a parallelized reduction.  This optimization is *not* done in
transducer pathways but it *is* done with java streams.


These optimizations are only available if we use the 4 argument form of reduce *and* if
we assume that map, filter, and concat are lazy and non-caching.


Given those assumtions, however, this means that we can parallelize a reduction over the
entries, keys or values of map using simple primitive composition:

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

Java streams have an interesting parallelization design that currently suffers from
two flaws, one minor and one major.

The first minor is that you can ask a stream for a parallel version of itself and it will
give you one if possible else return a copy of itself.  Unfortunately this only
works on the first stream in a pipeline so for instance:

```java
  coll.stream().map().filter().parallel().collect();
```

yeilds a serial collection while:

```java
  coll.stream().parallel().map().filter().collect();
```

yeilds a parallel collection.

This is unfortunate because it means you must go back in time
to get a parallel version of the stream if you want to perform a parallel collection;
something that may or may not be easily done.

The second more major flaw is stream-based parallelization is hampered additionally in that
it does not allow the user to pass in a fork-join pool at any point and thus it only works
on cpu-based reductions that can never hang in the ForkJoinPool's common pool.


## Parallel Reducers

If we combine all three functions: `init-val-fn`, `rfn`, and `merge-fn` into one object
then we get a ParallelReducer, defined in protocols.clj.  This protocol allows the
user to pass a single object into a parallelized reduction as opposed to three functions
which is useful when we want to have many reducers reduce over a single source of data:

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
two initial values and returns a new initial value."))
```

There are defaults to the reducer protocol for an IFn which simple assumes it can be
called with no arguments for a initial value and two arguments for both reduction
and merge.  This works for things like `+` and `*`.  Additionally there are implementations
provided for the ham_fisted Sum (Kahans compensated) and SimpleSum [DoubleConsumer](https://docs.oracle.com/javase/8/docs/api/java/util/function/DoubleConsumer.html) classes.


With the three functions bundled into one logical protocol or object it is easy then
to create complex or aggregate yet efficient parallelized reductions:

```clojure
user> (require '[ham-fisted.api :as hamf])
nil
user> (hamf/preduce-reducers {:sum + :product *} (range 1 20))
{:product 121645100408832000, :sum 190}
user>
```

## Consumers, Transducers, and `rfn` Chains

If we look at the reduction in terms of a push model as opposed to a pull model where the
stream will push data into a consumer then we can implement similar map,filter chains
that are based around create a new consumer that takes the older consumer and the predicate
or mapping function.  In this way we can both implement a pipeline on the input stream
and we can implement perhaps diverging pipelines on each reduction function in a
multiple reducer scenario.  Since our init and merge functions operate accumulator
space then remains unchanged so we can build up more and more sophisticated reduction
functions and then still perform a parallelized reduction.  We then build up things in reverse
which is the reason that `comp` works in reverse when working with transducers.

In fact, given that we now know the game about composing reduction functions, the definition
of the single argument `clojure.core/filter` I think is more clear:

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
(although expressed in pure Clojure as opposed to Java).  It is unfortunate but it
isn't clear to me at this time how to extend this design if `rf` isn't a simple IFn
but itself is a parallelizable reducer with the three functions mentioned above.
The issue is that there needs to be another overload of the two-argument form that
indicates this is a merge operation as opposed to a reduction operation - perhaps a
three arity form where the third argument is simply a placeholder.  Regardless in my
opinion overloading arity to provide an interface is less clear than simply
implementing the interface.


## Primitive Typed Serial Reductions

This comes last for a good reason :-) - it doesn't make a huge difference in performance
but it should be noted allowing objects to implemented typed reductions:

```java
default Object doubleReduction(IFn.ODO op, Object init);
default Object longReduction(IFn.OLO op, Object init)
```

where the next incoming value is a primitive object but the accumulator is still a generic
object allows us to use things like `DoubleConsumers` and `LongConsumers` and avoid boxing our
stream.  Furthermore if the aforementioned map and filter primitives are careful about
their rfn composition we can maintained a completely primitive typed pipeline through our
entire processing chain.


## One Final Note About Performance

Collapsing reductions brings the source iteration pathway closer to the final
reduction pathway in terms of machine stack space which I believe allows HotSpot to
optimize the entire chain more readily.  Regardless of how good HotSpot gets,
however, parallelizing will nearly always result in a larger win but both work
together to enable peak performance on the JVM given arbitrary partially typed compositions
of sequences and reductions.

If we increase the data size yet again then we can of course use the same design to
distribute the computations to different machines.  As some people have figured out, however,
simply implementing the transformations you need efficiently reduces or completely eliminates
the need to distribute computation in the first place leading to a simple, easier to test and
more robust system.

Ideally we can make achieving great performance for various algorithms clear and easy and
thus avoid myriad of issues regarding distributing computing in the first place.

* The first rule of distributed systems is to avoid distributing your computation in the first place - [1](https://bravenewgeek.com/service-disoriented-architecture/), [2](https://en.wikipedia.org/wiki/Fallacies_of_distributed_computing).
* The first law of distributed objects is to avoid using distributed objects. [3](https://martinfowler.com/bliki/FirstLaw.html).

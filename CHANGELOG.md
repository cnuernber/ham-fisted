# 1.000-beta-50
 * forgot type hints on array constructors.

# 1.000-beta-49
 * Final round of optimizations for double array creation.  Turns out reductions really
   are faster.

# 1.000-beta-48
 * macros for double, float, long, and int array creation that will inline a fastpath
   if the argument is a compile-time vector or integer.  Bugfix for casting floats
   to longs.
 * shorthand macros, ivec, lvec, fvec, dvec to create array-backed containers that
   allow nth destructuring.

# 1.000-beta-47
 * major double-array, float-array, long-array, int-array optimizations.

# 1.000-beta-46
 * Set protocol to supercede the set protocol from dtype-next.
 * lots and lots of fixes from dataset work.

# 1.000-beta-45
 * Small fixes and making helpers public for dtype-next work.

# 1.000-beta-43
 * long lists really are long lists - copy-paste mistake from int lists.

# 1.000-beta-42
 * declare-double-consumer-preducer! - given type derived from DoubleConsumer
   and a few others, create a parallel reducer.
 * declare-consumer-preducer! - similar to above, incoming data is not expected
   to be a stream of double values.

# 1.000-beta-41
 * ->collection is protocol driven allowing new non-collection things like bitmaps
   to be turned temporarily into collections.  This means that reductions and collection
   conversion are protocol driven.

# 1.000-beta-40
 * maps are iterable...

# 1.000-beta-39
 * Explicit protocols for serial and parallel reduction and reducers.
 * Explicit support for BitSet objects.
 * Updated (->reducible) pathways to check for protocol reducer support.
 * Protocol for conversion of arbitrary types to iterable for map, filter support.

# 1.000-beta-38
 * Fixed comparison of seq with nonseq.

# 1.000-beta-37
 * Small perf enhancements from tmd perf regression

# 1.000-beta-36
 * Added in explicit checks for long, double, and predicate objects in filter's reduction
   specializations.  Potentially these are too expensive but it does help a bit with
   longer sequences.
 * Changed things such that Double/NaN evaluates to false.  This matches that the null
   object evaluates to false and null evaluates to Double/NaN.


# 1.000-beta-35
 * Added finalize method to reducers to match transducer spec.
 * Exposed `compose-reducers` that produces a new reducer from a map or sequence of
   other reducers.
 * These changes simplfied `reduce-reducers` and `preduce-reducers`, `sum` and `sum-fast`.


# 1.000-beta-34
 * Enable parallelization for instances of clojure.core.PersistentHashMap.
 * protocol-based parallelization of reductions so you can extend the parallelization
   to new undiscovered classes.
 * reducer-xform->reducer - Given a reducer and a transducer xform produce a new reducer
   that will apply the transform to the reduction function of the reducer:

```clojure
ham-fisted.api> (reduce-reducer (reducer-xform->reducer (Sum.) (clojure.core/filter even?))
                                (range 1000))
#<Sum@70149930: {:sum 249500.0, :n-elems 500}>
```

# 1.000-beta-33
 * Finally a better api to group-by-reduce and group-by can now be implemented
   via group-by-reduce.  group-by-reduce uses same 3 function arguments as
   preduce so your reduction systems are interchangable between these two
   systems.
 * Fixed `conj` for all growable array lists.
 * Added a protocol for parallel reductions.  This allows you to pass in one object
   and transform it into the three functions required to do a parallel reduction.
 * Added preduce-reducer, preduce-reducers for a single reducer or a sequence or
   map of reducers, respectively.


# 1.000-beta-32
 * group-by, group-by-reduced fixed for large n.

# 1.000-beta-31
 * min-n is now a long with parallel options.
 * lazy-noncaching namespace now has map-indexed.  Faster reductions and random access
   objects stay random access.

# 1.000-beta-30
 * Various bugfixes from dtype work.

# 1.000-beta-29
 * Ranges with more than Integer/MAX_VALUE elems can be accessed via their IFn overloads
  and support custom lgetLong and lgetDouble methods that take long indexes for long and
  double ranges.


# 1.000-beta-28
 * Use lookahead and put timeouts for all parallelization primitives so that if a long
  running parallelization is cancelled the forkjoin pool itself isn't hung.
 * Enable long and double ranges whose size is larger than Integer/MAX_VALUE.  This includes
   parallelized reductions which even optimized take basically forever.
 * Add better defaults for reductions to long and double -specific IMutList interfaces.
 * Ensure reduction implementations do not dereference a reduced accumulator.
 * Fix reducible interface to it matches preduce.
 * Added persistent! implementation which fails gracefully if input is already persistent.
 * Fixed group-by, group-by-reduce, and pfrequencies implementation to use preduce.
 * conj works on map, filter, and concat from the lazy-noncaching library.

# 1.000-beta-27
 * Remove explicit support for boolean primitives.

# 1.000-beta-26
 * IFnDef overloads implement their appropriate java.util.function counterparts.

# 1.000-beta-25
 * Removed claypoole from dependencies.
 * Move typed clojure function interface definitions from Reductions to IFnDef.
 * Added overrides of keys, vals that produce parallelizable collections if the input
   itself is a parallelizable collection - either maps from this library or any java
   hashmap.

# 1.000-beta-24
 * preduce has new option to help parallelize concat operations - they can be parallelized
   two different ways, either elemwise where each container parallelizes its reduction or
   by sequence where an initial reduction is done with pmap then the results are merged.
 * all random access contains support spliterator and typed stream construction.
 * Fix bug in upmap causing hanging with short sequences.

# 1.000-beta-23
 * double conversion to long fails for NaN.
 * Careful combining of typed map/filter chains to avoid causing inaccuracies when
   converting from double to long.
 * Major parallelism upgrade - spliterator-based objects such as java.util.hashmap and
   all the hashmaps/hashsets from this library now support parallelized reduction.

# 1.000-beta-22
 * Numeric values are range checked on input to addLong.

# 1.000-beta-21
 * Removed ensureCapacity from IMutList.

# 1.000-beta-20
 * Moved to double reduction as opposed to double foreach.  Perftested heavily and found that reduce
   is just as fast and more general.

# 1.000-beta-18
 * expose sublistcheck.

# 1.000-beta-17
 * Stricter correctness checking for sublist types, everything implements Associative.

# 1.000-beta-16
 * Correctness fixes for pmap, upmap, pgroups, upgroups.

# 1.000-beta-15
 * Fixed sum for large n-elems.
 * upgroups - Unordered parallel groupings for random access systems.
 * Indexed consumers for copying, broadcasting type operations.
 * Reducible interface for objects that can reduce themselves.

# 1.000-beta-14
 * ArraySection is now first-class, will rebase dtype-next array pathways on this.

# 1.000-beta-12
 * pmap is guaranteed *not* to require `shutdown-agents`.

# 3.007
 * [issue 22](https://github.com/cnuernber/ham-fisted/issues/21) - better clj-condo support.
 * new [process](https://cnuernber.github.io/ham-fisted/ham-fisted.process.html) namespace for 
   launching and controlling sub processes.
 
# 3.003
 * New functions and docs added to 'iterator' namespace.
 
# 3.000
 * All protocols replaced with hamf's defprotocol impl.
 
# 2.039
 * Slightly faster protocol dispatch as avoids instance check for interface impl in favor of
   only using map lookup.
 * Implementation of cond that can be used in functions that need to avoid boxing.
 
# 2.038
 * Fixing some small perf issues with primitive protocol implementations.
 
# 2.037
 * Object constants work when explicitly specified via 'extend'.
 * Object arrays explicitly checked for protocol dispatch when 
   dealing with array types.
 * 'true' and 'false' supported as object constant types - nil unsupported.
 * Namespace comments.
 
# 2.036
 * Work on defprotocol to support primitive and constant return types.
 
# 2.035
 * Mutable treelists now result in less data on sublist and error on double persistent call.

# 2.034
 * Fix to smarter sublist impl - incorrect in some cases -- tests updated to catch in future.
 
# 2.033
 * MAJOR UPGRADE - implemented vec and vector as treelists.  This matches Clojure's
   persistent vector implementation in performance for mutable and immutable conj,
   reduce pathways but has a much smarter subvec implementation and faster
   conversion to and from object arrays.
 
# 2.032
 * Two merge-iterator implementations used to N-way merging of sorted sequences.  Linear is for small n <= 32 and
   the priority queue method is for larger N's.  The exact cutoff where performance will matter will depend on
   the dataset and the relative cost of the comparator.
 * Attempted to move all sorts to Arrays/parallelSort as it outperforms fastutils parallel sort by a bit.
 * added lsum, lsummary, dsummary for long-space sum, and simple summary statistics [min max mean sum n-elems] in long and double space respectively.

# 2.031
 * `(reduce + 0 (lznc/apply-concat nil))` works.

# 2.030
 * pmap-opts could produce an empty incorrect result if a custom pool was provided and parallelism was not specified.

# 2.029
 * Error in hash-map compute - did not remove key if compute fn was nil.

# 2.028
 * First class bloom filter support - uses apache parquet block-split-bloom-filter.

# 2.027
 * Fix for api/difference when left hand side is a java map.

# 2.026
 * Somewhat faster byte and short array creation when input is an IMutList impl.

# 2.025
 * Bugfix so update-values is consistent with persistent maps.

# 2.024
 * Bugfix for add-constant! default impls.

# 2.023
 * Bulk add-constant interface for all growable lists.

# 2.022
 * growable lists support clear.

# 2.021
 * `lines` - replacement for line-seq that returns an auto-closeable iterable and cannot cache nor hold-onto-head
    the data.
 * `re-matches` - faster version of re-matches.

# 2.020
 * split caffeine support off into its own namespace.

# 2.019
 * impl/pmap really does support user-defined thread pool.

# 2.018
 * Add clj-kondo exports and config, fix linting errors
 * Remove support for and call to `take-last` 1-arity, which was not valid.
 * Fix variable arity `merge-with`, which was not correctly implemented.
 * `apply-concat`, `concat-opts` accept cat-parallelism option allow you to specify how the concatenation
   should be parallelized at the creation source as opposed to at the preduce/parallel reduction callsite.

# 2.017
 * Faster compose-reducers especially where there really are a lot of reducers.

# 2.015
 * [issue 13](https://github.com/cnuernber/ham-fisted/issues/13) - any IMutList chunkedSeq was partially incorrect.

# 2.014
 * frequencies respects map-fn option to allow concurrent hashmaps to be used.

# 2.013
 * `cartesian-map` no longer has a random access variant.  The cooler version of this uses the tensor
   address mechanism to allow parallel redution.
 * fixed major issue with parallel frequencies.

# 2.011
 * Much faster every? implementation esp. for primitive arrays and persistent vectors.
 * More hlet extensions - `lng-fns` and `dbl-fns` which are faster in the general case
   then `lngs` and `dbls` as they avoid RT/nth.
 * efficient `cartesian-map` which does a cartesian join across its inputs and calls f on
   each value.
```clojure
user> (hamf/sum-fast (lznc/cartesian-map
                      #(h/let [[a b c d](lng-fns %)]
                         (-> (+ a b) (+ c) (+ d)))
                      [1 2 3]
                      [4 5 6]
                      [7 8 9]
                      [10 11 12 13 14]))
3645.0
```

# 2.010
 * Extensible let - hlet and helpers make using the primitive overloads of clojure functions easier.
   See the ham-fisted.hlet and ham-fisted.primtive-invoke namespaces.

# 2.009
 * typed 'nth' methods efficient for primitive manipulations - 'dnth', 'fnth', 'inth', 'lnth'.


# 2.008
 * Custom reduce implemented for object array wrappers.


2.007
* reduce namespace now has helper to create a parallel reducer.
* hashset has optimized addall pathway when input is another hashset.

# 2.006
 * partition-by accepts a predicate function in options - example in docs.

# 2.005
 * implemented lazy noncaching partition-all - similar perf to partition-by.
 * Faster default dispatch for pgroups, upgroups.
 * Faster sum-fast if input is random access.

# 2.004
 * Faster sort implemented as default in several places.

# 2.004
 * Ensure all object sorting is done with parallelQuickSort.
 * Small fix to array macros to use l2i instead of RT.intCast.

# 2.003
 * Major issue in compose-reducers - object composition was typed to double reduction.

# 2.002
 * slightly faster partition-by - inner loop written in java.

# 2.001
 * Added lazy-noncaching partition-by.  This method has somewhat higher performance than
   clojure.core/partition-by as it does not make intermediate containers and is strictly
   lazy-noncaching.

# 2.000
 * Rebuilt hashmaps on faster foundation especially for micro benchmarks.
 * Removed bits and pieces that do not provide enough return on investment.
 * For more in-depth comments see [PR-7](https://github.com/cnuernber/ham-fisted/pull/7).

# 1.009
 * new linked hashmap implementation with equiv-semantics and fast union op.

# 1.008
 * Fast set intersection for longer sequencers of sets (intersect-sets).

# 1.007
 * Fast pathways for finding min/max index of a collection of objects in a similar way to min-key and max-key.

# 1.006
 * Very specific upgrade to combine-reducers pathways.

# 1.005
 * pmap, upmap pathways now return an object that full implements seqable and ireduceinit.

# 1.002
 * Added n-lookahead to the parallel options pathway as for some problems
   this makes a major difference in the efficiency of the pmap pathway.

# 1.001
 * Fixed pmap implementation to release memory much more aggressively.

# 1.000-beta-98
 * Fixed serious but subtle issue when a transient hash map is resized.  This should be
   considered a must-have upgrade.

# 1.000-beta-96
 * Fixed from upgrading dtype-next.
 * Major breaking changes!! API functions have been moved to make the documentation
   clearer and the library more maintainable in the long run.
 * map-union of mutable or transient maps produces mutable or transient maps!
 * Final refactoring before 1.000 release.
 * Functions to make creating java.util.function objects are moved to ham-fisted.function.
 * Reduction-related systems are moved to ham-fisted.reduce.
 * java.util.Map helpers are moved to ham-fisted.mut-map.

# 1.000-beta-93
 * java implementation of a batched stream reducer.  This avoids adding java to
   the stream api.

# 1.000-beta-92
 * pure java reductions for situations where you have an index fn a count.  These mainly
   just make benchmarks a bit more stable.

# 1.000-beta-91
 * IFnDef supports interfaces for long supplier (L), double supplier (D), and
   Supplier (O).

# 1.000-beta-90
 * Accelerated map boolean union, intersection, difference  for hashtable, long hashtable.
 * Major bugfix in map dissoc.


# 1.000-beta-89
 * Added inc-consumer - returns a generic consumer that increments a long.  Useful for the various
   situations where you need to track an incrementing variable but don't want the overhead of
   using a volatile variable.

# 1.000-beta-88
 * Immutable maps and vectors derive from APersistentMap and APersistentVector so that downtream
   libraries can pick them up transparently.

# 1.000-beta-86
 * Error in reduction of empty ranges.

# 1.000-beta-85
 * Slightly faster map construction pathways.
 * In fact both the hamf base map `mut-map` and the integer-specialized `mut-long-hashtable-map` are
   faster than the default `clojure.data.int-map` pathway for construction and value lookup according
   to the benchmarks in `clojure.data.int-map`.  Interestingly enough they are fastest if you create
   an intermediate object array using lznc/apply-concat:

```clojure
user> (count entries)
1000000
user> (c/quick-bench (into (persistent! (hamf/mut-long-hashtable-map))  entries))
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 366.247830 ms
    Execution time std-deviation : 11.024896 ms
   Execution time lower quantile : 348.564420 ms ( 2.5%)
   Execution time upper quantile : 376.625750 ms (97.5%)
                   Overhead used : 1.492920 ns
nil
user> (c/quick-bench (into (i/int-map) entries))
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 568.189038 ms
    Execution time std-deviation : 1.677163 ms
   Execution time lower quantile : 566.564884 ms ( 2.5%)
   Execution time upper quantile : 570.564253 ms (97.5%)
                   Overhead used : 1.492920 ns
nil
user> (def ll (into (persistent! (hamf/mut-long-hashtable-map)) entries))
#'user/ll
user> (def il (into (i/int-map) entries))
#'user/il
user> (c/quick-bench
       (dotimes [idx (count entries)]
         (.get ^java.util.Map ll idx)))

Evaluation count : 84 in 6 samples of 14 calls.
             Execution time mean : 7.399383 ms
    Execution time std-deviation : 62.314286 µs
   Execution time lower quantile : 7.297162 ms ( 2.5%)
   Execution time upper quantile : 7.451677 ms (97.5%)
                   Overhead used : 1.492920 ns
nil
user> (c/quick-bench
       (dotimes [idx (count entries)]
         (.get ^java.util.Map il idx)))

Evaluation count : 30 in 6 samples of 5 calls.
             Execution time mean : 22.936125 ms
    Execution time std-deviation : 583.510734 µs
   Execution time lower quantile : 22.334216 ms ( 2.5%)
   Execution time upper quantile : 23.654541 ms (97.5%)
                   Overhead used : 1.492920 ns
nil
user>

user> (c/quick-bench (hamf/mut-long-hashtable-map (hamf/into-array Object (lznc/apply-concat entries))))
Evaluation count : 6 in 6 samples of 1 calls.
             Execution time mean : 271.755568 ms
    Execution time std-deviation : 1.545379 ms
   Execution time lower quantile : 270.184413 ms ( 2.5%)
   Execution time upper quantile : 273.736344 ms (97.5%)
                   Overhead used : 1.492920 ns
nil
```


# 1.000-beta-84
 * `wrap-array`, `wrap-array-growable`, major into-array optimizations and better
   `map-reducible`.

# 1.000-beta-83
 * Opening the door to custom IReduce implementations.

# 1.000-beta-82
 * convert hashsets to use hashtables instead of bitmap tries.
 * careful analysis of various vec-like object creation mechanisms.

# 1.000-beta-81
 * long primitive hashtables - these are quite a bit faster but especially when used directly.

# 1.000-beta-80
 * Helpers for very high performance scenarios.  lazy-noncaching/map-reducible,
   api/->long-predicate.

# 1.000-beta-78
 * fill-range is now property accelerated making all downstream projects that use addAll and
   friends far faster.

# 1.000-beta-77
 * see [commit 38596d8](https://github.com/cnuernber/ham-fisted/commit/38596d85541de7d2c926ac8b16522a764fcb6af9)

# 1.000-beta-76
 * Added group-by-consumer - this has different performance and functionality characteristics
   than group-by-reducer.  For instance, group-by-consumer with a linked hashmap will return
   a map with keys in the order of keys initially encounted.  group-by-reducer with the same
   hashmap will return a map with keys in the order of latest encountered.  Group-by-consumer
   uses `computeIfAbsent` which is a slightly faster primitive than `compute` as it doesn't
   need to check the return value of the reducer, only of the initialization of the map
   entry.

# 1.000-beta-75
 * MapForward class so we can use normal java maps in normal Clojure workflows.

# 1.000-beta-74
 * bugfix - Map's `compute` has to accept nil keys.

# 1.000-beta-73
 * memoize now supports `:eviction-fn` - for callbacks when things get evicted.
 * More helpers for memoized fns - cache-as-map, evict-memoized-call.

# 1.000-beta-72
 * Switch to caffeine for memoize cache and standard java library priority queue for take-min.
 This removed the dependency on google guava thus drastically cutting the chances for dependency
 conflicts.

# 1.000-beta-71
 * HUGE CHANGES!!! - moved to hashtable implementation for main non-array map instead of
   bitmap trie.  This is because in all my tests it is *much* faster for everything *aside*
   from non-transient (reduce assoc ...) type loops which are a waste of time to begin with.
 * Because there are now three full map implementations  (array, trie, hashtable) there is a
   more defined map structure making it less error prone to test out different map backends.
 * Lots of inner class renaming and such - however `frequencies`, `group-by-reduce`, and
   `mapmap` now a bit faster - about 2X.  Here is a telling performance metric:

```clojure
({:construct-μs 2.726739663709692,
  :access-μs 1.784634592104282,
  :iterate-μs 2.7345543552812073,
  :ds-name :java-hashmap}
 {:construct-μs 3.5414584143710885,
  :access-μs 2.761234751112207,
  :iterate-μs 2.1730894775185403,
  :ds-name :hamf-hashmap}
 {:construct-μs 6.475808180747403,
  :access-μs 2.484804237281106,
  :iterate-μs 1.8564705765641765,
  :ds-name :hamf-transient}
 {:construct-μs 11.43649782981362,
  :access-μs 5.152473242630386,
  :iterate-μs 8.793332955848225,
  :ds-name :clj-transient})
ham-fisted.hash-map-test>
```


# 1.000-beta-69
 * Faster `mode`.
 * Faster map iteration.
 * Corrected clojure persistent hash map iteration.

# 1.000-beta-67
 * Faster `mode`.
 * `mmax-key` - use `(mmax-key f data)` as opposed to `(apply max-key f data)`.  It is faster
   and handles empty sequences.  Same goes for `mmin-key`.


# 1.000-beta-66
 * Fixed `make-comparator`.
 * Added `mode`.

# 1.000-beta-65
 * Better obj->long and obj->double pathways that will always apply the appropriate cast
   and thus have 0 arg variants.
 * Better/faster sort-by pathway that avoids potential intermediate data creation.

# 1.000-beta-64
 * Fixed predicate, long-consumer, double-consumer and consumer pathways.
 * Faster dispatch for preduce.

# 1.000-beta-62
 * Faster dispatch for preduce.

# 1.000-beta-61
 * All lists are comparable.

# 1.000-beta-60
 * nil is convertible to iterable and collections without fail.

# 1.000-beta-59
 * Container reduction must respect reduced - I think this is a design flaw but not a
 serious or impactful one aside from requiring more complex per-container reduction code.

# 1.000-beta-58
 * Perf tweaks and small fixes from TMD.

# 1.000-beta-57
 * Additional round of optimizations around creation of persistent vector objects.
 * renamed a few of the functor-creation macros.
 * lznc/map explicity supports long->obj transformations as these are often used as
   index->obj lookup systems.
 * Rebuilt IMutList's toArray pathway to use reduction.

# 1.000-beta-56
 * Switched completely to clojure.core.protocols/CollReduce.
 * Removed a solid amount of cruft and simplified reduction architecture.
 * Now loading hamf transparently makes reductions on all arrays and many
   java such as hashmaps datastructures faster.


# 1.000-beta-55
 * Removed lots of old cruft.
 * Added IFnDef predicates so you can use IFn-based predicates from java.

# 1.000-beta-54
 * Removed lots of old cruft.
 * Added IFnDef predicates so you can use IFn-based predicates from java.

# 1.000-beta-53
 * `:unmerged-result?`, `:skip-finalize?` options for `preduce` and `preduce-reducer`.  This
   allows you to use the parallelized reductions pathway but get a sequence of results back
   as opposed to a single result.  It also allows you to used reducers or
   transducing-compatible rfn's that have no parallel merge pathway and handle the parallel
   merge yourself after the parallelized reduction.
 * Fixed issue with single-map parallel reductions to ensure that it passes the parallel
   reduction request to its source data.

# 1.000-beta-52
 * bulk union, intersection operations.
 * Faster `equiv` for longs and doubles but equivalent for everything else.

# 1.000-beta-51
 * additional set operation - parallelized `unique`.
 * exposed indexed accumulator macros in api for use outside library.
 * generic protocol fn add-fn that must return a reduction compatible function
   for a given collection.

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

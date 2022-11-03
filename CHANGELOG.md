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

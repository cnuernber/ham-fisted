# HAM-Fisted

[![Clojars Project](https://clojars.org/com.cnuernber/ham-fisted/latest-version.svg)](https://clojars.org/com.cnuernber/ham-fisted)
* [API docs](https://cnuernber.github.io/ham-fisted/)
* [Clojure Conj Talk](https://www.youtube.com/watch?v=ralZ4j_ruVg)

## Summary

Clojure-style immutable collections (and mutable counterparts), together with
some operations, all aimed at high performance.

In particular, high-performance in large-`n` and parallel contexts. Included are
a namespace of lazy but not caching operations, a `ForkJoinPool` oriented
`pmap`, and a system of parallel reductions. This gives the user
somewhat-drop-in replacements for familiar Clojure tools that can be faster.

## History

What started as a collection of efficient mutable and immutable data structures based
on Phil Bagwell's bitmap trie concept became an overall reimplementation of
Clojure's core datastructures and some of its base concepts specifically with
performance in mind.  This means, for instance, that the library prefers iterators
over sequences in many situations.  There are also new functional primitives developed
from my experience processing data and working with Clojure over the last 10 years.


My hope is this library becomes a platform to experiment and develop new and/or better
functional datastructures and algorithmic primitives.


Here are a few concepts to keep in mind -


## In-place Mutable -> Persistent

The mutable hashmap and vector implementations allow in-place instantaneous conversion to
their persistent counterparts.  This allows you to build a dataset using the sometimes much
faster mutable primitives (.compute and friends for instance in the hashmap case) and then
return data to the rest of the program in persistent form.  Using this method, for example
`frequencies` is quite a bit faster while still returning a persistent datastructure.


Along those lines construction of a persistent vector from an object array is very fast
so it is very efficient to construct a persistent vector from an object-array-list - the
array list being much faster to build.


## New Primitive Operations

There are many new primitive operations than listed below - please take a moment to scan
the api docs.  Some standouts are:

#### Map Union, Difference, Intersection

Aside from simply a reimplementation of hashmaps and persistent vectors this library
also introduces a few new algorithms namely map-union, map-intersection, and
map-difference.  These are implemented at the trie level so they avoid rehashing any
keys and use the structure of the hashmap in order to boost performance.  This means
`merge` and `merge-with` are much faster especially if you have larger maps.  But it
also means you can design novel set-boolean operations as you provide a
value-resolution operator for the map values.


Because the hamf hashmaps have fast unions, you can now design systems where for
instance each thread builds up a separate hashmap and the results are unioned together
back in the main thread in a map-reduce type design.  This type of design was the
original target of the union system.

These systems are substantially faster if the objects have a high cost to hash and do
not cache their hashcode but  this is rare for Clojure systems as persistent vectors, maps
and keywords cache their hash values.  Strings, however, are an example of something where
these primitives (and things like frequencies) will perform substantially better.


### Casting and Finite Numbers

Float and double values are only allowed to cast to long if they are
[finite](https://docs.oracle.com/javase/8/docs/api/java/lang/Double.html#isFinite-double-).
Boolean values casted to long or double are 0 for false and 1 for true.  Any nonzero
finite number casted to boolean is true, 0 is false, non-finite numbers are errors.  `nil`
casted to a floating point number is NaN.  NaN casted to an object is NaN.  If objects
are not number then nil is false and non-nil is true.  Any undefined cast falls back to
`clojure.lang.RT.xCast` where `x` denotes the target type.


Reading data from contains leads to unchecked casts while writing data to contains
leads to checked casts.



#### update-values, group-by-reduce, mapmap

* `update-vals` - is far faster than map->map pathways if you want to update every
  value in the map but leave the keys unchanged.
* `group-by-reduce` - perform a reduction during the group-by.  This avoids keeping
   a large map of the complete intermediate values which can be both faster and more
   memory efficient.
* `mapmap` - A techascent favorite, equivalent to:
```clojure
(->> (map map-fn src-map) (remove nil?) (into {}))
```

#### Parallelized Reductions - preduce


I have an entire topic on [Reductions](https://cnuernber.github.io/ham-fisted/Reductions.html) -
give it a read and then send me an email with your thoughts :-).

* [preduce](https://cnuernber.github.io/ham-fisted/ham-fisted.api.html#var-preduce)
* [preduce-reducer](https://cnuernber.github.io/ham-fisted/ham-fisted.api.html#var-preduce-reducer)
* [preduce-reducers](https://cnuernber.github.io/ham-fisted/ham-fisted.api.html#var-preduce-reducers)


These parallelization primitives allow users to pass in their own forkjoin pools
so you can use it for blocking tasks although it is setup be default for
cpu-bound operations.  Concat operations can parallelize reductions over
non-finite or non-parallelizable containers using the default `:seq-wise`
`:cat-parallelization` option.


#### All Arrays Are First Class

* Any array including any primitive array can be converted to an indexed operator
 with efficient sort, reduce, etc. implementations using the `lazy-noncaching`
 namespace's `->random-access` operator.  This allows you to pass arrays as is to
 the rest of your clojure program without conversion to a persistent vector -
 something that is both not particularly efficient and explodes the data size.


#### Random Access Containers Support Negative Indexes

All random access containers, be it vectors, lists, or array lists support
nth, ifn interfaces taking -1 to index from the end of the vector.  For performance
reasons, the implementation of List.get does not -

```clojure
ham-fisted.persistent-vector-test> ((api/vec (range 10)) -1)
9
ham-fisted.persistent-vector-test> (nth (api/vec (range 10)) -1)
9
ham-fisted.persistent-vector-test> (.get (api/vec (range 10)) -1)
Execution error (IndexOutOfBoundsException) at ham_fisted.ChunkedList/indexCheck (ChunkedList.java:210).
Index underflow: -1
ham-fisted.persistent-vector-test> ((api/->random-access (api/int-array (range 10))) -1)
9
ham-fisted.persistent-vector-test> (nth (api/->random-access (api/int-array (range 10))) -1)
9
```
Similarly `last` is constant time for all any list implementation deriving from
`java.util.RandomAccess`.

## Other ideas

 * `lazy-noncaching` namespace contains very efficient implementations of map,
   filter, concat, and repeatedly which perform as good as or better than the
   eduction variants without chunking or requiring you to convert your code from
   naive clojure to transducer form.  The drawback is they are lazy noncaching so
   for instance `(repeatedly 10 rand)` will produce 10 random values every time it
   is evaluated.  Furthermore `map` will produce a random-access return value if
   passed in all random-access inputs thus preserving the random-access property of
   the input.

 * lazy-caching namespace contains inefficient implementations that do in fact
   cache - it appears that Clojure's base implementation is very good or at least
   good enough I can't haven't come up with one better.  Potentially the decision
   to use chunking is the best optimization available here.


## Contributing

The best way to contribute is to fund me through github sponsors linked to
the right or to engage [TechAscent](https://techascent.com) - we are always
looking for new interesting projects and partners.


Aside from that as mentioned earlier my hope is this library becomes
a platform that enables experimentation with various functional primitives and
overall optimized ways of doing the type of programming that the Clojure community
enjoys.  Don't hesitate to file issues and PR's - I am happy to accept both.

If you want to work on the library you need to enable the `:dev` alias.


## Benchmarks

Lies, damn lies, and benchmarks - you can run the benchmarks with `./scripts/benchmark`.
Results will be printed to the console and saved to results directory prefixed by the
commit, your machine name and the jdk version.

Results will print normalized to either the base time for clojure.core (clj) or for
java.util (java).  One interesting thing here is in general how much better JDK-17
is for many of these tests than JDK-8.

Here are some example timings taken using my laptop plugged in with an external cooling
supply (frozen peas) applied to the bottom of the machine.  An interesting side note is
that I get better timings often when running from the REPL for specific benchmarks than
from the benchmark - perhaps due to the machine's heat management systems.


#### JDK-17


|                  :test | :n-elems | :java | :clj | :eduction | :hamf | :norm-factor-μs |
|------------------------|---------:|------:|-----:|----------:|------:|----------------:|
|              :assoc-in |        5 |       |  1.0 |           | 0.646 |           0.245 |
|          :assoc-in-nil |        5 |       |  1.0 |           | 0.371 |           0.120 |
|               :concatv |      100 |       |  1.0 |           | 0.099 |           9.827 |
|           :frequencies |    10000 |       |  1.0 |           | 0.412 |         966.154 |
|                :get-in |        5 |       |  1.0 |           | 0.564 |           0.124 |
|              :group-by |    10000 |       |  1.0 |           | 0.333 |        1414.480 |
|       :group-by-reduce |    10000 |       |  1.0 |           | 0.313 |        1408.028 |
|        :hashmap-access |    10000 | 0.700 |  1.0 |           | 0.989 |         549.468 |
|        :hashmap-access |       10 | 0.837 |  1.0 |           | 0.826 |           0.400 |
|  :hashmap-cons-obj-ary |        4 |       |  1.0 |           | 0.355 |           0.392 |
|  :hashmap-cons-obj-ary |       10 |       |  1.0 |           | 0.584 |           0.864 |
|  :hashmap-cons-obj-ary |     1000 |       |  1.0 |           | 0.541 |         124.811 |
|  :hashmap-construction |    10000 | 0.563 |  1.0 |           | 0.923 |        1331.130 |
|  :hashmap-construction |       10 | 0.240 |  1.0 |           | 0.357 |           2.337 |
|        :hashmap-reduce |    10000 | 0.792 |  1.0 |           | 0.860 |         316.433 |
|        :hashmap-reduce |       10 | 0.703 |  1.0 |           | 0.735 |           0.360 |
|              :int-list |    20000 | 1.000 |      |           | 1.147 |         467.994 |
|                :mapmap |     1000 |       |  1.0 |           | 0.276 |         275.786 |
|          :object-array |    20000 |       |  1.0 |           | 0.240 |        1560.975 |
|           :object-list |    20000 | 1.000 |      |           | 0.987 |         518.878 |
|    :sequence-summation |    20000 |       |  1.0 |      0.29 | 0.409 |        1380.496 |
|               :shuffle |    10000 |       |  1.0 |           | 0.353 |         329.709 |
|                  :sort |    10000 |       |  1.0 |           | 0.337 |        2418.307 |
|          :sort-doubles |    10000 |       |  1.0 |           | 0.374 |        2272.143 |
|             :sort-ints |    10000 |       |  1.0 |           | 0.291 |        2514.779 |
|                 :union |       10 | 0.155 |  1.0 |           | 0.088 |           1.785 |
|                 :union |    10000 | 0.275 |  1.0 |           | 0.174 |        1664.823 |
|            :union-disj |       10 | 0.156 |  1.0 |           | 0.085 |           1.798 |
|            :union-disj |    10000 | 0.279 |  1.0 |           | 0.178 |        1641.344 |
|          :union-reduce |       10 | 0.139 |  1.0 |           | 0.220 |          23.954 |
|          :union-reduce |    10000 | 0.100 |  1.0 |           | 0.159 |       41261.663 |
|             :update-in |        5 |       |  1.0 |           | 1.153 |           0.276 |
|         :update-in-nil |        5 |       |  1.0 |           | 0.276 |           0.158 |
|         :update-values |     1000 |       |  1.0 |           | 0.090 |         158.994 |
|         :vector-access |       10 | 1.568 |  1.0 |           | 1.008 |          77.945 |
|         :vector-access |    10000 | 0.957 |  1.0 |           | 1.027 |         125.778 |
| :vector-cons-obj-array |       10 | 1.184 |  1.0 |           | 0.356 |           0.071 |
| :vector-cons-obj-array |    10000 | 0.083 |  1.0 |           | 0.048 |         112.192 |
|   :vector-construction |       10 | 0.460 |  1.0 |           | 1.124 |           0.078 |
|   :vector-construction |    10000 | 0.082 |  1.0 |           | 0.078 |         117.432 |
|         :vector-reduce |       10 | 1.996 |  1.0 |           | 1.088 |           0.150 |
|         :vector-reduce |    10000 | 1.228 |  1.0 |           | 0.863 |         194.194 |
|       :vector-to-array |       10 | 0.256 |  1.0 |           | 0.503 |           0.041 |
|       :vector-to-array |    10000 | 0.063 |  1.0 |           | 0.124 |          69.590 |


#### JDK-1.8


|                  :test | :n-elems | :java | :clj | :eduction | :hamf | :norm-factor-μs |
|------------------------|---------:|------:|-----:|----------:|------:|----------------:|
|              :assoc-in |        5 |       |  1.0 |           | 0.801 |           0.274 |
|          :assoc-in-nil |        5 |       |  1.0 |           | 0.275 |           0.142 |
|               :concatv |      100 |       |  1.0 |           | 0.120 |           6.810 |
|           :frequencies |    10000 |       |  1.0 |           | 0.421 |         960.710 |
|                :get-in |        5 |       |  1.0 |           | 0.598 |           0.125 |
|              :group-by |    10000 |       |  1.0 |           | 0.335 |        1410.690 |
|       :group-by-reduce |    10000 |       |  1.0 |           | 0.293 |        1433.528 |
|        :hashmap-access |    10000 | 0.817 |  1.0 |           | 1.046 |         541.540 |
|        :hashmap-access |       10 | 0.791 |  1.0 |           | 0.904 |           0.402 |
|  :hashmap-cons-obj-ary |        4 |       |  1.0 |           | 0.398 |           0.407 |
|  :hashmap-cons-obj-ary |       10 |       |  1.0 |           | 0.696 |           0.682 |
|  :hashmap-cons-obj-ary |     1000 |       |  1.0 |           | 0.449 |         130.423 |
|  :hashmap-construction |    10000 | 0.586 |  1.0 |           | 0.927 |        1281.371 |
|  :hashmap-construction |       10 | 0.278 |  1.0 |           | 0.401 |           2.238 |
|        :hashmap-reduce |    10000 | 0.625 |  1.0 |           | 0.704 |         321.295 |
|        :hashmap-reduce |       10 | 0.714 |  1.0 |           | 0.862 |           0.312 |
|              :int-list |    20000 | 1.000 |      |           | 1.017 |         497.492 |
|                :mapmap |     1000 |       |  1.0 |           | 0.325 |         226.518 |
|          :object-array |    20000 |       |  1.0 |           | 0.391 |        1374.725 |
|           :object-list |    20000 | 1.000 |      |           | 0.981 |         493.795 |
|    :sequence-summation |    20000 |       |  1.0 |      0.37 | 0.227 |        1342.562 |
|               :shuffle |    10000 |       |  1.0 |           | 0.430 |         286.478 |
|                  :sort |    10000 |       |  1.0 |           | 0.284 |        2839.552 |
|          :sort-doubles |    10000 |       |  1.0 |           | 0.424 |        2485.058 |
|             :sort-ints |    10000 |       |  1.0 |           | 0.279 |        2885.107 |
|                 :union |       10 | 0.147 |  1.0 |           | 0.093 |           1.938 |
|                 :union |    10000 | 0.278 |  1.0 |           | 0.198 |        1446.922 |
|            :union-disj |       10 | 0.144 |  1.0 |           | 0.091 |           1.938 |
|            :union-disj |    10000 | 0.285 |  1.0 |           | 0.198 |        1440.777 |
|          :union-reduce |       10 | 0.114 |  1.0 |           | 0.230 |          25.724 |
|          :union-reduce |    10000 | 0.081 |  1.0 |           | 0.159 |       37008.010 |
|             :update-in |        5 |       |  1.0 |           | 1.401 |           0.292 |
|         :update-in-nil |        5 |       |  1.0 |           | 0.289 |           0.138 |
|         :update-values |     1000 |       |  1.0 |           | 0.083 |         166.794 |
|         :vector-access |       10 | 1.563 |  1.0 |           | 1.131 |          86.023 |
|         :vector-access |    10000 | 0.945 |  1.0 |           | 1.047 |         139.996 |
| :vector-cons-obj-array |       10 | 1.150 |  1.0 |           | 0.365 |           0.075 |
| :vector-cons-obj-array |    10000 | 0.066 |  1.0 |           | 0.040 |         103.369 |
|   :vector-construction |       10 | 0.508 |  1.0 |           | 1.119 |           0.073 |
|   :vector-construction |    10000 | 0.062 |  1.0 |           | 0.070 |         109.040 |
|         :vector-reduce |       10 | 2.041 |  1.0 |           | 1.031 |           0.152 |
|         :vector-reduce |    10000 | 1.392 |  1.0 |           | 1.026 |         146.636 |
|       :vector-to-array |       10 | 0.284 |  1.0 |           | 0.569 |           0.036 |
|       :vector-to-array |    10000 | 0.052 |  1.0 |           | 0.068 |          65.946 |


## CAVEATS!!

This code is minimally tested.  The datastructures especially need serious testing, potentially generative
testing of edge cases.

Also, microbenchmarks do not always indicate how your system will perform overall.  For instance- when
testing `assoc-in`, `update-in` in this project we see better performance.  In at least one real
world project, however, the inlining that makes the microbenchmark perform better definitely did
*not* result in the project running faster -- it ran a bit slower even though the profiler of the
original code indicated the sequence operations performed during assoc-in and update-in were a source
of some time.

The JVM is a complicated machine and there are issues with using, for instance, too many classes
at a particular callsite.  Overall I would recommend profiling and being careful.  My honest opinion
right now is that `assoc-in` and `update-in` do not improve program performance at least in
some of the use cases I have tested.


## Other Interesting Projects

* [clj-fast](https://github.com/bsless/clj-fast) - Great and important library more focused on compiler upgrades.
* [bifurcan](https://github.com/lacuna/bifurcan) - High speed functional datastructures for Java.  Perhaps ham-fisted should
be based on this or we should measure the differences and take the good parts.
* [Clojure Goes Fast](http://clojure-goes-fast.com/) - Grandaddy aggregator project with a lot of important information and a set of crucial github projects such as [clj-memory-meter](https://github.com/clojure-goes-fast/clj-memory-meter).

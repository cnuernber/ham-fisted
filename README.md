# HAM-Fisted

[![Clojars Project](https://clojars.org/com.cnuernber/ham-fisted/latest-version.svg)](https://clojars.org/com.cnuernber/ham-fisted)
* [API docs](https://cnuernber.github.io/ham-fisted/)

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



#### All Arrays Are First Class

* Any array including any primitive array can be converted to an indexed operator
 with efficient sort, reduce, etc. implementations using the `lazy-noncaching`
 namespace's `->random-access` operator.  This allows you to pass arrays as is to
 the rest of your clojure program without conversion to a persistent vector -
 something that is both not particularly efficient and explodes the data size.


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
|           :frequencies |    10000 |       |  1.0 |           | 0.385 |         967.531 |
|              :group-by |    10000 |       |  1.0 |           | 0.329 |        1329.194 |
|       :group-by-reduce |    10000 |       |  1.0 |           | 0.307 |        1355.767 |
|        :hashmap-access |    10000 | 0.690 |  1.0 |           | 0.992 |         553.074 |
|        :hashmap-access |       10 | 0.854 |  1.0 |           | 0.842 |           0.369 |
|  :hashmap-construction |    10000 | 0.575 |  1.0 |           | 0.936 |        1308.212 |
|  :hashmap-construction |       10 | 0.244 |  1.0 |           | 0.380 |           2.152 |
|        :hashmap-reduce |    10000 | 0.656 |  1.0 |           | 0.723 |         320.289 |
|        :hashmap-reduce |       10 | 0.752 |  1.0 |           | 0.751 |           0.298 |
|              :int-list |    20000 | 1.000 |      |           | 1.149 |         434.040 |
|                :mapmap |     1000 |       |  1.0 |           | 0.545 |         206.120 |
|          :object-array |    20000 |       |  1.0 |           | 0.425 |        1425.899 |
|           :object-list |    20000 | 1.000 |      |           | 0.983 |         462.833 |
|    :sequence-summation |    20000 |       |  1.0 |     0.329 | 0.206 |        1058.715 |
|               :shuffle |    10000 |       |  1.0 |           | 0.384 |         302.689 |
|                  :sort |    10000 |       |  1.0 |           | 0.267 |        2176.375 |
|          :sort-doubles |    10000 |       |  1.0 |           | 0.404 |        2085.365 |
|             :sort-ints |    10000 |       |  1.0 |           | 0.304 |        2288.625 |
|                 :union |       10 | 0.167 |  1.0 |           | 0.122 |           1.510 |
|                 :union |    10000 | 0.288 |  1.0 |           | 0.187 |        1425.948 |
|            :union-disj |       10 | 0.167 |  1.0 |           | 0.116 |           1.495 |
|            :union-disj |    10000 | 0.284 |  1.0 |           | 0.184 |        1440.896 |
|          :union-reduce |       10 | 0.141 |  1.0 |           | 0.219 |          23.976 |
|          :union-reduce |    10000 | 0.102 |  1.0 |           | 0.152 |       36723.854 |
|         :update-values |     1000 |       |  1.0 |           | 0.090 |         148.749 |
|         :vector-access |       10 | 1.549 |  1.0 |           | 1.037 |          75.591 |
|         :vector-access |    10000 | 0.942 |  1.0 |           | 0.983 |         121.010 |
| :vector-cons-obj-array |       10 | 1.063 |  1.0 |           | 0.334 |           0.069 |
| :vector-cons-obj-array |    10000 | 0.095 |  1.0 |           | 0.049 |         104.711 |
|   :vector-construction |       10 | 0.454 |  1.0 |           | 1.062 |           0.072 |
|   :vector-construction |    10000 | 0.092 |  1.0 |           | 0.088 |         111.198 |
|         :vector-reduce |       10 | 1.789 |  1.0 |           | 1.054 |           0.137 |
|         :vector-reduce |    10000 | 1.210 |  1.0 |           | 0.914 |         159.798 |
|       :vector-to-array |       10 | 0.256 |  1.0 |           | 0.470 |           0.037 |
|       :vector-to-array |    10000 | 0.062 |  1.0 |           | 0.110 |          68.389 |


#### JDK-1.8


|                  :test | :n-elems | :java | :clj | :eduction | :hamf | :norm-factor-μs |
|------------------------|---------:|------:|-----:|----------:|------:|----------------:|
|           :frequencies |    10000 |       |  1.0 |           | 0.448 |         819.862 |
|              :group-by |    10000 |       |  1.0 |           | 0.350 |        1139.116 |
|       :group-by-reduce |    10000 |       |  1.0 |           | 0.324 |        1196.253 |
|        :hashmap-access |    10000 | 0.855 |  1.0 |           | 1.030 |         547.340 |
|        :hashmap-access |       10 | 0.942 |  1.0 |           | 0.864 |           0.362 |
|  :hashmap-construction |    10000 | 0.584 |  1.0 |           | 0.910 |        1280.537 |
|  :hashmap-construction |       10 | 0.272 |  1.0 |           | 0.398 |           2.006 |
|        :hashmap-reduce |    10000 | 0.674 |  1.0 |           | 0.716 |         276.646 |
|        :hashmap-reduce |       10 | 0.699 |  1.0 |           | 0.843 |           0.313 |
|              :int-list |    20000 | 1.000 |      |           | 1.027 |         404.411 |
|                :mapmap |     1000 |       |  1.0 |           | 0.540 |         189.054 |
|          :object-array |    20000 |       |  1.0 |           | 0.359 |        1228.262 |
|           :object-list |    20000 | 1.000 |      |           | 0.986 |         407.321 |
|    :sequence-summation |    20000 |       |  1.0 |     0.425 | 0.296 |         939.321 |
|               :shuffle |    10000 |       |  1.0 |           | 0.436 |         253.450 |
|                  :sort |    10000 |       |  1.0 |           | 0.291 |        2450.002 |
|          :sort-doubles |    10000 |       |  1.0 |           | 0.344 |        2137.026 |
|             :sort-ints |    10000 |       |  1.0 |           | 0.305 |        2693.692 |
|                 :union |       10 | 0.124 |  1.0 |           | 0.073 |           1.676 |
|                 :union |    10000 | 0.278 |  1.0 |           | 0.206 |        1236.060 |
|            :union-disj |       10 | 0.122 |  1.0 |           | 0.072 |           1.665 |
|            :union-disj |    10000 | 0.277 |  1.0 |           | 0.206 |        1229.636 |
|          :union-reduce |       10 | 0.126 |  1.0 |           | 0.273 |          21.467 |
|          :union-reduce |    10000 | 0.089 |  1.0 |           | 0.155 |       31320.541 |
|         :update-values |     1000 |       |  1.0 |           | 0.092 |         134.339 |
|         :vector-access |       10 | 1.539 |  1.0 |           | 1.252 |          78.743 |
|         :vector-access |    10000 | 0.963 |  1.0 |           | 1.059 |         125.088 |
| :vector-cons-obj-array |       10 | 0.984 |  1.0 |           | 0.349 |           0.070 |
| :vector-cons-obj-array |    10000 | 0.070 |  1.0 |           | 0.044 |          88.196 |
|   :vector-construction |       10 | 0.472 |  1.0 |           | 1.119 |           0.065 |
|   :vector-construction |    10000 | 0.064 |  1.0 |           | 0.075 |          96.041 |
|         :vector-reduce |       10 | 2.140 |  1.0 |           | 1.096 |           0.126 |
|         :vector-reduce |    10000 | 1.302 |  1.0 |           | 0.950 |         133.458 |
|       :vector-to-array |       10 | 0.256 |  1.0 |           | 0.557 |           0.031 |
|       :vector-to-array |    10000 | 0.057 |  1.0 |           | 0.065 |          57.734 |


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
at a particular callsite.  Overally I would recommend profiling and being careful.  My honest opinion
right now is that `assoc-in` and `update-in` do not improve program performance at least in
some of the use cases I have tested.


## Other Interesting Projects

* [clj-fast](https://github.com/bsless/clj-fast) - Great and important library more focused on compiler upgrades.
* [bifurcan](https://github.com/lacuna/bifurcan) - High speed functional datastructures for Java.  Perhaps ham-fisted should
be based on this or we should measure the differences and take the good parts.
* [Clojure Goes Fast](http://clojure-goes-fast.com/) - Grandaddy aggregator project with a lot of important information and a set of crucial github projects such as [clj-memory-meter](https://github.com/clojure-goes-fast/clj-memory-meter).

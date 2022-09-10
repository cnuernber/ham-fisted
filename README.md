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


|                  :test | :n-elems |      :java | :clj |  :eduction |      :hamf | :norm-factor-μs |
|------------------------|---------:|-----------:|-----:|-----------:|-----------:|----------------:|
|           :frequencies |    10000 |            |  1.0 |            | 0.38513343 |    967.53104487 |
|              :group-by |    10000 |            |  1.0 |            | 0.32928832 |   1329.19395437 |
|       :group-by-reduce |    10000 |            |  1.0 |            | 0.30664465 |   1355.76698577 |
|        :hashmap-access |    10000 | 0.68956912 |  1.0 |            | 0.99167213 |    553.07427348 |
|        :hashmap-access |       10 | 0.85360471 |  1.0 |            | 0.84198438 |      0.36887030 |
|  :hashmap-construction |    10000 | 0.57460625 |  1.0 |            | 0.93610444 |   1308.21162821 |
|  :hashmap-construction |       10 | 0.24353700 |  1.0 |            | 0.38026331 |      2.15172593 |
|        :hashmap-reduce |    10000 | 0.65628856 |  1.0 |            | 0.72280577 |    320.28853345 |
|        :hashmap-reduce |       10 | 0.75154411 |  1.0 |            | 0.75096314 |      0.29791816 |
|              :int-list |    20000 | 1.00000000 |      |            | 1.14891325 |    434.04025167 |
|                :mapmap |     1000 |            |  1.0 |            | 0.54474620 |    206.11952355 |
|          :object-array |    20000 |            |  1.0 |            | 0.42465918 |   1425.89901515 |
|           :object-list |    20000 | 1.00000000 |      |            | 0.98345228 |    462.83265917 |
|    :sequence-summation |    20000 |            |  1.0 | 0.32866975 | 0.20599411 |   1058.71512153 |
|               :shuffle |    10000 |            |  1.0 |            | 0.38394391 |    302.68883795 |
|                  :sort |    10000 |            |  1.0 |            | 0.26723325 |   2176.37497101 |
|          :sort-doubles |    10000 |            |  1.0 |            | 0.40435442 |   2085.36461905 |
|             :sort-ints |    10000 |            |  1.0 |            | 0.30422289 |   2288.62516288 |
|                 :union |       10 | 0.16686057 |  1.0 |            | 0.12237473 |      1.50997230 |
|                 :union |    10000 | 0.28799629 |  1.0 |            | 0.18673330 |   1425.94835556 |
|            :union-disj |       10 | 0.16683291 |  1.0 |            | 0.11635302 |      1.49476615 |
|            :union-disj |    10000 | 0.28433232 |  1.0 |            | 0.18361443 |   1440.89627976 |
|          :union-reduce |       10 | 0.14136054 |  1.0 |            | 0.21927239 |     23.97626449 |
|          :union-reduce |    10000 | 0.10245037 |  1.0 |            | 0.15242466 |  36723.85416667 |
|         :update-values |     1000 |            |  1.0 |            | 0.09020129 |    148.74903731 |
|         :vector-access |       10 | 1.54900622 |  1.0 |            | 1.03689604 |     75.59120008 |
|         :vector-access |    10000 | 0.94224665 |  1.0 |            | 0.98267979 |    121.00997320 |
| :vector-cons-obj-array |       10 | 1.06324189 |  1.0 |            | 0.33415900 |      0.06910238 |
| :vector-cons-obj-array |    10000 | 0.09537717 |  1.0 |            | 0.04937273 |    104.71130159 |
|   :vector-construction |       10 | 0.45414250 |  1.0 |            | 1.06171544 |      0.07228341 |
|   :vector-construction |    10000 | 0.09182399 |  1.0 |            | 0.08842786 |    111.19818716 |
|         :vector-reduce |       10 | 1.78862996 |  1.0 |            | 1.05379602 |      0.13701922 |
|         :vector-reduce |    10000 | 1.20996221 |  1.0 |            | 0.91425731 |    159.79774942 |
|       :vector-to-array |       10 | 0.25568143 |  1.0 |            | 0.46957859 |      0.03721399 |
|       :vector-to-array |    10000 | 0.06207004 |  1.0 |            | 0.10982923 |     68.38903736 |


#### JDK-1.8


|                  :test | :n-elems |      :java | :clj | :eduction |      :hamf | :norm-factor-μs |
|------------------------|---------:|-----------:|-----:|----------:|-----------:|----------------:|
|           :frequencies |    10000 |            |  1.0 |           | 0.44801340 |    819.86180217 |
|              :group-by |    10000 |            |  1.0 |           | 0.35001460 |   1139.11578090 |
|       :group-by-reduce |    10000 |            |  1.0 |           | 0.32443220 |   1196.25342659 |
|        :hashmap-access |    10000 | 0.85499419 |  1.0 |           | 1.02968490 |    547.33959199 |
|        :hashmap-access |       10 | 0.94209214 |  1.0 |           | 0.86446901 |      0.36243090 |
|  :hashmap-construction |    10000 | 0.58368385 |  1.0 |           | 0.90981799 |   1280.53668565 |
|  :hashmap-construction |       10 | 0.27196976 |  1.0 |           | 0.39814485 |      2.00588691 |
|        :hashmap-reduce |    10000 | 0.67425825 |  1.0 |           | 0.71645804 |    276.64550091 |
|        :hashmap-reduce |       10 | 0.69870403 |  1.0 |           | 0.84345349 |      0.31338739 |
|              :int-list |    20000 | 1.00000000 |      |           | 1.02671354 |    404.41071707 |
|                :mapmap |     1000 |            |  1.0 |           | 0.54012783 |    189.05377256 |
|          :object-array |    20000 |            |  1.0 |           | 0.35899448 |   1228.26172892 |
|           :object-list |    20000 | 1.00000000 |      |           | 0.98579497 |    407.32050739 |
|    :sequence-summation |    20000 |            |  1.0 | 0.4246638 | 0.29644981 |    939.32127523 |
|               :shuffle |    10000 |            |  1.0 |           | 0.43628130 |    253.45043713 |
|                  :sort |    10000 |            |  1.0 |           | 0.29134128 |   2450.00228862 |
|          :sort-doubles |    10000 |            |  1.0 |           | 0.34371458 |   2137.02632979 |
|             :sort-ints |    10000 |            |  1.0 |           | 0.30522200 |   2693.69203070 |
|                 :union |       10 | 0.12391186 |  1.0 |           | 0.07255442 |      1.67619928 |
|                 :union |    10000 | 0.27846585 |  1.0 |           | 0.20617673 |   1236.05993699 |
|            :union-disj |       10 | 0.12178287 |  1.0 |           | 0.07231682 |      1.66473353 |
|            :union-disj |    10000 | 0.27714958 |  1.0 |           | 0.20640008 |   1229.63643699 |
|          :union-reduce |       10 | 0.12646417 |  1.0 |           | 0.27270204 |     21.46739216 |
|          :union-reduce |    10000 | 0.08878439 |  1.0 |           | 0.15548128 |  31320.54125000 |
|         :update-values |     1000 |            |  1.0 |           | 0.09203954 |    134.33942224 |
|         :vector-access |       10 | 1.53932486 |  1.0 |           | 1.25173743 |     78.74334075 |
|         :vector-access |    10000 | 0.96250678 |  1.0 |           | 1.05885568 |    125.08835259 |
| :vector-cons-obj-array |       10 | 0.98403319 |  1.0 |           | 0.34879760 |      0.06972045 |
| :vector-cons-obj-array |    10000 | 0.06972108 |  1.0 |           | 0.04355003 |     88.19588311 |
|   :vector-construction |       10 | 0.47232193 |  1.0 |           | 1.11884671 |      0.06518808 |
|   :vector-construction |    10000 | 0.06432903 |  1.0 |           | 0.07511941 |     96.04142723 |
|         :vector-reduce |       10 | 2.14042268 |  1.0 |           | 1.09596576 |      0.12589151 |
|         :vector-reduce |    10000 | 1.30209905 |  1.0 |           | 0.94990447 |    133.45770263 |
|       :vector-to-array |       10 | 0.25613090 |  1.0 |           | 0.55738459 |      0.03127104 |
|       :vector-to-array |    10000 | 0.05730688 |  1.0 |           | 0.06483461 |     57.73401966 |


## Other Interesting Projects

* [clj-fast](https://github.com/bsless/clj-fast) - Great and important library more focused on compiler upgrades.
* [bifurcan](https://github.com/lacuna/bifurcan) - High speed functional datastructures for Java.  Perhaps ham-fisted should
be based on this or we should measure the differences and take the good parts.
* [Clojure Goes Fast](http://clojure-goes-fast.com/) - Grandaddy aggregator project with a lot of important information and a set of crucial github projects such as [clj-memory-meter](https://github.com/clojure-goes-fast/clj-memory-meter).

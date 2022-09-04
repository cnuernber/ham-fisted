# HAM-Fisted

What started as a collection of efficient mutable and immutable datastructures based on Phil Bagwell's
bitmap trie concept became an overall reimplementation of clojure's core datastructures and some of
its base concepts specifically with performance in mind.  This means, for instance, that the library
prefers iterators over sequences in many situations.


Here are a few concepts to keep in mind -


## In-place Mutable -> Persistent

The mutable hashmap and vector implementations allow in-place instantaneous conversion to
their persistent counterparts.  This allows you to build a dataset using the sometimes much
faster mutable primitives (.compute and friends for instance in the hashmap case) and then
return data to the rest of the program in persistent form.  Using this method, for example
`frequencies` is quite a bit faster while still returning a persistent datastructure.


## New Primitive Operations

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



#### Update-Vals, Map-Reduce

* `update-vals` - is far faster than map->map pathways if you want to update every value in the map.
* `group-by-reduce` - perform a reduction during the group-by.  This avoids keeping a large map of
   the complete intermediate values which can be both faster and more memory efficient.



#### All Arrays Are First Class

* Any array including any primitive array  can be converted to an indexed operator with efficient sort,
 reduce, etc. implementations using the `lazy-noncaching` namespace's `->random-access` operator.  This
 allows you to pass arrays as is to the rest of your clojure program without conversion to a persistent
 vector - something that is both not particularly efficient and explodes the data size.


## Other ideas

 * lazy-noncaching namespace contains very efficient implementations of map, filter, concat, and
   repeatedly which perform as good as or better than the eduction variants without chunking or
   requiring you to convert  your code from naive clojure to transducer form.  The drawback is
   they are lazy noncaching so for instance `(repeatedly 10 rand)` will produce 10 random values
   every time it is evaluated.  Furthermore `map` will produce a random-access return value
   if passed in all random-access inputs thus preserving the random-access property of the input.

 * lazy-caching namespace contains inefficient implementations that do in fact cache - it appears
   that Clojure's base implementation is very good or at least good enough I can't beat it on
   first try.


## Benchmarks
Lies, damn lies, and benchmarks - you can run the benchmarks with `./scripts/benchmark`.
Results will be printed to the console and saved to results directory prefixed by the
jdk version.

Results will print normalized to either the base time for clojure.core (clj) or for java.util (java).


#### JDK-17

|                  :test | :n-elems |      :java | :clj | :eduction |      :hamf | :norm-factor-μs |
|------------------------|---------:|-----------:|-----:|----------:|-----------:|----------------:|
|           :frequencies |    10000 |            |  1.0 |           | 0.44327118 |    882.48900146 |
|        :hashmap-access |    10000 | 0.72566545 |  1.0 |           | 0.98844489 |    593.41440927 |
|        :hashmap-access |       10 | 0.86903012 |  1.0 |           | 0.84141120 |      0.42719049 |
|  :hashmap-construction |    10000 | 0.50567684 |  1.0 |           | 0.65178507 |   1322.99841009 |
|  :hashmap-construction |       10 | 0.21854393 |  1.0 |           | 0.26656983 |      2.16976678 |
|        :hashmap-reduce |    10000 | 3.00116069 |  1.0 |           | 0.84683863 |    427.20672112 |
|        :hashmap-reduce |       10 | 3.14790926 |  1.0 |           | 0.97221159 |      0.40092921 |
|          :object-array |    20000 |            |  1.0 |           | 0.28640189 |   1494.55725952 |
|           :object-list |    20000 | 1.00000000 |      |           | 1.09193524 |    461.30426788 |
|    :sequence-summation |    20000 |            |  1.0 | 0.3241913 | 0.23726946 |   1219.68434259 |
|               :shuffle |    10000 |            |  1.0 |           | 0.35235558 |    329.12442022 |
|                  :sort |    10000 |            |  1.0 |           | 0.27909771 |   2390.10160078 |
|                 :union |       10 | 0.15651234 |  1.0 |           | 0.12114759 |      1.75881299 |
|                 :union |    10000 | 0.28105205 |  1.0 |           | 0.18423923 |   1582.29949107 |
|            :union-disj |       10 | 0.15266875 |  1.0 |           | 0.11977650 |      1.74599689 |
|            :union-disj |    10000 | 0.28060054 |  1.0 |           | 0.18108048 |   1592.90203571 |
|          :union-reduce |       10 | 0.14609406 |  1.0 |           | 0.21091066 |     25.82326151 |
|          :union-reduce |    10000 | 0.10272794 |  1.0 |           | 0.16484666 |  40203.59127778 |
|         :vector-access |       10 | 1.42548310 |  1.0 |           | 1.05209387 |     75.84182829 |
|         :vector-access |    10000 | 0.91191053 |  1.0 |           | 1.08997551 |    125.49895912 |
| :vector-cons-obj-array |       10 | 1.00666608 |  1.0 |           | 0.35989745 |      0.06908563 |
| :vector-cons-obj-array |    10000 | 0.09504760 |  1.0 |           | 0.05288363 |    113.61156127 |
|   :vector-construction |       10 | 0.40379333 |  1.0 |           | 1.24364917 |      0.06731360 |
|   :vector-construction |    10000 | 0.08888222 |  1.0 |           | 0.08026739 |    110.23913062 |
|         :vector-reduce |       10 | 2.13559087 |  1.0 |           | 1.00022842 |      0.12834871 |
|         :vector-reduce |    10000 | 1.69029271 |  1.0 |           | 1.06980900 |    145.41413508 |
|       :vector-to-array |       10 | 0.24010381 |  1.0 |           | 0.54140837 |      0.03393830 |
|       :vector-to-array |    10000 | 0.05644951 |  1.0 |           | 0.12045362 |     72.85251556 |


#### JDK-1.8


|                  :test | :n-elems |      :java | :clj |  :eduction |      :hamf | :norm-factor-μs |
|------------------------|---------:|-----------:|-----:|-----------:|-----------:|----------------:|
|           :frequencies |    10000 |            |  1.0 |            | 0.29013761 |   1419.76829167 |
|        :hashmap-access |    10000 | 0.82822470 |  1.0 |            | 1.03397205 |    604.10846285 |
|        :hashmap-access |       10 | 0.90444637 |  1.0 |            | 0.91615125 |      0.74137840 |
|  :hashmap-construction |    10000 | 0.51272927 |  1.0 |            | 0.64072877 |   1284.84978481 |
|  :hashmap-construction |       10 | 0.21395102 |  1.0 |            | 0.24092851 |      2.13185616 |
|        :hashmap-reduce |    10000 | 2.07945283 |  1.0 |            | 0.94512678 |    585.63605458 |
|        :hashmap-reduce |       10 | 2.29235414 |  1.0 |            | 0.99892708 |      0.59863955 |
|          :object-array |    20000 |            |  1.0 |            | 0.45771745 |   1327.93985931 |
|           :object-list |    20000 | 1.00000000 |      |            | 0.98758537 |    510.36504433 |
|    :sequence-summation |    20000 |            |  1.0 | 0.41848006 | 0.29718377 |   1196.08050000 |
|               :shuffle |    10000 |            |  1.0 |            | 0.42562931 |    288.41684186 |
|                  :sort |    10000 |            |  1.0 |            | 0.30652082 |   2960.92651905 |
|                 :union |       10 | 0.13078551 |  1.0 |            | 0.09074539 |      2.02183738 |
|                 :union |    10000 | 0.28582857 |  1.0 |            | 0.20127572 |   1430.96357870 |
|            :union-disj |       10 | 0.12675884 |  1.0 |            | 0.08870927 |      2.04988737 |
|            :union-disj |    10000 | 0.28530791 |  1.0 |            | 0.19895523 |   1429.99126712 |
|          :union-reduce |       10 | 0.11069936 |  1.0 |            | 0.17424071 |     27.36547261 |
|          :union-reduce |    10000 | 0.08949208 |  1.0 |            | 0.15644330 |  38177.70605556 |
|         :vector-access |       10 | 1.54854216 |  1.0 |            | 1.24872202 |     86.07043793 |
|         :vector-access |    10000 | 0.97830437 |  1.0 |            | 1.04054437 |    140.85241168 |
| :vector-cons-obj-array |       10 | 1.06081452 |  1.0 |            | 0.36975671 |      0.07545321 |
| :vector-cons-obj-array |    10000 | 0.06620962 |  1.0 |            | 0.04118232 |    104.37199155 |
|   :vector-construction |       10 | 0.48167253 |  1.0 |            | 1.10194674 |      0.07426919 |
|   :vector-construction |    10000 | 0.06377729 |  1.0 |            | 0.07212370 |    106.88897712 |
|         :vector-reduce |       10 | 1.96576730 |  1.0 |            | 0.99480268 |      0.15652553 |
|         :vector-reduce |    10000 | 1.32351174 |  1.0 |            | 0.90514764 |    151.17531719 |
|       :vector-to-array |       10 | 0.28303144 |  1.0 |            | 0.55808887 |      0.03717378 |
|       :vector-to-array |    10000 | 0.05170250 |  1.0 |            | 0.06897929 |     66.11272041 |

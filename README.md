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

|                  :test | :n-elems |      :java | :clj |  :eduction |      :hamf | :norm-factor-μs |
|------------------------|---------:|-----------:|-----:|-----------:|-----------:|----------------:|
|           :frequencies |    10000 |            |  1.0 |            | 0.38896628 |   1027.37815993 |
|              :group-by |    10000 |            |  1.0 |            | 0.32917634 |   1453.21000000 |
|       :group-by-reduce |    10000 |            |  1.0 |            | 0.27906503 |   1485.35408333 |
|        :hashmap-access |    10000 | 0.71537266 |  1.0 |            | 0.97447167 |    603.33202695 |
|        :hashmap-access |       10 | 0.88244229 |  1.0 |            | 0.82739760 |      0.41858665 |
|  :hashmap-construction |    10000 | 0.50590846 |  1.0 |            | 0.68088050 |   1312.55598932 |
|  :hashmap-construction |       10 | 0.22917396 |  1.0 |            | 0.26463496 |      2.06801133 |
|        :hashmap-reduce |    10000 | 3.24320123 |  1.0 |            | 0.95099425 |    398.40951111 |
|        :hashmap-reduce |       10 | 3.25193988 |  1.0 |            | 1.02019986 |      0.38185726 |
|          :object-array |    20000 |            |  1.0 |            | 0.35393512 |   1516.17793035 |
|           :object-list |    20000 | 1.00000000 |      |            | 0.98234882 |    566.92052637 |
|    :sequence-summation |    20000 |            |  1.0 | 0.30306355 | 0.22679079 |   1248.91443217 |
|               :shuffle |    10000 |            |  1.0 |            | 0.34580659 |    329.98814006 |
|                  :sort |    10000 |            |  1.0 |            | 0.32298545 |   2373.22547287 |
|                 :union |       10 | 0.14294036 |  1.0 |            | 0.08278486 |      1.79452006 |
|                 :union |    10000 | 0.27975509 |  1.0 |            | 0.17603817 |   1594.76731250 |
|            :union-disj |       10 | 0.14262902 |  1.0 |            | 0.08398942 |      1.77474870 |
|            :union-disj |    10000 | 0.28251380 |  1.0 |            | 0.17067417 |   1618.62242560 |
|          :union-reduce |       10 | 0.13620585 |  1.0 |            | 0.28266082 |     25.89859208 |
|          :union-reduce |    10000 | 0.11066905 |  1.0 |            | 0.18131334 |  35686.99222222 |
|         :update-values |     1000 |            |  1.0 |            | 0.08484512 |    153.50255394 |
|         :vector-access |       10 | 1.51646112 |  1.0 |            | 1.34798004 |     77.40024383 |
|         :vector-access |    10000 | 0.86970138 |  1.0 |            | 0.94683863 |    126.97946324 |
| :vector-cons-obj-array |       10 | 1.16548345 |  1.0 |            | 0.34924547 |      0.07169945 |
| :vector-cons-obj-array |    10000 | 0.07436289 |  1.0 |            | 0.04868956 |    111.70605331 |
|   :vector-construction |       10 | 0.45800320 |  1.0 |            | 1.15991749 |      0.07063700 |
|   :vector-construction |    10000 | 0.08477309 |  1.0 |            | 0.08045179 |    118.93991039 |
|         :vector-reduce |       10 | 2.71158636 |  1.0 |            | 0.93841455 |      0.09658059 |
|         :vector-reduce |    10000 | 2.06235919 |  1.0 |            | 1.15994277 |    103.59217395 |
|       :vector-to-array |       10 | 0.28848331 |  1.0 |            | 0.52687706 |      0.04121000 |
|       :vector-to-array |    10000 | 0.05581303 |  1.0 |            | 0.11771873 |     72.73604767 |

#### JDK-1.8


|                  :test | :n-elems |      :java | :clj |  :eduction |      :hamf | :norm-factor-μs |
|------------------------|---------:|-----------:|-----:|-----------:|-----------:|----------------:|
|           :frequencies |    10000 |            |  1.0 |            | 0.29192801 |   1417.66357407 |
|              :group-by |    10000 |            |  1.0 |            | 0.25621808 |   1804.98648276 |
|       :group-by-reduce |    10000 |            |  1.0 |            | 0.23608139 |   1834.87009524 |
|        :hashmap-access |    10000 | 0.78373958 |  1.0 |            | 0.98976632 |    630.75599057 |
|        :hashmap-access |       10 | 0.93007408 |  1.0 |            | 0.94510630 |      0.72297910 |
|  :hashmap-construction |    10000 | 0.52449578 |  1.0 |            | 0.64293740 |   1236.40329268 |
|  :hashmap-construction |       10 | 0.22899023 |  1.0 |            | 0.25553376 |      1.94581824 |
|        :hashmap-reduce |    10000 | 2.10334817 |  1.0 |            | 0.92089308 |    579.99355651 |
|        :hashmap-reduce |       10 | 2.17064764 |  1.0 |            | 1.02690746 |      0.59212840 |
|          :object-array |    20000 |            |  1.0 |            | 0.33618844 |   1304.97993287 |
|           :object-list |    20000 | 1.00000000 |      |            | 0.97720865 |    504.89723667 |
|    :sequence-summation |    20000 |            |  1.0 | 0.45271402 | 0.28252154 |   1149.22647778 |
|               :shuffle |    10000 |            |  1.0 |            | 0.38635412 |    285.66280337 |
|                  :sort |    10000 |            |  1.0 |            | 0.29952952 |   2982.60847143 |
|                 :union |       10 | 0.14152044 |  1.0 |            | 0.09896856 |      1.77755553 |
|                 :union |    10000 | 0.26879074 |  1.0 |            | 0.20464223 |   1419.62047685 |
|            :union-disj |       10 | 0.13826771 |  1.0 |            | 0.09392497 |      1.48793271 |
|            :union-disj |    10000 | 0.26609073 |  1.0 |            | 0.19399990 |   1433.64821991 |
|          :union-reduce |       10 | 0.11760283 |  1.0 |            | 0.20133557 |     25.67824733 |
|          :union-reduce |    10000 | 0.08996769 |  1.0 |            | 0.16128722 |  37469.90033333 |
|         :update-values |     1000 |            |  1.0 |            | 0.08177715 |    194.90107454 |
|         :vector-access |       10 | 1.52985407 |  1.0 |            | 1.27887414 |     85.72962155 |
|         :vector-access |    10000 | 0.96044315 |  1.0 |            | 1.03990899 |    140.05697056 |
| :vector-cons-obj-array |       10 | 1.15541945 |  1.0 |            | 0.35233112 |      0.06975491 |
| :vector-cons-obj-array |    10000 | 0.06553496 |  1.0 |            | 0.04084781 |    103.54848668 |
|   :vector-construction |       10 | 0.44836144 |  1.0 |            | 1.10613541 |      0.06571984 |
|   :vector-construction |    10000 | 0.06086586 |  1.0 |            | 0.07036608 |    109.66255048 |
|         :vector-reduce |       10 | 2.33587434 |  1.0 |            | 0.87173673 |      0.10786816 |
|         :vector-reduce |    10000 | 1.61702302 |  1.0 |            | 0.94336340 |    103.81785879 |
|       :vector-to-array |       10 | 0.27671906 |  1.0 |            | 0.57043719 |      0.03683125 |
|       :vector-to-array |    10000 | 0.05027912 |  1.0 |            | 0.05855303 |     66.15862978 |

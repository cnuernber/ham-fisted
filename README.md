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
`frequencies` is about 10X faster while still returning a persistent datastructure.


## New Primitive Operations 

#### Map Union, Difference, Intersection

Aside from simply a reimplementation of hashmaps and persistent vectors this library also introduces
a few new algorithms namely map-union, map-intersection, and  map-difference.  These are implemented
at the trie level so they avoid rehashing any keys and use the structure of the hashmap in order to
boost performance.  This means `merge` and `merge-with` are much faster especially if you have larger
maps.  But it also means you can design novel set-boolean operations as you provide a value-resolution
operator for the map values.


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



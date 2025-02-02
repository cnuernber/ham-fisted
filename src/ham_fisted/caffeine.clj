(ns ham-fisted.caffeine
  (:require [ham-fisted.function :as hamf-fn])
  (:import [com.github.benmanes.caffeine.cache Caffeine LoadingCache CacheLoader Cache
            RemovalCause Weigher]
           [com.github.benmanes.caffeine.cache.stats CacheStats]
           [java.time Duration]
           [java.util Map]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(defn cache
  "Create a caffeine cache.  

  Options: 

  * `:write-ttl-ms` - Time that values should remain in the cache after write in milliseconds.
  * `:access-ttl-ms` - Time that values should remain in the cache after access in milliseconds.
  * `:soft-values?` - When true, the cache will store [SoftReferences](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/SoftReference.html) to the data.
  * `:weak-values?` - When true, the cache will store [WeakReferences](https://docs.oracle.com/javase/7/docs/api/java/lang/ref/WeakReference.html) to the data.
  * `:max-size` - When set, the cache will behave like an LRU cache.
  * `:record-stats?` - When true, the LoadingCache will record access statistics.  You can
     get those via the undocumented function memo-stats.
  * `:eviction-fn` - Function that receives 3 arguments, [args v cause], when a value is
     evicted.  Causes the keywords `:collected :expired :explicit :replaced and :size`.  See
     [caffeine documentation](https://www.javadoc.io/static/com.github.ben-manes.caffeine/caffeine/2.9.3/com/github/benmanes/caffeine/cache/RemovalCause.html) for cause definitions.
  * `:weight-limited` - tuple of `[weight-fn max-weight]`.  weight-fn takes 2 args - k, v.
  * `:load-fn` - Pass in a function that will automatically compute the answer."
  ^Cache [{:keys [write-ttl-ms
                  access-ttl-ms
                  soft-values?
                  weak-values?
                  max-size
                  record-stats?
                  eviction-fn
                  weight-limited
                  load-fn]}]
  (let [^Caffeine builder
        (cond-> (Caffeine/newBuilder)
          access-ttl-ms
          (.expireAfterAccess (Duration/ofMillis access-ttl-ms))
          write-ttl-ms
          (.expireAfterWrite (Duration/ofMillis write-ttl-ms))
          soft-values?
          (.softValues)
          weak-values?
          (.weakValues)
          max-size
          (.maximumSize (long max-size))
          record-stats?
          (.recordStats)
          eviction-fn
          (.evictionListener (reify com.github.benmanes.caffeine.cache.RemovalListener
                               (onRemoval [this args v cause]
                                 (eviction-fn args v
                                              (condp identical? cause
                                                RemovalCause/COLLECTED :collected
                                                RemovalCause/EXPIRED :expired
                                                RemovalCause/EXPLICIT :explicit
                                                RemovalCause/REPLACED :replaced
                                                RemovalCause/SIZE :size
                                                (keyword (.toLowerCase (str cause)))))))))
        ^Caffeine builder (if weight-limited
                            (let [[wfn weight] weight-limited]
                              (-> (.maximumWeight builder (long weight))
                                  (.weigher (reify Weigher
                                              (weigh [_ k v] (wfn k v))))))
                            builder)]
    (if load-fn
      (.build builder (proxy [CacheLoader] []
                        (load [k] (load-fn k))))
      (.build builder))))


(defn get-if-present
  ""
  [^Cache c k]
  (.getIfPresent c k))


(defn get-or-load!
  [^Cache c k load-fn]
  (.get c k (hamf-fn/->function load-fn)))


(defn invalidate!
  "Invalidate an entry.  Returns the cache."
  [^Cache c k] (.invalidate c k) c)

(defn invalidate-all!
  "Invalidate all entries or all entries with given keys.  Returns cache."
  ([^Cache c] (.invalidateAll c) c)
  ([^Cache c ks] (.invalidateAll c (or ks '())) c))

(defn as-map
  "Return a caffeine cache as a map.  Some Useful methods are .putIfAbsent and computeIfAbsent."
  ^Map [^Cache cache]
  (.asMap cache))

(defn stats
  "Return the caffeine cache stats."
  ^CacheStats [^Cache cache]
  (.stats cache))

(defn keyword-stats
  "Return a persistent map with keyword keys and caffeine stat values. 

  Returns:
  `{:hit-count (.hitCount stats)
     :hit-rate (.hitRate stats)
     :miss-count (.missCount stats)
     :miss-rate (.missRate stats)
     :load-success-count (.loadSuccessCount stats)
     :average-load-penalty-nanos (.averageLoadPenalty stats)
     :total-load-time-nanos (.totalLoadTime stats)
     :eviction-count (.evictionCount stats)}`"
  
  [^Cache cache]
  (let [stats (stats cache)]
    {:hit-count (.hitCount stats)
     :hit-rate (.hitRate stats)
     :miss-count (.missCount stats)
     :miss-rate (.missRate stats)
     :load-success-count (.loadSuccessCount stats)
     :average-load-penalty-nanos (.averageLoadPenalty stats)
     :total-load-time-nanos (.totalLoadTime stats)
     :eviction-count (.evictionCount stats)}))

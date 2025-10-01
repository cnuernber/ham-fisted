(ns ham-fisted.bloom-filter
  "Simple fast bloom filter based on apache parquet BlockSplitBloomFilter."
  (:require [ham-fisted.protocols :as hamf-proto]
            [ham-fisted.function :as hamf-fn])
  (:import [ham_fisted BlockSplitBloomFilter]
           [java.util UUID]
           [clojure.lang IFn$OL])
  (:refer-clojure :exclude [contains?]))

(set! *warn-on-reflection* true)

(def byte-array-cls (type (byte-array 0)))

(extend-protocol hamf-proto/SerializeObjBytes
  nil
  (serialize->bytes [v] (byte-array 0))
  UUID
  (serialize->bytes [v]
    (let [^UUID v v
          bdata (byte-array 16)
          ^java.nio.ByteBuffer bbuf (-> (java.nio.ByteBuffer/wrap bdata)
                                        (.order java.nio.ByteOrder/LITTLE_ENDIAN))]
      (.putLong bbuf (.getMostSignificantBits v))
      (.putLong bbuf (.getLeastSignificantBits v))
      bdata))
  String
  (serialize->bytes [v]
    (.getBytes ^String v)))

(defn serialize->bytes
  "Serialize an object to a byte array"
  ^bytes [o]
  (if (instance? byte-array-cls o)
    o
    (hamf-proto/serialize->bytes o)))

(defn hash-obj
  "Hash an object. If integer - return integer else serialize->bytes and hash those"
  ^long [obj]
  (if (or (instance? Long obj) (instance? Integer obj) (instance? Short obj) (instance? Byte obj))
    (long obj)
    (BlockSplitBloomFilter/hash (serialize->bytes obj))))

(defn bloom-filter
  "Create a bloom filter.
  * 'n' - Number of distinct values.
  * 'f' - Value from 1.0-0.0, defaults to 0.01.  False positive rate."
  [n f]
  (BlockSplitBloomFilter. (quot (BlockSplitBloomFilter/optimalNumOfBits n f) 8)))

(defn insert-hash!
  ^BlockSplitBloomFilter [^BlockSplitBloomFilter bf ^long hc]
  (.insertHash bf hc)
  bf)

(defn make-long-hash-predicate
  [^BlockSplitBloomFilter bf]
  (hamf-fn/long-predicate hs (.findHash bf hs)))

(defn make-obj-predicate
  [^BlockSplitBloomFilter bf]
  (hamf-fn/predicate hs (.findHash bf (hash-obj hs))))

(defn contains?
  [^BlockSplitBloomFilter bf obj]
  (.findHash bf (hash-obj obj)))

(defn insert-obj
  ^BlockSplitBloomFilter [bf o]
  (if (instance? Long o)
    (insert-hash! bf (long o))
    (insert-hash! bf (BlockSplitBloomFilter/hash (serialize->bytes o)))))

(defn bitset-size
  "Return the length of the byte array underlying this bitset"
  ^long [^BlockSplitBloomFilter fb]
  (.getBitsetSize fb))

(defn bloom-filter->byte-array
  ^bytes [^BlockSplitBloomFilter bf]
  (.bitset bf))

(defn byte-array->bloom-filter
  ^BlockSplitBloomFilter [^bytes data]
  (BlockSplitBloomFilter. data))

(defn make-uuid-hasher
  ^IFn$OL []
  (let [bbuf (-> (java.nio.ByteBuffer/allocate 16)
                 (.order java.nio.ByteOrder/LITTLE_ENDIAN))
        bdata (.array bbuf)]
    (hamf-fn/obj->long
     v
     (do
       (.putLong bbuf (.getMostSignificantBits ^UUID v))
       (.putLong bbuf (.getLeastSignificantBits ^UUID v))
       (.position bbuf 0)
       (BlockSplitBloomFilter/hash bdata)))))

(defn add-uuids!
  ^BlockSplitBloomFilter [bf val-seq]
  (let [^IFn$OL hasher (make-uuid-hasher)]
    (reduce (fn [bf v]
              (if (instance? UUID v)
                (insert-hash! bf (.invokePrim hasher v))
                (throw (Exception. (str "Unsupported datatype: " (type v))))))
            bf
            val-seq)))

(defn make-uuid-pred
  [^BlockSplitBloomFilter bf]
  (let [hasher (make-uuid-hasher)]
    (fn [uuid]
      (.findHash bf (.invokePrim ^IFn$OL hasher uuid)))))

(comment

  (def M (long 1e6))
  (def uuids (vec (repeatedly M #(UUID/randomUUID))))
  (def bf (bloom-filter M 0.01))

  (reduce insert-hash! bf (map hash-obj uuids))

  (def pred (make-pred bf))

  (pred (uuids 0))

  (pred (UUID/randomUUID))

  (reduce (fn [eax _]
            (if (pred (UUID/randomUUID))
              (inc eax)
              eax))
          0
          (range M))

  (reduce (fn [eax u]
            (if (pred u)
              (inc eax)
              eax))
          0
          uuids)
  )

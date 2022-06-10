(ns ham-fisted.iterator
  "Generialized pathways involving iterators.  Sometimes useful as opposed to reductions."
  (:import [java.util Iterator]
           [java.util.stream Stream]
           [java.util.function Supplier Function BiFunction Consumer Predicate BiConsumer]
           [clojure.lang ArraySeq]
           [ham_fisted StringCollection ArrayLists]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(defn- failed-coercion-message
  ^String [item target-type]
  (format "Item type %s has no coercion to %s"
          (type item) target-type))


(defn ary-iter
  "Create an iterator for any primitive or object java array."
  ^Iterator [ary-data]
  (.iterator (ArrayLists/toList ary-data)))


(defn array-seq-iter
  ^Iterator [as]
  (if (instance? ArraySeq as)
    (let [objs (.array ^ArraySeq as)]
      (ary-iter objs))
    (.iterator ^Iterable as)))


(defn array-seq-ary
  ^objects [as]
  (if (instance? ArraySeq as)
    (.array ^ArraySeq as)
    (object-array as)))


(defn ->iterator
  "Convert a stream or an iterable into an iterator."
  ^Iterator [item]
  (cond
    (nil? item)
    nil
    (instance? Iterator item)
    item
    (instance? ArraySeq item)
    (->iterator (.array ^ArraySeq item))
    (instance? Iterable item)
    (.iterator ^Iterable item)

    (.isArray (.getClass ^Object item))
    (ary-iter item)
    (instance? CharSequence item)
    (.iterator (StringCollection. ^CharSequence item))
    (instance? Stream item)
    (.iterator ^Stream item)
    (instance? Supplier item)
    (let [^Supplier item item
          curobj* (volatile! (.get item))]
      (reify Iterator
        (hasNext [this] (not (nil? @curobj*)))
        (next [this]
          (let [curval @curobj*]
            (vreset! curobj* (.get item))
            curval))))
    :else
    (throw (Exception. (failed-coercion-message item "iterator")))))


(defmacro doiter
  "Execute body for every item in the iterable.  Expecting side effects, returns nil."
  [varname iterable & body]
  `(let [iter# (->iterator ~iterable)]
     (loop [continue?# (.hasNext iter#)]
       (when continue?#
         (let [~varname (.next iter#)]
           ~@body
           (recur (.hasNext iter#)))))))

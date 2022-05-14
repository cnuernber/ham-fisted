(ns ham-fisted.iterator
  "Generialized pathways involving iterators.  Sometimes useful as opposed to reductions."
  (:import [java.util Iterator]
           [java.util.stream Stream]
           [java.util.function Supplier Function BiFunction Consumer Predicate BiConsumer]))


(defn- failed-coercion-message
  ^String [item target-type]
  (format "Item type %s has no coercion to %s"
          (type item) target-type))


(defmacro ^:private define-array-iter
  [name ary-type]
  `(do
     (deftype ~name [~(with-meta (symbol "ary") {:tag ary-type})
                     ~(with-meta (symbol "idx") {:unsynchronized-mutable true
                                                 :tag 'long})
                     ~(with-meta (symbol "alen") {:tag 'long})]
       Iterator
       (hasNext [this] (< ~'idx ~'alen))
       (next [this] (let [retval# (aget ~'ary ~'idx)]
                      (set! ~'idx (unchecked-inc ~'idx))
                      retval#)))
     (def ~(with-meta (symbol (str name "-ary-type"))
             {:private true
              :tag 'Class}) ~(Class/forName ary-type))))


(define-array-iter ByteArrayIter "[B")
(define-array-iter ShortArrayIter "[S")
(define-array-iter CharArrayIter "[C")
(define-array-iter IntArrayIter "[I")
(define-array-iter LongArrayIter "[J")
(define-array-iter FloatArrayIter "[F")
(define-array-iter DoubleArrayIter "[D")
(define-array-iter ObjectArrayIter "[Ljava.lang.Object;")


(defn ary-iter
  "Create an iterator for any primitive or object java array."
  ^Iterator [ary-data]
  (cond
    (instance? ByteArrayIter-ary-type ary-data)
    (ByteArrayIter. ary-data 0 (alength ^bytes ary-data))
    (instance? ShortArrayIter-ary-type ary-data)
    (ShortArrayIter. ary-data 0 (alength ^shorts ary-data))
    (instance? CharArrayIter-ary-type ary-data)
    (CharArrayIter. ary-data 0 (alength ^chars ary-data))
    (instance? IntArrayIter-ary-type ary-data)
    (IntArrayIter. ary-data 0 (alength ^ints ary-data))
    (instance? LongArrayIter-ary-type ary-data)
    (LongArrayIter. ary-data 0 (alength ^longs ary-data))
    (instance? FloatArrayIter-ary-type ary-data)
    (FloatArrayIter. ary-data 0 (alength ^floats ary-data))
    (instance? DoubleArrayIter-ary-type ary-data)
    (DoubleArrayIter. ary-data 0 (alength ^doubles ary-data))
    :else
    (ObjectArrayIter. ary-data 0 (alength ^objects ary-data))))


(defn ->iterator
  "Convert a stream or an iterable into an iterator."
  ^Iterator [item]
  (cond
    (nil? item)
    nil
    (instance? Iterator item)
    item
    (instance? Iterable item)
    (.iterator ^Iterable item)
    (.isArray (.getClass ^Object item))
    (ary-iter item)
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

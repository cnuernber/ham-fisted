(ns ham-fisted.iterator
  "Generialized pathways involving iterators.  Sometimes useful as opposed to reductions."
  (:require [ham-fisted.language :refer [cond]])
  (:import [java.util Iterator]
           [java.util.stream Stream]
           [java.util.function Supplier Function BiFunction Consumer Predicate BiConsumer]
           [clojure.lang ArraySeq]
           [ham_fisted StringCollection ArrayLists MergeIterator MergeIterator$CurrentIterator
            Transformables$MapIterable])
  (:refer-clojure :exclude [cond]))


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


(defn linear-merge-iterator
  (^Iterator [cmp p iterators] (MergeIterator/createMergeIterator iterators cmp p))
  (^Iterator [cmp iterators] (MergeIterator/createMergeIterator iterators cmp))
  (^Iterator [iterators] (MergeIterator/createMergeIterator iterators compare)))


(defn priority-queue-merge-iterator
  (^Iterator [^java.util.Comparator cmp p iterators]
   (let [pq (java.util.PriorityQueue. (reify java.util.Comparator
                                        (compare [this a b]
                                          (.compare cmp (aget ^objects a 1)
                                                    (aget ^objects b 1)))))
         p (cond
             (instance? java.util.function.Predicate p)
             p
             (nil? p)
             MergeIterator/alwaysTrue
             :else
             (reify java.util.function.Predicate
               (test [this l] (boolean (p l)))))]
     (reduce (fn [_ iter]
               (when (and iter (.hasNext ^Iterator iter))
                 (let [entry (object-array [iter (.next ^Iterator iter)])]
                   (.offer pq entry))))
             nil iterators)
     (reify Iterator
       (hasNext [this] (not (.isEmpty pq)))
       (next [this]
         (loop []
           (if(.isEmpty pq)
             nil
             (let [^objects entry (.poll pq)
                   ^Iterator iter (aget entry 0)
                   rv (aget entry 1)]
               (when (.hasNext iter)
                 (aset entry 1 (.next iter))
                 (.offer pq entry))
               (if (.test ^java.util.function.Predicate p rv)
                 rv
                 (recur)))))))))
  (^Iterator [cmp iterators] (priority-queue-merge-iterator cmp MergeIterator/alwaysTrue iterators))
  (^Iterator [iterators] (priority-queue-merge-iterator compare MergeIterator/alwaysTrue iterators)))


(deftype CurrentIterator [^Iterator iter ^:unsynchronized-mutable current]
  Iterator
  (hasNext [this] (.hasNext iter))
  (next [this] (let [c (.next iter)]
                 (set! current c)
                 c))
  clojure.lang.IDeref
  (deref [this] current))


(defn current-iterator
  "Return a current iterator - and iterator that retains the current object.
  This iterator is positioned just before the first object so it's current item
  is nil."
  ^CurrentIterator [item]
  (let [iter (->iterator item)]
    (cond
      (nil? iter)
      nil
      (.hasNext iter)
      (CurrentIterator. iter nil)
      :else
      nil)))

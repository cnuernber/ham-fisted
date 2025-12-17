(ns ham-fisted.iterator
  "Generialized efficient pathways involving iterators."
  (:require [ham-fisted.language :refer [cond]]
            [ham-fisted.print :refer [implement-tostring-print]])
  (:import [java.util Iterator]
           [java.util.stream Stream]
           [java.util.function Supplier Function BiFunction Consumer Predicate BiConsumer]
           [java.util.concurrent BlockingQueue]
           [clojure.lang ArraySeq Seqable IteratorSeq]
           [ham_fisted StringCollection ArrayLists MergeIterator MergeIterator$CurrentIterator
            Transformables$MapIterable ITypedReduce Reductions Transformables])
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
  "Create a merging iterator - fast for N < 8."
  (^Iterator [cmp p iterators] (MergeIterator/createMergeIterator iterators cmp p))
  (^Iterator [cmp iterators] (MergeIterator/createMergeIterator iterators cmp))
  (^Iterator [iterators] (MergeIterator/createMergeIterator iterators compare)))


(defn priority-queue-merge-iterator
  "Create a priority queue merging iterator - fast for most N"
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

(defn- non-nil? [a] (if (nil? a) false true))

(deftype NonThreadsafeIterator [valid? init-fn update-fn ^:unsynchronized-mutable v]
  Iterator
  (hasNext [this]
    (let [rv (if (identical? v ::empty)
               (do (set! v (init-fn)) v)
               v)]
      (boolean (valid? rv))))
  (next [this]
    (let [rv (if (identical? v ::empty)
               (do (set! v (init-fn)) v)
               v)]
      (when-not (valid? rv) (throw (java.util.NoSuchElementException. "Invalid iteration")))
      (set! v (update-fn rv))
      rv)))

(defn once-iterator
  ^Iterator [valid? init-fn update-fn]
  (NonThreadsafeIterator. valid? init-fn update-fn ::empty))

(defn once-iterable
  (^Iterable [valid? init-fn update-fn]
   ;;iter defined outside of iterator fn so we can correctly survive patterns like (when (seq v) ...)
   (let [iter (once-iterator valid? init-fn update-fn)]
     (reify Iterable (iterator [this] iter))))
  (^Iterable [valid? init-fn]
   (once-iterable valid? init-fn (fn [_] (init-fn))))
  (^Iterable [init-fn]
   (once-iterable non-nil? init-fn)))

(deftype ^:private SeqOnceIterable [^Iterable iable seq-data*]
  clojure.lang.Counted
  (count [this] (count (seq this)))
  Seqable
  (seq [this]
    (vswap! seq-data*
            (fn [val]
              (if val
                val
                (IteratorSeq/create (.iterator this))))))
  ITypedReduce
  (reduce [this rfn acc]
    (if-let [seq-impl @seq-data*]
      (reduce rfn acc seq-impl)
      (Reductions/iterReduce this acc rfn)))
  Iterable
  (iterator [this]
    (if-let [ss @seq-data*]
      (.iterator ^Iterable @seq-data*)
      (.iterator iable)))
  Object
  (toString [this] (Transformables/sequenceToString (seq this))))

(implement-tostring-print SeqOnceIterable)

(defn seq-once-iterable
  "Iterable with efficient reduce but also contains a cached seq conversion so patterns like:
  (when (seq v) ...) still work"
  (^Iterable [valid? init-fn update-fn]
   (SeqOnceIterable. (once-iterable valid? init-fn update-fn) (volatile! nil)))
  (^Iterable [init-fn]
   (SeqOnceIterable. (once-iterable init-fn) (volatile! nil)))
  (^Iterable [valid? init-fn]
   (SeqOnceIterable. (once-iterable valid? init-fn) (volatile! nil))))

(defn queue->iterable
  [^BlockingQueue queue term-symbol]
  (seq-once-iterable #(not (identical? % term-symbol))
                     #(let [v (.take queue)]
                        (when (instance? Throwable v)
                          (throw (RuntimeException. "Error retrieving queue value" v)))
                        v)))

(defn const-iterable
  "Return an iterable that always returns a arg."
  ^Iterable [arg]
  (reify Iterable
    (iterator [this]
      (reify Iterator
        (hasNext [this] true)
        (next [this] arg)))))

(deftype ^:private ConsIter [^:unsynchronized-mutable v ^Iterator iter]
  Iterator
  (hasNext [this] (boolean (or (not (identical? v ::empty)) (.hasNext iter))))
  (next [this]
    (if (identical? v ::empty)
      (.next iter)
      (do (let [rv v] (set! v ::empty) rv))))
  clojure.lang.IDeref
  (deref [this] v))

(defn iter-cons
  "Produce a new iterator that points to vv then defers to passed in iterator."
  ^Iterator [vv ^Iterator iter]
  ;;attempt to keep stack of cons-iters as small as possible
  (if (and (instance? ConsIter iter) (identical? @iter ::empty))
    (iter-cons vv (.-iter ^ConsIter iter))
    (ConsIter. vv iter)))

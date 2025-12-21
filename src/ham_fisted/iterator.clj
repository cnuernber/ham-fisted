(ns ham-fisted.iterator
  "Generialized efficient pathways involving iterators."
  (:require [ham-fisted.language :refer [cond not]]
            [ham-fisted.function :as hamf-fn]
            [ham-fisted.print :refer [implement-tostring-print]])
  (:import [java.util Iterator PriorityQueue Comparator]
           [java.util.stream Stream]
           [java.util.function Supplier Function BiFunction Consumer Predicate BiConsumer]
           [java.util.concurrent BlockingQueue]
           [clojure.lang ArraySeq Seqable IteratorSeq]
           [ham_fisted StringCollection ArrayLists MergeIterator MergeIterator$CurrentIterator
            Transformables$MapIterable ITypedReduce Reductions Transformables])
  (:refer-clojure :exclude [cond not]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def ^{:arglists '[[a]]} non-nil? (hamf-fn/predicate a (if (nil? a) false true)))

(defn- failed-coercion-message
  ^String [item target-type]
  (format "Item type %s has no coercion to %s"
          (type item) target-type))


(defn ary-iter
  "Create an iterator for any primitive or object java array."
  ^Iterator [ary-data]
  (.iterator (ArrayLists/toList ary-data)))

(deftype ^:private CtxIter [valid? init-fn update-fn val-fn ^:unsynchronized-mutable ctx]
  Iterator
  (hasNext [this]
    (when (identical? ctx ::empty)
      (set! ctx (init-fn)))
    (boolean (valid? ctx)))
  (next [this]
    (when (identical? ctx ::empty)
      (set! ctx (init-fn)))
    (let [rv (val-fn ctx)]
      (set! ctx (update-fn ctx))
      rv)))

(defn ->iterator
  "Convert a stream, supplier, or an iterable into an iterator."
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
          init-update (fn ([] (.get item)) ([_] (.get item)))]
      (CtxIter. non-nil? init-update init-update identity ::empty))
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

(deftype ^:private CurrentIterator [^Iterator iter ^:unsynchronized-mutable current]
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

(defn iterable
  "Create an iterable.  init-fn is not called until the first has-next call.

  * valid? - ctx->boolean - defaults to non-nil?
  * init-fn - creates new ctx
  * update-fn - function from ctx->ctx
  * val-fn - function from ctx->val - defaults to deref"
  (^Iterable [valid? init-fn update-fn val-fn]
   (reify Iterable
     (iterator [this]
       (CtxIter. valid? init-fn update-fn val-fn ::empty))))
  (^Iterable [init-fn update-fn]
   (iterable non-nil? init-fn update-fn deref)))

(defn once-iterable
  "Create an iterable that can only be iterated once - it always returns the same (non threadsafe) iterator
  every `iterator` call.  init-fn is not called until the first has-next call - also see [[iterable]].

  The arguments have different defaults as once-iterables can close over global context on construction
  as they can only be iterated once.

  * valid? - ctx->boolean - defaults to non-nil?
  * init-fn - creates new ctx
  * update-fn - function from ctx->ctx - defaults to init-fn ignoring argument.
  * val-fn - function from ctx->val - defaults to identity"
  (^Iterable [valid? init-fn update-fn val-fn]
   ;;iterator defined outside of iterable so we can correctly survive patterns like (when (seq v) ...)
   (let [iter (.iterator (iterable valid? init-fn update-fn val-fn))]
     (reify Iterable (iterator [this] iter))))
  (^Iterable [valid? init-fn]
   (once-iterable valid? init-fn (fn [_] (init-fn)) identity))
  (^Iterable [init-fn]
   (once-iterable non-nil? init-fn)))

(deftype ^:private SeqIterable [^Iterable iable seq-data*]
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
  (toString [this] (when-let [s (seq this)]
                     (Transformables/sequenceToString (seq this)))))

(implement-tostring-print SeqIterable)

(defn seq-iterable
  "Iterable with efficient reduce but also contains a cached seq conversion so patterns like:
  (when (seq v) ...) still work without needing to dereference the iterable twice."
  ^Iterable [iterable]
  (SeqIterable. iterable (volatile! nil)))

(defn- is-not-empty?
  [^java.util.Collection c]
  (not (.isEmpty c)))

(defn- obj-aget [^objects a ^long idx] (aget a idx))

(defn merge-iterable
  "Create an efficient n-way merge between a sequence of iterables using comparator.  If iterables themselves
  are sorted result will be sorted."
  [^Comparator cmp iterables]
  (iterable
   is-not-empty?
   #(let [pq (PriorityQueue. (hamf-fn/make-comparator a b (.compare cmp (obj-aget a 1) (obj-aget b 1))))]
      (run! (fn [iable]
              (when-let [iter (->iterator iable)]
                (when (.hasNext iter)
                  (.offer pq (object-array [iter (.next iter)])))))
            iterables)
      pq)
   (fn [^PriorityQueue pq]
     (let [^objects entry (.poll pq)
           ^Iterator iter (aget entry 0)]
       (when (.hasNext iter)
         (aset entry 1 (.next iter))
         (.offer pq entry)))
     pq)
   #(obj-aget (.peek ^PriorityQueue %) 1)))

(defn blocking-queue->iterable
  "Given a blocking queue return an iterable that iterates until queue returns term-symbol.  Uses
  take to block indefinitely  --  will throw any throwable that comes out of the queue."
  ([^BlockingQueue queue term-symbol]
   (seq-iterable (once-iterable
                  #(not (identical? % term-symbol))
                  #(let [v (.take queue)]
                     (when (instance? Throwable v)
                       (throw (RuntimeException. "Error retrieving queue value" v)))
                     v))))
  ([^BlockingQueue queue timeout-us timeout-symbol term-symbol]
   (seq-iterable (once-iterable
                  #(not (identical? % term-symbol))
                  #(let [v (.poll queue timeout-us java.util.concurrent.TimeUnit/MICROSECONDS)]
                     (cond
                       (instance? Throwable v) (throw (RuntimeException. "Error retrieving queue value" v))
                       (nil? v) timeout-symbol
                       v))))))

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

(defn has-next? "Given an iterator or nil return true if the iterator has next."
  [^Iterator iter] (boolean (and iter (.hasNext iter))))

(defn maybe-next "Given an iterator or nil return the next element if the iterator hasNext."
  [^Iterator iter] (when (has-next? iter) (.next iter)))

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
  (:refer-clojure :exclude [cond not next]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(defn has-next? "Given an iterator or nil return true if the iterator has next."
  [^Iterator iter] (boolean (and iter (.hasNext iter))))

(defn next "Given an iterator call next."
  [^Iterator iter] (.next iter))

(defn maybe-next "Given an iterator or nil return the next element if the iterator hasNext."
  [^Iterator iter] (when (has-next? iter) (.next iter)))

(def ^{:arglists '[[a]]} non-nil? (hamf-fn/predicate a (if (nil? a) false true)))

(defn- failed-coercion-message
  ^String [item target-type]
  (format "Item type %s has no coercion to %s"
          (type item) target-type))


(defn ary-iter
  "Create an iterator for any primitive or object java array."
  ^Iterator [ary-data]
  (.iterator (ArrayLists/toList ary-data)))

(definterface CtxAdvance
  (advance []))

(deftype ^:private CtxIter [valid? init-fn update-fn val-fn
                            ^:unsynchronized-mutable step
                            ^:unsynchronized-mutable ctx]
  CtxAdvance
  (advance [m]
    (let [s step]
      (cond
        (identical? s ::empty-init)
        (set! ctx (init-fn))
        (identical? s ::empty-update)
        (set! ctx (update-fn ctx)))
      (set! step ::value)))
  Iterator
  (hasNext [this]
    (.advance this)
    (boolean (valid? ctx)))
  (next [this]
    (.advance this)
    (set! step ::empty-update)
    (val-fn ctx)))

(defn ctx-iter
  ^CtxIter [valid? init-fn update-fn val-fn]
  (CtxIter. valid? init-fn update-fn val-fn ::empty-init nil))

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
      (ctx-iter non-nil? init-update init-update identity))
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
       (ctx-iter valid? init-fn update-fn val-fn))))
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

(deftype ^:private SeqIterable [^Iterable iable seq-data* m]
  clojure.lang.IPersistentCollection
  (cons [_ o]
    (if-let [sq @seq-data*]
      (.cons ^clojure.lang.IPersistentCollection sq o)
      (list o)))
  (empty [_] '())
  (equiv [this o] (clojure.lang.Util/equiv (seq this) o))
  clojure.lang.Sequential
  clojure.lang.IHashEq
  (hasheq [this] (hash (or (seq this) '())))
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
  clojure.lang.IObj
  (withMeta [this new-m] (SeqIterable. iable seq-data* new-m))
  (meta [this] m)
  Object
  (toString [this] (Transformables/sequenceToString (or (seq this) '()))))

(implement-tostring-print SeqIterable)

(defn seq-iterable
  "Iterable with efficient reduce but also contains a cached seq conversion so patterns like:
  (when (seq v) ...) still work without needing to dereference the iterable twice."
  ^Iterable [iterable]
  (SeqIterable. iterable (volatile! nil) nil))

(defn seq-once-iterable
  (^Iterable [valid? init update val-fn]
   (-> (once-iterable valid? init update val-fn)
       (seq-iterable)))
  (^Iterable [valid? init]
   (-> (once-iterable valid? init)
       (seq-iterable)))
  (^Iterable [init]
   (-> (once-iterable init)
       (seq-iterable))))

(defn- is-not-empty?
  [^java.util.Collection c]
  (not (.isEmpty c)))

(defn- obj-aget [^objects a ^long idx] (aget a idx))

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

(defn dedup-first-by
  "Given a sorted sequence remove duplicates keeping first."
  ([key-fn ^Comparator cmp data]
   (let [update (fn [{:keys [iter]}]
                  (when (has-next? iter)
                    (let [vv (next iter)
                          k (key-fn vv)]
                      (loop []
                        (if (has-next? iter)
                          (let [nv (next iter)]
                            (if (== 0 (.compare cmp k (key-fn nv)))
                              (recur)
                              {:iter (iter-cons nv iter)
                               :val vv}))
                          {:val vv})))))]
     (seq-once-iterable :val #(update {:iter (->iterator data)}) update :val)))
  ([^Comparator cmp data]
   (dedup-first-by identity cmp data)))

(defn merge-iterable
  "Create an efficient stable n-way merge between a sequence of iterables using comparator.  If iterables themselves
  are sorted result will be sorted.  If two items tie then the one from the leftmost iterable wins."
  [^Comparator cmp iterables]
  (seq-iterable
   (iterable
    is-not-empty?
    #(let [pq (PriorityQueue. (hamf-fn/make-comparator a b
                                                       (let [cc (.compare cmp (obj-aget a 1) (obj-aget b 1))]
                                                         (if (== cc 0)
                                                           (.compareTo ^Comparable (obj-aget a 2) (obj-aget b 2))
                                                           cc))))]
       (loop [outer-iter (->iterator iterables)
              idx 0]
         (when (has-next? outer-iter)
           (when-let [iter (->iterator (.next outer-iter))]
             (when (has-next? iter)
               (.offer pq (object-array [iter (.next iter) idx]))))
           (recur outer-iter (inc idx))))
       pq)
    (fn [^PriorityQueue pq]
      (let [^objects entry (.poll pq)
            ^Iterator iter (aget entry 0)]
        (when (.hasNext iter)
          (aset entry 1 (.next iter))
          (.offer pq entry)))
      pq)
    #(obj-aget (.peek ^PriorityQueue %) 1))))

(defn unstable-merge-iterable
  "Create an efficient n-way merge between a sequence of iterables using comparator.  If iterables themselves
  are sorted result will be sorted."
  [^Comparator cmp iterables]
  (seq-iterable
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
    #(obj-aget (.peek ^PriorityQueue %) 1))))

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

(deftype IterTake [^Iterator iter ^long n ^{:unsynchronized-mutable true
                                            :tag long} idx]
  Iterator
  (hasNext [this] (and (.hasNext iter) (< idx n)))
  (next [this]
    (set! idx (inc idx))
    (.next iter)))

(defn iter-take "take n from an iterator returning a new iterator"
  ^Iterator [^long n coll] (IterTake. (->iterator coll) n 0))

(defn wrap-iter
  "Wrap an iterator returning an iterable."
  ^Iterable [iter]
  (-> (reify Iterable (iterator [this] (->iterator iter)))
      seq-iterable))

(defn iter-take-while
  "Returns {:data :rest*} where rest* resolves to an iterator once data has been
  completely consumed."
  [pred iter]
  (let [iter (->iterator iter)]
    (when (has-next? iter)
      (let [res (promise)
            updater (fn []
                      (try
                        (if (has-next? iter)
                          (let [v (next iter)]
                            (if (pred v)
                              v
                              (do
                                (deliver res
                                         (seq-iterable
                                          (reify Iterable
                                            (iterator [this]
                                              (if v (iter-cons v iter) iter)))))
                                nil)))
                          (do
                            (deliver res nil)
                            nil))
                        (catch Throwable e
                          (println "During take while!!" e)
                          (deliver res e)
                          e)))]
        {:data (seq-once-iterable non-nil? updater)
         :rest* res}))))

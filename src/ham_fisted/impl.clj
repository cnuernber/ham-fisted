(ns ham-fisted.impl
  (:require [ham-fisted.lazy-noncaching :refer [map concat] :as lznc]
            [ham-fisted.protocols :as protocols]
            [clojure.core.protocols :as cl-proto])
  (:import [java.util.concurrent ForkJoinPool ForkJoinTask ArrayBlockingQueue Future
            TimeUnit]
           [java.util Iterator Set Map RandomAccess]
           [java.util.concurrent ConcurrentHashMap]
           [ham_fisted ParallelOptions BitmapTrieCommon$Box ITypedReduce IFnDef
            ICollectionDef ArrayLists$ObjectArrayList Reductions$ReduceConsumer
            Reductions Transformables IFnDef$OLO ArrayLists StringCollection]
           [clojure.lang IteratorSeq IReduceInit PersistentHashMap IFn$OLO IFn$ODO Seqable
            IReduce PersistentList]
           [java.util Spliterator BitSet Collection Iterator]
           [java.util.logging Logger Level])
  (:refer-clojure :exclude [map pmap concat]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)


(defn in-fork-join-task?
  "True if you are currently running in a fork-join task"
  []
  (ForkJoinTask/inForkJoinPool))

(defrecord ^:private GroupData [^long gsize ^long ngroups ^long leftover])


(defn- n-elems->groups
  "This algorithm goes somewhat over batch size but attempts to all the indexes evenly
  between tasks."
  ^GroupData [^long parallelism ^long n-elems ^long max-batch-size]
  (let [gsize (max 1 (min max-batch-size (quot n-elems parallelism)))
        ngroups (quot n-elems gsize)
        leftover (rem n-elems gsize)
        left-blocks (quot leftover ngroups)
        gsize (+ gsize left-blocks)
        leftover (rem leftover ngroups)]
    (GroupData. gsize ngroups leftover)))


(defn- seq->lookahead
  [^long n-ahead data]
  ;;Ensure lazy-caching
  (let [data (seq data)
        n-ahead (count (take n-ahead data))]
    [data (concat (drop n-ahead data) (repeat n-ahead nil))]))


(defn n-lookahead
  ^long [^ParallelOptions options]
  (* (.-parallelism options) 2))


(defn- pgroup-submission
  [^long n-elems body-fn ^ParallelOptions options]
  (let [pool (.-pool options)
        n-elems (long n-elems)
        parallelism (.-parallelism options)
        ^GroupData gdata (n-elems->groups parallelism n-elems (.-maxBatchSize options))
        ;;_ (println parallelism gdata)
        gsize (.gsize gdata)
        ngroups (.ngroups gdata)
        leftover (.leftover gdata)]
    (->> (range ngroups)
         (map (fn [^long gidx]
                (let [start-idx (long (+ (* gidx gsize)
                                         (min gidx leftover)))
                      end-idx (min n-elems
                                   (long (+ (+ start-idx gsize)
                                            (long (if (< gidx leftover)
                                                    1 0)))))]
                  (.submit pool ^Callable #(body-fn start-idx end-idx)))))
         (seq->lookahead (n-lookahead options)))))


(defrecord ^:private ErrorRecord [e])


(defn- queue-take
  [^ArrayBlockingQueue queue]
  (let [^BitmapTrieCommon$Box b (.take queue)
        v (.-obj b)]
    (if (instance? ErrorRecord v)
      (throw (.-e ^ErrorRecord v))
      v)))


(defmacro queue-put!
  [queue v put-timeout-ms]
  ;;You cannot put nil into a queue
  `(let [data# (-> (try ~v (catch Exception e# (ErrorRecord. e#)))
                   (BitmapTrieCommon$Box.))]
     (when-not (.offer ~queue data# ~put-timeout-ms TimeUnit/MILLISECONDS)
       (let [msg# (str ":put-timeout-ms " ~put-timeout-ms
                       "ms exceeded")]
         (.warning (Logger/getLogger "ham_fisted") msg#)
         (throw (RuntimeException. msg#))))
     data#))


(defn- iter-queue->seq
  [^Iterator iter ^ArrayBlockingQueue queue]
  ;;We are taking advantage of the fact that iteratorSeq locks the iterator so all of the
  ;;code below is threadsafe once it leaves this function.
  (IteratorSeq/create (reify Iterator
                        (hasNext [this] (.hasNext iter))
                        (next [this]
                          (.next iter)
                          (queue-take queue)))))


(defn- options->queue
  ^ArrayBlockingQueue [^ParallelOptions options]
  (when-not (.-ordered options)
    (ArrayBlockingQueue. (n-lookahead options))))

(defn pgroups
  "Run y groups across n-elems.   Y is common pool parallelism.

  body-fn gets passed two longs, startidx and endidx.

  Returns a sequence of the results of body-fn applied to each group of indexes.

  Options:

  * - See the ParallelOptions java object."
  ([n-elems body-fn ^ParallelOptions options]
   (let [parallelism (.-parallelism options)
         n-elems (long n-elems)]
     (if (or (in-fork-join-task?)
             (< n-elems (.-minN options))
             (< parallelism 2))
       [(body-fn 0 n-elems)]
       (let [queue (options->queue options)
             wrapped-body-fn (if (.-ordered options)
                               body-fn
                               (fn [^long sidx ^long eidx]
                                 (queue-put! queue (body-fn sidx eidx) (.-putTimeoutMs options))))
             [submissions lookahead] (pgroup-submission n-elems wrapped-body-fn options)]
         (if (.-ordered options)
           ;;This will correctly propagate errors
           (map (fn [l r] (.get ^Future l)) submissions lookahead)
           (iter-queue->seq (.iterator ^Iterable lookahead) queue))))))
  ([n-elems body-fn]
   (pgroups n-elems body-fn nil)))


(defn pmap
  [^ParallelOptions options map-fn sequences]
  (if (in-fork-join-task?)
    (apply map map-fn sequences)
    (let [pool (.-pool options)
          parallelism (.-parallelism options)
          ;;In this case we want a caching sequence - so we call 'seq' on a lazy noncaching
          ;;map
          queue (options->queue options)
          submit-fn
          (if (.-ordered options)
            (case (count sequences)
              1 (fn [arg] (.submit pool ^Callable (fn [] (map-fn arg))))
              2 (fn [a1 a2] (.submit pool ^Callable (fn [] (map-fn a1 a2))))
              3 (fn [a1 a2 a3] (.submit pool ^Callable (fn [] (map-fn a1 a2 a3))))
              (fn [& args]
                (.submit pool ^Callable (fn [] (apply map-fn args)))))
            (case (count sequences)
              1 (fn [arg] (.submit pool ^Callable (fn [] (queue-put! queue (map-fn arg) (.-putTimeoutMs options)))))
              2 (fn [a1 a2] (.submit pool ^Callable (fn [] (queue-put! queue (map-fn a1 a2) (.-putTimeoutMs options)))))
              3 (fn [a1 a2 a3] (.submit pool ^Callable (fn [] (queue-put! queue (map-fn a1 a2 a3) (.-putTimeoutMs options)))))
              (fn [& args]
                (.submit pool ^Callable (fn [] (queue-put! queue (apply map-fn args) (.-putTimeoutMs options)))))))

          [submissions lookahead] (->> (apply map submit-fn sequences)
                                       (seq->lookahead (n-lookahead options)))]
      (if (.-ordered options)
        ;;lazy noncaching map - the future itself does the caching for us.
        (map (fn [cur read-ahead] (.get ^Future cur)) submissions lookahead)
        (iter-queue->seq (.iterator ^Iterable lookahead) queue)))))


(defn- split-spliterator
  [^long n-splits ^Spliterator spliterator]
  (let [lhs spliterator
        ;;Mutates lhs...
        rhs (.trySplit spliterator)]
    (cond
      ;;Sometimes the splits fail as we have too few elements
      (nil? rhs) [lhs]
      ;;Here we have split enough
      (<= n-splits 1) [lhs rhs]
      :else
      (let [n-splits (dec n-splits)]
        (concat (split-spliterator n-splits lhs)
                (split-spliterator n-splits rhs))))))


(defn- consume-spliterator!
  [^Reductions$ReduceConsumer c ^Spliterator s]
  (.forEachRemaining s c)
  @c)


(defn parallel-spliterator-reduce
  [initValFn rfn mergeFn ^Spliterator s ^ParallelOptions options]
  (let [pool (.-pool options)
        ;;I just want enough splits so that there is decent granularity as compared
        ;;to the parallelism of the pool.  Another more common technique is to split
        ;;until each spliterator has less than or equal to some threshold - this is used
        ;;in various places but it requires the spliterator to estimate size correctly.
        ;;This technique does not have that requirement.
        n-splits (Math/round (/ (Math/log (* 4 (.-parallelism options)))
                                (Math/log 2)))
        spliterators (split-spliterator n-splits s)
        ^ArrayBlockingQueue queue (options->queue options)
        [submissions lookahead]
        (->> spliterators
             (map (fn [spliterator]
                    (.submit pool
                             ^Callable
                             (fn []
                               (let [c (Reductions$ReduceConsumer. (initValFn) rfn)
                                     ^Spliterator s spliterator]
                                 (if (.-ordered options)
                                   (consume-spliterator! c s)
                                   (queue-put! queue (consume-spliterator! c s)
                                               (.-putTimeoutMs options))))))))
             (seq->lookahead (n-lookahead options)))]
    (->> (if (.-ordered options)
           (map (fn [l r] (.get ^Future l)) submissions lookahead)
           (map (fn [l] (queue-take queue)) lookahead))
         (Reductions/iterableMerge options mergeFn))))


(defn- bitset-reduce
  ([^BitSet coll rfn acc]
   (let [^IFn$OLO rfn (Transformables/toLongReductionFn rfn)]
     (loop [bit (.nextSetBit coll 0)
            acc acc]
       (if (and (>= bit 0) (not (reduced? acc)))
         (recur (.nextSetBit coll (unchecked-inc bit))
                (.invokePrim rfn acc (Integer/toUnsignedLong bit)))
         acc))))
  ([^BitSet coll rfn]
   (if (.isEmpty coll)
     (rfn)
     (loop [bit (.nextSetBit coll 0)
            first true
            acc nil]
       (if (and (>= bit 0) (not (reduced? acc)))
         (recur (.nextSetBit coll (unchecked-inc bit))
                false
                (if first
                  bit
                  (rfn acc (Integer/toUnsignedLong bit))))
         acc)))))


(deftype ^:private BitSetIterator [^{:unsynchronized-mutable true
                                     :tag long} setbit
                                   ^BitSet data]
  Iterator
  (hasNext [this] (not (== setbit -1)))
  (next [this] (let [retval setbit
                     nextbit (.nextSetBit data (unchecked-inc setbit))]
                 (set! setbit (long (if (== nextbit -1) -1 nextbit)))
                 retval)))


(extend-protocol protocols/ToIterable
  nil
  (convertible-to-iterable? [item] true)
  (->iterable [item] PersistentList/EMPTY)
  Object
  (convertible-to-iterable? [item] (or (instance? Iterable item)
                                       (protocols/convertible-to-collection? item)))
  (->iterable [item]
    (cond
      (instance? Iterable item)
      item
      (protocols/convertible-to-collection? item)
      (protocols/->collection item)
      :else
      (throw (RuntimeException. (str "Item is not iterable: " (type item)))))))


(extend-protocol protocols/ToCollection
  nil
  (convertible-to-collection? [item] true)
  (->collection [item] PersistentList/EMPTY)

  Object
  (convertible-to-collection? [item] (or (instance? Collection item)
                                         (instance? Map item)
                                         (instance? Seqable item)
                                         (.isArray (.getClass item))))
  (->collection [item]
    (cond
      (instance? Collection item)
      item
      (.isArray (.getClass item))
      (ArrayLists/toList item)
      (instance? Seqable item)
      (seq item)
      :else
      (throw (RuntimeException. (str "Item is not iterable:" (type item))))))
  String
  (->collection [item] (StringCollection. item))
  BitSet
  (convertible-to-collection? [item] true)
  (->collection [item]
    (reify
      ICollectionDef
      (add [c obj]
        (let [obj (int obj)
              retval (.get item obj)]
          (.set item (int obj))
          retval))
      (remove [c obj]
        (let [obj (int obj)
              retval (.get item obj)]
          (.clear item obj)
          retval))
      (clear [c] (.clear item))
      (size [c] (.cardinality item))
      (iterator [c] (BitSetIterator. (.nextSetBit item 0) item))
      (toArray [c] (let [alist (ArrayLists$ObjectArrayList. (.cardinality item))]
                     (.addAllReducible alist c)
                     (.toArray alist)))
      IReduce
      (reduce [c rfn] (bitset-reduce item rfn))
      IReduceInit
      (reduce [c rfn init] (bitset-reduce item rfn init)))))


(extend-protocol protocols/PAdd
  Collection
  (add-fn [c] #(do (.add ^Collection %1 %2) %1)))


(extend BitSet
  protocols/Reduction
  {:reducible? (constantly true)}
  cl-proto/CollReduce
  {:coll-reduce bitset-reduce})


(defn- fjfork [task] (.fork ^ForkJoinTask task))
(defn- fjjoin [task] (.join ^ForkJoinTask task))
(defn- fjtask [^Callable f] (ForkJoinTask/adapt f))


(extend-protocol protocols/ParallelReduction
  Object
  (preduce [coll init-val-fn rfn merge-fn options]
    (cond
      (instance? ITypedReduce coll)
      (.parallelReduction ^ITypedReduce coll init-val-fn rfn merge-fn options)
      (instance? RandomAccess coll)
      (Reductions/parallelRandAccessReduction init-val-fn rfn merge-fn coll options)
      (instance? IReduceInit coll)
      (Reductions/serialParallelReduction init-val-fn rfn options coll)
      (instance? Set coll)
      (Reductions/parallelCollectionReduction init-val-fn rfn merge-fn coll options)
      (instance? Map coll)
      (Reductions/parallelCollectionReduction init-val-fn rfn merge-fn
                                              (.entrySet ^Map coll) options)
      :else
      (Reductions/serialParallelReduction init-val-fn rfn options coll)))
  PersistentHashMap
  (preduce [coll init-val-fn rfn merge-fn options]
    (let [options ^ParallelOptions options
          pool (.-pool options)
          n (.-minN options)
          _ (when (.-unmergedResult options)
              (throw (RuntimeException. "Persistent hash maps do not support unmerged results")))
          combinef (fn
                     ([] (init-val-fn))
                     ([lhs rhs] (merge-fn lhs rhs)))
          fjinvoke (fn [f]
                     (if (in-fork-join-task?)
                       (f)
                       (.invoke pool ^ForkJoinTask (fjtask f))))]
      (.fold coll n combinef rfn fjinvoke fjtask fjfork fjjoin))))



(defmacro array-fast-reduce
  [ary-cls]
  `(extend ~ary-cls
     protocols/Reduction
     {:reducible? (constantly true)}
     cl-proto/CollReduce
     {:coll-reduce (fn
                     ([coll# f#]
                      (.reduce (ArrayLists/toList coll#) f#))
                     ([coll# f# acc#]
                      (.reduce (ArrayLists/toList coll#) f# acc#)))}))


(array-fast-reduce (Class/forName "[B"))
(array-fast-reduce (Class/forName "[S"))
(array-fast-reduce (Class/forName "[C"))
(array-fast-reduce (Class/forName "[I"))
(array-fast-reduce (Class/forName "[J"))
(array-fast-reduce (Class/forName "[F"))
(array-fast-reduce (Class/forName "[D"))
(array-fast-reduce (Class/forName "[Z"))
(array-fast-reduce (Class/forName "[Ljava.lang.Object;"))


(defn map-fast-reduce
  [map-cls]
  (extend map-cls
    cl-proto/CollReduce
    {:coll-reduce (fn map-reducer
                    ([coll f]
                     (Reductions/iterReduce (.entrySet ^Map coll) f))
                    ([coll f init]
                     (Reductions/iterReduce (.entrySet ^Map coll) init f)))}
    protocols/ToCollection
    {:convertible-to-collection? (constantly true)
     :->collection (fn [^Map m]
                     (reify
                       ICollectionDef
                       (size [this] (.size m))
                       (iterator [this] (.iterator (.entrySet m)))
                       IReduce
                       (reduce [this rfn]
                         (if (instance? IReduce m)
                           (.reduce ^IReduce m rfn)
                           (Reductions/iterReduce (.entrySet m) rfn)))
                       IReduceInit
                       (reduce [this rfn init]
                         (if (instance? IReduceInit m)
                           (.reduce ^IReduceInit m rfn init)
                           (Reductions/iterReduce (.entrySet m) init rfn)))))}
    protocols/ToIterable
    {:convertible-to-iterable? (constantly true)
     :->iterable (fn [m] (protocols/->collection m))}))

(map-fast-reduce java.util.HashMap)
(map-fast-reduce java.util.LinkedHashMap)
(map-fast-reduce java.util.SortedMap)
(map-fast-reduce java.util.concurrent.ConcurrentHashMap)


(defn iterable-fast-reduce
  [coll-cls]
  (extend coll-cls
    cl-proto/CollReduce
    {:coll-reduce (fn map-reducer
                    ([coll f]
                     (Reductions/iterReduce coll f))
                    ([coll f init]
                     (Reductions/iterReduce coll init f)))}))


(iterable-fast-reduce java.util.HashSet)
(iterable-fast-reduce java.util.LinkedHashSet)
(iterable-fast-reduce java.util.SortedSet)

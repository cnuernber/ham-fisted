(ns ham-fisted.impl
  (:require [ham-fisted.lazy-noncaching :refer [map concat]])
  (:import [java.util.concurrent ForkJoinPool ForkJoinTask ArrayBlockingQueue Future
            TimeUnit]
           [java.util Iterator]
           [ham_fisted ParallelOptions BitmapTrieCommon$Box]
           [clojure.lang IteratorSeq]
           [java.util Spliterator]
           [ham_fisted Reductions$ReduceConsumer Reductions]
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
    (if (<= n-splits 1)
      [lhs rhs]
      (let [n-splits (dec n-splits)]
        (concat (split-spliterator (dec n-splits) lhs)
                (split-spliterator (dec n-splits) rhs))))))


(defn- consume-spliterator!
  [^Reductions$ReduceConsumer c ^Spliterator s]
  (if (and (not (.isReduced c))
           (.tryAdvance s c))
    (recur c s)
    (deref c)))


(defn parallel-spliterator-reduce
  [initValFn rfn mergeFn ^Spliterator s ^ParallelOptions options]
  (let [pool (.-pool options)
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
         (Reductions/iterableMerge mergeFn))))

(ns ham-fisted.impl
  (:require [ham-fisted.lazy-noncaching :ref [map]])
  (:import [java.util.concurrent ForkJoinPool ForkJoinTask ArrayBlockingQueue Future]
           [java.util Iterator]
           [ham_fisted ParallelOptions]
           [clojure.lang IteratorSeq])
  (:refer-clojure :exclude [pmap]))


(defn in-fork-join-task?
  "True if you are currently running in a fork-join task"
  []
  (ForkJoinTask/inForkJoinPool))

(defrecord ^:private GroupData [^long gsize ^long ngroups ^long leftover])


(defn- n-elems->groups
  ^GroupData [^long parallelism ^long n-elems ^long max-batch-size]
  (let [gsize (max 1 (min max-batch-size (quot n-elems parallelism)))
        ngroups (quot n-elems gsize)
        leftover (rem n-elems gsize)
        left-blocks (quot leftover ngroups)
        gsize (+ gsize left-blocks)
        leftover (rem leftover ngroups)]
    (GroupData. gsize ngroups leftover)))


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
         (mapv (fn [^long gidx]
                 (let [start-idx (long (+ (* gidx gsize)
                                          (min gidx leftover)))
                       end-idx (min n-elems
                                    (long (+ (+ start-idx gsize)
                                             (long (if (< gidx leftover)
                                                     1 0)))))]
                   (.submit pool ^Callable #(body-fn start-idx end-idx))))))))


(defrecord ^:private ErrorRecord [e])


(defmacro queue-put!
  [queue v]
  `(let [data# (try ~v (catch Exception e# (ErrorRecord. e#)))]
     (.put ~queue data#)
     data#))


(defn- iter-queue->seq
  [^Iterator iter ^ArrayBlockingQueue queue]
  ;;We are taking advantage of the fact that iteratorSeq locks the iterator so all of the
  ;;code below is threadsafe once it leaves this function.
  (IteratorSeq/create (reify Iterator
                        (hasNext [this] (.hasNext iter))
                        (next [this]
                          (.next iter)
                          (let [v (.take queue)]
                            (if (instance? ErrorRecord v)
                              ;;Propagate to caller
                              (throw (.-e ^ErrorRecord v))
                              v))))))

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
       (let [^ArrayBlockingQueue queue (when-not (.-ordered options)
                                         (ArrayBlockingQueue. (* parallelism 2)))
             wrapped-body-fn (if (.-ordered options)
                               body-fn
                               (fn [^long sidx ^long eidx]
                                 (queue-put! queue (body-fn sidx eidx))))
             submissions (pgroup-submission n-elems wrapped-body-fn options)]
         (if (.-ordered options)
           ;;This will correctly propagate errors
           (map #(.get ^Future %) submissions)
           (iter-queue->seq (.iterator ^Iterable (range (count submissions))) queue))))))
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
          ^ArrayBlockingQueue queue (when-not (.-ordered options)
                                      (ArrayBlockingQueue. (* parallelism 2)))
          submit-fn
          (if (.-ordered options)
            (case (count sequences)
              1 (fn [arg] (.submit pool ^Callable (fn [] (map-fn arg))))
              2 (fn [a1 a2] (.submit pool ^Callable (fn [] (map-fn a1 a2))))
              3 (fn [a1 a2 a3] (.submit pool ^Callable (fn [] (map-fn a1 a2 a3))))
              (fn [& args]
                (.submit pool ^Callable (fn [] (apply map-fn args)))))
            (case (count sequences)
              1 (fn [arg] (.submit pool ^Callable (fn [] (queue-put! queue (map-fn arg)))))
              2 (fn [a1 a2] (.submit pool ^Callable (fn [] (queue-put! queue (map-fn a1 a2)))))
              3 (fn [a1 a2 a3] (.submit pool ^Callable (fn [] (queue-put! queue (map-fn a1 a2 a3)))))
              (fn [& args]
                (.submit pool ^Callable (fn [] (queue-put! queue (apply map-fn args)))))))

          future-seq (->> (apply map submit-fn sequences) (seq))
          n-cpu (+ (ForkJoinPool/getCommonPoolParallelism) 2)
          read-ahead (->> (concat (drop n-cpu future-seq) (repeat n-cpu nil)))]
      (if (.-ordered options)
        ;;lazy noncaching map - the future itself does the caching for us.
        (map (fn [cur read-ahead] (.get ^Future cur)) future-seq read-ahead)
        (iter-queue->seq queue (.iterator ^Iterable read-ahead))))))

(ns gen-prim-invoke
  (:require [clojure.java.io :as io]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.api :as hamf]))


(defn single-arg-sigs
  [rv]
  (for [arg1 [:o :l :d]]
    [arg1 rv]))

(defn dual-arg-sigs
  [rv arg1]
  (for [arg2 [:o :l :d]]
    [arg1 arg2 rv]))


(defn triple-arg-sigs
  [rv arg1 arg2]
  (for [arg3 [:o :l :d]]
    [arg1 arg2 arg3 rv]))


(defn quad-arg-sigs
  [rv arg1 arg2 arg3]
  (for [arg4 [:o :l :d]]
    [arg1 arg2 arg3 arg4 rv]))



(def ifn-sigs
  (hamf/concatv
   [[:l]
    [:d]]
   (->> (lznc/concat
         (for [rv [:o :l :d]]
           (single-arg-sigs rv))
         (for [rv [:o :l :d]
               arg1 [:o :l :d]]
           (dual-arg-sigs rv arg1))
         (for [rv [:o :l :d]
               arg1 [:o :l :d]
               arg2 [:o :l :d]]
           (triple-arg-sigs rv arg1 arg2))
         (for [rv [:o :l :d]
               arg1 [:o :l :d]
               arg2 [:o :l :d]
               arg3 [:o :l :d]]
           (quad-arg-sigs rv arg1 arg2 arg3)))
        lznc/apply-concat
        (lznc/remove #(every? (fn [a](= :o a)) %)))))


(defn writeit
  []
  (with-open [w (io/writer (io/output-stream "src/ham_fisted/primitive_invoke.clj"))]
    (.write w (str "(ns ham-fisted.primitive-invoke
\"For statically traced calls the Clojure compiler calls the primitive version of type-hinted functions
  and this makes quite a difference in tight loops.  Often times, however, functions are passed by values
  or returned from if-statements and then you need to explicitly call the primitive overload - this makes
  that pathway less verbose.\")\n\n"))
    (doseq [sig ifn-sigs]
      (let [sname (apply str (map name sig))
            ifn-name (str "clojure.lang.IFn$" (.toUpperCase sname))]
        (.write w (str "(defn ->" sname " ^" ifn-name " [f]
  (if (instance? " ifn-name " f)
    f
    (throw (RuntimeException. (str f \" is not an instance of" ifn-name "\")))))\n"))
        (.write w (str "(defmacro " sname " [f"))
        (dotimes [i (dec (count sig))]
          (.write w (str " "))
          (.write w (str "arg" i)))
        (.write w "]\n")
        (.write w (str "`(.invokePrim ~f"))
        (dotimes [i (dec (count sig))]
          (.write w (str " "))
          (.write w (str "~arg" i)))
        (.write w "))\n")))))


(comment
  (writeit)
  )

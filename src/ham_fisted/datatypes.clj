(ns ham-fisted.datatypes
  (:require [ham-fisted.protocols :as protocols]
            [ham-fisted.defprotocol :as hamf-defproto]
            [ham-fisted.language :as hamf-language])
  (:import [java.util Set HashSet]))

;; apply datatypes to all primitive types, array types

(defn unsigned-type?
  [dt]
  (or (identical? :uint8 dt)
      (identical? :uint16 dt)
      (identical? :uint32 dt)
      (identical? :uint64 dt)))

(defn integer-type?
  [dt]
  (or (identical? :int8 dt)
      (identical? :byte dt)
      (identical? :int16 dt)
      (identical? :short dt)
      (identical? :int32 dt)
      (identical? :int dt)
      (identical? :int64 dt)
      (identical? :long dt)
      (unsigned-type? dt)))

(defn float-type?
  [dt]
  (or (identical? :float32 dt)
      (identical? :float64 dt)))

(defn datatype->simplified-datatype
  [dt]
  (if (integer-type? dt)
    :int64
    (if (float-type? dt) :float64
        :object)))

(defn extend-datatypes
  "dtype map is a map of class to datatype"
  [dtype-map]
  (->> dtype-map
       (run!
        (fn [kv]
          (hamf-defproto/extend (key kv) protocols/Datatype
                                {:datatype (val kv)
                                 :simplified-datatype (datatype->simplified-datatype (val kv))})))))

(extend-datatypes
 {Byte/TYPE :int8
  Byte :int8
  Short/TYPE :int16
  Short :int16
  Integer/TYPE :int32
  Integer :int32
  Long/TYPE :int64
  Long :int64
  Float/TYPE :float32
  Float :float32
  Double/TYPE :float64
  Double :float64
  String :string
  clojure.lang.Keyword :keyword})

(extend-datatypes
 (into {} (map (fn [kv] [(val kv) :array])) hamf-language/array-classes))

(def java-keyword-type->datatype {:byte :int8
                                  :short :int16
                                  :int :int32
                                  :integer :int32
                                  :long :int64
                                  :float :float32
                                  :double :float64
                                  :object :object})

(defn datatype->simplified-contained-datatype
  [dt]
  (when dt
    (datatype->simplified-datatype dt)))

(defn extend-contained-datatypes
  [dtype-map]
  (->> dtype-map
       (run!
        (fn [kv]
          (hamf-defproto/extend
              (key kv) protocols/ContainedDatatype
              {:contained-datatype (val kv)
               :simplified-contained-datatype (datatype->simplified-contained-datatype (val kv))})))))

(extend-contained-datatypes
 {CharSequence :char
  Iterable :object
  java.util.Map :object
  java.util.stream.Stream :object
  java.util.stream.LongStream :int64
  java.util.stream.IntStream :int32
  java.util.stream.DoubleStream :float64})

(extend-contained-datatypes
 (into {} (map (fn [kv]
                 [(val kv) (java-keyword-type->datatypes (key kv))])
               hamf-language/array-classes)))

(defn generated-classes
  [argcount]
  (let [argcount (inc argcount)
        n-fns (long (Math/pow 3 argcount))]
    (->> (for [^long fn-idx (range n-fns)]
           (let [fn-vars
                 (loop [arg-idx 0
                        fn-idx fn-idx
                        data '()]
                   (if (< arg-idx argcount)
                     (let [local-idx (rem fn-idx 3)
                           next-idx (quot fn-idx 3)
                           data (conj data (case local-idx
                                             0 "O"
                                             1 "L"
                                             2 "D"))]
                       (recur (inc arg-idx) next-idx data))
                     data))
                 rval (last fn-vars)]
             (if (every? #(= % "O") fn-vars)
               [clojure.lang.IFn :object]
               [(Class/forName
                 (apply str "clojure.lang.IFn$" fn-vars))
                (cond
                  (= "O" rval) :object
                  (= "L" rval) :int64
                  (= "D" rval) :float64)]))))))

(def fn-classes
  (into {clojure.lang.IFn :object}
        (mapcat generated-classes)
        (range 5)))

(run!
 (fn [kv]
   (hamf-defproto/extend (key kv)
     protocols/ReturnedDatatype
     {:returned-datatype (val kv)
      :simplified-returned-datatype (datatype->simplified-datatype (val kv))}))
 fn-classes)

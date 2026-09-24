(ns ham-fisted.lznc-shape-test
  "Vector-like in, vector-like out.  lazy-noncaching operations given random access inputs
  (vectors, java lists, primitive and object arrays) must return random access results that
  preserve the input's primitive element type and reduce correctly in parallel.  Sequential
  inputs must produce correct sequential results."
  (:require [clojure.test :refer [deftest is testing]]
            [ham-fisted.lazy-noncaching :as lznc]
            [ham-fisted.api :as hamf]
            [ham-fisted.reduce :as hamf-rf])
  (:import [java.util RandomAccess List]))

(def ^:private n 20)

(def ^:private ra-inputs
  {:vector (vec (range n))
   :long-array (long-array (range n))
   :double-array (double-array (range n))
   :int-array (int-array (range n))
   :float-array (float-array (range n))
   :object-array (object-array (range n))
   :long-list (hamf/long-array-list (range n))})

(def ^:private seq-input (map identity (range n)))

(defn- ra? [x] (and (instance? RandomAccess x) (instance? List x)))

(defn- in-type [x] (lznc/contained-type (lznc/as-random-access x)))

(defn- numeric-category
  [t]
  (cond
    (#{Byte/TYPE Short/TYPE Integer/TYPE Long/TYPE} t) :integer
    (#{Float/TYPE Double/TYPE} t) :floating
    :else t))

(defn- pvec
  "Ordered parallel reduction into a vector - splits by index so exercises subList."
  [coll]
  (hamf-rf/preduce (constantly []) conj into {:min-n 2} coll))

(defn- check-ra
  [lbl result expected]
  (is (ra? result) (str lbl " is random access"))
  (is (= expected (vec result)) (str lbl " values"))
  (is (= expected (mapv #(.get ^List result (int %)) (range (count expected)))) (str lbl " get"))
  (is (= expected (pvec result)) (str lbl " parallel reduction")))

(defn- check-seq
  [lbl result expected]
  (is (= expected (vec result)) (str lbl " values"))
  (is (= (seq expected) (seq result)) (str lbl " seq"))
  (is (= expected (into [] result)) (str lbl " reduce")))

;;[name lznc-fn core-fn random-access-result? preserves-element-type?]
;;cartesian-map mutates its argument list between calls so it is copied with into - vec would
;;alias the list as hamf array lists are persistent vectors.
(def ^:private cases
  [["map" #(lznc/map str %) #(map str %) true false]
   ["map2" #(lznc/map vector % %) #(map vector % %) true false]
   ["map3" #(lznc/map vector % % %) #(map vector % % %) true false]
   ["map-indexed" #(lznc/map-indexed vector %) #(map-indexed vector %) true false]
   ["take" #(lznc/take 5 %) #(take 5 %) true true]
   ["drop" #(lznc/drop 5 %) #(drop 5 %) true true]
   ["tuple-map" #(lznc/tuple-map first %) #(map identity %) true false]
   ["tuple-map2" #(lznc/tuple-map vec % %) #(map vector % %) true false]
   ["tuple-map3" #(lznc/tuple-map vec % % %) #(map vector % % %) true false]
   ["tuple-map5" #(lznc/tuple-map vec % % % % %) #(map vector % % % % %) true false]
   ["cartesian2" #(lznc/cartesian-map (partial into []) % %) #(for [a % b %] [a b]) true false]
   ["cartesian3" #(lznc/cartesian-map (partial into []) % % %) #(for [a % b % c %] [a b c]) true false]
   ["partition-all" #(lznc/partition-all 6 %) #(partition-all 6 %) true false]
   ["partition-all step" #(lznc/partition-all 6 4 %) #(partition-all 6 4 %) true false]
   ["filter" #(lznc/filter (fn [v] (even? (long v))) %) #(filter (fn [v] (even? (long v))) %) false false]
   ["concat" #(lznc/concat % %) #(concat % %) false false]])

(defn- normalize
  "Realize nested partitions so results compare as values."
  [nm coll]
  (if (#{"partition-all" "partition-all step"} nm)
    (mapv vec coll)
    (vec coll)))

(deftest random-access-in-random-access-out
  (doseq [[in-nm input] ra-inputs
          [nm lf cf ra-result? typed?] cases]
    (testing (str nm " " in-nm)
      (let [result (lf input)
            expected (normalize nm (cf (seq input)))]
        (if ra-result?
          (do
            (check-ra (str nm " " in-nm) (if (#{"partition-all" "partition-all step"} nm)
                                           (lznc/map vec result)
                                           result)
                      expected)
            (when typed?
              (is (= (in-type input) (lznc/contained-type result))
                  (str nm " " in-nm " preserves element type"))))
          (check-seq (str nm " " in-nm) (normalize nm result) expected))))))

(deftest partition-all-inner-random-access
  (doseq [[in-nm input] ra-inputs]
    (let [inner (first (lznc/partition-all 6 input))]
      (is (ra? inner) (str in-nm " inner partition is random access"))
      (is (= (in-type input) (lznc/contained-type inner)) (str in-nm " inner partition element type")))))

(deftest sequential-in-sequential-out
  (doseq [[nm lf cf] cases]
    (testing nm
      (check-seq nm (normalize nm (lf seq-input)) (normalize nm (cf seq-input))))))

(deftest map-indexed-sublist-keeps-indexes
  (let [mi (lznc/map-indexed vector [:a :b :c :d])]
    (is (= [[1 :b] [2 :c]] (vec (.subList ^List mi 1 3))))
    (is (= [[2 :c]] (vec (.subList ^List (.subList ^List mi 1 3) 1 2))))
    ;;parallel reduction splits by subList so each split must keep its indexes
    (is (= (vec (map-indexed vector (range 100)))
           (pvec (lznc/map-indexed vector (vec (range 100))))))))

(deftest random-access-results-with-typed-elements
  (doseq [[in-nm input] (dissoc ra-inputs :vector :object-array)]
    (testing in-nm
      (let [t (in-type input)]
        ;;shift widens to long/double but must stay primitive in the same numeric category
        (is (= (numeric-category t) (numeric-category (lznc/contained-type (lznc/shift 2 input)))) "shift")
        (is (ra? (lznc/shift 2 input)) "shift random access")
        (is (= t (lznc/contained-type (lznc/reindex input [3 1 2]))) "reindex")
        (is (= t (lznc/contained-type (lznc/shuffle input))) "shuffle")
        (is (= (sort (seq input)) (sort (seq (lznc/shuffle input)))) "shuffle values")))))

(deftest repeatedly-shapes
  (let [l (lznc/repeatedly 5 (fn ^long [] 1))
        d (lznc/repeatedly 5 (fn ^double [] 1.0))
        o (lznc/repeatedly 5 (fn [] :a))]
    (is (ra? l)) (is (= Long/TYPE (lznc/contained-type l))) (is (= [1 1 1 1 1] (vec l)))
    (is (ra? d)) (is (= Double/TYPE (lznc/contained-type d))) (is (= [1.0 1.0 1.0 1.0 1.0] (vec d)))
    (is (ra? o)) (is (= [:a :a :a :a :a] (vec o)))
    (is (= 5 (count o)))
    (is (= 5 (hamf-rf/preduce (constantly 0) (fn [acc _] (inc (long acc))) + {:min-n 2} l))))
  (let [inf (lznc/repeatedly (constantly 1))]
    (is (not (ra? inf)))
    (is (not (counted? inf)))
    (is (= [1 1 1] (vec (lznc/take 3 inf))))
    (is (= [1 1 1] (into [] (take 3) inf)))))

(deftest cartesian-map-shapes
  (let [cm (lznc/cartesian-map (partial into []) [1 2] [:a :b :c])]
    (is (ra? cm))
    (is (= 6 (count cm)))
    (is (= [2 :a] (.get ^List cm 3)))
    (is (thrown? IndexOutOfBoundsException (.get ^List cm 6)))
    (is (= [[1 :b] [1 :c] [2 :a]] (vec (.subList ^List cm 1 4)))))
  (is (= 0 (count (lznc/cartesian-map (partial into []) [1 2] []))))
  (testing "sequential input stays sequential"
    (let [cm (lznc/cartesian-map (partial into []) [1 2] (map identity [:a :b]))]
      (is (not (ra? cm)))
      (is (= [[1 :a] [1 :b] [2 :a] [2 :b]] (vec cm)))))
  (testing "results larger than an int stay sequential"
    (let [big (vec (range 100000))
          cm (lznc/cartesian-map (partial into []) big big)]
      (is (not (ra? cm)))
      (is (= [[0 0] [0 1]] (vec (lznc/take 2 cm))))))
  (testing "reduction sums"
    (let [data (vec (range 50))
          expected (reduce + (for [a data b data c data] (+ a b c)))]
      (is (= expected (reduce + 0 (lznc/cartesian-map #(apply + %) data data data))))
      (is (= expected (hamf-rf/preduce (constantly 0) + + {:min-n 100}
                                       (lznc/cartesian-map #(apply + %) data data data)))))))

;;   Copyright (c) Rich Hickey. All rights reserved.  The use and distribution terms for
;;   this software are covered by the Eclipse Public License 1.0
;;   (http://opensource.org/licenses/eclipse-1.0.php) which can be found in the file
;;   epl-v10.html at the root of this distribution.  By using this software in any
;;   fashion, you are agreeing to be bound by the terms of this license.  You must not
;;   remove this notice, or any other, from this software.

;; Author: Stuart Halloway
;; Heavy modification - Chris Nuernberger
(ns ham-fisted.defprotocol-test
  (:require [ham-fisted.defprotocol-test.examples :refer :all]
            [ham-fisted.defprotocol-test.more-examples :as other]
            [ham-fisted.defprotocol :refer [defprotocol extend-type extend extend-protocol satisfies? extends?]
             :as defprotocol]
            [ham-fisted.api :as hamf]
            [ham-fisted.reduce :as hamf-rf]
            [ham-fisted.lazy-noncaching :as lznc]
            [clojure.set :as set]
            [clojure.test :refer [deftest testing are is do-report assert-expr report]])
  (:import [ham_fisted.defprotocol_test.examples ExampleInterface]
           [java.util LongSummaryStatistics]
           [java.util.function LongConsumer])
  (:refer-clojure :exclude [defprotocol extend-type extend extend-protocol satisfies? extends?])
  )

(set! *warn-on-reflection* true)

(defn causes
  [^Throwable throwable]
  (loop [causes []
         t throwable]
    (if t (recur (conj causes t) (.getCause t)) causes)))

;; this is how I wish clojure.test/thrown? worked...
;; Does body throw expected exception, anywhere in the .getCause chain?
(defmethod assert-expr 'fails-with-cause?
  [msg [_ exception-class msg-re & body :as form]]
  `(try
     ~@body
     (report {:type :fail, :message ~msg, :expected '~form, :actual nil})
     (catch Throwable t#
       (if (some (fn [cause#]
                   (and
                    (= ~exception-class (class cause#))
                    (re-find ~msg-re (.getMessage ^Throwable cause#))))
                 (causes t#))
         (report {:type :pass, :message ~msg,
                  :expected '~form, :actual t#})
         (report {:type :fail, :message ~msg,
                  :expected '~form, :actual t#})))))


(defmethod clojure.test/assert-expr 'thrown-with-cause-msg? [msg form]
  ;; (is (thrown-with-cause-msg? c re expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Also asserts that the message string of the *cause* exception matches
  ;; (with re-find) the regular expression re.
  (let [klass (nth form 1)
        re (nth form 2)
        body (nthnext form 3)]
    `(try ~@body
          (do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
          (catch ~klass e#
            (let [m# (if (.getCause e#) (.. e# getCause getMessage) (.getMessage e#))]
              (if (re-find ~re m#)
                (do-report {:type :pass, :message ~msg,
                            :expected '~form, :actual e#})
                (do-report {:type :fail, :message ~msg,
                            :expected '~form, :actual e#})))
            e#))))


(defn method-names
  "return sorted list of method names on a class"
  [c]
  (->> (.getMethods ^Class c)
       (map #(.getName ^java.lang.reflect.Method %))
       (sort)))

(defrecord EmptyRecord [])
(defrecord TestRecord [a b])
(defn r
  ([a b] (TestRecord. a b))
  ([a b meta ext] (TestRecord. a b meta ext)))
(defrecord MapEntry [k v]
  java.util.Map$Entry
  (getKey [_] k)
  (getValue [_] v))

(deftest protocols-test
  (testing "protocol fns have useful metadata"
    (let [common-meta {:ns (find-ns 'ham-fisted.defprotocol-test.examples)
                       :tag nil}]
      (are [m f] (= (merge common-meta m)
                    (dissoc (meta (var f))
                            :line :column :file :hamf-protocol))
        {:name 'foo :arglists '([a]) :doc "method with one arg"} foo
        {:name 'bar :arglists '([a b]) :doc "method with two args"} bar
        {:name 'baz :arglists '([a] [a b]) :doc "method with multiple arities" :tag 'java.lang.String} baz
        {:name 'with-quux :arglists '([a]) :doc "method name with a hyphen"} with-quux)))
  (testing "protocol fns throw IllegalArgumentException if no impl matches"
    (is (thrown-with-msg?
         IllegalArgumentException
         #"No implementation of method: :foo of protocol: #'ham-fisted.defprotocol-test.examples/ExampleProtocol found for class: java.lang.Long"
         (foo 10))))
  (testing "protocols generate a corresponding interface using _ instead of - for method names"
    (is (= ["bar" "baz" "baz" "foo" "with_quux"] (method-names ham_fisted.defprotocol_test.examples.ExampleProtocol))))
  (testing "protocol will work with instances of its interface (use for interop, not in Clojure!)"
    (let [obj (proxy [ham_fisted.defprotocol_test.examples.ExampleProtocol] []
                (foo [] "foo!"))]
      (is (= "foo!" (.foo obj)) "call through interface")
      (is (= "foo!" (foo obj)) "call through protocol")))
  (testing "you can implement just part of a protocol if you want"
    (let [obj (reify ExampleProtocol
                (baz [a b] "two-arg baz!"))]
      (is (= "two-arg baz!" (baz obj nil)))
      (is (thrown? AbstractMethodError (baz obj)))))
  (testing "error conditions checked when defining protocols"
    (is #_{:clj-kondo/ignore [:unresolved-symbol]}
        (thrown-with-cause-msg?
         Exception
         #"Definition of function m in protocol badprotdef must take at least one arg."
         (eval '(defprotocol badprotdef (m [])))))
    (is #_{:clj-kondo/ignore [:unresolved-symbol]}
        (thrown-with-cause-msg?
         Exception
         #"Function m in protocol badprotdef was redefined. Specify all arities in single definition."
         (eval '(defprotocol badprotdef (m [this arg]) (m [this arg1 arg2]))))))
  (testing "you can redefine a protocol with different methods"
    (eval '(defprotocol Elusive (old-method [x])))
    (eval '(defprotocol Elusive (new-method [x])))
    (is (= :new-method (eval '(new-method (reify Elusive (new-method [x] :new-method))))))
    (is #_{:clj-kondo/ignore [:unresolved-symbol]}
        (fails-with-cause? IllegalArgumentException #"No method of interface: .*\.Elusive found for function: old-method of protocol: Elusive \(The protocol method may have been defined before and removed\.\)"
                           (eval '(old-method (reify Elusive (new-method [x] :new-method))))))))

(deftype HasMarkers []
  ExampleProtocol
  (foo [this] "foo")
  MarkerProtocol
  MarkerProtocol2)

(deftype WillGetMarker []
  ExampleProtocol
  (foo [this] "foo"))

(extend-type WillGetMarker MarkerProtocol)

(deftest marker-tests
  (testing "That a marker protocol has no methods"
    (is (= '() (method-names ham_fisted.defprotocol_test.examples.MarkerProtocol))))
  (testing "That types with markers are reportedly satifying them."
    (let [hm (HasMarkers.)
          wgm (WillGetMarker.)]
      (is (satisfies? MarkerProtocol hm))
      (is (satisfies? MarkerProtocol2 hm))
      (is (satisfies? MarkerProtocol wgm)))))

(deftype ExtendTestWidget [name])
(deftype HasProtocolInline []
  ExampleProtocol
  (foo [this] :inline))
(deftest extend-test
  (testing "you can extend a protocol to a class"
    (extend String ExampleProtocol
            {:foo identity})
    (is (= "pow" (foo "pow"))))
  (testing "you can have two methods with the same name. Just use namespaces!"
    (extend String other/SimpleProtocol
            {:foo (fn [s] (.toUpperCase ^String s))})
    (is (= "POW" (other/foo "pow"))))
  (testing "you can extend deftype types"
    (extend
        ExtendTestWidget
      ExampleProtocol
      {:foo (fn [this] (str "widget " (.name ^ExtendTestWidget this)))})
    (is (= "widget z" (foo (ExtendTestWidget. "z"))))))

(deftest illegal-extending
  (testing "you cannot extend a protocol to a type that implements the protocol inline"
    (is #_{:clj-kondo/ignore [:unresolved-symbol]}
        (fails-with-cause? IllegalArgumentException #".*HasProtocolInline already directly implements interface"
                           (eval '(extend ham_fisted.defprotocol_test.HasProtocolInline
                                    ham-fisted.defprotocol-test.examples/ExampleProtocol
                                    {:foo (fn [_] :extended)})))))
  (testing "you cannot extend to an interface"
    (is #_{:clj-kondo/ignore [:unresolved-symbol]}
        (fails-with-cause? IllegalArgumentException #"interface ham_fisted.defprotocol_test.examples.ExampleProtocol is not a protocol"
                           (eval '(extend ham_fisted.defprotocol_test.HasProtocolInline
                                    ham_fisted.defprotocol_test.examples.ExampleProtocol
                                    {:foo (fn [_] :extended)}))))))


                                        ; see CLJ-845
(defprotocol SyntaxQuoteTestProtocol
  (sqtp [p]))

(defmacro try-extend-type [c]
  `(extend-type ~c
     SyntaxQuoteTestProtocol
     (sqtp [p#] p#)))

(defmacro try-extend-protocol [c]
  `(extend-protocol SyntaxQuoteTestProtocol
     ~c
     (sqtp [p#] p#)))

(try-extend-type String)
(try-extend-protocol clojure.lang.Keyword)

(deftest test-no-ns-capture
  (is (= "foo" (sqtp "foo")))
  (is (= :foo (sqtp :foo))))

(defprotocol Dasherizer
  (-do-dashed [this]))
(deftype Dashed []
  Dasherizer
  (-do-dashed [this] 10))

(deftest test-leading-dashes
  (is (= 10 (-do-dashed (Dashed.))))
  (is (= [10] (map -do-dashed [(Dashed.)]))))

;; see CLJ-1879

(deftest test-base-reduce-kv
  (is (= {1 :a 2 :b}
         (reduce-kv #(assoc %1 %3 %2)
                    {}
                    (seq {:a 1 :b 2})))))

(defn aget-long-hinted ^long [x] (aget (longs-hinted x) 0))

(deftest test-longs-hinted-proto
  (is (= 1
         (aget-long-hinted
          (reify LongsHintedProto
            (longs-hinted [_] (long-array [1])))))))

;; CLJ-1180 - resolve type hints in protocol methods

(import 'clojure.lang.ISeq)
(defprotocol P
  (^ISeq f [_]))

;;; continues in defprotocol_test/other.clj

(defprotocol Ecount (^long ecount [m]))
(extend Object Ecount {:ecount 5})
(extend Number Ecount {:ecount (fn ^long [m] 0)})
(extend-type Long Ecount (ecount [m] 10))

;;Hamf protocols have to work with extend-type and reify
(deftype TestType []
  Ecount
  (ecount [this] 200))

(deftest primitive-hinted-test
  (is (== 5 (ecount :a)))
  (is (== 0 (ecount 1.0)))
  (is (== 10 (ecount 1)))
  (is (== 200 (ecount (TestType.))))
  (is (thrown? Exception (eval '(clojure.core/extend-type String
                                  Ecount
                                  (ecount [m] (.length m)))))))

(defprotocol SaneInheritance
  (a [r])
  (b [r]))

(extend-type Object
  SaneInheritance
  (a [r] :object)
  (b [r] :object))

(extend-type String
  SaneInheritance
  (b [r] :string))

(deftest sane-inheritance-test
  (is (= :object (a "hey")))
  (is (= :string (b "hey"))))


(defprotocol HamfMemsize
  (^long hamf-memsize [m]))

(extend Double HamfMemsize {:hamf-memsize 24})
(extend Long HamfMemsize {:hamf-memsize 24})
(extend clojure.lang.Keyword HamfMemsize {:hamf-memsize 48})

(extend-protocol HamfMemsize
  String
  (hamf-memsize [s] (+ 24 (.length ^String s)))
  java.util.Collection
  (hamf-memsize [c] (hamf/lsum (lznc/map (fn ^long [d] (+ 24 (hamf-memsize d))) c)))
  java.util.Map
  (hamf-memsize [c] (hamf/lsum (lznc/map (fn ^long [kv]
                                           (+ 36 (+ (hamf-memsize (key kv)) (hamf-memsize (val kv)))))
                                         c))))

(clojure.core/defprotocol CoreMemsize
  (core-memsize [m]))

(clojure.core/extend-protocol CoreMemsize
  Double (core-memsize [d] 24)
  Long (core-memsize [l] 24)
  clojure.lang.Keyword (core-memsize [k] 48)
  String (core-memsize [s] (+ 24 (.length ^String s)))
  java.util.Collection
  (core-memsize [c]
    (hamf/lsum (lznc/map (fn ^long [d] (+ 24 (long (core-memsize d)))) c))
    #_(reduce
       (fn [s v] (+ s 24 (core-memsize v)))
       0
       c))
  java.util.Map
  (core-memsize [m]
    (hamf/lsum (lznc/map (fn ^long [kv]
                           (+ 36 (+ (long (core-memsize (key kv)))
                                    (long (core-memsize (val kv))))))
                         m))
    #_(reduce
       (fn [s [k v]] (+ s 36 (core-memsize k) (core-memsize v)))
       0
       m)))

(def test-datastructure
  {:a "hello"
   :b 24
   :c (into [] (repeat 1000 (rand)))
   :d (into [] (repeat 1000 1))})


(def measure-data (into [] (repeat 10000 test-datastructure)))

(defn multithread-test
  [measure-fn]
  (hamf/lsum (hamf/pmap measure-fn measure-data)))


(defn preduce-multithread-test
  [measure-fn]
  (.getSum ^LongSummaryStatistics
           (hamf-rf/preduce #(LongSummaryStatistics.)
                            (fn [^LongConsumer l m]
                              (.accept l (long (measure-fn m)))
                              l)
                            (fn [^LongSummaryStatistics l ^LongSummaryStatistics r]
                              (.combine l r)
                              l)
                            measure-data)))

(comment
  ;;Single threaded calls show very little difference if any:

  (crit/quick-bench (core-memsize test-datastructure))
  ;; Evaluation count : 23868 in 6 samples of 3978 calls.
  ;;              Execution time mean : 25.018494 µs
  ;;     Execution time std-deviation : 26.939284 ns
  ;;    Execution time lower quantile : 24.969740 µs ( 2.5%)
  ;;    Execution time upper quantile : 25.046386 µs (97.5%)
  ;;                    Overhead used : 1.539613 ns

  ;; Multithreaded calls however:

  (crit/quick-bench (hamf-memsize test-datastructure))
  ;; Evaluation count : 22734 in 6 samples of 3789 calls.
  ;;              Execution time mean : 26.336031 µs
  ;;     Execution time std-deviation : 202.486707 ns
  ;;    Execution time lower quantile : 25.953198 µs ( 2.5%)
  ;;    Execution time upper quantile : 26.466131 µs (97.5%)
  ;;                    Overhead used : 1.539613 ns

  (crit/quick-bench (multithread-test core-memsize))
  ;; Evaluation count : 6 in 6 samples of 1 calls.
  ;;              Execution time mean : 634.046041 ms
  ;;     Execution time std-deviation : 9.874821 ms
  ;;    Execution time lower quantile : 622.889124 ms ( 2.5%)
  ;;    Execution time upper quantile : 646.951202 ms (97.5%)
  ;;                    Overhead used : 1.539613 ns

  (crit/quick-bench (multithread-test hamf-memsize))
  ;; Evaluation count : 18 in 6 samples of 3 calls.
  ;;              Execution time mean : 45.689563 ms
  ;;     Execution time std-deviation : 1.272542 ms
  ;;    Execution time lower quantile : 44.302374 ms ( 2.5%)
  ;;    Execution time upper quantile : 47.396205 ms (97.5%)
  ;;                    Overhead used : 1.539613 ns


  )

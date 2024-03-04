;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; Author: Stuart Halloway

(ns ham-fisted.defprotocol-test
  (:require [ham-fisted.defprotocol-test.examples :refer :all]
            [ham-fisted.defprotocol-test.more-examples :as other]
            [ham-fisted.defprotocol :refer [defprotocol extend-type extend extend-protocol satisfies? extends?]]
            [clojure.set :as set]
            [clojure.test :refer [deftest testing are is do-report assert-expr report]])
  (:import [ham_fisted.defprotocol_test.examples ExampleInterface])
  (:refer-clojure :exclude [defprotocol extend-type extend extend-protocol satisfies? extends?])
  )

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
                  (re-find ~msg-re (.getMessage cause#))))
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
  (->> (.getMethods c)
       (map #(.getName %))
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
     {:foo (fn [s] (.toUpperCase s))})
    (is (= "POW" (other/foo "pow"))))
  (testing "you can extend deftype types"
    (extend
     ExtendTestWidget
     ExampleProtocol
     {:foo (fn [this] (str "widget " (.name this)))})
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

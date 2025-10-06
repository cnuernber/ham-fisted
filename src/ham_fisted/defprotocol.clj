(ns ham-fisted.defprotocol
  "Alternative protocol implementation that has better semantics w/r/t runtime startup times
  and overall work done during extension and lookup.  We want to avoid dynamic variable definitions
  and prefer normal def, defn definitions which themselves respond to static linking.  This continues
  work on cnuernber/clojure attempting to dramatically decrease startup times.

  There are 4 major features of this implementation:

  1. Allows subclasses to override only a subset of the methods and if the superclass
  has overridden the method then the superclasses implementation will be used.

  2. Allows constants to be used (only with extend) - using a constant will avoid a function call
  and the constant itself is simply returned.

  3. Supports primitive typehints on function arguments and return values.

  4. Much higher and more predictable multithreaded performance for protocol method invocation due to
  the fewer number of global variables that are read and written to for a single protocol method
  invocation."
  (:refer-clojure :exclude [defprotocol extend extend-type extend-protocol extends? satisfies?
                            find-protocol-method find-protocol-impl extenders])
  (:require [ham-fisted.primitive-invoke :as primitive-invoke])
  (:import [ham_fisted MethodImplCache Casts]))

(set! *warn-on-reflection* true)

(defn find-protocol-cache-method
  [protocol ^MethodImplCache cache x]
  (when cache
    (let [cc (if (class? x) x (class x))]
      (if (.isAssignableFrom (.-iface cache) cc)
        (.-ifaceFn cache)
        (if-let [mfn (when (get protocol :extend-via-metadata)
                       (get (meta x) (.-ns_methodk cache)))]
          mfn
          (.findFnFor cache cc))))))

(defn find-protocol-method
  "It may be more efficient in a tight loop to bypass the protocol dispatch on a per-call basis."
  ([protocol methodk x]
   (find-protocol-cache-method protocol @(get (get protocol :method-caches) methodk) x)))

(defn- protocol?
  [maybe-p]
  (boolean (:on-interface maybe-p)))

(defn- implements? [protocol atype]
  (and atype (.isAssignableFrom ^Class (:on-interface protocol) atype)))

(defn extends?
  "Returns true if atype extends protocol"
  [protocol atype]
  (boolean (or (implements? protocol atype)
               (get (:impls protocol) atype))))

(defn extenders
  "Returns a collection of the types explicitly extending protocol"
  [protocol]
  (keys (:impls protocol)))

(defn satisfies?
  "Returns true if x satisfies the protocol"
  [protocol x]
  (or (instance? (get protocol :on-interface) x)
      (every? #(boolean (find-protocol-cache-method protocol % @x))
              (vals (get protocol :method-caches)))))

(defn- assert-same-protocol [protocol-var method-syms]
  (doseq [m method-syms]
    (let [v (resolve m)
          p (:protocol (meta v))]
      (when (and v (bound? v) (not= protocol-var p))
        (binding [*out* *err*]
          (println "Warning: protocol" protocol-var "is overwriting"
                   (if p
                     (str "method " (.sym ^clojure.lang.Var v) " of protocol " (.sym ^clojure.lang.Var p))
                     (str "function " (.sym ^clojure.lang.Var v)))))))))

(defn ^:no-doc find-fn
  [target ^MethodImplCache cache ns protocol]
  (let [rv (.findFnFor cache (class target))]
    (if-not (nil? rv)
      rv
      (throw (IllegalArgumentException. (format
                                         "No implementation of method: %s of protocol: #'%s/%s found for class: %s"
                                         (.-methodk cache)
                                         ns
                                         protocol
                                         (if-let [c (class target)]
                                           (.getName ^Class c)
                                           "nil")))))))

;;Instance check is already taken care of
(defn ^:no-doc find-fn-via-metadata
  [target ns-method cache ns protocol]
  (if-let [f (get (meta target) ns-method)]
    f
    (find-fn target cache ns protocol)))

(defn ^:no-doc invoker-for-tags
  [arg-tags]
  (-> (apply str "ham-fisted.primitive-invoke/" (map (fn [arg-tag]
                                                       (cond
                                                         (= 'long arg-tag) "l"
                                                         (= 'double arg-tag) "d"
                                                         :else "o"))
                                                     arg-tags))
      symbol))

(defn ^:no-doc fn-tag-for-tags
  [arg-tags]
  (-> (apply str "clojure.lang.IFn$" (map (fn [arg-tag]
                                            (cond
                                              (= 'long arg-tag) "L"
                                              (= 'double arg-tag) "D"
                                              :else "O"))
                                          arg-tags))
      symbol))

(defn- emit-protocol [name opts+sigs]
  (let [iname (symbol (str (munge (namespace-munge *ns*)) "." (munge name)))
        [opts sigs]
        (loop [opts {:on (list 'quote iname) :on-interface iname} sigs opts+sigs]
          (condp #(%1 %2) (first sigs)
            string? (recur (assoc opts :doc (first sigs)) (next sigs))
            keyword? (recur (assoc opts (first sigs) (second sigs)) (nnext sigs))
            [opts sigs]))
        sigs (when sigs
               (reduce (fn [m s]
                         (let [tag-to-class (fn [tag]
                                              (if-let [c (and (instance? clojure.lang.Symbol tag)
                                                              (= (.indexOf (.getName ^clojure.lang.Symbol tag) ".") -1)
                                                              (not (contains? '#{int long float double char short byte boolean void
                                                                                 ints longs floats doubles chars shorts bytes booleans objects} tag))
                                                              (resolve tag))]
                                                (symbol (.getName ^Class c))
                                                tag))
                               name-meta (update-in (meta (first s)) [:tag] tag-to-class)
                               mname (with-meta (first s) nil)
                               [arglists doc]
                               (loop [as [] rs (rest s)]
                                 (if (vector? (first rs))
                                   (recur (conj as (first rs)) (next rs))
                                   [(seq as) (first rs)]))
                               name-kwd (keyword mname)
                               mname (vary-meta mname assoc
                                                :doc doc
                                                :arglists arglists
                                                :tag (:tag name-meta))]
                           (when (some #{0} (map count arglists))
                             (throw (IllegalArgumentException. (str "Definition of function " mname " in protocol " name " must take at least one arg."))))
                           (when (m (keyword mname))
                             (throw (IllegalArgumentException. (str "Function " mname " in protocol " name " was redefined. Specify all arities in single definition."))))
                           (assoc m (keyword mname)
                                  (merge name-meta
                                         {:name mname
                                          :methodk name-kwd
                                          :ns-methodk (keyword (clojure.core/name (.-name *ns*))
                                                               (clojure.core/name mname))
                                          :arglists arglists
                                          :doc doc
                                          :cache-sym (symbol (str "-" mname "-cache"))
                                          :iface-sym (symbol (str "-" mname "-iface"))}))))
                       {} sigs))
        meths (mapcat (fn [sig]
                        (let [m (munge (:name sig))
                              m-tag (or (:tag (meta (:name sig))) 'Object)]
                          (map #(vector m (vec (repeat (dec (count %)) 'Object)) m-tag)
                               (:arglists sig))))
                      (vals sigs))
        opts (assoc opts :sigs sigs)
        name (if-let [proto-doc (:doc opts)]
               (with-meta name {:doc proto-doc})
               name)]
    `(do
       (gen-interface :name ~iname :methods ~meths)
       ~@(mapcat (fn [{:keys [methodk ns-methodk cache-sym iface-sym arglists tag]
                       mname :name}]
                   [`(defn ~(with-meta iface-sym
                              {:private true
                               :tag (list 'quote tag)})
                       ~@(map (fn [args]
                                (let [args (vec args) #_(mapv #(gensym (str %)) args)
                                      args (vary-meta (vec args) assoc :tag
                                                      (if (class? tag)
                                                        (list 'quote )
                                                        tag))
                                      target (first args)]
                                  `(~args
                                    (. ~(with-meta target
                                          {:tag iname})
                                       (~mname
                                        ~@(rest args))))))
                              arglists))
                    `(let [~'cache (ham_fisted.MethodImplCache. ~methodk ~ns-methodk ~iname ~iface-sym)]
                       (def ~(with-meta cache-sym
                               {:private true
                                :tag 'ham_fisted.MethodImplCache})
                         ~'cache)
                       (defn ~(vary-meta mname assoc :tag (list 'quote tag))
                         {:hamf-protocol ~(list 'quote name)}
                         ~@(map (fn [args]
                                  (let [args (vary-meta (vec args) assoc :tag
                                                        (if (class? tag)
                                                          (list 'quote )
                                                          tag))
                                        arg-tags (when (< (count args) 5)
                                                   (conj (mapv (comp :tag meta) args) tag))
                                        rval-tag (last arg-tags)
                                        invoker (when (first (filter #{'long 'double} arg-tags))
                                                  (invoker-for-tags arg-tags))
                                        target (first args)
                                        find-data (if (:extend-via-metadata opts)
                                                    `(find-fn-via-metadata ~target
                                                                           ~ns-methodk
                                                                           ~'cache
                                                                           ~(list 'quote (.-name *ns*))
                                                                           ~(list 'quote name))
                                                    `(find-fn ~target ~'cache ~(list 'quote (.-name *ns*))
                                                              ~(list 'quote name)))]
                                    `(~args
                                      ~(if invoker
                                         (cond
                                           (= rval-tag 'long)
                                           `(let [~'ff ~find-data]
                                              (if (instance? Long ~'ff)
                                                (unchecked-long ~'ff)
                                                (~invoker ~(with-meta 'ff {:tag (fn-tag-for-tags arg-tags)})
                                                 ~@args)))
                                           (= rval-tag 'double)
                                           `(let [~'ff ~find-data]
                                              (if (instance? Double~'ff)
                                                (unchecked-double ~'ff)
                                                (~invoker ~(with-meta 'ff {:tag (fn-tag-for-tags arg-tags)})
                                                 ~@args)))
                                           :else `(~invoker find-data @args))
                                         `(let [~'ff ~find-data]
                                            (if (fn? ~'ff)
                                              (~'ff ~@args)
                                              ~'ff))))))
                                arglists)))])
                 (vals sigs))
       (def ~name ~(assoc (update opts
                                  :sigs (fn [sigmap]
                                          (->> sigmap
                                               (map (fn [e]
                                                      [(key e) (-> (select-keys (val e) [:name :doc :arglists :tag])
                                                                   (update :arglists
                                                                           (fn [arglists]
                                                                             (mapv (fn [arglist]
                                                                                     (mapv (fn [sym]
                                                                                             (list 'quote sym))
                                                                                           arglist))
                                                                                   arglists)))
                                                                   (update :tag (fn [t] (when t (list 'quote t))))
                                                                   (update :name #(list 'quote %)))]))
                                               (into {}))))
                          :method-caches (->> (map (fn [{:keys [methodk cache-sym]}]
                                                     ;;Subtle issue here if you dereference the var --
                                                     ;;if the file is reloaded you can get protocol referenes that
                                                     ;;point to the wrong cache var.
                                                     [methodk (list 'var cache-sym)])
                                                   (vals sigs))
                                              (into {}))
                          ;;No more alter-var-root -- unnecessary
                          :impls `(atom {}))))))

(defmacro defprotocol
  "A protocol is a named set of named methods and their signatures:
  (defprotocol AProtocolName

    ;optional doc string
    \"A doc string for AProtocol abstraction\"

   ;options
   :extend-via-metadata true

  ;method signatures
    (bar [this a b] \"bar docs\")
    (baz [this a] [this a b] [this a b c] \"baz docs\"))

  No implementations are provided. Docs can be specified for the
  protocol overall and for each method. The above yields a set of
  polymorphic functions and a protocol object. All are
  namespace-qualified by the ns enclosing the definition The resulting
  functions dispatch on the type of their first argument, which is
  required and corresponds to the implicit target object ('this' in
  Java parlance). defprotocol is dynamic, has no special compile-time
  effect, and defines no new types or classes. Implementations of
  the protocol methods can be provided using extend.

  When :extend-via-metadata is true, values can extend protocols by
  adding metadata where keys are fully-qualified protocol function
  symbols and values are function implementations. Protocol
  implementations are checked first for direct definitions (defrecord,
  deftype, reify), then metadata definitions, then external
  extensions (extend, extend-type, extend-protocol)

  defprotocol will automatically generate a corresponding interface,
  with the same name as the protocol, i.e. given a protocol:
  my.ns/Protocol, an interface: my.ns.Protocol. The interface will
  have methods corresponding to the protocol functions, and the
  protocol will automatically work with instances of the interface.

  Note that you should not use this interface with deftype or
  reify, as they support the protocol directly:

  (defprotocol P
    (foo [this])
    (bar-me [this] [this y]))

  (deftype Foo [a b c]
   P
    (foo [this] a)
    (bar-me [this] b)
    (bar-me [this y] (+ c y)))

  (bar-me (Foo. 1 2 3) 42)
  => 45

  (foo
    (let [x 42]
      (reify P
        (foo [this] 17)
        (bar-me [this] x)
        (bar-me [this y] x))))
  => 17"
  {:added "1.2"}
  [name & opts+sigs]
  (emit-protocol name opts+sigs))

(defn correct-primitive-fn-type
  [arg-tags-v method]
  (doseq [arg-tags arg-tags-v]
    (when (first (filter #{'long 'double} arg-tags))
      (let [prim-cls (Class/forName (apply str "clojure.lang.IFn$"
                                           (map (fn [tag]
                                                  (cond
                                                    (= tag 'long) "L"
                                                    (= tag 'double) "D"
                                                    :else 'O))
                                                arg-tags)))]
        (when-not (.isAssignableFrom prim-cls (.getClass ^Object method))
          (throw (RuntimeException. "Primitive hinted protocol methods must have primitive hinted implementations!"))))))
  method)

(defn extend
  "Implementations of protocol methods can be provided using the extend construct:

  (extend AType
    AProtocol
     {:foo an-existing-fn
      :bar (fn [a b] ...)
      :baz (fn ([a]...) ([a b] ...)...)}
    BProtocol
      {...}
    ...)

  extend takes a type/class (or interface, see below), and one or more
  protocol + method map pairs. It will extend the polymorphism of the
  protocol's methods to call the supplied methods when an AType is
  provided as the first argument.

  Method maps are maps of the keyword-ized method names to ordinary
  fns. This facilitates easy reuse of existing fns and fn maps, for
  code reuse/mixins without derivation or composition. You can extend
  an interface to a protocol. This is primarily to facilitate interop
  with the host (e.g. Java) but opens the door to incidental multiple
  inheritance of implementation since a class can inherit from more
  than one interface, both of which extend the protocol. It is TBD how
  to specify which impl to use. You can extend a protocol on nil.

  If you are supplying the definitions explicitly (i.e. not reusing
  exsting functions or mixin maps), you may find it more convenient to
  use the extend-type or extend-protocol macros.

  Note that multiple independent extend clauses can exist for the same
  type, not all protocols need be defined in a single extend call.

  See also:
  extends?, satisfies?, extenders"
  {:added "1.2"}
  [atype & proto+mmaps]
  (doseq [[proto mmap] (partition 2 proto+mmaps)]
    (when-not (protocol? proto)
      (throw (IllegalArgumentException.
              (str proto " is not a protocol"))))
    (when (implements? proto atype)
      (throw (IllegalArgumentException.
              (str atype " already directly implements " (:on-interface proto)))))
    (let [impls (:impls proto)
          method-caches (:method-caches proto)]
      (swap! impls assoc atype mmap)
      (loop [^clojure.lang.ISeq es (clojure.lang.RT/seq method-caches)]
        (when es
          (let [e (.first es)
                methodk (key e)
                {:keys [tag arglists]} (get-in proto [:sigs methodk])
                arg-tags (mapv #(conj (mapv (comp :tag meta) %) tag) arglists)
                method (mmap methodk)
                method (cond
                         (and (= tag 'long) (number? method)) (Casts/longCast method)
                         (and (= tag 'double) (number? method)) (Casts/doubleCast method)
                         :else (if (fn? method)
                                 (correct-primitive-fn-type arg-tags method)
                                 method))]
            ;;Note the method cache has to handle potentially nil values.
            (.extend ^MethodImplCache @(val e) atype method)
            (recur (.next es))))))))

(defn- normalize-specs
  [specs]
  (if (vector? (first specs))
    (list specs)
    specs))

(defn- emit-fn-map
  [p hint fs]
  (let [pcol (deref (resolve p))
        sigs (get pcol :sigs)
        define-fn (fn [fn-entry]
                    (let [fn-name (-> fn-entry first name keyword)
                          specs (hint (normalize-specs (drop 1 fn-entry)))
                          {:keys [tag arglists] :as sig} (get sigs fn-name)
                          arglist-map (into {} (map (fn [arglist]
                                                      [(count arglist)
                                                       (mapv (comp :tag meta) arglist)]))
                                            arglists)
                          apply-primitive-typehints
                          (fn [[[target & args :as arglist] & body]]
                            (let [args (->> (map (fn [tag arg-sym]
                                                   (vary-meta arg-sym update :tag #(or % tag)))
                                                 (drop 1 (arglist-map (inc (count args)))) args)
                                            (into [target]))]
                              (cons (vary-meta args update :tag #(or % tag)) body)))]
                      [fn-name (cons 'fn (map apply-primitive-typehints specs))]))]
    (into {} (map define-fn) fs)))

(defn- emit-impl [[p fs]]
  [p (emit-fn-map p identity fs)])

(defn- emit-hinted-impl [c [p fs]]
  (let [typehint-first-arg
        #(->> % (map (fn [[[target & args :as arglist] & body]]
                       (cons (into [(vary-meta target assoc :tag c)] args)
                             body))))]
    [p (emit-fn-map p typehint-first-arg fs)]))

(defn- parse-impls [specs]
  (loop [ret {} s specs]
    (if (seq s)
      (recur (assoc ret (first s) (take-while seq? (next s)))
             (drop-while seq? (next s)))
      ret)))

(defn- emit-extend-type [c specs]
  (let [impls (parse-impls specs)]
    `(extend ~c
       ~@(mapcat (partial emit-hinted-impl c) impls))))

(defmacro extend-type
  "A macro that expands into an extend call. Useful when you are
  supplying the definitions explicitly inline, extend-type
  automatically creates the maps required by extend.  Propagates the
  class as a type hint on the first argument of all fns.

  (extend-type MyType
    Countable
      (cnt [c] ...)
    Foo
      (bar [x y] ...)
      (baz ([x] ...) ([x y & zs] ...)))

  expands into:

  (extend MyType
   Countable
     {:cnt (fn [c] ...)}
   Foo
     {:baz (fn ([x] ...) ([x y & zs] ...))
      :bar (fn [x y] ...)})"
  {:added "1.2"}
  [t & specs]
  (emit-extend-type t specs))

(defn- emit-extend-protocol [p specs]
  (let [impls (parse-impls specs)]
    `(do
       ~@(map (fn [[t fs]]
                `(extend-type ~t ~p ~@fs))
              impls))))

(defmacro extend-protocol
  "Useful when you want to provide several implementations of the same
  protocol all at once. Takes a single protocol and the implementation
  of that protocol for one or more types. Expands into calls to
  extend-type:

  (extend-protocol Protocol
    AType
      (foo [x] ...)
      (bar [x y] ...)
    BType
      (foo [x] ...)
      (bar [x y] ...)
    AClass
      (foo [x] ...)
      (bar [x y] ...)
    nil
      (foo [x] ...)
      (bar [x y] ...))

  expands into:

  (do
   (clojure.core/extend-type AType Protocol
     (foo [x] ...)
     (bar [x y] ...))
   (clojure.core/extend-type BType Protocol
     (foo [x] ...)
     (bar [x y] ...))
   (clojure.core/extend-type AClass Protocol
     (foo [x] ...)
     (bar [x y] ...))
   (clojure.core/extend-type nil Protocol
     (foo [x] ...)
     (bar [x y] ...)))"
  {:added "1.2"}

  [p & specs]
  (emit-extend-protocol p specs))

(ns ham-fisted.profile
  (:import [java.util Map]
           [java.util.concurrent ConcurrentHashMap]))

(set! *warn-on-reflection* true)

(def ^{:dynamic true :no-doc true :tag Map} time-map (ConcurrentHashMap.))

(defmacro time-ms
  "Time an operation returning the results.  Puts time in double milliseconds
  into the time map."
  [kw & code]
  `(let [start# (System/nanoTime)
         rv# (do ~@code)
         end# (System/nanoTime)]
     (.put time-map ~kw (* (- end# start#) 1e-6))
     rv#))

(defn current-times "Get the current time map" [] (into {} time-map))
(defn reset-times! "Clear times out of current time map" [] (.clear time-map))

(defmacro with-times
  "Returns {:result :times}. Run a block of code with time map bound to a new map."
  [& code]
  `(with-bindings {#'time-map (ConcurrentHashMap.)}
     (let [rv# (do ~@code)
           times# (current-times)]
       {:result rv#
        :times times#})))

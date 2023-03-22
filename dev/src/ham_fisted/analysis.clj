(ns ham-fisted.analysis
  (:require [clojure.edn :as edn]
            [charred.api :as charred]
            [applied-science.darkstar :as darkstar]
            [clojure.java.io :as io]))



(defn plot-perf-test
  [testgroup]
  (let [group-item (first testgroup)
        testname (:test group-item)
        numeric? (:numeric? group-item)
        bytes? (= (:test group-item)
                  :hashmap-bytes)
        testdata (->> testgroup
                      (mapcat (if bytes?
                                (fn [group-item]
                                  (let [n-elems (:n-elems group-item)
                                        clj-bytes (:clj group-item)]
                                    (->> (dissoc group-item :test :numeric? :n-elems)
                                         (map (fn [kv]
                                                {:datastructure (key kv)
                                                 :n-elems n-elems
                                                 :norm-bytes (double (/ (val kv) clj-bytes))})))))
                                (fn [group-item]
                                  (let [n-elems (:n-elems group-item)
                                        clj-val (get-in group-item [:clj :mean-μs])]
                                    (->> group-item
                                         (map (fn [kv]
                                                (let [k (key kv)
                                                      val (val kv)]
                                                  (when-let [us (get val :mean-μs)]
                                                    {:datastructure k
                                                     :norm-mean-μs (double (/ us clj-val))
                                                     :n-elems n-elems}))))
                                         (remove nil?))))))
                      (sort-by :datastructure)
                      (vec))
        chart-title (str (name testname) "-" (if numeric?
                                               "numeric"
                                               "non-numeric"))
        chartname (str "charts/" chart-title ".svg")]
    (io/make-parents chartname)
    (if bytes?
      (spit chartname
            (-> {:$schema "https://vega.github.io/schema/vega-lite/v5.1.0.json"
                 :mark {:type :line
                        :point true}
                 :title chart-title
                 :width 800
                 :height 600
                 :data {:values testdata}
                 :encoding
                 {:y {:field :norm-bytes, :type :quantitative :axis {:grid false}}
                  :x {:field :n-elems :type :quantitative :scale {:type :log}}
                  :color {:field :datastructure :type :nominal}
                  :shape {:field :datastructure :type :nominal}}}
                (charred/write-json-str)
                (darkstar/vega-lite-spec->svg)))
      (spit chartname
            (-> {:$schema "https://vega.github.io/schema/vega-lite/v5.1.0.json"
                 :mark {:type :line
                        :point true}
                 :title chart-title
                 :width 800
                 :height 600
                 :data {:values testdata}
                 :encoding
                 {:y {:field :norm-mean-μs, :type :quantitative :axis {:grid false}}
                  :x {:field :n-elems :type :quantitative :scale {:type :log}}
                  :color {:field :datastructure :type :nominal}
                  :shape {:field :datastructure :type :nominal}}}
                (charred/write-json-str)
                (darkstar/vega-lite-spec->svg))))
    ;;testdata
    ))

(defn general-hashmap-analysis
  []
  (->> (edn/read-string (slurp "results/general-hashmap.edn"))
       (group-by (juxt :numeric? :test))
       (vals)
       (map plot-perf-test)
       (dorun))
  :ok)


(defn random-update-analysis
  []
  (->> (edn/read-string (slurp "results/random-update.edn"))
       (group-by (juxt :numeric? :test))
       (vals)
       (map plot-perf-test)
       (dorun))
  :ok)


(defn union-analysis
  []
  (->> (concat (edn/read-string (slurp "results/union-overlapping.edn"))
               (edn/read-string (slurp "results/union-disj.edn"))
               (edn/read-string (slurp "results/union-reduce.edn"))
               (edn/read-string (slurp "results/update-values.edn")))
       (group-by (juxt :numeric? :test))
       (vals)
       (map plot-perf-test)
       (dorun))
  :ok)

(defn typed-reduction-analysis
  []
  (->> (edn/read-string (slurp "results/typed-reductions.edn"))
       (group-by (juxt :numeric? :test))
       (vals)
       (map plot-perf-test)
       (dorun))
  :ok)

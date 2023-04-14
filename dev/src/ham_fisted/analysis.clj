(ns ham-fisted.analysis
  (:require [clojure.edn :as edn]
            [charred.api :as charred]
            [applied-science.darkstar :as darkstar]
            [clojure.java.io :as io]))



(defn plot-perf-test
  [testgroup options]
  (let [group-item (first testgroup)
        testname (:test group-item)
        numeric? (:numeric? group-item)
        bytes? (= (:test group-item)
                  :hashmap-bytes)
        baseline (or (:baseline options) :clj)
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
                                        clj-val (get-in group-item [baseline :mean-μs])]
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

(defn- process-files
  ([fnames] (process-files fnames nil))
  ([fnames options]
   (->> fnames
        (mapcat #(edn/read-string (slurp %)))
        (group-by (juxt :numeric? :test))
        (vals)
        (map #(plot-perf-test % options))
        (dorun))
   :ok))

(defn general-hashmap-analysis
  []
  (process-files ["results/general-hashmap.edn"])
  )


(defn random-update-analysis
  []
  (process-files ["results/random-update.edn"]))


(defn union-analysis
  []
  (process-files ["results/union-overlapping.edn"
                  "results/union-disj.edn"
                  "results/union-reduce.edn"
                  "results/update-values.edn"]))

(defn typed-reduction-analysis
  []
  (process-files ["results/typed-reductions.edn"]))


(defn typed-parallel-reduction-analysis
  []
  (process-files ["results/typed-parallel-reductions.edn"]))

(defn union-reduce-transient
  []
  (process-files ["results/union-reduce-transient.edn"] {:baseline :hamf-hashmap})
  )


(defn persistent-vector
  []
  (process-files ["results/persistent-vector.edn"]))

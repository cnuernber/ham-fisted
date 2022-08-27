(ns perftest
  (:require [ham-fisted.api :as api]
            [ham-fisted.benchmark :as bench]
            [clojure.tools.logging :as log])
  (:gen-class))








(defn -main
  [& args]
  (log/info "Begin - perftest")
  (log/info "End - perftest"))

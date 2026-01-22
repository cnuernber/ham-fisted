(ns ham-fisted.process
  (:require [ham-fisted.iterator :as hamf-iter]
            [ham-fisted.reduce :as hamf-rf]))

(defn stream->strings
  ([input] (stream->strings input 256 (java.nio.charset.Charset/defaultCharset)))
  ([^java.io.InputStream input bufsize ^java.nio.charset.Charset charset]
   (let [buffer (byte-array bufsize)]
     (hamf-iter/once-iterable
      #(let [size (.read input buffer)]
         (when (pos? size)
           (String. buffer 0 size charset)))))))

(defn- strip-trailing
  ^String [data]
  (.stripTrailing (str data)))

(defn- naive-split-lines
  [^String data]
  (let [nl (int \newline)]
    (loop [data data
           nn (.indexOf data nl)
           rv []]
      (if (neg? nn)
        [rv data]
        (let [rv (conj rv (.substring data 0 nn))
              data (.substring data (inc nn))]
          (recur data (.indexOf data nl) rv))))))

(def ^{:doc "Print process output using println.  Example process output handler.
  Returns total output as a string to when finalized."}
  println-rf
  (fn println-rf
    ([] {:temp-buf (StringBuilder.) :total-buf (StringBuilder.)})
    ([{:keys [^StringBuilder temp-buf ^StringBuilder total-buf] :as acc} ^String data]
     (.append temp-buf data)
     (.append total-buf data)
     (let [temp-str (.toString temp-buf)
           [strs leftover] (naive-split-lines (.toString temp-buf))]
       (run! println (map strip-trailing strs))
       (.delete temp-buf (int 0) (int (.length temp-buf)))
       (.append temp-buf leftover))
     acc)
    ([{:keys [^StringBuilder temp-buf ^StringBuilder total-buf] :as acc}]
     (println (.toString temp-buf))
     (.toString total-buf))))

(def ^{:doc "Record all the strings and save them to a vector"}
  record-rf
  (fn record-rf
    ([] (transient []))
    ([acc v] (conj! acc v))
    ([acc] (persistent! acc))))

(defn destroy-forcibly!
  "Destroy the process handle's process forcibly."
  [^java.lang.ProcessHandle proc-hdl]
  (.destroyForcibly proc-hdl))

(defn process-descendants
  "Get the first descendants of a process handle."
  [^java.lang.ProcessHandle proc-hdl]
  (-> (reify Iterable
        (iterator [this]
          (.iterator (.descendants proc-hdl))))
      (vec)))

(defn launch
  "Launch a proccess.

  * cmd-line string command line.
  * stdout-hdrl, stderr-hdlr - transduce-style rf functions that receive each string read from stdout
    and stderr respectively.

  Returns `{:keys [^java.lang.ProcessHandle proc-hdl wait-or-kill]}`:

  * `proc-hdl` -  java.lang.ProcessHandle
  * `wait-or-kill` - function that has two arities:
    1. (proc) - kill the process returning any output as {:out :err}.
    2. (proc time-ms timeout-symbol) - wait specified time for process to terminate returning either
       the timeout symbol or {:out :err}.

  Example:

```clojure
ham-fisted.process> (launch \"ls -al\" {:print-cmd-line? false})
  {:proc-hdl #object[java.lang.ProcessHandleImpl 0x7f8e8742 \"31019\"],
  :wait-or-kill #function[ham-fisted.process/launch/wait-or-kill--65545]}
  ...

ham-fisted.process> (def result ((:wait-or-kill *1)))
#'ham-fisted.process/result
ham-fisted.process> (keys result)
(:out :err)
```"
  ([cmd-line] (launch cmd-line {}))
  ([^String cmd-line {:keys [stdout-hdlr stderr-hdlr print-cmd-line?]
                      :or {print-cmd-line? true}}]
   (when print-cmd-line? (println "launch-process:" cmd-line))
   (let [proc (.exec (Runtime/getRuntime) cmd-line)
         phandle (.toHandle proc)
         exit-future (.onExit phandle)
         stdout-hdlr (or stdout-hdlr println-rf)
         stderr-hdlr (or stderr-hdlr println-rf)
         _ (.close (.getOutputStream proc))
         stdout (.getInputStream proc)
         stderr (.getErrorStream proc)
         out (future (hamf-rf/reduce-reducer stdout-hdlr (stream->strings stdout)))
         err (future (hamf-rf/reduce-reducer stderr-hdlr (stream->strings stderr)))
         cleanup #(do (.close stdout)
                      (.close stderr)
                      {:out @out
                       :err @err})]
     {:proc-hdl phandle
      :wait-or-kill
      (fn wait-or-kill
        ([]
         (let [desc (process-descendants phandle)]
           (run! destroy-forcibly! desc)
           (destroy-forcibly! phandle)
           (cleanup)))
        ([^long time-ms timeout-symbol]
         (if-let [rv (try (.get exit-future time-ms java.util.concurrent.TimeUnit/MILLISECONDS)
                          (catch java.util.concurrent.TimeoutException e nil))]
           (cleanup)
           timeout-symbol)))})))

(defn ^:private map->cmd-line
  [args]
  (->> (flatten (seq args))
       (map str)))

(defn launch-jvm
  "Assumes a jvm process launched from a shell command.  Will hang looking for first process descendant.

  If shell command then arguments other than :xmx :jvm-opts may need to have quoted strings if they are
  being passed the clojure process.

  Example:

```clojure
ham-fisted.process> (launch-jvm \"clojure\" {:jvm-opts [\"-A:dev\" \"-X\" \"ham-fisted.protocol-perf/-main\"]})
launch-process: clojure -A:dev -X ham-fisted.protocol-perf/-main
{:proc-hdl #object[java.lang.ProcessHandleImpl 0x4f4a8dc7 \"31194\"],
 :wait-or-kill #function[ham-fisted.process/launch/wait-or-kill--10016],
 :jvm-pid 31199,
  :jvm-proc #object[java.lang.ProcessHandleImpl 0x2e52e7c8 \"31199\"]}
...
ham-fisted.process> (def result ((:wait-or-kill *1)))
#'ham-fisted.process/result
```"
  [cmd-name {:keys [xmx jvm-opts]
             :as args}]
  (let [jvm-opts (if xmx
                   (conj (or jvm-opts []) (str "-Xmx" xmx))
                   jvm-opts)
        cmd-line
        (clojure.string/join " " (concat [cmd-name]
                                         (map->cmd-line jvm-opts)
                                         (map->cmd-line (dissoc args :xmx :jvm-opts
                                                                :stdout-hdlr :stderr-hdlr))))
        {:keys [^java.lang.ProcessHandle proc-hdl] :as rv} (launch cmd-line args)
        desc (loop [desc (process-descendants proc-hdl)]
               (if (== 0 (count desc) )
                 (do (Thread/sleep 5)
                     (if (.isAlive proc-hdl)
                       (recur (process-descendants proc-hdl))
                       desc))
                 desc))
        ^java.lang.ProcessHandle jvm-proc (first desc)]
    (if jvm-proc
      (assoc rv :jvm-pid (.pid jvm-proc) :jvm-proc jvm-proc)
      rv)))

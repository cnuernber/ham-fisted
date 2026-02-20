(ns ham-fisted.thread-local)

(defn thread-local
  "Create a new thread local variable"
  (^ThreadLocal [] (thread-local nil))
  (^ThreadLocal [v] (ThreadLocal/withInitial
                     (reify java.util.function.Supplier
                       (get [this] v)))))

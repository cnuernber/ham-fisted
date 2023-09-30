package ham_fisted;


import clojure.lang.IPersistentSet;
import clojure.lang.ITransientSet;
import clojure.lang.IEditableCollection;


public interface IAPersistentSet extends ISet, IEditableCollection, IPersistentSet {
  default int count() { return size(); }
  default Object get(Object o) { return contains(o) ? o : null; }
  default IPersistentSet cons(Object o) {
    return (IPersistentSet)asTransient().conj(o).persistent();
  }
  default IPersistentSet disjoin(Object o) {
    return (IPersistentSet)asTransient().disjoin(o).persistent();
  }
  ITransientSet asTransient();
}

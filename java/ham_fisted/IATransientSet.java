package ham_fisted;


import clojure.lang.ITransientSet;


public interface IATransientSet extends ITransientSet {
  default Object get(Object o) { return contains(o) ? o : null; }
}

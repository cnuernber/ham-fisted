package ham_fisted;


import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Consumer;
import clojure.lang.Counted;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.RT;



public interface ISet extends Set, ITypedReduce, IFnDef, Counted, Seqable {

  default int count() { return size(); }
  @SuppressWarnings("unchecked")
  default boolean addAll(Collection c) {
    int sz = size();
    for(Object o: c)
      add(o);
    return sz == size();
  }
  @SuppressWarnings("unchecked")
  default void forEach(Consumer c) {
    ITypedReduce.super.forEach(c);
  }
  default boolean retainAll(Collection c) {
    int sz = size();
    for(Iterator iter = iterator(); iter.hasNext();) {
      if(!c.contains(iter.next()))
	iter.remove();
    }
    return sz == size();
  }
  default boolean containsAll(Collection c) {
    for(Object o: c) {
      if(!contains(c)) return false;
    }
    return true;
  }
  default boolean removeAll(Collection c) {
    int sz = size();
    for(Object o: c) {
      remove(o);
    }
    return sz == size();
  }
  default ISeq seq() { return RT.chunkIteratorSeq(iterator()); }
  default boolean isEmpty() { return size() == 0; }
  default Object[] toArray() { return ArrayLists.toArray(this); }
  default Object[] toArray(Object[] d) {
    return ArrayLists.toArray(this, d);
  }
  default Object invoke(Object arg) { return contains(arg) ? arg : null; }
}

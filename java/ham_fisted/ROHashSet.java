package ham_fisted;


import java.util.Collection;
import clojure.lang.IPersistentMap;


public class ROHashSet extends HashSet {
  public ROHashSet() {
    super();
  }
  public ROHashSet(HashBase hs) {
    this(hs, null);
  }
  public ROHashSet(HashBase hs, IPersistentMap meta) {
    super(hs, meta);
  }
  public boolean add(Object o) {
    throw new UnsupportedOperationException();
  }
  public boolean addAll(Collection c) {
    throw new UnsupportedOperationException();
  }
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }
}

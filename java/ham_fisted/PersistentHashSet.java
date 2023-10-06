package ham_fisted;


import java.util.Set;
import java.util.Collection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientSet;
import clojure.lang.IObj;


public class PersistentHashSet extends ROHashSet implements IAPersistentSet, IObj {
  public static final PersistentHashSet EMPTY = new PersistentHashSet();
  public PersistentHashSet() { super(); }
  public PersistentHashSet(HashBase hb) {
    super(hb);
  }
  public PersistentHashSet(HashBase hb, IPersistentMap meta) {
    super(hb,meta);
  }
  public PersistentHashSet withMeta(IPersistentMap m) {
    return new PersistentHashSet(this, m);
  }
  public ITransientSet asTransient() {
    return isEmpty() ?  new UnsharedHashSet(meta) : new TransientHashSet(this, meta);
  }
  public PersistentHashSet empty() { return EMPTY; }
  public PersistentHashSet union(Collection rhs) {
    return new PersistentHashSet(union(shallowClone(), rhs));
  }
  public PersistentHashSet intersection(Set rhs) {
    return new PersistentHashSet(intersection(shallowClone(), rhs));
  }
  public PersistentHashSet difference(Collection rhs) {
    return new PersistentHashSet(difference(shallowClone(), rhs));
  }
}

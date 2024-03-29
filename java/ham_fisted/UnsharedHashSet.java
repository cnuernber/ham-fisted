package ham_fisted;


import java.util.Collection;
import clojure.lang.IPersistentMap;


public class UnsharedHashSet extends HashSet implements IATransientSet {
  public UnsharedHashSet(IPersistentMap meta) {
    super(meta);
  }
  public UnsharedHashSet conj(Object key) {
    add(key);
    return this;
  }
  public UnsharedHashSet disjoin(Object key) {
    remove(key);
    return this;
  }
  public HashSet union(Collection rhs) {
    addAll(rhs);
    return this;
  }
  public PersistentHashSet persistent() { return new PersistentHashSet(this, meta); }
}

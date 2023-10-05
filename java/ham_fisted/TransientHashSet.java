package ham_fisted;

import clojure.lang.IPersistentMap;

public class TransientHashSet extends ROHashSet implements IATransientSet {
  public TransientHashSet(HashBase hb, IPersistentMap meta) { super(hb, meta); }
  public TransientHashSet conj(Object key) {
    int hc = hash(key);
    int idx = hc & mask;
    HashNode e = data[idx];
    data[idx] = e != null ? e.assoc(this, key, hc, VALUE) : newNode(key, hc, VALUE);
    return this;
  }
  public TransientHashSet disjoin(Object key) {
    int hc = hash(key);
    int idx = hc & mask;
    HashNode e = data[idx];
    if(e != null)
      data[idx] = e.dissoc(this, key);
    return this;
  }
  public PersistentHashSet persistent() { return new PersistentHashSet(this, meta); }
}

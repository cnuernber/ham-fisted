package ham_fisted;

import clojure.lang.IPersistentMap;

public class TransientHashSet extends ROHashSet implements IATransientSet {
  public TransientHashSet(HashBase hb, IPersistentMap meta) {
    super(hb, meta);
    //Writes go into the bucket array so it cannot be shared with the source set.
    this.data = this.data.clone();
  }
  public TransientHashSet conj(Object key) {
    ensureEditable();
    int hc = hash(key);
    int idx = hc & mask;
    HashNode e = data[idx];
    data[idx] = e != null ? e.assoc(this, key, hc, VALUE) : newNode(key, hc, VALUE);
    return this;
  }
  public TransientHashSet disjoin(Object key) {
    ensureEditable();
    int hc = hash(key);
    int idx = hc & mask;
    HashNode e = data[idx];
    if(e != null)
      data[idx] = e.dissoc(this, key);
    return this;
  }
  public PersistentHashSet persistent() {
    freeze();
    return new PersistentHashSet(this, meta);
  }
}

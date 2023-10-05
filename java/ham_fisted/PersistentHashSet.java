package ham_fisted;


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
}

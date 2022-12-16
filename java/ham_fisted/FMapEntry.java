package ham_fisted;


import java.util.Map;
import clojure.lang.IPersistentMap;

public class FMapEntry implements IMutList, Map.Entry {
  public final Object k;
  public final Object v;
  int _hash;
  IPersistentMap meta;
  public FMapEntry(Object _k, Object _v) {
    k = _k;
    v = _v;
    _hash = 0;
    meta = null;
  }
  public FMapEntry(FMapEntry e, IPersistentMap m) {
    k = e.k;
    v = e.v;
    _hash = e._hash;
    meta = m;
  }
  public boolean equals(Object o) { return equiv(o); }
  public int hashCode() { return hasheq(); }
  public int hasheq() {
    if (_hash == 0)
      _hash = IMutList.super.hasheq();
    return _hash;
  }
  public Object setValue( Object v) { throw new RuntimeException("Cannot set value."); }
  public Object getKey() { return k; }
  public Object getValue() { return v; }
  public int size() { return 2; }
  public Object get(int idx) {
    if(idx == 0) return k;
    if(idx == 1) return v;
    throw new RuntimeException("Index out of range: " + String.valueOf(idx));
  }
  public FMapEntry withMeta(IPersistentMap m) { return new FMapEntry(this, m); }
  public IPersistentMap meta() { return meta; }
}

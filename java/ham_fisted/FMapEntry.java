package ham_fisted;


import java.util.Map;
import clojure.lang.IPersistentMap;

public class FMapEntry<K,V> implements IMutList, Map.Entry<K,V> {
  public final K k;
  public final V v;
  int _hash;
  IPersistentMap meta;
  public FMapEntry(K _k, V _v) {
    k = _k;
    v = _v;
    _hash = 0;
    meta = null;
  }
  public FMapEntry(FMapEntry<K,V> e, IPersistentMap m) {
    k = e.k;
    v = e.v;
    _hash = e._hash;
    meta = m;
  }
  public static <K,V> FMapEntry<K,V> create(K k, V v) {
    return new FMapEntry<K,V>(k,v);
  }
  public boolean equals(Object o) { return equiv(o); }
  public int hashCode() { return hasheq(); }
  public int hasheq() {
    if (_hash == 0)
      _hash = IMutList.super.hasheq();
    return _hash;
  }
  public V setValue( Object v) { throw new RuntimeException("Cannot set value."); }
  public K getKey() { return k; }
  public V getValue() { return v; }
  public int size() { return 2; }
  public Object get(int idx) {
    if(idx == 0) return k;
    if(idx == 1) return v;
    throw new RuntimeException("Index out of range: " + String.valueOf(idx));
  }
  public FMapEntry<K,V> withMeta(IPersistentMap m) { return new FMapEntry<K,V>(this, m); }
  public IPersistentMap meta() { return meta; }
}

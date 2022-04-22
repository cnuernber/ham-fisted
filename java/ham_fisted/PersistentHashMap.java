package ham_fisted;

import static ham_fisted.HashBase.*;
import clojure.lang.APersistentMap;
import clojure.lang.Util;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.util.Objects;
import java.util.Map;


public abstract class PersistentHashMap extends APersistentMap {

  final HashBase hb;

  public static final HashProvider equivHashProvider = new HashProvider(){
      public int hash(Object obj) {
	return Util.hasheq(obj);
      }
      public boolean equals(Object lhs, Object rhs) {
	return Util.equiv(lhs,rhs);
      }
    };
  
  public PersistentHashMap() {
    hb = new HashBase(equivHashProvider);
  }
  public PersistentHashMap(HashBase hm) {
    hb = hm;
  }
  public PersistentHashMap(HashProvider _hp, boolean assoc, Object... kvs) {
    HashMap<Object,Object> hm = new HashMap<Object,Object>(_hp);
    final int nkvs = kvs.length;
    if (0 != (nkvs % 2))
      throw new RuntimeException("Uneven number of keyvals");
    final int nks = nkvs / 2;
    for (int idx = 0; idx < nks; ++idx) {
      final int kidx = idx * 2;
      final int vidx = kidx + 1;
      hm.put(kvs[kidx], kvs[vidx]);
    }
    if (assoc == false && hm.size() != nks)      
      throw new RuntimeException("Duplicate key detected: " + String.valueOf(kvs));
    hb = hm;
  }
  public PersistentHashMap(boolean assoc, Object... kvs) {
    this(equivHashProvider, assoc, kvs);
  }
  public boolean containsKey(Object key) {
    return hb.containsKey(key);
  }
  public boolean containsValue(Object v) {
    return hb.containsValue(v);
  }
  public int size() { return hb.size(); }
  public Set keySet() { return hb.keySet((Object)null, false); }
  public Set entrySet() { return hb.entrySet((Map.Entry<Object,Object>)null, false); }
  public Collection values() { return hb.values((Object)null, false); }
  public IMapEntry entryAt(Object key) {
    LeafNode node = hb.getNode(key);
    return node != null ? MapEntry.create(key, node.val()) : null;
  }
  
}
				       

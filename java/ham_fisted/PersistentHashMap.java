package ham_fisted;

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.IObj;
import clojure.lang.Indexed;


public class PersistentHashMap
  extends ROHashMap
  implements IAPersistentMap, IObj {
  public static final PersistentHashMap EMPTY = new PersistentHashMap(new HashMap());
  int _hasheq = 0;

  PersistentHashMap(HashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data, data.meta);
  }
  public PersistentHashMap(HashMap data, boolean marker) {
    super(data.loadFactor, data.capacity, data.length, data.data.clone(), data.meta);
  }
  PersistentHashMap(HashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }

  public int count() { return length; }
  public int hasheq() {
    if (_hasheq == 0)
      _hasheq = super.hasheq();
    return _hasheq;
  }
  public PersistentHashMap assoc(Object key, Object val) {
    final int hashcode = hash(key);
    final int idx = hashcode & mask;
    HashNode e, init = data[idx];
    Object k;
    for(e = init; e != null && !((k = e.k) == key || equals(k, key)); e = e.nextNode);
    if(e != null) {
      if(e.v == val) return this;
      PersistentHashMap rv = new PersistentHashMap(this, true);
      // We use e.k here for identity short circuiting in HashNode assoc
      rv.data[idx] = init.assoc(rv, e.k, hashcode, val);
      return rv;
    } else {
      PersistentHashMap rv = new PersistentHashMap(this, true);
      HashNode newNode = rv.newNode(key, hashcode, val);
      rv.data[idx] = newNode;
      newNode.nextNode = init;
      rv.checkResize(null);
      return rv;
    }
  }
  public IPersistentMap without(Object key) {
    final int hashcode = hash(key);
    final int idx = hashcode & mask;
    HashNode e, init = data[idx];
    Object k;
    for(e = init; e != null && !((k = e.k) == key || equals(k, key)); e = e.nextNode);
    if(e == null) return this;
    PersistentHashMap rv = new PersistentHashMap(this, true);
    rv.data[idx] = init.dissoc(rv, key);
    return rv;
  }
  public ITransientMap asTransient() {
    return isEmpty() ? new UnsharedHashMap(meta) :
      new TransientHashMap(this);
  }
  public PersistentHashMap withMeta(IPersistentMap m) {
    return new PersistentHashMap(this, m);
  }
  public PersistentHashMap empty() { return EMPTY; }
  public PersistentHashMap union(Map o, BiFunction bfn) {
    return new PersistentHashMap(union(shallowClone(), o, bfn));
  }
  public PersistentHashMap intersection(Map o, BiFunction bfn) {
    return new PersistentHashMap(intersection(shallowClone(), o, bfn));
  }
  public PersistentHashMap intersection(Set o) {
    return new PersistentHashMap(intersection(shallowClone(), o));
  }
  public PersistentHashMap difference(Collection c) {
    return new PersistentHashMap(difference(shallowClone(), c));
  }
  public PersistentHashMap updateValues(BiFunction valueMap) {
    return new PersistentHashMap(updateValues(shallowClone(), valueMap));
  }
  public PersistentHashMap updateValue(Object k, Function fn) {
    return new PersistentHashMap(updateValue(this, fn));
  }
}

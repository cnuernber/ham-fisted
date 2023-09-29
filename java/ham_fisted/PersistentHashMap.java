package ham_fisted;

import java.util.Map;
import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.IObj;
import clojure.lang.Indexed;

public class PersistentHashMap
  extends ROHashMap
  implements IEditableCollection, IPersistentMap, IObj {
  public static final PersistentHashMap EMPTY = new PersistentHashMap(new HashMap());
  int _hasheq = 0;
  
  public PersistentHashMap(HashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data, data.meta);
  }
  public PersistentHashMap(HashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }
  public int hasheq() {
    if (_hasheq == 0)
      _hasheq = super.hasheq();
    return _hasheq;
  }
  public int count() { return length; }
  public IPersistentMap cons(Object val) {
    Object k, v;
    if(val instanceof Indexed) {
      Indexed ii = (Indexed)val;
      k = ii.nth(0);
      v = ii.nth(1);
    } else if (val instanceof Map.Entry) {
      Map.Entry ii = (Map.Entry)val;
      k = ii.getKey();
      v = ii.getValue();
    } else {
      throw new RuntimeException("Value must be either indexed or map entry");
    }
    return assoc(k,v);
  }
  public IPersistentMap assocEx(Object key, Object val) {
    if(containsKey(key))
      throw new RuntimeException("Object already contains key :" + String.valueOf(key));
    return assoc(key, val);
  }
  public IPersistentMap assoc(Object key, Object val) {
    return asTransient().assoc(key,val).persistent();
  }
  public IPersistentMap without(Object key) {
    return asTransient().without(key).persistent();
  }
  public ITransientMap asTransient() {
    return isEmpty() ? new InitHashMap(meta) : 
      new TransientHashMap(this);
  }
  public PersistentHashMap withMeta(IPersistentMap m) {
    return new PersistentHashMap(this, m);
  }
  public PersistentHashMap empty() { return EMPTY; }
}
				       

package ham_fisted;

import java.util.Map;
import clojure.lang.Indexed;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;

public class TransientHashMap
  extends ROHashMap
  implements IATransientMap, IObj
{
  public TransientHashMap(HashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data.clone(), data.meta);
  }
  public TransientHashMap(TransientHashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }
  public TransientHashMap assoc(Object key, Object val) {
    int hc = hash(key);
    int idx = hc & mask;
    HashNode e = data[idx];
    data[idx] = e != null ? e.assoc(this, key, hc, val) : newNode(key, hc, val);
    return this;
  }
  public TransientHashMap without(Object key) {
    int hc = hash(key);
    int idx = hc & mask;
    HashNode e = data[idx];
    if(e!=null)
      data[idx] = e.dissoc(this, key);
    return this;
  }
  public PersistentHashMap persistent() {
    return new PersistentHashMap(this);
  }
  public TransientHashMap withMeta(IPersistentMap m) {
    return new TransientHashMap(this, m);
  }
}

package ham_fisted;

import java.util.Map;
import clojure.lang.Indexed;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;

public class TransientLongHashMap
  extends ROLongHashMap
  implements IATransientMap, IObj
{
  public TransientLongHashMap(LongHashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data.clone(), data.meta);
  }
  public TransientLongHashMap(TransientLongHashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }
  public TransientLongHashMap assoc(Object kk, Object val) {
    long key = Casts.longCast(kk);
    int hc = hash(key);
    int idx = hc & mask;
    LongHashNode e = data[idx];
    data[idx] = e != null ? e.assoc(this, key, hc, val) : newNode(key, hc, val);
    return this;
  }
  public TransientLongHashMap without(Object kk) {
    long key = Casts.longCast(kk);
    int hc = hash(key);
    int idx = hc & mask;
    LongHashNode e = data[idx];
    if(e!=null)
      data[idx] = e.dissoc(this, key);
    return this;
  }
  public PersistentLongHashMap persistent() {
    return new PersistentLongHashMap(this);
  }
  public TransientLongHashMap withMeta(IPersistentMap m) {
    return new TransientLongHashMap(this, m);
  }
}

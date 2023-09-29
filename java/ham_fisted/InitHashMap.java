package ham_fisted;


import java.util.Map;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.Indexed;


public class InitHashMap
  extends HashMap
  implements ITransientMap, ITransientAssociative2 {
  public InitHashMap(IPersistentMap meta) {
    super(meta);
  }
  public InitHashMap conj(Object val) {
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
  public InitHashMap assoc(Object key, Object val) {
    put(key,val);
    return this;
  }
  public InitHashMap without(Object key) {
    remove(key);
    return this;
  }
  public PersistentHashMap persistent() {
    return new PersistentHashMap(this);
  }
}

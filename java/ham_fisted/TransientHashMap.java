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
  implements ITransientMap, ITransientAssociative2, IObj
{
  public TransientHashMap(HashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data.clone(), data.meta);
  }
  public TransientHashMap(TransientHashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }
  public TransientHashMap conj(Object val) {
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
  public TransientHashMap assoc(Object key, Object val) {
    int hc = hash(key);
    int idx = hc & mask;
    HBNode lastNode = null;
    for(HBNode e = data[idx]; e != null; e = e.nextNode) {
      if(e.owner != this) {
	e = e.setOwner(this);
	if(lastNode == null)
	  data[idx] = e;
	else
	  lastNode.nextNode = e;
      }
      if(e.k == key || equals(e.k, key)) {
	e.setValue(val);
	return this;
      }
      lastNode = e;
    }
    if(lastNode != null)
      lastNode.nextNode = newNode(key, hc, val);
    else
      data[idx] = newNode(key,hc,val);
    return this;
  }
  public TransientHashMap without(Object key) {
    int hc = hash(key);
    int idx = hc & mask;
    HBNode lastNode = null;
    for(HBNode e = data[idx]; e != null; e = e.nextNode) {
      if(e.k == key || equals(e.k, key)) {
	if(lastNode == null)
	  data[idx] = e.nextNode;
	else
	  lastNode.nextNode = e.nextNode;
	dec(e);
	return this;
      }
      if(e.owner != this) {
	e = e.setOwner(this);
	if(lastNode == null)
	  data[idx] = e;
	else
	  lastNode.nextNode = e;
      }
      lastNode = e;
    }
    return this;
  }
  public PersistentHashMap persistent() {
    return new PersistentHashMap(this);
  }
  public TransientHashMap withMeta(IPersistentMap m) {
    return new TransientHashMap(this, m);
  }
}

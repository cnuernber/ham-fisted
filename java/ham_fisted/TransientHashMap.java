package ham_fisted;


import static ham_fisted.BitmapTrie.*;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentVector;
import clojure.lang.AFn;
import java.util.Map;
import java.util.Iterator;


public final class TransientHashMap
  extends AFn
  implements ITransientMap, ITransientAssociative2, IObj {


  final BitmapTrie hb;
  boolean editable = true;

  public TransientHashMap(BitmapTrie _hb) {
    hb = _hb;
  }

  TransientHashMap(TransientHashMap other) {
    hb = new BitmapTrie(other.hb);
  }

  public final TransientHashMap clone() {
    return new TransientHashMap(this);
  }

  public final boolean containsKey(Object key) {
    return hb.get(key) != null;
  }
  public final IMapEntry entryAt(Object key) {
    LeafNode lf = (LeafNode)hb.get(key);
    return lf == null ? null : new MapEntry(lf.key(), lf.val());
  }
  final void ensureEditable() {
    if (!editable)
      throw new RuntimeException("Transient map editted after persistent!");
  }
  public final TransientHashMap assoc(Object key, Object val) {
    ensureEditable();
    hb.assoc(key,val);
    return this;
  }
  public final TransientHashMap conj(Object val) {
    ensureEditable();
    if (val instanceof IPersistentVector) {
      IPersistentVector v = (IPersistentVector)val;
      if (v.count() != 2)
	throw new RuntimeException("Vector length != 2 during conj");
      return assoc(v.nth(0), v.nth(1));
    } else if (val instanceof Map.Entry) {
      Map.Entry e = (Map.Entry)val;
      return assoc(e.getKey(), e.getValue());
    } else {
      Iterator iter = ((Iterable)val).iterator();
      while(iter.hasNext()) {
	Map.Entry e = (Map.Entry)iter.next();
	hb.assoc(e.getKey(), e.getValue());
      }
      return this;
    }
  }

  public final ITransientMap without(Object key) {
    ensureEditable();
    hb.dissoc(key);
    return this;
  }

  public final PersistentHashMap persistent() {
    editable = false;
    return new PersistentHashMap(hb);
  }

  public final Object valAt(Object key) {
    return hb.get(key);
  }

  public final Object valAt(Object key, Object notFound) {
    return hb.getOrDefault(key, notFound);
  }
  public final Object invoke(Object arg1) {
    return hb.get(arg1);
  }

  public final Object invoke(Object arg1, Object notFound) {
    return hb.getOrDefault(arg1, notFound);
  }

  public int count() { return hb.size(); }

  public IPersistentMap meta() { return hb.meta; }
  public IObj withMeta(IPersistentMap newMeta) {
    return new TransientHashMap(hb.shallowClone(newMeta));
  }
  public void printNodes() { hb.printNodes(); }
}

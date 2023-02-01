package ham_fisted;


import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.Iterator;
import clojure.lang.IEditableCollection;
import clojure.lang.ITransientCollection;
import clojure.lang.ITransientMap;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;
import clojure.lang.IFn;
import clojure.lang.Indexed;
import clojure.lang.Counted;

import static ham_fisted.BitmapTrieCommon.*;


public class ImmutArrayMap
  extends APersistentMapBase
  implements IEditableCollection, IPersistentMap, IObj, ImmutValues {
  public ImmutArrayMap(HashProvider hp) {
    super(new ArrayMap(hp, new Object[4], 0, null));
  }
  public ImmutArrayMap(ArrayMap ht) {
    super(ht);
  }
  public IPersistentMap cons(Object obj) {
    if (obj instanceof Map.Entry) {
      Map.Entry me = (Map.Entry)obj;
      return assoc(me.getKey(), me.getValue());
    } else if (obj instanceof Indexed) {
      Indexed me = (Indexed)obj;
      if(2 != ((Counted)obj).count())
	throw new RuntimeException("Vector length != 2");
      return assoc(me.nth(0), me.nth(1));
    } else {
      ITransientMap t = asTransient();
      Iterator iter = ((Iterable)obj).iterator();
      while(iter.hasNext()) {
	Map.Entry e = (Map.Entry)iter.next();
	t = t.assoc(e.getKey(), e.getValue());
      }
      return t.persistent();
    }
  }
  @SuppressWarnings("unchecked")
  public IPersistentMap assoc(Object key, Object val) {
    ArrayMap am = (ArrayMap)ht;
    MapData md = am.assocAt(am.index(key), key, val, true);
    return md instanceof ArrayMap ? new ImmutArrayMap((ArrayMap)md) :
      new ImmutHashTable((HashTable)md);
  }
  @SuppressWarnings("unchecked")
  public IPersistentMap assocEx(Object key, Object val) {
    ArrayMap am = (ArrayMap)ht;
    int idx = am.index(key);
    if (idx != -1) throw new RuntimeException("Key already in map: " + String.valueOf(key));
    MapData md = am.assocAt(idx, key, val, true);
    return md instanceof ArrayMap ? new ImmutArrayMap((ArrayMap)md) :
      new ImmutHashTable((HashTable)md);
  }
  @SuppressWarnings("unchecked")
  public ImmutArrayMap without(Object key) {
    ArrayMap am = (ArrayMap)ht;
    return new ImmutArrayMap((ArrayMap)am.shallowClone().mutDissoc(key));
  }
  public ImmutArrayMap empty() {
    return new ImmutArrayMap(ht.hashProvider());
  }
  public ITransientMap asTransient() {
    if(isEmpty())
      return new MutArrayMap(((ArrayMap)ht).shallowClone());
    else
      return new TransientArrayMap(((ArrayMap)ht).shallowClone());
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable immutUpdateValues(BiFunction valueMap) {
    return (ImmutHashTable)((ImmutValues)asTransient()).immutUpdateValues(valueMap);
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable immutUpdateValue(Object key, IFn fn) {
    return (ImmutHashTable)((ImmutValues)asTransient()).immutUpdateValue(key,fn);
  }
  public ImmutArrayMap withMeta(IPersistentMap m) {
    if(m == meta())
      return this;
    return new ImmutArrayMap((ArrayMap)ht.withMeta(m));
  }

}

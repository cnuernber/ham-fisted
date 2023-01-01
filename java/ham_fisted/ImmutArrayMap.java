package ham_fisted;


import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.IEditableCollection;
import clojure.lang.ITransientCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;
import clojure.lang.IFn;

import static ham_fisted.BitmapTrieCommon.*;


public class ImmutArrayMap<K,V>
  extends NonEditableArrayMapBase<K,V>
  implements IEditableCollection, IPersistentMap, IObj, ImmutValues {
  public ImmutArrayMap(HashProvider hp) {
    super(new ArrayMap(hp, new Object[4], 0, null));
  }
  public ImmutArrayMap(ArrayMap ht) {
    super(ht);
  }
  public ImmutArrayMap<K,V> shallowClone() {
    return new ImmutArrayMap<K,V>((ArrayMap)ht.shallowClone());
  }
  @SuppressWarnings("unchecked")
  public ImmutArrayMap<K,V> cons(Object obj) {
    return (ImmutArrayMap<K,V>)(shallowClone().mutConj(obj));
  }
  @SuppressWarnings("unchecked")
  public IPersistentMap assoc(Object key, Object val) {
    return (IPersistentMap)(shallowClone().mutAssoc((K)key, (V)val));
  }
  @SuppressWarnings("unchecked")
  public IPersistentMap assocEx(Object key, Object val) {
    return (IPersistentMap)(shallowClone().mutAssoc((K)key, (V)val));
  }
  @SuppressWarnings("unchecked")
  public ImmutArrayMap<K,V>  without(Object key) {
    return (ImmutArrayMap<K,V>)(shallowClone().mutDissoc((K)key));
  }
  public ImmutArrayMap<K,V> empty() {
    return new ImmutArrayMap<K,V>(ht.hashProvider());
  }
  public ITransientCollection asTransient() {
    if(isEmpty())
      return new MutArrayMap<K,V>((ArrayMap)ht);
    else
      return new TransientArrayMap<K,V>((ArrayMap)ht);
  }
  @SuppressWarnings("unchecked")
  public ImmutArrayMap<K,V> immutUpdateValues(BiFunction valueMap) {
    return (ImmutArrayMap<K,V>)(shallowClone().mutUpdateValues(valueMap));
  }
  @SuppressWarnings("unchecked")
  public UpdateValues immutUpdateValue(Object key, IFn fn) {
    return (UpdateValues)(shallowClone().mutUpdateValue((K)key, fn));
  }
  public ImmutArrayMap<K,V> withMeta(IPersistentMap m) {
    return new ImmutArrayMap<K,V>((ArrayMap)ht.withMeta(m));
  }

}

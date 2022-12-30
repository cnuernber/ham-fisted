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

public class ImmutHashTable<K,V>
  extends NonEditableMapBase<K,V>
  implements IEditableCollection, IPersistentMap, IObj, ImmutValues {
  public ImmutHashTable(HashProvider hp) {
    super(new HashTable(hp, 0.75f, 0, 0, null, null));
  }
  public ImmutHashTable(HashTable ht) {
    super(ht);
  }
  public ImmutHashTable<K,V> shallowClone() {
    return new ImmutHashTable<K,V>((HashTable)ht.shallowClone());
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable<K,V> cons(Object obj) {
    return (ImmutHashTable<K,V>)(shallowClone().mutConj(obj));
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable<K,V> assoc(Object key, Object val) {
    return (ImmutHashTable<K,V>)(shallowClone().mutAssoc((K)key, (V)val));
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable<K,V> assocEx(Object key, Object val) {
    return (ImmutHashTable<K,V>)(shallowClone().mutAssoc((K)key, (V)val));
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable<K,V>  without(Object key) {
    return (ImmutHashTable<K,V>)(shallowClone().mutDissoc((K)key));
  }
  public ImmutHashTable<K,V> empty() {
    return new ImmutHashTable<K,V>(ht.hashProvider());
  }
  public ITransientCollection asTransient() {
    if(isEmpty())
      return new MutHashTable<K,V>((HashTable)ht);
    else
      return new TransientHashTable<K,V>((HashTable)ht);
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable<K,V> immutUpdateValues(BiFunction valueMap) {
    return (ImmutHashTable<K,V>)(shallowClone().mutUpdateValues(valueMap));
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable<K,V> immutUpdateValue(Object key, IFn fn) {
    return (ImmutHashTable<K,V>)(shallowClone().mutUpdateValue((K)key, fn));
  }
  public ImmutHashTable<K,V> withMeta(IPersistentMap m) {
    return new ImmutHashTable<K,V>((HashTable)ht.withMeta(m));
  }
}

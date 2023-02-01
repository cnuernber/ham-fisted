package ham_fisted;


import java.util.function.BiFunction;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;


public class LongTransientHashTable<K,V>
  extends NonEditableMapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj, UpdateValues {
  public LongTransientHashTable(LongHashTable ht) {
    super(ht);
  }
  public LongMutHashTable<K,V> clone() {
    return new LongMutHashTable<K,V>((LongHashTable)ht.clone());
  }
  public LongTransientHashTable<K,V> conj(Object val) {
    return (LongTransientHashTable<K,V>)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public LongTransientHashTable<K,V> assoc(Object key, Object val) {
    return (LongTransientHashTable<K,V>)mutAssoc((K)key,(V)val);
  }
  @SuppressWarnings("unchecked")
  public LongTransientHashTable<K,V> without(Object key) {
    return (LongTransientHashTable<K,V>)mutDissoc((K)key);
  }
  public LongTransientHashTable<K,V> withMeta(IPersistentMap m) {
    return new LongTransientHashTable<K,V>((LongHashTable)ht.withMeta(m));
  }
  IPersistentMap doPersistent() { return persistent(); }
  @SuppressWarnings("unchecked")
  public LongTransientHashTable<K,V> updateValues(BiFunction valueMap) {
    mutUpdateValues(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public UpdateValues updateValue(Object key, IFn fn) {
    mutUpdateValue((K)key,fn);
    return this;
  }
  public IPersistentMap persistent()  {
    return new LongImmutHashTable<K,V>((LongHashTable)ht);
  }
}

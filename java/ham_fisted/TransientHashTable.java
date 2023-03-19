package ham_fisted;


import java.util.function.BiFunction;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;

import static ham_fisted.BitmapTrieCommon.*;


public class TransientHashTable<K,V>
  extends NonEditableMapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj, UpdateValues, BitmapTrieCommon.MapSet,
	     HashTable.Owner {
  public TransientHashTable(HashTable ht) {
    super(ht);
  }
  public MutHashTable<K,V> clone() {
    return new MutHashTable<K,V>((HashTable)ht.clone());
  }
  public TransientHashTable<K,V> conj(Object val) {
    return (TransientHashTable<K,V>)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public TransientHashTable<K,V> assoc(Object key, Object val) {
    return (TransientHashTable<K,V>)mutAssoc((K)key,(V)val);
  }
  @SuppressWarnings("unchecked")
  public TransientHashTable<K,V> without(Object key) {
    return (TransientHashTable<K,V>)mutDissoc((K)key);
  }
  public HashTable getHashTable() { return (HashTable)ht; }
  public TransientHashTable<K,V> union(MapSet other, BiFunction mapper) {
    ((HashTable)ht).union(((HashTable.Owner)other).getHashTable(), mapper, false);
    return this;
  }
  public TransientHashTable<K,V> intersection(MapSet other, BiFunction mapper) {
    ((HashTable)ht).intersection(((HashTable.Owner)other).getHashTable(), mapper, false);
    return this;
  }
  public TransientHashTable<K,V> difference(MapSet other) {
    ((HashTable)ht).difference(((HashTable.Owner)other).getHashTable(), false);
    return this;
  }
  public TransientHashTable<K,V> withMeta(IPersistentMap m) {
    return new TransientHashTable<K,V>((HashTable)ht.withMeta(m));
  }
  IPersistentMap doPersistent() { return persistent(); }
  @SuppressWarnings("unchecked")
  public TransientHashTable<K,V> updateValues(BiFunction valueMap) {
    mutUpdateValues(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public UpdateValues updateValue(Object key, IFn fn) {
    mutUpdateValue((K)key,fn);
    return this;
  }
  public IPersistentMap persistent()  {
    return new ImmutHashTable((HashTable)ht);
  }
}

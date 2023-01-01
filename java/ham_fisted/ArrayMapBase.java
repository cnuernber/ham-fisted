package ham_fisted;


import clojure.lang.IFn;

public class ArrayMapBase<K,V> extends MapBase<K,V> {
  public ArrayMapBase(ArrayMap am) {
    super(am);
  }
  @Override
  MapBase<K,V> mutAssoc(K k, V v) {
    MapData md = ht.mutAssoc(k,v);
    if(md instanceof ArrayMap)
      return this;
    return new MutHashTable<K,V>((HashTable)md);
  }
  @Override
  MapBase<K,V> mutDissoc(K k) {
    ht.mutDissoc(k);
    return this;
  }
  @Override
  MapBase<K,V> mutUpdateValue(K k, IFn fn) {
    MapData md = ht.mutUpdateValue(k,fn);
    if(md instanceof ArrayMap)
      return this;
    return new MutHashTable<K,V>((HashTable)md);
  }
}

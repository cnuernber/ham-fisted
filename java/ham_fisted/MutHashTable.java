package ham_fisted;

import java.util.function.BiFunction;
import clojure.lang.IFn;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;

import static ham_fisted.BitmapTrieCommon.*;

public class MutHashTable<K,V>
  extends MapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj,
	     UpdateValues, MutableMap {
  public MutHashTable(HashProvider hp) {
    super(new HashTable(hp, 0.75f, 0, 0, null, null));
  }
  public MutHashTable(HashProvider hp, int initSize) {
    super(new HashTable(hp, 0.75f, (int)(initSize / 0.75f), 0, null, null));
  }
  public MutHashTable(HashProvider hp, IPersistentMap m, int initSize) {
    super(new HashTable(hp, 0.75f, (int)(initSize / 0.75f), 0, null, m));
  }
  public MutHashTable(HashTable ht) {
    super(ht);
  }
  public MutHashTable<K,V> clone() {
    return new MutHashTable<K,V>((HashTable)ht.clone());
  }
  public MutHashTable<K,V> conj(Object val) {
    return (MutHashTable<K,V>)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public MutHashTable<K,V> assoc(Object key, Object val) {
    put((K)key,(V)val);
    return this;
  }
  @SuppressWarnings("unchecked")
  public MutHashTable<K,V> without(Object key) {
    remove((K)key);
    return this;
  }
  public MutHashTable<K,V> withMeta(IPersistentMap m) {
    return new MutHashTable<K,V>((HashTable)ht.withMeta(m));
  }
  @SuppressWarnings("unchecked")
  public MutHashTable<K,V> updateValues(BiFunction valueMap) {
    replaceAll(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public MutHashTable<K,V> updateValue(Object key, IFn fn) {
    mutUpdateValue((K)key,fn);
    return this;
  }
  public IPersistentMap persistent()  {
    return new ImmutHashTable<K,V>((HashTable)ht);
  }

  @SuppressWarnings("unchecked")
  public static MutHashTable create(HashProvider hp, boolean byAssoc, Object[] data) {
    int nk = data.length/2;
    if((data.length % 2) != 0)
      throw new RuntimeException("Uneven number of keys-vals: " + data.length);
    MutHashTable rv = new MutHashTable(hp, null, nk);
    if(!byAssoc) {
      for(int idx = 0; idx < nk; ++idx) {
	final int kidx = idx*2;
	rv.put(data[kidx], data[kidx+1]);
	if(rv.size() != idx-1)
	  throw new RuntimeException("Duplicate key detected: " + data[kidx]);
      }
    } else {
      for(int idx = 0; idx < nk; ++idx) {
	final int kidx = idx*2;
	rv.put(data[kidx], data[kidx+1]);
      }
    }
    return rv;
  }
}

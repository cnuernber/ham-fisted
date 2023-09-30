package ham_fisted;

import java.util.Map;
import java.util.function.BiFunction;
import clojure.lang.IFn;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;

import static ham_fisted.BitmapTrieCommon.*;

public class LongMutHashTable<K,V>
  extends MapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj,
	     UpdateValues, MutableMap, MapSetOps,
	     LongHashTable.Owner {
  public LongMutHashTable() {
    super(new LongHashTable(0.75f, 0, 0, null, null));
  }
  public LongMutHashTable(int initSize) {
    super(new LongHashTable(0.75f, (int)(initSize / 0.75f), 0, null, null));
  }
  public LongMutHashTable(IPersistentMap m, int initSize) {
    super(new LongHashTable(0.75f, (int)(initSize / 0.75f), 0, null, m));
  }
  public LongMutHashTable(LongHashTable ht) {
    super(ht);
  }
  public LongMutHashTable<K,V> clone() {
    return new LongMutHashTable<K,V>((LongHashTable)ht.clone());
  }
  public LongMutHashTable<K,V> conj(Object val) {
    return (LongMutHashTable<K,V>)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public LongMutHashTable<K,V> assoc(Object key, Object val) {
    put((K)key,(V)val);
    return this;
  }
  @SuppressWarnings("unchecked")
  public LongMutHashTable<K,V> without(Object key) {
    remove((K)key);
    return this;
  }
  public LongMutHashTable<K,V> withMeta(IPersistentMap m) {
    if(m == meta())
      return this;
    return new LongMutHashTable<K,V>((LongHashTable)ht.withMeta(m));
  }
  @SuppressWarnings("unchecked")
  public LongMutHashTable<K,V> updateValues(BiFunction valueMap) {
    replaceAll(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public LongMutHashTable<K,V> updateValue(Object key, IFn fn) {
    mutUpdateValue((K)key,fn);
    return this;
  }
  public IPersistentMap persistent()  {
    return new LongImmutHashTable<K,V>((LongHashTable)ht);
  }
  public LongHashTable getLongHashTable() { return (LongHashTable)ht; }
  public LongMutHashTable union(Map other, BiFunction mapper) {
    ((LongHashTable)ht).union(((LongHashTable.Owner)other).getLongHashTable(), mapper, false);
    return this;
  }
  public LongMutHashTable intersection(Map other, BiFunction mapper) {
    ((LongHashTable)ht).intersection(((LongHashTable.Owner)other).getLongHashTable(), mapper, false);
    return this;
  }
  public LongMutHashTable difference(Map other) {
    ((LongHashTable)ht).difference(((LongHashTable.Owner)other).getLongHashTable(), false);
    return this;
  }

  @SuppressWarnings("unchecked")
  public static LongMutHashTable create(boolean byAssoc, Object[] data) {
    int nk = data.length/2;
    if((data.length % 2) != 0)
      throw new RuntimeException("Uneven number of keys-vals: " + data.length);
    LongMutHashTable rv = new LongMutHashTable(null, nk);
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

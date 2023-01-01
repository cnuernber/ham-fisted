package ham_fisted;

import java.util.function.BiFunction;
import clojure.lang.IFn;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;

import static ham_fisted.BitmapTrieCommon.*;

/**
 * Implementation of the java.util.Map interface backed by a bitmap
 * trie.
 */
public final class MutBitmapTrie<K,V>
  extends MapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj,
	     UpdateValues, MutableMap
{
  public MutBitmapTrie(HashProvider hp) {
    super(new BitmapTrie(hp));
  }
  public MutBitmapTrie(BitmapTrie ht) {
    super(ht);
  }
  public MutBitmapTrie<K,V> clone() {
    return new MutBitmapTrie<K,V>((BitmapTrie)ht.clone());
  }
  public MutBitmapTrie<K,V> conj(Object val) {
    return (MutBitmapTrie<K,V>)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public MutBitmapTrie<K,V> assoc(Object key, Object val) {
    return (MutBitmapTrie<K,V>)mutAssoc((K)key,(V)val);
  }
  @SuppressWarnings("unchecked")
  public MutBitmapTrie<K,V> without(Object key) {
    return (MutBitmapTrie<K,V>)mutDissoc((K)key);
  }
  public MutBitmapTrie<K,V> withMeta(IPersistentMap m) {
    return new MutBitmapTrie<K,V>((BitmapTrie)ht.withMeta(m));
  }
  @SuppressWarnings("unchecked")
  public MutBitmapTrie<K,V> updateValues(BiFunction valueMap) {
    replaceAll(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public MutBitmapTrie<K,V> updateValue(Object key, IFn fn) {
    mutUpdateValue((K)key,fn);
    return this;
  }
  public IPersistentMap persistent() {
    return new ImmutBitmapTrie<K,V>((BitmapTrie)ht);
  }

  @SuppressWarnings("unchecked")
  public static MutBitmapTrie create(HashProvider hp, boolean byAssoc, Object[] data) {
    int nk = data.length/2;
    if((data.length % 2) != 0)
      throw new RuntimeException("Uneven number of keys-vals: " + data.length);
    MutBitmapTrie rv = new MutBitmapTrie(hp);
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

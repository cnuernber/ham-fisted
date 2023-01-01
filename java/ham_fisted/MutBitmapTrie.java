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
	     UpdateValues
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
}

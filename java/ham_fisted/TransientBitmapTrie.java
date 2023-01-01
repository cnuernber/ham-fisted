package ham_fisted;


import java.util.function.BiFunction;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;


public class TransientBitmapTrie<K,V>
  extends NonEditableMapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj, UpdateValues {
  public TransientBitmapTrie(BitmapTrie ht) {
    super(ht);
  }
  public MutBitmapTrie<K,V> clone() {
    return new MutBitmapTrie<K,V>((BitmapTrie)ht.clone());
  }
  public TransientBitmapTrie<K,V> conj(Object val) {
    return (TransientBitmapTrie<K,V>)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public TransientBitmapTrie<K,V> assoc(Object key, Object val) {
    return (TransientBitmapTrie<K,V>)mutAssoc((K)key,(V)val);
  }
  @SuppressWarnings("unchecked")
  public TransientBitmapTrie<K,V> without(Object key) {
    return (TransientBitmapTrie<K,V>)mutDissoc((K)key);
  }
  public TransientBitmapTrie<K,V> withMeta(IPersistentMap m) {
    return new TransientBitmapTrie<K,V>((BitmapTrie)ht.withMeta(m));
  }
  IPersistentMap doPersistent() { return persistent(); }
  @SuppressWarnings("unchecked")
  public TransientBitmapTrie<K,V> updateValues(BiFunction valueMap) {
    mutUpdateValues(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public UpdateValues updateValue(Object key, IFn fn) {
    mutUpdateValue((K)key,fn);
    return this;
  }
  public IPersistentMap persistent() {
    return new ImmutBitmapTrie<K,V>((BitmapTrie)ht);
  }
}

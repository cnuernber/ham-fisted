package ham_fisted;



import java.util.function.BiFunction;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;


public class TransientArrayMap<K,V>
  extends NonEditableArrayMapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj, UpdateValues {
  public TransientArrayMap(ArrayMap ht) {
    super(ht);
  }
  public MutArrayMap<K,V> clone() {
    return new MutArrayMap<K,V>((ArrayMap)ht.clone());
  }
  public ITransientMap conj(Object val) {
    return (ITransientMap)mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public ITransientMap assoc(Object key, Object val) {
    return (ITransientMap)mutAssoc((K)key,(V)val);
  }
  @SuppressWarnings("unchecked")
  public TransientArrayMap<K,V> without(Object key) {
    return (TransientArrayMap<K,V>)mutDissoc((K)key);
  }
  public TransientArrayMap<K,V> withMeta(IPersistentMap m) {
    return new TransientArrayMap<K,V>((ArrayMap)ht.withMeta(m));
  }
  IPersistentMap doPersistent() { return persistent(); }
  @SuppressWarnings("unchecked")
  public TransientArrayMap<K,V> updateValues(BiFunction valueMap) {
    mutUpdateValues(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public UpdateValues updateValue(Object key, IFn fn) {
    return (UpdateValues)mutUpdateValue((K)key,fn);
  }
  public IPersistentMap persistent()  {
    return new ImmutArrayMap<K,V>((ArrayMap)ht);
  }
}

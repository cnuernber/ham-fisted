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

public class LongImmutHashTable<K,V>
  extends NonEditableMapBase<K,V>
  implements IEditableCollection, IPersistentMap, IObj, ImmutValues, BitmapTrieCommon.MapSet,
	     LongHashTable.Owner {
  public LongImmutHashTable(HashProvider hp) {
    super(new LongHashTable(0.75f, 0, 0, null, null));
  }
  public LongImmutHashTable(LongHashTable ht) {
    super(ht);
  }
  public LongImmutHashTable<K,V> shallowClone() {
    return new LongImmutHashTable<K,V>((LongHashTable)ht.shallowClone());
  }
  @SuppressWarnings("unchecked")
  public LongImmutHashTable<K,V> cons(Object obj) {
    return (LongImmutHashTable<K,V>)(shallowClone().mutConj(obj));
  }
  @SuppressWarnings("unchecked")
  public LongImmutHashTable<K,V> assoc(Object key, Object val) {
    return (LongImmutHashTable<K,V>)(shallowClone().mutAssoc((K)key, (V)val));
  }
  @SuppressWarnings("unchecked")
  public LongImmutHashTable<K,V> assocEx(Object key, Object val) {
    return (LongImmutHashTable<K,V>)(shallowClone().mutAssoc((K)key, (V)val));
  }
  @SuppressWarnings("unchecked")
  public LongImmutHashTable<K,V>  without(Object key) {
    return (LongImmutHashTable<K,V>)(shallowClone().mutDissoc((K)key));
  }
  public LongImmutHashTable<K,V> empty() {
    return new LongImmutHashTable<K,V>(ht.hashProvider());
  }
  public ITransientCollection asTransient() {
    if(isEmpty())
      return new LongMutHashTable<K,V>((LongHashTable)ht.shallowClone());
    else
      return new LongTransientHashTable<K,V>((LongHashTable)ht.shallowClone());
  }
  public LongHashTable getLongHashTable() { return (LongHashTable)ht; }
  public LongImmutHashTable union(MapSet other, BiFunction mapper) {
    return new LongImmutHashTable(((LongHashTable)ht).union(((LongHashTable.Owner)other).getLongHashTable(), mapper, true));
  }
  public LongImmutHashTable intersection(MapSet other, BiFunction mapper) {
    return new LongImmutHashTable(((LongHashTable)ht).intersection(((LongHashTable.Owner)other).getLongHashTable(), mapper, true));
  }
  public LongImmutHashTable difference(MapSet other) {
    return new LongImmutHashTable(((LongHashTable)ht).difference(((LongHashTable.Owner)other).getLongHashTable(), true));
  }
  @SuppressWarnings("unchecked")
  public LongImmutHashTable<K,V> immutUpdateValues(BiFunction valueMap) {
    return (LongImmutHashTable<K,V>)(shallowClone().mutUpdateValues(valueMap));
  }
  @SuppressWarnings("unchecked")
  public LongImmutHashTable<K,V> immutUpdateValue(Object key, IFn fn) {
    return (LongImmutHashTable<K,V>)(shallowClone().mutUpdateValue((K)key, fn));
  }
  public LongImmutHashTable<K,V> withMeta(IPersistentMap m) {
    if(m == meta())
      return this;
    return new LongImmutHashTable<K,V>((LongHashTable)ht.withMeta(m));
  }
}

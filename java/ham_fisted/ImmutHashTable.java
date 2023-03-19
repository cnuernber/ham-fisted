package ham_fisted;

import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.IEditableCollection;
import clojure.lang.ITransientCollection;
import clojure.lang.ITransientMap;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;
import clojure.lang.IFn;

import static ham_fisted.BitmapTrieCommon.*;

public class ImmutHashTable
  extends APersistentMapBase
  implements IEditableCollection, IPersistentMap, IObj, ImmutValues, BitmapTrieCommon.MapSet,
	     HashTable.Owner {
  public ImmutHashTable(HashProvider hp) {
    super(new HashTable(hp, 0.75f, 0, 0, null, null));
  }
  public ImmutHashTable(HashTable ht) {
    super(ht);
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable cons(Object obj) {
    return (ImmutHashTable)((ITransientCollection)asTransient()).conj(obj).persistent();
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable assoc(Object key, Object val) {
    return (ImmutHashTable)(asTransient().assoc(key,val).persistent());
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable assocEx(Object key, Object val) {
    if(containsKey(key))
      throw new RuntimeException("Object already contains key :" + String.valueOf(key));
    return (ImmutHashTable)asTransient().assoc(key,val).persistent();
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable  without(Object key) {
    return (ImmutHashTable)asTransient().without(key).persistent();
  }
  public ImmutHashTable empty() {
    return new ImmutHashTable(ht.hashProvider());
  }
  public ITransientMap asTransient() {
    if(isEmpty())
      return new MutHashTable((HashTable)ht.shallowClone());
    else
      return new TransientHashTable((HashTable)ht.shallowClone());
  }
  public HashTable getHashTable() { return (HashTable)ht; }
  public ImmutHashTable intersection(MapSet other, BiFunction mapper) {
    throw new RuntimeException("Unimplemented");
  }
  public ImmutHashTable union(MapSet other, BiFunction mapper) {
    return new ImmutHashTable(((HashTable)ht).union(((HashTable.Owner)other).getHashTable(), mapper, true));
  }
  public ImmutHashTable difference(MapSet other) {
    throw new RuntimeException("Unimplemented");
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable immutUpdateValues(BiFunction valueMap) {
    return (ImmutHashTable)((ImmutValues)asTransient()).immutUpdateValues(valueMap);
  }
  @SuppressWarnings("unchecked")
  public ImmutHashTable immutUpdateValue(Object key, IFn fn) {
    return (ImmutHashTable)((ImmutValues)asTransient()).immutUpdateValue(key,fn);
  }
  public ImmutHashTable withMeta(IPersistentMap m) {
    if(m == meta())
      return this;
    return new ImmutHashTable((HashTable)ht.withMeta(m));
  }
}

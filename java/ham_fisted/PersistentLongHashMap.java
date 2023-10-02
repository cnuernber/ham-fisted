package ham_fisted;

import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.Collection;
import java.util.Set;
import java.util.Map;
import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.IObj;
import clojure.lang.Indexed;

public class PersistentLongHashMap
  extends ROLongHashMap
  implements IAPersistentMap, IObj {
  public static final PersistentLongHashMap EMPTY = new PersistentLongHashMap(new LongHashMap());
  int _hasheq = 0;

  public PersistentLongHashMap(LongHashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data, data.meta);
  }
  public PersistentLongHashMap(LongHashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }
  public int count() { return length; }
  public int hasheq() {
    if (_hasheq == 0)
      _hasheq = super.hasheq();
    return _hasheq;
  }
  public ITransientMap asTransient() {
    return isEmpty() ? new UnsharedLongHashMap(meta) :
      new TransientLongHashMap(this);
  }
  public PersistentLongHashMap withMeta(IPersistentMap m) {
    return new PersistentLongHashMap(this, m);
  }
  public PersistentLongHashMap empty() { return EMPTY; }
  public PersistentLongHashMap union(Map o, BiFunction bfn) {
    return new PersistentLongHashMap(union(shallowClone(), o, bfn));
  }
  public PersistentLongHashMap intersection(Map o, BiFunction bfn) {
    return new PersistentLongHashMap(intersection(shallowClone(), o, bfn));
  }
  public PersistentLongHashMap intersection(Set o) {
    return new PersistentLongHashMap(intersection(shallowClone(), o));
  }
  public PersistentLongHashMap difference(Collection c) {
    return new PersistentLongHashMap(difference(shallowClone(), c));
  }
  public PersistentLongHashMap updateValues(BiFunction valueMap) {
    return new PersistentLongHashMap(updateValues(shallowClone(), valueMap));
  }
  public PersistentLongHashMap updateValue(Object k, Function fn) {
    return new PersistentLongHashMap(updateValue(this, fn));
  }
}

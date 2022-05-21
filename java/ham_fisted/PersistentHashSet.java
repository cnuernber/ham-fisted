package ham_fisted;

import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import clojure.lang.APersistentSet;
import clojure.lang.IPersistentSet;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentCollection;
import clojure.lang.ITransientSet;
import clojure.lang.AFn;
import clojure.lang.IHashEq;
import clojure.lang.IObj;
import clojure.lang.IteratorSeq;
import clojure.lang.ISeq;
import clojure.lang.ILookup;
import clojure.lang.IEditableCollection;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;


public class PersistentHashSet
  implements IPersistentSet, Collection, Set, Serializable, IHashEq, IObj,
	     MapSet, BitmapTrieOwner, ILookup, IEditableCollection, IFnDef {
  final BitmapTrie hb;
  int cachedHash = 0;

  public static final PersistentHashSet EMPTY = new PersistentHashSet();

  public PersistentHashSet() { hb = new BitmapTrie(equalHashProvider); }
  public PersistentHashSet(HashProvider hp) { hb = new BitmapTrie(hp); }
  PersistentHashSet(BitmapTrie _hb) { hb = _hb; }
  public final int hashCode() {
    if (cachedHash == 0) {
      return cachedHash = CljHash.setHashcode(hb);
    }
    return cachedHash;
  }

  public final int hasheq() {
    return hashCode();
  }

  public final boolean equals(Object o) {
    return CljHash.setEquiv(hb, o);
  }
  public final boolean equiv(Object o) {
    return equals(o);
  }

  public BitmapTrie bitmapTrie() { return hb; }
  public final int count() { return hb.size(); }
  public final int size() { return hb.size(); }
  public final void clear() { hb.clear(); }
  public final boolean containsAll(Collection values) {
    boolean retval = true;
    for (Object e : values)
      retval = retval && contains(e);
    return retval;
  }
  public final boolean removeAll(Collection values) {
    throw new RuntimeException("Unimplemented");
  }

  public final boolean retainAll(Collection values) {
    throw new RuntimeException("Unimplemented");
  }

  public final boolean addAll(Collection values) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean remove(Object e) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean add(Object e) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean isEmpty() { return hb.size() == 0; }

  public final IPersistentSet disjoin(Object key) {
    return new PersistentHashSet(hb.shallowClone().dissoc(key));
  }

  public final boolean contains(Object key) {
    return hb.getNode(key) != null;
  }

  public final Object get(Object key) {
    return contains(key) ? key : null;
  }

  public final IPersistentSet cons(Object o) {
    return new PersistentHashSet(hb.shallowClone().assoc(o, HashSet.PRESENT));
  }

  public final IPersistentCollection empty(){
    return EMPTY.withMeta(meta());
  }

  public final IPersistentMap meta() { return hb.meta(); }
  public final PersistentHashSet withMeta(IPersistentMap meta) {
    if(meta() == meta)
      return this;
    return new PersistentHashSet(hb.shallowClone(meta));
  }

  public final ISeq seq() { return IteratorSeq.create(iterator()); }

  public final Iterator iterator() { return hb.iterator(keyIterFn); }

  public PersistentHashSet intersection(MapSet rhs, BiFunction valueMap) {
    return new PersistentHashSet(hb.intersection(((BitmapTrieOwner)rhs).bitmapTrie(),
						 valueMap));
  }
  public PersistentHashSet union(MapSet rhs, BiFunction valueMap) {
    return new PersistentHashSet(hb.union(((BitmapTrieOwner)rhs).bitmapTrie(),
					  valueMap));
  }
  public PersistentHashSet difference(MapSet rhs) {
    return new PersistentHashSet(hb.difference(((BitmapTrieOwner)rhs).bitmapTrie()));
  }
  public PersistentHashSet immutUpdateValues(BiFunction valueMap) {
    throw new RuntimeException("Unimplemented");
  }
  public PersistentHashSet immutUpdateValue(Object key, Function valueMap) {
    throw new RuntimeException("Unimplemented");
  }
  public final Object[] toArray() {
    Object[] retval = new Object[size()];
    int idx = 0;
    for(Object obj: this) {
      retval[idx] = obj;
      idx++;
    }
    return retval;
  }
  public final Object[] toArray(Object[] data) {
    Object[] retval = Arrays.copyOf(data, size());
    int idx = 0;
    for(Object obj: this) {
      retval[idx] = obj;
      idx++;
    }
    return retval;
  }
  public final Object valAt(Object key) {
    return get(key);
  }
  public final Object valAt(Object key, Object notFound) {
    return contains(key) ? key : notFound;
  }
  public final Object invoke(Object key) {
    return get(key);
  }
  public final Object invoke(Object key, Object notFound) {
    return contains(key) ? key : notFound;
  }

  public ITransientSet asTransient() {
    if (hb.size() == 0)
      return new HashSet(equalHashProvider);
    return new TransientHashSet(hb);
  }

}

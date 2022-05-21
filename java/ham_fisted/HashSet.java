package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import clojure.lang.IObj;
import clojure.lang.ITransientSet;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientCollection;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.IteratorSeq;
import clojure.lang.ILookup;
import clojure.lang.IHashEq;
import clojure.lang.IPersistentCollection;


public class HashSet<E>
  extends AbstractSet<E>
  implements MapSet, BitmapTrieOwner, IObj, ITransientSet, Seqable,
	     ILookup, IFnDef, IHashEq {
  BitmapTrie hb;

  public static final Object PRESENT = new Object();
  public static final BiFunction<Object,Object,Object> setValueMapper = (a,b) -> PRESENT;

  public HashSet(){hb = new BitmapTrie(equalHashProvider);}
  public HashSet(HashProvider hp) {
    hb = new BitmapTrie(hp);
  }
  HashSet(BitmapTrie _hb) { hb = _hb; }
  public BitmapTrie bitmapTrie() { return hb; }
  public HashSet<E> clone() { return new HashSet<E>(hb.deepClone()); }
  public final int hashCode() {
    return CljHash.setHashcode(hb);
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
  public final boolean add(E e) {
    return (hb.getOrCreate(e).val(PRESENT)) == null;
  }
  public final void clear() {
    hb.clear();
  }
  public final boolean contains(Object e) {
    return hb.getNode(e) != null;
  }
  public final boolean isEmpty() { return hb.size() == 0; }
  @SuppressWarnings("unchecked")
  public final Iterator<E> iterator() {
    return hb.iterator(keyIterFn);
  }
  public final ISeq seq() { return IteratorSeq.create(iterator()); }
  public final boolean remove(Object k) { return hb.remove(k) != null; }
  public final int size() { return hb.size(); }
  public final Object[] toArray() {
    Object[] data = new Object[hb.size()];
    Iterator iter = iterator();
    int idx = 0;
    while(iter.hasNext()) {
      data[idx] = iter.next();
      ++idx;
    }
    return data;
  }
  @SuppressWarnings("unchecked")
  public final <T> T[] toArray(T[] ary) {
    T[] data = Arrays.copyOf(ary, hb.size());
    Iterator iter = iterator();
    int idx = 0;
    while(iter.hasNext()) {
      data[idx] = (T)iter.next();
      ++idx;
    }
    return data;
  }


  public final int count() { return hb.size(); }


  public PersistentHashSet intersection(MapSet rhs, BiFunction valueMap) {
    return new PersistentHashSet(hb.intersection(((BitmapTrieOwner)rhs).bitmapTrie(),
						 valueMap));
  }
  public final PersistentHashSet union(MapSet rhs, BiFunction valueMap) {
    return new PersistentHashSet(hb.union(((BitmapTrieOwner)rhs).bitmapTrie(),
					  valueMap));
  }
  public final PersistentHashSet difference(MapSet rhs) {
    return new PersistentHashSet(hb.difference(((BitmapTrieOwner)rhs).bitmapTrie()));
  }
  public final PersistentHashSet immutUpdateValues(BiFunction valueMap) {
    throw new RuntimeException("Unimplemented");
  }
  public final PersistentHashSet immutUpdateValue(Object key, Function valueMap) {
    throw new RuntimeException("Unimplemented");
  }

  public final IPersistentMap meta() { return hb.meta(); }
  public final HashSet<E> withMeta(IPersistentMap meta) {
    return new HashSet<E>(hb.shallowClone(meta));
  }
  //Transient implementation
  public final ITransientSet disjoin(Object key) {
    remove(key);
    return this;
  }
  public final Object get(Object key) {
    return contains(key) ? key : null;
  }
  @SuppressWarnings("unchecked")
  public final HashSet conj(Object key) {
    add((E)key);
    return this;
  }
  public final PersistentHashSet persistent() {
    return new PersistentHashSet(hb);
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
}

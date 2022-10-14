package ham_fisted;

import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.ChunkedList.*;

import java.util.List;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collection;
import java.util.Arrays;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.BiFunction;

import clojure.lang.Indexed;
import clojure.lang.IReduce;
import clojure.lang.IKVReduce;
import clojure.lang.IHashEq;
import clojure.lang.Seqable;
import clojure.lang.Reversible;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.IObj;
import clojure.lang.IEditableCollection;
import clojure.lang.Murmur3;
import clojure.lang.Util;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.lang.IteratorSeq;
import clojure.lang.ISeq;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.ITransientVector;


public class ArrayImmutList
  implements IMutList, RandomAccess, Indexed, IFnDef, IReduce, IKVReduce,
	     IHashEq, Seqable, Reversible, ChunkedListOwner, IPersistentVector,
	     IObj, IEditableCollection, ImmutValues
{
  final Object[] data;
  public final int startidx;
  public final int nElems;
  public final IPersistentMap m = null;
  int _hash = 0;

  public static final ArrayImmutList EMPTY = new ArrayImmutList(new Object[0], 0,0,null);

  public ArrayImmutList(Object[] d, int sidx, int eidx, IPersistentMap meta ) {
    data = d;
    startidx = sidx;
    nElems = eidx - sidx;
  }
  public static ArrayImmutList create(boolean owning, Object[] d,
				      int sidx, int eidx, IPersistentMap meta) {
    d = owning ? d : d.clone();
    return new ArrayImmutList(d, sidx, eidx, meta);
  }
  public static IPersistentVector create(boolean owning, IPersistentMap meta, Object... data) {
    if (data.length > 32)
      return ImmutList.create(owning, meta, data);
    return create(owning, data, 0, data.length, meta);
  }
  final int indexCheck(int idx) {
    return ChunkedList.indexCheck(startidx, nElems, idx);
  }
  final int wrapIndexCheck(int idx) {
    return ChunkedList.wrapIndexCheck(startidx, nElems, idx);
  }
  public ChunkedListSection getChunkedList() {
    return new ChunkedListSection(ChunkedList.create(true, null, data).data, startidx, startidx+nElems);
  }
  public final int hashCode() {
    if (_hash == 0) {
      final int ne = nElems;
      final int eidx = startidx + ne;
      int hash = 1;
      final Object[] mdata = data;
      for(int sidx = startidx; sidx < eidx; ++sidx) {
	hash = 31 * hash + Util.hasheq(mdata[sidx]);
      }
      _hash = hash = Murmur3.mixCollHash(hash, ne);
    }
    return _hash;
  }
  public final int hasheq() { return hashCode(); }
  public final boolean equiv(HashProvider hp, Object other) {
    if(other == this) return true;
    if(other == null) return false;
    final int ne = nElems;
    if(other instanceof ArrayImmutList) {
      final ArrayImmutList olist = (ArrayImmutList)other;
      if(olist.nElems != ne) return false;
      final int sidx = startidx;
      final int osidx = olist.startidx;
      final Object[] mdata = data;
      final Object[] odata = olist.data;
      for(int idx = 0; idx < ne; ++idx)
	if(!hp.equals(mdata[idx+sidx], odata[idx+osidx]))
	  return false;
      return true;
    } else {
      return CljHash.listEquiv(this, other);
    }
  }
  public final boolean equiv(Object other) {
    return equiv(defaultHashProvider, other);
  }
  public final boolean equals(Object other) {
    return equiv(other);
  }
  public final String toString() {
    return Transformables.sequenceToString(this);
  }
  public final IPersistentMap meta() { return m; }
  public final ArrayImmutList withMeta(IPersistentMap m) {
    return new ArrayImmutList(data, startidx, startidx+nElems, m);
  }
  public void clear() { throw new RuntimeException("Unimplemented"); }
  public boolean remove(Object c) { throw new RuntimeException("Unimplemented"); }
  public Character remove(int idx) { throw new RuntimeException("Unimplemented"); }
  public boolean add(Object c) { throw new RuntimeException("Unimplemented"); }
  public void add(int idx, Object c) { throw new RuntimeException("Unimplemented"); }
  public boolean addAll(int idx, Collection c)
  { throw new RuntimeException("Unimplemented"); }
  public Character set(int idx, Object c) { throw new RuntimeException("Unimplemented"); }
  public boolean retainAll(Collection c) { throw new RuntimeException("Unimplemented"); }
  public boolean removeAll(Collection c) { throw new RuntimeException("Unimplemented"); }
  public boolean addAll(Collection c) { throw new RuntimeException("Unimplemented"); }
  public Object get(int idx) { return data[indexCheck(idx)]; }
  public final int indexOf(Object obj) {
    final int ne = nElems;
    final int sidx = startidx;
    final Object[] mdata = data;
    for(int idx = 0; idx < ne; ++idx)
      if (Objects.equals(obj, mdata[idx+sidx]))
	return idx;
    return -1;
  }
  public final int lastIndexOf(Object obj) {
    final int ne = nElems;
    final int nne = ne - 1;
    final int sidx = startidx;
    final Object[] mdata = data;
    for(int idx = 0; idx < ne; ++idx) {
      int ridx = nne - idx;
      if (Objects.equals(obj, mdata[ridx+sidx]))
	return ridx;
    }
    return -1;
  }
  public final int size() { return nElems; }
  public final int length() { return nElems; }
  public final int count() { return nElems; }
  public final boolean contains(Object obj) {
    return indexOf(obj) != -1;
  }
  public final boolean containsAll(Collection c) {
    final int ne = nElems;
    Iterator minC;
    Iterator maxC;
    if (ne < c.size()) {
      minC = this.iterator();
      maxC = c.iterator();
    } else {
      maxC = this.iterator();
      minC = c.iterator();
    }
    //This set can contain null.
    HashSet<Object> hc = new HashSet<Object>();
    while(minC.hasNext()) hc.add(minC.next());
    while(maxC.hasNext()) {
      if (!hc.contains(maxC.next()))
	return false;
    }
    return true;
  }
  public boolean isEmpty() { return nElems == 0; }
  public Object[] fillArray(Object[] retval) {
    System.arraycopy(data, startidx, retval, 0, nElems);
    return retval;
  }
  public Object[] toArray() {
    return fillArray(new Object[nElems]);
  }
  public Object[] toArray(Object[] marker) {
    Object[] retval = Arrays.copyOf(marker, nElems);
    fillArray(retval);
    return retval;
  }
  public ListIterator<Character> listIterator(int idx) {
    throw new RuntimeException("Unimplemented");
  }
  public ListIterator<Character> listIterator() {
    return listIterator(0);
  }
  public ArrayImmutList subList(int sidx, int endidx) {
    ChunkedList.sublistCheck(sidx, endidx, nElems);
    return new ArrayImmutList(data, sidx+startidx, endidx + startidx, m);
  }
  static class Iter implements Iterator {
    final Object[] data;
    final int endidx;
    int idx;
    public Iter(Object[] d, int startidx, int nElems) {
      data = d;
      idx = startidx;
      endidx = startidx + nElems;
    }
    public final boolean hasNext() { return idx < endidx; }
    public final Object next() {
      if (!hasNext())
	throw new NoSuchElementException();
      final Object retval = data[idx];
      ++idx;
      return retval;
    }
  }
  public final Iterator iterator() { return new Iter(data, startidx, nElems); }
  static class RIter implements Iterator {
    final Object[] data;
    final int nne;
    final int sidx;

    int idx;
    public RIter(Object[] d, int startidx, int nElems) {
      data = d;
      nne = nElems - 1;
      sidx = startidx + nne;
      idx = 0;
    }
    public final boolean hasNext() { return idx <= nne; }
    public final Object next() {
      if (!hasNext())
	throw new NoSuchElementException();
      final Object retval = data[sidx - idx];
      ++idx;
      return retval;
    }
  }
  public final Iterator riterator() { return new RIter(data, startidx, nElems); }
  public final Object nth(int idx, Object notFound) {
    final int ne = nElems;
    if (idx < 0)
      idx = idx + ne;
    if (idx >= ne) return notFound;
    return data[idx+startidx];
  }
  public final Object nth(int idx) {
    return data[wrapIndexCheck(idx)];
  }
  public final Object invoke(Object idx) {
    if (Util.isInteger(idx))
      return nth(RT.intCast(idx));
    return null;
  }
  public final Object invoke(Object idx, Object notFound) {
    if (Util.isInteger(idx))
      return nth(RT.intCast(idx), notFound);
    return notFound;
  }
  public final Object valAt(Object idx) {
    return invoke(idx);
  }
  public final Object valAt(Object idx, Object notFound) {
    return invoke(idx, notFound);
  }
  public final IMapEntry entryAt(Object key) {
    if(Util.isInteger(key)) {
      final int idx = RT.intCast(key);
      if (idx >= 0 && idx < nElems)
	return MapEntry.create(idx, get(idx));
    }
    return null;
  }
  public final boolean containsKey(Object key) {
    if(Util.isInteger(key)) {
      final int idx = RT.intCast(key);
      return idx >= 0 && idx < nElems;
    }
    return false;
  }
  public final ArrayImmutList empty() { return EMPTY.withMeta(m); }
  public final Object reduce(IFn f, int sidx, Object init) {
    final int endidx = startidx + nElems;
    final Object[] d = data;
    while(sidx < endidx && !RT.isReduced(init)) {
      init = f.invoke(init, d[sidx]);
      sidx++;
    }
    return init;
  }
  public final Object reduce(IFn f, Object init) {
    return reduce(f, startidx, init);
  }
  public final Object reduce(IFn f) {
    if(nElems == 0)
      return f.invoke();
    return reduce(f, startidx+1, get(0));
  }
  public final Object kvreduce(IFn fn, Object init) {
    final int sidx = startidx;
    final Object[] d = data;
    final int ne = nElems;
    for(int idx = 0; idx < ne && !RT.isReduced(init); ++idx) {
      init = fn.invoke(init, idx, d[sidx+idx]);
    }
    return init;
  }
  public final ISeq seq() { return IteratorSeq.create(iterator()); }
  public final ISeq rseq() { return IteratorSeq.create(riterator()); }
  public final IPersistentVector cons(Object obj) {
    final int ne = nElems;
    switch(ne){
    case 0: return new ArrayImmutList(new Object[]{ obj}, 0, 1, m);
    case 1: return new ArrayImmutList(new Object[]{ data[startidx], obj}, 0, 2, m);
    case 2: return new ArrayImmutList(new Object[]{ data[startidx], data[startidx+1], obj},
				      0, 3, m);
    case 3: return new ArrayImmutList(new Object[]{ data[startidx], data[startidx+1], data[startidx+2], obj},
				      0, 4, m);
    }
    final int nne = nElems + 1;
    Object[] newD = Arrays.copyOfRange(data, startidx, startidx + nne);
    newD[ne] = obj;
    if(nne == 32) {
      return new ImmutList(0, nne, new ChunkedList(new Object[][] {newD}, nne, nne, m));
    } else {
      return new ArrayImmutList(newD, 0, nne, m);
    }
  }
  public final IPersistentVector assocN(int idx, Object obj) {
    final int ne = nElems;
    final int sidx = startidx;
    if (idx == ne)
      return cons(obj);
    indexCheck(idx);
    Object[] newD = Arrays.copyOfRange(data, sidx, sidx+ne);
    newD[idx] = obj;
    return new ArrayImmutList(newD, 0, ne, m);
  }
  public final IPersistentVector assoc(Object idx, Object obj) {
    if (!Util.isInteger(idx))
      throw new RuntimeException("Vector indexes must be integers: " + String.valueOf(idx));
    return assocN(RT.intCast(idx), obj);
  }
  public final ArrayImmutList pop() {
    final int ne = nElems;
    if (ne == 0)
      throw new RuntimeException("Attempt to pop empty vector");
    final Object[] newD = Arrays.copyOfRange(data, startidx, startidx + ne-1);
    return new ArrayImmutList(newD, 0, ne-1, m);
  }
  public final Object peek() {
    if (nElems == 0)
      return null;
    return get(nElems-1);
  }
  public final MutList<Object> asTransient() {
    if (nElems == 0)
      return new MutList<Object>();
    return MutList.create(true, meta(), Arrays.copyOfRange(data, startidx, startidx+nElems));
  }
  @SuppressWarnings("unchecked")
  public final ArrayImmutList immutUpdateValue(Object key, IFn fn) {
    int idx = RT.intCast(key);
    if (idx < 0)
      idx = nElems + idx;
    if(idx >= nElems)
      throw new RuntimeException("Index out of range: " + String.valueOf(key));
    final Object[] newD = Arrays.copyOfRange(data, startidx, startidx+nElems);
    newD[idx] = fn.invoke(newD[idx]);
    return new ArrayImmutList(newD, 0, nElems, m);
  }
  @SuppressWarnings("unchecked")
  public final ArrayImmutList immutUpdateValues(BiFunction fn) {
    final int ne = nElems;
    final Object[] newD = Arrays.copyOfRange(data, startidx, startidx+nElems);

    for(int idx = 0; idx < ne; ++idx)
      newD[idx] = fn.apply(idx, newD[idx]);

    return new ArrayImmutList(newD, 0, nElems, m);
  }
}

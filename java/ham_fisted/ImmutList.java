package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.ChunkedList.*;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.Indexed;
import clojure.lang.RT;
import clojure.lang.IReduce;
import clojure.lang.IKVReduce;
import clojure.lang.IFn;
import clojure.lang.IHashEq;
import clojure.lang.Seqable;
import clojure.lang.Reversible;
import clojure.lang.ISeq;
import clojure.lang.IPersistentVector;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.Util;
import clojure.lang.IEditableCollection;
import clojure.lang.ITransientVector;
import clojure.lang.IFn;
import clojure.lang.APersistentVector;



public class ImmutList
  extends APersistentVector
  implements IMutList, IHashEq, ChunkedListOwner, IPersistentVector,
	     IEditableCollection, ImmutValues
{
  public final int startidx;
  public final int nElems;
  final ChunkedList data;
  int _hash = 0;

  public static final ImmutList EMPTY = new ImmutList(0, 0, new ChunkedList());

  ImmutList(int sidx, int eidx, ChunkedList d) {
    startidx = sidx;
    nElems = eidx - sidx;
    data = d;
  }
  public static IPersistentVector create(boolean owning, IPersistentMap meta, Object... data) {
    if (data.length <= 32)
      return ArrayImmutList.create(owning, meta, data);
    return new ImmutList(0, data.length, ChunkedList.create(owning, meta, data));
  }
  final int indexCheck(int idx) {
    return ChunkedList.indexCheck(startidx, nElems, idx);
  }
  final int wrapIndexCheck(int idx) {
    return ChunkedList.wrapIndexCheck(startidx, nElems, idx);
  }
  public ChunkedListSection getChunkedList() {
    return new ChunkedListSection(data.data, startidx, startidx+nElems);
  }
  public final int hashCode() {
    if (_hash == 0) {
      _hash = data.hasheq(startidx, startidx+nElems);
    }
    return _hash;
  }
  public final int hasheq() { return hashCode(); }
  public final boolean equals(Object other) {
    if (other == this ) return true;
    return equiv(other);
  }
  public final String toString() { return Transformables.sequenceToString(this); }
  public final boolean equiv(Object other) {
    //return CljHash.listEquiv(this, other);
    return data.equiv(startidx, startidx + nElems, other);
  }
  public final int size() { return nElems; }
  public final int count() { return nElems; }
  public final int length() { return nElems; }
  public final void clear() {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean add(Object e) {
    throw new RuntimeException("Unimplemented");
  }
  public final void add(int idx, Object e) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean addAll(Collection e) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean addAll(int idx, Collection e) {
    throw new RuntimeException("Unimplemented");
  }
  public final Object remove(int idx) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean remove(Object o) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean removeAll(Collection c) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean retainAll(Collection c) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean isEmpty() { return nElems == 0; }
  @SuppressWarnings("unchecked")
  public final Object set(int idx, Object e) {
    throw new RuntimeException("Unimplemented");
  }
  @SuppressWarnings("unchecked")
  public final Object get(int idx) {
    return data.getValue(indexCheck(idx));
  }
  public final int indexOf(Object obj) {
    return data.indexOf(startidx, startidx+nElems, obj);
  }
  public final int lastIndexOf(Object obj) {
    return data.lastIndexOf(startidx, startidx+nElems, obj);
  }
  public final boolean contains(Object obj) {
    return data.contains(startidx, startidx+nElems, obj);
  }
  public final boolean containsAll(Collection c) {
    return data.containsAll(startidx, startidx+nElems, c);
  }
  @SuppressWarnings("unchecked")
  public final Iterator iterator() {
    return data.iterator(startidx, startidx + nElems);
  }
  public final IMutList subList(int sidx, int eidx) {
    ChunkedList.sublistCheck(sidx, eidx, nElems);
    return new ImmutList(sidx+startidx, eidx+startidx, data);
  }
  public final Object[] toArray() {
    return data.toArray(startidx, startidx+nElems);
  }
  public final Object[] toArray(Object[] marker) {
    return data.fillArray(startidx, startidx+nElems, Arrays.copyOf(marker, nElems));
  }
  public IPersistentMap meta() { return data.meta(); }
  public ImmutList withMeta(IPersistentMap m) {
    return new ImmutList(startidx, startidx+nElems, data.withMeta(m));
  }

  public final Object nth(int idx) { return data.getValue(wrapIndexCheck(idx)); }
  public final Object nth(int idx, Object notFound) {
    if (idx < 0)
      idx = idx + nElems;
    return data.getValue(indexCheck(idx));
  }
  public final Object invoke(Object idx) {
    return nth(RT.intCast(idx));
  }
  public final Object invoke(Object idx, Object notFound) {
    return nth(RT.intCast(idx), notFound);
  }
  public final Object reduce(IFn f) {
    return data.reduce(startidx, startidx+nElems, f);
  }
  public final Object reduce(IFn f, Object init) {
    return data.reduce(startidx, startidx+nElems, f, init);
  }
  public final Object kvreduce(IFn f, Object init) {
    return data.kvreduce(startidx, startidx+nElems, f, init);
  }
  public final ISeq seq() { return data.seq(startidx, startidx+nElems); }
  public final ISeq rseq() { return data.rseq(startidx, startidx+nElems); }
  public final ImmutList cons(Object obj) {
    return new ImmutList(0, nElems+1, data.conj(startidx, startidx+nElems, obj));
  }
  public final ImmutList assocN(int idx, Object obj) {
    if(idx == nElems)
      return cons(obj);
    indexCheck(idx);
    return new ImmutList(0, nElems, data.assoc(startidx, startidx+nElems, idx, obj));
  }
  public final ImmutList assoc(Object idx, Object obj) {
    return assocN(RT.intCast(idx), obj);
  }
  public IMapEntry entryAt(Object key) {
    return IMutList.super.entryAt(key);
  }
  public final boolean containsKey(Object key) {
    if (Util.isInteger(key)) {
      final int k = RT.intCast(key);
      return k >= 0 && k < nElems;
    }
    return false;
  }
  public final ImmutList empty() {
    return EMPTY.withMeta(meta());
  }
  public final Object valAt(Object obj, Object notFound) {
    if(Util.isInteger(obj)) {
      int k = RT.intCast(obj);
      if (k >= 0 && k < nElems)
	return data.getValue(k + startidx);
    }
    return notFound;
  }
  public final Object valAt(Object obj) {
    return valAt(obj, null);
  }
  public final ImmutList pop() {
    if (nElems == 0)
      throw new RuntimeException("Can't pop empty vector");
    if (nElems == 1)
      return EMPTY.withMeta(meta());
    return new ImmutList(0, nElems-1, data.pop(startidx, startidx+nElems));
  }
  public final Object peek() {
    if (nElems == 0)
      return null;
    return get(nElems-1);
  }

  @SuppressWarnings("unchecked")
  public ImmutList immutUpdateValues(BiFunction valueMap) {
    ChunkedList retval = data.clone(startidx, startidx+nElems, 0, true);
    final Object[][] rd = retval.data;
    final int ne = nElems;
    int idx = 0;
    while (idx < ne) {
      final Object[] chunk = rd[idx/32];
      final int csize = Math.min(ne-idx, chunk.length);
      for (int eidx = 0; eidx < csize; ++eidx, ++idx) {
	chunk[eidx] = valueMap.apply(idx, chunk[eidx]);
      }
    }
    return new ImmutList(0, ne, retval);
  }

  @SuppressWarnings("unchecked")
  public ImmutList immutUpdateValue(Object key, IFn valueMap) {
    if(!Util.isInteger(key))
      throw new RuntimeException("Vector indexes must be integers: " + String.valueOf(key));
    int idx = RT.intCast(key);
    if (idx == nElems)
      return cons(valueMap.invoke(null));


    indexCheck(idx);
    ChunkedList retval = data.clone(startidx, startidx+nElems, 0, false);
    final Object[][] mdata = retval.data;
    final int cidx = idx / 32;
    final int eidx = idx % 32;
    Object[] chunk = mdata[cidx].clone();
    mdata[cidx] = chunk;
    chunk[eidx] = valueMap.invoke(chunk[eidx]);
    return new ImmutList(0, nElems, retval);
  }

  public final ITransientVector asTransient() {
    if(nElems == 0)
      return new MutList();
    //We know the cloning operation deep copies if startidx isn't a multiple of 32.
    final boolean ownsEverything = (startidx % 32) != 0;
    return new TransientList(data.clone(startidx, startidx + nElems, 0, false),
			     nElems, ownsEverything);
  }

}

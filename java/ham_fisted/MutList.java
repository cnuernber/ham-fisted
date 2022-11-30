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
import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.Indexed;
import clojure.lang.RT;
import clojure.lang.IReduce;
import clojure.lang.Counted;
import clojure.lang.IKVReduce;
import clojure.lang.IFn;
import clojure.lang.IHashEq;
import clojure.lang.Seqable;
import clojure.lang.Reversible;
import clojure.lang.ISeq;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientVector;
import clojure.lang.Util;
import clojure.lang.Counted;
import clojure.lang.IReduceInit;


public class MutList<E>
  implements IMutList<E>, ChunkedListOwner, Cloneable, ITransientVector, ImmutValues
{
  final ChunkedList data;
  public MutList() { data = new ChunkedList(); }
  public MutList(int capacity) { data = new ChunkedList(capacity); }
  public MutList(ChunkedList other) {
    data = new ChunkedList(other, false);
  }
  //deep cloning constructor
  public MutList(MutList<E> other) {
    this(other.data.clone(0, other.data.nElems, 0, true));
  }
  @SafeVarargs
  public static <E> MutList<E> create(boolean owning, IPersistentMap meta, E... data) {
    return new MutList<E>(ChunkedList.create(owning, meta, data));
  }
  public final ChunkedListSection getChunkedList() {
    return new ChunkedListSection(data.data, 0, data.nElems);
  }
  public final MutList<E> clone() { return new MutList<E>(this); }
  public final MutList<E> cloneList() {
    return clone();
  }
  public final boolean add(E v) { data.add(v); return true; }
  public final void add(int idx, E v) { data.add(v,idx); }
  public final boolean addAll(Collection<? extends E> c) {
    return addAllReducible(c);
  }

  @SuppressWarnings("unchecked")
  public final boolean addAllReducible(Object c) {
    if( c instanceof Map)
      c = ((Map)c).entrySet();
    final int ssz = size();
    if (c instanceof ChunkedListOwner) {
      final ChunkedListSection section = ((ChunkedListOwner)c).getChunkedList();
      int idx = data.nElems;
      int oidx = section.startidx;
      int one = section.endidx - oidx;
      final int finalLen = idx + one;
      data.enlarge(finalLen);
      data.nElems = finalLen;
      final Object[][] mdata = data.data;
      final Object[][] odata = section.data;
      while(idx < finalLen) {
	final int cidx = idx / 32;
	final int ocidx = oidx / 32;
	final int leftover = finalLen - idx;
	final int eidx = idx % 32;
	final int oeidx = oidx % 32;
	final int copyLen = Math.min(leftover, Math.min(32-eidx, 32-oeidx));
	System.arraycopy(odata[ocidx], oeidx, mdata[cidx], eidx, copyLen);
	idx += copyLen;
	oidx += copyLen;
      }
    }
    else if (c instanceof IReduceInit) {
      final ChunkedList d = data;
      if( c instanceof RandomAccess || c instanceof Counted) {
	final int cz = c instanceof RandomAccess ? ((List)c).size() : ((Counted)c).count();
	if (cz == 0) return false;
	d.enlarge(ssz + cz);
	d.nElems = ssz + cz;
	d.fillRangeReduce(ssz, c);
      }
      else {
	((IReduceInit)c).reduce(new IFnDef() {
	    public Object invoke(Object lhs, Object rhs) {
	      d.add(rhs);
	      return this;
	    }
	  }, this);
      }
    }
    else if (c instanceof RandomAccess) {
      final List l = (List)c;
      final int cs = l.size();
      data.enlarge(cs + ssz);
      int idx = ssz;
      data.nElems = idx +  cs;
      final Object[][] mdata = data.data;
      int cidx = 0;
      while(cidx < cs) {
	final Object[] chunk = mdata[idx/32];
	int eidx = idx % 32;
	final int groupLen = Math.min(chunk.length-eidx, cs - cidx);
	for (int lidx = 0; lidx < groupLen; ++lidx, ++eidx, ++cidx) {
	  chunk[eidx] = l.get(cidx);
	}
	idx += groupLen;
      }
    } else {
      if(! (c instanceof Iterable))
	throw new RuntimeException("Object must either be instance of IReduceInit or java.util.Iterable");
      ((Iterable)c).forEach((v)->add((E)v));
    }
    return ssz != size();
  }

  public final boolean addAll(int idx, Collection<? extends E> c) {
    if (c.isEmpty())
      return false;
    if (idx == data.nElems)
      return addAll(c);

    indexCheck(idx);
    if (! (c instanceof RandomAccess)) {
      ArrayList<E> al = new ArrayList<E>();
      al.addAll(c);
      c = al;
    }

    final int cs = c.size();
    data.widen(idx, idx + cs);
    for(E item: c) data.setValue(idx++, item);

    return true;
  }

  public final void clear() { data.clear(); }

  public final boolean contains(Object v) {
    return data.contains(0, data.nElems, v);
  }

  public final boolean containsAll(Collection<?> c) {
    return data.containsAll(0, data.nElems, c);
  }

  final int indexCheck(int idx) {
    return ChunkedList.indexCheck(0, data.nElems, idx);
  }

  final int indexCheck(long idx) {
    return ChunkedList.indexCheck(0, data.nElems, (int)idx);
  }

  final int wrapIndexCheck(int idx) {
    return ChunkedList.wrapIndexCheck(0, data.nElems, idx);
  }

  @SuppressWarnings("unchecked")
  public final E set(int idx, E e) {
    return (E)data.setValueRV(indexCheck(idx), e);
  }

  @SuppressWarnings("unchecked")
  public final E get(int idx) {
    return (E)data.getValue(indexCheck(idx));
  }

  public final int indexOf(Object o) {
    return data.indexOf(0, data.nElems, o);
  }

  public final boolean isEmpty() { return data.nElems == 0; }

  @SuppressWarnings("unchecked")
  public final Iterator<E> iterator() { return (Iterator<E>)data.iterator(); }

  public static class SubMutList<E> implements IMutList<E>, ChunkedListOwner, Cloneable
  {
    final int startidx;
    final int nElems;
    final ChunkedList data;

    SubMutList(int sidx, int eidx, ChunkedList d) {
      startidx = sidx;
      nElems = eidx - sidx;
      data = d;
    }

    public final ChunkedListSection getChunkedList() {
      return new ChunkedListSection(data.data, startidx, startidx+nElems);
    }

    public final MutList<E> clone() {
      return new MutList<E>(data.clone(startidx, startidx+nElems));
    }

    public final MutList<E> cloneList() {
      return clone();
    }

    final int indexCheck(int idx) {
      return ChunkedList.indexCheck(startidx, nElems, idx);
    }

    final int wrapIndexCheck(int idx) {
      return ChunkedList.wrapIndexCheck(startidx, nElems, idx);
    }

    public final int size() { return nElems; }
    public final boolean isEmpty() { return nElems == 0; }
    @SuppressWarnings("unchecked")
    public final E set(int idx, E e) {
      return (E)data.setValueRV(indexCheck(idx), e);
    }
    @SuppressWarnings("unchecked")
    public final E get(int idx) {
      return (E)data.getValue(indexCheck(idx));
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
    public final boolean containsAll(Collection<?> c) {
      return data.containsAll(startidx, startidx+nElems, c);
    }
    @SuppressWarnings("unchecked")
    public final Iterator<E> iterator() {
      return (Iterator<E>)data.iterator(startidx, startidx + nElems);
    }
    public final ListIterator<E> listIterator(int idx) {
      return data.listIterator(indexCheck(idx), startidx+nElems, (E)null);
    }
    public final ListIterator<E> listIterator() {
      return listIterator(0);
    }
    public final IMutList<E> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, nElems);
      return new SubMutList<E>(ssidx + startidx, seidx + startidx, data);
    }
    public final Object[] toArray() {
      return data.toArray(startidx, startidx + nElems);
    }
    @SuppressWarnings("unchecked")
    public final <T> T[] toArray(T[] init) {
      return (T[])data.fillArray(startidx, startidx + nElems, Arrays.copyOf(init, nElems));
    }
    public final Object nth(int idx) { return data.getValue(wrapIndexCheck(idx)); }
    public final Object nth(int idx, Object notFound) {
      if (idx < 0)
	idx = idx + data.nElems;

      return idx < data.nElems && idx > -1 ? data.getValue(idx+startidx) : notFound;
    }
    public final Object invoke(Object idx) {
      return nth(RT.intCast(idx));
    }
    public final Object invoke(Object idx, Object notFound) {
      return nth(RT.intCast(idx), notFound);
    }
    public final int count() { return nElems; }

    public final Object reduce(IFn f) {
      return data.reduce(startidx, startidx+nElems, f);
    }

    public final Object reduce(IFn f, Object init) {
      return data.reduce(startidx, startidx+nElems, f,init);
    }

    public final Object kvreduce(IFn f, Object init) {
      return data.kvreduce(startidx, startidx+nElems, f, init);
    }
    public void fillRange(long sidx, long eidx, Object v) {
      final int ssidx = indexCheck((int)sidx);
      if (eidx < sidx || eidx > nElems)
	throw new RuntimeException("End index out of range: " + String.valueOf(eidx));
      data.fillRange((int)ssidx, (int)(eidx + startidx), v);
    }
    public void fillRange(long sidx, List v) {
      final int ssidx = indexCheck((int)sidx);
      final int eidx = (int)sidx + v.size();
      if (eidx > nElems)
	throw new RuntimeException("End index out of range: " + String.valueOf(eidx));
      data.fillRangeReduce(ssidx, v);
    }
    public final int hashCode() {
      return data.hasheq(startidx, startidx + nElems);
    }
    public final int hasheq() {
      return hashCode();
    }
    public final String toString() {
      return Transformables.sequenceToString(this);
    }
    public final boolean equals(Object other) {
      return equiv(other);
    }
    public final boolean equiv(Object other) {
      return CljHash.listEquiv(this, other);
    }
    public final ISeq seq() { return data.seq(startidx, startidx+nElems); }
    public final ISeq rseq() { return data.rseq(startidx, startidx+nElems); }
    public IPersistentMap meta() { return data.meta(); }
    public SubMutList<E> withMeta(IPersistentMap m) {
      return new SubMutList<E>(startidx, startidx + nElems, data.withMeta(m));
    }
  }

  public final IMutList<E> subList(int startidx, int endidx) {
    ChunkedList.sublistCheck(startidx, endidx, size());
    return new SubMutList<E>(startidx, endidx, data);
  }

  public final ListIterator<E> listIterator(int idx) {
    return data.listIterator(idx, data.nElems, (E)null);
  }
  public final ListIterator<E> listIterator() { return listIterator(0); }

  public final int lastIndexOf(Object obj) {
    return data.lastIndexOf(0, data.nElems, obj);
  }

  @SuppressWarnings("unchecked")
  public final E remove(int idx) {
    final E retval = (E)data.getValue(idx);
    data.shorten(idx, idx+1);
    return retval;
  }

  public void fillRange(int startidx, int endidx, Object v) {
    indexCheck(startidx);
    if(endidx < startidx || endidx > data.nElems)
      throw new RuntimeException("End index out of range: " + String.valueOf(endidx));
    data.fillRange(startidx, endidx, v);
  }

  public void fillRange(int startidx, List v) {
    indexCheck(startidx);
    final int endidx = v.size();
    if(endidx < startidx || endidx > data.nElems)
      throw new RuntimeException("End index out of range: " + String.valueOf(endidx));
    data.fillRangeReduce(startidx, v);
  }

  public void addRange(int startidx, int endidx, Object v) {
    indexCheck(startidx);
    data.addRange(startidx, endidx, v);
  }

  public void removeRange(int startidx, int endidx) {
    indexCheck(startidx);
    if (endidx == startidx)
      return;

    if(endidx < startidx || endidx > data.nElems)
      throw new RuntimeException("End index out of range: " + String.valueOf(endidx));

    data.shorten(startidx, endidx);
  }

  public final boolean remove(Object obj) {
    final int idx = indexOf(obj);
    if (idx != -1)
      remove(idx);
    return idx != -1;
  }

  public final boolean retainAll(Collection c) {
    throw new RuntimeException("Unimplemented");
  }

  public final boolean removeAll(Collection c) {
    throw new RuntimeException("Unimplemented");
  }

  @SuppressWarnings("unchecked")
  public final <T> T[] toArray(T[] typeMarker) {
    return (T[])data.fillArray(Arrays.copyOf(typeMarker, data.nElems));
  }

  public final Object[] toArray() {
    return data.toArray();
  }

  public final int size() { return data.nElems; }
  public final int capacity() { return data.capacity; }

  public final Object nth(int idx) { return data.getValue(wrapIndexCheck(idx)); }
  public final Object nth(int idx, Object notFound) {
    if (idx < 0)
      idx = idx + data.nElems;

    return idx < data.nElems && idx > -1 ? data.getValue(idx) : notFound;
  }
  public final Object invoke(Object idx) {
    return nth(RT.intCast(idx));
  }
  public final Object invoke(Object idx, Object notFound) {
    return nth(RT.intCast(idx), notFound);
  }
  public final int count() { return data.size(); }

  public final Object reduce(IFn f) {
    return data.reduce(0, data.nElems, f);
  }

  public final Object reduce(IFn f, Object init) {
    return data.reduce(0, data.nElems, f,init);
  }

  public final Object kvreduce(IFn f, Object init) {
    return data.kvreduce(0, data.nElems, f, init);
  }

  public final ISeq seq() { return data.seq(0, data.nElems); }
  public final ISeq rseq() { return data.rseq(0, data.nElems); }

  public final int hashCode() {
    return data.hasheq(0, data.nElems);
  }
  public final int hasheq() {
    return hashCode();
  }
  public final boolean equals(Object other) {
    if (other == this)
      return true;
    return equiv(other);
  }
  public final boolean equiv(Object other) {
    return CljHash.listEquiv(this, other);
  }
  public final String toString() {
    return Transformables.sequenceToString(this);
  }
  public IPersistentMap meta() { return data.meta(); }
  public MutList<E> withMeta(IPersistentMap m) {
    return new MutList<E>(data.withMeta(m));
  }
  @SuppressWarnings("unchecked")
  public final MutList<E> assocN(int i, Object obj) {
    if (i == data.nElems) add((E)obj);
    set(indexCheck(i), (E)obj);
    return this;
  }
  public final MutList<E> pop() {
    if(data.nElems == 0)
      throw new RuntimeException("Cannot pop empty vector.");
    remove(data.nElems-1);
    return this;
  }
  public final MutList<E> assoc(Object i, Object obj) {
    if(!Util.isInteger(i))
      throw new RuntimeException("Vectors must have integer keys");
    return assocN(RT.intCast(i), obj);
  }
  @SuppressWarnings("unchecked")
  public final MutList<E> conj(Object obj) {
    add((E)obj);
    return this;
  }
  public final Object valAt(Object key) {
    return valAt(key, null);
  }
  public final Object valAt(Object key, Object notFound) {
    if(Util.isInteger(key)) {
      int k = RT.intCast(key);
      if(k >= 0 && k < data.nElems)
	return data.getValue(k);
    }
    return notFound;
  }
  public ImmutList persistent() { return new ImmutList(0, data.nElems, data); }
  public ImmutList immutUpdateValues(BiFunction valueMap) {
    return persistent().immutUpdateValues(valueMap);
  }
  public ImmutList immutUpdateValue(Object key, IFn valueMap) {
    return persistent().immutUpdateValue(key, valueMap);
  }
}

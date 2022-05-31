package ham_fisted;


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
import clojure.lang.Indexed;
import clojure.lang.RT;
import clojure.lang.IReduce;
import clojure.lang.IKVReduce;
import clojure.lang.IFn;


public class MutList<E>
  implements List<E>, RandomAccess, Indexed, IFnDef, IReduce, IKVReduce,
	     RangeList<E>
{

  ChunkedList data;
  public MutList() { data = new ChunkedList(); }
  public MutList(int capacity) { data = new ChunkedList(capacity); }
  public MutList(ChunkedList other) {
    data = new ChunkedList(other, false);
  }
  //deep cloning constructor
  public MutList(MutList<E> other) {
    this(new ChunkedList(other.data, false));
  }
  public final MutList<E> clone() { return new MutList<E>(this); }
  public final boolean add(E v) { data.add(v); return true; }
  public final void add(int idx, E v) { data.add(v,idx); }

  public final boolean addAll(Collection<? extends E> c) {
    if (c.isEmpty())
      return false;
    if (c instanceof RandomAccess) {
      data.enlarge(c.size() + size());
      int idx = data.nElems;
      data.nElems = idx +  c.size();
      for(E e: c) data.setValue(idx++, e);
    } else {
      for (E e: c) add(e);
    }
    return true;
  }

  public final boolean addAll(int idx, Collection<? extends E> c) {
    if (c.isEmpty())
      return false;

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
    final int ne = data.nElems;
    for (E e:this)
      if(Objects.equals(e,v))
	return true;
    return false;
  }

  public final boolean containsAll(Collection<?> c) {
    Collection<?> minC = size() < c.size() ? this : c;
    //This set can contain null.
    HashSet<Object> hc = new HashSet<Object>();
    hc.addAll(minC);
    Collection<?> maxC = size() < c.size() ? c : this;
    for(Object e: maxC)
      if(!hc.contains(e))
	return false;
    return true;
  }

  final int indexCheck(int idx) {
    if (idx < 0)
      throw new RuntimeException("Index underflow: " + String.valueOf(idx));
    if(idx >= data.nElems)
      throw new RuntimeException("Index out of range: " + String.valueOf(idx) + " : "
				 + String.valueOf(data.nElems));
    return idx;
  }

  final int wrapIndexCheck(int idx) {
    if (idx < 0)
      idx = data.nElems + idx;
    return indexCheck(idx);
  }

  @SuppressWarnings("unchecked")
  public final E set(int idx, E e) {
    return (E)data.setValueRV(indexCheck(idx), e);
  }

  @SuppressWarnings("unchecked")
  public final E get(int idx) {
    return (E)data.getValue(wrapIndexCheck(idx));
  }

  public final int indexOf(Object o) {
    final int ne = data.nElems;
    for(int idx = 0; idx < ne; ++idx)
      if(Objects.equals(o, data.getValue(idx)))
	return idx;
    return -1;
  }
  public final boolean isEmpty() { return data.nElems == 0; }

  @SuppressWarnings("unchecked")
  public final Iterator<E> iterator() { return (Iterator<E>)data.iterator(); }

  static class SubMutList<E> implements List<E>, RandomAccess, Indexed, IFnDef,
					IReduce, IKVReduce, RangeList<E> {
    final int startidx;
    final int nElems;
    final ChunkedList data;

    SubMutList(int sidx, int eidx, ChunkedList d) {
      startidx = sidx;
      nElems = eidx - sidx;
      data = d;
    }

    public final boolean add(E e) {
      throw new RuntimeException("Unimplemented");
    }
    public final void add(int idx, E e) {
      throw new RuntimeException("Unimplemented");
    }
    public final boolean addAll(Collection<? extends E> e) {
      throw new RuntimeException("Unimplemented");
    }
    public final boolean addAll(int idx, Collection<? extends E> e) {
      throw new RuntimeException("Unimplemented");
    }
    public final E remove(int idx) {
      throw new RuntimeException("Unimplemented");
    }
    public final boolean remove(Object o) {
      throw new RuntimeException("Unimplemented");
    }
    public final boolean removeAll(Collection<?> c) {
      throw new RuntimeException("Unimplemented");
    }
    public final boolean retainAll(Collection<?> c) {
      throw new RuntimeException("Unimplemented");
    }

    final int indexCheck(int idx) {
      if (idx < 0)
	throw new RuntimeException("Index underflow: " + String.valueOf(idx));
      if(idx >= nElems)
	throw new RuntimeException("Index out of range: " + String.valueOf(idx) + " : "
				   + String.valueOf(nElems));
      return idx + startidx;
    }

    final int wrapIndexCheck(int idx) {
      if (idx < 0)
	idx = nElems + idx;
      return indexCheck(idx);
    }

    public final void clear() {
      throw new RuntimeException("Unimplemented");
    }
    public final int size() { return nElems; }
    public final boolean isEmpty() { return nElems == 0; }
    @SuppressWarnings("unchecked")
    public final E set(int idx, E e) {
      return (E)data.setValueRV(indexCheck(idx), e);
    }
    @SuppressWarnings("unchecked")
    public final E get(int idx) {
      return (E)data.getValue(wrapIndexCheck(idx));
    }
    public final int indexOf(Object obj) {
      for(int idx = 0; idx < nElems; ++idx)
	if (Objects.equals(obj, get(idx)))
	  return idx;
      return -1;
    }
    public final int lastIndexOf(Object obj) {
      final int nne = nElems - 1;
      for(int idx = 0; idx < nElems; ++idx) {
	final int ridx = nne - idx;
	if (Objects.equals(obj, get(ridx)))
	  return ridx;
      }
      return -1;
    }
    public final boolean contains(Object obj) {
      return indexOf(obj) != -1;
    }
    public final boolean containsAll(Collection<?> c) {
      Collection<?> minC = size() < c.size() ? this : c;
      //This set can contain null.
      HashSet<Object> hc = new HashSet<Object>();
      hc.addAll(minC);
      Collection<?> maxC = size() < c.size() ? c : this;
      for(Object e: maxC)
	if(!hc.contains(e))
	  return false;
      return true;
    }
    @SuppressWarnings("unchecked")
    public final Iterator<E> iterator() {
      return (Iterator<E>)data.iterator(startidx, startidx + nElems);
    }
    public final ListIterator<E> listIterator(int idx) {
      throw new RuntimeException("Unimplemented");
    }
    public final ListIterator<E> listIterator() {
      return listIterator(0);
    }
    public final List<E> subList(int sidx, int eidx) {
      sidx = indexCheck(sidx);
      if (eidx < sidx || eidx > nElems)
	throw new RuntimeException("End index out of range: " + String.valueOf(eidx));
      return new SubMutList<E>(sidx, eidx + startidx, data);
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
      return data.kvreduce(startidx, startidx+nElems, startidx, f, init);
    }
    public void fillRange(int sidx, int eidx, E v) {
      final int ssidx = indexCheck(sidx);
      if (eidx < sidx || eidx > nElems)
	throw new RuntimeException("End index out of range: " + String.valueOf(eidx));
      data.fillRange(ssidx, eidx + startidx, v);
    }
    public void fillRange(int sidx, List<? extends E> v) {
      final int ssidx = indexCheck(sidx);
      final int eidx = sidx + v.size();
      if (eidx > nElems)
	throw new RuntimeException("End index out of range: " + String.valueOf(eidx));
      data.fillRange(ssidx, v);
    }
    public void addRange(int startidx, int endidx, E v) {
      throw new RuntimeException("Unimplemented");
    }
    public void removeRange(int startidx, int endidx) {
      throw new RuntimeException("Unimplemented");
    }
  }

  public final List<E> subList(int startidx, int endidx) {
    return new SubMutList<E>(startidx, endidx, data);
  }

  public final ListIterator<E> listIterator(int idx) { throw new RuntimeException("Unimplemented"); }
  public final ListIterator<E> listIterator() { return listIterator(0); }

  public final int lastIndexOf(Object obj) {

    final int nem1 = size() - 1;
    final int ne = size();

    for (int idx = 0; idx < ne; ++idx) {
      final int ridx = nem1 - idx;
      if(Objects.equals(data.getValue(ridx), obj))
	return ridx;
    }
    return -1;
  }

  @SuppressWarnings("unchecked")
  public final E remove(int idx) {
    final E retval = (E)data.getValue(idx);
    data.shorten(idx, idx+1);
    return retval;
  }

  public void fillRange(int startidx, int endidx, E v) {
    indexCheck(startidx);
    if(endidx < startidx || endidx > data.nElems)
      throw new RuntimeException("End index out of range: " + String.valueOf(endidx));
    data.fillRange(startidx, endidx, v);
  }

  public void fillRange(int startidx, List<? extends E> v) {
    indexCheck(startidx);
    final int endidx = v.size();
    if(endidx < startidx || endidx > data.nElems)
      throw new RuntimeException("End index out of range: " + String.valueOf(endidx));
    data.fillRange(startidx, v);
  }

  public void addRange(int startidx, int endidx, E v) {
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
    return data.kvreduce(0, data.nElems, 0, f, init);
  }

}

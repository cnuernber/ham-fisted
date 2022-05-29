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


public class MutList<E> implements List<E>, RandomAccess {
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
    if (c.size() == 0 )
      return false;
    data.enlarge(data.size() + c.size() + data.offset);
    for (E e: c) add(e);
    return true;
  }

  public final boolean addAll(int idx, Collection<? extends E> c) {
    if (c.size() == 0 )
      return false;
    data.enlarge(data.size() + c.size() + data.offset);
    for(E item: c) add(idx++, item);
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

  @SuppressWarnings("unchecked")
  public final E set(int idx, E e) {
    return (E)data.setValueRV(indexCheck(idx), e);
  }

  @SuppressWarnings("unchecked")
  public final E get(int idx) {
    return (E)data.getValue(indexCheck(idx));
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
  public final List<E> subList(int off, int endoff) {
    if (off == 0 && endoff == data.nElems)
      return this;
    return new MutList<E>(new ChunkedList(data, off, endoff));
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

  public final E remove(int idx) {
    throw new RuntimeException("Unimplemented");
  }

  public final boolean remove(Object obj) {
    throw new RuntimeException("Unimplemented");
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
}

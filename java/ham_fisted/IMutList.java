package ham_fisted;


import java.util.List;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Consumer;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.floats.FloatComparator;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;


import clojure.lang.Indexed;
import clojure.lang.IReduce;
import clojure.lang.IKVReduce;
import clojure.lang.IHashEq;
import clojure.lang.Seqable;
import clojure.lang.Reversible;
import clojure.lang.RT;
import clojure.lang.Util;
import clojure.lang.IFn;
import clojure.lang.IDeref;
import clojure.lang.IMapEntry;
import clojure.lang.MapEntry;
import clojure.lang.IteratorSeq;
import clojure.lang.ISeq;
import clojure.lang.IPersistentVector;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;


public interface IMutList<E>
  extends List<E>, RandomAccess, Indexed, IFnDef, IReduce, IKVReduce,
	  IHashEq, Seqable, Reversible, IObj, ImmutSort<E>,
	  RangeList

{
  default void clear() { throw new RuntimeException("Unimplemented"); }
  default boolean add(E v) { throw new RuntimeException("Unimplemented"); }
  default void add(int idx, E v) { throw new RuntimeException("Unimplemented"); }
  default boolean addLong(long v) { throw new RuntimeException("Unimplemented"); }
  default boolean addDouble(double v) { throw new RuntimeException("Unimplemented"); }
  default void addRange(int startidx, int endidx, Object v) { throw new RuntimeException("Unimplemented"); }
  default void removeRange(int startidx, int endidx) { throw new RuntimeException("Unimplemented"); }
  @SuppressWarnings("unchecked")
  default void fillRange(int startidx, final int endidx, Object v) {
    for(; startidx < endidx; ++startidx) {
      set(startidx, (E)v);
    }
  }
  @SuppressWarnings("unchecked")
  default void fillRange(int startidx, List v) {
    final int eidx = startidx + v.size();
    int idx = 0;
    for(; startidx < eidx; ++startidx, ++idx)
      set(startidx, (E)v.get(idx));
  }
  default E remove(int idx) {
    throw new RuntimeException("Unimplemented");
  }
  default boolean remove(Object o) {
    final int idx = indexOf(o);
    if ( idx == -1)
      return false;
    remove(idx);
    return true;
  }
  default boolean addAll(Collection<? extends E> c) {
    if (c.isEmpty())
      return false;
    for (E e: c) add(e);
    return true;
  }
  default boolean addAll(int idx, Collection<? extends E> c) {
    if (c.isEmpty())
      return false;
    final int sz = size();
    if (idx == sz)
      return addAll(c);

    for (E e: c) add(idx++, e);
    return true;
  }
  default boolean removeAll(Collection<?> c) {
    HashSet<Object> hs = new HashSet<Object>();
    hs.addAll(c);
    int sz = size();
    final int osz = sz;
    for(int idx = 0; idx < sz; ++idx) {
      if(hs.contains(get(idx))) {
	remove(idx);
	--idx;
	--sz;
      }
    }
    return size() == osz;
  }
  default boolean retainAll(Collection<?> c) {
    HashSet<Object> hs = new HashSet<Object>();
    hs.addAll(c);
    int sz = size();
    final int osz = sz;
    for(int idx = 0; idx < sz; ++idx) {
      if(!hs.contains(get(idx))) {
	remove(idx);
	--idx;
	--sz;
      }
    }
    return size() == osz;
  }
  default List<E> subList(int startidx, int endidx) {
    throw new RuntimeException("Unimplemented");
  }
  default int indexOf(Object o) {
    final int sz = size();
    for(int idx = 0; idx < sz; ++idx)
      if (Objects.equals(o, get(idx)))
	return idx;
    return -1;
  }
  default int lastIndexOf(Object o) {
    final int sz = size();
    final int ssz = sz - 1;
    for(int idx = 0; idx < sz; ++idx) {
      int ridx = ssz - idx;
      if (Objects.equals(o, get(ridx)))
	return ridx;
    }
    return -1;
  }
  default boolean contains(Object o) {
    return indexOf(o) != -1;
  }
  default boolean isEmpty() { return size() == 0; }

  public static class ListIter<E> implements ListIterator<E> {
    List<E> list;
    int idx = 0;
    int previdx = 0;
    ListIter(List<E> ls, int _idx){list = ls; idx = _idx; previdx = _idx;}
    public final boolean hasNext() {
      return idx < list.size();
    }
    public final boolean hasPrevious() {
      return idx > 0;
    }
    public final E next() {
      if(!hasNext())
	throw new NoSuchElementException();
      final E retval = list.get(idx);
      previdx = idx;
      ++idx;
      return retval;
    }
    public final E previous() {
      if(!hasPrevious())
	throw new NoSuchElementException();
      --idx;
      previdx = idx;
      return list.get(idx);
    }
    public final int nextIndex() {
      return idx;
    }
    public final int previousIndex() {
      return idx-1;
    }
    public final void remove() {
      list.remove(previdx);
    }
    public final void set(E e) {
      list.set(previdx, e);
    }
    public final void add(E e) {
      list.add(previdx, e);
    }
  }
  default ListIterator<E> listIterator(int idx) {
    if (idx < 0 || idx >= size())
      throw new NoSuchElementException();
    return new ListIter<E>(this, idx);
  }
  default ListIterator<E> listIterator() {
    return listIterator(0);
  }
  default Iterator<E> iterator() { return listIterator(0); }
  public class RIter<E> implements Iterator<E> {
    List<E> list;
    int idx;
    public RIter(List<E> ls) { list = ls; idx = 0; }
    public final boolean hasNext() { return idx < list.size(); }
    public final E next() {
      if(!hasNext())
	throw new NoSuchElementException();
      int ridx = list.size() - idx - 1;
      ++idx;
      return list.get(ridx);
    }
  }
  default Iterator<E> riterator() { return new RIter<E>(this); }
  default Object[] fillArray(Object[] data) {
    final int sz = size();
    for (int idx = 0; idx < sz; ++idx)
      data[idx] = get(idx);
    return data;
  }
  default Object[] toArray() {
    return fillArray(new Object[size()]);
  }
  default <T> T[] toArray(T[] marker) {
    final T[] retval = Arrays.copyOf(marker, size());
    fillArray(retval);
    return retval;
  }
  default int[] toIntArray() {
    final int sz = size();
    final int[] retval = new int[sz];
    for(int idx = 0; idx < sz; ++idx)
      retval[idx] = RT.intCast(getLong(idx));
    return retval;
  }
  default long[] toLongArray() {
    final int sz = size();
    final long[] retval = new long[sz];
    for(int idx = 0; idx < sz; ++idx)
      retval[idx] = getLong(idx);
    return retval;
  }
  default float[] toFloatArray() {
    final int sz = size();
    final float[] retval = new float[sz];
    for(int idx = 0; idx < sz; ++idx)
      retval[idx] = (float)getDouble(idx);
    return retval;
  }
  default double[] toDoubleArray() {
    final int sz = size();
    final double[] retval = new double[sz];
    for(int idx = 0; idx < sz; ++idx)
      retval[idx] = getDouble(idx);
    return retval;
  }
  @SuppressWarnings("unchecked")
  default IntComparator indexComparator() {
    return new IntComparator() {
      public int compare(int lidx, int ridx) {
	return ((Comparable)get(lidx)).compareTo(get(ridx));
      }
    };
  }
  @SuppressWarnings("unchecked")
  default IntComparator indexComparator(Comparator c) {
    if(c instanceof DoubleComparator) {
      final DoubleComparator dc = (DoubleComparator)c;
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return dc.compare(getDouble(lidx), getDouble(ridx));
	}
      };
    } else if (c instanceof IntComparator) {
      final IntComparator dc = (IntComparator)c;
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return dc.compare((int)getLong(lidx), (int)getLong(ridx));
	}
      };
    } else if (c instanceof LongComparator) {
      final LongComparator dc = (LongComparator)c;
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return dc.compare(getLong(lidx), getLong(ridx));
	}
      };
    } else if (c instanceof FloatComparator) {
      final FloatComparator dc = (FloatComparator)c;
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return dc.compare((float)getDouble(lidx), (float)getDouble(ridx));
	}
      };
    } else {
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return c.compare(get(lidx), get(ridx));
	}
      };
    }
  }

  default int[] sortIndirect(Comparator c) {
    final int sz = size();
    int[] retval = ArrayLists.iarange(0, sz, 1);
    if(sz < 2)
      return retval;
    if(c == null)
      IntArrays.parallelQuickSort(retval, indexComparator());
    else
      IntArrays.parallelQuickSort(retval, indexComparator(c));
    return retval;
  }

  default Object nth(int idx) {
    final int sz = size();
    if (idx < 0)
      idx = idx + sz;
    return idx < sz && idx > -1 ? get(idx) : null;
  }
  default Object nth(int idx, Object notFound) {
    final int sz = size();
    if (idx < 0)
      idx = idx + size();
    return idx < sz && idx > -1 ? get(idx) : notFound;
  }
  default E set(int idx, E v) { throw new RuntimeException("Unimplemented"); }
  @SuppressWarnings("unchecked")
  default long setLong(int idx, long v) { return (Long)set(idx, (E)Long.valueOf(v)); }
  @SuppressWarnings("unchecked")
  default double setDouble(int idx, double v) { return (Double)set(idx, (E)Double.valueOf(v)); }
  default long getLong(int idx) { return RT.longCast(get(idx)); }
  default double getDouble(int idx) { return RT.doubleCast(get(idx)); }

  default Object invoke(Object idx) {
    return nth(RT.intCast(idx));
  }
  default Object invoke(Object idx, Object notFound) {
    return nth(RT.intCast(idx), notFound);
  }
  default Object valAt(Object idx) {
    return invoke(idx);
  }
  default Object valAt(Object idx, Object def) {
    return invoke(idx, def);
  }
  default IMapEntry entryAt(Object key) {
    if(Util.isInteger(key)) {
      int idx = RT.intCast(key);
      if (idx >= 0 && idx < size())
	return MapEntry.create(idx, get(idx));
    }
    return null;
  }

  @SuppressWarnings("unimplemented")
  default boolean containsAll(Collection<?> c) {
    HashSet<Object> hc = new HashSet<Object>();
    hc.addAll(c);
    for(E e: this) {
      if(!hc.contains(e))
	return false;
    }
    return true;
  }

  default boolean containsKey(Object key) {
    if(Util.isInteger(key)) {
      int idx = RT.intCast(key);
      if (idx >= 0 && idx < size())
	return true;
    }
    return false;
  }

  default int count() { return size(); }
  default int length() { return size(); }

  default Object reduce(IFn f) {
    final int sz = size();
    if (sz == 0 )
      return f.invoke();
    Object init = get(0);
    for(int idx = 1; idx < sz && (!RT.isReduced(init)); ++idx) {
      init = f.invoke(init, get(idx));
    }
    if (RT.isReduced(init))
      return ((IDeref)init).deref();
    return init;
  }

  default Object reduce(IFn f, Object init) {
    final int sz = size();
    for(int idx = 0; idx < sz && (!RT.isReduced(init)); ++idx) {
      init = f.invoke(init, get(idx));
    }
    if (RT.isReduced(init))
      return ((IDeref)init).deref();
    return init;
  }

  default Object kvreduce(IFn f, Object init) {
    final int sz = size();
    for(int idx = 0; idx < sz && (!RT.isReduced(init)); ++idx) {
      init = f.invoke(init, idx, get(idx));
    }
    if (RT.isReduced(init))
      return ((IDeref)init).deref();
    return init;
  }
  @SuppressWarnings("unchecked")
  default public void forEach(Consumer c) {
    final int sz = size();
    for (int idx = 0; idx < sz; ++idx)
      c.accept(get(idx));
  }
  default int hasheq() {
    return CljHash.listHasheq(this);
  }
  default boolean equiv(Object other) {
    return CljHash.listEquiv(this, other);
  }
  default ISeq seq() { return IteratorSeq.create(iterator()); }
  default ISeq rseq() { return IteratorSeq.create(riterator()); }
  default IPersistentMap meta() { return null; }
  default IObj withMeta(IPersistentMap meta ) { throw new RuntimeException("Unimplemented"); }
  default double doubleReduction(DoubleBinaryOperator op, double init) {
    final int sz = size();
    for(int idx = 0; idx < sz; ++idx)
      init = op.applyAsDouble(init, getDouble(idx));
    return init;
  }
  default long longReduction(LongBinaryOperator op, long init) {
    final int sz = size();
    for(int idx = 0; idx < sz; ++idx)
      init = op.applyAsLong(init, getLong(idx));
    return init;
  }
}

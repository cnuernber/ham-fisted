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
import java.util.Random;
import java.util.Collections;
import java.util.Spliterator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
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
import clojure.lang.Sequential;
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
import clojure.lang.ASeq;
import clojure.lang.IReduceInit;
import clojure.lang.Associative;
import clojure.lang.IChunk;
import clojure.lang.IChunkedSeq;
import clojure.lang.PersistentList;
import clojure.lang.ChunkedCons;
import clojure.lang.ArrayChunk;


public interface IMutList<E>
  extends List<E>, RandomAccess, Indexed, IFnDef, ITypedReduce<E>, IKVReduce, IReduce,
	  IHashEq, Seqable, Reversible, IObj, ImmutSort<E>, RangeList, Cloneable,
	  Sequential, Associative, Comparable

{
  default IMutList cloneList() { return (IMutList)ArrayLists.toList(toArray()); }
  default void clear() { throw new UnsupportedOperationException("Unimplemented"); }
  default boolean add(E v) { throw new UnsupportedOperationException("Unimplemented"); }
  default void add(int idx, E v) {
    add(idx, 1, v);
  }
  default void add(int idx, int count, E v) {
    int end = idx + count;
    if(idx == size()) {
      for(; idx < end; ++idx) add( v );
    } else {
      for(; idx < end; ++idx) add( idx, v );
    }
  }
  @SuppressWarnings("unchecked")
  default void addLong(long v) { add((E)Long.valueOf(v)); }
  @SuppressWarnings("unchecked")
  default void addDouble(double v) { add((E)Double.valueOf(v)); }
  default void removeRange(long startidx, long endidx) {
    ChunkedList.checkIndexRange(0, size(), startidx, endidx);
    final int sidx = (int)startidx;
    for(; startidx < endidx; ++startidx) {
      remove(sidx);
    }
  }
  @SuppressWarnings("unchecked")
  default void fillRange(long startidx, final long endidx, Object v) {
    final int sz = size();
    ChunkedList.checkIndexRange(0, (long)sz, startidx, endidx);
    int ss = (int)startidx;
    final int ee = (int)endidx;
    for(; ss < ee; ++ss) {
      set(ss, (E)v);
    }
  }
  @SuppressWarnings("unchecked")
  default void fillRangeReducible(final long startidx, Object v) {
    final int sz = size();
    if (v instanceof RandomAccess) {
      ChunkedList.checkIndexRange(0, sz, startidx, startidx+((List)v).size());
    }
    final int ss = (int)startidx;
    Reductions.serialReduction(new Reductions.IndexedAccum(new IFnDef.OLOO() {
	public Object invokePrim(Object acc, long idx, Object v) {
	  ((List)acc).set((int)idx+ss, v);
	  return acc;
	}
      }), this, v);
  }
  default E remove(int idx) {
    final E retval = get(idx);
    removeRange(idx, idx+1);
    return retval;
  }
  default boolean remove(Object o) {
    final int idx = indexOf(o);
    if ( idx == -1)
      return false;
    remove(idx);
    return true;
  }
  default boolean addAll(Collection<? extends E> c) {
    return addAllReducible(c);
  }
  @SuppressWarnings("unchecked")
  default boolean addAllReducible(Object obj) {
    final int sz = size();
    Reductions.serialReduction(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  ((IMutList<E>)lhs).add((E)rhs);
	  return lhs;
	}
      }, this, obj);
    return sz != size();
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
    // HashSet<Object> hs = new HashSet<Object>();
    // hs.addAll(c);
    // int sz = size();
    // final int osz = sz;
    // for(int idx = 0; idx < sz; ++idx) {
    //   if(hs.contains(get(idx))) {
    // 	remove(idx);
    // 	--idx;
    // 	--sz;
    //   }
    // }
    // return size() == osz;
    throw new UnsupportedOperationException();
  }
  default boolean retainAll(Collection<?> c) {
    // HashSet<Object> hs = new HashSet<Object>();
    // hs.addAll(c);
    // int sz = size();
    // final int osz = sz;
    // for(int idx = 0; idx < sz; ++idx) {
    //   if(!hs.contains(get(idx))) {
    // 	remove(idx);
    // 	--idx;
    // 	--sz;
    //   }
    // }
    // return size() == osz;
    throw new UnsupportedOperationException();
  }

  public static class MutSubList<E> implements IMutList<E> {
    final IMutList<E> list;
    final int sidx;
    final int eidx;
    final int nElems;
    public MutSubList(IMutList<E> list, int ss, int ee) {
      this.list = list;
      sidx = ss;
      eidx = ee;
      nElems = ee - ss;
    }
    public int size() { return nElems; }
    public E get(int idx) {
      ChunkedList.indexCheck(sidx, nElems, idx);
      return list.get(sidx+idx);
    }
    public long getLong(int idx) {
      ChunkedList.indexCheck(sidx, nElems, idx);
      return list.getLong(sidx+idx);
    }
    public double getDouble(int idx) {
      ChunkedList.indexCheck(sidx, nElems, idx);
      return list.getDouble(sidx+idx);
    }
    @SuppressWarnings("unchecked")
    public E set(int idx, E v) {
      ChunkedList.indexCheck(sidx, nElems, idx);
      return list.set(sidx+idx, v);
    }
    public void setLong(int idx, long v) {
      ChunkedList.indexCheck(sidx, nElems, idx);
      list.setLong(sidx+idx, v);
    }
    public void setDouble(int idx, double v) {
      ChunkedList.indexCheck(sidx, nElems, idx);
      list.setDouble(sidx+idx, v);
    }
    public Object reduce(IFn rfn, Object init) {
      final int ee = eidx;
      final IMutList l = list;
      for(int idx = sidx; idx < ee && !RT.isReduced(init); ++idx)
	init = rfn.invoke(init, l.get(idx));
      return Reductions.unreduce(init);
    }
    @SuppressWarnings("unchecked")
    public IMutList<E> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, nElems);
      if(ssidx == 0 && seidx == nElems)
	return this;
      return list.subList(ssidx + sidx, seidx + sidx);
    }
    public IPersistentMap meta() { return list.meta(); }
    @SuppressWarnings("unchecked")
    public IMutList<E> withMeta(IPersistentMap meta) {
      return ((IMutList<E>)list.withMeta(meta)).subList(sidx, eidx);
    }
  }

  @SuppressWarnings("unchecked")
  default IMutList<E> subList(int startidx, int endidx) {
    final int sz = size();
    if (startidx == 0 && endidx == sz)
      return this;
    ChunkedList.sublistCheck(startidx, endidx, size());
    return new MutSubList<E>(this, startidx, endidx);
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

  default int compareTo(Object o) {
    final List l = (List)o;
    final int sz = size();
    final int lsz = l.size();
    if(sz < lsz)
      return -1;
    else if(sz > lsz)
      return 1;
    for(int i = 0; i < sz; i++) {
      int c = Util.compare(get(i),l.get(i));
      if(c != 0)
	return c;
    }
    return 0;
  }

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
    if (idx < 0 || idx > size())
      throw new NoSuchElementException("Index(" + String.valueOf(idx) + ") out of range 0-" + size());
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
  default Spliterator<E> spliterator() {
    return new RandomAccessSpliterator<E>(this);
  }
  default Object[] fillArray(Object[] data) {
    Reductions.serialReduction(new Reductions.IndexedAccum(new IFnDef.OLOO() {
	public Object invokePrim(Object acc, long idx, Object v) {
	  ((Object[])acc)[(int)idx] = v;
	  return acc;
	}
      }), data, this);
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
  default Object toNativeArray() {
    return toArray();
  }
  default int[] toIntArray() {
    final int[] retval = new int[size()];
    ArrayLists.toList(retval).fillRange(0, this);
    return retval;
  }
  default long[] toLongArray() {
    final long[] retval = new long[size()];
    ArrayLists.toList(retval).fillRange(0, this);
    return retval;
  }
  default float[] toFloatArray() {
    final float[] retval = new float[size()];
    ArrayLists.toList(retval).fillRange(0, this);
    return retval;
  }
  default double[] toDoubleArray() {
    final double[] retval = new double[size()];
    ArrayLists.toList(retval).fillRange(0, this);
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

  @SuppressWarnings("unchecked")
  default int[] sortIndirect(Comparator c) {
    final int sz = size();
    int[] retval = ArrayLists.iarange(0, sz, 1);
    final Object[] data = toArray();
    if (c == null)
      ObjectArrays.parallelQuickSortIndirect(retval, data);
    else
      IntArrays.parallelQuickSort(retval,
				  new IntComparator() {
				    public int compare(int lhs, int rhs) {
				      return c.compare(data[lhs], data[rhs]);
				    }
				  });
    return retval;
  }

  default Object nth(int idx) {
    final int sz = size();
    if (idx < 0)
      idx = idx + sz;
    return get(idx);
  }
  default Object nth(int idx, Object notFound) {
    final int sz = size();
    if (idx < 0)
      idx = idx + sz;
    return idx < sz && idx > -1 ? get(idx) : notFound;
  }
  default E set(int idx, E v) { throw new UnsupportedOperationException("Unimplemented"); }
  @SuppressWarnings("unchecked")
  default void setLong(int idx, long v) { set(idx, (E)Long.valueOf(v)); }
  @SuppressWarnings("unchecked")
  default void setDouble(int idx, double v) { set(idx, (E)Double.valueOf(v)); }
  default long getLong(int idx) { return Casts.longCast(get(idx)); }
  default double getDouble(int idx) {
    final Object obj = get(idx);
    return obj != null ? Casts.doubleCast(obj) : Double.NaN;
  }
  default void accPlusLong(int idx, long val) {
    setLong( idx, getLong(idx) + val );
  }
  default void accPlusDouble(int idx, double val) {
    setDouble( idx, getDouble(idx) + val );
  }

  default Object invoke(Object idx) {
    return nth(RT.intCast(Casts.longCast(idx)));
  }
  default Object invoke(Object idx, Object notFound) {
    if(Util.isInteger(idx))
      return nth(RT.intCast(Casts.longCast(idx)), notFound);
    return notFound;
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
    // HashSet<Object> hc = new HashSet<Object>();
    // hc.addAll(c);
    // for(E e: this) {
    //   if(!hc.contains(e))
    // 	return false;
    // }
    // return true;
    throw new UnsupportedOperationException();
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
    return Reductions.unreduce(init);
  }

  default Object reduce(IFn f, Object init) {
    final int sz = size();
    for(int idx = 0; idx < sz && (!RT.isReduced(init)); ++idx) {
      init = f.invoke(init, get(idx));
    }
    return Reductions.unreduce(init);
  }

  default Object kvreduce(IFn f, Object init) {
    final int sz = size();
    for(int idx = 0; idx < sz && (!RT.isReduced(init)); ++idx) {
      init = f.invoke(init, idx, get(idx));
    }
    return Reductions.unreduce(init);
  }
  default Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				   ParallelOptions options) {
    return Reductions.parallelRandAccessReduction(initValFn, rfn, mergeFn,
						  this, options);
  }
  @SuppressWarnings("unchecked")
  default void forEach(Consumer c) {
    ITypedReduce.super.forEach(c);
  }
  default int hasheq() {
    return CljHash.listHasheq(this);
  }
  default boolean equiv(Object other) {
    return CljHash.listEquiv(this, other);
  }

  public static IChunk sublistAsChunk(List data, int sidx) {
    final int nElems = data.size();
    final int len = nElems - sidx;
    if(len > 0) {
      return new IChunk() {
	public int count() { return len; }
	public Object nth(int idx, Object defVal) {
	  return (idx < len) ? data.get(idx+sidx) : defVal;
	}
	public Object nth(int idx) {
	  if(idx < len)
	    return data.get(idx+sidx);
	  throw new IndexOutOfBoundsException();
	}
	public IChunk dropFirst() {
	  return len > 1 ? sublistAsChunk(data, sidx+1) : null;
	}
	public Object reduce(IFn rfn, Object acc) {
	  final int ne = len;
	  for(int idx = 0; idx < ne; ++idx) {
	    acc = rfn.invoke(acc, data.get(idx+sidx));
	    if(RT.isReduced(acc))
	      return ((IDeref)acc).deref();
	  }
	  return acc;
	}
      };
    }
    return null;
  }

  public static IChunkedSeq inplaceSublistSeq(List l, int sidx, int eidx) {
    final int ne = eidx - sidx;
    if(ne > 0) {
      final int len = Math.min(32, ne);
      return new LazyChunkedSeq(new IFnDef() {
	  public Object invoke() {
	    return new ChunkedCons(sublistAsChunk(l.subList(sidx, sidx + len), 0),
				   (ne - len) <= 0 ? null : inplaceSublistSeq(l, sidx+len, eidx));
	  }
	});
    } else {
      return null;
    }
  }

  default IChunkedSeq inplaceSublistSeq() {
    if(isEmpty()) return null; return inplaceSublistSeq(this, 0, size());
  }

  public static IChunkedSeq copyingArraySeq(List l, int sidx, int eidx) {
    final int ne = eidx - sidx;
    if(ne > 0) {
      final int len = Math.min(32, ne);
      return new LazyChunkedSeq(new IFnDef() {
	  public Object invoke() {
	    return new ChunkedCons(new ArrayChunk(l.subList(sidx, sidx + len).toArray()),
				   (ne - len) <= 0 ? null : inplaceSublistSeq(l, sidx+len, eidx));
	  }
	});
    } else {
      return null;
    }
  }
  default IChunkedSeq copyingArraySeq() {
    if(isEmpty()) return null; return copyingArraySeq(this, 0, size());
  }
  default ISeq seq() { return copyingArraySeq(); }
  default ISeq rseq() {
    return isEmpty() ? null : new ReverseList(this, meta()).seq();
  }

  default IPersistentMap meta() { return null; }
  default IObj withMeta(IPersistentMap meta ) { throw new UnsupportedOperationException("Unimplemented"); }

  @SuppressWarnings("unchecked")
  default void sort(Comparator<? super E> c) {
    final Object[] data = toArray();
    if(c == null) {
      ObjectArrays.parallelQuickSort(data);
    } else {
      ObjectArrays.parallelQuickSort(data, (Comparator<? super Object>)c);
    }
    fillRange(0, ArrayLists.toList(data));
  }

  @SuppressWarnings("unchecked")
  default List immutSort(Comparator c) {
    final IMutList retval = cloneList();
    retval.sort(c);
    return retval;
  }

  default void shuffle(Random r) {
    Collections.shuffle(this, r);
  }
  default List reindex(int[] indexes) {
    return ReindexList.create(indexes, this, this.meta());
  }
  default List immutShuffle(Random r) {
    final Object[] retval = toArray();
    ObjectArrays.shuffle(retval, r);
    return ArrayLists.toList(retval, 0, size(), meta());
  }
  default List reverse() {
    return ReverseList.create(this, meta());
  }
  @SuppressWarnings("unchecked")
  default int binarySearch(E v, Comparator<? super E> c) {
    int rv;
    if(c == null)
      rv = Collections.binarySearch(this,v,new Comparator<E>() {
	  public int compare(E lhs, E rhs) {
	    return Util.compare(lhs, rhs);
	  }
	});
    else
      rv = Collections.binarySearch(this,v,c);
    return rv < 0 ? -1 - rv : rv;
  }
  default int binarySearch(E v) { return binarySearch(v, null); }
  default IPersistentVector immut() { return ArrayImmutList.create(true, toArray(), 0, size(), meta()); }
  default Associative assoc(Object idx, Object o) {
    return immut().assoc(idx, o);
  }
  default IPersistentVector cons(Object o) {
    return immut().cons(o);
  }
  default IPersistentVector empty() {
    return ArrayImmutList.EMPTY;
  }

  //Long stream to account for IMutLists that are longer than Integer.MAX_VALUE.
  default LongStream indexStream(boolean parallel) {
    LongStream retval = LongStream.range(0, size());
    return parallel ? retval.parallel() : retval;
  }

  default Stream objStream(boolean parallel) {
    return indexStream(parallel).mapToObj((long idx)->get((int)idx));
  }

  default DoubleStream doubleStream(boolean parallel) {
    return indexStream(parallel).mapToDouble((long idx)->getDouble((int)idx));
  }

  default LongStream longStream(boolean parallel) {
    return indexStream(parallel).map((long idx)->getLong((int)idx));
  }
}

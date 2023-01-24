package ham_fisted;


import java.util.List;
import java.util.Arrays;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Collections;
import java.util.Collection;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.function.IntFunction;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.lang.IReduce;
import clojure.lang.IReduceInit;
import clojure.lang.IPersistentVector;
import clojure.lang.Associative;
import clojure.lang.IMapEntry;
import clojure.lang.Util;
import clojure.lang.MapEntry;
import clojure.lang.IPersistentStack;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.bytes.ByteComparator;
import it.unimi.dsi.fastutil.shorts.ShortArrays;
import it.unimi.dsi.fastutil.shorts.ShortComparator;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.fastutil.chars.CharComparator;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatComparator;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;

import it.unimi.dsi.fastutil.objects.ObjectArrays;


public class ArrayLists {
  public static int checkIndex(final int idx, final int dlen) {
    if (idx >= 0 && idx < dlen) return idx;
    return ChunkedList.indexCheck(0, dlen, idx);
  }
  public static int wrapCheckIndex(int idx, final int dlen) {
    if(idx < 0)
      idx += dlen;
    return checkIndex(idx, dlen);
  }
  public static void checkIndexRange(int dlen, int ssidx, int seidx) {
    ChunkedList.checkIndexRange(0, dlen, ssidx, seidx);
  }
  public static void checkIndexRange(long dlen, long ssidx, long seidx) {
    ChunkedList.checkIndexRange(0, dlen, ssidx, seidx);
  }
  public static DoubleConsumer asDoubleConsumer(Object c) {
    if (c instanceof DoubleConsumer)
      return (DoubleConsumer) c;
    return null;
  }
  public static LongConsumer asLongConsumer(Object c) {
    if (c instanceof LongConsumer)
      return (LongConsumer) c;
    return null;
  }
  public interface ArrayOwner {
    ArraySection getArraySection();
    void fill(int sidx, int eidx, Object v);
    Object copyOfRange(int sidx, int eidx);
    Object copyOf(int len);
  }
  public interface ArrayPersistentVector extends IMutList<Object>, IPersistentVector {
    IPersistentVector unsafeImmut();
    default boolean equiv(Object other) {
      return IMutList.super.equiv(other);
    }
    default IPersistentVector cons(Object o) { return immut().cons(o); }
    default IPersistentVector assocN(int i, Object o) {
      return immut().assocN(i, o);
    }
    default int length() { return size(); }
    default Associative assoc(Object idx, Object o) {
      return immut().assoc(idx, o);
    }
    default IPersistentStack pop() {
      final int nElems = size();
      if (nElems == 0)
	throw new RuntimeException("Can't pop empty vector");
      if (nElems == 1)
	return ImmutList.EMPTY.withMeta(meta());
      return immut().pop();
    }
    default Object peek() {
      final int nElems = size();
      if (nElems == 0)
	return null;
      return get(nElems-1);
    }
    default ImmutList empty() {
      return ImmutList.EMPTY.withMeta(meta());
    }
  }
  @SuppressWarnings("unchecked")
  static boolean fillRangeArrayCopy(Object dest, long sidx, long eidx,
				    long startidx, Object ll) {
    //True means this function took care of the transfer, false means
    //fallback to a more generalized transfer
    if(ll instanceof RandomAccess) {
      final List l = (List)ll;
      if (l.isEmpty()) return true;
      final long endidx = startidx + l.size();
      checkIndexRange(eidx-sidx, startidx, endidx);
    }
    if(ll instanceof ArrayOwner) {
      final ArraySection as = ((ArrayOwner)ll).getArraySection();
      final int sz = as.size();
      if(dest.getClass().isAssignableFrom(as.array.getClass())) {
	System.arraycopy(as.array, as.sidx, dest, (int)(sidx+startidx), sz);
	return true;
      }
    }
    return false;
  }

  static List immutShuffleDefault(IMutList m, Random r) {
    final IMutList retval = m.cloneList();
    retval.shuffle(r);
    return retval;
  }
  @SuppressWarnings("unchecked")
  static List immutSortDefault(IMutList m, Comparator c) {
    final IMutList retval = m.cloneList();
    retval.sort(c);
    return retval;
  }
  public interface IArrayList extends IMutList<Object>, ArrayOwner, TypedList,
				      ArrayPersistentVector {
    default Class containedType() { return getArraySection().array.getClass().getComponentType(); }
    default IPersistentVector unsafeImmut() { return ImmutList.create(true, meta(), (Object[])getArraySection().array); }
    default List immutShuffle(Random r) { return immutShuffleDefault(this, r); }
    default List immutSort(Comparator c) { return immutSortDefault(this, c); }
    default void fillRange(long startidx, long endidx, Object v) {
      checkIndexRange(size(), startidx, endidx);
      fill((int)startidx, (int)endidx, v);
    }
    default void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	ArrayPersistentVector.super.fillRangeReducible(startidx, v);
      }
    }
    default Object ensureCapacity(int newlen) {
      throw new RuntimeException("unimplemented");
    }
  }
  public interface ILongArrayList extends LongMutList, ArrayOwner, TypedList,
					  ArrayPersistentVector
  {
    default Class containedType() { return getArraySection().array.getClass().getComponentType(); }
    default IPersistentVector unsafeImmut() { return ImmutList.create(true, meta(), (Object[])getArraySection().array); }
    default List immutShuffle(Random r) { return immutShuffleDefault(this, r); }
    default List immutSort(Comparator c) { return immutSortDefault(this, c); }
    default void fillRange(long startidx, long endidx, Object v) {
      checkIndexRange(size(), startidx, endidx);
      fill((int)startidx, (int)endidx, v);
    }
    default void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, (int)startidx, v))
	LongMutList.super.fillRangeReducible(startidx, v);
    }
    default Object reduce(IFn rfn, Object init) {
      return LongMutList.super.reduce(rfn, init);
    }
    default Object toNativeArray() { return copyOf(size()); }
    default Object ensureCapacity(int newlen) {
      throw new RuntimeException("unimplemented");
    }
  }
  public interface IDoubleArrayList extends DoubleMutList, ArrayOwner, TypedList,
					    ArrayPersistentVector
  {
    default Class containedType() { return getArraySection().array.getClass().getComponentType(); }
    default IPersistentVector unsafeImmut() { return ImmutList.create(true, meta(), (Object[])getArraySection().array); }
    default List immutShuffle(Random r) { return immutShuffleDefault(this, r); }
    default List immutSort(Comparator c) { return immutSortDefault(this, c); }
    default void fillRange(long startidx, long endidx, Object v) {
      checkIndexRange(size(), startidx, endidx);
      fill((int)startidx, (int)endidx, v);
    }
    default void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v))
	DoubleMutList.super.fillRangeReducible(startidx, v);
    }
    default Object toNativeArray() { return copyOf(size()); }
    default Object ensureCapacity(int newlen) {
      throw new RuntimeException("unimplemented");
    }
  }

  static int fixSubArrayBinarySearch(final int sidx, final int len, final int res) {
    return res < 0 ? -1 - (res + sidx) : res - sidx;
  }

  public static Object[] objectArray(int len) { return new Object[len]; }

  public static class ObjectArraySubList implements IArrayList {
    public final Object[] data;
    public final int sidx;
    public final int eidx;
    public final int nElems;
    public final IPersistentMap meta;
    public ObjectArraySubList(Object[] d, int _sidx, int _eidx, IPersistentMap m) {
      data = d;
      sidx = _sidx;
      eidx = _eidx;
      nElems = eidx - sidx;
      meta = m;
    }
    public boolean isCompatible(Object other) {
      return other instanceof Object[];
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, eidx); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[checkIndex(idx, nElems) + sidx]; }
    public Object nth(int idx) {
      return data[checkIndex(idx < 0 ? idx + nElems : idx, nElems) + sidx];
    }
    public Object set(int idx, Object obj) {
      idx = checkIndex(idx, nElems) + sidx;
      final Object retval = data[idx];
      data[idx] = obj;
      return retval;
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return (IMutList<Object>)toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, eidx, m);
    }
    public Object[] toArray() {
      return Arrays.copyOfRange(data, sidx, eidx);
    }
    public void sort(Comparator<? super Object> c) {
      if(c == null)
	Arrays.sort(data, sidx, eidx);
      else
	Arrays.sort(data, sidx, eidx, c);
    }
    public void shuffle(Random r) {
      ObjectArrays.shuffle(data, sidx, eidx, r);
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      return fixSubArrayBinarySearch(sidx, size(),
				     c == null ? ObjectArrays.binarySearch(data, sidx, eidx, v)
				     : ObjectArrays.binarySearch(data, sidx, eidx, v, c));

    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      final int es = eidx;
      final Object[] d = data;
      for(int ss = sidx; ss < es; ++ss)
	c.accept(d[ss]);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ss = (int)startidx + sidx;
	final int ee = sidx + size();
	Reductions.serialReduction(new Reductions.IndexedAccum( startidx+sidx, new IFn.OLOO() {
	    public Object invokePrim(Object acc, long idx, Object v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    " is out of range: " +
						    String.valueOf(size()));
	      ((Object[])acc)[(int)idx] = v;
	      return acc;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx + ssidx, sidx + seidx, v);
    }
    public Object copyOfRange(int ssidx, int seidx) {
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static int newArrayLen(int len) {
    return len < 100000 ? len * 2 : (int)(len * 1.5);
  }

  public static long newArrayLen(long len) {
    return len < 100000 ? len * 2 : (long)(len * 1.5);
  }

  public static class ObjectArrayList implements IArrayList {
    Object[] data;
    int nElems;
    IPersistentMap meta;
    public ObjectArrayList(Object[] d, int ne, IPersistentMap meta) {
      data = d;
      nElems = ne;
    }
    public ObjectArrayList(int capacity) {
      this(new Object[capacity], 0, null);
    }
    public ObjectArrayList() {
      this(4);
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return new ObjectArrayList((Object[])copyOf(nElems),
							     nElems, meta); }
    public ArraySection getArraySection() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[checkIndex(idx, nElems)]; }
    public Object set(int idx, Object obj) {
      idx = checkIndex(idx, nElems);
      final Object retval = data[idx];
      data[idx] = obj;
      return retval;
    }
    public int capacity() { return data.length; }
    public Object[] ensureCapacity(int len) {
      Object[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, newArrayLen(len));
      }
      return d;
    }
    public boolean add(Object obj) {
      final int ne = nElems;
      final Object [] d = ensureCapacity(ne+1);
      d[ne] = obj;
      nElems = ne+1;
      return true;
    }
    public void add(int idx, Object obj) {
      idx = checkIndex(idx, nElems);
      if (idx == nElems) { add(obj); return; }

      final int ne = nElems;
      final Object [] d = ensureCapacity(ne+1);
      System.arraycopy(d, idx, d, idx+1, ne - idx);
      d[idx] = obj;
      nElems = ne+1;
    }
    /// Extra method because some things that implement IReduceInit are not
    /// collections.
    public boolean addAllReducible(Object c) {
      final int sz = size();
      if (c instanceof RandomAccess) {
	final List cl = (List) c;
	if (cl.isEmpty() ) return false;
	final int cs = cl.size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRangeReducible(sz, cl);
      } else  {
	IArrayList.super.addAllReducible(c);
      }
      return sz != size();
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return (IMutList<Object>)toList(data, ssidx, seidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      meta = m;
      return this;
    }
    public Object[] toArray() {
      return Arrays.copyOf(data, nElems);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      Arrays.fill(data, endidx, nElems, null);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public void sort(Comparator<? super Object> c) {
      subList(0, nElems).sort(c);
    }
    public void shuffle(Random r) {
      ((IMutList)subList(0, nElems)).shuffle(r);
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      final int es = nElems;
      final Object[] d = data;
      for(int ss = 0; ss < es; ++ss)
	c.accept(d[ss]);
    }
    public void fillRangeReducible(long startidx, Object v) {
      subList(0, size()).fillRangeReducible(startidx, v);
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(nElems, ssidx, seidx);
      Arrays.fill(data, ssidx, seidx, v);
    }
    public Object copyOfRange(int ssidx, int seidx) {
      return Arrays.copyOfRange(data, ssidx, seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOf(data, len);
    }
    public static ObjectArrayList wrap(final Object[] data, int nElems, IPersistentMap m) {
      if (data.length < nElems)
	throw new RuntimeException("Array len less than required");
      return new ObjectArrayList(data, nElems, m);
    }
    public static ObjectArrayList wrap(final Object[] data, IPersistentMap m) {
      return new ObjectArrayList(data, data.length, m);
    }
  }

  @SuppressWarnings("unchecked")
  public static Object[] toArray(Collection c) {
    final ObjectArrayList res = new ObjectArrayList();
    res.addAllReducible(c);
    return res.toArray();
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] toArray(Collection c, T[] d) {
    final ObjectArrayList res = ObjectArrayList.wrap(d, 0, null);
    res.addAllReducible(c);
    return (T[])res.toArray();
  }

  public static IMutList<Object> toList(final Object[] data, final int sidx, final int eidx, final IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ObjectArraySubList(data, sidx, eidx, meta);
  }
  public static IMutList<Object> toList(final Object[] data) { return toList(data, 0, data.length, null); }


  public static class ByteArraySubList implements ILongArrayList {
    public final byte[] data;
    public final int dlen;
    public final int sidx;
    public final IPersistentMap meta;
    public ByteArraySubList(byte[] d, int s, int len, IPersistentMap _meta) {
      data = d;
      sidx = s;
      dlen = len;
      meta = _meta;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, sidx + dlen); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return dlen; }
    public Byte get(int idx) { return data[checkIndex(idx, dlen) + sidx]; }
    public long getLong(int idx) { return data[checkIndex(idx, dlen) + sidx]; }
    public void setLong(int idx, long oobj) {
      data[checkIndex(idx, dlen) + sidx] = RT.byteCast(oobj);
    }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, sidx+dlen)); }
    public IntComparator indexComparator() {
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return Byte.compare(data[lidx+sidx], data[ridx+sidx]);
	}
      };
    }
    public LongMutList subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return (LongMutList)toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return new ByteArraySubList(data, sidx, sidx + dlen, m);
    }
    public Object[] toArray() {
      final int sz = size();
      Object[] retval = new Object[size()];
      for (int idx = 0; idx < sz; ++idx)
	retval[idx] = data[idx+sidx];
      return retval;
    }
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super Object> c) {
      if (c == null)
	Arrays.sort(data, sidx, sidx+dlen);
      else {
	ILongArrayList.super.sort(c);
      }
    }
    public void shuffle(Random r) {
      ByteArrays.shuffle(data, sidx, sidx+dlen, r);
    }
    public static ByteComparator asByteComparator(Comparator c) {
      if (c instanceof ByteComparator)
	return (ByteComparator)c;
      else if (c instanceof LongComparator) {
	final LongComparator lc = (LongComparator)c;
	return new ByteComparator() {
	  public int compare(byte l, byte r) { return lc.compare(l,r); }
	};
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final byte vv = RT.byteCast(Casts.longCast(v));
      final ByteComparator bc = asByteComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? ByteArrays.binarySearch(data, sidx, sidx+size(), vv) : ByteArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
      return ILongArrayList.super.binarySearch(v, c);
    }

    public Object longReduction(IFn.OLO rfn, Object init) {
      final int es = sidx + dlen;
      final byte[] d = data;
      for(int ss = sidx; ss < es && !RT.isReduced(init); ++ss)
	init = rfn.invokePrim(init, d[ss]);
      return Reductions.unreduce(init);
    }

    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ss = (int)startidx + sidx;
	final int ee = sidx + size();
	Reductions.serialReduction(new Reductions.IndexedLongAccum( startidx+sidx,
								    new IFn.OLLO() {
	    public Object invokePrim(Object acc, long idx, long v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    " is out of range: " +
						    String.valueOf(size()));
	      data[(int)idx] = RT.byteCast(v);
	      return data;
	    }}), data, v);
      }
    }

    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx + ssidx, sidx + seidx, RT.byteCast(Casts.longCast(v)));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static IMutList<Object> toList(final byte[] data, final int sidx, final int eidx, final IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ByteArraySubList(data, sidx, dlen, meta);
  }
  public static IMutList<Object> toList(final byte[] data) { return toList(data, 0, data.length, null); }


  public static class ShortArraySubList implements ILongArrayList {
    public final short[] data;
    public final int sidx;
    public final int dlen;
    public final IPersistentMap meta;

    public ShortArraySubList(short[] d, int s, int len, IPersistentMap _meta) {
      data = d;
      sidx = s;
      dlen = len;
      meta = _meta;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, sidx+dlen)); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, sidx + dlen); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return dlen; }
    public long getLong(int idx) { return data[checkIndex(idx, dlen) + sidx]; }
    public void setLong(int idx, long oobj) {
      data[checkIndex(idx, dlen) + sidx] = RT.shortCast(Casts.longCast(oobj));
    }
    public IntComparator indexComparator() {
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return Short.compare(data[lidx+sidx], data[ridx+sidx]);
	}
      };
    }
    public LongMutList subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return (LongMutList)toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, sidx + dlen, m);
    }
    public Object[] toArray() {
      final int sz = size();
      Object[] retval = new Object[size()];
      for (int idx = 0; idx < sz; ++idx)
	retval[idx] = data[idx+sidx];
      return retval;
    }
    @SuppressWarnings("unchecked")
    public void sort(Comparator c) {
      if (c == null)
	Arrays.sort(data, sidx, sidx+dlen);
      else {
	final Object[] odata = toArray();
	Arrays.sort(odata, c);
	final short[] d = data;
	final int sz = size();
	final int ss = sidx;
	for (int idx = 0; idx < sz; ++idx) {
	  d[idx+ss] = (short)odata[idx];
	}
      }
    }

    public void shuffle(Random r) {
      ShortArrays.shuffle(data, sidx, sidx+dlen, r);
    }
    public static ShortComparator asShortComparator(Comparator c) {
      if (c instanceof ShortComparator)
	return (ShortComparator)c;
      else if (c instanceof LongComparator) {
	final LongComparator lc = (LongComparator)c;
	return new ShortComparator() {
	  public int compare(short l, short r) { return lc.compare(l,r); }
	};
      }
      return null;
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final short vv = RT.shortCast(Casts.longCast(v));
      final ShortComparator bc = asShortComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? ShortArrays.binarySearch(data, sidx, sidx+size(), vv) : ShortArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
      return ILongArrayList.super.binarySearch(v, c);
    }
    public Object longReduction(IFn.OLO rfn, Object init) {
      final int es = sidx + dlen;
      final short[] d = data;
      for(int ss = sidx; ss < es && !RT.isReduced(init); ++ss)
	init = rfn.invokePrim(init, d[ss]);
      return Reductions.unreduce(init);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ss = (int)startidx + sidx;
	final int ee = sidx + size();
	Reductions.serialReduction(new Reductions.IndexedLongAccum( startidx+sidx,
								    new IFn.OLLO() {
	    public Object invokePrim(Object acc, long idx, long v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    " is out of range: " +
						    String.valueOf(size()));
	      data[(int)idx] = RT.shortCast(v);
	      return data;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx + ssidx, sidx + seidx, RT.shortCast(Casts.longCast(v)));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, size());
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static IMutList<Object> toList(final short[] data, final int sidx, final int eidx, final IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ShortArraySubList(data, sidx, dlen, meta);
  }
  public static IMutList<Object> toList(final short[] data) { return toList(data, 0, data.length, null); }

  public static int[] intArray(int len) { return new int[len]; }
  public static class IntArraySubList implements ILongArrayList {
    public final int[] data;
    public final int sidx;
    public final int eidx;
    public final int nElems;
    public final IPersistentMap meta;
    public IntArraySubList(int[] d, int _sidx, int _eidx, IPersistentMap m) {
      data = d;
      sidx = _sidx;
      eidx = _eidx;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, eidx)); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, eidx); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public long getLong(int idx) { return data[checkIndex(idx, nElems) + sidx]; }
    static void setLong(final int[] d, final int sidx, final int nElems,
			int idx, final long obj) {
      int v = RT.intCast(obj);
      idx = checkIndex(idx, nElems) + sidx;
      d[idx] = v;
    }
    public void setLong(int idx, long obj) {
      setLong(data, sidx, nElems, idx, obj);
    }
    public LongMutList subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return (LongMutList)toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, eidx, m);
    }
    public Object[] toArray() {
      final int[] d = data;
      final int ss = sidx;
      final int ne = nElems;
      Object[] retval = new Object[ne];
      for(int idx = 0; idx < ne; ++idx)
	retval[idx] = d[idx+ss];
      return retval;
    }
    public int[] toIntArray() {
      return Arrays.copyOfRange(data, sidx, eidx);
    }
    @SuppressWarnings("unchecked")
    public static IntComparator indexComparator(int[] d, int sidx, Comparator c) {
      if (c == null) {
	if (sidx != 0) {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return Integer.compare(d[lidx+sidx], d[ridx+sidx]);
	    }
	  };
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return Integer.compare(d[lidx], d[ridx]);
	    }
	  };
	}
      } else {
	if(c instanceof LongComparator) {
	  final LongComparator lc = (LongComparator) c;
	  if (sidx != 0) {
	    return new IntComparator() {
	      public int compare(int lidx, int ridx) {
		return lc.compare(d[lidx+sidx], d[ridx+sidx]);
	      }
	    };
	  } else {
	    return new IntComparator() {
	      public int compare(int lidx, int ridx) {
		return lc.compare(d[lidx], d[ridx]);
	      }
	    };
	  }
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return c.compare(d[lidx+sidx], d[ridx+sidx]);
	    }
	  };
	}
      }
    }
    public IntComparator indexComparator() {
      return indexComparator(data, sidx, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return indexComparator(data, sidx, c);
    }
    @SuppressWarnings("unchecked")
    public static IntComparator toIntComparator(Comparator c) {
      if (c instanceof IntComparator)
	return (IntComparator) c;
      else
	return new IntComparator() {
	public int compare(int lhs, int rhs) {
	  return c.compare(lhs, rhs);
	}
      };
    }
    public void sort(Comparator<? super Object> c) {
      if(c == null)
	IntArrays.parallelQuickSort(data, sidx, eidx);
      else {
	IntArrays.parallelQuickSort(data, sidx, eidx, toIntComparator(c));
      }
    }
    public void shuffle(Random r) {
      IntArrays.shuffle(data, sidx, eidx, r);
    }
    public static IntComparator asIntComparator(Comparator c) {
      if (c instanceof IntComparator)
	return (IntComparator)c;
      else if (c instanceof LongComparator) {
	final LongComparator lc = (LongComparator)c;
	return new IntComparator() {
	  public int compare(int l, int r) { return lc.compare(l,r); }
	};
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final int vv = RT.intCast(Casts.longCast(v));
      final IntComparator bc = asIntComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? IntArrays.binarySearch(data, sidx, sidx+size(), vv) : IntArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
	return ILongArrayList.super.binarySearch(v, c);
    }
    public int[] sortIndirect(Comparator c) {
      final int sz = size();
      int[] retval = iarange(0, sz, 1);
      if(sz < 2)
	return retval;
      if(c == null)
	IntArrays.parallelQuickSortIndirect(retval, data, sidx, eidx);
      else
	IntArrays.parallelQuickSort(retval, indexComparator(c));
      return retval;
    }
    public Object reduce(IFn rfn, Object init) {
      return ILongArrayList.super.reduce(rfn, init);
    }
    public Object longReduction(IFn.OLO rfn, Object init) {
      final int es = eidx;
      final int[] d = data;
      for(int ss = sidx; ss < es && !RT.isReduced(init); ++ss)
	init = rfn.invokePrim(init, d[ss]);
      return Reductions.unreduce(init);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int[] d = data;
	final int ee = sidx + size();
	Reductions.serialReduction(new Reductions.IndexedLongAccum( startidx + sidx,
								    new IFn.OLLO() {
	    public Object invokePrim(Object acc, long idx, long v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    "> length "  + String.valueOf(size()));
	      d[(int)idx] = RT.intCast(v);
	      return d;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx + ssidx, sidx + seidx, RT.intCast(Casts.longCast(v)));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, size());
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static class IntArrayList implements ILongArrayList {
    int[] data;
    int nElems;
    IPersistentMap meta;
    public IntArrayList(int[] d, int ne, IPersistentMap meta) {
      data = d;
      nElems = ne;
    }
    public IntArrayList(int capacity) {
      this(new int[capacity], 0, null);
    }
    public IntArrayList() {
      this(4);
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return new IntArrayList((int[])copyOf(nElems),
							  nElems, meta); }
    public ArraySection getArraySection() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public long getLong(int idx) { return data[checkIndex(idx, nElems)]; }
    public void setLong(int idx, long obj) {
      IntArraySubList.setLong(data, 0, nElems, idx, obj);
    }
    public int capacity() { return data.length; }
    public int[] ensureCapacity(int len) {
      int[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
      }
      return d;
    }
    public void addLong(long obj) {
      int val = RT.intCast(obj);
      final int ne = nElems;
      final int[] d = ensureCapacity(ne+1);
      d[ne] = val;
      nElems = ne+1;
    }
    public void add(int idx, Object obj) {
      idx = wrapCheckIndex(idx, nElems);
      if (idx == nElems) { add(obj); return; }

      final int val = RT.intCast(Casts.longCast(obj));
      final int ne = nElems;
      final int[] d = ensureCapacity(ne+1);
      System.arraycopy(d, idx, d, idx+1, ne - idx);
      d[idx] = val;
      nElems = ne+1;
    }
    public boolean addAllReducible(Object c) {
      final int sz = size();
      if (c instanceof RandomAccess) {
	final List cl = (List) c;
	if (cl.isEmpty() ) return false;
	final int cs = cl.size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRangeReducible(sz, cl);
      } else {
	ILongArrayList.super.addAllReducible(c);
      }
      return sz != size();
    }
    public boolean addAll(int sidx, Collection <? extends Object> c) {
      sidx = wrapCheckIndex(sidx, nElems);
      if (c.isEmpty()) return false;
      final int cs = c.size();
      final int sz = size();
      final int eidx = sidx + cs;
      ensureCapacity(cs+sz);
      nElems += cs;
      System.arraycopy(data, sidx, data, eidx, sz - sidx);
      fillRangeReducible(sidx, c);
      return true;
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx, seidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      meta = m;
      return this;
    }
    public Object[] toArray() {
      return subList(0, nElems).toArray();
    }
    public int[] toIntArray() {
      return Arrays.copyOf(data, nElems);
    }
    public void fillRange(long startidx, long endidx, Object v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, endidx, v);
    }
    public void fillRangeReducible(long startidx, Object v) {
      subList(0,size()).fillRangeReducible(startidx, v);
    }
    public void addRange(final int startidx, final int endidx, final Object v) {
      final int ne = nElems;
      checkIndexRange(ne, startidx, endidx);
      final int rangeLen = endidx - startidx;
      final int newLen = ne + rangeLen;
      ensureCapacity(newLen);
      System.arraycopy(data, startidx, data, endidx, nElems - startidx);
      fillRange(startidx, endidx, v);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public Object longReduction(IFn.OLO op, long init) {
      return ((LongMutList)subList(0, nElems)).longReduction(op, init);
    }
    public void sort(Comparator<? super Object> c) {
      subList(0, nElems).sort(c);
    }
    public void shuffle(Random r) {
      ((IMutList)subList(0, nElems)).shuffle(r);
    }
    public int[] sortIndirect(Comparator c) {
      return ((IMutList)subList(0, nElems)).sortIndirect(c);
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      return ((IMutList)subList(0, nElems)).binarySearch(v, c);
    }
    public IntComparator indexComparator() {
      return IntArraySubList.indexComparator(data, 0, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return IntArraySubList.indexComparator(data, 0, c);
    }
    public Object longReduction(IFn.OLO rfn, Object init) {
      final int es = nElems;
      final int[] d = data;
      for(int ss = 0; ss < es && !RT.isReduced(init); ++ss)
	init = rfn.invokePrim(init, d[ss]);
      return Reductions.unreduce(init);
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, ssidx, seidx, RT.intCast(Casts.longCast(v)));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, size());
      return Arrays.copyOfRange(data, ssidx, seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOf(data, len);
    }
    public static IntArrayList wrap(final int[] data, int nElems, IPersistentMap m) {
      if (data.length < nElems)
	throw new RuntimeException("Array len less than required");
      return new IntArrayList(data, nElems, m);
    }
    public static IntArrayList wrap(final int[] data, IPersistentMap m) {
      return new IntArrayList(data, data.length, m);
    }
  }

  @SuppressWarnings("unchecked")
  public static IntComparator intIndexComparator(List srcData, Comparator comp) {
    if(comp != null) {
      if(srcData instanceof IMutList) {
	return ((IMutList)srcData).indexComparator(comp);
      } else {
	return new IntComparator() {
	  public int compare(int l, int r) {
	    return comp.compare(srcData.get(l), srcData.get(r));
	  }
	};
      }
    } else {
      if (srcData instanceof IMutList) {
	return ((IMutList)srcData).indexComparator();
      } else {
	return new IntComparator() {
	  public int compare(int l, int r) {
	    return ((Comparable)srcData.get(l)).compareTo(srcData.get(r));
	  }
	};
      }
    }
  }

  public static IMutList<Object> toList(final int[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new IntArraySubList(data, sidx, eidx, meta);
  }
  public static IMutList<Object> toList(final int[] data) { return toList(data, 0, data.length, null); }

  public static long[] longArray(int len) { return new long[len]; }
  public static class LongArraySubList implements ILongArrayList {
    public final long[] data;
    public final int sidx;
    public final int eidx;
    public final int nElems;
    public final IPersistentMap meta;
    public LongArraySubList(long[] d, int _sidx, int _eidx, IPersistentMap m) {
      data = d;
      sidx = _sidx;
      eidx = _eidx;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, eidx); }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, eidx)); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public long getLong(int idx) { return data[checkIndex(idx, nElems) + sidx]; }
    public Object get(int idx) { return data[checkIndex(idx, nElems) + sidx]; }
    public Object nth(int idx) {
      return data[checkIndex(idx < 0 ? idx + nElems : idx, nElems) + sidx];
    }
    static void setLong(final long[] d, final int sidx, final int nElems,
			int idx, final long v) {
      idx = checkIndex(idx, nElems) + sidx;
      d[idx] = v;
    }
    public void setLong(int idx, long obj) {
      setLong(data, sidx, nElems, idx, obj);
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, eidx, m);
    }
    public Object[] toArray() {
      final long[] d = data;
      final int ss = sidx;
      final int ne = nElems;
      Object[] retval = new Object[ne];
      for(int idx = 0; idx < ne; ++idx)
	retval[idx] = d[idx+ss];
      return retval;
    }
    public long[] toLongArray() {
      return Arrays.copyOfRange(data, sidx, eidx);
    }
    @SuppressWarnings("unchecked")
    public static IntComparator indexComparator(long[] d, int sidx, Comparator c) {
      if (c == null) {
	if (sidx != 0) {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return Long.compare(d[lidx+sidx], d[ridx+sidx]);
	    }
	  };
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return Long.compare(d[lidx], d[ridx]);
	    }
	  };
	}
      } else {
	if(c instanceof LongComparator) {
	  final LongComparator lc = (LongComparator) c;
	  if (sidx != 0) {
	    return new IntComparator() {
	      public int compare(int lidx, int ridx) {
		return lc.compare(d[lidx+sidx], d[ridx+sidx]);
	      }
	    };
	  } else {
	    return new IntComparator() {
	      public int compare(int lidx, int ridx) {
		return lc.compare(d[lidx], d[ridx]);
	      }
	    };
	  }
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return c.compare(d[lidx+sidx], d[ridx+sidx]);
	    }
	  };
	}
      }
    }
    public IntComparator indexComparator() {
      return indexComparator(data, sidx, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return indexComparator(data, sidx, c);

    }
    @SuppressWarnings("unchecked")
    public static LongComparator toLongComparator(Comparator c) {
      if (c instanceof LongComparator)
	return (LongComparator) c;
      else
	return new LongComparator() {
	public int compare(long lhs, long rhs) {
	  return c.compare(lhs, rhs);
	}
      };
    }
    public void sort(Comparator<? super Object> c) {
      if(c == null)
	LongArrays.parallelQuickSort(data, sidx, eidx);
      else {
	LongArrays.parallelQuickSort(data, sidx, eidx, toLongComparator(c));
      }
    }
    public int[] sortIndirect(Comparator c) {
      final int sz = size();
      int[] retval = iarange(0, sz, 1);
      if(sz < 2)
	return retval;
      if(c == null)
	LongArrays.parallelQuickSortIndirect(retval, data, sidx, eidx);
      else
	IntArrays.parallelQuickSort(retval, indexComparator(c));
      return retval;
    }
    public void shuffle(Random r) {
      LongArrays.shuffle(data, sidx, eidx, r);
    }
    public static LongComparator asLongComparator(Comparator c) {
      if (c instanceof LongComparator)
	return (LongComparator)c;
      return null;
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final long vv = Casts.longCast(v);
      final LongComparator bc = asLongComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? LongArrays.binarySearch(data, sidx, sidx+size(), vv) : LongArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
      return ILongArrayList.super.binarySearch(v, c);
    }
    public Object longReduction(IFn.OLO rfn, Object init) {
      final int ee = size();
      final long[] d = data;
      for(int idx = 0; idx < ee && !RT.isReduced(init); ++idx)
	init = rfn.invokePrim(init, data[idx+sidx]);
      return Reductions.unreduce(init);
    }
    public void longForEach(LongConsumer c) {
      final int es = eidx;
      final long[] d = data;
      for(int ss = sidx; ss < es; ++ss)
	c.accept(d[ss]);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ee = sidx + size();
	final long[] d = data;
	Reductions.serialReduction(new Reductions.IndexedLongAccum( startidx + sidx,
								    new IFn.OLLO() {
	    public Object invokePrim(Object acc, long idx, long v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx-sidx) +
						    " > length: " + String.valueOf(size()));
	      d[(int)idx] = v;
	      return d;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx + ssidx, sidx + seidx, Casts.longCast(v));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static class LongArrayList implements ILongArrayList {
    long[] data;
    int nElems;
    IPersistentMap meta;
    public LongArrayList(long[] d, int ne, IPersistentMap meta) {
      data = d;
      nElems = ne;
    }
    public LongArrayList(int capacity) {
      this(new long[capacity], 0, null);
    }
    public LongArrayList() {
      this(4);
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return new LongArrayList((long[])copyOf(nElems),
							   nElems, meta); }
    public ArraySection getArraySection() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public long getLong(int idx) { return data[checkIndex(idx, nElems)]; }
    public void setLong(int idx, long obj) {
      LongArraySubList.setLong(data, 0, nElems, idx, obj);
    }
    public int capacity() { return data.length; }
    public long[] ensureCapacity(int len) {
      long[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
      }
      return d;
    }
    public void addLong(long val) {
      final int ne = nElems;
      final long[] d = ensureCapacity(ne+1);
      d[ne] = val;
      nElems = ne+1;
    }
    public boolean add(Object obj) { addLong(Casts.longCast(obj)); return true; }
    public void add(int idx, Object obj) {
      idx = wrapCheckIndex(idx, nElems);
      if (idx == nElems) { add(obj); return; }

      final long val = Casts.longCast(obj);
      final int ne = nElems;
      final long[] d = ensureCapacity(ne+1);
      System.arraycopy(d, idx, d, idx+1, ne - idx);
      d[idx] = val;
      nElems = ne+1;
    }
    public boolean addAllReducible(Object c) {
      final int sz = size();
      if (c instanceof RandomAccess) {
	final List cl = (List) c;
	if (cl.isEmpty() ) return false;
	final int cs = cl.size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRangeReducible(sz, cl);
      } else {
	ILongArrayList.super.addAllReducible(c);
      }
      return sz != size();
    }
    public boolean addAll(int sidx, Collection <? extends Object> c) {
      sidx = wrapCheckIndex(sidx, nElems);
      if (c.isEmpty()) return false;
      final int cs = c.size();
      final int sz = size();
      final int eidx = sidx + cs;
      ensureCapacity(cs+sz);
      nElems += cs;
      System.arraycopy(data, sidx, data, eidx, sz - sidx);
      fillRangeReducible(sidx, c);
      return true;
    }
    public Object remove(int idx) {
      idx = wrapCheckIndex(idx, nElems);
      final int ne = nElems;
      final int nne = ne - 1;
      final long[] d = data;
      final long retval = d[idx];
      if (idx != nne) {
	final int copyLen = ne - idx - 1;
	System.arraycopy(d, idx+1, d, idx, copyLen);
      }
      --nElems;
      return retval;
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx, seidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      meta = m;
      return this;
    }
    public Object[] toArray() {
      return subList(0, nElems).toArray();
    }
    public long[] toLongArray() {
      return Arrays.copyOf(data, nElems);
    }
    public void fillRange(long startidx, long endidx, Object v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, endidx, v);
    }
    public void fillRangeReducible(long startidx, List v) {
      ((RangeList)subList(0, nElems)).fillRangeReducible(startidx, v);
    }
    public void addRange(final int startidx, final int endidx, final Object v) {
      final int ne = nElems;
      checkIndexRange(ne, startidx, endidx);
      final int rangeLen = endidx - startidx;
      final int newLen = ne + rangeLen;
      ensureCapacity(newLen);
      System.arraycopy(data, startidx, data, endidx, nElems - startidx);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public Object longReduction(IFn.OLO op, Object init) {
      return ((LongMutList)subList(0, nElems)).longReduction(op, init);
    }
    public IntComparator indexComparator() {
      return LongArraySubList.indexComparator(data, 0, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return LongArraySubList.indexComparator(data, 0, c);
    }
    public void sort(Comparator<? super Object> c) {
      subList(0, nElems).sort(c);
    }
    public void shuffle(Random r) {
      ((IMutList)subList(0, nElems)).shuffle(r);
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      return ((IMutList)subList(0, nElems)).binarySearch(v, c);
    }
    public int[] sortIndirect(Comparator c) {
      return ((IMutList)subList(0, nElems)).sortIndirect(c);
    }
    public void fillRangeReducible(long startidx, Object v) {
      subList(0,size()).fillRangeReducible(startidx, v);
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, ssidx, seidx, Casts.longCast(v));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, nElems);
      return Arrays.copyOfRange(data, ssidx, seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOf(data, len);
    }
    public static LongArrayList wrap(final long[] data, int nElems, IPersistentMap m) {
      if (data.length < nElems)
	throw new RuntimeException("Array len less than required");
      return new LongArrayList(data, nElems, m);
    }
    public static LongArrayList wrap(final long[] data, IPersistentMap m) {
      return new LongArrayList(data, data.length, m);
    }
  }


  public static IMutList<Object> toList(final long[] data, final int sidx, final int eidx, IPersistentMap meta) {
    return new LongArraySubList(data, sidx, eidx, meta);
  }
  public static IMutList<Object> toList(final long[] data) { return toList(data, 0, data.length, null); }

  public static float[] floatArray(int len) { return new float[len]; }
  public static class FloatArraySubList implements IDoubleArrayList {
    public final float[] data;
    public final int sidx;
    public final int dlen;
    public final IPersistentMap meta;

    public FloatArraySubList(float[] d, int s, int len, IPersistentMap _meta) {
      data = d;
      sidx = s;
      dlen = len;
      meta = _meta;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, sidx+dlen)); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, sidx + dlen); }
    public int size() { return dlen; }
    public Float get(int idx) { return data[checkIndex(idx, dlen) + sidx]; }
    public double getDouble(int idx) { return data[checkIndex(idx, dlen) + sidx];}
    public void setDouble(int idx, double v) {
      float obj = (float)v;
      idx = checkIndex(idx, dlen) + sidx;
      data[idx] = obj;
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, sidx + dlen, m);
    }
    public Object[] toArray() {
      final int sz = size();
      Object[] retval = new Object[size()];
      for (int idx = 0; idx < sz; ++idx)
	retval[idx] = data[idx+sidx];
      return retval;
    }
    public float[] toFloatArray() {
      return Arrays.copyOfRange(data, sidx, sidx + dlen);
    }
    public IntComparator indexComparator() {
      if (sidx == 0) {
	return new IntComparator() {
	  public int compare(int lidx, int ridx) {
	    return Float.compare(data[lidx], data[ridx]);
	  }
	};
      } else {
	return new IntComparator() {
	  public int compare(int lidx, int ridx) {
	    return Float.compare(data[lidx+sidx], data[ridx+sidx]);
	  }
	};
      }
    }
    public IntComparator indexComparator(Comparator c) {
      if (c == null) return indexComparator();
      if (c instanceof DoubleComparator) {
	final DoubleComparator dc = (DoubleComparator)c;
	if (sidx == 0) {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return dc.compare(data[lidx], data[ridx]);
	    }
	  };
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return dc.compare(data[lidx+sidx], data[ridx+sidx]);
	    }
	  };
	}
      } else {
	return IDoubleArrayList.super.indexComparator(c);
      }
    }
    public static FloatComparator asFloatComparator(Comparator c) {
      if (c instanceof FloatComparator)
	return (FloatComparator)c;
      else if (c instanceof DoubleComparator) {
	final DoubleComparator lc = (DoubleComparator)c;
	return new FloatComparator() {
	  public int compare(float l, float r) { return lc.compare(l,r); }
	};
      }
      return null;
    }
    @SuppressWarnings("unchecked")
    public void sort(Comparator c) {
      if(c == null) {
	FloatArrays.parallelQuickSort(data, sidx, sidx+dlen);
      } else {
	FloatComparator fc = asFloatComparator(c);
	if (fc != null)
	  FloatArrays.parallelQuickSort(data, sidx, sidx+dlen, fc);
	else
	  IDoubleArrayList.super.sort(c);
      }
    }
    public void shuffle(Random r) {
      FloatArrays.shuffle(data, sidx, sidx+dlen, r);
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final float vv = RT.floatCast(Casts.doubleCast(v));
      final FloatComparator bc = asFloatComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? FloatArrays.binarySearch(data, sidx, sidx+size(), vv) : FloatArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
      return IDoubleArrayList.super.binarySearch(v, c);
    }
    public Object doubleReduction(IFn.ODO rfn, Object init) {
      final int es = sidx + dlen;
      final float[] d = data;
      for(int ss = sidx; ss < es && !RT.isReduced(init); ++ss)
	init = rfn.invokePrim(init, d[ss]);
      return Reductions.unreduce(init);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ee = sidx + size();
	final float[] d = data;
	Reductions.serialReduction(new Reductions.IndexedDoubleAccum( startidx+sidx,
								      new IFn.OLDO() {
	    public Object invokePrim(Object acc, long idx, double v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    " is out of range: " +
						    String.valueOf(size()));
	      d[(int)idx] = (float)v;
	      return d;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, ssidx, seidx, (float)Casts.doubleCast(v));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, dlen);
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static IMutList<Object> toList(final float[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new FloatArraySubList(data, sidx, dlen, meta);
  }
  public static IMutList<Object> toList(final float[] data) { return toList(data, 0, data.length, null); }

  public static double[] doubleArray(int len) {
    return new double[len];
  }

  public static class DoubleArraySubList implements IDoubleArrayList {
    public final double[] data;
    public final int sidx;
    public final int eidx;
    public final int nElems;
    public final IPersistentMap meta;
    public DoubleArraySubList(double[] d, int _sidx, int _eidx, IPersistentMap m) {
      data = d;
      sidx = _sidx;
      eidx = _eidx;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, eidx)); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, eidx); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public double getDouble(int idx) { return data[checkIndex(idx, nElems) + sidx]; }
    public Object get(int idx) { return data[checkIndex(idx, nElems) + sidx]; }
    public Object nth(int idx) {
      if(idx < 0)
	idx += nElems;
      return data[checkIndex(idx, nElems) + sidx];
    }
    static void setDouble(final double[] d, final int sidx, final int nElems,
			int idx, final double v) {
      idx = checkIndex(idx, nElems) + sidx;
      d[idx] = v;
    }
    public void setDouble(int idx, double obj) {
      setDouble(data, sidx, nElems, idx, obj);
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, eidx, m);
    }
    public Object[] toArray() {
      final double[] d = data;
      final int ss = sidx;
      final int ne = nElems;
      Object[] retval = new Object[ne];
      for(int idx = 0; idx < ne; ++idx)
	retval[idx] = d[idx+ss];
      return retval;
    }
    public double[] toDoubleArray() {
      return Arrays.copyOfRange(data, sidx, eidx);
    }
    @SuppressWarnings("unchecked")
    public static IntComparator indexComparator(final double[] d, final int sidx,
						final Comparator comp) {
      if (comp == null) {
	if (sidx != 0) {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return Double.compare(d[lidx+sidx], d[ridx+sidx]);
	    }
	  };
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return Double.compare(d[lidx], d[ridx]);
	    }
	  };
	}
      } else {
	if (comp instanceof DoubleComparator) {
	  final DoubleComparator dc = (DoubleComparator)comp;
	  if(sidx != 0) {
	    return new IntComparator() {
	      public int compare(int lidx, int ridx) {
		return dc.compare(d[lidx+sidx], d[ridx+sidx]);
	      }
	    };
	  } else {
	    return new IntComparator() {
	      public int compare(int lidx, int ridx) {
		return dc.compare(d[lidx], d[ridx]);
	      }
	    };
	  }
	} else {
	  return new IntComparator() {
	    public int compare(int lidx, int ridx) {
	      return comp.compare(d[lidx+sidx], d[ridx+sidx]);
	    }
	  };
	}
      }
    }
    public IntComparator indexComparator() {
      return indexComparator(data, sidx, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return indexComparator(data, sidx, c);
    }
    @SuppressWarnings("unchecked")
    public static DoubleComparator toDoubleComparator(Comparator c) {
      if (c instanceof DoubleComparator)
	return (DoubleComparator) c;
      return null;
    }
    public void sort(Comparator<? super Object> c) {
      if(c == null)
	DoubleArrays.parallelQuickSort(data, sidx, eidx);
      else {
	DoubleComparator dc = toDoubleComparator(c);
	if (dc != null) {
	  DoubleArrays.parallelQuickSort(data, sidx, eidx, toDoubleComparator(c));
	} else {
	  IDoubleArrayList.super.sort(c);
	}
      }
    }
    public int[] sortIndirect(Comparator c) {
      final int sz = size();
      int[] retval = iarange(0, sz, 1);
      if(sz < 2)
	return retval;
      if(c == null)
	DoubleArrays.parallelQuickSortIndirect(retval, data, sidx, eidx);
      else
	IntArrays.parallelQuickSort(retval, indexComparator(c));
      return retval;
    }
    public void shuffle(Random r) {
      DoubleArrays.shuffle(data, sidx, eidx, r);
    }
    public static DoubleComparator asDoubleComparator(Comparator c) {
      if (c instanceof DoubleComparator)
	return (DoubleComparator)c;
      return null;
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final double vv = RT.doubleCast(Casts.doubleCast(v));
      final DoubleComparator bc = asDoubleComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? DoubleArrays.binarySearch(data, sidx, sidx+size(), vv) : DoubleArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
      return IDoubleArrayList.super.binarySearch(v, c);
    }
    public Object doubleReduction(IFn.ODO rfn, Object init) {
      final int es = eidx;
      final double[] d = data;
      for(int ss = sidx; ss < es && !RT.isReduced(init); ++ss)
	init = rfn.invokePrim(init, d[ss]);
      return Reductions.unreduce(init);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ee = sidx + size();
	final double[] d = data;
	Reductions.serialReduction(new Reductions.IndexedDoubleAccum( startidx+sidx,
								      new IFn.OLDO() {
	    public Object invokePrim(Object acc, long idx, double v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    "> length " + String.valueOf(size()));
	      d[(int)idx] = v;
	      return d;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx + ssidx, sidx + seidx, Casts.doubleCast(v));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, nElems);
      return Arrays.copyOfRange(data, sidx + ssidx, sidx + seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx + len);
    }
  }

  public static class DoubleArrayList implements IDoubleArrayList {
    double[] data;
    int nElems;
    IPersistentMap meta;
    public DoubleArrayList(double[] d, int ne, IPersistentMap meta) {
      data = d;
      nElems = ne;
    }
    public DoubleArrayList(int capacity) {
      this(new double[capacity], 0, null);
    }
    public DoubleArrayList() {
      this(4);
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public IMutList cloneList() { return new DoubleArrayList((double[])copyOf(nElems),
							     nElems, meta); }
    public ArraySection getArraySection() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public double getDouble(int idx) { return data[checkIndex(idx, nElems)]; }
    public void setDouble(int idx, double obj) {
      DoubleArraySubList.setDouble(data, 0, nElems, idx, obj);
    }
    public int capacity() { return data.length; }
    public double[] ensureCapacity(int len) {
      double[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
      }
      return d;
    }
    public void addDouble(double obj) {
      final int ne = nElems;
      final double[] d = ensureCapacity(ne+1);
      d[ne] = obj;
      nElems = ne+1;
    }
    public boolean add(Object obj) { addDouble(Casts.doubleCast(obj)); return true; }
    public void add(int idx, Object obj) {
      if (idx == nElems) { add(obj); return; }
      idx = wrapCheckIndex(idx, nElems);

      final double val = Casts.doubleCast(obj);
      final int ne = nElems;
      final double[] d = ensureCapacity(ne+1);
      System.arraycopy(d, idx, d, idx+1, ne - idx);
      d[idx] = val;
      nElems = ne+1;
    }
    public boolean addAllReducible(Object c) {
      final int sz = size();
      if (c instanceof RandomAccess) {
	final List cl = (List) c;
	if (cl.isEmpty() ) return false;
	final int cs = cl.size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRangeReducible(sz, cl);
      } else {
	IDoubleArrayList.super.addAllReducible(c);
      }
      return sz != size();
    }
    public boolean addAll(int sidx, Collection <? extends Object> c) {
      sidx = wrapCheckIndex(sidx, nElems);
      if (c.isEmpty()) return false;
      final int cs = c.size();
      final int sz = size();
      final int eidx = sidx + cs;
      ensureCapacity(cs+sz);
      nElems += cs;
      System.arraycopy(data, sidx, data, eidx, sz - sidx);
      if (c instanceof List) {
	//Hit fastpath
	fillRangeReducible(sidx, (List)c);
      } else {
	int idx = sidx;
	for(Object o: c) {
	  set(idx, o);
	  ++idx;
	}
      }
      return true;
    }
    public Object remove(int idx) {
      idx = wrapCheckIndex(idx, nElems);
      final int ne = nElems;
      final int nne = ne - 1;
      final double[] d = data;
      final double retval = d[idx];
      if (idx != nne) {
	final int copyLen = ne - idx - 1;
	System.arraycopy(d, idx+1, d, idx, copyLen);
      }
      --nElems;
      return retval;
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx, seidx, meta());
    }
    public void fillRangeReducible(long startidx, List v) {
      ((IMutList)subList(0, nElems)).fillRangeReducible(startidx, v);
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      meta = m;
      return this;
    }
    public Object[] toArray() {
      return subList(0, nElems).toArray();
    }
    public double[] toDoubleArray() {
      return Arrays.copyOf(data, nElems);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(size(), startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) {
      return ((IReduceInit)subList(0, nElems)).reduce(fn,init);
    }
    public Object doubleReduction(IFn.ODO fn, Object init) {
      return ((DoubleMutList)subList(0, nElems)).doubleReduction(fn, init);
    }
    public IntComparator indexComparator() {
      return DoubleArraySubList.indexComparator(data, 0, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return DoubleArraySubList.indexComparator(data, 0, c);
    }
    public void sort(Comparator<? super Object> c) {
      subList(0, nElems).sort(c);
    }
    public void shuffle(Random r) {
      ((IMutList)subList(0, nElems)).shuffle(r);
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      return ((IMutList)subList(0, nElems)).binarySearch(v, c);
    }
    public int[] sortIndirect(Comparator c) {
      return ((IMutList)subList(0, nElems)).sortIndirect(c);
    }
    public void doubleForEach(DoubleConsumer c) {
      final int es = nElems;
      final double[] d = data;
      for(int ss = 0; ss < es; ++ss)
	c.accept(d[ss]);
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, ssidx, seidx, Casts.doubleCast(v));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, nElems);
      return Arrays.copyOfRange(data, ssidx, seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOf(data, len);
    }
    public static DoubleArrayList wrap(final double[] data, int nElems, IPersistentMap m) {
      if (data.length < nElems)
	throw new RuntimeException("Array len less than required");
      return new DoubleArrayList(data, nElems, m);
    }
    public static DoubleArrayList wrap(final double[] data, IPersistentMap m) {
      return new DoubleArrayList(data, data.length, m);
    }
  }

  public static IMutList<Object> toList(final double[] data, final int sidx, final int eidx, IPersistentMap meta) {
    return new DoubleArraySubList(data, sidx, eidx, meta);
  }
  public static IMutList<Object> toList(final double[] data) { return toList(data, 0, data.length, null); }

  public static class CharArraySubList implements ILongArrayList {
    public final char[] data;
    public final int sidx;
    public final int dlen;
    public final IPersistentMap meta;
    public CharArraySubList(char[] d, int s, int len, IPersistentMap m) {
      data = d;
      sidx = s;
      dlen = len;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, sidx + dlen); }
    public int size() { return dlen; }
    public Character set(int idx, Object obj) { final Character c = get(idx); setLong(idx, Casts.longCast(obj)); return c; }
    public Character get(int idx) { return data[checkIndex(idx, dlen) + sidx]; }
    public long getLong(int idx) { return data[checkIndex(idx, dlen) + sidx]; }
    public void setLong(int idx, long obj) {
      char v = Casts.charCast(obj);
      idx = checkIndex(idx, dlen) + sidx;
      data[idx] = v;
    }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, sidx + dlen, m);
    }
    public Object[] toArray() {
      final int sz = size();
      Object[] retval = new Object[size()];
      for (int idx = 0; idx < sz; ++idx)
	retval[idx] = data[idx+sidx];
      return retval;
    }
    public static CharComparator asCharComparator(Comparator c) {
      if (c instanceof CharComparator)
	return (CharComparator)c;
      else if (c instanceof LongComparator) {
	final LongComparator lc = (LongComparator)c;
	return new CharComparator() {
	  public int compare(char l, char r) { return lc.compare(l,r); }
	};
      }
      return null;
    }
    @SuppressWarnings("unchecked")
    public void sort(Comparator c) {
      if(c==null)
	CharArrays.parallelQuickSort(data, sidx, sidx+dlen);
      else {
	CharComparator cc = asCharComparator(c);
	if( cc != null)
	  CharArrays.parallelQuickSort(data, sidx, sidx+dlen, cc);
	else
	  ILongArrayList.super.sort(c);
      }
    }
    public void shuffle(Random r) {
      CharArrays.shuffle(data, sidx, sidx+dlen, r);
    }
    @SuppressWarnings("unchecked")
    public int binarySearch(Object v, Comparator c) {
      final char vv = RT.charCast(Casts.longCast(v));
      final CharComparator bc = asCharComparator(c);
      if(c == null || bc != null)
	return fixSubArrayBinarySearch(sidx, size(),
				       bc == null ? CharArrays.binarySearch(data, sidx, sidx+size(), vv) : CharArrays.binarySearch(data, sidx, sidx+size(), vv, bc));
      return ILongArrayList.super.binarySearch(v, c);
    }
    public Object reduce(IFn rfn, Object init) {
      final int sz = size();
      for (int idx = 0; idx < sz && !RT.isReduced(init); ++idx)
	init = rfn.invoke(init, data[idx+sidx]);
      return Reductions.unreduce(init);
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ss = (int)startidx + sidx;
	final int ee = sidx + size();
	Reductions.serialReduction(new Reductions.IndexedAccum( startidx+sidx,
								new IFn.OLOO() {
	    public Object invokePrim(Object acc, long idx, Object v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    " is out of range: " +
						    String.valueOf(size()));
	      data[(int)idx] = Casts.charCast(v);
	      return data;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, ssidx+sidx, seidx+sidx, RT.charCast(Casts.longCast(v)));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndex(ssidx, size());
      return Arrays.copyOfRange(data, ssidx+sidx, seidx+sidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static IMutList<Object> toList(final char[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new CharArraySubList(data, sidx, dlen, meta);
  }
  public static IMutList<Object> toList(final char[] data) { return toList(data, 0, data.length, null); }


  public static class BooleanArraySubList implements IArrayList {
    public final boolean[] data;
    public final int sidx;
    public final int dlen;
    public final IPersistentMap meta;

    public BooleanArraySubList(boolean[] d, int s, int len, IPersistentMap m) {
      data = d;
      sidx = s;
      dlen = len;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArraySection() { return new ArraySection(data, sidx, sidx + dlen); }
    public int size() { return dlen; }
    public boolean getBoolean(int idx) { return data[checkIndex(idx, dlen)+sidx]; }
    public void setBoolean(int idx, boolean obj) {
      data[checkIndex(idx, dlen)+sidx] = obj;
    }
    public long getLong(int idx) { return Casts.longCast(data[checkIndex(idx, dlen)+sidx]); }
    public void setLong(int idx, long obj) {
      data[checkIndex(idx, dlen)+sidx] = Casts.booleanCast(obj);
    }
    public Object get(int idx) { return getBoolean(idx); }
    public Object set(int idx, Object v) {
      final boolean retval = getBoolean(idx);
      setBoolean(idx, Casts.booleanCast(v));
      return retval;
    }
    public IMutList cloneList() { return (IMutList)toList(Arrays.copyOfRange(data, sidx, sidx+dlen)); }
    public IMutList<Object> subList(int ssidx, int seidx) {
      ChunkedList.sublistCheck(ssidx, seidx, size());
      return toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, sidx + dlen, m);
    }
    public Object[] toArray() {
      final int sz = size();
      Object[] retval = new Object[size()];
      for (int idx = 0; idx < sz; ++idx)
	retval[idx] = data[idx+sidx];
      return retval;
    }
    public void fillRangeReducible(long startidx, Object v) {
      final ArraySection as = getArraySection();
      if (!fillRangeArrayCopy(as.array, as.sidx, as.eidx, startidx, v)) {
	final int ss = (int)startidx + sidx;
	final int ee = sidx + size();
	Reductions.serialReduction(new Reductions.IndexedAccum( startidx+sidx,
								new IFn.OLOO() {
	    public Object invokePrim(Object acc, long idx, Object v) {
	      if(idx >= ee)
		throw new IndexOutOfBoundsException("Index " + String.valueOf(idx - sidx) +
						    " is out of range: " +
						    String.valueOf(size()));
	      data[(int)idx] = Casts.booleanCast(v);
	      return data;
	    }}), data, v);
      }
    }
    public void fill(int ssidx, int seidx, Object v) {
      checkIndexRange(size(), ssidx, seidx);
      Arrays.fill(data, sidx+ssidx, sidx+seidx, Casts.booleanCast(v));
    }
    public Object copyOfRange(int ssidx, int seidx) {
      checkIndexRange(size(), ssidx, seidx);
      return Arrays.copyOfRange(data, sidx+ssidx, sidx+seidx);
    }
    public Object copyOf(int len) {
      return Arrays.copyOfRange(data, sidx, sidx+len);
    }
  }

  public static IMutList<Object> toList(final boolean[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new BooleanArraySubList(data, sidx, dlen, meta);
  }
  public static IMutList<Object> toList(final boolean[] data) { return toList(data, 0, data.length, null); }


  public static IMutList<Object> toList(Object obj, int sidx, int eidx, IPersistentMap meta) {
    if (obj == null) return null;
    Class cls = obj.getClass();
    if(!cls.isArray())
      throw new RuntimeException("Object is not an array: " + String.valueOf(obj));
    if(obj instanceof Object[])
      return toList((Object[])obj, sidx, eidx, meta);
    else if (cls == long[].class)
      return toList((long[])obj, sidx, eidx, meta);
    else if (cls == double[].class)
      return toList((double[])obj, sidx, eidx, meta);
    else if (cls == byte[].class)
      return toList((byte[])obj, sidx, eidx, meta);
    else if (cls == short[].class)
      return toList((short[])obj, sidx, eidx, meta);
    else if (cls == int[].class)
      return toList((int[])obj, sidx, eidx, meta);
    else if (cls == float[].class)
      return toList((float[])obj, sidx, eidx, meta);
    else if (cls == char[].class)
      return toList((char[])obj, sidx, eidx, meta);
    else if (cls == boolean[].class)
      return toList((boolean[])obj, sidx, eidx, meta);
    else
      throw new RuntimeException("Invalid array type.");
  }

  public static IMutList<Object> toList(Object obj) {
    if (obj == null) return null;
    Class cls = obj.getClass();
    if(!cls.isArray())
      throw new RuntimeException("Object is not an array: " + String.valueOf(obj));
    return toList(obj, 0, Array.getLength(obj), null);
  }

  @SuppressWarnings("unchecked")
  public static IMutList<Object> toList(ArraySection data) {
    if(data == null) return null;
    if(data instanceof IMutList)
      return (IMutList)data;
    return toList(data.array, data.sidx, data.eidx, null);
  }

  public static int[] iarange(int start, int end, int step) {
    final int len = (end - start)/step;
    if (len < 0 )
      throw new RuntimeException("Invalid range - start: " + String.valueOf(start) +
				 " end: " + String.valueOf(end) +
				 " step: " + String.valueOf(step));
    final int[] retval = new int[len];
    for(int idx = 0; idx < len; ++idx) {
      retval[idx] = start + idx * step;
    }
    return retval;
  }
  public static long[] larange(long start, long end, long step) {
    final int len = RT.intCast((end - start)/step);
    if (len < 0 )
      throw new RuntimeException("Invalid range.");
    final long[] retval = new long[len];
    for(int idx = 0; idx < len; ++idx) {
      retval[idx] = start + idx * step;
    }
    return retval;
  }
  public static double[] darange(double start, double end, double step) {
    final int len = RT.intCast((end - start)/step);
    if (len < 0 )
      throw new RuntimeException("Invalid range.");
    final double[] retval = new double[len];
    for(int idx = 0; idx < len; ++idx) {
      retval[idx] = start + idx * step;
    }
    return retval;
  }
}

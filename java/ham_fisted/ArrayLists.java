package ham_fisted;


import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.Collection;
import java.util.Random;
import java.util.RandomAccess;
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
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;


public class ArrayLists {
  static int wrapCheckIndex(int idx, int dlen) {
    if(idx < 0)
      idx += dlen;
    if(idx < 0 || idx >= dlen)
      throw new RuntimeException("Index out of range: " + String.valueOf(idx));
    return idx;
  }
  static void checkIndexRange(int sidx, int dlen, int ssidx, int seidx) {
    if (ssidx <0 || ssidx >= dlen)
      throw new RuntimeException("Index out of range: " + String.valueOf(ssidx));
    if(seidx < ssidx || seidx > dlen)
      throw new RuntimeException("Index out of range: " + String.valueOf(seidx));
  }
  @SuppressWarnings("unchecked")
  public static void fillRangeDefault(final List l, int startidx, final int endidx, final Object v) {
    for(; startidx < endidx; ++startidx)
      l.set(startidx, v);
  }
  @SuppressWarnings("unchecked")
  public static void fillRangeDefault(final List l, int startidx, List v) {
    final int endidx = startidx + v.size();
    int idx = 0;
    for(; startidx < endidx; ++startidx, ++idx)
      l.set(idx+startidx, v.get(idx));
  }
  public static void removeRangeDefault(final List l, final int startidx, final int endidx) {
    final int ne = endidx - startidx;
    for(int idx = 0; idx < ne; ++idx)
      l.remove(startidx);
  }
  @SuppressWarnings("unchecked")
  public static void addRangeDefault(final List l, final int startidx, final int endidx, Object v) {
    final int ne = endidx - startidx;
    for(int idx = 0; idx < ne; ++idx)
      l.add(idx+startidx, v);
  }
  public static void fill(Object[] data, int sidx, final int eidx, IntFunction f) {
    for(; sidx < eidx; ++sidx)
      data[sidx] = f.apply(sidx);
  }
  //Kahan compesated summation
  public static class SummationConsumer implements DoubleConsumer, Consumer {
    public double d0 = 0;
    //High order summation bits
    public double d1 = 0;
    public double simpleSum = 0;
    public long nElems = 0;
    public SummationConsumer() {}
    public void sumWithCompensation(double value) {
      double tmp = value - d1;
      double sum = d0;
      double velvel = sum + tmp; // Little wolf of rounding error
      d1 =  (velvel - sum) - tmp;
      d0 = velvel;
    }
    public void accept(double d) {
      sumWithCompensation(d);
      simpleSum += d;
      nElems++;
    }
    public void accept(Object o) { accept(Casts.doubleCast(o)); }
    public double value() {
      // Better error bounds to add both terms as the final sum
      double tmp = d0 + d1;
      if (Double.isNaN(tmp) && Double.isInfinite(simpleSum))
	return simpleSum;
      else
	return tmp;
    }
  }

  public static class ReductionConsumer implements Consumer {
    Object init;
    IFn fn;
    public ReductionConsumer(IFn _fn, Object _init) {
      fn = _fn;
      init = _init;
    }
    public void accept(Object obj) {
      if(!RT.isReduced(init))
	init = fn.invoke(init, obj);
    }
    public Object value() {
      if (RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
  }

  public static class ArraySection {
    public final Object array;
    public final int sidx;
    public final int eidx;
    public ArraySection(Object ary, int _sidx, int _eidx) {
      array = ary;
      sidx = _sidx;
      eidx = _eidx;
    }
  }
  public interface ArrayOwner {
    public ArraySection getArray();
  }
  public interface IArrayList extends IMutList<Object>, ArrayOwner, TypedList {
    default Class containedType() { return getArray().array.getClass().getComponentType(); }
  }
  public interface ILongArrayList extends LongMutList, ArrayOwner, TypedList {
    default Class containedType() { return getArray().array.getClass().getComponentType(); }
  }
  public interface IDoubleArrayList extends DoubleMutList, ArrayOwner, TypedList {
    default Class containedType() { return getArray().array.getClass().getComponentType(); }
  }

  public static class ObjectArraySubList extends ArraySection implements IArrayList {
    public final Object[] data;
    public final int nElems;
    public final IPersistentMap meta;
    public ObjectArraySubList(Object[] d, int sidx, int eidx, IPersistentMap m) {
      super(d, sidx, eidx);
      data = d;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArray() { return this; }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    public Object set(int idx, Object obj) {
      idx = wrapCheckIndex(idx, nElems) + sidx;
      final Object retval = data[idx];
      data[idx] = obj;
      return retval;
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(sidx, eidx, ssidx, seidx);
      return toList(data, ssidx + sidx, seidx + sidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      return (IObj)toList(data, sidx, eidx, m);
    }
    public Object[] toArray() {
      return Arrays.copyOfRange(data, sidx, eidx);
    }
    public void fillRange(int startidx, int endidx, Object v) {
      checkIndexRange(sidx, eidx, startidx, endidx);
      Arrays.fill(data, startidx+sidx, endidx+sidx, v);
    }
    public void fillRange(int startidx, List v) {
      final int vs = v.size();
      final int endidx = startidx + vs;
      checkIndexRange(sidx, eidx, startidx, endidx);
      if(v instanceof ArrayOwner) {
	ArraySection src = ((ArrayOwner)v).getArray();
	if( src.array instanceof Object[]) {
	  System.arraycopy((Object[])src.array, src.sidx, data, sidx, vs);
	  return;
	}
      }
      final int sd = sidx;
      final Object[] d = data;
      for(int idx = 0; idx < vs; ++idx)
	d[idx+sidx] = v.get(idx);
    }
    public Object reduce(IFn fn) {
      if(nElems ==0) return fn.invoke();
      final int ne = nElems;
      final int ss = sidx;
      final Object[] d = data;
      Object init = d[ss];
      for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public Object reduce(IFn fn, Object init) {
      if(nElems ==0) return init;
      final int ne = nElems;
      final int ss = sidx;
      final Object[] d = data;
      for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public void sort(Comparator<? super Object> c) {
      if(c == null)
	Arrays.sort(data, sidx, eidx);
      else
	Arrays.sort(data, sidx, eidx, c);
    }
    @SuppressWarnings("unchecked")
    public List immutSort(Comparator c) {
      List retval = toList(Arrays.copyOfRange(data, sidx, eidx), 0, (eidx - sidx), meta());
      retval.sort(c);
      return retval;
    }

    public List immutShuffle(Random r) {
      Object[] retval = toArray();
      ObjectArrays.shuffle(retval, r);
      return toList(retval);
    }
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
    public ArraySection getArray() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public Object set(int idx, Object obj) {
      idx = wrapCheckIndex(idx, nElems);
      final Object retval = data[idx];
      data[idx] = obj;
      return retval;
    }
    public int capacity() { return data.length; }
    Object[] ensureCapacity(int len) {
      Object[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
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
      idx = wrapCheckIndex(idx, nElems);
      if (idx == nElems) { add(obj); return; }

      final int ne = nElems;
      final Object [] d = ensureCapacity(ne+1);
      System.arraycopy(d, idx, d, idx+1, ne - idx);
      d[idx] = obj;
      nElems = ne+1;
    }
    public boolean addAll(Collection <? extends Object> c) {
      if (c.isEmpty()) return false;
      if (c instanceof RandomAccess) {
	final int cs = c.size();
	final int sz = size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRange(sz, (List)c);
      } else {
	for(Object o: c) {
	  add(o);
	}
      }
      return true;
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
	fillRange(sidx, (List)c);
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
      final Object[] d = data;
      final Object retval = d[idx];
      if (idx == nne) {
	d[idx] = null;
      } else {
	final int copyLen = ne - idx - 1;
	System.arraycopy(d, idx+1, d, idx, copyLen);
	//Release references to later elements.
	d[nne] = null;
      }
      --nElems;
      return retval;
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(0, nElems, ssidx, seidx);
      return toList(data, ssidx, seidx, meta());
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) {
      meta = m;
      return this;
    }
    public Object[] toArray() {
      return Arrays.copyOf(data, nElems);
    }
    public void fillRange(int startidx, int endidx, Object v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, endidx, v);
    }
    public void fillRange(int startidx, List v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, v);
    }
    public void addRange(final int startidx, final int endidx, final Object v) {
      final int ne = nElems;
      checkIndexRange(0, ne, startidx, endidx);
      final int rangeLen = endidx - startidx;
      final int newLen = ne + rangeLen;
      ensureCapacity(newLen);
      System.arraycopy(data, startidx, data, endidx, nElems - startidx);
      Arrays.fill(data, startidx, endidx, v);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(0, nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      Arrays.fill(data, endidx, nElems, null);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public void sort(Comparator<? super Object> c) {
      subList(0, nElems).sort(c);
    }
    public List immutSort(Comparator c) {
      return ((ImmutSort)subList(0, nElems)).immutSort(c);
    }
    public List immutShuffle(Random r) {
      return ((IMutList)subList(0, nElems)).immutShuffle(r);
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

  public static List<Object> toList(final Object[] data, final int sidx, final int eidx, final IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ObjectArraySubList(data, sidx, eidx, meta);
  }
  public static List<Object> toList(final Object[] data) { return toList(data, 0, data.length, null); }

  public static List<Object> toList(final byte[] data, final int sidx, final int eidx, final IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ILongArrayList() {
      public String toString() { return Transformables.sequenceToString(this); }
      public boolean equals(Object other) {
	return equiv(other);
      }
      public int hashCode() { return hasheq(); }
      public ArraySection getArray() { return new ArraySection(data, sidx, eidx); }
      public Class containedType() { return data.getClass().getComponentType(); }
      public int size() { return dlen; }
      public Byte get(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public long getLong(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public Byte set(int idx, Object oobj) {
	return (byte)setLong(idx, Casts.longCast(oobj));
      }
      public IntComparator indexComparator() {
	return new IntComparator() {
	  public int compare(int lidx, int ridx) {
	    return Byte.compare(data[lidx+sidx], data[ridx+sidx]);
	  }
	};
      }
      public long setLong(int idx, long oobj) {
	byte obj = RT.byteCast(oobj);
	idx = wrapCheckIndex(idx, dlen) + sidx;
	final byte retval = data[idx];
	data[idx] = obj;
	return retval;
      }
      public List<Object> subList(int ssidx, int seidx) {
	checkIndexRange(sidx, dlen, ssidx, seidx);
	return toList(data, ssidx + sidx, seidx + sidx, meta());
      }
      public IPersistentMap meta() { return meta; }
      public IObj withMeta(IPersistentMap m) {
	return (IObj)toList(data, sidx, eidx, m);
      }
      public Object[] toArray() {
	final int sz = size();
	Object[] retval = new Object[size()];
	for (int idx = 0; idx < sz; ++idx)
	  retval[idx] = data[idx+sidx];
	return retval;
      }
      public Object reduce(IFn fn) {
	if(dlen==0) return fn.invoke();
	final int ne = dlen;
	final int ss = sidx;
	final byte[] d = data;
	Object init = d[ss];
	for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public Object reduce(IFn fn, Object init) {
	if(dlen ==0) return init;
	final int ne = dlen;
	final int ss = sidx;
	final byte[] d = data;
	for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public long longReduction(LongBinaryOperator op, long init) {
	final int sz = dlen;
	final int ss = sidx;
	for (int idx = 0; idx < sz; ++idx)
	  init = op.applyAsLong(init, data[idx+ss]);
	return init;
      }
      public List immutShuffle(Random r) {
	final int sz = size();
	final int[] perm = IntArrays.shuffle(ArrayLists.iarange(0, sz, 1), r);
	byte[] bdata = new byte[sz];
	for(int idx = 0; idx < sz; ++idx)
	  bdata[idx] = data[perm[idx]];
	return toList(bdata);
      }
    };
  }
  public static List<Object> toList(final byte[] data) { return toList(data, 0, data.length, null); }

  public static List<Object> toList(final short[] data, final int sidx, final int eidx, final IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ILongArrayList() {
      public String toString() { return Transformables.sequenceToString(this); }
      public boolean equals(Object other) {
	return equiv(other);
      }
      public int hashCode() { return hasheq(); }
      public ArraySection getArray() { return new ArraySection(data, sidx, eidx); }
      public Class containedType() { return data.getClass().getComponentType(); }
      public int size() { return dlen; }
      public Short get(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public long getLong(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public Short set(int idx, Object oobj) {
	return (short)setLong(idx, Casts.longCast(oobj));
      }
      public long setLong(int idx, long oobj) {
	short obj = RT.shortCast(Casts.longCast(oobj));
	idx = wrapCheckIndex(idx, dlen) + sidx;
	final short retval = data[idx];
	data[idx] = obj;
	return retval;
      }
      public IntComparator indexComparator() {
	return new IntComparator() {
	  public int compare(int lidx, int ridx) {
	    return Short.compare(data[lidx+sidx], data[ridx+sidx]);
	  }
	};
      }
      public List<Object> subList(int ssidx, int seidx) {
	checkIndexRange(sidx, dlen, ssidx, seidx);
	return toList(data, ssidx + sidx, seidx + sidx, meta());
      }
      public IPersistentMap meta() { return meta; }
      public IObj withMeta(IPersistentMap m) {
	return (IObj)toList(data, sidx, eidx, m);
      }
      public Object[] toArray() {
	final int sz = size();
	Object[] retval = new Object[size()];
	for (int idx = 0; idx < sz; ++idx)
	  retval[idx] = data[idx+sidx];
	return retval;
      }
      public List immutSort() {
	short[] retval = Arrays.copyOfRange(data, sidx, eidx);
	Arrays.sort(retval);
	return toList(retval, 0, retval.length, meta);
      }
      public Object reduce(IFn fn) {
	if(dlen ==0) return fn.invoke();
	final int ne = dlen;
	final int ss = sidx;
	final short[] d = data;
	Object init = d[ss];
	for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public Object reduce(IFn fn, Object init) {
	if(dlen ==0) return init;
	final int ne = dlen;
	final int ss = sidx;
	final short[] d = data;
	for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public long longReduction(LongBinaryOperator op, long init) {
	final int sz = dlen;
	final int ss = sidx;
	for (int idx = 0; idx < sz; ++idx)
	  init = op.applyAsLong(init, data[idx+ss]);
	return init;
      }
      public List immutShuffle(Random r) {
	final int sz = size();
	final int[] perm = IntArrays.shuffle(ArrayLists.iarange(0, sz, 1), r);
	final short[] bdata = new short[sz];
	for(int idx = 0; idx < sz; ++idx)
	  bdata[idx] = data[perm[idx]];
	return toList(bdata);
      }
    };
  }
  public static List<Object> toList(final short[] data) { return toList(data, 0, data.length, null); }


  public static class IntArraySubList extends ArraySection implements ILongArrayList {
    public final int[] data;
    public final int nElems;
    public final IPersistentMap meta;
    public IntArraySubList(int[] d, int sidx, int eidx, IPersistentMap m) {
      super(d, sidx, eidx);
      data = d;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArray() { return this; }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    public long getLong(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    static int setLong(final int[] d, final int sidx, final int nElems,
		       int idx, final long obj) {
      int v = RT.intCast(obj);
      idx = wrapCheckIndex(idx, nElems) + sidx;
      final int retval = d[idx];
      d[idx] = v;
      return retval;
    }
    public Object set(int idx, Object obj) {
      return setLong(data, sidx, nElems, idx, Casts.longCast(obj));
    }
    public long setLong(int idx, long obj) {
      return setLong(data, sidx, nElems, idx, obj);
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(sidx, eidx, ssidx, seidx);
      return toList(data, ssidx + sidx, seidx + sidx, meta());
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
    public void fillRange(int startidx, int endidx, Object v) {
      checkIndexRange(sidx, eidx, startidx, endidx);
      Arrays.fill(data, startidx+sidx, endidx+sidx, RT.intCast(Casts.longCast(v)));
    }
    public void fillRange(int startidx, List v) {
      final int vs = v.size();
      final int endidx = startidx + vs;
      checkIndexRange(sidx, eidx, startidx, endidx);
      if(v instanceof ArrayOwner) {
	ArraySection src = ((ArrayOwner)v).getArray();
	if( src.array instanceof int[]) {
	  System.arraycopy((int[])src.array, src.sidx, data, sidx, vs);
	  return;
	}
      }
      final int sd = sidx;
      final int[] d = data;
      if (v instanceof IMutList) {
	IMutList vv = (IMutList)v;
	for(int idx = 0; idx < vs; ++idx)
	  d[idx+sidx] = RT.intCast(vv.getLong(idx));
      } else {
	for(int idx = 0; idx < vs; ++idx)
	  d[idx+sidx] = RT.intCast(Casts.longCast(v.get(idx)));
      }
    }
    public Object reduce(IFn fn) {
      if(nElems ==0) return fn.invoke();
      final int ne = nElems;
      final int ss = sidx;
      final int[] d = data;
      Object init = d[ss];
      for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public Object reduce(IFn fn, Object init) {
      if(nElems ==0) return init;
      final int ne = nElems;
      final int ss = sidx;
      final int[] d = data;
      for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public long longReduction(LongBinaryOperator op, long init) {
      final int sz = size();
      for (int idx = 0; idx < sz; ++idx)
	init = op.applyAsLong(init, data[idx+sidx]);
      return init;
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
    @SuppressWarnings("unchecked")
    public List immutSort(Comparator c) {
      List retval = toList(Arrays.copyOfRange(data, sidx, eidx), 0, (eidx - sidx), meta());
      retval.sort(c);
      return retval;
    }
    public List immutShuffle(Random r) {
      int[] retval = toIntArray();
      IntArrays.shuffle(retval, r);
      return toList(retval);
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
    public ArraySection getArray() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public long getLong(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public Object set(int idx, Object obj) {
      return IntArraySubList.setLong(data, 0, nElems, idx, Casts.longCast(obj));
    }
    public long setLong(int idx, long obj) {
      return IntArraySubList.setLong(data, 0, nElems, idx, obj);
    }
    public int capacity() { return data.length; }
    int[] ensureCapacity(int len) {
      int[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
      }
      return d;
    }
    public boolean addLong(long obj) {
      int val = RT.intCast(obj);
      final int ne = nElems;
      final int[] d = ensureCapacity(ne+1);
      d[ne] = val;
      nElems = ne+1;
      return true;
    }
    public boolean add(Object obj) { return addLong(Casts.longCast(obj)); }
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
    public boolean addAll(Collection <? extends Object> c) {
      if (c.isEmpty()) return false;
      if (c instanceof RandomAccess) {
	//Hit fastpath
	final int cs = c.size();
	final int sz = size();
	ensureCapacity(cs+sz);
	nElems += cs;
	fillRange(sz, (List)c);
      } else {
	for(Object o: c)
	  add(o);
      }
      return true;
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
	fillRange(sidx, (List)c);
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
      final int[] d = data;
      final int retval = d[idx];
      if (idx != nne) {
	final int copyLen = ne - idx - 1;
	System.arraycopy(d, idx+1, d, idx, copyLen);
      }
      --nElems;
      return retval;
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(0, nElems, ssidx, seidx);
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
    public void fillRange(int startidx, int endidx, Object v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, endidx, v);
    }
    public void fillRange(int startidx, List v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, v);
    }
    public void addRange(final int startidx, final int endidx, final Object v) {
      final int ne = nElems;
      checkIndexRange(0, ne, startidx, endidx);
      final int rangeLen = endidx - startidx;
      final int newLen = ne + rangeLen;
      ensureCapacity(newLen);
      System.arraycopy(data, startidx, data, endidx, nElems - startidx);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(0, nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public long longReduction(LongBinaryOperator op, long init) {
      return ((IMutList)subList(0, nElems)).longReduction(op, init);
    }
    public void sort(Comparator<? super Object> c) {
      subList(0, nElems).sort(c);
    }
    public int[] sortIndirect(Comparator c) {
      return ((IMutList)subList(0, nElems)).sortIndirect(c);
    }
    public List immutSort(Comparator c) {
      return ((ImmutSort)subList(0, nElems)).immutSort(c);
    }
    public IntComparator indexComparator() {
      return IntArraySubList.indexComparator(data, 0, null);
    }
    public IntComparator indexComparator(Comparator c) {
      return IntArraySubList.indexComparator(data, 0, c);
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

  public static List<Object> toList(final int[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new IntArraySubList(data, sidx, eidx, meta);
  }
  public static List<Object> toList(final int[] data) { return toList(data, 0, data.length, null); }


  public static class LongArraySubList extends ArraySection implements ILongArrayList {
    public final long[] data;
    public final int nElems;
    public final IPersistentMap meta;
    public LongArraySubList(long[] d, int sidx, int eidx, IPersistentMap m) {
      super(d, sidx, eidx);
      data = d;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArray() { return this; }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    public long getLong(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    static long setLong(final long[] d, final int sidx, final int nElems,
			int idx, final long v) {
      idx = wrapCheckIndex(idx, nElems) + sidx;
      final long retval = d[idx];
      d[idx] = v;
      return retval;
    }
    public Object set(int idx, Object obj) {
      return setLong(data, sidx, nElems, idx, Casts.longCast(obj));
    }
    public long setLong(int idx, long obj) {
      return setLong(data, sidx, nElems, idx, obj);
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(sidx, eidx, ssidx, seidx);
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
    public void fillRange(int startidx, int endidx, Object v) {
      checkIndexRange(sidx, eidx, startidx, endidx);
      Arrays.fill(data, startidx+sidx, endidx+sidx, Casts.longCast(v));
    }
    public void fillRange(int startidx, List v) {
      final int vs = v.size();
      final int endidx = startidx + vs;
      checkIndexRange(sidx, eidx, startidx, endidx);
      if(v instanceof ArrayOwner) {
	ArraySection src = ((ArrayOwner)v).getArray();
	if( src.array instanceof long[]) {
	  System.arraycopy((long[])src.array, src.sidx, data, sidx, vs);
	  return;
	}
      }
      final int sd = sidx;
      final long[] d = data;
      if (v instanceof IMutList) {
	IMutList vv = (IMutList)v;
	for(int idx = 0; idx < vs; ++idx)
	  d[idx+sidx] = vv.getLong(idx);
      } else {
	for(int idx = 0; idx < vs; ++idx)
	  d[idx+sidx] = Casts.longCast(v.get(idx));
      }
    }
    public Object reduce(IFn fn) {
      if(nElems ==0) return fn.invoke();
      final int ne = nElems;
      final int ss = sidx;
      final long[] d = data;
      Object init = d[ss];
      for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public Object reduce(IFn fn, Object init) {
      if(nElems ==0) return init;
      final int ne = nElems;
      final int ss = sidx;
      final long[] d = data;
      for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public long longReduction(LongBinaryOperator op, long init) {
      final int sz = size();
      for (int idx = 0; idx < sz; ++idx)
	init = op.applyAsLong(init, data[idx+sidx]);
      return init;
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
    @SuppressWarnings("unchecked")
    public List immutSort(Comparator c) {
      List retval = toList(Arrays.copyOfRange(data, sidx, eidx), 0, (eidx - sidx), meta());
      retval.sort(c);
      return retval;
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
    public List immutShuffle(Random r) {
      long[] data = toLongArray();
      LongArrays.shuffle(data, r);
      return toList(data);
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
    public ArraySection getArray() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public long getLong(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public Object set(int idx, Object obj) {
      return LongArraySubList.setLong(data, 0, nElems, idx, Casts.longCast(obj));
    }
    public long setLong(int idx, long obj) {
      return LongArraySubList.setLong(data, 0, nElems, idx, obj);
    }
    public int capacity() { return data.length; }
    long[] ensureCapacity(int len) {
      long[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
      }
      return d;
    }
    public boolean addLong(long obj) {
      int val = RT.intCast(obj);
      final int ne = nElems;
      final long[] d = ensureCapacity(ne+1);
      d[ne] = val;
      nElems = ne+1;
      return true;
    }
    public boolean add(Object obj) { return addLong(Casts.longCast(obj)); }
    public void add(int idx, Object obj) {
      idx = wrapCheckIndex(idx, nElems);
      if (idx == nElems) { add(obj); return; }

      final int val = RT.intCast(Casts.longCast(obj));
      final int ne = nElems;
      final long[] d = ensureCapacity(ne+1);
      System.arraycopy(d, idx, d, idx+1, ne - idx);
      d[idx] = val;
      nElems = ne+1;
    }
    public boolean addAll(Collection <? extends Object> c) {
      if (c.isEmpty()) return false;
      if (c instanceof RandomAccess) {
	final int cs = c.size();
	final int sz = size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRange(sz, (List)c);
      } else {
	for(Object o: c) {
	  add(o);
	}
      }
      return true;
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
	fillRange(sidx, (List)c);
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
      final long[] d = data;
      final long retval = d[idx];
      if (idx != nne) {
	final int copyLen = ne - idx - 1;
	System.arraycopy(d, idx+1, d, idx, copyLen);
      }
      --nElems;
      return retval;
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(0, nElems, ssidx, seidx);
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
    public void fillRange(int startidx, int endidx, Object v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, endidx, v);
    }
    public void fillRange(int startidx, List v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, v);
    }
    public void addRange(final int startidx, final int endidx, final Object v) {
      final int ne = nElems;
      checkIndexRange(0, ne, startidx, endidx);
      final int rangeLen = endidx - startidx;
      final int newLen = ne + rangeLen;
      ensureCapacity(newLen);
      System.arraycopy(data, startidx, data, endidx, nElems - startidx);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(0, nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public long longReduction(LongBinaryOperator op, long init) {
      return ((IMutList)subList(0, nElems)).longReduction(op, init);
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
    public List immutSort(Comparator c) {
      return ((ImmutSort)subList(0, nElems)).immutSort(c);
    }
    public int[] sortIndirect(Comparator c) {
      return ((IMutList)subList(0, nElems)).sortIndirect(c);
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


  public static List<Object> toList(final long[] data, final int sidx, final int eidx, IPersistentMap meta) {
    return new LongArraySubList(data, sidx, eidx, meta);
  }
  public static List<Object> toList(final long[] data) { return toList(data, 0, data.length, null); }

  public static List<Object> toList(final float[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new IDoubleArrayList() {
      public String toString() { return Transformables.sequenceToString(this); }
      public boolean equals(Object other) {
	return equiv(other);
      }
      public int hashCode() { return hasheq(); }
      public ArraySection getArray() { return new ArraySection(data, sidx, eidx); }
      public int size() { return dlen; }
      public Float get(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public double getDouble(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx];}
      public Float set(int idx, Object obj) {
	return (float)setDouble(idx, Casts.doubleCast(obj));
      }
      public double setDouble(int idx, double v) {
	float obj = (float)v;
	idx = wrapCheckIndex(idx, dlen) + sidx;
	final float retval = data[idx];
	data[idx] = obj;
	return retval;
      }
      public List<Object> subList(int ssidx, int seidx) {
	checkIndexRange(sidx, dlen, ssidx, seidx);
	return toList(data, ssidx + sidx, seidx + sidx, meta());
      }
      public IPersistentMap meta() { return meta; }
      public IObj withMeta(IPersistentMap m) {
	return (IObj)toList(data, sidx, eidx, m);
      }
      public Object[] toArray() {
	final int sz = size();
	Object[] retval = new Object[size()];
	for (int idx = 0; idx < sz; ++idx)
	  retval[idx] = data[idx+sidx];
	return retval;
      }
      public float[] toFloatArray() {
	return Arrays.copyOfRange(data, sidx, eidx);
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
      public List immutSort() {
	float[] retval = Arrays.copyOfRange(data, sidx, eidx);
	Arrays.sort(retval);
	return toList(retval, 0, retval.length, meta);
      }
      public Object reduce(IFn fn) {
	if(dlen ==0) return fn.invoke();
	final int ne = dlen;
	final int ss = sidx;
	final float[] d = data;
	Object init = d[ss];
	for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public Object reduce(IFn fn, Object init) {
	if(dlen ==0) return init;
	final int ne = dlen;
	final int ss = sidx;
	final float[] d = data;
	for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public double doubleReduction(DoubleBinaryOperator op, double init) {
	final int sz = size();
	for (int idx = 0; idx < sz; ++idx)
	  init = op.applyAsDouble(init, data[idx+sidx]);
	return init;
      }
      public List immutShuffle(Random r) {
	final int sz = size();
	final int[] perm = IntArrays.shuffle(ArrayLists.iarange(0, sz, 1), r);
	final float[] bdata = new float[sz];
	for(int idx = 0; idx < sz; ++idx)
	  bdata[idx] = data[perm[idx]];
	return toList(bdata);
      }
    };
  }
  public static List<Object> toList(final float[] data) { return toList(data, 0, data.length, null); }


  public static class DoubleArraySubList extends ArraySection implements IDoubleArrayList {
    public final double[] data;
    public final int nElems;
    public final IPersistentMap meta;
    public DoubleArraySubList(double[] d, int sidx, int eidx, IPersistentMap m) {
      super(d, sidx, eidx);
      data = d;
      nElems = eidx - sidx;
      meta = m;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public ArraySection getArray() { return this; }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    public double getDouble(int idx) { return data[wrapCheckIndex(idx, nElems) + sidx]; }
    static double setDouble(final double[] d, final int sidx, final int nElems,
			int idx, final double v) {
      idx = wrapCheckIndex(idx, nElems) + sidx;
      final double retval = d[idx];
      d[idx] = v;
      return retval;
    }
    public Object set(int idx, Object obj) {
      return setDouble(data, sidx, nElems, idx, Casts.doubleCast(obj));
    }
    public double setDouble(int idx, double obj) {
      return setDouble(data, sidx, nElems, idx, obj);
    }
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(sidx, eidx, ssidx, seidx);
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
    public void fillRange(int startidx, int endidx, Object v) {
      checkIndexRange(sidx, eidx, startidx, endidx);
      Arrays.fill(data, startidx+sidx, endidx+sidx, Casts.longCast(v));
    }
    public void fillRange(int startidx, List v) {
      final int vs = v.size();
      final int endidx = startidx + vs;
      checkIndexRange(sidx, eidx, startidx, endidx);
      if(v instanceof ArrayOwner) {
	ArraySection src = ((ArrayOwner)v).getArray();
	if( src.array instanceof double[]) {
	  System.arraycopy((double[])src.array, src.sidx, data, sidx, vs);
	  return;
	}
      }
      final int sd = sidx;
      final double[] d = data;
      if (v instanceof IMutList) {
	IMutList vv = (IMutList)v;
	for(int idx = 0; idx < vs; ++idx)
	  d[idx+sidx] = vv.getDouble(idx);
      } else {
	for(int idx = 0; idx < vs; ++idx)
	  d[idx+sidx] = Casts.longCast(v.get(idx));
      }
    }
    public Object reduce(IFn fn) {
      if(isEmpty()) return fn.invoke();
      final int ne = nElems;
      final int ss = sidx;
      final double[] d = data;
      Object init = d[ss];
      for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public Object reduce(IFn fn, Object init) {
      if(isEmpty()) return init;
      final int ne = nElems;
      final int ss = sidx;
      final double[] d = data;
      for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	init = fn.invoke(init, d[idx+ss]);
      if(RT.isReduced(init))
	return ((IDeref)init).deref();
      return init;
    }
    public static double doubleReduction(final double[] d, final int sidx, final int sz,
					 final DoubleBinaryOperator op, double init) {
      if (sidx == 0) {
	for(int idx = 0; idx < sz; ++idx)
	  init = op.applyAsDouble(init, d[idx]);
      } else {
	for(int idx = 0; idx < sz; ++idx)
	  init = op.applyAsDouble(init, d[idx+sidx]);
      }
      return init;
    }
    public double doubleReduction(DoubleBinaryOperator op, double init) {
      return doubleReduction(data, sidx, size(), op, init);
    }
    @SuppressWarnings("unchecked")
    public static void forEach(final double[] d, final int sidx, final int sz, Consumer c) {
      if(c instanceof DoubleConsumer) {
	final DoubleConsumer dc = (DoubleConsumer)c;
	if(sidx == 0) {
	  for(int idx = 0; idx < sz; ++idx)
	    dc.accept(d[idx]);
	} else {
	  for(int idx = 0; idx < sz; ++idx)
	    dc.accept(d[idx+sidx]);
	}
      }
      else {
	for(int idx = 0; idx < sz; ++idx)
	  c.accept(d[idx+sidx]);
      }
    }
    public void forEach(Consumer c) {
      forEach(data, sidx, nElems, c);
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
	  //Avoid boxing on each comparison
	  Object[] sd = toArray();
	  Arrays.sort(sd, c);
	  final int sz = size();
	  for(int idx = 0; idx < sz; ++idx)
	    setDouble(idx, (Double)sd[idx]);
	}
      }
    }
    @SuppressWarnings("unchecked")
    public List immutSort(Comparator c) {
      List retval = toList(Arrays.copyOfRange(data, sidx, eidx), 0, (eidx - sidx), meta());
      retval.sort(c);
      return retval;
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
    public List immutShuffle(Random r) {
      final double[] bdata = toDoubleArray();
      DoubleArrays.shuffle(bdata, r);
      return toList(bdata);
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
    public ArraySection getArray() { return new ArraySection(data, 0, nElems); }
    public Class containedType() { return data.getClass().getComponentType(); }
    public int size() { return nElems; }
    public Object get(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public double getDouble(int idx) { return data[wrapCheckIndex(idx, nElems)]; }
    public Object set(int idx, Object obj) {
      return DoubleArraySubList.setDouble(data, 0, nElems, idx, Casts.longCast(obj));
    }
    public double setDouble(int idx, double obj) {
      return DoubleArraySubList.setDouble(data, 0, nElems, idx, obj);
    }
    public int capacity() { return data.length; }
    double[] ensureCapacity(int len) {
      double[] d = data;
      if (len >= d.length) {
	d = data = Arrays.copyOf(d, len < 100000 ? len * 2 : (int)(len * 1.5));
      }
      return d;
    }
    public boolean addDouble(double obj) {
      int val = RT.intCast(obj);
      final int ne = nElems;
      final double[] d = ensureCapacity(ne+1);
      d[ne] = val;
      nElems = ne+1;
      return true;
    }
    public boolean add(Object obj) { return addDouble(Casts.longCast(obj)); }
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
    public boolean addAll(Collection <? extends Object> c) {
      if (c.isEmpty()) return false;
      if (c instanceof RandomAccess) {
	final int cs = c.size();
	final int sz = size();
	ensureCapacity(cs+sz);
	nElems += cs;
	//Hit fastpath
	fillRange(sz, (List)c);
      } else {
	for(Object o: c)
	  add(o);
      }
      return true;
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
	fillRange(sidx, (List)c);
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
    public List<Object> subList(int ssidx, int seidx) {
      checkIndexRange(0, nElems, ssidx, seidx);
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
    public double[] toDoubleArray() {
      return Arrays.copyOf(data, nElems);
    }
    public void fillRange(int startidx, int endidx, Object v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, endidx, v);
    }
    public void fillRange(int startidx, List v) {
      ((RangeList)subList(0, nElems)).fillRange(startidx, v);
    }
    public void addRange(final int startidx, final int endidx, final Object v) {
      final int ne = nElems;
      checkIndexRange(0, ne, startidx, endidx);
      final int rangeLen = endidx - startidx;
      final int newLen = ne + rangeLen;
      ensureCapacity(newLen);
      System.arraycopy(data, startidx, data, endidx, nElems - startidx);
    }
    public void removeRange(int startidx, int endidx) {
      checkIndexRange(0, nElems, startidx, endidx);
      System.arraycopy(data, startidx, data, endidx, nElems - endidx);
      nElems -= endidx - startidx;
    }
    public Object reduce(IFn fn) { return ((IReduce)subList(0, nElems)).reduce(fn); }
    public Object reduce(IFn fn, Object init) { return ((IReduceInit)subList(0, nElems)).reduce(fn,init); }
    public double doubleReduction(DoubleBinaryOperator op, double init) {
      return DoubleArraySubList.doubleReduction(data, 0, size(), op, init);
    }
    public void forEach(Consumer c) {
      DoubleArraySubList.forEach(data, 0, nElems, c);
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
    public List immutSort(Comparator c) {
      return ((ImmutSort)subList(0, nElems)).immutSort(c);
    }
    public int[] sortIndirect(Comparator c) {
      return ((IMutList)subList(0, nElems)).sortIndirect(c);
    }
    public List immutShuffle(Random r) {
      return ((IMutList)subList(0, nElems)).immutShuffle(r);
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

  public static List<Object> toList(final double[] data, final int sidx, final int eidx, IPersistentMap meta) {
    return new DoubleArraySubList(data, sidx, eidx, meta);
  }
  public static List<Object> toList(final double[] data) { return toList(data, 0, data.length, null); }
  public static List<Object> toList(final char[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ILongArrayList() {
      public String toString() { return Transformables.sequenceToString(this); }
      public boolean equals(Object other) {
	return equiv(other);
      }
      public int hashCode() { return hasheq(); }
      public ArraySection getArray() { return new ArraySection(data, sidx, eidx); }
      public int size() { return dlen; }
      public Character get(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public long getLong(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public Character set(int idx, Character obj) {
	return (char)setLong(idx, Casts.longCast(obj));
      }
      public long setLong(int idx, long obj) {
	char v = RT.charCast(obj);
	idx = wrapCheckIndex(idx, dlen) + sidx;
	final char retval = data[idx];
	data[idx] = v;
	return retval;
      }
      public List<Object> subList(int ssidx, int seidx) {
	checkIndexRange(sidx, dlen, ssidx, seidx);
	return toList(data, ssidx + sidx, seidx + sidx, meta());
      }
      public IPersistentMap meta() { return meta; }
      public IObj withMeta(IPersistentMap m) {
	return (IObj)toList(data, sidx, eidx, m);
      }
      public Object[] toArray() {
	final int sz = size();
	Object[] retval = new Object[size()];
	for (int idx = 0; idx < sz; ++idx)
	  retval[idx] = data[idx+sidx];
	return retval;
      }
      public List immutSort() {
	char[] retval = Arrays.copyOfRange(data, sidx, eidx);
	Arrays.sort(retval);
	return toList(retval, 0, retval.length, meta);
      }
      public Object reduce(IFn fn) {
	if(dlen ==0) return fn.invoke();
	final int ne = dlen;
	final int ss = sidx;
	final char[] d = data;
	Object init = d[ss];
	for(int idx = 1; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
      public Object reduce(IFn fn, Object init) {
	if(dlen ==0) return init;
	final int ne = dlen;
	final int ss = sidx;
	final char[] d = data;
	for(int idx = 0; idx < ne && (!RT.isReduced(init)); ++idx)
	  init = fn.invoke(init, d[idx+ss]);
	if(RT.isReduced(init))
	  return ((IDeref)init).deref();
	return init;
      }
    };
  }
  public static List<Object> toList(final char[] data) { return toList(data, 0, data.length, null); }

  public static List<Object> toList(final boolean[] data, final int sidx, final int eidx, IPersistentMap meta) {
    final int dlen = eidx - sidx;
    return new ILongArrayList() {
      public String toString() { return Transformables.sequenceToString(this); }
      public boolean equals(Object other) {
	return equiv(other);
      }
      public int hashCode() { return hasheq(); }
      public ArraySection getArray() { return new ArraySection(data, sidx, eidx); }
      public int size() { return dlen; }
      public Boolean get(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx]; }
      public long getLong(int idx) { return data[wrapCheckIndex(idx, dlen) + sidx] ? 1 : 0; }
      public Boolean set(int idx, Object obj) {
	boolean bval = Casts.booleanCast(obj);
	idx = wrapCheckIndex(idx, dlen) + sidx;
	final Boolean retval = data[idx];
	data[idx] = bval;
	return retval;
      }
      public long setLong(int idx, long obj) {
	boolean bval = Casts.booleanCast(obj);
	idx = wrapCheckIndex(idx, dlen) + sidx;
	final boolean retval = data[idx];
	data[idx] = bval;
	return Casts.longCast(retval);
      }
      public List<Object> subList(int ssidx, int seidx) {
	checkIndexRange(sidx, dlen, ssidx, seidx);
	return toList(data, ssidx + sidx, seidx + sidx, meta());
      }
      public IPersistentMap meta() { return meta; }
      public IObj withMeta(IPersistentMap m) {
	return (IObj)toList(data, sidx, eidx, m);
      }
      public Object[] toArray() {
	final int sz = size();
	Object[] retval = new Object[size()];
	for (int idx = 0; idx < sz; ++idx)
	  retval[idx] = data[idx+sidx];
	return retval;
      }
    };
  }
  public static List<Object> toList(final boolean[] data) { return toList(data, 0, data.length, null); }

  public static List toList(Object obj) {
    if (obj == null) return null;
    Class cls = obj.getClass();
    if(!cls.isArray())
      throw new RuntimeException("Object is not an array: " + String.valueOf(obj));
    if(obj instanceof Object[])
      return toList((Object[])obj);
    else if (cls == byte[].class)
      return toList((byte[])obj);
    else if (cls == short[].class)
      return toList((short[])obj);
    else if (cls == int[].class)
      return toList((int[])obj);
    else if (cls == long[].class)
      return toList((long[])obj);
    else if (cls == float[].class)
      return toList((float[])obj);
    else if (cls == double[].class)
      return toList((double[])obj);
    else if (cls == char[].class)
      return toList((char[])obj);
    else if (cls == boolean[].class)
      return toList((boolean[])obj);
    else
      throw new RuntimeException("Invalid array type.");
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

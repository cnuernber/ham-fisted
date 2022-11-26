package ham_fisted;

import java.util.Comparator;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import clojure.lang.IPersistentMap;
import clojure.lang.RT;
import clojure.lang.IFn;
import it.unimi.dsi.fastutil.ints.IntComparator;


public class ConstList implements IMutList<Object>, TypedList {
  public final long nElems;
  public final Object value;
  public final IPersistentMap meta;
  public ConstList(long _nElems, Object _v, IPersistentMap m) {
    nElems = _nElems;
    value = _v;
    meta = m;
  }
  public IMutList cloneList() { return this; }
  public Class containedType() { return value != null ? value.getClass() : Object.class; }
  public static ConstList create(long nElems, Object value, IPersistentMap m) {
    if (value instanceof Long || value instanceof Integer
	|| value instanceof Short || value instanceof Byte
	|| value instanceof Character)
      return new LongConstList(nElems, Casts.longCast(value), m);
    if (value instanceof Double || value instanceof Float)
      return new DoubleConstList(nElems, Casts.doubleCast(value), m);
    return new ConstList(nElems, value, m);
  }
  public int size() { return RT.intCast(nElems); }
  public Object get(int idx) {
    return value;
  }
  public ConstList subList(long sidx, long eidx) {
    ChunkedList.sublistCheck(sidx, eidx, nElems);
    return create(eidx-sidx, value, meta);
  }
  public ConstList subList(int sidx, int eidx) {
    return subList((long)sidx, (long)eidx);
  }
  public void sort(Comparable c) { }
  public ConstList immutSort(Comparable c) { return this; }
  public ConstList ImmutSort() { return this; }
  public ConstList reverse() { return this; }
  public int[] sortIndirect() { return ArrayLists.iarange(0, size(), 1); }
  public Object[] toArray() {
    Object[] retval = new Object[size()];
    Arrays.fill(retval, value);
    return retval;
  }
  public int[] toIntArray() {
    int v = RT.intCast(Casts.longCast(value));
    int[] retval = new int[size()];
    Arrays.fill(retval, v);
    return retval;
  }
  public long[] toLongArray() {
    long v = Casts.longCast(value);
    long[] retval = new long[size()];
    Arrays.fill(retval, v);
    return retval;
  }
  public double[] toDoubleArray() {
    double v = Casts.doubleCast(value);
    double[] retval = new double[size()];
    Arrays.fill(retval, v);
    return retval;
  }
  public ConstList reindex(int[] indexes) {
    return ConstList.create(indexes.length, value, meta);
  }
  public List immutShuffle(Random r) { return this; }
  public IntComparator indexComparator(Comparator c) {
    return new IntComparator() { public int compare(int l, int r) {return 0;}};
  }
  public IPersistentMap meta() { return meta; }
  public ConstList withMeta(IPersistentMap m) {
    return ConstList.create(nElems, value, m);
  }
  public static class LongConstList extends ConstList implements LongMutList {
    public final long lval;
    public LongConstList(long ne, long v, IPersistentMap m ) {
      super(ne, v, m);
      lval = v;
    }
    public Class containedType() { return Long.TYPE; }
    public long getLong(int idx) { return lval; }
    public Object reduce(IFn rfn, Object init) { return LongMutList.super.reduce(rfn, init); }
  }
  public static class DoubleConstList extends ConstList implements DoubleMutList {
    public final double lval;
    public DoubleConstList(long ne, double v, IPersistentMap m ) {
      super(ne, v, m);
      lval = v;
    }
    public Class containedType() { return Double.TYPE; }
    public double getDouble(int idx) { return lval; }
    public Object reduce(IFn rfn, Object init) { return DoubleMutList.super.reduce(rfn, init); }
  }
}

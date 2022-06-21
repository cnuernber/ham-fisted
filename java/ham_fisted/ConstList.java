package ham_fisted;

import java.util.Comparator;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import clojure.lang.IPersistentMap;
import clojure.lang.RT;
import it.unimi.dsi.fastutil.ints.IntComparator;


public class ConstList implements IMutList<Object>, TypedList {
  public final int nElems;
  public final Object value;
  public final IPersistentMap meta;
  public ConstList(int _nElems, Object _v, IPersistentMap m) {
    nElems = _nElems;
    value = _v;
    meta = m;
  }
  public Class containedType() { return value != null ? value.getClass() : Object.class; }
  public static ConstList create(int nElems, Object value, IPersistentMap m) {
    if (value instanceof Long || value instanceof Integer
	|| value instanceof Short || value instanceof Byte
	|| value instanceof Character)
      return new LongConstList(nElems, Casts.longCast(value), m);
    if (value instanceof Double || value instanceof Float)
      return new DoubleConstList(nElems, Casts.doubleCast(value), m);
    return new ConstList(nElems, value, m);
  }
  public int size() { return nElems; }
  public Object get(int idx) {
    return value;
  }
  public ConstList subList(int sidx, int eidx) {
    if( sidx < 0 || sidx >= nElems)
      throw new RuntimeException("Start index out of range.");
    if (eidx < sidx || eidx > nElems)
      throw new RuntimeException("End index out of range.");
    return create(eidx-sidx, value, meta);
  }
  public ConstList sort(Comparable c) { return this; }
  public ConstList immutSort(Comparable c) { return this; }
  public ConstList ImmutSort() { return this; }
  public ConstList reverse() { return this; }
  public int[] sortIndirect() { return new int[nElems]; }
  public Object[] toArray() {
    Object[] retval = new Object[nElems];
    Arrays.fill(retval, value);
    return retval;
  }
  public int[] toIntArray() {
    int v = RT.intCast(Casts.longCast(value));
    int[] retval = new int[nElems];
    Arrays.fill(retval, v);
    return retval;
  }
  public long[] toLongArray() {
    long v = Casts.longCast(value);
    long[] retval = new long[nElems];
    Arrays.fill(retval, v);
    return retval;
  }
  public double[] toDoubleArray() {
    double v = Casts.doubleCast(value);
    double[] retval = new double[nElems];
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
    long lval;
    public LongConstList(int ne, long v, IPersistentMap m ) {
      super(ne, v, m);
      lval = v;
    }
    public Class containedType() { return Long.TYPE; }
    public long getLong(int idx) { return lval; }
  }
  public static class DoubleConstList extends ConstList implements DoubleMutList {
    double lval;
    public DoubleConstList(int ne, double v, IPersistentMap m ) {
      super(ne, v, m);
      lval = v;
    }
    public Class containedType() { return Double.TYPE; }
    public double getDouble(int idx) { return lval; }
  }
}

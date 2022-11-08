package ham_fisted;

import java.util.Random;
import java.util.List;
import java.util.Comparator;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;
import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.function.DoubleConsumer;
import clojure.lang.IFn;


public interface DoubleMutList extends IMutList<Object> {
  default boolean add(Object obj) { addDouble(Casts.doubleCast(obj)); return true; }
  default void addLong(long obj) { addDouble(Casts.doubleCast(obj)); }
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { double v = getDouble(idx); setDouble(idx, Casts.doubleCast(obj)); return v; }
  default void setBoolean(int idx, boolean obj) { setDouble(idx, obj ? 1.0 : 0.0); }
  default void setLong(int idx, long obj) { setDouble(idx, (double)obj); }
  default Object get(int idx) { return getDouble(idx); }
  default long getLong(int idx) { return (long)getDouble(idx); }
  static class DoubleSubList extends IMutList.MutSubList<Object> implements DoubleMutList {
    @SuppressWarnings("unchecked")
    public DoubleSubList(IMutList l, int ss, int ee) {
      super(l, ss, ee);
    }
    public Object reduce(IFn rfn, Object init) { return DoubleMutList.super.reduce(rfn, init); }
    public Object longReduction(IFn.OLO rfn, Object init) {
      return DoubleMutList.super.longReduction(rfn, init);
    }
  }
  default IMutList<Object> subList(int sidx, int eidx) {
    ChunkedList.sublistCheck(sidx, eidx, size());
    return new DoubleSubList(this, sidx, eidx);
  }
  default boolean addAllReducible(Object obj) {
    final int sz = size();
    Reductions.serialReduction(new IFnDef.ODO() {
	public Object invokePrim(Object lhs, double rhs) {
	  ((IMutList)lhs).addDouble(rhs);
	  return lhs;
	}
      }, this, obj);
    return sz != size();
  }
  default void fillRange(int startidx, final int endidx, Object v) {
    double l = Casts.doubleCast(v);
    for(; startidx < endidx; ++startidx) {
      setDouble(startidx, l);
    }
  }
  default void fillRange(int startidx, List l) {
    if (l.isEmpty())
      return;
    final int sz = size();
    final int endidx = startidx + l.size();
    ArrayLists.checkIndexRange(size(), startidx, endidx);
    if(l instanceof ITypedReduce) {
      ((ITypedReduce)l).genericIndexedForEach(startidx, new IndexedDoubleConsumer() {
	  public void accept(long idx, double v) {
	    setDouble((int)idx, v);
	  }
	});
    } else {
      IMutList.super.fillRange(startidx, l);
    }
  }
  default void addRange(int startidx, int endidx, Object v) {
    Double l = Double.valueOf(Casts.doubleCast(v));
    for(; startidx < endidx; ++startidx) {
      add(startidx, l);
    }
  }
  default IntComparator indexComparator() {
    return new IntComparator() {
      public int compare(int lidx, int ridx) {
	return Double.compare(getDouble(lidx), getDouble(ridx));
      }
    };
  }
  default void sort(Comparator<? super Object> c) {
    DoubleComparator lc = ArrayLists.DoubleArraySubList.asDoubleComparator(c);
    if (c == null || lc != null) {
      final double[] data = toDoubleArray();
      if(c == null)
	DoubleArrays.parallelQuickSort(data);
      else
	DoubleArrays.parallelQuickSort(data, lc);
      fillRange(0, ArrayLists.toList(data));
    } else {
      IMutList.super.sort(c);
    }
  }
  default void shuffle(Random r) {
    fillRange(0, immutShuffle(r));
  }
  default List immutShuffle(Random r) {
    final double[] bdata = toDoubleArray();
    DoubleArrays.shuffle(bdata, r);
    return ArrayLists.toList(bdata);
  }

  default Object reduce(final IFn fn, Object init) {
    final IFn.ODO rrfn = fn instanceof IFn.ODO ? (IFn.ODO)fn : new IFn.ODO() {
	public Object invokePrim(Object lhs, double v) {
	  return fn.invoke(lhs, v);
	}
      };
    return doubleReduction(rrfn, init);
  }
  default Object longReduction(IFn.OLO fn, Object init) {
    return doubleReduction(new IFn.ODO() {
	public Object invokePrim(Object lhs, double v) {
	  return fn.invokePrim(lhs, Casts.longCast(v));
	}
      }, init);
  }
}

package ham_fisted;

import java.util.Random;
import java.util.List;
import java.util.Comparator;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleComparator;
import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.function.DoubleConsumer;


public interface DoubleMutList extends IMutList<Object> {
  default boolean add(Object obj) { addDouble(Casts.doubleCast(obj)); return true; }
  default void addLong(long obj) { addDouble(Casts.doubleCast(obj)); }
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { double v = getDouble(idx); setDouble(idx, Casts.doubleCast(obj)); return v; }
  default void setBoolean(int idx, boolean obj) { setDouble(idx, obj ? 1.0 : 0.0); }
  default void setLong(int idx, long obj) { setDouble(idx, (double)obj); }
  default Object get(int idx) { return getDouble(idx); }
  default long getLong(int idx) { return (long)getDouble(idx); }
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
    if(l instanceof IMutList) {
      final IMutList im = (IMutList)l;
      int idx = 0;
      for(; startidx < endidx; ++startidx, ++idx)
	setDouble(startidx, im.getDouble(idx));
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
  @SuppressWarnings("unchecked")
  default DoubleComparator asDoubleComparator(Comparator c) {
    if (c instanceof DoubleComparator)
      return (DoubleComparator)c;
    return null;
  }
  default void sort(Comparator<? super Object> c) {
    DoubleComparator lc = asDoubleComparator(c);
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
}

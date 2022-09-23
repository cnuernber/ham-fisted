package ham_fisted;

import java.util.Random;
import java.util.List;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.function.DoubleConsumer;


public interface DoubleMutList extends IMutList<Object> {
  default boolean add(Object obj) { addDouble(Casts.doubleCast(obj)); return true; }
  default void addLong(long obj) { addDouble(Casts.doubleCast(obj)); }
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { return setDouble(idx, Casts.doubleCast(obj)); }
  default long setLong(int idx, long obj) { return (long)setDouble(idx, (double)obj); }
  default Object get(int idx) { return getDouble(idx); }
  default long getLong(int idx) { return (long)getDouble(idx); }
  default void fillRange(int startidx, final int endidx, Object v) {
    double l = Casts.doubleCast(v);
    for(; startidx < endidx; ++startidx) {
      setDouble(startidx, l);
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
  default List immutShuffle(Random r) {
    final double[] bdata = toDoubleArray();
    DoubleArrays.shuffle(bdata, r);
    return ArrayLists.toList(bdata);
  }
}

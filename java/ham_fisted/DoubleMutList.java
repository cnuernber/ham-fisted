package ham_fisted;


import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.function.DoubleConsumer;


public interface DoubleMutList extends IMutList<Object> {
  default boolean add(Object obj) { return addDouble(Casts.doubleCast(obj)); }
  default boolean addLong(long obj) { return addDouble(Casts.doubleCast(obj)); }
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
}

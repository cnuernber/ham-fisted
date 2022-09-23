package ham_fisted;

import java.util.Random;
import java.util.List;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.function.LongConsumer;


public interface LongMutList extends IMutList<Object> {
  default boolean add(Object obj) { addLong(Casts.longCast(obj)); return true; }
  default void addDouble(double obj) { addLong(Casts.longCast(obj));}
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { return setLong(idx, Casts.longCast(obj)); }
  default double setDouble(int idx, double obj) { return setLong(idx, (long)obj); }
  default Object get(int idx) { return getLong(idx); }
  default double getDouble(int idx) { return getLong(idx); }
  default void fillRange(int startidx, final int endidx, Object v) {
    long l = Casts.longCast(v);
    for(; startidx < endidx; ++startidx) {
      setLong(startidx, l);
    }
  }
  default void addRange(int startidx, int endidx, Object v) {
    Long l = Long.valueOf(Casts.longCast(v));
    for(; startidx < endidx; ++startidx) {
      add(startidx, l);
    }
  }
  default IntComparator indexComparator() {
    return new IntComparator() {
      public int compare(int lidx, int ridx) {
	return Long.compare(getLong(lidx), getLong(ridx));
      }
    };
  }
  default List immutShuffle(Random r) {
    final long[] data = toLongArray();
    LongArrays.shuffle(data, r);
    return ArrayLists.toList(data);
  }
}

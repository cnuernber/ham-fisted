package ham_fisted;

import java.util.Random;
import java.util.List;
import java.util.Comparator;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.function.LongConsumer;


public interface LongMutList extends IMutList<Object> {
  default boolean add(Object obj) { addLong(Casts.longCast(obj)); return true; }
  default void addBoolean( boolean obj ) { addLong(obj ? 1 : 0); }
  default void addDouble(double obj) { addLong(Casts.longCast(obj));}
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { final long v = getLong(idx); setLong(idx, Casts.longCast(obj)); return v; }
  default void setBoolean(int idx, boolean obj) { setLong(idx, obj ? 1 : 0); }
  default void setDouble(int idx, double obj) { setLong(idx, (long)obj); }
  default Object get(int idx) { return getLong(idx); }
  default double getDouble(int idx) { return getLong(idx); }
  default void fillRange(int startidx, final int endidx, Object v) {
    long l = Casts.longCast(v);
    for(; startidx < endidx; ++startidx) {
      setLong(startidx, l);
    }
  }
  default void fillRange(int startidx, List l) {
    if (l.isEmpty())
      return;
    final int sz = size();
    final int endidx = startidx + l.size();
    ArrayLists.checkIndexRange(size(), startidx, endidx);
    if(l instanceof ITypedReduce) {
      ((ITypedReduce)l).genericIndexedForEach(startidx, new IndexedLongConsumer() {
	  public void accept(long idx, long v) {
	    setLong((int)idx, v);
	  }
	});
    } else {
      IMutList.super.fillRange(startidx, l);
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

  default void sort(Comparator<? super Object> c) {
    LongComparator lc = ArrayLists.LongArraySubList.asLongComparator(c);
    if (c == null || lc != null) {
      final long[] data = toLongArray();
      if(c == null)
	LongArrays.parallelQuickSort(data);
      else
	LongArrays.parallelQuickSort(data, lc);
      fillRange(0, ArrayLists.toList(data));
    } else {
      IMutList.super.sort(c);
    }
  }
  default void shuffle(Random r) {
    fillRange(0, immutShuffle(r));
  }
  default List immutShuffle(Random r) {
    final long[] data = toLongArray();
    LongArrays.shuffle(data, r);
    return ArrayLists.toList(data);
  }
  default void doubleForEach(DoubleConsumer dc) {
    longForEach(new LongConsumer() {
	public void accept(long c) {
	  dc.accept((double)c);
	}
      });
  }
}

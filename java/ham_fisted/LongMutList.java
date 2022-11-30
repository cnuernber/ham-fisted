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
import clojure.lang.IFn;
import clojure.lang.RT;


public interface LongMutList extends IMutList<Object> {
  default boolean add(Object obj) { addLong(Casts.longCast(obj)); return true; }
  default void addLong(long v) { throw new RuntimeException("Object " + String.valueOf(getClass()) + " failed to define addLong method"); }
  default void addBoolean( boolean obj ) { addLong(obj ? 1 : 0); }
  default void addDouble(double obj) { addLong(Casts.longCast(obj));}
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { final long v = getLong(idx); setLong(idx, Casts.longCast(obj)); return v; }
  default void setBoolean(int idx, boolean obj) { setLong(idx, obj ? 1 : 0); }
  default void setDouble(int idx, double obj) { setLong(idx, Casts.longCast(obj)); }
  default Object get(int idx) { return getLong(idx); }
  default double getDouble(int idx) { return getLong(idx); }
  default void fillRange(long startidx, final long endidx, Object v) {
    ChunkedList.checkIndexRange(0, size(), startidx, endidx);
    long l = Casts.longCast(v);
    final int ee = (int)endidx;
    int ss = (int)startidx;
    for(; ss < ee; ++ss) {
      setLong(ss, l);
    }
  }
  static class LongSubList extends IMutList.MutSubList<Object> implements LongMutList {
    @SuppressWarnings("unchecked")
    public LongSubList(IMutList l, int ss, int ee) {
      super(l, ss, ee);
    }
    public Object reduce(IFn rfn, Object init) { return LongMutList.super.reduce(rfn, init); }
  }
  default IMutList<Object> subList(int ssidx, int seidx) {
    ChunkedList.sublistCheck(ssidx, seidx, size());
    return new LongSubList(this, ssidx, seidx);
  }
  default void fillRange(final int startidx, List l) {
    if (l.isEmpty())
      return;
    final int sz = size();
    final int endidx = startidx + l.size();
    ArrayLists.checkIndexRange(size(), startidx, endidx);
    Reductions.serialReduction(new Reductions.IndexedLongAccum(new IFnDef.OLLO() {
	public Object invokePrim(Object acc, long idx, long v) {
	  ((IMutList)acc).setLong((int)idx+startidx, v);
	  return acc;
	}
      }), this, l);
  }
  default void addRange(int startidx, int endidx, Object v) {
    Long l = Long.valueOf(Casts.longCast(v));
    for(; startidx < endidx; ++startidx) {
      add(startidx, l);
    }
  }

  default boolean addAllReducible(Object obj) {
    final int sz = size();
    Reductions.serialReduction(new IFnDef.OLO() {
	public Object invokePrim(Object lhs, long rhs) {
	  ((IMutList)lhs).addLong(rhs);
	  return lhs;
	}
      }, this, obj);
    return sz != size();
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

  default Object reduce(final IFn rfn, Object init) {
    return longReduction(Transformables.toLongReductionFn(rfn), init);
  }
  default Object longReduction(IFn.OLO rfn, Object init) {
    final int sz = size();
    for (int idx = 0; idx < sz && !RT.isReduced(init); ++idx)
      init = rfn.invokePrim(init, getLong(idx));
    return init;
  }
}

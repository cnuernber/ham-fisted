package ham_fisted;

import java.util.Spliterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;


public class RandomAccessSpliterator<E> implements Spliterator<E> {

  protected final List<E> list;
  protected final int sidx;
  protected int eidx;
  int curIdx;

  RandomAccessSpliterator(List<E> list, int sidx, int eidx) {
    assert list instanceof RandomAccess;

    this.list = list;
    this.sidx = sidx;
    this.eidx = eidx;
    curIdx = sidx;
  }

  RandomAccessSpliterator(List<E> list) {
    this(list, 0, list.size());
  }

  protected Spliterator<E> construct(List<E> list, int split, int eidx) {
    return new RandomAccessSpliterator<E>(list, split, eidx);
  }

  protected Spliterator<E> doSplit() {
    final int ne = eidx - sidx;
    if(ne > 1) {
      int split = sidx + (ne / 2);
      int eeidx = eidx;
      eidx = split;
      return construct(list, split, eeidx);
    }
    return null;
  }

  public Spliterator<E> trySplit() {
    return doSplit();
  }

  public long estimateSize() { return eidx - sidx; }

  public boolean tryAdvance(Consumer<? super E> action) {
    if (action == null)
      throw new NullPointerException();
    final boolean retval = curIdx < eidx;
    if(retval) {
      action.accept(list.get(curIdx));
      ++curIdx;
    }
    return retval;
  }

  public void forEachRemaining(Consumer<? super E> c) {
    final int ee = eidx;
    if(list instanceof IMutList) {
      final IMutList ll = (IMutList)list;
      if(c instanceof DoubleConsumer) {
	final DoubleConsumer dc = (DoubleConsumer)c;
	for(int cc = curIdx; cc < ee; ++cc) {
	  dc.accept(ll.getDouble(cc));
	}
	curIdx = eidx;
	return;
      }
      else if (c instanceof LongConsumer) {
	final LongConsumer dc = (LongConsumer)c;
	for(int cc = curIdx; cc < ee; ++cc) {
	  dc.accept(ll.getLong(cc));
	}
	curIdx = eidx;
	return;
      }
    }
    final List<E> ll = list;
    for(int cc = curIdx; cc < ee; ++cc) {
      c.accept(ll.get(cc));
    }
    curIdx = eidx;
  }

  public int characteristics() {
    return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE;
  }
  public static class LongSpliterator extends RandomAccessSpliterator<Long> implements Spliterator.OfLong {
    @SuppressWarnings("unchecked")
    public LongSpliterator(IMutList list, int sidx, int eidx) {
      super(list, sidx, eidx);
    }
    protected Spliterator<Long> construct(List list, int sidx, int eidx) {
      return new LongSpliterator((IMutList)list, sidx, eidx);
    }
    public Spliterator.OfLong trySplit() {
      return (Spliterator.OfLong) doSplit();
    }
    public boolean tryAdvance(LongConsumer action) {
      if (action == null)
	throw new NullPointerException();
      final boolean retval = curIdx < eidx;
      if(retval) {
	action.accept(((IMutList)list).getLong(curIdx));
	++curIdx;
      }
      return retval;
    }
    public void forEachRemaining(LongConsumer dc) {
      final int ee = eidx;
      final IMutList ll = (IMutList)list;
      for(int cc = curIdx; cc < ee; ++cc) {
	dc.accept(ll.getLong(cc));
      }
      curIdx = eidx;
    }
  }
  public static class DoubleSpliterator extends RandomAccessSpliterator<Double> implements Spliterator.OfDouble {
    @SuppressWarnings("unchecked")
    public DoubleSpliterator(IMutList list, int sidx, int eidx) {
      super(list, sidx, eidx);
    }
    protected Spliterator<Double> construct(List list, int sidx, int eidx) {
      return new DoubleSpliterator((IMutList)list, sidx, eidx);
    }
    public Spliterator.OfDouble trySplit() {
      return (Spliterator.OfDouble) doSplit();
    }
    public boolean tryAdvance(DoubleConsumer action) {
      if (action == null)
	throw new NullPointerException();
      final boolean retval = curIdx < eidx;
      if(retval) {
	action.accept(((IMutList)list).getDouble(curIdx));
	++curIdx;
      }
      return retval;
    }
    public void forEachRemaining(DoubleConsumer dc) {
      final int ee = eidx;
      final IMutList ll = (IMutList)list;
      for(int cc = curIdx; cc < ee; ++cc) {
	dc.accept(ll.getDouble(cc));
      }
      curIdx = eidx;
    }
  }
}

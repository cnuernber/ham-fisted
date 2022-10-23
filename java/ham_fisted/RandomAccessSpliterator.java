package ham_fisted;

import java.util.Spliterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;


public class RandomAccessSpliterator<E> implements Spliterator<E> {

  private final List<E> list;
  private final int sidx;
  private int eidx;
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

  public Spliterator<E> trySplit() {
    final int nsidx = (eidx - sidx) / 2;
    final Spliterator<E> retval = new RandomAccessSpliterator<E>(list, nsidx, eidx);
    eidx = nsidx;
    return retval;
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
}

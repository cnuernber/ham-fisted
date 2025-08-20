package ham_fisted;

import java.util.Iterator;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.PriorityQueue;
import java.util.NoSuchElementException;


public class MergeIterator implements Iterator {
  public static class CurrentIterator implements Iterator {
    public static final Object invalid = new Object();
    Object current;
    Iterator iter;
    public CurrentIterator(Iterator iter) {
      this.iter = iter;
      this.current = invalid;
    }
    public CurrentIterator(Iterator iter, Object cur) {
      this.iter = iter;
      this.current = cur;
    }
    public boolean hasNext() { return iter.hasNext(); }
    public Object next() {
      return current = iter.next();
    }
    public Object current() { return current; }
    public boolean hasCurrent() { return current != invalid; }
    public static CurrentIterator create(Iterator iter) {
      if(iter.hasNext())
	return new CurrentIterator(iter, iter.next());
      return null;
    }
  }

  java.util.Comparator cmp;
  CurrentIterator[] iters;
  int curIdx;
  Predicate p;
  @SuppressWarnings("unchecked")
  int leastIndex() {
    int nIters = iters.length;
    if(nIters <= 1) return 0;
    Object leastv = iters[0].current();
    int leastIdx = 0;
    //Potential for binary search here I guess
    for(int idx = 1; idx < nIters; ++idx) {
      Object cur = iters[idx].current();
      if (cmp.compare(leastv, cur) > -1) {
	leastv = cur;
	leastIdx = idx;
      }
    }
    return leastIdx;
  }
  public MergeIterator(CurrentIterator[] iters, Comparator cmp, Predicate p) {
    this.iters = iters;
    this.p = p;
    this.cmp = cmp;
    curIdx = leastIndex();
  }
  public boolean hasNext() {
    return iters.length != 0;
  }
  @SuppressWarnings("unchecked")
  public Object next() {
    Object rv = null;
    do {
      if(iters.length == 0) return null;
      CurrentIterator iter = iters[curIdx];
      rv = iter.current();
      if(iter.hasNext()) {
	iter.next();
      } else {
	int ne = iters.length;
	CurrentIterator[] newIters = new CurrentIterator[ne-1];
	for(int idx = 0; idx < ne; ++ idx) {
	  if(idx < curIdx) {
	    newIters[idx] = iters[idx];
	  } else if (idx > curIdx) {
	    newIters[idx-1] = iters[idx];
	  }
	}
	iters = newIters;
      }
      curIdx = leastIndex();
    } while(!p.test(rv));
    return rv;
  }
  public static final Iterator emptyIter = new Iterator() {
      public boolean hasNext() { return false; }
      public Object next() { throw new java.util.NoSuchElementException(); }
    };
  public static class TwoWayMergeIterator implements Iterator {
    CurrentIterator lhs;
    CurrentIterator rhs;
    Comparator cmp;
    Predicate p;
    boolean left;
    @SuppressWarnings("unchecked")
    public TwoWayMergeIterator(CurrentIterator lhs, CurrentIterator rhs, Comparator cmp, Predicate p) {
      this.lhs = lhs;
      this.rhs = rhs;
      this.cmp = cmp;
      this.p = p;
      this.left = cmp.compare(lhs.current(), rhs.current()) < 0 ? true : false;
    }
    public boolean hasNext() {
      return lhs != null || rhs != null;
    }
    @SuppressWarnings("unchecked")
    public Object next() {
      if(lhs == null && rhs == null)
	throw new java.util.NoSuchElementException();
      Object rv;
      do {
	CurrentIterator update = left ? lhs : rhs;
	if(update == null) return null;
	rv = update.current();
	if(update.hasNext()) {
	  update.next();
	} else {
	  if(left) lhs = null; else rhs = null;
	}
	if(lhs == null) left = false;
	else if (rhs == null) left = true;
	else left = cmp.compare(lhs.current(), rhs.current()) < 0 ? true : false;
      } while(!p.test(rv));
      return rv;
    }
    public static Iterator create(Iterator lhs, Iterator rhs, Comparator cmp) {
      return new TwoWayMergeIterator(new CurrentIterator(lhs, lhs.next()),
				     new CurrentIterator(rhs, rhs.next()),
				     cmp, MergeIterator.alwaysTrue);
    }
  }
  public static class PriorityQueueIterator implements Iterator {
    PriorityQueue pq;
    Predicate p;
    public PriorityQueueIterator(PriorityQueue pq, Predicate p) {
      this.pq = pq;
      this.p = p;
    }
    public boolean hasNext() {
      return !pq.isEmpty();
    }
    @SuppressWarnings("unchecked")
    public Object next() {
      if(pq.isEmpty()) throw new NoSuchElementException();
      while(true) {
	if(pq.isEmpty()) return null;
	final Object[] entry = (Object[])pq.poll();
	Iterator iter = (Iterator)entry[0];
	Object rv = entry[1];
	if(iter.hasNext()) {
	  entry[1] = iter.next();
	  pq.offer(entry);
	}
	if(p.test(rv)) return rv;
      }
    }
    @SuppressWarnings("unchecked")
    public static Iterator create(Iterable<Iterator> srcIters, Comparator cmp, Predicate p) {
      Comparator pqCmp = new Comparator() {
	  public int compare(Object lhs, Object rhs) {
	    return cmp.compare(((Object[])lhs)[1],((Object[])rhs)[1]);
	  }
	};
      PriorityQueue pq = new PriorityQueue(pqCmp);
      for(Iterator iter : srcIters) {
	if(iter != null && iter.hasNext())
	  pq.offer(new Object[] {iter, iter.next()});
      }
      return new PriorityQueueIterator(pq, p);
    }
    public static Iterator create(Iterable<Iterator> srcIters, Comparator cmp) {
      return create(srcIters, cmp, alwaysTrue);
    }
  }
  @SuppressWarnings("unchecked")
  public static final Iterator createMergeIterator(Iterable<Iterator> srcIters, Comparator cmp, Predicate p) {
    ArrayList<Iterator> validIters = new ArrayList<Iterator>();
    for(Iterator iter : srcIters) {
      if(iter != null && iter.hasNext()) {
	validIters.add(iter);
      }
    }
    if(validIters.isEmpty())
      return emptyIter;
    if(validIters.size() >= 8) {
      return PriorityQueueIterator.create(validIters, cmp, p);
    }
    CurrentIterator[] iters = new CurrentIterator[validIters.size()];
    for(int idx = 0; idx < iters.length; ++idx) {
      Iterator iter = validIters.get(idx);
      iters[idx] = new CurrentIterator(iter, iter.next());
    }
    if(validIters.size() == 2)
      return new TwoWayMergeIterator(iters[0], iters[1], cmp, p);
    else
      return new MergeIterator(iters, cmp, p);
  }
  public static final Predicate alwaysTrue = new Predicate() { public boolean test(Object o){ return true; } };
  public static Iterator createMergeIterator(Iterable<Iterator> srcIters, Comparator cmp) {
    return createMergeIterator(srcIters, cmp, alwaysTrue);
  }
}

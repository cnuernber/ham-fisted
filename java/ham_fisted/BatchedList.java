package ham_fisted;

import clojure.lang.IDeref;

public class BatchedList implements IMutList, IDeref {
  public static final int tailWidth = 64;
  public static final int leafWidth = 64;
  Object[] tail = new Object[tailWidth];
  int nTail;
  Object[][] leafTail = new Object[leafWidth][];
  int nLeafTail;
  public static class Link {
    public final Object[][] leaf;
    public Link next;
    public Link(Object[][] leaf) {
      this.leaf = leaf;
      this.next = null;
    }
  }
  Link first;
  Link last;
  int count;
  public BatchedList() {
    first = null;
    last = null;
    count = 0;
  }
  public boolean add(Object obj) {
    if(nTail == tailWidth) {
      if(nLeafTail == leafWidth) {
	Link l = new Link(leafTail);
	leafTail = new Object[leafWidth][];
	nLeafTail = 0;
	if(first == null) first = l;
	if(last != null)
	  last.next = l;
	last = l;
      } else {
	leafTail[nLeafTail++] = tail;
      }
      tail = new Object[tailWidth];
      nTail = 0;
    }
    tail[nTail++] = obj;
    ++count;
    return true;
  }
  public Object get(int idx) { throw new RuntimeException(); }
  public int count() { return count; }
  public int size() { return count; }
  public void clear() {
    nTail = 0;
    first = null;
    last = null;
    count = 0;
  }
  public TreeList deref() {
    return TreeList.EMPTY;
  }
}

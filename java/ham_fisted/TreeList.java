package ham_fisted;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.ArrayList;
import clojure.lang.IPersistentVector;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.Util;

public class TreeList implements IMutList, IPersistentVector {
  public static final int branchWidth = 32;
  public static final int leafWidth = branchWidth;
  public static final int tailWidth = branchWidth;
  public static final int shiftWidth = Integer.numberOfTrailingZeros(branchWidth);
  public static class SublistResult {
    public final Object node;
    public final Object[] tail;
    public SublistResult(Object node, Object[] tail) {
      this.node = node;
      this.tail = tail;
    }
  }
  public static class Leaf {
    Object[][] data;
    public Object[][] data() { return this.data; }
    public Leaf() { this.data = new Object[0][]; }
    public Leaf(Object[][] data) { this.data = data; }
    public Leaf(Object[] tail) { this.data = new Object[][]{ tail }; }
    public Object cons(Object[] tail) {
      if(data.length == leafWidth) {
	return new Object[]{this, new Leaf(tail) };
      } else {
	Object[][] newData = Arrays.copyOf(data, data.length+1);
	newData[data.length]=tail;
	return new Leaf(newData);
      }
    }
    public Object[] getArray(int idx) { return data[idx/leafWidth]; }
    public Leaf assocN(int idx, Object obj) {
      int localIdx = idx/leafWidth;
      Object[] entry = Arrays.copyOf(data[localIdx], tailWidth);
      entry[idx % tailWidth] = obj;
      Object[][] newData = Arrays.copyOf(data, data.length);
      newData[localIdx] = entry;
      return new Leaf(newData);
    }
    //Assumption here is that sidx lies exactly on a tailWidth boundary.
    public SublistResult subList(int sidx, int eidx) {
      int dataSidx = sidx / tailWidth;
      int numTails = ((eidx - sidx) + (tailWidth-1))/tailWidth;
      int dataEidx = dataSidx + numTails;
      int len = eidx - sidx;
      int leftover = len % tailWidth;
      /* System.out.println("Leaf SubList sidx " + String.valueOf(sidx) + " eidx " + String.valueOf(eidx) */
      /* 			 + " dataSidx " + String.valueOf(dataSidx) + " dataEidx " + String.valueOf(dataEidx) */
      /* 			 + " leftover " + leftover); */
      if(sidx == 0 && eidx == (data.length * tailWidth))
	return new SublistResult(this, new Object[0]);
      if(leftover == 0) {
	return new SublistResult(new Leaf(Arrays.copyOfRange(data, dataSidx, dataEidx)), new Object[0]);
      } else {

	Object[] tail = data[dataEidx-1];
	return new SublistResult(new Leaf(Arrays.copyOfRange(data, dataSidx, dataEidx-1))
				 , Arrays.copyOf(tail, leftover));
      }
    }
  }
  public static class Branch {
    Object[] data; //either branch or leaf
    public Object[] data() { return this.data; }
    public Branch() { data = new Object[0]; }
    public Branch(Object[] data) { this.data = data; }
    public Branch(Leaf leaf) { this.data = new Object[]{leaf}; }
    public Branch(Branch branch) { this.data = new Object[]{branch}; }
    public Branch(int shift, Object[] tail) {
      this.data = new Object[] { shift == 1 ? new Leaf(tail) : new Branch(shift-1, tail) };
    }
    public Object cons(int shift, Object[] tail) {
      if(data.length == 0)
	return new Branch(shift, tail);
      int lastIdx = data.length-1;
      Object last = data[lastIdx];
      Object res = shift == 1 ? ((Leaf)last).cons(tail) : ((Branch)last).cons(shift-1, tail);
      if(res instanceof Object[]) {
	Object newNode = ((Object[])res)[1];
	if(data.length == branchWidth) {
	  return new Object[] { this, new Branch( new Object[] { newNode } ) };
	} else {
	  Object[] newData = Arrays.copyOf(data, data.length+1);
	  newData[data.length] = newNode;
	  return new Branch(newData);
	}
      }
      Object[] newData = data.clone();
      newData[newData.length-1] = res;
      return new Branch(newData);
    }
    public Object getNode(int shift, int idx) {
      int shiftAmt = shift * shiftWidth;
      int level = branchWidth << shiftAmt;
      int localIdx = idx / level;
      return data[localIdx];
    }
    public Object[] getArray(int shift, int idx) {
      int shiftAmt = shift * shiftWidth;
      int level = branchWidth << shiftAmt;
      int localIdx = idx / level;
      int leftover = idx % level;
      Object item = data[localIdx];
      return shift == 1 ? ((Leaf)item).getArray(leftover) : ((Branch)item).getArray(shift-1, leftover);
    }
    public Branch assocN(int shift, int idx, Object obj) {
      int shiftAmt = shift * shiftWidth;
      int level = branchWidth << shiftAmt;
      int localIdx = idx / level;
      int leftover = idx % level;
      Object item = data[localIdx];
      Object newItem = shift == 1 ? ((Leaf)item).assocN(leftover, obj)
	: ((Branch)item).assocN(shift-1, leftover, obj);
      Object[] newData = Arrays.copyOf(data, data.length);
      newData[localIdx] = newItem;
      return new Branch(newData);
    }
    public static final BiFunction leafMergeRight = new BiFunction() {
	public Object apply(Object lhs, Object rhs) {
	  Leaf ll = (Leaf)lhs;
	  Leaf rr = (Leaf)rhs;
	  // System.out.println("ll " + String.valueOf(ll.data.length) + " rr " + String.valueOf(rr.data.length));
	  if(ll.data.length < leafWidth) {
	    int totalWidth = ll.data.length + rr.data.length;
	    int newLen = Math.min(leafWidth, ll.data.length + rr.data.length);
	    Object[][] newLeafData = Arrays.copyOf(ll.data, newLen);
	    System.arraycopy(rr.data, 0, newLeafData, ll.data.length, newLen - ll.data.length);
	    ll = new Leaf(newLeafData);
	    int newRightLen = totalWidth - newLen;
	    if(newRightLen > 0)
	      rr = new Leaf(Arrays.copyOfRange(rr.data, rr.data.length-newRightLen, rr.data.length));
	    else
	      rr = null;
	  }
	  return rr == null ? ll : new Object[] {ll, rr};
	}
      };
    public static final BiFunction branchMergeRight = new BiFunction() {
	public Object apply(Object lhs, Object rhs) {
	  Branch ll = (Branch)lhs;
	  Branch rr = (Branch)rhs;
	  // System.out.println("ll " + String.valueOf(ll.data.length) + " rr " + String.valueOf(rr.data.length));
	  if(ll.data.length < branchWidth) {
	    int totalWidth = ll.data.length + rr.data.length;
	    int newLen = Math.min(branchWidth, ll.data.length + rr.data.length);
	    Object[] newBranchData = Arrays.copyOf(ll.data, newLen);
	    System.arraycopy(rr.data, 0, newBranchData, ll.data.length, newLen - ll.data.length);
	    ll = new Branch(newBranchData);
	    int newRightLen = totalWidth - newLen;
	    if(newRightLen > 0)
	      rr = new Branch(Arrays.copyOfRange(rr.data, rr.data.length-newRightLen, rr.data.length));
	    else
	      rr = null;
	  }
	  return rr == null ? ll : new Object[] {ll, rr};
	}
      };

    @SuppressWarnings("unchecked")
    public SublistResult subList(int shift, int sidx, int eidx) {
      int shiftAmt = shift * shiftWidth;
      int level = branchWidth << shiftAmt;
      int localSidx = sidx / level;
      int numNodes = (eidx - (localSidx * level) + (level - 1)) / level;
      int localEidx = localSidx + numNodes;
      ArrayList newData = new ArrayList(numNodes);
      Object[] newTail = null;
      final BiFunction mergeFunction = shift == 1 ? leafMergeRight : branchMergeRight;
      for(int nodeIdx = 0; nodeIdx < numNodes; ++nodeIdx) {
	int dataIdx = localSidx + nodeIdx;
	int nodeStart = dataIdx * level;
	int nodeEnd = nodeStart + level;
	int nodeSidx = Math.max(nodeStart, sidx);
	int nodeEidx = Math.min(nodeEnd, eidx);
	Object dataNode = data[dataIdx];
	if(nodeStart == nodeSidx && nodeEnd == nodeEidx) {
	  newData.add(dataNode);
	} else {
	  int subSidx = nodeSidx - nodeStart;
	  int subEidx = nodeEidx - nodeStart;
	  SublistResult res = shift == 1 ?
	    ((Leaf)dataNode).subList(subSidx, subEidx) :
	    ((Branch)dataNode).subList(shift-1, subSidx, subEidx);
	  newData.add(res.node);
	  newTail = res.tail;
	}
	int sz = newData.size();
	if(sz > 1 ) {
	  Object merged = mergeFunction.apply(newData.get(sz-2), newData.get(sz-1));
	  if(merged instanceof Object[]) {
	    Object[] mm = (Object[])merged;
	    newData.set(sz-2, mm[0]);
	    newData.set(sz-1, mm[1]);
	  } else {
	    newData.set(sz-2, merged);
	    //right node was completely consumed by left
	    newData.remove(sz-1);
	  }
	}
      }
      return new SublistResult(new Branch(newData.toArray()), newTail);
    }
  };
  Object root;
  Object[] tail;
  final Object meta;
  int count;
  int shift;
  public Object root() { return root; }
  public Object[] tail() { return tail; }
  public int shift() { return shift; }
  public TreeList(Object root, Object[] tail, Object meta, int shift, int count) {
    this.root = root;
    this.tail = tail;
    this.meta = meta;
    this.shift = shift;
    this.count = count;
  }
  public TreeList(Object meta) {
    this(new Leaf(), new Object[0], meta, 0, 0);
  }
  public TreeList() {
    this(null);
  }
  public static TreeList EMPTY = new TreeList();
  public TreeList cons(Object d) {
    final int tlen = tail.length;
    final int newCount = count+1;
    if(tlen == 32) {
      Object rv = shift == 0 ? ((Leaf)root).cons(tail) : ((Branch)root).cons(shift, tail);
      if(rv instanceof Object[]) {
	return new TreeList(new Branch((Object[])rv), new Object[]{d}, meta, shift+1, newCount);
      } else {
	return new TreeList(rv, new Object[]{d}, meta, shift, newCount);
      }
    } else {
      Object[] newTail = new Object[tlen+1];
      System.arraycopy(tail, 0, newTail, 0, tail.length);
      newTail[tlen] = d;
      return new TreeList(root, newTail, meta, shift, newCount);
    }
  }

  public TreeList empty() { return EMPTY; }

  public final TreeList assocN(int idx, Object obj) {
    if(idx == count)
      return cons(obj);
    checkIndex(idx, count);
    int cutoff = count - tail.length;
    if( idx < cutoff) {
      return new TreeList(shift == 0 ? ((Leaf)root).assocN(idx, obj) : ((Branch)root).assocN(shift,idx,obj),
			  tail, meta, shift, count);
    } else {
      Object[] newTail = Arrays.copyOf(tail, tail.length);
      newTail[idx % tailWidth] = obj;
      return new TreeList(root, newTail, meta, shift, count);
    }
  }
  public final TreeList pop() {
    if (count == 0)
      throw new RuntimeException("Can't pop empty vector");
    if (count == 1)
      return new TreeList(new Branch(), new Object[0], meta, 1, 0);
    return null;
  }
  public final Object peek() {
    if (count == 0)
      return null;
    return get(count-1);
  }
  public int length() { return count; }
  public int count() { return count; }
  public int size() { return count; }
  public Object[] getArray(int idx) {
    int cutoff = count - tail.length;
    return (idx < cutoff)
      ? (shift == 0)
	 ? ((Leaf)root).getArray(idx) : ((Branch)root).getArray(shift, idx)
	 : tail; }
  public static int checkIndex(int idx, int nElems) {
    if (idx < 0 || idx >= nElems)
      throw new IndexOutOfBoundsException("Index: " + String.valueOf(idx) + " is out of range 0-" + String.valueOf(nElems));
    return idx;
  }
  public Object get(int idx) {
    checkIndex(idx, count);
    return getArray(idx)[idx % 32];
  }
  public static class ArrayIterator implements Iterator<Object[]> {
    final TreeList data;
    int arySidx;
    final int aryEidx;
    public ArrayIterator(TreeList data, int arySidx, int aryEidx) {
      this.data = data;
      this.arySidx = arySidx;
      this.aryEidx = aryEidx;
    }
    public boolean hasNext() { return arySidx < aryEidx; }
    public Object[] next() {
      if(arySidx >= aryEidx) throw new NoSuchElementException();
      Object[] rv = data.getArray(arySidx * tailWidth);
      ++arySidx;
      return rv;
    }
  }
  public Iterator<Object[]> arrayIterator(int sidx, int eidx) {
    int arySidx = sidx / tailWidth;
    int nArrays = (eidx - (arySidx * tailWidth) + (tailWidth - 1))/tailWidth;
    int aryEidx = arySidx + nArrays;
    return new ArrayIterator(this, arySidx, aryEidx);
  }

  public Object reduce(int sidx, int eidx, IFn rfn, Object acc) {
    Iterator<Object[]> iter = arrayIterator(sidx, eidx);
    int arySidx = sidx - (sidx % tailWidth);
    while(iter.hasNext()) {
      int startoff = Math.max(sidx, arySidx);
      int endoff = Math.min(eidx, arySidx + tailWidth);
      int copyLen = endoff - startoff;
      Object[] ary = iter.next();
      /* System.out.println("startoff: " + String.valueOf(startoff) + " endoff " + String.valueOf(endoff) + */
      /* 			 " copyLen " + String.valueOf(copyLen) + */
      /* 			 " sidx " + String.valueOf(sidx) + */
      /* 			 " eidx " + String.valueOf(eidx)); */
      int startIdx = startoff % 32;
      int endIdx = startIdx + copyLen;
      for(int idx = startIdx; idx < endIdx; ++idx) {
	acc = rfn.invoke(acc, ary[idx]);
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
      arySidx += tailWidth;
    }
    return acc;
  }
  
  public Object reduce(IFn rfn, Object acc) {
    return reduce(0, count, rfn, acc);
  }
  public Object[] fillArray(int sidx, int eidx, Object[] data) {
    Iterator<Object[]> iter = arrayIterator(sidx, eidx);
    int arySidx = sidx - (sidx % tailWidth);
    int writeOff = 0;
    while(iter.hasNext()) {
      int startoff = Math.max(sidx, arySidx);
      int endoff = Math.min(eidx, arySidx + tailWidth);
      int copyLen = endoff - startoff;
      System.arraycopy(iter.next(), startoff % 32, data, writeOff, copyLen);
      writeOff += copyLen;
      arySidx += tailWidth;
    }
    return data;
  }
  public Object[] fillArray(Object[] data) {
    return fillArray(0, count, data);
  }
  public static final void sublistCheck(long sidx, long eidx, long nElems) {
    if(sidx < 0 || sidx > nElems)
      throw new IndexOutOfBoundsException("Start index out of range: start-index("
					  + String.valueOf(sidx) +"), n-elems("
					  + String.valueOf(nElems) + ")");
    if(eidx < 0 || eidx > nElems)
      throw new IndexOutOfBoundsException("End index out of range: end-index("
					  + String.valueOf(eidx) +"), n-elems("
					  + String.valueOf(nElems) + ")");
    if(eidx < sidx)
      throw new IndexOutOfBoundsException("End index underflow: end-index("
					  + String.valueOf(eidx) +") < start-index("
					  + String.valueOf(sidx) + ")");
  }
  @SuppressWarnings("unchecked")
  public List immutSort(Comparator c) {
    return ImmutSort.immutSortList(this, c);
  }
  public static class SubList implements IMutList, IPersistentVector {
    int offset; //<= 32
    TreeList data;
    public SubList(int offset, TreeList data) {
      this.offset = offset;
      this.data = data;
    }
    public int offset() { return offset; }
    public TreeList data() { return data; }
    public int count() { return data.count() - offset; }
    public int size() { return data.count() - offset; }
    public int length() { return data.count() - offset; }
    public Object get(int idx) {
      return data.get(idx + offset);
    }
    public SubList cons(Object a) {
      return new SubList(offset, data.cons(a));
    }
    public SubList assocN(int idx, Object a) {
      return new SubList(offset, data.assocN(idx, a));
    }
    public IPersistentVector pop() {
      int cnt = count();
      if ( cnt == 0 ) throw new UnsupportedOperationException("Underflow");
      if ( cnt == 1 ) return TreeList.EMPTY;
      return new SubList(offset, data.pop());
    }
    public Object peek() { return data.peek(); }
    public IMutList subList(int sidx, int eidx) {
      sublistCheck(sidx, eidx, size());
      return data.subList(sidx+offset, eidx+offset);
    }
    public Object[] fillArray(Object[] ary) {
      return data.fillArray(offset, data.count(), ary);
    }
    public Object reduce(IFn rfn, Object acc) {
      return data.reduce(offset, data.count(), rfn, acc);
    }
    public static IMutList create(int offset, TreeList data) {
      if(offset == 0)
	return data;
      return new SubList(offset, data);
    }
  }
  public IMutList subList(int sidx, int eidx) {
    sublistCheck(sidx, eidx, size());
    int tlen = tail.length;
    int cutoff = count - tail.length;
    if(sidx == 0 && eidx == count)
      return this;
    if(sidx == eidx)
      return EMPTY;
    Object[] tailPart = null;
    if(eidx > cutoff) {
      int tailSidx = Math.max(sidx, cutoff);
      tailPart = Arrays.copyOfRange(tail, tailSidx - cutoff, eidx - cutoff);
    }
    int newShift = shift;
    Object newNode = null;
    Object[] treeTail = null;
    int offset = sidx % tailWidth;
    int roundedSidx = sidx - offset;
    if(sidx < cutoff) {
      int treeEidx = Math.min(eidx, cutoff);
      SublistResult treePart = shift==0
	? ((Leaf)root).subList(roundedSidx, treeEidx)
	: ((Branch)root).subList(shift, roundedSidx, treeEidx);
      newNode = treePart.node;
      treeTail = treePart.tail;
      while(newShift > 0) {
	Branch b = (Branch)newNode;
	if(b.data.length == 1) {
	  newNode = b.data[0];
	  newShift--;
	} else {
	  break;
	}
      }
    }
    if(newNode == null)
      return new TreeList(new Leaf(), tailPart, meta, 0, tailPart.length);
    TreeList newList = new TreeList(newNode, tailPart == null ? treeTail : tailPart,
				    meta, newShift, eidx-roundedSidx);
    return SubList.create(offset, newList);
  }
}

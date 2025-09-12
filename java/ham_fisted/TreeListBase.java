package ham_fisted;

import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.lang.IPersistentVector;
import clojure.lang.Box;



public class TreeListBase implements IMutList {
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
  public static int checkIndex(int idx, int nElems) {
    if (idx < 0 || idx >= nElems)
      throw new IndexOutOfBoundsException("Index: " + String.valueOf(idx) + " is out of range 0-" + String.valueOf(nElems));
    return idx;
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
  public static final Object[][] emptyObjAryAry = new Object[0][];
  public static final Object[] emptyObjAry = new Object[0];
  public static class ConsAllResult {
    public final Object[] nodes;
    public final Iter nextData;
    public final Object[] tail;
    public final int added;
    public ConsAllResult(Object[] nodes, Iter nextData, Object[] tail, int added) {
      this.nodes = nodes;
      this.nextData = nextData;
      this.tail = tail;
      this.added = added;
    }
  }
  public static class Leaf {
    final Object owner;
    Object[][] data;
    public Object[][] data() { return this.data; }
    public Leaf() { this.owner = null; this.data = emptyObjAryAry; }
    public Leaf(Object owner, Object[][] data) { this.owner = owner; this.data = data; }
    public Leaf(Object[][] data) { this(null, data); }
    public Leaf(Object[] tail) { this.owner = null; this.data = new Object[][]{ tail }; }
    public Object cons(Object owner, Object[] tail) {
      boolean force = owner == null || this.owner != owner;
      if(data.length == leafWidth) {
	return new Object[]{this, new Leaf(owner, new Object[][] { tail })};
      } else {
	Object[][] newData = Arrays.copyOf(data, data.length+1);
	newData[data.length]=tail;
	if(force)
	  return new Leaf(owner, newData);
	else {
	  this.data = newData;
	  return this;
	}
      }
    }
    public Object cons(Object[] tail) {
      return cons(null, tail);
    }
    public Object add(Object owner, Object[] tail) {
      return cons(owner, tail);
    }
    public ConsAllResult consAll(Object owner, int maxSiblings, Iter dataIter) {
      int maxTails = leafWidth  - data.length + leafWidth * maxSiblings;
      ArrayList<Object[]> tails = new ArrayList<Object[]>();
      Object[] tail = new Object[32];
      int nTail = 0;
      int added = 0;
      while(tails.size() < maxTails && dataIter != null) {
	if(nTail == tailWidth)
	  tails.add(tail.clone());
	for(nTail = 0; nTail < tailWidth && dataIter != null; ++nTail) {
	  tail[nTail++] = dataIter.get();
	  dataIter = dataIter.next();
	  ++added;
	}
	nTail = 0;
      }
      //Rectify tail
      if(nTail == 0)
	tail = new Object[0];
      else if (nTail != tailWidth)
	tail = Arrays.copyOf(tail, nTail);
      
      if(tails.isEmpty())
	return new ConsAllResult(new Object[]{this}, dataIter, tail, added);
      int totalTails = tails.size();
      int nLocalTails = Math.min(leafWidth - data.length, totalTails);
      Object[][] newData = Arrays.copyOf(data, nLocalTails);
      for(int idx = data.length; idx < nLocalTails; ++idx)
	newData[idx] = tails.get(idx-data.length);

      int nOtherLeaves = (totalTails - nLocalTails + leafWidth - 1) / leafWidth;
      Object[] leaves = new Object[1 + nOtherLeaves];
      leaves[0] = new Leaf(owner, newData);
      int leafIdx = 1;
      for(int idx = nLocalTails; idx < totalTails; idx += leafWidth) {
	int nextIdx = Math.min(totalTails, idx + leafWidth);
	int nLeafTails = nextIdx - idx;
	leaves[leafIdx++] = new Leaf(owner, tails.subList(idx, nextIdx).toArray(emptyObjAryAry));
      }
      return new ConsAllResult(leaves, dataIter, tail, added);
    }
    public Object[] getArray(int idx) { return data[idx/leafWidth]; }
    public Leaf assocN(Object owner, int idx, Object obj, Box oldVal) {
      boolean force = owner == null || this.owner != owner;
      int localIdx = idx/leafWidth;
      Object[] entry = force ? Arrays.copyOf(data[localIdx], tailWidth) : data[localIdx];
      int objIdx = idx % tailWidth;
      if(oldVal != null) oldVal.val = entry[objIdx];
      entry[objIdx] = obj;
      if(force) {
	Object[][] newData = Arrays.copyOf(data, data.length);
	newData[localIdx] = entry;
	return new Leaf(owner, newData);
      } else {
	return this;
      }
    }
    public SublistResult pop(Object owner) {
      int dlen = data.length;
      Object[] lastTail = data[dlen-1];
      Object[][] newD;
      if(dlen-1 == 0) {
	newD = new Object[0][];
      } else {
	newD = Arrays.copyOf(data, dlen-1);
      }
      boolean force = owner == null || this.owner != owner;
      Leaf newLeaf;
      if(force) {
	newLeaf = new Leaf(owner, newD);
      } else {
	this.data = newD;
	newLeaf = this;
      }
      return new SublistResult(newLeaf, lastTail);
    }
    public boolean isEmpty() { return data.length == 0; }
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
    final Object owner;
    Object[] data; //either branch or leaf
    public Object[] data() { return this.data; }
    public Branch() { this.owner = null; data = new Object[0]; }
    public Branch(Object owner, Object[] data) { this.owner = owner; this.data = data; }
    public Branch(Leaf leaf) { this.owner = null; this.data = new Object[]{leaf}; }
    public Branch(Branch branch) { this.owner = null; this.data = new Object[]{branch}; }
    public Branch(Object owner, int shift, Object[] tail) {
      this(owner, new Object[] { shift == 1 ? new Leaf(tail) : new Branch(shift-1, tail) } );
    }
    public Object cons(Object owner, int shift, Object[] tail) {
      if(data.length == 0)
	return new Branch(owner, shift, tail);
      int lastIdx = data.length-1;
      Object last = data[lastIdx];
      boolean force = owner == null || this.owner != owner;
      Object res = shift == 1 ? ((Leaf)last).cons(owner, tail) : ((Branch)last).cons(owner, shift-1, tail);
      if(res instanceof Object[]) {
	Object newNode = ((Object[])res)[1];
	if(data.length == branchWidth) {
	  return new Object[] { this, new Branch( owner, new Object[] { newNode } ) };
	} else {
	  Object[] newData = Arrays.copyOf(data, data.length+1);
	  newData[data.length] = newNode;
	  if(force) {
	    return new Branch(owner, newData);
	  } else {
	    data = newData;
	    return this;
	  }
	}
      }
      Object[] newData = force ? data.clone() : data;
      newData[newData.length-1] = res;
      if(force) {
	return new Branch(owner, newData);
      } else {
	return this;
      }
    }
    public Object cons(int shift, Object[] tail) {
      return cons(null, shift, tail);
    }
    public Object add(Object owner, int shift, Object[] tail) {
      return cons(owner, shift, tail);
    }
    public ConsAllResult consAll(Object owner, int shift, int maxSiblings, Iter dataIter) {
      int maxChildren = branchWidth - data.length + branchWidth * maxSiblings;
      Object lastNode = data[data.length-1];
      ConsAllResult res = shift == 1 ?
	((Leaf)lastNode).consAll(owner, maxChildren, dataIter) :
	((Branch)lastNode).consAll(owner, shift-1, maxChildren, dataIter);
      int numChildren = res.nodes.length;
      Object[] tail = res.tail;
      dataIter = res.nextData;
      int added = res.added;
      if(res.nodes.length == 1 && res.nodes[0] == lastNode)
	return new ConsAllResult(new Object[]{this}, dataIter, tail, added);
      int nLocalNodes = Math.min(numChildren, branchWidth - data.length);
      Object[] newData = Arrays.copyOf(data, data.length + nLocalNodes);
      int nNewBranches = (numChildren - nLocalNodes + branchWidth -1)/branchWidth;
      Object[] rv = new Object[1 + nNewBranches];
      rv[0] = new Branch(owner, newData);
      for(int idx = 0; idx < nNewBranches; ++idx) {
	int copyBegin = nLocalNodes + (idx * branchWidth);
	int copyEnd = Math.min(copyBegin + branchWidth, numChildren);
	rv[idx+1] = new Branch(owner, Arrays.copyOfRange(data, copyBegin, copyEnd));
      }
      return new ConsAllResult(rv, dataIter, tail, added);
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
    public Branch assocN(Object owner, int shift, int idx, Object obj, Box oldVal) {
      boolean force = owner == null || this.owner != owner;
      int shiftAmt = shift * shiftWidth;
      int level = branchWidth << shiftAmt;
      int localIdx = idx / level;
      int leftover = idx % level;
      Object item = data[localIdx];
      Object newItem = shift == 1 ? ((Leaf)item).assocN(owner, leftover, obj, oldVal)
	: ((Branch)item).assocN(owner, shift-1, leftover, obj, oldVal);
      Object[] newData = force ? Arrays.copyOf(data, data.length) : data;
      if(newItem != item) {	
	newData[localIdx] = newItem;
      }
      return force ? new Branch(owner, newData) : this;
    }
    public boolean isEmpty() { return data.length == 0; }
    public SublistResult pop(Object owner, int shift) {
      boolean force = owner == null || owner != this.owner;
      int dlen = data.length;
      Object lastObj = data[dlen-1];      
      SublistResult res = shift == 1 ? ((Leaf)lastObj).pop(owner) : ((Branch)lastObj).pop(owner, shift-1);
      Object node = res.node;
      Object[] newTail = res.tail;
      boolean empty = shift == 1 ? ((Leaf)node).isEmpty() : ((Branch)node).isEmpty();
      Object[] newD;
      if(empty) {
	if(dlen-1 == 0) {
	  newD = new Object[0];
	} else {
	  newD = Arrays.copyOf(data, dlen-1);
	}
      }else {
	newD = force ? data.clone() : data;
      }
      int newLen = newD.length;
      if(!empty)
	newD[newLen-1] = node;      
      
      Branch newBranch;
      if(force) {
	newBranch = new Branch(owner, newD);
      } else {
	this.data = newD;
	newBranch = this;
      }
      return new SublistResult(newBranch, newTail);
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
	    ll = new Branch(ll.owner, newBranchData);
	    int newRightLen = totalWidth - newLen;
	    if(newRightLen > 0)
	      rr = new Branch(rr.owner, Arrays.copyOfRange(rr.data, rr.data.length-newRightLen, rr.data.length));
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
      return new SublistResult(new Branch(owner, newData.toArray()), newTail);
    }
  };

  Object[] tail;
  Object root;
  int count;
  int shift;
  public Object[] tail() { return tail; }
  public Object[] validTail() { return tail; }
  public Object root() { return root; }
  public int length() { return count; }
  public int count() { return count; }
  public int size() { return count; }
  public int shift() { return shift; }
  public int nTail() { return tail.length; }
  
  public TreeListBase(Object root, Object[] tail, int shift, int count) {
    this.tail = tail;
    this.root = root;
    this.count = count;
    this.shift = shift;
  }
  public TreeListBase() {
    this.tail = new Object[0];
    this.root = new Leaf();
    this.count = 0;
    this.shift = 0;
  }
  public TreeListBase(TreeListBase other) {
    this.tail = other.validTail();
    this.root = other.root;
    this.count = other.count;
    this.shift = other.shift;
  }
  public static class ArrayIterator implements Iterator<Object[]> {
    final TreeListBase data;
    int arySidx;
    final int aryEidx;
    public ArrayIterator(TreeListBase data, int arySidx, int aryEidx) {
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
  public Object[] getArray(int idx) {
    int cutoff = count - nTail();
    return (idx < cutoff)
      ? (shift == 0)
      ? ((Leaf)root).getArray(idx) : ((Branch)root).getArray(shift, idx)
      : tail;
  }
  public Object get(int idx) {
    checkIndex(idx, count);
    return getArray(idx)[idx % 32];
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
      return TreeList.EMPTY;
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
      return new TreeList(new Leaf(), tailPart, meta(), 0, tailPart.length);
    TreeList newList = new TreeList(newNode, tailPart == null ? treeTail : tailPart,
				    meta(), newShift, eidx-roundedSidx);
    return SubList.create(offset, newList);
  }
}

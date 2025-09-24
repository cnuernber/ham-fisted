package ham_fisted;

import java.util.Arrays;
import clojure.lang.Box;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientVector;
import clojure.lang.RT;
import clojure.lang.IPersistentVector;

public class MutTreeList extends TreeListBase implements ITransientVector {
  int nTail;
  final IPersistentMap meta;
  public int nTail() { return nTail; }
  public Object[] validTail() {
    return nTail == tail.length ? tail : Arrays.copyOf(tail, nTail);
  }
  public MutTreeList(Object root, Object[] tail, IPersistentMap meta, int shift, int count) {
    super(root, tail, shift, count);
    this.meta = meta;
  }
  public MutTreeList() {
    super(new Leaf(null, new Object[0][]), new Object[tailWidth], 0, 0);
    this.nTail = 0;
    this.meta = null;
  }
  public MutTreeList(TreeListBase other, IPersistentMap meta) {
    super(other);
    this.meta = meta;
    nTail = tail.length;
    this.tail = Arrays.copyOf(this.tail, tailWidth);
  }
  void consTail(Object[] tail) {
    Object rv = shift == 0 ? ((Leaf)root).add(this, tail) : ((Branch)root).add(this, shift, tail);
    if(rv instanceof Object[]) {
      shift = shift+1;
      root = new Branch(this, (Object[])rv);
    } else {
      root = rv;
    }
  }
  public boolean add(Object obj) {
    final int tlen = nTail();
    final int newCount = count+1;
    if(tlen == 32) {
      consTail(tail);
      tail = new Object[tailWidth];
      nTail = 0;
    }
    tail[nTail++] = obj;
    this.count = newCount;
    return true;
  }
  public Object set(int idx, Object obj) {
    checkIndex(idx, count);
    int cutoff = count - nTail();
    if(idx < cutoff) {
      Box b = new Box(null);
      Object newRoot = shift == 1 ? ((Leaf)root).assocN(this, idx, obj, b) :
	((Branch)root).assocN(this, shift, idx, obj, b);
      root = newRoot;
      return b.val;
    } else {
      idx = idx - cutoff;
      Object rv = tail[idx];
      tail[idx] = obj;
      return rv;
    }
  }
  public void setObject(int idx, Object obj) {
    checkIndex(idx, count);
    int cutoff = count - nTail();
    if(idx < cutoff) {
      Object newRoot = shift == 1 ? ((Leaf)root).assocN(this, idx, obj, null) :
	((Branch)root).assocN(this, shift, idx, obj, null);
      root = newRoot;
    } else {
      idx = idx - cutoff;
      tail[idx] = obj;
    }
  }
  public MutTreeList assocN(int i, Object val) {
    if(i == count) add(val);
    setObject(i, val);
    return this;
  }
  public MutTreeList pop() {
    if(count == 0) throw new IllegalStateException("Can't pop empty vector");
    if(nTail > 0) {
      nTail--;
    } else {
      Object popResult = shift == 1 ? ((Leaf)root).pop(this) : ((Branch)root).pop(this, shift);
      if(popResult instanceof SublistResult) {
	SublistResult r = (SublistResult)popResult;
	this.root = r.node;
	nTail = tailWidth-1;
	System.arraycopy(tail, 0, r.tail, 0, nTail);
      } else {
	this.root = popResult;
      }
    }
    count--;
    return this;
  }
  public MutTreeList assoc(Object key, Object val) {
    return assocN(RT.intCast(key), val);
  }
  public MutTreeList conj(Object val) { add(val); return this; }
  public TreeList persistent() { return new TreeList( this, meta ); }
  public IPersistentVector immut() { return persistent(); }
  public static MutTreeList create(boolean owning, IPersistentMap meta, Object[] data) {
    int nLeaves = (data.length  + tailWidth - 1) / tailWidth;
    MutTreeList newList = new MutTreeList(new Leaf(null, new Object[0][]), new Object[tailWidth], meta, 0, 0);
    for(int idx = 0; idx < nLeaves; ++idx) {
      int dataOff = idx*32;
      int dataEnd = Math.min(dataOff + 32, data.length);
      if(idx == (nLeaves - 1)) {
	System.arraycopy(data, dataOff, newList.tail, 0, dataEnd - dataOff);
	newList.nTail = dataEnd - dataOff;
      } else {
	newList.consTail(Arrays.copyOfRange(data, dataOff, dataEnd));
      }
    }
    return newList;
  }
}

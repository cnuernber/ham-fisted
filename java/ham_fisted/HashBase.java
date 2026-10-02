package ham_fisted;


import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.Map;
import clojure.lang.IPersistentMap;
import clojure.lang.IHashEq;
import clojure.lang.IMeta;
import clojure.lang.IFn;
import clojure.lang.IDeref;
import clojure.lang.RT;


public class HashBase implements IMeta {
  int capacity;
  int mask;
  int length;
  int threshold;
  float loadFactor;
  HashNode[] data;
  IPersistentMap meta;
  //Set once persistent! has been called - the persistent result shares this table.
  boolean frozen;
  //Set when nodes are shared with another table (shallowClone).  Paths that edit nodes
  //in place must take ownership of them first.
  boolean sharedNodes;
  public HashBase(float loadFactor, int initialCapacity,
		  int length, HashNode[] data,
		  IPersistentMap meta) {
    this.loadFactor = loadFactor;
    this.capacity = IntegerOps.nextPow2(Math.max(4, initialCapacity));
    this.mask = this.capacity - 1;
    this.length = length;
    this.data = data == null ? new HashNode[this.capacity] : data;
    this.threshold = (int)(capacity * loadFactor);
    this.meta = meta;
  }
  
  public HashBase(HashBase other, IPersistentMap m) {
    this.loadFactor = other.loadFactor;
    this.capacity = other.capacity;
    this.mask = other.mask;
    this.length = other.length;
    this.data = other.data;
    this.threshold = other.threshold;
    this.sharedNodes = other.sharedNodes;
    this.meta = m;
  }
  final void ensureEditable() {
    if(frozen)
      throw new IllegalAccessError("Transient used after persistent! call");
  }
  //For paths that edit existing nodes in place as opposed to copying them.
  final void ensureOwned() {
    ensureEditable();
    if(sharedNodes) {
      final HashNode[] d = this.data;
      for(int idx = 0; idx < d.length; ++idx)
	if(d[idx] != null) d[idx] = d[idx].clone(this);
      sharedNodes = false;
    }
  }
  final void freeze() { frozen = true; }
  public int capacity() { return capacity; }
  public int size() { return length; }
  public int count() { return length; }
  //protected so clients can override as desired.
  protected int hash(Object k) {
    return
      k == null ? 0 :
      k instanceof IHashEq ? ((IHashEq)k).hasheq() :
      IntegerOps.mixhash(k.hashCode());
  }
  protected boolean equals(Object lhs, Object rhs) {
    return
      lhs == rhs ? true :
      lhs == null || rhs == null ? false :
      CljHash.nonNullEquiv(lhs,rhs);
  }
  protected void inc(HashNode lf) { ++this.length; }
  protected void dec(HashNode lf) { --this.length; }
  protected void modify(HashNode lf) {}
  protected HashNode newNode(Object key, int hc, Object val) {
    return new HashNode(this,key,hc,val,null);
  }
  
  Object checkResize(Object rv) {
    if(this.length >= this.threshold) {
      final int newCap = this.capacity * 2;
      final HashNode[] newD = new HashNode[newCap];
      final HashNode[] oldD = this.data;
      final int oldCap = oldD.length;
      final int mask = newCap - 1;
      for(int idx = 0; idx < oldCap; ++idx) {
	HashNode lf;
	if((lf = oldD[idx]) != null) {
	  oldD[idx] = null;
	  if(lf.nextNode == null) {
	    newD[lf.hashcode & mask] = lf;
	  } else {
	    //https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/HashMap.java#L722
	    //Because we only allow capacities that are powers of two, we have
	    //exactly 2 locations in the new data array where these can go.  We want
	    //to avoid writing to any locations more than once and instead make the
	    //at most two new linked lists, one for the new high position and one
	    //for the new low position.
	    HashNode loHead = null, loTail = null, hiHead = null, hiTail = null;
	    while(lf != null) {
	      HashNode e = lf.setOwner(this);
	      lf = lf.nextNode;
	      //Check high bit
	      if((e.hashcode & oldCap) == 0) {
		if(loTail == null) loHead = e;
		else loTail.nextNode = e;
		loTail = e;
	      } else {
		if(hiTail == null) hiHead = e;
		else hiTail.nextNode = e;
		hiTail = e;
	      }
	    }
	    if(loHead != null) {
	      loTail.nextNode = null;
	      newD[idx] = loHead;
	    }
	    if(hiHead != null) {
	      hiTail.nextNode = null;
	      newD[idx+oldCap] = hiHead;
	    }
	  }
	}
      }
      this.capacity = newCap;
      this.threshold = (int)(newCap * this.loadFactor);
      this.mask = mask;
      this.data = newD;
    }
    return rv;
  }
  public void clear() {
    ensureEditable();
    for(int idx = 0; idx < data.length; ++idx) {
      for(HashNode lf = data[idx]; lf != null; lf = lf.nextNode) {
	dec(lf);
      }
    }
    length = 0;
    Arrays.fill(data, null);
  }
  public IPersistentMap meta() { return meta; }
  static class HTIter implements Iterator {
    final HashNode[] d;
    final Function<Map.Entry,Object> fn;
    HashNode l;
    int idx;
    final int dlen;
    HTIter(HashNode[] data, Function<Map.Entry,Object> fn) {
      this.d = data;
      this.fn = fn;
      this.l = null;
      this.idx = 0;
      this.dlen = d.length;
      advance();
    }
    void advance() {
      if(l != null)
	l = l.nextNode;
      if(l == null) {
	for(; idx < this.dlen && l == null; ++idx)
	  l = this.d[idx];
      }
    }
    public boolean hasNext() { return l != null; }
    public Object next() {
      if(l == null) throw new NoSuchElementException();
      HashNode rv = l;
      advance();
      return fn.apply(rv);
    }
  }
  static class HTSpliterator implements Spliterator, ITypedReduce {
    final HashNode[] d;
    final Function<Map.Entry,Object> fn;
    int sidx;
    int eidx;
    int estimateSize;
    //Sizes are only exact before the first split.
    boolean split;
    HashNode l;
    public HTSpliterator(HashNode[] d, int len, Function<Map.Entry,Object> fn) {
      this.d = d;
      this.fn = fn;
      this.sidx = 0;
      this.eidx = d.length;
      this.estimateSize = len;
      this.l = null;
    }
    public HTSpliterator(HashNode[] d, int sidx, int eidx, int es, Function<Map.Entry,Object> fn) {
      this.d = d;
      this.fn = fn;
      this.sidx = sidx;
      this.eidx = eidx;
      this.estimateSize = es;
      this.l = null;
    }
    public HTSpliterator trySplit() {
      final int nIdxs = this.eidx - this.sidx;
      if(nIdxs > 4) {
	final int idxLen = nIdxs/2;
	final int oldIdx = this.eidx;
	this.eidx = this.sidx + idxLen;
	final int rhsSize = this.estimateSize / 2;
	this.estimateSize -= rhsSize;
	this.split = true;
	HTSpliterator rv = new HTSpliterator(d, this.eidx, oldIdx, rhsSize, this.fn);
	rv.split = true;
	return rv;
      }
      return null;
    }
    public int characteristics() { return Spliterator.DISTINCT | (split ? 0 : Spliterator.SIZED); }
    public long estimateSize() { return estimateSize; }
    public long getExactSizeIfKnown() { return split ? -1 : estimateSize; }
    void consumed() { if(estimateSize > 0) --estimateSize; }
    @SuppressWarnings("unchecked")
    public boolean tryAdvance(Consumer c) {
      if(this.l != null) {
	final HashNode ll = this.l;
	this.l = ll.nextNode;
	consumed();
	c.accept(this.fn.apply(ll));
	return true;
      }
      for(; sidx < eidx; ++sidx) {
	final HashNode ll = this.d[sidx];
	if(ll != null) {
	  ++sidx;
	  this.l = ll.nextNode;
	  consumed();
	  c.accept(this.fn.apply(ll));
	  return true;
	}
      }
      return false;
    }
    public Object reduce(IFn rfn, Object acc) {
      final HashNode[] dd = this.d;
      final int ee = this.eidx;
      final Function<Map.Entry,Object> ffn = this.fn;
      for(HashNode e = this.l; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, ffn.apply(e));
	if(RT.isReduced(acc)) return ((IDeref)acc).deref();
      }
      for(int idx = sidx; idx < ee; ++idx) {
	for(HashNode e = dd[idx]; e != null; e = e.nextNode) {
	  acc = rfn.invoke(acc, ffn.apply(e));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
      }
      return acc;
    }
  }

  final boolean containsNodeKey(Object key) {
    for(HashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return true;
    }
    return false;
  }
}

package ham_fisted;



import java.util.Map;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.Spliterator;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.ILookup;
import clojure.lang.IHashEq;
import clojure.lang.IReduceInit;
import clojure.lang.IDeref;
import clojure.lang.Reduced;

import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import static ham_fisted.IntegerOps.*;


public final class HashTable implements TrieBase, MapData {
  final HashProvider hp;
  int capacity;
  int mask;
  int length;
  int threshold;
  float loadFactor;
  LeafNode[] data;
  IPersistentMap meta;
  public HashTable(HashProvider hashProvider, float loadFactor, int initialCapacity,
		   int length, LeafNode[] data, IPersistentMap meta) {
    this.hp = hashProvider;
    this.loadFactor = loadFactor;
    this.capacity = nextPow2(Math.max(4, initialCapacity));
    this.mask = this.capacity - 1;
    this.length = length;
    this.data = data == null ? new LeafNode[this.capacity] : data;
    this.meta = meta;
    this.threshold = (int)(capacity * loadFactor);
  }
  public HashProvider hashProvider() { return hp; }
  public int hash(Object k) { return hp.hash(k); }
  public boolean equals(Object lhs, Object rhs) { return hp.equals(lhs, rhs); }
  public void inc() { this.length++;}
  public void dec() { this.length--;}
  public int size() { return this.length; }
  public boolean isEmpty() { return this.length == 0; }
  public HashTable shallowClone() {
    return new HashTable(this.hp, this.loadFactor, this.capacity, this.length,
			 this.data.clone(), this.meta);
  }
  public HashTable clone() {
    HashTable rv = shallowClone();
    final int dl = rv.data.length;
    final LeafNode[] d = rv.data;
    for (int idx = 0; idx < dl; ++idx) {
      final LeafNode e = d[idx];
      if(e != null)
	d[idx] = e.clone(rv);
    }
    return rv;
  }

  Object checkResize(Object rv) {
    if(this.length >= this.threshold) {
      final int newCap = this.capacity * 2;
      final LeafNode[] newD = new LeafNode[newCap];
      final LeafNode[] oldD = this.data;
      final int oldCap = oldD.length;
      final int mask = newCap - 1;
      for(int idx = 0; idx < oldCap; ++idx) {
	LeafNode lf;
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
	    LeafNode loHead = null, loTail = null, hiHead = null, hiTail = null;
	    while(lf != null) {
	      LeafNode e = lf;
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
  public LeafNode getOrCreate(Object key) {
    final int hc = hp.hash(key);
    final int idx = hc & this.mask;
    final HashProvider hp = this.hp;
    LeafNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(LeafNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(hp.equals(e.k, key))
	return e;
    }
    if(lastNode != null)
      return (LeafNode)checkResize(lastNode.getOrCreate(key,hc));
    else {
      lastNode = new LeafNode(this, key, hc, null, null);
      this.data[idx] = lastNode;
      return (LeafNode)checkResize(lastNode);
    }
  }
  public LeafNode getNode(Object key) {
    final int hc = hp.hash(key);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    return e != null ? e.get(key) : null;
  }
  @SuppressWarnings("unchecked")
  public Object compute(Object k, BiFunction bfn) {
    final HashProvider hp = this.hp;
    final int hash = hp.hash(k);
    final LeafNode[] d = this.data;
    final int idx = hash & this.mask;
    LeafNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || hp.equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    Object newV = bfn.apply(k, e == null ? null : e.v);
    if(e != null) {
      if(newV != null)
	e.v = newV;
      else
	remove(k, null);
    } else if(newV != null) {
      LeafNode nn = new LeafNode(this, k, hash, newV, null);
      if(ee != null)
	ee.nextNode = nn;
      else
	d[idx] = nn;
      checkResize(null);
    }
    return newV;
  }
  @SuppressWarnings("unchecked")
  public Object merge(Object k, Object v, BiFunction bfn) {
    final HashProvider hp = this.hp;
    final int hash = hp.hash(k);
    final LeafNode[] d = this.data;
    final int idx = hash & this.mask;
    LeafNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || hp.equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    Object newV = e == null ? v : bfn.apply(e.v, v);
    if(e != null) {
      if(newV != null)
	e.v = newV;
      else
	remove(k, null);
    } else if(newV != null) {
      LeafNode nn = new LeafNode(this, k, hash, newV, null);
      if(ee != null)
	ee.nextNode = nn;
      else
	d[idx] = nn;
      checkResize(null);
    }
    return newV;
  }
  public void remove(Object k, Box b) {
    final int hc = hp.hash(k);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e != null)
      this.data[idx] = e.remove(k, b);
  }
  public void clear() {
    length = 0;
    Arrays.fill(data, null);
  }
  public void mutAssoc(Object k, Object v) {
    final int hc = hp.hash(k);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e != null) {
      e.assoc(this, k, hc, v);
    } else {
      this.data[idx] = new LeafNode(this, k, hc, v, null);
    }
    checkResize(null);
  }
  public void mutDissoc(Object k) {
    final int hc = hp.hash(k);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e != null)
      this.data[idx] = e.dissoc(this, k);
  }
  public Object reduce(Function<ILeaf, Object> lfn, IFn rfn, Object acc) {
    final LeafNode[] d = this.data;
    final int dl = d.length;
    for(int idx = 0; idx < dl; ++idx) {
      for(LeafNode e = d[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, lfn.apply(e));
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;
  }
  public void mutUpdateValues(BiFunction bfn) {
    final LeafNode[] d = data;
    final int dl = d.length;
    for(int idx = 0; idx < dl; ++idx) {
      LeafNode e = d[idx];
      d[idx] = e.immutUpdate(this, bfn);
    }
  }
  public void mutUpdateValue(Object key, IFn fn) {
    final int hc = hp.hash(key);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e != null) {
      this.data[idx] = e.immutUpdate(this, key, hc, fn);
    } else {
      this.data[idx] = new LeafNode(this, key, hc, fn.invoke(null));
    }
    checkResize(null);
  }

  static class HTIter implements Iterator {
    final LeafNode[] d;
    final Function<ILeaf,Object> fn;
    LeafNode l;
    int idx;
    final int dlen;
    HTIter(LeafNode[] data, Function<ILeaf,Object> fn) {
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
      LeafNode rv = l;
      advance();
      return fn.apply(rv);
    }
  }
  public Iterator iterator(Function<ILeaf,Object> leafFn) {
    return new HTIter(this.data, leafFn);
  }
  static class HTSpliterator implements Spliterator, IReduceInit {
    final LeafNode[] d;
    final Function<ILeaf,Object> fn;
    int sidx;
    int eidx;
    int estimateSize;
    LeafNode l;
    public HTSpliterator(LeafNode[] d, int len, Function<ILeaf,Object> fn) {
      this.d = d;
      this.fn = fn;
      this.sidx = 0;
      this.eidx = d.length;
      this.estimateSize = len;
      this.l = null;
    }
    public HTSpliterator(LeafNode[] d, int sidx, int eidx, int es, Function<ILeaf,Object> fn) {
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
	this.estimateSize = this.estimateSize / 2;
	return new HTSpliterator(d, this.eidx, oldIdx, this.estimateSize, this.fn);
      }
      return null;
    }
    public int characteristics() { return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.SIZED; }
    public long estimateSize() { return estimateSize; }
    public long getExactSizeIfKnown() { return estimateSize(); }
    @SuppressWarnings("unchecked")
    public boolean tryAdvance(Consumer c) {
      if(this.l != null) {
	c.accept(this.fn.apply(this.l));
	this.l = this.l.nextNode;
	return true;
      }
      for(; sidx < eidx; ++sidx) {
	final LeafNode ll = this.d[sidx];
	if(ll != null) {
	  c.accept(this.fn.apply(ll));
	  this.l = ll.nextNode;
	  return true;
	}
      }
      return false;
    }
    public Object reduce(IFn rfn, Object acc) {
      final LeafNode[] dd = this.d;
      final int ee = this.eidx;
      final Function<ILeaf,Object> ffn = this.fn;
      for(int idx = sidx; idx < ee; ++idx) {
	for(LeafNode e = dd[idx]; e != null; e = e.nextNode) {
	  acc = rfn.invoke(acc, ffn.apply(e));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
      }
      return acc;
    }
  }
  public Spliterator spliterator(Function<ILeaf,Object> leafFn) { return new HTSpliterator(this.data, this.length, leafFn); }
  public IPersistentMap meta() { return this.meta; }
  public HashTable withMeta(IPersistentMap m) {
    HashTable rv = shallowClone();
    rv.meta = m;
    return rv;
  }
}

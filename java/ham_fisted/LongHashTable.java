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


public final class LongHashTable implements TrieBase, MapData {
  static final HashProvider hp = BitmapTrieCommon.equalHashProvider;
  int capacity;
  int mask;
  int length;
  int threshold;
  float loadFactor;
  LongLeafNode[] data;
  IPersistentMap meta;
  public LongHashTable(float loadFactor, int initialCapacity,
		       int length, LongLeafNode[] data, IPersistentMap meta) {
    this.loadFactor = loadFactor;
    this.capacity = nextPow2(Math.max(4, initialCapacity));
    this.mask = this.capacity - 1;
    this.length = length;
    this.data = data == null ? new LongLeafNode[this.capacity] : data;
    this.meta = meta;
    this.threshold = (int)(capacity * loadFactor);
  }
  public static int longHash(long v) {
    return IntegerOps.mixhash(Long.hashCode(v));
  }
  public HashProvider hashProvider() { return hp; }
  public int hash(Object k) { return longHash(Casts.longCast(k)); }
  public boolean equals(Object lhs, Object rhs) { return hp.equals(lhs, rhs); }
  public void inc() { this.length++;}
  public void dec() { this.length--;}
  public int size() { return this.length; }
  public boolean isEmpty() { return this.length == 0; }
  public LongHashTable shallowClone() {
    return new LongHashTable(this.loadFactor, this.capacity, this.length,
			     this.data.clone(), this.meta);
  }
  public LongHashTable clone() {
    LongHashTable rv = shallowClone();
    //Length is updated during clone.
    rv.length = 0;
    final int dl = rv.data.length;
    final LongLeafNode[] d = rv.data;
    for (int idx = 0; idx < dl; ++idx) {
      final LongLeafNode e = d[idx];
      if(e != null)
	d[idx] = e.clone(rv);
    }
    return rv;
  }

  Object checkResize(Object rv) {
    if(this.length >= this.threshold) {
      final int newCap = this.capacity * 2;
      final LongLeafNode[] newD = new LongLeafNode[newCap];
      final LongLeafNode[] oldD = this.data;
      final int oldCap = oldD.length;
      final int mask = newCap - 1;
      for(int idx = 0; idx < oldCap; ++idx) {
	LongLeafNode lf;
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
	    LongLeafNode loHead = null, loTail = null, hiHead = null, hiTail = null;
	    while(lf != null) {
	      LongLeafNode e = lf;
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
  public LongLeafNode getOrCreate(Object _key) {
    long key = Casts.longCast(_key);
    final int hc = longHash(key);
    final int idx = hc & this.mask;
    final HashProvider hp = this.hp;
    LongLeafNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(LongLeafNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(e.k == key)
	return e;
    }
    if(lastNode != null)
      return (LongLeafNode)checkResize(lastNode.getOrCreate(key,hc));
    else {
      lastNode = new LongLeafNode(this, key, hc, null, null);
      this.data[idx] = lastNode;
      return (LongLeafNode)checkResize(lastNode);
    }
  }
  public Object put(long key, Object val) {
    final int hc = longHash(key);
    final int idx = hc & this.mask;
    final HashProvider hp = this.hp;
    LongLeafNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(LongLeafNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(e.k == key) {
	final Object rv = e.v;
	e.v = val;
	return rv;
      }
    }
    LongLeafNode lf = new LongLeafNode(this, key, hc, val, null);
    if(lastNode != null) {
      lastNode.nextNode = lf;
    } else {
      data[idx] = lf;
    }
    return checkResize(null);
  }
  public LongLeafNode getNode(long key) {
    final int hc = longHash(key);
    final int idx = hc & this.mask;
    for(LongLeafNode e = this.data[idx]; e != null; e = e.nextNode) {
      if(e.k == key)
	return e;
    }
    return null;
  }
  public LongLeafNode getNode(Object _key) {
    return getNode(Casts.longCast(_key));
  }
  public Object getOrDefault(Object _key, Object dv) {
    long key = Casts.longCast(_key);
    for(LongLeafNode e = this.data[longHash(key) & this.mask]; e != null; e = e.nextNode) {
      if(e.k == key)
	return e.v;
    }
    return dv;
  }
  public Object get(Object _key) {
    long key = Casts.longCast(_key);
    for(LongLeafNode e = this.data[longHash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if(e.k == key)
	return e.v;
    }
    return null;
  }
  @SuppressWarnings("unchecked")
  public Object compute(Object _k, BiFunction bfn) {
    final long k = Casts.longCast(_k);
    final HashProvider hp = this.hp;
    final int hash = longHash(k);
    final LongLeafNode[] d = this.data;
    final int idx = hash & this.mask;
    LongLeafNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k); e = e.nextNode) {
      ee = e;
    }
    Object newV = bfn.apply(k, e == null ? null : e.v);
    if(e != null) {
      if(newV != null)
	e.v = newV;
      else
	remove(k, null);
    } else if(newV != null) {
      LongLeafNode nn = new LongLeafNode(this, k, hash, newV, null);
      if(ee != null)
	ee.nextNode = nn;
      else
	d[idx] = nn;
      checkResize(null);
    }
    return newV;
  }
  @SuppressWarnings("unchecked")
  public Object computeIfAbsent(Object _k, Function afn) {
    final long k = (long)_k;
    final int hash = longHash(k);
    final LongLeafNode[] d = this.data;
    final int idx = hash & this.mask;
    LongLeafNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k); e = e.nextNode) {
      ee = e;
    }
    if(e != null) {
      return e.v;
    } else {
      final Object newv = afn.apply(k);
      if(newv != null) {
	LongLeafNode nn = new LongLeafNode(this, k, hash, newv, null);
	if(ee != null)
	  ee.nextNode = nn;
	else
	  d[idx] = nn;
	checkResize(null);
      }
      return newv;
    }
  }
  @SuppressWarnings("unchecked")
  public Object merge(Object _k, Object v, BiFunction bfn) {
    final long k = Casts.longCast(_k);
    final HashProvider hp = this.hp;
    final int hash = longHash(k);
    final LongLeafNode[] d = this.data;
    final int idx = hash & this.mask;
    LongLeafNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k); e = e.nextNode) {
      ee = e;
    }
    Object newV = e == null ? v : bfn.apply(e.v, v);
    if(e != null) {
      if(newV != null)
	e.v = newV;
      else
	remove(k, null);
    } else if(newV != null) {
      LongLeafNode nn = new LongLeafNode(this, k, hash, newV, null);
      if(ee != null)
	ee.nextNode = nn;
      else
	d[idx] = nn;
      checkResize(null);
    }
    return newV;
  }
  public void remove(Object _k, Box b) {
    final long k = (long)_k;
    final int hc = longHash(k);
    final int idx = hc & this.mask;
    LongLeafNode e = this.data[idx];
    if(e != null)
      this.data[idx] = e.remove(k, b);
  }
  public void clear() {
    length = 0;
    Arrays.fill(data, null);
  }
  public LongHashTable mutAssoc(Object _k, Object v) {
    final long k = (long)_k;
    final int hc = longHash(k);
    final int idx = hc & this.mask;
    LongLeafNode e = this.data[idx];
    this.data[idx] = e != null ?
      e.assoc(this, k, hc, v) :
      new LongLeafNode(this, k, hc, v, null);
    checkResize(null);
    return this;
  }
  public LongHashTable mutDissoc(Object _k) {
    final long k = Casts.longCast(_k);
    final int hc = longHash(k);
    final int idx = hc & this.mask;
    LongLeafNode e = this.data[idx];
    if(e != null)
      this.data[idx] = e.dissoc(this, k);
    return this;
  }
  public Object reduce(Function<ILeaf, Object> lfn, IFn rfn, Object acc) {
    final LongLeafNode[] d = this.data;
    final int dl = d.length;
    for(int idx = 0; idx < dl; ++idx) {
      for(LongLeafNode e = d[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, lfn.apply(e));
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;
  }
  public void mutUpdateValues(BiFunction bfn) {
    final LongLeafNode[] d = data;
    final int dl = d.length;
    for(int idx = 0; idx < dl; ++idx) {
      LongLeafNode e = d[idx];
      d[idx] = e.immutUpdate(this, bfn);
    }
  }
  public LongHashTable mutUpdateValue(Object _key, IFn fn) {
    final long key = (long)_key;
    final int hc = longHash(key);
    final int idx = hc & this.mask;
    LongLeafNode e = this.data[idx];
    this.data[idx] = e != null ?
      e.immutUpdate(this, key, hc, fn) :
      new LongLeafNode(this, key, hc, fn.invoke(null));
    checkResize(null);
    return this;
  }

  static class HTIter implements Iterator {
    final LongLeafNode[] d;
    final Function<ILeaf,Object> fn;
    LongLeafNode l;
    int idx;
    final int dlen;
    HTIter(LongLeafNode[] data, Function<ILeaf,Object> fn) {
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
      LongLeafNode rv = l;
      advance();
      return fn.apply(rv);
    }
  }
  public Iterator iterator(Function<ILeaf,Object> leafFn) {
    return new HTIter(this.data, leafFn);
  }
  static class HTSpliterator implements Spliterator, IReduceInit {
    final LongLeafNode[] d;
    final Function<ILeaf,Object> fn;
    int sidx;
    int eidx;
    int estimateSize;
    LongLeafNode l;
    public HTSpliterator(LongLeafNode[] d, int len, Function<ILeaf,Object> fn) {
      this.d = d;
      this.fn = fn;
      this.sidx = 0;
      this.eidx = d.length;
      this.estimateSize = len;
      this.l = null;
    }
    public HTSpliterator(LongLeafNode[] d, int sidx, int eidx, int es, Function<ILeaf,Object> fn) {
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
	final LongLeafNode ll = this.d[sidx];
	if(ll != null) {
	  c.accept(this.fn.apply(ll));
	  this.l = ll.nextNode;
	  return true;
	}
      }
      return false;
    }
    public Object reduce(IFn rfn, Object acc) {
      final LongLeafNode[] dd = this.d;
      final int ee = this.eidx;
      final Function<ILeaf,Object> ffn = this.fn;
      for(int idx = sidx; idx < ee; ++idx) {
	for(LongLeafNode e = dd[idx]; e != null; e = e.nextNode) {
	  acc = rfn.invoke(acc, ffn.apply(e));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
      }
      return acc;
    }
  }
  public Spliterator spliterator(Function<ILeaf,Object> leafFn) { return new HTSpliterator(this.data, this.length, leafFn); }
  public IPersistentMap meta() { return this.meta; }
  public LongHashTable withMeta(IPersistentMap m) {
    LongHashTable rv = shallowClone();
    rv.meta = m;
    return rv;
  }
}

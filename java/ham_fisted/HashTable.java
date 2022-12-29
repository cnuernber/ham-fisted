package ham_fisted;



import java.util.Map;
import java.util.Arrays;
import java.util.Iterator;
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

import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import static ham_fisted.IntegerOps.*;


public final class HashTable implements TrieBase {
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
  public int hash(Object k) { return hp.hash(k); }
  public boolean equals(Object lhs, Object rhs) { return hp.equals(lhs, rhs); }
  public void inc() { this.length++;}
  public void dec() { this.length--;}
  public int size() { return this.length; }
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
      final int oldCap = this.data.length;
      final LeafNode[] oldD = this.data;
      final int mask = newCap - 1;
      for(int idx = 0; idx < oldCap; ++idx) {
	LeafNode lf = oldD[idx];
	while(lf != null) {
	  LeafNode nn = lf.nextNode;
	  lf.nextNode = null;
	  final int newIdx = lf.hashcode & mask;
	  final LeafNode existing = newD[newIdx];
	  if(existing == null)
	    newD[newIdx] = lf;
	  else
	    existing.append(lf);
	  lf = nn;
	}
      }
      this.capacity = newCap;
      this.threshold = (int)(newCap * this.loadFactor);
      this.mask = mask;
      this.data = newD;
    }
    return rv;
  }
  LeafNode getOrCreate(Object key) {
    final int hc = hp.hash(key);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e == null) {
      e = new LeafNode(this, key, hc, null, null);
      this.data[idx] = e;
      return (LeafNode)checkResize(e);
    } else {
      return (LeafNode)checkResize(e.getOrCreate(key,hc));
    }
  }
  LeafNode getNode(Object key) {
    final int hc = hp.hash(key);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    return e != null ? e.get(key) : null;
  }
  void remove(Object k, Box b) {
    final int hc = hp.hash(k);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e != null)
      this.data[idx] = e.remove(k, b);    
  }
  void mutAssoc(Object k, Object v) {
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
  void mutDissoc(Object k) {
    final int hc = hp.hash(k);
    final int idx = hc & this.mask;
    LeafNode e = this.data[idx];
    if(e != null)
      this.data[idx] = e.dissoc(this, k);
  }
  Object reduce(IFn rfn, Object acc) {
    final LeafNode[] d = this.data;
    final int dl = d.length;
    for(int idx = 0; idx < dl; ++idx) {      
      for(LeafNode e = d[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, e);
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;
  }
  void mutUpdateValues(BiFunction bfn) {
    final LeafNode[] d = data;
    final int dl = d.length;
    for(int idx = 0; idx < dl; ++idx) {
      LeafNode e = d[idx];
      d[idx] = e.immutUpdate(this, bfn);
    }
  }
  void mutUpdateValue(Object key, IFn fn) {
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
    LeafNode l;
    int idx;
    final int dlen;
    HTIter(LeafNode[] data) {
      this.d = data;
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
      return rv;
    }
  }
  Iterator iterator() {
    return new HTIter(this.data);
  }
  static class HTSpliterator implements Spliterator, IReduceInit {
    final LeafNode[] d;
    int sidx;
    int eidx;
    int estimateSize;
    LeafNode l;
    public HTSpliterator(LeafNode[] d, int len) {
      this.d = d;
      this.sidx = 0;
      this.eidx = d.length;
      this.estimateSize = len;
      this.l = null;
    }
    public HTSpliterator(LeafNode[] d, int sidx, int eidx, int es) {
      this.d = d;
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
	return new HTSpliterator(d, this.eidx, oldIdx, this.estimateSize);
      }
      return null;
    }
    public int characteristics() { return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.SIZED; }
    public long estimateSize() { return estimateSize; }
    public long getExactSizeIfKnown() { return estimateSize(); }
    @SuppressWarnings("unchecked")
    public boolean tryAdvance(Consumer c) {
      if(this.l != null) {
	c.accept(this.l);
	this.l = this.l.nextNode;
	return true;
      }
      for(; sidx < eidx; ++sidx) {
	final LeafNode ll = this.d[sidx];
	if(ll != null) {
	  c.accept(ll);
	  this.l = ll.nextNode;
	  return true;
	}
      }
      return false;
    }
    public Object reduce(IFn rfn, Object acc) {
      final LeafNode[] dd = this.d;
      final int ee = this.eidx;
      for(int idx = sidx; idx < ee; ++idx) {
	for(LeafNode e = dd[idx]; e != null; e = e.nextNode) {
	  acc = rfn.invoke(acc, e);
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}	  
      }
      return acc;
    }
  }
  Spliterator spliterator() { return new HTSpliterator(this.data, this.length); }
  IPersistentMap meta() { return this.meta; }
  HashTable withMeta(IPersistentMap m) {
    HashTable rv = shallowClone();
    rv.meta = m;
    return rv;
  }
}

package ham_fisted;



import java.util.Map;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.Spliterator;
import java.util.Objects;
import java.util.Set;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Collection;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IHashEq;
import clojure.lang.IDeref;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IMeta;
import clojure.lang.IMapEntry;
import clojure.lang.MapEntry;


public class HashMap implements IMap, IMeta, BitmapTrieCommon.MapSet {
  int capacity;
  int mask;
  int length;
  int threshold;
  float loadFactor;
  HBNode[] data;
  IPersistentMap meta;
  public HashMap(float loadFactor, int initialCapacity,
		  int length, HBNode[] data,
		  IPersistentMap meta) {
    this.loadFactor = loadFactor;
    this.capacity = IntegerOps.nextPow2(Math.max(4, initialCapacity));
    this.mask = this.capacity - 1;
    this.length = length;
    this.data = data == null ? new HBNode[this.capacity] : data;
    this.threshold = (int)(capacity * loadFactor);
    this.meta = meta;
  }
  public HashMap() {
    this(0.75f, 0, 0, null, null);
  }
  public HashMap(IPersistentMap m) {
    this(0.75f, 0, 0, null, m);
  }
  public HashMap(HashMap other, IPersistentMap m) {
    this.loadFactor = other.loadFactor;
    this.capacity = other.capacity;
    this.mask = other.mask;
    this.length = other.length;
    this.data = other.data;
    this.threshold = other.threshold;
    this.meta = m;
  }
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
  protected HBNode newNode(Object key, int hc, Object val) {
    return new HBNode(this,key,hc,val,null);
  }
  protected void inc(HBNode lf) { ++this.length; }
  protected void dec(HBNode lf) { --this.length; }


  public HashMap shallowClone() {
    return new HashMap(loadFactor, capacity, length, data.clone(), meta);
  }
  public HashMap clone() {
    final int l = data.length;
    HBNode[] newData = new HBNode[l];
    HashMap retval = new HashMap(loadFactor, capacity, length, newData, meta);
    for(int idx = 0; idx < l; ++idx) {
      HBNode orig = data[idx];
      if(orig != null)
	newData[idx] = orig.clone(retval);
    }
    return retval;
  }
  public int hashCode() {
    return hasheq();
  }
  public int hasheq() {
    return CljHash.mapHashcode(this);
  }
  public  boolean equals(Object o) {
    return equiv(o);
  }
  public boolean equiv(Object o) {
    return CljHash.mapEquiv(this, o);
  }
  public int size() { return this.length; }
  public boolean isEmpty() { return this.length == 0; }
  public String toString() {
    final StringBuilder b =
      (StringBuilder) reduce(new IFnDef() {
	  public Object invoke(Object acc, Object v) {
	    final StringBuilder b = (StringBuilder)acc;
	    final Map.Entry lf = (Map.Entry)v;
	    if(b.length() > 2)
	      b.append(",");
	    return b.append(lf.getKey())
	      .append(" ")
	      .append(lf.getValue());
	  }
	}, new StringBuilder().append("{"));
    return b.append("}").toString();
  }
  Object checkResize(Object rv) {
    if(this.length >= this.threshold) {
      final int newCap = this.capacity * 2;
      final HBNode[] newD = new HBNode[newCap];
      final HBNode[] oldD = this.data;
      final int oldCap = oldD.length;
      final int mask = newCap - 1;
      for(int idx = 0; idx < oldCap; ++idx) {
	HBNode lf;
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
	    HBNode loHead = null, loTail = null, hiHead = null, hiTail = null;
	    while(lf != null) {
	      HBNode e = lf.setOwner(this);
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
  public Object put(Object key, Object val) {
    final int hc = hash(key);
    final int idx = hc & this.mask;
    HBNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(HBNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(e.k == key || equals(e.k, key)) {
	return e.setValue(val);
      }
    }
    HBNode lf = newNode(key,hc,val);
    if(lastNode != null) {
      lastNode.nextNode = lf;
    } else {
      data[idx] = lf;
    }
    return checkResize(null);
  }
  public Object getOrDefault(Object key, Object dv) {
    for(HBNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return e.v;
    }
    return dv;
  }
  public Object get(Object key) {
    for(HBNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return e.v;
    }
    return null;
  }
  public IMapEntry entryAt(Object key) {
    for(HBNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))	
	return MapEntry.create(e.k, e.v);
    }
    return null;
  }
  public boolean containsKey(Object key) {
    for(HBNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return true;
    }
    return false;
  }
  @SuppressWarnings("unchecked")
  public Object compute(Object k, BiFunction bfn) {
    final int hash = hash(k);
    final HBNode[] d = this.data;
    final int idx = hash & this.mask;
    HBNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    Object newV = bfn.apply(k, e == null ? null : e.v);
    if(e != null) {
      if(newV != null)
	e.v = newV;
      else
	remove(k, null);
    } else if(newV != null) {
      HBNode nn = newNode(k, hash, newV);
      if(ee != null)
	ee.nextNode = nn;
      else
	d[idx] = nn;
      checkResize(null);
    }
    return newV;
  }
  @SuppressWarnings("unchecked")
  public Object computeIfAbsent(Object k, Function afn) {
    final int hash = hash(k);
    final HBNode[] d = this.data;
    final int idx = hash & this.mask;
    HBNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    if(e != null) {
      return e.v;
    } else {
      final Object newv = afn.apply(k);
      if(newv != null) {
	HBNode nn = newNode(k, hash, newv);
	if(ee != null)
	  ee.nextNode = nn;
	else
	  d[idx] = nn;
	checkResize(null);
      }
      return newv;
    }
  }
  public Object remove(Object key) {
    HBNode lastNode = null;
    int loc = hash(key) & this.mask;
    for(HBNode e = this.data[loc]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key)) {
	dec(e);
	if(lastNode != null)
	  lastNode.nextNode = e.nextNode;
	else
	  this.data[loc] = e.nextNode;
	return e.getValue();
      }
      lastNode = e;
    }
    return null;    
  }
  public void clear() {
    for(int idx = 0; idx < data.length; ++idx) {
      for(HBNode lf = data[idx]; lf != null; lf = lf.nextNode) {
	dec(lf);
      }
    }
    length = 0;
    Arrays.fill(data, null);
  }
  public Object reduce(IFn rfn, Object acc) {
    final int l = data.length;
    for(int idx = 0; idx < l; ++idx) {
      for(HBNode e = this.data[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, e);
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;
  }
  public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				  ParallelOptions options ) {
    return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this.entrySet(), options);
  }
  @SuppressWarnings("unchecked")
  public void replaceAll(BiFunction bfn) {
    final int l = data.length;
    for(int idx = 0; idx < l; ++idx) {
      HBNode lastNode = null;
      for(HBNode e = this.data[idx]; e != null; e = e.nextNode) {
	Object newv = bfn.apply(e.k, e.v);
	if(newv != null) {
	  e.setValue(newv);
	  lastNode = e;
	}
	else {
	  dec(e);
	  if(lastNode != null) {
	    lastNode.nextNode = e.nextNode;
	  } else {
	    data[idx] = e.nextNode;
	  }
	}
      }
    }
  }
  @SuppressWarnings("unchecked")
  public HashMap union(BitmapTrieCommon.MapSet o, BiFunction bfn) {
    if(!(o instanceof HashMap))
      throw new RuntimeException("Accelerated union must have same type on both sides");
    HashMap other = (HashMap)o;
    HashMap rv = shallowClone();
    final HBNode[] od = other.data;
    final int nod = od.length;
    HBNode[] rvd = rv.data;
    int mask = rv.mask;
    for (int idx = 0; idx < nod; ++idx) {
      for(HBNode lf = od[idx]; lf != null; lf = lf.nextNode) {
	final int rvidx = lf.hashcode & mask;
	final Object k = lf.k;
	HBNode init = rvd[rvidx], e = init;
	for(;e != null && !(e.k==k || equals(e.k, k)); e = e.nextNode);
	if(e != null) {
	  rvd[rvidx] = init.assoc(rv, lf.k, lf.hashcode, bfn.apply(e.v, lf.v));
	}
	else {
	  if(init != null)
	    rvd[rvidx] = init.assoc(rv, lf.k, lf.hashcode, lf.v);
	  else
	    rvd[rvidx] = rv.newNode(k, lf.hashcode, lf.v);
	  rv.checkResize(null);
	  mask = rv.mask;
	  rvd = rv.data;
	}
      }
    }
    return rv;
  }
  @SuppressWarnings("unchecked")
  public HashMap intersection(BitmapTrieCommon.MapSet o, BiFunction bfn) {
    if(!(o instanceof HashMap))
      throw new RuntimeException("Accelerated union must have same type on both sides");
    HashMap other = (HashMap)o;
    HashMap rv = shallowClone();
    final HBNode[] od = other.data;
    final int omask = other.mask;
    final HBNode[] rvd = rv.data;
    final int ne = rvd.length;
    for (int idx = 0; idx < ne; ++idx) {
      HBNode lf = rvd[idx];
      while(lf != null) {
	final HBNode curlf = lf;
	lf = lf.nextNode;
	final int oidx = curlf.hashcode & omask;
	HBNode e = od[oidx];
	final Object k = curlf.k;
	for(;e != null && !(e.k==k || equals(e.k, k)); e = e.nextNode);
	// System.out.println("curlf.k: " + String.valueOf(curlf.k) + " found: " + String.valueOf(e != null));
	rvd[idx] = (e != null)
	  ? rvd[idx].assoc(rv, e.k, e.hashcode, bfn.apply(curlf.v, e.v))
	  : rvd[idx].dissoc(rv, curlf.k);
	// System.out.println("rvidx: " + String.valueOf(rvd[idx]) + ":" + String.valueOf(rvd[idx] != null ? rvd[idx].k : null));
      }
    }
    return new PersistentHashMap(rv);
  }
  @SuppressWarnings("unchecked")
  public PersistentHashMap difference(BitmapTrieCommon.MapSet o) {
    if(!(o instanceof HashMap))
      throw new RuntimeException("Accelerated union must have same type on both sides");
    HashMap other = (HashMap)o;
    HashMap rv = shallowClone();
    final HBNode[] od = other.data;
    final int nod = od.length;
    final HBNode[] rvd = rv.data;
    final int mask = rv.mask;
    for (int idx = 0; idx < nod; ++idx) {
      for(HBNode lf = od[idx]; lf != null; lf = lf.nextNode) {
	final int rvidx = lf.hashcode & mask;
	final Object k = lf.k;
	HBNode e = rvd[rvidx];
	for(;e != null && !(e.k==k || equals(e.k, k)); e = e.nextNode);
	if(e != null) {
	  rvd[rvidx] = rvd[rvidx].dissoc(rv, e.k);
	}
      }
    }
    return new PersistentHashMap(rv);
  }
  public IPersistentMap meta() { return meta; }
  static class HTIter implements Iterator {
    final HBNode[] d;
    final Function<Map.Entry,Object> fn;
    HBNode l;
    int idx;
    final int dlen;
    HTIter(HBNode[] data, Function<Map.Entry,Object> fn) {
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
      HBNode rv = l;
      advance();
      return fn.apply(rv);
    }
  }
  public Iterator iterator(Function<Map.Entry,Object> leafFn) {
    return new HTIter(this.data, leafFn);
  }
  static class HTSpliterator implements Spliterator, ITypedReduce {
    final HBNode[] d;
    final Function<Map.Entry,Object> fn;
    int sidx;
    int eidx;
    int estimateSize;
    HBNode l;
    public HTSpliterator(HBNode[] d, int len, Function<Map.Entry,Object> fn) {
      this.d = d;
      this.fn = fn;
      this.sidx = 0;
      this.eidx = d.length;
      this.estimateSize = len;
      this.l = null;
    }
    public HTSpliterator(HBNode[] d, int sidx, int eidx, int es, Function<Map.Entry,Object> fn) {
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
	final HBNode ll = this.d[sidx];
	if(ll != null) {
	  c.accept(this.fn.apply(ll));
	  this.l = ll.nextNode;
	  return true;
	}
      }
      return false;
    }
    public Object reduce(IFn rfn, Object acc) {
      final HBNode[] dd = this.d;
      final int ee = this.eidx;
      final Function<Map.Entry,Object> ffn = this.fn;
      for(int idx = sidx; idx < ee; ++idx) {
	for(HBNode e = dd[idx]; e != null; e = e.nextNode) {
	  acc = rfn.invoke(acc, ffn.apply(e));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
      }
      return acc;
    }
  }
  public Spliterator spliterator(Function<Map.Entry,Object> leafFn) { return new HTSpliterator(this.data, this.length, leafFn); }
}

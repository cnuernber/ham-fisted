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


public class LongHashMap extends LongHashBase implements IMap, MapSetOps, UpdateValues {
  Set keySet = null;
  public LongHashMap(float loadFactor, int initialCapacity,
		 int length, LongHashNode[] data,
		 IPersistentMap meta) {
    super(loadFactor, initialCapacity, length, data, meta);
  }
  public LongHashMap() {
    this(0.75f, 0, 0, null, null);
  }
  public LongHashMap(IPersistentMap m) {
    this(0.75f, 0, 0, null, m);
  }
  public LongHashMap(LongHashMap other, IPersistentMap m) {
    super(other, m);
  }

  public LongHashMap shallowClone() {
    return new LongHashMap(loadFactor, capacity, length, data.clone(), meta);
  }
  public LongHashMap clone() {
    final int l = data.length;
    LongHashNode[] newData = new LongHashNode[l];
    LongHashMap retval = new LongHashMap(loadFactor, capacity, length, newData, meta);
    for(int idx = 0; idx < l; ++idx) {
      LongHashNode orig = data[idx];
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
  public Object put(Object kk, Object val) {
    long key = Casts.longCast(kk);
    final int hc = hash(key);
    final int idx = hc & this.mask;
    LongHashNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(LongHashNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(e.k == key || equals(e.k, key)) {
	Object rv = e.v;
	e.v = val;
	modify(e);
	return rv;
      }
    }
    LongHashNode lf = newNode(key,hc,val);
    if(lastNode != null) {
      lastNode.nextNode = lf;
    } else {
      data[idx] = lf;
    }
    return checkResize(null);
  }
  public void putAll(Map other) {
    LongHashNode[] d = data;
    int mask = this.mask;
    for(Object o: other.entrySet()) {
      Map.Entry ee = (Map.Entry)o;
      long k = Casts.longCast(ee.getKey());
      int hashcode = hash(k);
      int idx = hashcode & mask;
      LongHashNode e;
      for(e = d[idx]; e != null && !(k == e.k); e = e.nextNode);
      if(e != null) {
	e.v = ee.getValue();
      }
      else {
	LongHashNode n = newNode(k, hashcode, ee.getValue());
	n.nextNode = d[idx];
	d[idx] = n;
	checkResize(null);
	d = data;
	mask = this.mask;
      }
    }
  }
  public Object getOrDefault(Object kk, Object dv) {
    if(kk instanceof Number) {
      long key = Casts.longCast(kk);
      for(LongHashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
	if(e.k == key)
	  return e.v;
      }
    }
    return dv;
  }
  public Object get(Object kk) {
    if(kk instanceof Number) {
      long key = Casts.longCast(kk);
      for(LongHashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
	if(e.k == key)
	  return e.v;
      }
    }
    return null;
  }
  public IMapEntry entryAt(Object kk) {
    if(kk instanceof Number) {
      long key = Casts.longCast(kk);
      for(LongHashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
	if(e.k == key)
	  return MapEntry.create(e.k, e.v);
      }
    }
    return null;
  }
  public boolean containsKey(Object key) {
    return containsNodeKey(key);
  }
  @SuppressWarnings("unchecked")
  public Object compute(Object kk, BiFunction bfn) {
    long k = Casts.longCast(kk);
    final int hash = hash(k);
    final LongHashNode[] d = this.data;
    final int idx = hash & this.mask;
    LongHashNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    Object newV = bfn.apply(k, e == null ? null : e.v);
    if(e != null) {
      if(newV != null) {
	e.v = newV;
	modify(e);
      }
      else
	remove(k, null);
    } else if(newV != null) {
      LongHashNode nn = newNode(k, hash, newV);
      if(ee != null)
	ee.nextNode = nn;
      else
	d[idx] = nn;
      checkResize(null);
    }
    return newV;
  }
  @SuppressWarnings("unchecked")
  public Object computeIfAbsent(Object kk, Function afn) {
    long k = Casts.longCast(kk);
    final int hash = hash(k);
    final LongHashNode[] d = this.data;
    final int idx = hash & this.mask;
    LongHashNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    if(e != null) {
      return e.v;
    } else {
      final Object newv = afn.apply(k);
      if(newv != null) {
	LongHashNode nn = newNode(k, hash, newv);
	if(ee != null)
	  ee.nextNode = nn;
	else
	  d[idx] = nn;
	checkResize(null);
      }
      return newv;
    }
  }
  public Object remove(Object kk) {
    long key = Casts.longCast(kk);
    int loc = hash(key) & this.mask;
    LongHashNode lastNode = null;
    for(LongHashNode e = this.data[loc]; e != null; e = e.nextNode) {
      if(e.k == key) {
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
  public Object reduce(IFn rfn, Object acc) {
    final int l = data.length;
    LongHashNode[] d = data;
    for(int idx = 0; idx < l; ++idx) {
      for(LongHashNode e = d[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, e);
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;
  }
  public Object kvreduce(IFn rfn, Object acc) {
    final int l = data.length;
    LongHashNode[] d = data;
    for(int idx = 0; idx < l; ++idx) {
      for(LongHashNode e = d[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, e.k, e.v);
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
      LongHashNode lastNode = null;
      for(LongHashNode e = this.data[idx]; e != null; e = e.nextNode) {
	Object newv = bfn.apply(e.k, e.v);
	if(newv != null) {
	  e.v = newv;
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

  public Set keySet() {
    if(this.keySet  == null )
      this.keySet = IMap.super.keySet();
    return this.keySet;
   }

  @SuppressWarnings("unchecked")
  public static LongHashMap union(LongHashMap rv, Map o, BiFunction bfn) {
    LongHashNode[] rvd = rv.data;
    int mask = rv.mask;
    for(Object ee : o.entrySet()) {
      Map.Entry lf = (Map.Entry)ee;
      final long k = Casts.longCast(lf.getKey());
      final int hashcode = rv.hash(k);
      final int rvidx = hashcode & mask;
      LongHashNode init = rvd[rvidx], e = init;
      for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
      if(e != null) {
	rvd[rvidx] = init.assoc(rv, k, hashcode, bfn.apply(e.v, lf.getValue()));
      }
      else {
	if(init != null)
	  rvd[rvidx] = init.assoc(rv, k, hashcode, lf.getValue());
	else
	  rvd[rvidx] = rv.newNode(k, hashcode, lf.getValue());
	rv.checkResize(null);
	mask = rv.mask;
	rvd = rv.data;
      }
    }
    return rv;
  }

  @SuppressWarnings("unchecked")
  public LongHashMap union(Map o, BiFunction bfn) {
    return union(this, o, bfn);
  }
  @SuppressWarnings("unchecked")
  static LongHashMap intersection(LongHashMap rv, Map o, BiFunction bfn) {
    final LongHashNode[] rvd = rv.data;
    final int ne = rvd.length;
    for (int idx = 0; idx < ne; ++idx) {
      LongHashNode lf = rvd[idx];
      while(lf != null) {
	final LongHashNode curlf = lf;
	lf = lf.nextNode;
	final Object v = o.get(curlf.k);
	rvd[idx] = (v != null)
	  ? rvd[idx].assoc(rv, curlf.k, curlf.hashcode, bfn.apply(curlf.v, v))
	  : rvd[idx].dissoc(rv, curlf.k);
      }
    }
    return rv;
  }


  public LongHashMap intersection(Map o, BiFunction bfn) {
    return intersection(this, o, bfn);
  }

  public static LongHashMap intersection(LongHashMap rv, Set o) {
    final LongHashNode[] rvd = rv.data;
    final int ne = rvd.length;
    for (int idx = 0; idx < ne; ++idx) {
      LongHashNode lf = rvd[idx];
      while(lf != null) {
	final LongHashNode curlf = lf;
	final long k = curlf.k;
	lf = lf.nextNode;
	if(!o.contains(k))
	  rvd[idx] = rvd[idx].dissoc(rv,k);
      }
    }
    return rv;
  }
  public LongHashMap intersection(Set o) {
    return intersection(this, o);
  }


  @SuppressWarnings("unchecked")
  static LongHashMap difference(LongHashMap rv, Collection o) {
    final LongHashNode[] rvd = rv.data;
    final int mask = rv.mask;
    for (Object kk : o) {
      long k = Casts.longCast(kk);
      final int hashcode = rv.hash(k);
      final int rvidx = hashcode & mask;
      LongHashNode e = rvd[rvidx];
      for(;e != null && !(e.k==k); e = e.nextNode);
      if(e != null) {
	rvd[rvidx] = rvd[rvidx].dissoc(rv, e.k);
      }
    }
    return rv;
  }

  public LongHashMap difference(Collection o) {
    return difference(this, o);
  }

  @SuppressWarnings("unchecked")
  static LongHashMap updateValues(LongHashMap rv, BiFunction valueMap) {
    final LongHashNode[] d = rv.data;
    final int nl = d.length;
    for(int idx = 0; idx < nl; ++idx) {
      LongHashNode lf = d[idx];
      while(lf != null) {
	LongHashNode cur = lf;
	lf = lf.nextNode;
	Object newv = valueMap.apply(cur.k, cur.v);
	d[idx] = newv == null ? d[idx].dissoc(rv, cur.k) :
	  d[idx].assoc(rv, cur.k, cur.hashcode, newv);
      }
    }
    return rv;
  }
  public LongHashMap updateValues(BiFunction valueMap) {
    return updateValues(this, valueMap);
  }
  @SuppressWarnings("unchecked")
  static LongHashMap updateValue(LongHashMap rv, Object kk, Function fn) {
    long k = Casts.longCast(kk);
    final int hc = rv.hash(k);
    final int idx = hc & rv.mask;
    final LongHashNode[] data = rv.data;
    LongHashNode e = data[idx];
    for(; e != null && !((e.k == k)); e = e.nextNode);
    final Object newv = e != null ? fn.apply(e.v) : fn.apply(null);
    data[idx] = newv == null ? data[idx].dissoc(rv, k) : data[idx].assoc(rv, k, hc, newv);
    if(newv != null && e == null) rv.checkResize(null);
    return rv;
  }

  public LongHashMap updateValue(Object k, Function fn) {
    return updateValue(this, fn);
  }

  public Iterator iterator(Function<Map.Entry,Object> leafFn) {
    return new HTIter(this.data, leafFn);
  }
  public Spliterator spliterator(Function<Map.Entry,Object> leafFn) { return new HTSpliterator(this.data, this.length, leafFn); }
}

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
import clojure.lang.IReduceInit;


public class HashMap extends HashBase implements IMap, MapSetOps, UpdateValues {
  Set keySet;
  public HashMap(float loadFactor, int initialCapacity,
		  int length, HashNode[] data,
		  IPersistentMap meta) {
    super(loadFactor, initialCapacity, length, data, meta);
  }
  public HashMap() {
    this(0.75f, 0, 0, null, null);
  }
  public HashMap(IPersistentMap m) {
    this(0.75f, 0, 0, null, m);
  }
  public HashMap(HashMap other, IPersistentMap m) {
    super(other, m);
  }

  public HashMap shallowClone() {
    return new HashMap(loadFactor, capacity, length, data.clone(), meta);
  }
  public HashMap clone() {
    final int l = data.length;
    HashNode[] newData = new HashNode[l];
    HashMap retval = new HashMap(loadFactor, capacity, length, newData, meta);
    for(int idx = 0; idx < l; ++idx) {
      HashNode orig = data[idx];
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
  public Object put(Object key, Object val) {
    final int hc = hash(key);
    final int idx = hc & mask;
    final HashNode init = data[idx];
    if(init != null) {
      HashNode e = init;
      do {
	if(e.k == key || equals(e.k, key)) {
	  Object rv = e.v;
	  e.v = val;
	  modify(e);
	  return rv;
	}
	e = e.nextNode;
      } while(e != null);
    }
    HashNode lf = newNode(key,hc,val);
    lf.nextNode = init;
    data[idx] = lf;
    return checkResize(null);
  }
  public void putAll(Map other) {
    HashNode[] d = data;
    int mask = this.mask;
    for(Object o: other.entrySet()) {
      Map.Entry ee = (Map.Entry)o;
      Object k = ee.getKey();
      int hashcode = hash(k);
      int idx = hashcode & mask;
      HashNode e;
      for(e = d[idx]; e != null && !(k == e.k || equals(k,e.k)); e = e.nextNode);
      if(e != null) {
	e.v = ee.getValue();
      }
      else {
	HashNode n = newNode(k, hashcode, ee.getValue());
	n.nextNode = d[idx];
	d[idx] = n;
	checkResize(null);
	d = data;
	mask = this.mask;
      }
    }
  }
  public Object getOrDefault(Object key, Object dv) {
    for(HashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return e.v;
    }
    return dv;
  }
  public Object get(Object key) {
    for(HashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return e.v;
    }
    return null;
  }
  public IMapEntry entryAt(Object key) {
    for(HashNode e = this.data[hash(key) & this.mask]; e != null; e = e.nextNode) {
      Object k;
      if((k = e.k) == key || equals(k, key))
	return MapEntry.create(e.k, e.v);
    }
    return null;
  }
  public boolean containsKey(Object key) {
    return containsNodeKey(key);
  }
  @SuppressWarnings("unchecked")
  public Object compute(Object k, BiFunction bfn) {
    final int hash = hash(k);
    final HashNode[] d = this.data;
    final int idx = hash & this.mask;
    HashNode e = d[idx], ee = null;
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
      HashNode nn = newNode(k, hash, newV);
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
    final HashNode[] d = this.data;
    final int idx = hash & this.mask;
    HashNode e = d[idx], ee = null;
    for(; e != null && !(e.k == k || equals(e.k, k)); e = e.nextNode) {
      ee = e;
    }
    if(e != null) {
      return e.v;
    } else {
      final Object newv = afn.apply(k);
      if(newv != null) {
	HashNode nn = newNode(k, hash, newv);
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
    HashNode lastNode = null;
    int loc = hash(key) & this.mask;
    for(HashNode e = this.data[loc]; e != null; e = e.nextNode) {
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
  public Object reduce(IFn rfn, Object acc) {
    final int l = data.length;
    for(int idx = 0; idx < l; ++idx) {
      for(HashNode e = this.data[idx]; e != null; e = e.nextNode) {
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
      HashNode lastNode = null;
      for(HashNode e = this.data[idx]; e != null; e = e.nextNode) {
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
  public static HashMap hashMapUnion(HashMap rv, HashMap om, BiFunction bfn) {
    final HashNode[] od = om.data;
    final int l = od.length;
    HashNode[] rvd = rv.data;
    int mask = rv.mask;
    for(int idx = 0; idx < l; ++idx) {
      for(HashNode lf = od[idx]; lf != null; lf = lf.nextNode) {
	final Object k = lf.k;
	final int hashcode = lf.hashcode;
	final int rvidx = hashcode & mask;
	HashNode init = rvd[rvidx], e = init;
	final Object v = lf.v;
	for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
	if(e != null) {
	  rvd[rvidx] = init.assoc(rv, k, hashcode, bfn.apply(e.v, v));
	}
	else {
	  if(init != null)
	    rvd[rvidx] = init.assoc(rv, k, hashcode, v);
	  else
	    rvd[rvidx] = rv.newNode(k, hashcode, v);
	  rv.checkResize(null);
	  mask = rv.mask;
	  rvd = rv.data;
	}
      }
    }
    return rv;
  }
  @SuppressWarnings("unchecked")
  public static HashMap reduceUnion(HashMap rv, IReduceInit o, BiFunction bfn) {
    return (HashMap)o.reduce(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  Map.Entry lf = (Map.Entry)v;
	  final Object k = lf.getKey();
	  final int hashcode = rv.hash(k);
	  final int rvidx = hashcode & rv.mask;
	  final HashNode[] rvd = rv.data;
	  HashNode init = rvd[rvidx], e = init;
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
	  }
	  return rv;
	}
      }, rv);
  }

  @SuppressWarnings("unchecked")
  public static HashMap entrySetUnion(HashMap rv, Map o, BiFunction bfn) {
    int mask = rv.mask;
    HashNode[] rvd = rv.data;
    for(Object ee : o.entrySet()) {
      Map.Entry lf = (Map.Entry)ee;
      final Object k = lf.getKey();
      final int hashcode = rv.hash(k);
      final int rvidx = hashcode & mask;
      HashNode init = rvd[rvidx], e = init;
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

  public static HashMap union(HashMap rv, Map o, BiFunction bfn) {
    if(o instanceof HashMap) {
      return hashMapUnion(rv, (HashMap)o, bfn);
    } else if (o instanceof IReduceInit) {
      return reduceUnion(rv, (IReduceInit)o, bfn);
    }
    else {
      return entrySetUnion(rv, o, bfn);
    }
  }

  @SuppressWarnings("unchecked")
  public HashMap union(Map o, BiFunction bfn) {
    return union(this, o, bfn);
  }
  @SuppressWarnings("unchecked")
  static HashMap intersection(HashMap rv, Map o, BiFunction bfn) {
    final HashNode[] rvd = rv.data;
    final int ne = rvd.length;
    for (int idx = 0; idx < ne; ++idx) {
      HashNode lf = rvd[idx];
      while(lf != null) {
	final HashNode curlf = lf;
	lf = lf.nextNode;
	final Object v = o.get(curlf.k);
	rvd[idx] = (v != null)
	  ? rvd[idx].assoc(rv, curlf.k, curlf.hashcode, bfn.apply(curlf.v, v))
	  : rvd[idx].dissoc(rv, curlf.k);
      }
    }
    return rv;
  }


  public HashMap intersection(Map o, BiFunction bfn) {
    return intersection(this, o, bfn);
  }

  public static HashMap intersection(HashMap rv, Set o) {
    final HashNode[] rvd = rv.data;
    final int ne = rvd.length;
    for (int idx = 0; idx < ne; ++idx) {
      HashNode lf = rvd[idx];
      while(lf != null) {
	final HashNode curlf = lf;
	final Object k = curlf.k;
	lf = lf.nextNode;
	if(!o.contains(k))
	  rvd[idx] = rvd[idx].dissoc(rv,k);
      }
    }
    return rv;
  }
  public HashMap intersection(Set o) {
    return intersection(this, o);
  }


  @SuppressWarnings("unchecked")
  static HashMap difference(HashMap rv, Collection o) {
    final HashNode[] rvd = rv.data;
    final int mask = rv.mask;
    for (Object k : o) {
      final int hashcode = rv.hash(k);
      final int rvidx = hashcode & mask;
      HashNode e = rvd[rvidx];
      for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
      if(e != null) {
	rvd[rvidx] = rvd[rvidx].dissoc(rv,k);
      }
    }
    return rv;
  }

  public HashMap difference(Collection o) {
    return difference(this, o);
  }

  @SuppressWarnings("unchecked")
  static HashMap updateValues(HashMap rv, BiFunction valueMap) {
    final HashNode[] d = rv.data;
    final int nl = d.length;
    for(int idx = 0; idx < nl; ++idx) {
      HashNode lf = d[idx];
      while(lf != null) {
	HashNode cur = lf;
	lf = lf.nextNode;
	Object newv = valueMap.apply(cur.k, cur.v);
	d[idx] = newv == null ? d[idx].dissoc(rv, cur.k) :
	  d[idx].assoc(rv, cur.k, cur.hashcode, newv);
      }
    }
    return rv;
  }
  public HashMap updateValues(BiFunction valueMap) {
    return updateValues(this, valueMap);
  }
  @SuppressWarnings("unchecked")
  static HashMap updateValue(HashMap rv, Object k, Function fn) {
    final int hc = rv.hash(k);
    final int idx = hc & rv.mask;
    final HashNode[] data = rv.data;
    HashNode e = data[idx];
    for(; e != null && !((e.k == k) || rv.equals(e.k, k)); e = e.nextNode);
    final Object newv = e != null ? fn.apply(e.v) : fn.apply(null);
    data[idx] = newv == null ? data[idx].dissoc(rv, k) : data[idx].assoc(rv, k, hc, newv);
    if(newv != null && e == null) rv.checkResize(null);
    return rv;
  }

  public HashMap updateValue(Object k, Function fn) {
    return updateValue(this, fn);
  }

  public Iterator iterator(Function<Map.Entry,Object> leafFn) {
    return new HTIter(this.data, leafFn);
  }
  public Spliterator spliterator(Function<Map.Entry,Object> leafFn) { return new HTSpliterator(this.data, this.length, leafFn); }
}

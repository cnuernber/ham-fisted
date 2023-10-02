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


public class HashMap extends HashBase implements IMap, MapSetOps {
  ROHashSet keySet;
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
    final int idx = hc & this.mask;
    HashNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(HashNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(e.k == key || equals(e.k, key)) {
	Object rv = e.v;
	e.v = val;
	modify(e);
	return rv;
      }
    }
    HashNode lf = newNode(key,hc,val);
    if(lastNode != null) {
      lastNode.nextNode = lf;
    } else {
      data[idx] = lf;
    }
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

  @SuppressWarnings("unchecked")
  public static HashMap union(HashMap rv, Map o, BiFunction bfn) {
    HashNode[] rvd = rv.data;
    int mask = rv.mask;
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

  public HashSet keySet() {
    if(this.keySet  == null )
      this.keySet = new PersistentHashSet(this);
    return this.keySet;
  }

  @SuppressWarnings("unchecked")
  public HashMap union(Map o, BiFunction bfn) {
    return new PersistentHashMap(union(shallowClone(), o, bfn));
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
    return new PersistentHashMap(intersection(shallowClone(), o, bfn));
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
    return new PersistentHashMap(intersection(shallowClone(), o));
  }


  @SuppressWarnings("unchecked")
  static HashMap difference(HashMap rv, Set o) {
    final HashNode[] rvd = rv.data;
    final int mask = rv.mask;
    for (Object k : o) {
      final int hashcode = rv.hash(k);
      final int rvidx = hashcode & mask;
      HashNode e = rvd[rvidx];
      for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
      if(e != null) {
	rvd[rvidx] = rvd[rvidx].dissoc(rv, e.k);
      }
    }
    return rv;
  }

  public HashMap difference(Set o) {
    return difference(shallowClone(), o);
  }

  public Iterator iterator(Function<Map.Entry,Object> leafFn) {
    return new HTIter(this.data, leafFn);
  }
  public Spliterator spliterator(Function<Map.Entry,Object> leafFn) { return new HTSpliterator(this.data, this.length, leafFn); }
}

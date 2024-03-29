package ham_fisted;


import clojure.lang.IPersistentMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Collection;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.lang.IReduceInit;


public class HashSet extends HashBase implements ISet, SetOps {
  public static final Object VALUE = new Object();
  public HashSet(float loadFactor, int initialCapacity,
		 int length, HashNode[] data,
		 IPersistentMap meta) {
    super(loadFactor, initialCapacity, length, data, meta);
  }
  public HashSet() {
    this(0.75f, 0, 0, null, null);
  }
  public HashSet(IPersistentMap m) {
    this(0.75f, 0, 0, null, m);
  }
  public HashSet(HashBase other, IPersistentMap m) {
    super(other, m);
  }
  public HashSet(HashBase other) {
    super(other, null);
  }
  public HashSet shallowClone() {
    return new HashSet(loadFactor, capacity, length, data.clone(), meta);
  }
  public HashSet clone() {
    final int l = data.length;
    HashNode[] newData = new HashNode[l];
    HashSet retval = new HashSet(loadFactor, capacity, length, newData, meta);
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
    return CljHash.setHashcode(this);
  }
  public  boolean equals(Object o) {
    return equiv(o);
  }
  public boolean equiv(Object o) {
    return CljHash.setEquiv(this, o);
  }
  public boolean add(Object key) {
    final int hc = hash(key);
    final int idx = hc & this.mask;
    HashNode lastNode = null;
    //Avoid unneeded calls to both equals and checkResize
    for(HashNode e = this.data[idx]; e != null; e = e.nextNode) {
      lastNode = e;
      if(e.k == key || equals(e.k, key))
	return false;
    }
    HashNode lf = newNode(key,hc,VALUE);
    if(lastNode != null) {
      lastNode.nextNode = lf;
    } else {
      data[idx] = lf;
    }
    checkResize(null);
    return true;
  }

  public void addAllReduceGeneric(IReduceInit r) {
    final HashBase rv = this;
    r.reduce(new IFnDef() {
	public Object invoke(Object acc, Object k) {
	  final int hc = rv.hash(k);
	  final int idx = hc & rv.mask;
	  final HashNode[] d = rv.data;
	  HashNode e = d[idx];
	  for(; e != null && !(k == e.k || rv.equals(k,e.k)); e = e.nextNode);
	  if(e == null) {
	    final HashNode lf = rv.newNode(k, hc, VALUE);
	    lf.nextNode = d[idx];
	    d[idx] = lf;
	    rv.checkResize(null);
	  }
	  return this;
	}
      }, this);
  }
  public boolean addAll(Collection c) {
    int sz = length;
    if(c instanceof HashSet) {
      HashSet other = (HashSet) c;
      HashNode[] d = data;
      int m = mask;
      final HashNode[] od = other.data;
      final int odl = od.length;
      for (int idx = 0; idx < odl; ++idx) {
	for (HashNode e = od[idx]; e != null; e = e.nextNode) {
	  // Its tempting to use the hashcode here but we don't know if
	  // the other hashset has overridden hash or not.
	  int hc = hash(e.k);
	  int didx = hc & m;
	  HashNode ee = d[didx];
	  for(; ee != null && !(ee.k == e.k || equals(ee.k, e.k)); ee = ee.nextNode);
	  if( ee == null) {
	    HashNode n = newNode(e.k, hc, e.v);
	    n.nextNode = d[didx];
	    d[didx] = n;
	    checkResize(null);
	    d = data;
	    m = mask;
	  }
	}
      }
    } else if(c instanceof IReduceInit) {
      addAllReduceGeneric((IReduceInit)c);
    } else {
      HashNode[] d = data;
      int m = mask;
      for(Object k: c) {
	int hc = hash(k);
	int idx = hc & m;
	HashNode e = d[idx];
	for(; e != null && !(k == e.k || equals(k,e.k)); e = e.nextNode);
	if(e == null) {
	  HashNode lf = newNode(k, hc, VALUE);
	  lf.nextNode = d[idx];
	  d[idx] = lf;
	  checkResize(null);
	  d = data;
	  m = mask;
	}
      }
    }
    return sz != length;
  }
  public boolean remove(Object key) {
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
	return true;
      }
      lastNode = e;
    }
    return false;
  }
  public boolean contains(Object key) {
    return containsNodeKey(key);
  }
  public Iterator iterator() {
    return new HTIter(this.data, (e)->e.getKey());
  }
  public Spliterator spliterator() {
    return new HTSpliterator(this.data, this.length, (e)->e.getKey());
  }
  public Object reduce(IFn rfn, Object acc) {
    final int l = data.length;
    for(int idx = 0; idx < l; ++idx) {
      for(HashNode e = this.data[idx]; e != null; e = e.nextNode) {
	acc = rfn.invoke(acc, e.k);
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;
  }

  public static HashSet union(HashSet rv, Collection rhs) {
    if(rhs instanceof IReduceInit) {
      return (HashSet)((IReduceInit)rhs).reduce(new IFnDef() {
	  public Object invoke(Object acc, Object k) {
	    final int hashcode = rv.hash(k);
	    final int rvidx = hashcode & rv.mask;
	    HashNode init = rv.data[rvidx], e = init;
	    for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
	    if(e == null) {
	      if(init != null)
		rv.data[rvidx] = init.assoc(rv, k, hashcode, VALUE);
	      else
		rv.data[rvidx] = rv.newNode(k, hashcode, VALUE);
	      rv.checkResize(null);
	    }
	    return rv;
	  }
	}, rv);
    } else {
      HashNode[] rvd = rv.data;
      int mask = rv.mask;
      for(Object k: rhs) {
	final int hashcode = rv.hash(k);
	final int rvidx = hashcode & mask;
	HashNode init = rvd[rvidx], e = init;
	for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
	if(e == null) {
	  if(init != null)
	    rvd[rvidx] = init.assoc(rv, k, hashcode, VALUE);
	  else
	    rvd[rvidx] = rv.newNode(k, hashcode, VALUE);
	  rv.checkResize(null);
	  mask = rv.mask;
	  rvd = rv.data;
	}
      }
    }
    return rv;
  }

  public HashSet union(Collection rhs) {
    return union(this, rhs);
  }

  public static HashSet intersection(HashSet rv, Set rhs) {
    final HashNode[] rvd = rv.data;
    final int ne = rvd.length;
    for (int idx = 0; idx < ne; ++idx) {
      HashNode lf = rvd[idx];
      while(lf != null) {
	final Object k = lf.k;
	lf = lf.nextNode;
	if(!rhs.contains(k))
	  rvd[idx] = rvd[idx].dissoc(rv, k);
      }
    }
    return rv;
  }

  public HashSet intersection(Set rhs) {
    return intersection(this, rhs);
  }

  public static HashSet difference(HashSet rv, Set rhs) {
    final HashNode[] rvd = rv.data;
    final int mask = rv.mask;
    if(rhs instanceof IReduceInit) {
      return (HashSet)((IReduceInit)rhs).reduce(new IFnDef() {
	  public Object invoke(Object acc, Object k) {
	    final int hashcode = rv.hash(k);
	    final int rvidx = hashcode & mask;
	    HashNode e = rvd[rvidx];
	    for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
	    if(e != null) {
	      rvd[rvidx] = rvd[rvidx].dissoc(rv, e.k);
	    }
	    return rv;
	  }
	}, rv);
    } else {
      for (Object k : rhs) {
	final int hashcode = rv.hash(k);
	final int rvidx = hashcode & mask;
	HashNode e = rvd[rvidx];
	for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
	if(e != null) {
	  rvd[rvidx] = rvd[rvidx].dissoc(rv, e.k);
	}
      }
    }
    return rv;
  }

  public HashSet difference(Set rhs) {
    HashSet rv = shallowClone();
    final HashNode[] rvd = rv.data;
    final int mask = rv.mask;
    for (Object k : rhs) {
      final int hashcode = rv.hash(k);
      final int rvidx = hashcode & mask;
      HashNode e = rvd[rvidx];
      for(;e != null && !(e.k==k || rv.equals(e.k, k)); e = e.nextNode);
      if(e != null) {
	rvd[rvidx] = rvd[rvidx].dissoc(rv, k);
      }
    }
    return rv;
  }
}

package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import static ham_fisted.IntegerOps.*;

import clojure.lang.Associative;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.IPersistentCollection;
import clojure.lang.IObj;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.ISeq;
import clojure.lang.RT;
import clojure.lang.IteratorSeq;
import clojure.lang.IObj;
import clojure.lang.IEditableCollection;
import clojure.lang.ITransientCollection;
import clojure.lang.IMapIterable;
import clojure.lang.IKVReduce;
import clojure.lang.IHashEq;
import clojure.lang.IFn;
import clojure.lang.IDeref;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicReference;


public class PersistentArrayMap
  implements IPersistentMap, Map, IObj, IEditableCollection, MapSet, BitmapTrieOwner,
	     IMapIterable, IKVReduce, IHashEq, ImmutValues {
  final HashProvider hp;
  final Object[] kvs;
  final int nElems;
  final IPersistentMap meta;
  HashMap<Object,Object> cachedTrie = null;
  public static final int MAX_SIZE = 8;

  public static final PersistentArrayMap EMPTY = new PersistentArrayMap(equalHashProvider);

  public PersistentArrayMap(HashProvider _hp) {
    hp = _hp;
    kvs = new Object[0];
    nElems = 0;
    meta = null;
  }
  PersistentArrayMap(HashProvider _hp, int _nElems, Object[] _kvs, IPersistentMap _meta) {
    hp = _hp;
    kvs = _kvs;
    nElems = _nElems;
    meta = _meta;
  }
  public PersistentArrayMap(HashProvider _hp, Object k, Object v, IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v };
    nElems = 1;
    meta = _meta;
  }

  public PersistentArrayMap(HashProvider _hp, Object k, Object v, Object k2, Object v2,
			    IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2 };
    nElems = 2;
    meta = _meta;
  }
  public PersistentArrayMap(HashProvider _hp, Object k, Object v, Object k2, Object v2,
			    Object k3, Object v3, IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2, k3, v3 };
    nElems = 3;
    meta = _meta;
  }

  public PersistentArrayMap(HashProvider _hp, Object k, Object v, Object k2, Object v2,
			    Object k3, Object v3, Object k4, Object v4,
			    IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2, k3, v3, k4, v4 };
    nElems = 4;
    meta = _meta;
  }

  public PersistentArrayMap(HashProvider _hp,
			    Object k, Object v, Object k2, Object v2,
			    Object k3, Object v3, Object k4, Object v4,
			    Object k5, Object v5,
			    IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2, k3, v3, k4, v4, k5, v5 };
    nElems = 5;
    meta = _meta;
  }

  public PersistentArrayMap(HashProvider _hp,
			    Object k, Object v, Object k2, Object v2,
			    Object k3, Object v3, Object k4, Object v4,
			    Object k5, Object v5, Object k6, Object v6,
			    IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6};
    nElems = 6;
    meta = _meta;
  }

  public PersistentArrayMap(HashProvider _hp,
			    Object k, Object v, Object k2, Object v2,
			    Object k3, Object v3, Object k4, Object v4,
			    Object k5, Object v5, Object k6, Object v6,
			    Object k7, Object v7,
			    IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7};
    nElems = 7;
    meta = _meta;
  }

  public PersistentArrayMap(HashProvider _hp,
			    Object k, Object v, Object k2, Object v2,
			    Object k3, Object v3, Object k4, Object v4,
			    Object k5, Object v5, Object k6, Object v6,
			    Object k7, Object v7, Object k8, Object v8,
			    IPersistentMap _meta) {
    hp = _hp;
    kvs = new Object[] { k, v, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8};
    nElems = 8;
    meta = _meta;
  }

  public static final boolean different(HashProvider hp, Object a, Object b) {
    return !hp.equals(a,b);
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(b,c));
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) ||
	     hp.equals(b,c) || hp.equals(b,d) ||
	     hp.equals(c,d));
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) ||
	     hp.equals(c,d) || hp.equals(c,e) ||
	     hp.equals(d,e)
	    );
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e, Object f) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) || hp.equals(a, f) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) || hp.equals(b, f) ||
	     hp.equals(c,d) || hp.equals(c,e) || hp.equals(c,f) ||
	     hp.equals(d,e) || hp.equals(d,f) ||
	     hp.equals(e,f)
	     );
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) || hp.equals(a,f) || hp.equals(a,g) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) || hp.equals(b,f) || hp.equals(b,g) ||
	     hp.equals(c,d) || hp.equals(c,e) || hp.equals(c,f) || hp.equals(c,g) ||
	     hp.equals(d,e) || hp.equals(d,f) || hp.equals(d,g) ||
	     hp.equals(e,f) || hp.equals(e,g) ||
	     hp.equals(f,g)
	     );
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) || hp.equals(a,f) || hp.equals(a,g) || hp.equals(a,h) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) || hp.equals(b,f) || hp.equals(b,g) || hp.equals(b,h) ||
	     hp.equals(c,d) || hp.equals(c,e) || hp.equals(c,f) || hp.equals(c,g) || hp.equals(c,h) ||
	     hp.equals(d,e) || hp.equals(d,f) || hp.equals(d,g) || hp.equals(d,h) ||
	     hp.equals(e,f) || hp.equals(e,g) || hp.equals(e,h) ||
	     hp.equals(f,g) || hp.equals(f,h) ||
	     hp.equals(g,h)
	     );
  }
  public final int count() { return nElems; }
  public static final int indexOf(HashProvider hp, int ne, Object[] kvs, Object key) {
    final int nne = ne*2;
    for (int idx = 0; idx < nne; idx += 2)
      if (hp.equals(kvs[idx], key))
	return idx;
    return -1;
  }
  final int indexOf(Object key) {
    return indexOf(hp, nElems, kvs, key);
  }
  static final HashMap<Object,Object> copyToTrie(HashProvider hp, int ne, Object[] kvs,
						 IPersistentMap meta, int newNe) {
    HashMap<Object,Object> retval = new HashMap<Object,Object>(hp, meta, newNe);
    retval.putAll(kvs);
    return retval;
  }
  public final IPersistentMap assoc(Object k, Object v) {
    final int ne = nElems;
    if (ne == 0)
      return new PersistentArrayMap(hp, k, v, meta);


    final Object[] data = kvs;
    final int idx = indexOf(hp, ne, data, k);
    if (idx == -1) {
      if (ne == MAX_SIZE) {
	final HashMap<Object,Object> retval = copyToTrie(hp, ne, data, meta, ne+1);
	retval.put(k,v);
	return retval.persistent();
      } else {
	switch(ne) {
	case 1: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      k, v, meta);
	case 2: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      data[2], data[3],
					      k, v, meta);
	case 3: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      data[2], data[3],
					      data[4], data[5],
					      k, v, meta);
	case 4: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      data[2], data[3],
					      data[4], data[5],
					      data[6], data[7],
					      k, v, meta);
	case 5: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      data[2], data[3],
					      data[4], data[5],
					      data[6], data[7],
					      data[8], data[9],
					      k, v, meta);
	case 6: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      data[2], data[3],
					      data[4], data[5],
					      data[6], data[7],
					      data[8], data[9],
					      data[10], data[11],
					      k, v, meta);
	case 7: return new PersistentArrayMap(hp,
					      data[0], data[1],
					      data[2], data[3],
					      data[4], data[5],
					      data[6], data[7],
					      data[8], data[9],
					      data[10], data[11],
					      data[12], data[13],
					      k, v, meta);
	}
	final int newNe = ne+1;
	final Object[] newData = Arrays.copyOf(data, newNe*2);
	final int nne = ne * 2;
	newData[nne] = k;
	newData[nne+1] = v;
	return new PersistentArrayMap(hp, newNe, newData, meta);
      }
    } else {
      final Object[] mdata = data.clone();
      mdata[idx] = k;
      mdata[idx+1] = v;
      return new PersistentArrayMap(hp, ne, mdata, meta);
    }
  }
  public final IPersistentMap assocEx(Object k, Object v) {
    IPersistentMap retval = assoc(k,v);
    if (retval.count() == count())
      throw new RuntimeException("Assoc with duplicate key: " + String.valueOf(k));
    return retval;
  }
  public final IPersistentMap without(Object k) {
    final int ne = nElems;
    final Object[] data = kvs;
    final int idx = indexOf(hp, ne, data, k);
    if (idx == -1)
      return this;
    if (ne == 1)
      return EMPTY;
    final int newNe = ne-1;
    final Object[] newObjs = Arrays.copyOf(data, newNe*2);
    final int nne = newNe*2;
    for (int iidx = idx; iidx < nne; iidx += 1)
      newObjs[iidx] = data[iidx+2];

    return new PersistentArrayMap(hp, newNe, newObjs, meta);
  }

  public final boolean containsKey(Object key) {
    return indexOf(key) != -1;
  }

  public final IMapEntry entryAt(Object key) {
    final int idx = indexOf(key);
    return idx == -1 ? null : new MapEntry(kvs[idx], kvs[idx+1]);
  }

  public final Object valAt(Object key) {
    final int idx = indexOf(key);
    return idx == -1 ? null : kvs[idx+1];
  }

  public final Object valAt(Object key, Object notFound) {
    final int idx = indexOf(key);
    return idx == -1 ? notFound : kvs[idx+1];
  }

  public final IPersistentCollection cons(Object o) {
    if(o instanceof Map.Entry) {
      Map.Entry e = (Map.Entry) o;
      return assoc(e.getKey(), e.getValue());
    }
    else if(o instanceof IPersistentVector) {
      IPersistentVector v = (IPersistentVector) o;
      if(v.count() != 2)
	throw new IllegalArgumentException("Vector arg to map conj must be a pair");
      return assoc(v.nth(0), v.nth(1));
    }

    HashMap<Object,Object> retval = copyToTrie(hp, nElems, kvs, meta, nElems);
    for(ISeq es = RT.seq(o); es != null; es = es.next()) {
      Map.Entry e = (Map.Entry) es.first();
      retval.put(e.getKey(), e.getValue());
    }
    return retval.persistent();
  }

  public final PersistentArrayMap empty() { return EMPTY; }

  public final boolean equiv(Object o) {
    return cachedMap().equals(o);
  }

  public final int hasheq() {
    return hashCode();
  }

  public final Object kvreduce(IFn f, Object init) {
    final int ne = nElems;
    final Object[] data = kvs;
    final int nne = ne *2;
    for (int idx = 0; idx < nne; idx += 2) {
      init = f.invoke(init, data[idx], data[idx+1]);
      if (RT.isReduced(init))
	return ((IDeref)init).deref();
    }
    return init;
  }

  public final ITransientCollection asTransient() {
    return copyToTrie(hp, nElems, kvs, meta, nElems);
  }

  public final ISeq seq() { return IteratorSeq.create(iterator()); }
  public final IPersistentMap meta() { return meta; }
  public final PersistentArrayMap withMeta(IPersistentMap newMeta) {
    if (Objects.equals(newMeta, meta))
      return this;
    return new PersistentArrayMap(hp, nElems, kvs, newMeta);
  }

  //Implement minimal map interface
  public final int size() { return nElems; }
  public final void clear() { throw new RuntimeException("Unimplemented"); }
  public final Object compute(Object k, BiFunction fn) {
    throw new RuntimeException("Unimplemented");
  }
  public final Object computeIfPresent(Object k, BiFunction fn) {
    throw new RuntimeException("Unimplemented");
  }
  public final Object computeIfAbsent(Object k, Function fn) {
    throw new RuntimeException("Unimplemented");
  }
  public final boolean containsValue(Object v) {
    final int ne = nElems;
    final Object[] data = kvs;
    final int nne = ne*2;
    for (int idx = 1; idx < nne; idx+=2) {
      if (Objects.equals(v, data[idx]))
	return true;
    }
    return false;
  }
  public boolean equals(Object o) {
    return cachedMap().equals(o);
  }
  public int hashCode() {
    return cachedMap().hashCode();
  }
  public final Object get(Object k) {
    final int idx = indexOf(k);
    return idx == -1 ? null : kvs[idx+1];
  }
  public final Object getOrDefault(Object k, Object defVal) {
    final int idx = indexOf(k);
    return idx == -1 ? defVal : kvs[idx+1];
  }
  public final boolean isEmpty() { return size() == 0; }
  public final Object merge(Object k, Object v, BiFunction remapper) {
    throw new RuntimeException("Unimplemented");
  }
  public final Object put(Object k, Object v) {
    throw new RuntimeException("Unimplemented");
  }
  public final void putAll(Map other) {
    throw new RuntimeException("Unimplemented");
  }
  public final Object remove(Object k)  {
    throw new RuntimeException("Unimplemented");
  }
  static class AMIter implements Iterator {
    public final int nElems;
    public final Object[] entries;
    public final BiFunction<Object,Object,Object> entryFn;
    int idx = 0;
    public AMIter(int ne, Object[] en, BiFunction<Object,Object,Object> _entryFn) {
      nElems = ne;
      entries = en;
      entryFn = _entryFn;
    }
    public final boolean hasNext() { return idx < nElems; }
    public final Object next() {
      if (idx >= nElems)
	throw new UnsupportedOperationException();
      final int lidx = idx*2;
      ++idx;
      final Object[] data = entries;
      return entryFn.apply(data[lidx], data[lidx+1]);
    }
  }
  final Iterator iterator(BiFunction<Object,Object,Object> iterFn) {
    return new AMIter(nElems, kvs, iterFn);
  }

  public static final BiFunction<Object,Object,Object> entryFn = (a,b) -> new MapEntry(a,b);
  public static final BiFunction<Object,Object,Object> keyFn = (a,b) -> a;
  public static final BiFunction<Object,Object,Object> valFn = (a,b) -> b;
  public final Iterator iterator() {
    return iterator(entryFn);
  }
  public final Iterator keyIterator() {
    return iterator(keyFn);
  }
  public final Iterator valIterator() {
    return iterator(valFn);
  }
  public final Set entrySet() {
    return new AbstractSet() {
      public final int size() {
	return PersistentArrayMap.this.size();
      }

      public final void clear() {
	throw new RuntimeException("Unimplemented");
      }

      @SuppressWarnings("unchecked")
      public final Iterator<Map.Entry> iterator() {
	return (Iterator<Map.Entry>)PersistentArrayMap.this.iterator();
      }

      public final boolean contains(Object o) {
	if (!(o instanceof Map.Entry))
	  return false;
	@SuppressWarnings("unchecked") Map.Entry e = (Map.Entry)o;
	final int idx = indexOf(e.getKey());
	return idx == -1 ? false : Objects.equals(kvs[idx+1], e.getValue());
      }
    };
  }
  public final Set keySet() {
    return new AbstractSet() {
      public final int size() {
	return PersistentArrayMap.this.size();
      }

      public final void clear() {
	throw new RuntimeException("Unimplemented");
      }

      @SuppressWarnings("unchecked")
      public final Iterator<Object> iterator() {
	return PersistentArrayMap.this.keyIterator();
      }

      public final boolean contains(Object o) {
	return containsKey(o);
      }
    };
  }
  public final Collection values() {
    return new AbstractSet() {
      public final int size() {
	return PersistentArrayMap.this.size();
      }

      public final void clear() {
	throw new RuntimeException("Unimplemented");
      }

      @SuppressWarnings("unchecked")
      public final Iterator<Object> iterator() {
	return PersistentArrayMap.this.valIterator();
      }
    };
  }

  final HashMap<Object,Object> cachedMap() {
    if (cachedTrie == null) {
      synchronized(this) {
	if ( cachedTrie == null)
	  cachedTrie = copyToTrie(hp, nElems, kvs, meta, nElems);
      }
    }
    return cachedTrie;
  }

  public final BitmapTrie bitmapTrie() {
    return cachedMap().bitmapTrie();
  }

  public MapSet intersection(MapSet rhs, BiFunction valueMap) {
    return cachedMap().intersection(rhs, valueMap);
  }
  public MapSet union(MapSet rhs, BiFunction valueMap) {
    return cachedMap().union(rhs, valueMap);
  }
  public MapSet difference(MapSet rhs) {
    return cachedMap().difference(rhs);
  }
  @SuppressWarnings("unchecked")
  public ImmutValues immutUpdateValues(BiFunction valueMap) {
    final Object[] mdata = kvs;
    final int ne = nElems;
    final int nne = ne * 2;
    final Object[] data = new Object[nne];
    for (int idx = 0; idx < nne; idx += 2) {
      final Object srcKey = mdata[idx];
      final Object srcVal = mdata[idx+1];
      data[idx] = srcKey;
      data[idx+1] = valueMap.apply(srcKey, srcVal);
    }
    return new PersistentArrayMap(hp, ne, data, meta);
  }
  @SuppressWarnings("unchecked")
  public ImmutValues immutUpdateValue(Object key, Function fn) {
    final int ne = nElems;
    final Object[] mdata = kvs;
    final int idx = indexOf(hp, ne, mdata, key);
    if (idx == -1) {
      return (ImmutValues)assoc(key, fn.apply(null));
    } else {
      final Object[] data = mdata.clone();
      data[idx+1] = fn.apply(data[idx+1]);
      return new PersistentArrayMap(hp, ne, data, meta);
    }
  }
}

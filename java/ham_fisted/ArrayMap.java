package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.IntegerOps.*;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Arrays;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Consumer;

import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.IReduceInit;
import clojure.lang.RT;
import clojure.lang.IDeref;


public class ArrayMap implements MapData {
  HashProvider hp;
  Object[] kvs;
  int length;
  IPersistentMap meta;

  public ArrayMap(HashProvider hp, Object[] kvs, int len, IPersistentMap m) {
    this.hp = hp;
    this.kvs = kvs;
    this.meta = m;
    this.length = len;
  }

  public ArrayMap(HashProvider hp, Object[] kvs) {
    this(hp, kvs, kvs.length/2, null);
  }

  public int size() { return this.length; }
  public boolean isEmpty() { return length == 0; }
  public ArrayMap clone() { return new ArrayMap(hp, kvs.clone(), length, meta); }
  public ArrayMap shallowClone() { return clone(); }
  public int index(Object k) {
    final HashProvider hp = this.hp;
    final int l = length;
    final Object[] kvs = this.kvs;
    for(int idx = 0; idx < l; ++idx) {
      final int kidx = idx*2;
      final Object kk = kvs[kidx];
      if(kk == k || hp.equals(kk, k))
	return kidx;
    }
    return -1;
  }
  public IPersistentMap meta() { return meta; }
  public MapData withMeta(IPersistentMap m) {
    return new ArrayMap(hp, kvs, length, m);
  }
  static class ArrayLeaf implements ILeaf, Map.Entry, IMutList {
    final Object[] kvs;
    final int kidx;
    public ArrayLeaf(Object[] kvs, int kidx) {
      this.kvs = kvs;
      this.kidx = kidx;
    }
    public Object key() { return kvs[kidx]; }
    public Object getKey() { return kvs[kidx]; }
    public Object val() { return kvs[kidx+1]; }
    public Object getValue() { return kvs[kidx+1]; }
    public Object setValue(Object vv) { return val(vv); }
    public Object val(Object nv) {
      Object rv = kvs[kidx+1];
      kvs[kidx+1] = nv;
      return rv;
    }
    public int size() { return 2; }
    public Object get(int idx) {
      if(idx == 0) return kvs[kidx];
      if(idx == 1) return kvs[kidx+1];
      throw new RuntimeException("Index out of range");
    }
  }
  public ILeaf getOrCreate(Object k) {
    int kidx = index(k);
    if(kidx != -1) {
      return new ArrayLeaf(kvs, kidx);
    } else {
      final int oldCap = kvs.length;
      final int oldL = length;
      int newCap = oldCap;
      if(oldCap == oldL*2) {
	newCap = Math.max(4, oldCap*2);
      }
      kidx = oldL*2;
      ++length;
      kvs = Arrays.copyOf(kvs, newCap);
      kvs[kidx] = k;
      return new ArrayLeaf(kvs, kidx);
    }
  }
  public ILeaf getNode(Object k) {
    int kidx = index(k);
    return kidx != -1 ? new ArrayLeaf(kvs, kidx) : null;
  }
  public Object get(Object k) {
    int kidx = index(k);
    return kidx != -1 ? kvs[kidx+1] : null;
  }
  public Object getOrDefault(Object k, Object d) {
    int kidx = index(k);
    return kidx != -1 ? kvs[kidx+1] : d;
  }
  public boolean containsKey(Object k) { return index(k) != -1; }
  public void clear() {
    length = 0;
    Arrays.fill(kvs, null);
  }
  void backfill(final int kidx) {
    final int l = length--;
    final int ll = l*2 - 1;
    for(int idx = kidx; idx < ll; ++idx)
      kvs[idx] = kvs[idx+1];      
    kvs[ll] = null;
    kvs[ll-1] = null;
  }
  public void remove(Object k, Box b) {
    int kidx = index(k);
    if(kidx != -1) {
      if(b != null)
	b.obj = kvs[kidx+1];
      backfill(kidx);
    }
  }
  public HashTable toMap() {
    final int l = length;
    final Object[] kvs = this.kvs;
    HashTable rv = new HashTable(hp, 0.75f, (int)(l/0.75), 0, null, meta);
    for(int idx = 0; idx < l; ++idx) {
      final int kidx = idx*2;
      rv.put(kvs[kidx], kvs[kidx+1]);
    }
    return rv;
  }
  MapData assocAt(int kidx, Object k, Object v) {
    if(kidx == -1) {
      if(length == 8) {
	HashTable rv= toMap();
	rv.put(k,v);
	return rv;
      } else {
	kidx = length*2;
	++length;
	if((kidx + 1) >= kvs.length)
	  kvs = Arrays.copyOf(kvs, kvs.length*2);
	kvs[kidx] = k;
	kvs[kidx+1] = v;
	return this;
      }      
    } else {
      kvs[kidx+1] = v;
      return this;
    }
  }
  public MapData mutAssoc(Object k, Object v) {
    return assocAt(index(k), k, v);
  }
  public void mutDissoc(Object k) {
    int kidx = index(k);
    if(kidx != -1) {
      backfill(kidx);
    }
  }
  public MapData mutUpdateValue(Object k, IFn fn) {
    int kidx = index(k);
    return assocAt(kidx, k, kidx == -1 ? fn.invoke(null) : fn.invoke(kvs[kidx+1]));
  }
  @SuppressWarnings("unchecked")
  public void mutUpdateValues(BiFunction bfn) {
    final int l = length;
    for(int idx = 0; idx < l; ++idx) {
      final int vidx = (idx * 2) + 1;
      kvs[vidx] = bfn.apply(kvs[vidx-1], kvs[vidx]);
    }
  }
  
  public Object reduce(Function<ILeaf,Object> lf, IFn rfn, Object acc) {
    final int l = length;
    if(lf == keyIterFn) {
      
    } else if (lf == valIterFn) {
      for(int idx = 0; idx < l; ++idx) {
	acc = rfn.invoke(acc, kvs[idx*2+1]);
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    } else if (lf == entryIterFn || lf == identityIterFn) {
      for(int idx = 0; idx < l; ++idx) {
	acc = rfn.invoke(acc, new ArrayLeaf(kvs, idx*2));
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    } else {
      for(int idx = 0; idx < l; ++idx) {
	acc = rfn.invoke(acc, lf.apply(new ArrayLeaf(kvs, idx*2)));
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
    }
    return acc;   
  }
  public IntFunction classifyLf(Function<ILeaf,Object> lf) {
    final Object[] kk = kvs;
    if(lf == keyIterFn)
      return (idx)->kk[idx*2];
    if(lf == valIterFn)
      return (idx)->kk[(idx*2)+1];
    if(lf == identityIterFn || lf == entryIterFn)
      return (idx)->new ArrayLeaf(kk, idx*2);
    else
      return (idx)->lf.apply(new ArrayLeaf(kk, idx*2));
  }
  static class IndexIter implements Iterator {
    final int l;
    int idx;
    final IntFunction lf;
    public IndexIter(int l, IntFunction lf) {
      this.l = l;
      this.idx = 0;
      this.lf = lf;
    }
    public boolean hasNext() {
      return idx < l;
    }
    public Object next() {
      Object rv = lf.apply(idx);
      ++idx;
      return rv;
    }
  }
  public Iterator iterator(Function<ILeaf,Object> lf) {
    return new IndexIter(length, classifyLf(lf));
  }
  static class IndexSpliterator implements Spliterator, IReduceInit {
    final IntFunction lf;
    int sidx;
    int eidx;
    public IndexSpliterator(IntFunction lf, int l) {
      this.lf = lf;
      this.sidx = 0;
      this.eidx = l;
    }
    public IndexSpliterator(IntFunction lf, int sidx, int eidx) {
      this.lf = lf;
      this.sidx = sidx;
      this.eidx = eidx;
    }
    public long estimateSize() { return eidx - sidx; }
    public long getExactSizeIfKnown() { return estimateSize(); }
    public int characteristics() { return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.SIZED; }
    public Spliterator trySplit() {
      if(estimateSize() < 8)
	return null;
      final int olde = eidx;
      final int newSize = (int)(estimateSize()/2);
      eidx = sidx + newSize;
      return new IndexSpliterator(lf, eidx, olde);
    }
    @SuppressWarnings("unchecked")
    public boolean tryAdvance(Consumer c) {
      if(sidx < eidx) {
	c.accept(lf.apply(sidx));
	++sidx;
	return true;
      }
      return false;
    }
    public Object reduce(IFn rfn, Object acc) {
      final int ee = eidx;      
      for(int idx = sidx; idx < ee; ++idx) {
	acc = rfn.invoke(acc, lf.apply(idx));
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
      return acc;
    }
  }
  public Spliterator spliterator(Function<ILeaf,Object> lf) {
    return new IndexSpliterator(classifyLf(lf), length);
  }
  public HashProvider hashProvider() { return hp; }
}

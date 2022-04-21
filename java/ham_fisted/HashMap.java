package ham_fisted;

import static ham_fisted.IntBitmap.*;
import clojure.lang.MapEntry;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Function;




public class HashMap<K,V> implements Map<K,V> {
  /**
   * Ripped directly from HashMap.java in openjdk source code -
   *
   * Computes key.hashCode() and spreads (XORs) higher bits of hash
   * to lower.  Because the table uses power-of-two masking, sets of
   * hashes that vary only in bits above the current mask will
   * always collide. (Among known examples are sets of Float keys
   * holding consecutive whole numbers in small tables.)  So we
   * apply a transform that spreads the impact of higher bits
   * downward. There is a tradeoff between speed, utility, and
   * quality of bit-spreading. Because many common sets of hashes
   * are already reasonably distributed (so don't benefit from
   * spreading), and because we use trees to handle large sets of
   * collisions in bins, we just XOR some shifted bits in the
   * cheapest possible way to reduce systematic lossage, as well as
   * to incorporate impact of the highest bits that would otherwise
   * never be used in index calculations because of table bounds.
   */
  public static final int mixhash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
  }
  public interface HashProvider {
    public default int hash(Object obj) {
      if (obj != null)
	return HashMap.mixhash(obj);
      return 0;
    }
    public default boolean equals(Object lhs, Object rhs) {
      return Objects.equals(lhs,rhs);
    }
  }

  public static final HashProvider hashcodeProvider = new HashProvider(){};

  public static final class Counter {
    int count;
    public Counter(){}
    public int count() { return count; }
    public void count(int val) { count = val; }
    public void inc() { ++count; }
    public void dec() { --count; }
  }

  public static final class Box {
    public Object obj;
    public Box() { obj = null; }
  }

  public interface LeafNodeIterator extends Iterator {
    public LeafNode nextLeaf();
    public default Object next() { return nextLeaf(); }
  }

  public static final class LeafNode {
    public final int hashcode;
    public final Object k;
    //compute-at support
    Object v;
    LeafNode nextNode;

    public LeafNode(Object _k, int hc) {
      k = _k;
      v = null;
      hashcode = hc;
      nextNode = null;
    }
    public Object key() { return k; }
    public Object val() { return v; }
    public Object val(Object newv) {
      Object retval = v;
      v = newv;
      return retval;
    }

    public final LeafNode getOrCreate(HashProvider hp, Counter c, Object _k) {
      if (hp.equals(k,_k)) {
	return this;
      } else if (nextNode != null) {
	return nextNode.getOrCreate(hp,c,_k);
      } else {
	final LeafNode retval = new LeafNode(_k, hashcode);
	c.inc();
	nextNode = retval;
	return retval;
      }
    }
    public final LeafNode get(HashProvider hp, Object _k) {
      if (hp.equals(k,_k)) {
	return this;
      } else if (nextNode != null) {
	return nextNode.get(hp,_k);
      } else {
	return null;
      }
    }
    public final LeafNode remove(HashProvider hp, Counter c, Object _k, Box b) {
      if(hp.equals(_k,k)) {
	c.dec();
	b.obj = v;
	return nextNode;
      }
      if(nextNode != null) {
	nextNode = nextNode.remove(hp,c,_k,b);
      }
      return this;
    }
    static class LFIter implements LeafNodeIterator {
      LeafNode curNode;
      LFIter(LeafNode lf) {
	curNode = lf;
      }
      public boolean hasNext() { return curNode != null; }
      public LeafNode nextLeaf() {
	if (curNode == null)
	  throw new NoSuchElementException();
	LeafNode retval = curNode;
	curNode = retval.nextNode;
	return retval;
      }
    }
    public final LeafNodeIterator iterator() {
      return new LFIter(this);
    }
  }
  public static final class BitmapNode {
    final int shift;
    int bitmap;
    Object[] data;

    public BitmapNode(int _bitmap, int _shift, Object[] _data) {
      bitmap = _bitmap;
      shift = _shift;
      data = _data;
    }
    void put(int bm, int idx, Object lf) {
      final Object[] objData = data;
      final int objLen = objData.length;
      final int nelems = Integer.bitCount(bm);
      final Object[] newData = nelems <= objLen ? objData : new Object[objLen *2];
      if(newData != objData) {
	for(int nidx = 0; nidx < idx; ++nidx)
	  newData[nidx] = data[nidx];
      }
      int lastCopyPos = nelems - 1;
      for(int nidx = idx; nidx < lastCopyPos; ++nidx)
	newData[nidx+1] = data[nidx];
      newData[idx] = lf;
      data = newData;
      bitmap = bm;
    }
    public LeafNode getOrCreate(HashProvider hp, Counter c, int hash, Object k) {
      final int bpos = bitpos(hash, shift);
      final Object[] objData = data;
      final int objLen = objData.length;
      final int bm = bitmap;
      final int index = index(bm,bpos);

      if ((bm & bpos) == 0) {
	final LeafNode retval = new LeafNode(k, hash);
	c.inc();
	put(bm | bpos, index, retval);
	return retval;
      }
      final Object entry = objData[index];
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (hash == lf.hashcode) {
	  return lf.getOrCreate(hp, c, k);
	} else {
	  final int nshift = incShift(shift);
	  final Object[] ndata = new Object[4];
	  ndata[0] = lf;
	  final BitmapNode node = new BitmapNode(bitpos(lf.hashcode, nshift),
						 nshift, ndata);
	  objData[index] = node;
	  return node.getOrCreate(hp, c, hash, k);
	}
      } else {
	  return ((BitmapNode)entry).getOrCreate(hp, c, hash, k);
      }
    }
    public LeafNode get(HashProvider hp, int hash, Object k) {
      final int bpos = bitpos(hash, shift);
      final int bm = bitmap;
      if ((bm & bpos) != 0) {
	final Object[] objData = data;
	final int index = index(bm,bpos);
	final Object entry = objData[index];
	final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
	if (lf != null) {
	  if (hash == lf.hashcode) {
	    return lf.get(hp,k);
	  }
	} else {
	  return ((BitmapNode)entry).get(hp, hash, k);
	}
      }
      return null;
    }
    public Object remove(HashProvider hp, Counter c, int hash, Object k, Box b) {
      final int bpos = bitpos(hash,shift);
      int bm = bitmap;
      if ((bm & bpos) != 0) {
	final Object[] objData = data;
	final int objLen = objData.length;
	final int index = index(bm,bpos);
	Object entry = objData[index];
	final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
	if (lf != null) {
	  if (hash == lf.hashcode) {
	    entry = lf.remove(hp, c, k, b);
	  } else {
	    entry = null;
	  }
	} else {
	  entry = ((BitmapNode)entry).remove(hp, c, hash, k, b);
	}
	objData[index] = entry;
	if (entry == null) {
	  bm ^= bpos;
	}
	if (bm == 0)
	  return null;
	bitmap = bm;
      }
      return this;
    }
    static class BitmapNodeIterator implements LeafNodeIterator {
      final Object[] data;
      int idx;
      LeafNodeIterator nextObj;
      BitmapNodeIterator(Object[] _data) {
	data = _data;
	idx = -1;
	advance();
      }
      void advance() {
	if (nextObj != null && nextObj.hasNext()) {
	    return;
	}
	int curIdx = idx + 1;
	final Object[] objData = data;
	final int len = objData.length;
	for (; curIdx < len && objData[curIdx] == null; ++curIdx);
	idx = curIdx;
	Object iterObj = curIdx == len ? null : objData[curIdx];
	if (iterObj == null) {
	  nextObj = null;
	} else if (iterObj instanceof LeafNode) {
	  LeafNode lf = (LeafNode)iterObj;
	  nextObj = lf.iterator();
	} else {
	  BitmapNode childNode = (BitmapNode)iterObj;
	  nextObj = childNode.iterator();
	}
      }
      public boolean hasNext() {
	return nextObj != null;
      }
      public LeafNode nextLeaf() {
	if (nextObj == null)
	  throw new UnsupportedOperationException();
	LeafNode lf = nextObj.nextLeaf();
	advance();
	return lf;
      }
    }
    LeafNodeIterator iterator() { return new BitmapNodeIterator(data); }
  }

  static class HTIterator implements Iterator {
    Function<LeafNode,Object> tfn;
    LeafNode nullEntry;
    LeafNodeIterator rootIter;
    HTIterator(Function<LeafNode,Object> _tfn, LeafNode _nullEntry, LeafNodeIterator _rootIter) {
      tfn = _tfn;
      _nullEntry = _nullEntry;
      rootIter = _rootIter;
    }
    public boolean hasNext() { return nullEntry != null || rootIter.hasNext(); }
    public Object next() {
      if (nullEntry != null) {
	Object retval = tfn.apply(nullEntry);
	nullEntry = null;
	return retval;
      }
      return tfn.apply(rootIter.nextLeaf());
    }
  }

  final Counter c;
  LeafNode nullEntry;
  BitmapNode root;
  HashProvider hp;
  public static Function<LeafNode,Object> valIterFn = lf -> lf.val();
  public static Function<LeafNode,Object> keyIterFn = lf -> lf.key();
  public static Function<LeafNode,Object> entryIterFn = lf -> new MapEntry(lf.key(), lf.val());
  public static Function<LeafNode,Object> identityIterFn = lf -> lf;

  public HashMap(HashProvider _hp) {
    c = new Counter();
    nullEntry = null;
    root = new BitmapNode(0,0,new Object[4]);
    hp = _hp;
  }

  public HashMap() {
    this(hashcodeProvider);
  }

  public void clear() {
    c.count(0);
    nullEntry = null;
    root = new BitmapNode(0,0,new Object[4]);
  }
  public V compute(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    throw new RuntimeException("unimplemented");
  }
  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    throw new RuntimeException("unimplemented");
  }
  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    throw new RuntimeException("unimplemented");
  }
  public boolean containsKey(Object key) {
    return getNode(key) != null;
  }
  public boolean containsValue(Object v) {
    throw new RuntimeException("unimplemented");
  }
  class EntrySet extends AbstractSet<Map.Entry<K,V>> {
    public final int size()                 { return HashMap.this.size(); }
    public final void clear()               { HashMap.this.clear(); }
    public final Iterator<Map.Entry<K,V>> iterator() {
      @SuppressWarnings("unchecked") Iterator<Map.Entry<K,V>> retval = (Iterator<Map.Entry<K,V>>) new HTIterator(HashMap.entryIterFn, nullEntry, root.iterator());
      return retval;
    }
    public final boolean contains(Object o) {
      if (!(o instanceof Map.Entry))
	return false;
      @SuppressWarnings("unchecked") Map.Entry e = (Map.Entry)o;
      Object key = e.getKey();
      LeafNode candidate = getNode(key);
      return candidate != null &&
	Objects.equals(candidate.key(), key) &&
	Objects.equals(candidate.val(), e.getValue());
    }
  }
  Set<Map.Entry<K,V>> cachedSet = null;
  public Set<Map.Entry<K,V>> entrySet() {
    if (cachedSet == null)
      cachedSet = new EntrySet();
    return cachedSet;
  }
  public boolean equals(Object o) {
    throw new RuntimeException("unimplemented");
  }
  public void forEach(BiConsumer<? super K,? super V> action) {
    throw new RuntimeException("unimplemented");
  }
  LeafNode getNode(Object key) {
    if (key == null)
      return nullEntry;
    return root.get(hp,hp.hash(key),key);
  }
  public V getOrDefault(Object key, V defaultValue) {
    LeafNode lf = getNode(key);
    @SuppressWarnings("unchecked") V retval = lf == null ? defaultValue : (V)lf.val();
    return retval;
  }
  public V get(Object k) {
    return getOrDefault(k,null);
  }
  public int hashCode() { return -1; }
  public boolean isEmpty() { return c.count() == 0; }
  class KeySet extends AbstractSet<K> {
    public final int size()                 { return HashMap.this.size(); }
    public final void clear()               { HashMap.this.clear(); }
    public final Iterator<K> iterator() {
      @SuppressWarnings("unchecked") Iterator<K> retval = (Iterator<K>) new HTIterator(HashMap.keyIterFn, nullEntry, root.iterator());
      return retval;
    }
    public final boolean contains(Object key) {
      return getNode(key) != null;
    }
  }
  public Set<K> keySet() { return new KeySet(); }
  LeafNode getOrCreate(K key) {
    if (key == null) {
      if (nullEntry == null) {
	c.inc();
	nullEntry = new LeafNode(null, 0);
      }
      return  nullEntry;
    } else {
      return root.getOrCreate(hp, c, hp.hash(key), key);
    }
  }
  /**
   * If the specified key is not already associated with a value or is associated
   * with null, associates it with the given non-null value. Otherwise, replaces the
   * associated value with the results of the given remapping function, or removes
   * if the result is null.
   */
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    if (value == null || remappingFunction == null)
      throw new NullPointerException();
    LeafNode lf = getOrCreate(key);
    @SuppressWarnings("unchecked") V valval = (V)lf.val();
    if (valval == null) {
      lf.val(value);
      return value;
    } else {
      V retval = remappingFunction.apply(valval, value);
      if (retval == null)
	remove(key);
      else
	lf.val(retval);
      return retval;
    }
  }
  public V put(K key, V value) {
    LeafNode lf = getOrCreate(key);
    @SuppressWarnings("unchecked") V retval = (V)lf.val(value);
    return retval;
  }
  public void putAll(Map<? extends K,? extends V> m) {
    final Iterator iter = m.entrySet().iterator();
    while(iter.hasNext()) {
      Map.Entry entry = (Map.Entry)iter.next();
      @SuppressWarnings("unchecked") K key = (K)entry.getKey();
      @SuppressWarnings("unchecked") V val = (V)entry.getValue();
      put(key, val);
    }
  }
  public V putIfAbsent(K key, V value) {
    LeafNode lf;
    int cval = c.count();
    if (key == null) {
      if (nullEntry == null) {
	c.inc();
	nullEntry = new LeafNode(null, 0);
      }
      lf = nullEntry;
    } else {
      lf = root.getOrCreate(hp, c, hp.hash(key), key);
    }
    if (c.count() > cval) {
      lf.val(value);
      return value;
    } else {
      @SuppressWarnings("unchecked") V retval = (V)lf.val();
      return retval;
    }
  }
  public V remove(Object key) {
    if (key == null) {
      if (nullEntry != null) {
	@SuppressWarnings("unchecked") V retval = (V)nullEntry.val();
	nullEntry = null;
	c.dec();
	return retval;
      }
      return null;
    }
    Box b = new Box();
    root.remove(hp, c, hp.hash(key), key, b);
    @SuppressWarnings("unchecked") V retval = (V)b.obj;
    return retval;
  }
  public int size() { return c.count(); }
  class ValueCollection extends AbstractCollection<V> {
    public final int size()                 { return HashMap.this.size(); }
    public final void clear()               { HashMap.this.clear(); }
    public final Iterator<V> iterator() {
      @SuppressWarnings("unchecked") Iterator<V> retval = (Iterator<V>) new HTIterator(HashMap.valIterFn, nullEntry, root.iterator());
      return retval;
    }
  }
  public Collection<V> values() {
    return new ValueCollection();
  }
}

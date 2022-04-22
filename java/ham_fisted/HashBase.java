package ham_fisted;


import static ham_fisted.IntBitmap.*;
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Objects;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.BiConsumer;
import clojure.lang.MapEntry;


public class HashBase {
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
	return mixhash(obj);
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
    public final int owner;
    public final int hashcode;
    public final Object k;
    //compute-at support
    Object v;
    LeafNode nextNode;
    public LeafNode(int _owner, Object _k, Object _v, int hc) {
      owner = _owner;
      hashcode = hc;
      k = _k;
      v = _v;
      nextNode = null;
    }
    public LeafNode(Object _k, Object _v, int hc) {
      this(0, _k, _v, hc);
    }

    public LeafNode(Object _k, int hc) {
      this(0, _k, null, hc);
    }
    public LeafNode(int _owner, LeafNode prev) {
      owner = _owner;
      hashcode = prev.hashcode;
      k = prev.k;
      v = prev.v;
      nextNode = prev.nextNode;
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
    public final LeafNode setOwner(int nowner) {
      if (owner != nowner) {
	return new LeafNode(nowner, this);
      } else {
	return this;
      }
    }
    public final LeafNode assoc(HashProvider hp, Counter c, int nowner, Object _k, Object _v) {
      LeafNode retval = setOwner(nowner);
      if (hp.equals(_k,k)) {
	retval.v = _v;
      } else {
	if (retval.nextNode != null) {
	  retval.nextNode = retval.nextNode.assoc(hp, c, nowner, _k, _v);
	} else {
	  c.inc();
	  retval.nextNode = new LeafNode(nower, _k, _v, hashcode);
	}
      }
      return retval;
    }
    public final LeafNode dissoc(HashProvider hp, Counter c, int nowner, Object _k) {
      if (hp.equals(k, _k)) {
	c.dec();
	return nextNode;
      }
      if (nextNode != null) {
	LeafNode nn = nextNode.dissoc(hp,c,nowner,k);
	if (nn != nextNode) {
	  LeafNode retval = setOwner(nowner);
	  retval.nextNode = nn;
	  return retval;
	}
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
    public final int owner;
    public final int shift;
    int bitmap;
    Object[] data;

    public BitmapNode(int _owner, int _bitmap, int _shift, Object[] _data) {
      owner = _owner;
      bitmap = _bitmap;
      shift = _shift;
      data = _data;
    }
    public BitmapNode(int _bitmap, int _shift, Object[] _data) {
      this(0, _bitmap, _shift,  _data);
    }
    public BitmapNode() {
      this(0,0, new Object[4]);
    }
    public BitmapNode(int _owner, BitmapNode node, int nelems) {
      owner = _owner;
      bitmap = node.bitmap;
      shift = node.shift;
      final Object[] curData = node.data;
      final int dlen = curData.length;
      if (dlen < nelems) {
	data = new Object[2*dlen];
	System.arraycopy(curData,0,data,0,dlen);
      } else (dlen > nextPow2(nelems)) {
	int nelemsp2 = nextPow2(nelems);
	data = new Object[nelemsp2];
	System.arraycopy(curData,0,data,0,nelems);
      } else {
	data = curData.clone();
      }
    }
    void put(int bm, int idx, Object lf) {
      final Object[] objData = data;
      final int objLen = objData.length;
      final int nelems = Integer.bitCount(bm);
      final Object[] newData = nelems <= objLen ? objData : new Object[objLen *2];
      if(newData != objData) {
	for(int nidx = 0; nidx < idx; ++nidx)
	  newData[nidx] = objData[nidx];
      }
      int lastCopyPos = nelems - 1;
      for(int nidx = idx; nidx < lastCopyPos; ++nidx)
	newData[nidx+1] = objData[nidx];
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
	  //Keep array packed
	  final int ne = Integer.BitCount(bm) + 1;
	  for (; index < ne; ++index)
	    objData[index] = objData[index+1];
	}
	bitmap = bm;
	if (bm == 0)
	  return null;
      }
      return this;
    }
    public BitmapNode assoc(HashProvider hp, Counter c, int nowner,
			    int hash, Object _k, Object _v) {
      final int bpos = bitpos(hash,shift);
      int bm = bitmap;
      final boolean hasEntry = (bm & bpos) != 0;
      final int nelems = hasEntry ? Integer.bitCount(bitmap) : Integer.bitCount(bitmap) + 1;
      bm = bm | bpos;
      BitmapNode retval = owner == nowner ? this : new BitmapNode(nowner,this,nelems);
      retval.bitmap = bm;
      if (!hasEntry) {
	final LeafNode lf = new LeafNode(nowner, k, v, hash);
	c.inc();
	retval.put(bm, index, lf);
	return retval;
      } else {
	final int index = index(bm,bpos);
	final Object[] rdata = retval.data;
	final Object curVal = rdata[index];
	if (curVal instanceof LeafNode) {
	  final LeafNode lf = (LeafNode)curVal;
	  if (lf.hashcode == hash) {
	    rdata[index] = lf.assoc(hp, c, nowner, _k, _v);	    
	  } else {
	    final int nshift = incShift(shift);
	    final Object[] ndata = new Object[4];
	    ndata[0] = lf;
	    final BitmapNode node = new BitmapNode(nowner,
						   bitpos(lf.hashcode, nshift),
						   nshift, ndata);
	    rdata[index] = node.assoc(hp, c, nowner, hash, _k, _v);
	  }
	} else {
	  BitmapNode bm = (BitmapNode)curVal;
	  rdata[index] = bm.assoc(hp,c,nowner,hash,_k,_v);
	}
      }
      return retval;
    }
    public Object dissoc(HashProvider hp, Counter c, int nowner, int hash, Object _k) {
      final int bpos = bitpos(hash,shift);
      int bm = bitmap;
      final boolean hasEntry = (bm & bpos) != 0;
      if(!hasEntry)
	return this;
      final int index = index(bm,bpos);
      Object entry = data[index];
      Object nentry = null;
      if (entry instanceof LeafNode) {
	LeafNode lf = (LeafNode)entry;
	if (lf.hashcode != hash)
	  return this;
	nentry = lf.dissoc(hp, c, nwoner, _k);
      } else {
	BitmapNode node = (BitmapNode)entry;
	nentry = node.dissoc(hp, c, nowner, hash, _k);
      }
      //No change
      if (nentry == entry)
	return this;
      //We have to preserve nelems to copy elems over the existing one we removed.
      final int nelems = Integer.BitCount(bm);
      final int nNewElems = nentry == null ? nelems - 1 : nelems;
      if(nNewElems == 0) {
	return null;
      }
      final BitmapNode retval = owner == nowner ? this : new BitmapNode(nowner, this, nelems);
      final Object[] rdata = retval.data;
      if (nentry == null) {
	for(int idx = index; idx < nNewElems; ++idx)
	  rdata[idx] = rdata[idx+1];
      } else {
	rdata[index] = nentry;
      }
      return retval;
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
    public LeafNodeIterator iterator() { return new BitmapNodeIterator(data); }
  }

  public static class HTIterator implements LeafNodeIterator {
    Function<LeafNode,Object> tfn;
    LeafNode nullEntry;
    LeafNodeIterator rootIter;
    HTIterator(Function<LeafNode,Object> _tfn, LeafNode _nullEntry, LeafNodeIterator _rootIter) {
      tfn = _tfn;
      _nullEntry = _nullEntry;
      rootIter = _rootIter;
    }
    public boolean hasNext() { return nullEntry != null || rootIter.hasNext(); }
    public LeafNode nextLeaf() {
      if ( nullEntry != null) {
	LeafNode retval = nullEntry;
	nullEntry = null;
	return retval;
      } else {
	return rootIter.nextLeaf();
      }
    }
    public Object next() {
      return tfn.apply(nextLeaf());
    }
  }

  public static Function<LeafNode,Object> valIterFn = lf -> lf.val();
  public static Function<LeafNode,Object> keyIterFn = lf -> lf.key();
  public static Function<LeafNode,Object> entryIterFn = lf -> new MapEntry(lf.key(), lf.val());
  public static Function<LeafNode,Object> identityIterFn = lf -> lf;

  protected final HashProvider hp;
  protected final Counter c;
  protected BitmapNode root;
  protected LeafNode nullEntry;
  
  public HashBase(HashProvider _hp, Counter _c, BitmapNode r, LeafNode ne) {
    hp = _hp;
    c = _c;
    root = r;
    nullEntry = ne;
  }

  public HashBase(HashProvider _hp) {
    c = new Counter();
    nullEntry = null;
    root = new BitmapNode(0,0,new Object[4]);
    hp = _hp;
  }

  public HashBase() {
    this(hashcodeProvider);
  }

  final LeafNode getNode(Object key) {
    if(key == null)
      return nullEntry;
    return root.get(hp,hp.hash(key),key);
  }

  final LeafNode getOrCreate(Object key) {
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

  public void clear() {
    c.count(0);
    nullEntry = null;
    root = new BitmapNode(0,0,new Object[4]);
  }

  public int size() { return c.count(); }
  
  class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>> {

    final boolean allowsClear;
    
    EntrySet(boolean _allowsClear) {
      allowsClear = _allowsClear;
    }
    
    public final int size() {
      return HashBase.this.size();
    }
    public final void clear() {
      if (allowsClear) {
	c.count(0);
	nullEntry = null;
	root = new BitmapNode(0,0,new Object[4]);
      }
      else
	throw new RuntimeException("Unimplemented");
    }
    public final Iterator<Map.Entry<K,V>> iterator() {
      @SuppressWarnings("unchecked") Iterator<Map.Entry<K,V>> retval = (Iterator<Map.Entry<K,V>>) new HTIterator(entryIterFn, nullEntry, root.iterator());
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
  final <K,V> Set<Map.Entry<K,V>> entrySet(Map.Entry<K,V> me, boolean allowsClear) {
    return new EntrySet<K,V>(allowsClear);
  }

  
  class KeySet<K> extends AbstractSet<K> {
    final boolean allowsClear;
    KeySet(boolean _ac) {
      allowsClear = _ac;
    }
    public final int size() {
      return HashBase.this.size();
    }
    public final void clear() {
      if (allowsClear)
	HashBase.this.clear();
      else
	throw new RuntimeException("Unimplemented");
    }
    public final Iterator<K> iterator() {
      @SuppressWarnings("unchecked") Iterator<K> retval = (Iterator<K>) new HTIterator(keyIterFn, nullEntry, root.iterator());
      return retval;
    }
    public final boolean contains(Object key) {
      return getNode(key) != null;
    }
  }  
  final <K> Set<K> keySet(K k, boolean allowsClear) {
    return new KeySet<K>(allowsClear);
  }

  
  class ValueCollection<V>  extends AbstractCollection<V> {
    boolean allowsClear;
    ValueCollection(boolean ac) { allowsClear = ac; }
    public final int size() { return HashBase.this.size(); }
    public final void clear() {
      if (allowsClear)
	HashBase.this.clear();
      else
	throw new RuntimeException("Unimplemented");
    }
    public final Iterator<V> iterator() {
      @SuppressWarnings("unchecked") Iterator<V> retval = (Iterator<V>) new HTIterator(valIterFn, nullEntry, root.iterator());
      return retval;
    }
  }
  final <V> Collection<V> values(V obj, boolean allowsClear) {
    return new ValueCollection<V>(allowsClear);
  }

  
  public boolean containsKey(Object key) {
    return getNode(key) != null;
  }
  public boolean containsValue(Object v) {
    Iterator iter = new HTIterator(identityIterFn, nullEntry, root.iterator());
    while(iter.hasNext()) {
      LeafNode lf = (LeafNode)iter.next();
      if (Objects.equals(lf.val(), v))
	return true;
    }
    return false;
  }  
  //TODO - spliterator parallelization
  @SuppressWarnings("unchecked")
  final void forEachImpl(BiConsumer action) {
    Iterator iter = new HTIterator(identityIterFn, nullEntry, root.iterator());
    while(iter.hasNext()) {
      LeafNode lf = (LeafNode)iter.next();
      action.accept(lf.key(), lf.val());
    }
  }

  final Object getOrDefaultImpl(Object key, Object defaultValue) {
    LeafNode lf = getNode(key);
    return lf == null ? defaultValue : lf.val();
  }
  public Object get(Object k) {
    return getOrDefaultImpl(k,null);
  }
}

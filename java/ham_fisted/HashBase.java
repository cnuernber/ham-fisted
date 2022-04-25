package ham_fisted;

import static java.lang.System.out;
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
//Metadata is one of the truly brilliant ideas from Clojure
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;


public class HashBase implements IObj {
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
    public Counter(){ count = 0; }
    public Counter(Counter p) { count = p.count; }
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
    public final HashBase owner;
    public final int hashcode;
    public final Object k;
    //compute-at support
    Object v;
    LeafNode nextNode;
    public LeafNode(HashBase _owner, Object _k, Object _v, int hc, LeafNode nn) {
      owner = _owner;
      hashcode = hc;
      k = _k;
      v = _v;
      nextNode = nn;
    }
    public LeafNode(HashBase _owner, Object _k, Object _v, int hc) {
      this(_owner, _k, _v, hc, null);
    }
    public LeafNode(Object _k, Object _v, int hc) {
      this(null, _k, _v, hc, null);
    }

    public LeafNode(Object _k, int hc) {
      this(null, _k, null, hc);
    }
    public LeafNode(HashBase _owner, LeafNode prev) {
      owner = _owner;
      hashcode = prev.hashcode;
      k = prev.k;
      v = prev.v;
      nextNode = prev.nextNode;
    }
    public final LeafNode clone() {
      return new LeafNode(owner, k, v, hashcode, nextNode != null ? nextNode.clone() : null);
    }
    public final Object key() { return k; }
    public final Object val() { return v; }
    public final Object val(Object newv) {
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
    public final LeafNode setOwner(HashBase nowner) {
      if (owner != nowner) {
	return new LeafNode(nowner, this);
      } else {
	return this;
      }
    }
    public final LeafNode assoc(HashProvider hp, Counter c, HashBase nowner, Object _k, Object _v) {
      LeafNode retval = setOwner(nowner);
      if (hp.equals(_k,k)) {
	retval.v = _v;
      } else {
	if (retval.nextNode != null) {
	  retval.nextNode = retval.nextNode.assoc(hp, c, nowner, _k, _v);
	} else {
	  c.inc();
	  retval.nextNode = new LeafNode(nowner, _k, _v, hashcode);
	}
      }
      return retval;
    }
    public final LeafNode dissoc(HashProvider hp, Counter c, HashBase nowner, Object _k) {
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
    public void print(int indent) {
      for(int idx = 0; idx < indent; ++idx)
	out.print("  ");
      out.println("leafNode - " + String.valueOf(k) + ": " + String.valueOf(v));
      ++indent;
      if (nextNode != null)
	nextNode.print(indent);
    }
  }
  public static final class BitmapNode {
    public final HashBase owner;
    public final int shift;
    int bitmap;
    Object[] data;

    public BitmapNode(HashBase _owner, int _bitmap, int _shift, Object[] _data) {
      owner = _owner;
      bitmap = _bitmap;
      shift = _shift;
      data = _data;
    }

    public BitmapNode(int _bitmap, int _shift, Object[] _data) {
      this(null, _bitmap, _shift,  _data);
    }

    public BitmapNode() {
      this(null,0,0, new Object[4]);
    }

    public BitmapNode(HashBase _owner, int _bitmap, int _shift) {
      owner = _owner;
      bitmap = _bitmap;
      shift = _shift;
      data = new Object[nextPow2(Integer.bitCount(_bitmap))];
    }

    public BitmapNode(HashBase _owner, int _shift, LeafNode leaf) {
      owner = _owner;
      shift = _shift;
      bitmap = leaf != null ? bitpos(leaf.hashcode, _shift) : 0;
      data = new Object[] {leaf, null, null, null};
    }

    public final BitmapNode clone() {
      final Object[] newData = data.clone();
      final int len = newData.length;
      for (int idx = 0; idx < len; ++idx) {
	final Object entry = newData[idx];
	if (entry != null) {
	  newData[idx] = entry instanceof LeafNode ? ((LeafNode)entry).clone() : ((BitmapNode)entry).clone();
	}
      }
      return new BitmapNode(owner, bitmap, shift, newData);
    }

    final static Object[] insert(Object[] srcData, Object obj, int insertIdx, int newlen,
				 boolean forceCopy) {
      final int srcLen = srcData.length;
      final int dstLen = nextPow2(newlen);
      boolean copy = forceCopy | dstLen > srcLen;
      final Object[] dstData = copy ? new Object[dstLen] : srcData;
      if ( dstData != srcData ) {
	for(int idx = 0; idx < insertIdx; ++idx)
	  dstData[idx] = srcData[idx];
      }
      for(int ridx = newlen-1; ridx > insertIdx; --ridx)
	dstData[ridx] = srcData[ridx-1];
      dstData[insertIdx] = obj;
      return dstData;
    }

    public final LeafNode getOrCreate(HashProvider hp, Counter c, int hash, Object k) {
      final int bpos = bitpos(hash, shift);
      final Object[] objData = data;
      final int objLen = objData.length;
      int bm = bitmap;
      final int index = index(bm,bpos);

      if ((bm & bpos) == 0) {
	bm |= bpos;
	final LeafNode retval = new LeafNode(k, hash);
	c.inc();
	data = insert(objData, retval, index, Integer.bitCount(bm), false);
	bitmap = bm;
	return retval;
      }
      final Object entry = objData[index];
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (hash == lf.hashcode) {
	  return lf.getOrCreate(hp, c, k);
	} else {
	  final BitmapNode node = new BitmapNode(owner, incShift(shift), lf);
	  objData[index] = node;
	  return node.getOrCreate(hp, c, hash, k);
	}
      } else {
	  return ((BitmapNode)entry).getOrCreate(hp, c, hash, k);
      }
    }

    public final LeafNode get(HashProvider hp, int hash, Object k) {
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

    final static Object[] remove(Object[] srcData, int remidx, int nelems, boolean forceCopy) {
      //nelems is nelems *after* removal
      Object[] dstData = forceCopy ? new Object[Math.max(4, nextPow2(nelems))] : srcData;
      if(dstData != srcData)
	for(int idx = 0; idx < remidx; ++idx)
	  dstData[idx] = srcData[idx];
      for(int idx = remidx; idx < nelems; ++idx)
	dstData[idx] = srcData[idx+1];
      //In the case of a resize dstData may not have the nelems+1 range.
      if(dstData == srcData)
	dstData[nelems] = null;
      return dstData;
    }

    public final Object remove(HashProvider hp, Counter c, int hash, Object k, Box b,
			       boolean collapse) {
      final int bpos = bitpos(hash,shift);
      int bm = bitmap;
      if ((bm & bpos) == 0)
	return this;
      final Object[] objData = data;
      final int index = index(bm,bpos);
      Object entry = objData[index];
      Object nentry = null;
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (hash != lf.hashcode)
	  return this;
	nentry = lf.remove(hp, c, k, b);
      } else {
	nentry = ((BitmapNode)entry).remove(hp, c, hash, k, b, true);
      }
      //No change
      if (nentry == entry)
	return this;
      final boolean removed = nentry == null;
      if (removed)
	bm ^= bpos;
      final int nelems = Integer.bitCount(bm);
      if (collapse) {
	if (nelems == 0) {
	  return null;
	} else if (nelems == 1) {
	  if (removed) {
	    final Object item = index == 0 ? objData[1] : objData[0];
	    if (item instanceof LeafNode)
	      return item;
	  } else if (entry instanceof LeafNode) {
	    return entry;
	  }
	}
      }
      bitmap = bm;
      if (removed) {
	remove(objData, index, nelems, false);
      } else {
	objData[index] = entry;
      }
      return this;
    }

    public final BitmapNode assoc(HashProvider hp, Counter c, HashBase nowner,
				  int hash, Object _k, Object _v) {
      final int bpos = bitpos(hash,shift);
      final boolean forceCopy = nowner != owner;
      final int bm = bitmap;
      if ((bm & bpos) == 0) {
	c.inc();
	final int nbm = bm | bpos;
	final Object[] dstData = insert(data, new LeafNode(nowner, _k, _v, hash),
					index(nbm,bpos),
					Integer.bitCount(nbm),
					forceCopy);
	if(nowner != owner) {
	  return new BitmapNode(nowner, nbm, shift, dstData);
	} else {
	  data = dstData;
	  bitmap = nbm;
	  return this;
	}
      } else {
	final Object[] srcData = data;
	final Object[] dstData = forceCopy ? srcData.clone() : srcData;
	final int index = index(bm,bpos);
	final Object curVal = dstData[index];
	if (curVal instanceof LeafNode) {
	  final LeafNode lf = (LeafNode)curVal;
	  if (lf.hashcode == hash) {
	    dstData[index] = lf.assoc(hp, c, nowner, _k, _v);
	  } else {
	    BitmapNode node = new BitmapNode(nowner, incShift(shift), lf);
	    dstData[index] = node.assoc(hp, c, nowner, hash, _k, _v);
	  }
	} else {
	  final BitmapNode node = (BitmapNode)curVal;
	  dstData[index] = node.assoc(hp,c,nowner,hash,_k,_v);
	}
	return forceCopy ? new BitmapNode(nowner, bm, shift, dstData) : this;
      }
    }

    public Object dissoc(HashProvider hp, Counter c, HashBase nowner, int hash, Object _k,
			 boolean collapse) {
      final int bpos = bitpos(hash,shift);
      int bm = bitmap;
      if((bm & bpos) == 0)
	return this;
      final Object[] srcData = data;
      final int index = index(bm,bpos);
      Object entry = srcData[index];
      Object nentry = null;
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (lf.hashcode != hash)
	  return this;
	nentry = lf.dissoc(hp, c, nowner, _k);
      } else {
	nentry = ((BitmapNode)entry).dissoc(hp, c, nowner, hash, _k, true);
      }
      //No change
      if (nentry == entry)
	return this;
      //We have to preserve nelems to copy elems over the existing one we removed.
      final boolean removed = nentry == null;
      if (removed)
	bm ^= bpos;
      final int nelems = Integer.bitCount(bm);
      //Look for quick opportunities to collapse the tree.
      if (collapse) {
	if(nelems == 0) {
	  return null;
	} else if (nelems == 1) {
	  if (removed) {
	    Object leftover = srcData[index == 0 ? 1 : 0];
	    if (leftover instanceof LeafNode)
	      return leftover;
	  } else {
	    if (nentry instanceof LeafNode)
	      return nentry;
	  }
	}
      }
      boolean forceCopy = nowner != owner;
      Object[] dstData;
      if(removed)
	dstData = remove(srcData, index, nelems, forceCopy);
      else {
	dstData = forceCopy ? srcData.clone() : srcData;
	dstData[index] = nentry;
      }
      if (forceCopy) {
	return new BitmapNode(owner, bm, shift, dstData);
      } else {
	data = dstData;
	bitmap = bm;
	return this;
      }
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

    public void print(int indent) {
      for(int idx = 0; idx < indent; ++idx) {
	out.print("  ");
      }
      int bc = Integer.bitCount(bitmap);
      out.println("Shift: " + String.valueOf(shift) + " Bitmap: " + String.valueOf(bitmap) +
		  " bitcount: " + bc + " Owner: " + String.valueOf(owner));
      ++indent;
      for (int idx = 0; idx < bc; ++idx) {
	final Object obj = data[idx];
	if ( obj instanceof BitmapNode) {
	  ((BitmapNode)obj).print(indent);
	} else {
	  ((LeafNode)obj).print(indent);
	}
      }
    }

    public void print() {
      print(0);
    }
  }

  public static class HTIterator implements LeafNodeIterator {
    Function<LeafNode,Object> tfn;
    LeafNode nullEntry;
    LeafNodeIterator rootIter;
    HTIterator(Function<LeafNode,Object> _tfn, LeafNode _nullEntry, LeafNodeIterator _rootIter) {
      tfn = _tfn;
      nullEntry = _nullEntry;
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
  protected final IPersistentMap meta;

  public HashBase(HashProvider _hp, Counter _c, BitmapNode r, LeafNode ne, IPersistentMap _meta) {
    hp = _hp;
    c = _c;
    root = r;
    nullEntry = ne;
    meta = _meta;
  }

  public HashBase(HashProvider _hp) {
    c = new Counter();
    nullEntry = null;
    root = new BitmapNode(0,0,new Object[4]);
    hp = _hp;
    meta = null;
  }

  public HashBase() {
    this(hashcodeProvider);
  }

  public HashBase(HashBase other) {
    hp = other.hp;
    c = new Counter(other.c);
    root = other.root != null ? other.root.clone() : null;
    nullEntry = nullEntry  != null ? other.nullEntry.clone() : null;
    meta = other.meta;
  }

  HashBase shallowClone(IPersistentMap newMeta) {
    return new HashBase(hp, new Counter(c), root, nullEntry, newMeta);
  }
  public HashBase shallowClone() {
    return shallowClone(meta);
  }

  HashBase deepClone() {
    return new HashBase(this);
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

  public LeafNodeIterator iterator(Function<LeafNode,Object> fn) {
    return new HTIterator(fn, nullEntry, root.iterator());
  }

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
      @SuppressWarnings("unchecked") Iterator<Map.Entry<K,V>> retval = (Iterator<Map.Entry<K,V>>) HashBase.this.iterator(entryIterFn);
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
      @SuppressWarnings("unchecked") Iterator<K> retval = (Iterator<K>) HashBase.this.iterator(keyIterFn);
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
      @SuppressWarnings("unchecked") Iterator<V> retval = (Iterator<V>) HashBase.this.iterator(valIterFn);
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

  //Mutating assoc.  Uses identity hashcode to only copy necessary information.
  //Always returns this
  final HashBase assoc(Object key, Object val) {
    if (key == null) {
      if (nullEntry == null)
	c.inc();
      nullEntry = new LeafNode(this, key, val, 0);
    } else {
      root = root.assoc(hp, c, this, hp.hash(key), key, val);
    }
    return this;
  }

  //Dissoc.  No check for null key identity - always returns this
  final HashBase dissoc(Object key) {
    if(key == null) {
      if(nullEntry != null) {
	nullEntry = null;
	c.dec();
      }
    } else {
      root = (BitmapNode)root.dissoc(hp,c,this,hp.hash(key),key,false);
    }
    return this;
  }

  final Object getOrDefaultImpl(Object key, Object defaultValue) {
    LeafNode lf = getNode(key);
    return lf == null ? defaultValue : lf.val();
  }

  public Object get(Object k) {
    return getOrDefaultImpl(k,null);
  }

  public IObj withMeta(IPersistentMap meta) {
    return shallowClone(meta);
  }

  public IPersistentMap meta() { return meta; }

  public void printNodes() {
    if (nullEntry != null)
      out.println("nullEntry: " + String.valueOf(nullEntry.val()));
    root.print();
  }

}

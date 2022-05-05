package ham_fisted;

import static java.lang.System.out;
import static ham_fisted.IntegerOps.*;
import static ham_fisted.BitmapTrieCommon.*;
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
import java.util.function.Consumer;
import java.util.function.BiFunction;
import clojure.lang.MapEntry;
//Metadata is one of the truly brilliant ideas from Clojure
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;


public class BitmapTrie implements IObj, TrieBase {

  public static final class LeafNode implements INode, ILeaf {
    public final TrieBase owner;
    public final int hashcode;
    public final Object k;
    //compute-at support means we can modify v.
    Object v;
    LeafNode nextNode;
    public LeafNode(TrieBase _owner, Object _k, int hc, Object _v, LeafNode nn) {
      owner = _owner;
      hashcode = hc;
      k = _k;
      v = _v;
      nextNode = nn;
      _owner.inc();
    }
    public LeafNode(TrieBase _owner, Object _k, int hc, Object _v) {
      this(_owner, _k, hc, _v, null);
    }
    public LeafNode(TrieBase _owner, Object _k, int hc) {
      this(_owner, _k, hc, null, null);
    }
    public LeafNode(TrieBase _owner, LeafNode prev) {
      owner = _owner;
      hashcode = prev.hashcode;
      k = prev.k;
      v = prev.v;
      nextNode = prev.nextNode;
    }
    public final LeafNode clone(TrieBase nowner) {
      nowner = nowner == null ? owner : nowner;
      return new LeafNode(nowner, k, hashcode, v,
			  nextNode != null ? nextNode.clone(nowner) : null);
    }
    public final LeafNode setOwner(TrieBase nowner) {
      if (owner == nowner)
	return this;
      return new LeafNode(nowner, this);
    }
    public final int countLeaves() {
      return nextNode != null ? 1 + nextNode.countLeaves() : 1;
    }
    public final Object key() { return k; }
    public final Object val() { return v; }
    public final Object val(Object newv) {
      Object retval = v;
      v = newv;
      return retval;
    }
    //Mutable pathway
    public final LeafNode getOrCreate(Object _k) {
      if (owner.equals(k,_k)) {
	return this;
      } else if (nextNode != null) {
	return nextNode.getOrCreate(_k);
      } else {
	final LeafNode retval = new LeafNode(owner, _k, hashcode);
	nextNode = retval;
	return retval;
      }
    }
    public final LeafNode get(Object _k) {
      if (owner.equals(k,_k)) {
	return this;
      } else if (nextNode != null) {
	return nextNode.get(_k);
      } else {
	return null;
      }
    }
    public final LeafNode remove(Object _k, Box b) {
      if(owner.equals(_k,k)) {
	owner.dec();
	b.obj = v;
	return nextNode;
      }
      if(nextNode != null) {
	nextNode = nextNode.remove(_k,b);
      }
      return this;
    }

    public final LeafNode assoc(TrieBase nowner, Object _k, Object _v) {
      LeafNode retval = setOwner(nowner);
      if (nowner.equals(_k,k)) {
	retval.v = _v;
      } else {
	if (retval.nextNode != null) {
	  retval.nextNode = retval.nextNode.assoc(nowner, _k, _v);
	} else {
	  retval.nextNode = new LeafNode(nowner, _k, hashcode, _v);
	}
      }
      return retval;
    }
    @SuppressWarnings("unchecked")
    public final LeafNode union(TrieBase nowner, Object _k, Object v, BiFunction valueMapper) {
      LeafNode retval = setOwner(nowner);
      if(owner.equals(_k, k))
	retval.v = valueMapper.apply(retval.v, v);
      else if (nextNode != null) {
	retval.nextNode = nextNode.union(nowner, _k, v, valueMapper);
      } else {
	retval.nextNode = new LeafNode(nowner, _k, hashcode, v);
      }
      return retval;
    }
    public final LeafNode dissoc(TrieBase nowner, Object _k) {
      if (owner.equals(k, _k)) {
	nowner.dec();
	return nextNode;
      }
      if (nextNode != null) {
	LeafNode nn = nextNode.dissoc(nowner,k);
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
  public static final class BitmapNode implements INode {
    public final TrieBase owner;
    public final int shift;
    int bitmap;
    INode[] data;

    public BitmapNode(TrieBase _owner, int _bitmap, int _shift, INode[] _data) {
      owner = _owner;
      bitmap = _bitmap;
      shift = _shift;
      data = _data;
    }

    public BitmapNode(TrieBase _owner, int _bitmap, int _shift) {
      owner = _owner;
      bitmap = _bitmap;
      shift = _shift;
      data = new INode[nextPow2(Integer.bitCount(_bitmap))];
    }

    public BitmapNode(TrieBase _owner, int _shift, LeafNode leaf) {
      owner = _owner;
      shift = _shift;
      bitmap = leaf != null ? bitpos(_shift, leaf.hashcode) : 0;
      data = new INode[] {leaf, null, null, null};
    }

    public final BitmapNode clone(TrieBase nowner) {
      nowner = nowner == null ? owner : nowner;
      final INode[] srcData = data;
      final int bm = bitmap;
      final int len = Integer.bitCount(bm);
      final INode[] newData = new INode[Math.max(4, nextPow2(len))];
      for (int idx = 0; idx < len; ++idx) {
	newData[idx] = srcData[idx].clone(nowner);
      }
      return new BitmapNode(nowner, bitmap, shift, newData);
    }

    public final BitmapNode refIndexes(TrieBase nowner, int sidx, int eidx) {
      if (eidx > 31)
	throw new RuntimeException("End idx (inclusive) must be less than 32");
      final int bm = bitmap;
      int bitmapMax = highBits(sidx, eidx);
      int newBitmap = bm & bitmapMax;
      if (newBitmap == 0)
	return null;
      if(newBitmap == bm) {
	return this;
      }

      int nelems = Integer.bitCount(newBitmap);
      INode[] odata = new INode[Math.max(4, nextPow2(nelems))];
      final INode[] mdata = data;
      int pidx = 0;
      for (int didx = sidx; didx <= eidx; ++didx) {
	final int bitpos = 1 << didx;
	if ((newBitmap & bitpos) != 0) {
	  odata[pidx] = mdata[index(bm, bitpos)];
	  ++pidx;
	}
      }
      return new BitmapNode(nowner, newBitmap, shift, odata);
    }

    public final LeafNode getOrCreate(Object k, int hash) {
      final int bpos = bitpos(shift, hash);
      final INode[] objData = data;
      final int objLen = objData.length;
      int bm = bitmap;
      final int index = index(bm,bpos);
      if ((bm & bpos) == 0) {
	bm |= bpos;
	final LeafNode retval = new LeafNode(owner, k, hash);
	data = insert(objData, retval, index, Integer.bitCount(bm), false);
	bitmap = bm;
	return retval;
      }
      final Object entry = objData[index];
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (hash == lf.hashcode) {
	  return lf.getOrCreate(k);
	} else {
	  final BitmapNode node = new BitmapNode(owner, incShift(shift), lf);
	  objData[index] = node;
	  return node.getOrCreate(k, hash);
	}
      } else {
	return ((BitmapNode)entry).getOrCreate(k, hash);
      }
    }

    public final LeafNode get(Object k, int hash) {
      final int bpos = bitpos(shift, hash);
      final int bm = bitmap;
      if ((bm & bpos) != 0) {
	final Object[] objData = data;
	final int index = index(bm,bpos);
	final Object entry = objData[index];
	final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
	if (lf != null) {
	  if (hash == lf.hashcode) {
	    return lf.get(k);
	  }
	} else {
	  return ((BitmapNode)entry).get(k, hash);
	}
      }
      return null;
    }

    public final INode remove(Object k, int hash, Box b, boolean collapse) {
      final int bpos = bitpos(shift, hash);
      int bm = bitmap;
      if ((bm & bpos) == 0)
	return this;
      final INode[] objData = data;
      final int index = index(bm,bpos);
      INode entry = objData[index];
      INode nentry = null;
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (hash != lf.hashcode)
	  return this;
	nentry = lf.remove(k, b);
      } else {
	nentry = ((BitmapNode)entry).remove(k, hash, b, true);
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
	    final INode item = index == 0 ? objData[1] : objData[0];
	    if (item instanceof LeafNode)
	      return item;
	  } else if (nentry instanceof LeafNode) {
	    return nentry;
	  }
	}
      }
      bitmap = bm;
      if (removed) {
	data = BitmapTrieCommon.remove(objData, index, nelems, false);
      } else {
	objData[index] = nentry;
      }
      return this;
    }

    public final BitmapNode assoc(TrieBase nowner, Object _k, int hash, Object _v) {
      final int bpos = bitpos(shift, hash);
      final boolean forceCopy = nowner != owner;
      final int bm = bitmap;
      if ((bm & bpos) == 0) {
	final int nbm = bm | bpos;
	final INode[] dstData = insert(data, new LeafNode(nowner, _k, hash, _v),
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
	final INode[] srcData = data;
	final INode[] dstData = forceCopy ? srcData.clone() : srcData;
	final int index = index(bm,bpos);
	final INode curVal = dstData[index];
	if (curVal instanceof LeafNode) {
	  final LeafNode lf = (LeafNode)curVal;
	  if (lf.hashcode == hash) {
	    dstData[index] = lf.assoc(nowner, _k, _v);
	  } else {
	    BitmapNode node = new BitmapNode(nowner, incShift(shift), lf);
	    dstData[index] = node.assoc(nowner, _k, hash, _v);
	  }
	} else {
	  final BitmapNode node = (BitmapNode)curVal;
	  dstData[index] = node.assoc(nowner,_k,hash,_v);
	}
	return forceCopy ? new BitmapNode(nowner, bm, shift, dstData) : this;
      }
    }

    public INode dissoc(TrieBase nowner, Object _k, int hash, boolean collapse) {
      final int bpos = bitpos(shift, hash);
      int bm = bitmap;
      if((bm & bpos) == 0)
	return this;
      final INode[] srcData = data;
      final int index = index(bm,bpos);
      INode entry = srcData[index];
      INode nentry = null;
      final LeafNode lf = entry instanceof LeafNode ? (LeafNode)entry : null;
      if (lf != null) {
	if (lf.hashcode != hash)
	  return this;
	nentry = lf.dissoc(nowner, _k);
      } else {
	nentry = ((BitmapNode)entry).dissoc(nowner, _k, hash, true);
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
	    INode leftover = srcData[index == 0 ? 1 : 0];
	    if (leftover instanceof LeafNode)
	      return leftover;
	  } else {
	    if (nentry instanceof LeafNode)
	      return nentry;
	  }
	}
      }
      boolean forceCopy = nowner != owner;
      INode[] dstData;
      if(removed)
	dstData = BitmapTrieCommon.remove(srcData, index, nelems, forceCopy);
      else {
	dstData = forceCopy ? srcData.clone() : srcData;
	dstData[index] = nentry;
      }
      if (forceCopy) {
	return new BitmapNode(nowner, bm, shift, dstData);
      } else {
	data = dstData;
	bitmap = bm;
	return this;
      }
    }

    public final int countLeaves() {
      final INode[] mdata = data;
      final int nelems = Integer.bitCount(bitmap);
      int sum = 0;
      for (int idx = 0; idx < nelems; ++idx)
	sum += mdata[idx].countLeaves();
      return sum;
    }

    static class BitmapNodeIterator implements LeafNodeIterator {
      final INode[] data;
      int idx;
      final int endidx;
      LeafNodeIterator nextObj;
      BitmapNodeIterator(INode[] _data, int _endidx) {
	data = _data;
	idx = -1;
	endidx = _endidx;
	advance();
      }
      BitmapNodeIterator(INode[] _data) {
	this(_data, _data.length);
      }
      void advance() {
	if (nextObj != null && nextObj.hasNext()) {
	    return;
	}
	int curIdx = idx + 1;
	final INode[] objData = data;
	final int len = endidx;
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

      public ILeaf nextLeaf() {
	if (nextObj == null)
	  throw new UnsupportedOperationException();
	ILeaf lf = nextObj.nextLeaf();
	advance();
	return lf;
      }
    }

    public final LeafNodeIterator iterator() { return new BitmapNodeIterator(data); }


    @SuppressWarnings("unchecked")
    static final Object mapValue(LeafNode lhs, LeafNode rhs, BiFunction valueMapper) {
      if (lhs == null) return rhs.v;
      if (rhs == null) return lhs.v;
      return valueMapper.apply(lhs.v, rhs.v);
    }

    @SuppressWarnings("unchecked")
    public final BitmapNode union(TrieBase nowner, BitmapNode other, BiFunction valueMapper) {
      int mbm = bitmap;
      final int obm = other.bitmap;
      INode[] mdata = data;
      final INode[] odata = other.data;
      boolean copy = owner != nowner;
      for (int idx = 0; idx < 32; ++idx) {
	final int bitpos = 1 << idx;
	final boolean mvalid = (mbm & bitpos) != 0;
	final boolean ovalid = (obm & bitpos) != 0;
	//If we care about this position at all
	if (ovalid) {
	  //If we do not have an entry in this position - this is a fast path where we
	  //simply reference the nodes.
	  final int oidx = index(obm, bitpos);
	  final INode oobj = odata[oidx];
	  if (!mvalid) {
	    mbm = mbm | bitpos;
	    final int midx = index(mbm, bitpos);
	    mdata = insert(mdata, oobj, midx, Integer.bitCount(mbm), copy);
	    copy = false;
	  } else {
	    final int midx = index(mbm, bitpos);
	    final INode mobj = mdata[midx];
	    mdata = copy ? mdata.clone() : mdata;
	    copy = false;
	    if (mobj instanceof LeafNode) {
	      LeafNode mlf = (LeafNode) mobj;
	      final Object key = mlf.key();
	      if(oobj instanceof LeafNode) {
		LeafNode olf = (LeafNode)oobj;
		if (mlf.hashcode == olf.hashcode) {
		  for(; olf != null; olf = olf.nextNode) {
		    mlf = mlf.union(nowner, olf.k, olf.v, valueMapper);
		  }
		  mdata[midx] = mlf;
		} else { //mlf.hashcode != olf.hashcode
		  final BitmapNode node = new BitmapNode(nowner, 0, incShift(shift));
		  node.getOrCreate(olf.k, olf.hashcode).v = olf.v;
		  node.getOrCreate(mlf.k, mlf.hashcode).v = mlf.v;
		  mdata[midx] = node;
		}
	      } else { //oobj instanceof BitmapNode
		final BitmapNode onode = (BitmapNode)oobj;
		mdata[midx] = onode.assoc(nowner, mlf.k, mlf.hashcode,
					  mapValue(mlf,
						   onode.get(mlf.key(), mlf.hashcode),
						   valueMapper));
	      }
	    } else { //mobj instanceof BitmapNode
	      final BitmapNode mnode = (BitmapNode) mobj;
	      if(oobj instanceof LeafNode) {
		final LeafNode olf = (LeafNode)oobj;
		mdata[midx] = mnode.assoc(nowner, olf.k, olf.hashcode,
					  mapValue(mnode.get(olf.k, olf.hashcode),
						   olf,
						   valueMapper));
	      } else {
		mdata[midx] = mnode.union(nowner, (BitmapNode)oobj, valueMapper);
	      }
	    }
	  }
	}
      }
      return new BitmapNode(nowner, mbm, shift, mdata);
    }

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
    Function<ILeaf,Object> tfn;
    LeafNode nullEntry;
    LeafNodeIterator rootIter;
    HTIterator(Function<ILeaf,Object> _tfn, LeafNode _nullEntry, LeafNodeIterator _rootIter) {
      tfn = _tfn;
      nullEntry = _nullEntry;
      rootIter = _rootIter;
    }

    public boolean hasNext() { return nullEntry != null || rootIter.hasNext(); }

    public ILeaf nextLeaf() {
      if (nullEntry != null) {
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

  public static Function<ILeaf,Object> valIterFn = lf -> lf.val();
  public static Function<ILeaf,Object> keyIterFn = lf -> lf.key();
  public static Function<ILeaf,Object> entryIterFn = lf -> new MapEntry(lf.key(), lf.val());
  public static Function<ILeaf,Object> identityIterFn = lf -> lf;

  protected final HashProvider hp;
  protected int count;
  protected BitmapNode root;
  protected LeafNode nullEntry;
  protected final IPersistentMap meta;

  public BitmapTrie(HashProvider _hp, int _c, BitmapNode r, LeafNode ne, IPersistentMap _meta) {
    hp = _hp;
    count = _c;
    root = r;
    nullEntry = ne;
    meta = _meta;
  }

  public BitmapTrie(HashProvider _hp) {
    count = 0;
    nullEntry = null;
    root = new BitmapNode(this, 0, 0, new INode[4]);
    hp = _hp;
    meta = null;
  }

  public BitmapTrie() {
    this(hashcodeProvider);
  }

  //Unsafe shallow clone version
  BitmapTrie(BitmapTrie other, boolean marker) {
    hp = other.hp;
    count = other.count;
    root = other.root;
    nullEntry = other.nullEntry;
    meta = other.meta;
  }

  public BitmapTrie(BitmapTrie other) {
    hp = other.hp;
    count = 0;
    nullEntry = nullEntry  != null ? other.nullEntry.clone(this) : null;
    root = other.root != null ? other.root.clone(this) : null;
    meta = other.meta;
  }

  BitmapTrie shallowClone(IPersistentMap newMeta) {
    return new BitmapTrie(this, true);
  }

  public BitmapTrie shallowClone() {
    return shallowClone(meta);
  }

  BitmapTrie deepClone() {
    return new BitmapTrie(this);
  }

  public final void inc() { ++count; }
  public final void dec() { --count; }
  public final int hash(Object obj) { return hp.hash(obj); }
  public final boolean equals(Object lhs, Object rhs) { return hp.equals(lhs, rhs); }

  public final int size() {
    if (count == -1) {
      final int nc = nullEntry != null ? 1 : 0;
      count = nc + root.countLeaves();
    }
    return count;
  }


  final LeafNode getNode(Object key) {
    if(key == null)
      return nullEntry;
    return root.get(key, hp.hash(key));
  }

  final LeafNode getOrCreate(Object key) {
    if (key == null) {
      if (nullEntry == null) {
	nullEntry = new LeafNode(this, null, 0);
      }
      return nullEntry;
    } else {
      return root.getOrCreate(key, hp.hash(key));
    }
  }

  public void clear() {
    count = 0;
    nullEntry = null;
    root.bitmap = 0;
    root.data = new INode[4];
  }

  final LeafNodeIterator iterator(Function<ILeaf,Object> fn) {
    return new HTIterator(fn, nullEntry, root.iterator());
  }

  class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>> {

    final boolean allowsClear;

    EntrySet(boolean _allowsClear) {
      allowsClear = _allowsClear;
    }

    public final int size() {
      return BitmapTrie.this.size();
    }

    public final void clear() {
      if (allowsClear) {
	BitmapTrie.this.clear();
      }
      else
	throw new RuntimeException("Unimplemented");
    }

    public final Iterator<Map.Entry<K,V>> iterator() {
      @SuppressWarnings("unchecked") Iterator<Map.Entry<K,V>> retval = (Iterator<Map.Entry<K,V>>) BitmapTrie.this.iterator(entryIterFn);
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
      return BitmapTrie.this.size();
    }
    public final void clear() {
      if (allowsClear)
	BitmapTrie.this.clear();
      else
	throw new RuntimeException("Unimplemented");
    }

    public final Iterator<K> iterator() {
      @SuppressWarnings("unchecked") Iterator<K> retval = (Iterator<K>) BitmapTrie.this.iterator(keyIterFn);
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
    public final int size() { return BitmapTrie.this.size(); }
    public final void clear() {
      if (allowsClear)
	BitmapTrie.this.clear();
      else
	throw new RuntimeException("Unimplemented");
    }
    public final Iterator<V> iterator() {
      @SuppressWarnings("unchecked") Iterator<V> retval = (Iterator<V>) BitmapTrie.this.iterator(valIterFn);
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
  //Returned hashbase's count or size is incorrect.
  final BitmapTrie keyspaceSplit(int splitidx, int nsplits) {
    final int groupSize = 1024 / nsplits;
    final int leftover = 1024 % nsplits;
    final int startidx = (splitidx * groupSize) + (splitidx < leftover ? splitidx : leftover);
    final int localsize = groupSize + (splitidx < leftover ? 1 : 0);
    final int endidx = startidx + localsize;
    /* out.println("groupsize: " + String.valueOf(groupSize) + */
    /* 		" splitidx: " + String.valueOf(splitidx) + */
    /* 		" localsize: " + String.valueOf(localsize) + */
    /* 		" startidx: " + String.valueOf(startidx) + */
    /* 		" endidx: " + String.valueOf(endidx)); */
    BitmapTrie retval = new BitmapTrie(hp);
    retval.count = -1;
    int obitmap = 0;
    INode[] odata = retval.root.data;
    final int rbitmap = root.bitmap;
    final INode[] rdata = root.data;
    if (startidx == 0 && nullEntry != null) {
      retval.nullEntry = nullEntry;
    }
    for(int idx = startidx; idx < endidx; ++idx) {
      final int rootgroup = idx / 32;
      final int subgroup = idx % 32;
      final int rbitpos = 1 << rootgroup;
      if ((rbitmap & rbitpos) != 0) {
	final INode rnode = rdata[index(rbitmap,rbitpos)];
	//If we can fast-path this and be done.
	if(subgroup == 0 && (((idx + 32) <= endidx) || (rnode instanceof LeafNode))) {
	  obitmap |= rbitpos;
	  final int oidx = index(obitmap,rbitpos);
	  final int onelems = Integer.bitCount(obitmap);
	  odata = insert(odata, rnode, oidx, onelems, false);
	  idx += 31;
	} else if (rnode instanceof BitmapNode) {
	  //Inclusive leftover.
	  final int subleftover = Math.min(31 - subgroup, endidx - idx - 1);
	  final int endgroup = subgroup + subleftover;
	  /* out.println("subgroup: " + String.valueOf(subgroup) + */
	  /* 	      " endgroup: " + String.valueOf(endgroup)); */
	  final BitmapNode onode = ((BitmapNode)rnode).refIndexes(retval, subgroup, endgroup);
	  idx += subleftover;
	  if(onode != null) {
	    obitmap |= rbitpos;
	    final int oidx = index(obitmap,rbitpos);
	    final int onelems = Integer.bitCount(obitmap);
	    odata = insert(odata, onode, oidx, onelems, false);
	  }
	}
      }
    }
    //Make that retval's count is incorrect.  This is the price to pay for not keeping
    //sub-counts on the nodes and still trying to share structure.
    retval.root.bitmap = obitmap;
    retval.root.data = odata;
    retval.count = -1;
    return retval;
  }

  final BitmapTrie[] splitBases(int nsplits ) {
    nsplits = Math.min(nsplits, 1024);
    final int nelemsPerSplit = Math.max(1, 1024 / nsplits);
    BitmapTrie[] retval = new BitmapTrie[nsplits];
    for (int idx = 0; idx < nsplits; ++idx ) {
      retval[idx] = keyspaceSplit(idx, nsplits);
    }
    return retval;
  }

  final Iterator[] splitIterators(int nsplits, Function<ILeaf,Object> fn) {
    BitmapTrie[] bases = splitBases(nsplits);
    int nbases = bases.length;
    Iterator[] retval = new Iterator[nbases];
    for (int idx = 0; idx < nbases; ++idx)
      retval[idx] = bases[idx].iterator(fn);
    return retval;
  }

  public final Iterator[] splitKeys(int nsplits ) {
    return splitIterators(nsplits, keyIterFn);
  }
  public final Iterator[] splitValues(int nsplits ) {
    return splitIterators(nsplits, valIterFn);
  }
  public final Iterator[] splitEntries(int nsplits ) {
    return splitIterators(nsplits, entryIterFn);
  }

  final void serialTraversal(Consumer<ILeaf> action) {
    HTIterator iter = new HTIterator(identityIterFn, nullEntry, root.iterator());
    while(iter.hasNext()) {
      action.accept(iter.nextLeaf());
    }
  }

  final void parallelTraversal(Consumer<ILeaf> action, ExecutorService executor,
			       int parallelism) throws InterruptedException,
						       ExecutionException {
    if (ForkJoinTask.inForkJoinPool()
	&& executor == ForkJoinPool.commonPool()) {
      serialTraversal(action);
    }
    final int nelems = size();
    int splits = Math.min(1024, parallelism);
    BitmapTrie[] bases = splitBases(splits);
    final int nTasks = bases.length;
    final Future[] tasks = new Future[nTasks];
    for(int idx = 0; idx < nTasks; ++idx) {
      final int localIdx = idx;
      tasks[localIdx] = executor.submit(new Runnable() {
	  public void run() {
	    bases[localIdx].serialTraversal(action);
	  }
	});
    }
    //Finish iteration
    for(int idx = 0; idx < nTasks; ++idx) {
      tasks[idx].get();
    }
  }

  final void parallelTraversal(Consumer<ILeaf> action) throws InterruptedException,
							      ExecutionException {
    parallelTraversal(action, ForkJoinPool.commonPool(),
		      ForkJoinPool.getCommonPoolParallelism());
  }

  @SuppressWarnings("unchecked")
  final void forEach(BiConsumer action) {
    serialTraversal(lf -> action.accept(lf.key(), lf.val()));
  }
  @SuppressWarnings("unchecked")
  final void parallelForEach(BiConsumer action, ExecutorService executor,
			     int parallelism) throws Exception {
    parallelTraversal(lf -> action.accept(lf.key(), lf.val()), executor, parallelism);
  }
  @SuppressWarnings("unchecked")
  final void parallelForEach(BiConsumer action) throws Exception {
    parallelTraversal(lf -> action.accept(lf.key(), lf.val()));
  }

  @SuppressWarnings("unchecked")
  final void parallelUpdateValues(BiFunction action, ExecutorService executor,
				  int parallelism) throws Exception {
    parallelTraversal(lf->lf.val(action.apply(lf.key(), lf.val())), executor, parallelism);
  }
  @SuppressWarnings("unchecked")
  final void parallelUpdateValues(BiFunction action) throws Exception {
    parallelTraversal(lf->lf.val(action.apply(lf.key(), lf.val())));
  }

  final Object remove(Object key) {
    if (key == null) {
      if (nullEntry != null) {
	final Object retval = nullEntry.val();
	nullEntry = null;
	dec();
	return retval;
      }
      return null;
    }
    Box b = new Box();
    root.remove(key, hp.hash(key), b, false);
    return b.obj;
  }

  final BitmapTrie assoc(Object key, Object val) {
    if (key == null) {
      if (nullEntry == null)
	nullEntry = new LeafNode(this, key, 0, val);
      else
	nullEntry = nullEntry.assoc(this, key, val);
    } else {
      root = root.assoc(this, key, hp.hash(key), val);
    }
    return this;
  }

  //Dissoc.  No check for null key identity - always returns this
  final BitmapTrie dissoc(Object key) {
    if(key == null) {
      if(nullEntry != null) {
	nullEntry = null;
	dec();
      }
    } else {
      root = (BitmapNode)root.dissoc(this,key,hp.hash(key),false);
    }
    return this;
  }

  final Object getOrDefault(Object key, Object defaultValue) {
    LeafNode lf = getNode(key);
    return lf == null ? defaultValue : lf.val();
  }

  public Object get(Object k) {
    return getOrDefault(k,null);
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

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import clojure.lang.MapEntry;
//Metadata is one of the truly brilliant ideas from Clojure
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IReduceInit;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;


class BitmapTrie implements IObj, TrieBase {

  interface BitmapTrieOwner {
    BitmapTrie bitmapTrie();
  }

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
    public final LeafNode valueClone(TrieBase nowner, Iterator valIter) {
      nowner = nowner == null ? owner : nowner;
      return new LeafNode(nowner, k, hashcode, valIter.next(),
			  nextNode != null ? nextNode.valueClone(nowner, valIter) : null);
    }
    public final LeafNode setOwner(TrieBase nowner) {
      if (owner == nowner)
	return this;
      return new LeafNode(nowner, this);
    }
    public final void append(LeafNode n) {
      if(nextNode == null)
	nextNode = n;
      else
	nextNode.append(n);
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
    public final LeafNode getOrCreate(Object _k, int hashcode) {
      if (owner.equals(k,_k)) {
	return this;
      } else if (nextNode != null) {
	return nextNode.getOrCreate(_k,hashcode);
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
    public final LeafNode get(Object _k, int hc) {
      if (hc == hashcode)
	return get(_k);
      return null;
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

    public final LeafNode assoc(TrieBase nowner, Object _k, int hash, Object _v) {
      LeafNode retval = setOwner(nowner);
      if (nowner.equals(_k,k)) {
	retval.v = _v;
      } else {
	if (retval.nextNode != null) {
	  retval.nextNode = retval.nextNode.assoc(nowner, _k, hash, _v);
	} else {
	  retval.nextNode = new LeafNode(nowner, _k, hash, _v);
	}
      }
      return retval;
    }

    @SuppressWarnings("unchecked")
    public final LeafNode immutUpdate(TrieBase nowner, BiFunction bfn) {
      LeafNode retval = setOwner(nowner);
      retval.val(bfn.apply(retval.k, retval.v));
      retval.nextNode = retval.nextNode != null ? retval.nextNode.immutUpdate(nowner, bfn)
	: null;
      return retval;
    }

    @SuppressWarnings("unchecked")
    public final LeafNode immutUpdate(TrieBase nowner, Object key, int _hashcode, IFn fn) {
      LeafNode retval = setOwner(nowner);
      if (nowner.equals(k, key)) {
	retval.v = fn.invoke(retval.v);
	retval.nextNode = nextNode;
	return retval;
      }
      retval.nextNode = nextNode != null ? nextNode.immutUpdate(nowner, key, _hashcode, fn)
	: new LeafNode(nowner, key, _hashcode, fn.invoke(null));
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

    public final INode dissoc(TrieBase nowner, Object _k, int _hashcode) {
      if (hashcode == _hashcode) {
	return dissoc(nowner, k);
      } else {
	return this;
      }
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
    public Object reduce(IFn rfn, Object acc) {
      for ( LeafNode curNode = this; curNode != null && !RT.isReduced(acc);
	    curNode = curNode.nextNode ) {
	acc = rfn.invoke(acc, curNode);
      }
      return acc;
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

    public final BitmapNode valueClone(TrieBase nowner, Iterator valIter) {
      final INode[] srcData = data;
      final int bm = bitmap;
      final int len = Integer.bitCount(bm);
      final INode[] newData = new INode[Math.max(4, nextPow2(len))];
      for (int idx = 0; idx < len; ++idx)
	newData[idx] = srcData[idx].valueClone(nowner, valIter);

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
      if (entry instanceof BitmapNode) {
	return ((BitmapNode)entry).getOrCreate(k, hash);
      } else {
	LeafNode lf = (LeafNode)entry;
	if (hash == lf.hashcode) {
	  return lf.getOrCreate(k, hash);
	} else {
	  final BitmapNode node = new BitmapNode(owner, incShift(shift), lf);
	  objData[index] = node;
	  return node.getOrCreate(k, hash);
	}
      }
    }

    public final LeafNode get(Object k, int hash, int nshift, HashProvider hp) {
      final int bpos = bitpos(nshift, hash);
      final int bm = bitmap;
      if ((bm & bpos) != 0) {
	final Object entry = data[index(bm,bpos)];
	if (entry instanceof BitmapNode) {
	  return ((BitmapNode)entry).get(k,hash,incShift(nshift),hp);
	} else {
	  for (LeafNode lf = (LeafNode)entry; lf != null; lf = lf.nextNode) {
	    if (hp.equals(lf.k,k))
	      return lf;
	  }
	}
      }
      return null;
    }

    public final LeafNode get(Object k, int hash) {
      return get(k,hash,shift,owner);
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

    //Concrete assoc operation shared between assoc, union, and intersection
    static final INode unionAssoc(TrieBase owner, INode entry, Object key, int hashcode, int shift, Object val) {
      if(entry == null)
	return new LeafNode(owner, key, hashcode, val);

      if (entry instanceof LeafNode) {
	LeafNode lf = (LeafNode)entry;
	if (hashcode == lf.hashcode)
	  return lf.assoc(owner, key, hashcode, val);
	entry = new BitmapNode(owner, incShift(shift), lf);
      }

      return ((BitmapNode)entry).assoc(owner, key, hashcode, val);
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
	dstData[index] = unionAssoc(nowner, dstData[index], _k, hash, shift, _v);
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
      final INode entry = srcData[index];
      final INode nentry = entry.dissoc(nowner, _k, hash);
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

    public final INode dissoc(TrieBase nowner, Object k, int hash) {
      return dissoc(nowner, k, hash, true);
    }

    public final BitmapNode immutUpdate(TrieBase nowner, BiFunction bfn) {
      final boolean copy = owner != nowner;
      final INode[] mdata = copy ? data.clone() : data;
      final int nelems = Integer.bitCount(bitmap);
      for (int idx = 0; idx < nelems; ++idx) {
	mdata[idx] = mdata[idx].immutUpdate(nowner, bfn);
      }
      return copy ? new BitmapNode(nowner, bitmap, shift, mdata) : this;
    }
    @SuppressWarnings("unchecked")
    public final BitmapNode immutUpdate(TrieBase nowner, Object key, int hashcode, IFn fn) {
      final int bpos = bitpos(shift, hashcode);
      if ((bitmap & bpos) != 0) {
	final boolean copy = owner != nowner;
	final int idx = index(bitmap, bpos);
	final INode[] mdata = copy ? data.clone() : data;
	mdata[idx] = mdata[idx].immutUpdate(nowner, key, hashcode, fn);
	return copy ? new BitmapNode(nowner, bitmap, shift, mdata) : this;
      } else {
	//There is no known value at that position.
	return assoc(nowner, key, hashcode, fn.invoke(null));
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
      final int len;
      int idx;
      LeafNode leaf;
      LeafNodeIterator nextObj;
      BitmapNodeIterator(INode[] _data, int bitmap) {
	data = _data;
	len = Integer.bitCount(bitmap);
	idx = -1;
	leaf = null;
	nextObj = null;
	advance();
      }
      final void advance() {
	if (nextObj != null && nextObj.hasNext()) {
	    return;
	}
	if (leaf != null) {
	  leaf = leaf.nextNode;
	}
	if (leaf != null)
	  return;

	int curIdx = idx + 1;
	idx = curIdx;
	if (curIdx < len) {
	  final Object iterObj = data[curIdx];
	  if (iterObj instanceof LeafNode)
	    leaf = (LeafNode)iterObj;
	  else
	    nextObj = ((BitmapNode)iterObj).iterator();
	} else {
	  nextObj = null;
	  leaf = null;
	}
      }

      public final boolean hasNext() {
	return nextObj != null || leaf != null;
      }

      public final ILeaf nextLeaf() {
	ILeaf lf;
	if (leaf != null) {
	  lf = leaf;
	} else if (nextObj != null) {
	  lf = nextObj.nextLeaf();
	} else {
	  throw new UnsupportedOperationException();
	}
	advance();
	return lf;
      }
    }

    public final LeafNodeIterator iterator() { return new BitmapNodeIterator(data, bitmap); }

    public Object reduce(IFn rfn, Object acc) {
      final INode[] md = data;
      final int nelems = Integer.bitCount(bitmap);
      for(int idx = 0; idx < nelems && !RT.isReduced(acc); ++idx)
	acc = md[idx].reduce(rfn, acc);
      return acc;
    }

    @SuppressWarnings("unchecked")
    static final Object mapValue(ILeaf lhs, ILeaf rhs, BiFunction valueMapper) {
      if (lhs == null) return rhs.val();
      if (rhs == null) return lhs.val();
      return valueMapper.apply(lhs.val(), rhs.val());
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
	    mdata = insert(mdata, oobj, index(mbm, bitpos), Integer.bitCount(mbm), copy);
	    copy = false;
	  } else {
	    final int midx = index(mbm, bitpos);
	    final INode mobj = mdata[midx];
	    mdata = copy ? mdata.clone() : mdata;
	    copy = false;
	    INode entry = null;
	    if (mobj instanceof LeafNode) {
	      entry = oobj;
	      for(LeafNode mlf = (LeafNode)mobj; mlf != null; mlf = mlf.nextNode) {
		ILeaf olf = oobj.get(mlf.k, mlf.hashcode);
		entry = unionAssoc(nowner, entry, mlf.k, mlf.hashcode, shift, mapValue(mlf, olf, valueMapper));
	      }
	    } else if (oobj instanceof LeafNode) {
	      entry = mobj;
	      for(LeafNode olf = (LeafNode)oobj; olf != null; olf = olf.nextNode) {
		ILeaf mlf = mobj.get(olf.k, olf.hashcode);
		entry = unionAssoc(nowner, entry, olf.k, olf.hashcode, shift, mapValue(mlf, olf, valueMapper));
	      }
	    } else {
	      entry = ((BitmapNode)mobj).union(nowner, (BitmapNode)oobj, valueMapper);
	    }
	    mdata[midx] = entry;
	  }
	}
      }
      return new BitmapNode(nowner, mbm, shift, mdata);
    }

    public final INode difference(BitmapTrie nowner, BitmapNode rhs, boolean collapse) {
      int mbm = bitmap;
      final int obm = rhs.bitmap;
      final int overlap = mbm & obm;
      if (overlap == 0)
	return this;
      INode[] mdata = data;
      final INode[] odata = rhs.data;
      boolean copy = owner != nowner;
      for(int idx = 31; idx >= 0 && mbm != 0; --idx) {
	final int bpos = 1 << idx;
	//Remove shared values.
	if ((overlap & bpos) != 0) {
	  final INode mobj = mdata[index(mbm, bpos)];
	  final INode oobj = odata[index(obm, bpos)];
	  INode entry = mobj;
	  if (mobj instanceof LeafNode) {
	    for (LeafNode lf = (LeafNode)mobj; lf != null && entry != null; lf = lf.nextNode) {
	      if (oobj.get(lf.k, lf.hashcode) != null) {
		entry = entry.dissoc(nowner, lf.key(), lf.hashcode);
	      }
	    }
	  } else if (oobj instanceof LeafNode) {
	    for (LeafNode lf = (LeafNode)oobj; lf != null && entry != null; lf = lf.nextNode) {
	      entry = entry.dissoc(nowner, lf.key(), lf.hashcode);
	    }
	  } else {
	    entry = ((BitmapNode)mobj).difference(nowner, (BitmapNode)oobj, true);
	  }

	  if (entry == null) {
	    mbm ^= bpos;
	    mdata = BitmapTrieCommon.remove(mdata, index(mbm, bpos),
					    Integer.bitCount(mbm), copy);
	    copy = false;
	  } else {
	    mdata = copy ? mdata.clone() : mdata;
	    copy = false;
	    mdata[index(mbm,bpos)] = entry;
	  }
	}
      } //end of foreach bit loop
      if (collapse) {
	int nelems = Integer.bitCount(mbm);
	if (nelems == 0) {
	  return null;
	}
	if(nelems == 1 && (mdata[0] instanceof LeafNode))
	  return mdata[0];
      }
      if(mdata == data) {
	//We may have just removed nodes but not allocated a new array in the case
	//where owner == nowner;
	bitmap = mbm;
	return this;
      } else {
	return new BitmapNode(nowner, mbm, shift, mdata);
      }
    }

    @SuppressWarnings("unchecked")
    public final INode intersection(TrieBase nowner, BitmapNode other, BiFunction valueMapper, boolean collapse) {
      int mbm = bitmap;
      if (mbm == 0)
	return this;
      final int obm = other.bitmap;
      final int overlap = mbm & obm;
      if (overlap == 0) {
	if (collapse) {
	  return null;
	}
	else {
	  return new BitmapNode(nowner, 0, shift, new INode[4]);
	}
      }
      final INode[] mdata = data;
      final INode[] odata = other.data;
      INode[] rdata = null;
      int rbm = 0;
      int rnelems = 0;
      for(int idx = 0; idx < 32; ++idx) {
	int bpos = 1 << idx;
	if ((overlap & bpos) != 0) {
	  final INode mobj = mdata[index(mbm, bpos)];
	  final INode oobj = odata[index(obm, bpos)];
	  INode entry = null;
	  if (oobj instanceof LeafNode) {
	    for(LeafNode olf = (LeafNode)oobj; olf != null; olf = olf.nextNode) {
	      ILeaf mlf = mobj.get(olf.k, olf.hashcode);
	      if(mlf != null) {
		entry = unionAssoc(nowner, entry, olf.k, olf.hashcode, shift, valueMapper.apply(mlf.val(), olf.v));
	      }
	    }
	  } else if (mobj instanceof LeafNode) {
	    for (LeafNode mlf = (LeafNode)mobj; mlf != null; mlf = mlf.nextNode) {
	      ILeaf olf = oobj.get(mlf.k, mlf.hashcode);
	      if (olf != null) {
		entry = unionAssoc(nowner, entry, mlf.k, mlf.hashcode, shift, valueMapper.apply(mlf.v, olf.val()));
	      }
	    }
	  } else {
	    entry = ((BitmapNode)mobj).intersection(nowner, (BitmapNode)oobj, valueMapper, true);
	  }

	  if (entry != null) {
	    if (rdata == null)
	      rdata = new INode[4];
	    rbm |= bpos;
	    int ridx = rnelems;
	    ++rnelems;
	    rdata = insert(rdata, entry, ridx, rnelems, false);
	  }
	}
      } //end bitmap for idx
      if (collapse) {
	if (rdata == null)
	  return null;
	if (rnelems == 1 && (rdata[0] instanceof LeafNode))
	  return rdata[0];
      }
      return new BitmapNode(nowner, rbm, shift, rdata != null ? rdata : new INode[4]);
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

  public BitmapTrie(HashProvider _hp, IPersistentMap _meta, int nkeys) {
    count = 0;
    nullEntry = null;
    root = new BitmapNode(this, 0, 0, new INode[Math.max(32, nextPow2(nkeys))]);
    hp = _hp;
    meta = _meta;
  }

  public BitmapTrie(HashProvider _hp, IPersistentMap _meta) {
    this(_hp, _meta, 4);
  }

  public BitmapTrie(HashProvider _hp) {
    count = 0;
    nullEntry = null;
    root = new BitmapNode(this, 0, 0, new INode[4]);
    hp = _hp;
    meta = null;
  }

  public BitmapTrie(HashProvider _hp, IPersistentMap _meta, Object key, Object val) {
    hp = _hp;
    meta = _meta;
    //Increments count
    LeafNode lf = new LeafNode(this, key, _hp.hash(key), val);
    if (key == null) {
      nullEntry = lf;
      root = new BitmapNode(this, 0, 0, new INode[4]);
    } else {
      nullEntry = null;
      root = new BitmapNode(this, 0, lf);
    }
  }

  public BitmapTrie() {
    this(defaultHashProvider);
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

  /** Clone replacing values with values from valIter **/
  public BitmapTrie(BitmapTrie other, Iterator valIter) {
    hp = other.hp;
    count = 0;
    nullEntry = nullEntry  != null ? other.nullEntry.valueClone(this, valIter) : null;
    root = other.root != null ? other.root.valueClone(this, valIter) : null;
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
    return root.get(key, hp.hash(key), 0, hp);
  }

  final LeafNode getOrCreate(Object key, int hash) {
    if (key == null) {
      if (nullEntry == null) {
	nullEntry = new LeafNode(this, null, 0);
      }
      return nullEntry;
    } else {
      return root.getOrCreate(key, hash);
    }
  }

  final LeafNode getOrCreate(Object key) {
    return getOrCreate(key, hp.hash(key));
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

  static class TrieSpliterator implements Spliterator, IReduceInit {
    BitmapTrie root;
    BitmapTrie subtree;
    Iterator subtreeIter;
    int sidx;
    int eidx;
    final Function<ILeaf,Object> fn;
    public TrieSpliterator(BitmapTrie _root, int _sidx, int _eidx,
			   Function<ILeaf,Object> _fn) {
      root = _root;
      subtree = null;
      subtreeIter = null;
      sidx = _sidx;
      eidx = _eidx;
      fn = _fn;
    }
    public TrieSpliterator(BitmapTrie _root, Function<ILeaf,Object> _fn) {
      root = _root;
      subtree = null;
      subtreeIter = null;
      sidx = 0;
      eidx = 1024;
      fn = _fn;
    }
    BitmapTrie getSubtree() { if(subtree == null) subtree = root.keyspaceSplit(sidx,eidx); return subtree; }
    public int characteristics() { return Spliterator.DISTINCT | Spliterator.IMMUTABLE | Spliterator.SIZED; }
    public long estimateSize() { return getSubtree().size(); }
    public long getExactSizeIfKnown() { return estimateSize(); }
    @SuppressWarnings("unchecked")
    public boolean tryAdvance(Consumer c) {
      if(subtreeIter == null)
	subtreeIter = getSubtree().iterator(fn);
      final boolean retval = subtreeIter.hasNext();
      if(retval) c.accept(subtreeIter.next());
      return retval;
    }
    public Object reduce(IFn rfn, Object acc) {
      return getSubtree().reduceLeaves(new IFnDef() {
	  public Object invoke(Object acc, Object val) {
	    return rfn.invoke(acc, fn.apply((ILeaf)val));
	  }
	}, acc);
    }
    public Spliterator trySplit() {
      final int partLen = (eidx - sidx)/2;
      final int nextSidx = sidx + partLen;
      subtree = null;
      subtreeIter = null;
      final TrieSpliterator retval = new TrieSpliterator(root, nextSidx, eidx, fn);
      eidx = nextSidx;
      return retval;
    }
  };

  final Spliterator spliterator(Function<ILeaf,Object> fn) {
    return new TrieSpliterator(this, fn);
  }

  class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>> implements ITypedReduce {

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

    @SuppressWarnings("unchecked")
    public final Spliterator<Map.Entry<K,V>> spliterator() {
      return (Spliterator<Map.Entry<K,V>>)BitmapTrie.this.spliterator(entryIterFn);
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
    public Object reduce(IFn rfn) {
      return Reductions.iterReduce(this, rfn);
    }
    public Object reduce(IFn rfn, Object init) {
      return BitmapTrie.this.reduceEntries(rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      reduce( new IFnDef() {
	  public Object invoke(Object lhs, Object rhs) {
	    c.accept(rhs);
	    return c;
	  }
	}, c);
    }
  }

  final <K,V> Set<Map.Entry<K,V>> entrySet(Map.Entry<K,V> me, boolean allowsClear) {
    return new EntrySet<K,V>(allowsClear);
  }


  class KeySet<K> extends AbstractSet<K> implements ITypedReduce {
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

    @SuppressWarnings("unchecked")
    public final Spliterator<K> spliterator() {
      return (Spliterator<K>)BitmapTrie.this.spliterator(keyIterFn);
    }

    public final boolean contains(Object key) {
      return getNode(key) != null;
    }

    public Object reduce(IFn rfn, Object init) {
      return BitmapTrie.this.reduceKeys(rfn, init);
    }

    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      reduce( new IFnDef() {
	  public Object invoke(Object lhs, Object rhs) {
	    c.accept(rhs);
	    return c;
	  }
	}, c);
    }
  }

  final <K> Set<K> keySet(K k, boolean allowsClear) {
    return new KeySet<K>(allowsClear);
  }


  class ValueCollection<V>  extends AbstractCollection<V> implements ITypedReduce {
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
    @SuppressWarnings("unchecked")
    public final Spliterator<V> spliterator() {
      return (Spliterator<V>)BitmapTrie.this.spliterator(valIterFn);
    }
    public Object reduce(IFn rfn, Object init) {
      return BitmapTrie.this.reduceValues(rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      reduce( new IFnDef() {
	  public Object invoke(Object lhs, Object rhs) {
	    c.accept(rhs);
	    return c;
	  }
	}, c);
    }
  }
  final <V> Collection<V> values(V obj, boolean allowsClear) {
    return new ValueCollection<V>(allowsClear);
  }

  Object reduceLeaves(IFn rfn, Object acc) {
    if(nullEntry != null && !RT.isReduced(acc))
      acc = rfn.invoke(acc, nullEntry);
    return Reductions.unreduce(root != null ? root.reduce(rfn, acc) : acc);
  }

  public Object reduceEntries(IFn rfn, Object acc) {
    return reduceLeaves(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final ILeaf il = (ILeaf)v;
	  return rfn.invoke(acc, new FMapEntry<Object,Object>(il.key(), il.val()));
	}
      }, acc);
  }

  public Object reduceKeys(IFn rfn, Object acc) {
    return reduceLeaves(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final ILeaf il = (ILeaf)v;
	  return rfn.invoke(acc, il.key());
	}
      }, acc);
  }

  public Object reduceValues(IFn rfn, Object acc) {
    return reduceLeaves(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final ILeaf il = (ILeaf)v;
	  return rfn.invoke(acc, il.val());
	}
      }, acc);
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
  //Keyspaces is viewed as linear space 0-1024.  This encompasses the first 2 levels of the
  //bitmap trie.
  final BitmapTrie keyspaceSplit(final int startidx, final int endidx) {
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

  final void serialTraversal(Consumer<ILeaf> action) {
    HTIterator iter = new HTIterator(identityIterFn, nullEntry, root.iterator());
    while(iter.hasNext()) {
      action.accept(iter.nextLeaf());
    }
  }

  @SuppressWarnings("unchecked")
  final void forEach(BiConsumer action) {
    serialTraversal(lf -> action.accept(lf.key(), lf.val()));
  }

  final BitmapTrie immutUpdate(BiFunction action) {
    BitmapTrie retval = shallowClone();
    if (nullEntry != null) {
      retval.nullEntry = nullEntry.immutUpdate(retval, action);
    }
    retval.root = root.immutUpdate(retval, action);
    return retval;
  }

  @SuppressWarnings("unchecked")
  final BitmapTrie immutUpdate(Object key, IFn action) {
    final BitmapTrie retval = shallowClone();
    if (key == null) {
      if (nullEntry != null)
	retval.nullEntry = nullEntry.immutUpdate(retval, key, 0, action);
      else
	retval.nullEntry = new LeafNode(retval, null, 0, action.invoke(null));
    } else {
      retval.root = root.immutUpdate(retval, key, hp.hash(key), action);
    }
    return retval;
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
	nullEntry = nullEntry.assoc(this, key, 0, val);
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

  final Object get(Object key) {
    LeafNode lf = getNode(key);
    return lf == null ? null : lf.val();
  }

  @SuppressWarnings("unchecked")
  final BitmapTrie union(BitmapTrie other, BiFunction valueMapper, boolean inPlace) {
    BitmapTrie result = inPlace ? this : shallowClone();
    if (other.nullEntry != null) {
      if (nullEntry != null) {
	result.nullEntry = new LeafNode(result, null, 0,
					valueMapper.apply(nullEntry.val(),
							  other.nullEntry.val()));
      } else {
	result.nullEntry = other.nullEntry;
      }
    } else if (nullEntry != null) {
      result.nullEntry = nullEntry;
    }
    result.root = result.root.union(result, other.root, valueMapper);
    //Currently count is not maintained by to avoid unnecessary traversal of datastructures during
    //union operation.
    result.count = -1;
    return result;
  }

  final BitmapTrie union(BitmapTrie other, BiFunction valueMapper) {
    return union(other,valueMapper,false);
  }

  final BitmapTrie difference(BitmapTrie other) {
    BitmapTrie result = shallowClone();
    if (nullEntry != null && other.nullEntry != null) {
      result.nullEntry = null;
    }
    result.root = (BitmapNode)result.root.difference(result, other.root, false);
    //Currently count is not maintained by to avoid traversal of datastructures during
    //structural mapping.
    result.count = -1;
    return result;
  }

  @SuppressWarnings("unchecked")
  final BitmapTrie intersection(BitmapTrie other, BiFunction valueMapper) {
    BitmapTrie result = shallowClone();
    result.count = 0;
    if (nullEntry != null && other.nullEntry != null) {
      result.nullEntry = new LeafNode(result, null, 0, valueMapper.apply(nullEntry.v, other.nullEntry.v));
    }
    result.root = (BitmapNode)root.intersection(result, other.root, valueMapper, false);
    // For intersection the count is correct as we have to create new nodes in the trie for every
    // overlapping value.
    return result;
  }


  public BitmapTrie withMeta(IPersistentMap meta) {
    return shallowClone(meta);
  }

  public IPersistentMap meta() { return meta; }

  public void printNodes() {
    if (nullEntry != null)
      out.println("nullEntry: " + String.valueOf(nullEntry.val()));
    root.print();
  }

  static class IndexedIter implements Iterator {
    final int[] indexes;
    int idx;
    final Object[] values;
    final int nvals;
    IndexedIter(int[] _indexes, Object[] _values) {
      indexes = _indexes;
      idx = 0;
      values = _values;
      nvals = _indexes.length;
    }
    public boolean hasNext() { return idx < nvals; }
    public Object next() {
      final Object nextVal = values[indexes[idx]];
      ++idx;
      return nextVal;
    }
  }

  public static Function<Object[],BitmapTrie> makeFactory(HashProvider hp, Object[] keys) {
    final int nkeys = keys.length;
    final BitmapTrie srcTrie = new BitmapTrie(hp);
    for(int idx = 0; idx < nkeys; ++idx) {
      LeafNode lf = srcTrie.getOrCreate(keys[idx]);
      if (lf.v != null)
	throw new RuntimeException("Duplicate key detected: " + String.valueOf(keys[idx]));
      lf.v = idx;
    }
    final int[] indexes = new int[nkeys];
    LeafNodeIterator lfIter = srcTrie.iterator(identityIterFn);
    for(int idx = 0; idx < nkeys; ++idx) {
      ILeaf lf = lfIter.nextLeaf();
      indexes[idx] = (int)lf.val();
    }
    return new Function<Object[],BitmapTrie>() {
      public BitmapTrie apply(Object[] values) {
	if (values.length != nkeys)
	  throw new RuntimeException("Value array len != key array len");
	return new BitmapTrie(srcTrie, new IndexedIter(indexes, values));
      }
    };
  }
}

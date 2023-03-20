package ham_fisted;

import static ham_fisted.BitmapTrieCommon.*;
import static java.lang.System.out;

import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.Iterator;
import java.util.NoSuchElementException;
import clojure.lang.IFn;
import clojure.lang.RT;

final class LongLeafNode implements INode, ILeaf, Map.Entry, IMutList {
  public final TrieBase owner;
  public final int hashcode;
  public final long k;
  //compute-at support means we can modify v.
  Object v;
  LongLeafNode nextNode;
  public LongLeafNode(TrieBase _owner, long _k, int hc, Object _v, LongLeafNode nn) {
    owner = _owner;
    hashcode = hc;
    k = _k;
    v = _v;
    nextNode = nn;
    _owner.inc();
  }
  public LongLeafNode(TrieBase _owner, long _k, int hc, Object _v) {
    this(_owner, _k, hc, _v, null);
  }
  public LongLeafNode(TrieBase _owner, long _k, int hc) {
    this(_owner, _k, hc, null, null);
  }
  public LongLeafNode(TrieBase _owner, LongLeafNode prev) {
    owner = _owner;
    hashcode = prev.hashcode;
    k = prev.k;
    v = prev.v;
    nextNode = prev.nextNode;
  }
  public final LongLeafNode clone(TrieBase nowner) {
    nowner = nowner == null ? owner : nowner;
    return new LongLeafNode(nowner, k, hashcode, v,
			nextNode != null ? nextNode.clone(nowner) : null);
  }
  public final LongLeafNode valueClone(TrieBase nowner, Iterator valIter) {
    nowner = nowner == null ? owner : nowner;
    return new LongLeafNode(nowner, k, hashcode, valIter.next(),
			nextNode != null ? nextNode.valueClone(nowner, valIter) : null);
  }
  public final LongLeafNode setOwner(TrieBase nowner) {
    if (owner == nowner)
      return this;
    return new LongLeafNode(nowner, this);
  }
  public final void append(LongLeafNode n) {
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
  public final Object getKey() { return k; }
  public final Object getValue() { return v; }
  public final Object setValue(Object vv) { Object rv = v; v = vv; return rv; }
  public final int size() { return 2; }
  public final Object get(int idx) {
    if(idx == 0) return k;
    if(idx == 1) return v;
    throw new RuntimeException("Index out of range.");
  }
  //Mutable pathway
  public final LongLeafNode getOrCreate(long _k, int hashcode) {
    if (_k == k) {
      return this;
    } else if (nextNode != null) {
      return nextNode.getOrCreate(_k,hashcode);
    } else {
      final LongLeafNode retval = new LongLeafNode(owner, _k, hashcode);
      nextNode = retval;
      return retval;
    }
  }
  public final LongLeafNode get(Object _k) {
    if (owner.equals(k,_k)) {
      return this;
    } else if (nextNode != null) {
      return nextNode.get(_k);
    } else {
      return null;
    }
  }
  public final LongLeafNode get(long _k, int hc) {
    if (hc == hashcode)
      return get(_k);
    return null;
  }
  public final LongLeafNode get(Object _k, int hc) {
    return get((long)_k, hc);
  }
  public final LongLeafNode remove(long _k, Box b) {
    if(k == _k) {
      owner.dec();
      if(b != null) b.obj = v;
      return nextNode;
    }
    if(nextNode != null) {
      nextNode = nextNode.remove(_k,b);
    }
    return this;
  }

  public final LongLeafNode assoc(TrieBase nowner, long _k, int hash, Object _v) {
    LongLeafNode retval = setOwner(nowner);
    if (k == _k) {
      retval.v = _v;
    } else {
      if (retval.nextNode != null) {
	retval.nextNode = retval.nextNode.assoc(nowner, _k, hash, _v);
      } else {
	retval.nextNode = new LongLeafNode(nowner, _k, hash, _v);
      }
    }
    return retval;
  }

  @SuppressWarnings("unchecked")
  public final LongLeafNode immutUpdate(TrieBase nowner, BiFunction bfn) {
    LongLeafNode retval = setOwner(nowner);
    retval.val(bfn.apply(retval.k, retval.v));
    retval.nextNode = retval.nextNode != null ? retval.nextNode.immutUpdate(nowner, bfn)
      : null;
    return retval;
  }

  @SuppressWarnings("unchecked")
  public final LongLeafNode immutUpdate(TrieBase nowner, Object _key, int _hashcode, IFn fn) {
    final long key = (long)_key;
    LongLeafNode retval = setOwner(nowner);
    if (k == key) {
      retval.v = fn.invoke(retval.v);
      retval.nextNode = nextNode;
      return retval;
    }
    retval.nextNode = nextNode != null ? nextNode.immutUpdate(nowner, key, _hashcode, fn)
      : new LongLeafNode(nowner, key, _hashcode, fn.invoke(null));
    return retval;
  }

  public final LongLeafNode dissoc(TrieBase nowner, long _k) {
    if (k == _k) {
      nowner.dec();
      return nextNode;
    }
    if (nextNode != null) {
      LongLeafNode nn = nextNode.dissoc(nowner,_k);
      if (nn != nextNode) {
	LongLeafNode retval = setOwner(nowner);
	retval.nextNode = nn;
	return retval;
      }
    }
    return this;
  }

  public final INode dissoc(TrieBase nowner, Object _k, int _hashcode) {
    if (hashcode == _hashcode) {
      return dissoc(nowner, Casts.longCast(_k));
    } else {
      return this;
    }
  }

  static class LFIter implements LeafNodeIterator {
    LongLeafNode curNode;
    LFIter(LongLeafNode lf) {
      curNode = lf;
    }
    public boolean hasNext() { return curNode != null; }
    public LongLeafNode nextLeaf() {
      if (curNode == null)
	throw new NoSuchElementException();
      LongLeafNode retval = curNode;
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
    for ( LongLeafNode curNode = this; curNode != null && !RT.isReduced(acc);
	  curNode = curNode.nextNode ) {
      acc = rfn.invoke(acc, curNode);
    }
    return acc;
  }
}

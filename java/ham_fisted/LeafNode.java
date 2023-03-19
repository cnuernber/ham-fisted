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

final class LeafNode implements INode, ILeaf, Map.Entry, IMutList {
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
      if(b != null) b.obj = v;
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
      LeafNode nn = nextNode.dissoc(nowner,_k);
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
      return dissoc(nowner, _k);
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

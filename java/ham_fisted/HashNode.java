package ham_fisted;

import java.util.Map;
import java.util.Iterator;
import clojure.lang.IMapEntry;
import ham_fisted.IMutList;

public class HashNode implements Map.Entry, IMutList, IMapEntry {
  public final HashBase owner;
  public final int hashcode;
  public final Object k;
  //compute-at support means we can modify v.
  Object v;
  HashNode nextNode;

  public HashNode(HashBase _owner, Object _k, int hc, Object _v, HashNode nn) {
    owner = _owner;
    hashcode = hc;
    k = _k;
    v = _v;
    nextNode = nn;
    _owner.inc(this);
  }
  public HashNode(HashBase _owner, Object _k, int hc, Object _v) {
    this(_owner, _k, hc, _v, null);
  }
  public HashNode(HashBase _owner, Object _k, int hc) {
    this(_owner, _k, hc, null, null);
  }
  // Cloning constructor
  HashNode(HashBase _owner, HashNode prev) {
    owner = _owner;
    hashcode = prev.hashcode;
    k = prev.k;
    v = prev.v;
    nextNode = null;
  }
  public HashNode setOwner(HashBase nowner) {
    if (owner == nowner)
      return this;
    return new HashNode(nowner, this);
  }
  public HashNode clone(HashBase nowner) {
    HashNode rv = new HashNode(nowner, this);
    if(nextNode != null)
      rv.nextNode = nextNode.clone(nowner);
    return rv;
  }
  public final Object key() { return k; }
  public final Object val() { return v; }
  public final Object getKey() { return k; }
  public final Object getValue() { return v; }
  public Object setValue(Object vv) { Object rv = v; v = vv; return rv; }
  public final int size() { return 2; }
  public final Object get(int idx) {
    if(idx == 0) return k;
    if(idx == 1) return v;
    throw new RuntimeException("Index out of range.");
  }
  public HashNode assoc(HashBase nowner, Object _k, int hash, Object _v) {
    HashNode retval = setOwner(nowner);
    if (nowner.equals(_k,k)) {
      retval.setValue(_v);
    } else {
      if (retval.nextNode != null) {
	retval.nextNode = retval.nextNode.assoc(nowner, _k, hash, _v);
      } else {
	retval.nextNode = nowner.newNode(_k, hash, _v);
      }
    }
    return retval;
  }
  public HashNode dissoc(HashBase nowner, Object _k) {
    if (owner.equals(k, _k)) {
      nowner.dec(this);
      return nextNode;
    }
    if (nextNode != null) {
      HashNode nn = nextNode.dissoc(nowner,_k);
      if (nn != nextNode) {
	HashNode retval = setOwner(nowner);
	retval.nextNode = nn;
	return retval;
      }
    }
    return this;
  }
}

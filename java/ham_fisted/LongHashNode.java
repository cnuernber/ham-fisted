package ham_fisted;

import java.util.Map;
import java.util.Iterator;
import clojure.lang.IMapEntry;
import ham_fisted.IMutList;

public class LongHashNode implements Map.Entry, IMutList, IMapEntry {
  public final LongHashBase owner;
  public final int hashcode;
  public final long k;
  //compute-at support means we can modify v.
  Object v;
  LongHashNode nextNode;

  public LongHashNode(LongHashBase _owner, long _k, int hc, Object _v, LongHashNode nn) {
    owner = _owner;
    hashcode = hc;
    k = _k;
    v = _v;
    nextNode = nn;
    _owner.inc(this);
  }
  public LongHashNode(LongHashBase _owner, long _k, int hc, Object _v) {
    this(_owner, _k, hc, _v, null);
  }
  public LongHashNode(LongHashBase _owner, long _k, int hc) {
    this(_owner, _k, hc, null, null);
  }
  LongHashNode(LongHashBase _owner, LongHashNode prev) {
    owner = _owner;
    hashcode = prev.hashcode;
    k = prev.k;
    v = prev.v;
    nextNode = prev.nextNode;
  }
  public LongHashNode setOwner(LongHashBase nowner) {
    if (owner == nowner)
      return this;
    return new LongHashNode(nowner, this);
  }
  public LongHashNode clone(LongHashBase nowner) {
    LongHashNode rv = new LongHashNode(nowner, this);
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
  public LongHashNode assoc(LongHashBase nowner, long _k, int hash, Object _v) {
    LongHashNode retval = setOwner(nowner);
    if (k == _k) {
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
  public LongHashNode dissoc(LongHashBase nowner, long _k) {
    if (k == _k) {
      nowner.dec(this);
      return nextNode;
    }
    if (nextNode != null) {
      LongHashNode nn = nextNode.dissoc(nowner,_k);
      if (nn != nextNode) {
	LongHashNode retval = setOwner(nowner);
	retval.nextNode = nn;
	return retval;
      }
    }
    return this;
  }
}

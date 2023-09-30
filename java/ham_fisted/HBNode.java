package ham_fisted;

import java.util.Map;
import clojure.lang.IMapEntry;
import ham_fisted.IMutList;

public class HBNode implements Map.Entry, IMutList, IMapEntry {
  public final HashMap owner;
  public final int hashcode;
  public final Object k;
  //compute-at support means we can modify v.
  Object v;
  HBNode nextNode;

  public HBNode(HashMap _owner, Object _k, int hc, Object _v, HBNode nn) {
    owner = _owner;
    hashcode = hc;
    k = _k;
    v = _v;
    nextNode = nn;
    _owner.inc(this);
  }
  public HBNode(HashMap _owner, Object _k, int hc, Object _v) {
    this(_owner, _k, hc, _v, null);
  }
  public HBNode(HashMap _owner, Object _k, int hc) {
    this(_owner, _k, hc, null, null);
  }
  public HBNode(HashMap _owner, HBNode prev) {
    owner = _owner;
    hashcode = prev.hashcode;
    k = prev.k;
    v = prev.v;
    nextNode = prev.nextNode;
  }
  public HBNode setOwner(HashMap nowner) {
    if (owner == nowner)
      return this;
    return new HBNode(nowner, this);
  }
  public HBNode clone(HashMap nowner) {
    HBNode rv = new HBNode(nowner, k, hashcode, v, null);
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
  public HBNode assoc(HashMap nowner, Object _k, int hash, Object _v) {
    HBNode retval = setOwner(nowner);
    if (nowner.equals(_k,k)) {
      retval.setValue(_v);
    } else {
      if (retval.nextNode != null) {
	retval.nextNode = retval.nextNode.assoc(nowner, _k, hash, _v);
      } else {
	retval.nextNode = nowner.newNode(k, hash, _v);
      }
    }
    return retval;
  }
  public HBNode dissoc(HashMap nowner, Object _k) {
    if (owner.equals(k, _k)) {
      nowner.dec(this);
      return nextNode;
    }
    if (nextNode != null) {
      HBNode nn = nextNode.dissoc(nowner,_k);
      if (nn != nextNode) {
	HBNode retval = setOwner(nowner);
	retval.nextNode = nn;
	return retval;
      }
    }
    return this;
  }
}

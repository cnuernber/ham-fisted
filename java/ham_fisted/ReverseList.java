package ham_fisted;

import java.util.List;
import clojure.lang.IPersistentMap;
import clojure.lang.IObj;


public class ReverseList implements IMutList, TypedList {
  final List data;
  final IPersistentMap meta;
  final int nElems;
  final int nne;
  int _hash = 0;


  public ReverseList(List _data, IPersistentMap _meta) {
    data = _data;
    meta = _meta;
    nElems = data.size();
    nne = nElems - 1;
  }

  public static ReverseList create(List data, IPersistentMap meta) {
    return data instanceof IMutList ? new MutReverseList((IMutList)data, meta) : new ReverseList(data, meta);
  }

  public String toString() { return Transformables.sequenceToString(this); }
  public int hashCode() { return hasheq(); }
  public int hasheq() {
    if (_hash == 0) {
      _hash = IMutList.super.hasheq();
    }
    return _hash;
  }
  public boolean equals(Object other) { return equiv(other); }
  public Class containedType() { return data instanceof TypedList ? ((TypedList)data).containedType() : null; }
  public int size() { return nElems; }
  public Object get(int idx) { if(idx < 0) idx += nElems; return data.get(nne-idx); }
  public ReverseList subList(int sidx, int eidx) {
    sidx = nElems - sidx;
    eidx = nElems - eidx;
    return new ReverseList(data.subList(eidx, sidx), meta);
  }
  public List reverse() { return data instanceof IObj ? (List)((IObj)data).withMeta(meta) : data; }
  public IPersistentMap meta() { return meta; }
  public ReverseList withMeta(IPersistentMap m) { return new ReverseList(data, m); }


  public static class MutReverseList extends ReverseList {
    final IMutList mlist;
    public MutReverseList(IMutList ml, IPersistentMap meta) {
      super(ml, meta);
      mlist = ml;
    }
    public long getLong(int idx) {
      return mlist.getLong(nne - idx);
    }
    public double getDouble(int idx) {
      return mlist.getDouble(nne - idx);
    }
  }
}

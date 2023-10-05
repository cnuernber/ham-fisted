package ham_fisted;

import java.util.List;
import java.util.Map;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.Indexed;


public class UnsharedLongHashMap
  extends LongHashMap
  implements MutableMap, IATransientMap {
  public UnsharedLongHashMap() {
    super(null);
  }
  public UnsharedLongHashMap(IPersistentMap meta) {
    super(meta);
  }
  public UnsharedLongHashMap(IPersistentMap meta, int capacity) {
    super(0.75f, Math.max(4, IntegerOps.nextPow2((int)(capacity/0.75f))),
	  0, null, meta);
  }
  public static UnsharedLongHashMap create(Object[] data) {
    final int l = data.length;
    if((l%2) != 0)
      throw new RuntimeException("Data length not evenly divisible by 2");
    final UnsharedLongHashMap rv = new UnsharedLongHashMap(null, l/2);
    final LongHashNode[] d = rv.data;
    final int m = rv.mask;
    for(int idx = 0; idx < l; idx += 2) {
      final long k = Casts.longCast(data[idx]);
      final Object v = data[idx+1];
      final int hc = rv.hash(k);
      final int didx = hc & m;
      LongHashNode lf, init = d[didx];
      for(lf = init; lf != null && !(lf.k == k); lf = lf.nextNode);
      if(lf != null) {
	lf.v = v;
      } else {
	final LongHashNode newNode = rv.newNode(k, hc, v);
	newNode.nextNode = d[didx];
	d[didx] = newNode;
      }
    }
    return rv;
  }
  public static UnsharedLongHashMap createInterleaved(List data) {
    final int l = data.size();
    if((l%2) != 0)
      throw new RuntimeException("Data length not evenly divisible by 2");
    final UnsharedLongHashMap rv = new UnsharedLongHashMap(null, l/2);
    final LongHashNode[] d = rv.data;
    final int m = rv.mask;
    for(int idx = 0; idx < l; idx += 2) {
      final long k = Casts.longCast(data.get(idx));
      final Object v = data.get(idx+1);
      final int hc = rv.hash(k);
      final int didx = hc & m;
      LongHashNode lf, init = d[didx];
      for(lf = init; lf != null && !(lf.k == k); lf = lf.nextNode);
      if(lf != null) {
	lf.v = v;
      } else {
	final LongHashNode newNode = rv.newNode(k, hc, v);
	newNode.nextNode = d[didx];
	d[didx] = newNode;
      }
    }
    return rv;
  }
  public UnsharedLongHashMap assoc(Object key, Object val) {
    put(key,val);
    return this;
  }
  public UnsharedLongHashMap without(Object key) {
    remove(key);
    return this;
  }
  public PersistentLongHashMap persistent() {
    return new PersistentLongHashMap(this);
  }
}

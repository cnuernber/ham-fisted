package ham_fisted;


import java.util.Map;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.Indexed;


public class UnsharedHashMap
  extends HashMap
  implements IATransientMap {
  public UnsharedHashMap(IPersistentMap meta) {
    super(meta);
  }
  public UnsharedHashMap(IPersistentMap meta, int capacity) {
    super(0.75f, Math.max(4, IntegerOps.nextPow2((int)(capacity/0.75f))),
	  0, null, meta);
  }
  public static UnsharedHashMap create(Object[] data) {
    final int l = data.length;
    if((l%2) != 0)
      throw new RuntimeException("Data length not evenly divisible by 2");
    final UnsharedHashMap rv = new UnsharedHashMap(null, l/2);
    final HashNode[] d = rv.data;
    final int m = rv.mask;
    for(int idx = 0; idx < l; idx += 2) {
      final Object k = data[idx];
      final Object v = data[idx+1];
      final int hc = rv.hash(k);
      final int didx = hc & m;
      HashNode lf, init = d[didx];
      for(lf = init; lf != null && !(lf.k == k || rv.equals(lf.k, k)); lf = lf.nextNode);
      if(lf != null) {
	lf.v = v;
      } else {
	final HashNode newNode = rv.newNode(k, hc, v);
	newNode.nextNode = d[idx];
	d[idx] = newNode;
      }
    }
    return rv;
  }
  public UnsharedHashMap assoc(Object key, Object val) {
    put(key,val);
    return this;
  }
  public UnsharedHashMap without(Object key) {
    remove(key);
    return this;
  }
  public PersistentHashMap persistent() {
    return new PersistentHashMap(this);
  }
}

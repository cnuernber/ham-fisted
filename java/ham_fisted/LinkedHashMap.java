package ham_fisted;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IPersistentMap;
import clojure.lang.IDeref;


public class LinkedHashMap extends HashMap {
  //Most recently modified
  LinkedHashNode firstLink;
  //Least recently modified
  LinkedHashNode lastLink;
  public LinkedHashMap(IPersistentMap meta) {
    super(meta);
  }
  public LinkedHashMap() {
    this(null);
  }
  public LinkedHashMap(float loadFactor, int initialCapacity,
		       int length, HashNode[] data,
		       IPersistentMap meta) {
    super(loadFactor, initialCapacity, length, data, meta);
  }
  public LinkedHashMap clone() {
    LinkedHashMap rv = new LinkedHashMap(loadFactor, capacity, 0, new HashNode[data.length], meta);
    final HashNode[] data = this.data;
    final int mask = this.mask;
    final HashNode[] newData = rv.data;
    //Table is already correct size - no need to check resize.
    for(LinkedHashNode lf = lastLink; lf != null; lf = lf.nextLink) {
      int idx = lf.hashcode & mask;
      HashNode loc = newData[idx];
      HashNode newNode = rv.newNode(lf.k, lf.hashcode, lf.v);
      newData[idx] = newNode;
      newNode.nextNode = loc;
    }
    return rv;
  }
  protected HashNode newNode(Object key, int hc, Object val) {
    return new LinkedHashNode(this,key,hc,val,null);
  }
  protected void inc(HashNode lf) {
    super.inc(lf);
    LinkedHashNode hn = (LinkedHashNode)lf;
    if(lastLink == null)
      lastLink = hn;
    hn.prevLink = firstLink;
    if(firstLink != null)
      firstLink.nextLink = hn;
    firstLink = hn;
  }
  protected static void removeLink(LinkedHashNode hn) {
    if (hn.prevLink != null)
      hn.prevLink.nextLink = hn.nextLink;
    if(hn.nextLink != null)
      hn.nextLink.prevLink = hn.prevLink;
  }
  protected void dec(HashNode lf) {
    super.dec(lf);
    LinkedHashNode hn = (LinkedHashNode)lf;
    if(hn == firstLink)
      firstLink = hn.prevLink;
    if(hn == lastLink)
      lastLink = hn.nextLink;
    removeLink(hn);
    hn.nextLink = hn.prevLink = null;
  }
  protected void modify(HashNode n) {
    // The algorithm below is lightly tested but currently linkedhashmaps only
    // record insertion and deletion events.

    // LinkedHashNode hn = (LinkedHashNode)n;
    // if(firstLink != hn) {
    //   removeLink(hn);
    //   if(hn == lastLink)
    // 	lastLink = hn.nextLink;
    //   hn.prevLink = firstLink;
    //   hn.nextLink = null;

    //   if(firstLink != null)
    // 	firstLink.nextLink = hn;
    //   hn.prevLink = firstLink;
    //   firstLink = hn;
    // }
  }
  public static class LinkedIter implements Iterator {
    LinkedHashNode current;
    Function<Map.Entry,Object> fn;
    public LinkedIter(Function<Map.Entry,Object> fn, LinkedHashNode c) { this.current = c; this.fn = fn; }
    public boolean hasNext() { return current != null; }
    public Object next() {
      if(current == null) throw new NoSuchElementException();
      Object rv = fn.apply(current);
      current = current.nextLink;
      return rv;
    }
  }
  public Iterator iterator(Function<Map.Entry,Object> fn) {
    return new LinkedIter(fn, lastLink);
  }
  public Object reduce(IFn rfn, Object acc) {
    for(LinkedHashNode hn = lastLink; hn != null; hn = hn.nextLink) {
      acc = rfn.invoke(acc, hn);
      if(RT.isReduced(acc))
	return ((IDeref)acc).deref();
    }
    return acc;
  }
}

package ham_fisted;

import java.util.Map;
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
		       int length, HBNode[] data,
		       IPersistentMap meta) {
    super(loadFactor, initialCapacity, length, data, meta);
  }
  public LinkedHashMap clone() {
    LinkedHashMap rv = new LinkedHashMap(loadFactor, capacity, 0, new HBNode[data.length], meta);
    final HBNode[] data = this.data;
    final int mask = this.mask;
    final HBNode[] newData = rv.data;
    //Table is already correct size - no need to check resize.
    for(LinkedHashNode lf = lastLink; lf != null; lf = lf.nextLink) {
      int idx = lf.hashcode & mask;
      HBNode loc = newData[idx];
      HBNode newNode = rv.newNode(lf.k, lf.hashcode, lf.v);
      newData[idx] = newNode;
      newNode.nextNode = loc;
    }
    return rv;
  }
  protected HBNode newNode(Object key, int hc, Object val) {
    return new LinkedHashNode(this,key,hc,val,null);
  }
  protected void inc(HBNode lf) {
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
  protected void dec(HBNode lf) {
    super.dec(lf);
    LinkedHashNode hn = (LinkedHashNode)lf;
    if(hn == firstLink)
      firstLink = hn.prevLink;
    if(hn == lastLink)
      lastLink = hn.nextLink;
    removeLink(hn);
    hn.nextLink = hn.prevLink = null;
  }
  protected void modify(HBNode n) {
    // The algorithm below is correct but currently linkedhashmaps only
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

  @SuppressWarnings("unchecked")
  public LinkedHashMap union(Map o, BiFunction bfn) {
    if(!(o instanceof LinkedHashMap))
      return (LinkedHashMap)HashMap.unionFallback(this, o, bfn);
    LinkedHashMap other = (LinkedHashMap)o;
    LinkedHashMap rv = this;
    HBNode[] rvd = rv.data;
    int mask = rv.mask;
    for(LinkedHashNode lf = other.lastLink; lf != null; lf = lf.nextLink) {      
      final int rvidx = lf.hashcode & mask;
      final Object k = lf.k;
      HBNode e = rvd[rvidx], lastNode = null;
      for(;e != null && !(e.k==k || equals(e.k, k)); e = e.nextNode) { lastNode = e; }
      if(e != null) {
	e.v = bfn.apply(e.v, lf.v);
	modify(e);
      }
      else {
	HBNode nn = newNode(lf.k, lf.hashcode, lf.v);
	if(lastNode != null)
	  lastNode.nextNode = nn;
	else
	  rvd[rvidx] = nn;
	rv.checkResize(null);
	mask = rv.mask;
	rvd = rv.data;
      }
    }
    return rv;
  }

  @SuppressWarnings("unchecked")
  public LinkedHashMap intersection(Map o, BiFunction bfn) {
    if(!(o instanceof HashMap))
      return (LinkedHashMap)HashMap.intersectionFallback(this, o, bfn);
    return (LinkedHashMap)HashMap.intersection(this, (HashMap)o, bfn);
  }
  
  @SuppressWarnings("unchecked")
  public LinkedHashMap difference(Map o) {
    if(!(o instanceof HashMap))
      return (LinkedHashMap)differenceFallback(this, o);
    return (LinkedHashMap)HashMap.difference(this, (HashMap)o);
  }
}

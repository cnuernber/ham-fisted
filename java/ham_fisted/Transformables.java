package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;


import java.util.List;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Map;

import clojure.lang.IFn;
import clojure.lang.ArraySeq;
import clojure.lang.Seqable;
import clojure.lang.RT;
import clojure.lang.IteratorSeq;
import clojure.lang.ISeq;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;


public class Transformables {
  public interface IMapable extends Iterable, IObj {
    default IMapable map(IFn fn) {
      return new MapIterable(fn, meta(), this);
    }
    default IMapable filter(IFn fn) {
      return new FilterIterable(fn, meta(), this);
    }
    default IMapable cat(Iterable iters) {
      return new CatIterable(meta(), new Iterable[]{iters});
    }
  }
  public static boolean truthy(final Object obj) {
    return obj != null && (!(obj.equals(RT.F)));
  }
  public static Iterable toIterable(Object obj) {
    if( obj == null) return null;
    if( obj instanceof Iterable) return (Iterable)obj;
    if( obj instanceof Map) return ((Map)obj).entrySet();
    if( obj.getClass().isArray()) return ArrayLists.toList(obj);
    if( obj instanceof String) return new StringCollection((String)obj);
    return (Iterable)RT.seq(obj);
  }
  public static int iterCount(Iterator iter) {
    int c = 0;
    while(iter.hasNext()) {
      c++;
      iter.next();
    }
    return c;
  }
  public static class MapIterable extends AbstractCollection implements Seqable, IMapable {
    final Iterable[] iterables;
    final IFn fn;
    final IPersistentMap meta;
    public MapIterable(IFn _fn, IPersistentMap _meta, Iterable... _its) {
      fn = _fn;
      meta = _meta;
      iterables = _its;
    }
    public MapIterable(MapIterable o, IPersistentMap m) {
      fn = o.fn;
      iterables = o.iterables;
      meta = m;
    }
    public boolean isEmpty() {
      return seq() == null;
    }
    public int size() {
      return iterCount(iterator());
    }
    public Iterator iterator() {
      final int ss = iterables.length;
      final Iterator[] iterators = new Iterator[ss];
      for(int idx = 0; idx < ss; ++idx) {
	iterators[idx] = iterables[idx].iterator();
      }
      return new Iterator() {
	public boolean hasNext() {
	  for(int idx = 0; idx < ss; ++idx)
	    if( iterators[idx].hasNext() == false)
	      return false;
	  return true;
	}
	public Object next() {
	  switch(ss) {
	  case 1: return fn.invoke(iterators[0].next());
	  case 2: return fn.invoke(iterators[0].next(), iterators[1].next());
	  case 3: return fn.invoke(iterators[0].next(), iterators[1].next(), iterators[2].next());
	  case 4: return fn.invoke(iterators[0].next(), iterators[1].next(), iterators[2].next(), iterators[3].next());
	  default:
	    Object[] args = new Object[ss];
	    for (int idx = 0; idx < ss; ++idx)
	      args[idx] = iterators[idx].next();
	    return fn.applyTo(ArraySeq.create(args));
	  }
	}
      };
    }
    public ISeq seq() {
      return IteratorSeq.create(iterator());
    }
    public MapIterable map(IFn nfn) {
      return new MapIterable(new MapFn(fn, nfn), meta(), iterables);
    }
    public IPersistentMap meta() { return meta; }
    public MapIterable withMeta(IPersistentMap m) {
      return new MapIterable(this, m);
    }
  }
  public static class FilterIterable
    extends AbstractCollection
    implements Seqable, IMapable {
    final IFn pred;
    final Iterable src;
    final IPersistentMap meta;
    public FilterIterable(IFn _p, IPersistentMap _meta, Iterable _i) {
      pred = _p;
      src = _i;
      meta = _meta;
    }
    public FilterIterable(FilterIterable o, IPersistentMap m) {
      pred = o.pred;
      src = o.src;
      meta = m;
    }
    public boolean isEmpty() {
      return seq() == null;
    }
    public int size() { return iterCount(iterator()); }
    static class FilterIterator implements Iterator {
      final Iterator iter;
      final IFn pred;
      Box nextObj = new Box();
      public FilterIterator(Iterator _i, IFn p) {
	iter = _i;
	pred = p;
	advance();
      }
      void advance() {
	while(iter.hasNext()) {
	  final Object nobj = iter.next();
	  if (truthy(pred.invoke(nobj))) {
	    nextObj.obj = nobj;
	    return;
	  }
	}
	nextObj = null;
      }
      public boolean hasNext() { return nextObj != null; }
      public Object next() {
	if (nextObj == null)
	  throw new NoSuchElementException();
	final Object retval = nextObj.obj;
	advance();
	return retval;
      }
    }
    public Iterator iterator() {
      return new FilterIterator(src.iterator(), pred);
    }
    public ISeq seq() {
      return IteratorSeq.create(iterator());
    }
    public IMapable filter(IFn nfn) {
      return new FilterIterable( new IFnDef() {
	  public Object invoke(Object obj) {
	    return truthy(pred.invoke(obj)) && truthy(nfn.invoke(obj));
	  }
	}, meta(), src);
    }
    public IPersistentMap meta() { return meta; }
    public FilterIterable withMeta(IPersistentMap m) {
      return new FilterIterable(this, m);
    }
  }


  public static class CatIterable
    extends AbstractCollection
    implements Seqable, IMapable {
    //this is an array of iterables of iterables.
    final Iterable[] data;
    final IPersistentMap meta;
    public CatIterable(IPersistentMap _meta, Iterable[] f) {
      data = f;
      meta = _meta;
    }
    public CatIterable(CatIterable other, IPersistentMap m) {
      data = other.data;
      meta = m;
    }
    public int size() { return iterCount(iterator()); }
    public boolean isEmpty() {
      return iterator().hasNext() == false;
    }
    static class CatIterator implements Iterator {
      Iterator gpIter;
      Iterator parentIter;
      Iterator curIter;
      CatIterator(Iterator _gpIter) {
	gpIter = _gpIter;
	parentIter = null;
	curIter = null;
	advance();
      }
      public boolean hasNext() { return curIter != null && curIter.hasNext(); }
      public Object next() {
	final Object rv = curIter.next();
	if(!curIter.hasNext())
	  advance();
	return rv;
      }
      boolean advanceParent() {
	if(hasNext()) return true;
	while(parentIter != null && parentIter.hasNext()) {
	  final Iterable iter = toIterable(parentIter.next());
	  if(iter != null)
	    curIter = iter.iterator();
	  else
	    curIter = null;
	  if(hasNext()) return true;
	}
	parentIter = null;
	curIter = null;
	return false;
      }
      void advance() {
	if(hasNext()) { return; }
	if(advanceParent()) { return;}
	while(gpIter != null && gpIter.hasNext()) {
	  parentIter = null;
	  final Iterable parent = (Iterable)gpIter.next();
	  if(parent != null) {
	    parentIter = parent.iterator();
	  }
	  if(advanceParent())
	    return;
	}
	gpIter = null;
	parentIter = null;
	curIter = null;
      }
    }
    public Iterator iterator() {
      return new CatIterator(ArrayLists.toList(data).iterator());
    }
    public IMapable cat(Iterable _iters) {
      final int dlen = data.length;
      final Iterable[] newd = Arrays.copyOf(data, dlen+1);
      newd[dlen] = _iters;
      return new CatIterable(meta(), newd);
    }
    public ISeq seq() { return IteratorSeq.create(iterator()); }
    public IPersistentMap meta() { return meta; }
    public CatIterable withMeta(IPersistentMap m) {
      return new CatIterable(this, m);
    }
  }

  public static class MapList implements IMutList, IMapable {
    final int nElems;
    final List[] lists;
    final IFn fn;
    final IPersistentMap meta;
    public MapList(IFn _fn, IPersistentMap _meta, List... _lists) {
      final int ll = _lists.length;
      if(ll == 0)
	nElems = 0;
      else {
	int ne = _lists[0].size();
	for(int idx = 1; idx < ll; ++idx)
	  ne = Math.min(ne, _lists[idx].size());
	nElems = ne;
      }
      lists = _lists;
      fn = _fn;
      meta = _meta;
    }
    public MapList(MapList other, IPersistentMap m) {
      nElems = other.nElems;
      lists = other.lists;
      fn = other.fn;
      meta = m;
    }
    public int size() { return nElems; }
    public Object get(int idx) {
      if(idx < 0)
	idx += nElems;
      if(idx < 0 || idx >= nElems)
	throw new RuntimeException("Index out of range.");
      final List[] ll = lists;
      final int ls = ll.length;
      switch(ls) {
      case 1: return fn.invoke(ll[0].get(idx));
      case 2: return fn.invoke(ll[0].get(idx), ll[1].get(idx));
      case 3: return fn.invoke(ll[0].get(idx), ll[1].get(idx), ll[2].get(idx));
      case 4: return fn.invoke(ll[0].get(idx), ll[1].get(idx), ll[2].get(idx), ll[3].get(idx));
      default:
	Object[] args = new Object[ls];
	for (int aidx = 0; aidx < ls; ++aidx)
	  args[idx] = lists[aidx].get(idx);
	return fn.applyTo(ArraySeq.create(args));
      }
    }
    public IMutList subList(int sidx, int eidx) {
      final int sz = size();
      if (sidx < 0 || sidx >= sz)
	throw new RuntimeException("Start index out of range.");
      if (eidx < sidx || eidx > sz)
	throw new RuntimeException("End index out of range.");
      final int ll = lists.length;
      List[] newLists = new List[ll];
      for(int idx = 0; idx < ll; ++idx)
	newLists[idx] = lists[idx].subList(sidx, eidx);
      return new MapList(fn, meta(), newLists);
    }
    public MapList map(IFn nfn) {
      return new MapList(new MapFn(fn, nfn), meta(), lists);
    }
    public IPersistentMap meta() { return meta; }
    public MapList withMeta(IPersistentMap m) {
      return new MapList(this, m);
    }
  }
}

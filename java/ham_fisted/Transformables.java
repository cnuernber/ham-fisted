package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;


import java.util.List;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Collection;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import clojure.lang.IFn;
import clojure.lang.ArraySeq;
import clojure.lang.Seqable;
import clojure.lang.RT;
import clojure.lang.IteratorSeq;
import clojure.lang.ISeq;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;
import clojure.lang.IReduce;
import clojure.lang.IReduceInit;
import clojure.lang.IDeref;


public class Transformables {
  public interface IMapable extends Iterable, IObj {
    default IMapable map(IFn fn) {
      return new MapIterable(fn, meta(), this);
    }
    default IMapable filter(IFn fn) {
      return new FilterIterable(fn, meta(), this);
    }
    default IMapable cat(Iterable iters) {
      return new CatIterable(meta(), new Iterable[]{ArrayLists.toList(new Iterable[] { this }), iters});
    }
  }
  public static boolean truthy(final Object obj) {
    return !(obj == null || (Objects.equals(obj, RT.F)));
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
  public static Object iterReduce(Object obj, IFn fn) {
    final Iterator it = toIterable(obj).iterator();
    Object init = it.hasNext() ? it.next() : null;
    while(it.hasNext() && !RT.isReduced(init)) {
      init = fn.invoke(init, it.next());
    }
    if (RT.isReduced(init))
      return ((IDeref)init).deref();
    return init;
  }

  public static Object iterReduce(Object obj, Object init, IFn fn) {
    final Iterator it = toIterable(obj).iterator();
    while(it.hasNext() && !RT.isReduced(init)) {
      init = fn.invoke(init, it.next());
    }
    if (RT.isReduced(init))
      return ((IDeref)init).deref();
    return init;
  }

  public static class MapIterable
    extends AbstractCollection
    implements Seqable, IMapable, IReduce, IReduceInit {
    final Iterable[] iterables;
    final IFn fn;
    final IPersistentMap meta;
    public MapIterable(IFn _fn, IPersistentMap _meta, Iterable... _its) {
      fn = _fn;
      meta = _meta;
      iterables = _its;
    }
    public static MapIterable createSingle(IFn fn, IPersistentMap meta, Iterable single) {
      return new MapIterable(fn, meta, new Iterable[] { single });
    }
    public MapIterable(MapIterable o, IPersistentMap m) {
      fn = o.fn;
      iterables = o.iterables;
      meta = m;
    }
    public String toString() { return sequenceToString(this); }
    public boolean isEmpty() {
      return seq() == null;
    }
    public int size() {
      return iterCount(iterator());
    }
    static class SingleIterator implements Iterator {
      final Iterator iter;
      final IFn fn;
      public SingleIterator(IFn _fn, Iterator it) {
	iter = it;
	fn = _fn;
      }
      public boolean hasNext() { return iter.hasNext(); }
      public Object next() { return fn.invoke(iter.next()); }
    }

    static class DualIterator implements Iterator {
      final Iterator lhs;
      final Iterator rhs;
      final IFn fn;
      public DualIterator(IFn f, Iterator l, Iterator r) {
	lhs = l;
	rhs = r;
	fn = f;
      }
      public boolean hasNext() { return lhs.hasNext() && rhs.hasNext(); }
      public Object next() {
	return fn.invoke(lhs.next(), rhs.next());
      }
    }

    public Iterator iterator() {
      final int ss = iterables.length;
      switch(ss) {
      case 1: return new SingleIterator(fn, iterables[0].iterator());
      case 2: return new DualIterator(fn, iterables[0].iterator(), iterables[1].iterator());
      default:
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
    public Object reduce(IFn fn) {
      return iterReduce(this, fn);
    }
    public Object reduce(IFn rfn, Object init) {
      if(iterables.length == 1 && iterables[0] instanceof IReduceInit) {
	IReduceInit reducer = (IReduceInit)iterables[0];
	final IFn mfn = fn;
	return reducer.reduce(new IFnDef() {
	    public Object invoke(Object lhs, Object rhs) {
	      return rfn.invoke(lhs, mfn.invoke(rhs));
	    }
	  }, init);
      } else {
	return iterReduce(this, init, rfn);
      }
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }
  public static class FilterIterable
    extends AbstractCollection
    implements Seqable, IMapable, IReduce, IReduceInit {
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
    public String toString() { return sequenceToString(this); }
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
    public Object reduce(IFn fn) {
      final Iterator iter = src.iterator();

      if(iter.hasNext() == false) return fn.invoke();

      Object ret = iter.next();
      while(iter.hasNext() && !RT.isReduced(ret)) {
	final Object nobj = iter.next();
	if(truthy(pred.invoke(nobj)))
	  ret = fn.invoke(ret, nobj);
      }
      return RT.isReduced(ret) ? ((IDeref)ret).deref() : ret;
    }
    public Object reduce(final IFn fn, final Object init) {
      if(src instanceof IReduceInit) {
	return ((IReduceInit)src).reduce(new IFnDef() {
	    public Object invoke(Object lhs, Object rhs) {
	      if(truthy(pred.invoke(rhs)))
		return fn.invoke(lhs, rhs);
	      else
		return lhs;
	    }
	  }, init);
      } else {
	final Iterator iter = src.iterator();

	Object ret = init;
	while(iter.hasNext() && !RT.isReduced(ret)) {
	  final Object nobj = iter.next();
	  if(truthy(pred.invoke(nobj)))
	    ret = fn.invoke(ret, nobj);
	}
	return RT.isReduced(ret) ? ((IDeref)ret).deref() : ret;
      }
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }


  public static class CatIterable
    extends AbstractCollection
    implements Seqable, IMapable, IReduce, IReduceInit {
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
    public String toString() { return sequenceToString(this); }
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
    public Object reduce(IFn fn) {
      return iterReduce(this, fn);
    }
    public Object reduce(IFn fn, Object init) {
      return iterReduce(this, init, fn);
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }

  public static class SingleMapList implements IMutList, IMapable {
    final int nElems;
    final List list;
    final IFn fn;
    final IPersistentMap meta;
    public SingleMapList(IFn _fn, IPersistentMap m, List l) {
      nElems = l.size();
      list = l;
      fn = _fn;
      meta = m;
    }
    public SingleMapList(SingleMapList o, IPersistentMap m) {
      nElems = o.nElems;
      list = o.list;
      fn = o.fn;
      meta = m;
    }
    public String toString() { return sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public int size() { return nElems; }
    public Object get(int idx) { return fn.invoke(list.get(idx)); }
    public SingleMapList subList(int sidx, int eidx) {
      return new SingleMapList(fn, meta, list.subList(sidx, eidx));
    }
    public IPersistentMap meta() { return meta; }
    public SingleMapList withMeta(IPersistentMap m) {
      return new SingleMapList(this, m);
    }
    public SingleMapList map(IFn nfn) {
      return new SingleMapList(new MapFn(fn, nfn), meta, list);
    }
    public Object reduce(IFn rfn, Object init) {
      if(list instanceof IReduceInit) {
	IReduceInit reducer = (IReduceInit)list;
	final IFn mfn = fn;
	return reducer.reduce(new IFnDef() {
	    public Object invoke(Object lhs, Object rhs) {
	      return rfn.invoke(lhs, mfn.invoke(rhs));
	    }
	  }, init);
      } else {
	return iterReduce(this, init, rfn);
      }
    }
  }

  public static class DualMapList implements IMutList, IMapable {
    final int nElems;
    final List lhs;
    final List rhs;
    final IFn fn;
    final IPersistentMap meta;
    public DualMapList(IFn _fn, IPersistentMap m, List l, List r) {
      nElems = Math.min(l.size(), r.size());
      lhs = l;
      rhs = r;
      fn = _fn;
      meta = m;
    }
    public DualMapList(DualMapList o, IPersistentMap m) {
      nElems = o.nElems;
      lhs = o.lhs;
      rhs = o.rhs;
      fn = o.fn;
      meta = m;
    }
    public String toString() { return sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public int size() { return nElems; }
    public Object get(int idx) { return fn.invoke(lhs.get(idx), rhs.get(idx)); }
    public DualMapList subList(int sidx, int eidx) {
      return new DualMapList(fn, meta, lhs.subList(sidx, eidx), rhs.subList(sidx, eidx));
    }
    public IPersistentMap meta() { return meta; }
    public DualMapList withMeta(IPersistentMap m) {
      return new DualMapList(this, m);
    }
    public DualMapList map(IFn nfn) {
      return new DualMapList(new MapFn(fn, nfn), meta, lhs, rhs);
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
    public static IMutList create(IFn fn, IPersistentMap meta, List... lists) {
      if (lists.length == 1)
	return new SingleMapList(fn, meta, lists[0]);
      else if (lists.length == 2)
	return new DualMapList(fn, meta, lists[0], lists[1]);
      else
	return new MapList(fn, meta, lists);
    }
    public String toString() { return sequenceToString(this); }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
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

  public static class CachingIterable extends AbstractCollection
    implements Seqable {
    final Iterable src;
    final IPersistentMap meta;
    AtomicReference<ISeq> seq;
    public CachingIterable(Iterable _src, IPersistentMap _meta) {
      src = _src;
      meta = _meta;
      seq = new AtomicReference<ISeq>();
    }
    CachingIterable(CachingIterable other, IPersistentMap m) {
      src = other.src;
      meta = m;
      seq = other.seq;
    }
    public ISeq seq() {
      return seq.updateAndGet(new UnaryOperator<ISeq>() {
	  public ISeq apply(ISeq v) {
	    if(v != null) return v;
	    return src instanceof Seqable ? ((Seqable)src).seq() : IteratorSeq.create(src.iterator());
	  }
	});
    }
    public Iterator iterator() { return ((Collection)seq()).iterator(); }
    public int size() { return ((Collection)seq()).size(); }
    public IPersistentMap meta() { return meta; }
    public CachingIterable withMeta(IPersistentMap m) { return new CachingIterable(src, m); }
  }
  public static class CachingList implements IMutList {
    final List src;
    final Object[] dataCache;
    final BitSet cachedIndexes;
    final IPersistentMap meta;
    int _hash;
    public CachingList(List srcData, IPersistentMap _meta) {
      src = srcData;
      meta = _meta;
      dataCache = new Object[srcData.size()];
      cachedIndexes = new BitSet();
    }
    CachingList(CachingList other, IPersistentMap _meta) {
      src = other.src;
      dataCache = other.dataCache;
      cachedIndexes = other.cachedIndexes;
      meta = _meta;
    }
    public int hashCode() {
      return hasheq();
    }
    public int hasheq() {
      if (_hash == 0)
	_hash = IMutList.super.hasheq();
      return _hash;
    }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public String toString() { return sequenceToString(this); }
    public Object get(final int idx) {
      synchronized(cachedIndexes) {
	if(cachedIndexes.get(idx))
	  return dataCache[idx];
	final Object retval = src.get(idx);
	dataCache[idx] = retval;
	cachedIndexes.set(idx);
	return retval;
      }
    }
    public int size() { return src.size(); }
    public CachingList withMeta(IPersistentMap m) {
      return new CachingList(this, m);
    }
  }

  static void appendObjects(StringBuilder sb, Collection data) {
    boolean first = true;
    for(Object o: data) {
      if(!first)
	sb.append(" ");
      first = false;
      sb.append(o == null ? "nil" : o.toString());
    }
  }

  public static String sequenceToString(Collection data) {
    StringBuilder sb = new StringBuilder();
    if(data instanceof RandomAccess) {
      final List ra = (List) data;
      final int sz = ra.size();
      sb.append("[");
      if(sz < 50) {
	appendObjects(sb, ra);
      } else {
	appendObjects(sb, ra.subList(0, 20));
	sb.append(" ... ");
	appendObjects(sb, ra.subList(sz-20, sz));
      }
      sb.append("]");
    } else {
      sb.append("(");
      int idx = 0;
      for(Object o: data) {
	if(idx >= 50) {
	  sb.append(" ...");
	  break;
	}
	if (idx > 0)
	  sb.append(" ");
	sb.append(o == null ? "nil" : o.toString());
	++idx;
      }
      sb.append(")");
    }
    return sb.toString();
  }

}

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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;

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
  public static IFn toReductionFn(Object rfn) {
    if(rfn instanceof IFn) return (IFn)rfn;
    if(rfn instanceof IFn.OLO) {
      final IFn.OLO rrfn = (IFn.OLO)rfn;
      return new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return rrfn.invokePrim(lhs, Casts.longCast(rhs));
	}
      };
    }
    if(rfn instanceof IFn.ODO) {
      final IFn.ODO rrfn = (IFn.ODO)rfn;
      return new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return rrfn.invokePrim(lhs, Casts.doubleCast(rhs));
	}
      };
    }
    else
      throw new RuntimeException("Unrecognised function type: " + String.valueOf(rfn.getClass()));
  }

  public static IFn.OLO toLongReductionFn(Object rfn) {
    if(rfn instanceof IFn.OLO) {
      return (IFn.OLO)rfn;
    }
    if(rfn instanceof IFn.ODO) {
      final IFn.ODO rrfn = (IFn.ODO)rfn;
      return new IFn.OLO() {
	public Object invokePrim(Object lhs, long rhs) {
	  return rrfn.invokePrim(lhs, (double)rhs);
	}
      };
    }
    if(rfn instanceof IFn) {
      IFn rrfn = (IFn)rfn;
      return new IFn.OLO() {
	public Object invokePrim(Object lhs, long rhs) {
	  return rrfn.invoke(lhs, rhs);
	}
      };
    }
    else
      throw new RuntimeException("Unrecognised function type: " + String.valueOf(rfn.getClass()));
  }

  public static IFn.ODO toDoubleReductionFn(Object rfn) {
    if(rfn instanceof IFn.ODO) {
      return (IFn.ODO)rfn;
    }
    if(rfn instanceof IFn.OLO) {
      final IFn.OLO rrfn = (IFn.OLO)rfn;
      return new IFn.ODO() {
	public Object invokePrim(Object lhs, double rhs) {
	  return rrfn.invokePrim(lhs, (long)rhs);
	}
      };
    }
    if(rfn instanceof IFn) {
      IFn rrfn = (IFn)rfn;
      return new IFn.ODO() {
	public Object invokePrim(Object lhs, double rhs) {
	  return rrfn.invoke(lhs, rhs);
	}
      };
    }
    else
      throw new RuntimeException("Unrecognised function type: " + String.valueOf(rfn.getClass()));
  }

  public static Object longReduce(Object rfn, Object init, Object coll) {
    if (coll instanceof ITypedReduce)
      return ((ITypedReduce)coll).longReduction(toLongReductionFn(rfn), init);

    final IFn rrfn = toReductionFn(rfn);
    if (coll instanceof IReduceInit) {
      return ((IReduceInit)coll).reduce(rrfn, init);
    } else {
      if(!(coll instanceof Iterable))
	throw new RuntimeException("Collection is neither reducible nor iterable: " + String.valueOf(rfn.getClass()));
      return iterReduce(coll, init, rrfn);
    }
  }

  public static Object doubleReduce(Object rfn, Object init, Object coll) {
    if (coll instanceof ITypedReduce)
      return ((ITypedReduce)coll).doubleReduction(toDoubleReductionFn(rfn), init);

    final IFn rrfn = toReductionFn(rfn);
    if (coll instanceof IReduceInit) {
      return ((IReduceInit)coll).reduce(rrfn, init);
    } else {
      if(!(coll instanceof Iterable))
	throw new RuntimeException("Collection is neither reducible nor iterable: " + String.valueOf(rfn.getClass()));
      return iterReduce(coll, init, rrfn);
    }
  }

  public static Object genericReduce(Object rfn, Object init, Object coll) {
    if (coll instanceof ITypedReduce)
      return ((ITypedReduce)coll).genericReduction(rfn, init);

    final IFn rrfn = toReductionFn(rfn);
    if (coll instanceof IReduceInit) {
      return ((IReduceInit)coll).reduce(rrfn, init);
    } else {
      if(!(coll instanceof Iterable))
	throw new RuntimeException("Collection is neither reducible nor iterable: " + String.valueOf(rfn.getClass()));
      return iterReduce(coll, init, rrfn);
    }
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
    return init;
  }

  public static Object iterReduce(Object obj, Object init, IFn fn) {
    final Iterator it = toIterable(obj).iterator();
    while(it.hasNext() && !RT.isReduced(init)) {
      init = fn.invoke(init, it.next());
    }
    return init;
  }
  public static Object singleMapReduce(final Object item, final IFn rfn,
				       final IFn mapFn, Object init) {
    return genericReduce(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return rfn.invoke(lhs, mapFn.invoke(rhs));
	}
      }, init, item);
  }
  public interface IMapFn {
    IFn.DD toDoubleFn();
    IFn.LL toLongFn();
  }
  public static IFn.DD toDoubleMapFn(final IFn srcFn) {
    if (srcFn instanceof IFn.DD)
      return (IFn.DD)srcFn;
    else if (srcFn instanceof IFn.LL){
      final IFn.LL sfn = (IFn.LL)srcFn;
      return new IFn.DD() {
	public double invokePrim(double v) {
	  return (double)sfn.invokePrim((long)v);
	}
      };
    } else if (srcFn instanceof IMapFn) {
      return ((IMapFn)srcFn).toDoubleFn();
    } else {
      return new IFn.DD() {
	public double invokePrim(double v) {
	  return Casts.doubleCast(srcFn.invoke(v));
	}
      };
    }
  }
  public static Object singleMapDoubleReduce(final Object item, final IFn.ODO rfn,
					     final IFn mapFn, Object init) {
    final IFn.DD dmap = toDoubleMapFn(mapFn);
    final IFn.ODO redFn = new IFn.ODO() {
	public Object invokePrim(Object lhs, double rhs) {
	  return rfn.invokePrim(lhs, dmap.invokePrim(rhs));
	}
      };
    return doubleReduce(redFn, init, item);
  }

  public static IFn.LL toLongMapFn(final IFn srcFn) {
    if (srcFn instanceof IFn.LL)
      return (IFn.LL)srcFn;
    else if (srcFn instanceof IFn.DD){
      final IFn.DD sfn = (IFn.DD)srcFn;
      return new IFn.LL() {
	public long invokePrim(long v) {
	  return (long)sfn.invokePrim((double)v);
	}
      };
    } else if (srcFn instanceof IMapFn) {
      return ((IMapFn)srcFn).toLongFn();
    } else {
      return new IFn.LL() {
	public long invokePrim(long v) {
	  return Casts.longCast(srcFn.invoke(v));
	}
      };
    }
  }
  public static Object singleMapLongReduce(final Object item, final IFn.OLO rfn,
					   final IFn mapFn, Object init) {
    final IFn.LL dmap = toLongMapFn(mapFn);
    final IFn.OLO redFn = new IFn.OLO() {
	public Object invokePrim(Object lhs, long rhs) {
	  return rfn.invokePrim(lhs, dmap.invokePrim(rhs));
	}
      };
    return longReduce(redFn, init, item);
  }

  public static class MapIterable
    extends AbstractCollection
    implements Seqable, IMapable, ITypedReduce {
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
      if(iterables.length == 1)
	return singleMapReduce(iterables[0], rfn, fn, init);
      else
	return iterReduce(this, init, rfn);
    }
    public Object doubleReduction(final IFn.ODO rfn, Object init) {
      if(iterables.length == 1) {
	return singleMapDoubleReduce(iterables[0], rfn, fn, init);
      } else {
	return ITypedReduce.super.doubleReduction(rfn, init);
      }
    }
    public Object longReduction(final IFn.OLO rfn, Object init) {
      if(iterables.length == 1) {
	return singleMapLongReduce(iterables[0], rfn, fn, init);
      } else {
	return ITypedReduce.super.longReduction(rfn, init);
      }
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      ITypedReduce.super.forEach(c);
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }
  public interface IPredFn {
    public IFn.DO toDoubleFn();
    public IFn.LO toLongFn();
  };
  public static IFn.DO toDoublePredFn(IFn src) {
    if(src instanceof IFn.DO)
      return (IFn.DO)src;
    if(src instanceof IFn.LO) {
      final IFn.LO ss = (IFn.LO)src;
      return new IFn.DO() {
	public Object invokePrim(double v) {
	  return ss.invokePrim((long)v);
	}
      };
    }
    if(src instanceof IPredFn)
      return ((IPredFn)src).toDoubleFn();
    return new IFn.DO() {
      public Object invokePrim(double v) {
	return src.invoke(v);
      }
    };
  }
  public static IFn.LO toLongPredFn(IFn src) {
    if(src instanceof IFn.LO)
      return (IFn.LO)src;
    if(src instanceof IFn.DO) {
      final IFn.DO ss = (IFn.DO)src;
      return new IFn.LO() {
	public Object invokePrim(long v) {
	  return ss.invokePrim((double)v);
	}
      };
    }
    if(src instanceof IPredFn)
      return ((IPredFn)src).toLongFn();
    return new IFn.LO() {
      public Object invokePrim(long v) {
	return src.invoke(v);
      }
    };
  }
  public static class PredFn implements IFnDef, IPredFn {
    final IFn srcPred;
    final IFn dstPred;
    public PredFn(IFn sp, IFn dp) {
      srcPred = sp;
      dstPred = dp;
    }
    public Object invoke(Object v) {
      return truthy(srcPred.invoke(v)) && truthy(dstPred.invoke(v));
    }
    public IFn.DO toDoubleFn() {
      IFn.DO ss = toDoublePredFn(srcPred);
      IFn.DO dd = toDoublePredFn(dstPred);
      return new IFn.DO() {
	public Object invokePrim(double v) {
	  return truthy(ss.invokePrim(v)) && truthy(dd.invokePrim(v));
	}
      };
    }
    public IFn.LO toLongFn() {
      IFn.LO ss = toLongPredFn(srcPred);
      IFn.LO dd = toLongPredFn(dstPred);
      return new IFn.LO() {
	public Object invokePrim(long v) {
	  return truthy(ss.invokePrim(v)) && truthy(dd.invokePrim(v));
	}
      };
    }
  };
  public static class FilterIterable
    extends AbstractCollection
    implements Seqable, IMapable, ITypedReduce {
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
      return new FilterIterable(new PredFn(pred, nfn), meta(), src);
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
      return ret;
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
	return ret;
      }
    }
    public Object doubleReduction(final IFn.ODO rfn, Object init) {
      final IFn.DO pfn = toDoublePredFn(pred);
      return doubleReduce(new IFn.ODO() {
	  public Object invokePrim(Object lhs, double v) {
	    if(truthy(pfn.invokePrim(v)))
	      return rfn.invokePrim(lhs, v);
	    return lhs;
	  }
	}, init, src);
    }
    public Object longReduction(final IFn.OLO rfn, Object init) {
      final IFn.LO pfn = toLongPredFn(pred);
      return longReduce(new IFn.OLO() {
	  public Object invokePrim(Object lhs, long v) {
	    if(truthy(pfn.invokePrim(v)))
	      return rfn.invokePrim(lhs, v);
	    return lhs;
	  }
	}, init, src);
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      ITypedReduce.super.forEach(c);
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }


  public static class CatIterable
    extends AbstractCollection
    implements Seqable, IMapable, ITypedReduce {
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
      final int nData = data.length;
      final Iterable[] d = data;
      for(int idx = 0; idx < nData && !RT.isReduced(init); ++idx) {
	final Iterable item = d[idx];
	for(Iterator iter = item.iterator(); iter.hasNext() && !RT.isReduced(init);)
	  init = genericReduce(fn, init, iter.next());
      }
      return init;
    }
    public Object longReduction(final IFn.OLO rfn, Object init) {
      final int nData = data.length;
      final Iterable[] d = data;
      for(int idx = 0; idx < nData && !RT.isReduced(init); ++idx) {
	final Iterable item = d[idx];
	for(Iterator iter = item.iterator(); iter.hasNext() && !RT.isReduced(init);)
	  init = longReduce(rfn, init, iter.next());
      }
      return init;
    }
    public Object doubleReduction(final IFn.ODO rfn, Object init) {
      final int nData = data.length;
      final Iterable[] d = data;
      for(int idx = 0; idx < nData && !RT.isReduced(init); ++idx) {
	final Iterable item = d[idx];
	for(Iterator iter = item.iterator(); iter.hasNext() && !RT.isReduced(init);)
	  init = doubleReduce(rfn, init, iter.next());
      }
      return init;
    }
    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      ITypedReduce.super.forEach(c);
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
      return singleMapReduce(list, rfn, fn, init);
    }
    public Object doubleReduction(IFn.ODO rfn, Object init) {
      return singleMapDoubleReduce(list, rfn, fn, init);
    }
    public Object longReduction(IFn.OLO rfn, Object init) {
      return singleMapLongReduce(list, rfn, fn, init);
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

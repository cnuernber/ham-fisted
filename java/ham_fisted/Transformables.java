package ham_fisted;



import java.util.List;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Collection;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.LongPredicate;
import java.util.function.DoublePredicate;

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
import clojure.lang.Sequential;
import clojure.lang.Murmur3;
import clojure.lang.Util;
import clojure.lang.IHashEq;
import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentList;
import clojure.lang.Reduced;
import clojure.lang.Delay;
import clojure.lang.Box;
import clojure.java.api.Clojure;


public class Transformables {

  public static final Delay toIterableDelay = new Delay(new IFnDef() {
      public Object invoke() {
	return ((IDeref)Clojure.var("ham-fisted.protocols", "->iterable")).deref();
      }
    } );

  public static final Iterable toIterable(Object obj) {
    if (obj instanceof Iterable)
      return (Iterable) obj;
    //else bail to protocol
    return (Iterable)((IFn)toIterableDelay.deref()).invoke(obj);
  }
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
    return Casts.booleanCast(obj);
  }
  public static boolean not(final Object obj) {
    return obj == null || obj == Boolean.FALSE;
  }
  public static boolean not(final boolean v) {
    return !v;
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
    if(rfn instanceof IFnDef.OLO) {
      return (IFn.OLO)rfn;
    }
    if(rfn instanceof IFnDef.ODO) {
      final IFn.ODO rrfn = (IFn.ODO)rfn;
      return new IFnDef.OLO() {
	public Object invokePrim(Object lhs, long rhs) {
	  return rrfn.invokePrim(lhs, (double)rhs);
	}
      };
    }
    IFn rrfn = (IFn)rfn;
    return new IFnDef.OLO() {
      public Object invokePrim(Object lhs, long rhs) {
	return rrfn.invoke(lhs, rhs);
      }
    };
  }

  public static IFn.ODO toDoubleReductionFn(Object rfn) {
    if(rfn instanceof IFn.ODO) {
      return (IFn.ODO)rfn;
    }
    if(rfn instanceof IFnDef.OLO) {
      final IFn.OLO rrfn = (IFn.OLO)rfn;
      return new IFnDef.ODO() {
	public Object invokePrim(Object lhs, double rhs) {
	  return rrfn.invokePrim(lhs, Casts.longCast(rhs));
	}
      };
    }
    IFn rrfn = (IFn)rfn;
    return new IFnDef.ODO() {
      public Object invokePrim(Object lhs, double rhs) {
	return rrfn.invoke(lhs, rhs);
      }
    };
  }

  public static int iterCount(Iterator iter) {
    int c = 0;
    while(iter.hasNext()) {
      c++;
      iter.next();
    }
    return c;
  }
  public static IFn mapReducer(final IFn rfn, final IFn mapFn) {
    return new IFnDef() {
      public Object invoke() { return rfn.invoke(); }
      public Object invoke(Object res) { return rfn.invoke(res); }
      public Object invoke(Object lhs, Object rhs) {
	return rfn.invoke(lhs, mapFn.invoke(rhs));
      }
      public Object applyTo(Object arglist) {
	return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
      }
    };
  }
  public static IFn longMapReducer(final IFn rfn, final IFn mapFn) {
    final IFn.OLO rr = (IFn.OLO)rfn;
    if (mapFn instanceof IFn.OL) {
      final IFn.OL mfn = (IFn.OL)mapFn;
      return new IFnDef() {
	public Object invoke() { return rfn.invoke(); }
	public Object invoke(Object res) { return rfn.invoke(res); }
	public Object invoke(Object lhs, Object v) {
	  return rr.invokePrim(lhs, mfn.invokePrim(v));
	}
	public Object applyTo(Object arglist) {
	  return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	}
      };
    } else if (mapFn instanceof IFn.LL) {
      final IFn.LL mfn = (IFn.LL)mapFn;
      return new Reductions.LongAccum() {
	public Object invoke() { return rfn.invoke(); }
	public Object invoke(Object res) { return rfn.invoke(res); }
	public Object invokePrim(Object lhs, long v) {
	  return rr.invokePrim(lhs, mfn.invokePrim(v));
	}
	public Object applyTo(Object arglist) {
	  return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	}
      };
    } else {
      final IFn.DL mfn = (IFn.DL)mapFn;
      return new Reductions.DoubleAccum() {
	public Object invoke() { return rfn.invoke(); }
	public Object invoke(Object res) { return rfn.invoke(res); }
	public Object invokePrim(Object lhs, double v) {
	  return rr.invokePrim(lhs, mfn.invokePrim(v));
	}
	public Object applyTo(Object arglist) {
	  return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	}
      };
    }
  }

  final static HashMap<Class,BiFunction<IFn,IFn,IFn>> mappingReducer = new HashMap<Class,BiFunction<IFn,IFn,IFn>>();
  static {
    mappingReducer.put(IFn.LD.class, (IFn rfn, IFn mapFn)-> {
	final IFn.ODO rr = toDoubleReductionFn(rfn);
	final IFn.LD mfn = (IFn.LD)mapFn;
	return new IFnDef.OLO() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, long v) {
	    return rr.invokePrim(lhs, mfn.invokePrim(v));
	  }
	  public Object applyTo(Object arglist) {
	    return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	  }
	};
      });
    mappingReducer.put(IFnDef.LD.class, mappingReducer.get(IFn.LD.class));
    mappingReducer.put(IFn.OD.class, (IFn rfn, IFn mapFn)->{
	final IFn.ODO rr = toDoubleReductionFn(rfn);
	final IFn.OD mfn = (IFn.OD)mapFn;
	return new IFnDef.OOO() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invoke(Object lhs, Object v) {
	    return rr.invokePrim(lhs, mfn.invokePrim(v));
	  }
	  public Object applyTo(Object arglist) {
	    return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	  }
	};
      });
    mappingReducer.put(IFnDef.OD.class, mappingReducer.get(IFn.OD.class));
    mappingReducer.put(IFn.DD.class, (IFn rfn, IFn mapFn)-> {
	final IFn.ODO rr = toDoubleReductionFn(rfn);
	final IFn.DD mfn = (IFn.DD)mapFn;
	return new IFnDef.ODO() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, double rhs) {
	    return rr.invokePrim(lhs, mfn.invokePrim(rhs));
	  }
	  public Object applyTo(Object arglist) {
	    return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	  }
	};
      });
    mappingReducer.put(IFnDef.DD.class, mappingReducer.get(IFn.DD.class));
    mappingReducer.put(IFn.OL.class, (IFn rfn, IFn mapFn)-> {
	final IFn.OLO rr = toLongReductionFn(rfn);
	final IFn.OL mfn = (IFn.OL)mapFn;
	return new IFnDef() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invoke(Object lhs, Object v) {
	    return rr.invokePrim(lhs, mfn.invokePrim(v));
	  }
	  public Object applyTo(Object arglist) {
	    return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	  }
	};
      });
    mappingReducer.put(IFnDef.OL.class, mappingReducer.get(IFn.OL.class));
    mappingReducer.put(IFn.DL.class, (IFn rfn, IFn mapFn)-> {
	final IFn.OLO rr = toLongReductionFn(rfn);
	final IFn.DL mfn = (IFn.DL)mapFn;
	return new IFnDef.ODO() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, double v) {
	    return rr.invokePrim(lhs, mfn.invokePrim(v));
	  }
	  public Object applyTo(Object arglist) {
	    return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	  }
	};
      });
    mappingReducer.put(IFnDef.DL.class, mappingReducer.get(IFn.DL.class));
    mappingReducer.put(IFn.LL.class, (IFn rfn, IFn mapFn)-> {
	final IFn.OLO rr = toLongReductionFn(rfn);
	final IFn.LL mfn = (IFn.LL)mapFn;
	return new IFnDef.OLO() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, long v) {
	    return rr.invokePrim(lhs, mfn.invokePrim(v));
	  }
	  public Object applyTo(Object arglist) {
	    return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
	  }
	};
      });
    mappingReducer.put(IFnDef.LL.class, mappingReducer.get(IFn.LL.class));
  }

  public static IFn typedMapReducer(IFn rfn, IFn mapFn) {
    final Class[] ifaces = mapFn.getClass().getInterfaces();
    final int il = ifaces.length;
    for(int idx = 0; idx < il; ++idx) {
      final BiFunction<IFn, IFn, IFn> ff = mappingReducer.get(ifaces[idx]);
      if(ff != null)
	return ff.apply(rfn, mapFn);
    }
    return new IFnDef() {
      public Object invoke() { return rfn.invoke(); }
      public Object invoke(Object res) { return rfn.invoke(res); }
      public Object invoke(Object lhs, Object v) {
	return rfn.invoke(lhs, mapFn.invoke(v));
      }
      public Object applyTo(Object arglist) {
	return rfn.invoke(RT.first(arglist), mapFn.applyTo(RT.next(arglist)));
      }
    };
  }
  public static Object singleMapReduce(final Object item, final IFn rfn,
				       final IFn mapFn, Object init) {
    return Reductions.serialReduction(typedMapReducer(rfn, mapFn), init, item);
  }

  public static boolean seqEquiv(Seqable ss, Object o){
	ISeq s = ss.seq();
	if(s != null)
	  return s.equiv(o);
	else
	  return (o instanceof Sequential || o instanceof List) && RT.seq(o) == null;
  }

  public static int seqHashCode(Seqable ss) {
    ISeq s = ss.seq();
    if(s == null)
      return 1;
    return Util.hash(s);
  }

  public interface IterableSeq extends Collection, Seqable, IMapable, ITypedReduce,
				       IHashEq, Sequential, IPersistentCollection {
    default int hasheq() { return Murmur3.hashOrdered(this); }
    default boolean equiv(Object o) { return seqEquiv(this, o); }
    default int count() {
      long retval = 0;
      final Iterator iter = iterator();
      while(iter.hasNext()) {
	++retval;
	iter.next();
      }
      return RT.intCast(retval);
    }
    default IPersistentCollection cons(Object o) {
      return RT.cons(o, seq());
    }
    default IPersistentCollection empty() {
      return PersistentList.EMPTY;
    }
    @SuppressWarnings("unchecked")
    default void forEach(Consumer c) {
      ITypedReduce.super.forEach(c);
    }
    default ISeq seq() { return LazyChunkedSeq.chunkIteratorSeq(iterator()); }
  }

  public static class MapIterable
    extends AbstractCollection
    implements IterableSeq {
    final Object[] iterables;
    final IFn fn;
    final IPersistentMap meta;
    public MapIterable(IFn _fn, IPersistentMap _meta, Object... _its) {
      fn = _fn;
      meta = _meta;
      iterables = _its;
    }
    public static MapIterable createSingle(IFn fn, IPersistentMap meta, Object single) {
      return new MapIterable(fn, meta, new Object[] { single });
    }
    public MapIterable(MapIterable o, IPersistentMap m) {
      fn = o.fn;
      iterables = o.iterables;
      meta = m;
    }
    public String toString() { return sequenceToString(this); }
    public boolean isEmpty() {
      return iterator().hasNext() == false;
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
      case 1: return new SingleIterator(fn, toIterable(iterables[0]).iterator());
      case 2: return new DualIterator(fn, toIterable(iterables[0]).iterator(),
				      toIterable(iterables[1]).iterator());
      default:
	final Iterator[] iterators = new Iterator[ss];
	for(int idx = 0; idx < ss; ++idx) {
	  iterators[idx] = toIterable(iterables[idx]).iterator();
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
      return LazyChunkedSeq.chunkIteratorSeq(iterator());
    }
    public boolean equals(Object o) { return equiv(o); }
    public int hashCode(){ return hasheq(); }

    public MapIterable map(IFn nfn) {
      return new MapIterable(MapFn.create(fn, nfn), meta(), iterables);
    }
    public IPersistentMap meta() { return meta; }
    public MapIterable withMeta(IPersistentMap m) {
      return new MapIterable(this, m);
    }
    public Object reduce(IFn rfn, Object acc) {
      final int nLists = iterables.length;
      if(nLists == 1) {
	return singleMapReduce(iterables[0], rfn, fn, acc);
      } else {
	final Iterator[] iterators = new Iterator[nLists];
	for(int idx = 0; idx < nLists; ++idx)
	  iterators[idx] = toIterable(iterables[idx]).iterator();
	switch(iterables.length) {
	case 2: {
	  final Iterator l = iterators[0];
	  final Iterator r = iterators[1];
	  while(l.hasNext() && r.hasNext()) {
	    acc = rfn.invoke(acc, fn.invoke(l.next(), r.next()));
	    if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	  }
	  break;
	}
	case 3: {
	  final Iterator x = iterators[0];
	  final Iterator y = iterators[1];
	  final Iterator z = iterators[2];
	  while(x.hasNext() && y.hasNext() && z.hasNext()) {
	    acc = rfn.invoke(acc, fn.invoke(x.next(), y.next(), z.next()));
	    if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	  }
	  break;
	}
	case 4: {
	  final Iterator x = iterators[0];
	  final Iterator y = iterators[1];
	  final Iterator z = iterators[2];
	  final Iterator w = iterators[3];
	  while(x.hasNext() && y.hasNext() && z.hasNext() && w.hasNext()) {
	    acc = rfn.invoke(acc, fn.invoke(x.next(), y.next(), z.next(), w.next()));
	    if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	  }
	  break;
	}
	default: {
	  boolean hn = true;
	  for(int idx = 0; idx < nLists && hn; ++idx) {
	    hn = hn && iterators[idx].hasNext();
	  }
	  if(hn) {
	    final Object[] args = new Object[nLists];
	    final ISeq argSeq = ArraySeq.create(args);
	    while(hn) {
	      for(int idx = 0; idx < nLists && hn; ++idx) {
		args[idx] = iterators[idx].next();
	      }
	      acc = rfn.invoke(acc, fn.applyTo(argSeq));
	      if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	      for(int idx = 0; idx < nLists && hn; ++idx) {
		hn = hn && iterators[idx].hasNext();
	      }
	    }
	  }
	}
	}
      }
      return acc;
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options) {
      if(iterables.length == 1) {
	return Reductions.parallelReduction(initValFn, typedMapReducer(rfn, fn), mergeFn,
					    iterables[0], options);
      } else {
	return Reductions.serialParallelReduction(initValFn, rfn, options, this);
      }
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }
  public static class PredFn implements IFnDef {
    static IFn create(IFn src, IFn dst) {
      if ((src instanceof LongPredicate) && (dst instanceof LongPredicate)) {
	final LongPredicate ss = (LongPredicate)src;
	final LongPredicate dd = (LongPredicate)dst;
	return new IFnDef.LongPredicate() {
	  public boolean test(long v) {
	    return ss.test(v) && dd.test(v);
	  }
	};
      } else if((src instanceof IFn.LO) && (dst instanceof IFn.LO)) {
	final IFn.LO ss = (IFn.LO)src;
	final IFn.LO dd = (IFn.LO)dst;
	return new IFnDef.LO() {
	  public Object invokePrim(long v) {
	    return truthy(ss.invokePrim(v)) && truthy(dd.invokePrim(v));
	  }
	};
      } else if ((src instanceof DoublePredicate) && (dst instanceof DoublePredicate)) {
	final DoublePredicate ss = (DoublePredicate)src;
	final DoublePredicate dd = (DoublePredicate)dst;
	return new IFnDef.DoublePredicate() {
	  public boolean test(double v) {
	    return ss.test(v) && dd.test(v);
	  }
	};
      } else if((src instanceof IFn.DO) && (dst instanceof IFn.DO)) {
	final IFn.DO ss = (IFn.DO)src;
	final IFn.DO dd = (IFn.DO)dst;
	return new IFnDef.DO() {
	  public Object invokePrim(double v) {
	    return truthy(ss.invokePrim(v)) && truthy(dd.invokePrim(v));
	  }
	};
      } else if ((src instanceof Predicate) && (dst instanceof Predicate)) {
	final Predicate ss = (Predicate)src;
	final Predicate dd = (Predicate)dst;
	return new IFnDef.Predicate() {
	  @SuppressWarnings("unchecked")
	  public boolean test(Object v) {
	    return ss.test(v) && dd.test(v);
	  }
	};
      } else {
	return new PredFn(src,dst);
      }
    }

    final IFn srcPred;
    final IFn dstPred;
    public PredFn(IFn sp, IFn dp) {
      srcPred = sp;
      dstPred = dp;
    }
    public Object invoke(Object v) {
      return truthy(srcPred.invoke(v)) && truthy(dstPred.invoke(v));
    }
  };
  public static class FilterIterable
    extends AbstractCollection
    implements IterableSeq {
    final IFn pred;
    final Object src;
    final IPersistentMap meta;
    public FilterIterable(IFn _p, IPersistentMap _meta, Object _i) {
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
      Box nextObj = new Box(null);
      public FilterIterator(Iterator _i, IFn p) {
	iter = _i;
	pred = p;
	advance();
      }
      void advance() {
	while(iter.hasNext()) {
	  final Object nobj = iter.next();
	  if (truthy(pred.invoke(nobj))) {
	    nextObj.val = nobj;
	    return;
	  }
	}
	nextObj = null;
      }
      public boolean hasNext() { return nextObj != null; }
      public Object next() {
	if (nextObj == null)
	  throw new NoSuchElementException();
	final Object retval = nextObj.val;
	advance();
	return retval;
      }
    }
    public Iterator iterator() {
      return new FilterIterator(toIterable(src).iterator(), pred);
    }

    public int hashCode(){ return hasheq(); }
    public boolean equals(Object o) { return equiv(o); }
    public ISeq seq() {
      return LazyChunkedSeq.chunkIteratorSeq(iterator());
    }
    public IMapable filter(IFn nfn) {
      return new FilterIterable(PredFn.create(pred, nfn), meta(), src);
    }
    public IPersistentMap meta() { return meta; }
    public FilterIterable withMeta(IPersistentMap m) {
      return new FilterIterable(this, m);
    }
    @SuppressWarnings("unchecked")
    public static IFn typedReducer(final IFn rfn, final IFn pred) {
      if(pred instanceof LongPredicate) {
	final LongPredicate pfn = (LongPredicate)pred;
	final IFn.OLO rrfn = toLongReductionFn(rfn);
	return new Reductions.LongAccum() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, long v) {
	    return pfn.test(v) ? rrfn.invokePrim(lhs, v) : lhs;
	  }
	};
      } else if(pred instanceof IFn.LO) {
	final IFn.LO pfn = (IFn.LO)pred;
	final IFn.OLO rrfn = toLongReductionFn(rfn);
	return new Reductions.LongAccum() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, long v) {
	    return truthy(pfn.invokePrim(v)) ? rrfn.invokePrim(lhs, v) : lhs;
	  }
	};
      } else if (pred instanceof DoublePredicate) {
	final DoublePredicate pfn = (DoublePredicate)pred;
	final IFn.ODO rrfn = toDoubleReductionFn(rfn);
	return new Reductions.DoubleAccum() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, double v) {
	    return pfn.test(v) ? rrfn.invokePrim(lhs, v) : lhs;
	  }
	};
      } else if (pred instanceof IFn.DO) {
	final IFn.DO pfn = (IFn.DO)pred;
	final IFn.ODO rrfn = toDoubleReductionFn(rfn);
	return new Reductions.DoubleAccum() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invokePrim(Object lhs, double v) {
	    return truthy(pfn.invokePrim(v)) ? rrfn.invokePrim(lhs, v) : lhs;
	  }
	};
      } else if (pred instanceof Predicate) {
	final Predicate pfn = (Predicate)pred;
	return new IFnDef() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invoke(Object lhs, Object v) {
	    return pfn.test(v) ? rfn.invoke(lhs, v) : lhs;
	  }
	};
      } else {
	return new IFnDef() {
	  public Object invoke() { return rfn.invoke(); }
	  public Object invoke(Object res) { return rfn.invoke(res); }
	  public Object invoke(Object lhs, Object v) {
	    return truthy(pred.invoke(v)) ? rfn.invoke(lhs, v) : lhs;
	  }
	};
      }
    }
    public Object reduce(final IFn rfn, final Object init) {
      return Reductions.serialReduction(typedReducer(rfn,pred), init, src);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options) {
      return Reductions.parallelReduction(initValFn, typedReducer(rfn, pred), mergeFn,
					  src, options);
    }
    public Object[] toArray() {
      return ArrayLists.toArray(this);
    }
  }


  public static class CatIterable
    extends AbstractCollection
    implements IterableSeq {
    //this is an array of iterables of iterables.
    final Iterable[] data;
    public final IPersistentMap meta;
    public final ParallelOptions.CatParallelism parallelism;
    public CatIterable(IPersistentMap _meta, ParallelOptions.CatParallelism parallelism, Iterable arglist) {
      this.data = new Iterable[] {arglist};
      this.meta = _meta;
      this.parallelism = parallelism;
    }
    public CatIterable(IPersistentMap _meta, Iterable[] f) {
      this.data = f;
      this.meta = _meta;
      this.parallelism = null;
    }
    public CatIterable(Iterable arglist) {
      data = new Iterable[]{arglist};
      meta = null;
      parallelism = null;
    }
    public CatIterable(CatIterable other, IPersistentMap m) {
      data = other.data;
      meta = m;
      parallelism = other.parallelism;
    }
    public String toString() { return sequenceToString(this); }
    public boolean equals(Object o) { return equiv(o); }
    public int hashCode() { return hasheq(); }
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
    public ISeq seq() { return LazyChunkedSeq.chunkIteratorSeq(iterator()); }
    public IPersistentMap meta() { return meta; }
    public CatIterable withMeta(IPersistentMap m) {
      return new CatIterable(this, m);
    }
    public Object reduce(IFn fn) {
      return Reductions.iterReduce(this, fn);
    }
    public static IFn catReducer(IFn rfn) {
      if(rfn instanceof IFn.OLO) {
	final IFn.OLO ff = (IFn.OLO)rfn;
	return new IFnDef.OLO() {
	  public Object invokePrim(Object acc, long v) {
	    acc = ff.invokePrim(acc, v);
	    return RT.isReduced(acc) ? new Reduced(acc) : acc;
	  }
	};
      } else if (rfn instanceof IFn.ODO) {
	final IFn.ODO ff = (IFn.ODO)rfn;
	return new IFnDef.ODO() {
	  public Object invokePrim(Object acc, double v) {
	    acc = ff.invokePrim(acc, v);
	    return RT.isReduced(acc) ? new Reduced(acc) : acc;
	  }
	};
      } else {
	return new IFnDef() {
	  public Object invoke(Object acc, Object v) {
	    acc = rfn.invoke(acc,v);
	    return RT.isReduced(acc) ? new Reduced(acc) : acc;
	  }
	};
      }
    }
    public Object reduce(IFn rfn, Object init) {
      final int nData = data.length;
      final Iterable[] d = data;
      final IFn rf = catReducer(rfn);
      for(int idx = 0; idx < nData && !RT.isReduced(init); ++idx) {
	final Iterable item = d[idx];
	final Iterator iter = item != null ? item.iterator() : null;
	if (iter != null) {
	  for(; iter.hasNext() && !RT.isReduced(init);)
	    init = Reductions.serialReduction(rf, init, iter.next());
	}
      }
      return Reductions.unreduce(init);
    }
    static class CatIterIter implements Iterator {
      Iterator gpIter;
      Iterator parentIter;
      public CatIterIter(Iterator _gpIter) {
	gpIter = _gpIter;
	parentIter = null;
	advance();
      }
      public boolean hasNext() { return parentIter != null && parentIter.hasNext(); }
      public Object next() {
	final Object rv = parentIter.next();
	if(!parentIter.hasNext())
	  advance();
	return rv;
      }
      void advance() {
	if(hasNext()) { return; }
	while(gpIter != null && gpIter.hasNext()) {
	  parentIter = null;
	  final Iterable parent = (Iterable)gpIter.next();
	  if(parent != null) {
	    parentIter = parent.iterator();
	  }
	  if(hasNext())
	    return;
	}
	gpIter = null;
	parentIter = null;
      }
    }
    public Iterator containerIter() { return new CatIterIter(ArrayLists.toList(data).iterator()); }
    Object preduceSeqwise(IFn initValFn, IFn rfn, IFn mergeFn, Object init,
				 ParallelOptions options) {
      final Iterable initSequence = new Iterable() {
	  public Iterator iterator() {
	    return containerIter();
	  }
	};

      final Iterable partiallyReduced = ForkJoinPatterns.pmap(options, new IFnDef() {
	  public Object invoke(Object arg) {
	    return Reductions.serialReduction(rfn, initValFn.invoke(), arg);
	  }
	}, ArrayLists.toList(new Object[] { initSequence }));
      if(options.unmergedResult)
	return partiallyReduced;
      return Reductions.serialReduction(mergeFn, init, partiallyReduced);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options) {
      final int nData = data.length;
      final Iterable[] d = data;
      Object init = initValFn.invoke();
      final IFn rf = catReducer(rfn);
      ParallelOptions.CatParallelism catP = this.parallelism != null ? this.parallelism : options.catParallelism;
      switch (catP) {
      case ELEMWISE: {
	final Iterable initSequence = new Iterable() {
	    public Iterator iterator() {
	      return new CatIterIter(ArrayLists.toList(data).iterator());
	    }
	  };
	final Iterable mapped = new MapIterable(new IFnDef() {
	    public Object invoke(Object data) {
	      return Reductions.parallelReduction(initValFn, rf, mergeFn, data, options);
	    }
	  }, null, initSequence);

	if(options.unmergedResult) {
	  init = new CatIterable(mapped);
	} else {
	  init = Reductions.iterableMerge(options, mergeFn, mapped);
	}
	break;
      }
      case SEQWISE: {
	init = preduceSeqwise(initValFn, rf, mergeFn, init, options);
	break;
      }
      };
      return Reductions.unreduce(init);
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
      return new SingleMapList(MapFn.create(fn, nfn), meta, list);
    }
    public Object reduce(IFn rfn, Object init) {
      return singleMapReduce(list, rfn, fn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn, ParallelOptions options) {
      return Reductions.parallelReduction(initValFn, typedMapReducer(rfn, fn), mergeFn,
					  list, options);
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
      return new DualMapList(MapFn.create(fn, nfn), meta, lhs, rhs);
    }
  }

  public static class MapList implements IMutList, IMapable {
    final int nElems;
    final List[] lists;
    final IFn fn;
    final IPersistentMap meta;
    final IFn.LO getter;
    public MapList(IFn _fn, IPersistentMap _meta, List... _lists) {
      final int nLists = _lists.length;
      if(nLists == 0)
	nElems = 0;
      else {
	int ne = _lists[0].size();
	for(int idx = 1; idx < nLists; ++idx)
	  ne = Math.min(ne, _lists[idx].size());
	nElems = ne;
      }
      lists = _lists;
      fn = _fn;
      meta = _meta;
      IFn.LO getter = null;
      final List[] ll = lists;
      switch(nLists) {
      case 1: getter = new IFnDef.LO() {
	  public Object invokePrim(long idx) { return fn.invoke(ll[0].get((int)idx));
	  } };
	break;
      case 2:  getter = new IFnDef.LO() {
	  public Object invokePrim(long idx) { return fn.invoke(ll[0].get((int)idx), ll[1].get((int)idx));
	  } };
	break;
      case 3:  getter = new IFnDef.LO() {
	  public Object invokePrim(long lidx) { final int idx = (int)lidx; return fn.invoke(ll[0].get(idx), ll[1].get(idx), ll[2].get(idx));
	  } };
	break;
      case 4:
	getter = new IFnDef.LO() {
	  public Object invokePrim(long lidx) { final int idx = (int)lidx; return fn.invoke(ll[0].get(idx),
											    ll[1].get(idx),
											    ll[2].get(idx),
											    ll[3].get(idx));
	  } };
	break;
      default:
	getter = new IFnDef.LO() {
	    public Object invokePrim(long lidx) {
	      final int idx = (int)lidx;
	      Object[] args = new Object[nLists];
	      for (int aidx = 0; aidx < nLists; ++aidx)
		args[aidx] = lists[aidx].get(idx);
	      return fn.applyTo(ArraySeq.create(args));
	    }
	  };
      }
      this.getter = getter;
    }
    public MapList(MapList other, IPersistentMap m) {
      nElems = other.nElems;
      lists = other.lists;
      fn = other.fn;
      getter = other.getter;
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
      return getter.invokePrim(idx);

    }
    public Object reduce(IFn rfn, Object acc) {
      final int ne = nElems;
      final List[] ll = lists;
      final int ls = ll.length;
      switch(ls) {
      case 1: return Reductions.serialReduction( mapReducer(rfn, this.fn), acc, ll[0] );
      case 2:
	for (int idx = 0; idx < ne; ++idx) {
	  acc = rfn.invoke(acc, fn.invoke(lists[0].get(idx), lists[1].get(idx)));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
	return acc;
      case 3:
	for (int idx = 0; idx < ne; ++idx) {
	  acc = rfn.invoke(acc, fn.invoke(lists[0].get(idx), lists[1].get(idx), lists[2].get(idx)));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
	return acc;
      case 4:
	for (int idx = 0; idx < ne; ++idx) {
	  acc = rfn.invoke(acc, fn.invoke(lists[0].get(idx), lists[1].get(idx),
					  lists[2].get(idx), lists[3].get(idx)));
	  if(RT.isReduced(acc)) return ((IDeref)acc).deref();
	}
	return acc;
      }
      //fallthrough
      final Object[] args = new Object[ls];
      final ISeq arglist = ArraySeq.create(args);
      for(int oidx = 0; oidx < ne; ++oidx) {
	for (int aidx = 0; aidx < ls; ++aidx)
	  args[aidx] = lists[aidx].get(oidx);
	acc = rfn.invoke(acc, fn.applyTo(arglist));
	if(RT.isReduced(acc))
	  return ((IDeref)acc).deref();
      }
      return acc;
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
      return new MapList(MapFn.create(fn, nfn), meta(), lists);
    }
    public IPersistentMap meta() { return meta; }
    public MapList withMeta(IPersistentMap m) {
      return new MapList(this, m);
    }
  }

  public static class IndexedMapper extends AbstractCollection
    implements IterableSeq {
    final IFn mapFn;
    final Object src;
    final IPersistentMap meta;
    public IndexedMapper(IFn mapFn, Object src, IPersistentMap m) {
      this.mapFn = mapFn;
      this.src = src;
      this.meta = m;
    }
    public String toString() { return sequenceToString(this); }
    public boolean equals(Object other) { return equiv(other); }
    public int hashCode() { return hasheq(); }
    public boolean isEmpty() { return iterator().hasNext() == false; }
    public int size() { return iterCount(toIterable(src).iterator()); }
    public static class CountingFn implements IFnDef {
      long cnt;
      final IFn mapFn;
      public CountingFn(IFn mapFn) { cnt = 0; this.mapFn = mapFn; }
      public Object invoke(Object arg) {
	return mapFn.invoke(cnt++, arg);
      }
    }
    MapIterable mapper() { return new MapIterable(new CountingFn(mapFn), meta, new Object[] { src }); }
    public Iterator iterator() {
      return mapper().iterator();
    }
    public Object reduce(IFn rfn, Object init) {
      return mapper().reduce(rfn, init);
    }
    public IMapable map(IFn fn) {
      final IFn srcFn = mapFn;
      return new IndexedMapper(new IFnDef() {
	  public Object invoke(Object idx, Object arg) {
	    return fn.invoke(srcFn.invoke(idx,arg));
	  }
	}, src, meta);
    }
    public IPersistentMap meta() { return meta; }
    public IObj withMeta(IPersistentMap m) { return new IndexedMapper(mapFn, src, meta); }
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
	    return src instanceof Seqable ? ((Seqable)src).seq() : LazyChunkedSeq.chunkIteratorSeq(src.iterator());
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

  public static String sequenceToString(Iterable data) {
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

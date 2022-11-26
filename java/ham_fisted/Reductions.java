package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IReduceInit;
import clojure.lang.IDeref;
import clojure.lang.Seqable;
import clojure.java.api.Clojure;
import java.util.RandomAccess;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.concurrent.Future;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Callable;
import java.util.concurrent.ArrayBlockingQueue;


public class Reductions {

  public interface DoubleAccum extends IFnDef.ODO {}

  public interface LongAccum extends IFnDef.OLO {}

  public static Iterable toIterable(Object obj) {
    if( obj == null) return null;
    if( obj instanceof Iterable) return (Iterable)obj;
    if( obj instanceof Map) return ((Map)obj).entrySet();
    if( obj.getClass().isArray()) return ArrayLists.toList(obj);
    if( obj instanceof String) return new StringCollection((String)obj);
    return (Iterable)RT.seq(obj);
  }

  public static Object iterReduce(Object obj, IFn fn) {
    if(obj == null) return fn.invoke();

    final Iterator it = toIterable(obj).iterator();
    Object init = it.hasNext() ? it.next() : null;
    while(it.hasNext() && !RT.isReduced(init)) {
      init = fn.invoke(init, it.next());
    }
    return init;
  }

  public static Object iterReduce(Object obj, Object init, IFn fn) {
    if(obj == null) return init;

    final Iterator it = toIterable(obj).iterator();
    while(it.hasNext() && !RT.isReduced(init)) {
      init = fn.invoke(init, it.next());
    }
    return init;
  }

  public static Reducible reduceReducibles(Iterable<Reducible> data) {
    Iterator<Reducible> iter = data.iterator();
    Reducible initial = iter.hasNext() ? iter.next() : null;
    return initial.reduceIter(iter);
  }

  public static Object serialReduction(IFn rfn, Object init, Object coll) {
    if( coll == null) return init;
    if( coll instanceof IReduceInit) {
      return ((IReduceInit)coll).reduce(rfn, init);
    }
    return Clojure.var("clojure.core.protocols", "coll-reduce").invoke(coll, rfn, init);
  }

  public static Object iterableMerge(ParallelOptions options, IFn mergeFn,
				     final Iterable groups) {
    if(options.unmergedResult)
      return groups;

    final Iterator giter = groups.iterator();
    Object initObj = giter.hasNext() ? giter.next() : null;
    while(giter.hasNext())
      initObj = mergeFn.invoke(initObj, giter.next());
    return initObj;
  }

  public static Object parallelIndexGroupReduce(IFn groupFn, long nElems, IFn mergeFn,
						ParallelOptions options) {
    return iterableMerge(options, mergeFn,
			 ForkJoinPatterns.parallelIndexGroups(nElems, groupFn, options));
  }

  public static Object parallelRandAccessReduction(IFn initValFn, IFn rfn, IFn mergeFn,
						   List l, ParallelOptions options) {
    return parallelIndexGroupReduce( new IFnDef.LLO() {
	public Object invokePrim(long sidx, long eidx) {
	  return serialReduction(rfn, initValFn.invoke(),
				 l.subList(RT.intCast(sidx), RT.intCast(eidx)));
	}
      }, l.size(), mergeFn, options);
  }

  public static class ReduceConsumer implements Consumer, IDeref {
    Object init;
    public final IFn rfn;

    public static class DoubleReduceConsumer extends ReduceConsumer implements DoubleConsumer {
      public final IFn.ODO dfn;
      public DoubleReduceConsumer(Object in, IFn _rfn) {
	super(in, _rfn);
	dfn = (IFn.ODO)_rfn;
      }
      public void accept(Object obj) { accept(Casts.doubleCast(obj)); }
      public void accept(double v) {
	if(!isReduced())
	  init = dfn.invokePrim(init, v);
      }
    }

    public static class LongReduceConsumer extends ReduceConsumer implements LongConsumer {
      public final IFn.OLO dfn;
      public LongReduceConsumer(Object in, IFn _rfn) {
	super(in, _rfn);
	dfn = (IFn.OLO)_rfn;
      }
      public void accept(Object obj) { accept(Casts.longCast(obj)); }
      public void accept(long v) {
	if(!isReduced())
	  init = dfn.invokePrim(init, v);
      }
    }

    public ReduceConsumer(Object in, IFn _rfn) {
      init = in;
      rfn = _rfn;
    }
    public void accept(Object obj) {
      if(!isReduced())
	init = rfn.invoke(init, obj);
    }
    public boolean isReduced() {
      return RT.isReduced(init);
    }
    public Object deref() { return init; }

    public static ReduceConsumer create(Object init, IFn rfn) {
      if(rfn instanceof IFn.ODO) {
	return new DoubleReduceConsumer(init, rfn);
      } else if (rfn instanceof IFn.OLO) {
	return new LongReduceConsumer(init, rfn);
      } else {
	return new ReduceConsumer(init,rfn);
      }
    }
  }

  public static Object parallelCollectionReduction(IFn initValFn, IFn rfn, IFn mergeFn,
						   Collection coll, ParallelOptions options ) {
    if(coll.size() <= options.minN)
      return serialReduction(rfn, initValFn.invoke(), coll);
    return ForkJoinPatterns.parallelSpliteratorReduce(initValFn, rfn, mergeFn,
						      coll.spliterator(), options);
  }

  public static Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
					 Object coll, ParallelOptions options) {
    if(coll == null)
      return options.unmergedResult ?
	ArrayImmutList.create( true, null, initValFn.invoke() ) :
	initValFn.invoke();

    if(options.parallelism < 2) {
      return serialParallelReduction(initValFn, rfn, options, coll);
    }

    //Delegate back to clojure here so we can use a protocol to dispatch the
    //parallel reduction.
    return Clojure.var("ham-fisted.protocols", "preduce").invoke(coll, initValFn,
								 rfn, mergeFn, options);
  }

  public static Object serialParallelReduction(IFn initValFn, IFn rfn,
					       ParallelOptions options,
					       Object coll) {
    final Object retval = serialReduction(rfn, initValFn.invoke(), coll);
    return options.unmergedResult ?
      ArrayImmutList.create( true, null, retval ) :
      retval;
  }

  public static class IndexedDoubleAccum implements IFnDef.ODO {
    long idx;
    final IFn.OLDO rfn;
    public IndexedDoubleAccum(IFn.OLDO rfn) {
      this.rfn = rfn;
      this.idx = 0;
    }
    public Object invokePrim(Object acc, double v) {
      return rfn.invokePrim(acc, idx++, v);
    }
  }
  public static class IndexedLongAccum implements IFnDef.OLO {
    long idx;
    final IFn.OLLO rfn;
    public IndexedLongAccum(IFn.OLLO rfn) {
      this.rfn = rfn;
      this.idx = 0;
    }
    public Object invokePrim(Object acc, long v) {
      return rfn.invokePrim(acc, idx++, v);
    }
  }
  public static class IndexedAccum implements IFnDef {
    long idx;
    final IFn.OLOO rfn;
    public IndexedAccum(IFn.OLOO rfn) {
      this.rfn = rfn;
      this.idx = 0;
    }
    public Object invoke(Object acc, Object v) {
      return rfn.invokePrim(acc, idx++, v);
    }
  }
}

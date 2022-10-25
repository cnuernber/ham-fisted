package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IReduceInit;
import clojure.lang.IDeref;
import java.util.RandomAccess;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.concurrent.Future;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Callable;
import java.util.concurrent.ArrayBlockingQueue;


public class Reductions {

  public interface DoubleAccum extends IFnDef.ODO {}

  public interface LongAccum extends IFnDef.OLO {}

  public static Object serialReduction(IFn rfn, Object init, Object coll) {
    if(coll instanceof ITypedReduce) {
      final ITypedReduce l = (ITypedReduce)coll;
      if(rfn instanceof IFn.ODO)
	return l.doubleReduction((IFn.ODO)rfn, init);
      else if (rfn instanceof IFn.OLO)
	return l.longReduction((IFn.OLO)rfn, init);
      else
	return l.reduce(rfn, init);
    } else if (coll instanceof IReduceInit) {
      return ((IReduceInit)coll).reduce(rfn, init);
    } else if (coll instanceof Map) {
      return Transformables.iterReduce(((Map)coll).entrySet(), init, rfn);
    } else {
      return Transformables.iterReduce(coll, init, rfn);
    }
  }

  public static Object parallelRandAccessReduction(IFn initValFn, IFn rfn, IFn mergeFn, List l, ParallelOptions options) {
    final IFn gfn = new IFnDef() {
	public Object invoke(Object osidx, Object oeidx) {
	  final int sidx = RT.intCast(osidx);
	  final int eidx = RT.intCast(oeidx);
	  return serialReduction(rfn, initValFn.invoke(), l.subList(sidx, eidx));
	}
      };
    final Iterable groups = (Iterable) ForkJoinPatterns.parallelIndexGroups(l.size(), gfn, options);
    final Iterator giter = groups.iterator();
    Object initObj = giter.next();
    while(giter.hasNext())
      initObj = mergeFn.invoke(initObj, giter.next());
    return initObj;
  }

  public static class ReduceConsumer implements Consumer, IDeref {
    Object init;
    public final IFn rfn;
    public ReduceConsumer(Object in, IFn _rfn) {
      init = in;
      rfn = _rfn;
    }
    public void accept(Object obj) {
      init = rfn.invoke(init, obj);
    }
    public boolean isReduced() {
      return RT.isReduced(init);
    }
    public Object deref() { return init; }
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
    if(options.parallelism < 2)
      return serialReduction(rfn, initValFn.invoke(), coll);
    if(coll instanceof ITypedReduce) {
      return ((ITypedReduce)coll).parallelReduction(initValFn, rfn, mergeFn, options);
    }  else if (coll instanceof RandomAccess) {
      return parallelRandAccessReduction(initValFn, rfn, mergeFn, (List)coll, options);
      //Early check here for IReduceInit to catch Clojure's hashmap, hashset and
      //transducer map,filter chains not parallelizable datastructures.
    } else if (coll instanceof IReduceInit) {
      return serialReduction(rfn, initValFn.invoke(), coll);
      //java hashmap and concurrent hashmap, linked hashmap, etc.
    } else if (coll instanceof Map) {
      return parallelCollectionReduction(initValFn, rfn, mergeFn,
					 ((Map)coll).entrySet(), options);
      //All java.util set implementations.
    } else if (coll instanceof Set) {
      return parallelCollectionReduction(initValFn, rfn, mergeFn,
					 (Collection)coll, options);
      //Fallthrough - these are probably not parallelizeable but we can try.
    } else if (coll instanceof Iterable) {
      return ForkJoinPatterns.parallelSpliteratorReduce(initValFn, rfn, mergeFn,
							((Iterable)coll).spliterator(),
							options);
      //Finally everything else.
    } else {
      return serialReduction(rfn, initValFn.invoke(), coll);
    }
  }
}

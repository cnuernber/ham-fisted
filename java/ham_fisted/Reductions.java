package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IReduceInit;
import java.util.RandomAccess;
import java.util.List;
import java.util.Iterator;


public class Reductions {
  public static interface DoubleAccum extends IFnDef, IFn.ODO {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(lhs, Casts.doubleCast(rhs));
    }
  };
  public static interface LongAccum extends IFnDef, IFn.OLO {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(lhs, Casts.longCast(rhs));
    }
  }

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
    } else {
      return Transformables.iterReduce(coll, init, rfn);
    }
  }

  public static Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn, ParallelOptions options, Object coll) {
    if(coll instanceof ITypedReduce) {
      return ((ITypedReduce)coll).parallelReduction(initValFn, rfn, mergeFn, options);
    } else if (coll instanceof RandomAccess) {
      final List l = (List) coll;
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
    } else {
	return serialReduction(rfn, initValFn.invoke(), coll);
    }
  }
}

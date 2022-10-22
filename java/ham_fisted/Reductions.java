package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IReduceInit;
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
  public interface LO extends IFnDef, IFn.LO {
    default Object invoke(Object arg) {
      return invokePrim(Casts.longCast(arg));
    }
  }
  public interface LL extends IFnDef, IFn.LL {
    default Object invoke(Object arg) {
      return invokePrim(Casts.longCast(arg));
    }
  }
  public interface DO extends IFnDef, IFn.DO {
    default Object invoke(Object arg) {
      return invokePrim(Casts.doubleCast(arg));
    }
  }
  public interface DD extends IFnDef, IFn.DD {
    default Object invoke(Object arg) {
      return invokePrim(Casts.doubleCast(arg));
    }
  }
  public interface DDD extends IFnDef, IFn.DDD {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(Casts.doubleCast(lhs), Casts.doubleCast(rhs));
    }
  }
  public interface LLL extends IFnDef, IFn.LLL {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(Casts.longCast(lhs), Casts.longCast(rhs));
    }
  }
  public interface OL extends IFnDef, IFn.OL {
    default Object invoke(Object arg) {
      return invokePrim(arg);
    }
  }
  public interface OD extends IFnDef, IFn.OD {
    default Object invoke(Object arg) {
      return invokePrim(arg);
    }
  }
  public interface DoubleAccum extends IFnDef, IFn.ODO {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(lhs, Casts.doubleCast(rhs));
    }
  }
  public interface LongAccum extends IFnDef, IFn.OLO {
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

  static class ReduceConsumer implements Consumer {
    Object init;
    final IFn rfn;
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
  }

  static class ErrorRecord {
    public final Exception e;
    public ErrorRecord(Exception _e) { e = _e; }
    public static Object checkError(Object obj) {
      if (obj instanceof ErrorRecord)
	throw new RuntimeException("Error during reduction", ((ErrorRecord)obj).e);
      return obj;
    }
  }

  @SuppressWarnings("unchecked")
  public static Object parallelSpliteratorReduction(IFn initValFn, IFn rfn, IFn mergeFn,
						    Spliterator s, ParallelOptions options) {
    try {
      final ForkJoinPool pool = options.pool;
      final long nSplits = Math.round(Math.log(options.parallelism * 2) / Math.log(2));
      Spliterator[] splitIters = new Spliterator[] { s };
      for(int idx = 0; idx < nSplits; ++idx) {
	final int sl = splitIters.length;
	Spliterator[] si = new Spliterator[splitIters.length * 2];
	for(int sidx = 0; sidx < sl; ++sidx) {
	  Spliterator orig = splitIters[sidx];
	  Spliterator niter = orig.trySplit();
	  si[sidx*2] = orig;
	  si[sidx*2 + 1] = niter;
	}
	splitIters = si;
      }
      ArrayBlockingQueue ab = options.ordered ? null : new ArrayBlockingQueue(options.parallelism + 2);
      final int nSubs = splitIters.length;
      final Future[] futures = new Future[nSubs];
      for(int idx = 0; idx < nSubs; ++idx) {
	final Spliterator sp = splitIters[idx];
	if(options.ordered) {
	  futures[idx] = pool.submit(new Callable() {
	      public Object call() {
		final ReduceConsumer c = new ReduceConsumer(initValFn.invoke(), rfn);
		//Empty loop intentional
		while(!c.isReduced() && sp.tryAdvance(c)) {}
		return c.init;
	      }
	    });
	} else {
	  futures[idx] = pool.submit(new Callable() {
	      public Object call() {
		final ReduceConsumer c = new ReduceConsumer(initValFn.invoke(), rfn);
		try {
		  //Empty loop intentional
		  while(!c.isReduced() && sp.tryAdvance(c)) {}
		  ab.put(c.init);
		  return c.init;
		}catch(Exception e) {
		  try {
		    ab.put(new ErrorRecord(e));
		    return e;
		  } catch(Exception ee) {
		    System.err.println("Error during queue put: " + String.valueOf(ee));
		    return ee;
		  }
		}
	      }
	    });
	}
      }
      if(options.ordered) {
	Object fval = futures[0].get();
	for(int idx = 1; idx < nSubs; ++idx) {
	  fval = mergeFn.invoke(fval, futures[idx].get());
	}
	return fval;
      } else {
	Object fval = ErrorRecord.checkError(ab.take());
	for(int idx = 1; idx < nSubs; ++idx) {
	  fval = mergeFn.invoke(fval, ErrorRecord.checkError(ab.take()));
	}
	return fval;
      }
    } catch(Exception e) {
      throw new RuntimeException("Error during spliterator parallelization", e);
    }
  }

  public static Object parallelCollectionReduction(IFn initValFn, IFn rfn, IFn mergeFn,
						   Collection coll, ParallelOptions options ) {
    if(coll.size() <= options.minN)
      return serialReduction(rfn, initValFn.invoke(), coll);
    return parallelSpliteratorReduction(initValFn, rfn, mergeFn, coll.spliterator(),
					options);
  }

  public static Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
					 Object coll, ParallelOptions options) {
    if(options.parallelism < 2)
      return serialReduction(rfn, initValFn.invoke(), coll);
    if(coll instanceof ITypedReduce) {
      return ((ITypedReduce)coll).parallelReduction(initValFn, rfn, mergeFn, options);
    } else if (coll instanceof RandomAccess) {
      return parallelRandAccessReduction(initValFn, rfn, mergeFn, (List)coll, options);
    } else if (coll instanceof Map) {
      return parallelCollectionReduction(initValFn, rfn, mergeFn,
					 ((Map)coll).entrySet(), options);
    } else if (coll instanceof Set) {
      return parallelCollectionReduction(initValFn, rfn, mergeFn,
					 (Collection)coll, options);
      //Cull out clojure things that do not have a good spliterator implementation.
    } else if (coll instanceof IReduceInit) {
      return serialReduction(rfn, initValFn.invoke(), coll);
    } else if (coll instanceof Iterable) {
      return parallelSpliteratorReduction(initValFn, rfn, mergeFn,
					  ((Iterable)coll).spliterator(), options);
    } else {
      return serialReduction(rfn, initValFn.invoke(), coll);
    }
  }
}

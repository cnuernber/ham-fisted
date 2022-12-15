package ham_fisted;

import clojure.lang.IFn;
import clojure.lang.Delay;
import clojure.lang.IDeref;
import clojure.java.api.Clojure;
import java.util.Spliterator;


public class ForkJoinPatterns {
  private ForkJoinPatterns(){}


  static final Delay pgroupsPtr = new Delay(new IFnDef() {
      public Object invoke() {
	return ((IDeref)Clojure.var("ham-fisted.impl", "pgroups")).deref();
      }
    });

  public static Iterable parallelIndexGroups(long nElems, IFn bodyFn, ParallelOptions options) {
    return (Iterable)((IFn)pgroupsPtr.deref()).invoke(nElems, bodyFn, options);
  }

  static final Delay pmapPtr = new Delay(new IFnDef() {
      public Object invoke() {
	return ((IDeref)Clojure.var("ham-fisted.impl", "pmap")).deref();
      }
    });

  public static Iterable pmap(ParallelOptions options, IFn bodyFn, Object sequences) {
    return (Iterable)((IFn)pmapPtr.deref()).invoke(options, bodyFn, sequences);
  }

  static final Delay spliteratorPtr = new Delay(new IFnDef() {
      public Object invoke() {
	return ((IDeref)Clojure.var("ham-fisted.impl", "parallel-spliterator-reduce")).deref();
      }
    });
  public static Object parallelSpliteratorReduce(IFn initValFn, IFn rfn, IFn mergeFn,
						 Spliterator s, ParallelOptions options) {
    return ((IFn)spliteratorPtr.deref()).invoke(initValFn, rfn, mergeFn, s, options);
  }
}

package ham_fisted;

import clojure.lang.IFn;
import clojure.java.api.Clojure;
import java.util.Spliterator;


public class ForkJoinPatterns {
  private ForkJoinPatterns(){}

  public static Iterable parallelIndexGroups(long nElems, IFn bodyFn, ParallelOptions options) {
    return (Iterable)Clojure.var("ham-fisted.impl", "pgroups").invoke(nElems, bodyFn, options);
  }
  public static Iterable pmap(ParallelOptions options, IFn bodyFn, Object sequences) {
    return (Iterable)Clojure.var("ham-fisted.impl", "pmap").invoke(options, bodyFn, sequences);
  }
  public static Object parallelSpliteratorReduce(IFn initValFn, IFn rfn, IFn mergeFn,
						 Spliterator s, ParallelOptions options) {
    return Clojure.var("ham-fisted.impl", "parallel-spliterator-reduce").invoke(initValFn, rfn, mergeFn, s, options);
  }
}

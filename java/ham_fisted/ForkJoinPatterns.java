package ham_fisted;

import clojure.lang.IFn;
import clojure.java.api.Clojure;


public class ForkJoinPatterns {
  private ForkJoinPatterns(){}

  public static Iterable parallelIndexGroups(long nElems, IFn bodyFn, ParallelOptions options) {
    return (Iterable)Clojure.var("ham-fisted.impl", "pgroups").invoke(nElems, bodyFn, options);
  }
}

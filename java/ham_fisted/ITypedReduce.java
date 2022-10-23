package ham_fisted;


import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.function.IntConsumer;
import java.util.concurrent.ForkJoinPool;
import clojure.lang.IReduceInit;
import clojure.lang.IFn;

/**
 *  Typed reductions - a typed extension of clojure.lang.IReduceInit and java.util.Iterable.forEach.
 */
public interface ITypedReduce<E> extends IReduceInit {
  default Object doubleReduction(IFn.ODO op, Object init) {
    return reduce(new Reductions.DoubleAccum() {
	public Object invokePrim(Object lhs, double rhs) {
	  return op.invokePrim(lhs, rhs);
	}
      }, init);
  }
  default Object longReduction(IFn.OLO op, Object init) {
    return reduce(new Reductions.LongAccum() {
	public Object invokePrim(Object lhs, long rhs) {
	  return op.invokePrim(lhs, rhs);
	}
      }, init);
  }
  public static class LongIncrementor {
    long val;
    public LongIncrementor(long initIdx) { val = initIdx; }
    public final long inc() { return val++; }
  }
  default Object indexedDoubleReduction(IFn.OLDO op, long initIdx, Object init) {
    final LongIncrementor inc = new LongIncrementor(initIdx);
    return reduce(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return op.invokePrim(lhs, inc.inc(), Casts.doubleCast(rhs));
	}
      }, init);
  }
  default Object indexedLongReduction(IFn.OLLO op, long initIdx, Object init) {
    final LongIncrementor inc = new LongIncrementor(initIdx);
    return reduce(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return op.invokePrim(lhs, inc.inc(), Casts.longCast(rhs));
	}
      }, init);
  }
  default Object indexedReduction(IFn.OLOO op, long initIdx, Object init) {
    final LongIncrementor inc = new LongIncrementor(initIdx);
    return reduce(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return op.invokePrim(lhs, inc.inc(), rhs);
	}
      }, init);
  }
  default Object genericReduction(Object rfn, Object init) {
    if(rfn instanceof IFn.ODO)
      return doubleReduction((IFn.ODO)rfn, init);
    else if (rfn instanceof IFn.OLO)
      return longReduction((IFn.OLO)rfn, init);
    else {
      if(!(rfn instanceof IFn))
	throw new RuntimeException("reducer must be an instance of clojure.lang.IFn, java.util.function.DoubleBinaryOperator or java.util.function.LongBinaryOperator.");
      return reduce((IFn)rfn, init);
    }
  }
  default Object genericIndexedReduction(Object rfn, long initIdx, Object init) {
    if(rfn instanceof IFn.OLDO)
      return indexedDoubleReduction((IFn.OLDO)rfn, initIdx, init);
    else if (rfn instanceof IFn.OLLO)
      return indexedLongReduction((IFn.OLLO)rfn, initIdx, init);
    else {
      if(!(rfn instanceof IFn.OLOO))
	throw new RuntimeException("reducer must be a typehinted instance of clojure.lang.IFn.");
      return indexedReduction((IFn.OLOO)rfn, initIdx, init);
    }
  }

  default Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				   ParallelOptions options) {
    return genericReduction(rfn, initValFn.invoke());
  }

  //Typed this way in order to match java.util.List's forEach
  @SuppressWarnings("unchecked")
  default void forEach(Consumer<E> c) {
    //Reduce is strictly more powerful.  You can define consume in terms of reduce with
    //full generality but you cannot define reduce in terms of consume.
    reduce( new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  c.accept((E)rhs);
	  return c;
	}
      }, c);
  }
  @SuppressWarnings("unchecked")
  default void doubleForEach(final DoubleConsumer c) {
    doubleReduction(new IFn.ODO() {
	public Object invokePrim(Object lhs, double v) {
	  c.accept(v);
	  return c;
	}
      }, c);
  }
  @SuppressWarnings("unchecked")
  default void longForEach(final LongConsumer c) {
    longReduction(new IFn.OLO() {
	public Object invokePrim(Object obj, long l) {
	  c.accept(l);
	  return c;
	}
      }, c);
  }
  @SuppressWarnings("unchecked")
  default void genericForEach(Object c) {
    DoubleConsumer dc;
    LongConsumer lc;
    if((dc = ArrayLists.asDoubleConsumer(c)) != null) {
      doubleForEach(dc);
    } else if ((lc = ArrayLists.asLongConsumer(c)) != null) {
      longForEach(lc);
    }
    else {
      if(! (c instanceof Consumer))
	throw new RuntimeException("Consumer must be an instance of java.util.function Consumer, DoubleConsumer, or LongConsumer.");
      forEach((Consumer<E>) c);
    }
  }
  @SuppressWarnings("unchecked")
  default void genericIndexedForEach(final long initIdx, Object c) {
    IndexedDoubleConsumer dc;
    IndexedLongConsumer lc;
    IndexedConsumer ic;
    if((dc = Consumers.asIndexedDoubleConsumer(c)) != null) {
      doubleForEach(Consumers.toDoubleConsumer(initIdx, dc));
    } else if ((lc = Consumers.asIndexedLongConsumer(c)) != null) {
      longForEach(Consumers.toLongConsumer(initIdx, lc));
    }
    else {
      if( (ic = Consumers.asIndexedConsumer(c)) == null)
	throw new RuntimeException("Consumer must be an instance of ham_fisted IndexedConsumer, IndexedDoubleConsumer, or IndexedLongConsumer.");
      forEach(Consumers.toConsumer(initIdx, ic));
    }
  }
}

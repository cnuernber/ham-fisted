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
  default Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				   ParallelOptions options) {
    return Reductions.serialParallelReduction(initValFn, rfn, options, this);
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
}

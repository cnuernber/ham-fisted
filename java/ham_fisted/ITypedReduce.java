package ham_fisted;


import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.function.IntConsumer;
import clojure.lang.IReduceInit;
import clojure.lang.IFn;


public interface ITypedReduce<E> extends IReduceInit {
  default double doubleReduction(DoubleBinaryOperator op, double init) {
    return Casts.doubleCast(reduce(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return op.applyAsDouble(Casts.doubleCast(lhs), Casts.doubleCast(rhs));
	}
      }, init));
  }
  default long longReduction(LongBinaryOperator op, long init) {
    return Casts.longCast(reduce(new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  return op.applyAsLong(Casts.longCast(lhs), Casts.longCast(rhs));
	}
      }, init));
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
  default Object genericReduction(Object rfn, Object init) {
    if(rfn instanceof DoubleBinaryOperator)
      return doubleReduction((DoubleBinaryOperator)rfn, Casts.doubleCast(init));
    else if (rfn instanceof LongBinaryOperator)
      return longReduction((LongBinaryOperator)rfn, Casts.longCast(init));
    else {
      if(!(rfn instanceof IFn))
	throw new RuntimeException("reducer must be an instance of clojure.lang.IFn, java.util.function.DoubleBinaryOperator or java.util.function.LongBinaryOperator.");
      return reduce((IFn)rfn, init);
    }
  }
  @SuppressWarnings("unchecked")
  default void doubleForEach(DoubleConsumer c) {
    forEach(new Consumer() {
	public void accept(Object obj) {
	  c.accept(Casts.doubleCast(obj));
	}
      });
  }
  @SuppressWarnings("unchecked")
  default void longForEach(LongConsumer c) {
    forEach(new Consumer() {
	public void accept(Object obj) {
	  c.accept(Casts.longCast(obj));
	}
      });
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
}

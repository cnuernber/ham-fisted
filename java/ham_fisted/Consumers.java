package ham_fisted;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.LongConsumer;
import java.util.function.Function;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.DoublePredicate;
import java.util.function.LongPredicate;
import clojure.lang.IDeref;

public class Consumers {
  public interface IDerefDoubleConsumer extends IDeref, DoubleConsumer {}
  public interface IDerefLongConsumer extends IDeref, LongConsumer {}
  public interface IDerefConsumer extends IDeref, Consumer {}
  public static DoubleConsumer map(final DoubleUnaryOperator fn, final DoubleConsumer c) {
    return new IDerefDoubleConsumer() {
      public void accept(double v) {
	c.accept(fn.applyAsDouble(v));
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  public static LongConsumer map(final LongUnaryOperator fn, final LongConsumer c) {
    return new IDerefLongConsumer() {
      public void accept(long v) {
	c.accept(fn.applyAsLong(v));
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  @SuppressWarnings("unchecked")
  public static Consumer map(final Function fn, final Consumer c) {
    return new IDerefConsumer() {
      public void accept(Object v) {
	c.accept(fn.apply(v));
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }

  public static DoubleConsumer filter(final DoublePredicate pred, final DoubleConsumer c) {
    return new IDerefDoubleConsumer() {
      public void accept(double v) {
	if(pred.test(v))
	  c.accept(v);
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  public static LongConsumer filter(final LongPredicate pred, final LongConsumer c) {
    return new IDerefLongConsumer() {
      public void accept(long v) {
	if(pred.test(v))
	  c.accept(v);
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  @SuppressWarnings("unchecked")
  public static Consumer filter(final Predicate pred, final Consumer c) {
    return new IDerefConsumer() {
      public void accept(Object v) {
	if(pred.test(v))
	  c.accept(v);
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }

}

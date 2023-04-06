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
  public interface IDerefDoubleConsumer extends IDeref, DoubleConsumer
  {
  }
  public interface IDerefLongConsumer extends IDeref, LongConsumer {}
  public interface IDerefConsumer extends IDeref, Consumer {}
  public interface IDerefIndexedDoubleConsumer extends IDeref, IndexedDoubleConsumer {}
  public interface IDerefIndexedLongConsumer extends IDeref, IndexedLongConsumer {}
  public interface IDerefIndexedConsumer extends IDeref, IndexedConsumer {}
  public static IndexedDoubleConsumer asIndexedDoubleConsumer(Object obj) {
    if (obj instanceof IndexedDoubleConsumer)
      return (IndexedDoubleConsumer)obj;
    return null;
  }
  public static IndexedLongConsumer asIndexedLongConsumer(Object obj) {
    if (obj instanceof IndexedLongConsumer)
      return (IndexedLongConsumer)obj;
    return null;
  }
  public static IndexedConsumer asIndexedConsumer(Object obj) {
    if (obj instanceof IndexedConsumer)
      return (IndexedConsumer)obj;
    return null;
  }
  public static final class IndexedDoubleConsumerConverter implements IDerefDoubleConsumer {
    long idx;
    public final IndexedDoubleConsumer c;
    public IndexedDoubleConsumerConverter(long initIdx, IndexedDoubleConsumer cc) {
      idx = initIdx;
      c = cc;
    }
    public void accept(double v) {
      c.accept(idx, v);
      ++idx;
    }
    public Object deref() { return ((IDeref)c).deref(); }
  }
  public static DoubleConsumer toDoubleConsumer(long initIdx, IndexedDoubleConsumer c) {
    return new IndexedDoubleConsumerConverter(initIdx, c);
  }
  public static final class IndexedLongConsumerConverter implements IDerefLongConsumer {
    long idx;
    public final IndexedLongConsumer c;
    public IndexedLongConsumerConverter(long initIdx, IndexedLongConsumer cc) {
      idx = initIdx;
      c = cc;
    }
    public void accept(long v) {
      c.accept(idx, v);
      ++idx;
    }
    public Object deref() { return ((IDeref)c).deref(); }
  }
  public static LongConsumer toLongConsumer(long initIdx, IndexedLongConsumer c) {
    return new IndexedLongConsumerConverter(initIdx, c);
  }

  public static final class IndexedConsumerConverter implements IDerefConsumer {
    long idx;
    public final IndexedConsumer c;
    public IndexedConsumerConverter(long initIdx, IndexedConsumer cc) {
      idx = initIdx;
      c = cc;
    }
    public void accept(Object v) {
      c.accept(idx, v);
      ++idx;
    }
    public Object deref() { return ((IDeref)c).deref(); }
  }
  public static Consumer toConsumer(long initIdx, IndexedConsumer c) {
    return new IndexedConsumerConverter(initIdx, c);
  }

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

  public static IndexedDoubleConsumer map(final DoubleUnaryOperator fn, final IndexedDoubleConsumer c) {
    return new IDerefIndexedDoubleConsumer() {
      public void accept(long idx, double v) {
	c.accept(idx, fn.applyAsDouble(v));
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  public static IndexedLongConsumer map(final LongUnaryOperator fn, final IndexedLongConsumer c) {
    return new IDerefIndexedLongConsumer() {
      public void accept(long idx, long v) {
	c.accept(idx, fn.applyAsLong(v));
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  @SuppressWarnings("unchecked")
  public static IndexedConsumer map(final Function fn, final IndexedConsumer c) {
    return new IDerefIndexedConsumer() {
      public void accept(long idx, Object v) {
	c.accept(idx, fn.apply(v));
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

  public static IndexedDoubleConsumer filter(final DoublePredicate pred, final IndexedDoubleConsumer c) {
    return new IDerefIndexedDoubleConsumer() {
      public void accept(long idx, double v) {
	if(pred.test(v))
	  c.accept(idx, v);
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  public static IndexedLongConsumer filter(final LongPredicate pred, final IndexedLongConsumer c) {
    return new IDerefIndexedLongConsumer() {
      public void accept(long idx, long v) {
	if(pred.test(v))
	  c.accept(idx, v);
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }
  @SuppressWarnings("unchecked")
  public static IndexedConsumer filter(final Predicate pred, final IndexedConsumer c) {
    return new IDerefIndexedConsumer() {
      public void accept(long idx, Object v) {
	if(pred.test(v))
	  c.accept(idx, v);
      }
      public Object deref() {
	return ((IDeref)c).deref();
      }
    };
  }

  public static class IncConsumer implements Consumer, Reducible, IDeref {
    public static java.util.function.Function cfn = new java.util.function.Function() {
	public IncConsumer apply(Object obj) { return new IncConsumer(); }
      };
    long nElems;
    public IncConsumer(long v) { nElems = v;}
    public IncConsumer() { this(0); }
    public void accept(Object o) { ++nElems; }
    public void inc() { ++nElems; }
    public IncConsumer reduce(Reducible o) {
      nElems += ((IncConsumer)o).nElems;
      return this;
    }
    public long value() { return nElems; }
    public Object deref() { return nElems; }
  }

}

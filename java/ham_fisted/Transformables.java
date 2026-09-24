package ham_fisted;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.RandomAccess;
import java.util.function.Consumer;

import clojure.lang.IFn;
import clojure.lang.Seqable;
import clojure.lang.RT;
import clojure.lang.ISeq;
import clojure.lang.IObj;
import clojure.lang.IDeref;
import clojure.lang.Sequential;
import clojure.lang.Murmur3;
import clojure.lang.Util;
import clojure.lang.IHashEq;
import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentList;
import clojure.lang.Delay;
import clojure.java.api.Clojure;

/**
 * Interfaces and helpers shared by the lazy noncaching transformations.  The
 * implementations of map, filter, concat and friends live in
 * ham-fisted.lazy-noncaching.
 */
public class Transformables {

  public static final Delay toIterableDelay = new Delay(new IFnDef() {
      public Object invoke() {
	return ((IDeref)Clojure.var("ham-fisted.protocols", "->iterable")).deref();
      }
    } );

  public static final Iterable toIterable(Object obj) {
    if (obj instanceof Iterable)
      return (Iterable) obj;
    //else bail to protocol
    return (Iterable)((IFn)toIterableDelay.deref()).invoke(obj);
  }

  /** Constructors for the default IMapable implementations: [map-iterable filter-iterable cat-iterable] */
  static final Delay lzncCtorsDelay = new Delay(new IFnDef() {
      public Object invoke() {
	Clojure.var("clojure.core", "require").invoke(Clojure.read("ham-fisted.lazy-noncaching"));
	return new IFn[] { Clojure.var("ham-fisted.lazy-noncaching", "map-iterable"),
			   Clojure.var("ham-fisted.lazy-noncaching", "filter-iterable"),
			   Clojure.var("ham-fisted.lazy-noncaching", "cat-iterable") };
      }
    });
  static IFn lzncCtor(int idx) { return ((IFn[])lzncCtorsDelay.deref())[idx]; }

  /**
   * Containers that can fuse subsequent map, filter and concat operations.  Defaults
   * wrap this container in the lazy-noncaching implementation of the operation.
   */
  public interface IMapable extends Iterable, IObj {
    default IMapable map(IFn fn) { return (IMapable)lzncCtor(0).invoke(fn, this); }
    default IMapable filter(IFn fn) { return (IMapable)lzncCtor(1).invoke(fn, this); }
    default IMapable cat(Iterable iters) { return (IMapable)lzncCtor(2).invoke(this, iters); }
  }

  public static boolean truthy(final Object obj) {
    return Casts.booleanCast(obj);
  }
  public static boolean not(final Object obj) {
    return obj == null || obj == Boolean.FALSE;
  }
  public static boolean not(final boolean v) {
    return !v;
  }

  public static IFn.OLO toLongReductionFn(Object rfn) {
    if(rfn instanceof IFn.OLO) {
      return (IFn.OLO)rfn;
    }
    if(rfn instanceof IFnDef.ODO) {
      final IFn.ODO rrfn = (IFn.ODO)rfn;
      return new IFnDef.OLO() {
	public Object invokePrim(Object lhs, long rhs) {
	  return rrfn.invokePrim(lhs, (double)rhs);
	}
      };
    }
    IFn rrfn = (IFn)rfn;
    return new IFnDef.OLO() {
      public Object invokePrim(Object lhs, long rhs) {
	return rrfn.invoke(lhs, rhs);
      }
    };
  }

  public static IFn.ODO toDoubleReductionFn(Object rfn) {
    if(rfn instanceof IFn.ODO) {
      return (IFn.ODO)rfn;
    }
    if(rfn instanceof IFnDef.OLO) {
      final IFn.OLO rrfn = (IFn.OLO)rfn;
      return new IFnDef.ODO() {
	public Object invokePrim(Object lhs, double rhs) {
	  return rrfn.invokePrim(lhs, Casts.longCast(rhs));
	}
      };
    }
    IFn rrfn = (IFn)rfn;
    return new IFnDef.ODO() {
      public Object invokePrim(Object lhs, double rhs) {
	return rrfn.invoke(lhs, rhs);
      }
    };
  }

  public static int iterCount(Iterator iter) {
    int c = 0;
    while(iter.hasNext()) {
      c++;
      iter.next();
    }
    return c;
  }

  public static boolean seqEquiv(Seqable ss, Object o){
	ISeq s = ss.seq();
	if(s != null)
	  return s.equiv(o);
	else
	  return (o instanceof Sequential || o instanceof List) && RT.seq(o) == null;
  }

  /**
   * A lazy, noncaching, reducible sequence.  Implementors need only provide iterator,
   * reduce, meta and withMeta.
   */
  public interface IterableSeq extends ICollectionDef, Seqable, IMapable, ITypedReduce,
				       IHashEq, Sequential, IPersistentCollection {
    default int hasheq() { return Murmur3.hashOrdered(this); }
    default boolean equiv(Object o) { return seqEquiv(this, o); }
    default int size() { return iterCount(iterator()); }
    default int count() { return size(); }
    default IPersistentCollection cons(Object o) {
      return RT.cons(o, seq());
    }
    default IPersistentCollection empty() {
      return PersistentList.EMPTY;
    }
    @SuppressWarnings("unchecked")
    default void forEach(Consumer c) {
      ITypedReduce.super.forEach(c);
    }
    default ISeq seq() { return LazyChunkedSeq.chunkIteratorSeq(iterator()); }
  }

  static void appendObjects(StringBuilder sb, Collection data) {
    boolean first = true;
    for(Object o: data) {
      if(!first)
	sb.append(" ");
      first = false;
      sb.append(o == null ? "nil" : o.toString());
    }
  }

  public static String sequenceToString(Iterable data) {
    StringBuilder sb = new StringBuilder();
    if(data instanceof RandomAccess) {
      final List ra = (List) data;
      final int sz = ra.size();
      sb.append("[");
      if(sz < 50) {
	appendObjects(sb, ra);
      } else {
	appendObjects(sb, ra.subList(0, 20));
	sb.append(" ... ");
	appendObjects(sb, ra.subList(sz-20, sz));
      }
      sb.append("]");
    } else {
      sb.append("(");
      if (data != null) {
	int idx = 0;
	for(Object o: data) {
	  if(idx >= 50) {
	    sb.append(" ...");
	    break;
	  }
	  if (idx > 0)
	    sb.append(" ");
	  sb.append(o == null ? "nil" : o.toString());
	  ++idx;
	}
      }
      sb.append(")");
    }
    return sb.toString();
  }
}

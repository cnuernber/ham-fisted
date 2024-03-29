package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.ISeq;
import clojure.lang.Util;
import clojure.lang.RT;
import clojure.lang.ArityException;
import java.util.List;
import java.util.RandomAccess;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;


//UnaryOperator and BinaryOperator have mutually invalid overloads for andThen so we can't implement
//those here.
public interface IFnDef extends IFn
{

  default Object call() {
    return invoke();
  }

  default void run(){
    invoke();
  }

  default Object invoke() {
    return throwArity(0);
  }

  default Object invoke(Object arg1) {
    return throwArity(1);
  }

  default Object invoke(Object arg1, Object arg2) {
    return throwArity(2);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3) {
    return throwArity(3);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
    return throwArity(4);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
    return throwArity(5);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
    return throwArity(6);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7)
  {
    return throwArity(7);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8) {
    return throwArity(8);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9) {
    return throwArity(9);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10) {
    return throwArity(10);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11) {
    return throwArity(11);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
    return throwArity(12);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13)
  {
    return throwArity(13);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14)
  {
    return throwArity(14);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15) {
    return throwArity(15);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16) {
    return throwArity(16);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17) {
    return throwArity(17);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18) {
    return throwArity(18);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18, Object arg19) {
    return throwArity(19);
  }

  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20)
  {
    return throwArity(20);
  }


  default Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20,
		       Object... args)
  {
    return throwArity(21);
  }

  default Object applyTo(ISeq arglist) {
    return ifaceApplyToHelper(this, Util.ret1(arglist,arglist = null));
  }

  static public List asRandomAccess(Object arglist) {
    if(arglist instanceof RandomAccess)
      return (List)arglist;
    else if (arglist instanceof Object[])
      return ArrayLists.toList((Object[])arglist);
    else
      return null;
  }
  @SuppressWarnings("unchecked")
  static public Object ifaceApplyToHelper(IFn ifn, Object arglist) {
    List args = null;
    if( arglist != null ) {
      args = asRandomAccess(arglist);
      if(args == null) {
	ISeq c = RT.seq(arglist);
	if(c != null) {
	  ArrayList al = new ArrayList();
	  for(; c != null; c = c.next()) {
	    al.add(c.first());
	  }
	  args = al;
	}
      }
      arglist = null;
    }
    switch(args != null ? args.size() : 0) {
    case 0:
      return ifn.invoke();
    case 1:
      return ifn.invoke(args.get(0));
    case 2:
      return ifn.invoke(args.get(0), args.get(1));
    case 3:
      return ifn.invoke(args.get(0), args.get(1), args.get(2));
    case 4:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3));
    case 5:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4));
    case 6:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5));
    case 7:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6));
    case 8:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7));
    case 9:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8));
    case 10:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9));
    case 11:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10));
    case 12:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11));
    case 13:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12));
    case 14:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13));
    case 15:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14));
    case 16:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14),
			args.get(15));
    case 17:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14),
			args.get(15), args.get(16));
    case 18:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14),
			args.get(15), args.get(16), args.get(17));
    case 19:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14),
			args.get(15), args.get(16), args.get(17), args.get(18));
    case 20:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14),
			args.get(15), args.get(16), args.get(17), args.get(18), args.get(19));
    default:
      return ifn.invoke(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4),
			args.get(5), args.get(6), args.get(7), args.get(8), args.get(9),
			args.get(10), args.get(11), args.get(12), args.get(13), args.get(14),
			args.get(15), args.get(16), args.get(17), args.get(18), args.get(19),
			args.subList(20, args.size()).toArray());
    }
  }

  default Object throwArity(int n){
    String name = getClass().getName();
    throw new ArityException(n, name);
  }

  public interface O extends IFnDef, Supplier {
    default Object get() { return invoke(); }
  }

  public interface D extends IFnDef, DoubleSupplier, Supplier, IFn.D {
    default double getAsDouble() { return invokePrim(); }
    default Object get() { return invokePrim(); }
    default Object invoke() { return invokePrim(); }
  }

  public interface L extends IFnDef, DoubleSupplier, Supplier, IFn.L {
    default long getAsLong() { return invokePrim(); }
    default Object get() { return invokePrim(); }
    default Object invoke() { return invokePrim(); }
  }

  public interface OO extends IFnDef, UnaryOperator {
    default Object apply(Object arg) { return invoke(arg); }
  }

  public interface OOO extends IFnDef, BinaryOperator {
    default Object apply(Object l, Object r) { return invoke(l,r); }
  }

  public interface LO extends IFnDef, IFn.LO, LongFunction {
    default Object invoke(Object arg) {
      return invokePrim(Casts.longCast(arg));
    }
    default Object apply(long v) { return invokePrim(v); }
  }
  public interface LongPredicate extends IFnDef, IFn.LO, LongFunction,
					 java.util.function.LongPredicate {
    default Object invokePrim(long arg) {
      return test(arg);
    }
    default Object invoke(Object arg) {
      return test(Casts.longCast(arg));
    }
    default Object apply(long v) { return test(v); }
  }
  public interface LL extends IFnDef, IFn.LL, LongUnaryOperator {
    default Object invoke(Object arg) {
      return invokePrim(Casts.longCast(arg));
    }
    default long applyAsLong(long v) {
      return invokePrim(v);
    }
  }
  public interface OL extends IFnDef, IFn.OL, ToLongFunction {
    default Object invoke(Object arg) {
      return invokePrim(arg);
    }
    default long applyAsLong(Object v) { return invokePrim(v); }
  }
  public interface DO extends IFnDef, IFn.DO, DoubleFunction {
    default Object invoke(Object arg) {
      return invokePrim(Casts.doubleCast(arg));
    }
    default Object apply(double v) { return invokePrim(v); }
  }
  public interface DoublePredicate extends IFnDef, IFn.DO, DoubleFunction,
					   java.util.function.DoublePredicate {
    default Object invoke(Object arg) {
      return test(Casts.doubleCast(arg));
    }
    default Object invokePrim(double arg) {
      return test(arg);
    }
    default Object apply(double v) { return test(v); }
  }
  public interface DD extends IFnDef, IFn.DD, DoubleUnaryOperator {
    default Object invoke(Object arg) {
      return invokePrim(Casts.doubleCast(arg));
    }
    default double applyAsDouble(double v) {
      return invokePrim(v);
    }
  }
  public interface OD extends IFnDef, IFn.OD, ToDoubleFunction {
    default Object invoke(Object arg) {
      return invokePrim(arg);
    }
    default double applyAsDouble(Object v) { return invokePrim(v); }
  }
  public interface LD extends IFnDef, IFn.LD {
    default Object invoke(Object arg) {
      return invokePrim(Casts.longCast(arg));
    }
  }
  public interface DL extends IFnDef, IFn.DL {
    default Object invoke(Object arg) {
      return invokePrim(Casts.doubleCast(arg));
    }
  }
  @SuppressWarnings("unchecked")
  public interface Predicate extends IFnDef, UnaryOperator, java.util.function.Predicate {
    default Object invoke(Object v) { return test(v); }
    default Object apply(Object arg) { return test(arg); }
  }
  public interface DDD extends IFnDef, IFn.DDD, DoubleBinaryOperator {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(Casts.doubleCast(lhs), Casts.doubleCast(rhs));
    }
    default double applyAsDouble(double l, double r) {
      return invokePrim(l,r);
    }
  }
  public interface LLL extends IFnDef, IFn.LLL, LongBinaryOperator {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(Casts.longCast(lhs), Casts.longCast(rhs));
    }
    default long applyAsLong(long l, long r) {
      return invokePrim(l,r);
    }
  }
  public interface ODO extends IFnDef, IFn.ODO {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(lhs, Casts.doubleCast(rhs));
    }
  }
  public interface OLO extends IFnDef, IFn.OLO {
    default Object invoke(Object lhs, Object rhs) {
      return invokePrim(lhs, Casts.longCast(rhs));
    }
  }
  public interface LLO extends IFnDef, IFn.LLO {
    default Object invoke(Object l, Object r) {
      return invokePrim(Casts.longCast(l), Casts.longCast(r));
    }
  }

  public interface OLOO extends IFnDef, IFn.OLOO {
    default Object invoke(Object acc, Object idx, Object v) {
      return invokePrim(acc, Casts.longCast(idx), v);
    }
  }
  public interface OLDO extends IFnDef, IFn.OLDO {
    default Object invoke(Object acc, Object idx, Object v) {
      return invokePrim(acc, Casts.longCast(idx), Casts.doubleCast(v));
    }
  }
  public interface OLLO extends IFnDef, IFn.OLLO {
    default Object invoke(Object acc, Object idx, Object v) {
      return invokePrim(acc, Casts.longCast(idx), Casts.longCast(v));
    }
  }
  public interface DDDD extends IFnDef, IFn.DDDD {
    default Object invoke (Object a, Object b, Object c) {
      return invokePrim(Casts.doubleCast(a),
			Casts.doubleCast(b),
			Casts.doubleCast(c));
    }
  }
  public interface LLLL extends IFnDef, IFn.LLLL {
    default Object invoke (Object a, Object b, Object c) {
      return invokePrim(Casts.longCast(a),
			Casts.longCast(b),
			Casts.longCast(c));
    }
  }
}

package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.ISeq;

public class MapFn implements IFnDef {
  public static IFn create(IFn src, IFn dst) {
    if(src instanceof IFn.OD && dst instanceof IFn.DD) {
      final IFn.OD ss = (IFn.OD)src;
      final IFn.DD dd = (IFn.DD)dst;
      return new Reductions.OD() {
	public double invokePrim(Object obj) {
	  return dd.invokePrim(ss.invokePrim(obj));
	}
      };
    }
    else if (src instanceof IFn.OL && dst instanceof IFn.LL) {
      final IFn.OL ss = (IFn.OL)src;
      final IFn.LL dd = (IFn.LL)dst;
      return new Reductions.OL() {
	public long invokePrim(Object obj) {
	  return dd.invokePrim(ss.invokePrim(obj));
	}
      };
    } else if( src instanceof IFn.LL && dst instanceof IFn.LL) {
      final IFn.LL ss = (IFn.LL)src;
      final IFn.LL dd = (IFn.LL)dst;
      return new Reductions.LL() {
	public long invokePrim(long v) {
	  return dd.invokePrim(ss.invokePrim(v));
	}
      };
    }
    else if( src instanceof IFn.DD && dst instanceof IFn.DD) {
      final IFn.DD ss = (IFn.DD)src;
      final IFn.DD dd = (IFn.DD)dst;
      return new Reductions.DD() {
	public double invokePrim(double v) {
	  return dd.invokePrim(ss.invokePrim(v));
	}
      };
    }
    //Fallthrough, no special treatment
    return new MapFn(src, dst);
  }

  public final IFn srcFn;
  public final IFn dstFn;
  private MapFn(IFn sfn, IFn dfn) {
    srcFn = sfn;
    dstFn = dfn;
  }

  public Object invoke() {
    return dstFn.invoke(srcFn.invoke());
  }

  public Object invoke(Object arg1) {
    return dstFn.invoke(srcFn.invoke(arg1));
  }

  public Object invoke(Object arg1, Object arg2) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7)
  {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13)
  {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14)
  {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15, arg16));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15, arg16, arg17));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18, Object arg19) {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19));
  }

  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20)
  {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20));
  }


  public Object invoke(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
		       Object arg8, Object arg9, Object arg10, Object arg11, Object arg12, Object arg13, Object arg14,
		       Object arg15, Object arg16, Object arg17, Object arg18, Object arg19, Object arg20,
		       Object... args)
  {
    return dstFn.invoke(srcFn.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10,
				     arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20,
				     args));
  }
  public Object applyTo(ISeq args) {
    return dstFn.invoke(srcFn.invoke(args));
  }

}

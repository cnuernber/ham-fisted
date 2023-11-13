package ham_fisted;


import java.util.Random;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongBinaryOperator;
import clojure.lang.RT;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.IDeref;
import clojure.lang.Util;
import clojure.lang.Reduced;
import clojure.lang.ISeq;



public class Ranges {
  public static class LongRange implements LongMutList, TypedList {
    public final long start;
    public final long end;
    public final long step;
    public final long nElems;
    public final IPersistentMap meta;
    int _hash = 0;
    public LongRange(long s, long e, long _step, IPersistentMap m) {
      start = s;
      end = e;
      step = _step;
      nElems = (e - s)/_step;
      if (nElems < 0)
	throw new RuntimeException("Invalid Range - start: " + String.valueOf(s)
				   + " end: " + String.valueOf(e) + " step: " +
				   String.valueOf(_step));
      meta = m;
    }
    public IMutList cloneList() { return this; }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public int hasheq() {
      if (_hash == 0) {
	_hash = LongMutList.super.hasheq();
      }
      return _hash;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public Class containedType() { return Long.TYPE; }
    public int size() { return RT.intCast(nElems); }
    public long lgetLong(long idx) {
      final long sz = nElems;
      if(idx < 0 || idx >= sz)
	throw new IndexOutOfBoundsException("Index out of range: " + String.valueOf(idx) +
					    " size: " + String.valueOf(sz));
      return start + step*idx;
    }
    public long getLong(int idx) {
      return lgetLong(idx);
    }
    public int[] toIntArray() {
      return ArrayLists.iarange(RT.intCast(start), RT.intCast(end), RT.intCast(step));
    }
    public long[] toLongArray() {
      return ArrayLists.larange(start, end, step);
    }
    public double[] toDoubleArray() {
      return ArrayLists.darange(start, end, step);
    }
    public Object[] toArray() {
      final int sz = size();
      final Object[] retval = new Object[sz];
      long st = start;
      final long se = step;
      for(int idx = 0; idx < sz; ++idx, st += se)
	retval[idx] = st;

      return retval;
    }
    public LongMutList subList(long sidx, long eidx) {
      ChunkedList.sublistCheck(sidx, eidx, nElems);
      return new LongRange(start + sidx*step, start + eidx*step, step, meta);
    }
    public ISeq seq() { return new SublistSeq(this, 0, null); }
    public LongMutList subList(int sidx, int eidx) {
      return subList((long)sidx, (long)eidx);
    }
    public Object reduce(final IFn fn, Object init) {
      if(nElems == 0) return init;
      Object acc = init;
      long n = nElems;
      long i = start;
      if(!(fn instanceof IFn.OLO))
	do {
	  acc = fn.invoke(acc, i);
	  if (RT.isReduced(acc)) return ((Reduced)acc).deref();
	  i += step;
	  n--;
	} while(n > 0);
      else {
	final IFn.OLO ff = (IFn.OLO)fn;
	do {
	  acc = ff.invokePrim(acc, i);
	  if (RT.isReduced(acc)) return ((Reduced)acc).deref();
	  i += step;
	  n--;
	} while(n > 0);
      }
      return acc;
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				     ParallelOptions options) {
      return Reductions.parallelIndexGroupReduce(new IFnDef.LLO() {
	  public Object invokePrim(long sidx, long eidx) {
	    return Reductions.serialReduction(rfn, initValFn.invoke(), subList(sidx, eidx));
	  }
	}, nElems, mergeFn, options);
    }
    public Object lnth(long idx) {
      final long sz = nElems;
      if (idx < 0)
	idx = idx + sz;
      return lgetLong(idx);
    }
    public Object lnth(long idx, Object notFound) {
      final long sz = nElems;
      if (idx < 0)
	idx = idx + sz;
      return idx < sz && idx > -1 ? lgetLong(idx) : notFound;
    }
    public Object invoke(Object arg) {
      return lnth(Casts.longCast(arg));
    }
    public Object invoke(Object arg, Object defVal) {
      if(Util.isInteger(arg)) {
	return lnth(Casts.longCast(arg), defVal);
      } else {
	return defVal;
      }
    }
    public IPersistentMap meta() { return meta; }
    public LongRange withMeta(IPersistentMap m) {
      return new LongRange(start, end, step, m);
    }
  };

  public static class DoubleRange implements DoubleMutList, TypedList {
    public final double start;
    public final double end;
    public final double step;
    public final long nElems;
    public final IPersistentMap meta;
    int _hash = 0;
    public DoubleRange(double s, double e, double _step, IPersistentMap _meta) {
      start = s;
      end = e;
      step = _step;
      meta = _meta;
      //Floor to long intentional
      nElems = Math.max(0, (long)((e - s)/_step));
      if (nElems < 0)
	throw new IndexOutOfBoundsException("Invalid Range - start: " + String.valueOf(s)
				   + " end: " + String.valueOf(e) + " step: " +
				   String.valueOf(_step));
    }
    public IMutList cloneList() { return this; }
    public boolean equals(Object other) {
      return equiv(other);
    }
    public int hashCode() { return hasheq(); }
    public int hasheq() {
      if (_hash == 0) {
	_hash = DoubleMutList.super.hasheq();
      }
      return _hash;
    }
    public String toString() { return Transformables.sequenceToString(this); }
    public Class containedType() { return Double.TYPE; }
    public int size() { return RT.intCast(nElems); }
    public double lgetDouble(long idx) {
      final long sz = nElems;
      if(idx < 0)
	idx += sz;
      if(idx < 0 || idx >= sz)
	throw new IndexOutOfBoundsException("Index out of range: " + String.valueOf(idx) +
				   " size: " + String.valueOf(sz));
      return start + step*idx;
    }
    public ISeq seq() { return new SublistSeq(this, 0, null); }
    public double getDouble(int idx) { return lgetDouble(idx); }
    public int[] toIntArray() {
      final int st = RT.intCast(step);
      if (st != 0)
	return ArrayLists.iarange(RT.intCast(start), RT.intCast(end), RT.intCast(step));
      throw new IndexOutOfBoundsException("Infinite range: " + String.valueOf(step) + " : " + String.valueOf(st));
    }
    public long[] toLongArray() {
      final long st = Casts.longCast(step);
      if (st != 0)
	return ArrayLists.larange(Casts.longCast(start), Casts.longCast(end), Casts.longCast(step));
      throw new IndexOutOfBoundsException("Infinite range: " + String.valueOf(step) + " : " + String.valueOf(st));
    }
    public double[] toDoubleArray() {
      return ArrayLists.darange(start, end, step);
    }
    public DoubleMutList subList(long sidx, long eidx) {
      ChunkedList.sublistCheck(sidx, eidx, size());
      return new DoubleRange(start + sidx*step, start + eidx*step, step, meta);
    }
    public DoubleMutList subList(int sidx, int eidx) {
      return subList((long)sidx, (long)eidx);
    }
    public Object reduce(final IFn fn, Object init) {
      final IFn.ODO rrfn = fn instanceof IFn.ODO ? (IFn.ODO)fn : new IFn.ODO() {
	  public Object invokePrim(Object lhs, double v) {
	    return fn.invoke(lhs, v);
	  }
	};
      return doubleReduction(rrfn, init);
    }
    public Object doubleReduction(IFn.ODO op, Object init) {
      final long sz = nElems;
      final double se = step;
      final double st = start;
      for(long idx = 0; idx < sz && !RT.isReduced(init); ++idx)
	init = op.invokePrim(init, st + (se*idx));
      return Reductions.unreduce(init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				     ParallelOptions options) {
      if(nElems > options.minN) {
	return Reductions.parallelIndexGroupReduce(new IFnDef.LLO() {
	    public Object invokePrim(long sidx, long eidx) {
	      return Reductions.serialReduction(rfn, initValFn.invoke(), subList(sidx, eidx));
	    }
	  }, nElems, mergeFn, options);
      } else {
	return Reductions.serialReduction(rfn, initValFn.invoke(), this);
      }
    }
    public Object lnth(long idx) {
      final long sz = nElems;
      if (idx < 0)
	idx = idx + sz;
      return lgetDouble(idx);
    }
    public Object lnth(long idx, Object notFound) {
      final long sz = nElems;
      if (idx < 0)
	idx = idx + sz;
      return idx < sz && idx > -1 ? lgetDouble(idx) : notFound;
    }
    public Object invoke(Object arg) {
      return lnth(Casts.longCast(arg));
    }
    public Object invoke(Object arg, Object defVal) {
      if(Util.isInteger(arg)) {
	return lnth(Casts.longCast(arg), defVal);
      } else {
	return defVal;
      }
    }
    public IPersistentMap meta() { return meta; }
    public DoubleRange withMeta(IPersistentMap m) {
      return new DoubleRange(start, end, step, m);
    }
  };
}

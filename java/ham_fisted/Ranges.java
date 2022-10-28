package ham_fisted;


import java.util.Random;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.LongBinaryOperator;
import clojure.lang.RT;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.IDeref;



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
    public long getLong(int idx) {
      final long sz = nElems;
      if(idx < 0)
	idx += sz;
      if(idx < 0 || idx >= sz)
	throw new RuntimeException("Index out of range: " + String.valueOf(idx) +
				   " size: " + String.valueOf(sz));
      return start + step*idx;
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
    public LongMutList subList(int sidx, int eidx) {
      return subList((long)sidx, (long)eidx);
    }
    public Object reduce(final IFn fn, Object init) {
      final IFn.OLO rrfn = fn instanceof IFn.OLO ? (IFn.OLO)fn : new IFn.OLO() {
	  public Object invokePrim(Object lhs, long v) {
	    return fn.invoke(lhs, v);
	  }
	};
      return longReduction(rrfn, init);
    }
    public Object longReduction(IFn.OLO op, Object init) {
      final long sz = nElems;
      final long se = step;
      long st = start;
      for(long idx = 0; idx < sz && !RT.isReduced(init); ++idx, st += se)
	init = op.invokePrim(init, st);
      return init;
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
	throw new RuntimeException("Invalid Range - start: " + String.valueOf(s)
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
    public double getDouble(int idx) {
      final long sz = nElems;
      if(idx < 0)
	idx += sz;
      if(idx < 0 || idx >= sz)
	throw new RuntimeException("Index out of range: " + String.valueOf(idx) +
				   " size: " + String.valueOf(sz));
      return start + step*idx;
    }
    public int[] toIntArray() {
      final int st = RT.intCast(step);
      if (st != 0)
	return ArrayLists.iarange(RT.intCast(start), RT.intCast(end), RT.intCast(step));
      throw new RuntimeException("Infinite range: " + String.valueOf(step) + " : " + String.valueOf(st));
    }
    public long[] toLongArray() {
      final long st = Casts.longCast(step);
      if (st != 0)
	return ArrayLists.larange(Casts.longCast(start), Casts.longCast(end), Casts.longCast(step));
      throw new RuntimeException("Infinite range: " + String.valueOf(step) + " : " + String.valueOf(st));
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
      return init;
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
    public IPersistentMap meta() { return meta; }
    public DoubleRange withMeta(IPersistentMap m) {
      return new DoubleRange(start, end, step, m);
    }
  };
}

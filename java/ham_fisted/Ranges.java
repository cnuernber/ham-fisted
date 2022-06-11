package ham_fisted;


import clojure.lang.RT;


public class Ranges {
  public static class LongRange implements LongMutList, TypedList {
    public final long start;
    public final long end;
    public final long step;
    public final int nElems;
    public LongRange(long s, long e, long _step) {
      start = s;
      end = e;
      step = _step;
      nElems = RT.intCast((e - s)/_step);
      if (nElems < 0)
	throw new RuntimeException("Invalid Range - start: " + String.valueOf(s)
				   + " end: " + String.valueOf(e) + " step: " +
				   String.valueOf(_step));
    }
    public Class containedType() { return Long.TYPE; }
    public int size() { return nElems; }
    public long getLong(int idx) {
      final int sz = size();
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
    public LongMutList subList(int sidx, int eidx) {
      final int sz = size();
      if(sidx < 0 || sidx >= sz)
	throw new RuntimeException("Start index out of range");
      if(eidx < sidx || eidx > sz)
	throw new RuntimeException("End index out of range");
      return new LongRange(start + sidx*step, start + eidx*step, step);
    }
  };

  public static class DoubleRange implements DoubleMutList, TypedList {
    public final double start;
    public final double end;
    public final double step;
    public final int nElems;
    public DoubleRange(double s, double e, double _step) {
      start = s;
      end = e;
      step = _step;
      nElems = RT.intCast((e - s)/_step);
      if (nElems < 0)
	throw new RuntimeException("Invalid Range - start: " + String.valueOf(s)
				   + " end: " + String.valueOf(e) + " step: " +
				   String.valueOf(_step));
    }
    public Class containedType() { return Double.TYPE; }
    public int size() { return nElems; }
    public double getDouble(int idx) {
      final int sz = size();
      if(idx < 0)
	idx += sz;
      if(idx < 0 || idx >= sz)
	throw new RuntimeException("Index out of range: " + String.valueOf(idx) +
				   " size: " + String.valueOf(sz));
      return start + step*idx;
    }
    public double[] toDoubleArray() {
      return ArrayLists.darange(start, end, step);
    }
    public DoubleMutList subList(int sidx, int eidx) {
      final int sz = size();
      if(sidx < 0 || sidx >= sz)
	throw new RuntimeException("Start index out of range");
      if(eidx < sidx || eidx > sz)
	throw new RuntimeException("End index out of range");
      return new DoubleRange(start + sidx*step, start + eidx*step, step);
    }
  };
}

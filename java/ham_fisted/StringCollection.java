package ham_fisted;


import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.Objects;
import clojure.lang.IReduce;
import clojure.lang.IReduceInit;
import clojure.lang.IFn;
import clojure.lang.RT;


public class StringCollection implements IMutList<Character> {
  public final  String cs;
  public StringCollection(String _cs) {
    cs = _cs;
  }
  public final int size() { return cs.length(); }
  public final Character get(int idx) { return cs.charAt(idx); }
  public final IMutList<Character> subList(int startidx, int endidx) {
    return new StringCollection((String)cs.subSequence(startidx, endidx));
  }
  public final Object[] fillArray(Object[] data) {
    Reductions.serialReduction(new Reductions.IndexedAccum( new IFnDef.OLOO() {
	public Object invokePrim(Object acc, long idx, Object v) {
	  ((Object[])acc)[(int)idx] = v;
	  return acc;
	}
      }),  data, this);
    return data;
  }
  public Object reduce(IFn rfn) {
    final int sz = size();
    if (sz == 0) return rfn.invoke();
    Object acc = null;
    for( int idx = 0; idx < sz && !RT.isReduced(acc); ++idx)
      acc = idx == 0 ? cs.charAt(idx) : rfn.invoke(acc, cs.charAt(idx));
    return acc;
  }

  public Object reduce(IFn rfn, Object acc) {
    final int sz = size();
    for( int idx = 0; idx < sz && !RT.isReduced(acc); ++idx)
      rfn.invoke(acc, cs.charAt(idx));
    return acc;
  }
}

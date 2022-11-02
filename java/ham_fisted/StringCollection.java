package ham_fisted;


import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.Objects;


public class StringCollection implements IMutList<Character> {
  public final  String cs;
  char[] csData;
  public StringCollection(String _cs) {
    cs = _cs;
  }
  public final int size() { return cs.length(); }
  public final char[] toCharArray() {
    if (csData == null)
      csData = cs.toCharArray();
    return csData;
  }
  public final Character get(int idx) { return toCharArray()[idx]; }
  public final IMutList<Character> subList(int startidx, int endidx) {
    return new StringCollection((String)cs.subSequence(startidx, endidx));
  }
  public final Object[] fillArray(Object[] data) {
    final char[] cd = toCharArray();
    final int sz = cd.length;
    for (int idx = 0; idx < sz; ++idx)
      data[idx] = cd[idx];
    return data;
  }
}

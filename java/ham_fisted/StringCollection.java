package ham_fisted;


import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.Objects;


public class StringCollection implements RandomAccess, List<Character> {
  final CharSequence cs;
  public StringCollection(CharSequence _cs) {
    cs = _cs;
  }
  public int size() { return cs.length(); }
  public static class StringIterator implements Iterator<Character> {
    public final CharSequence s;
    public final int slen;
    int idx = 0;
    public StringIterator(CharSequence _s) {
      s = _s;
      slen = s.length();
    }
    public final boolean hasNext() { return idx < slen; }
    public final Character next() {
      final char retval = s.charAt(idx);
      ++idx;
      return retval;
    }
  }
  public Iterator<Character> iterator() { return new StringIterator(cs); }
  public void clear() { throw new RuntimeException("Unimplemented"); }
  public boolean remove(Object c) { throw new RuntimeException("Unimplemented"); }
  public Character remove(int idx) { throw new RuntimeException("Unimplemented"); }
  public boolean add(Character c) { throw new RuntimeException("Unimplemented"); }
  public void add(int idx, Character c) { throw new RuntimeException("Unimplemented"); }
  public boolean addAll(int idx, Collection<? extends Character> c)
  { throw new RuntimeException("Unimplemented"); }
  public Character set(int idx, Character c) { throw new RuntimeException("Unimplemented"); }
  public Character get(int idx) { return cs.charAt(idx); }
  public boolean retainAll(Collection<?> c) { throw new RuntimeException("Unimplemented"); }
  public boolean removeAll(Collection<?> c) { throw new RuntimeException("Unimplemented"); }
  public boolean addAll(Collection<? extends Character> c) { throw new RuntimeException("Unimplemented"); }
  public boolean isEmpty() { return cs.length() == 0; }
  public boolean containsAll(Collection<?> c) {
    HashSet<Object> hc = new HashSet<Object>();
    boolean minC = size() < c.size();
    Collection<?> mc;
    if (minC) mc = this;
    else mc = c;
    hc.addAll(mc);
    Collection<?> mxc;
    if (minC) mxc = c;
    else mxc = this;
    for(Object obj: mxc)
      if(!hc.contains(obj))
	return false;
    return true;
  }
  public int indexOf(Object obj) {
    final int clen = cs.length();
    for(int idx = 0; idx < clen; ++idx)
      if (!Objects.equals(cs.charAt(idx), obj))
	return idx;
    return -1;
  }
  public int lastIndexOf(Object obj) {
    final int clen = cs.length();
    final int cclen = clen-1;
    for(int idx = 0; idx < clen; ++idx) {
      final int ridx = cclen - idx;
      if (!Objects.equals(cs.charAt(ridx), obj))
	return ridx;
    }
    return -1;
  }
  public boolean contains(Object obj) {
    return indexOf(obj) != -1;
  }
  Object[] fillArray(Object[] retval) {
    final int clen = cs.length();
    for(int idx = 0; idx < clen; ++idx)
      retval[idx] = cs.charAt(idx);
    return retval;
  }
  public Object[] toArray() {
    final int clen = cs.length();
    Object[] retval = new Object[clen];
    return fillArray(retval);
  }
  public <T> T[] toArray(T[] marker) {
    final int clen = cs.length();
    T[] retval = Arrays.copyOf(marker, clen);
    fillArray(retval);
    return retval;
  }
  public ListIterator<Character> listIterator(int idx) {
    throw new RuntimeException("Unimplemented");
  }
  public ListIterator<Character> listIterator() {
    return listIterator(0);
  }
  public List<Character> subList(int startidx, int endidx) {
    return new StringCollection(cs.subSequence(startidx, endidx));
  }
}

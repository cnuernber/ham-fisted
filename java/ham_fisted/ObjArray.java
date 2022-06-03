package ham_fisted;


import java.util.Arrays;
import java.util.Iterator;


public class ObjArray {
  public static final Object[] create() { return new Object[0]; }
  public static final Object[] create(Object a) { return new Object[] {a}; }
  public static final Object[] create(Object a, Object b) { return new Object[] {a, b}; }
  public static final Object[] create(Object a, Object b, Object c) {
    return new Object[] {a, b, c};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d) {
    return new Object[] {a, b, c, d};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d, Object e) {
    return new Object[] {a, b, c, d, e};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f) {
    return new Object[] {a, b, c, d, e, f};
  }
  public static final Object[] createv(Object a, Object b, Object c, Object d,
				       Object e, Object f, Object[] extra) {
    final int el = extra.length;
    final int len = 6 + el;
    Object[] retval = new Object[len];
    retval[0] = a; retval[1] = b; retval[2] = c;
    retval[3] = d; retval[4] = e; retval[5] = f;
    System.arraycopy(extra, 0, retval, 6, el);
    return retval;
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g) {
    return new Object[] {a, b, c, d, e, f, g};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h) {
    return new Object[] {a, b, c, d, e, f, g, h};
  }
  public static final Object[] createv(Object a, Object b, Object c, Object d,
				       Object e, Object f, Object g, Object h,
				       Object[] extra) {
    final int el = extra.length;
    final int len = 8 + el;
    Object[] retval = new Object[len];
    retval[0] = a; retval[1] = b; retval[2] = c;
    retval[3] = d; retval[4] = e; retval[5] = f;
    retval[6] = g; retval[7] = h;
    System.arraycopy(extra, 0, retval, 8, el);
    return retval;
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i) {
    return new Object[] {a, b, c, d, e, f, g, h, i};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j, Object k) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j, k};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j, Object k, Object l) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j, k, l};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j, Object k, Object l,
				      Object m) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j, k, l, m};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j, Object k, Object l,
				      Object m, Object n) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j, k, l, m, n};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j, Object k, Object l,
				      Object m, Object n, Object o) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j, k, l, m, n, o};
  }
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object g, Object h,
				      Object i, Object j, Object k, Object l,
				      Object m, Object n, Object o, Object p) {
    return new Object[] {a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p};
  }
  public static final Object[] createv(Object a, Object b, Object c, Object d,
				       Object e, Object f, Object g, Object h,
				       Object i, Object j, Object k, Object l,
				       Object m, Object n, Object o, Object p,
				       Object[] extra) {
    final int el = extra.length;
    final int len = 16 + el;
    Object[] retval = new Object[len];
    retval[0] = a; retval[1] = b; retval[2] = c;
    retval[3] = d; retval[4] = e; retval[5] = f;
    retval[6] = g; retval[7] = h; retval[8] = i;
    retval[9] = j; retval[10] = k; retval[11] = l;
    retval[12] = m; retval[13] = n; retval[14] = o;
    retval[15] = p;
    System.arraycopy(extra, 0, retval, 16, el);
    return retval;
  }


  public static Object[] iterFill(Object[] data, int idx, Iterator iter) {
    while(iter.hasNext())
      data[idx++] = iter.next();
    return data;
  }
}

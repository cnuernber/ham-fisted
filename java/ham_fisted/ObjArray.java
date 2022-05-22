package ham_fisted;


import java.util.Arrays;


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
  public static final Object[] create(Object a, Object b, Object c, Object d,
				      Object e, Object f, Object... extra) {
    final int el = extra.length;
    final int len = 6 + el;
    Object[] retval = new Object[len];
    retval[0] = a; retval[1] = b; retval[2] = c;
    retval[3] = d; retval[4] = e; retval[5] = f;
    System.arraycopy(extra, 0, retval, 6, el);
    return retval;
  }
}

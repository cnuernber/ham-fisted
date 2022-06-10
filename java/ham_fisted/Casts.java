package ham_fisted;



import clojure.lang.Util;
import clojure.lang.RT;


public class Casts {
  public static boolean booleanCast(Object obj) {
    if(obj == null)
      return false;
    if(obj instanceof Boolean)
      return (Boolean)obj;
    if(obj instanceof Number)
      return ((Number)obj).longValue() != 0;
    if(obj instanceof Character)
      return ((Character)obj).charValue() != 0;
    return true;
  }
  public static boolean booleanCast(long obj) {
    return obj != 0;
  }
  public static boolean booleanCast(double obj) {
    return obj != 0.0;
  }
  public static boolean booleanCast(boolean obj) {
    return obj;
  }
  public static long longCast(Object obj) {
    if (obj instanceof Boolean)
      return ((Boolean)obj) ? 1 : 0;
    return RT.longCast(obj);
  }
  public static long longCast(long obj) {
    return obj;
  }
  public static long longCast(double obj) {
    return (long)obj;
  }
  public static long longCast(boolean obj) {
    return obj ? 1 : 0;
  }
  public static double doubleCast(Object obj) {
    return RT.doubleCast(obj);
  }
  public static double doubleCast(long obj) {
    return RT.doubleCast(obj);
  }
  public static double doubleCast(double obj) {
    return RT.doubleCast(obj);
  }
  public static double doubleCast(boolean obj) {
    return obj ? 1.0 : 0.0;
  }
}

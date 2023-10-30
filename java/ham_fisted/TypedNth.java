package ham_fisted;


import clojure.lang.RT;


public class TypedNth {
  public static double dnth(Object v, long idx) {    
    return (v instanceof double[]) ? dnth((double[])v, idx) : Casts.doubleCast(RT.nth(v, (int)idx));
  }
  public static double dnth(double[] v, long idx) {
    return v[(int)idx];
  }
  public static float fnth(Object v, long idx) {    
    return (v instanceof float[]) ? fnth((float[])v, idx) : Casts.floatCast(RT.nth(v, (int)idx));
  }
  public static float fnth(float[] v, long idx) {
    return v[(int)idx];
  }
  public static long lnth(Object v, long idx) {
    return (v instanceof long[]) ? lnth((long[])v, idx) : Casts.longCast(RT.nth(v, (int)idx));
  }
  public static long lnth(long[] v, long idx) {
    return v[(int)idx];
  }
  public static int inth(Object v, long idx) {
    return (v instanceof int[]) ? inth((int[])v, idx) : Casts.intCast(RT.nth(v, (int)idx));
  }
  public static int inth(int[] v, long idx) {
    return v[(int)idx];
  }
}
  

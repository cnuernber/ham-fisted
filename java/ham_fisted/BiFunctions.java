package ham_fisted;


import java.util.function.BiFunction;


public class BiFunctions {
  public static final BiFunction incBiFn = (k, v)-> v == null ? Long.valueOf(1) : Long.valueOf(((long)v) + 1);
  public static final BiFunction rhsWins = (v1,v2)->v2;
}

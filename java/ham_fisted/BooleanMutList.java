package ham_fisted;


import java.util.Random;
import java.util.List;
import it.unimi.dsi.fastutil.booleans.BooleanArrays;


public interface BooleanMutList extends IMutList<Object> {
  default boolean add(Object obj) { addBoolean(Casts.booleanCast(obj)); return true; }
  default void addDouble(double obj) { addBoolean(Casts.booleanCast(obj));}
  default void addLong(double obj) { addBoolean(Casts.booleanCast(obj));}
  @SuppressWarnings("unchecked")
  default Object set(int idx, Object obj) { final Boolean v = getBoolean(idx); setBoolean(idx, Casts.booleanCast(obj)); return v; }
  default void setDouble(int idx, double obj) { setBoolean(idx, Casts.booleanCast(obj)); }
  default void setLong(int idx, long obj) { setBoolean(idx, Casts.booleanCast(obj)); }
  default Object get(int idx) { return getBoolean(idx); }
  default double getDouble(int idx) { return Casts.doubleCast(getBoolean(idx)); }
  default long getLong(int idx) { return Casts.longCast(getBoolean(idx)); }
  default void fillRange(int startidx, final int endidx, Object v) {
    boolean l = Casts.booleanCast(v);
    for(; startidx < endidx; ++startidx) {
      setBoolean(startidx, l);
    }
  }
}

package ham_fisted;


import java.util.function.BiFunction;
import java.util.function.Function;

public interface ImmutValues {
  public ImmutValues immutUpdateValues(BiFunction valueMap);
  public ImmutValues immutUpdateValue(Object key, Function fn);
}

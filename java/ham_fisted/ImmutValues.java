package ham_fisted;


import java.util.function.BiFunction;
import clojure.lang.IFn;

public interface ImmutValues {
  public ImmutValues immutUpdateValues(BiFunction valueMap);
  //Sorry java people, this has to be a clojure IFn for perf reasons.
  public ImmutValues immutUpdateValue(Object key, IFn fn);
}

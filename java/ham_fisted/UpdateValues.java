package ham_fisted;

import java.util.function.BiFunction;
import clojure.lang.IFn;
import clojure.lang.ITransientCollection;

public interface UpdateValues extends ImmutValues, ITransientCollection {
  public UpdateValues updateValues(BiFunction valueMap);
  public UpdateValues updateValue(Object key, IFn fn);
  
  default ImmutValues immutUpdateValues(BiFunction valueMap) {
    return (ImmutValues)updateValues(valueMap).persistent();
  }
  default ImmutValues immutUpdateValue(Object key, IFn fn) {
    return (ImmutValues)updateValue(key, fn).persistent();
  }
}

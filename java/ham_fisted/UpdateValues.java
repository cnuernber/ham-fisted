package ham_fisted;

import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.IFn;
import clojure.lang.ITransientCollection;

public interface UpdateValues {
  public UpdateValues updateValues(BiFunction valueMap);
  public UpdateValues updateValue(Object key, Function fn);
}

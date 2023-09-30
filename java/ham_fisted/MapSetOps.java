package ham_fisted;


import java.util.Map;
import java.util.function.BiFunction;


public interface MapSetOps {
  Map union(Map rhs, BiFunction bfn);
  Map intersection(Map rhs, BiFunction bfn);
  Map difference(Map rhs);
}

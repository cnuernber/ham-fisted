package ham_fisted;


import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;


public interface MapSetOps {
  Map union(Map rhs, BiFunction bfn);
  Map intersection(Map rhs, BiFunction bfn);
  //Trim to these keys
  Map intersection(Set rhs);
  default Map difference(Map rhs) { return difference(rhs.keySet()); }
  Map difference(Set rhs);
}

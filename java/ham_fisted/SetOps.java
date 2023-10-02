package ham_fisted;

import java.util.Set;
import java.util.Collection;

public interface SetOps {
  Set union(Collection rhs);
  Set intersection(Set rhs);
  Set difference(Collection rhs);
}

package ham_fisted;


import java.util.Objects;


public interface HashProvider {
  public default int hash(Object obj) {
    return obj != null ? IntegerOps.mixhash(obj.hashCode()) : 0;
  }
  public default boolean equals(Object lhs, Object rhs) {
    return Objects.equals(lhs,rhs);
  }
}

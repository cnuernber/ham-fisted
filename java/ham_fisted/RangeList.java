package ham_fisted;


import java.util.List;


public interface RangeList {
  public void fillRange(long startidx, long endidx, Object v);
  default void fillRange(long startidx, List v) {
    fillRangeReducible(startidx, v);
  }
  public void fillRangeReducible(long startidx, Object v);
  public void removeRange(long startidx, long endidx);
}

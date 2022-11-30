package ham_fisted;


import java.util.List;


public interface RangeList {
  public void fillRange(long startidx, long endidx, Object v);
  public void fillRange(long startidx, List v);
  public void removeRange(long startidx, long endidx);
}

package ham_fisted;


import java.util.List;


public interface RangeList {
  public void fillRange(int startidx, int endidx, Object v);
  public void fillRange(int startidx, List v);
  public void removeRange(int startidx, int endidx);
}

package ham_fisted;


import java.util.List;


public interface RangeList<E> {
  public void fillRange(int startidx, int endidx, E v);
  public void fillRange(int startidx, List<? extends E> v);
  public void addRange(int startidx, int endidx, E v);
  public void removeRange(int startidx, int endidx);
}

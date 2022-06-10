package ham_fisted;


import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import clojure.lang.RT;


public interface ImmutSort<E> extends List<E> {
  default List immutSort() { return immutSort(null); }
  @SuppressWarnings("unchecked")
  default List immutSort(Comparator c) {
    final Object[] data = toArray();
    if (c != null)
      Arrays.sort(data, (Comparator)c);
    else
      Arrays.sort(data);
    return ArrayLists.toList(data, 0, data.length, RT.meta(this));
  }
}

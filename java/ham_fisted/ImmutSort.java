package ham_fisted;


import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import clojure.lang.RT;


public interface ImmutSort<E> extends List<E> {
  default List immutSort() { return immutSort(null); }
  @SuppressWarnings("unchecked")
  default List immutSort(Comparator c) {
    final Object[] data = toArray();
    if (c != null)
      ObjectArrays.parallelQuickSort(data, (Comparator)c);
    else
      ObjectArrays.parallelQuickSort(data);
    return ArrayLists.toList(data, 0, data.length, RT.meta(this));
  }
}

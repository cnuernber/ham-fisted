package ham_fisted;


import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import clojure.lang.RT;
import clojure.lang.Util;


public interface ImmutSort<E> extends List<E> {

  @SuppressWarnings("unchecked")
  public static final Comparator defaultComparator = new Comparator() {
      public int compare(Object l, Object r) {
	if(l == r) return 0;
	if(l == null) return -1;
	if(r == null) return 1;
	Class cls = l.getClass();
	if(Comparable.class.isAssignableFrom(cls)
	   && cls == r.getClass())
	  return ((Comparable)l).compareTo(r);
	return Util.compare(l,r);
      }
    };
  @SuppressWarnings("unchecked")
  public static List immutSortList(List a, Comparator c) {
    if(c == null)
      c = defaultComparator;
    final Object[] data = a.toArray();
    if (c != null)
      Arrays.parallelSort(data, 0, data.length, (Comparator)c);
    else
      Arrays.parallelSort(data, 0, data.length, c);
    return ArrayLists.toList(data, 0, data.length, RT.meta(a));
  }
  default List immutSort() { return immutSort(null); }  
  default List immutSort(Comparator c) {
    return immutSortList(this, c);
  }
}

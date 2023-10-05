package ham_fisted;


import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

public interface ICollectionDef<E> extends Collection<E> {
  default boolean add(E e) { throw new UnsupportedOperationException("Unimplemented"); }
  default boolean addAll(Collection<? extends E> c) { throw new UnsupportedOperationException("Unimplemented"); }
  default void clear() { throw new UnsupportedOperationException("Unimplemented"); }

  default boolean contains(Object o) {
    for(E e: this) {
      if(Objects.equals(e, o)) return true;
    }
    return false;
  }
  default boolean containsAll(Collection<?> c) { throw new UnsupportedOperationException("Unimplemented"); }

  default boolean isEmpty() { return iterator().hasNext() == false; }

  default boolean remove(Object o) { throw new UnsupportedOperationException("Unimplemented"); }

  default boolean removeAll(Collection<?> c) { throw new UnsupportedOperationException("Unimplemented"); }
  default boolean removeIf(Predicate<? super E> filter) { throw new UnsupportedOperationException("Unimplemented"); }
  default boolean retainAll(Collection<?> c) { throw new UnsupportedOperationException("Unimplemented"); }
  default Object[] toArray() { return ArrayLists.toArray(this); }
  default <T> T[] toArray(T[] d) {
    return ArrayLists.toArray(this, d);
  }
}

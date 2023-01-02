package ham_fisted;


import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.IFn;


public class NonEditableArrayMapBase<K,V> extends ArrayMapBase<K,V> {
  NonEditableArrayMapBase(ArrayMap ht) { super(ht); }
  public V remove(Object k) {
    throw new RuntimeException("Unimplemented");
  }
  public V put(K key, V value) {
    throw new RuntimeException("Unimplemented");
  }
  public void putAll(Map<? extends K,? extends V> m) {
    throw new RuntimeException("Unimplemented");
  }
  public void clear() {
    throw new RuntimeException("Unimplemented");
  }
  public V compute(K key, BiFunction<? super K,? super V,? extends V> bfn) {
    throw new RuntimeException("Unimplemented");
  }
  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    throw new RuntimeException("Unimplemented");
  }
  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    throw new RuntimeException("Unimplemented");
  }
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    throw new RuntimeException("Unimplemented");
  }
  public void replaceAll(BiFunction<? super K,? super V,? extends V> function) {
    throw new RuntimeException("Unimplemented");
  }
}

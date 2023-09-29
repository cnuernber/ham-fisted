package ham_fisted;

import java.util.Map;
import java.util.function.Function;
import java.util.function.BiFunction;
import clojure.lang.IPersistentMap;
import clojure.lang.IMapEntry;
import clojure.lang.MapEntry;


public class ROHashMap extends HashMap {
  ROHashMap(float loadFactor, int initialCapacity,
	     int length, HBNode[] data,
	     IPersistentMap meta) {
    super(loadFactor, initialCapacity, length, data, meta);
  }
  ROHashMap(HashMap other, IPersistentMap m) {
    super(other, m);
  }
  public Object remove(Object k) {
    throw new UnsupportedOperationException();
  }
  public Object put(Object key, Object value) {
    throw new UnsupportedOperationException();
  }
  public void putAll(Map m) {
    throw new UnsupportedOperationException();
  }
  public void clear() {
    throw new UnsupportedOperationException();
  }
  public Object compute(Object key, BiFunction bfn) {
    throw new UnsupportedOperationException();
  }
  public Object computeIfAbsent(Object key, Function mappingFunction) {
    throw new UnsupportedOperationException();
  }
  public Object computeIfPresent(Object key, BiFunction remappingFunction) {
    throw new UnsupportedOperationException();
  }
  public Object merge(Object key, Object value, BiFunction remappingFunction) {
    throw new UnsupportedOperationException();
  }
  public void replaceAll(BiFunction function) {
    throw new UnsupportedOperationException();
  }
}

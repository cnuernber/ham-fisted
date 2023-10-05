package ham_fisted;


import clojure.lang.IMapEntry;
import java.util.Map;

public interface IAMapEntry extends Map.Entry, IMutList, IMapEntry {
  default Object setValue(Object v) { throw new UnsupportedOperationException(); }
  default Object key() { return getKey(); }
  default Object val() { return getValue(); }
  default int size() { return 2; }
  default Object get(int idx) {
    if(idx == 0) return getKey();
    if(idx == 1) return getValue();
    throw new RuntimeException("Index out of range.");
  }
}

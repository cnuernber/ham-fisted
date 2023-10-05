package ham_fisted;

import java.util.Map;
import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;

interface IAPersistentMap extends Map, IEditableCollection, IPersistentMap {
  default int count() { return size(); }
  default IPersistentMap cons(Object v) { return (IPersistentMap)(asTransient().conj(v).persistent()); }
  default IPersistentMap assocEx(Object key, Object val) {
    if(containsKey(key))
      throw new RuntimeException("Object already contains key :" + String.valueOf(key));
    return assoc(key, val);
  }
  default IPersistentMap assoc(Object key, Object val) {
    return (IPersistentMap)(asTransient().assoc(key,val).persistent());
  }
  default IPersistentMap without(Object key) {
    return (IPersistentMap)(asTransient().without(key).persistent());
  }
  ITransientMap asTransient();
}

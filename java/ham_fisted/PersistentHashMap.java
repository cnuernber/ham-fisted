package ham_fisted;

import java.util.Map;
import clojure.lang.IEditableCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.IObj;
import clojure.lang.Indexed;

public class PersistentHashMap
  extends ROHashMap
  implements IAPersistentMap, IObj {
  public static final PersistentHashMap EMPTY = new PersistentHashMap(new HashMap());
  int _hasheq = 0;
  
  public PersistentHashMap(HashMap data) {
    super(data.loadFactor, data.capacity, data.length, data.data, data.meta);
  }
  public PersistentHashMap(HashMap data, IPersistentMap m) {
    super(data.loadFactor, data.capacity, data.length, data.data, m);
  }
  public int count() { return length; }
  public int hasheq() {
    if (_hasheq == 0)
      _hasheq = super.hasheq();
    return _hasheq;
  }
  public ITransientMap asTransient() {
    return isEmpty() ? new UnsharedHashMap(meta) : 
      new TransientHashMap(this);
  }
  public PersistentHashMap withMeta(IPersistentMap m) {
    return new PersistentHashMap(this, m);
  }
  public PersistentHashMap empty() { return EMPTY; }
}
				       

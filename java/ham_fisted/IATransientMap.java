package ham_fisted;


import java.util.Map;
import clojure.lang.Indexed;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;


public interface IATransientMap extends ITransientMap, ITransientAssociative2 {
  default ITransientMap conj(Object val) {
    Object k, v;
    if(val instanceof Indexed) {
      Indexed ii = (Indexed)val;
      k = ii.nth(0);
      v = ii.nth(1);
    } else if (val instanceof Map.Entry) {
      Map.Entry ii = (Map.Entry)val;
      k = ii.getKey();
      v = ii.getValue();
    } else {
      throw new RuntimeException("Value must be either indexed or map entry");
    }
    return assoc(k,v);
  }
}

package ham_fisted;


import java.util.Map;
import clojure.lang.Indexed;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IKVReduce;


public interface IATransientMap extends ITransientMap, ITransientAssociative2 {
  default ITransientMap conjVal(Object val) {
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
      throw new RuntimeException("Value must be either indexed or map entry :" + String.valueOf(val != null ? val.getClass() : "null"));
    }
    return assoc(k,v);
  }
  default ITransientMap conj(Object val) {
    if(val instanceof Map) {
      if(val instanceof IKVReduce) {
	return (IATransientMap)
	  ((IKVReduce)val).kvreduce(new IFnDef() {
	      public Object invoke(Object acc, Object k, Object v) {
		return ((ITransientAssociative2)acc).assoc(k,v);
	      }
	    }, this);
      } else {
	ITransientMap m = this;
	for(Object o: ((Map)val).entrySet()) {
	  Map.Entry lf = (Map.Entry)o;
	  m = (ITransientMap)((ITransientAssociative2)m).assoc(lf.getKey(), lf.getValue());
	}
	return m;
      }
    } else {
      return conjVal(val);
    }
  }
}

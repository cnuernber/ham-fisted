package ham_fisted;

import clojure.lang.Util;
import clojure.lang.IHashEq;


public class HashProviders {
  public static final HashProvider equalHashProvider = new HashProvider(){};
  /**
   * Hashcode provider using Clojure's hasheq/equiv pathway
   */
  public static final HashProvider equivHashProvider = new HashProvider() {
      public int hash(Object obj) {
	return Util.hasheq(obj);
      }
      public boolean equals(Object lhs, Object rhs) {
	return CljHash.equiv(lhs,rhs);
      }
    };

  //Equiv-pathway with small optimizations.
  public static final HashProvider hybridHashProvider = new HashProvider() {
      public int hash(Object k) {
	return	  
	  k == null ? 0 :
	  k instanceof IHashEq ? ((IHashEq)k).hasheq() :
	  IntegerOps.mixhash(k.hashCode());
      }
      public boolean equals(Object lhs, Object rhs) {
	return
	  lhs == rhs ? true :
	  lhs == null || rhs == null ? false :
	  CljHash.nonNullEquiv(lhs,rhs);
      }
    };

  public static final HashProvider defaultHashProvider = hybridHashProvider;
}

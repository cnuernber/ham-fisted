package ham_fisted;


import static ham_fisted.BitmapTrie.*;
import static ham_fisted.BitmapTrieCommon.*;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.RandomAccess;
import java.util.Collection;
import clojure.lang.Murmur3;
import clojure.lang.APersistentMap;
import clojure.lang.Util;
import clojure.lang.RT;
import clojure.lang.Numbers;
import clojure.lang.IPersistentCollection;
import clojure.lang.Reduced;


public class CljHash {

  public static int mapHashcode(Map data) {
    return Murmur3.hashUnordered(data.entrySet());
  }
  public static int mapHashcode(BitmapTrie data) {
    return Murmur3.hashUnordered(data.entrySet((Map.Entry<Object,Object>)null, false));
  }
  public static int setHashcode(Set data) {
    return Murmur3.hashUnordered(data);
  }
  public static int setHashcode(BitmapTrie data) {
    return Murmur3.hashUnordered(data.keySet((Object)null, false));
  }

  public static boolean equiv(Object k1, Object k2) {
    if(k1 == k2)
      return true;
    //Somewhat faster version of equiv *if* both are longs or both are doubles.
    //which happens to be a very common case in Clojure.
    if(k1 != null) {
      if( k1 instanceof Long && k2 instanceof Long)
	return (long)k1 == (long)k2;

      if ( k1 instanceof Double && k2 instanceof Double)
	return (double)k1 == (double)k2;

      if(k1 instanceof Number && k2 instanceof Number)
	return Numbers.equal((Number)k1, (Number)k2);

      if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
	return Util.pcequiv(k1,k2);

      return k1.equals(k2);
    }
    return false;
  }

  public static boolean mapEquiv(BitmapTrie data, Object rhs) {
    BitmapTrie rhsBm = null;
    if (rhs instanceof BitmapTrieOwner) {
       rhsBm = ((BitmapTrieOwner)rhs).bitmapTrie();
    } else if (rhs instanceof BitmapTrie) {
      rhsBm = (BitmapTrie)rhs;
    }
    if (rhsBm != null) {

      if(data.size() != rhsBm.size()) return false;

      LeafNodeIterator iter = rhsBm.iterator(identityIterFn);
      while(iter.hasNext()) {
	ILeaf rlf = iter.nextLeaf();
	LeafNode llf = data.getNode(rlf.key());
	if (llf == null || equiv(llf.v, rlf.val()) == false)
	  return false;
      }
      return true;
    } else if (rhs instanceof Map) {
      Map rhsMap = (Map)rhs;
      if (data.size() != rhsMap.size()) return false;
      for(Object obj: rhsMap.entrySet()) {
	Map.Entry me = (Map.Entry)obj;
	LeafNode llf = data.getNode(me.getKey());
	if (llf == null || equiv(llf.val(), me.getValue()) == false)
	  return false;
      }
      return true;
    }
    return false;
  }

  public static boolean mapEquiv(Map lhs, Object rhs) {
    if(rhs instanceof Map) {
      final Map rm = (Map)rhs;
      if(lhs.size() == rm.size()) {
	return (Boolean)Reductions.serialReduction(new IFnDef() {
	    public Object invoke(Object acc, Object v) {
	      Map.Entry me = (Map.Entry)v;
	      if(!equiv(lhs.get(me.getKey()), me.getValue()))
		return new Reduced(false);
	      return acc;
	    }
	  }, true, rhs);
      }
    }
    return false;
  }

  public static boolean setEquiv(BitmapTrie data, Object rhs) {
    BitmapTrie rhsBm = null;
    if (rhs instanceof BitmapTrieOwner) {
       rhsBm = ((BitmapTrieOwner)rhs).bitmapTrie();
    } else if (rhs instanceof BitmapTrie) {
      rhsBm = (BitmapTrie)rhs;
    }

    if (rhsBm != null) {
      if(data.size() != rhsBm.size()) return false;

      LeafNodeIterator iter = rhsBm.iterator(identityIterFn);
      while(iter.hasNext()) {
	ILeaf rlf = iter.nextLeaf();
	LeafNode llf = data.getNode(rlf.key());
	if (llf == null)
	  return false;
      }
      return true;
    } else if (rhs instanceof Set) {
      Set rhsMap = (Set)rhs;
      if (data.size() != rhsMap.size()) return false;

      for(Object obj: rhsMap) {
	LeafNode llf = data.getNode(obj);
	if (llf == null)
	  return false;
      }
      return true;
    }
    return false;
  }

  public static int listHasheq(List l) {
    int hash = 1;
    final int n = l.size();
    for(int idx = 0; idx < n; ++idx) {
      hash = 31 * hash + Util.hasheq(l.get(idx));
    }
    hash = Murmur3.mixCollHash(hash, n);
    return hash;
  }
  public static boolean listEquiv(List l, Object rhs) {
    if (l == rhs) return true;
    if (rhs == null) return false;
    if (rhs instanceof RandomAccess) {
      List r = (List)rhs;
      final int sz = l.size();
      if(sz != r.size()) return false;
      for(int idx = 0; idx < sz; ++idx) {
	if(!equiv(l.get(idx), r.get(idx)))
	  return false;
      }
      return true;
    } else if ( rhs instanceof Iterable) {
      Collection r = rhs instanceof Collection ? (Collection)rhs : (Collection)RT.seq(rhs);
      Iterator iter = r.iterator();
      final int sz = l.size();
      int idx;
      for(idx = 0; idx < sz && iter.hasNext(); ++idx) {
	if(!equiv(l.get(idx), iter.next()))
	  return false;
      }
      return idx != sz || iter.hasNext() ? false : true;
    }
    return false;
  }
}

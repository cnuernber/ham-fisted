package ham_fisted;


import static ham_fisted.BitmapTrie.*;
import static ham_fisted.BitmapTrieCommon.*;
import java.util.Map;
import java.util.Set;
import clojure.lang.Murmur3;
import clojure.lang.APersistentMap;
import clojure.lang.Util;


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
	if (llf == null || Util.equiv(llf.v, rlf.val()) == false)
	  return false;
      }
      return true;
    } else if (rhs instanceof Map) {
      Map rhsMap = (Map)rhs;
      if (data.size() != rhsMap.size()) return false;
      for(Object obj: rhsMap.entrySet()) {
	Map.Entry me = (Map.Entry)obj;
	LeafNode llf = data.getNode(me.getKey());
	if (llf == null || Util.equiv(llf.val(), me.getValue()) == false)
	  return false;
      }
      return true;
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
}

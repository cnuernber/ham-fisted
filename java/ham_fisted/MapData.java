package ham_fisted;

import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.Iterator;
import java.util.Spliterator;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;

import static ham_fisted.BitmapTrieCommon.*;

public interface MapData {
  int size();
  boolean isEmpty();
  MapData clone();
  MapData shallowClone();
  IPersistentMap meta();
  MapData withMeta(IPersistentMap m);
  ILeaf getOrCreate(Object k);
  ILeaf getNode(Object k);
  void clear();
  void remove(Object k, Box b);
  MapData mutAssoc(Object k, Object v);
  MapData mutDissoc(Object k);
  //Leaf nodes are immutable here, but the this object, the structure
  //of the hashtable is mutable.
  MapData mutUpdateValue(Object k, IFn fn);
  //Leaf nodes are immutable here, but the this object, the structure
  //of the hashtable is mutable.
  void mutUpdateValues(BiFunction bfn);
  Object reduce(Function<ILeaf,Object> lf, IFn rfn, Object acc);
  Iterator iterator(Function<ILeaf,Object> lf);
  Spliterator spliterator(Function<ILeaf,Object> lf);
  HashProvider hashProvider();


  default Object get(Object k) {
    ILeaf n = getNode(k);
    return n != null ? n.val() : null;
  }
  default Object getOrDefault(Object k, Object v) {
    ILeaf n = getNode(k);
    return n != null ? n.val() : v;
  }
  default boolean containsKey(Object k) {
    return getNode(k) != null;
  }
  default Object put(Object k, Object v) {
    return getOrCreate(k).val(v);
  }

  //These are slow general versions.
  @SuppressWarnings("unchecked")
  default Object compute(Object key, BiFunction bfn) {
    int startc = size();
    ILeaf node = getOrCreate(key);
    Object newv = null;
    try {
      newv = bfn.apply(key, node.val());
      if(newv == null)
	remove(key, null);
      else
	node.val(newv);
      return newv;
    } catch(Exception e) {
      if (startc != size())
	remove(key, null);
      throw e;
    }
  }
  //These are slow general versions.
  @SuppressWarnings("unchecked")
  default Object computeIfAbsent(Object key, Function bfn) {
    int startc = size();
    ILeaf node = getOrCreate(key);
    try {
      Object newv = node.val();
      if(newv != null)
	return newv;
      newv = bfn.apply(key);
      if(newv == null)
	remove(key, null);
      node.val(newv);
      return newv;
    } catch(Exception e) {
      if (startc != size())
	remove(key, null);
      throw e;
    }
  }
  @SuppressWarnings("unchecked")
  default Object merge(Object key, Object value, BiFunction bfn) {
    if (value == null || bfn == null)
      throw new NullPointerException("Neither value nor remapping function may be null");
    int startc = size();
    ILeaf lf = getOrCreate(key);
    Object valval = lf.val();
    try {
      Object newv = valval == null ? value : bfn.apply(valval, value);
      if(newv == null) {
	remove(key, null);
      } else {
	lf.val(newv);
      }
      return newv;
    } catch(Exception e) {
      if(startc != size())
	remove(key, null);
      throw e;
    }
  }
};

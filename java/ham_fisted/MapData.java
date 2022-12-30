package ham_fisted;

import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.Iterator;
import java.util.Spliterator;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;

import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;

public interface MapData {
  int size();
  boolean isEmpty();
  MapData clone();
  MapData shallowClone();
  IPersistentMap meta();
  MapData withMeta(IPersistentMap m);
  LeafNode getOrCreate(Object k);
  LeafNode getNode(Object k);
  void clear();
  void remove(Object k, Box b);
  void mutAssoc(Object k, Object v);
  void mutDissoc(Object k);
  void mutUpdateValue(Object k, IFn fn);
  void mutUpdateValues(BiFunction bfn);
  Object reduce(Function<ILeaf,Object> lf, IFn rfn, Object acc);
  Iterator iterator(Function<ILeaf,Object> lf);
  Spliterator spliterator(Function<ILeaf,Object> lf);
  HashProvider hashProvider();
};

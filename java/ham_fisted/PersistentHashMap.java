package ham_fisted;

import static ham_fisted.IntegerOps.*;
import static ham_fisted.BitmapTrie.*;
import static ham_fisted.BitmapTrieCommon.*;
import clojure.lang.APersistentMap;
import clojure.lang.Util;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentMap;
import clojure.lang.ITransientMap;
import clojure.lang.IPersistentCollection;
import clojure.lang.IteratorSeq;
import clojure.lang.IEditableCollection;
import clojure.lang.IMapIterable;
import clojure.lang.ISeq;
import clojure.lang.IObj;
import clojure.lang.IKVReduce;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.lang.IHashEq;
import clojure.lang.Numbers;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.util.Objects;
import java.util.Map;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.math.BigDecimal;


public final class PersistentHashMap
  extends APersistentMap
  implements IObj, IMapIterable, IKVReduce, IEditableCollection,
	     MapSet, BitmapTrieOwner, IHashEq {

  final BitmapTrie hb;
  int cachedHash = 0;

  public BitmapTrie bitmapTrie() { return hb; }

  public PersistentHashMap() {
    hb = new BitmapTrie(equivHashProvider);
  }
  public PersistentHashMap(HashProvider hp) {
    hb = new BitmapTrie(hp);
  }
  public PersistentHashMap(BitmapTrie hm) {
    hb = hm;
  }

  public PersistentHashMap(HashProvider _hp, boolean assoc, Object... kvs) {
    HashMap<Object,Object> hm = new HashMap<Object,Object>(_hp, assoc, kvs);
    hb = hm.hb;
  }

  public PersistentHashMap(Object... kvs) {
    this(equivHashProvider, false, kvs);
  }
  public final BitmapTrie unsafeGetBitmapTrie() { return hb; }
  public PersistentHashMap(boolean assoc, Object... kvs) {
    this(equivHashProvider, assoc, kvs);
  }

  public final int hashCode() {
    if (cachedHash == 0) {
      return cachedHash = CljHash.mapHashcode(hb);
    }
    return cachedHash;
  }

  public final int hasheq() {
    return hashCode();
  }

  public final boolean equals(Object o) {
    return CljHash.mapEquiv(hb, o);
  }
  public final boolean equiv(Object o) {
    return equals(o);
  }
  public final boolean containsKey(Object key) {
    return hb.containsKey(key);
  }
  public final boolean containsValue(Object v) {
    return hb.containsValue(v);
  }
  public final int size() { return hb.size(); }
  public final int count() { return hb.size(); }
  public final Set keySet() { return new PersistentHashSet(hb); }
  public final Set entrySet() { return hb.entrySet((Map.Entry<Object,Object>)null, false); }
  public final Collection values() { return hb.values((Object)null, false); }
  public final IMapEntry entryAt(Object key) {
    final LeafNode node = hb.getNode(key);
    return node != null ? MapEntry.create(key, node.val()) : null;
  }
  public final ISeq seq() { return IteratorSeq.create(iterator()); }
  @Override
  public final Object get(Object key) {
    return hb.get(key);
  }
  @Override
  public final Object getOrDefault(Object key, Object defval) {
    return hb.getOrDefault(key, defval);
  }
  public final Object valAt(Object key, Object notFound) {
    return hb.getOrDefault(key, notFound);
  }
  public final Object valAt(Object key){
    return hb.getOrDefault(key, null);
  }
  public final Iterator iterator(){
    return hb.iterator(entryIterFn);
  }

  public final Iterator keyIterator(){
    return hb.iterator(keyIterFn);
  }

  public final Iterator valIterator() {
    return hb.iterator(valIterFn);
  }

  public final PersistentHashMap[] splitMaps(int nsplits) {
    BitmapTrie[] bases = hb.splitBases(nsplits);
    PersistentHashMap[] retval = new PersistentHashMap[bases.length];
    for (int idx = 0; idx < retval.length; ++idx)
      retval[idx] = new PersistentHashMap(bases[idx]);
    return retval;
  }

  public final Iterator[] splitKeys(int nsplits ) {
    return hb.splitKeys(nsplits);
  }
  public final Iterator[] splitValues(int nsplits ) {
    return hb.splitValues(nsplits);
  }
  public final Iterator[] splitEntries(int nsplits ) {
    return hb.splitEntries(nsplits);
  }

  public final IPersistentMap assoc(Object key, Object val) {
    if (hb.size() == 0) {
      return new PersistentHashMap(new BitmapTrie(hb.hp, hb.meta, key, val));
    } else {
      return new PersistentHashMap(hb.shallowClone().assoc(key, val));
    }
  }
  public final IPersistentMap assocEx(Object key, Object val) {
    if(containsKey(key))
      throw new RuntimeException("Key already present");
    return assoc(key, val);
  }
  public final IPersistentMap without(Object key) {
    if (hb.size() == 0 || (key == null && hb.nullEntry == null))
      return this;
    return new PersistentHashMap(hb.shallowClone().dissoc(key));
  }

  public static PersistentHashMap EMPTY = new PersistentHashMap(new BitmapTrie(equalHashProvider));

  public final IPersistentCollection empty() {
    return (IPersistentCollection)EMPTY.withMeta(hb.meta);
  }
  public final IPersistentMap meta() { return hb.meta; }
  public final IObj withMeta(IPersistentMap newMeta) {
    return new PersistentHashMap(hb.shallowClone(newMeta));
  }
  public final Object kvreduce(IFn f, Object init) {
    LeafNodeIterator iter = hb.iterator(hb.identityIterFn);
    while(iter.hasNext()) {
      ILeaf elem = iter.nextLeaf();
      init = f.invoke(init, elem.key(), elem.val());
      if (RT.isReduced(init))
	return ((IDeref)init).deref();
    }
    return init;
  }
  public final ITransientMap asTransient() {
    if (size() == 0)
      return new HashMap(new BitmapTrie(hb.hp, hb.meta));
    else
      return new TransientHashMap(hb.shallowClone());
  }
  public void forEach(BiConsumer action) {
    hb.forEach(action);
  }
  public void parallelForEach(BiConsumer action, ExecutorService es,
			      int parallelism) throws Exception {
    hb.parallelForEach(action, es, parallelism);
  }
  public void parallelForEach(BiConsumer action) throws Exception {
    hb.parallelForEach(action);
  }

  public final PersistentHashMap union(MapSet other, BiFunction remappingFunction) {
    return new PersistentHashMap(hb.union(((BitmapTrieOwner)other).bitmapTrie(),
					  remappingFunction));
  }
  public final PersistentHashMap difference(MapSet other) {
    return new PersistentHashMap(hb.difference(((BitmapTrieOwner)other).bitmapTrie()));
  }
  public final PersistentHashMap intersection(MapSet other, BiFunction remappingFunction) {
    return new PersistentHashMap(hb.intersection(((BitmapTrieOwner)other).bitmapTrie(),
						 remappingFunction));
  }
  public final PersistentHashMap immutUpdateValues(BiFunction bfn) {
    return new PersistentHashMap(hb.immutUpdate(bfn));
  }
  public final PersistentHashMap immutUpdateValue(Object key, Function fn) {
    return new PersistentHashMap(hb.immutUpdate(key, fn));
  }
  public <K,V> HashMap<K,V> unsafeAsHashMap(K kTypeMarker, V vTypeMarker) {
    return new HashMap<K,V>(hb, true);
  }
  public <K,V> HashMap<K,V> copyToHashMap(K kTypeMarker, V vTypeMarker) {
    return new HashMap<K,V>(hb);
  }

  final static BitmapTrie unpackFromObject(Object obj) {
    if (obj instanceof BitmapTrie)
      return (BitmapTrie) obj;
    if (obj instanceof BitmapTrieOwner)
      return ((BitmapTrieOwner)obj).bitmapTrie();
    throw new RuntimeException("Cannot get bitmap trie from object: " + String.valueOf(obj));
  }

  @SuppressWarnings("unchecked")
  public final static PersistentHashMap unionReduce(BiFunction valueMapper, Iterable bitmaps) {
    Iterator bmIter = bitmaps.iterator();
    if (bmIter.hasNext() == false)
      return null;
    BitmapTrie retval = unpackFromObject(bmIter.next());
    if (bmIter.hasNext() == false)
      return new PersistentHashMap(retval);
    retval = retval.shallowClone(null);
    while(bmIter.hasNext())
      retval = retval.union(unpackFromObject(bmIter.next()), valueMapper, true);
    return new PersistentHashMap(retval);
  }

  @SuppressWarnings("unchecked")
  public final static PersistentHashMap parallelUnionReduce(BiFunction valueMapper, Iterable bitmaps,
							    ExecutorService executor, int parallelism)
    throws Exception {

    if((ForkJoinTask.inForkJoinPool()
	&& executor == ForkJoinPool.commonPool())
       || parallelism == 1) {
      return unionReduce(valueMapper, bitmaps);
    }

    ArrayList<BitmapTrie> bms = new ArrayList();
    for(Object obj : bitmaps) {
      bms.add((obj instanceof BitmapTrie ? (BitmapTrie)obj :
	       ((BitmapTrieOwner)obj).bitmapTrie()));
    }
    int nbms = bms.size();
    if (nbms == 0)
      return null;
    if (nbms == 1)
      return new PersistentHashMap(bms.get(0));
    final BitmapTrie initial = bms.get(0);
    //We can't currently split up maps in more than 1024 elems.
    parallelism = Math.min(1024, parallelism);
    final int nsplits = parallelism;
    Future[] futures = new Future[parallelism];
    for (int idx = 0; idx < parallelism; ++idx) {
      final int splitidx = idx;
      futures[idx] = executor.submit(new Callable<BitmapTrie>() {
	  public BitmapTrie call() {
	    BitmapTrie target = initial.keyspaceSplit(splitidx, nsplits).shallowClone(null);
	    for( int bidx = 1; bidx < nbms; ++bidx) {
	      target = target.union(bms.get(bidx).keyspaceSplit(splitidx, nsplits),
				    valueMapper,
				    true);
	    }
	    //Force traversal to calculate map size in the work thread.
	    target.size();
	    return target;
	  }
	});
    }
    //Second loop is hyper fast because we know the keys can't collide meaning the 'union'
    //operation really is in-place with no merging and full structural sharing.
    BitmapTrie result = (BitmapTrie)futures[0].get();
    int sum = result.size();
    for (int idx = 1; idx < parallelism; ++idx ) {
      BitmapTrie nextTrie = (BitmapTrie)futures[idx].get();
      sum += nextTrie.size();
      result = result.union(nextTrie, valueMapper);
    }
    result.count = sum;
    return new PersistentHashMap(result);
  }

  public final static PersistentHashMap parallelUnionReduce(BiFunction valueMapper, Iterable bitmaps)
    throws Exception {
    return parallelUnionReduce(valueMapper, bitmaps, ForkJoinPool.commonPool(),
			       ForkJoinPool.getCommonPoolParallelism());
  }

  public void printNodes() { hb.printNodes(); }

  public Function<Object[],PersistentHashMap> makeFactory(Object[] keys) {
    Function<Object[], BitmapTrie> srcFn = BitmapTrie.makeFactory(hb.hp, keys);
    return new Function<Object[], PersistentHashMap>() {
      public PersistentHashMap apply(Object[] values) {
	return new PersistentHashMap(srcFn.apply(values));
      }
    };
  }
}

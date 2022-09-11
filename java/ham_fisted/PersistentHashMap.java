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
import clojure.lang.MapEquivalence;
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
	     MapSet, BitmapTrieOwner, IHashEq, ImmutValues, MapEquivalence {

  final BitmapTrie hb;
  int cachedHash = 0;

  public BitmapTrie bitmapTrie() { return hb; }

  public PersistentHashMap() {
    hb = new BitmapTrie(defaultHashProvider);
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
    this(defaultHashProvider, false, kvs);
  }
  public final BitmapTrie unsafeGetBitmapTrie() { return hb; }
  public PersistentHashMap(boolean assoc, Object... kvs) {
    this(defaultHashProvider, assoc, kvs);
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

  public static PersistentHashMap EMPTY = new PersistentHashMap(new BitmapTrie(defaultHashProvider));

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
  public final PersistentHashMap immutUpdateValue(Object key, IFn fn) {
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


  static final void ensureDifferent(HashProvider hp, Object[] keys) {
    final int nk = keys.length;
    for(int i = 0; i < nk; ++i) {
      for(int j = i+1; j < nk; ++j) {
	if (hp.equals(keys[i], keys[j]))
	  throw new RuntimeException("Duplicate keys provided: " + String.valueOf(keys[i]));
      }
    }
  }

  public static final Function<Object[],IPersistentMap> makeFactory(HashProvider hp, Object[] keys) {
    switch(keys.length) {
    case 0:
      final PersistentArrayMap zeroCase = new PersistentArrayMap(hp);
      return objs -> zeroCase;
    case 1: return objs -> new PersistentArrayMap(hp, keys[0], objs[0], null);
    case 2:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1], null);
    case 3:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1],
					    keys[2], objs[2], null);
    case 4:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1],
					    keys[2], objs[2], keys[3], objs[3], null);
    case 5:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1],
					    keys[2], objs[2], keys[3], objs[3],
					    keys[4], objs[4], null);
    case 6:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1],
					    keys[2], objs[2], keys[3], objs[3],
					    keys[4], objs[4], keys[5], objs[5], null);
    case 7:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1],
					    keys[2], objs[2], keys[3], objs[3],
					    keys[4], objs[4], keys[5], objs[5],
					    keys[6], objs[6], null);
    case 8:
      ensureDifferent(hp, keys);
      return objs -> new PersistentArrayMap(hp, keys[0], objs[0], keys[1], objs[1],
					    keys[2], objs[2], keys[3], objs[3],
					    keys[4], objs[4], keys[5], objs[5],
					    keys[6], objs[6], keys[7], objs[7], null);
    default:
      final Function<Object[], BitmapTrie> srcFn = BitmapTrie.makeFactory(hp, keys);
      return objs -> new PersistentHashMap(srcFn.apply(objs));
    }
  }

  public static final IPersistentMap create(HashProvider hp,
					    boolean errorOnDuplicate,
					    Object... kvs) {
    if (kvs == null || kvs.length == 0)
      return new PersistentArrayMap(hp);
    final int nelems = kvs.length;
    if ((nelems % 2) != 0 )
      throw new RuntimeException("Number of elements not divisible by 2");
    final int nentries = nelems / 2;
    return createN(hp, errorOnDuplicate, nentries, kvs);
  }

  public static final IPersistentMap createN(HashProvider hp,
					     boolean errorOnDuplicate,
					     int nentries,
					     Object[] kvs) {
    if (nentries == 0)
      return new PersistentArrayMap(hp);
    IPersistentMap retval = null;
    switch(nentries) {
    case 1: retval = new PersistentArrayMap(hp, kvs[0], kvs[1], null);
      break;
    case 2:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3], null);
      break;
    case 3:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2], kvs[4]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3],
					kvs[4], kvs[5], null);
      break;
    case 4:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2], kvs[4], kvs[6]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3],
					kvs[4], kvs[5], kvs[6], kvs[7], null);
      break;
    case 5:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2], kvs[4], kvs[6], kvs[8]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3],
					kvs[4], kvs[5], kvs[6], kvs[7],
					kvs[8], kvs[9], null);
      break;
    case 6:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2], kvs[4], kvs[6], kvs[8], kvs[10]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3],
					kvs[4], kvs[5], kvs[6], kvs[7],
					kvs[8], kvs[9], kvs[10], kvs[11], null);
      break;
    case 7:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2], kvs[4], kvs[6], kvs[8], kvs[10], kvs[12]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3],
					kvs[4], kvs[5], kvs[6], kvs[7],
					kvs[8], kvs[9], kvs[10], kvs[11],
					kvs[12], kvs[13], null);
      break;
    case 8:
      if (PersistentArrayMap.different(hp, kvs[0], kvs[2], kvs[4], kvs[6], kvs[8], kvs[10], kvs[12], kvs[14]))
	retval = new PersistentArrayMap(hp, kvs[0], kvs[1], kvs[2], kvs[3],
					kvs[4], kvs[5], kvs[6], kvs[7],
					kvs[8], kvs[9], kvs[10], kvs[11],
					kvs[12], kvs[13], kvs[14], kvs[15], null);
      break;
    }
    if (retval == null) {
      HashMap<Object,Object> hm = new HashMap<Object,Object>(hp);
      final int nelems = nentries * 2;
      for (int idx = 0; idx < nelems; idx += 2)
	hm.put(kvs[idx], kvs[idx+1]);
      retval = hm.persistent();
    }
    if (errorOnDuplicate && retval.count() != nentries) {
      HashSet<Object> keySet = new HashSet<Object>(hp);
      ArrayList<Object> dupKeys = new ArrayList<Object>();
      for(int idx = 0; idx < nentries; ++idx) {
	final Object k = kvs[idx*2];
	if(keySet.contains(k))
	  dupKeys.add(k);
	keySet.add(k);
      }
      throw new RuntimeException("Map contains duplicate keys: " + String.valueOf(dupKeys));
    }
    return retval;
  }
}

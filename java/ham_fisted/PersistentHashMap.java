package ham_fisted;

import static ham_fisted.IntBitmap.*;
import static ham_fisted.HashBase.*;
import static ham_fisted.HAMTCommon.*;
import clojure.lang.APersistentMap;
import clojure.lang.Util;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentMap;
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
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.concurrent.ExecutorService;
import java.math.BigDecimal;


public final class PersistentHashMap
  extends APersistentMap
  implements IObj, IMapIterable, IKVReduce, IEditableCollection {

  final HashBase hb;

  public static final HashProvider equivHashProvider = new HashProvider(){
      public int hash(Object obj) {
	//Faster (slightly) implementation of hashcode work.
	if (obj == null) return 0;
	if (obj instanceof IHashEq)
	  return ((IHashEq)obj).hasheq();

	if (obj instanceof Long ||
	    obj instanceof Integer ||
	    obj instanceof Short ||
	    obj instanceof Byte)
	  return mixhash(obj.hashCode());
	if (obj instanceof Double)
	  return obj.equals(-0.0) ? 0 : mixhash(obj.hashCode());
	if (obj instanceof Float)
	  return obj.equals(-0.0F) ? 0 : mixhash(obj.hashCode());
	if (obj instanceof Number)
	  return Util.hasheq(obj);
	return mixhash(obj);
      }
      public boolean equals(Object lhs, Object rhs) {
	return Util.equiv(lhs,rhs);
      }
    };

  public PersistentHashMap() {
    hb = new HashBase(equivHashProvider);
  }
  public PersistentHashMap(HashBase hm) {
    hb = hm;
  }
  public PersistentHashMap(HashProvider _hp, boolean assoc, Object... kvs) {
    HashMap<Object,Object> hm = new HashMap<Object,Object>(_hp);
    final int nkvs = kvs.length;
    if (0 != (nkvs % 2))
      throw new RuntimeException("Uneven number of keyvals");
    final int nks = nkvs / 2;
    for (int idx = 0; idx < nks; ++idx) {
      final int kidx = idx * 2;
      final int vidx = kidx + 1;
      hm.put(kvs[kidx], kvs[vidx]);
    }
    if (assoc == false && hm.size() != nks)
      throw new RuntimeException("Duplicate key detected: " + String.valueOf(kvs));
    hb = hm.hb;
  }
  public final HashBase unsafeGetHashBase() { return hb; }
  public PersistentHashMap(boolean assoc, Object... kvs) {
    this(equivHashProvider, assoc, kvs);
  }
  public final boolean containsKey(Object key) {
    return hb.containsKey(key);
  }
  public final boolean containsValue(Object v) {
    return hb.containsValue(v);
  }
  public final int size() { return hb.size(); }
  public final int count() { return hb.size(); }
  public final Set keySet() { return hb.keySet((Object)null, false); }
  public final Set entrySet() { return hb.entrySet((Map.Entry<Object,Object>)null, false); }
  public final Collection values() { return hb.values((Object)null, false); }
  public final IMapEntry entryAt(Object key) {
    final LeafNode node = hb.getNode(key);
    return node != null ? MapEntry.create(key, node.val()) : null;
  }
  public final ISeq seq() { return  IteratorSeq.create(iterator()); }
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
    return new PersistentHashMap(hb.shallowClone().assoc(key, val));
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
  public static PersistentHashMap EMPTY = new PersistentHashMap(new HashBase(equivHashProvider));
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
  public final TransientHashMap asTransient() {
    return new TransientHashMap(hb.shallowClone());
  }
  @Override
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
  public <K,V> HashMap<K,V> unsafeAsHashMap(K kTypeMarker, V vTypeMarker) {
    return new HashMap<K,V>(hb, true);
  }
  public <K,V> HashMap<K,V> copyToHashMap(K kTypeMarker, V vTypeMarker) {
    return new HashMap<K,V>(hb);
  }

  public void printNodes() { hb.printNodes(); }


}

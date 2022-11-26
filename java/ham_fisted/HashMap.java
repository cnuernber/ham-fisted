package ham_fisted;

import static ham_fisted.IntegerOps.*;
import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentVector;
import clojure.lang.IObj;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentCollection;
import clojure.lang.IteratorSeq;
import clojure.lang.ISeq;
import clojure.lang.ILookup;
import clojure.lang.IHashEq;
import clojure.lang.IFn;
import clojure.lang.IReduce;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.concurrent.ExecutorService;


/**
 * Implementation of the java.util.Map interface backed by a bitmap
 * trie.
 */
public final class HashMap<K,V>
  implements Map<K,V>, ITransientMap, ITransientAssociative2, IObj,
	     MapSet, BitmapTrieOwner, ILookup, IFnDef, IHashEq,
	     ImmutValues, ITypedReduce, IReduce
{

  final BitmapTrie hb;
  boolean editable = true;

  public BitmapTrie bitmapTrie() { return hb; }
  public HashMap() {
    hb = new BitmapTrie();
  }

  public HashMap(HashProvider _hp) {
    hb = new BitmapTrie(_hp);
  }

  public HashMap(HashProvider _hp, IPersistentMap meta) {
    hb = new BitmapTrie(_hp, meta);
  }
  public HashMap(HashProvider _hp, IPersistentMap meta, int nkeys) {
    hb = new BitmapTrie(_hp, meta, nkeys);
  }

  public HashMap(HashProvider _hp, boolean assoc, Object... kvs) {
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

  //Unsafe construction without clone.
  HashMap(HashMap other, boolean marker) {
    hb = new BitmapTrie(other.hb, marker);
  }
  HashMap(BitmapTrie other, boolean marker) {
    hb = new BitmapTrie(other, marker);
  }
  //Safe construction with deep clone.
  public HashMap(Map<K,V> other) {
    if (other instanceof HashMap) {
      hb = new BitmapTrie(((HashMap)other).hb);
    } else {
      hb = new BitmapTrie();
      for(Map.Entry<K,V> entry : other.entrySet())
	put(entry.getKey(), entry.getValue());
    }
  }

  //Safe construction with deep clone.
  HashMap(BitmapTrie bt) {
    hb = new BitmapTrie(bt);
  }

  public final int hashCode() {
    return CljHash.mapHashcode(hb);
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

  public HashMap<K,V> clone() {
    return new HashMap<K,V>(this.hb);
  }

  public void clear() {
    hb.clear();
  }

  @SuppressWarnings("unchecked")
  final V applyMapping(K key, LeafNode node, Object val) {
    if(val == null)
      hb.remove(key);
    else
      node.val(val);
    return (V)val;
  }

  public V compute(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    int startc = hb.size();
    LeafNode node = hb.getOrCreate(key);
    try {
      @SuppressWarnings("unchecked") V valval = (V)node.val();
      return applyMapping(key, node, remappingFunction.apply(key, valval));
    } catch(Exception e) {
      if (startc != hb.size())
	hb.remove(key);
      throw e;
    }
  }

  @SuppressWarnings("unchecked")
  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    int startc = hb.size();
    LeafNode node = hb.getOrCreate(key);
    try {
      return applyMapping(key, node, node.val() == null ? mappingFunction.apply(key) : (V)node.val());
    } catch(Exception e) {
      if (startc != hb.size())
	remove(key);
      throw e;
    }
  }

  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    LeafNode node = hb.getNode(key);
    if (node == null || node.val() == null)
      return null;
    @SuppressWarnings("unchecked") V valval = (V)node.val();
    return applyMapping(key,node, remappingFunction.apply(key, valval));
  }

  Set<Map.Entry<K,V>> cachedSet = null;
  public Set<Map.Entry<K,V>> entrySet() {
    if (cachedSet == null)
      cachedSet = hb.new EntrySet<K,V>(true);
    return cachedSet;
  }
  public boolean containsKey(Object k) {
    return hb.containsKey(k);
  }
  public boolean containsValue(Object v) {
    return hb.containsValue(v);
  }

  public void forEach(BiConsumer<? super K,? super V> action) {
    hb.forEach(action);
  }

  @SuppressWarnings("unchecked")
  public V getOrDefault(Object key, V defaultValue) {
    return (V)hb.getOrDefault(key, defaultValue);
  }

  @SuppressWarnings("unchecked")
  public V get(Object k) {
    return (V)hb.get(k);
  }
  public ISeq seq() {
    return IteratorSeq.create(entrySet().iterator());
  }
  public boolean isEmpty() { return hb.size() == 0; }

  @SuppressWarnings("unchecked")
  public Set<K> keySet() { return (Set<K>)new PersistentHashSet(hb); }


  /**
   * If the specified key is not already associated with a value or is associated
   * with null, associates it with the given non-null value. Otherwise, replaces the
   * associated value with the results of the given remapping function, or removes
   * if the result is null.
   */
  @SuppressWarnings("unchecked")
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    if (value == null || remappingFunction == null)
      throw new NullPointerException("Neither value nor remapping function may be null");
    LeafNode lf = hb.getOrCreate(key);
    V valval = (V)lf.val();
    if (valval == null) {
      lf.val(value);
      return value;
    } else {
      return applyMapping(key, lf, remappingFunction.apply(valval, value));
    }
  }

  @SuppressWarnings("unchecked")
  public V put(K key, V value) {
    return (V)hb.getOrCreate(key).val(value);
  }


  public final void putAll(Map<? extends K,? extends V> m) {
    final Iterator iter = m.entrySet().iterator();
    while(iter.hasNext()) {
      Map.Entry entry = (Map.Entry)iter.next();
      hb.getOrCreate(entry.getKey()).val(entry.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  public final void putAll(Object[] data) {
    if(!((data.length % 2) == 0))
       throw new RuntimeException("data has odd length.");
    final int alen = data.length;
    for (int idx = 0; idx < alen; idx += 2)
      put((K)data[idx], (V)data[idx+1]);
  }

  @SuppressWarnings("unchecked")
  public V putIfAbsent(K key, V value) {
    int cval = hb.size();
    LeafNode lf = hb.getOrCreate(key);
    if (hb.size() > cval) {
      lf.val(value);
      return value;
    } else {
      return (V)lf.val();
    }
  }

  @SuppressWarnings("unchecked")
  public V remove(Object key) {
    return (V)hb.remove(key);
  }

  public int size() { return hb.size(); }
  public Collection<V> values() {
    return hb.new ValueCollection<V>(true);
  }

  public final PersistentHashMap union(HashMap<K,V> other, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    return new PersistentHashMap(hb.union(other.hb, remappingFunction));
  }

  public final PersistentHashMap union(MapSet other, BiFunction remappingFunction) {
    return new PersistentHashMap(hb.union(((BitmapTrieOwner)other).bitmapTrie(),
					  remappingFunction));
  }

  public final PersistentHashMap difference(HashMap<K,V> other) {
    return new PersistentHashMap(hb.difference(other.hb));
  }

  public final PersistentHashMap difference(MapSet other) {
    return new PersistentHashMap(hb.difference(((BitmapTrieOwner)other).bitmapTrie()));
  }

  public final PersistentHashMap intersection(HashMap<K,V> other, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    return new PersistentHashMap(hb.intersection(other.hb, remappingFunction));
  }

  public final PersistentHashMap intersection(MapSet other, BiFunction remappingFunction) {
    return new PersistentHashMap(hb.intersection(((BitmapTrieOwner)other).bitmapTrie(),
						 remappingFunction));
  }

  @SuppressWarnings("unchecked")
  public void replaceAll(BiFunction<? super K,? super V,? extends V> function) {
    final LeafNodeIterator iter = hb.iterator(hb.identityIterFn);
    while(iter.hasNext()) {
      final ILeaf elem = iter.nextLeaf();
      elem.val(function.apply((K)elem.key(), (V)elem.val()));
    }
  }

  public final PersistentHashMap immutUpdateValues(BiFunction bfn) {
    return new PersistentHashMap(hb.immutUpdate(bfn));
  }

  public final PersistentHashMap immutUpdateValue(Object key, IFn bfn) {
    return new PersistentHashMap(hb.immutUpdate(key, bfn));
  }

  public final IMapEntry entryAt(Object key) {
    LeafNode lf = (LeafNode)hb.get(key);
    return lf == null ? null : new MapEntry(lf.key(), lf.val());
  }

  final void ensureEditable() {
    if (!editable)
      throw new RuntimeException("Transient map editted after persistent!");
  }

  @SuppressWarnings("unchecked")
  public final HashMap<K,V> assoc(Object key, Object val) {
    ensureEditable();
    put((K)key, (V)val);
    return this;
  }

  @SuppressWarnings("unchecked")
  public final HashMap<K,V> conj(Object val) {
    ensureEditable();
    if (val instanceof IPersistentVector) {
      IPersistentVector v = (IPersistentVector)val;
      if (v.count() != 2)
	throw new RuntimeException("Vector length != 2 during conj");
      put((K)v.nth(0), (V)v.nth(1));
    } else if (val instanceof Map.Entry) {
      Map.Entry e = (Map.Entry)val;
      put((K)e.getKey(), (V)e.getValue());
    } else {
      Iterator iter = ((Iterable)val).iterator();
      while(iter.hasNext()) {
	Map.Entry e = (Map.Entry)iter.next();
	put((K)e.getKey(), (V)e.getValue());
      }
    }
    return this;
  }

  public final ITransientMap without(Object key) {
    ensureEditable();
    hb.remove(key);
    return this;
  }

  public final Object valAt(Object key) {
    return hb.get(key);
  }

  public final Object valAt(Object key, Object notFound) {
    return hb.getOrDefault(key, notFound);
  }
  public final Object invoke(Object arg1) {
    return hb.get(arg1);
  }

  public final Object invoke(Object arg1, Object notFound) {
    return hb.getOrDefault(arg1, notFound);
  }
  public int count() { return hb.size(); }

  public IPersistentMap meta() { return hb.meta; }
  public IObj withMeta(IPersistentMap newMeta) {
    return new HashMap(hb.shallowClone(newMeta));
  }

  /**
   * Further changes to this hashmap will also change the persistent map so you must allow
   * this hashmap to fall out of scope after this call.
   */
  public PersistentHashMap persistent() {
    editable = false;
    return new PersistentHashMap(hb);
  }
  public void printNodes() { hb.printNodes(); }
  public Function<V[],HashMap<K,V>> makeFactory(K[] keys) {
    Function<Object[], BitmapTrie> srcFn = BitmapTrie.makeFactory(hb.hp, keys);
    return new Function<V[], HashMap<K,V>>() {
      public HashMap<K,V> apply(V[] values) {
	return new HashMap<K,V>(srcFn.apply(values));
      }
    };
  }

  public Object reduce(IFn rfn) {
    return Reductions.iterReduce(entrySet(), rfn);
  }

  public Object reduce(IFn rfn, Object init) {
    return Reductions.iterReduce(entrySet(), init, rfn);
  }

  public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				  ParallelOptions options ) {
    return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, entrySet(),
						  options);
  }

  @SuppressWarnings("unchecked")
  public void forEach(Consumer c) {
    reduce( new IFnDef() {
	public Object invoke(Object lhs, Object rhs) {
	  c.accept(rhs);
	  return c;
	}
      }, c);
  }
}

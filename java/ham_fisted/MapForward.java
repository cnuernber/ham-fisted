package ham_fisted;


import java.util.Map;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Set;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IMeta;
import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.IObj;
import clojure.lang.ILookup;
import clojure.lang.IHashEq;
import clojure.lang.Counted;
import clojure.lang.IKVReduce;
import clojure.lang.MapEquivalence;
import clojure.lang.IMapIterable;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.RT;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;


public class MapForward<K,V>
  implements Map<K,V>, ITypedReduce, IFnDef, IHashEq, ILookup, Counted,
	     IMeta, IObj, IKVReduce, MapEquivalence, Seqable {
  final Map<K,V> ht;
  final IPersistentMap meta;

  public MapForward(Map<K,V> ht, IPersistentMap meta) { this.ht = ht; this.meta = meta; }
  public void clear() { ht.clear(); }
  public V compute(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    return ht.compute(key, remappingFunction);
  }
  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    return ht.computeIfPresent(key, remappingFunction);
  }
  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    return ht.computeIfAbsent(key, mappingFunction);
  }
  public boolean containsKey(Object key) { return ht.containsKey(key); }
  public boolean containsValue(Object v) { return ht.containsValue(v); }
  public Set<Map.Entry<K,V>> entrySet() { return ht.entrySet(); }
  public boolean equals(Object o) {
    return CljHash.mapEquiv(ht, o);
  }
  public boolean equiv(Object o) { return equals(o); }
  public void forEach(BiConsumer<? super K,? super V> action) {
    ht.forEach(action);
  }
  public V get(Object k) { return ht.get(k); }
  public V getOrDefault(Object k, V dv) { return ht.getOrDefault(k, dv); }
  public int hashCode() { return CljHash.mapHashcode(ht); }
  public int hasheq() { return hashCode(); }
  public boolean isEmpty() { return ht.isEmpty(); }
  public Set<K> keySet() { return ht.keySet(); }
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    return ht.merge(key, value, remappingFunction);
  }
  public V put(K key, V value) { return ht.put(key, value); }
  public void putAll(Map<? extends K,? extends V> m) { ht.putAll(m); }
  public V putIfAbsent(K key, V value) { return ht.putIfAbsent(key, value); }
  public V remove(Object k) { return ht.remove(k); }
  public boolean remove(Object key, Object value) { return ht.remove(key,value); }
  public V replace(K key, V value) { return ht.replace(key, value); }
  public boolean replace(K key, V oldValue, V newValue) {
    return ht.replace(key, oldValue, newValue);
  }
  public void replaceAll(BiFunction<? super K,? super V,? extends V> function) {
    ht.replaceAll(function);
  }
  public int size() { return ht.size(); }
  public int count() { return ht.size(); }
  public Collection<V> values() { return ht.values(); }
  public Iterator iterator() {
    return entrySet().iterator();
  }
  public Iterator keyIterator() { return keySet().iterator(); }
  public Iterator valIterator() { return values().iterator(); }
  public Object reduce(IFn rfn, Object acc) {
    return Reductions.serialReduction(rfn, acc, entrySet());
  }
  @SuppressWarnings("unchecked")
  public final IMapEntry entryAt(Object key) {
    return containsKey(key) ? new MapEntry(key, get(key)) : null;
  }
  public Object valAt(Object key) {
    return get(key);
  }
  @SuppressWarnings("unchecked")
  public Object valAt(Object key, Object notFound) {
    return getOrDefault(key, (V)notFound);
  }
  public final Object invoke(Object arg1) {
    return get(arg1);
  }
  @SuppressWarnings("unchecked")
  public final Object invoke(Object arg1, Object notFound) {
    return getOrDefault(arg1, (V)notFound);
  }
  public ISeq seq() {
    return RT.chunkIteratorSeq(iterator());
  }
  public IPersistentMap meta() { return meta; }
  public MapForward<K,V> withMeta(IPersistentMap m) { return new MapForward<K,V>(ht, m); }
  @SuppressWarnings("unchecked")
  public Object kvreduce(IFn f, Object init) {
    return reduce(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final Map.Entry me = (Map.Entry)v;
	  return f.invoke(acc, me.getKey(), me.getValue());
	}
      }, init);
  }
}

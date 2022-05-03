package ham_fisted;

import static ham_fisted.IntBitmap.*;
import static ham_fisted.HAMTCommon.*;
import static ham_fisted.HashBase.*;
import clojure.lang.MapEntry;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Callable;



public final class HashMap<K,V> implements Map<K,V> {

  final HashBase hb;

  public HashMap() {
    hb = new HashBase();
  }

  public HashMap(HashProvider _hp) {
    hb = new HashBase(_hp);
  }

  //Unsafe construction without clone.
  HashMap(HashBase other, boolean marker) {
    hb = new HashBase(other,marker);
  }
  //Safe construction with deep clone.
  public HashMap(HashBase other) {
    hb = new HashBase(other);
  }

  public HashMap<K,V> clone() {
    return new HashMap<K,V>(this.hb);
  }

  public void clear() {
    hb.clear();
  }

  final V applyMapping(K key, LeafNode node, Object val) {
    if(val == null)
      hb.remove(key);
    else
      node.val(val);
    @SuppressWarnings("unchecked") V valval = (V)val;
    return valval;
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

  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    int startc = hb.size();
    LeafNode node = hb.getOrCreate(key);
    try {
      return applyMapping(key, node, node.val() == null ? mappingFunction.apply(key) : node.val());
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
  public boolean equals(Object o) {
    throw new RuntimeException("unimplemented");
  }

  public void forEach(BiConsumer<? super K,? super V> action) {
    hb.forEach(action);
  }
  public void parallelForEach(BiConsumer<? super K,? super V> action, ExecutorService es,
			      int parallelism) throws Exception {
    hb.parallelForEach(action, es, parallelism);
  }
  public void parallelForEach(BiConsumer<? super K,? super V> action) throws Exception {
    hb.parallelForEach(action);
  }
  public void parallelUpdateValues(BiFunction<? super V,? super V,? extends V> action,
				   ExecutorService es,
				   int parallelism) throws Exception {
    hb.parallelUpdateValues(action, es, parallelism);
  }
  public void parallelUpdateValues(BiFunction<? super K,? super V,? extends V> action) throws Exception {
    hb.parallelUpdateValues(action);
  }


  public V getOrDefault(Object key, V defaultValue) {
    @SuppressWarnings("unchecked") V retval = (V)hb.getOrDefault(key, defaultValue);
    return retval;
  }
  public V get(Object k) {
    return getOrDefault(k,null);
  }
  public int hashCode() { return -1; }
  public boolean isEmpty() { return hb.size() == 0; }
  public Set<K> keySet() { return hb.new KeySet<K>(true); }


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
  public void putAll(Map<? extends K,? extends V> m) {
    final Iterator iter = m.entrySet().iterator();
    while(iter.hasNext()) {
      Map.Entry entry = (Map.Entry)iter.next();
      hb.getOrCreate(entry.getKey()).val(entry.getValue());
    }
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
}

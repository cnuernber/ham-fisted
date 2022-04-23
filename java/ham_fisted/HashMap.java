package ham_fisted;

import static ham_fisted.IntBitmap.*;
import static ham_fisted.HashBase.*;
import clojure.lang.MapEntry;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Function;



public class HashMap<K,V> extends HashBase implements Map<K,V> {

  public HashMap(HashProvider _hp) {
    super(_hp);
  }

  public HashMap() {
    super();
  }

  public HashMap(HashMap<K,V> other) {
    super(other);
  }

  public HashMap<K,V> clone() {
    return new HashMap<K,V>(this);
  }

  public void clear() {
    super.clear();
  }

  V applyMapping(K key, LeafNode node, Object val) {
    if(val == null)
      remove(key);
    else
      node.val(val);
    @SuppressWarnings("unchecked") V valval = (V)val;
    return valval;
  }

  public V compute(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    int startc = c.count();
    LeafNode node = getOrCreate(key);
    boolean added = startc != c.count();
    try {
      @SuppressWarnings("unchecked") V valval = (V)node.val();
      return applyMapping(key, node, remappingFunction.apply(key, valval));
    } catch(Exception e) {
      if (added)
	remove(key);
      throw e;
    }
  }

  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    int startc = c.count();
    LeafNode node = getOrCreate(key);
    try {
      return applyMapping(key, node, node.val() == null ? mappingFunction.apply(key) : node.val());
    } catch(Exception e) {
      if (startc != c.count())
	remove(key);
      throw e;
    }
  }

  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    LeafNode node = getNode(key);
    if (node == null || node.val() == null)
      return null;
    @SuppressWarnings("unchecked") V valval = (V)node.val();
    return applyMapping(key,node, remappingFunction.apply(key, valval));
  }

  Set<Map.Entry<K,V>> cachedSet = null;
  public Set<Map.Entry<K,V>> entrySet() {
    if (cachedSet == null)
      cachedSet = new EntrySet<K,V>(true);
    return cachedSet;
  }
  public boolean equals(Object o) {
    throw new RuntimeException("unimplemented");
  }
  //TODO - spliterator parallelization
  public void forEach(BiConsumer<? super K,? super V> action) {
    forEachImpl(action);
  }

  public V getOrDefault(Object key, V defaultValue) {
    @SuppressWarnings("unchecked") V retval = (V)getOrDefaultImpl(key, defaultValue);
    return retval;
  }
  public V get(Object k) {
    return getOrDefault(k,null);
  }
  public int hashCode() { return -1; }
  public boolean isEmpty() { return c.count() == 0; }
  public Set<K> keySet() { return new KeySet<K>(true); }
  
  
  /**
   * If the specified key is not already associated with a value or is associated
   * with null, associates it with the given non-null value. Otherwise, replaces the
   * associated value with the results of the given remapping function, or removes
   * if the result is null.
   */
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    if (value == null || remappingFunction == null)
      throw new NullPointerException("Neither value nor remapping function may be null");
    LeafNode lf = getOrCreate(key);
    @SuppressWarnings("unchecked") V valval = (V)lf.val();
    if (valval == null) {
      lf.val(value);
      return value;
    } else {
      V retval = remappingFunction.apply(valval, value);
      if (retval == null)
	remove(key);
      else
	lf.val(retval);
      return retval;
    }
  }
  public V put(K key, V value) {
    LeafNode lf = getOrCreate(key);
    @SuppressWarnings("unchecked") V retval = (V)lf.val(value);
    return retval;
  }
  public void putAll(Map<? extends K,? extends V> m) {
    final Iterator iter = m.entrySet().iterator();
    while(iter.hasNext()) {
      Map.Entry entry = (Map.Entry)iter.next();
      LeafNode lf = getOrCreate(entry.getKey());
      lf.val(entry.getValue());
    }
  }
  public V putIfAbsent(K key, V value) {
    LeafNode lf;
    int cval = c.count();
    if (key == null) {
      if (nullEntry == null) {
	c.inc();
	nullEntry = new LeafNode(null, 0);
      }
      lf = nullEntry;
    } else {
      lf = root.getOrCreate(hp, c, hp.hash(key), key);
    }
    if (c.count() > cval) {
      lf.val(value);
      return value;
    } else {
      @SuppressWarnings("unchecked") V retval = (V)lf.val();
      return retval;
    }
  }
  public V remove(Object key) {
    if (key == null) {
      if (nullEntry != null) {
	@SuppressWarnings("unchecked") V retval = (V)nullEntry.val();
	nullEntry = null;
	c.dec();
	return retval;
      }
      return null;
    }
    Box b = new Box();
    root.remove(hp, c, hp.hash(key), key, b);
    @SuppressWarnings("unchecked") V retval = (V)b.obj;
    return retval;
  }
  public int size() { return c.count(); }
  public Collection<V> values() {
    return new ValueCollection<V>(true);
  }
}

package ham_fisted;

import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.ILookup;
import clojure.lang.IHashEq;
import clojure.lang.IPersistentMap;
import clojure.lang.Reduced;
import clojure.lang.IFn;
import clojure.lang.IReduceInit;
  

import static ham_fisted.BitmapTrie.*;
import static ham_fisted.BitmapTrieCommon.*;


public class MutHashTable<K,V>
  implements Map<K,V>, ITypedReduce {
  HashTable ht;
  Set<Map.Entry<K,V>> entrySet;
  Set<K> keySet;
  MutHashTable(HashTable ht) { this.ht = ht; }
  public MutHashTable(HashProvider hp, int count, IPersistentMap m) {
    ht = new HashTable(hp, 0.75f, count, 0, null, m);
  }
  public MutHashTable(HashProvider hp) {
    this(hp, 0, null);
  }
  public int size() { return ht.size(); }
  public MutHashTable<K,V> clone() { return new MutHashTable<K,V>(ht.clone()); }
  public String toString() {
    final StringBuilder b = 
      (StringBuilder) ht.reduce(identityIterFn,
				new IFnDef() {
	  public Object invoke(Object acc, Object v) {
	    final StringBuilder b = (StringBuilder)acc;
	    final LeafNode lf = (LeafNode)v;
	    if(b.length() > 2)
	      b.append(",");
	    return b.append(lf.k)
	      .append(" ")
	      .append(lf.v);
	  }
	}, new StringBuilder().append("{"));
    return b.append("}").toString();
  }
  public final int hashCode() {
    return CljHash.mapHashcode(this);
  }

  public final int hasheq() {
    return hashCode();
  }

  public final boolean equals(Object o) {
    return CljHash.mapEquiv(this, o);
  }

  public final boolean equiv(Object o) {
    return equals(o);
  }
  @SuppressWarnings("unchecked")
  public V get(Object k) {
    LeafNode lf = ht.getNode(k);
    return (V)(lf != null ? lf.v : null);
  }
  @SuppressWarnings("unchecked")
  public V getOrDefault(Object k, V v) {
    LeafNode lf = ht.getNode(k);
    return (V)(lf != null ? lf.v : v);
  }
  @SuppressWarnings("unchecked")
  public V put(K key, V value) {
    return (V)ht.getOrCreate(key).val(value);
  }
  @SuppressWarnings("unchecked")
  public void putAll(Map<? extends K,? extends V> m) {
    Reductions.serialReduction(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final Map.Entry ev = (Map.Entry)v;
	  ((Map<K,V>)acc).put((K)ev.getKey(), (V)ev.getValue());
	  return acc;
	}
      }, this, m);
  }
  public boolean isEmpty() { return ht.isEmpty(); }
  public void clear() {
    ht.clear();    
  }
  @SuppressWarnings("unchecked")
  public V remove(Object k) {
    Box b = new Box();
    ht.remove(k, b);
    return (V)b.obj;
  }
  public MutHashTable<K,V> mutAssoc(K k, V v) {
    ht.mutAssoc(k,v);
    return this;
  }
  public MutHashTable<K,V> mutDissoc(K k) {
    ht.mutDissoc(k);
    return this;
  }
  @SuppressWarnings("unchecked")
  public V compute(K key, BiFunction<? super K,? super V,? extends V> bfn) {
    LeafNode lf = ht.getOrCreate(key);
    V rv = bfn.apply(key, (V)lf.v);
    if(rv == null)
      ht.remove(key, new Box());
    else {
      lf.v = rv;
    }
    return rv;
  }
  @SuppressWarnings("unchecked")
  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> bfn) {
    LeafNode lf = ht.getNode(key);
    V rv = null;
    if(lf != null) {
      rv = bfn.apply(key, (V)lf.v);
      if(rv == null)
	ht.remove(key, new Box());
      else {
	lf.v = rv;
      }
    }
    return rv;
  }
  public V computeIfAbsent(K key, Function<? super K,? extends V> fn) {
    LeafNode lf = ht.getOrCreate(key);
    V rv = null;
    if(lf.v == null) {
      rv = fn.apply(key);
      if (rv == null)
	ht.remove(key, new Box());
      lf.v = rv;
    }
    return rv;
  }
  @SuppressWarnings("unchecked")
  final V applyMapping(K key, LeafNode node, Object val) {
    if(val == null)
      ht.remove(key, new Box());
    else
      node.val(val);
    return (V)val;
  }
  @SuppressWarnings("unchecked")
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    if (value == null || remappingFunction == null)
      throw new NullPointerException("Neither value nor remapping function may be null");
    LeafNode lf = ht.getOrCreate(key);
    V valval = (V)lf.val();
    if (valval == null) {
      lf.val(value);
      return value;
    } else {
      return applyMapping(key, lf, remappingFunction.apply(valval, value));
    }
  }
  
  public boolean containsKey(Object key) { return ht.getNode(key) != null; }
  public boolean containsValue(Object value) {
    return (Boolean)ht.reduce(valIterFn, new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  if(Objects.equals(v, value))
	    return new Reduced(true);
	  return false;
	}
      }, false);
  }
  class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>> {
        final boolean allowsClear;

    EntrySet(boolean _allowsClear) {
      allowsClear = _allowsClear;
    }

    public final int size() {
      return MutHashTable.this.size();
    }

    public final void clear() {
      if (allowsClear) {
	MutHashTable.this.clear();
      }
      else
	throw new RuntimeException("Unimplemented");
    }

    public final Iterator<Map.Entry<K,V>> iterator() {
      @SuppressWarnings("unchecked") Iterator<Map.Entry<K,V>> retval = (Iterator<Map.Entry<K,V>>) MutHashTable.this.iterator(entryIterFn);
      return retval;
    }

    @SuppressWarnings("unchecked")
    public final Spliterator<Map.Entry<K,V>> spliterator() {
      return (Spliterator<Map.Entry<K,V>>)MutHashTable.this.spliterator(entryIterFn);
    }

    public final boolean contains(Object o) {
      if (!(o instanceof Map.Entry))
	return false;
      @SuppressWarnings("unchecked") Map.Entry e = (Map.Entry)o;
      Object key = e.getKey();
      LeafNode candidate = ht.getNode(key);
      return candidate != null &&
	Objects.equals(candidate.key(), key) &&
	Objects.equals(candidate.val(), e.getValue());
    }
    public Object reduce(IFn rfn, Object init) {
      return MutHashTable.this.reduce(entryIterFn, rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }  
  }
  @SuppressWarnings("unchecked")
  public Set<Map.Entry<K,V>> entrySet() {
    if(entrySet == null)
      entrySet = new EntrySet(true);
    return entrySet;
  }
  class KeySet<K> extends AbstractSet<K> implements ITypedReduce {
    final boolean allowsClear;
    KeySet(boolean _ac) {
      allowsClear = _ac;
    }
    public final int size() {
      return MutHashTable.this.size();
    }
    public final void clear() {
      if (allowsClear)
	MutHashTable.this.clear();
      else
	throw new RuntimeException("Unimplemented");
    }

    public final Iterator<K> iterator() {
      @SuppressWarnings("unchecked") Iterator<K> retval = (Iterator<K>) MutHashTable.this.iterator(keyIterFn);
      return retval;
    }

    @SuppressWarnings("unchecked")
    public final Spliterator<K> spliterator() {
      return (Spliterator<K>)MutHashTable.this.spliterator(keyIterFn);
    }

    public final boolean contains(Object key) {
      return ht.getNode(key) != null;
    }

    public Object reduce(IFn rfn, Object init) {
      return MutHashTable.this.reduce(keyIterFn, rfn, init);
    }

    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
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
  public final Set<K> keySet() {
    if(keySet == null)
      keySet = new KeySet<K>(true);
    return keySet;
  }
  
  class ValueCollection<V>  extends AbstractCollection<V> implements ITypedReduce {
    boolean allowsClear;
    ValueCollection(boolean ac) { allowsClear = ac; }
    public final int size() { return MutHashTable.this.size(); }
    public final void clear() {
      if (allowsClear)
	MutHashTable.this.clear();
      else
	throw new RuntimeException("Unimplemented");
    }
    public final Iterator<V> iterator() {
      @SuppressWarnings("unchecked") Iterator<V> retval = (Iterator<V>) MutHashTable.this.iterator(valIterFn);
      return retval;
    }
    @SuppressWarnings("unchecked")
    public final Spliterator<V> spliterator() {
      return (Spliterator<V>)MutHashTable.this.spliterator(valIterFn);
    }
    public Object reduce(IFn rfn, Object init) {
      return MutHashTable.this.reduce(valIterFn, rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
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
  public Collection<V> values() {
    return new ValueCollection<V>(true);
  }
  public Iterator iterator(Function<ILeaf,Object> fn)  {
    return ht.iterator(fn);
  }
  public Spliterator spliterator(Function<ILeaf,Object> fn) {
    return ht.spliterator(fn);
  }
  public Object reduce(Function<ILeaf,Object> lfn, IFn rfn, Object acc) {
    return this.ht.reduce(lfn, rfn, acc);
  }
  public Object reduce(IFn rfn, Object acc) {
    return ((IReduceInit)entrySet()).reduce(rfn, acc);
  }
  public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				  ParallelOptions options ) {
    return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, entrySet(),
						  options);
  }
  
}

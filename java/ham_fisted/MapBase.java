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
import java.util.function.BiConsumer;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.ILookup;
import clojure.lang.IHashEq;
import clojure.lang.IPersistentMap;
import clojure.lang.Reduced;
import clojure.lang.IFn;
import clojure.lang.IReduceInit;
import clojure.lang.IMapEntry;
import clojure.lang.MapEntry;
import clojure.lang.Counted;
import clojure.lang.IPersistentMap;
import clojure.lang.IMeta;
import clojure.lang.IPersistentVector;
import clojure.lang.IKVReduce;
import clojure.lang.MapEquivalence;
import clojure.lang.IMapIterable;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.RT;


import static ham_fisted.BitmapTrie.*;
import static ham_fisted.BitmapTrieCommon.*;


public class MapBase<K,V>
  implements Map<K,V>, ITypedReduce, IFnDef, IHashEq, ILookup, Counted, IMeta,
	     IMapIterable, IKVReduce, MapEquivalence, Iterable, Seqable {
  MapData ht;
  Set<Map.Entry<K,V>> entrySet;
  Set<K> keySet;
  public MapBase(MapData ht) {
    this.ht = ht;
  }
  public int size() { return ht.size(); }
  public int count() { return ht.size(); }
  public MapBase<K,V> clone() { return new MapBase<K,V>(ht.clone()); }
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
    return (V)ht.get(k);
  }
  @SuppressWarnings("unchecked")
  public V getOrDefault(Object k, V v) {
    return (V)ht.getOrDefault(k,v);
  }
  @SuppressWarnings("unchecked")
  public V put(K key, V value) {
    return (V)ht.put(key,value);
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
  MapBase<K,V> mutAssoc(K k, V v) {
    ht.mutAssoc(k,v);
    return this;
  }
  MapBase<K,V> mutDissoc(K k) {
    ht.mutDissoc(k);
    return this;
  }
  MapBase<K,V> mutUpdateValue(K k, IFn fn) {
    ht.mutUpdateValue(k, fn);
    return this;
  }
  MapBase<K,V> mutUpdateValues(BiFunction<? super K,? super V,? extends V> fn) {
    ht.mutUpdateValues(fn);
    return this;
  }
  @SuppressWarnings("unchecked")
  MapBase<K,V> mutConj(Object val) {
    if (val instanceof IPersistentVector) {
      IPersistentVector v = (IPersistentVector)val;
      if (v.count() != 2)
	throw new RuntimeException("Vector length != 2 during conj");
      return mutAssoc((K)v.nth(0), (V)v.nth(1));
    } else if (val instanceof Map.Entry) {
      Map.Entry e = (Map.Entry)val;
      reutrn mutAssoc((K)e.getKey(), (V)e.getValue());
    } else {
      Iterator iter = ((Iterable)val).iterator();
      MapBase<K,V> rv = this;
      while(iter.hasNext()) {
	Map.Entry e = (Map.Entry)iter.next();
	rv = rv.mutAssoc((K)e.getKey(), (V)e.getValue());
      }
      return rv;
    }
  }
  @SuppressWarnings("unchecked")
  final V applyMapping(K key, ILeaf node, Object val) {
    if(val == null)
      ht.remove(key, new Box());
    else
      node.val(val);
    return (V)val;
  }
  @SuppressWarnings("unchecked")
  public V compute(K key, BiFunction<? super K,? super V,? extends V> bfn) {
    //This operation is a performance sensitive operation so it must be done at the
    //lowest level
    if (key == null || bfn == null)
      throw new NullPointerException("Neither key nor compute function may be null");
    return (V)ht.compute(key, bfn);
  }
  @SuppressWarnings("unchecked")
  public V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction) {
    int startc = ht.size();
    ILeaf node = ht.getOrCreate(key);
    try {
      return applyMapping(key, node, node.val() == null ? mappingFunction.apply(key) : (V)node.val());
    } catch(Exception e) {
      if (startc != ht.size())
	remove(key);
      throw e;
    }
  }
  public V computeIfPresent(K key, BiFunction<? super K,? super V,? extends V> remappingFunction) {
    ILeaf node = ht.getNode(key);
    if (node == null || node.val() == null)
      return null;
    @SuppressWarnings("unchecked") V valval = (V)node.val();
    return applyMapping(key,node, remappingFunction.apply(key, valval));
  }
  @SuppressWarnings("unchecked")
  public V merge(K key, V value, BiFunction<? super V,? super V,? extends V> remappingFunction) {
    if (value == null || remappingFunction == null)
      throw new NullPointerException("Neither value nor remapping function may be null");
    return (V)ht.merge(key,value,remappingFunction);
  }
  @SuppressWarnings("unchecked")
  public void forEach(BiConsumer<? super K,? super V> action) {
    ht.reduce(identityIterFn, new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  ILeaf lf = (ILeaf)v;
	  action.accept((K)lf.key(), (V)lf.val());
	  return action;
	}
      }, action);
  }
  @SuppressWarnings("unchecked")
  public void replaceAll(BiFunction<? super K,? super V,? extends V> function) {
    ht.reduce(identityIterFn, new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  ILeaf lf = (ILeaf)v;
	  lf.val(function.apply((K)lf.key(), (V)lf.val()));
	  return function;
	}
      }, function);
  }

  public boolean containsKey(Object key) { return ht.containsKey(key); }
  public boolean containsValue(Object value) {
    return (Boolean)ht.reduce(valIterFn, new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  if(Objects.equals(v, value))
	    return new Reduced(true);
	  return false;
	}
      }, false);
  }
  class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>>
			      implements ITypedReduce<Map.Entry<K,V>> {
    EntrySet() {
    }

    public final int size() {
      return MapBase.this.size();
    }

    public final void clear() {
      MapBase.this.clear();
    }

    public final Iterator<Map.Entry<K,V>> iterator() {
      @SuppressWarnings("unchecked") Iterator<Map.Entry<K,V>> retval = (Iterator<Map.Entry<K,V>>) MapBase.this.iterator(identityIterFn);
      return retval;
    }

    public final void forEach(Consumer<? super Map.Entry<K,V>> c) {
      ITypedReduce.super.forEach(c);
    }

    @SuppressWarnings("unchecked")
    public final Spliterator<Map.Entry<K,V>> spliterator() {
      return (Spliterator<Map.Entry<K,V>>)MapBase.this.spliterator(identityIterFn);
    }

    public final boolean contains(Object o) {
      if (!(o instanceof Map.Entry))
	return false;
      @SuppressWarnings("unchecked") Map.Entry e = (Map.Entry)o;
      Object key = e.getKey();
      ILeaf candidate = ht.getNode(key);
      return candidate != null &&
	Objects.equals(candidate.key(), key) &&
	Objects.equals(candidate.val(), e.getValue());
    }
    public Object reduce(IFn rfn, Object init) {
      return MapBase.this.reduce(identityIterFn, rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }
  }
  @SuppressWarnings("unchecked")
  public Set<Map.Entry<K,V>> entrySet() {
    if(entrySet == null)
      entrySet = new EntrySet();
    return entrySet;
  }
  class KeySet<K> extends AbstractSet<K> implements ITypedReduce<K> {
    KeySet() {}
    public final int size() {
      return MapBase.this.size();
    }
    public final void clear() {
      MapBase.this.clear();
    }

    public final Iterator<K> iterator() {
      @SuppressWarnings("unchecked") Iterator<K> retval = (Iterator<K>) MapBase.this.iterator(keyIterFn);
      return retval;
    }

    @SuppressWarnings("unchecked")
    public final Spliterator<K> spliterator() {
      return (Spliterator<K>)MapBase.this.spliterator(keyIterFn);
    }

    public final boolean contains(Object key) {
      return ht.getNode(key) != null;
    }

    public Object reduce(IFn rfn, Object init) {
      return MapBase.this.reduce(keyIterFn, rfn, init);
    }

    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super K> c) {
      reduce( new IFnDef() {
	  public Object invoke(Object lhs, Object rhs) {
	    c.accept((K)rhs);
	    return c;
	  }
	}, c);
    }
  }
  public final Set<K> keySet() {
    if(keySet == null)
      keySet = new KeySet<K>();
    return keySet;
  }

  class ValueCollection<V>  extends AbstractCollection<V> implements ITypedReduce<V> {
    ValueCollection() {}
    public final int size() { return MapBase.this.size(); }
    public final void clear() {
      MapBase.this.clear();
    }
    public final Iterator<V> iterator() {
      @SuppressWarnings("unchecked") Iterator<V> retval = (Iterator<V>) MapBase.this.iterator(valIterFn);
      return retval;
    }
    @SuppressWarnings("unchecked")
    public final Spliterator<V> spliterator() {
      return (Spliterator<V>)MapBase.this.spliterator(valIterFn);
    }
    public Object reduce(IFn rfn, Object init) {
      return MapBase.this.reduce(valIterFn, rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super V> c) {
      reduce( new IFnDef() {
	  public Object invoke(Object lhs, Object rhs) {
	    c.accept((V)rhs);
	    return c;
	  }
	}, c);
    }
  }
  public Collection<V> values() {
    return new ValueCollection<V>();
  }
  Iterator iterator(Function<ILeaf,Object> fn)  {
    return ht.iterator(fn);
  }
  public Iterator iterator() {
    return entrySet().iterator();
  }
  @SuppressWarnings("unchecked")
  public void forEach(Consumer c) {
    ITypedReduce.super.forEach(c);
  }
  public Iterator keyIterator() { return ht.iterator(keyIterFn); }
  public Iterator valIterator() { return ht.iterator(valIterFn); }

  Spliterator spliterator(Function<ILeaf,Object> fn) {
    return ht.spliterator(fn);
  }
  Object reduce(Function<ILeaf,Object> lfn, IFn rfn, Object acc) {
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
  public Object kvreduce(IFn f, Object init) {
    return ht.reduce(identityIterFn, new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final ILeaf lf = (ILeaf)v;
	  return f.invoke(acc, lf.key(), lf.val());
	}
      }, init);
  }
  @SuppressWarnings("unchecked")
  public final IMapEntry entryAt(Object key) {
    LeafNode lf = (LeafNode)ht.getNode(key);
    return lf == null ? null : new MapEntry((K)lf.key(), (V)lf.val());
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
  public IPersistentMap meta() { return ht.meta(); }
}

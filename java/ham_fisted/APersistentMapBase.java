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
import clojure.lang.APersistentMap;


import static ham_fisted.BitmapTrie.*;
import static ham_fisted.BitmapTrieCommon.*;


public abstract class APersistentMapBase extends APersistentMap
  implements Map, ITypedReduce, IHashEq, ILookup, Counted, IMeta,
	     IMapIterable, IKVReduce, MapEquivalence, Iterable, Seqable {
  MapData ht;
  Set<Map.Entry> entrySet;
  Set keySet;
  public APersistentMapBase(MapData ht) {
    this.ht = ht;
  }
  public int size() { return ht.size(); }
  public int count() { return ht.size(); }
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
  @SuppressWarnings("unchecked")
  public Object get(Object k) {
    return ht.get(k);
  }
  @SuppressWarnings("unchecked")
  public Object getOrDefault(Object k, Object v) {
    return ht.getOrDefault(k,v);
  }
  public boolean isEmpty() { return ht.isEmpty(); }
  public void clear() {
    ht.clear();
  }
  @SuppressWarnings("unchecked")
  public void forEach(BiConsumer action) {
    ht.reduce(identityIterFn, new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  ILeaf lf = (ILeaf)v;
	  action.accept(lf.key(),lf.val());
	  return action;
	}
      }, action);
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
  class EntrySet extends AbstractSet<Map.Entry>
    implements ITypedReduce<Map.Entry> {
    EntrySet() {
    }

    public final int size() {
      return APersistentMapBase.this.size();
    }
    
    public final void clear() {
      APersistentMapBase.this.clear();
    }

    public final Iterator<Map.Entry> iterator() {
      @SuppressWarnings("unchecked") Iterator<Map.Entry> retval = (Iterator<Map.Entry>) APersistentMapBase.this.iterator(identityIterFn);
      return retval;
    }
    @SuppressWarnings("unchecked")
    public final void forEach(Consumer c) {
      ITypedReduce.super.forEach(c);
    }

    @SuppressWarnings("unchecked")
    public final Spliterator<Map.Entry> spliterator() {
      return (Spliterator<Map.Entry>)APersistentMapBase.this.spliterator(identityIterFn);
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
      return APersistentMapBase.this.reduce(identityIterFn, rfn, init);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return Reductions.parallelCollectionReduction(initValFn, rfn, mergeFn, this, options);
    }
  }
  @SuppressWarnings("unchecked")
  public Set<Map.Entry> entrySet() {
    if(entrySet == null)
      entrySet = new EntrySet();
    return entrySet;
  }
  class KeySet extends AbstractSet implements ITypedReduce {
    KeySet() {}
    public final int size() {
      return APersistentMapBase.this.size();
    }
    public final void clear() {
      APersistentMapBase.this.clear();
    }

    public final Iterator iterator() {
      @SuppressWarnings("unchecked") Iterator retval = (Iterator) APersistentMapBase.this.iterator(keyIterFn);
      return retval;
    }

    @SuppressWarnings("unchecked")
    public final Spliterator spliterator() {
      return (Spliterator)APersistentMapBase.this.spliterator(keyIterFn);
    }

    public final boolean contains(Object key) {
      return ht.getNode(key) != null;
    }

    public Object reduce(IFn rfn, Object init) {
      return APersistentMapBase.this.reduce(keyIterFn, rfn, init);
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
  public final Set keySet() {
    if(keySet == null)
      keySet = new KeySet();
    return keySet;
  }

  class ValueCollection  extends AbstractCollection implements ITypedReduce {
    ValueCollection() {}
    public final int size() { return APersistentMapBase.this.size(); }
    public final void clear() {
      APersistentMapBase.this.clear();
    }
    public final Iterator iterator() {
      @SuppressWarnings("unchecked") Iterator retval = (Iterator) APersistentMapBase.this.iterator(valIterFn);
      return retval;
    }
    @SuppressWarnings("unchecked")
    public final Spliterator spliterator() {
      return (Spliterator)APersistentMapBase.this.spliterator(valIterFn);
    }
    public Object reduce(IFn rfn, Object init) {
      return APersistentMapBase.this.reduce(valIterFn, rfn, init);
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
  public Collection values() {
    return new ValueCollection();
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
    return lf == null ? null : new MapEntry(lf.key(), lf.val());
  }
  public Object valAt(Object key) {
    return get(key);
  }
  @SuppressWarnings("unchecked")
  public Object valAt(Object key, Object notFound) {
    return getOrDefault(key, notFound);
  }
  public final Object invoke(Object arg1) {
    return get(arg1);
  }
  @SuppressWarnings("unchecked")
  public final Object invoke(Object arg1, Object notFound) {
    return getOrDefault(arg1, notFound);
  }
  public ISeq seq() {
    return RT.chunkIteratorSeq(iterator());
  }
  public IPersistentMap meta() { return ht.meta(); }
}

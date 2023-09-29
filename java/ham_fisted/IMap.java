package ham_fisted;


import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Collection;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.Objects;
import clojure.lang.IFn;
import clojure.lang.ILookup;
import clojure.lang.IMapIterable;
import clojure.lang.Counted;
import clojure.lang.MapEquivalence;
import clojure.lang.IKVReduce;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.RT;


public interface IMap extends Map, ITypedReduce, ILookup, IFnDef, Iterable, IMapIterable, Counted,
			      MapEquivalence, IKVReduce, Seqable
{
  Iterator iterator(Function<Map.Entry, Object> fn);
  default Spliterator spliterator(Function<Map.Entry, Object> fn) {
    throw new RuntimeException("Unimplemented");
  }
  default Iterator keyIterator() { return iterator((k)->k.getKey()); }
  default Iterator valIterator() { return iterator((k)->k.getValue()); }
  default Iterator entryIterator() { return iterator((k)->k); }
  default Iterator iterator() { return entryIterator(); }
  @SuppressWarnings("unchecked")
  default void putAll(Map data) {
    data.forEach(new BiConsumer() {
	public void accept(Object k, Object v) {
	  put(k,v);
	}
      });
  }
  default boolean containsValue(Object val) {
    return values().contains(val);
  }
  default Object valAt(Object k) { return get(k); }
  @SuppressWarnings("unchecked")
  default Object valAt(Object k, Object defVal) { return getOrDefault(k, defVal); }
  default Object invoke(Object k) { return get(k); }
  @SuppressWarnings("unchecked")
  default Object invoke(Object k, Object defVal) { return getOrDefault(k, defVal); }
  default boolean isEmpty() { return this.size() == 0; }
  @SuppressWarnings("unchecked")
  default void forEach(Consumer c) {
    ITypedReduce.super.forEach(c);
  }
  default int count() { return size(); }
  default Object kvreduce(IFn f, Object init) {
    return reduce(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  final Map.Entry lf = (Map.Entry)v;
	  return f.invoke(acc, lf.getKey(), lf.getValue());
	}
      }, init);
  }
  default ISeq seq() {
    return RT.chunkIteratorSeq(iterator());
  }
  public static class MapKeySet extends AbstractSet implements ITypedReduce, IFnDef, Counted {
    public final IMap data;
    public MapKeySet(IMap data) { this.data = data; }
    public int size() { return data.size(); }
    public int count() { return data.size(); }
    public boolean contains(Object k) { return data.containsKey(k); }
    public Iterator iterator() { return data.keyIterator(); }
    public Spliterator spliterator() { return data.spliterator( (k)->k.getKey() ); }
    public void clear() { data.clear(); }
    static IFn wrapRfn(IFn rfn) { return new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  return rfn.invoke(acc, ((Map.Entry)v).getKey());
	}
      };
    }
    public Object reduce(IFn rfn, Object acc) {
      return data.reduce(wrapRfn(rfn), acc);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return data.parallelReduction(initValFn, wrapRfn(rfn), mergeFn, options);
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
    public Object invoke(Object k) { return data.containsKey(k) ? k : null; }
  }
  default Set keySet() {
    return new MapKeySet(this);
  }
  public static class MapEntrySet extends AbstractSet implements ITypedReduce, IFnDef, Counted {
    public final IMap data;
    public MapEntrySet(IMap data) { this.data = data; }
    public int size() { return data.size(); }
    public int count() { return data.size(); }
    public boolean contains(Object k) {
      if(!(k instanceof Map.Entry)) return false;
      Map.Entry e = (Map.Entry)k;
      return Objects.equals(data.get(e), e.getValue());
    }
    public Iterator iterator() { return data.entryIterator(); }
    public Spliterator spliterator() { return data.spliterator( (k)->k ); }
    public void clear() { data.clear(); }
    public Object reduce(IFn rfn, Object acc) {
      return data.reduce(rfn, acc);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return data.parallelReduction(initValFn, rfn, mergeFn, options);
    }

    @SuppressWarnings("unchecked")
    public void forEach(Consumer c) {
      data.forEach(c);
    }
    public Object invoke(Object k) { return contains(k); }
  }
  default Set entrySet() {
    return new MapEntrySet(this);
  }
  public static class ValueCollection  extends AbstractCollection implements ITypedReduce, Counted {
    public final IMap data;
    public ValueCollection(IMap data) { this.data = data;}
    public final int size() { return data.size(); }
    public int count() { return data.size(); }
    public final void clear() {
      data.clear();
    }
    public final Iterator iterator() {
      return data.valIterator(); 
    }
    @SuppressWarnings("unchecked")
    public final Spliterator spliterator() {
      return data.spliterator((k)->k.getValue());
    }
    public static IFn wrapRfn(IFn val) {
      return new IFnDef() {
	public Object invoke(Object acc, Object e) {
	  return val.invoke(acc, ((Map.Entry)e).getValue());
	}
      };
    }
    public Object reduce(IFn rfn, Object acc) {
      return data.reduce(wrapRfn(rfn), acc);
    }
    public Object parallelReduction(IFn initValFn, IFn rfn, IFn mergeFn,
				    ParallelOptions options ) {
      return data.parallelReduction(initValFn, wrapRfn(rfn), mergeFn, options);
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
  default Collection values() {
    return new ValueCollection(this);
  }
}

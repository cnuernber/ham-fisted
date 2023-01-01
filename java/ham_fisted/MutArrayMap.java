package ham_fisted;


import java.util.function.BiFunction;
import java.util.Map;
import clojure.lang.IFn;
import clojure.lang.ITransientMap;
import clojure.lang.ITransientAssociative2;
import clojure.lang.IObj;
import clojure.lang.IPersistentMap;

import static ham_fisted.BitmapTrieCommon.*;


public class MutArrayMap<K,V>
  extends ArrayMapBase<K,V>
  implements ITransientMap, ITransientAssociative2, IObj,
	     UpdateValues {
  public MutArrayMap(HashProvider hp) {
    super(new ArrayMap(hp, new Object[4], 0, null));
  }
  public MutArrayMap(ArrayMap ht) {
    super(ht);
  }

  public MutArrayMap<K,V> clone() {
    return new MutArrayMap<K,V>((ArrayMap)ht.clone());
  }
  public ITransientMap conj(Object val) {
    return (ITransientMap) mutConj(val);
  }
  @SuppressWarnings("unchecked")
  public ITransientMap assoc(Object key, Object val) {
    return (ITransientMap) super.mutAssoc((K)key,(V)val);
  }
  @SuppressWarnings("unchecked")
  public MutArrayMap<K,V> without(Object key) {
    return (MutArrayMap<K,V>)mutDissoc((K)key);
  }
  public MutArrayMap<K,V> withMeta(IPersistentMap m) {
    return new MutArrayMap<K,V>((HashTable)ht.withMeta(m));
  }
  @SuppressWarnings("unchecked")
  public MutArrayMap<K,V> updateValues(BiFunction valueMap) {
    replaceAll(valueMap);
    return this;
  }
  @SuppressWarnings("unchecked")
  public UpdateValues updateValue(Object key, IFn fn) {
    return (UpdateValues)super.mutUpdateValue((K)key, fn);
  }
  public IPersistentMap persistent() {
    return new ImmutArrayMap<K,V>((ArrayMap)ht);
  }

  public static Map create(HashProvider hp, boolean byAssoc, Object... data) {
    final int nk = data.length/2;
    if((data.length % 2) != 0)
      throw new RuntimeException("Uneven number of keys-vals: " + data.length);

    MapData md;
    if(nk <= 8) {
      md = new ArrayMap(hp, new Object[nk*2], 0, null);
    } else {
      md = new HashTable(hp, 0.75f, (int)(nk / 0.75), 0, null, null);
    }
    if(byAssoc) {
      for(int idx = 0; idx < nk; ++idx) {
	int kidx = idx*2;
	md.put(data[kidx], data[kidx+1]);
      }
    } else {
      for(int idx = 0; idx < nk; ++idx) {
	int kidx = idx*2;
	md.put(data[kidx], data[kidx+1]);
	if(md.size() != idx+1)
	  throw new RuntimeException("Duplicate key: " + String.valueOf(data[kidx]));
      }
    }
    return md instanceof ArrayMap ?
      new MutArrayMap((ArrayMap)md) :
      new MutHashTable((HashTable)md);
  }
  public static MapBase createReducible(HashProvider hp, Object data) {
    return (MapBase) Reductions.serialReduction(new IFnDef() {
	public Object invoke(Object acc, Object v) {
	  return ((MapBase)acc).mutConj(v);
	}
      }, new MutArrayMap(hp), data);
  }
  public static final boolean different(HashProvider hp, Object a, Object b) {
    return !hp.equals(a,b);
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(b,c));
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) ||
	     hp.equals(b,c) || hp.equals(b,d) ||
	     hp.equals(c,d));
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) ||
	     hp.equals(c,d) || hp.equals(c,e) ||
	     hp.equals(d,e)
	     );
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e, Object f) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) || hp.equals(a, f) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) || hp.equals(b, f) ||
	     hp.equals(c,d) || hp.equals(c,e) || hp.equals(c,f) ||
	     hp.equals(d,e) || hp.equals(d,f) ||
	     hp.equals(e,f)
	     );
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e, Object f, Object g) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) || hp.equals(a,f) || hp.equals(a,g) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) || hp.equals(b,f) || hp.equals(b,g) ||
	     hp.equals(c,d) || hp.equals(c,e) || hp.equals(c,f) || hp.equals(c,g) ||
	     hp.equals(d,e) || hp.equals(d,f) || hp.equals(d,g) ||
	     hp.equals(e,f) || hp.equals(e,g) ||
	     hp.equals(f,g)
	     );
  }
  public static final boolean different(HashProvider hp, Object a, Object b, Object c, Object d, Object e, Object f, Object g, Object h) {
    return !(hp.equals(a,b) || hp.equals(a,c) || hp.equals(a,d) || hp.equals(a,e) || hp.equals(a,f) || hp.equals(a,g) || hp.equals(a,h) ||
	     hp.equals(b,c) || hp.equals(b,d) || hp.equals(b,e) || hp.equals(b,f) || hp.equals(b,g) || hp.equals(b,h) ||
	     hp.equals(c,d) || hp.equals(c,e) || hp.equals(c,f) || hp.equals(c,g) || hp.equals(c,h) ||
	     hp.equals(d,e) || hp.equals(d,f) || hp.equals(d,g) || hp.equals(d,h) ||
	     hp.equals(e,f) || hp.equals(e,g) || hp.equals(e,h) ||
	     hp.equals(f,g) || hp.equals(f,h) ||
	     hp.equals(g,h)
	     );
  }
  public static MutArrayMap createKV(HashProvider hp, Object k, Object v) {
    return new MutArrayMap(new ArrayMap(hp, new Object[]{k,v}, 1, null));
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1) {
    if(different(hp, k0, k1))
      return new MutArrayMap(new ArrayMap(hp, new Object[]{k0,v0,k1,v1}, 2, null));
    return createKV(hp, k1, v1);
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1,
				     Object k2, Object v2) {
    final Object[] objs = new Object[] {k0, v0, k1, v1, k2, v2};
    if(different(hp, k0, k1, k2))
      return new MutArrayMap(new ArrayMap(hp, objs, 3, null));
    return (MutArrayMap)create(hp, true, objs);
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1,
				     Object k2, Object v2,
				     Object k3, Object v3) {
    final Object[] objs = new Object[] {k0, v0, k1, v1, k2, v2, k3, v3};
    if(different(hp, k0, k1, k2, k3))
      return new MutArrayMap(new ArrayMap(hp, objs, 4, null));
    return (MutArrayMap)create(hp, true, objs);
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1,
				     Object k2, Object v2,
				     Object k3, Object v3,
				     Object k4, Object v4) {
    final Object[] objs = new Object[] {k0, v0, k1, v1, k2, v2, k3, v3, k4, v4};
    if(different(hp, k0, k1, k2, k3, k4))
      return new MutArrayMap(new ArrayMap(hp, objs, 5, null));
    return (MutArrayMap)create(hp, true, objs);
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1,
				     Object k2, Object v2,
				     Object k3, Object v3,
				     Object k4, Object v4,
				     Object k5, Object v5) {
    final Object[] objs = new Object[] {k0, v0, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5};
    if(different(hp, k0, k1, k2, k3, k4, k5))
      return new MutArrayMap(new ArrayMap(hp, objs, 6, null));
    return (MutArrayMap)create(hp, true, objs);
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1,
				     Object k2, Object v2,
				     Object k3, Object v3,
				     Object k4, Object v4,
				     Object k5, Object v5,
				     Object k6, Object v6) {
    final Object[] objs = new Object[] {k0, v0, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6};
    if(different(hp, k0, k1, k2, k3, k4, k5, k6))
      return new MutArrayMap(new ArrayMap(hp, objs, 7, null));
    return (MutArrayMap)create(hp, true, objs);
  }
  public static MutArrayMap createKV(HashProvider hp,
				     Object k0, Object v0,
				     Object k1, Object v1,
				     Object k2, Object v2,
				     Object k3, Object v3,
				     Object k4, Object v4,
				     Object k5, Object v5,
				     Object k6, Object v6,
				     Object k7, Object v7) {
    final Object[] objs = new Object[] {k0, v0, k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6,
	k7, v7};
    if(different(hp, k0, k1, k2, k3, k4, k5, k6, k7))
      return new MutArrayMap(new ArrayMap(hp, objs, 8, null));
    return (MutArrayMap)create(hp, true, objs);
  }
}

package ham_fisted;


import java.util.function.BiFunction;
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
  static <K,V> Map<K,V> doCreate(MapData md, boolean byAssoc, Object[] data) {
    if(byAssoc) {
      for(int idx = 0; idx < nk; ++idx) {
	int kidx = idx*2;
	md.put(data[kidx], data[kidx+2]);
      }
    } else {
      for(int idx = 0; idx < nk; ++idx) {
	int kidx = idx*2;
	md.put(data[kidx], data[kidx+2]);
	if(md.size() != idx)
	  throw new RuntimeException("Duplicate key: " + String.valueOf(data[kidx]));
      }
    }
    return (Map<K,V>)md.mutable();
  }
  public static Map create(HashProvider hp, boolean byAssoc, Object[] data) {
    final int nk = data.length/2;
    if((data.length % 2) != 0)
      throw new RuntimeException("Uneven number of keys-vals: " + data.length);

    MapData md;
    if(nk <= 8) {
      md = new ArrayMap(hp, new Object[nk*2], 0, null);
    } else {
      md = new HashTable(hp, null, nk);
    }
    if(byAssoc) {
      for(int idx = 0; idx < nk; ++idx) {
	int kidx = idx*2;
	md.put(data[kidx], data[kidx+2]);
      }
    } else {
      for(int idx = 0; idx < nk; ++idx) {
	int kidx = idx*2;
	md.put(data[kidx], data[kidx+2]);
	if(md.size() != idx)
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
	  return ((MapData)acc).mutConj(v);
	}
      }, new MutArrayMap(hp), data);
  }
}

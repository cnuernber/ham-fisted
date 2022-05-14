package ham_fisted;


import static ham_fisted.BitmapTrieCommon.*;
import static ham_fisted.BitmapTrie.*;
import clojure.lang.ITransientSet;
import clojure.lang.IObj;
import clojure.lang.ILookup;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.IteratorSeq;
import clojure.lang.IPersistentMap;
import java.util.Iterator;


public class TransientHashSet
  implements ITransientSet, IObj, IFnDef, ILookup, Seqable {

  final BitmapTrie hb;
  boolean editable;

  TransientHashSet(BitmapTrie _hb) { this.hb = _hb.shallowClone(); editable = true; }

  final void ensureEditable() { if (!editable) throw new RuntimeException("Transient set edited after persistent!"); }
     
  public final int count() { return hb.size(); }
  public final ITransientSet disjoin(Object key) {
    ensureEditable();
    hb.dissoc(key);
    return this;
  }
  public final boolean contains(Object key) {
    return hb.getNode(key) != null;
  }
  public final Object get(Object key) {
    return contains(key) ? key : null;
  }
  public final TransientHashSet conj(Object key) {
    ensureEditable();
    hb.assoc(key, HashSet.PRESENT);
    return this;
  }
  public final PersistentHashSet persistent() {
    editable = false;
    return new PersistentHashSet(hb);
  }
  public final Iterator iterator() { return hb.iterator(keyIterFn); }
  public final ISeq seq() { return IteratorSeq.create(iterator()); }
  public final IPersistentMap meta() { return hb.meta(); }
  public final TransientHashSet withMeta(IPersistentMap meta) {
    return new TransientHashSet(hb.shallowClone(meta));
  }
  public final Object valAt(Object key) {
    return get(key);
  }
  public final Object valAt(Object key, Object notFound) {
    return contains(key) ? key : notFound;
  }
  public final Object invoke(Object key) {
    return get(key);
  }
  public final Object invoke(Object key, Object notFound) {
    return contains(key) ? key : notFound;
  }
  
}

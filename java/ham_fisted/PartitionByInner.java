package ham_fisted;


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import clojure.lang.IFn;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.IDeref;
import clojure.lang.Util;
import clojure.lang.IMeta;
import clojure.lang.RT;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentArrayMap;
import clojure.lang.Keyword;



public class PartitionByInner implements ITypedReduce, Iterator, Seqable, IDeref, IMeta {
  public final Iterator iter;
  public final IFn f;
  public final Object fv;
  public final BiPredicate pred;
  boolean lastVValid;
  Object lastV;
  Object lastFV;

  public PartitionByInner(Iterator i, IFn f, Object v, BiPredicate pred) {
    this.iter = i;
    this.f = f;
    this.lastV = v;
    this.lastVValid = true;
    final Object fv = f.invoke(v);
    this.fv = fv;
    this.lastFV = fv;
    this.pred = pred == null ? (x,y)->CljHash.equiv(x,y) : pred;
  }

  Object advance() {
    Object rv = lastV;
    if (iter.hasNext()) {
      lastVValid = true;
      Object vv= iter.next();
      lastV = vv;
      lastFV = f.invoke(vv);
    } else {
      lastVValid = false;
      lastV = null;
      lastFV = null;
    }
    return rv;
  }

  @SuppressWarnings("unchecked")
  public Object reduce(IFn rfn, Object acc) {
    final Iterator iter = this.iter;
    final IFn ff = f;
    final Object ffv = fv;
    if(!hasNext()) return acc;
    Object vv = lastV;
    Object fvv = lastFV;
    do {
      acc = rfn.invoke(acc, vv);
      if(RT.isReduced(acc)) {
	advance();
	return ((IDeref)acc).deref();
      }
      if(!iter.hasNext()) {
	advance();
	return acc;
      }
      vv = iter.next();
      fvv = ff.invoke(vv);
    } while(ffv == fvv || pred.test(ffv, fvv));
    lastVValid = true;
    lastV = vv;
    lastFV = fvv;
    return acc;
  }

  @SuppressWarnings("unchecked")
  public boolean hasNext() {
    return lastVValid && (fv == lastFV || pred.test(fv, lastFV));
  }
  public Object next() {
    if(!lastVValid) throw new NoSuchElementException();
    return advance();
  }
  public ISeq seq() { return LazyChunkedSeq.chunkIteratorSeq(this); }
  public Object deref() {
    if(hasNext())
      reduce(new IFnDef() { public Object invoke(Object acc, Object v) { return v; } }, null);
    return lastVValid ? ImmutList.create(true, null, lastV, lastFV) : null;
  }
  public IPersistentMap meta() {
    return new PersistentArrayMap(new Object[] { Keyword.intern("fv"), fv });
  }
}

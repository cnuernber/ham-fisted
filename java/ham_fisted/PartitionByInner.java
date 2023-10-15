package ham_fisted;


import java.util.Iterator;
import java.util.NoSuchElementException;
import clojure.lang.IFn;
import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.IDeref;
import clojure.lang.Util;
import clojure.lang.RT;



public class PartitionByInner implements ITypedReduce, Iterator, Seqable, IDeref {
  public final Iterator iter;
  public final IFn f;
  public final Object fv;
  boolean lastVValid;
  Object lastV;
  Object lastFV;

  public PartitionByInner(Iterator i, IFn f, Object v) {
    this.iter = i;
    this.f = f;
    this.lastV = v;
    this.lastVValid = true;
    this.fv = f.invoke(v);
    this.lastFV = fv;
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
    } while(ffv == fvv || CljHash.equiv(ffv, fvv));
    lastVValid = true;
    lastV = vv;
    lastFV = fvv;
    return acc;
  }
  public boolean hasNext() {
    return lastVValid && (fv == lastFV || CljHash.equiv(fv, lastFV));
  }
  public Object next() {
    if(!lastVValid) throw new NoSuchElementException();
    return advance();
  }
  public ISeq seq() { return RT.chunkIteratorSeq(this); }
  public Object deref() {
    if(hasNext())
      reduce(new IFnDef() { public Object invoke(Object acc, Object v) { return v; } }, null);
    return lastVValid ? ImmutList.create(true, null, lastV, lastFV) : null;
  }
}

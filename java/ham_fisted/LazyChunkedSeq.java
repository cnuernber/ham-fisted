package ham_fisted;


import clojure.lang.IFn;
import clojure.lang.IChunkedSeq;
import clojure.lang.IChunk;
import clojure.lang.ASeq;
import clojure.lang.Util;
import clojure.lang.PersistentList;
import clojure.lang.ISeq;
import clojure.lang.IPersistentMap;
import clojure.lang.ChunkedCons;
import clojure.lang.ArrayChunk;
import java.util.Iterator;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LazyChunkedSeq extends ASeq implements IChunkedSeq {
  IFn fn;
  IChunkedSeq mySeq;
  Throwable e;
  volatile Lock lock;
  public LazyChunkedSeq(IFn fn, IChunkedSeq s, Throwable e, IPersistentMap meta) {
    super(meta);
    this.fn = fn;
    this.mySeq = s;
    this.e = e;
    this.lock = new ReentrantLock();
  }
  public LazyChunkedSeq(IFn fn) {
    this(fn, null, null, null);
  }
  public LazyChunkedSeq withMeta(IPersistentMap m) { return new LazyChunkedSeq(fn, mySeq, e, m); }
  IChunkedSeq unlockedUnwrap() {
    if(fn != null) {
      try {
	mySeq = (IChunkedSeq)fn.invoke();
      } catch (Exception e) {
	this.e = e;
      }
      fn = null;
      lock = null;
    }
    if(this.e != null) throw Util.sneakyThrow(e);
    return mySeq;
  }
  IChunkedSeq lockedUnwrap() {
    if(mySeq != null) return mySeq;
    Lock l = lock;
    if(l != null) {
      l.lock();
      try {
	return unlockedUnwrap();
      }finally {
	l.unlock();
      }
    }
    return unlockedUnwrap();
  }
  public Object first() {
    IChunkedSeq s = lockedUnwrap();
    return s != null ? s.first() : null;
  }
  public ISeq next() {
    IChunkedSeq s = lockedUnwrap();
    return s != null ? s.next() : null;
  }
  public ISeq more() {
    ISeq rv = next();
    return rv != null ? rv : PersistentList.EMPTY;
  }
  public IChunk chunkedFirst() {
    IChunkedSeq s = lockedUnwrap();
    return s != null ? s.chunkedFirst() : null;
  }
  public ISeq chunkedNext() {
    IChunkedSeq s = lockedUnwrap();
    return s != null ? s.chunkedNext() : null;
  }
  public ISeq chunkedMore() {
    ISeq rv = chunkedNext();
    return rv != null ? rv : PersistentList.EMPTY;
  }

  public static IChunkedSeq chunkIteratorSeq(Iterator i) {
    if(i.hasNext()) {
      return new LazyChunkedSeq(new IFnDef() {
	  public Object invoke() {
	    Object[] ar = new Object[32];
	    int idx;
	    for(idx = 0; idx < 32 && i.hasNext(); ++idx)
	      ar[idx] = i.next();
	    return new ChunkedCons(new ArrayChunk(ar, 0, idx), chunkIteratorSeq(i));
	  }
	});
    } else {
      return null;
    }
  }
}

package ham_fisted;

import clojure.lang.Sequential;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import clojure.lang.RT;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class BatchReducer implements IFnDef.O, ITypedReduce, Sequential, Iterable {
  final IFn batchSrc;
  List batchData;
  int idx;
  int nElems;

  public BatchReducer(IFn batchSrc) {
    this.batchSrc = batchSrc;
    batchData = null;
    idx = 0;
    nElems = 0;
  }
  public Object reduce(IFn rfn, Object acc) {
    List bd = batchData;
    int ix = idx;
    int ne = nElems;
    do {
      if(ix == ne) {
	bd = (List)batchSrc.invoke();
	ix = 0;
	ne = bd.size();
      }
      acc = rfn.invoke(acc, bd.get(ix++));
    } while(!RT.isReduced(acc));
    batchData = bd;
    idx = ix;
    nElems = ne;
    return ((IDeref)acc).deref();
  }
  public Object invoke() {
    if(idx == nElems) {
      batchData = (List)batchSrc.invoke();
      idx = 0;
      nElems = batchData.size();
    }
    return batchData.get(idx++);
  }
  public Iterator iterator() {
    final IFn invoker = this;
    return new Iterator() {
      public boolean hasNext() { return true; }
      public Object next() { return invoker.invoke(); }
    };
  }
  @SuppressWarnings("unchecked")
  public void forEach(Consumer c) { ITypedReduce.super.forEach(c); }
}

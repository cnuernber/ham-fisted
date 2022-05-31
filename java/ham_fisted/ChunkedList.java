package ham_fisted;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static ham_fisted.IntegerOps.*;

import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.ISeq;
import clojure.lang.IteratorSeq;



class ChunkedList {
  Object[][] data;
  //Global capacity ignoring offset
  int capacity;
  //nElems for this sub-chunk - true nElems is nElems + offset
  int nElems;

  final IPersistentMap meta;

  public static final int CHUNKSIZE = 32;
  static final int CM1 = CHUNKSIZE - 1;

  static final int numChunks(int capacity) {
    return (capacity + CM1)/CHUNKSIZE;
  }

  static final int lastChunkSize(int capacity) {
    if (capacity == 0)
      return 4;
    final int leftover = capacity % CHUNKSIZE;
    if (leftover == 0)
      return CHUNKSIZE;
    return Math.max(4, nextPow2(leftover));
  }

  public ChunkedList(int initSize) {
    initSize = Math.max(initSize, 4);
    final int nChunks = numChunks(initSize);
    final int leftover = lastChunkSize(initSize);
    data = new Object[nChunks][];
    final int nnc = nChunks - 1;
    for (int idx = 0; idx < nChunks; ++idx)
      data[idx] = new Object[idx == nnc ? leftover : CHUNKSIZE];

    nElems = 0;
    capacity = (CHUNKSIZE * (nChunks-1)) + leftover;
    meta = null;
  }

  public ChunkedList() {this(0);}


  ChunkedList(ChunkedList other, boolean shallow) {
    if (shallow) {
      data = other.data;
    } else {
      final Object[][] odata = other.data;
      final Object[][] mdata = odata.clone();
      final int ne = mdata.length;
      for (int idx = 0; idx < ne; ++idx)
	mdata[idx] = odata[idx].clone();
      data = mdata;
    }
    capacity = other.capacity;
    nElems = other.nElems;
    meta = other.meta;
  }

  void clear(int offset, int len) {
    final Object[][] mdata = data;
    final int nChunks = data.length;
    final int ne = len;
    final int le = ne + offset;
    int idx = offset;
    while(idx < le) {
      final Object[] chunk = mdata[idx / CHUNKSIZE];
      final int eidx = idx % CHUNKSIZE;
      Arrays.fill(chunk, eidx, chunk.length - eidx, null);
      idx += CHUNKSIZE - eidx;
    }
    nElems = 0;
  }

  void clear() { clear(0, nElems); }

  void enlarge(int cap) {
    if (cap <= capacity) return;

    final int nChunks = numChunks(cap);
    final int nnc = nChunks -1;
    final int leftover = lastChunkSize(cap);
    // System.out.println("leftover: " + String.valueOf(leftover) + " Requested Capacity: " +
    // 		       String.valueOf(cap));
    Object[][] mdata = data;
    if (nChunks != mdata.length)
      mdata = Arrays.copyOf(mdata, nChunks);

    for(int idx = 0; idx < nChunks; ++idx) {
      final Object[] existing = mdata[idx];
      if (existing == null || existing.length != CHUNKSIZE) {
	final int targetLen = idx == nnc ? leftover : CHUNKSIZE;
	final int exLen = existing == null ? 0 : existing.length;
	if (exLen != targetLen) {
	  mdata[idx] = existing == null ? new Object[targetLen] : Arrays.copyOf(existing, targetLen);
	}
      }
    }
    data = mdata;
    capacity = (CHUNKSIZE * (nChunks-1)) + leftover;
  }

  final Object setValueRV(int idx, Object obj) {
    final Object[] ary = data[idx / CHUNKSIZE];
    final int eidx = idx % CHUNKSIZE;
    final Object rv = ary[eidx];
    ary[eidx] = obj;
    return rv;
  }
  final void setValue(int idx, Object obj) {
    data[idx / CHUNKSIZE][idx % CHUNKSIZE] = obj;
  }
  final Object getValue(int idx) {
    return data[idx / CHUNKSIZE][idx % CHUNKSIZE];
  }

  final boolean add(Object obj) {
    final int ne = nElems;
    final int cap = capacity;
    // System.out.println("Capacity: " + String.valueOf(cap) + " ne: " + String.valueOf(ne));
    if (ne >= cap)
      enlarge(cap+1);

    data[nElems/32][nElems%32] = obj;
    nElems = ne + 1;
    return true;
  }

  final void widen(final int startidx, final int endidx) {
    final int wne = endidx - startidx;
    if (wne == 0) return;
    final int ne = nElems;
    final int cap = capacity;
    enlarge(ne + wne);
    int copyNe = ne - startidx;
    final Object[][] mdata = data;
    //Copy contiguous sections starting from the end so we
    //do not overwrite elements we later need to move.
    while(copyNe > 0) {
      //Get the last valid index to move data into for start/end blocks
      final int sidx = startidx + copyNe - 1;
      final int eidx = endidx + copyNe - 1;
      //Find chunks related to those indexes.
      Object[] srcc = mdata[sidx / 32];
      Object[] endc = mdata[eidx / 32];
      //Find the relative end indexes in the blocks
      final int srceidx = sidx % 32;
      final int endeidx = eidx % 32;
      final int copyLen = Math.min(copyNe, Math.min(srceidx+1, endeidx+1));
      // System.out.println("Widen - srceidx: " + String.valueOf(srceidx)
      // 			 + " - endeidx: " + String.valueOf(endeidx)
      // 			 + " - copyNe: " + String.valueOf(copyNe)
      // 			 + " - copyLen: " + String.valueOf(copyLen));
      System.arraycopy(srcc, srceidx - copyLen + 1,
		       endc, endeidx - copyLen + 1,
		       copyLen);
      copyNe -= copyLen;
    }
    nElems = ne + wne;
  }

  final void shorten(int startidx, int endidx) {
    if(startidx == 0 && endidx == nElems) {
      clear();
      return;
    }
    final int ne = nElems;
    final int wne = endidx - startidx;
    int copyNe = ne - endidx;
    final Object[][] mdata = data;
    while(copyNe > 0) {
      final Object[] startc = data[startidx/32];
      final Object[] endc = data[endidx/32];
      final int seidx = startidx % 32;
      final int eeidx = endidx % 32;
      int copyLen = Math.min(copyNe, Math.min(startc.length - seidx,
					      endc.length - eeidx));
      // System.out.println("Shorten - startidx: " + String.valueOf(startidx)
      // 			 + " - endidx: " + String.valueOf(endidx)
      // 			 + " - copyNe: " + String.valueOf(copyNe)
      // 			 + " - copyLen: " + String.valueOf(copyLen));
      System.arraycopy(endc, eeidx, startc, seidx, copyLen);
      copyNe -= copyLen;
      startidx += copyLen;
      endidx += copyLen;
    }
    //Zero out remaining blocks to ensure we don't hold onto any object references.
    clear(startidx, wne);
    nElems = ne - wne;
  }

  //Extremely inefficent operation.  Make another list and insert the list all at once.
  final void add(Object obj, int idx) {
    final int ne = nElems;

    if (idx > ne)
      throw new RuntimeException("Index out of range: " + String.valueOf(idx) +
				 " > " + String.valueOf(ne));
    if (idx == ne) {
      add(obj);
      return;
    }

    widen(idx, idx+1);
    setValue(idx,obj);
  }

  final ChunkedList conj(Object obj) {
    ChunkedList retval = new ChunkedList(this, true);
    final int ne = nElems;
    final int cap = capacity;
    final int cidx = ne / CHUNKSIZE;
    Object[][] mdata;
    if (ne >= cap) {
      retval.enlarge(cap+1);
      mdata = retval.data;
    } else {
      mdata = retval.data.clone();
      mdata[cidx] = mdata[cidx].clone();
      retval.data = mdata;
    }
    mdata[cidx][ne % CHUNKSIZE] = obj;
    retval.nElems = ne+1;
    return retval;
  }

  final ChunkedList assoc(int idx, Object obj) {
    if (idx == nElems)
      return conj(obj);
    ChunkedList retval = new ChunkedList(this, true);
    final int cidx = idx / CHUNKSIZE;
    final int eidx = idx % CHUNKSIZE;
    final Object[][] mdata = retval.data.clone();
    retval.data = mdata;
    Object[] edata = mdata[cidx].clone();
    edata[eidx] = obj;
    mdata[cidx] = edata;
    return retval;
  }

  final int size() { return nElems; }

  final Object[] fillArray(int startidx, int endidx, Object[] retval) {
    final int finalCidx = endidx / 32;
    final int finalEidx = endidx % 32;
    int cidx = startidx / 32;
    int eidx = startidx % 32;
    final Object[][] mdata = data;
    int dstOff = 0;
    while(cidx <= finalCidx) {
      final int copyLen = cidx == finalCidx ? finalEidx - eidx : 32 - eidx;
      System.arraycopy(mdata[cidx], eidx, retval, dstOff, copyLen);
      dstOff += copyLen;
      eidx = 0;
      ++cidx;
    }
    return retval;
  }

  final void fillRange(int startidx, int endidx, Object v) {
    final Object[][] mdata = data;
    for(; startidx < endidx; ++startidx)
      mdata[startidx/32][startidx%32] = v;
  }

  final void fillRange(int startidx, List v) {
    final Object[][] mdata = data;
    final int endidx = startidx + v.size();
    int idx = 0;
    for(; startidx < endidx; ++startidx, ++idx)
      mdata[startidx/32][startidx%32] = v.get(idx);
  }

  final void addRange(int startidx, int endidx, Object v) {
    widen(startidx, endidx);
    final Object[][] mdata = data;
    for(; startidx < endidx; ++startidx)
      mdata[startidx/32][startidx%32] = v;
  }

  final Object[] fillArray(Object[] retval) {
    return fillArray(0, nElems, retval);
  }

  final Object[] toArray(int startidx, int endidx) {
    return fillArray(startidx, endidx, new Object[endidx - startidx]);
  }

  final Object[] toArray() {
    return toArray(0, nElems);
  }

  static class CLIter implements Iterator {
    final int finalCidx;
    final int finalEidx;
    final Object[][] data;
    int cidx;
    int eidx;
    Object[] chunk;
    public CLIter(int startidx, int endidx, Object[][] _data) {
      finalCidx = endidx / 32;
      finalEidx = endidx % 32;
      cidx = startidx / CHUNKSIZE;
      eidx = (startidx % CHUNKSIZE) - 1;
      data = _data;
      advance();
    }
    final void advance() {
      ++eidx;
      if (eidx == CHUNKSIZE) {
	++cidx;
	eidx = 0;
	chunk = null;
      }
    }
    public final boolean hasNext() {
      if(cidx < finalCidx)
	return true;
      return eidx < finalEidx;
    }
    public final Object next() {
      if (chunk == null)
	chunk = data[cidx];
      final Object retval = chunk[eidx];
      advance();
      return retval;
    }
  }

  public Iterator iterator(int startidx, int endidx) {
    return new CLIter(startidx, endidx, data);
  }

  public Iterator iterator() {
    return iterator(0, nElems);
  }

  interface ChunkedListOwner {
    public ChunkedList getChunkedList();
  }

  Object reduce(int startidx, int endidx, IFn f, Object start) {
    final Object[][] mdata = data;
    Object ret = f.invoke(start, getValue(startidx));
    for(int x = startidx + 1; x < endidx; x++) {
      if (RT.isReduced(ret))
	return ((IDeref)ret).deref();
      ret = f.invoke(ret, mdata[x/32][x%32]);
    }
    if (RT.isReduced(ret))
      return ((IDeref)ret).deref();
    return ret;
  }

  Object reduce(int startidx, int endidx, IFn f) {
    if(startidx == endidx)
      return f.invoke();
    return reduce(startidx+1, endidx, f, getValue(startidx));
  }

  Object kvreduce(int startidx, int endidx, int idxoff, IFn f, Object init) {
    final Object[][] mdata = data;
    for (int i=startidx; i<endidx; i++) {
      init = f.invoke(init, i + idxoff, mdata[i/32][i%32]);
      if (RT.isReduced(init))
	return ((IDeref)init).deref();
    }
    return init;
  }

  ISeq seq(int startidx, int endidx) {
    return IteratorSeq.create(iterator(startidx, endidx));
  }
}

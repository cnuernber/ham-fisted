package ham_fisted;


import java.util.Arrays;
import java.util.Iterator;
import static ham_fisted.IntegerOps.*;

import clojure.lang.IPersistentMap;



class ChunkedList {
  Object[][] data;
  //Global capacity ignoring offset
  int capacity;
  //nElems for this sub-chunk - true nElems is nElems + offset
  int nElems;
  //Implementing subList/subVector
  final int offset;
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
    return Math.min(4, nextPow2(leftover));
  }

  public ChunkedList(int initSize) {
    initSize = Math.min(initSize, 4);
    final int nChunks = numChunks(initSize);
    final int leftover = lastChunkSize(initSize);
    data = new Object[nChunks][];
    final int nnc = nChunks - 1;
    for (int idx = 0; idx < nChunks; ++idx)
      data[idx] = new Object[idx == nnc ? leftover : CHUNKSIZE];

    nElems = 0;
    offset = 0;
    capacity = (CHUNKSIZE * nChunks) + leftover;
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
    offset = other.offset;
  }

  ChunkedList(ChunkedList other, int off, int endoff) {
    data = other.data;
    capacity = other.capacity;
    nElems = off - endoff;
    meta = other.meta;
    offset = other.offset + off;
  }

  void clear() {
    final Object[][] mdata = data;
    final int nChunks = data.length;
    final int ne = nElems;
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

  void enlarge(int cap) {
    if (cap <= capacity) return;

    final int nChunks = numChunks(cap);
    final int nnc = nChunks -1;
    final int leftover = lastChunkSize(cap);
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
    capacity = (CHUNKSIZE * nChunks) + leftover;
  }
  final Object setValueRV(int idx, Object obj) {
    idx = idx + offset;
    final Object[] ary = data[idx / CHUNKSIZE];
    final int eidx = idx % CHUNKSIZE;
    final Object rv = ary[eidx];
    ary[eidx] = obj;
    return rv;
  }
  final void setValue(int idx, Object obj) {
    idx = idx + offset;
    data[idx / CHUNKSIZE][idx % CHUNKSIZE] = obj;
  }
  final Object getValue(int idx) {
    idx = idx + offset;
    return data[idx / CHUNKSIZE][idx % CHUNKSIZE];
  }
  final boolean add(Object obj) {
    final int ne = nElems;
    final int cap = capacity;
    if ((ne+offset) >= cap)
      enlarge(cap+1);

    setValue(ne, obj);
    nElems = ne + 1;
    return true;
  }

  //Extremely inefficent operation.  Make another list and insert the list all at once.
  final void add(Object obj, int idx) {
    final int ne = nElems;

    if (idx > ne)
      throw new RuntimeException("Index out of range: " + String.valueOf(idx) +
				 " > " + String.valueOf(ne));
    if (idx == ne)
      add(obj);

    idx = idx + offset;
    final int cidx = idx / CHUNKSIZE;
    final int eidx = idx % CHUNKSIZE;
    final int cap = capacity;
    if ((ne+offset) >= cap)
      enlarge(cap+1);
    final Object[][] mdata = data;
    final int nChunks = mdata.length;
    final int leftover = lastChunkSize(ne);
    final int nnc = nChunks - 1;
    Object temp = obj;
    for (int chunk = cidx; chunk < nChunks; ++chunk) {
      final Object[] cdata = mdata[chunk];
      final int ce = cdata.length;
      final Object stored = chunk == cidx ? cdata[eidx] : cdata[ce-1];
      final Object[] ndata = new Object[Math.min(CHUNKSIZE, ce+1)];
      final int copyStart = chunk == cidx ? eidx : 0;
      System.arraycopy(cdata, copyStart,
		       cdata, copyStart+1,
		       ce - copyStart - 1);
      cdata[copyStart] = temp;
      temp = cdata[ce-1];
    }
    nElems = ne + 1;
  }

  final ChunkedList conj(Object obj) {
    ChunkedList retval = new ChunkedList(this, true);
    final int ne = nElems;
    final int lne = ne + offset;
    final int cap = capacity;
    final int cidx = lne / CHUNKSIZE;
    Object[][] mdata;
    if (lne >= cap) {
      retval.enlarge(cap+1);
      mdata = retval.data;
    } else {
      mdata = retval.data.clone();
      mdata[cidx] = mdata[cidx].clone();
      retval.data = mdata;
    }
    mdata[cidx][lne % CHUNKSIZE] = obj;
    retval.nElems = ne+1;
    return retval;
  }

  final ChunkedList assoc(int idx, Object obj) {
    if (idx == nElems)
      return conj(obj);
    ChunkedList retval = new ChunkedList(this, true);
    idx = idx + offset;
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

  final Object[] fillArray(Object[] retval) {
    final int ne = nElems;
    final int lne = ne + offset;
    final int nChunks = numChunks(lne);
    final int nnc = nChunks-1;
    final int leftover = lastChunkSize(lne);
    final Object[][] mdata = data;
    final int startChunk = offset / CHUNKSIZE;
    int dstOff = 0;
    int cidx = startChunk;
    while(cidx < nChunks) {
      int copyLen = cidx == nnc ? leftover :
	cidx == startChunk ? CHUNKSIZE - (offset % CHUNKSIZE) : CHUNKSIZE;
      System.arraycopy(mdata[cidx], cidx == startChunk ? offset : 0,
		       retval, dstOff, copyLen);
      dstOff += copyLen;
      ++cidx;
    }
    return retval;
  }

  final Object[] toArray() {
    return fillArray(new Object[nElems]);
  }

  static class CLIter implements Iterator {
    final int finalCidx;
    final int finalEidx;
    final Object[][] data;
    int cidx;
    int eidx;
    Object[] chunk;
    public CLIter(int offset, int nElems, Object[][] _data) {
      final int lne = nElems + offset;
      finalCidx = numChunks(lne);
      finalEidx = lastChunkSize(lne);
      cidx = offset / CHUNKSIZE;
      eidx = (offset % CHUNKSIZE) - 1;
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

  public Iterator iterator() {
    final int ne = nElems;
    return new CLIter(offset, nElems, data);
  }

  interface ChunkedListOwner {
    public ChunkedList getChunkedList();
  }
}

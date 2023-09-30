package ham_fisted;

import static ham_fisted.IntegerOps.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Objects;

import clojure.lang.IPersistentMap;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.ISeq;
import clojure.lang.IteratorSeq;
import clojure.lang.Util;
import clojure.lang.Murmur3;
import clojure.lang.Reduced;


public final class ChunkedList {
  Object[][] data;
  //Global capacity ignoring offset
  int capacity;
  //nElems for this sub-chunk - true nElems is nElems + offset
  int nElems;

  final IPersistentMap meta;

  static final int numChunks(int capacity) {
    return (capacity + 31)/32;
  }

  static final int lastChunkSize(int capacity) {
    if (capacity == 0)
      return 4;
    final int leftover = capacity % 32;
    if (leftover == 0)
      return 32;
    return Math.max(4, nextPow2(leftover));
  }

  public ChunkedList(int initSize) {
    initSize = Math.max(initSize, 16);
    final int nChunks = numChunks(initSize);
    final int leftover = lastChunkSize(initSize);
    data = new Object[nChunks][];
    final int nnc = nChunks - 1;
    for (int idx = 0; idx < nChunks; ++idx)
      data[idx] = new Object[idx == nnc ? leftover : 32];

    nElems = 0;
    capacity = (32 * (nChunks-1)) + leftover;
    meta = null;
  }

  public ChunkedList() {this(0);}

  ChunkedList(Object[][] d, int c, int e, IPersistentMap m) {
    data = d;
    capacity = c;
    nElems = e;
    meta = m;
  }

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

  static ChunkedList create(boolean owning, IPersistentMap meta, Object... data) {
    final int dlen = data.length;
    if (owning && dlen <= 32) {
      return new ChunkedList(new Object[][] { data }, dlen, dlen, null);
    } else {
      final int nElems = data.length;
      final int nChunks = numChunks(nElems);
      final Object[][] mdata = new Object[nChunks][];
      int idx = 0;
      while(idx < nElems) {
	final int clen = Math.min(32, nElems - idx);
	final Object[] chunk = new Object[clen];
	System.arraycopy(data, idx, chunk, 0, clen);
	mdata[idx/32] = chunk;
	idx += clen;
      }
      return new ChunkedList(mdata, nElems, nElems, meta);
    }
  }

  ChunkedList clone(int startidx, int endidx, int extraAlloc, boolean deep) {
    final int ne = endidx - startidx;
    final int nne = ne + extraAlloc;
    final boolean shallow = deep == false && ((startidx % 32) == 0);
    final Object[][] mdata = data;
    if(shallow) {
      final Object[][] odata = Arrays.copyOfRange(mdata,
						  startidx/32,
						  (endidx + extraAlloc + 31)/32);
      return new ChunkedList(odata, nne, nne, meta);
    }
    final int nChunks = numChunks(nne);
    final int nnc = nChunks - 1;
    final Object[][] retval = new Object[nChunks][];
    final int sidx = startidx;
    int dstCapacity = 0;
    while(startidx < endidx) {
      final int leftover = endidx - startidx;
      final Object[] srcc = mdata[startidx/32];

      final int eidx = startidx % 32;
      final int deidx = (startidx - sidx) % 32;
      final int dcidx = (startidx - sidx) / 32;
      Object[] dstc = retval[dcidx];
      if (dstc == null) {
	dstc = dcidx == nnc ? new Object[nne%32] : new Object[32];
	retval[dcidx] = dstc;
	dstCapacity += dstc.length;
      }
      final int copyLen = Math.min(leftover, Math.min(srcc.length - eidx,
						      dstc.length - deidx));
      // System.out.println("srcc: " + String.valueOf(srcc.length) + " eidx: " + String.valueOf(eidx) +
      // 			 " dstc: " + String.valueOf(dstc.length) + " deidx: " + String.valueOf(deidx) +
      // 			 " copyLen: " + String.valueOf(copyLen));
      System.arraycopy(srcc, eidx, dstc, deidx, copyLen);
      startidx += copyLen;
    }
    return new ChunkedList(retval, dstCapacity, nne, meta);
  }

  ChunkedList clone(int startidx, int endidx) { return clone(startidx, endidx, 0, true); }

  void clear(int offset, int len) {
    if (offset == 0 && len == nElems) {
      final Object[][] mdata = data;
      final int nChunks = data.length;
      final int ne = nElems;
      int idx = 0;
      while(idx < ne) {
	final Object[] chunk = mdata[idx / 32];
	final int eidx = idx % 32;
	Arrays.fill(chunk, eidx, chunk.length - eidx, null);
	idx += 32 - eidx;
      }
      nElems = 0;
    } else {
      shorten(offset, offset+len);
    }
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
      if (existing == null || existing.length != 32) {
	final int targetLen = idx == nnc ? leftover : 32;
	final int exLen = existing == null ? 0 : existing.length;
	if (exLen != targetLen) {
	  mdata[idx] = existing == null ? new Object[targetLen] : Arrays.copyOf(existing, targetLen);
	}
      }
    }
    data = mdata;
    capacity = (32 * (nChunks-1)) + leftover;
  }

  final Object setValueRV(int idx, Object obj) {
    final Object[] ary = data[idx / 32];
    final int eidx = idx % 32;
    final Object rv = ary[eidx];
    ary[eidx] = obj;
    return rv;
  }
  final void setValue(int idx, Object obj) {
    data[idx / 32][idx % 32] = obj;
  }
  final Object getValue(final int idx) {
    return data[idx / 32][idx % 32];
  }

  public static final void sublistCheck(long sidx, long eidx, long nElems) {
    if(sidx < 0 || sidx > nElems)
      throw new IndexOutOfBoundsException("Start index out of range: start-index("
					  + String.valueOf(sidx) +"), n-elems("
					  + String.valueOf(nElems) + ")");
    if(eidx < 0 || eidx > nElems)
      throw new IndexOutOfBoundsException("End index out of range: end-index("
					  + String.valueOf(eidx) +"), n-elems("
					  + String.valueOf(nElems) + ")");
    if(eidx < sidx)
      throw new IndexOutOfBoundsException("End index underflow: end-index("
					  + String.valueOf(eidx) +") < start-index("
					  + String.valueOf(sidx) + ")");
  }

  public static final int indexCheck(int nElems, int idx) {
    if (idx < 0 || idx >= nElems)
      throw new IndexOutOfBoundsException("Index " + String.valueOf(idx) + "out of range 0-" + String.valueOf(nElems));
    return idx;
  }

  public static final int indexCheck(int startidx, int nElems, int idx) {
    return indexCheck(nElems,idx) + startidx;
  }


  public static final long indexCheck(long startidx, long nElems, long idx) {
    if (idx < 0 || idx >= nElems)
      throw new IndexOutOfBoundsException("Index " + String.valueOf(idx) + "out of range 0-" + String.valueOf(nElems));
    return idx + startidx;
  }

  static final int wrapIndexCheck(int startidx, int nElems, int idx) {
    if (idx < 0)
      idx = nElems + idx;
    return indexCheck(startidx, nElems, idx);
  }


  static public final void checkIndexRange(int startidx, int nElems, int sidx, int eidx) {
    final int rne = eidx - sidx;
    if(rne == 0 )
      return;
    indexCheck(startidx, nElems, sidx);
    if (rne < 0)
      throw new RuntimeException("Range end: " + String.valueOf(eidx)
				 + " is less than start: " + String.valueOf(sidx));
    if(eidx > nElems)
      throw new RuntimeException("Range end point: " + String.valueOf(eidx)
				 + " is past end of valid range: " +
				 String.valueOf(nElems));
  }

  static public final void checkIndexRange(long startidx, long nElems, long sidx, long eidx) {
    final long rne = eidx - sidx;
    if(rne == 0 )
      return;
    indexCheck(startidx, nElems, sidx);
    if (rne < 0)
      throw new RuntimeException("Range end: " + String.valueOf(eidx)
				 + " is less than start: " + String.valueOf(sidx));
    if(eidx > nElems)
      throw new RuntimeException("Range end polong: " + String.valueOf(eidx)
				 + " is past end of valid range: " +
				 String.valueOf(nElems));
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

  final ChunkedList conj(int startidx, int endidx, Object obj) {
    ChunkedList retval = clone(startidx, endidx, 1, false);
    final int nc = endidx - startidx;
    final Object[][] mdata = retval.data;
    final int cidx = nc/32;
    Object[] chunk = mdata[cidx];
    final int eidx = nc % 32;
    if (chunk == null) {
      chunk = new Object[1];
    } else {
      chunk = Arrays.copyOf(chunk, eidx+1);
    }
    mdata[cidx] = chunk;
    chunk[eidx] = obj;
    return retval;
  }

  final ChunkedList assoc(int startidx, int endidx, int idx, Object obj) {
    final int nc = endidx - startidx;
    if (idx == nc)
      return conj(startidx, endidx, obj);
    ChunkedList retval = clone(startidx, endidx, 0, false);
    final int cidx = idx / 32;
    final int eidx = idx % 32;
    final Object[][] mdata = retval.data;
    Object[] edata = mdata[cidx].clone();
    edata[eidx] = obj;
    mdata[cidx] = edata;
    return retval;
  }

  final ChunkedList pop(int startidx, int endidx) {
    ChunkedList retval = clone(startidx, endidx-1, 0, false);
    final int nc = endidx - startidx;
    final Object[][] rdata = retval.data;
    final int cidx = nc/32;
    if ( rdata.length > cidx ) {
      final Object[] c = rdata[cidx];
      final int eidx = nc % 32;
      if (c.length > eidx) {
	rdata[cidx] = Arrays.copyOf(c, eidx);
      }
    }
    return retval;
  }

  final int size() { return nElems; }

  final void fillRange(int startidx, int endidx, Object v) {
    final Object[][] mdata = data;
    for(; startidx < endidx; ++startidx)
      mdata[startidx/32][startidx%32] = v;
  }

  final void fillRangeReduce(final int startidx, Object v) {
    final Object[][] mdata = data;
    Reductions.serialReduction(new Reductions.IndexedAccum(new IFnDef.OLOO() {
	public Object invokePrim(Object acc, long idx, Object v) {
	  final int ss = (int)idx+startidx;
	  ((Object[][])acc)[ss/32][ss%32] = v;
	  return acc;
	}
      }), mdata, v);
  }

  final void addRange(int startidx, int endidx, Object v) {
    widen(startidx, endidx);
    final Object[][] mdata = data;
    for(; startidx < endidx; ++startidx)
      mdata[startidx/32][startidx%32] = v;
  }

  final Object[] fillArray(int startidx, int endidx, Object[] retval) {
    final int finalCidx = endidx / 32;
    final int finalEidx = endidx % 32;
    int cidx = startidx / 32;
    int eidx = startidx % 32;
    final Object[][] mdata = data;
    int dstOff = 0;
    while(cidx <= finalCidx) {
      final int copyLen = cidx == finalCidx ? finalEidx - eidx : 32 - eidx;
      //In the case where the end idx falls exactly on a boundary we get a copyLen of 0 here.
      if(copyLen > 0) {
	System.arraycopy(mdata[cidx], eidx, retval, dstOff, copyLen);
	dstOff += copyLen;
      }
      eidx = 0;
      ++cidx;
    }
    return retval;
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
      cidx = startidx / 32;
      eidx = (startidx % 32) - 1;
      data = _data;
      advance();
    }
    final void advance() {
      ++eidx;
      if (eidx == 32) {
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
      if ( cidx >= finalCidx &&
	   eidx >= finalEidx )
	throw new NoSuchElementException();
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

  static class RIterator implements Iterator {
    final int startidx;
    final Object[][] data;
    int idx;
    public RIterator( int sidx, int eidx, Object[][] d) {
      startidx = sidx;
      idx = eidx;
      data = d;
      advance();
    }
    void advance() {
      --idx;
    }
    public final boolean hasNext() { return idx >= startidx; }
    public final Object next() {
      if (idx < startidx)
	throw new NoSuchElementException();
      final Object retval = data[idx/32][idx%32];
      advance();
      return retval;
    }
  }

  public Iterator riterator(int startidx, int endidx) {
    return new RIterator(startidx, endidx, data);
  }

  static class ListIter<E> implements ListIterator<E> {
    final int startidx;
    final int endidx;
    final Object[][] data;
    int idx;
    int prevIdx;

    ListIter(int sidx, int eidx, Object[][] d) {
      startidx = sidx;
      endidx = eidx;
      data = d;
      idx = sidx;
      prevIdx = idx;
    }
    public void add(E obj) { throw new RuntimeException("Unimplemented."); }
    public void remove() { throw new RuntimeException("Unimplemented."); }
    public final boolean hasNext() { return idx < endidx; }
    public final boolean hasPrevious() { return idx > startidx; }
    @SuppressWarnings("unchecked")
    public final E next() {
      if (idx == endidx)
	throw new NoSuchElementException();
      final Object retval = data[idx/32][idx%32];
      prevIdx = idx;
      ++idx;
      return (E)retval;
    }
    public final int nextIndex() {
      return idx - startidx;
    }
    @SuppressWarnings("unchecked")
    public final E previous() {
      --idx;
      if (idx < startidx)
	throw new NoSuchElementException();
      prevIdx = idx;
      return (E)data[idx/32][idx%32];
    }
    public final int previousIndex() {
      return idx - startidx - 1;
    }
    @SuppressWarnings("unchecked")
    public final void set(E v) {
      data[prevIdx/32][prevIdx%32] = v;
    }
  }

  <E> ListIterator<E> listIterator(int startidx, int endidx, E marker) {
    return new ListIter<E>(startidx, endidx, data);
  }

  static class ChunkedListSection {
    public final Object[][] data;
    public final int startidx;
    public final int endidx;
    public int size() { return endidx - startidx; }
    public ChunkedListSection(Object[][] cl, int sidx, int eidx) {
      data = cl;
      startidx = sidx;
      endidx = eidx;
    }
  }


  interface ChunkedListOwner {
    ChunkedListSection getChunkedList();
  }

  Object reduce(final int startidx, final int endidx, final IFn f, final Object start) {
    final Object[][] mdata = data;
    Object ret = start;
    int sidx = startidx;
    while(sidx < endidx && !RT.isReduced(ret)) {
      final Object[] cdata = mdata[sidx/32];
      int cstart = sidx % 32;
      final int clen = Math.min(endidx - sidx, 32 - cstart);
      final int cstop = clen + cstart;
      for(int idx = cstart; idx < cstop && !RT.isReduced(ret); ++idx)
	ret = f.invoke(ret, cdata[idx]);
      sidx += clen;
    }
    return Reductions.unreduce(ret);
  }

  Object reduce(final int startidx, final int endidx, IFn f) {
    if(startidx == endidx)
      return f.invoke();
    return reduce(startidx+1, endidx, f, getValue(startidx));
  }

  Object kvreduce(int startidx, int endidx, IFn f, Object init) {
    final Object[][] mdata = data;
    for (int i=startidx; i<endidx && !RT.isReduced(init); i++) {
      init = f.invoke(init, i - startidx, mdata[i/32][i%32]);
    }
    return init;
  }

  final ISeq seq(int startidx, int endidx) {
    return IteratorSeq.create(iterator(startidx, endidx));
  }

  final ISeq rseq(int startidx, int endidx) {
    return IteratorSeq.create(riterator(startidx, endidx));
  }

  final int hasheq(int startidx, int endidx) {
    int hash = 1;
    final int n = endidx - startidx;
    final Object[][] mdata = data;
    for( ; startidx < endidx; ++startidx) {
      hash = 31 * hash + Util.hasheq(mdata[startidx/32][startidx%32]);
    }
    hash = Murmur3.mixCollHash(hash, n);
    return hash;
  }
  final boolean equiv(int sidx, int eidx, Object o) {
    if(o instanceof RandomAccess) {
      List lo = (List)o;
      final int osz = lo.size();
      final int sz = eidx - sidx;
      if(osz != sz)
	return false;
      final int ee = eidx;
      int idx = 0;
      final Object[][] d = data;
      while(idx < sz) {
	final int midx = idx + sidx;
	final Object[] chunk = d[midx / 32];
	int cidx = midx % 32;
	final int chunkSize = Math.min(32 - cidx, sz - idx);
	final int endChunkIdx = idx + chunkSize;
	for(; idx < endChunkIdx; ++idx, ++cidx) {
	  final Object vv = chunk[cidx];
	  if(CljHash.equiv(vv, lo.get(idx)) == false)
	    return false;
	}
      }
      return true;
    } else if (o instanceof Iterable) {
      final Object[][] d = data;
      final int ss = sidx;
      return (Boolean)Reductions.serialReduction(new Reductions.IndexedAccum(new IFnDef.OLOO() {
	  public Object invokePrim(Object acc, long idx, Object v) {
	    final int iidx = (int)idx + ss;
	    if(iidx >= eidx)
	      return new Reduced(false);
	    final Object vv = d[iidx/32][iidx%32];
	    if(CljHash.equiv(vv, v) == false)
	      return new Reduced(false);
	    return acc;
	  }
	}), true, o);
    } else {
      return false;
    }
  }
  final IPersistentMap meta() { return meta; }
  final ChunkedList withMeta(IPersistentMap m) {
    return new ChunkedList(data, capacity, nElems, m);
  }
  final int indexOf(int startidx, int endidx, Object obj) {
    final int ne = endidx - startidx;
    final Object[][] mdata = data;
    for(int idx = 0; idx < ne; ++idx)
      if (Objects.equals(obj, mdata[idx/32][idx%32]))
	return idx;
    return -1;
  }
  final int lastIndexOf(int startidx, int endidx, Object obj) {
    final int ne = endidx - startidx;
    final int nne = ne - 1;
    final Object[][] mdata = data;
    for(int idx = 0; idx < ne; ++idx) {
      final int ridx = nne - idx + startidx;
      if (Objects.equals(obj, mdata[ridx/32][ridx%32]))
	return ridx;
    }
    return -1;
  }
  final boolean contains(int startidx, int endidx, Object obj) {
    return indexOf(startidx, endidx, obj) != -1;
  }

  final boolean containsAll(int startidx, int endidx, Collection<?> c) {
    final int ne = endidx - startidx;
    Iterator minC;
    Iterator maxC;
    if (ne < c.size()) {
      minC = this.iterator(startidx, endidx);
      maxC = c.iterator();
    } else {
      maxC = this.iterator(startidx, endidx);
      minC = c.iterator();
    }
    //This set can contain null.
    // HashSet<Object> hc = new HashSet<Object>();
    // while(minC.hasNext()) hc.add(minC.next());
    // while(maxC.hasNext()) {
    //   if (!hc.contains(maxC.next()))
    // 	return false;
    // }
    // return true;
    throw new UnsupportedOperationException();
  }
}

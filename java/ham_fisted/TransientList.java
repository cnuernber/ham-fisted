package ham_fisted;

import static ham_fisted.IntegerOps.*;

import clojure.lang.ITransientVector;
import clojure.lang.RT;
import clojure.lang.Util;
import java.util.BitSet;
import java.util.Arrays;


public class TransientList implements ITransientVector, IFnDef {
  final ChunkedList data;
  final BitSet ownedChunks;
  //Note there is no startidx.  We cannot share structure with sub-lists so startidx is
  //always 0.  Thus calling transient on a subvector results
  int nElems;
  public TransientList(ChunkedList _data, int _nElems, boolean _ownsEverything) {
    data = _data;
    nElems = _nElems;
    ownedChunks = new BitSet();
    if( _ownsEverything) {
      final int nChunks = ChunkedList.numChunks(nElems);
      for(int idx = 0; idx < nChunks; ++idx)
	ownedChunks.set(idx);
    }
  }
  final int indexCheck(int idx) {
    return ChunkedList.indexCheck(0, nElems, idx);
  }
  final int wrapIndexCheck(int idx) {
    return ChunkedList.wrapIndexCheck(0, nElems, idx);
  }
  public final int count() { return nElems; }
  public final int size() { return nElems; }
  public final int length() { return nElems; }
  public final Object nth(int idx) {
    return nth(idx, null);
  }
  public final Object nth(int idx, Object notFound) {
    if (idx < 0)
      idx += nElems;
    if (idx >= 0 && idx < nElems)
      return data.getValue(idx);
    return notFound;
  }
  public final Object invoke(Object idx) {
    return nth(RT.intCast(idx));
  }
  public final Object invoke(Object idx, Object nf) {
    return nth(RT.intCast(idx), nf);
  }
  public final Object valAt(Object idx) {
    if (Util.isInteger(idx))
      return nth(RT.intCast(idx));
    return null;
  }
  public final Object valAt(Object idx, Object notFound) {
    if (Util.isInteger(idx))
      return nth(RT.intCast(idx), notFound);
    return notFound;
  }
  public final TransientList assocN(int idx, Object v) {
    if (idx == nElems)
      return conj(v);
    indexCheck(idx);
    int cidx = idx/32;
    int eidx = idx%32;
    final Object[][] mdata = data.data;
    Object[] chunk = mdata[cidx];
    if (!ownedChunks.get(cidx)) {
      ownedChunks.set(cidx);
      chunk = chunk.clone();
      mdata[cidx] = chunk;
    }
    chunk[eidx] = v;
    return this;
  }
  public final TransientList assoc(Object obj, Object v) {
    if (!Util.isInteger(obj))
      throw new RuntimeException("Vectors must have integer indexes: " + String.valueOf(obj));
    return assocN(RT.intCast(obj), v);
  }
  public final TransientList conj(Object v) {
    final int idx = nElems++;
    final int cidx = idx / 32;
    final int eidx = idx % 32;
    Object[][] mdata = data.data;
    Object[] chunk;
    if (cidx == mdata.length) {
      mdata = Arrays.copyOf(mdata, cidx+1);
      chunk = new Object[4];
      mdata[cidx] = chunk;
      ownedChunks.set(cidx);
    } else {
      chunk = mdata[cidx];
      if ((!ownedChunks.get(cidx)) || chunk.length <= eidx) {
	chunk = Arrays.copyOf(chunk, nextPow2(eidx+1));
	mdata[cidx] = chunk;
	ownedChunks.set(cidx);
      }
    }
    chunk[eidx] = v;
    return this;
  }
  public final TransientList pop() {
    if(nElems == 0)
      throw new RuntimeException("Attempt to pop empty vector");
    final int idx = --nElems;
    final int cidx = idx / 32;
    final int eidx = idx % 32;
    Object[][] mdata = data.data;
    Object[] chunk = mdata[cidx];
    if (!ownedChunks.get(cidx)) {
      chunk = chunk.clone();
      ownedChunks.set(cidx);
      mdata[cidx] = chunk;
    }
    //Release any outstanding references.
    chunk[eidx] = null;
    return this;
  }
  public final ImmutList persistent() {
    return new ImmutList(0, nElems, data);
  }
}

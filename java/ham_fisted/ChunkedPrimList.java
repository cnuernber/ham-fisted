package ham_fisted;
import java.util.Arrays;
import clojure.lang.IFn;
import clojure.lang.RT;

public class ChunkedPrimList {
  public static class ChunkedLongList implements LongMutList {
    static final int chunkSize = 1024;
    long [][] data = new long[][] { new long[chunkSize] };
    int nElems;
    public ChunkedLongList(){}
    public void addLong(long lval) {
      int oidx = nElems / chunkSize;
      int iidx = nElems % chunkSize;
      long[] target;
      if(data.length == oidx) {
	int nlen = data.length+1;
	long[][] ndata = new long[nlen][];
	System.arraycopy(data, 0, ndata, 0, nlen-1);
	data = ndata;
	target = new long[chunkSize];
	data[oidx] = target;
      } else {
	target = data[oidx];
      }
      target[iidx] = lval;
      nElems++;
    }
    public long getLong(int idx) {
      ChunkedList.indexCheck(0, nElems, idx);
      return data[idx/chunkSize][idx%chunkSize];
    }
    public int size() { return nElems; }
    public Object longReduce(IFn.OLO rfn, Object acc) {
      int sz = size();
      int nOuter = (sz + (chunkSize - 1)) / chunkSize;
      for(int oidx = 0; oidx < nOuter; ++oidx) {
	long[] target = data[oidx];
	int nInner = Math.min(chunkSize, sz - (oidx * chunkSize));
	for(int iidx = 0; iidx < nInner; ++iidx ) {
	  acc = rfn.invokePrim(acc, target[iidx]);
	  if(RT.isReduced(acc))
	    return Reductions.unreduce(acc);
	}
      }
      return acc;
    }
    public long[] toPrimitiveArray() {
      long[] retval = new long[nElems];
      return (long[])reduce(new Reductions.IndexedLongAccum(0, new IFn.OLLO() {
	  public Object invokePrim(Object acc, long idx, long lval) {
	    retval[(int)idx] = lval;
	    return retval;
	  }
	}), retval);
    }
  }
}

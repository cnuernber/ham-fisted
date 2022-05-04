package ham_fisted;

import static ham_fisted.IntBitmap.*;
import java.util.Objects;
import java.util.Arrays;
import java.util.Iterator;

public class HAMTCommon {
  public interface HashProvider {
    public default int hash(Object obj) {
      if (obj != null)
	return mixhash(obj);
      return 0;
    }
    public default boolean equals(Object lhs, Object rhs) {
      return Objects.equals(lhs,rhs);
    }
  }


  public static final HashProvider hashcodeProvider = new HashProvider(){};


  public interface HAMTBase extends HashProvider {
    //Increase/decrease the reported size of the HAMT
    public void inc();
    public void dec();
  }

  public static final class Box {
    public Object obj;
    public Box() { obj = null; }
  }

  public interface ILeaf {
    Object key();
    Object val();
    //Mutation of the leaf for in-place computation
    //Returns previous value.
    Object val(Object v);
  }

  public interface LeafNodeIterator extends Iterator {
    public ILeaf nextLeaf();
    public default Object next() { return nextLeaf(); }
  }

  public interface INode {
    public INode clone(HAMTBase owner);
    public int countLeaves();
    public LeafNodeIterator iterator();
  }

  final static <V> V[] insert(V[] srcData, V obj, int insertIdx, int newlen,
			      boolean forceCopy) {
    final int srcLen = srcData.length;
    final int dstLen = nextPow2(newlen);
    boolean copy = forceCopy | (dstLen > srcLen);
    final V[] dstData = copy ? Arrays.copyOf(srcData, dstLen) : srcData;
    for(int ridx = newlen-1; ridx > insertIdx; --ridx)
      dstData[ridx] = srcData[ridx - 1];
    dstData[insertIdx] = obj;
    return dstData;
  }

  final static <V> V[] remove(V[] srcData, int remidx, int nelems, boolean forceCopy) {
    //nelems is nelems *after* removal
    V[] dstData = forceCopy ? Arrays.copyOf(srcData, Math.max(4, nextPow2(nelems))) : srcData;
    for(int idx = remidx; idx < nelems; ++idx)
      dstData[idx] = srcData[idx+1];
    if(dstData.length > nelems)
      dstData[nelems] = null;
    return dstData;
  }

}

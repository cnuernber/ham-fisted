package ham_fisted;

import static ham_fisted.IntegerOps.*;

import java.util.Objects;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import clojure.lang.Util;
import clojure.lang.IHashEq;
import clojure.lang.IFn;
import clojure.lang.RT;

/**
 * Interfaces and definitions used for implementing the bitmap trie.
 */
public class BitmapTrieCommon {

  /**
   * Hashcode provider to specialize the hash and equals properties of
   * a bitmap trie system.
   */
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

  /**
   * The default hashcode provide simply uses maxhash(obj.hashCode) and Objects.equals(a,b)
   */
  public static final HashProvider equalHashProvider = new HashProvider(){};
  /**
   * Hashcode provider using Clojure's hasheq/equiv pathway
   */
  public static final HashProvider equivHashProvider = new HashProvider() {
      public int hash(Object obj) {
	return Util.hasheq(obj);
      }
      public boolean equals(Object lhs, Object rhs) {
	return CljHash.equiv(lhs,rhs);
      }
    };

  public static final HashProvider hybridHashProvider = new HashProvider() {
      public int hash(Object obj) {
	if (obj instanceof IHashEq)
	  return ((IHashEq)obj).hasheq();
	else if (obj == null)
	  return 0;
	else
	  return mixhash(obj.hashCode());
      }
      public boolean equals(Object lhs, Object rhs) {
	return CljHash.equiv(lhs,rhs);
      }
    };

  public static final HashProvider defaultHashProvider = hybridHashProvider;


  /**
   * A base trie interface to define the information that nodes within the
   * trie itself use.
   */
  interface TrieBase extends HashProvider {
    //Increase/decrease the reported size of the HAMT
    public void inc();
    public void dec();
  }

  /**
   * Return-by-reference pathway.
   */
  static public final class Box {
    public Object obj;
    public Box() { obj = null; }
    public Box(Object _obj) { obj = _obj; }
    @SuppressWarnings("unchecked")
    public void inplaceUpdate(BiFunction bfn, Object newv) {
      obj = obj == null ? newv : bfn.apply(obj, newv);
    }
    public Box inplaceReduce(IFn rfn, Object newv) {
      obj = RT.isReduced(obj) ? obj : rfn.invoke(obj, newv);
      return this;
    }
    public Box inplaceMerge(IFn mergeFn, Box other) {
      obj = mergeFn.invoke(obj, other.obj);
      return this;
    }
  }

  /**
   * Generic leaf.
   */
  public interface ILeaf {
    Object key();
    Object val();
    //Mutation of the leaf for in-place computation
    //Returns previous value.
    Object val(Object v);
  }

  /**
   * An iterator that returns leaf nodes.  Useful to differentiate between
   * the next fn which may apply a transformation on the nextLeaf.  Internally
   * using nextLeaf() will be more efficient.
   */
  interface LeafNodeIterator extends Iterator {
    public ILeaf nextLeaf();
    public default Object next() { return nextLeaf(); }
  }

  public interface MapSet {
    public MapSet intersection(MapSet rhs, BiFunction valueMap);
    public MapSet union(MapSet rhs, BiFunction valueMap);
    public MapSet difference(MapSet rhs);
  }

  public static final BiFunction<Object,Object,Object> rhsWins = (a,b) -> b;
  public static final BiFunction<Object,Object,Object> lhsWins = (a,b) -> a;
  public static final BiFunction<Object,Object,Object> nonNilWins = (a,b) -> a == null ? b : a;
  public static final BiFunction<Long,Long,Long> sumBiFn = (a,b) -> a + b;
  public static final BiFunction<Object,Long,Long> incBiFn = (k,v) -> v == null ? 1 : 1 + v;

  /**
   * A node in the bitmap trie.  There aren't too many places where virtualizing the
   * nodes helps because in most cases the child's type indicates which algorithm the
   * parent should be using.  But the places where it is useful are defined here.
   */
  interface INode {
    /** Clone this node incrementing the node count of owner once per leaf */
    public INode clone(TrieBase owner);
    /** Clone this node but use iter for new values */
    public INode valueClone(TrieBase owner, Iterator valIter);
    /** Count the number of leaves starting at this node */
    public int countLeaves();
    /** Return an iterator that returns the leaf nodes starting at this node. */
    public LeafNodeIterator iterator();
    /** Return the leaf that corresponds to this key and hashcode */
    public ILeaf get(Object key, int hashcode);
    /** Remove an entry returning an new INode instance.  Return value may be nil */
    public INode dissoc(TrieBase owner, Object key, int hashcode);
    /** Immutable update all values returning a new node */
    public INode immutUpdate(TrieBase nowner, BiFunction bfn);
    /** Immutable update a single key returning a new node **/
    public INode immutUpdate(TrieBase nowner, Object key, int hashcode, IFn fn);
    /** Reduce this node - does not call unreduce at end */
    public Object reduce(IFn rfn, Object acc);
  }

  /**
   * Insert an item v in srcData at insertIdx.  The length *after* the insert
   * operation is newlen and forceCopy means treat srcData as immutable.
   */
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

  /**
   * Remove an item v in srcData at remidx.  The length *after* the remove
   * operation is nelems and forceCopy means treat srcData as immutable.
   */
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

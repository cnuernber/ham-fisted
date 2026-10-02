package ham_fisted;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.ArrayList;
import clojure.lang.IPersistentVector;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.Util;
import clojure.lang.IPersistentMap;
import clojure.lang.IEditableCollection;

public class TreeList extends TreeListBase implements IPersistentVector, IEditableCollection {
  final IPersistentMap meta;
  int _hash = 0;
  public TreeList(Object root, Object[] tail, IPersistentMap meta, int shift, int count) {
    super(root, tail, shift, count);
    this.meta = meta;
  }
  public TreeList(IPersistentMap meta) {
    this(new Leaf(), new Object[0], meta, 0, 0);
  }
  public TreeList() {
    this(null);
  }
  public TreeList(TreeListBase other, IPersistentMap meta) {
    super(other);
    this.meta = meta;
  }
  public static TreeList EMPTY = new TreeList();
  public TreeList cons(Object d) {
    final int tlen = tail.length;
    final int newCount = count+1;
    if(tlen == 32) {
      Object rv = shift == 0 ? ((Leaf)root).cons(tail) : ((Branch)root).cons(shift, tail);
      if(rv instanceof Object[]) {
	return new TreeList(new Branch(null, (Object[])rv), new Object[]{d}, meta, shift+1, newCount);
      } else {
	return new TreeList(rv, new Object[]{d}, meta, shift, newCount);
      }
    } else {
      Object[] newTail = new Object[tlen+1];
      System.arraycopy(tail, 0, newTail, 0, tail.length);
      newTail[tlen] = d;
      return new TreeList(root, newTail, meta, shift, newCount);
    }
  }
  public int hasheq() {
    if(_hash == 0) _hash = CljHash.listHasheq(this);
    return _hash;
  }
  public int hashCode() { return hasheq(); }
  public TreeList withMeta(IPersistentMap newMeta) {
    return new TreeList(root, tail, newMeta, shift, count);
  }
  public IPersistentMap meta() { return meta; }

  public TreeList empty() { return meta == null ? EMPTY : EMPTY.withMeta(meta); }

  public TreeList assocN(int idx, Object obj) {
    if(idx == count)
      return cons(obj);
    checkIndex(idx, count);
    int cutoff = count - tail.length;
    if( idx < cutoff) {
      return new TreeList(shift == 0 ? ((Leaf)root).assocN(null, idx, obj, null)
			  : ((Branch)root).assocN(null, shift,idx,obj, null),
			  tail, meta, shift, count);
    } else {
      Object[] newTail = Arrays.copyOf(tail, tail.length);
      newTail[idx % tailWidth] = obj;
      return new TreeList(root, newTail, meta, shift, count);
    }
  }
  public TreeList assoc(Object idx, Object o) {
    return assocN(RT.intCast(idx), o);
  }
  public TreeList pop() {
    if (count == 0)
      throw new IllegalStateException("Can't pop empty vector");
    if (count == 1)
      return empty();
    Object[] newTail;
    Object newRoot;
    int newShift = shift;
    if(tail.length != 0) {
      newRoot = root;
      newShift = shift;
      newTail = Arrays.copyOf(tail, tail.length-1);
    } else {
      SublistResult res = shift == 0 ? ((Leaf)root).pop(null) : ((Branch)root).pop(null, shift);
      newRoot = res.node;
      newTail = Arrays.copyOf(res.tail, tailWidth-1);
      while(newShift > 0) {
	Branch newBranch = (Branch)newRoot;
	if(newBranch.data.length > 1)
	  break;
	
	newRoot = newBranch.data[0];
	newShift--;
      }
    }
    return new TreeList(newRoot, newTail, meta, newShift, count-1);
  }
  public Object peek() {
    if (count == 0)
      return null;
    return get(count-1);
  }
  public MutTreeList asTransient() {
    return new MutTreeList(this, meta());
  }
  public IPersistentVector immut() { return this; }
  public static TreeList create(boolean owning, IPersistentMap meta, Object[] data) {
    int nTails = (data.length + tailWidth - 1) / tailWidth;
    if(nTails == 1) {
      return new TreeList(Leaf.EMPTY, owning ? data : data.clone(), meta, 0, data.length);
    }
    return MutTreeList.create(owning, meta, data).persistent();
  }
  public static TreeList create(IPersistentMap meta, Object[] tail, Object obj) {
    if(tail.length != tailWidth)
      throw new RuntimeException("Tail must be exactly 32 len");
    return new TreeList(new Leaf(null, new Object[][] { tail }), new Object[] { obj }, meta, 0, 33);
  }
}

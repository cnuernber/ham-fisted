package ham_fisted;


import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import it.unimi.dsi.fastutil.ints.IntComparator;
import clojure.lang.IPersistentMap;


public class ReindexList implements IMutList, TypedList {
  final int[] indexes;
  final List data;
  final IPersistentMap meta;

  public static ReindexList create(int[] idx, List d, IPersistentMap m) {
    if(d instanceof IMutList)
      return new MutReindexList(idx, (IMutList)d, m);
    return new ReindexList(idx, d, m);
  }

  public ReindexList(int[] idx, List d, IPersistentMap m) {
    indexes = idx;
    data = d;
    meta = m;
  }

  public Class containedType() { return data instanceof TypedList ? ((TypedList)data).containedType() : null; }
  public int size() { return indexes.length; }
  public Object get(int idx) {
    final int sz = size();
    if (idx < 0)
      idx += sz;
    return data.get(indexes[idx]);
  }
  @SuppressWarnings("unchecked")
  public Object set(int idx, Object nv) {
    final int sz = size();
    if (idx < 0)
      idx += sz;
    return data.set(indexes[idx], nv);
  }
  public ReindexList subList(int sidx, int eidx) {
    return ReindexList.create(Arrays.copyOfRange(indexes, sidx, eidx), data, meta);
  }
  @SuppressWarnings("unchecked")
  public IntComparator indexComparator() {
    if(data instanceof IMutList) {
      final IMutList d = (IMutList)data;
      IntComparator srcComparator = d.indexComparator();
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return srcComparator.compare(indexes[lidx], indexes[ridx]);
	}
      };
    }
    return new IntComparator() {
      public int compare(int lidx, int ridx) {
	return ((Comparable)(data.get(indexes[lidx]))).compareTo(data.get(indexes[ridx]));
      }
    };
  }
  @SuppressWarnings("unchecked")
  public IntComparator indexComparator(Comparator comp) {
    if (comp == null) return indexComparator();
    if (data instanceof IMutList) {
      final IMutList d = (IMutList)data;
      IntComparator srcComparator = d.indexComparator(comp);
      return new IntComparator() {
	public int compare(int lidx, int ridx) {
	  return srcComparator.compare(indexes[lidx], indexes[ridx]);
	}
      };
    }
    return new IntComparator() {
      public int compare(int lidx, int ridx) {
	return comp.compare(data.get(indexes[lidx]), data.get(indexes[ridx]));
      }
    };
  }
  public IPersistentMap meta() { return meta; }
  public ReindexList withMeta(IPersistentMap m) {
    return ReindexList.create(indexes, data, m);
  }
  public List reindex(int[] newIndexes) {
    final int nsz = newIndexes.length;
    final int[] idxs = new int[nsz];
    boolean inOrder = true;
    for (int idx = 0; idx < nsz; ++idx) {
      final int nidx = indexes[newIndexes[idx]];
      inOrder = inOrder && nidx == idx;
      idxs[idx] = nidx;
    }
    //If we are in-order, no new indexing is required.
    if(inOrder) {
      if (nsz == data.size())
	return data;
      else
	return data.subList(0, nsz);
    }
    //Else create one new reindex with new indexes.  This keeps the cost
    //of indirection to a the cost of 1 reindexing operation.
    return ReindexList.create(idxs, data, meta);
  }
  public static class MutReindexList extends ReindexList {
    IMutList mlist;
    public MutReindexList(int[] indexes, IMutList ml, IPersistentMap meta) {
      super(indexes, ml, meta);
      mlist = ml;
    }
    public long getLong(int idx) {
      final int sz = size();
      if (idx < 0)
	idx += sz;
      return mlist.getLong(indexes[idx]);
    }
    public long setLong(int idx, long nv) {
      final int sz = size();
      if (idx < 0)
	idx += sz;
      return mlist.setLong(indexes[idx], nv);
    }
    public double getDouble(int idx) {
      final int sz = size();
      if (idx < 0)
	idx += sz;
      return mlist.getDouble(indexes[idx]);
    }
    public double setDouble(int idx, double nv) {
      final int sz = size();
      if (idx < 0)
	idx += sz;
      return mlist.setDouble(indexes[idx], nv);
    }
  }
}

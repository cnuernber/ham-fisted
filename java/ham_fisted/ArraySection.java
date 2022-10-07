package ham_fisted;



public class ArraySection
{
  public final Object array;
  public final int sidx;
  public final int eidx;
  public ArraySection(Object ary, int _sidx, int _eidx) {
    if(! (_eidx >= _sidx))
      throw new RuntimeException("End index: " + String.valueOf(_eidx) + " is not >= start index: " + String.valueOf(_sidx));

    array = ary;
    sidx = _sidx;
    eidx = _eidx;
  }
  public ArraySection(ArraySection other) {
    this(other.array, other.sidx, other.eidx);
  }
  public int size() { return eidx - sidx; }
  public String toString() {
    return "ArraySection<" + array.getClass().getComponentType().getCanonicalName() + ">[" + String.valueOf(sidx) + ":" + String.valueOf(eidx) + "]";
  }
}

package ham_fisted;


import clojure.lang.IDeref;
import clojure.lang.Keyword;
import clojure.lang.IReduceInit;
import java.util.function.DoubleConsumer;
import java.util.Collection;



public final class Sum implements DoubleConsumer, Reducible, IDeref
{
  public static final Keyword sumKwd = Keyword.intern(null, "sum");
  public static final Keyword nElemsKwd = Keyword.intern(null, "n-elems");
  //Low order summation bits
  public double d0;
  //High order summation bits
  public double d1;
  public double simpleSum;
  public long nElems;
  /**
   * Incorporate a new double value using Kahan summation /
   * compensation summation.
   *
   * High-order bits of the sum are in intermediateSum[0], low-order
   * bits of the sum are in intermediateSum[1], any additional
   * elements are application-specific.
   *
   * @param intermediateSum the high-order and low-order words of the intermediate sum
   * @param value the name value to be included in the running sum
   */
  public void sumWithCompensation(double value) {
    double tmp = value - d1;
    double sum = d0;
    double velvel = sum + tmp; // Little wolf of rounding error
    d1 =  (velvel - sum) - tmp;
    d0 = velvel;
  }

  public double computeFinalSum() {
    // Better error bounds to add both terms as the final sum
    double tmp = d0 + d1;
    if (Double.isNaN(tmp) && Double.isInfinite(simpleSum))
      return simpleSum;
    else
      return tmp;
  }

  public Sum(double _d0, double _d1, double _simpleSum, long _nElems) {
    d0 = _d0;
    d1 = _d1;
    simpleSum = _simpleSum;
    nElems = _nElems;
  }
  public Sum() {
    this(0, 0, 0, 0);
  }
  public void accept(double data) {
    sumWithCompensation(data);
    simpleSum += data;
    nElems++;
  }

  public void merge(Sum other) {
    final long ne = nElems;
    accept(other.computeFinalSum());
    nElems = ne + other.nElems;
  }

  public Sum reduce(Collection<Reducible> rest) {
    final Sum retval = new Sum(d0, d1, simpleSum, nElems);
    for(Reducible rhs: rest) {
      retval.merge((Sum)rhs);
    }
    return retval;
  }

  public Object deref() {
    return new PersistentArrayMap(BitmapTrieCommon.defaultHashProvider,
				  sumKwd, computeFinalSum(),
				  nElemsKwd, nElems, null);
  }

  public static class SimpleSum implements DoubleConsumer, IDeref, Reducible
  {
    double simpleSum;
    public SimpleSum() { simpleSum = 0.0; }
    public void accept(double val) { simpleSum += val; }
    public SimpleSum reduce(Collection<Reducible> rest) {
      final SimpleSum retval = new SimpleSum();
      retval.simpleSum = simpleSum;
      for(Reducible rhs : rest) {
	final SimpleSum other = (SimpleSum)rhs;
	retval.simpleSum += other.simpleSum;
      }
      return retval;
    }
    public Object deref() {
      return simpleSum;
    }
  };
}

package ham_fisted;



import java.util.function.LongConsumer;
import clojure.lang.IDeref;



public class LongAccum implements LongConsumer, IDeref {
  long val;
  public LongAccum(long v) { val = v; }
  public LongAccum() { this(0); }
  public void accept(long v) { val += v; }
  public Object deref() { return val; }
}

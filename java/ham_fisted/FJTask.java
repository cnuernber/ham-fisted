package ham_fisted;

import clojure.lang.IFn;
import clojure.lang.IDeref;
import clojure.lang.Delay;
import java.util.concurrent.RecursiveTask;

public class FJTask extends RecursiveTask {
  public final IDeref c;
  public FJTask(Object c) {
    if(c instanceof IDeref) {
      this.c = (IDeref)c;
    } else {
      this.c = new Delay( (IFn) c);
    }
  }
  public Object compute() { return c.deref(); }
}

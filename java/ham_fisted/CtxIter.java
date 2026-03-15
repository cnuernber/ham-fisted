package ham_fisted;

import java.util.Iterator;
import java.util.function.Supplier;

public class CtxIter implements Iterator {
  public static interface Ctx {
    public Ctx update();
    public boolean valid();
    public Object val();
  }
  public final Supplier<Ctx> init;
  Ctx ctx;
  public static final int stepInit = 0;
  public static final int stepUpdate = 1;
  public static final int stepVal = 2;
  int step;
  public CtxIter(Supplier<Ctx> init) {
    this.init = init;
    step = stepInit;
    ctx = null;
  }
  void advance() {
    if(step == stepInit) {
      ctx = init.get();
    } else if (step == stepUpdate) {
      ctx = ctx.update();
    }
    step = stepVal;
  }
  public int step() { return step; }
  public Ctx ctx() { return ctx; }
  public boolean hasNext() {
    advance();
    return ctx.valid();
  }
  public Object next() {
    advance();
    step = stepUpdate;
    return ctx.val();
  }
}

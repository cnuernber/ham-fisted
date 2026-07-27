package ham_fisted;

import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

public class BinaryPriorityFutureTask<T> extends FutureTask<T> {
    static ThreadLocal<Boolean> inBinaryPriorityTask = new ThreadLocal<Boolean>();
    public static ThreadLocal<Boolean> isHighPriorityVar = new ThreadLocal<Boolean>();
    public static boolean isInBinaryPriorityTask() { Object oldVal = inBinaryPriorityTask.get(); return oldVal == null ? false : (boolean)oldVal; }
    public final boolean isHighPriority;
    public boolean isHighPriority() { return isHighPriority; }
    public BinaryPriorityFutureTask(Callable<T> c, boolean isHigh) {
	super(c);
	isHighPriority=isHigh;
    }
    public void run() {
	Boolean oldVal = Casts.booleanCast(inBinaryPriorityTask.get());
	boolean oldPriority = Casts.booleanCast(isHighPriorityVar.get());
	try {
	    inBinaryPriorityTask.set(true);
	    isHighPriorityVar.set(isHighPriority);
	    super.run();
	} finally {
	    isHighPriorityVar.set(oldPriority);
	    inBinaryPriorityTask.set(oldVal);
	}
    }
}

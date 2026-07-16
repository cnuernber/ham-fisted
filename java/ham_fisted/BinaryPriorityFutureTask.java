package ham_fisted;

import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;

public class BinaryPriorityFutureTask<T> extends FutureTask<T> {
    static ThreadLocal<Boolean> inBinaryPriorityTask = new ThreadLocal<Boolean>();
    public static boolean isInBinaryPriorityTask() { Object oldVal = inBinaryPriorityTask.get(); return oldVal == null ? false : (boolean)oldVal; }
    public final boolean isHighPriority;
    public boolean isHighPriority() { return isHighPriority; }
    public BinaryPriorityFutureTask(Callable<T> c, boolean isHigh) {
	super(c);
	isHighPriority=isHigh;
    }
    public void run() {
	Boolean oldVal = inBinaryPriorityTask.get();
	try {	    
	    inBinaryPriorityTask.set(true);
	    super.run();
	} finally {
	    inBinaryPriorityTask.set(oldVal);
	}
    }
}

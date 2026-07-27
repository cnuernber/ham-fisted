package ham_fisted;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

//A Thread pool executor that allows cooperative blocking and maintains an variable target thread count
public class CooperativeTPE extends ThreadPoolExecutor {
    int desiredThreadCount;
    ReentrantLock lock = new ReentrantLock();

    public CooperativeTPE(int desiredThreadCount, int threadTimeoutMs, ThreadFactory threadFactory, BlockingQueue<Runnable> queue, RejectedExecutionHandler handler) {
	super(desiredThreadCount, desiredThreadCount, threadTimeoutMs, TimeUnit.MILLISECONDS, queue, threadFactory, handler);
	this.desiredThreadCount = desiredThreadCount;
    }

    // 1. The Custom Thread that stores its parent pool
    public static class PooledThread extends Thread {
	private final CooperativeTPE pool;
	
	public PooledThread(Runnable target, CooperativeTPE pool, String name) {
	    super(target, name);
	    this.pool = pool;
	}
	
	public CooperativeTPE getPool() {
	    return pool;
	}
    }

    public int desiredThreadCount() { return desiredThreadCount; }
    public static boolean isRealized(Object o) {
	if (o instanceof java.util.concurrent.Future)
	    return ((java.util.concurrent.Future) o).isDone();
	else
	    return ((clojure.lang.IPending) o).isRealized();
    }

    public static Object deref(Object o) throws InterruptedException, ExecutionException {
	if (o instanceof java.util.concurrent.Future)
	    return ((java.util.concurrent.Future) o).get();
	else
	    return ((clojure.lang.IDeref) o).deref();
    }

    public static Object managedBlock(Object blocker) throws InterruptedException, ExecutionException {
	Thread t = Thread.currentThread();
	if (isRealized(blocker) || (! (t instanceof PooledThread))) {
	    //System.out.println("Unmanaged block - " + String.valueOf(t.getClass()));
	    return deref(blocker);
	}
	CooperativeTPE pool = ((PooledThread)t).getPool();
	pool.lock.lock();
	int tc = pool.getCorePoolSize();
	try {
	    pool.setMaximumPoolSize(tc+1);
	    pool.setCorePoolSize(tc+1);
	    //System.out.println("Managed blocking - " + String.valueOf(tc));
	} finally {
	    pool.lock.unlock();
	}
	Object rv = deref(blocker);
	pool.lock.lock();
	int curtc = pool.getCorePoolSize();
	int diff = pool.desiredThreadCount - curtc;
	try {
	    if(diff > 0) {
		pool.setMaximumPoolSize(curtc+diff);
		pool.setCorePoolSize(curtc+diff);
	    } else if (diff < 0) {
		pool.setCorePoolSize(curtc-1);
		pool.setMaximumPoolSize(curtc-1);
	    }
	} finally {
	    pool.lock.unlock();
	}
	return rv;
    }

    public void setDesiredThreadCount(int newNC) {
	if(newNC != desiredThreadCount) {
	    lock.lock();
	    try {
		if(newNC > desiredThreadCount) {
		    setMaximumPoolSize(newNC);
		    setCorePoolSize(newNC);
		} else {
		    setCorePoolSize(newNC);
		    setMaximumPoolSize(newNC);
		}
		desiredThreadCount = newNC;
	    } finally {
		lock.unlock();
	    }
	}
    }
}

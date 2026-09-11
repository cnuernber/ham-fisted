package ham_fisted;

import clojure.lang.ISeq;
import clojure.lang.Seqable;
import clojure.lang.Cons;
import clojure.lang.PersistentList;
import clojure.lang.IPersistentCollection;
import clojure.lang.RT;
import clojure.lang.IFn;

import java.util.concurrent.locks.LockSupport;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;


public class LockFreeLazyCons implements ISeqDef {
    private final Object val;
    private int hasheq = 0;
    // Volatile field for atomic operations via VarHandle
    private volatile Object tail;

    // Static sentinel representing an active evaluation in progress
    private static final Object EVALUATING = new Object();

    // Static VarHandle bound to the 'tail' field
    private static final VarHandle TAIL_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            TAIL_HANDLE = lookup.findVarHandle(LockFreeLazyCons.class, "tail", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    public LockFreeLazyCons(Object val, Object tail) {
        this.val = val;
        this.tail = tail;
    }
    
    @Override
    public Object first() {
        return this.val;
    }
    public int hasheq() {
	if (hasheq == 0)
	    hasheq = calcHasheq();
	return hasheq;
    }
    public int hashCode () { return calcHashCode(); }

    @Override
    public ISeq next() {
	return RT.seq(realizeTail());
    }

    @Override
    public String toString() {
        return "(" + val + " ...)";
    }

    // --- Lock-Free Tail Realization ---

    public Object realizeTail() {
	int spins = 0;
        while (true) {
            Object current = TAIL_HANDLE.getVolatile(this);
	    
            // 1. Another thread is actively executing the thunk
            if (current == EVALUATING) {
		spins++;
		if(spins < 50)
		    Thread.yield();
		else {
		    spins = 50; //avoid ever wrapping spins.
		    LockSupport.parkNanos(10000);
		}
                continue;
            }

            // 2. Already realized (not an IFn thunk)
            if (!(current instanceof IFn)) {
                return current;
            }

            // 3. Unevaluated thunk -> CAS claim via VarHandle
            if (TAIL_HANDLE.compareAndSet(this, current, EVALUATING)) {
                try {
                    IFn thunk = (IFn) current;
                    Object realized = thunk.invoke();
                    TAIL_HANDLE.setVolatile(this, realized);
                    return realized;
                } catch (Throwable e) {
                    // Reset state on failure so another thread can attempt or propagate
                    TAIL_HANDLE.setVolatile(this, current);
                    throw e;
                }
            }
        }
    }
}


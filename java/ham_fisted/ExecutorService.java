package ham_fisted;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;

//An executor service you can override in Clojure.
public interface ExecutorService extends java.util.concurrent.ExecutorService {
    Future<?> submitRunnable(Runnable task);
    <T> Future<T> submitRunnable(Runnable task, T result);
    <T> Future<T> submitCallable(Callable<T> task);
    default Future<?> submit(Runnable task) { return submitRunnable(task); }
    default <T> Future<T> submit(Runnable task, T result) { return submitRunnable(task, result); }
    default <T> Future<T> submit(Callable<T> task) { return submitCallable(task); }
}

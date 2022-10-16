package ham_fisted;


import java.util.concurrent.ForkJoinPool;


public class ParallelOptions {
  public final int minN;
  public final int maxBatchSize;
  public final boolean ordered;
  public final ForkJoinPool pool;
  public final int parallelism;

  public ParallelOptions(int _minN, int batchSize, boolean _ordered,
			 ForkJoinPool _pool, int _parallelism) {
    minN = _minN;
    maxBatchSize = batchSize;
    ordered = _ordered;
    pool = _pool;
    parallelism = _parallelism;
  }
  public ParallelOptions(int minN, int batchSize, boolean ordered) {
    this(minN, batchSize, ordered,
	 ForkJoinPool.commonPool(), ForkJoinPool.getCommonPoolParallelism());
  }
}

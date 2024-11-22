package ham_fisted;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;


public class ParallelOptions {
  public final long minN;
  public final int maxBatchSize;
  public final boolean ordered;
  public final ExecutorService pool;
  public final int parallelism;
  public final CatParallelism catParallelism;
  public final int putTimeoutMs;
  public final int nLookahead;
  //In some cases users may want a sequence of unmerged results to do some
  //other form of merge.
  public final boolean unmergedResult;

  public enum CatParallelism {
    //Parallelize assuming catenation of large-N containers
    ELEMWISE,
    //Parallelize assuming large catenation of small-N containers -
    //default parallelism.
    SEQWISE,
  }

  public ParallelOptions(long _minN, int batchSize, boolean _ordered,
			 ExecutorService _pool, int _parallelism,
			 CatParallelism _catParallelism, int _putTimeoutMs,
			 boolean unmergedResult, int nLookahead) {
    minN = _minN;
    maxBatchSize = batchSize;
    ordered = _ordered;
    pool = _pool;
    parallelism = _parallelism;
    catParallelism = _catParallelism;
    putTimeoutMs = _putTimeoutMs;
    this.unmergedResult = unmergedResult;
    this.nLookahead = nLookahead;
  }
  public ParallelOptions(long minN, int batchSize, boolean ordered) {
    this(minN, batchSize, ordered,
	 ForkJoinPool.commonPool(), ForkJoinPool.getCommonPoolParallelism(),
	 CatParallelism.SEQWISE, 5000, false, -1);
  }
  public ParallelOptions() {
    this(1000, 64000, true);
  }
  public ParallelOptions minN(long newMinN) {
    return new ParallelOptions(newMinN, maxBatchSize, ordered, pool, parallelism,
			       catParallelism, putTimeoutMs, false, -1);
  }
}

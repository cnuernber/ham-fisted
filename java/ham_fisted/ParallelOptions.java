package ham_fisted;


import java.util.concurrent.ForkJoinPool;


public class ParallelOptions {
  public final int minN;
  public final int maxBatchSize;
  public final boolean ordered;
  public final ForkJoinPool pool;
  public final int parallelism;
  public final CatParallelism catParallelism;

  public enum CatParallelism {
    //Parallelize assuming catenation of large-N containers
    ELEMWISE,
    //Parallelize assuming large catenation of small-N containers -
    //default parallelism.
    SEQWISE,
  }

  public ParallelOptions(int _minN, int batchSize, boolean _ordered,
			 ForkJoinPool _pool, int _parallelism,
			 CatParallelism _catParallelism) {
    minN = _minN;
    maxBatchSize = batchSize;
    ordered = _ordered;
    pool = _pool;
    parallelism = _parallelism;
    catParallelism = _catParallelism;
  }
  public ParallelOptions(int minN, int batchSize, boolean ordered) {
    this(minN, batchSize, ordered,
	 ForkJoinPool.commonPool(), ForkJoinPool.getCommonPoolParallelism(),
	 CatParallelism.SEQWISE);
  }
}

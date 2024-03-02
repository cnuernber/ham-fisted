package ham_fisted;


public class ConsumerAccumulators {
  public static class DoubleConsumerAccumulator implements IFnDef.ODO  {
    public static final DoubleConsumerAccumulator INST = new DoubleConsumerAccumulator();
    public DoubleConsumerAccumulator(){}
    public Object invokePrim(Object acc, double val) {
      ((java.util.function.DoubleConsumer)acc).accept(val);
      return acc;
    }
  }
  public static class LongConsumerAccumulator implements IFnDef.OLO  {
    public static final LongConsumerAccumulator INST = new LongConsumerAccumulator();
    public LongConsumerAccumulator(){}
    public Object invokePrim(Object acc, long val) {
      ((java.util.function.LongConsumer)acc).accept(val);
      return acc;
    }
  }
  public static class ConsumerAccumulator implements IFnDef.OOO  {
    public static final ConsumerAccumulator INST = new ConsumerAccumulator();
    public ConsumerAccumulator(){}
    @SuppressWarnings("unchecked")
    public Object invoke(Object acc, Object val) {
      ((java.util.function.Consumer)acc).accept(val);
      return acc;
    }
  }
}

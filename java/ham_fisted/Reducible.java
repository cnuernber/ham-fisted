package ham_fisted;


import java.util.Iterator;


public interface Reducible
{
  default Reducible reduceIter(Iterator<Reducible> rest) {
    Reducible retval = this;
    while(rest.hasNext())
      retval = retval.reduce(rest.next());
    return retval;
  }
  Reducible reduce(Reducible rhs);
}

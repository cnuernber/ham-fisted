package ham_fisted;


import java.util.Collection;


public interface Reducible
{
  Reducible reduce(Collection<Reducible> rest);
}

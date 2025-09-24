package ham_fisted;

import java.util.Iterator;

public interface Iter {
  Object get();
  Iter next();

  public static class IteratorIter implements Iter {
    public final Iterator iter;
    Object get;
    public IteratorIter(Iterator iter) {
      this.iter = iter;
      get = iter.next();
    }
    public Object get() { return get; }
    public Iter next() {
      if(iter.hasNext()) {
	get = iter.next();
	return this;
      }
      return null;
    }
  }

  public static class IterIterator implements Iterator {
    Iter iter;
    public IterIterator(Iter iter) {
      this.iter = iter;
    }
    public Iter iter() { return iter; }
    public boolean hasNext() { return iter != null; }
    public Object next() {
      Object rv = iter.get();
      iter = iter.next();
      return rv;
    }
  }

  public static Iter fromIterator(Iterator iter) {
    return iter != null && iter.hasNext() ? new IteratorIter(iter) : null;
  }

  public static Iterator fromIter(Iter iter) {
    return new IterIterator(iter);
  }

  public static Iter fromIterable(Iterable iable) {
    return iable != null ? fromIterator(iable.iterator()) : null;
  }
  public static Iter prepend(Object obj, Iter item) {
    return new Iter() {
      public Object get() { return obj; }
      public Iter next() { return item; }
    };
  }
}

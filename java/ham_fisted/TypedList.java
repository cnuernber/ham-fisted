package ham_fisted;


public interface TypedList {
  default Class containedType() { return Object.class; }
}

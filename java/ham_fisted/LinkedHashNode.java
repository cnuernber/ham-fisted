package ham_fisted;


public class LinkedHashNode extends HashNode {
  //Less recently modified
  LinkedHashNode prevLink;
  //More recently modified
  LinkedHashNode nextLink;
  public LinkedHashNode(LinkedHashMap owner, Object _k, int hc, Object _v, LinkedHashNode nn) {
    super(owner, _k, hc, _v, nn);
  }
  public LinkedHashNode(LinkedHashMap owner, LinkedHashNode prev) {
    super(owner, prev);
    nextLink = prev.nextLink;
    prevLink = prev.prevLink;
  }
  public HashNode clone(HashMap nowner) {
    throw new UnsupportedOperationException("LinkedHashNodes cannot clone");
  }
  public HashNode setOwner(HashMap nowner) {
    if(nowner != owner)
      throw new RuntimeException("LinkedHashMap nodes cannot be structurally shared");
    return this;
  }
  //Linked node assoc/dissoc are not functional like their counterparts -
  //they just keep the same signature for use in the set algorithms.
  public final HashNode assoc(HashMap nowner, Object _k, int hash, Object _v) {
    if(nowner != owner)
      throw new RuntimeException("LinkedHashMap assoc called in functional pathway");
    HashNode retval = this;
    if (owner.equals(_k,k)) {
      retval.setValue(_v);      
    } else {
      if (retval.nextNode != null) {
	retval.nextNode = retval.nextNode.assoc(nowner, _k, hash, _v);
      } else {
	retval.nextNode = nowner.newNode(k, hash, _v);
      }
    }
    return retval;
  }
  public final HashNode dissoc(HashMap nowner, Object _k) {
    if(nowner != owner)
      throw new RuntimeException("LinkedHashMap assoc called in functional pathway");
    if (owner.equals(k, _k)) {
      owner.dec(this);
      return nextNode;
    }
    if (nextNode != null) {
      nextNode = nextNode.dissoc(nowner, _k);
    }
    return this;
  }
}

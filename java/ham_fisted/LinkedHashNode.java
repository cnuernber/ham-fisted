package ham_fisted;


public class LinkedHashNode extends HBNode {
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
}

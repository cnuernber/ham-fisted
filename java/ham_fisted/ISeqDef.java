package ham_fisted;


import clojure.lang.ISeq;
import clojure.lang.Sequential;
import clojure.lang.IHashEq;
import clojure.lang.IPersistentCollection;
import clojure.lang.PersistentList;
import clojure.lang.Util;
import clojure.lang.Counted;
import clojure.lang.RT;
import clojure.lang.SeqIterator;
import clojure.lang.Murmur3;
import clojure.lang.Cons;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;


public interface ISeqDef extends ISeq, Sequential, List, IHashEq {
  
  default IPersistentCollection empty(){
    return PersistentList.EMPTY;
  }
  
  default boolean equiv(Object obj){
    if(!(obj instanceof Sequential || obj instanceof List))
      return false;

    if(this instanceof Counted && obj instanceof Counted &&
       ((Counted)this).count() != ((Counted)obj).count())
      return false;

    ISeq ms = RT.seq(obj);
    for(ISeq s = seq(); s != null; s = s.next(), ms = ms.next()) {
	if(ms == null || !Util.equiv(s.first(), ms.first()))
	  return false;
      }
    return ms == null;

  }
  default boolean seqEquals(Object obj){
    if(this == obj) return true;
    if(!(obj instanceof Sequential || obj instanceof List))
      return false;
    ISeq ms = RT.seq(obj);
    for(ISeq s = seq(); s != null; s = s.next(), ms = ms.next()) {
	if(ms == null || !Util.equals(s.first(), ms.first()))
	  return false;
    }
    return ms == null;
  }
  default int calcHashCode(){
    int hash = 1;
    for(ISeq s = seq(); s != null; s = s.next()) {
      hash = 31 * hash + (s.first() == null ? 0 : s.first().hashCode());
    }
    return hash;
  }

  default int calcHasheq(){
    return Murmur3.hashOrdered(this);
  }
  
  default int count(){
    int i = 1;
    for(ISeq s = next(); s != null; s = s.next(), i++)
      if(s instanceof Counted)
	return i + s.count();
    return i;
  }

  default ISeq seq(){
    return this;
  }

  default ISeq cons(Object o){
    return new Cons(o, this);
  }

  default ISeq more(){
    ISeq s = next();
    if(s == null)
      return PersistentList.EMPTY;
    return s;
  }
  
  @SuppressWarnings("unchecked")
  default List toArrayList() {
    ArrayList retval = new ArrayList();
    for(ISeq s = this; s != null; s = s.next())
      retval.add(s.first());    
    return retval;
  }

  default Object[] toArray(){
    return toArrayList().toArray();
  }

  default boolean add(Object o){
    throw new UnsupportedOperationException();
  }

  default boolean remove(Object o){
    throw new UnsupportedOperationException();
  }

  default boolean addAll(Collection c){
    throw new UnsupportedOperationException();
  }

  default void clear(){
    throw new UnsupportedOperationException();
  }

  default boolean retainAll(Collection c){
    throw new UnsupportedOperationException();
  }

  default boolean removeAll(Collection c){
    throw new UnsupportedOperationException();
  }

  default boolean containsAll(Collection c){
    for(Object o : c)
      {
	if(!contains(o))
	  return false;
      }
    return true;
  }

  default Object[] toArray(Object[] a){
    return RT.seqToPassedArray(seq(), a);
  }

  default int size(){
    return count();
  }

  default boolean isEmpty(){
    return seq() == null;
  }

  default boolean contains(Object o){
    for(ISeq s = seq(); s != null; s = s.next())
      {
	if(Util.equiv(s.first(), o))
	  return true;
      }
    return false;
  }

  default Iterator iterator(){
    return new SeqIterator(this);
  }

  //////////// List stuff /////////////////
  @SuppressWarnings("unchecked")
  default List reify(){
    return Collections.unmodifiableList(toArrayList());
  }

  default List subList(int fromIndex, int toIndex){
    return reify().subList(fromIndex, toIndex);
  }

  default Object set(int index, Object element){
    throw new UnsupportedOperationException();
  }

  default Object remove(int index){
    throw new UnsupportedOperationException();
  }

  default int indexOf(Object o){
    ISeq s = seq();
    for(int i = 0; s != null; s = s.next(), i++)
      {
	if(Util.equiv(s.first(), o))
	  return i;
      }
    return -1;
  }

  default int lastIndexOf(Object o){
    return reify().lastIndexOf(o);
  }

  default ListIterator listIterator(){
    return reify().listIterator();
  }

  default ListIterator listIterator(int index){
    return reify().listIterator(index);
  }

  default Object get(int index){
    return RT.nth(this, index);
  }

  default void add(int index, Object element){
    throw new UnsupportedOperationException();
  }

  default boolean addAll(int index, Collection c){
    throw new UnsupportedOperationException();
  }  
}

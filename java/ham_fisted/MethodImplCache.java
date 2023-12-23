/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/* rich Nov 8, 2009 */
/* Heavily modified by Chris Nuernberger - Dec 23, 2023 */

package ham_fisted;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import clojure.lang.IFn;
import clojure.lang.Keyword;

public final class MethodImplCache {
  final ReentrantLock extLock = new ReentrantLock();
  
  //Sparse table of class to ifn.
  //a lock-protected hashmap as we need to perform potentially many lookups
  //in rapid succession.
  final HashMap<Class,IFn> extensions = new HashMap<Class,IFn>();
  volatile IFn nullExtension = null;
  
  //Potentially dense table of resolved lookups.
  final ConcurrentHashMap<Class,IFn> lookupCache = new ConcurrentHashMap<Class,IFn>();

  public static final IFn DEFAULT = new clojure.lang.AFn() { public Object invoke() { return null; }};
  
  public final Keyword methodk;
  public final Keyword ns_methodk;
  public final Class iface;
  public final IFn ifaceFn;


  public MethodImplCache(Keyword methodk, Keyword ns_methodk, Class iface, IFn ifaceFn) {
    this.methodk = methodk;
    this.ns_methodk = ns_methodk;
    this.iface = iface;
    this.ifaceFn = ifaceFn;
  }

  public void extend(Class c, IFn fn) {
    extLock.lock();
    try {
      if(c == null)
	nullExtension = fn;
      else {
	extensions.put(c, fn);
      }
    } finally {
      extLock.unlock();
    }
    lookupCache.clear();
  }

  public IFn findFnFor(Class c) {
    if(c == null) return nullExtension;
    final IFn defVal = DEFAULT;
    IFn rv = lookupCache.getOrDefault(c, defVal);
    //Include caching when lookup fails.
    if(rv != defVal) return rv;
    //rv is the default value at this point
    extLock.lock();
    try {
      for(Class cc = c; cc != null && rv == defVal; cc = cc.getSuperclass()) {
	rv = extensions.getOrDefault(cc, defVal);
	if(rv == defVal) {
	  Class[] ifaces = cc.getInterfaces();
	  int ni = ifaces.length;
	  for(int idx = 0; idx < ni && rv == defVal; ++idx)
	    rv = extensions.getOrDefault(ifaces[idx], defVal);
	}
      }
    } finally {
      extLock.unlock();
    }
    if(rv != defVal)
      lookupCache.put(c, rv);

    return rv == defVal ? null : rv;
  }

}

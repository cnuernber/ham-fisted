(ns ham-fisted.primitive-invoke
"For statically traced calls the Clojure compiler calls the primitive version of type-hinted functions
  and this makes quite a difference in tight loops.  Often times, however, functions are passed by values
  or returned from if-statements and then you need to explicitly call the primitive overload - this makes
  that pathway less verbose.  Functions must first be check-casted to their primitive types and then
  calling them will use their primitive overloads avoiding all casting.

```clojure
(defn doit [f x y]
   (let [f (pi/->ddd f)]
     (loop [x x y y]
      (if (< x y)
        (recur (pi/ddd f x y) y)
        x))))
```")

(defn ->l ^clojure.lang.IFn$L [f]
  (if (instance? clojure.lang.IFn$L f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$L")))))
(defmacro l [f]
`(.invokePrim ~f))
(defn ->d ^clojure.lang.IFn$D [f]
  (if (instance? clojure.lang.IFn$D f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$D")))))
(defmacro d [f]
`(.invokePrim ~f))
(defn ->lo ^clojure.lang.IFn$LO [f]
  (if (instance? clojure.lang.IFn$LO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LO")))))
(defmacro lo [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->do ^clojure.lang.IFn$DO [f]
  (if (instance? clojure.lang.IFn$DO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DO")))))
(defmacro do [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->ol ^clojure.lang.IFn$OL [f]
  (if (instance? clojure.lang.IFn$OL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OL")))))
(defmacro ol [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->ll ^clojure.lang.IFn$LL [f]
  (if (instance? clojure.lang.IFn$LL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LL")))))
(defmacro ll [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->dl ^clojure.lang.IFn$DL [f]
  (if (instance? clojure.lang.IFn$DL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DL")))))
(defmacro dl [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->od ^clojure.lang.IFn$OD [f]
  (if (instance? clojure.lang.IFn$OD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OD")))))
(defmacro od [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->ld ^clojure.lang.IFn$LD [f]
  (if (instance? clojure.lang.IFn$LD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LD")))))
(defmacro ld [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->dd ^clojure.lang.IFn$DD [f]
  (if (instance? clojure.lang.IFn$DD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DD")))))
(defmacro dd [f arg0]
`(.invokePrim ~f ~arg0))
(defn ->olo ^clojure.lang.IFn$OLO [f]
  (if (instance? clojure.lang.IFn$OLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLO")))))
(defmacro olo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->odo ^clojure.lang.IFn$ODO [f]
  (if (instance? clojure.lang.IFn$ODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODO")))))
(defmacro odo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->loo ^clojure.lang.IFn$LOO [f]
  (if (instance? clojure.lang.IFn$LOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOO")))))
(defmacro loo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->llo ^clojure.lang.IFn$LLO [f]
  (if (instance? clojure.lang.IFn$LLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLO")))))
(defmacro llo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ldo ^clojure.lang.IFn$LDO [f]
  (if (instance? clojure.lang.IFn$LDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDO")))))
(defmacro ldo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->doo ^clojure.lang.IFn$DOO [f]
  (if (instance? clojure.lang.IFn$DOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOO")))))
(defmacro doo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->dlo ^clojure.lang.IFn$DLO [f]
  (if (instance? clojure.lang.IFn$DLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLO")))))
(defmacro dlo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ddo ^clojure.lang.IFn$DDO [f]
  (if (instance? clojure.lang.IFn$DDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDO")))))
(defmacro ddo [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ool ^clojure.lang.IFn$OOL [f]
  (if (instance? clojure.lang.IFn$OOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOL")))))
(defmacro ool [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->oll ^clojure.lang.IFn$OLL [f]
  (if (instance? clojure.lang.IFn$OLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLL")))))
(defmacro oll [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->odl ^clojure.lang.IFn$ODL [f]
  (if (instance? clojure.lang.IFn$ODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODL")))))
(defmacro odl [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->lol ^clojure.lang.IFn$LOL [f]
  (if (instance? clojure.lang.IFn$LOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOL")))))
(defmacro lol [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->lll ^clojure.lang.IFn$LLL [f]
  (if (instance? clojure.lang.IFn$LLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLL")))))
(defmacro lll [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ldl ^clojure.lang.IFn$LDL [f]
  (if (instance? clojure.lang.IFn$LDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDL")))))
(defmacro ldl [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->dol ^clojure.lang.IFn$DOL [f]
  (if (instance? clojure.lang.IFn$DOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOL")))))
(defmacro dol [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->dll ^clojure.lang.IFn$DLL [f]
  (if (instance? clojure.lang.IFn$DLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLL")))))
(defmacro dll [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ddl ^clojure.lang.IFn$DDL [f]
  (if (instance? clojure.lang.IFn$DDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDL")))))
(defmacro ddl [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ood ^clojure.lang.IFn$OOD [f]
  (if (instance? clojure.lang.IFn$OOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOD")))))
(defmacro ood [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->old ^clojure.lang.IFn$OLD [f]
  (if (instance? clojure.lang.IFn$OLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLD")))))
(defmacro old [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->odd ^clojure.lang.IFn$ODD [f]
  (if (instance? clojure.lang.IFn$ODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODD")))))
(defmacro odd [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->lod ^clojure.lang.IFn$LOD [f]
  (if (instance? clojure.lang.IFn$LOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOD")))))
(defmacro lod [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->lld ^clojure.lang.IFn$LLD [f]
  (if (instance? clojure.lang.IFn$LLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLD")))))
(defmacro lld [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ldd ^clojure.lang.IFn$LDD [f]
  (if (instance? clojure.lang.IFn$LDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDD")))))
(defmacro ldd [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->dod ^clojure.lang.IFn$DOD [f]
  (if (instance? clojure.lang.IFn$DOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOD")))))
(defmacro dod [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->dld ^clojure.lang.IFn$DLD [f]
  (if (instance? clojure.lang.IFn$DLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLD")))))
(defmacro dld [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->ddd ^clojure.lang.IFn$DDD [f]
  (if (instance? clojure.lang.IFn$DDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDD")))))
(defmacro ddd [f arg0 arg1]
`(.invokePrim ~f ~arg0 ~arg1))
(defn ->oolo ^clojure.lang.IFn$OOLO [f]
  (if (instance? clojure.lang.IFn$OOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLO")))))
(defmacro oolo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oodo ^clojure.lang.IFn$OODO [f]
  (if (instance? clojure.lang.IFn$OODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODO")))))
(defmacro oodo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oloo ^clojure.lang.IFn$OLOO [f]
  (if (instance? clojure.lang.IFn$OLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOO")))))
(defmacro oloo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ollo ^clojure.lang.IFn$OLLO [f]
  (if (instance? clojure.lang.IFn$OLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLO")))))
(defmacro ollo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oldo ^clojure.lang.IFn$OLDO [f]
  (if (instance? clojure.lang.IFn$OLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDO")))))
(defmacro oldo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->odoo ^clojure.lang.IFn$ODOO [f]
  (if (instance? clojure.lang.IFn$ODOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOO")))))
(defmacro odoo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->odlo ^clojure.lang.IFn$ODLO [f]
  (if (instance? clojure.lang.IFn$ODLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLO")))))
(defmacro odlo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oddo ^clojure.lang.IFn$ODDO [f]
  (if (instance? clojure.lang.IFn$ODDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDO")))))
(defmacro oddo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->looo ^clojure.lang.IFn$LOOO [f]
  (if (instance? clojure.lang.IFn$LOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOO")))))
(defmacro looo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lolo ^clojure.lang.IFn$LOLO [f]
  (if (instance? clojure.lang.IFn$LOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLO")))))
(defmacro lolo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lodo ^clojure.lang.IFn$LODO [f]
  (if (instance? clojure.lang.IFn$LODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODO")))))
(defmacro lodo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lloo ^clojure.lang.IFn$LLOO [f]
  (if (instance? clojure.lang.IFn$LLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOO")))))
(defmacro lloo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lllo ^clojure.lang.IFn$LLLO [f]
  (if (instance? clojure.lang.IFn$LLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLO")))))
(defmacro lllo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lldo ^clojure.lang.IFn$LLDO [f]
  (if (instance? clojure.lang.IFn$LLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDO")))))
(defmacro lldo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ldoo ^clojure.lang.IFn$LDOO [f]
  (if (instance? clojure.lang.IFn$LDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOO")))))
(defmacro ldoo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ldlo ^clojure.lang.IFn$LDLO [f]
  (if (instance? clojure.lang.IFn$LDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLO")))))
(defmacro ldlo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lddo ^clojure.lang.IFn$LDDO [f]
  (if (instance? clojure.lang.IFn$LDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDO")))))
(defmacro lddo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dooo ^clojure.lang.IFn$DOOO [f]
  (if (instance? clojure.lang.IFn$DOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOO")))))
(defmacro dooo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dolo ^clojure.lang.IFn$DOLO [f]
  (if (instance? clojure.lang.IFn$DOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLO")))))
(defmacro dolo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dodo ^clojure.lang.IFn$DODO [f]
  (if (instance? clojure.lang.IFn$DODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODO")))))
(defmacro dodo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dloo ^clojure.lang.IFn$DLOO [f]
  (if (instance? clojure.lang.IFn$DLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOO")))))
(defmacro dloo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dllo ^clojure.lang.IFn$DLLO [f]
  (if (instance? clojure.lang.IFn$DLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLO")))))
(defmacro dllo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dldo ^clojure.lang.IFn$DLDO [f]
  (if (instance? clojure.lang.IFn$DLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDO")))))
(defmacro dldo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ddoo ^clojure.lang.IFn$DDOO [f]
  (if (instance? clojure.lang.IFn$DDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOO")))))
(defmacro ddoo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ddlo ^clojure.lang.IFn$DDLO [f]
  (if (instance? clojure.lang.IFn$DDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLO")))))
(defmacro ddlo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dddo ^clojure.lang.IFn$DDDO [f]
  (if (instance? clojure.lang.IFn$DDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDO")))))
(defmacro dddo [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oool ^clojure.lang.IFn$OOOL [f]
  (if (instance? clojure.lang.IFn$OOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOL")))))
(defmacro oool [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ooll ^clojure.lang.IFn$OOLL [f]
  (if (instance? clojure.lang.IFn$OOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLL")))))
(defmacro ooll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oodl ^clojure.lang.IFn$OODL [f]
  (if (instance? clojure.lang.IFn$OODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODL")))))
(defmacro oodl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->olol ^clojure.lang.IFn$OLOL [f]
  (if (instance? clojure.lang.IFn$OLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOL")))))
(defmacro olol [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->olll ^clojure.lang.IFn$OLLL [f]
  (if (instance? clojure.lang.IFn$OLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLL")))))
(defmacro olll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oldl ^clojure.lang.IFn$OLDL [f]
  (if (instance? clojure.lang.IFn$OLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDL")))))
(defmacro oldl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->odol ^clojure.lang.IFn$ODOL [f]
  (if (instance? clojure.lang.IFn$ODOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOL")))))
(defmacro odol [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->odll ^clojure.lang.IFn$ODLL [f]
  (if (instance? clojure.lang.IFn$ODLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLL")))))
(defmacro odll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oddl ^clojure.lang.IFn$ODDL [f]
  (if (instance? clojure.lang.IFn$ODDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDL")))))
(defmacro oddl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lool ^clojure.lang.IFn$LOOL [f]
  (if (instance? clojure.lang.IFn$LOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOL")))))
(defmacro lool [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->loll ^clojure.lang.IFn$LOLL [f]
  (if (instance? clojure.lang.IFn$LOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLL")))))
(defmacro loll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lodl ^clojure.lang.IFn$LODL [f]
  (if (instance? clojure.lang.IFn$LODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODL")))))
(defmacro lodl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->llol ^clojure.lang.IFn$LLOL [f]
  (if (instance? clojure.lang.IFn$LLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOL")))))
(defmacro llol [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->llll ^clojure.lang.IFn$LLLL [f]
  (if (instance? clojure.lang.IFn$LLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLL")))))
(defmacro llll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lldl ^clojure.lang.IFn$LLDL [f]
  (if (instance? clojure.lang.IFn$LLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDL")))))
(defmacro lldl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ldol ^clojure.lang.IFn$LDOL [f]
  (if (instance? clojure.lang.IFn$LDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOL")))))
(defmacro ldol [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ldll ^clojure.lang.IFn$LDLL [f]
  (if (instance? clojure.lang.IFn$LDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLL")))))
(defmacro ldll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lddl ^clojure.lang.IFn$LDDL [f]
  (if (instance? clojure.lang.IFn$LDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDL")))))
(defmacro lddl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dool ^clojure.lang.IFn$DOOL [f]
  (if (instance? clojure.lang.IFn$DOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOL")))))
(defmacro dool [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->doll ^clojure.lang.IFn$DOLL [f]
  (if (instance? clojure.lang.IFn$DOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLL")))))
(defmacro doll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dodl ^clojure.lang.IFn$DODL [f]
  (if (instance? clojure.lang.IFn$DODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODL")))))
(defmacro dodl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dlol ^clojure.lang.IFn$DLOL [f]
  (if (instance? clojure.lang.IFn$DLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOL")))))
(defmacro dlol [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dlll ^clojure.lang.IFn$DLLL [f]
  (if (instance? clojure.lang.IFn$DLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLL")))))
(defmacro dlll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dldl ^clojure.lang.IFn$DLDL [f]
  (if (instance? clojure.lang.IFn$DLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDL")))))
(defmacro dldl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ddol ^clojure.lang.IFn$DDOL [f]
  (if (instance? clojure.lang.IFn$DDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOL")))))
(defmacro ddol [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ddll ^clojure.lang.IFn$DDLL [f]
  (if (instance? clojure.lang.IFn$DDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLL")))))
(defmacro ddll [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dddl ^clojure.lang.IFn$DDDL [f]
  (if (instance? clojure.lang.IFn$DDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDL")))))
(defmacro dddl [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oood ^clojure.lang.IFn$OOOD [f]
  (if (instance? clojure.lang.IFn$OOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOD")))))
(defmacro oood [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oold ^clojure.lang.IFn$OOLD [f]
  (if (instance? clojure.lang.IFn$OOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLD")))))
(defmacro oold [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oodd ^clojure.lang.IFn$OODD [f]
  (if (instance? clojure.lang.IFn$OODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODD")))))
(defmacro oodd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->olod ^clojure.lang.IFn$OLOD [f]
  (if (instance? clojure.lang.IFn$OLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOD")))))
(defmacro olod [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->olld ^clojure.lang.IFn$OLLD [f]
  (if (instance? clojure.lang.IFn$OLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLD")))))
(defmacro olld [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oldd ^clojure.lang.IFn$OLDD [f]
  (if (instance? clojure.lang.IFn$OLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDD")))))
(defmacro oldd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->odod ^clojure.lang.IFn$ODOD [f]
  (if (instance? clojure.lang.IFn$ODOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOD")))))
(defmacro odod [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->odld ^clojure.lang.IFn$ODLD [f]
  (if (instance? clojure.lang.IFn$ODLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLD")))))
(defmacro odld [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->oddd ^clojure.lang.IFn$ODDD [f]
  (if (instance? clojure.lang.IFn$ODDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDD")))))
(defmacro oddd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lood ^clojure.lang.IFn$LOOD [f]
  (if (instance? clojure.lang.IFn$LOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOD")))))
(defmacro lood [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lold ^clojure.lang.IFn$LOLD [f]
  (if (instance? clojure.lang.IFn$LOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLD")))))
(defmacro lold [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lodd ^clojure.lang.IFn$LODD [f]
  (if (instance? clojure.lang.IFn$LODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODD")))))
(defmacro lodd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->llod ^clojure.lang.IFn$LLOD [f]
  (if (instance? clojure.lang.IFn$LLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOD")))))
(defmacro llod [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->llld ^clojure.lang.IFn$LLLD [f]
  (if (instance? clojure.lang.IFn$LLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLD")))))
(defmacro llld [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lldd ^clojure.lang.IFn$LLDD [f]
  (if (instance? clojure.lang.IFn$LLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDD")))))
(defmacro lldd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ldod ^clojure.lang.IFn$LDOD [f]
  (if (instance? clojure.lang.IFn$LDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOD")))))
(defmacro ldod [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ldld ^clojure.lang.IFn$LDLD [f]
  (if (instance? clojure.lang.IFn$LDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLD")))))
(defmacro ldld [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->lddd ^clojure.lang.IFn$LDDD [f]
  (if (instance? clojure.lang.IFn$LDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDD")))))
(defmacro lddd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dood ^clojure.lang.IFn$DOOD [f]
  (if (instance? clojure.lang.IFn$DOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOD")))))
(defmacro dood [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dold ^clojure.lang.IFn$DOLD [f]
  (if (instance? clojure.lang.IFn$DOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLD")))))
(defmacro dold [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dodd ^clojure.lang.IFn$DODD [f]
  (if (instance? clojure.lang.IFn$DODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODD")))))
(defmacro dodd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dlod ^clojure.lang.IFn$DLOD [f]
  (if (instance? clojure.lang.IFn$DLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOD")))))
(defmacro dlod [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dlld ^clojure.lang.IFn$DLLD [f]
  (if (instance? clojure.lang.IFn$DLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLD")))))
(defmacro dlld [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dldd ^clojure.lang.IFn$DLDD [f]
  (if (instance? clojure.lang.IFn$DLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDD")))))
(defmacro dldd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ddod ^clojure.lang.IFn$DDOD [f]
  (if (instance? clojure.lang.IFn$DDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOD")))))
(defmacro ddod [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ddld ^clojure.lang.IFn$DDLD [f]
  (if (instance? clojure.lang.IFn$DDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLD")))))
(defmacro ddld [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->dddd ^clojure.lang.IFn$DDDD [f]
  (if (instance? clojure.lang.IFn$DDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDD")))))
(defmacro dddd [f arg0 arg1 arg2]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2))
(defn ->ooolo ^clojure.lang.IFn$OOOLO [f]
  (if (instance? clojure.lang.IFn$OOOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOLO")))))
(defmacro ooolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooodo ^clojure.lang.IFn$OOODO [f]
  (if (instance? clojure.lang.IFn$OOODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOODO")))))
(defmacro ooodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooloo ^clojure.lang.IFn$OOLOO [f]
  (if (instance? clojure.lang.IFn$OOLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLOO")))))
(defmacro ooloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oollo ^clojure.lang.IFn$OOLLO [f]
  (if (instance? clojure.lang.IFn$OOLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLLO")))))
(defmacro oollo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooldo ^clojure.lang.IFn$OOLDO [f]
  (if (instance? clojure.lang.IFn$OOLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLDO")))))
(defmacro ooldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oodoo ^clojure.lang.IFn$OODOO [f]
  (if (instance? clojure.lang.IFn$OODOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODOO")))))
(defmacro oodoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oodlo ^clojure.lang.IFn$OODLO [f]
  (if (instance? clojure.lang.IFn$OODLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODLO")))))
(defmacro oodlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooddo ^clojure.lang.IFn$OODDO [f]
  (if (instance? clojure.lang.IFn$OODDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODDO")))))
(defmacro ooddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olooo ^clojure.lang.IFn$OLOOO [f]
  (if (instance? clojure.lang.IFn$OLOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOOO")))))
(defmacro olooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ololo ^clojure.lang.IFn$OLOLO [f]
  (if (instance? clojure.lang.IFn$OLOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOLO")))))
(defmacro ololo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olodo ^clojure.lang.IFn$OLODO [f]
  (if (instance? clojure.lang.IFn$OLODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLODO")))))
(defmacro olodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olloo ^clojure.lang.IFn$OLLOO [f]
  (if (instance? clojure.lang.IFn$OLLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLOO")))))
(defmacro olloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olllo ^clojure.lang.IFn$OLLLO [f]
  (if (instance? clojure.lang.IFn$OLLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLLO")))))
(defmacro olllo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olldo ^clojure.lang.IFn$OLLDO [f]
  (if (instance? clojure.lang.IFn$OLLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLDO")))))
(defmacro olldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oldoo ^clojure.lang.IFn$OLDOO [f]
  (if (instance? clojure.lang.IFn$OLDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDOO")))))
(defmacro oldoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oldlo ^clojure.lang.IFn$OLDLO [f]
  (if (instance? clojure.lang.IFn$OLDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDLO")))))
(defmacro oldlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olddo ^clojure.lang.IFn$OLDDO [f]
  (if (instance? clojure.lang.IFn$OLDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDDO")))))
(defmacro olddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odooo ^clojure.lang.IFn$ODOOO [f]
  (if (instance? clojure.lang.IFn$ODOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOOO")))))
(defmacro odooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odolo ^clojure.lang.IFn$ODOLO [f]
  (if (instance? clojure.lang.IFn$ODOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOLO")))))
(defmacro odolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ododo ^clojure.lang.IFn$ODODO [f]
  (if (instance? clojure.lang.IFn$ODODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODODO")))))
(defmacro ododo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odloo ^clojure.lang.IFn$ODLOO [f]
  (if (instance? clojure.lang.IFn$ODLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLOO")))))
(defmacro odloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odllo ^clojure.lang.IFn$ODLLO [f]
  (if (instance? clojure.lang.IFn$ODLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLLO")))))
(defmacro odllo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odldo ^clojure.lang.IFn$ODLDO [f]
  (if (instance? clojure.lang.IFn$ODLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLDO")))))
(defmacro odldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oddoo ^clojure.lang.IFn$ODDOO [f]
  (if (instance? clojure.lang.IFn$ODDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDOO")))))
(defmacro oddoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oddlo ^clojure.lang.IFn$ODDLO [f]
  (if (instance? clojure.lang.IFn$ODDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDLO")))))
(defmacro oddlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odddo ^clojure.lang.IFn$ODDDO [f]
  (if (instance? clojure.lang.IFn$ODDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDDO")))))
(defmacro odddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loooo ^clojure.lang.IFn$LOOOO [f]
  (if (instance? clojure.lang.IFn$LOOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOOO")))))
(defmacro loooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loolo ^clojure.lang.IFn$LOOLO [f]
  (if (instance? clojure.lang.IFn$LOOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOLO")))))
(defmacro loolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loodo ^clojure.lang.IFn$LOODO [f]
  (if (instance? clojure.lang.IFn$LOODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOODO")))))
(defmacro loodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loloo ^clojure.lang.IFn$LOLOO [f]
  (if (instance? clojure.lang.IFn$LOLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLOO")))))
(defmacro loloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lollo ^clojure.lang.IFn$LOLLO [f]
  (if (instance? clojure.lang.IFn$LOLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLLO")))))
(defmacro lollo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loldo ^clojure.lang.IFn$LOLDO [f]
  (if (instance? clojure.lang.IFn$LOLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLDO")))))
(defmacro loldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lodoo ^clojure.lang.IFn$LODOO [f]
  (if (instance? clojure.lang.IFn$LODOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODOO")))))
(defmacro lodoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lodlo ^clojure.lang.IFn$LODLO [f]
  (if (instance? clojure.lang.IFn$LODLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODLO")))))
(defmacro lodlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loddo ^clojure.lang.IFn$LODDO [f]
  (if (instance? clojure.lang.IFn$LODDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODDO")))))
(defmacro loddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llooo ^clojure.lang.IFn$LLOOO [f]
  (if (instance? clojure.lang.IFn$LLOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOOO")))))
(defmacro llooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llolo ^clojure.lang.IFn$LLOLO [f]
  (if (instance? clojure.lang.IFn$LLOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOLO")))))
(defmacro llolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llodo ^clojure.lang.IFn$LLODO [f]
  (if (instance? clojure.lang.IFn$LLODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLODO")))))
(defmacro llodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llloo ^clojure.lang.IFn$LLLOO [f]
  (if (instance? clojure.lang.IFn$LLLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLOO")))))
(defmacro llloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llllo ^clojure.lang.IFn$LLLLO [f]
  (if (instance? clojure.lang.IFn$LLLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLLO")))))
(defmacro llllo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llldo ^clojure.lang.IFn$LLLDO [f]
  (if (instance? clojure.lang.IFn$LLLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLDO")))))
(defmacro llldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lldoo ^clojure.lang.IFn$LLDOO [f]
  (if (instance? clojure.lang.IFn$LLDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDOO")))))
(defmacro lldoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lldlo ^clojure.lang.IFn$LLDLO [f]
  (if (instance? clojure.lang.IFn$LLDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDLO")))))
(defmacro lldlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llddo ^clojure.lang.IFn$LLDDO [f]
  (if (instance? clojure.lang.IFn$LLDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDDO")))))
(defmacro llddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldooo ^clojure.lang.IFn$LDOOO [f]
  (if (instance? clojure.lang.IFn$LDOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOOO")))))
(defmacro ldooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldolo ^clojure.lang.IFn$LDOLO [f]
  (if (instance? clojure.lang.IFn$LDOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOLO")))))
(defmacro ldolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldodo ^clojure.lang.IFn$LDODO [f]
  (if (instance? clojure.lang.IFn$LDODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDODO")))))
(defmacro ldodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldloo ^clojure.lang.IFn$LDLOO [f]
  (if (instance? clojure.lang.IFn$LDLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLOO")))))
(defmacro ldloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldllo ^clojure.lang.IFn$LDLLO [f]
  (if (instance? clojure.lang.IFn$LDLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLLO")))))
(defmacro ldllo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldldo ^clojure.lang.IFn$LDLDO [f]
  (if (instance? clojure.lang.IFn$LDLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLDO")))))
(defmacro ldldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lddoo ^clojure.lang.IFn$LDDOO [f]
  (if (instance? clojure.lang.IFn$LDDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDOO")))))
(defmacro lddoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lddlo ^clojure.lang.IFn$LDDLO [f]
  (if (instance? clojure.lang.IFn$LDDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDLO")))))
(defmacro lddlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldddo ^clojure.lang.IFn$LDDDO [f]
  (if (instance? clojure.lang.IFn$LDDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDDO")))))
(defmacro ldddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doooo ^clojure.lang.IFn$DOOOO [f]
  (if (instance? clojure.lang.IFn$DOOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOOO")))))
(defmacro doooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doolo ^clojure.lang.IFn$DOOLO [f]
  (if (instance? clojure.lang.IFn$DOOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOLO")))))
(defmacro doolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doodo ^clojure.lang.IFn$DOODO [f]
  (if (instance? clojure.lang.IFn$DOODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOODO")))))
(defmacro doodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doloo ^clojure.lang.IFn$DOLOO [f]
  (if (instance? clojure.lang.IFn$DOLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLOO")))))
(defmacro doloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dollo ^clojure.lang.IFn$DOLLO [f]
  (if (instance? clojure.lang.IFn$DOLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLLO")))))
(defmacro dollo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doldo ^clojure.lang.IFn$DOLDO [f]
  (if (instance? clojure.lang.IFn$DOLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLDO")))))
(defmacro doldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dodoo ^clojure.lang.IFn$DODOO [f]
  (if (instance? clojure.lang.IFn$DODOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODOO")))))
(defmacro dodoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dodlo ^clojure.lang.IFn$DODLO [f]
  (if (instance? clojure.lang.IFn$DODLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODLO")))))
(defmacro dodlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doddo ^clojure.lang.IFn$DODDO [f]
  (if (instance? clojure.lang.IFn$DODDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODDO")))))
(defmacro doddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlooo ^clojure.lang.IFn$DLOOO [f]
  (if (instance? clojure.lang.IFn$DLOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOOO")))))
(defmacro dlooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlolo ^clojure.lang.IFn$DLOLO [f]
  (if (instance? clojure.lang.IFn$DLOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOLO")))))
(defmacro dlolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlodo ^clojure.lang.IFn$DLODO [f]
  (if (instance? clojure.lang.IFn$DLODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLODO")))))
(defmacro dlodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlloo ^clojure.lang.IFn$DLLOO [f]
  (if (instance? clojure.lang.IFn$DLLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLOO")))))
(defmacro dlloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlllo ^clojure.lang.IFn$DLLLO [f]
  (if (instance? clojure.lang.IFn$DLLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLLO")))))
(defmacro dlllo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlldo ^clojure.lang.IFn$DLLDO [f]
  (if (instance? clojure.lang.IFn$DLLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLDO")))))
(defmacro dlldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dldoo ^clojure.lang.IFn$DLDOO [f]
  (if (instance? clojure.lang.IFn$DLDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDOO")))))
(defmacro dldoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dldlo ^clojure.lang.IFn$DLDLO [f]
  (if (instance? clojure.lang.IFn$DLDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDLO")))))
(defmacro dldlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlddo ^clojure.lang.IFn$DLDDO [f]
  (if (instance? clojure.lang.IFn$DLDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDDO")))))
(defmacro dlddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddooo ^clojure.lang.IFn$DDOOO [f]
  (if (instance? clojure.lang.IFn$DDOOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOOO")))))
(defmacro ddooo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddolo ^clojure.lang.IFn$DDOLO [f]
  (if (instance? clojure.lang.IFn$DDOLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOLO")))))
(defmacro ddolo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddodo ^clojure.lang.IFn$DDODO [f]
  (if (instance? clojure.lang.IFn$DDODO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDODO")))))
(defmacro ddodo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddloo ^clojure.lang.IFn$DDLOO [f]
  (if (instance? clojure.lang.IFn$DDLOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLOO")))))
(defmacro ddloo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddllo ^clojure.lang.IFn$DDLLO [f]
  (if (instance? clojure.lang.IFn$DDLLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLLO")))))
(defmacro ddllo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddldo ^clojure.lang.IFn$DDLDO [f]
  (if (instance? clojure.lang.IFn$DDLDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLDO")))))
(defmacro ddldo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dddoo ^clojure.lang.IFn$DDDOO [f]
  (if (instance? clojure.lang.IFn$DDDOO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDOO")))))
(defmacro dddoo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dddlo ^clojure.lang.IFn$DDDLO [f]
  (if (instance? clojure.lang.IFn$DDDLO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDLO")))))
(defmacro dddlo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddddo ^clojure.lang.IFn$DDDDO [f]
  (if (instance? clojure.lang.IFn$DDDDO f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDDO")))))
(defmacro ddddo [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooool ^clojure.lang.IFn$OOOOL [f]
  (if (instance? clojure.lang.IFn$OOOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOOL")))))
(defmacro ooool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oooll ^clojure.lang.IFn$OOOLL [f]
  (if (instance? clojure.lang.IFn$OOOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOLL")))))
(defmacro oooll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooodl ^clojure.lang.IFn$OOODL [f]
  (if (instance? clojure.lang.IFn$OOODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOODL")))))
(defmacro ooodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oolol ^clojure.lang.IFn$OOLOL [f]
  (if (instance? clojure.lang.IFn$OOLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLOL")))))
(defmacro oolol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oolll ^clojure.lang.IFn$OOLLL [f]
  (if (instance? clojure.lang.IFn$OOLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLLL")))))
(defmacro oolll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooldl ^clojure.lang.IFn$OOLDL [f]
  (if (instance? clojure.lang.IFn$OOLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLDL")))))
(defmacro ooldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oodol ^clojure.lang.IFn$OODOL [f]
  (if (instance? clojure.lang.IFn$OODOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODOL")))))
(defmacro oodol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oodll ^clojure.lang.IFn$OODLL [f]
  (if (instance? clojure.lang.IFn$OODLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODLL")))))
(defmacro oodll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooddl ^clojure.lang.IFn$OODDL [f]
  (if (instance? clojure.lang.IFn$OODDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODDL")))))
(defmacro ooddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olool ^clojure.lang.IFn$OLOOL [f]
  (if (instance? clojure.lang.IFn$OLOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOOL")))))
(defmacro olool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ololl ^clojure.lang.IFn$OLOLL [f]
  (if (instance? clojure.lang.IFn$OLOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOLL")))))
(defmacro ololl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olodl ^clojure.lang.IFn$OLODL [f]
  (if (instance? clojure.lang.IFn$OLODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLODL")))))
(defmacro olodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ollol ^clojure.lang.IFn$OLLOL [f]
  (if (instance? clojure.lang.IFn$OLLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLOL")))))
(defmacro ollol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ollll ^clojure.lang.IFn$OLLLL [f]
  (if (instance? clojure.lang.IFn$OLLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLLL")))))
(defmacro ollll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olldl ^clojure.lang.IFn$OLLDL [f]
  (if (instance? clojure.lang.IFn$OLLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLDL")))))
(defmacro olldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oldol ^clojure.lang.IFn$OLDOL [f]
  (if (instance? clojure.lang.IFn$OLDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDOL")))))
(defmacro oldol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oldll ^clojure.lang.IFn$OLDLL [f]
  (if (instance? clojure.lang.IFn$OLDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDLL")))))
(defmacro oldll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olddl ^clojure.lang.IFn$OLDDL [f]
  (if (instance? clojure.lang.IFn$OLDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDDL")))))
(defmacro olddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odool ^clojure.lang.IFn$ODOOL [f]
  (if (instance? clojure.lang.IFn$ODOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOOL")))))
(defmacro odool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odoll ^clojure.lang.IFn$ODOLL [f]
  (if (instance? clojure.lang.IFn$ODOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOLL")))))
(defmacro odoll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ododl ^clojure.lang.IFn$ODODL [f]
  (if (instance? clojure.lang.IFn$ODODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODODL")))))
(defmacro ododl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odlol ^clojure.lang.IFn$ODLOL [f]
  (if (instance? clojure.lang.IFn$ODLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLOL")))))
(defmacro odlol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odlll ^clojure.lang.IFn$ODLLL [f]
  (if (instance? clojure.lang.IFn$ODLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLLL")))))
(defmacro odlll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odldl ^clojure.lang.IFn$ODLDL [f]
  (if (instance? clojure.lang.IFn$ODLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLDL")))))
(defmacro odldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oddol ^clojure.lang.IFn$ODDOL [f]
  (if (instance? clojure.lang.IFn$ODDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDOL")))))
(defmacro oddol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oddll ^clojure.lang.IFn$ODDLL [f]
  (if (instance? clojure.lang.IFn$ODDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDLL")))))
(defmacro oddll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odddl ^clojure.lang.IFn$ODDDL [f]
  (if (instance? clojure.lang.IFn$ODDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDDL")))))
(defmacro odddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loool ^clojure.lang.IFn$LOOOL [f]
  (if (instance? clojure.lang.IFn$LOOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOOL")))))
(defmacro loool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->looll ^clojure.lang.IFn$LOOLL [f]
  (if (instance? clojure.lang.IFn$LOOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOLL")))))
(defmacro looll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loodl ^clojure.lang.IFn$LOODL [f]
  (if (instance? clojure.lang.IFn$LOODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOODL")))))
(defmacro loodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lolol ^clojure.lang.IFn$LOLOL [f]
  (if (instance? clojure.lang.IFn$LOLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLOL")))))
(defmacro lolol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lolll ^clojure.lang.IFn$LOLLL [f]
  (if (instance? clojure.lang.IFn$LOLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLLL")))))
(defmacro lolll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loldl ^clojure.lang.IFn$LOLDL [f]
  (if (instance? clojure.lang.IFn$LOLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLDL")))))
(defmacro loldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lodol ^clojure.lang.IFn$LODOL [f]
  (if (instance? clojure.lang.IFn$LODOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODOL")))))
(defmacro lodol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lodll ^clojure.lang.IFn$LODLL [f]
  (if (instance? clojure.lang.IFn$LODLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODLL")))))
(defmacro lodll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loddl ^clojure.lang.IFn$LODDL [f]
  (if (instance? clojure.lang.IFn$LODDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODDL")))))
(defmacro loddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llool ^clojure.lang.IFn$LLOOL [f]
  (if (instance? clojure.lang.IFn$LLOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOOL")))))
(defmacro llool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lloll ^clojure.lang.IFn$LLOLL [f]
  (if (instance? clojure.lang.IFn$LLOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOLL")))))
(defmacro lloll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llodl ^clojure.lang.IFn$LLODL [f]
  (if (instance? clojure.lang.IFn$LLODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLODL")))))
(defmacro llodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lllol ^clojure.lang.IFn$LLLOL [f]
  (if (instance? clojure.lang.IFn$LLLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLOL")))))
(defmacro lllol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lllll ^clojure.lang.IFn$LLLLL [f]
  (if (instance? clojure.lang.IFn$LLLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLLL")))))
(defmacro lllll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llldl ^clojure.lang.IFn$LLLDL [f]
  (if (instance? clojure.lang.IFn$LLLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLDL")))))
(defmacro llldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lldol ^clojure.lang.IFn$LLDOL [f]
  (if (instance? clojure.lang.IFn$LLDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDOL")))))
(defmacro lldol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lldll ^clojure.lang.IFn$LLDLL [f]
  (if (instance? clojure.lang.IFn$LLDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDLL")))))
(defmacro lldll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llddl ^clojure.lang.IFn$LLDDL [f]
  (if (instance? clojure.lang.IFn$LLDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDDL")))))
(defmacro llddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldool ^clojure.lang.IFn$LDOOL [f]
  (if (instance? clojure.lang.IFn$LDOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOOL")))))
(defmacro ldool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldoll ^clojure.lang.IFn$LDOLL [f]
  (if (instance? clojure.lang.IFn$LDOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOLL")))))
(defmacro ldoll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldodl ^clojure.lang.IFn$LDODL [f]
  (if (instance? clojure.lang.IFn$LDODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDODL")))))
(defmacro ldodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldlol ^clojure.lang.IFn$LDLOL [f]
  (if (instance? clojure.lang.IFn$LDLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLOL")))))
(defmacro ldlol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldlll ^clojure.lang.IFn$LDLLL [f]
  (if (instance? clojure.lang.IFn$LDLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLLL")))))
(defmacro ldlll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldldl ^clojure.lang.IFn$LDLDL [f]
  (if (instance? clojure.lang.IFn$LDLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLDL")))))
(defmacro ldldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lddol ^clojure.lang.IFn$LDDOL [f]
  (if (instance? clojure.lang.IFn$LDDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDOL")))))
(defmacro lddol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lddll ^clojure.lang.IFn$LDDLL [f]
  (if (instance? clojure.lang.IFn$LDDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDLL")))))
(defmacro lddll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldddl ^clojure.lang.IFn$LDDDL [f]
  (if (instance? clojure.lang.IFn$LDDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDDL")))))
(defmacro ldddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doool ^clojure.lang.IFn$DOOOL [f]
  (if (instance? clojure.lang.IFn$DOOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOOL")))))
(defmacro doool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dooll ^clojure.lang.IFn$DOOLL [f]
  (if (instance? clojure.lang.IFn$DOOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOLL")))))
(defmacro dooll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doodl ^clojure.lang.IFn$DOODL [f]
  (if (instance? clojure.lang.IFn$DOODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOODL")))))
(defmacro doodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dolol ^clojure.lang.IFn$DOLOL [f]
  (if (instance? clojure.lang.IFn$DOLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLOL")))))
(defmacro dolol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dolll ^clojure.lang.IFn$DOLLL [f]
  (if (instance? clojure.lang.IFn$DOLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLLL")))))
(defmacro dolll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doldl ^clojure.lang.IFn$DOLDL [f]
  (if (instance? clojure.lang.IFn$DOLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLDL")))))
(defmacro doldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dodol ^clojure.lang.IFn$DODOL [f]
  (if (instance? clojure.lang.IFn$DODOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODOL")))))
(defmacro dodol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dodll ^clojure.lang.IFn$DODLL [f]
  (if (instance? clojure.lang.IFn$DODLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODLL")))))
(defmacro dodll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doddl ^clojure.lang.IFn$DODDL [f]
  (if (instance? clojure.lang.IFn$DODDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODDL")))))
(defmacro doddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlool ^clojure.lang.IFn$DLOOL [f]
  (if (instance? clojure.lang.IFn$DLOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOOL")))))
(defmacro dlool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dloll ^clojure.lang.IFn$DLOLL [f]
  (if (instance? clojure.lang.IFn$DLOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOLL")))))
(defmacro dloll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlodl ^clojure.lang.IFn$DLODL [f]
  (if (instance? clojure.lang.IFn$DLODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLODL")))))
(defmacro dlodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dllol ^clojure.lang.IFn$DLLOL [f]
  (if (instance? clojure.lang.IFn$DLLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLOL")))))
(defmacro dllol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dllll ^clojure.lang.IFn$DLLLL [f]
  (if (instance? clojure.lang.IFn$DLLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLLL")))))
(defmacro dllll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlldl ^clojure.lang.IFn$DLLDL [f]
  (if (instance? clojure.lang.IFn$DLLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLDL")))))
(defmacro dlldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dldol ^clojure.lang.IFn$DLDOL [f]
  (if (instance? clojure.lang.IFn$DLDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDOL")))))
(defmacro dldol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dldll ^clojure.lang.IFn$DLDLL [f]
  (if (instance? clojure.lang.IFn$DLDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDLL")))))
(defmacro dldll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlddl ^clojure.lang.IFn$DLDDL [f]
  (if (instance? clojure.lang.IFn$DLDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDDL")))))
(defmacro dlddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddool ^clojure.lang.IFn$DDOOL [f]
  (if (instance? clojure.lang.IFn$DDOOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOOL")))))
(defmacro ddool [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddoll ^clojure.lang.IFn$DDOLL [f]
  (if (instance? clojure.lang.IFn$DDOLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOLL")))))
(defmacro ddoll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddodl ^clojure.lang.IFn$DDODL [f]
  (if (instance? clojure.lang.IFn$DDODL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDODL")))))
(defmacro ddodl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddlol ^clojure.lang.IFn$DDLOL [f]
  (if (instance? clojure.lang.IFn$DDLOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLOL")))))
(defmacro ddlol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddlll ^clojure.lang.IFn$DDLLL [f]
  (if (instance? clojure.lang.IFn$DDLLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLLL")))))
(defmacro ddlll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddldl ^clojure.lang.IFn$DDLDL [f]
  (if (instance? clojure.lang.IFn$DDLDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLDL")))))
(defmacro ddldl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dddol ^clojure.lang.IFn$DDDOL [f]
  (if (instance? clojure.lang.IFn$DDDOL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDOL")))))
(defmacro dddol [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dddll ^clojure.lang.IFn$DDDLL [f]
  (if (instance? clojure.lang.IFn$DDDLL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDLL")))))
(defmacro dddll [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddddl ^clojure.lang.IFn$DDDDL [f]
  (if (instance? clojure.lang.IFn$DDDDL f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDDL")))))
(defmacro ddddl [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooood ^clojure.lang.IFn$OOOOD [f]
  (if (instance? clojure.lang.IFn$OOOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOOD")))))
(defmacro ooood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooold ^clojure.lang.IFn$OOOLD [f]
  (if (instance? clojure.lang.IFn$OOOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOOLD")))))
(defmacro ooold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooodd ^clojure.lang.IFn$OOODD [f]
  (if (instance? clojure.lang.IFn$OOODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOODD")))))
(defmacro ooodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oolod ^clojure.lang.IFn$OOLOD [f]
  (if (instance? clojure.lang.IFn$OOLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLOD")))))
(defmacro oolod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oolld ^clojure.lang.IFn$OOLLD [f]
  (if (instance? clojure.lang.IFn$OOLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLLD")))))
(defmacro oolld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooldd ^clojure.lang.IFn$OOLDD [f]
  (if (instance? clojure.lang.IFn$OOLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OOLDD")))))
(defmacro ooldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oodod ^clojure.lang.IFn$OODOD [f]
  (if (instance? clojure.lang.IFn$OODOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODOD")))))
(defmacro oodod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oodld ^clojure.lang.IFn$OODLD [f]
  (if (instance? clojure.lang.IFn$OODLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODLD")))))
(defmacro oodld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ooddd ^clojure.lang.IFn$OODDD [f]
  (if (instance? clojure.lang.IFn$OODDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OODDD")))))
(defmacro ooddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olood ^clojure.lang.IFn$OLOOD [f]
  (if (instance? clojure.lang.IFn$OLOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOOD")))))
(defmacro olood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olold ^clojure.lang.IFn$OLOLD [f]
  (if (instance? clojure.lang.IFn$OLOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLOLD")))))
(defmacro olold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olodd ^clojure.lang.IFn$OLODD [f]
  (if (instance? clojure.lang.IFn$OLODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLODD")))))
(defmacro olodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ollod ^clojure.lang.IFn$OLLOD [f]
  (if (instance? clojure.lang.IFn$OLLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLOD")))))
(defmacro ollod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ollld ^clojure.lang.IFn$OLLLD [f]
  (if (instance? clojure.lang.IFn$OLLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLLD")))))
(defmacro ollld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olldd ^clojure.lang.IFn$OLLDD [f]
  (if (instance? clojure.lang.IFn$OLLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLLDD")))))
(defmacro olldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oldod ^clojure.lang.IFn$OLDOD [f]
  (if (instance? clojure.lang.IFn$OLDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDOD")))))
(defmacro oldod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oldld ^clojure.lang.IFn$OLDLD [f]
  (if (instance? clojure.lang.IFn$OLDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDLD")))))
(defmacro oldld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->olddd ^clojure.lang.IFn$OLDDD [f]
  (if (instance? clojure.lang.IFn$OLDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$OLDDD")))))
(defmacro olddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odood ^clojure.lang.IFn$ODOOD [f]
  (if (instance? clojure.lang.IFn$ODOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOOD")))))
(defmacro odood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odold ^clojure.lang.IFn$ODOLD [f]
  (if (instance? clojure.lang.IFn$ODOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODOLD")))))
(defmacro odold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ododd ^clojure.lang.IFn$ODODD [f]
  (if (instance? clojure.lang.IFn$ODODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODODD")))))
(defmacro ododd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odlod ^clojure.lang.IFn$ODLOD [f]
  (if (instance? clojure.lang.IFn$ODLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLOD")))))
(defmacro odlod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odlld ^clojure.lang.IFn$ODLLD [f]
  (if (instance? clojure.lang.IFn$ODLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLLD")))))
(defmacro odlld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odldd ^clojure.lang.IFn$ODLDD [f]
  (if (instance? clojure.lang.IFn$ODLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODLDD")))))
(defmacro odldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oddod ^clojure.lang.IFn$ODDOD [f]
  (if (instance? clojure.lang.IFn$ODDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDOD")))))
(defmacro oddod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->oddld ^clojure.lang.IFn$ODDLD [f]
  (if (instance? clojure.lang.IFn$ODDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDLD")))))
(defmacro oddld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->odddd ^clojure.lang.IFn$ODDDD [f]
  (if (instance? clojure.lang.IFn$ODDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$ODDDD")))))
(defmacro odddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loood ^clojure.lang.IFn$LOOOD [f]
  (if (instance? clojure.lang.IFn$LOOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOOD")))))
(defmacro loood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loold ^clojure.lang.IFn$LOOLD [f]
  (if (instance? clojure.lang.IFn$LOOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOOLD")))))
(defmacro loold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loodd ^clojure.lang.IFn$LOODD [f]
  (if (instance? clojure.lang.IFn$LOODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOODD")))))
(defmacro loodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lolod ^clojure.lang.IFn$LOLOD [f]
  (if (instance? clojure.lang.IFn$LOLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLOD")))))
(defmacro lolod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lolld ^clojure.lang.IFn$LOLLD [f]
  (if (instance? clojure.lang.IFn$LOLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLLD")))))
(defmacro lolld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loldd ^clojure.lang.IFn$LOLDD [f]
  (if (instance? clojure.lang.IFn$LOLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LOLDD")))))
(defmacro loldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lodod ^clojure.lang.IFn$LODOD [f]
  (if (instance? clojure.lang.IFn$LODOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODOD")))))
(defmacro lodod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lodld ^clojure.lang.IFn$LODLD [f]
  (if (instance? clojure.lang.IFn$LODLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODLD")))))
(defmacro lodld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->loddd ^clojure.lang.IFn$LODDD [f]
  (if (instance? clojure.lang.IFn$LODDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LODDD")))))
(defmacro loddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llood ^clojure.lang.IFn$LLOOD [f]
  (if (instance? clojure.lang.IFn$LLOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOOD")))))
(defmacro llood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llold ^clojure.lang.IFn$LLOLD [f]
  (if (instance? clojure.lang.IFn$LLOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLOLD")))))
(defmacro llold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llodd ^clojure.lang.IFn$LLODD [f]
  (if (instance? clojure.lang.IFn$LLODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLODD")))))
(defmacro llodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lllod ^clojure.lang.IFn$LLLOD [f]
  (if (instance? clojure.lang.IFn$LLLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLOD")))))
(defmacro lllod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lllld ^clojure.lang.IFn$LLLLD [f]
  (if (instance? clojure.lang.IFn$LLLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLLD")))))
(defmacro lllld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llldd ^clojure.lang.IFn$LLLDD [f]
  (if (instance? clojure.lang.IFn$LLLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLLDD")))))
(defmacro llldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lldod ^clojure.lang.IFn$LLDOD [f]
  (if (instance? clojure.lang.IFn$LLDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDOD")))))
(defmacro lldod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lldld ^clojure.lang.IFn$LLDLD [f]
  (if (instance? clojure.lang.IFn$LLDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDLD")))))
(defmacro lldld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->llddd ^clojure.lang.IFn$LLDDD [f]
  (if (instance? clojure.lang.IFn$LLDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LLDDD")))))
(defmacro llddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldood ^clojure.lang.IFn$LDOOD [f]
  (if (instance? clojure.lang.IFn$LDOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOOD")))))
(defmacro ldood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldold ^clojure.lang.IFn$LDOLD [f]
  (if (instance? clojure.lang.IFn$LDOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDOLD")))))
(defmacro ldold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldodd ^clojure.lang.IFn$LDODD [f]
  (if (instance? clojure.lang.IFn$LDODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDODD")))))
(defmacro ldodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldlod ^clojure.lang.IFn$LDLOD [f]
  (if (instance? clojure.lang.IFn$LDLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLOD")))))
(defmacro ldlod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldlld ^clojure.lang.IFn$LDLLD [f]
  (if (instance? clojure.lang.IFn$LDLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLLD")))))
(defmacro ldlld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldldd ^clojure.lang.IFn$LDLDD [f]
  (if (instance? clojure.lang.IFn$LDLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDLDD")))))
(defmacro ldldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lddod ^clojure.lang.IFn$LDDOD [f]
  (if (instance? clojure.lang.IFn$LDDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDOD")))))
(defmacro lddod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->lddld ^clojure.lang.IFn$LDDLD [f]
  (if (instance? clojure.lang.IFn$LDDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDLD")))))
(defmacro lddld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ldddd ^clojure.lang.IFn$LDDDD [f]
  (if (instance? clojure.lang.IFn$LDDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$LDDDD")))))
(defmacro ldddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doood ^clojure.lang.IFn$DOOOD [f]
  (if (instance? clojure.lang.IFn$DOOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOOD")))))
(defmacro doood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doold ^clojure.lang.IFn$DOOLD [f]
  (if (instance? clojure.lang.IFn$DOOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOOLD")))))
(defmacro doold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doodd ^clojure.lang.IFn$DOODD [f]
  (if (instance? clojure.lang.IFn$DOODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOODD")))))
(defmacro doodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dolod ^clojure.lang.IFn$DOLOD [f]
  (if (instance? clojure.lang.IFn$DOLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLOD")))))
(defmacro dolod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dolld ^clojure.lang.IFn$DOLLD [f]
  (if (instance? clojure.lang.IFn$DOLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLLD")))))
(defmacro dolld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doldd ^clojure.lang.IFn$DOLDD [f]
  (if (instance? clojure.lang.IFn$DOLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DOLDD")))))
(defmacro doldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dodod ^clojure.lang.IFn$DODOD [f]
  (if (instance? clojure.lang.IFn$DODOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODOD")))))
(defmacro dodod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dodld ^clojure.lang.IFn$DODLD [f]
  (if (instance? clojure.lang.IFn$DODLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODLD")))))
(defmacro dodld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->doddd ^clojure.lang.IFn$DODDD [f]
  (if (instance? clojure.lang.IFn$DODDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DODDD")))))
(defmacro doddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlood ^clojure.lang.IFn$DLOOD [f]
  (if (instance? clojure.lang.IFn$DLOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOOD")))))
(defmacro dlood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlold ^clojure.lang.IFn$DLOLD [f]
  (if (instance? clojure.lang.IFn$DLOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLOLD")))))
(defmacro dlold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlodd ^clojure.lang.IFn$DLODD [f]
  (if (instance? clojure.lang.IFn$DLODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLODD")))))
(defmacro dlodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dllod ^clojure.lang.IFn$DLLOD [f]
  (if (instance? clojure.lang.IFn$DLLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLOD")))))
(defmacro dllod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dllld ^clojure.lang.IFn$DLLLD [f]
  (if (instance? clojure.lang.IFn$DLLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLLD")))))
(defmacro dllld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlldd ^clojure.lang.IFn$DLLDD [f]
  (if (instance? clojure.lang.IFn$DLLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLLDD")))))
(defmacro dlldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dldod ^clojure.lang.IFn$DLDOD [f]
  (if (instance? clojure.lang.IFn$DLDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDOD")))))
(defmacro dldod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dldld ^clojure.lang.IFn$DLDLD [f]
  (if (instance? clojure.lang.IFn$DLDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDLD")))))
(defmacro dldld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dlddd ^clojure.lang.IFn$DLDDD [f]
  (if (instance? clojure.lang.IFn$DLDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DLDDD")))))
(defmacro dlddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddood ^clojure.lang.IFn$DDOOD [f]
  (if (instance? clojure.lang.IFn$DDOOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOOD")))))
(defmacro ddood [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddold ^clojure.lang.IFn$DDOLD [f]
  (if (instance? clojure.lang.IFn$DDOLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDOLD")))))
(defmacro ddold [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddodd ^clojure.lang.IFn$DDODD [f]
  (if (instance? clojure.lang.IFn$DDODD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDODD")))))
(defmacro ddodd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddlod ^clojure.lang.IFn$DDLOD [f]
  (if (instance? clojure.lang.IFn$DDLOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLOD")))))
(defmacro ddlod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddlld ^clojure.lang.IFn$DDLLD [f]
  (if (instance? clojure.lang.IFn$DDLLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLLD")))))
(defmacro ddlld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddldd ^clojure.lang.IFn$DDLDD [f]
  (if (instance? clojure.lang.IFn$DDLDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDLDD")))))
(defmacro ddldd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dddod ^clojure.lang.IFn$DDDOD [f]
  (if (instance? clojure.lang.IFn$DDDOD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDOD")))))
(defmacro dddod [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->dddld ^clojure.lang.IFn$DDDLD [f]
  (if (instance? clojure.lang.IFn$DDDLD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDLD")))))
(defmacro dddld [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))
(defn ->ddddd ^clojure.lang.IFn$DDDDD [f]
  (if (instance? clojure.lang.IFn$DDDDD f)
    f
    (throw (RuntimeException. (str f " is not an instance ofclojure.lang.IFn$DDDDD")))))
(defmacro ddddd [f arg0 arg1 arg2 arg3]
`(.invokePrim ~f ~arg0 ~arg1 ~arg2 ~arg3))

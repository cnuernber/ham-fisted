(ns ham-fisted.primitive-invoke
"For statically traced calls the Clojure compiler calls the primitive version of type-hinted functions
  and this makes quite a difference in tight loops.  Often times, however, functions are passed by values
  or returned from if-statements and then you need to explicitly call the primitive overload - this makes
  that pathway less verbose.")

(defn as-l ^clojure.lang.IFn$L[f] f)
(defmacro l [f]
`(.invokePrim (as-l ~f)))
(defn as-d ^clojure.lang.IFn$D[f] f)
(defmacro d [f]
`(.invokePrim (as-d ~f)))
(defn as-lo ^clojure.lang.IFn$LO[f] f)
(defmacro lo [f arg0]
`(.invokePrim (as-lo ~f) ~arg0))
(defn as-do ^clojure.lang.IFn$DO[f] f)
(defmacro do [f arg0]
`(.invokePrim (as-do ~f) ~arg0))
(defn as-ol ^clojure.lang.IFn$OL[f] f)
(defmacro ol [f arg0]
`(.invokePrim (as-ol ~f) ~arg0))
(defn as-ll ^clojure.lang.IFn$LL[f] f)
(defmacro ll [f arg0]
`(.invokePrim (as-ll ~f) ~arg0))
(defn as-dl ^clojure.lang.IFn$DL[f] f)
(defmacro dl [f arg0]
`(.invokePrim (as-dl ~f) ~arg0))
(defn as-od ^clojure.lang.IFn$OD[f] f)
(defmacro od [f arg0]
`(.invokePrim (as-od ~f) ~arg0))
(defn as-ld ^clojure.lang.IFn$LD[f] f)
(defmacro ld [f arg0]
`(.invokePrim (as-ld ~f) ~arg0))
(defn as-dd ^clojure.lang.IFn$DD[f] f)
(defmacro dd [f arg0]
`(.invokePrim (as-dd ~f) ~arg0))
(defn as-olo ^clojure.lang.IFn$OLO[f] f)
(defmacro olo [f arg0 arg1]
`(.invokePrim (as-olo ~f) ~arg0 ~arg1))
(defn as-odo ^clojure.lang.IFn$ODO[f] f)
(defmacro odo [f arg0 arg1]
`(.invokePrim (as-odo ~f) ~arg0 ~arg1))
(defn as-loo ^clojure.lang.IFn$LOO[f] f)
(defmacro loo [f arg0 arg1]
`(.invokePrim (as-loo ~f) ~arg0 ~arg1))
(defn as-llo ^clojure.lang.IFn$LLO[f] f)
(defmacro llo [f arg0 arg1]
`(.invokePrim (as-llo ~f) ~arg0 ~arg1))
(defn as-ldo ^clojure.lang.IFn$LDO[f] f)
(defmacro ldo [f arg0 arg1]
`(.invokePrim (as-ldo ~f) ~arg0 ~arg1))
(defn as-doo ^clojure.lang.IFn$DOO[f] f)
(defmacro doo [f arg0 arg1]
`(.invokePrim (as-doo ~f) ~arg0 ~arg1))
(defn as-dlo ^clojure.lang.IFn$DLO[f] f)
(defmacro dlo [f arg0 arg1]
`(.invokePrim (as-dlo ~f) ~arg0 ~arg1))
(defn as-ddo ^clojure.lang.IFn$DDO[f] f)
(defmacro ddo [f arg0 arg1]
`(.invokePrim (as-ddo ~f) ~arg0 ~arg1))
(defn as-ool ^clojure.lang.IFn$OOL[f] f)
(defmacro ool [f arg0 arg1]
`(.invokePrim (as-ool ~f) ~arg0 ~arg1))
(defn as-oll ^clojure.lang.IFn$OLL[f] f)
(defmacro oll [f arg0 arg1]
`(.invokePrim (as-oll ~f) ~arg0 ~arg1))
(defn as-odl ^clojure.lang.IFn$ODL[f] f)
(defmacro odl [f arg0 arg1]
`(.invokePrim (as-odl ~f) ~arg0 ~arg1))
(defn as-lol ^clojure.lang.IFn$LOL[f] f)
(defmacro lol [f arg0 arg1]
`(.invokePrim (as-lol ~f) ~arg0 ~arg1))
(defn as-lll ^clojure.lang.IFn$LLL[f] f)
(defmacro lll [f arg0 arg1]
`(.invokePrim (as-lll ~f) ~arg0 ~arg1))
(defn as-ldl ^clojure.lang.IFn$LDL[f] f)
(defmacro ldl [f arg0 arg1]
`(.invokePrim (as-ldl ~f) ~arg0 ~arg1))
(defn as-dol ^clojure.lang.IFn$DOL[f] f)
(defmacro dol [f arg0 arg1]
`(.invokePrim (as-dol ~f) ~arg0 ~arg1))
(defn as-dll ^clojure.lang.IFn$DLL[f] f)
(defmacro dll [f arg0 arg1]
`(.invokePrim (as-dll ~f) ~arg0 ~arg1))
(defn as-ddl ^clojure.lang.IFn$DDL[f] f)
(defmacro ddl [f arg0 arg1]
`(.invokePrim (as-ddl ~f) ~arg0 ~arg1))
(defn as-ood ^clojure.lang.IFn$OOD[f] f)
(defmacro ood [f arg0 arg1]
`(.invokePrim (as-ood ~f) ~arg0 ~arg1))
(defn as-old ^clojure.lang.IFn$OLD[f] f)
(defmacro old [f arg0 arg1]
`(.invokePrim (as-old ~f) ~arg0 ~arg1))
(defn as-odd ^clojure.lang.IFn$ODD[f] f)
(defmacro odd [f arg0 arg1]
`(.invokePrim (as-odd ~f) ~arg0 ~arg1))
(defn as-lod ^clojure.lang.IFn$LOD[f] f)
(defmacro lod [f arg0 arg1]
`(.invokePrim (as-lod ~f) ~arg0 ~arg1))
(defn as-lld ^clojure.lang.IFn$LLD[f] f)
(defmacro lld [f arg0 arg1]
`(.invokePrim (as-lld ~f) ~arg0 ~arg1))
(defn as-ldd ^clojure.lang.IFn$LDD[f] f)
(defmacro ldd [f arg0 arg1]
`(.invokePrim (as-ldd ~f) ~arg0 ~arg1))
(defn as-dod ^clojure.lang.IFn$DOD[f] f)
(defmacro dod [f arg0 arg1]
`(.invokePrim (as-dod ~f) ~arg0 ~arg1))
(defn as-dld ^clojure.lang.IFn$DLD[f] f)
(defmacro dld [f arg0 arg1]
`(.invokePrim (as-dld ~f) ~arg0 ~arg1))
(defn as-ddd ^clojure.lang.IFn$DDD[f] f)
(defmacro ddd [f arg0 arg1]
`(.invokePrim (as-ddd ~f) ~arg0 ~arg1))
(defn as-oolo ^clojure.lang.IFn$OOLO[f] f)
(defmacro oolo [f arg0 arg1 arg2]
`(.invokePrim (as-oolo ~f) ~arg0 ~arg1 ~arg2))
(defn as-oodo ^clojure.lang.IFn$OODO[f] f)
(defmacro oodo [f arg0 arg1 arg2]
`(.invokePrim (as-oodo ~f) ~arg0 ~arg1 ~arg2))
(defn as-oloo ^clojure.lang.IFn$OLOO[f] f)
(defmacro oloo [f arg0 arg1 arg2]
`(.invokePrim (as-oloo ~f) ~arg0 ~arg1 ~arg2))
(defn as-ollo ^clojure.lang.IFn$OLLO[f] f)
(defmacro ollo [f arg0 arg1 arg2]
`(.invokePrim (as-ollo ~f) ~arg0 ~arg1 ~arg2))
(defn as-oldo ^clojure.lang.IFn$OLDO[f] f)
(defmacro oldo [f arg0 arg1 arg2]
`(.invokePrim (as-oldo ~f) ~arg0 ~arg1 ~arg2))
(defn as-odoo ^clojure.lang.IFn$ODOO[f] f)
(defmacro odoo [f arg0 arg1 arg2]
`(.invokePrim (as-odoo ~f) ~arg0 ~arg1 ~arg2))
(defn as-odlo ^clojure.lang.IFn$ODLO[f] f)
(defmacro odlo [f arg0 arg1 arg2]
`(.invokePrim (as-odlo ~f) ~arg0 ~arg1 ~arg2))
(defn as-oddo ^clojure.lang.IFn$ODDO[f] f)
(defmacro oddo [f arg0 arg1 arg2]
`(.invokePrim (as-oddo ~f) ~arg0 ~arg1 ~arg2))
(defn as-looo ^clojure.lang.IFn$LOOO[f] f)
(defmacro looo [f arg0 arg1 arg2]
`(.invokePrim (as-looo ~f) ~arg0 ~arg1 ~arg2))
(defn as-lolo ^clojure.lang.IFn$LOLO[f] f)
(defmacro lolo [f arg0 arg1 arg2]
`(.invokePrim (as-lolo ~f) ~arg0 ~arg1 ~arg2))
(defn as-lodo ^clojure.lang.IFn$LODO[f] f)
(defmacro lodo [f arg0 arg1 arg2]
`(.invokePrim (as-lodo ~f) ~arg0 ~arg1 ~arg2))
(defn as-lloo ^clojure.lang.IFn$LLOO[f] f)
(defmacro lloo [f arg0 arg1 arg2]
`(.invokePrim (as-lloo ~f) ~arg0 ~arg1 ~arg2))
(defn as-lllo ^clojure.lang.IFn$LLLO[f] f)
(defmacro lllo [f arg0 arg1 arg2]
`(.invokePrim (as-lllo ~f) ~arg0 ~arg1 ~arg2))
(defn as-lldo ^clojure.lang.IFn$LLDO[f] f)
(defmacro lldo [f arg0 arg1 arg2]
`(.invokePrim (as-lldo ~f) ~arg0 ~arg1 ~arg2))
(defn as-ldoo ^clojure.lang.IFn$LDOO[f] f)
(defmacro ldoo [f arg0 arg1 arg2]
`(.invokePrim (as-ldoo ~f) ~arg0 ~arg1 ~arg2))
(defn as-ldlo ^clojure.lang.IFn$LDLO[f] f)
(defmacro ldlo [f arg0 arg1 arg2]
`(.invokePrim (as-ldlo ~f) ~arg0 ~arg1 ~arg2))
(defn as-lddo ^clojure.lang.IFn$LDDO[f] f)
(defmacro lddo [f arg0 arg1 arg2]
`(.invokePrim (as-lddo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dooo ^clojure.lang.IFn$DOOO[f] f)
(defmacro dooo [f arg0 arg1 arg2]
`(.invokePrim (as-dooo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dolo ^clojure.lang.IFn$DOLO[f] f)
(defmacro dolo [f arg0 arg1 arg2]
`(.invokePrim (as-dolo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dodo ^clojure.lang.IFn$DODO[f] f)
(defmacro dodo [f arg0 arg1 arg2]
`(.invokePrim (as-dodo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dloo ^clojure.lang.IFn$DLOO[f] f)
(defmacro dloo [f arg0 arg1 arg2]
`(.invokePrim (as-dloo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dllo ^clojure.lang.IFn$DLLO[f] f)
(defmacro dllo [f arg0 arg1 arg2]
`(.invokePrim (as-dllo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dldo ^clojure.lang.IFn$DLDO[f] f)
(defmacro dldo [f arg0 arg1 arg2]
`(.invokePrim (as-dldo ~f) ~arg0 ~arg1 ~arg2))
(defn as-ddoo ^clojure.lang.IFn$DDOO[f] f)
(defmacro ddoo [f arg0 arg1 arg2]
`(.invokePrim (as-ddoo ~f) ~arg0 ~arg1 ~arg2))
(defn as-ddlo ^clojure.lang.IFn$DDLO[f] f)
(defmacro ddlo [f arg0 arg1 arg2]
`(.invokePrim (as-ddlo ~f) ~arg0 ~arg1 ~arg2))
(defn as-dddo ^clojure.lang.IFn$DDDO[f] f)
(defmacro dddo [f arg0 arg1 arg2]
`(.invokePrim (as-dddo ~f) ~arg0 ~arg1 ~arg2))
(defn as-oool ^clojure.lang.IFn$OOOL[f] f)
(defmacro oool [f arg0 arg1 arg2]
`(.invokePrim (as-oool ~f) ~arg0 ~arg1 ~arg2))
(defn as-ooll ^clojure.lang.IFn$OOLL[f] f)
(defmacro ooll [f arg0 arg1 arg2]
`(.invokePrim (as-ooll ~f) ~arg0 ~arg1 ~arg2))
(defn as-oodl ^clojure.lang.IFn$OODL[f] f)
(defmacro oodl [f arg0 arg1 arg2]
`(.invokePrim (as-oodl ~f) ~arg0 ~arg1 ~arg2))
(defn as-olol ^clojure.lang.IFn$OLOL[f] f)
(defmacro olol [f arg0 arg1 arg2]
`(.invokePrim (as-olol ~f) ~arg0 ~arg1 ~arg2))
(defn as-olll ^clojure.lang.IFn$OLLL[f] f)
(defmacro olll [f arg0 arg1 arg2]
`(.invokePrim (as-olll ~f) ~arg0 ~arg1 ~arg2))
(defn as-oldl ^clojure.lang.IFn$OLDL[f] f)
(defmacro oldl [f arg0 arg1 arg2]
`(.invokePrim (as-oldl ~f) ~arg0 ~arg1 ~arg2))
(defn as-odol ^clojure.lang.IFn$ODOL[f] f)
(defmacro odol [f arg0 arg1 arg2]
`(.invokePrim (as-odol ~f) ~arg0 ~arg1 ~arg2))
(defn as-odll ^clojure.lang.IFn$ODLL[f] f)
(defmacro odll [f arg0 arg1 arg2]
`(.invokePrim (as-odll ~f) ~arg0 ~arg1 ~arg2))
(defn as-oddl ^clojure.lang.IFn$ODDL[f] f)
(defmacro oddl [f arg0 arg1 arg2]
`(.invokePrim (as-oddl ~f) ~arg0 ~arg1 ~arg2))
(defn as-lool ^clojure.lang.IFn$LOOL[f] f)
(defmacro lool [f arg0 arg1 arg2]
`(.invokePrim (as-lool ~f) ~arg0 ~arg1 ~arg2))
(defn as-loll ^clojure.lang.IFn$LOLL[f] f)
(defmacro loll [f arg0 arg1 arg2]
`(.invokePrim (as-loll ~f) ~arg0 ~arg1 ~arg2))
(defn as-lodl ^clojure.lang.IFn$LODL[f] f)
(defmacro lodl [f arg0 arg1 arg2]
`(.invokePrim (as-lodl ~f) ~arg0 ~arg1 ~arg2))
(defn as-llol ^clojure.lang.IFn$LLOL[f] f)
(defmacro llol [f arg0 arg1 arg2]
`(.invokePrim (as-llol ~f) ~arg0 ~arg1 ~arg2))
(defn as-llll ^clojure.lang.IFn$LLLL[f] f)
(defmacro llll [f arg0 arg1 arg2]
`(.invokePrim (as-llll ~f) ~arg0 ~arg1 ~arg2))
(defn as-lldl ^clojure.lang.IFn$LLDL[f] f)
(defmacro lldl [f arg0 arg1 arg2]
`(.invokePrim (as-lldl ~f) ~arg0 ~arg1 ~arg2))
(defn as-ldol ^clojure.lang.IFn$LDOL[f] f)
(defmacro ldol [f arg0 arg1 arg2]
`(.invokePrim (as-ldol ~f) ~arg0 ~arg1 ~arg2))
(defn as-ldll ^clojure.lang.IFn$LDLL[f] f)
(defmacro ldll [f arg0 arg1 arg2]
`(.invokePrim (as-ldll ~f) ~arg0 ~arg1 ~arg2))
(defn as-lddl ^clojure.lang.IFn$LDDL[f] f)
(defmacro lddl [f arg0 arg1 arg2]
`(.invokePrim (as-lddl ~f) ~arg0 ~arg1 ~arg2))
(defn as-dool ^clojure.lang.IFn$DOOL[f] f)
(defmacro dool [f arg0 arg1 arg2]
`(.invokePrim (as-dool ~f) ~arg0 ~arg1 ~arg2))
(defn as-doll ^clojure.lang.IFn$DOLL[f] f)
(defmacro doll [f arg0 arg1 arg2]
`(.invokePrim (as-doll ~f) ~arg0 ~arg1 ~arg2))
(defn as-dodl ^clojure.lang.IFn$DODL[f] f)
(defmacro dodl [f arg0 arg1 arg2]
`(.invokePrim (as-dodl ~f) ~arg0 ~arg1 ~arg2))
(defn as-dlol ^clojure.lang.IFn$DLOL[f] f)
(defmacro dlol [f arg0 arg1 arg2]
`(.invokePrim (as-dlol ~f) ~arg0 ~arg1 ~arg2))
(defn as-dlll ^clojure.lang.IFn$DLLL[f] f)
(defmacro dlll [f arg0 arg1 arg2]
`(.invokePrim (as-dlll ~f) ~arg0 ~arg1 ~arg2))
(defn as-dldl ^clojure.lang.IFn$DLDL[f] f)
(defmacro dldl [f arg0 arg1 arg2]
`(.invokePrim (as-dldl ~f) ~arg0 ~arg1 ~arg2))
(defn as-ddol ^clojure.lang.IFn$DDOL[f] f)
(defmacro ddol [f arg0 arg1 arg2]
`(.invokePrim (as-ddol ~f) ~arg0 ~arg1 ~arg2))
(defn as-ddll ^clojure.lang.IFn$DDLL[f] f)
(defmacro ddll [f arg0 arg1 arg2]
`(.invokePrim (as-ddll ~f) ~arg0 ~arg1 ~arg2))
(defn as-dddl ^clojure.lang.IFn$DDDL[f] f)
(defmacro dddl [f arg0 arg1 arg2]
`(.invokePrim (as-dddl ~f) ~arg0 ~arg1 ~arg2))
(defn as-oood ^clojure.lang.IFn$OOOD[f] f)
(defmacro oood [f arg0 arg1 arg2]
`(.invokePrim (as-oood ~f) ~arg0 ~arg1 ~arg2))
(defn as-oold ^clojure.lang.IFn$OOLD[f] f)
(defmacro oold [f arg0 arg1 arg2]
`(.invokePrim (as-oold ~f) ~arg0 ~arg1 ~arg2))
(defn as-oodd ^clojure.lang.IFn$OODD[f] f)
(defmacro oodd [f arg0 arg1 arg2]
`(.invokePrim (as-oodd ~f) ~arg0 ~arg1 ~arg2))
(defn as-olod ^clojure.lang.IFn$OLOD[f] f)
(defmacro olod [f arg0 arg1 arg2]
`(.invokePrim (as-olod ~f) ~arg0 ~arg1 ~arg2))
(defn as-olld ^clojure.lang.IFn$OLLD[f] f)
(defmacro olld [f arg0 arg1 arg2]
`(.invokePrim (as-olld ~f) ~arg0 ~arg1 ~arg2))
(defn as-oldd ^clojure.lang.IFn$OLDD[f] f)
(defmacro oldd [f arg0 arg1 arg2]
`(.invokePrim (as-oldd ~f) ~arg0 ~arg1 ~arg2))
(defn as-odod ^clojure.lang.IFn$ODOD[f] f)
(defmacro odod [f arg0 arg1 arg2]
`(.invokePrim (as-odod ~f) ~arg0 ~arg1 ~arg2))
(defn as-odld ^clojure.lang.IFn$ODLD[f] f)
(defmacro odld [f arg0 arg1 arg2]
`(.invokePrim (as-odld ~f) ~arg0 ~arg1 ~arg2))
(defn as-oddd ^clojure.lang.IFn$ODDD[f] f)
(defmacro oddd [f arg0 arg1 arg2]
`(.invokePrim (as-oddd ~f) ~arg0 ~arg1 ~arg2))
(defn as-lood ^clojure.lang.IFn$LOOD[f] f)
(defmacro lood [f arg0 arg1 arg2]
`(.invokePrim (as-lood ~f) ~arg0 ~arg1 ~arg2))
(defn as-lold ^clojure.lang.IFn$LOLD[f] f)
(defmacro lold [f arg0 arg1 arg2]
`(.invokePrim (as-lold ~f) ~arg0 ~arg1 ~arg2))
(defn as-lodd ^clojure.lang.IFn$LODD[f] f)
(defmacro lodd [f arg0 arg1 arg2]
`(.invokePrim (as-lodd ~f) ~arg0 ~arg1 ~arg2))
(defn as-llod ^clojure.lang.IFn$LLOD[f] f)
(defmacro llod [f arg0 arg1 arg2]
`(.invokePrim (as-llod ~f) ~arg0 ~arg1 ~arg2))
(defn as-llld ^clojure.lang.IFn$LLLD[f] f)
(defmacro llld [f arg0 arg1 arg2]
`(.invokePrim (as-llld ~f) ~arg0 ~arg1 ~arg2))
(defn as-lldd ^clojure.lang.IFn$LLDD[f] f)
(defmacro lldd [f arg0 arg1 arg2]
`(.invokePrim (as-lldd ~f) ~arg0 ~arg1 ~arg2))
(defn as-ldod ^clojure.lang.IFn$LDOD[f] f)
(defmacro ldod [f arg0 arg1 arg2]
`(.invokePrim (as-ldod ~f) ~arg0 ~arg1 ~arg2))
(defn as-ldld ^clojure.lang.IFn$LDLD[f] f)
(defmacro ldld [f arg0 arg1 arg2]
`(.invokePrim (as-ldld ~f) ~arg0 ~arg1 ~arg2))
(defn as-lddd ^clojure.lang.IFn$LDDD[f] f)
(defmacro lddd [f arg0 arg1 arg2]
`(.invokePrim (as-lddd ~f) ~arg0 ~arg1 ~arg2))
(defn as-dood ^clojure.lang.IFn$DOOD[f] f)
(defmacro dood [f arg0 arg1 arg2]
`(.invokePrim (as-dood ~f) ~arg0 ~arg1 ~arg2))
(defn as-dold ^clojure.lang.IFn$DOLD[f] f)
(defmacro dold [f arg0 arg1 arg2]
`(.invokePrim (as-dold ~f) ~arg0 ~arg1 ~arg2))
(defn as-dodd ^clojure.lang.IFn$DODD[f] f)
(defmacro dodd [f arg0 arg1 arg2]
`(.invokePrim (as-dodd ~f) ~arg0 ~arg1 ~arg2))
(defn as-dlod ^clojure.lang.IFn$DLOD[f] f)
(defmacro dlod [f arg0 arg1 arg2]
`(.invokePrim (as-dlod ~f) ~arg0 ~arg1 ~arg2))
(defn as-dlld ^clojure.lang.IFn$DLLD[f] f)
(defmacro dlld [f arg0 arg1 arg2]
`(.invokePrim (as-dlld ~f) ~arg0 ~arg1 ~arg2))
(defn as-dldd ^clojure.lang.IFn$DLDD[f] f)
(defmacro dldd [f arg0 arg1 arg2]
`(.invokePrim (as-dldd ~f) ~arg0 ~arg1 ~arg2))
(defn as-ddod ^clojure.lang.IFn$DDOD[f] f)
(defmacro ddod [f arg0 arg1 arg2]
`(.invokePrim (as-ddod ~f) ~arg0 ~arg1 ~arg2))
(defn as-ddld ^clojure.lang.IFn$DDLD[f] f)
(defmacro ddld [f arg0 arg1 arg2]
`(.invokePrim (as-ddld ~f) ~arg0 ~arg1 ~arg2))
(defn as-dddd ^clojure.lang.IFn$DDDD[f] f)
(defmacro dddd [f arg0 arg1 arg2]
`(.invokePrim (as-dddd ~f) ~arg0 ~arg1 ~arg2))
(defn as-ooolo ^clojure.lang.IFn$OOOLO[f] f)
(defmacro ooolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooodo ^clojure.lang.IFn$OOODO[f] f)
(defmacro ooodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooloo ^clojure.lang.IFn$OOLOO[f] f)
(defmacro ooloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oollo ^clojure.lang.IFn$OOLLO[f] f)
(defmacro oollo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oollo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooldo ^clojure.lang.IFn$OOLDO[f] f)
(defmacro ooldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oodoo ^clojure.lang.IFn$OODOO[f] f)
(defmacro oodoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oodoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oodlo ^clojure.lang.IFn$OODLO[f] f)
(defmacro oodlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oodlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooddo ^clojure.lang.IFn$OODDO[f] f)
(defmacro ooddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olooo ^clojure.lang.IFn$OLOOO[f] f)
(defmacro olooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ololo ^clojure.lang.IFn$OLOLO[f] f)
(defmacro ololo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ololo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olodo ^clojure.lang.IFn$OLODO[f] f)
(defmacro olodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olloo ^clojure.lang.IFn$OLLOO[f] f)
(defmacro olloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olllo ^clojure.lang.IFn$OLLLO[f] f)
(defmacro olllo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olllo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olldo ^clojure.lang.IFn$OLLDO[f] f)
(defmacro olldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oldoo ^clojure.lang.IFn$OLDOO[f] f)
(defmacro oldoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oldoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oldlo ^clojure.lang.IFn$OLDLO[f] f)
(defmacro oldlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oldlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olddo ^clojure.lang.IFn$OLDDO[f] f)
(defmacro olddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odooo ^clojure.lang.IFn$ODOOO[f] f)
(defmacro odooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odolo ^clojure.lang.IFn$ODOLO[f] f)
(defmacro odolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ododo ^clojure.lang.IFn$ODODO[f] f)
(defmacro ododo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ododo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odloo ^clojure.lang.IFn$ODLOO[f] f)
(defmacro odloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odllo ^clojure.lang.IFn$ODLLO[f] f)
(defmacro odllo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odllo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odldo ^clojure.lang.IFn$ODLDO[f] f)
(defmacro odldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oddoo ^clojure.lang.IFn$ODDOO[f] f)
(defmacro oddoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oddoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oddlo ^clojure.lang.IFn$ODDLO[f] f)
(defmacro oddlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oddlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odddo ^clojure.lang.IFn$ODDDO[f] f)
(defmacro odddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loooo ^clojure.lang.IFn$LOOOO[f] f)
(defmacro loooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loolo ^clojure.lang.IFn$LOOLO[f] f)
(defmacro loolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loodo ^clojure.lang.IFn$LOODO[f] f)
(defmacro loodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loloo ^clojure.lang.IFn$LOLOO[f] f)
(defmacro loloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lollo ^clojure.lang.IFn$LOLLO[f] f)
(defmacro lollo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lollo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loldo ^clojure.lang.IFn$LOLDO[f] f)
(defmacro loldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lodoo ^clojure.lang.IFn$LODOO[f] f)
(defmacro lodoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lodoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lodlo ^clojure.lang.IFn$LODLO[f] f)
(defmacro lodlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lodlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loddo ^clojure.lang.IFn$LODDO[f] f)
(defmacro loddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llooo ^clojure.lang.IFn$LLOOO[f] f)
(defmacro llooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llolo ^clojure.lang.IFn$LLOLO[f] f)
(defmacro llolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llodo ^clojure.lang.IFn$LLODO[f] f)
(defmacro llodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llloo ^clojure.lang.IFn$LLLOO[f] f)
(defmacro llloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llllo ^clojure.lang.IFn$LLLLO[f] f)
(defmacro llllo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llllo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llldo ^clojure.lang.IFn$LLLDO[f] f)
(defmacro llldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lldoo ^clojure.lang.IFn$LLDOO[f] f)
(defmacro lldoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lldoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lldlo ^clojure.lang.IFn$LLDLO[f] f)
(defmacro lldlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lldlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llddo ^clojure.lang.IFn$LLDDO[f] f)
(defmacro llddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldooo ^clojure.lang.IFn$LDOOO[f] f)
(defmacro ldooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldolo ^clojure.lang.IFn$LDOLO[f] f)
(defmacro ldolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldodo ^clojure.lang.IFn$LDODO[f] f)
(defmacro ldodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldloo ^clojure.lang.IFn$LDLOO[f] f)
(defmacro ldloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldllo ^clojure.lang.IFn$LDLLO[f] f)
(defmacro ldllo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldllo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldldo ^clojure.lang.IFn$LDLDO[f] f)
(defmacro ldldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lddoo ^clojure.lang.IFn$LDDOO[f] f)
(defmacro lddoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lddoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lddlo ^clojure.lang.IFn$LDDLO[f] f)
(defmacro lddlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lddlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldddo ^clojure.lang.IFn$LDDDO[f] f)
(defmacro ldddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doooo ^clojure.lang.IFn$DOOOO[f] f)
(defmacro doooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doolo ^clojure.lang.IFn$DOOLO[f] f)
(defmacro doolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doodo ^clojure.lang.IFn$DOODO[f] f)
(defmacro doodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doloo ^clojure.lang.IFn$DOLOO[f] f)
(defmacro doloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dollo ^clojure.lang.IFn$DOLLO[f] f)
(defmacro dollo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dollo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doldo ^clojure.lang.IFn$DOLDO[f] f)
(defmacro doldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dodoo ^clojure.lang.IFn$DODOO[f] f)
(defmacro dodoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dodoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dodlo ^clojure.lang.IFn$DODLO[f] f)
(defmacro dodlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dodlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doddo ^clojure.lang.IFn$DODDO[f] f)
(defmacro doddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlooo ^clojure.lang.IFn$DLOOO[f] f)
(defmacro dlooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlolo ^clojure.lang.IFn$DLOLO[f] f)
(defmacro dlolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlodo ^clojure.lang.IFn$DLODO[f] f)
(defmacro dlodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlloo ^clojure.lang.IFn$DLLOO[f] f)
(defmacro dlloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlllo ^clojure.lang.IFn$DLLLO[f] f)
(defmacro dlllo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlllo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlldo ^clojure.lang.IFn$DLLDO[f] f)
(defmacro dlldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dldoo ^clojure.lang.IFn$DLDOO[f] f)
(defmacro dldoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dldoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dldlo ^clojure.lang.IFn$DLDLO[f] f)
(defmacro dldlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dldlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlddo ^clojure.lang.IFn$DLDDO[f] f)
(defmacro dlddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddooo ^clojure.lang.IFn$DDOOO[f] f)
(defmacro ddooo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddooo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddolo ^clojure.lang.IFn$DDOLO[f] f)
(defmacro ddolo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddolo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddodo ^clojure.lang.IFn$DDODO[f] f)
(defmacro ddodo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddodo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddloo ^clojure.lang.IFn$DDLOO[f] f)
(defmacro ddloo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddloo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddllo ^clojure.lang.IFn$DDLLO[f] f)
(defmacro ddllo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddllo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddldo ^clojure.lang.IFn$DDLDO[f] f)
(defmacro ddldo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddldo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dddoo ^clojure.lang.IFn$DDDOO[f] f)
(defmacro dddoo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dddoo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dddlo ^clojure.lang.IFn$DDDLO[f] f)
(defmacro dddlo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dddlo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddddo ^clojure.lang.IFn$DDDDO[f] f)
(defmacro ddddo [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddddo ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooool ^clojure.lang.IFn$OOOOL[f] f)
(defmacro ooool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oooll ^clojure.lang.IFn$OOOLL[f] f)
(defmacro oooll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oooll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooodl ^clojure.lang.IFn$OOODL[f] f)
(defmacro ooodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oolol ^clojure.lang.IFn$OOLOL[f] f)
(defmacro oolol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oolol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oolll ^clojure.lang.IFn$OOLLL[f] f)
(defmacro oolll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oolll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooldl ^clojure.lang.IFn$OOLDL[f] f)
(defmacro ooldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oodol ^clojure.lang.IFn$OODOL[f] f)
(defmacro oodol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oodol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oodll ^clojure.lang.IFn$OODLL[f] f)
(defmacro oodll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oodll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooddl ^clojure.lang.IFn$OODDL[f] f)
(defmacro ooddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olool ^clojure.lang.IFn$OLOOL[f] f)
(defmacro olool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ololl ^clojure.lang.IFn$OLOLL[f] f)
(defmacro ololl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ololl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olodl ^clojure.lang.IFn$OLODL[f] f)
(defmacro olodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ollol ^clojure.lang.IFn$OLLOL[f] f)
(defmacro ollol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ollol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ollll ^clojure.lang.IFn$OLLLL[f] f)
(defmacro ollll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ollll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olldl ^clojure.lang.IFn$OLLDL[f] f)
(defmacro olldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oldol ^clojure.lang.IFn$OLDOL[f] f)
(defmacro oldol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oldol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oldll ^clojure.lang.IFn$OLDLL[f] f)
(defmacro oldll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oldll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olddl ^clojure.lang.IFn$OLDDL[f] f)
(defmacro olddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odool ^clojure.lang.IFn$ODOOL[f] f)
(defmacro odool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odoll ^clojure.lang.IFn$ODOLL[f] f)
(defmacro odoll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odoll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ododl ^clojure.lang.IFn$ODODL[f] f)
(defmacro ododl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ododl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odlol ^clojure.lang.IFn$ODLOL[f] f)
(defmacro odlol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odlol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odlll ^clojure.lang.IFn$ODLLL[f] f)
(defmacro odlll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odlll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odldl ^clojure.lang.IFn$ODLDL[f] f)
(defmacro odldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oddol ^clojure.lang.IFn$ODDOL[f] f)
(defmacro oddol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oddol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oddll ^clojure.lang.IFn$ODDLL[f] f)
(defmacro oddll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oddll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odddl ^clojure.lang.IFn$ODDDL[f] f)
(defmacro odddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loool ^clojure.lang.IFn$LOOOL[f] f)
(defmacro loool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-looll ^clojure.lang.IFn$LOOLL[f] f)
(defmacro looll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-looll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loodl ^clojure.lang.IFn$LOODL[f] f)
(defmacro loodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lolol ^clojure.lang.IFn$LOLOL[f] f)
(defmacro lolol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lolol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lolll ^clojure.lang.IFn$LOLLL[f] f)
(defmacro lolll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lolll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loldl ^clojure.lang.IFn$LOLDL[f] f)
(defmacro loldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lodol ^clojure.lang.IFn$LODOL[f] f)
(defmacro lodol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lodol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lodll ^clojure.lang.IFn$LODLL[f] f)
(defmacro lodll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lodll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loddl ^clojure.lang.IFn$LODDL[f] f)
(defmacro loddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llool ^clojure.lang.IFn$LLOOL[f] f)
(defmacro llool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lloll ^clojure.lang.IFn$LLOLL[f] f)
(defmacro lloll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lloll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llodl ^clojure.lang.IFn$LLODL[f] f)
(defmacro llodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lllol ^clojure.lang.IFn$LLLOL[f] f)
(defmacro lllol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lllol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lllll ^clojure.lang.IFn$LLLLL[f] f)
(defmacro lllll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lllll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llldl ^clojure.lang.IFn$LLLDL[f] f)
(defmacro llldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lldol ^clojure.lang.IFn$LLDOL[f] f)
(defmacro lldol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lldol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lldll ^clojure.lang.IFn$LLDLL[f] f)
(defmacro lldll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lldll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llddl ^clojure.lang.IFn$LLDDL[f] f)
(defmacro llddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldool ^clojure.lang.IFn$LDOOL[f] f)
(defmacro ldool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldoll ^clojure.lang.IFn$LDOLL[f] f)
(defmacro ldoll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldoll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldodl ^clojure.lang.IFn$LDODL[f] f)
(defmacro ldodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldlol ^clojure.lang.IFn$LDLOL[f] f)
(defmacro ldlol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldlol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldlll ^clojure.lang.IFn$LDLLL[f] f)
(defmacro ldlll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldlll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldldl ^clojure.lang.IFn$LDLDL[f] f)
(defmacro ldldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lddol ^clojure.lang.IFn$LDDOL[f] f)
(defmacro lddol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lddol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lddll ^clojure.lang.IFn$LDDLL[f] f)
(defmacro lddll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lddll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldddl ^clojure.lang.IFn$LDDDL[f] f)
(defmacro ldddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doool ^clojure.lang.IFn$DOOOL[f] f)
(defmacro doool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dooll ^clojure.lang.IFn$DOOLL[f] f)
(defmacro dooll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dooll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doodl ^clojure.lang.IFn$DOODL[f] f)
(defmacro doodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dolol ^clojure.lang.IFn$DOLOL[f] f)
(defmacro dolol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dolol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dolll ^clojure.lang.IFn$DOLLL[f] f)
(defmacro dolll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dolll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doldl ^clojure.lang.IFn$DOLDL[f] f)
(defmacro doldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dodol ^clojure.lang.IFn$DODOL[f] f)
(defmacro dodol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dodol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dodll ^clojure.lang.IFn$DODLL[f] f)
(defmacro dodll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dodll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doddl ^clojure.lang.IFn$DODDL[f] f)
(defmacro doddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlool ^clojure.lang.IFn$DLOOL[f] f)
(defmacro dlool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dloll ^clojure.lang.IFn$DLOLL[f] f)
(defmacro dloll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dloll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlodl ^clojure.lang.IFn$DLODL[f] f)
(defmacro dlodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dllol ^clojure.lang.IFn$DLLOL[f] f)
(defmacro dllol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dllol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dllll ^clojure.lang.IFn$DLLLL[f] f)
(defmacro dllll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dllll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlldl ^clojure.lang.IFn$DLLDL[f] f)
(defmacro dlldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dldol ^clojure.lang.IFn$DLDOL[f] f)
(defmacro dldol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dldol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dldll ^clojure.lang.IFn$DLDLL[f] f)
(defmacro dldll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dldll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlddl ^clojure.lang.IFn$DLDDL[f] f)
(defmacro dlddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddool ^clojure.lang.IFn$DDOOL[f] f)
(defmacro ddool [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddool ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddoll ^clojure.lang.IFn$DDOLL[f] f)
(defmacro ddoll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddoll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddodl ^clojure.lang.IFn$DDODL[f] f)
(defmacro ddodl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddodl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddlol ^clojure.lang.IFn$DDLOL[f] f)
(defmacro ddlol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddlol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddlll ^clojure.lang.IFn$DDLLL[f] f)
(defmacro ddlll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddlll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddldl ^clojure.lang.IFn$DDLDL[f] f)
(defmacro ddldl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddldl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dddol ^clojure.lang.IFn$DDDOL[f] f)
(defmacro dddol [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dddol ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dddll ^clojure.lang.IFn$DDDLL[f] f)
(defmacro dddll [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dddll ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddddl ^clojure.lang.IFn$DDDDL[f] f)
(defmacro ddddl [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddddl ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooood ^clojure.lang.IFn$OOOOD[f] f)
(defmacro ooood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooold ^clojure.lang.IFn$OOOLD[f] f)
(defmacro ooold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooodd ^clojure.lang.IFn$OOODD[f] f)
(defmacro ooodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oolod ^clojure.lang.IFn$OOLOD[f] f)
(defmacro oolod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oolod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oolld ^clojure.lang.IFn$OOLLD[f] f)
(defmacro oolld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oolld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooldd ^clojure.lang.IFn$OOLDD[f] f)
(defmacro ooldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oodod ^clojure.lang.IFn$OODOD[f] f)
(defmacro oodod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oodod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oodld ^clojure.lang.IFn$OODLD[f] f)
(defmacro oodld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oodld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ooddd ^clojure.lang.IFn$OODDD[f] f)
(defmacro ooddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ooddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olood ^clojure.lang.IFn$OLOOD[f] f)
(defmacro olood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olold ^clojure.lang.IFn$OLOLD[f] f)
(defmacro olold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olodd ^clojure.lang.IFn$OLODD[f] f)
(defmacro olodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ollod ^clojure.lang.IFn$OLLOD[f] f)
(defmacro ollod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ollod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ollld ^clojure.lang.IFn$OLLLD[f] f)
(defmacro ollld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ollld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olldd ^clojure.lang.IFn$OLLDD[f] f)
(defmacro olldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oldod ^clojure.lang.IFn$OLDOD[f] f)
(defmacro oldod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oldod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oldld ^clojure.lang.IFn$OLDLD[f] f)
(defmacro oldld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oldld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-olddd ^clojure.lang.IFn$OLDDD[f] f)
(defmacro olddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-olddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odood ^clojure.lang.IFn$ODOOD[f] f)
(defmacro odood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odold ^clojure.lang.IFn$ODOLD[f] f)
(defmacro odold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ododd ^clojure.lang.IFn$ODODD[f] f)
(defmacro ododd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ododd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odlod ^clojure.lang.IFn$ODLOD[f] f)
(defmacro odlod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odlod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odlld ^clojure.lang.IFn$ODLLD[f] f)
(defmacro odlld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odlld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odldd ^clojure.lang.IFn$ODLDD[f] f)
(defmacro odldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oddod ^clojure.lang.IFn$ODDOD[f] f)
(defmacro oddod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oddod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-oddld ^clojure.lang.IFn$ODDLD[f] f)
(defmacro oddld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-oddld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-odddd ^clojure.lang.IFn$ODDDD[f] f)
(defmacro odddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-odddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loood ^clojure.lang.IFn$LOOOD[f] f)
(defmacro loood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loold ^clojure.lang.IFn$LOOLD[f] f)
(defmacro loold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loodd ^clojure.lang.IFn$LOODD[f] f)
(defmacro loodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lolod ^clojure.lang.IFn$LOLOD[f] f)
(defmacro lolod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lolod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lolld ^clojure.lang.IFn$LOLLD[f] f)
(defmacro lolld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lolld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loldd ^clojure.lang.IFn$LOLDD[f] f)
(defmacro loldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lodod ^clojure.lang.IFn$LODOD[f] f)
(defmacro lodod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lodod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lodld ^clojure.lang.IFn$LODLD[f] f)
(defmacro lodld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lodld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-loddd ^clojure.lang.IFn$LODDD[f] f)
(defmacro loddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-loddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llood ^clojure.lang.IFn$LLOOD[f] f)
(defmacro llood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llold ^clojure.lang.IFn$LLOLD[f] f)
(defmacro llold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llodd ^clojure.lang.IFn$LLODD[f] f)
(defmacro llodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lllod ^clojure.lang.IFn$LLLOD[f] f)
(defmacro lllod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lllod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lllld ^clojure.lang.IFn$LLLLD[f] f)
(defmacro lllld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lllld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llldd ^clojure.lang.IFn$LLLDD[f] f)
(defmacro llldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lldod ^clojure.lang.IFn$LLDOD[f] f)
(defmacro lldod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lldod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lldld ^clojure.lang.IFn$LLDLD[f] f)
(defmacro lldld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lldld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-llddd ^clojure.lang.IFn$LLDDD[f] f)
(defmacro llddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-llddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldood ^clojure.lang.IFn$LDOOD[f] f)
(defmacro ldood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldold ^clojure.lang.IFn$LDOLD[f] f)
(defmacro ldold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldodd ^clojure.lang.IFn$LDODD[f] f)
(defmacro ldodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldlod ^clojure.lang.IFn$LDLOD[f] f)
(defmacro ldlod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldlod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldlld ^clojure.lang.IFn$LDLLD[f] f)
(defmacro ldlld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldlld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldldd ^clojure.lang.IFn$LDLDD[f] f)
(defmacro ldldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lddod ^clojure.lang.IFn$LDDOD[f] f)
(defmacro lddod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lddod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-lddld ^clojure.lang.IFn$LDDLD[f] f)
(defmacro lddld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-lddld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ldddd ^clojure.lang.IFn$LDDDD[f] f)
(defmacro ldddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ldddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doood ^clojure.lang.IFn$DOOOD[f] f)
(defmacro doood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doold ^clojure.lang.IFn$DOOLD[f] f)
(defmacro doold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doodd ^clojure.lang.IFn$DOODD[f] f)
(defmacro doodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dolod ^clojure.lang.IFn$DOLOD[f] f)
(defmacro dolod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dolod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dolld ^clojure.lang.IFn$DOLLD[f] f)
(defmacro dolld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dolld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doldd ^clojure.lang.IFn$DOLDD[f] f)
(defmacro doldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dodod ^clojure.lang.IFn$DODOD[f] f)
(defmacro dodod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dodod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dodld ^clojure.lang.IFn$DODLD[f] f)
(defmacro dodld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dodld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-doddd ^clojure.lang.IFn$DODDD[f] f)
(defmacro doddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-doddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlood ^clojure.lang.IFn$DLOOD[f] f)
(defmacro dlood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlold ^clojure.lang.IFn$DLOLD[f] f)
(defmacro dlold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlodd ^clojure.lang.IFn$DLODD[f] f)
(defmacro dlodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dllod ^clojure.lang.IFn$DLLOD[f] f)
(defmacro dllod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dllod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dllld ^clojure.lang.IFn$DLLLD[f] f)
(defmacro dllld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dllld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlldd ^clojure.lang.IFn$DLLDD[f] f)
(defmacro dlldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dldod ^clojure.lang.IFn$DLDOD[f] f)
(defmacro dldod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dldod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dldld ^clojure.lang.IFn$DLDLD[f] f)
(defmacro dldld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dldld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dlddd ^clojure.lang.IFn$DLDDD[f] f)
(defmacro dlddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dlddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddood ^clojure.lang.IFn$DDOOD[f] f)
(defmacro ddood [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddood ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddold ^clojure.lang.IFn$DDOLD[f] f)
(defmacro ddold [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddold ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddodd ^clojure.lang.IFn$DDODD[f] f)
(defmacro ddodd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddodd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddlod ^clojure.lang.IFn$DDLOD[f] f)
(defmacro ddlod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddlod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddlld ^clojure.lang.IFn$DDLLD[f] f)
(defmacro ddlld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddlld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddldd ^clojure.lang.IFn$DDLDD[f] f)
(defmacro ddldd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddldd ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dddod ^clojure.lang.IFn$DDDOD[f] f)
(defmacro dddod [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dddod ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-dddld ^clojure.lang.IFn$DDDLD[f] f)
(defmacro dddld [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-dddld ~f) ~arg0 ~arg1 ~arg2 ~arg3))
(defn as-ddddd ^clojure.lang.IFn$DDDDD[f] f)
(defmacro ddddd [f arg0 arg1 arg2 arg3]
`(.invokePrim (as-ddddd ~f) ~arg0 ~arg1 ~arg2 ~arg3))

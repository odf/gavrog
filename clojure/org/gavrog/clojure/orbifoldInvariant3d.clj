(ns org.gavrog.clojure.orbifoldInvariant3d
  (:use (org.gavrog.clojure
          delaney
          delaney2d)))

(defn- orbifold-type [ds idcs D]
  (case (count idcs)
    0 "1"
    1 (if (= D (s ds (first idcs) D)) "1*" "1")
    2 (let [[i j] idcs, n (v ds i j D)]
        (if (orbit-loopless? ds idcs D)
          (if (= n 1) "1" (str n n))
          (if (= n 1) "1*" (str "*" n n)))) 
    3 (orbifold-symbol (orbit ds idcs D))))

(defn- sublists
  ([i j k] [[i j] [i k] [j k]])
  ([i j]   [[i] [j]])
  ([i]     []))

(defn- raw-orbifold-graph [ds]
  (let [[i j k m] (indices ds)
        index-combinations [[i] [j] [k] [m]
                            [i j] [i k] [i m] [j k] [j m] [k m]
                            [i j k] [i j m] [i k m] [j k m]]
        to-rep (into {} (for [idcs index-combinations
                              D (orbit-reps ds idcs)
                              E (orbit-elements ds idcs D)]
                          [[idcs E] [idcs D]]))
        sub-orbits (fn [idcs D]
                     (let [o (orbit ds idcs D)]
                       (for [is (apply sublists idcs)
                             D (orbit-reps o is)]
                         (to-rep [is D]))))]
    (into {} (for [idcs index-combinations
                   D (orbit-reps ds idcs)]
               [[idcs D] {:type (orbifold-type ds idcs D)
                          :elms (orbit-elements ds idcs D) ;; for debugging
                          :subs (sub-orbits idcs D)}]))))

(defn- filter-graph [p g]
  (let [good (set (filter (comp p g) (keys g)))]
    (into {} (for [[k v] g :when (good k)]
               [k (assoc v :subs (filter good (:subs v)))]))))

(defn- quotient-graph [keyfn g]
  )



(ns org.gavrog.clojure.combineTiles
  (:use (org.gavrog.clojure
          [partition]
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- components-with-multiplicities [ds]
  (let [idcs (indices ds)]
    (frequencies (for [D (orbit-reps ds idcs)] (canonical (orbit ds idcs D))))))

(defn- partition-by-automorphism-group [ds]
  (or (seq (into pempty (apply concat (automorphisms ds))))
      (map hash-set (elements ds))))

(defn- inequivalent-forms [ds]
  (for [orb (partition-by-automorphism-group ds)]
    (canonical ds (first orb))))

(defn- signatures [ds, idcs]
  (into {} (for [D (orbit-reps ds idcs)
                 :let [sub (orbit ds idcs D)]
                 block (partition-by-automorphism-group sub)
                 :let [inv (invariant sub (first block))],
                 E block]
             [E inv])))

(defn combine-tiles [ds]
  (let [d (dim ds)
        face-idcs (range d)
        counts (components-with-multiplicities ds)
        parts (keys counts)
        forms (into {} (for [sub parts] [sub (inequivalent-forms sub)]))
        sigs (into {} (for [sub parts] [sub (signatures sub face-idcs)]))]
    (make-backtracker 
      {:root (let [sub (first parts)]
               [sub
                (sigs sub)
                (orbit-reps sub face-idcs)
                (assoc counts sub (dec (counts sub)))])
       :extract (fn [[symbol _ free-elements free-components]]
                  (when (and (empty? free-elements)
                             (every? (comp zero? second) free-components)
                             (canonical? symbol))
                    symbol))
       :children (fn [[]])})))

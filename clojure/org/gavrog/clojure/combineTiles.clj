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

(defn- signatures [ds]
  (into {} (for [orb (partition-by-automorphism-group ds)
                 :let [inv (invariant ds (first orb))],
                 D orb]
             [D inv])))

(defn combine-tiles [ds]
  (make-backtracker 
    {:root []
     :extract (fn [[]])
     :children (fn [[]])}))

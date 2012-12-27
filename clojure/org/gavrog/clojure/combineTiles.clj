(ns org.gavrog.clojure.combineTiles
  (:use (org.gavrog.clojure
          [partition]
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- components-with-multiplicities [ds]
  (let [idcs (indices ds)]
    (frequencies (for [D (orbit-reps ds idcs)] (canonical (orbit ds idcs D))))))

(defn- partition-by-automorphism-group [ds]
  (into pempty (apply concat (automorphisms ds))))

(defn- inequivalent-forms [ds]
  (for [orb (partition-by-automorphism-group ds)]
    (renumbered-from ds (first orb))))

(defn combine-tiles [ds]
  (make-backtracker 
    {:root []
     :extract (fn [[]])
     :children (fn [[]])}))

(ns org.gavrog.clojure.combineTiles
  (:use (org.gavrog.clojure
          [partition]
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- components-with-multiplicities [ds]
  (let [idcs (indices ds)]
    (frequencies (for [D (orbit-reps ds idcs)] (canonical (orbit ds idcs D))))))

(defn- orbit-partition [maps]
  (reduce (fn [p [D E]] (punion p D E)) pempty (apply concat maps)))

(defn- reps-by-automorphism-group [ds]
  (map first (orbit-partition (automorphisms ds))))

(defn- inequivalent-forms [ds]
  (map (partial renumbered-from ds) (reps-by-automorphism-group ds)))

(defn combine-tiles [ds]
  (make-backtracker 
    {:root []
     :extract (fn [[]])
     :children (fn [[]])}))

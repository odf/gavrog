(ns org.gavrog.clojure.combineTiles
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- components-with-multiplicities [ds]
  (let [idcs (indices ds)]
    (frequencies (for [D (orbit-reps ds idcs)] (canonical (orbit ds idcs D))))))

(defn combine-tiles [ds]
  (make-backtracker 
    {:root []
     :extract (fn [[]])
     :children (fn [[]])}))

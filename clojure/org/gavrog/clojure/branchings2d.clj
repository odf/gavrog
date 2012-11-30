(ns org.gavrog.clojure.branchings
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn canonical? [ds]
  (= (zipmap (elements ds) (elements ds))
     (into {} (.getMapToCanonical (java-dsymbol ds)))))

(defn branchings [d-set min-face-degree min-vert-degree min-curvature]
  (make-backtracker 
    {:root (let [orbs (for [i [0 1], D (orbit-reps d-set [i (inc i)])] [i D])]
             [d-set (into #{} orbs) {}])
     :extract (fn [[ds unused spins]]
                (when (and (empty? unused) (canonical? ds)) ds))
     :children (fn [[]])}))

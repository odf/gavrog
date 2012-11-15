(ns org.gavrog.clojure.branchings
  (:use (org.gavrog.clojure [generators :only [make-backtracker results]])))

(defn branchings [d-set min-face-degree min-vert-degree min-curvature]
  (make-backtracker 
    {:root []
     :extract (fn [[]])
     :children (fn [[]])}))

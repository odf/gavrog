(ns org.gavrog.clojure.permutations
  (:use (org.gavrog.clojure [generators :only [make-backtracker results]])))

(defn permutations [n]
  (make-backtracker 
    {:root (let [elms (into #{} (range 1 (inc n)))]
             [{} elms elms])
     :extract (fn [[perm unused unseen]]
                (when (empty? unused) perm))
     :children (fn [[perm unused unseen]]
                 (when-let [i (first unused)]
                   (for [k unseen]
                     [(assoc perm i k)
                      (disj unused i)
                      (disj unseen k)])))}))

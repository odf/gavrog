(ns org.gavrog.clojure.permutations
  (:use (org.gavrog.clojure [generators :only [make-backtracker results]])))

(defn permutations [n]
  (let [keys (range 1 (inc n))
        root [{} (into #{} keys) (into #{} keys)]
        children (fn [[perm unused unseen]]
                   (when-let [i (first unused)]
                     (for [k unseen]
                       [(assoc perm i k) (disj unused i) (disj unseen k)])))
        extract (fn [node] (when (empty? (second node)) (first node)))]
        (make-backtracker {:children children :extract extract :root root})))

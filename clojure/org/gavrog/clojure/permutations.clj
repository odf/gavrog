(ns org.gavrog.clojure.permutations
  (:use (org.gavrog.clojure [generators :only [make-backtracker results]])))

(defn permutations [n]
  (let [keys (range 1 (inc n))
        root {:perm {}
              :unused (into #{} keys)
              :unseen (into #{} keys)}
        children (fn [{:keys [perm unused unseen]}]
                   (when-let [i (first unused)]
                     (for [k unseen]
                       {:perm (assoc perm i k)
                        :unused (disj unused i)
                        :unseen (disj unseen k)})))
        extract (fn [node] (when (empty? (:unused node)) (:perm node)))]
        (make-backtracker {:children children :extract extract :root root})))

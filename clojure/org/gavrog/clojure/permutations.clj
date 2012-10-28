(ns org.gavrog.clojure.permutations)

(deftype BacktrackingGenerator [root children extract desc]
  clojure.lang.Seqable
  (seq [_] (filter (comp not nil?)
                   (map extract (tree-seq (constantly true) children root))))
  Object
  (toString [_] (str desc)))

(defn permutations [n]
  (let [keys (range 1 (inc n))
        root {:perm {}
              :unused (into #{} keys)
              :unseen (into #{} keys)}
        children (fn [node] (let [{:keys [perm unused unseen]} node]
                              (when-let [i (first unused)]
                                (for [k unseen]
                                  {:perm (assoc perm i k)
                                   :unused (disj unused i)
                                   :unseen (disj unseen k)}))))
        extract (fn [node] (when (empty? (:unused node)) (:perm node)))
        desc (list `permutations n)]
        (BacktrackingGenerator. root children extract desc)))

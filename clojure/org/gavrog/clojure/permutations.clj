(ns org.gavrog.clojure.permutations)

(deftype BacktrackingGenerator [root complete? branch? children extract]
  clojure.lang.Seqable
  (seq [gen] (map extract (filter complete? (tree-seq branch? children root)))))

(defn permutations [n]
  (let [keys (range 1 (inc n))
        root {:perm {}
              :unused (into #{} keys)
              :unseen (into #{} keys)}
        complete? (comp empty? :unused)
        branch? (comp seq :unused)
        children (fn [node] (let [{:keys [perm unused unseen]} node]
                              (when-let [i (first unused)]
                                (for [k unseen]
                                  {:perm (assoc perm i k)
                                   :unused (disj unused i)
                                   :unseen (disj unseen k)}))))
        extract :perm]
        (BacktrackingGenerator. root complete? branch? children extract)))

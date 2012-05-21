(ns org.gavrog.clojure.permutations)

(defn permutations [degree]
  (let [keys (range 1 (inc degree))
        complete? (comp empty? :unused)
        children (fn [node]
                   (let [{:keys [perm unused unseen]} node]
                     (when-let [i (first unused)]
                       (for [n unseen]
                         {:perm (assoc perm i n)
                          :unused (disj unused i)
                          :unseen (disj unseen n)}))))
        root {:perm {}
              :unused (into #{} keys)
              :unseen (into #{} keys)}]
    (map :perm
         (filter complete?
                 (tree-seq (comp not complete?) children root)))))

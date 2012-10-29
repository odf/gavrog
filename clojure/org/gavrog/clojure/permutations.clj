(ns org.gavrog.clojure.permutations)

(defprotocol SubstepGenerator
  (result [_])
  (step [_]))

(defn results [gen]
  (letfn [(s [gen] (when gen (lazy-seq (cons gen (s (step gen))))))]
    (filter (comp not nil?) (map result (s gen)))))

(defprotocol Resumable
  (checkpoint [_])
  (resume [_ checkpoint]))

(deftype BacktrackingGenerator [gen-children extract desc stack]
  SubstepGenerator
  (result [_] (extract (:node (first stack))))
  (step [_]
        (let [stack
              (if-let [children (seq (gen-children (:node (first stack))))]
                (conj stack
                      {:node (first children)
                       :branch-nr 0
                       :siblings-left (rest children)})
                (when-let [stack (seq (drop-while
                                        #(not (seq (:siblings-left %))) stack))]
                  (let [{:keys [siblings-left branch-nr]} (first stack)]
                    (conj (rest stack)
                          {:node (first siblings-left)
                           :branch-nr (inc branch-nr)
                           :siblings-left (rest siblings-left)}))))]
          (when (seq stack)
            (BacktrackingGenerator. gen-children extract desc stack))))
  Object
  (toString [_] (str desc)))

(defn make-backtracker [gen-children extract desc root]
  (BacktrackingGenerator. gen-children extract desc (list {:node root})))

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
        (make-backtracker children extract desc root)))

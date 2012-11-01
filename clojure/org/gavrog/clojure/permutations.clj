(ns org.gavrog.clojure.permutations)

(defprotocol SubstepGenerator
  (result [_])
  (step [_]))

(defn results [gen]
  (letfn [(s [gen] (when gen (lazy-seq (cons gen (s (step gen))))))]
    (filter (comp not nil?) (map result (s gen)))))

(defprotocol SplittableGenerator
  (sub-generator [_])
  (skip [_]))

(defprotocol Resumable
  (checkpoint [_])
  (resume [_ checkpoint]))

(deftype BacktrackingGenerator [spec stack]
  SubstepGenerator
  (result [_] ((:extract spec) (:node (first stack))))
  (step [gen]
        (if-let [children (seq ((:children spec) (:node (first stack))))]
          (let [stack (conj stack
                            {:node (first children)
                             :branch-nr 0
                             :siblings-left (rest children)})]
            (BacktrackingGenerator. spec stack))
          (skip gen)))
  Resumable
  (checkpoint [_] (rest (reverse (map :branch-nr stack))))
  (resume [_ checkpoint]
          (loop [todo checkpoint stack stack]
            (if (seq todo)
              (let [n (first todo)
                    children (drop n ((:children spec) (:node (first stack))))
                    stack (conj stack
                                {:node (first children)
                                 :branch-nr n
                                 :siblings-left (rest children)})]
                (recur (rest todo) stack))
              (BacktrackingGenerator. spec stack))))
  SplittableGenerator
  (sub-generator [gen]
                 (let [stack (list {:node (:node (first stack))})]
                   (BacktrackingGenerator. spec stack)))
  (skip [_]
        (when-let [stack
                   (seq (drop-while #(not (seq (:siblings-left %))) stack))]
          (let [{:keys [siblings-left branch-nr]} (first stack)
                stack (conj (rest stack)
                            {:node (first siblings-left)
                             :branch-nr (inc branch-nr)
                             :siblings-left (rest siblings-left)})]
            (BacktrackingGenerator. spec stack)))))

(defn make-backtracker [spec]
  (BacktrackingGenerator. spec (list {:node (:root spec)})))

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

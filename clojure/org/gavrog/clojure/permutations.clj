(ns org.gavrog.clojure.permutations)

(defn positions [coll x]
  (keep-indexed #(when (= %2 x) %1) coll))

(defn permutations [degree]
  (let [complete? #(empty? (positions %1 false))
        branch? (comp not complete?)
        children (fn [perm]
                   (when-let [i (first (positions perm false))]
                     (for [n (range degree) :when (empty? (positions perm n))]
                       (assoc perm i n))))
        root (into [] (repeat degree false))]
    (filter complete? (tree-seq branch? children root))))

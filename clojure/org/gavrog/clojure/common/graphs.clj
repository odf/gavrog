(ns org.gavrog.clojure.common.graphs
  (:require (org.gavrog.clojure.common [util :as util])))

(defn traversal
  "Generic traversal function"
  ([adj seen todo push head tail]
    (when-let [node (head todo)]
      (let [neighbors (adj node)
            todo (reduce push (tail todo) (filter (complement seen) neighbors))
            seen (into (conj seen node) neighbors)]
        (lazy-seq (cons node (traversal adj seen todo push head tail)))))))

(defn dfs
  "Performs a lazy depth first traversal of the directed graph determined by
  the list 'sources' of source nodes and the adjacency function 'adj'."
  ([adj & sources]
    (traversal adj #{} (into '() sources) conj first rest)))

(defn bfs
  "Performs a lazy breadth first traversal of the directed graph determined by
  the list 'sources' of source nodes and the adjacency function 'adj'."
  ([adj & sources]
    (traversal adj #{} (into util/empty-queue sources) conj first pop)))

(defn bfs-shells [adj source]
  (let [next
        (fn [[prev this]]
          [this (set (for [u this, v (adj u)
                           :when (not (or (this v) (prev v)))] v))])]
    (take-while seq
                (conj
                  (map second (iterate next [#{source} (set (adj source))]))
                  #{source}))))

(defn bfs-radius [adj source]
  (dec (count (bfs-shells adj source))))

(defn diameter [adj sources]
  (apply max (map (partial bfs-radius adj) sources)))

(defn morphism [v w edge-target incidence-pairs]
  (loop [src2img {}
         q (conj util/empty-queue [:nodes v w])]
    (let [[kind a b] (first q)]
      (cond
        (empty? q)
        src2img

        (nil? b)
        nil

        (= b (src2img a))
        (recur src2img (pop q))

        (not (nil? (src2img a)))
        nil

        (= kind :edges)
        (recur (assoc src2img a b)
               (conj (pop q) [:nodes (edge-target a) (edge-target b)]))

        :else
        (when-let [matches (incidence-pairs a b)]
          (recur (assoc src2img a b)
                 (into (pop q) (map (fn [[a b]] [:edges a b]) matches))))))))

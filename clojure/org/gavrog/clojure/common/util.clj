(ns org.gavrog.clojure.common.util)

(defn abs [n] (if (neg? n) (- n) n))

(defn sign [n]
  (cond (zero? n) 0
        (neg? n) -1
        :else 1))

(def empty-queue clojure.lang.PersistentQueue/EMPTY)

(defn pop-while [pred coll]
  "Like drop-while, but with pop."
  (cond
    (empty? coll) coll
    (pred (first coll)) (recur pred (pop coll))
    :else coll))

(defn iterate-cycle [coll x]
  "Returns a lazy sequence of intermediate results, starting at x, of
   cycling through the functions in coll and applying each to the
   previous result."
  (reductions #(%2 %1) x (cycle coll)))

(defn compare-lexicographically
  ([cmp xs ys]
    "Compares two sequences lexicographically via the given function cmp."
    (cond (empty? xs) (if (empty? ys) 0 -1)
          (empty? ys) 1
          :else (let [d (cmp (first xs) (first ys))]
                  (if (not= 0 d)
                    d
                    (recur cmp (rest xs) (rest ys))))))
  ([xs ys]
    "Compares two sequences lexicographically via compare. Elements of
     the first sequence must implements Comparable."   
    (compare-lexicographically compare xs ys)))

(defn lexicographically-smallest
  "Returns the lexicographically smallest sequence, via compare."
  ([cmp xs] xs)
  ([cmp xs ys] (if (neg? (compare-lexicographically cmp xs ys)) xs ys))
  ([cmp xs ys & more] (reduce (partial lexicographically-smallest cmp)
                          (lexicographically-smallest cmp xs ys) more)))

(defn multi-assoc [m k v]
  (assoc m k (conj (get m k #{}) v)))

(defn multi-map [pairs]
  (reduce (fn [m [k v]] (multi-assoc m k v)) {} pairs))

(defn traversal [adj seen todo push head tail]
  "Generic traversal function"
  (when-let [node (head todo)]
    (let [neighbors (adj node)
          todo (reduce push (tail todo) (filter (complement seen) neighbors))
          seen (into (conj seen node) neighbors)]
      (lazy-seq (cons node (traversal adj seen todo push head tail))))))

(defn dfs [adj & sources]
  "Performs a lazy depth first traversal of the directed graph determined by
  the list 'sources' of source nodes and the adjacency function 'adj'."
  (traversal adj #{} (into '() sources) conj first rest))

(defn bfs [adj & sources]
  "Performs a lazy breadth first traversal of the directed graph determined by
  the list 'sources' of source nodes and the adjacency function 'adj'."
  (traversal adj #{} (into empty-queue sources) conj first pop))

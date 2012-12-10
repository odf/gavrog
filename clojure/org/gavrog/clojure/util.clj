(ns org.gavrog.clojure.util)

;; General purpose definitions and entities

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

(defn compare-lexicographically [xs ys]
  "Compares two sequences lexicographically via compare. Elements of
   the first sequence must implements Comparable."
  (cond (empty? xs) (if (empty? ys) 0 -1)
        (empty? ys) 1
        :else (let [d (compare (first xs) (first ys))]
                (if (not= 0 d)
                  d
                  (recur (rest xs) (rest ys))))))

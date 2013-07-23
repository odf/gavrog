(ns org.gavrog.clojure.common.simple-generators
  (:use (org.gavrog.clojure.common
          [generators :only [make-backtracker results]])))

(defn permutations [n]
  (make-backtracker 
    {:root (let [elms (into #{} (range 1 (inc n)))]
             [{} elms elms])
     :extract (fn [[perm unused unseen]]
                (when (empty? unused) perm))
     :children (fn [[perm unused unseen]]
                 (when-let [i (first unused)]
                   (for [k unseen]
                     [(assoc perm i k)
                      (disj unused i)
                      (disj unseen k)])))}))

(defn integer-partitions [n]
  (make-backtracker
    {:root [[] 0 1]
     :extract (fn [[xs sz mx]] (when (= sz n) xs))
     :children (fn [[xs sz mx]]
                 (for [i (range mx (inc (- n sz)))]
                   [(conj xs i) (+ sz i) (max mx i)]))}))

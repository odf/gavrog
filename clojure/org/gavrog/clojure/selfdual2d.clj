(ns org.gavrog.clojure.selfdual2d
  (:use (org.gavrog.clojure
          [delaney]
          [delaney2d]
          [generators :only [results]]
          [dsets :only [dsets]]
          [branchings2d :only [branchings]]))
  (:import (org.gavrog.joss.tilings Tiling))
  (:gen-class))

(defn- good-node [net pos v]
  (not (empty? (.goodCombinations net (.allIncidences net v) pos))))

(defn convex? [ds]
  (if-let [net (try (.getSkeleton (Tiling. (java-dsymbol ds)))
                 (catch IllegalArgumentException e))]
    (let [pos (.barycentricPlacement net)]
      (every? (partial good-node net pos)
              (.nodes net)))))

(defn andp [& preds]
  (fn [& args]
    (reduce #(and %1 (apply %2 args)) true preds)))

(defn print-seq [xs format-item format-summary]
  (let [start-time (. System (nanoTime))
        n (count (for [x xs] (println (format-item x))))
        t (/ (double (- (. System (nanoTime)) start-time)) 1000000000.0)]
    (println (format-summary n t))))


;; All convex, self-dual, 2d euclidean tilings.
(defn d-syms [max-size]
  (let [good? (andp self-dual? minimal? euclidean? convex?)]
    (for [dset (results (dsets 2 max-size))
          :when (and (self-dual? dset) (proto-euclidean? dset))
          dsym (results (branchings dset))
          :when (good? dsym)]
    (canonical dsym))))

;; Main entry point when used as a script
(defn -main [& args]
  (print-seq (d-syms (Integer/parseInt (first args)))
             print-str
             #(str "# Generated " %1 " symbols in " %2 " seconds.")))

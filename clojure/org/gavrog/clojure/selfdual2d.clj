(ns org.gavrog.clojure.selfdual2d
  (:use (org.gavrog.clojure
          [util :only [unique]]
          [delaney]))
  (:import (org.gavrog.jane.fpgroups SmallActionsIterator)
           (org.gavrog.jane.numbers Whole)
           (org.gavrog.joss.dsyms.basic DSymbol)
           (org.gavrog.joss.dsyms.derived FundamentalGroup DSCover Covers)
           (org.gavrog.joss.tilings Tiling)
           (org.gavrog.joss.dsyms.generators DefineBranching2d))
  (:gen-class))

(defn face-sizes [ds]
  (map #(.m ds 0 1 %) (orbit-reps ds [0 1])))

(defn minimal? [ds] (.isMinimal ds))

(defn self-dual? [ds] (= ds (.dual ds)))

(defn euclidean? [ds] (= (curvature ds 1) 0))

(defn proto-euclidean? [ds] (>= (curvature ds 1) 0))

(defn good-face-sizes? [ds]
  (let [t (face-sizes ds)]
    (and (<= (count (unique t)) 2) (some #(= 3 %) t))))

(defn- good-node [net pos v]
  (not (empty? (.goodCombinations net (.allIncidences net v) pos))))

(defn convex? [ds]
  (if-let [net (try (.getSkeleton (Tiling. ds))
                 (catch IllegalArgumentException e))]
    (let [pos (.barycentricPlacement net)]
      (every? (partial good-node net pos)
              (.nodes net)))))

(defn andp [& preds]
  (fn [& args]
    (reduce #(and %1 (apply %2 args)) true preds)))

(defn d-sets [max-size]
  (let [G (FundamentalGroup. (DSymbol. "1:1,1,1:0,0"))]
    (filter (andp self-dual? proto-euclidean?)
            (map #(.flat (DSCover. G %))
                 (SmallActionsIterator. (.getPresentation G) max-size false)))))

;; All self-dual, 2d euclidean tilings with only two face sizes and at least
;; one triangle.
(defn d-syms [max-size]
  (let [good? (andp self-dual? minimal? euclidean? good-face-sizes? convex?)]
    (for [dset (d-sets max-size)
          dsym (lazy-seq (DefineBranching2d. dset 3 3 Whole/ZERO))
          :when (good? dsym)]
    dsym)))

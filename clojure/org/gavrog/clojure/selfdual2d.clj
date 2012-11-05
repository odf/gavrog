(ns org.gavrog.clojure.selfdual2d
  (:use (org.gavrog.clojure [util :only [unique]]
                            [delaney :only [s v size dim curvature]]))
  (:import (org.gavrog.jane.fpgroups SmallActionsIterator)
           (org.gavrog.jane.numbers Whole)
           (org.gavrog.joss.dsyms.basic DSymbol)
           (org.gavrog.joss.dsyms.derived FundamentalGroup DSCover)
           (org.gavrog.joss.dsyms.generators DefineBranching2d))
  (:gen-class))

(defn self-dual? [ds] (= ds (.dual ds)))

(defn proto-euclidean? [ds] (>= (curvature ds 1) 0))

(defn d-sets [max-size]
  (let [G (FundamentalGroup. (DSymbol. "1:1,1,1:0,0"))]
    (filter #(and (self-dual? %) (proto-euclidean? %))
            (map #(.flat (DSCover. G %))
                 (SmallActionsIterator. (.getPresentation G) max-size false)))))

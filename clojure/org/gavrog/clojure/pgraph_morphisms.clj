(ns org.gavrog.clojure.pgraph-morphisms
  (:import (org.gavrog.joss.pgraphs.basic PeriodicGraph$CoverNode)
           (org.gavrog.joss.pgraphs.io Net)
           (org.gavrog.joss.geometry Operator Vector)))

(defn nets [filename]
  (iterator-seq (Net/iterator filename)))

(defn identity-matrix [net]
  (Operator/identity (.getDimension net)))

(defn adjacent [node]
  (map #(.target %) (iterator-seq (.incidences node))))

(defn nextShellPair [[prev this]]
  [this (set (for [u this, v (adjacent u) :when (not (prev v))] v))])

(defn shells [net node]
  (let [v0 (PeriodicGraph$CoverNode. net node)]
    (iterate nextShellPair [#{v0} (set (adjacent v0))])))

(defn node-signatures [net]
  (let [pos (.barycentricPlacement net)
        pos* (zipmap (keys pos) (map #(.modZ %) (vals pos)))
        by-pos (group-by pos* (.nodes net))]
    by-pos))

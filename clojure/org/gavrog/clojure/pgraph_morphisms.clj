(ns org.gavrog.clojure.pgraph-morphisms
  (:import (org.gavrog.joss.pgraphs.basic PeriodicGraph$CoverNode)
           (org.gavrog.joss.pgraphs.io Net)
           (org.gavrog.joss.geometry Operator Vector)))

(defn nets [filename]
  (iterator-seq (Net/iterator filename)))

(defn cover-node [net node]
  (PeriodicGraph$CoverNode. net node))

(defn cover-node-position [pos node]
  (.plus (pos (.getOrbitNode node)) (.getShift node)))

(defn identity-matrix [net]
  (Operator/identity (.getDimension net)))

(defn adjacent [node]
  (map #(.target %) (iterator-seq (.incidences node))))

(defn next-shell-pair [[prev this]]
  [this (set (for [u this, v (adjacent u) :when (not (prev v))] v))])

(defn shells [net node]
  (conj
    (map second (iterate next-shell-pair [#{node} (set (adjacent node))]))
    #{node}))

(defn shell-positions [net node]
  (let [pos (into {} (.barycentricPlacement net))]
    (map (partial map (partial cover-node-position pos))
         (shells net (cover-node net node)))))

(defn node-signatures [net]
  (let [pos (.barycentricPlacement net)
        pos* (zipmap (keys pos) (map #(.modZ %) (vals pos)))
        shifts (zipmap (keys pos) (map #(.minus (pos %) (pos* %)) (keys pos)))
        by-pos (group-by pos* (.nodes net))]
    by-pos))

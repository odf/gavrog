(ns org.gavrog.clojure.pgraph-morphisms
  (:import (org.gavrog.joss.pgraphs.io Net)
           (org.gavrog.joss.geometry Operator)))

(defn nets [filename]
  (iterator-seq (Net/iterator filename)))

(defn identity-matrix [net]
  (Operator/identity (.getDimension net)))

(defn node-signatures [net]
  (let [mod-1 (fn [[v p]] [v (.modZ p)])
        pos (into {} (map mod-1 (.barycentricPlacement net)))
        by-pos (group-by pos (.nodes net))]
    by-pos))

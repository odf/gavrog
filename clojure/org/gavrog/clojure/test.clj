(ns org.gavrog.clojure.test)

(def nets
  (iterator-seq (org.gavrog.joss.pgraphs.io.Net/iterator "Fivecases.cgd")))

(def G (first nets))

(def nodes (iterator-seq (.nodes G)))
(def I (org.gavrog.joss.geometry.Operator/identity (.getDimension G)))

(def phi
  (org.gavrog.joss.pgraphs.basic.Morphism. (nth nodes 0) (nth nodes 1) I))

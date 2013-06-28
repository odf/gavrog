(ns org.gavrog.clojure.test)

(def G (first (iterator-seq (org.gavrog.joss.pgraphs.io.Net/iterator "x.cgd"))))
(def C (.getGraph (first (.connectedComponents G))))
(def M (.minimalImage C))
(def nodes (iterator-seq (.nodes M)))
(def I (org.gavrog.joss.geometry.Operator/identity 3))

(def phi
  (org.gavrog.joss.pgraphs.basic.Morphism. (nth nodes 0) (nth nodes 1) I))

(ns org.gavrog.clojure.pgraph-morphisms
  (:use (clojure test)
        (org.gavrog.clojure.common [util :only [empty-queue classify]]))
  (:import (org.gavrog.joss.pgraphs.basic
             INode
             IEdge
             PeriodicGraph$CoverNode Morphism Morphism$NoSuchMorphismException)
           (org.gavrog.joss.pgraphs.io Net)
           (org.gavrog.joss.geometry Operator Vector SpaceGroup)
           (org.gavrog.jane.compounds Matrix)
           (org.gavrog.jane.numbers Whole))
  (:gen-class))

;; --- TODO put generic functions into packages under common

(defn bfs-radius [adj source]
  (loop [seen #{source}, maxdist 0, q (conj empty-queue [source 0])]
    (if (empty? q)
      maxdist
      (let [[v d] (first q)
            ws (remove seen (adj v))]
        (recur (into seen ws)
               (max maxdist d)
               (into (pop q) (map vector ws (repeat (inc d)))))))))

(defn diameter [adj sources]
  (apply max (map (partial bfs-radius adj) sources)))

(defn bfs-shells [adj source]
  (let [next
        (fn [[prev this]]
          [this (set (for [u this, v (adj u) :when (not (prev v))] v))])]
    (conj
      (map second (iterate next [#{source} (set (adj source))]))
      #{source})))

(defn morphism [v w edge-target incidence-pairs]
  (loop [src2img {}
         q (conj empty-queue [v w])]
    (let [[a b] (first q)]
      (cond
        (empty? q)
        src2img
              
        (nil? b)
        nil
            
        (= b (src2img a))
        (recur src2img (pop q))
            
        (not (nil? (src2img a)))
        nil

        (instance? IEdge a)
        (recur (assoc src2img a b)
               (conj (pop q) [(edge-target a) (edge-target b)]))
          
        :else
        (when-let [matches (incidence-pairs a b)]
          (recur (assoc src2img a b) (into (pop q) matches)))))))

;; ---

(defn extend-matrix [M]
  (let [n (.numberOfRows M)
        m (.numberOfColumns M)
        M* (Matrix/zero (inc n) (inc m))]
    (.setSubMatrix M* 0 0 M)
    (.set M* n m (Whole/ONE))
    M*))

(defn nets [filename]
  (iterator-seq (Net/iterator filename)))

(defn identity-matrix [net]
  (Operator/identity (.getDimension net)))

(defn nodes [net]
  (iterator-seq (.nodes net)))

(defn barycentric-positions [net]
  (into {} (.barycentricPlacement net)))

(defn adjacent [node]
  (map #(.target %) (iterator-seq (.incidences node))))

(defn cover-node [net node]
  (PeriodicGraph$CoverNode. net node))

(defn cover-node-position [pos node]
  (.plus (pos (.getOrbitNode node)) (.getShift node)))

(defn shell-positions [net pos node]
  (let [shift (.minus (pos node) (.modZ (pos node)))
        pos* (fn [v] (.minus (cover-node-position pos v) shift))]
    (map (comp sort (partial map pos*))
         (bfs-shells adjacent (cover-node net node)))))

(defn node-classification [net]
  (let [pos (barycentric-positions net)
        vs (nodes net)
        dia (diameter adjacent vs)
        shells (for [v vs]
                 (map vec (take (inc dia) (shell-positions net pos v))))]
    (classify (zipmap vs shells))))

(defn node-signatures [net]
  (let [nclass (node-classification net)]
    (when (every? (fn [xs] (== 1 (count xs))) (vals nclass))
      (into {} (for [[key vals] nclass] [(first vals) key])))))

(defn map-sig [M sig]
  (let [p (.times (first (first sig)) M)
        op (.times M (Operator. (.minus (.modZ p) p)))]
    (into (vector)
          (map (fn [s] (->> s (map #(.times % op)) sort (into (vector))))
               sig))))

(defn incidences-by-signature [net v sigs]
  (into {} (for [e0 (map #(.oriented %) (.incidences v))
                 e [e0 (.reverse e0)]
                 :when (= v (.source e))]
             [[(.differenceVector net e) (sigs (.target e))] e])))

(defn matched-incidences [net v w op sigs]
  (let [nv (incidences-by-signature net v sigs)
        nw (incidences-by-signature net w sigs)]
    (when (= (count nv) (count nw))
      (for [[d sig] (keys nv)
            :let [e1 (.get nv [d sig])
                  e2 (.get nw [(.times d op) (map-sig op sig)])]]
        [e1 e2]))))

(defn affineOperator [v w M sigs]
  (let [M* (Operator. (extend-matrix M))
        pv (first (first (sigs v)))
        pw (first (first (sigs w)))
        d (.minus pw (.times pv M*))]
    (.times M* (Operator. d))))

(defn symmetry-from-base-pair [net b1 b2 sigs]
  (let [v (.source (.get b1 0))
        w (.source (.get b2 0))
        M (Matrix/solve (.differenceMatrix net b1) (.differenceMatrix net b2))
        op (affineOperator v w M sigs)
        incidence-pairs (fn [a b] (matched-incidences net a b op sigs))]
    (when (.isUnimodularIntegerMatrix M)
      (when-let [phi (morphism v w #(.target %) incidence-pairs)]
        [op phi]))))

(defn symmetries [net]
  (let [sigs (node-signatures net)]
    (assert sigs)
    (let [bases (iterator-seq (.iterator (.characteristicBases net)))]
      (->> bases
        (map #(symmetry-from-base-pair net (first bases) % sigs))
        (filter identity)))))

(defn spacegroup [net]
  (SpaceGroup. (.getDimension net) (map first (symmetries net))))

(defn systreable? [net]
  (and (.isConnected net)
       (.isLocallyStable net)
       (not (.hasSecondOrderCollisions net))))

(deftest spacegroup-test
  (defn test-net [net]
    (let [n1 (when (systreable? net) (.getName (.getSpaceGroup net)))
          n2 (when (node-signatures net) (.getName (spacegroup net)))]
      (or (nil? n1) (= n1 n2))))

  (doseq [f ["dia.pgr", "test.pgr", "Fivecases.cgd", "xbad.pgr"]
          g (nets f)]
        (when (.isConnected g) (is (test-net g)))))

(defn -main [path]
  (doseq [G (nets path)]
    (print (.getName G) "")
    (try
      (if (not (systreable? G))
        (if (and (.isConnected G) (node-signatures G))
          (println "->" (.getName (spacegroup G)))
          (println "n.a."))
        (let [n1 (.getName (.getSpaceGroup G))
              n2 (.getName (spacegroup G))]
          (if (= n1 n2)
            (println "good")
            (println (str "bad (" n1 " vs " n2 ")")))))
      (catch Throwable x
        (do (println "error") (throw x))))))

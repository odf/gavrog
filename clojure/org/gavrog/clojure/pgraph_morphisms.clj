(ns org.gavrog.clojure.pgraph-morphisms
  (:use (clojure test)
        (org.gavrog.clojure.common [util :only [empty-queue]]))
  (:import (org.gavrog.joss.pgraphs.basic
             INode
             IEdge
             PeriodicGraph$CoverNode Morphism Morphism$NoSuchMorphismException)
           (org.gavrog.joss.pgraphs.io Net)
           (org.gavrog.joss.geometry Operator Vector SpaceGroup)
           (org.gavrog.jane.compounds Matrix)
           (org.gavrog.jane.numbers Whole))
  (:gen-class))

(defn nets [filename]
  (iterator-seq (Net/iterator filename)))

(defn identity-matrix [net]
  (Operator/identity (.getDimension net)))

(defn barycentric-positions [net]
  (into {} (.barycentricPlacement net)))

(defn distances [net]
  "Pair-wise distances between vertices using the Floyd-Warshall algorithm"
  (let [nodes (iterator-seq (.nodes net))
        edges (iterator-seq (.edges net))
        n (count nodes)
        dist (into {} (for [v nodes, w nodes] [[v w] (if (= v w) 0 n)]))
        dist (into dist (for [e edges, e* [e (.reverse e)]]
                          [[(.source e*) (.target e*)] 1]))]
    (reduce (fn [d [u v w]]
              (assoc d [v w] (min (d [v w]) (+ (d [v u]) (d [u w])))))
            dist
            (for [u nodes, v nodes, w nodes] [u v w]))))

(defn diameter [net]
  (apply max (vals (distances net))))

(defn cover-node [net node]
  (PeriodicGraph$CoverNode. net node))

(defn cover-node-position [pos node]
  (.plus (pos (.getOrbitNode node)) (.getShift node)))

(defn adjacent [node]
  (map #(.target %) (iterator-seq (.incidences node))))

(defn next-shell-pair [[prev this]]
  [this (set (for [u this, v (adjacent u) :when (not (prev v))] v))])

(defn shells [net node]
  (conj
    (map second (iterate next-shell-pair [#{node} (set (adjacent node))]))
    #{node}))

(defn shell-positions [net pos node]
  (let [shift (.minus (pos node) (.modZ (pos node)))
        pos* (fn [v] (.minus (cover-node-position pos v) shift))]
    (map (comp sort (partial map pos*)) (shells net (cover-node net node)))))

(defn classify-once [items2seqs]
  (for [[k c] (group-by first (for [[item s] items2seqs]
                                [(first s) [item (rest s)]]))]
    [k (map second c)]))

(defn classify-recursively [items2seqs]
  (cond
    (empty? items2seqs)
    {}
    
    (= 1 (count items2seqs))
    { (vec (take 1 (second (first items2seqs)))) (take 1 (first items2seqs)) }
    
    :else
    (loop [classes (sorted-map [(- (count items2seqs)) []] items2seqs)]
      (let [[[n k] c] (first classes)]
        (if (or (nil? n) (>= n -1))
          (zipmap (map (comp second first) classes)
                  (map #(map first (last %)) classes))
          (recur (into (dissoc classes [n k])
                       (for [[key cl] (classify-once c)]
                         (if (nil? key)
                           [[0 k] cl]
                           [[(- (count cl)) (conj k key)] cl])))))))))

(defn node-classification [net]
  (let [pos (barycentric-positions net)
        nodes (iterator-seq (.nodes net))
        dia (diameter net)
        shells (for [v nodes]
                 (map vec (take (inc dia) (shell-positions net pos v))))]
    (classify-recursively (zipmap nodes shells))))

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

(defn extend-matrix [M]
  (let [n (.numberOfRows M)
        m (.numberOfColumns M)
        M* (Matrix/zero (inc n) (inc m))]
    (.setSubMatrix M* 0 0 M)
    (.set M* n m (Whole/ONE))
    M*))

(defn morphism
  ([net v w M sigs]
    (when (.isUnimodularIntegerMatrix M)
      (let [M* (Operator. (extend-matrix M))
            pv (first (first (sigs v)))
            pw (first (first (sigs w)))
            d (.minus pw (.times pv M*))
            op (.times M* (Operator. d))]
        (loop [src2img {}
               q (conj empty-queue [v w])]
          (let [[a b] (first q)]
            (cond
              (empty? q)
              [op src2img]
              
              (nil? b)
              nil
            
              (= b (src2img a))
              (recur src2img (pop q))
            
              (not (nil? (src2img a)))
              nil

              (instance? IEdge a)
              (recur (assoc src2img a b)
                     (conj (pop q) [(.target a) (.target b)]))
            
              :else
              (when-let [matches (matched-incidences net a b op sigs)]
                (recur (assoc src2img a b) (into (pop q) matches)))))))))
  ([net v w M]
    (morphism net v w M (node-signatures net))))

(defn symmetry-from-base-pair [net b1 b2 sigs]
  (let [start #(.source (.get % 0))
        mat #(.differenceMatrix net %)]
    (morphism net (start b1) (start b2) (Matrix/solve (mat b1) (mat b2)) sigs)))

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
    (if (not (systreable? G))
      (println "n.a.")
      (try
        (let [n1 (.getName (.getSpaceGroup G))
              n2 (.getName (spacegroup G))]
          (if (= n1 n2)
            (println "good")
            (println (str "bad (" n1 " vs " n2 ")"))))
        (catch Throwable x
          (do (println "error") (throw x)))))))

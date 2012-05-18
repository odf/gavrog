(ns org.gavrog.clojure.azulenoids
  (:import (org.gavrog.jane.numbers Whole)
           (org.gavrog.joss.dsyms.basic DSymbol DynamicDSymbol)
           (org.gavrog.joss.dsyms.generators CombineTiles DefineBranching2d)))

(defn iterate-cycle [coll x]
  (reductions (fn [x f] (f x)) x (cycle coll)))

(defn lazy-mapcat [f & colls]
  (lazy-seq
   (when (every? seq colls)
     (concat (apply f (map first colls))
             (apply lazy-mapcat f (map rest colls))))))

(defn walk [ds D & idxs]
  (reduce (fn [D i] (.op ds i D)) D idxs))

(defn chain-end [ds D i j]
  (letfn [(step [E]
                (let [E* (walk ds E j)]
                  (cond
                    (nil? E*) E
                    (= E E*) E
                    (= D E*) nil
                    :else (recur (walk ds E j i)))))]
    (step (walk ds D i))))

(defn boundary-chambers [ds D i j k]
  (letfn [(a [D] (walk ds D i))
          (b [D] (chain-end ds D j k))]
    (iterate-cycle [a b] D)))

(defn max-curvature [ds]
  (let [dsx (.clone ds)]
    (do (.setVDefaultToOne dsx true) (.curvature2D dsx))))

(defn syms-for [ds]
  (if (.isNegative (max-curvature ds))
    []
    (filter (fn [ds] (= Whole/ZERO (.curvature2D ds)))
            (lazy-seq (new DefineBranching2d ds 3 2 Whole/ZERO)))))

(def template
  (new DSymbol (str "1.1:60:"
                    "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 "
                    "32 34 36 38 40 42 44 46 48 50 52 54 56 58 60,"
                    "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 "
                    "33 35 42 39 41 48 45 47 54 51 53 60 57 59,"
                    "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 "
                    "29 0 0 0 0 42 41 0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
                    "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0")))

(def octagon
  (new DSymbol "1.1:16 1:2 4 6 8 10 12 14 16,16 3 5 7 9 11 13 15:8"))

(def boundary-mappings
  (let [start (fn [p] (mod (- 20 p) 16))
        box (fn [i] (Integer. i))
        template-boundary (boundary-chambers template (box 1) 0 1 2)
        octagon-boundary (cycle (map box (range 1 17)))
        mapping (fn [p] (zipmap (drop (start p) octagon-boundary)
                                (take 16 template-boundary)))]
    (map mapping (range 1 16 2))))

(defn apply-to-template [ds oct2tmp]
  (let [tmp2oct (clojure.set/map-invert oct2tmp)
        tmp (DynamicDSymbol. template)
        finish (fn [D]
                 (if (not (.definesOp tmp 2 D))
                   (.redefineOp tmp 2 D (oct2tmp (.op ds 2 (tmp2oct D)))))
                 (if (not (.definesV tmp 1 2 D))
                   (.redefineV tmp 1 2 D (.v ds 1 2 (tmp2oct D)))))]
    (doall (map finish (keys tmp2oct)))
    (-> tmp .dual .minimal .canonical)))

(defn octa-sets [] (lazy-seq (new CombineTiles octagon)))

(defn octa-syms [] (lazy-mapcat syms-for (octa-sets)))

(defn azul-syms-raw []
  (lazy-mapcat (fn [ds] (map (partial apply-to-template ds) boundary-mappings))
               (octa-syms)))

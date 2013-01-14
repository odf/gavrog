(ns org.gavrog.clojure.branchings2d
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney]
          [delaney2d])))

(defn- open-orbits [ds]
  (for [i [0 1] :let [j (inc i)]
        D (orbit-reps ds [i j])
        :when (nil? (v ds i j D))]
    [i D (r ds i j D) (orbit-loopless? ds [i j] D)]))

(defn- sign [s]
  (cond (empty? s) 0
        (= 0 (first s)) (recur (rest s))
        :else (compare (first s) 0)))

(defn- good? [ds maps]
  (let [diffs (fn [m] (for [D (elements ds), i [0 1], :let [j (inc i)]]
                        (- (v ds i j D) (v ds i j (m D)))))]
    (every? #(-> % diffs sign (>= 0)) maps)))

(defn branchings
  [ds & {:keys [face-sizes-at-least
                   vertex-degrees-at-least
                   curvature-at-least
                   try-spins]
            :or {face-sizes-at-least 3
                 vertex-degrees-at-least 3
                 curvature-at-least 0
                 try-spins [1 2 3 4 6]}}]
  (let [maps (automorphisms ds)
        new-curvature (fn [c s v] (+ c (* (if s 2 1) (- (/ v) 1))))
        still-good? (fn [c i D r s v]
                     (and (<= curvature-at-least (new-curvature c s v))
                          (cond (= 0 i) (<= face-sizes-at-least (* r v))
                                (= 1 i) (<= vertex-degrees-at-least (* r v)))))]
    (make-backtracker 
      {:root [ds (curvature ds 1) (into #{} (open-orbits ds))]
       :extract (fn [[ds c unused]]
                  (when (and (empty? unused)
                             (good? ds maps))
                    ds))
       :children (fn [[ds c unused]]
                   (when-let [[i D r s] (first unused)]
                     (for [v try-spins :when (still-good? c i D r s v)]
                       [(spin ds i (inc i) D v)
                        (new-curvature c s v)
                        (disj unused [i D r s])])))})))

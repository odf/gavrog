(ns org.gavrog.clojure.branchings2d
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn branchings
  [d-set & {:keys [face-sizes-at-least
                   vertex-degrees-at-least
                   curvature-at-least
                   try-spins]
            :or {face-sizes-at-least 3
                 vertex-degrees-at-least 3
                 curvature-at-least 0
                 try-spins [1 2 3 4 6]}}]
  (let [ds (canonical d-set)
        new-curvature (fn [c s v] (+ c (* (if s 2 1) (- (/ v) 1))))
        still-good (fn [c i D r s v]
                     (and (<= curvature-at-least (new-curvature c s v))
                          (cond (= 0 i) (<= face-sizes-at-least (* r v))
                                (= 1 i) (<= vertex-degrees-at-least (* r v)))))]
    (make-backtracker 
      {:root (let [orbs (for [i [0 1] :let [j (inc i)]
                              D (orbit-reps ds [i j])]
                          [i D (r ds i j D) (orbit-loopless? ds [i j] D)])]
               [ds (curvature ds 1) (into #{} orbs)])
       :extract (fn [[ds c unused]]
                  (when (and (empty? unused) (canonical? ds))
                    ds))
       :children (fn [[ds c unused]]
                   (when-let [[i D r s] (first unused)]
                     (for [v try-spins
                           :when (still-good c i D r s v)]
                       [(spin ds i (inc i) D v)
                        (new-curvature c s v)
                        (disj unused [i D r s])])))})))

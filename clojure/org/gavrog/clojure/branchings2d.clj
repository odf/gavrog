(ns org.gavrog.clojure.branchings2d
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn branchings [d-set min-face-degree min-vert-degree min-curvature]
  (let [ds (canonical d-set)
        good-degree (fn [i D v]
                      (cond (= 0 i) (<= min-face-degree (* v (r ds 0 1 D)))
                            (= 1 i) (<= min-vert-degree (* v (r ds 1 2 D)))))]
    (make-backtracker 
      {:root (let [orbs (for [i [0 1], D (orbit-reps ds [i (inc i)])] [i D])]
               [ds (into #{} orbs)])
       :extract (fn [[ds unused]]
                  (when (and (empty? unused)
                             (canonical? ds)
                             (<= min-curvature (curvature ds)))
                    ds))
       :children (fn [[ds unused]]
                   (when-let [[i D] (first unused)]
                     (for [v [1 2 3 4 6]
                           :when (good-degree i D v)]
                       [(spin ds i (inc i) D v) (disj unused [i D])])))})))

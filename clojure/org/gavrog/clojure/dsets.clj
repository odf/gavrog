(ns org.gavrog.clojure.dsets
  (:use (org.gavrog.clojure
          [util]
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- orbits-okay? [ds i D]
  (not-any? #(> (size %) (if (loopless? %) 4 2))
            (for [j (indices ds) :when (or (< j (dec i)) (> j (inc i)))]
              (orbit ds [i j] D))))

(defn- trace [ds D]
  (let [plus-all (fn [pairs D] (into pairs (for [i (indices ds)] [i D])))
        step (fn step [o2n n2o n free]
               (when-let [[i D] (first free)]
                 (when-let [oDi (s ds i (n2o D))]
                   (let [Di (or (o2n oDi) (inc n))
                         free (if (<= Di n) free (plus-all free Di))]
                     (lazy-seq (cons Di (step (assoc o2n oDi Di)
                                              (assoc n2o Di oDi)
                                              (max n Di)
                                              (disj free [i D] [i Di]))))))))]
    (step {D 1} {1 D} 1 (plus-all (sorted-set) 1))))

(defn- cmp [xs ys]
  (if (or (empty? xs) (empty? ys))
    0
    (let [d (compare (first xs) (first ys))]
      (if (not= 0 d)
        d
        (recur (rest xs) (rest ys))))))

(defn- best? [ds alt-starts]
  (let [base (trace ds 1)
        results (into {} (for [D alt-starts] [D (cmp (trace ds D) base)]))]
    [(not-any? #(neg? (results %)) alt-starts)
     (apply disj alt-starts (filter #(pos? (results %)) alt-starts))]))

(defn dsets [dim max-size]
  (let [idcs (range (inc dim))
        still-good? orbits-okay?]
    (make-backtracker
      {:root [(dsymbol (str "1 " dim))
              (into (sorted-set) (for [i idcs] [i 1]))
              #{}]
       :extract (fn [[ds free alt-starts]]
                  (when (and (empty? free) (first (best? ds alt-starts)))
                    (canonical ds)))
       :children (fn [[ds free alt-starts]]
                   (when (seq free)
                     (let [[i D] (first free)
                           adding (for [[j E] free :when (= i j)] [E free])
                           extending (when (< (size ds) max-size)
                                       (let [E (inc (size ds))]
                                         [[E (into free (for [j idcs]
                                                          [j E]))]]))
                           alt-starts (if (seq extending)
                                        (conj alt-starts (inc (size ds)))
                                        alt-starts)]
                       (for [[E free] (concat adding extending)
                             :let [ds (glue ds i D E)]
                             :when (still-good? ds i D)
                             :let [[okay? alt-starts] (best? ds alt-starts)]
                             :when okay?]
                         [ds (disj free [i D] [i E]) alt-starts]))))})))

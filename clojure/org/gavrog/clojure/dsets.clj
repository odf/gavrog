(ns org.gavrog.clojure.dsets
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- orbits-okay? [ds i D]
  (not-any? #(> (size %) (if (loopless? %) 4 2))
            (for [j (indices ds) :when (or (< j (dec i)) (> j (inc i)))]
              (orbit ds [i j] D))))

(defn- compare-with-start [ds D]
  (loop [o2n {D 1}
         n2o {1 D}
         n 1
         free (into (sorted-set) (for [i (indices ds)] [i 1]))]
    (if (empty? free)
      0
      (let [[i D] (first free)
            oDi (s ds i (n2o D))]
        (if (nil? oDi)
          0
          (let [Di (or (o2n oDi) (inc n))
                d (- Di (s ds i D))
                free (if (<= Di n)
                       free
                       (into free (for [j (indices ds)] [j Di])))]
            (cond (nil? (s ds i D)) 0
                  (neg? d) -1
                  (pos? d) 1
                  :else (recur (assoc o2n oDi Di)
                               (assoc n2o Di oDi)
                               (max n Di)
                               (disj free [i D] [i Di])))))))))

(defn- best? [ds alt-starts]
  (let [results (into {} (for [D alt-starts] [D (compare-with-start ds D)]))]
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

(ns org.gavrog.clojure.dsets
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn- orbits-okay? [ds i D]
  (not-any? #(> (size %) (if (loopless? %) 4 2))
            (for [j (indices ds) :when (or (< j (dec i)) (> j (inc i)))]
              (orbit ds [i j] D))))

(defn- better-start? [ds D]
  (loop [o2n {D 1}
         n2o {1 D}
         n 1
         free (into (sorted-set) (for [i (indices ds)] [i 1]))]
    (if (empty? free)
      false
      (let [[i D] (first free)
            oDi (s ds i (n2o D))]
        (if (nil? oDi)
          false
          (let [Di (or (o2n oDi) (inc n))
                d (- Di (s ds i D))
                free (if (<= Di n)
                       free
                       (into free (for [j (indices ds)] [j Di])))]
            (cond (neg? d) true
                  (pos? d) false
                  :else (recur (assoc o2n oDi Di)
                               (assoc n2o Di oDi)
                               (max n Di)
                               (disj free [i D] [i Di])))))))))

(defn- best? [ds]
  (not-any? (partial better-start? ds) (rest (elements ds))))

(defn dsets [dim max-size]
  (let [idcs (range (inc dim))
        still-good? (fn [ds i D] (and (orbits-okay? ds i D) (best? ds)))]
    (make-backtracker
      {:root [(dsymbol (str "1 " dim))
              (into (sorted-set) (for [i idcs] [i 1]))]
       :extract (fn [[ds free]] (when (empty? free) (canonical ds)))
       :children (fn [[ds free]]
                   (when (seq free)
                     (let [[i D] (first free)
                           adding (for [[j E] free :when (= i j)] [E free])
                           extending (when (< (size ds) max-size)
                                       (let [E (inc (size ds))]
                                         [[E (into free (for [j idcs]
                                                          [j E]))]]))]
                       (for [[E free] (concat adding extending)
                             :let [ds (glue ds i D E)]
                             :when (still-good? ds i D)]
                         [ds (disj free [i D] [i E])]))))})))

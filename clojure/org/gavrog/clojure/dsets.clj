(ns org.gavrog.clojure.dsets
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn dsets [dim max-size]
  (let [idcs (range (inc dim))
        still-good? (fn [ds i D]
                      (empty? (for [j idcs 
                                    :when (or (< j (dec i)) (> j (inc i)))
                                    :let [o (orbit ds [i j] D)]
                                    :when (> (size o) (if (loopless? o) 4 2))]
                                j)))]
    (make-backtracker
      {:root [(dsymbol (str "1 " dim))
              (into (sorted-set) (for [i idcs] [i 1]))]
       :extract (fn [[ds free]] (when (and (empty? free) (canonical? ds)) ds))
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

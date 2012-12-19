(ns org.gavrog.clojure.dsets
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [delaney])))

(defn dsets [dim max-size]
  (let [idcs (range (inc dim))]
    (make-backtracker
      {:root [(dsymbol (str "1 " dim))
              (into (sorted-set) (for [i idcs] [i 1]))]
       :extract (fn [[ds free]] (when (empty? free) ds))
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
                             :when (canonical? ds)]
                         [ds (disj free [i D] [i E])]))))})))

(ns org.gavrog.clojure.covers
  (:use (org.gavrog.clojure
         delaney
         fundamental
         cosets)))

(defn subgroup-cover
  ([ds fund-group subgroup-gens]
    (let [{:keys [nr-generators relators edge-to-word]} fund-group
          table (coset-table nr-generators relators subgroup-gens)]
      (cover ds (count table) (fn [k i D]
                                (reduce (fn [k g] ((table k) g))
                                        k (edge-to-word [D i]))))))
  ([ds subgroup-gens]
    (subgroup-cover ds (fundamental-group ds) subgroup-gens)))

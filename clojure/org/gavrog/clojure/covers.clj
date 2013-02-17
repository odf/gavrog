(ns org.gavrog.clojure.covers
  (:use (org.gavrog.clojure
         delaney
         fundamental
         cosets)))

(defn subgroup-cover [ds subgroup-gens]
  (let [{:keys [nr-generators relators edge-to-word]} (fundamental-group ds)
        table (coset-table nr-generators relators subgroup-gens)]
    (cover ds (count table) (fn [k i D]
                              (reduce (fn [k g] ((table k) g))
                                      k (edge-to-word [D i]))))))

(defn finite-universal-cover [ds]
  (subgroup-cover ds []))

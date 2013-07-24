(ns org.gavrog.clojure.dsymbols.covers
  (:use (org.gavrog.clojure.dsymbols
          delaney
          fundamental)
        (org.gavrog.clojure.common
          [generators :only [results]])
        (org.gavrog.clojure.fpgroups
          cosets)))

(defn cover-for-table [ds table edge-to-word]
  (cover ds (count table) (fn [k i D]
                            (reduce (fn [k g] ((table k) g))
                                    k (edge-to-word [D i])))))

(defn subgroup-cover [ds subgroup-gens]
  (let [{:keys [nr-generators relators edge-to-word]} (fundamental-group ds)
        table (coset-table nr-generators relators subgroup-gens)]
    (cover-for-table ds table edge-to-word)))

(defn finite-universal-cover [ds]
  (subgroup-cover ds []))

(defn covers [ds max-degree]
  (let [{:keys [nr-generators relators edge-to-word]} (fundamental-group ds)]
    (for [table (results (table-generator nr-generators relators max-degree))]
      (cover-for-table ds table edge-to-word))))

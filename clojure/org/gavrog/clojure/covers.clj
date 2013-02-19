(ns org.gavrog.clojure.covers
  (:use (org.gavrog.clojure
         delaney
         fundamental
         cosets
         [generators :only [results]])))

(defn- cover-for-table [ds table edge-to-word]
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
    (for [table (results (tables-generator nr-generators relators max-degree))]
      (cover-for-table ds table edge-to-word))))

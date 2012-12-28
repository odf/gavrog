(ns org.gavrog.clojure.combineTiles
  (:use (clojure
          [set :only [difference]])
        (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          delaney
          partition)))

(defn- components-with-multiplicities [ds]
  (let [idcs (indices ds)]
    (frequencies (for [D (orbit-reps ds idcs)] (canonical (orbit ds idcs D))))))

(defn- partition-by-automorphism-group [ds]
  (or (seq (into pempty (apply concat (automorphisms ds))))
      (map hash-set (elements ds))))

(defn- inequivalent-forms [ds]
  (for [orb (partition-by-automorphism-group ds)]
    (canonical ds (first orb))))

(defn- signatures [ds, idcs]
  (into {} (for [D (orbit-reps ds idcs)
                 :let [sub (orbit ds idcs D)]
                 block (partition-by-automorphism-group sub)
                 :let [inv (invariant sub (first block))],
                 E block]
             [E inv])))

(defn- glue-faces [ds i D E] ;; TODO implement this
  )

(defn- children [d forms [symbol sig free-elements free-components]]
  (when-let [D (first (free-elements))]
    (let [face-idcs (range (dec d))
          face (fn [D] (orbit-elements symbol face-idcs D))
          adding (for [E free-elements :when (= (sig D) (sig E))]
                   [(glue-faces ds d D E)
                    signatures
                    (difference free-elements (face D) (face E))
                    free-components])
          extending (for [[part n] free-components
                          :when (pos? n)
                          [form s] (forms part)
                          :when (= (sig D) (s (first (elements form))))]
                      ;; TODO need to concatenate form to ds, then glue
                      [])
        ])))

(defn combine-tiles [ds]
  (let [d (inc (dim ds))
        face-idcs (range (dim ds))
        counts (components-with-multiplicities ds)
        parts (keys counts)
        forms (into {} (for [sub parts]
                         [sub (for [f (inequivalent-forms sub)]
                                [f (signatures f face-idcs)])]))]
    (make-backtracker 
      {:root (let [[sub sigs] (first (forms (first parts)))]
               [sub
                sigs
                (into #{} (orbit-reps sub face-idcs))
                (assoc counts sub (dec (counts sub)))])
       :extract (fn [[symbol _ free-elements free-components]]
                  (when (and (empty? free-elements)
                             (every? (comp zero? second) free-components)
                             (canonical? symbol))
                    symbol))
       :children (partial children d forms)})))

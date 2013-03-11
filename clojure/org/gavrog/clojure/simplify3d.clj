(ns org.gavrog.clojure.simplify3d
  (:use (clojure set)
        (org.gavrog.clojure delaney fundamental util)))

(defn merge-volumes [ds]
  (let [idx (last (indices ds))]
    (collapse ds idx (for [[D i] (inner-edges ds) :when (= i idx)
                           E [D (s ds i D)]]
                       E))))

(defn merge-facets [ds]
  (let [[i j] (drop (dec (dim ds)) (indices ds))]
    (collapse ds i (for [D (orbit-reps ds [i j]) :when (= 2 (m ds i j D))
                         E (orbit-elements ds [i j] D)]
                     E))))

(defn contract-edges [ds]
  (-> ds dual merge-volumes dual))

(defn squish-digons [ds]
  (-> ds dual merge-facets dual))

;; === The following are specific to 3d symbols

(defn- orbits [ds idcs]
  (map (partial orbit-elements ds idcs) (orbit-reps ds idcs)))

(defn- representatives-map [ds idcs]
  (into {} (for [D (orbit-reps ds idcs)
                 E (orbit-elements ds idcs D)]
             [E D])))

(defn- local-1-cuts [ds]
  (let [face-rep (representatives-map ds [0 1])
        vert-rep (representatives-map ds [1 2])
        orientation (partial-orientation ds)
        pairs (multi-map (for [D (elements ds) :when (pos? (orientation D))]
                           [[(face-rep D) (vert-rep D)] D]))]
    (for [[_ Ds] pairs :when (< 1 (count Ds))] (vec Ds))))

(defn- pinch-face [ds D E]
  (let [[F G] (map (partial s ds 3) [D E])
        [D* E* F* G*] (map (partial s ds 1) [D E F G])
        ops* (assoc (ops ds) 1 (conj ((ops ds) 1)
                                     [D E*] [E D*] [D* E] [E* D]
                                     [F G*] [G F*] [F* G] [G* F]))]
    (make-dsymbol 3 (size ds) ops* (vs ds))))

(defn pinch-first-local-1-cut [ds]
  (when-let [[D E] (first (local-1-cuts ds))]
    (pinch-face ds D E)))

(defn- face-intersections [ds]
  (let [vert-rep (representatives-map ds [1 2])
        ori (partial-orientation ds)
        faces (vec (map (partial filter (comp pos? ori)) (orbits ds [0 1])))]
    (for [i (range (count faces))
          j (range (inc i) (count faces))
          :let [[f g] (map faces [i j])
                [vf vg] (map (comp set (partial map vert-rep)) [f g])
                common (intersection vf vg)]
          :when (pos? (count common))]
      [f g common])))

(defn- positions [p xs]
  (for [[i x] (zipmap (range) xs) :when (p x)] i))

(defn- local-2-cuts [ds]
  (filter (fn [[f g common]]
            (let [verts (map (fn [D] (set (orbit-elements ds [1 2] D))) common)
                  find (fn [D] (first (positions #(% D) verts)))
                  pf (map find f)
                  pg (map find g)
                  _ (println pf pg)]
            ))
          (face-intersections ds)))

(defn- pinch-tile [ds D E]
  (let [[D* E*] (map (partial s ds 0) [D E])
        [F G F* G*] (map (partial s ds 2) [D E D* E*])
        ops* (assoc (ops ds) 2 (conj ((ops ds) 2)
                                     [D E*] [E D*] [D* E] [E* D]
                                     [F G*] [G F*] [F* G] [G* F]))]
    (make-dsymbol 3 (size ds) ops* (vs ds))))

(defn- cut-face [ds D E])

(defn- cut-conditionally [ds D]
  (if (< 3 (m ds 0 1 D))
    (cut-face ds (walk ds D 0) (walk ds D 1 0))
    ds))

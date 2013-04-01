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

(defn- local-2-cuts [ds]
  (let [ori (partial-orientation ds)
        faces (vec (orbits ds [0 1]))]
    (for [i (range (count faces))
          :let [f (set (faces i))
                marked (set (for [D f, E (orbit-elements ds [1 2] D)
                                  :when (not (f (s ds 2 E)))] E))]
          j (range (inc i) (count faces))
          :let [cut (filter #(and (marked %) (pos? (ori %))) (faces j))]
          :when (<= 2 (count cut))]
      (apply concat (for [D cut]
                      [D (first (filter #(and (f %) (pos? (ori %)))
                                        (orbit-elements ds [1 2] D)))])))))

(defn- pinch-tile [ds D E]
  (let [[D* E*] (map (partial s ds 0) [D E])
        [F G F* G*] (map (partial s ds 2) [D E D* E*])
        ops* (assoc (ops ds) 2 (conj ((ops ds) 2)
                                     [D E*] [E D*] [D* E] [E* D]
                                     [F G*] [G F*] [F* G] [G* F]))]
    (make-dsymbol 3 (size ds) ops* (vs ds))))

(def ^{:private true} wedge
  (dsymbol "8:2 4 6 8,3 4 7 8,3 4 7 8,5 6 7 8:2 2,1 1 1 1,2 2"))

(defn- cut-face [ds D E]
  (if (#{(walk ds D 1 0) (walk ds D 0 1)} E)
    ds
    (let [tmp (append ds wedge)
          n (size ds)
          [F G] (map (partial s ds 3) [D E])
          [D* E* F* G*] (map (partial s ds 1) [D E F G])
          [d e* d* e f g* f* g] (map (partial + n) (range 1 9))
          ops* (assoc (ops tmp) 1 (conj ((ops tmp) 1)
                                         [D d] [d D] [D* d*] [d* D*]
                                         [E e] [e E] [E* e*] [e* E*]
                                         [F f] [f F] [F* f*] [f* F*]
                                         [G g] [g G] [G* g*] [g* G*]))
          t (make-dsymbol 3 (size tmp) ops* {})
          vs* (into {} (for [i (range 3)]
                        [i (into {} (for [D (orbit-reps t [i (inc i)])
                                          :let [v* 1]
                                          E (orbit-elements t [i (inc i)] D)]
                                      [E v*]))]))]
      (make-dsymbol (dim t) (size t) ops* vs*))))

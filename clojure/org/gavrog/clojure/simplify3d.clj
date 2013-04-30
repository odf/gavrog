(ns org.gavrog.clojure.simplify3d
  (:use (clojure set)
        (org.gavrog.clojure delaney fundamental util)))

;;TODO Code assumes input symbols are orientable and trivially branched.

(defn valid-input? [ds]
  (and (oriented? ds)
       (every? (partial = 1)
               (for [i (indices ds), j (indices ds), D (elements ds)]
                 (v ds i j D))))) 

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

(defn on-dual [f]
  (fn [ds] (-> ds dual f dual)))

(def contract-edges (on-dual merge-volumes))

(def squish-digons (on-dual merge-facets))

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
  (if-let [[D E] (first (local-1-cuts ds))]
    (pinch-face ds D E)
    ds))

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
  (cond (= E (walk ds D 1 0)) [ds (s ds 1 D)]
        (= E (walk ds D 0 1)) [ds D]
        :else
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
              vs* (into {} (for [i (range 3), :let [j (inc i)]]
                             [i (into {} (for [D (orbit-reps t [i j])
                                               E (orbit-elements t [i j] D)]
                                           [E 1]))]))]
          [(make-dsymbol (dim t) (size t) ops* vs*) d*])))

(defn- o-range [ds i j start end]
  (if (= start end)
    (list end)
    (lazy-seq (cons start (o-range ds i j (walk ds start i j) end)))))

(defn- liftable? [ds [A B C D]]
  (->> (o-range ds 0 1 A C) (map #(walk ds % 3 1)) (filter #{B D}) count
    (not= 1)))

(defn- kf-complexity [ds]
  (reduce + (for [D (orbit-reps ds [0 1 3])] (- (r ds 0 1 D) 2))))

(defn- complexity [ds]
  [(kf-complexity ds) (size ds)])

(defn pinch-first-local-2-cut [ds]
  (if-let [[A B C D] (first (filter (partial liftable? ds) (local-2-cuts ds)))]
    (let [[ds E1] (cut-face ds A C)
          [ds E2] (cut-face ds D B)
          ds (pinch-tile ds E1 E2)
          _ (println ds)]
      (merge-volumes ds))
    ds))

(defn simplified [ds]
  (let [clean (fn [ds] (-> ds
                         merge-volumes merge-facets dual
                         merge-volumes merge-facets dual))
        operations [pinch-first-local-1-cut
                    pinch-first-local-2-cut
                    (on-dual pinch-first-local-1-cut)
                    (on-dual pinch-first-local-2-cut)]]
    (loop [ds (clean ds), pending operations]
      (if-let [f (first pending)]
        (let [t (f ds)]
          (if (= t ds)
            (recur ds (rest pending))
            (let [t (clean t)]
              (assert (neg? (compare [(complexity t) (size t)]
                                     [(complexity ds) (size ds)]))
                      (str "Complexity not reduced by\n  " (prn-str (class f))
                           (prn-str ds) (prn-str (complexity ds)
                           "  =>\n" (prn-str t) (prn-str (complexity t)))))
              (recur t operations))))
        ds))))

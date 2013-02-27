(ns org.gavrog.clojure.arithmetic)

(defn abs [n] (if (neg? n) (- n) n))

(defn sign [n]
  (cond (zero? n) 0
        (neg? n) -1
        :else 1))

(defn gcdex [m n]
  (loop [f (abs m), fm (sign m), g (abs n), gm 0]
    (if (zero? g)
      (if (zero? n)
        [f [fm 0 gm 1]]
        [f [fm (quot (- f (* fm m)) n) gm (quot (- (* gm m)) n)]])
      (let [x (quot f g)]
        (recur g gm (- f (* x g)) (- fm (* x gm)))))))

(defn- combine-rows [M cols i1 i2 f11 f12 f21 f22]
  (let [stuff! (partial reduce conj!)]
    (-> (transient M)
      (stuff! (for [j cols] [[i1 j] (+ (* (M [i1 j]) f11) (* (M [i2 j]) f12))]))
      (stuff! (for [j cols] [[i2 j] (+ (* (M [i1 j]) f21) (* (M [i2 j]) f22))]))
      persistent!)))

(defn- combine-cols [M rows j1 j2 f11 f12 f21 f22]
  (let [stuff! (partial reduce conj!)]
    (-> (transient M)
      (stuff! (for [i rows] [[i j1] (+ (* (M [i j1]) f11) (* (M [i j2]) f12))]))
      (stuff! (for [i rows] [[i j2] (+ (* (M [i j1]) f21) (* (M [i j2]) f22))]))
      persistent!)))

(defn- smallest [xs]
  (when (seq xs)
    (reduce (fn [a b] (if (neg? (compare a b)) a b)) (first xs) (rest xs))))

(defn- clear-col [M rows cols]
  (let [i0 (first rows)
        j0 (first cols)]
    (reduce (fn [M i]
              (let [a (M [i0 j0]) b (M [i j0])]
                (cond (and (not (zero? a)) (zero? (mod b a)))
                      (combine-rows M cols i0 i 1 0 (- (quot b a)) 1)
                      
                      (not (zero? b))
                      (let [[_ [f11 f12 f21 f22]] (gcdex a b)]
                        (combine-rows M cols i0 i f11 f12 f21 f22))
                      
                      :else
                      M)))
            (rest rows))))

(defn- clear-row [M rows cols])

(defn- eliminate [M rows cols]
  (let [M (clear-col M rows cols)
        [M dirty] (clear-row M rows cols)]
    (if dirty
      (recur M rows cols)
      M)))

(defn diagonalized [M rows cols]
  (if (or (empty? rows) (empty? cols))
    M
    (let [i0 (first rows)
          j0 (first cols)
          [_ ip jp] (smallest (for [i rows, j cols
                                    :when (not (zero? (M [i j])))]
                                [i j]))
          M (combine-rows M cols i0 ip 0 1 1 0)
          M (combine-cols M rows j0 jp 0 (sign (M [ip jp])) 1 0)]
      (recur (eliminate M rows cols) (rest rows) (rest cols)))))

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

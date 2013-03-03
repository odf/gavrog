(ns org.gavrog.clojure.arithmetic
  (:use (org.gavrog.clojure util)))

(defn divides? [a b]
  (and (not (zero? a)) (zero? (mod b a))))

(defn gcdex [m n]
  (loop [f (abs m), fm (sign m), g (abs n), gm 0]
    (if (zero? g)
      (if (zero? n)
        [fm 0 gm 1]
        [fm (quot (- f (* fm m)) n) gm (quot (- (* gm m)) n)])
      (let [x (quot f g)]
        (recur g gm (- f (* x g)) (- fm (* x gm)))))))

(defn gcd [m n]
  (loop [f (abs m), g (abs n)]
    (if (zero? g)
      f
      (recur g (mod f g)))))

(defn lcm [m n]
  (if (and (zero? m) (zero? n))
    0
    (abs (* (quot m (gcd m n)) n))))

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
  (let [i0 (first rows), j0 (first cols)]
    (reduce (fn [M i]
              (let [a (M [i0 j0]), b (M [i j0])]
                (cond (divides? a b)
                      (combine-rows M cols i0 i 1 0 (- (quot b a)) 1)
                      
                      (not (zero? b))
                      (apply combine-rows M cols i0 i (gcdex a b))
                      
                      :else
                      M)))
            M
            (rest rows))))

(defn- try-to-clear-row [M rows cols]
  (let [i0 (first rows), j0 (first cols)]
    (loop [M M, js (rest cols)]
      (if-let [j (first js)]
        (let [a (M [i0 j0]), b (M [i0 j])]
          (cond (divides? a b)
                (recur (assoc M [i0 j] 0) (rest js))
                      
                (not (zero? b))
                [(apply combine-cols M rows j0 j (gcdex a b)) true]
                      
                :else
                (recur M (rest js))))
        [M false]))))

(defn- eliminate [M rows cols]
  (let [[M dirty] (try-to-clear-row (clear-col M rows cols) rows cols)]
    (if dirty (recur M rows cols) M)))

(defn diagonalized [M rows cols]
  (if-let [[_ i j s] (smallest (for [i rows, j cols
                                     :let [x (M [i j])]
                                     :when (not (zero? x))]
                                 [(abs x) i j (sign x)]))]
    (let [M (combine-rows M cols (first rows) i 0 1 1 0)
          M (combine-cols M rows (first cols) j 0 1 s 0)]
      (recur (eliminate M rows cols) (rest rows) (rest cols)))
    M))

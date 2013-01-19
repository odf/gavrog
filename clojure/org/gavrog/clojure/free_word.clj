(ns org.gavrog.clojure.free-word)

(defn normalise [xs]
  (assert (every? integer? xs))
  (reduce (fn [w x] (cond (= 0 x)
                          w,
                          (= (last w) (- x))
                          (pop w),
                          :else
                          (conj w x)))
          [] xs))

(defn- overlap [w1 w2]
  (assert (every? integer? w1))
  (assert (every? integer? w2))
  (let [n1 (count w1)
        n2 (count w2)
        n (min n1 n2)
        k (some (fn [k] (if (= (- (nth w2 k)) (nth w1 (- (dec n1) k))) nil k))
                (range n))]
        (or k n)))

(defn product
  ([] [])
  ([w] w)
  ([w1 w2]
    (let [k (overlap w1 w2)]
      (into (subvec w1 0 (- (count w1) k)) (subvec w2 k))))
  ([w1 w2 & ws]
    (reduce product (product w1 w2) ws)))

(defn inverse [w]
  (assert (every? integer? w))
  (into [] (map - (reverse w))))

(defn raised-to [w m]
  (cond
    (zero? m)
    [],
    (neg? m)
    (raised-to (inverse w) (- m)),
    :else
    (let [n (count w)
          k (overlap w w)]
      (if (= k n)
        (if (odd? m) w [])
        (let [head (subvec w 0 (- (dec n) k))
              tail (subvec w (inc k))
              mid (subvec w k (- n k))]
          (into (reduce into head (repeat m mid)) tail))))))

(defn commutator [a b]
  (product a b (inverse a) (inverse b)))

(def w* product)

(def w** raised-to)

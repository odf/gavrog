(ns org.gavrog.clojure.delaney2d
  (:use (org.gavrog.clojure delaney)))

(defn curvature
  ([ds default-v]
    (do
      (assert (= 2 (dim ds)) (str "Expected 2d symbol, got " (dim ds) "d"))
      (assert (connected? ds) "Symbol must be connected")
      (let [[i j k] (indices ds)]
        (reduce +
                (- (size ds))
                (for [[i j] [[i j] [i k] [j k]]
                      :let [s #(if (orbit-loopless? ds [i j] %) 2 1)
                            v #(or (v ds i j %) default-v)]
                      D (orbit-reps ds [i j])]
                  (/ (s D) (v D)))))))
  ([ds]
    (curvature ds 0)))

(defn euclidean? [ds] (zero? (curvature ds 1)))

(defn proto-euclidean? [ds] (not (neg? (curvature ds 1))))

(defn hyperbolic? [ds] (neg? (curvature ds 1)))

(defn spherical? [ds]
  (and (pos? (curvature ds 1))
       (let [ds (oriented-cover ds)
             [i j k] (indices ds)
             cones (for [[i j] [[i j] [i k] [j k]]
                         D (orbit-reps ds [i j])
                         :when (< 1 (v ds i j D))]
                     (v ds i j D))
             n (count cones)]
         (and (not= 1 n) (or (not= 2 n) (= (first cones) (second cones)))))))

(defn proto-spherical? [ds] (pos? (curvature ds 1)))

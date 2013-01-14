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

(defn euclidean? [ds] (= (curvature ds 1) 0))

(defn proto-euclidean? [ds] (>= (curvature ds 1) 0))

(ns org.gavrog.clojure.delaney3d
  (:use (org.gavrog.clojure
          util
          delaney
          fundamental
          cosets
          [generators :only [results]])))

(defn massoc [m k v]
  (assoc m k (conj (get m k #{}) v)))

(def ^{:private true} core-type
  {1 :z1, 2 :z2, 3 :z3, 6 :s3, 8 :d4, 12 :a4, 24 :s4})

(defn- fully-involutive? [ct]
  (every? true? (for [row (vals ct), g (keys row)] (= (row g) (row (- g))))))

(defn- trace-word [ct k wd]
  (reduce (fn [[k g]] ((ct k) g)) k wd))

(defn- flattens? [ct [wd deg]]
  (= deg (some (fn [[i k]] (when (zero? k) i))
               (reductions (fn [[i k]] [(inc i) (trace-word ct k wd)])
                           [0 0]
                           (range (inc deg))))))

(defn- flattens-all? [ct cones]
  (every? (partial flattens? ct) cones))

(defn pseudo-toroidal-cover [ds]
  (let [{:keys [nr-gens relators cones edge-to-word]} (fundamental-group ds)
        cones2 (filter (fn [[wd deg]] (= deg 2)) cones)
        cones3 (filter (fn [[wd deg]] (= deg 3)) cones)
        base (map core-table (results (table-generator nr-gens relators 4)))
        good (for [ct base :when (flattens-all? ct cones)
                   type (if (= (count ct) 4)
                          (if (not (fully-involutive? ct)) :z4 :v4)
                          (map core-type (count ct)))]
               [type ct])
        candidates (reduce (fn [m [k v]] (massoc m k v)) {} good)
        ]))

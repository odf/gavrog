(ns org.gavrog.clojure.delaney3d
  (:use (org.gavrog.clojure
          util
          delaney
          fundamental
          cosets
          covers
          [generators :only [results]])))

(defn massoc [m k v]
  (assoc m k (conj (get m k #{}) v)))

(def ^{:private true} core-type
  {1 :z1, 2 :z2, 3 :z3, 6 :s3, 8 :d4, 12 :a4, 24 :s4})

(defn- fully-involutive? [ct]
  (every? true? (for [row (vals ct), g (keys row)] (= (row g) (row (- g))))))

(defn- trace-word [ct k wd]
  (reduce (fn [k g] ((ct k) g)) k wd))

(defn- degree [ct wd]
  (some (fn [[i k]] (when (zero? k) i))
        (rest (reductions (fn [[_ k] i] [(inc i) (trace-word ct k wd)])
                          [0 0] (range)))))

(defn- flattens? [ct [wd deg]]
  (= deg (degree ct wd)))

(defn- flattens-all? [ct cones]
  (every? (partial flattens? ct) cones))

(defn pseudo-toroidal-cover [ds]
  (let [ds (oriented-cover ds)
        {:keys
         [nr-generators relators cones edge-to-word]} (fundamental-group ds)
        cones2 (filter (fn [[wd deg]] (= deg 2)) cones)
        cones3 (filter (fn [[wd deg]] (= deg 3)) cones)
        base (map core-table
                  (results (table-generator nr-generators relators 4)))
        good (for [ct base :when (flattens-all? ct cones)]
               (let [type (if (= (count ct) 4)
                            (if (fully-involutive? ct) :v4 :z4)
                            (core-type (count ct)))]
                 [type ct]))
        candidates (reduce (fn [m [k v]] (massoc m k v)) {} good)
        
        ]
    candidates))

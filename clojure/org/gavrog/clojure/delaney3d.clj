(ns org.gavrog.clojure.delaney3d
  (:use (org.gavrog.clojure
          util
          delaney
          fundamental
          cosets
          covers
          simplify3d
          [generators :only [results]])))

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

(defn- invariants [ds]
  (let [{:keys [nr-generators relators]} (fundamental-group ds)]
    (abelian-invariants nr-generators relators)))

(defn pseudo-toroidal-cover [ds]
  (let [ds (oriented-cover ds)
        {:keys
         [nr-generators relators cones edge-to-word]} (fundamental-group ds)
        _ (assert (every? (comp #{1 2 3 4 6} second) cones)
                  "Symbol violates the crystallographic restriction")
        cones2 (filter (fn [[wd deg]] (= deg 2)) cones)
        cones3 (filter (fn [[wd deg]] (= deg 3)) cones)
        base (map core-table
                  (results (table-generator nr-generators relators 4)))
        cores (for [ct base :when (flattens-all? ct cones)]
                (let [type (if (= (count ct) 4)
                             (if (fully-involutive? ct) :v4 :z4)
                             (core-type (count ct)))]
                  [type ct]))
        z2a (filter (fn [ct] (and (= 2 (count ct))
                                  (flattens-all? ct cones2)))
                    base)
        z2b (filter (fn [ct] (and (= 2 (count ct))
                                  (not (flattens-all? ct cones2))))
                    base)
        z3a (filter (fn [ct] (and (= 3 (count ct))
                                  (flattens-all? ct cones3)))
                    base)
        s3a (filter (fn [ct] (and (= 6 (count ct))
                                  (flattens-all? ct cones3)))
                    base)
        z6 (for [a z3a, b z2a
                 :let [c (intersection-table a b)]
                 :when (and (= 6 (count c)) (flattens-all? c cones))]
             [:z6 c])
        d6 (for [a s3a, b z2b
                 :let [c (intersection-table a b)]
                 :when (and (= 12 (count c)) (flattens-all? c cones))]
             [:d6 c])
        categorized (multi-map (concat cores z6 d6))
        candidates (for [type [:z1 :z2 :z3 :z4 :v4 :s3 :z6 :d4 :d6 :a4 :s4]
                         ct (categorized type)]
                     (cover-for-table ds ct edge-to-word))]
    (some (fn [cov] (when (= [0 0 0] (invariants cov)) cov))
          candidates)))

(defn- check-axes [{:keys [symbol] :as input}]
  (let [[a b c d] (indices symbol)
        idx-pairs [[a b] [a c] [a d] [b c] [b d] [c d]]
        degrees (for [[i j] idx-pairs, D (orbit-reps symbol [i j])]
                  (v symbol i j D))]
    (if (every? #{1 2 3 4 6} degrees)
      input
      (conj input
            [:result false]
            [:explanation "violates crystallographic restriction"]))))

(defn- check-cover [{:keys [symbol] :as input}]
  (if-let [cover (pseudo-toroidal-cover symbol)]
    (conj input [:cover cover] [:output cover]) 
    (conj input [:result false] [:explanation "no pseudo-toroidal cover"])))

(def ^{:private true} proto-tori
  (set (map (comp canonical pseudo-toroidal-cover dsymbol)
            ["1 3:1,1,1,1:4,3,4"
             "8 3:2 4 6 8,6 3 5 7 8,1 2 4 7 8,2 6 5 8:3 4,3 5,4 3"])))

(defn- check-simplified [{:keys [cover] :as input}]
  (let [simple (simplified cover)]
    (cond (zero? (size simple))
          (conj input [:result false] [:explanation "cover is a lens space"])
          
          (not (connected? cover)) ;; TODO inspect components
          (conj input [:result :maybe] [:explanation "cover is connected sum"])
          
          (proto-tori (canonical simple))
          (conj input [:result true] [:explanation "simplified cover known"])
          
          :else
          (conj input [:simple simple] [:output simple]))))

(def checks [check-axes check-cover check-simplified])

(defn check-euclicidity [ds]
  (loop [data {:symbol ds}, to-do checks]
    (if (seq to-do)
      (let [data ((first to-do) data)]
        (if (nil? (:result data))
          (recur data (rest to-do))
          (let [extra (case (:result data)
                        false []
                        true  [(:symbol data)]
                        [(:output data)])]
            (concat [(:result data) (:explanation data)] extra))))
      [:maybe "no decision found" (:output data)])))

(ns org.gavrog.clojure.cosets
  (:use (org.gavrog.clojure
          free-word
          partition
          util)))

(defn- merge-rows [table equiv q a b]
  (let [merge (fn [ra rb]
                (reduce (fn [r g] (if (ra g) r (assoc r g (rb g))))
                        ra (keys rb)))
        row-a (merge (table a) (table b))
        row-b (merge (table b) (table a))
        q (into q (for [g (keys row-a)
                        :when (not= (pfind equiv (row-a g))
                                    (pfind equiv (row-b g)))]
                    [(row-a g) (row-b g)]))
        table (conj table [a row-a] [b row-a])]
    [table q]))

(defn- identify [table equiv i j]
  (loop [q (conj empty-queue [i j])
         table table
         equiv equiv]
    (if-let [[i j] (first q)]
      (let [q (pop q)
            a (pfind equiv i)
            b (pfind equiv j)]
        (if (= a b)
          (recur q table equiv)
          (let [equiv (punion equiv a b)
                [table q] (merge-rows table equiv q a b)]
            (recur q table equiv))))
      [table equiv])))

(defn- scan [table equiv w start]
  (let [n (count w)
        [head a] (loop [row start, i 0]
                   (if (>= i n)
                     [row i]
                     (if-let [next ((table row) (get w i))]
                       (recur next (inc i))
                       [row i])))
        [tail b] (loop [row start, i n]
                   (if (<= i a)
                     [row i]
                     (if-let [next ((table row) (- (get w (dec i))))]
                       (recur next (dec i))
                       [row i])))]
    (cond (= (inc a) b)
          [(-> table
             (assoc-in [head (get w a)] tail)
             (assoc-in [tail (- (get w a))] head))
           equiv]
          
          (and (= a b) (not= head tail))
          (identify table equiv head tail)
          
          :else
          [table equiv])))

(defn- scan-relations [rels subgens table equiv n]
  (let [[table equiv]
        (reduce (fn [[t p] w] (scan t p w n)) [table equiv] rels)]
    (reduce (fn [[t p] w] (scan t p w (pfind equiv 0))) [table equiv] subgens)))

(defn- compressed-table [table equiv]
  (let [canon (into {} (for [i (keys table)] [i (pfind equiv i)]))]
    (into {} (for [[i row] table :when (= i (canon i))]
               [i (into {} (map (fn [[j v]] [j (canon v)]) row))]))))

(defn coset-table [nr-gens relators subgroup-gens]
  (let [with-inverses (fn [ws] (vec (into #{} (concat ws (map inverse ws)))))
        gens (vec (concat (range 1 (inc nr-gens))
                          (range -1 (- (inc nr-gens)) -1)))
        rels (with-inverses (for [w relators, i (range (count w))]
                              (into (subvec w i) (subvec w 0 i))))
        subgens (with-inverses subgroup-gens)]
    (loop [table {0 {}}
           equiv pempty
           i 0
           j 0]
      (assert (< (count table) 500))
      (cond (>= i (count table))
            (compressed-table table equiv)
            
            (or (>= j (count gens)) (not= i (pfind equiv i)))
            (recur table equiv (inc i) 0)
            
            ((table i) (get gens j))
            (recur table equiv i (inc j))
            
            :else
            (let [g (get gens j)
                  g* (- g)
                  n (count table)
                  table (-> table (assoc-in [i g] n) (assoc-in [n g*] i))
                  [table equiv] (scan-relations rels subgens table equiv n)]
              (recur table equiv i (inc j)))))))

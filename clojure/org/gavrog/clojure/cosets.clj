(ns org.gavrog.clojure.cosets
  (:use (org.gavrog.clojure
          free-word
          partition)))

(defn- scan [table equiv w start]
  (let [n (dec (count w))
        [head a] (loop [row start, i 0]
                   (let [next ((table row) (get w i))]
                     (if (and next (< i n))
                       (recur next (inc i))
                       [row i])))
        [tail b] (loop [row start, i n]
                   (let [next ((table row) (inverse (get w i)))]
                     (if (and next (>= i a))
                       (recur next (dec i))
                       [row i])))]
    (cond (= a b)
          [(-> table
             (assoc-in head (get w a) tail)
             (assoc-in tail (inverse (get w a)) head))
           equiv]
          
          (and (< b a) (not= head tail))
          [table (punion equiv head tail)]
          
          :else
          [table equiv])))

(defn- scan-relations [rels subgens table equiv n]
  (let [[table equiv]
        (reduce (fn [[t p] w] (scan t p w n)) [table equiv] rels)]
    (reduce (fn [[t p] w] (scan t p w 0)) [table equiv] subgens)))

(defn- compressed-table [table equiv]
  table)

(defn coset-table [nr-gens relators subgroup-gens]
  (let [with-inverses (fn [ws] (vec (concat ws (map inverse ws))))
        gens (with-inverses (map vector (range 1 (inc nr-gens))))
        rels (with-inverses (for [w relators, i (range (count w))]
                              (into (subvec w i) (subvec w 0 i))))
        subgens (with-inverses subgroup-gens)]
    
    (loop [table {0 {}}
           equiv pempty
           i 0
           j 0]
      (cond (>= i (count table))     (compressed-table table equiv)
            (>= j (count gens))      (recur table equiv (inc i) 0)
            ((table i) (get gens j)) (recur table equiv i (inc j))
            
            :else
            (let [g (get gens j)
                  g* (inverse g)
                  n (count table)
                  table (-> table (assoc-in i g n) (assoc-in n g* i))
                  [table equiv] (scan-relations rels subgens table equiv n)]
              (recur table equiv i (inc j)))))))

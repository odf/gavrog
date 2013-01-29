(ns org.gavrog.clojure.dsets-combining
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [simple-generators :only [integer-partitions]]
          [delaney]
          [combineTiles])))

(defn- circuit [n]
  (assert (even? n))
  (let [elms (range 1 (inc n))
        circ (fn [i] (inc (mod (dec i) n)))
        cinc (fn [i] (circ (inc i)))
        cdec (fn [i] (circ (dec i)))]
    (make-dsymbol 1
                  n
                  {0 (into {} (for [i elms]
                                [i (if (odd? i) (cinc i) (cdec i))]))
                   1 (into {} (for [i elms]
                                [i (if (odd? i) (cdec i) (cinc i))]))}
                  {})))

(defn- chain [n i]
  (assert (#{0 1} i))
  (let [elms (range 1 (inc n))
        op-a (fn [i] (cond ((if (odd? n) #{1} #{1 n}) i) i
                           (even? i) (inc i)
                           :else (dec i)))
        op-b (fn [i] (cond (and (odd? n) (= n i)) i
                           (even? i) (dec i)
                           :else (inc i)))
        [op-0 op-1] (if (zero? i) [op-a op-b] [op-b op-a])]
    (make-dsymbol 1 n {0 (into {} (for [i elms] [i (op-0 i)]))
                       1 (into {} (for [i elms] [i (op-1 i)]))} {})))

(defn- orbit-lists [sizes]
  (let [sizes (vec sizes)
        n (count sizes)
        make-orbit (fn [n i] (if (= i 2) (circuit n) (chain n i)))]
    (make-backtracker
      {:root []
       :extract (fn [xs]
                  (when (= n (count xs))
                    (reduce append (dsymbol "0 1") (map make-orbit sizes xs))))
       :children (fn [xs]
                   (let [m (count xs)]
                     (when (< m n)
                       (if (odd? (nth sizes m))
                         [(conj xs 0)]
                         (let [k (if (and (> m 0)
                                          (= (nth sizes m) (nth sizes (dec m))))
                                   (nth xs (dec m))
                                   0)]
                           (for [i (range k 3)] (conj xs i)))))))})))

(defn dsets2d [max-size]
  (for [s (range 1 (inc max-size))
        sizes (results (integer-partitions s))
        olist (results (orbit-lists (reverse sizes)))
        ds (results (combine-tiles olist))]
    ds))

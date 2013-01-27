(ns org.gavrog.clojure.dsets-new
  (:use (org.gavrog.clojure
          [generators :only [make-backtracker results]]
          [simple-generators :only [integer-partitions]]
          [delaney])))

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

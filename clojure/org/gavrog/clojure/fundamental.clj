(ns org.gavrog.clojure.fundamental
  (:use (org.gavrog.clojure delaney)))

(defn- other [a b c]
  (if (= c a) b a))

(defn- initial-todo [ds]
  (loop [result []
         seen #{}
         todo (traversal ds (indices ds) (elements ds))]
    (if (empty? todo)
      (seq result)
      (let [[D i E] (first todo)
            todo (rest todo)]
        (cond (= :root i)
              (recur result (conj seen D) todo)
              (seen E)
              (recur result seen todo)
              :else
              (recur (conj result [D i nil]) (conj seen E) todo))))))

(defn- update-todo [ds todo opposite D i]
  (into todo (for [j (indices ds) :when (not= i j)]
               (let [[E k _] (opposite [D i j])]
                 [E k (other i j k)]))))

(defn- initial-boundary [ds]
  [(set (for [D (elements ds), i (indices ds)] [D i]))
   (into {} (for [D (elements ds)
                  i (indices ds)
                  j (indices ds) :when (not= i j)]
              [[D i j] [D j 1]]))])

(defn- glue-boundary [ds [on-bnd opposite] D i]
  (let [E (s ds i D)]
    [(disj on-bnd [D i] [E i])
     (into opposite
           (apply concat (for [j (indices ds) :when (not= i j)]
                           (let [[D* r a] (opposite [D i j])
                                 [E* s b] (opposite [E i j])
                                 n (+ a b)]
                             [[[D* r (other i j r)] [E* s n]]
                              [[E* s (other i j s)] [D* r n]]]))))]))

(defn fundamental-edges [ds]
  (loop [todo (initial-todo ds)
         [on-bnd opposite :as boundary] (initial-boundary ds)
         result []]
    (if (empty? todo)
      result
      (let [[D i j] (first todo)
            [_ _ n] (opposite [D i j])
            todo (rest todo)]
        (if (and (on-bnd [D i])
                 (or (nil? j)
                     (= n (* 2 (m ds i j D)))))
          (recur (update-todo ds todo opposite D i)
                 (glue-boundary ds boundary D i)
                 (conj result [D i]))
          (recur todo boundary result))))))

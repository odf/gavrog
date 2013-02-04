(ns org.gavrog.clojure.fundamental
  (:use (org.gavrog.clojure delaney free-word)))

(defn- other [a b c]
  (if (= c a) b a))

(defn- update-todo [ds todo opposite D i]
  (into todo (for [j (indices ds) :when (not= i j)]
               (let [[E k _] (opposite [D i j])]
                 [E k (other i j k)]))))

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

(defn- glue-recursively [ds boundary edges]
  (loop [todo edges
         [on-bnd opposite :as boundary] boundary
         glued []]
    (if (empty? todo)
      [boundary glued]
      (let [[D i j] (first todo)
            [_ _ n] (opposite [D i j])
            todo (rest todo)]
        (if (and (on-bnd [D i])
                 (or (nil? j)
                     (= n (* 2 (m ds i j D)))))
          (recur (update-todo ds todo opposite D i)
                 (glue-boundary ds boundary D i)
                 (conj glued [D i]))
          (recur todo boundary glued))))))

(defn- initial-todo [ds]
  (first
    (reduce (fn [[result seen] [D i E]]
              [(if (or (= :root i) (seen E)) result (conj result [D i nil]))
               (conj seen E)])
            [[] #{}]
            (traversal ds (indices ds) (elements ds)))))

(defn- initial-boundary [ds]
  [(set (for [D (elements ds), i (indices ds)] [D i]))
   (into {} (for [D (elements ds)
                  i (indices ds)
                  j (indices ds) :when (not= i j)]
              [[D i j] [D j 1]]))])

(defn inner-edges [ds]
  "Returns the list of inner edges for a fundamental domain."
  (second (glue-recursively ds (initial-boundary ds) (initial-todo ds))))

(defn- trace-word [ds edge2word D i]
  )

(defn- glue-generator [ds [_ opposite :as boundary] edge2word gen2edge D i]
  (let [gen (count gen2edge)
        gen2edge (conj gen2edge [gen [D i]])
        [boundary glued] (glue-recursively ds boundary [[D i nil]])
        edge2word (reduce (fn [e2w [D i]]
                            (conj e2w (inverse (trace-word ds e2w D i))))
                          (conj edge2word [[D i] [gen]])
                          (rest glued))]
    [boundary edge2word gen2edge]))

(defn- find-generators [ds]
  (let [boundary (reduce (fn [bnd [D i]] (glue-boundary ds bnd D i))
                         (initial-boundary ds)
                         (inner-edges ds))]
    (drop 1 (reduce (fn [[[on-bnd _ :as boundary] edge2word gen2edge] [D i]]
                      (if (on-bnd [D i])
                        (glue-generator ds boundary edge2word gen2edge D i)                        
                        [boundary edge2word gen2edge]))
                    (for [D (elements ds), i (indices ds)] [D i])
                    [boundary {} {}]))))

(defn fundamental-group [ds]
  (let [[edge2word, gen2edge] (find-generators ds)]
    ))

(ns org.gavrog.clojure.fundamental
  (:use (org.gavrog.clojure
          delaney
          free-word)
        (org.gavrog.clojure.common
          util)))

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
  (loop [todo (into empty-queue edges)
         [on-bnd opposite :as boundary] boundary
         glued []]
    (if (empty? todo)
      [boundary glued]
      (let [[D i j] (first todo)
            [_ _ n] (opposite [D i j])
            todo (pop todo)]
        (if (and (on-bnd [D i])
                 (or (nil? j)
                     (= n (* 2 (or (m ds i j D) 0)))))
          (recur (update-todo ds todo opposite D i)
                 (glue-boundary ds boundary D i)
                 (conj glued [D i j]))
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

(defn- trace-word [ds edge2word i j D]
  (loop [w [], E (s ds i D), k j]
    (let [f (or (edge2word [E k]) (inverse (edge2word [(s ds k E) k])) [])]
      (if (= [E k] [D i])
        (-* w f)
        (recur (-* w f) (s ds k E) (other i j k))))))

(defn- glue-generator [ds [_ opposite :as boundary] edge2word gen2edge D i]
  (let [gen (inc (count gen2edge))
        gen2edge (conj gen2edge [gen [D i]])
        [boundary glued] (glue-recursively ds boundary [[D i nil]])
        edge2word (reduce (fn [e2w [D i j]]
                            (conj e2w
                                  [[D i] (inverse (trace-word ds e2w i j D))]))
                          (conj edge2word [[D i] [gen]])
                          (rest glued))]
    [boundary edge2word gen2edge]))

(defn- add-inverses [ds edge2word]
  (into edge2word (for [[[D i] w] edge2word]
                    [[(s ds i D) i] (inverse w)])))

(defn- find-generators [ds]
  (let [boundary
        (reduce (fn [bnd [D i]] (glue-boundary ds bnd D i))
                (initial-boundary ds)
                (inner-edges ds))

        [_ e2w g2e]
        (reduce (fn [[[on-bnd _ :as boundary] e2w g2e] [D i]]
                  (if (on-bnd [D i])
                    (glue-generator ds boundary e2w g2e D i)                        
                    [boundary e2w g2e]))
                [boundary {} {}]
                (for [D (elements ds), i (indices ds)] [D i]))]
    [(add-inverses ds e2w) g2e]))

(defn- mirror-relators [ds g2e]
  (for [[g [D i]] g2e
        :when (= D (s ds i D))]
   [g g]))

(defn- relator-rep [w]
  (apply (partial lexicographically-smallest (fn [a b] (* a b (- a b))))
    (for [i (range (count w))
          :let [w* (into (subvec w i) (subvec w 0 i))]
          x [w* (inverse w*)]]
      x)))

(defn fundamental-group [ds]
  (let [[edge2word, gen2edge] (find-generators ds)
        orbits (for [i (indices ds), j (indices ds), :when (> j i)
                     D (orbit-reps ds [i j])
                     :let [w (trace-word ds edge2word i j D)]
                     :when (seq w)]
                 [D i j (relator-rep w) (v ds i j D)])]
    {:nr-generators (count gen2edge)
     :relators (sort (concat (for [[D i j w v] orbits :when v] (-** w v))
                             (mirror-relators ds gen2edge)))
     :cones (sort (for [[D i j w v] orbits, :when (and v (> v 1))] [w v]))
     :gen-to-edge gen2edge
     :edge-to-word edge2word
     }))

(comment
  A test symbol.
  
(def ds (dsymbol
          (str "24:2 4 6 8 10 12 14 16 18 20 22 24,"
               "16 3 5 7 9 11 13 15 24 19 21 23,"
               "10 9 18 17 14 13 20 19 22 21 24 23:"
               "8 4,3 3 3 3")))
)

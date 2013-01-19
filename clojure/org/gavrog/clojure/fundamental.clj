(ns org.gavrog.clojure.fundamental
  (:use (org.gavrog.clojure delaney free-word)))

(defn- other [a b c]
  (if (= c a) b a))

(defn- initial-todo [ds]
  (first
    (reduce (fn [[result seen] [D i E]]
              [(if (or (= :root i) (seen E)) result (conj result [D i nil]))
               (conj seen E)])
            [[] #{}]
            (traversal ds (indices ds) (elements ds)))))

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

(defn inner-edges [ds]
  "Returns the list of inner edges for a fundamental domain."
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

(defn- trace-word [ds edge2word D i]
  )

(defn- glue-generator [ds [_ opposite :as boundary] edge2word gen2edge D i]
  (let [gen (count gen2edge)
        gen2edge (conj gen2edge [gen [D i]])]
    (loop [todo (update-todo ds '() opposite D i)
           [on-bnd opposite :as boundary] (glue-boundary ds boundary D i)
           edge2word (conj edge2word [[D i] [gen]])]
      (if (empty? todo)
        [boundary edge2word gen2edge]
        (let [[D i j] (first todo)
              [_ _ n] (opposite [D i j])
              todo (rest todo)]
          (if (and (on-bnd [D i])
                   (or (nil? j)
                       (= n (* 2 (m ds i j D)))))
            (recur (update-todo ds todo opposite D i)
                   (glue-boundary ds boundary D i)
                   (conj edge2word
                         [[D i] (inverse (trace-word ds edge2word D i))]))
            (recur todo boundary edge2word)))))))

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

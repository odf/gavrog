(ns org.gavrog.clojure.delaney
  (:use (clojure [test])
        (org.gavrog.clojure [util :only [empty-queue pop-while]]))
  (:import (org.gavrog.joss.dsyms.basic DelaneySymbol)
           (java.io Writer)))

(defprotocol IDSymbol
  (element? [_ D])
  (elements [_])
  (index? [_ i])
  (indices [_])
  (s [_ i D])
  (v [_ i j D]))

(defprotocol IPersistentDSymbol
  (dsconj [_ new])
  (dsdisj [_ old])
  (glue [_ i D E])
  (unglue [_ i D]))

(extend-type DelaneySymbol
  IDSymbol
  (element? [ds D] (.hasElement ds D))
  (elements [ds] (iterator-seq (.elements ds)))
  (index? [ds i] (.hasIndex ds i))
  (indices [ds] (iterator-seq (.indices ds)))
  (s [ds i D] (when (.definesOp ds i D) (.op ds i D)))
  (v [ds i j D] (when (.definesV ds i j D) (.v ds i j D))))

(defn- ops [ds]
  (into {} (for [i (indices ds)]
             [i (into {} (for [D (elements ds)
                               :when (s ds i D)]
                           [D (s ds i D)]))])))

(defn- vs [ds]
  (into {} (for [i (indices ds)
                 :when (index? ds (inc i))]
             [i (into {} (for [D (elements ds)
                               :when (v ds i (inc i) D)]
                           [D (v ds i (inc i) D)]))])))

(deftype DSymbol [dim size s# v#]
  IDSymbol
  (element? [_ D] (and (integer? D)
                       (>= D 1)
                       (<= D size)))
  (elements [_] (range 1 (inc size)))
  (index? [_ i] (and (integer? i)
                     (>= i 0)
                     (<= i dim)))
  (indices [_] (range 0 (inc dim)))
  (s [_ i D] ((or (s# i) {}) D))
  (v [ds i j D]
     (when (and (element? ds D) (index? ds i) (index? ds j))
       (cond (= j (inc i)) ((or (v# i) {}) D)
             (= j (dec i)) ((or (v# j) {}) D)
             (= j i) 1
             (= (s ds i D) (s ds j D)) 2
             :else 1)))
  IPersistentDSymbol
  (dsconj [_ new]
          (when (and (integer? new) (not (neg? new)))
            (DSymbol. dim (max size new) s# v#)))
  (dsdisj [_ old]
          (when (= old size)
            (let [s-remove (fn [[i a]]
                             [i (reduce dissoc a (concat old (map (s# i) old)))])
                  v-remove (fn [[i a]] [i (reduce dissoc a old)])
                  new-s# (into {} (map s-remove s#))
                  new-v# (into {} (map v-remove v#))]
              (DSymbol. dim (dec size) new-s# new-v#))))
  (glue [ds i D E]
          (when (and (integer? i) (not (neg? i))
                     (integer? D) (pos? D)
                     (integer? E) (pos? E))
            (DSymbol. (max dim i)
                      (max size D E)
                      (assoc s# i (assoc (s# i) D E, E D))
                      v#)))
  (unglue [ds i D]
            (when (index? ds i)
              (DSymbol. dim
                        size
                        (assoc s# i (dissoc (s# i) D ((s# i) D)))
                        v#)))
  Object
  (equals [self other]
          (and (satisfies? IDSymbol other)
               (= (indices self) (indices other))
               (= (elements self) (elements other))
               (= (ops self) (ops other))
               (= (vs self) (vs other)))))


;; === Functions for IDSymbol instances

(defn size [ds] (count (elements ds)))

(defn dim [ds] (dec (count (indices ds))))

(defmethod print-method DSymbol [ds ^Writer w]
  (print-method (list (symbol "DSymbol.")
                      (dim ds)
                      (size ds)
                      (ops ds)
                      (vs ds))
                w))

(defn walk [ds D & idxs]
  "Returns the result of applying the D-symbol operators on ds with the
   given indices in order, starting with the element D. If the result of
   any step is undefined, nil is returned."
  (reduce #(s ds %2 %1) D idxs))

(defn r [ds i j D]
  (loop [n 1, E D]
    (when-let [F (walk ds E i j)]
      (if (= F D)
        n
        (recur (inc n) F)))))

(defn m [ds i j D]
  (let [v (v ds i j D)
        r (r ds i j D)]
    (when (and v r) (* v r))))

(defn chain-end [ds D i j]
  "Returns the result of alternately applying operators indexed i and j,
   starting with the element D, until the end of the chain is reached.
   In case of a cycle, nil is returned."
  (loop [E (walk ds D i)]
    (let [E* (walk ds E j)]
      (cond
        (nil? E*) E
        (= E E*) E
        (= D E*) nil
        :else (recur (walk ds E j i))))))

(defn pretty-traversal [ds indices seeds]
  (let [stacks (map #(vector % ()) (take 2 indices))
        queues (map #(vector % empty-queue) (drop 2 indices))
        as-root #(vector % :root)
        unseen (fn [i seen bag] (pop-while #(seen [% i]) bag))
        pop-seen #(for [[i ys] %1] (vector i (unseen i %2 ys)))
        push-neighbors #(for [[i ys] %1] (vector i (conj ys %2)))]
    ((fn collect [seeds-left todo seen]
       (let [seeds-left (drop-while (comp seen as-root) seeds-left)
             todo (pop-seen todo seen)
             [i todo-for-i] (->> todo (filter (comp seq second)) first)]
         (cond
           (seq todo-for-i)
           (let [D (first todo-for-i)
                 Di (walk ds D i)
                 todo (if Di (doall (push-neighbors todo Di)) todo)
                 seen (conj seen (as-root Di) [D i] [Di i])]
             (lazy-seq (cons [D i Di] (collect seeds-left todo seen))))
           (seq seeds-left)
           (let [D (first seeds-left)
                 todo (doall (push-neighbors todo D))
                 seen (conj seen (as-root D))]
             (lazy-seq
               (cons (as-root D) (collect (rest seeds-left) todo seen))))
           :else
           ())))
      (seq seeds) (doall (concat stacks queues)) #{})))

(defn orbit-reps
  ([ds indices seeds]
    (for [[D i] (pretty-traversal ds indices seeds) :when (= :root i)] D))
  ([ds indices]
    (orbit-reps ds indices (elements ds))))

(defn orbit [ds indices seed]
  (for [[D i] (pretty-traversal ds indices [seed])] D))

(defn orbit-loopless? [ds indices D]
  (empty? (for [[D i] (pretty-traversal ds indices [D])
                :when (and (not= i :root) (or (nil? D) (= D (walk ds D i))))]
            D)))

(defn curvature
  ([ds default-v]
    (reduce +
            (- (size ds))
            (for [[i j] [[0 1] [0 2] [1 2]]
                  :let [s #(if (orbit-loopless? ds [i j] %) 2 1)
                        v #(or (v ds i j %) default-v)]
                  D (orbit-reps ds [i j])]
              (/ (s D) (v D)))))
  ([ds]
    (curvature ds 0)))

;; === Tests

(deftest gluing
  (is (= (glue (DSymbol. 0 0 {} {}) 2 1 2)
         (DSymbol. 2 2 {2 {1 2 2 1}} {}))))

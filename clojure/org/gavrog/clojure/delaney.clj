(ns org.gavrog.clojure.delaney
  (:require (clojure [string :as s]))
  (:use (clojure test)
        (org.gavrog.clojure [util :only [empty-queue pop-while unique]]))
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
  (unglue [_ i D])
  (spin [_ i j D v])
  (unspin [_ i j D]))

(extend-type DelaneySymbol
  IDSymbol
  (element? [ds D] (.hasElement ds D))
  (elements [ds] (iterator-seq (.elements ds)))
  (index? [ds i] (.hasIndex ds i))
  (indices [ds] (iterator-seq (.indices ds)))
  (s [ds i D] (when (.definesOp ds i D) (.op ds i D)))
  (v [ds i j D] (when (.definesV ds i j D) (.v ds i j D))))


;; === Helper functions

(defn assert-arg [arg-name val test description]
  (assert (test val)
          (str "Expected " description " for " arg-name ", got " val)))

;; === Exportable functions for IDSymbol instances

(defn size [ds] (count (elements ds)))

(defn dim [ds] (dec (count (indices ds))))

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
                 Di (s ds i D)
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
  (unique (for [[D i] (pretty-traversal ds indices [seed])] D)))

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


;; === Persistent Clojure implementation of IDSymbol with some common
;;     restrictions.

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
  (dsconj [_ D]
          (do
            (assert-arg "D" D #(and (integer? %) (pos? %)) "a positive integer")
            (DSymbol. dim (max size D) s# v#)))
  (dsdisj [ds D]
          (do
            (assert-arg "D" D #(= % size) size)
            (let [s-remove (fn [[i a]] [i (reduce dissoc a [D ((s# i) D)])])
                  v-remove (fn [[i a]] [i (dissoc a D)])
                  new-s# (into {} (map s-remove s#))
                  new-v# (into {} (map v-remove v#))]
              (DSymbol. dim (dec size) new-s# new-v#))))
  (glue [ds i D E]
          (do
            (assert-arg "i" i #(and (integer? %) (not (neg? %)))
                       "a non-negative integer")
            (assert-arg "D" D #(and (integer? %) (pos? %)) "a positive integer")
            (assert-arg "E" E #(and (integer? %) (pos? %)) "a positive integer")
            (assert (= (v ds i (inc i) D) (v ds i (inc i) E)))
            (assert (= (v ds (dec i) i D) (v ds (dec i) i E)))
            (assert (= E (or (s ds i D) E)) "must unglue first")
            (assert (= D (or (s ds i E) D)) "must unglue first")
            (DSymbol. (max dim i)
                      (max size D E)
                      (assoc s# i (assoc (s# i) D E, E D))
                      v#)))
  (unglue [ds i D]
          (do
            (assert-arg "i" i #(index? ds %) "an existing index")
            (assert-arg "D" D #(element? ds %) "an existing element")
            (DSymbol. dim
                      size
                      (assoc s# i (dissoc (s# i) D ((s# i) D)))
                      v#)))
  (spin [ds i j D v]
        (do
          (assert-arg "i" i #(and (integer? %) (not (neg? %)))
                      "a non-negative integer")
          (assert-arg "j" j #(= (inc i) %) (inc i))
          (assert-arg "D" D #(and (integer? %) (pos? %)) "a positive integer")
          (assert-arg "v" v #(and (integer? %) (pos? %)) "a positive integer")
          (DSymbol. (max dim i j)
                    (max size D)
                    s#
                    (assoc v# i (reduce #(assoc %1 %2 v)
                                        (v# i)
                                        (orbit ds [i j] D))))))
  (unspin [ds i j D]
          (do
            (assert-arg "i" i #(index? ds %) "an existing index")
            (assert-arg "j" j #(= (inc i) %) (inc i))
            (assert-arg "D" D #(element? ds %) "an existing element")
            (DSymbol. dim
                      size
                      s#
                      (assoc v# i (reduce dissoc (v# i) (orbit ds [i j] D))))))
  Object
  (equals [self other]
          ;;TODO this should really test isomorphism via the canonical form
          (let [ops (fn [ds]
                      (into {} (for [i (indices ds)]
                                 [i (into {} (for [D (elements ds)
                                                   :when (s ds i D)]
                                               [D (s ds i D)]))])))
                vs (fn [ds]
                     (into {} (for [i (indices ds)
                                    :when (index? ds (inc i))]
                                [i (into {} (for [D (elements ds)
                                                  :when (v ds i (inc i) D)]
                                              [D (v ds i (inc i) D)]))])))]
            (and (satisfies? IDSymbol other)
                 (= (indices self) (indices other))
                 (= (elements self) (elements other))
                 (= (ops self) (ops other))
                 (= (vs self) (vs other))))))

(defmethod print-method DSymbol [ds ^Writer w]
  (let [images (fn [i] (map #(or (s ds i %) 0) (orbit-reps ds [i])))
        m-vals (fn [i] (map #(or (m ds i (inc i) %) 0)
                            (orbit-reps ds [i (inc i)])))
        ops-str (s/join "," (map #(s/join " " (images %)) (indices ds)))
        ms-str (s/join "," (map #(s/join " " (m-vals %)) (range (.dim ds))))
        [size dim] [(.size ds) (.dim ds)]
        dims-str (s/join " " (if (= 2 dim) [size] [size dim]))
        code (str "<1.1:" dims-str ":" ops-str ":" ms-str ">")]
    (if *print-readably*
      (print-simple (str "(dsymbol \"" code "\")") w)
      (print-simple code w))))


;; === Factories for DSymbol instances

(defn- parse-numbers [str]
  (when (and str (< 0 (count (s/trim str))))
    (map #(Integer/parseInt %) (-> str s/trim (s/split #"\s+")))))

(defn- parse-number-lists [str]
  (when str
    (map parse-numbers (-> str s/trim (s/split #",")))))

(defn- pairs [data free]
  (when (seq free)
    (let [pair [(first free) (first data)]
          rest-free (filter (comp not (set pair)) free)]
      (lazy-seq (cons pair (pairs (rest data) rest-free))))))

(defn- with-gluings [ds gluings]
  (reduce (fn [ds i]
            (reduce (fn [ds [D E]] (glue ds i D E))
                    ds
                    (filter (fn [[D E]] (element? ds E))
                            (pairs (nth gluings i) (elements ds)))))
          ds
          (range (count gluings))))

(defn- with-m-vals [ds spins]
  (reduce (fn [ds i]
            (let [j (inc i)]
              (reduce (fn [ds [D m]] (spin ds i j D (/ m (r ds i j D))))
                      ds
                      (filter (fn [[D m]] (and (r ds i j D) (> m 0)))
                              (zipmap (orbit-reps ds [i j]) (nth spins i))))))
          ds
          (range (count spins))))

(defn- ds-from-str [code]
  (let [parts (-> code s/trim
                (s/replace #"^<" "") (s/replace #">$" "") (s/split #":"))
        [dims gluings spins] (vec (if (re-matches #"\d+\.\d+" (first parts))
                                    (rest parts)
                                    parts))
        [size dim] (parse-numbers dims)
        d-set (with-gluings (DSymbol. (or dim 2) size {} {})
                (parse-number-lists gluings))
        d-sym (with-m-vals d-set
                (parse-number-lists spins))]
    d-sym))

(defn- ds-from-ds [ds]
  (let [emap (zipmap (elements ds) (range 1 (inc (size ds))))
        imap (zipmap (indices ds) (range (inc (dim ds))))
        gluings (for [i (indices ds)
                      D (orbit-reps ds [i])
                      :when (s ds i D)]
                    [(imap i) (emap D) (emap (s ds i D))])
        spins (for [[i j] (zipmap (indices ds) (rest (indices ds)))
                    D (orbit-reps ds [i j])
                    :when (v ds i j D)]
                  [(imap i) (emap D) (v ds i j D)])
        d-set (reduce (fn [ds [i D E]] (glue ds i D E))
                      (DSymbol. (dim ds) (size ds) {} {})
                      gluings)
        d-sym (reduce (fn [ds [i D v]] (spin ds i (inc i) D v))
                      d-set
                      spins)]
    d-sym))

(defprotocol DSymbolSource
  (dsymbol [_]))

(extend-type String
  DSymbolSource
  (dsymbol [code] (ds-from-str code)))

(extend-type DSymbol
  DSymbolSource
  (dsymbol [ds] ds))

(extend-type DelaneySymbol
  DSymbolSource
  (dsymbol [ds] (ds-from-ds ds)))


;; === Building a flat Java DSymbol instance

(defn java-dsymbol [ds]
  (let [ds (dsymbol ds)
        ops (make-array Integer/TYPE (inc (dim ds)) (inc (size ds)))
        vs (make-array Integer/TYPE (dim ds) (inc (size ds)))]
    (doseq [i (range 0 (inc (dim ds)))
            D (range 1 (inc (size ds)))]
      (if-let [E (s ds i D)] (aset-int ops i D (s ds i D)))
      (if (< i (dim ds))
        (if-let [b (v ds i (inc i) D)] (aset-int vs i D b))))
    (org.gavrog.joss.dsyms.basic.DSymbol. ops vs)))


;; === Wrapped Java methods

(defn canonical [ds]
  (-> ds java-dsymbol .canonical dsymbol))

(defn canonical? [ds]
  (every? (fn [[a b]] (= a b))
          (into {} (-> ds java-dsymbol .getMapToCanonical))))


;; === Tests

(deftest adding-elements
  (is (= (dsconj (DSymbol. 0 0 {} {})
                 2)
         (DSymbol. 0 2 {} {}))))

(deftest removing-elements
  (is (= (dsdisj (DSymbol. 2 2
                           {0 {1 1, 2 2} 1 {1 2, 2 1} 2 {1 2, 2 1}}
                           {0 {1 4, 2 4} 1 {1 4, 2 4}})
                 2)
         (DSymbol. 2 1 {0 {1 1} 1 {} 2 {}} {0 {1 4} 1 {1 4}}))))

(deftest gluing
  (is (= (glue (DSymbol. 0 0 {} {})
               2 1 2)
         (DSymbol. 2 2 {2 {1 2 2 1}} {}))))

(deftest spinning
  (is (= (spin (DSymbol. 2 2
                         {0 {1 1, 2 2} 1 {1 2, 2 1} 2 {1 2, 2 1}}
                         {})
               0 1 1 3)
         (DSymbol. 2 2
                   {0 {1 1, 2 2} 1 {1 2, 2 1} 2 {1 2, 2 1}}
                   {0 {1 3, 2 3}})))
  (is (= (unspin (DSymbol. 2 2
                           {0 {1 1, 2 2} 1 {1 2, 2 1} 2 {1 2, 2 1}}
                           {0 {1 4, 2 4} 1 {1 4, 2 4}})
                 0 1 2)
         (DSymbol. 2 2
                   {0 {1 1, 2 2} 1 {1 2, 2 1} 2 {1 2, 2 1}}
                   {1 {1 4, 2 4}}))))

(deftest input-output
  (is (= (dsymbol "<1.1:2:2,1 2,2:4,6>")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 1 {1 1, 2 2} 2 {1 2, 2 1}}
                   {0 {1 2, 2 2} 1 {1 3, 2 3}})))
  (is (= (dsymbol "1.1:2:2,1 2,2:4,6")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 1 {1 1, 2 2} 2 {1 2, 2 1}}
                   {0 {1 2, 2 2} 1 {1 3, 2 3}})))
  (is (= (dsymbol "2:2,1 2,2:4,6")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 1 {1 1, 2 2} 2 {1 2, 2 1}}
                   {0 {1 2, 2 2} 1 {1 3, 2 3}})))
  (is (= (dsymbol "2:2,,2:4,6")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 2 {1 2, 2 1}}
                   {})))
  (is (thrown-with-msg? AssertionError #"Expected a positive integer for v"
                        (= (dsymbol "<1.1:2:2,1 2,2:4,5>"))))
  (is (= (dsymbol "<1.1:2:2,1 2,2:0,6>")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 1 {1 1, 2 2} 2 {1 2, 2 1}}
                   {1 {1 3, 2 3}})))
  (is (= (dsymbol "<1.1:2:2,1 2,2:4,>")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 1 {1 1, 2 2} 2 {1 2, 2 1}}
                   {0 {1 2, 2 2}})))
  (is (= (dsymbol "<1.1:2:2,1 2,2:>")
         (DSymbol. 2 2
                   {0 {1 2, 2 1} 1 {1 1, 2 2} 2 {1 2, 2 1}}
                   {})))
  (is (= (dsymbol "<1.1:2:2:>")
         (DSymbol. 2 2
                   {0 {1 2, 2 1}}
                   {})))
  (is (= (dsymbol "<1.1:2>")
         (DSymbol. 2 2
                   {}
                   {})))
  )

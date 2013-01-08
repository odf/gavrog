(ns org.gavrog.clojure.delaney
  (:require (clojure [string :as s]))
  (:use (clojure test)
        (org.gavrog.clojure partition)
        (org.gavrog.clojure util))
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
  (s [ds i D] (if (.definesOp ds i D) (.op ds i D)))
  (v [ds i j D] (if (.definesV ds i j D) (.v ds i j D))))


;; === Helper functions

(defn assert-arg [arg-name val test description]
  (assert (test val)
          (str "Expected " description " for " arg-name ", got " val)))

(defn ops [ds]
  (into {} (for [i (indices ds)]
             [i (into {} (for [D (elements ds) :when (s ds i D)]
                           [D (s ds i D)]))])))

(defn vs [ds]
  (into {} (for [i (indices ds) :when (index? ds (inc i))]
             [i (into {} (for [D (elements ds) :when (v ds i (inc i) D)]
                           [D (v ds i (inc i) D)]))])))

;; === Exportable functions for IDSymbol instances

(defn size [ds] (count (elements ds)))

(defn dim [ds] (dec (count (indices ds))))

(defn complete? [ds]
  (and (every? (comp not nil?)
               (for [i (indices ds), D (elements ds)]
                 (s ds i D)))
       (every? (comp not nil?)
               (for [i (indices ds), j (indices ds), D (elements ds)]
                 (v ds i j D)))))

(defn traversal [ds indices seeds]
  (let [stacks (map #(vector % ()) (take 2 indices))
        queues (map #(vector % empty-queue) (drop 2 indices))
        as-root #(vector % :root %)
        unseen (fn [i seen bag] (pop-while #(seen [% i]) bag))
        pop-seen #(for [[i ys] %1] (vector i (unseen i %2 ys)))
        push-neighbors #(for [[i ys] %1] (vector i (conj ys %2)))]
    ((fn collect [seeds-left todo seen]
       (let [seeds-left (drop-while (comp seen as-root) seeds-left)
             todo (pop-seen todo seen)
             [i todo-for-i] (first (filter (comp seq second) todo))]
         (cond
           (seq todo-for-i)
           (let [D (first todo-for-i)
                 Di (s ds i D)
                 head [D i Di]
                 todo (if Di (vec (push-neighbors todo Di)) todo)
                 seen (conj seen (as-root Di) [D i] [Di i])]
             (lazy-seq (cons head (collect seeds-left todo seen))))
           (seq seeds-left)
           (let [D (first seeds-left)
                 head (as-root D)
                 seeds-left (rest seeds-left)
                 todo (vec (push-neighbors todo D))
                 seen (conj seen (as-root D))]
             (lazy-seq (cons head (collect seeds-left todo seen))))
           :else
           ())))
      (seq seeds) (vec (concat stacks queues)) #{})))

(defn trav-alt [ds idcs seeds]
  (let [[i1 i2] (take 2 idcs)
        high-idcs (drop 2 idcs)
        plus-all (fn [pairs D] (into pairs (for [i high-idcs] [i D])))]
    (letfn [(step [o2n n2o n stack free seeds]
                  (cond
                    (seq stack)
                    (let [[i D] (first stack), oDi (s ds i (n2o D))]
                      (if (and (not (nil? oDi)) (not= (or (o2n oDi) D) D))
                        (recur o2n n2o n (pop stack) free seeds)
                        (continue (first stack) o2n n2o n (pop stack) free
                                  seeds)))
                    (seq free)
                    (continue (first free) o2n n2o n stack free seeds)
                    (seq seeds)
                    (let [D (first seeds)]
                      (if (o2n D)
                        (recur o2n n2o n stack free (rest seeds))
                        (let [head [D :root D]
                              o2n (assoc o2n D n)
                              n2o (assoc n2o n D)
                              stack (conj stack [i2 n] [i1 n])
                              free (plus-all free n)
                              seeds (rest seeds)
                              n (inc n)]
                          (lazy-seq
                            (cons head (step o2n n2o n stack free seeds))))))))
            (continue [[i D] o2n n2o n stack free seeds]
                      (if-let [oDi (s ds i (n2o D))]
                        (let [Di (or (o2n oDi) n)
                              stack (if (< Di n)
                                      stack (conj stack [i2 Di] [i1 Di]))
                              free (if (< Di n)
                                     free (plus-all free Di))
                              o2n (assoc o2n oDi Di)
                              n2o (assoc n2o Di oDi)
                              n (max n (inc Di))
                              free (disj free [i D] [i Di])]
                          (lazy-seq (cons [(n2o D) i (n2o Di)]
                                          (step o2n n2o n stack free seeds))))
                        (lazy-seq (cons [(n2o D) i nil]
                                        (step o2n n2o n stack free seeds)))))]
           (step {} {} 1 '() (sorted-set) (seq seeds)))))

(defn orbit-reps
  ([ds indices seeds]
    (for [[D i] (traversal ds indices seeds) :when (= :root i)] D))
  ([ds indices]
    (orbit-reps ds indices (elements ds))))

(defn orbit-elements [ds indices seed]
  (distinct (for [[_ i D] (traversal ds indices [seed])] D)))

(defn connected? [ds]
  (> 2 (count (orbit-reps ds (indices ds)))))

(defn partial-orientation [ds]
  (loop [ori {}
         [[Di i D] & xs] (traversal ds (indices ds) (elements ds))]
    (cond (nil? i) ori
          (or (nil? D) (ori D)) (recur ori xs)
          :else (recur (assoc ori D (if (= :root i) 1 (- (ori Di)))) xs))))

(defn loopless? [ds]
  (every? (fn [[i D]] (not= D (s ds i D)))
          (for [i (indices ds), D (elements ds)] [i D])))

(defn oriented? [ds]
  (let [ori (partial-orientation ds)]
    (every? (fn [[i D]] (not= (ori D) (ori (s ds i D))))
            (for [i (indices ds), D (elements ds)] [i D]))))

(defn weakly-oriented? [ds]
  (let [ori (partial-orientation ds)]
    (every? (fn [[i D]]
              (let [Di (s ds i D)] (or (= D Di) (not= (ori D) (ori Di)))))
            (for [i (indices ds), D (elements ds)] [i D]))))

(defn- protocol [ds indices trav]
  (let [imap (zipmap indices (range (count indices)))
        ipairs (map vector indices (rest indices))
        spins (fn [D] (for [[i j] ipairs] (or (v ds i j D) 0)))
        step (fn step [xs emap n]
               (if-let [[Di i D] (first xs)]
                 (if (nil? D)
                   (recur (rest xs) emap n)
                   (let [[Ei E] (sort [(emap Di) (or (emap D) n)])
                         head (if (= i :root) [-1 E] [(imap i) Ei E])
                         xs (rest xs)]
                     (if (not= E n)
                       (lazy-seq (concat head (step xs emap n)))
                       (let [head (vec (concat head (spins D)))
                             emap (assoc emap D n)
                             n (inc n)]
                         (lazy-seq (concat head (step xs emap n)))))))))]
    (step trav {} 1)))

(defn invariant
  ([ds D]
    (let [idcs (indices ds)]
      (protocol ds idcs (traversal ds idcs [D]))))
  ([ds]
    (when (pos? (size ds))
      (assert (connected? ds) "Symbol must be connected")
      (apply lexicographically-smallest
             (for [D (elements ds)] (invariant ds D))))))

(defn walk [ds D & idxs]
  "Returns the result of applying the D-symbol operators on ds with the
   given indices in order, starting with the element D. If the result of
   any step is undefined, nil is returned."
  (reduce #(s ds %2 %1) D idxs))

(defn r [ds i j D]
  (loop [n 1, E D]
    (if-let [F (walk ds E i j)]
      (if (= F D)
        n
        (recur (inc n) F)))))

(defn m [ds i j D]
  (let [v (v ds i j D)
        r (r ds i j D)]
    (if (and v r) (* v r))))

(defn chain-end [ds D i j]
  "Returns the result of alternately applying operators indexed i and j,
   starting with the element D, until the end of the chain is reached.
   In case of a cycle, nil is returned."
  (loop [E (s ds i D)]
    (let [E* (s ds j E)]
      (cond
        (nil? E*) E
        (= E E*) E
        (= D E*) nil
        :else (recur (walk ds E j i))))))

(defn orbit-loopless? [ds indices D]
  (empty? (for [[D i] (traversal ds indices [D])
                :when (and (not= i :root) (or (nil? D) (= D (s ds i D))))]
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

(defn- type-map [ds]
  (reduce (fn [m [D x]] (assoc m D (conj (or (m D) []) x)))
          {}
          (for [[i j] (map vector (indices ds) (rest (indices ds)))
                D (orbit-reps ds [i j])
                :let [x (m ds i j D)]
                E (orbit-elements ds [i j] D)]
            [E x])))

(defn- type-partition [ds]
  (do
    (assert (connected? ds) "Symbol must be connected")
    (let [D0 (first (elements ds)) 
          tm (type-map ds)
          spread (fn [D E] (for [i (indices ds)] [(s ds i D), (s ds i E)]))
          unite (fn [p A B]
                  (loop [p p, q (conj empty-queue [A B])]
                    (if-let [[D E] (first q)]
                      (cond
                        (= (pfind p D) (pfind p E))
                        (recur p (pop q))
                        (= (tm D) (tm E))
                        (recur (punion p D E)
                               (apply conj (pop q) (spread D E))))
                      p)))]
      (reduce (fn [p D] (or (unite p D0 D) p)) pempty (rest (elements ds))))))

(defn morphism [src img src-base img-base]
  (do
    (assert (connected? src) "Source symbol must be connected")
    (assert (= (indices src) (indices img)) "Index lists must be equal")
    (let [idcs (indices src)
          t-src (type-map src)
          t-img (type-map img)
          match (fn [mapping [D E]]
                  (or (= E (mapping D))
                      (and (nil? (mapping D)) (= (t-src D) (t-img E)))))]
      (loop [mapping {src-base img-base}
             q (conj empty-queue [src-base img-base])]
        (if (empty? q)
          mapping
          (let [[D E] (first q)
                pairs (filter (partial not= [nil nil])
                              (for [i idcs] [(s src i D) (s img i E)]))]
            (if (every? (partial match mapping) pairs)
              (recur (into mapping pairs)
                     (into (rest q) (filter (comp nil? mapping first) pairs)))
              nil)))))))

(defn automorphisms [ds]
  (if-let [D (first (elements ds))]
    (keep (partial morphism ds ds D) (elements ds))))

(defn automorphism-orbits [ds]
  (or (seq (into pempty (apply concat (automorphisms ds)))) (elements ds)))


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
     (if (and (element? ds D) (index? ds i) (index? ds j))
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
                                        (orbit-elements ds [i j] D))))))
  (unspin [ds i j D]
          (do
            (assert-arg "i" i #(index? ds %) "an existing index")
            (assert-arg "j" j #(= (inc i) %) (inc i))
            (assert-arg "D" D #(element? ds %) "an existing element")
            (DSymbol. dim
                      size
                      s#
                      (assoc v# i (reduce dissoc (v# i)
                                          (orbit-elements ds [i j] D))))))
  Object
  (equals [self other]
          (and (satisfies? IDSymbol other)
               (= (indices self) (indices other))
               (= (elements self) (elements other))
               (= (ops self) (ops other))
               (= (vs self) (vs other))))
  (hashCode [self]
            (.hashCode (list dim size (ops self) (vs self)))))

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


;; === An IDSymbol implementation representing a restricted view into a base
;;     symbol.

(defprotocol IRestriction
  (base [_]))

(deftype Restriction [base idx-set elm-set]
  IRestriction
  (base [_] base)
  IDSymbol
  (element? [_ D] (elm-set D))
  (elements [_] (seq elm-set))
  (index? [_ i] (idx-set i))
  (indices [_] (seq idx-set))
  (s [self i D]
     (if (and (index? self i) (element? self D))
       (s base i D)))
  (v [self i j D]
     (if (and (index? self i) (index? self j) (element? self D))
       (v base i j D)))
  Object
  (equals [self other]
          (and (satisfies? IDSymbol other)
               (= (indices self) (indices other))
               (= (elements self) (elements other))
               (= (ops self) (ops other))
               (= (vs self) (vs other))))
  (hashCode [self]
            (.hashCode (list dim size (ops self) (vs self)))))

(defmethod print-method Restriction [ds ^Writer w]
  (print-simple
    (str "(Restriction. "
         (prn-str (base ds)) " "
         (into #{} (indices ds)) " "
         (into #{} (elements ds)) ")")
    w))

(defn restriction [ds idcs elms]
  (Restriction. ds (into #{} idcs) (into #{} elms)))

(defn orbit [ds idcs seed]
  (restriction ds idcs (orbit-elements ds idcs seed)))

;; === Factories for DSymbol instances

(defn- parse-numbers [str]
  (if (and str (< 0 (count (s/trim str))))
    (map #(Integer/parseInt %) (s/split (s/trim str) #"\s+"))))

(defn- parse-number-lists [str]
  (if str
    (map parse-numbers (s/split (s/trim str) #","))))

(defn- pairs [data free]
  (if (seq free)
    (let [pair [(first free) (first data)]
          rest-free (remove (set pair) free)]
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
        gluings-for (fn [i] (into {} (for [D (elements ds) :when (s ds i D)]
                                       [(emap D) (emap (s ds i D))])))
        gluings (into {} (for [i (indices ds)]
                           [(imap i) (gluings-for i)]))
        spins-for (fn [i j] (into {} (for [D (elements ds) :when (v ds i j D)]
                                       [(emap D) (v ds i j D)])))
        spins (into {} (for [[i j] (zipmap (indices ds) (rest (indices ds)))]
                         [(imap i) (spins-for i j)]))]
    (DSymbol. (dim ds) (size ds) gluings spins)))

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

(extend-type Restriction
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

;; === Functions that build and compare with specific DSymbol instances

(defn dual [ds]
  (let [ds (dsymbol ds)]
    (DSymbol. (dim ds)
              (size ds)
              (into {} (for [[i s] (.s# ds)] [(- (dim ds) i) s]))
              (into {} (for [[i v] (.v# ds)] [(- (dim ds) 1 i) v])))))

(defn- from-protocol [dim# size# prot]
  (let [update (fn [m data D]
                 (reduce (fn [m [i v]] (if (= 0 v) m (assoc-in m [i D] v)))
                         m
                         (map-indexed list data)))]
    (loop [todo prot, ops {}, vs {}, n 0]
      (cond (empty? todo)
            (DSymbol. dim# size# ops vs)
            (neg? (first todo))
            (let [D (second todo)
                  [data todo] (split-at dim# (drop 2 todo))]
              (recur todo ops (update vs data D) (max n D)))
            :else
            (let [[i E D] (take 3 todo)
                  [data todo] (split-at (if (> D n) dim# 0) (drop 3 todo))]
              (recur todo
                     (-> ops (assoc-in [i E] D) (assoc-in [i D] E))
                     (update vs data D) (max n D)))))))

(defn canonical
  ([ds]
    (let [ds (dsymbol ds)]
      (from-protocol (dim ds) (size ds) (invariant ds))))
  ([ds D]
    (let [ds (dsymbol ds)]
      (from-protocol (dim ds) (size ds) (invariant ds D)))))

(defn inequivalent-forms [ds]
  (for [orb (automorphism-orbits ds)] (canonical ds (first orb))))

(defn canonical?
  ([ds]
    (= (dsymbol ds) (canonical ds)))
  ([ds D]
    (= (dsymbol ds) (canonical ds D))))

(defn isomorphic? [ds1 ds2]
  (= (invariant ds1) (invariant ds2)))

(defn self-dual? [ds]
  (isomorphic? ds (dual ds)))

(defn minimal? [ds]
  (let [p (type-partition ds)]
    (every? (fn [D] (= D (pfind p D))) (elements ds))))

(defn minimal [ds]
  (let [p (type-partition ds)
        reps (filter (fn [D] (= D (pfind p D))) (elements ds))]
    (if (= (size ds) (count reps))
      ds
      (let [emap (zipmap reps (range 1 (inc (count reps))))
            idcs (indices ds)
            imap (zipmap idcs (range (inc (dim ds))))
            pairs (fn [i] (for [D reps] [(emap D) (emap (pfind p (s ds i D)))]))
            ops (into {} (for [i idcs] [(imap i), (into {} (pairs i))]))
            t (DSymbol. (dim ds) (count reps) ops {})
            spin (fn [i j D] (/ (m ds i j D) (r t (imap i) (imap j) (emap D))))
            spins (fn [i j] (for [D reps] [(emap D) (spin i j D)]))
            ipairs (zipmap idcs (rest idcs))
            vs (into {} (for [[i j] ipairs] [(imap i) (into {} (spins i j))]))]
        (DSymbol. (dim ds) (count reps) ops vs)))))

(defn append
  ([] (dsymbol "0"))
  ([ds] ds)
  ([ds1 ds2]
    (let [ds1 (dsymbol ds1)
          ds2 (dsymbol ds2)
          d (max (dim ds1) (dim ds2))
          s1 (size ds1)
          s2 (size ds2)
          ops (into {} (for [i (range (inc d))]
                         [i (into {} (concat (for [D (elements ds1)]
                                               [D (s ds1 i D)])
                                             (for [D (elements ds2)]
                                               [(+ s1 D)
                                                (if-let [E (s ds2 i D)]
                                                  (+ s1 E))])))]))
          vs (into {} (for [i (range d)]
                        [i (into {} (concat (for [D (elements ds1)]
                                              [D (v ds1 i (inc i) D)])
                                            (for [D (elements ds2)]
                                              [(+ s1 D)
                                               (v ds2 i (inc i) D)])))]))]
      (DSymbol. d (+ s1 s2) ops vs)))
  ([ds1 ds2 & xds]
    (reduce append (append ds1 ds2) xds)))

;; === Tests

(deftest delaney-test
  (testing "adding-elements"
           (is (= (dsconj (DSymbol. 0 0 {} {})
                          2)
                  (DSymbol. 0 2 {} {}))))

  (testing "removing-elements"
           (is (= (dsdisj (DSymbol. 2 2
                                    {0 {1 1, 2 2} 1 {1 2, 2 1} 2 {1 2, 2 1}}
                                    {0 {1 4, 2 4} 1 {1 4, 2 4}})
                          2)
                  (DSymbol. 2 1 {0 {1 1} 1 {} 2 {}} {0 {1 4} 1 {1 4}}))))

  (testing "gluing"
           (is (= (glue (DSymbol. 0 0 {} {})
                        2 1 2)
                  (DSymbol. 2 2 {2 {1 2 2 1}} {}))))

  (testing "spinning"
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

  (testing "input-output"
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
           (is (thrown-with-msg?
                 AssertionError #"Expected a positive integer for v"
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
                            {}))))

  (testing "invariants"
           (are [x] (let [ds (dsymbol x)]
                      (= (invariant ds) (.invariant (java-dsymbol ds))))
                "2:2,1 2,2:6,4"
                "2:2,1 2,2"
                "2:2,1 2"
                "3:2 3,1 3,2 3:3,6"
                "9:1 3 4 6 8 9,2 3 4 9 7 8,4 5 6 7 8 9:3 4 5,5 3 4"
                (str "12:1 3 4 6 8 10 12,2 3 5 7 8 9 11 12,4 9 10 5 6 12 11:"
                     "3 5 4,5 4 3")
                (str "6 3:1 2 3 4 6,1 2 3 5 6,2 4 5 6,1 3 4 5 6:"
                     "4 3 3 3,4 3 3,4 4 4"))
           (is (thrown-with-msg?
                 AssertionError #"Symbol must be connected"
                 (invariant (dsymbol "2")))))

  (testing "canonical-forms"
           (are [x] (let [ds (dsymbol x)]
                      (= (canonical ds)
                         (dsymbol (.canonical (java-dsymbol ds)))))
                "2:2,1 2,2:6,4"
                "2:2,1 2,2"
                "2:2,1 2"
                "3:2 3,1 3,2 3:3,6"
                "9:1 3 4 6 8 9,2 3 4 9 7 8,4 5 6 7 8 9:3 4 5,5 3 4"
                (str "12:1 3 4 6 8 10 12,2 3 5 7 8 9 11 12,4 9 10 5 6 12 11:"
                     "3 5 4,5 4 3")
                (str "6 3:1 2 3 4 6,1 2 3 5 6,2 4 5 6,1 3 4 5 6:"
                     "4 3 3 3,4 3 3,4 4 4"))
           (is (thrown-with-msg?
                 AssertionError #"Symbol must be connected"
                 (canonical (dsymbol "2"))))))

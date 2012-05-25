(ns org.gavrog.clojure.azulenoids
  (:import (org.gavrog.jane.numbers Whole)
           (org.gavrog.joss.dsyms.basic DSymbol DynamicDSymbol IndexList)
           (org.gavrog.joss.dsyms.generators CombineTiles DefineBranching2d))
  (:gen-class))

;; General purpose definitions and entities

(def empty-queue clojure.lang.PersistentQueue/EMPTY)

(defn pop-while [pred coll]
  "Like drop-while, but with pop."
  (cond
    (empty? coll) coll
    (pred (first coll)) (recur pred (pop coll))
    :else coll))

(defn iterate-cycle [coll x]
  "Returns a lazy sequence of intermediate results, starting at x, of
   cycling through the functions in coll and applying each to the
   previous result."
  (reductions #(%2 %1) x (cycle coll)))

(defn unique
  "Returns a lazy sequence of values from coll with duplicates removed.
   If key-fun is given, it is applied to the original values before
   determining equality."
  ([key-fun coll]
    (letfn [(step [[seen _] x]
                  (let [key (key-fun x)]
                    (if (seen key)
                      [seen false]
                      [(conj seen key) x])))]
           (filter identity (map second (reductions step [#{} false] coll)))))
  ([coll]
    (unique identity coll)))

;; General D-symbol functions

(defn walk [ds D & idxs]
  "Returns the result of applying the D-symbol operators on ds with the
   given indices in order, starting with the element D."
  (reduce #(.op ds %2 %1) D idxs))

(defn chain-end [ds D i j]
  (loop [E (walk ds D i)]
    (let [E* (walk ds E j)]
      (cond
        (nil? E*) E
        (= E E*) E
        (= D E*) nil
        :else (recur (walk ds E j i))))))

(defn boundary-chambers [ds D i j k]
  (iterate-cycle [#(walk ds %1 i) #(chain-end ds %1 j k)] D))

(defn traversal [ds indices seeds]
  (let [op (fn [i D] (when D (.op ds i D)))
        stacks (map #(vector % ()) (take 2 indices))
        queues (map #(vector % empty-queue) (drop 2 indices))
        as-root #(vector % :root)
        unseen (fn [i seen bag] (pop-while #(seen [% i]) bag))
        pop-seen #(for [[i ys] %1] (vector i (unseen i %2 ys)))
        push-neighbors #(for [[i ys] %1] (vector i (conj ys (op i %2))))]
    ((fn collect [seeds-left todo seen]
       (let [seeds-left (drop-while (comp seen as-root) seeds-left)
             todo (pop-seen todo seen)
             [i todo-for-i] (->> todo (filter (comp seq second)) first)]
         (cond
           (seq todo-for-i)
           (let [D (first todo-for-i)
                 todo (doall (push-neighbors todo D))
                 seen (conj seen (as-root D) [D i] [(op i D) i])]
             (lazy-seq (conj (collect seeds-left todo seen) [D i])))
           (seq seeds-left)
           (let [D (first seeds-left)
                 todo (doall (push-neighbors todo D))
                 seen (conj seen (as-root D))]
             (lazy-seq
               (conj (collect (rest seeds-left) todo seen) (as-root D))))
           :else
           ())))
      (seq seeds) (doall (concat stacks queues)) #{})))

(defn orbit-reps
  ([ds indices seeds]
    (for [[D i] (traversal ds indices seeds) :when (= :root i)] D))
  ([ds indices]
    (orbit-reps ds indices (iterator-seq (.elements ds)))))

(defn orbit-loopless? [ds indices D]
  (empty? (for [[D i] (traversal ds indices [D])
                :when (and (not= i :root) (or (nil? D) (= D (walk ds D i))))]
            D)))

(defn curvature
  ([ds default-v]
    (reduce +
            (- (.size ds))
            (for [[i j] [[0 1] [0 2] [1 2]]
                  :let [s #(if (orbit-loopless? ds [i j] %) 2 1)
                        v #(if (.definesV ds i j %) (.v ds i j %) default-v)]
                  D (orbit-reps ds [i j])]
              (/ (s D) (v D)))))
  ([ds]
    (curvature ds 0)))

;; Azulenoid-specific functions

(def template
  (DSymbol. (str "1.1:60:"
                 "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 "
                 "32 34 36 38 40 42 44 46 48 50 52 54 56 58 60,"
                 "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 "
                 "33 35 42 39 41 48 45 47 54 51 53 60 57 59,"
                 "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 "
                 "29 0 0 0 0 42 41 0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
                 "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0")))

(def octagon
  (DSymbol. "1.1:16 1:2 4 6 8 10 12 14 16,16 3 5 7 9 11 13 15:8"))

(def boundary-mappings
  (let [template-boundary (boundary-chambers template (Integer. 1) 0 1 2)
        octagon-boundary (cycle (map #(Integer. %1) (range 1 17)))]
    (for [p (range 1 16 2)]
      (zipmap (drop (mod (- 19 p) 16) octagon-boundary)
              (take 16 template-boundary)))))

(defn on-template [ds oct2tmp]
  (let [result (DynamicDSymbol. template)]
    (doseq [[D D*] oct2tmp :when (not (.definesOp result 2 D*))]
      (.redefineOp result 2 D* (oct2tmp (.op ds 2 D))))
    (doseq [[D D*] oct2tmp :when (not (.definesV result 1 2 D*))]
      (.redefineV result 1 2 D* (.v ds 1 2 D)))
    result))

(def octa-sets (filter #(-> % (curvature 1) (>= 0))
                       (lazy-seq (CombineTiles. octagon))))

(def octa-syms
  (for [dset octa-sets
        dsym (lazy-seq (DefineBranching2d. dset 3 2 Whole/ZERO))
        :when (-> dsym curvature (= 0))]
    dsym))

(def azul-syms
  (let [raw (for [ds octa-syms o2t boundary-mappings] (on-template ds o2t))]
    (for [ds (unique #(-> %1 .minimal .invariant) raw)]
      (-> ds .dual .minimal .canonical))))

;; Main entry point when used as a script

(defn -main []
  (do
    (doseq [ds azul-syms] (println (str ds)))
    (println "#Generated:")
    (println "#   " (count octa-sets) "octagonal D-sets.")
    (println "#   " (count octa-syms) "octagonal D-symbols.")
    (println "#   " (count azul-syms) "azulenoid D-symbols.")))

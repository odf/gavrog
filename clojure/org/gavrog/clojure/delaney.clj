(ns org.gavrog.clojure.delaney
  (:use (org.gavrog.clojure [util :only [empty-queue pop-while]]))
  (:import (org.gavrog.joss.dsyms.basic DelaneySymbol)))

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
  (dsglue [_ i D E])
  (dsunglue [_ i D]))

(extend-type DelaneySymbol
  IDSymbol
  (element? [ds D] (.hasElement ds D))
  (elements [ds] (iterator-seq (.elements ds)))
  (index? [ds i] (.hasIndex ds i))
  (indices [ds] (iterator-seq (.indices ds)))
  (s [ds i D] (when (.definesOp ds i D) (.op ds i D)))
  (v [ds i j D] (when (.definesV ds i j D) (.v ds i j D))))

(deftype DSymbol [idcs elms s# v#]
  IDSymbol
  (element? [_ D] (elms D))
  (elements [_] (seq elms))
  (index? [_ i] (idcs i))
  (indices [_] (seq idcs))
  (s [_ i D] ((or (s# i) {}) D))
  (v [ds i j D]
     (when (and (element? ds D) (index? ds i) (index? ds j))
       (cond (= j (inc i)) ((or (v# i) {}) D)
             (= j (dec i)) ((or (v# j) {}) D)
             (= j i) 1
             (= (s ds i D) (s ds j D)) 2
             :else 1)))
  IPersistentDSymbol
  (dsconj [_ new] (DSymbol. idcs (into elms new) s# v#))
  (dsdisj [_ old]
          (let [s-remove (fn [[i a]]
                           [i (reduce dissoc a (concat old (map (s# i) old)))])
                v-remove (fn [[i a]] [i (reduce dissoc a old)])
                new-elms (persistent! (reduce disj! (transient elms) old))
                new-s# (into {} (map s-remove s#))
                new-v# (into {} (map v-remove v#))]
            (DSymbol. idcs new-elms new-s# new-v#)))
  (dsglue [ds i D E]
          (DSymbol. (conj idcs i)
                    (conj elms D E)
                    (assoc s# i (assoc (s# i) D E, E D))
                    v#))
  (dsunglue [ds i D]
            (DSymbol. idcs
                      elms
                      (assoc s# i (dissoc (s# i) D ((s# i) D)))
                      v#)))


;; General D-symbol functions

(defn size [ds] (count (elements ds)))

(defn dim [ds] (dec (count (indices ds))))

(defn walk [ds D & idxs]
  "Returns the result of applying the D-symbol operators on ds with the
   given indices in order, starting with the element D. If the result of
   any step is undefined, nil is returned."
  (reduce #(s ds %2 %1) D idxs))

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

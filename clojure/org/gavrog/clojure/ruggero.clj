(ns org.gavrog.clojure.ruggero
  (:use (org.gavrog.clojure
          [delaney]
          [delaney2d]
          [generators :only [results]]
          [combineTiles :only [combine-tiles]]))
  (:gen-class))

(def triangle
  (dsymbol "1.1:6 1:2 4 6,6 3 5:3"))

(def quadrangle
  (dsymbol "1.1:8 1:2 4 6 8,8 3 5 7:4"))

(defn- orbit-complete? [ds indices D]
    (empty? (for [D (orbit-elements ds indices [D])
                  i indices
                  :when (nil? (s ds i D))] [D i])))

(defn- good-orbit? [ds [i j] D]
  (or (not (orbit-complete? ds [i j] D))
      (#{3 4} (r ds i j D))))

(defn- good? [ds]
  (and (every? #(not= % (s ds 2 %)) (elements ds))
       (every? (partial good-orbit? ds [1 2]) (orbit-reps ds [1 2]))))

(defn self-dual-3-4-regular [n]
  (for [i (range (inc (- n 4)))
        :let [face-list (apply append (concat (repeat 4 triangle)
                                              (repeat i quadrangle)))]
        set (results (combine-tiles face-list) (comp good? first))
        :when (and (self-dual? set) (proto-spherical? set))
        ]
    set))

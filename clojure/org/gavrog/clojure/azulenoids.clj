(ns org.gavrog.clojure.azulenoids
  (:use (org.gavrog.clojure
          [util :only [iterate-cycle]]
          [delaney]
          [generators :only [results]]
          [branchings2d :only [branchings]]
          [combineTiles :only [combine-tiles]]))
  (:gen-class))

;; Azulenoid-specific functions

(def template
  (dsymbol (str "1.1:60:"
                "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 "
                "32 34 36 38 40 42 44 46 48 50 52 54 56 58 60,"
                "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 "
                "33 35 42 39 41 48 45 47 54 51 53 60 57 59,"
                "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 "
                "29 0 0 0 0 42 41 0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
                "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0")))

(def octagon
  (dsymbol "1.1:16 1:2 4 6 8 10 12 14 16,16 3 5 7 9 11 13 15:8"))

(defn boundary-chambers [ds D i j k]
  (iterate-cycle [#(s ds i %1) #(chain-end ds %1 j k)] D))

(def boundary-mappings
  (let [template-boundary (boundary-chambers template 1 0 1 2)
        octagon-boundary (cycle (range 1 17))]
    (for [p (range 1 16 2)]
      (zipmap (drop (mod (- 19 p) 16) octagon-boundary)
              (take 16 template-boundary)))))

(defn on-template [ds oct2tmp]
  (let [glue-mapped (fn [tmp [D D*]] (glue tmp 2 D* (oct2tmp (s ds 2 D))))
        spin-mapped (fn [tmp [D D*]] (spin tmp 1 2 D* (v ds 1 2 D)))
        glued (reduce glue-mapped template oct2tmp)
        spun (reduce spin-mapped glued oct2tmp)]
    spun))

(def octa-sets (filter #(-> % (curvature 1) (>= 0))
                       (results (combine-tiles octagon))))

(def octa-syms
  (for [dset octa-sets
        dsym (results (branchings dset
                                  :face-sizes-at-least 3
                                  :vertex-degrees-at-least 2
                                  :curvature-at-least 0
                                  :try-spins [1 2 3 4 6]))
        :when (-> dsym curvature (= 0))]
    dsym))

(def azul-syms
  (distinct (for [ds octa-syms, o2t boundary-mappings]
              (-> (on-template ds o2t) minimal dual canonical))))

;; Main entry point when used as a script

(defn -main []
  (do
    (doseq [ds azul-syms] (println ds))
    (println "#Generated:")
    (println "#   " (count octa-sets) "octagonal D-sets.")
    (println "#   " (count octa-syms) "octagonal D-symbols.")
    (println "#   " (count azul-syms) "azulenoid D-symbols.")))

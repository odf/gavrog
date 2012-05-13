(ns org.gavrog.clojure.azulenoids
  (:import (org.gavrog.jane.numbers Whole)
           (org.gavrog.joss.dsyms.basic DSymbol DynamicDSymbol)))

(def template
  (new DSymbol (str "1.1:60:"
                    "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 "
                    "32 34 36 38 40 42 44 46 48 50 52 54 56 58 60,"
                    "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 "
                    "33 35 42 39 41 48 45 47 54 51 53 60 57 59,"
                    "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 "
                    "29 0 0 0 0 42 41 0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
                    "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0")))

(defn initial-symbol []
  (new DynamicDSymbol
       (str "1.1:16:2 4 6 8 10 12 14 16,16 3 5 7 9 11 13 15,"
            "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0:8,0 0 0 0 0 0 0 0")))

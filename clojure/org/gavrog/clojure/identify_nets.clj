(ns org.gavrog.clojure.identify_nets
  (:import (org.gavrog.joss.pgraphs.io Net Archive)
           (java.lang ClassLoader)
           (java.io InputStreamReader BufferedReader)))

(defn add-to-archive [archive path]
  (let [stream (ClassLoader/getSystemResourceAsStream path)]
    (.addAll archive (-> stream InputStreamReader. BufferedReader.))))

(defn make-archive [version & paths]
  (let [archive (new Archive version)]
    (doall (map add-to-archive (repeat archive) paths))
    archive))

(defn find-net [archive net]
  (.get archive (-> net .minimalImage .getSystreKey)))

(defn identify [archive net]
  (cond
    (not (.isLocallyStable net)) :unstable
    (.isLadder net) :ladder
    :else (if-let [found (find-net archive net)]
            (.getName found)
            :unknown)))

(defn identify-all-from-file [archive path]
  (map (partial identify archive) (-> path Net/iterator iterator-seq)))

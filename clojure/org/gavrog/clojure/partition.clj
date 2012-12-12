(ns org.gavrog.clojure.partition
  (:use (clojure test)))

(defprotocol IPartition
  (pfind [_ x])
  (punion [_ x y]))

(deftype Partition [rank ^{:volatile-mutable true} parent]
  IPartition
  (pfind [self x]
         (let [root (loop [c x, pc (parent x)]
                      (if (nil? pc) c (recur pc (parent pc))))
               p (loop [p parent, c x, pc (parent x)]
                   (if (nil? pc)
                     p
                     (recur (assoc p c root) pc (parent pc))))]
           (set! parent p)
           root))
  (punion [self x y]
         (let [x (pfind self x), y (pfind self y)]
           (if (= x y)
             self
             (cond (< (or (rank x) 0) (or (rank y) 0))
                   (Partition. rank (assoc parent x y))
                   (> (or (rank x) 0) (or (rank y) 0))
                   (Partition. rank (assoc parent y x))
                   :else
                   (Partition. (assoc rank x (inc (or (rank x) 0)))
                               (assoc parent y x))))))
  clojure.lang.Seqable
  (seq [self] 
       (let [add-in (fn [b x]
                      (let [root (pfind self x)]
                        (assoc b root (conj (or (b root) #{}) x))))
             blocks (reduce add-in {} (apply concat parent))]
         (vals blocks))))

(def pempty (Partition. {} {}))

;; === Tests

(def ^{:private true} p
  (-> pempty
    (punion 1 2) (punion 3 4) (punion 2 4)
    (punion 1 5) (punion 6 7) (punion 8 8)))

(deftest partition-test
  (testing "find"
           (are [xs] (apply = (map (partial pfind p) xs))
                [1 2 3 4 5]
                [6 7]
                [8]
                [9]))
  (testing "blocks"
           (are [b] (some #{b} p)
                #{1 2 3 4 5}
                #{6 7})
           (are [b] (not (some #{b} p))
               #{8}
               #{9})))

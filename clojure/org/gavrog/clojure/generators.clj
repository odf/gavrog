(ns org.gavrog.clojure.generators)

(defprotocol SubstepGenerator
  (result [_])
  (step [_]))

(defn results [gen]
  (letfn [(s [gen] (when gen (lazy-seq (cons gen (s (step gen))))))]
    (filter (comp not nil?) (map result (s gen)))))

(defprotocol SplittableGenerator
  (sub-generator [_])
  (skip [_]))

(defprotocol Resumable
  (checkpoint [_])
  (resume [_ checkpoint]))

(deftype BacktrackingGenerator [extract make-children stack]
  SubstepGenerator
  (result [_] (extract (first (first stack))))
  (step [gen]
        (if-let [children (seq (make-children (first (first stack))))]
          (let [stack (conj stack [(first children) 0 (rest children)])]
            (BacktrackingGenerator. extract make-children stack))
          (skip gen)))
  Resumable
  (checkpoint [_] (rest (reverse (map second stack))))
  (resume [_ checkpoint]
          (loop [todo checkpoint
                 stack stack]
            (if (seq todo)
              (let [n (first todo)
                    children (drop n (make-children (first (first stack))))
                    stack (conj stack [(first children) n (rest children)])]
                (recur (rest todo) stack))
              (BacktrackingGenerator. extract make-children stack))))
  SplittableGenerator
  (sub-generator [gen]
                 (let [stack (list [(first (first stack))])]
                   (BacktrackingGenerator. extract make-children stack)))
  (skip [_]
        (when-let [stack (seq (drop-while #(not (seq (nth % 2))) stack))]
          (let [[_ branch-nr siblings-left] (first stack)
                stack (conj (rest stack)
                            [(first siblings-left)
                             (inc branch-nr)
                             (rest siblings-left)])]
            (BacktrackingGenerator. extract make-children stack)))))

(defn make-backtracker [spec]
  (BacktrackingGenerator. (:extract spec)
                          (:children spec)
                          (list [(:root spec) 0 nil])))


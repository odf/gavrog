(ns org.gavrog.clojure.permutations)

;    def permutations(degree):
;        def choices(perm):
;            if 0 in perm:
;                i = perm.index(0)
;                return Seq.range(1, degree).select(lambda n: not n in perm).map(
;                    lambda n: perm[:i] + [n] + perm[i+1:])
;
;        return Seq.tree_walk([0] * degree, choices).select(lambda p: not 0 in p)


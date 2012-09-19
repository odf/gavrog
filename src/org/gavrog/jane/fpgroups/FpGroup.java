/*
   Copyright 2012 Olaf Delgado-Friedrichs

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package org.gavrog.jane.fpgroups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Whole;


/**
 * A finitely presented group, that is, a group given by abstract generators and
 * relators. The relators are each converted to their lexicographically smallest
 * form and then sorted. This makes it easier to compare group presentations
 * over the same alphabet.
 */
public class FpGroup<E> {
    final static private Whole ZERO = Whole.ZERO;
    final static private Whole ONE = Whole.ONE;
    
    final private FiniteAlphabet<E> alphabet;
    final private List<FreeWord<E>> relators;
 
    /**
     * Copies an FpGroup instance.
     * @param group the model instance.
     */
    public <X extends E> FpGroup(final FpGroup<X> group) {
        this(group.getAlphabet(), group.getRelators());
    }

    /**
     * Constructs a FpGroup instance without relators.
     * @param alphabet the generators (all letters in the given alphabet).
     */
    @SuppressWarnings("unchecked")
    public FpGroup(final FiniteAlphabet<? extends E> alphabet) {
        this(alphabet, new FreeWord[] {});
    }
    
    /**
     * Constructs a FpGroup instance.
     * @param alphabet the generators (all letters in the given alphabet).
     * @param relators String specifications for the group relators.
     */
    public <X extends E> FpGroup(
            final FiniteAlphabet<X> alphabet,
            final String[] relators) {
        this(alphabet, asWords(alphabet, relators));
    }
    
    /**
     * Constructs a FpGroup instance.
     * @param alphabet the generators (all letters in the given alphabet).
     * @param relators the relators for the group.
     */
    private static <X> List<FreeWord<X>> asWords(
            final FiniteAlphabet<X> alphabet,
            final String[] relators) {
        final List<FreeWord<X>> results = new LinkedList<FreeWord<X>>();
        for (int i = 0; i < relators.length; ++i) {
            results.add(FreeWord.parsedWord(alphabet, relators[i]));
        }
        return results;
    }

    /**
     * Constructs a FpGroup instance.
     * @param alphabet the generators (all letters in the given alphabet).
     * @param relators the relators for the group.
     */
    public <X extends E, Y extends X> FpGroup(final FiniteAlphabet<X> alphabet,
                                              final FreeWord<Y>[] relators) {
        this(alphabet, Arrays.asList(relators));
    }
    
    /**
     * Constructs a FpGroup instance. The relators given are each converted to
     * their lexicographically smallest form and then sorted. This makes it
     * easier to compare group presentations over the same alphabet.
     * @param alphabet the generators (all letters in the given alphabet).
     * @param relators the relators for the group.
     */
    public <X extends E, Y extends X> FpGroup(
            final FiniteAlphabet<X> alphabet,
            final List<FreeWord<Y>> relators) {
        @SuppressWarnings("unchecked")
        final FiniteAlphabet<E> A = (FiniteAlphabet<E>) alphabet;
        final List<FreeWord<E>> tmp = new LinkedList<FreeWord<E>>();
        final Set<FreeWord<E>> seen = new HashSet<FreeWord<E>>();
        for (FreeWord<Y> rel: relators) {
            @SuppressWarnings("unchecked")
            final FreeWord<E> w = (FreeWord<E>) relatorRepresentative(rel);
            if (w.length() > 0 && !seen.contains(w)) {
                tmp.add(w);
                seen.add(w);
            }
        }
        Collections.sort(tmp);
        this.relators = Collections.unmodifiableList(tmp);
        this.alphabet = A;
    }
    
    /**
     * Returns the lexicographically smallest of all cyclic permutations of a
     * word and their inverses. This produces a canonical representation for
     * each class of equivalent single relators.
     * @param w the word to permute.
     * @return the smallest cyclic representation.
     */
    public static <X> FreeWord<X> relatorRepresentative(final FreeWord<X> w) {
        final int n = w.length();
        FreeWord<X> best = w;
        for (int i = 0; i < n; ++i) {
            final FreeWord<X> cand = w.subword(i, n).times(w.subword(0, i));
            if (best.compareTo(cand) > 0) {
                best = cand;
            }
            final FreeWord<X> inv = cand.inverse();
            if (best.compareTo(inv) > 0) {
                best = inv;
            }
        }
        return best;
    }
    
    /**
     * Returns the value of alphabet.
     * @return the current value of alphabet.
     */
    public FiniteAlphabet<E> getAlphabet() {
        return this.alphabet;
    }
    
    /**
     * Returns a list of group generators.
     * @return the generators of this group.
     */
    public List<FreeWord<E>> getGenerators() {
        final FiniteAlphabet<E> A = getAlphabet();
        final List<FreeWord<E>> res = new ArrayList<FreeWord<E>>();
        for (final E name: A.getNameList()) {
            res.add(new FreeWord<E>(A, A.nameToLetter(name)));
        }
        return Collections.unmodifiableList(res);
    }
    
    /**
     * Returns the value of relators.
     * @return the current value of relators.
     */
    public List<FreeWord<E>> getRelators() {
        return this.relators;
    }
    
    /**
     * Returns the identity of this group.
     * @return the identity word in this group's alphabet.
     */
    public FreeWord<E> getIdentity() {
    	return new FreeWord<E>(getAlphabet());
    }
    
    /**
     * Produces a string representation.
     */
    public String toString() {
        final StringBuffer buf = new StringBuffer(100);
        buf.append("FpGroup(");
        buf.append(getAlphabet());
        buf.append(", {");
        boolean first = true;
        for (final FreeWord<E> r: getRelators()) {
            if (!first) {
                buf.append(", ");
            }
            buf.append(r);
            first = false;
        }
        buf.append("})");
        return buf.toString();
    }
    
    /**
	 * Returns a 2-dimensional array with a column for each generator and a row
	 * for each relator.
	 * 
	 * @return the relator array.
	 */
    private Whole[][] relatorMatrixAsArray() {
        final int ngens = this.getGenerators().size();
        final List<FreeWord<E>> rels = this.getRelators();
        final int nrels = rels.size();

        final Whole M[][] = new Whole[nrels][ngens];

        for (int i = 0; i < nrels; ++i) {
            final FreeWord<E> r = rels.get(i);
            final int row[] = new int[ngens];
            for (int j = 0; j < r.length(); ++j) {
                final int k = r.getLetter(j) - 1;
                if (r.getSign(j) > 0) {
                    ++row[k];
                } else {
                    --row[k];
                }
            }
            for (int j = 0; j < ngens; ++j) {
                M[i][j] = new Whole(row[j]);
            }
        }
        
        return M;
    }
    
    /**
     * Returns a matrix with a column for each generator and a row for each relator.
     * 
     * @return the relator matrix.
     */
    public Matrix relatorMatrix() {
    	return new Matrix(relatorMatrixAsArray());
    }
    
    /**
     * Returns the abelian invariants of this group as a sorted list of
     * {@link Whole}s.
     * 
     * @return the sorted abelian invariants.
     */
    public List<Whole> abelianInvariants() {
        final int ngens = this.getGenerators().size();
        final List<FreeWord<E>> rels = this.getRelators();
        final int nrels = rels.size();
        final int n = Math.min(nrels, ngens);
        final List<Whole> res = new LinkedList<Whole>();
        
        if (nrels > 0) {
            // --- compute and diagonalize the relator matrix
        	final Whole M[][] = relatorMatrixAsArray();
            diagonalize(M);

            // --- collect the diagonal elements
            final Whole divs[] = new Whole[n];
            for (int i = 0; i < n; ++i) {
                divs[i] = M[i][i];
            }

            // --- convert to reflect the actual abelian factors
            for (int i = 0; i < n - 1; ++i) {
                for (int j = i + 1; i < n; ++i) {
                    final Whole a = divs[i];
                    final Whole b = divs[j];
                    if (!(a.equals(ZERO) || b.mod(a).equals(ZERO))) {
                        final Whole gcd = a.gcd(b);
                        divs[j] = (Whole) b.dividedBy(gcd).times(a);
                        divs[i] = gcd;
                    }
                }
            }

            // --- collect the results
            for (int i = 0; i < n; ++i) {
                if (!(divs[i].equals(ONE))) {
                    res.add(divs[i]);
                }
            }
        }
        
        // --- add the non-torsion factors
        for (int i = 0; i < ngens - n; ++i) {
            res.add(ZERO);
        }
        
        Collections.sort(res);
        
        // --- return the result
        return res;
    }

    /**
     * Helper method, diagonalizes an integer matrix using row and column
     * operations.
     * 
     * Note: This method has default visibility to allow for unit testing.
     * 
     * @param M the matrix to diagonalize.
     */
    static void diagonalize(Whole[][] M) {
        // --- don't bother with empty matrices
        if (M.length == 0 || M[0].length == 0) {
            return;
        }
        
        // -- get the sizes
        final int nrows = M.length;
        final int ncols = M[0].length;
        final int n = Math.min(nrows, ncols);

        // --- eliminate off-diagonal elements in a diagonal sweep
        for (int i = 0; i < n; ++i) {

            // --- find the nonzero submatrix entry with smallest absolute value
            Whole pivotVal = null;
            int pivotRow = 0;
            int pivotCol = 0;
            for (int row = i; row < nrows; ++row) {
                for (int col = i; col < ncols; ++col) {
                    final Whole entry = (Whole) M[row][col].abs();
                    if (!entry.equals(ZERO)) {
                        if (pivotVal == null || entry.compareTo(pivotVal) < 0) {
                            pivotVal = entry;
                            pivotRow = row;
                            pivotCol = col;
                        }
                    }
                }
            }
            
            if (pivotVal == null) {
                // --- submatrix contains only zeroes
                return;
            }
            
            // --- move the pivot to the diagonal and make it positive
            if (pivotRow != i) {
                for (int col = i; col < ncols; ++col) {
                    final Whole t = M[i][col];
                    M[i][col] = M[pivotRow][col];
                    M[pivotRow][col] = t;
                }
            }
            if (pivotCol != i) {
                for (int row = i; row < nrows; ++row) {
                    final Whole t = M[row][i];
                    M[row][i] = M[row][pivotCol];
                    M[row][pivotCol] = t;
                }
            }
            if (M[i][i].compareTo(ZERO) < 0) {
                for (int col = i; col < ncols; ++col) {
                    M[i][col] = (Whole) M[i][col].negative();
                }
            }
            
            // --- eliminate off-diagonal entries in i-th row and column
            boolean done = false;
            while (!done) {
                // --- clear the i-th column by row operations
                for (int row = i+1; row < nrows; ++row) {
                    final Whole e = M[i][i];
                    final Whole f = M[row][i];
                    
                    if (!e.equals(ZERO) && f.mod(e).equals(ZERO)) {
                        final Whole x = (Whole) f.dividedBy(e);
                        for (int col = i; col < ncols; ++col) {
                            M[row][col] = (Whole) M[row][col].minus(x.times(M[i][col]));
                        }
                    } else if (!f.equals(ZERO)) {
                        final Whole x[] = gcdex(e, f);
                        for (int col = i; col < ncols; ++col) {
                            final Whole v = M[i][col];
                            final Whole w = M[row][col];
                            M[i][col] = (Whole) v.times(x[1]).plus(w.times(x[2]));
                            M[row][col] = (Whole) v.times(x[3]).plus(w.times(x[4]));
                        }
                    }
                }
                
                // --- now try to clear the i-th row by column operations
                done = true;
                
                for (int col = i+1; col < ncols; ++col) {
                    final Whole e = M[i][i];
                    final Whole f = M[i][col];
                    
                    if (!e.equals(ZERO) && f.mod(e).equals(ZERO)) {
                        M[i][col] = ZERO;
                    } else if (!f.equals(ZERO)) {
                        final Whole x[] = gcdex(e, f);
                        for (int row = i; row < nrows; ++row) {
                            final Whole v = M[row][i];
                            final Whole w = M[row][col];
                            M[row][i] = (Whole) v.times(x[1]).plus(w.times(x[2]));
                            M[row][col] = (Whole) v.times(x[3]).plus(w.times(x[4]));
                        }
                        // --- i-th column no longer clear, need another pass
                        done = false;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Helper method, implements the extended Euclidean algorithm to compute the
     * greatest common divisor (gcd) of two integers m and n. This method
     * returns an array of five integers, (x, a, b, c, d), where x is the gcd
     * itself and the others are factors providing a*m + b*n = x and c*m + d*n =
     * 0.
     * 
     * Note: This method has default visibility to allow for unit testing.
     * 
     * @param m the first number.
     * @param n the second number.
     * @return the array containing the gcd and the factors.
     */
    static Whole[] gcdex(Whole m, Whole n) {
        Whole f, g, fm, gm, t, tm;
        
        f = (Whole) m.abs();
        fm = (Whole) f.dividedBy(m);
        g = (Whole) n.abs();
        gm = ZERO;

        while (!g.equals(ZERO)) {
            final Whole q = f.div(g);
            t = g;
            tm = gm;
            g = (Whole) f.minus(q.times(g));
            gm = (Whole) fm.minus(q.times(gm));
            f = t;
            fm = tm;
        }
        
        if (n.equals(ZERO)) {
            return new Whole[] { f, fm, ZERO, gm, ONE };
        } else {
            return new Whole[] {
                    f,
                    fm, (Whole) f.minus(fm.times(m)).dividedBy(n),
                    gm, (Whole) ZERO.minus(gm.times(m)).dividedBy(n)
            };
        }
    }
}

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

package org.gavrog.joss.dsyms.generators;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;


/**
 * An iterator that takes a 3-dimensional Delaney symbol with some undefined
 * branching numbers and defines these in all possible combinations such that
 * the results are locally euclidean symbols.
 * 
 * Solutions which would correspond to tilings with face or edge degrees of 2 or
 * less are excluded.
 * 
 * For each isomorphism class of resulting symbols, only one respresentative is
 * produced. The order or naming of elements is not preserved.
 */
public class DefineBranching3d extends IteratorAdapter<DSymbol> {
    // --- set to true to enable logging
    final private static boolean LOGGING = false;
    
    // --- the input data
    final private List<Integer> acceptedValues;
    final private boolean allowEdgeDegreeTwo;
    
    // --- properties of the input symbol
    final private int size;
    final private int dim;
    
    // --- auxiliary data
    final private Map<Integer, Integer> nextValue;
    final private List<DSMorphism<Integer, Integer>> inputAutomorphisms;

    // --- true if current sybmol is complete and has not yet been delivered
    boolean immediate = false;
    
    // --- the current state
    final private DynamicDSymbol current;
    final private LinkedList<Move> stack;
    
    // --- statistics
    int countNonSpherical = 0;
    
    /**
     * The instances of this class represent individual moves of setting
     * branch values. These become the entries of the trial stack.
     */
    protected class Move {
        final public int index;
        final public int element;
        final public int value;
        final public boolean isChoice;
        
        public Move(final int index, final int element, final int value,
                final boolean isChoice)
        {
            this.index = index;
            this.element = element;
            this.value = value;
            this.isChoice = isChoice;
        }

        public String toString() {
            return "Move(" + index + ", " + element + ", " + value + ", " + isChoice
                    + ")";
        }
    }
    
    /**
     * The accepted branch values for a symbol fullfilling the crystallographic
     * restriction.
     */
    private final static List<Integer> standardValues =
            Collections.unmodifiableList(
                    Arrays.asList(new Integer[] { 1, 2, 3, 4, 6 }));
    
    /**
     * Constructs an instance with standard options.
     */
    public <T> DefineBranching3d(final DelaneySymbol<T> ds) {
        this(ds, standardValues, false);
    }
    
    /**
     * Constructs an instance with the standard set of accepted branch values.
     */
    public <T> DefineBranching3d(
            final DelaneySymbol<T> ds,
            final boolean allowEdgeDegreeTwo)
    {
        this(ds, standardValues, allowEdgeDegreeTwo);
    }
    
    /**
     * Constructs an instance with no edges of degree two allowed.
     */
    public <T> DefineBranching3d(
            final DelaneySymbol<T> ds,
            final List<Integer> acceptedValues)
    {
        this(ds, acceptedValues, false);
    }
    
    /**
     * Constructs an instance.
     */
    public <T> DefineBranching3d(
            final DelaneySymbol<T> ds,
            final List<Integer> acceptedValues,
    		final boolean allowEdgeDegreeTwo)
    {
        // --- check the argument
        check(ds, acceptedValues);
        
        // --- store the input parameters
        this.acceptedValues = acceptedValues;
        this.allowEdgeDegreeTwo = allowEdgeDegreeTwo;
        
        // --- compute successors for acepted values
        this.nextValue = new HashMap<Integer, Integer>();
        final Set<Integer> seen = new HashSet<Integer>();
        seen.add(null);
        
        int previous = 0;
        for (final int v: this.acceptedValues) {
            if (!seen.contains(v)) {
                this.nextValue.put(previous, v);
                seen.add(v);
                previous = v;
            }
        }
        
        // --- initialize the state
        this.size = ds.size();
        this.dim = ds.dim();
        this.stack = new LinkedList<Move>();
        this.current = new DynamicDSymbol(new DSymbol(ds.canonical()));

        // --- compute more auxiliary data
        this.inputAutomorphisms = DSMorphism.automorphisms(this.current);
        
        // --- compute intitial deductions
        if (LOGGING) {
            System.err.println("Computing initial deductions...");
        }
        for (int i = 0; i < this.dim; ++i) {
            final int j = i+1;
            final IndexList idcs = new IndexList(i, j);
            for (final int D: this.current.orbitReps(idcs)) {
                if (this.current.definesV(i, j, D)) {
                    final boolean success = performMove(
                            new Move(i, D, this.current.v(i, j, D), true));
                    this.stack.clear();
                    if (!success) {
                        // --- no solution since given branching inconsistent
                        return;
                    }
                }
            }
        }
        if (LOGGING) {
            System.err.println("Initial deductions done.");
        }
        
        // --- find the next open choice
        final Move choice = nextChoice(-1, 1);
        
        if (choice == null) {
            // --- only one solution
            immediate = true;
        } else {
            // --- put a dummy move on the stack
            this.stack.addLast(choice);
        }
    }

    /**
     * Checks if a given symbol is a valid argument to the constructor.
     * @param ds the symbol to check.
     * @param acceptedValues list of accepted branching values.
     */
    private <T> void check(final DelaneySymbol<T> ds,
            final List<Integer> acceptedValues)
    {
        // --- check simple properties
        if (ds == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (ds.dim() != 3) {
            throw new IllegalArgumentException("dimension must be 3");
        }
        try {
            ds.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        if (!ds.isConnected()) {
            throw new UnsupportedOperationException("symbol must be connected");
        }
        
        // --- check that all neighbor relations (ops) are defined.
        for (final T D: ds.elements()) {
            for (final int i: ds.indices()) {
                if (!ds.definesOp(i, D)) {
                    throw new UnsupportedOperationException(
                            "symbol must define all ops");
                }
            }
        }
        
        if (!acceptedValues.contains(new Integer(1))) {
            throw new UnsupportedOperationException(
                    "1 must be an accepted branching value.");
        }
        if (!acceptedValues.contains(new Integer(2))) {
            throw new UnsupportedOperationException(
                    "2 must be an accepted branching value.");
        }
        
        // --- local curvatures must be positive for locally Euclidean result
        if (!Utils.mayBecomeLocallyEuclidean3D(ds)) {
            throw new IllegalArgumentException(
                    "symbol must have positive local curvatures");
        }
    }

    /**
     * Repeatedly finds the next legal choice in the enumeration tree and
     * executes it, together with all its implications, until all branch values
     * are defined and in canonical form, in which case the resulting symbol is
     * returned.
     * 
     * Does appropriate backtracking in order to find the respective next
     * choice. Also backtracks if the partial symbol resulting from the latest
     * choice is not in canonical form.
     * 
     * To simplify the code, the algorithm makes use of "dummy moves", which are
     * put on the stack as fallback entries but do not have any effect on the
     * symbol. A dummy move is of the form
     * <code>Move(index, element, 0, false)</code> and effectively indicates
     * that the next value to be set is for that index and element.
     * 
     * @return the next symbol, if any.
     */
    protected DSymbol findNext() throws NoSuchElementException {
        if (LOGGING) {
            System.err.println("findNext(): stack size = " + this.stack.size());
        }
        if (immediate) {
            if (LOGGING) {
                System.err.println("  delivering a precomputed solution");
            }
            immediate = false;
            return new DSymbol(this.current);
        }
        
        while (true) {
            final Move choice = undoLastChoice();
            if (LOGGING) {
                System.err.println("  last choice was " + choice);
            }
            if (choice == null) {
                if (LOGGING) {
                    System.err.println("Encountered " + this.countNonSpherical
                            + " bad subsymbols.");
                    System.err.println();
                }
                throw new NoSuchElementException();
            }
            final Move move = nextMove(choice);
            if (move == null) {
                if (LOGGING) {
                    System.err.println("  no potential move");
                }
                continue;
            }
            if (LOGGING) {
                System.err.println("  found potential move " + move);
            }
            if (performMove(move)) {
                if (LOGGING) {
                    System.err.println("  move was successful");
                }
                if (isCanonical()) {
                    final Move next = nextChoice(choice.index, choice.element);
                    if (next == null) {
                        return new DSymbol(this.current);
                    } else {
                        this.stack.addLast(next);
                    }
                } else {
                    if (LOGGING) {
                        System.err.println("  result of move is not canonical");
                    }
                }
            } else {
                if (LOGGING) {
                    System.err.println("  move was rejected");
                }
            }
        }
    }

    /**
     * Finds the next undefined branching position, giving rise to a new choice.
     * 
     * @param lastIndex index of last defined position.
     * @param lastElement element of last defined position.
     * @return a move describing the choice found.
     */
    private Move nextChoice(final int lastIndex, final int lastElement) {
        int D = lastElement;
        int i = lastIndex;
        while (D <= this.size) {
            while (i < this.dim-1) {
                ++i;
                if (!this.current.definesV(i, i+1, D)) {
                    return new Move(i, D, 0, true);
                }
            }
            ++D;
            i = -1;
        }

        // --- nothing found
        return null;
    }

    /**
     * Undoes the last choice and all its implications by popping moves from the
     * stack until one is found which is a choice. The corresponding branching
     * assignment are cleared and the last choice is returned. If there was no
     * choice left on the stack, a <code>null</code> result is returned.
     * 
     * @return the last choice or null.
     */
    private Move undoLastChoice() {
        Move last;
        do {
            if (this.stack.size() == 0) {
                return null;
            }
            last = (Move) this.stack.removeLast();
            
            if (LOGGING) {
                System.err.println("Undoing " + last);
            }
            this.current.undefineV(last.index, last.index+1, last.element);
        } while (!last.isChoice);
    
        return last;
    }

    /**
     * Finds the next legal move for the same choice.
     * @param choice describes the previous move.
     * @return the next move or null.
     */
    private Move nextMove(Move choice) {
        final Integer next = (Integer) this.nextValue.get(choice.value);
        if (next == null) {
            return null;
        } else {
            return new Move(choice.index, choice.element, next, true);
        }
    }

    /**
     * Performs a move with all its implications. This includes setting the
     * branching value described by the move, pushing the move on the stack
     * and as well performing all the deduced moves as dictated by the
     * local euclidicity condition.
     * 
     * @param initial the move to try.
     * @return true if the move did not lead to a contradiction.
     */
    private boolean performMove(final Move initial) {
        if (LOGGING) {
            System.err.println("Performing " + initial);
        }
        
        // --- a little shortcut
        final DynamicDSymbol ds = this.current;

        // --- we maintain a queue of deductions, starting with the initial move
        final LinkedList<Move> queue = new LinkedList<Move>();
        queue.addLast(initial);
        
        boolean firstMove = true;

        while (queue.size() > 0) {
            // --- get some info on the next move in the queue
            final Move move = queue.removeFirst();
            final int i = move.index;
            final int D = move.element;

            // --- see if the move would contradict the current state
            if (ds.definesV(i, i+1, D)) {
                if (ds.v(i, i+1, D) == move.value) {
                    if (!firstMove) {
                        continue;
                    }
                } else {
                    if (LOGGING) {
                        System.err.println(
                                "    found contradiction at " + move);
                    }
                    if (move == initial) {
                        // --- the initial move was impossible
                        throw new IllegalArgumentException(
                                "Internal error: received illegal move.");
                    }
                    return false;
                }
            }
            
            firstMove = false;
            
            // --- perform the move
            ds.redefineV(i, i+1, D, move.value);

            // --- record the move we have performed
            this.stack.addLast(move);
            
            // --- make sure we have not introduced a face of degree 2 or less
            if (i == 0 && ds.m(i, i+1, D) < 3) {
                if (LOGGING) {
                    System.err.println("    found degenerate face");
                }
                return false;
            }
            
            // --- make sure we have not introduced an edge of degree 2 or less
            if (i == 2 && ds.m(i, i+1, D) < (this.allowEdgeDegreeTwo ? 2 : 3)) {
                if (LOGGING) {
                    System.err.println("    found degenerate edge");
                }
                return false;
            }
            
            // --- handle deductions or contradictions in a derived class
            final List<Move> extraDeductions = getExtraDeductions(ds, move);
            if (extraDeductions == null) {
                return false;
            } else {
                if (LOGGING) {
                    for (final Move ded: extraDeductions) {
                        System.err.println("    found extra deduction " + ded);
                    }
                }
                queue.addAll(extraDeductions);
            }
            
            // --- look for problems or deductions from local euclidicity
            for (int j = 0; j <= this.dim; ++j) {
                if (j != i-1 && j != i+2) {
                    continue;
                }
                final IndexList idcs = new IndexList(i, i+1, j);
                final DelaneySymbol<Integer> sub =
                        new Subsymbol<Integer>(ds, idcs, D);
                final List<Move> deductions = getDeductions(sub);
                if (deductions == null) {
                    if (LOGGING) {
                        System.err.println("    found invalid 2d subsymbol");
                    }
                    ++this.countNonSpherical;
                    return false;
                } else {
                    if (LOGGING) {
                        for (final Move ded: deductions) {
                            System.err.println("    found deduction " + ded);
                        }
                    }
                    queue.addAll(deductions);
                }
            }
            
            // --- look for more deductions at "trivial" 2d orbits
            final int k;
            if (i == 0) {
                k = 3;
            } else if (i == 2) {
                k = 0;
            } else {
                continue;
            }
            final Integer Dk = (Integer) this.current.op(k, D);
            final Move deduction =
                    new Move(i, Dk.intValue(), move.value, false);
            if (ds.definesV(i, i + 1, Dk)) {
                if (ds.v(i, i + 1, Dk) != move.value) {
                    if (LOGGING) {
                        System.err.println("    found contradiction at "
                                + deduction);
                    }
                    return false;
                }
            } else {
                if (LOGGING) {
                    System.err.println("    found deduction " + deduction);
                }
                queue.addLast(deduction);
            }
        }

        return true;
    }

    /**
     * Tests if the current symbol is in canonical form with respect to this
     * generator class. That means it is the form of the symbol that should be
     * reported.
     * 
     * @return true if the symbol is canonical.
     */
    private boolean isCanonical() {
        for (final DSMorphism<Integer, Integer> map: this.inputAutomorphisms) {
            if (compareWithPermuted(this.current, map) > 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Lexicographically compares the sequence of v-values of a Delaney symbol
     * with the sequence as permuted by an automorphism of the symbol. If both
     * sequences are equal, 0 is returned. If the unpermuted one is smaller, a
     * negative value, and if the permuted one is smaller, a positive value is
     * returned. An undefined v-value is considered larger than any defined
     * v-value.
     * 
     * @param ds the input symbol.
     * @param map the automorphism.
     * @return an integer indicating if the result.
     */
    private static <T> int compareWithPermuted(
            final DelaneySymbol<T> ds,
            final DSMorphism<T, T> map) {
        for (final T D1: ds.elements()) {
            final T D2 = map.getASource(D1);
            for (int i = 0; i < ds.dim(); ++i) {
                final int v1 =
                        ds.definesV(i, i + 1, D1) ? ds.v(i, i + 1, D1) : 0;
                final int v2 =
                        ds.definesV(i, i + 1, D2) ? ds.v(i, i + 1, D2) : 0;
                if (v1 != v2) {
                    if (v1 == 0) {
                        return 1;
                    } else if (v2 == 0) {
                        return -1;
                    } else {
                        return v1 - v2;
                    }
                }
            }
        }
        return 0;
    }
    
    /**
     * Computes those settings for undefined branching numbers which are
     * deducible from the defined ones in the given 2-dimensional Delaney
     * symbol.
     * 
     * @param ds the input symbol.
     * @return the list of deductions (may be empty) or null if contradiction.
     */
    private List<Move> getDeductions(final DelaneySymbol<Integer> ds) {
        if (ds.dim() != 2) {
            throw new IllegalArgumentException("symbol must be 2-dimensional");
        }
        ds.setVDefaultToOne(true);
        if (!ds.curvature2D().isPositive()) {
            return null;
        }
        
        class Undefined {
            final public int i;
            final public int D;
            final public boolean twice;
            
            public Undefined(final int i, final int D, final boolean twice) {
                this.i = i;
                this.D = D;
                this.twice = twice;
            }
        }
        
        final IndexList allIndices = new IndexList(ds);
        final boolean oriented = ds.isOriented();

        final List<Move> result = new LinkedList<Move>();
        final List<Integer> degrees = new ArrayList<Integer>();
        final List<Undefined> undefined = new ArrayList<Undefined>();
        boolean singleUndefinedExists = false;
        
        for (int ii = 0; ii < 2; ++ii) {
            final int i = allIndices.get(ii);
            for (int jj = ii+1; jj <= 2; ++jj) {
                final int j = allIndices.get(jj);
                final IndexList idcs = new IndexList(i, j);
                for (final int D: ds.orbitReps(idcs)) {
                    final boolean twice =
                            !oriented && ds.orbitIsOriented(idcs, D);
                    if (ds.definesV(i, j, D)) {
                        final int v = ds.v(i, j, D);
                        if (v > 1) {
                            degrees.add(v);
                            if (twice) { 
                                degrees.add(v);
                            }
                        }
                    } else {
                        if (j != i+1) {
                            throw new RuntimeException(
                                    "this should not happen");
                        }
                        undefined.add(new Undefined(i, D, twice));
                        if (!twice) {
                            singleUndefinedExists = true;
                        }
                    }
                }
            }
        }
        
        // --- find contradictions and deductions
        final int n = degrees.size();
        
        if (n == 3) {
            for (final Undefined orb: undefined) {
                result.add(new Move(orb.i, orb.D, 1, false));
            }
        } else if (n == 2) {
            final int a = Collections.max(degrees);
            final int b = Collections.min(degrees);
            if (a != b) {
                if ((a > 5 && b > 2) || b > 3 || undefined.size() == 0
                        || !singleUndefinedExists) {
                    return null;
                } else if (undefined.size() == 1) {
                    final Undefined orb = undefined.get(0);
                    if (!orb.twice) {
                        if ((a <= 5 && b == 3) || (a >= 5 && b == 2)) {
                            result.add(new Move(orb.i, orb.D, 2, false));
                        }
                    }
                }
            } else {
                if (a >= 4 || !singleUndefinedExists) {
                    for (final Undefined orb: undefined) {
                        result.add(new Move(orb.i, orb.D, 1, false));
                    }
                }
            }
        } else if (n == 1) {
            final int a = degrees.get(0);
            if (undefined.size() == 0) {
                return null;
            } else if (undefined.size() == 1) {
                final Undefined orb = undefined.get(0);
                if (orb.twice) {
                    if (a != 2) {
                        result.add(new Move(orb.i, orb.D, 2, false));
                    }
                } else {
                    result.add(new Move(orb.i, orb.D, degrees.get(0), false));
                }
            }
        } else if (n == 0) {
            if (undefined.size() == 1) {
                final Undefined orb = undefined.get(0);
                if (!orb.twice) {
                    result.add(new Move(orb.i, orb.D, 1, false));
                }
            }
        }
        
        return result;
    }
    
    /**
     * Hook for derived classes to specify additional deductions of a move.
     * 
     * @param ds the current symbol.
     * @param move the last move performed.
     * @return the list of deductions (may be empty) or null if contradiction.
     */
    protected List<Move> getExtraDeductions(
            final DelaneySymbol<Integer> ds,
            final Move move) {
        return new ArrayList<Move>();
    }
    
    public static void main(String[] args) {
        boolean verbose = false;
        boolean check = true;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
            if (args[i].equals("-v")) {
                verbose = !verbose;
            } else if (args[i].equals("-e")){
                check = !check;
            } else {
                System.err.println("Unknown option '" + args[i] + "'");
            }
            ++i;
        }
        
        final Iterator<DSymbol> syms;
        if (args.length > i) {
            syms = Iterators.singleton(new DSymbol(args[i]));
        } else {
            syms = new InputIterator(
                    new BufferedReader(new InputStreamReader(System.in)));
        }
        
        int inCount = 0;
        int outCount = 0;
        int countGood = 0;
        int countAmbiguous = 0;
        
        while (syms.hasNext()) {
            final DSymbol ds = syms.next();
            final Iterator<DSymbol> iter = new DefineBranching3d(ds);
            ++inCount;

            try {
                while (iter.hasNext()) {
                    final DSymbol out = iter.next();
                    ++outCount;
                    if (check) {
                        final EuclidicityTester tester =
                                new EuclidicityTester(out);
                        if (tester.isAmbiguous()) {
                            System.out.println("??? " + out);
                            ++countAmbiguous;
                        } else if (tester.isGood()) {
                            System.out.println(out);
                            ++countGood;
                        }
                    } else {
                        System.out.println(out);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
        System.err.println("Processed " + inCount + " input symbols.");
        System.err.println("Produced " + outCount + " output symbols.");
        if (check) {
            System.err.println("Of the latter, " + countGood
                    + " were found euclidean.");
            if (countAmbiguous > 0) {
                System.err.println("For " + countAmbiguous
                        + " symbols, euclidicity could not yet be decided.");
            }
        }
        System.err.println("Options: " + (check ? "" : "no")
                + " euclidicity check, "
                + (verbose ? "verbose" : "quiet") + ".");
    }
}

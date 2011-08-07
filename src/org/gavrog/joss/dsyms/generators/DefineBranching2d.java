/*
   Copyright 2008 Olaf Delgado-Friedrichs

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.algorithms.BranchAndCut;
import org.gavrog.joss.algorithms.CheckpointEvent;
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;

import buoy.event.EventProcessor;

/**
 * An iterator that takes a 2-dimensional Delaney symbol with some undefined
 * branching numbers and defines these in all possible combinations. Only
 * branching numbers 1, 2, 3, 4 and 6 are tried. Derived classes may change the
 * definition of {@link #nextChoice(Move)} to change that.
 * 
 * For each isomorphism class of resulting symbols, only one respresentative is
 * produced. The order or naming of elements is not preserved.
 * 
 * @author Olaf Delgado
 * @version $Id: DefineBranching2d.java,v 1.10 2007/04/23 20:57:06 odf Exp $
 * 
 */
public class DefineBranching2d extends BranchAndCut<DSymbol> {
    //TODO add option to change the set of branching numbers tried

    private final int minFaceDeg;
	private final int minVertDeg;
	private final Rational minCurv;
	private DynamicDSymbol current;
	private List<DSMorphism> inputAutomorphisms;

    /**
	 * The instances of this class represent individual moves of setting branch
	 * values. These become the entries of the trial stack.
	 */
	private class BMove implements Move {
		final public int index;
		final public int element;
		final public int value;

		public BMove(final int index, final int element, final int value) {
			this.index = index;
			this.element = element;
			this.value = value;
		}

		public String toString() {
			return String.format("(%d, %d, %d)", index, element, value);
		}
	}
    
	/**
	 * Constructs a new instance.
	 */
	public DefineBranching2d(final DelaneySymbol ds, int minFaceDeg,
            final int minVertDeg, final Rational minCurv) {
		super();
        this.minFaceDeg = minFaceDeg;
		this.minVertDeg = minVertDeg;
		this.minCurv = minCurv;
		
		// --- create a canonical dynamic symbol isomorphic to the input one
        this.current = new DynamicDSymbol(new DSymbol(ds.canonical()));
        
        // --- compute the maximal curvature
        this.current.setVDefaultToOne(true);
        this.current.setVDefaultToOne(false);
        
        // --- compute automorphisms for later use in {@link #isValid}.
        this.inputAutomorphisms = DSMorphism.automorphisms(this.current);
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#nextChoice(org.gavrog.joss.algorithms.Move)
	 */
	protected Move nextChoice(final Move previous) {
		final DynamicDSymbol ds = this.current;
		int i = 0;
		int D = 1;
		if (previous != null) {
			i = ((BMove) previous).index;
			D = ((BMove) previous).element + 1;
		}
		while (i < ds.dim()) {
			while (D <= ds.size() && ds.definesV(i, i+1, new Integer(D))) {
				++D;
			}
			if (D <= ds.size()) {
				break;
			}
			++i;
			D = 1;
		}

		if (i >= ds.dim()) {
			return null;
		} else {
			return new BMove(i, D, 0);
		}
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#nextDecision(org.gavrog.joss.algorithms.Move)
	 */
	protected Move nextDecision(final Move previous) {
		final BMove move = (BMove) previous;
		final int v = move.value;
		final int next;
		
		if (v < 4) {
			next = v + 1;
		} else if (v == 4) {
			next = 6;
		} else {
			return null;
		}
		
		return new BMove(move.index, move.element, next);
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#checkMove(org.gavrog.joss.algorithms.Move)
	 */
	protected Status checkMove(final Move move) {
		final int idx = ((BMove) move).index;
		final Integer D = new Integer(((BMove) move).element);
		final int val = ((BMove) move).value;
		
        if (idx == 0 && this.current.r(idx, idx+1, D) * val < this.minFaceDeg) {
            return Status.ILLEGAL;
        }
		if (idx == 1 && this.current.r(idx, idx+1, D) * val < this.minVertDeg) {
			return Status.ILLEGAL;
		}
		return Status.OK;
	}
	
	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#performMove(org.gavrog.joss.algorithms.Move)
	 */
	protected void performMove(final Move move) {
		final int idx = ((BMove) move).index;
		final Integer D = new Integer(((BMove) move).element);
		
		this.current.redefineV(idx, idx+1, D, ((BMove) move).value);
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#undoMove(org.gavrog.joss.algorithms.Move)
	 */
	protected void undoMove(Move move) {
		final int idx = ((BMove) move).index;
		final int elm = ((BMove) move).element;
		
		this.current.undefineV(idx, idx+1, new Integer(elm));
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#deductions(org.gavrog.joss.algorithms.Move)
	 */
	protected List<Move> deductions(Move move) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#isValid()
	 */
	protected boolean isValid() {
		final DynamicDSymbol ds = this.current;
		ds.setVDefaultToOne(true);
		final Rational curv = ds.curvature2D();
		ds.setVDefaultToOne(false);
		if (curv.isLessThan(this.minCurv)) {
			return false;
		}
		for (final DSMorphism map: this.inputAutomorphisms) {
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
    private static int compareWithPermuted(final DelaneySymbol ds,
			final DSMorphism map) {
        for (final Iterator elms = ds.elements(); elms.hasNext();) {
            final Object D1 = elms.next();
            final Object D2 = map.getASource(D1);
            for (int i = 0; i < ds.dim(); ++i) {
                int v1 = ds.definesV(i, i + 1, D1) ? ds.v(i, i + 1, D1) : 0;
                int v2 = ds.definesV(i, i + 1, D2) ? ds.v(i, i + 1, D2) : 0;
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
    
    /* (non-Javadoc)
     * @see org.gavrog.joss.algorithms.BranchAndCut#isComplete()
     */
    protected boolean isComplete() {
		final DynamicDSymbol ds = this.current;
		
		// --- check for completeness
		for (int i = 0; i < ds.dim(); ++i) {
			for (int D = 1; D <= ds.size(); ++D) {
				if (!ds.definesV(i, i+1, new Integer(D))) {
					return false;
				}
			}
		}
		
		return true;
    }
    
	/* (non-Javadoc)
	 * @see org.gavrog.joss.algorithms.BranchAndCut#makeResult()
	 */
	protected DSymbol makeResult() {
		// --- return the result as a flat symbol.
		return new DSymbol(this.current);
	}

	/**
	 * Example main method, generates euclidean symbols.
	 * 
	 * @throws FileNotFoundException if an input file was not found.
	 */
	public static void main(final String[] args) {
        String filename = null;
        String resume = null;
        int i = 0;
        while (i < args.length && args[i].startsWith("-")) {
        	if (args[i].equals("-i")) {
        		filename = args[++i];
        	} else if (args[i].equals("-r")) {
        		resume = args[++i];
        	} else {
        		System.err.println("Unknown option '" + args[i] + "'");
        	}
            ++i;
        }
        
        final Iterator syms;
        if (args.length > i) {
            final DSymbol ds = new DSymbol(args[i]);
            syms = Iterators.singleton(ds);
        } else if (filename != null) {
        	try {
				syms = new InputIterator(new BufferedReader(new FileReader(
						filename)));
			} catch (final FileNotFoundException ex) {
				ex.printStackTrace(System.err);
				return;
			}
        } else {
            syms = new InputIterator(new BufferedReader(new InputStreamReader(
					System.in)));
        }
        
        int inCount = 0;
        int outCount = 0;
        
        while (syms.hasNext()) {
            final DSymbol ds = (DSymbol) syms.next();
            ++inCount;
            final DefineBranching2d iter =
            	new DefineBranching2d(ds, 3, 2, Whole.ZERO);
            iter.setResumePoint(resume);
    		iter.addEventLink(CheckpointEvent.class, new EventProcessor() {
    			@Override
    			public void handleEvent(Object event) {
    				System.out.println(event);
    				System.out.flush();
    			}
    		});

            try {
            	for (final DSymbol out: iter) {
                    if (out.curvature2D().isZero()) {
                    	++outCount;
                    	System.out.println(out);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
        System.out.println("# Processed " + inCount + " input symbols.");
        System.out.println("# Produced " + outCount + " output symbols.");
	}
}

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

package org.gavrog.joss.dsyms.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.NiftyList;
import org.gavrog.box.simple.Strings;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Rational;


/**
 */
public abstract class DelaneySymbol<T> implements Comparable<DelaneySymbol<T>>
{
    private boolean vDefaultToOne = false;
    
    /**
     * Returns the value of vDefaultToOne.
     * @return the current value of vDefaultToOne.
     */
    public boolean isVDefaultToOne() {
        return this.vDefaultToOne;
    }
    /**
     * Sets vDefaultToOne.
     * @param value the new value of vDefaultToOne.
     */
    public void setVDefaultToOne(final boolean value) {
        this.vDefaultToOne = value;
    }
    
    /**
     * Returns a normalized v result according to the current default.
     * @param val the unnormalized v-value.
     * @return the normalized v-value.
     */
    protected int normalizedV(final int val) {
        if (val > 0) {
            return val;
        } else if (isVDefaultToOne()) {
            return 1;
        } else {
            return 0;
        }
    }
    
    /* --- The minimal set of methods each derived class must implement. */
    
    public abstract int dim();

    public abstract int size();

    public abstract IteratorAdapter<T> elements();

    public abstract boolean hasElement(T D);
    
    public abstract IteratorAdapter<Integer> indices();

    public abstract boolean hasIndex(int i);
    
    public abstract boolean definesOp(int i, T D);

    public abstract T op(int i, T D);

    public abstract boolean definesV(int i, int j, T D);

    public abstract int v(int i, int j, T D);


    /* --- Default implementations for the rest of the interface. */

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#tabularDisplay()
     */
    public String tabularDisplay() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        final StringBuffer buf = new StringBuffer(500);
        final List<Integer> idcs = new IndexList(this);
        
        int elmSize = 4;
        int vSize = 4;
        for (final T D: this.elements()) {
            elmSize = Math.max(elmSize, D.toString().length());
            for (int i = 0; i < dim(); ++i) {
                int i1 = ((Integer) idcs.get(i)).intValue();
                int i2 = ((Integer) idcs.get(i+1)).intValue();
                vSize = Math.max(vSize, String.valueOf(v(i1, i2, D)).length());
            }
        }
        
        buf.append(Strings.rjust("D", elmSize));
        buf.append(" |");

        for (int i = 0; i <= dim(); ++i) {
            buf.append(" ");
            buf.append(Strings.rjust("op" + idcs.get(i), elmSize));
        }
        buf.append(" |");
        for (int i = 0; i < dim(); ++i) {
            buf.append(" ");
            buf.append(Strings.rjust("v" + idcs.get(i) + idcs.get(i+1), vSize));
        }
        buf.append("\n");

        buf.append(Strings.rjust("", elmSize, '-'));
        buf.append("-+");
        for (int i = 0; i <= dim(); ++i) {
            buf.append(Strings.rjust("", elmSize+1, '-'));
        }
        buf.append("-+");
        for (int i = 0; i < dim(); ++i) {
            buf.append(Strings.rjust("", vSize+1, '-'));
        }
        buf.append("-\n");

        for (final T D: this.elements()) {
            buf.append(Strings.rjust(D.toString(), elmSize));
            buf.append(" |");
            for (int i = 0; i <= dim(); i++) {
            	final int ii = idcs.get(i);
            	final String s = definesOp(ii, D) ? ("" + op(ii, D)) : "-";
                buf.append(" ");
                buf.append(Strings.rjust(s, elmSize));
            }
            buf.append(" |");
            for (int i = 0; i < dim(); i++) {
                final int i1 = idcs.get(i);
                final int i2 = idcs.get(i+1);
                final String s =
                		definesV(i1, i2, D) ? ("" + v(i1, i2, D)) : "-";
                buf.append(" ");
                buf.append(Strings.rjust(s, vSize));
            }
            buf.append("\n");
        }
        return buf.toString();
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#hasStandardIndexSet()
     */
    public boolean hasStandardIndexSet() {
    	final List<Integer> idcs = new IndexList(this);
    	for (int i = 0; i < idcs.size(); ++i) {
    		if (idcs.get(i) != i) {
    			return false;
    		}
    	}
    	return true;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#isComplete()
     */
    public boolean isComplete() {
        final List<Integer> idcs = new IndexList(this);
        for (final T D: elements()) {
            for (int i = 0; i < idcs.size()-1; ++i) {
                final int ii = idcs.get(i);
                if (!definesOp(ii, D)) {
                    return false;
                }
                for (int j = i+1; j < idcs.size(); ++j) {
                    final int jj = idcs.get(j);
                    if (!definesV(ii, jj, D)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#r(int, int, java.lang.Object)
     */
    public int r(int i, int j, final T D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }

        T Di = D;
        int k = 0;
        while (true) {
            if (definesOp(i, Di)) {
                Di = op(i, Di);
            }
            if (definesOp(j, Di)) {
                Di = op(j, Di);
            }
            k++;
            if (Di.equals(D)) {
                break;
            }
        }
        return k;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#m(int, int, java.lang.Object)
     */
    public int m(final int i, final int j, final T D) {
        return v(i, j, D) * r(i, j, D);
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#numberOfOrbits(java.util.Collection)
     */
    public int numberOfOrbits(final List<Integer> indices) {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
    		throw new UnsupportedOperationException("symbol must be finite");
    	}
        
    	int count = 0;
    	for (final DSPair<T> e: new Traversal<T>(this, indices, elements())) {
    		if (e.getIndex() < 0) {
    			++count;
    		}
    	}
    	return count;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isConnected()
     */
    public boolean isConnected() {
    	return numberOfOrbits(new IndexList(this)) <= 1;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#orbitRepresentatives(java.util.List)
     */
    public IteratorAdapter<T> orbitReps(final List<Integer> indices) {
		return new FilteredIterator<T, DSPair<T>>(
		        new Traversal<T>(this, indices, elements()))
		{
			public T filter(final DSPair<T> e) {
				if (e.getIndex() < 0) {
					return e.getElement();
				} else {
					return null;
				}
			}
		};
	}

    /* (non-Javadoc)
	 * @see javaDSym.DelaneySymbol#elementsOfOrbit(java.util.List, java.lang.Object)
	 */
    public IteratorAdapter<T> orbit(final List<Integer> indices, final T seed) {
    	return new FilteredIterator<T, DSPair<T>>(
    	        new Traversal<T>(this, indices, seed)) {
    		public T filter(final DSPair<T> x) {
    			return x.getElement();
    		}
    	};
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#partialOrientation(java.util.List, java.util.Iterator)
     */
    public Map<T, Integer> partialOrientation(
            final List<Integer> indices, final Iterator<T> seeds)
    {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
    		throw new UnsupportedOperationException("symbol must be finite");
    	}
        
        final HashMap<T, Integer> or = new HashMap<T, Integer>();
        for (final DSPair<T> e: new Traversal<T>(this, indices, seeds)) {
            final int i = e.getIndex();
            final T D = e.getElement();
            if (i < 0) {
                or.put(D, 1);
            } else {
                or.put(D, -or.get(op(i, D)));
            }
        }
        return or;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#partialOrientation()
     */
    public Map<T, Integer> partialOrientation() {
        return partialOrientation(new IndexList(this), this.elements());
    }

    /**
     * Determines if an orbit is weakly oriented and/or loopless. Returns an
     * number between 0 and 3 which is 2 or larger exactly if the orbit is
     * weakly oriented and odd exactly if the orbit is loopless. In particular,
     * the orbit is oriented exactly if the result returned is 3.
     * 
     * @param indices the indices to use.
     * @param seed the seed for the orbit.
     * @return an integer encoding the result.
     */
    private int orbitOrientation(final List<Integer> indices, final T seed) {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        final Map<T, Integer> or =
                partialOrientation(indices, Iterators.singleton(seed));
        boolean weaklyOriented = true;
        boolean loopless = true;
        for (final T D: orbit(indices, seed)) {
            for (final int i: indices) {
                final T Di = op(i, D);
                if (D.equals(Di)) {
                    loopless = false;
                } else if (or.get(D).equals(or.get(Di))) {
                    weaklyOriented = false;
                }
            }
        }
        return (weaklyOriented ? 2 : 0) + (loopless ? 1 : 0);
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isOriented(java.util.List, java.lang.Object)
     */
    public boolean orbitIsOriented(final List<Integer> indices, final T seed) {
        return orbitOrientation(indices, seed) == 3;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isOriented()
     */
    public boolean isOriented() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
    		throw new UnsupportedOperationException("symbol must be finite");
    	}

        final List<Integer> idcs = new IndexList(this);
    	for (final T D: this.orbitReps(idcs)) {
            if (!orbitIsOriented(idcs, D)) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isLoopless(java.util.List, java.lang.Object)
     */
    public boolean orbitIsLoopless(final List<Integer> indices, final T seed) {
        return orbitOrientation(indices, seed) % 2 == 1;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isLoopless()
     */
    public boolean isLoopless() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        final List<Integer> idcs = new IndexList(this);
        for (final T D: this.orbitReps(idcs)) {
            if (!orbitIsLoopless(idcs, D)) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isWeaklyOriented(java.util.List,
     *      java.lang.Object)
     */
    public boolean orbitIsWeaklyOriented(
            final List<Integer> indices, final T seed) {
        return orbitOrientation(indices, seed) >= 2;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isWeaklyOriented()
     */
    public boolean isWeaklyOriented() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        final List<Integer> idcs = new IndexList(this);
        for (final T D: this.orbitReps(idcs)) {
            if (!orbitIsWeaklyOriented(idcs, D)) {
                return false;
            }
        }
        return true;
    }

    // --- Caches for invariant and map from original to canonical element names
    private NiftyList<Integer> _invariant = null;
    private Map<T, Integer> original2canonical;
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#invariant()
     */
    public NiftyList<Integer> invariant() {
    	if (this._invariant != null) {
    		return this._invariant;
    	}
    	
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        if (!isConnected()) {
            // --- For non-connected symbols, collect invariants for components and sort.
            final List<NiftyList<Integer>> invariants =
                    new ArrayList<NiftyList<Integer>>();
            final IndexList idcs = new IndexList(this);
            for (final T D: this.orbitReps(idcs)) {
                final DelaneySymbol<Integer> sub =
                        new Subsymbol<T>(this, idcs, D).flat();
                invariants.add(sub.invariant());
            }
            Collections.sort(invariants);
            final List<Integer> result = new LinkedList<Integer>();
            for (final List<Integer> inv: invariants) {
                result.addAll(inv);
            }
            this._invariant = new NiftyList<Integer>(result);
            return this._invariant;
        }
        
        /* --- Preparations. */
        final List<Integer> idcs = new IndexList(this);

        int[] best = null;
        int[] current = new int[(size() + 1) * (4 * dim() + 3)];
        int bestk = 0;
        Map<T, Integer> bestMap = null;

        /* --- Map indices. */
        HashMap<Integer, Integer> i2pos = new HashMap<Integer, Integer>();
        for (int i = 0; i <= dim(); ++i) {
            i2pos.put(idcs.get(i), i);
        }
        
        /* --- Try each element in turn as the seed for a traversal. */
        for (final T seed: this.elements()) {
            /* --- Elements will be numbered in the order they appear. */
            final HashMap<T, Integer> old2new = new HashMap<T, Integer>();
            int nextE = 1;

            /* --- Follow the traversal and create a protocol. */
            int k = 0;
            
            for (final DSPair<T> e: new Traversal<T>(this, idcs, seed, true)) {
                final T D = e.getElement();
                final int i = e.getIndex();
                
                /* --- Determine a running number E for the target element D. */
                final int E;
                final boolean elementIsNew;
                if (!old2new.containsKey(D)) {
                	/* --- Element D is encountered for the first time. */
                    elementIsNew = true;
                    E = nextE++;
                    old2new.put(D, new Integer(E));
                } else {
                	/* --- Element D already has a number. */
                    elementIsNew = false;
                    E = old2new.get(D);
                }
                
                /* --- Add the mapped edge index to the protocol. */
                int ip = i;
                if (i >= 0) {
                    ip = i2pos.get(i);
                }
                if (best != null) {
                    if (ip > best[k]) {
                    	/* --- We've seen a better (smaller) protocol. */
                        break;
                    } else if (ip < best[k]) {
                    	/* --- This protocol is smallest so far. Keeping it. */
                        best = null;
                    }
                }
                current[k++] = ip;
                
                /* --- If we're not at the seed, add the source element. */
                if (i >= 0) {
					final int Ei = old2new.get(op(i, D));
					if (best != null) {
						if (Ei > best[k]) {
							break;
						} else if (Ei < best[k]) {
							best = null;
						}
					}
					current[k++] = Ei;
				}
                
                /* --- Add the target element. */
				if (best != null) {
					if (E > best[k]) {
						break;
					} else if (E < best[k]) {
						best = null;
					}
				}
                current[k++] = E;
                
                /* --- For unseen elements, add branching numbers to protocol. */
                if (elementIsNew) {
                    boolean bad = false;
                    for (int m = 0; m < dim(); ++m) {
                        final int j0 = idcs.get(m);
                        final int j1 = idcs.get(m + 1);
                        final int v = definesV(j0, j1, D) ? v(j0, j1, D) : 0;
                        if (best != null) {
                            if (v > best[k] || (v == 0 && best[k] != 0)) {
                                bad = true;
                                break;
                            } else if (v < best[k] || (v != 0 && best[k] == 0)) {
                                best = null;
                            }
                        }
                        current[k++] = v;
                    }
                    /* --- We've already seen a lexicographically smaller protocol. */
                    if (bad) {
                        break;
                    }
                }
            }
            
            if (best == null) {
                /* --- The new protocol is lexicographically smallest so far. */
                best = (int[]) current.clone();
                bestk = k;
                bestMap = old2new;
            }
        }
        
        // --- remember the mapping from original to canonical
        this.original2canonical = Collections.unmodifiableMap(bestMap);
        
        /* --- Convert the best protocol into a list. */
        final List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < bestk; ++i) {
            result.add(best[i]);
        }
        
        /* --- Cache and return it. */
        this._invariant = new NiftyList<Integer>(result);
        return this._invariant;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals()
     */
    public boolean equals(Object other) {
    	if (other instanceof DelaneySymbol) {
    	    @SuppressWarnings("unchecked")
            final DelaneySymbol<T> ds = (DelaneySymbol<T>) other;
    	    if (ds.dim() == this.dim() && ds.size() == this.size()) {
    	        return this.invariant().equals(ds.invariant());
    	    }
    	}
    	return false;
    }
    
    /* (non-Javadoc)
     * @see int java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final DelaneySymbol<T> other) {
        if (this.dim() != other.dim()) {
            return this.dim() - other.dim();
        } else {
            return this.invariant().compareTo(other.invariant());
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.invariant().hashCode();
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#canonical()
     */
    public DelaneySymbol<Integer> canonical() {
    	int op[][] = new int[dim() + 1][size() + 1];
    	int v[][] = new int[dim()][size() + 1];
    	final List<Integer> invariant = invariant();
        int offset = 0;
    	
    	int maxD = 0;
    	int k = 0;
    	while (k < invariant.size()) {
    		int i = invariant.get(k++);
            if (i < 0) {
                offset = maxD;
            }
    		int D = invariant.get(k++) + offset;
    		int Di = 0;
    		if (i >= 0) {
    			Di = D;
    			D = invariant.get(k++) + offset;
    			op[i][D] = Di;
    			op[i][Di] = D;
            }
    		if (D > maxD) {
    			for (i = 0; i < dim(); ++i) {
    	    		v[i][D] = invariant.get(k++);
    	    		maxD = D;
    			}
    		}
    	}
    	return new DSymbol(op, v);
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#getMapToCanonical()
     */
    public Map<T, Integer> getMapToCanonical() {
        if (!this.isConnected()) {
            throw new UnsupportedOperationException("symbol must be connected");
        }
        this.invariant();
        return this.original2canonical;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#minimal()
     */
    public boolean isMinimal() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        /* --- Determine classes of elements to be mapped to a common one. */
        final TypedPartition<T> P = new TypedPartition<T>(this);

        T D0 = null;
        for (final T D: elements()) {
            if (D0 == null) {
                D0 = D;
            } else {
                if (P.unite(D0, D)) {
                    return false;
                }
            }
        }
        return true;
    }
        
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#minimal()
     */
    public DelaneySymbol<Integer> minimal() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        /* --- Determine classes of elements to be mapped to a common one. */
    	final TypedPartition<T> P = new TypedPartition<T>(this);
    	P.uniteAll();
    	
    	/* --- Map elements of old to elements of new symbol. */
    	final Map<T, Integer> old2new = new HashMap<T, Integer>();
    	final List<T> new2old = new ArrayList<T>();
    	for (final T D: elements()) {
    		final T E = P.find(D);
    		if (!old2new.containsKey(E)) {
                new2old.add(E);
    			old2new.put(E, new2old.size());
    		}
    		old2new.put(D, old2new.get(E));
    	}
    	
    	/** --- Specify operations for new symbol. */
    	int newSize = new2old.size();
    	int op[][] = new int[dim() + 1][newSize + 1];
    	int v[][] = new int[dim()][newSize + 1];
    	final List<Integer> idcs = new IndexList(this);
   	
    	for (int D = 1; D <= newSize; ++D) {
    		final T E = new2old.get(D-1);
    		for (int i = 0; i <= dim(); ++i) {
    		    int ii = idcs.get(i);
    			op[i][D] = old2new.get(op(ii, E));
    			if (i < dim()) {
    				v[i][D] = 1;
    			}
    		}
    	}
    	
    	/** --- Specify branching limits for new symbol. */
    	DSymbol tmp = new DSymbol(op, v);
    	
    	for (int i = 0; i < dim(); ++i) {
    		final List<Integer> idcsI = new IndexList(i, i+1);
    		for (final int D: tmp.orbitReps(idcsI)) {
        		final T E = new2old.get(D-1);
    		    final int ii = idcs.get(i);
    		    final int ii1 = idcs.get(i+1);
        		final int m = this.m(ii, ii1, E);
        		final int r = tmp.r(i, i+1, D);
        		final int b = m / r;
        		for (final int C: tmp.orbit(idcsI, D)) {
        			v[i][C] = b;
        		}
    		}
    	}
    	
    	/* --- make and return new symbol. */
    	return new DSymbol(op, v);
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#flat()
     */
    public DelaneySymbol<Integer> flat() {
        return new DSymbol(this);
    }
    
    /**
     * Helper method. Computes curvature summands for an index pair.
     * @param i first index.
     * @param j second index.
     * @return the sum of summands for this index pair.
     */
    private Rational curvatureSummands(int i, int j) {
        Rational result = new Fraction(0, 1);

		for (final T D: orbitReps(new IndexList(i, j))) {
		    int s;
			if (orbitIsOriented(new IndexList(i, j), D)) {
				s = 2;
			} else {
				s = 1;
			}
			result = (Rational) result.plus(new Fraction(s, v(i, j, D)));
		}
		return result;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#curvature2D()
     */
    public Rational curvature2D() {
    	if (dim() != 2) {
    		String text = "symbol must be 2-dimensional";
    		throw new UnsupportedOperationException(text);
    	}
    	
    	Rational result = new Fraction(-size(), 1);
    	final Iterator<Integer> idcs = indices();
    	int i = idcs.next();
    	int j = idcs.next();
    	int k = idcs.next();
    	result = (Rational) result.plus(curvatureSummands(i, j));
    	result = (Rational) result.plus(curvatureSummands(i, k));
    	result = (Rational) result.plus(curvatureSummands(j, k));

    	return result;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isSpherical2D()
     */
    public boolean isSpherical2D() {
        if (dim() != 2) {
            throw new IllegalArgumentException("symbol must be 2-dimensional");
        }
        if (!isConnected()) {
            throw new IllegalArgumentException("symbol must be connected");
        }
        if (curvature2D().isPositive()) {
            final DSymbol dso = new DSymbol(orientedCover());
            final List<Integer> degrees = new ArrayList<Integer>();
            for (int i = 0; i < 2; ++i) {
                for (int j = i+1; j <= 2; ++j) {
                    for (final int D: dso.orbitReps(new IndexList(i, j))) {
                        final int v = dso.v(i, j, D);
                        if (v > 1) {
                            degrees.add(new Integer(v));
                        }
                    }
                }
            }
            if (degrees.size() == 1) {
                return false;
            } else if (degrees.size() == 2) {
                return degrees.get(0).equals(degrees.get(1));
            } else if (degrees.size() == 3 || degrees.size() == 0) {
                return true;
            } else {
                throw new RuntimeException("this should not happen");
            }
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#sphericalGroupSize2D()
     */
    public int sphericalGroupSize2D() {
        if (!isSpherical2D()) {
            throw new NonSphericalException("symbol must be spherical");
        }
        final Rational size =
                (Rational) new Fraction(4, 1).dividedBy(curvature2D());
        return size.numerator().intValue();
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isLocallyEuclidean3D()
     */
    public boolean isLocallyEuclidean3D() {
    	if (dim() != 3) {
    		String text = "symbol must be 3-dimensional";
    		throw new UnsupportedOperationException(text);
    	}
    	
    	for (int k = 0; k <= dim(); ++k) {
        	final List<Integer> idcs = new IndexList(this);
    		idcs.remove(k);
    		for (final T D: orbitReps(idcs)) {
    			final DelaneySymbol<T> sub = new Subsymbol<T>(this, idcs, D);
    			if (! sub.isSpherical2D()) {
    				return false;
    			}
    		}
    	}
    	return true;
    }

	/**
	 * For two-dimensional symbols, computes the Conway orbifold symbol.
	 * 
	 * @return the orbifold symbol as a string.
	 */
	public String orbifoldSymbol2D() {
        if (dim() != 2) {
            throw new IllegalArgumentException("symbol must be 2-dimensional");
        }
        if (!isConnected()) {
            throw new IllegalArgumentException("symbol must be connected");
        }
		final List<String> cones = new ArrayList<String>();
		final List<String> corners = new ArrayList<String>();
		final int x[] = new int[3];
		final Iterator<Integer> indices = indices();
		x[0] = indices.next();
		x[1] = indices.next();
		x[2] = indices.next();
		
		for (int k = 0; k < 3; ++k) {
			final int i = (k + 1) % 3;
			final int j = (k + 2) % 3;
			final List<Integer> idcs = new IndexList(x[i], x[j]);
			for (final T D: orbitReps(idcs)) {
				final int v = v(i, j, D);
				if (v > 1) {
					if (orbitIsLoopless(idcs, D)) {
						cones.add(String.valueOf(v));
					} else {
						corners.add(String.valueOf(v));
					}
				}
			}
		}
		Collections.sort(cones);
		Collections.reverse(cones);
		Collections.sort(corners);
		Collections.reverse(corners);
		
        final StringBuffer buf = new StringBuffer(20);
        if (cones.isEmpty() && corners.isEmpty()) {
        	buf.append('1');
        }
        for (final String cone: cones) {
        	buf.append(cone);
        }
        if (!isLoopless()) {
        	buf.append('*');
        }
        for (final String corner: corners) {
        	buf.append(corner);
        }
        if (!isWeaklyOriented()) {
        	buf.append('x');
        }
        return buf.toString();
	}
	
    /**
     * Returns the oriented cover as a flat symbol such that the covering
     * morphism maps the first element of this symbol onto the element 1.
     * 
     * @return the oriented cover of this symbol.
     */
    public DelaneySymbol<Integer> orientedCover() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }

        if (isOriented()) {
            return this.flat();
        } else {
            final int n = size();
            final int d = dim();
            final int op[][] = new int[d + 1][2 * n + 1];
            final int v[][] = new int[d][2 * n + 1];
            final DelaneySymbol<Integer> ds = this.flat();
            final Map<Integer, Integer> ori = ds.partialOrientation();

            for (int i = 0; i <= d; ++i) {
                for (int D = 1; D <= n; ++D) {
                    if (!ds.definesOp(i, D)) {
                        continue;
                    }
                    final int Di = ds.op(i, D);
                    if (ori.get(D).equals(ori.get(Di))) {
                        op[i][D] = Di + n;
                        op[i][Di] = D + n;
                        op[i][D + n] = Di;
                        op[i][Di + n] = D;
                    } else {
                        op[i][D] = Di;
                        op[i][Di] = D;
                        op[i][D + n] = Di + n;
                        op[i][Di + n] = D + n;
                    }
                    if (i < d) {
                        v[i][D] = v[i][D + n] = ds.v(i, i+1, D);
                    }
                }
            }
            return new DSymbol(op, v);
        }
    }
}

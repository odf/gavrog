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

package org.gavrog.joss.dsyms.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.NiftyList;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Rational;


/**
 * @author Olaf Delgado
 * @version $Id: DelaneySymbol.java,v 1.10 2008/04/07 00:56:41 odf Exp $
 */
public abstract class DelaneySymbol implements Comparable {

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
    public void setVDefaultToOne(boolean value) {
        this.vDefaultToOne = value;
    }
    
    /**
     * Returns a normailized v result according to the current default.
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
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#dim()
     */
    public abstract int dim();

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#size()
     */
    public abstract int size();

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#elements()
     */
    public abstract Iterator elements();

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isElement(java.lang.Object)
     */
    public abstract boolean hasElement(Object D);
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#indices()
     */
    public abstract Iterator indices();

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isIndex(int)
     */
    public abstract boolean hasIndex(int i);
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#op(int, java.lang.Object)
     */
    public abstract boolean definesOp(int i, Object D);

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#op(int, java.lang.Object)
     */
    public abstract Object op(int i, Object D);

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#v(int, int, java.lang.Object)
     */
    public abstract boolean definesV(int i, int j, Object D);

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#v(int, int, java.lang.Object)
     */
    public abstract int v(int i, int j, Object D);


    /* --- Default implementations for the rest of the interface. */

    /**
     * Helper method: adjusts a string to a given length by filling from the
     * left. If the string is already longer, it is returned unchanged.
     * @param s the string to adjust.
     * @param n the new length.
     * @param fill the fill character to use. 
     * @return the adjusted string.
     */
    private String rjust(String s, int n, char fill) {
        if (s.length() >= n) {
            return s;
        } else {
            StringBuffer buf = new StringBuffer(n);
            for (int i = n - s.length(); i > 0; --i) {
                buf.append(fill);
            }
            buf.append(s);
            return buf.toString();
        }
    }
    
    /**
     * Helper method: adjusts a string to a given length by filling with blanks
     * from the left. If the string is already longer, it is returned unchanged.
     * @param s the string to adjust.
     * @param n the new length.
     * @return the adjusted string.
     */
    private String rjust(String s, int n) {
        return rjust(s, n, ' ');
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#tabularDisplay()
     */
    public String tabularDisplay() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        StringBuffer buf = new StringBuffer(500);
        List idcs = new IndexList(this);
        
        int elmSize = 4;
        int vSize = 4;
        Iterator elms = elements();
        while (elms.hasNext()) {
            Object D = elms.next();
            elmSize = Math.max(elmSize, D.toString().length());
            for (int i = 0; i < dim(); ++i) {
                int i1 = ((Integer) idcs.get(i)).intValue();
                int i2 = ((Integer) idcs.get(i+1)).intValue();
                vSize = Math.max(vSize, String.valueOf(v(i1, i2, D)).length());
            }
        }
        
        buf.append(rjust("D", elmSize));
        buf.append(" |");

        for (int i = 0; i <= dim(); ++i) {
            buf.append(" ");
            buf.append(rjust("op" + idcs.get(i), elmSize));
        }
        buf.append(" |");
        for (int i = 0; i < dim(); ++i) {
            buf.append(" ");
            buf.append(rjust("v" + idcs.get(i) + idcs.get(i+1), vSize));
        }
        buf.append("\n");

        buf.append(rjust("", elmSize, '-'));
        buf.append("-+");
        for (int i = 0; i <= dim(); ++i) {
            buf.append(rjust("", elmSize+1, '-'));
        }
        buf.append("-+");
        for (int i = 0; i < dim(); ++i) {
            buf.append(rjust("", vSize+1, '-'));
        }
        buf.append("-\n");

        elms = elements();
        while (elms.hasNext()) {
            Object D = elms.next();
            buf.append(rjust(D.toString(), elmSize));
            buf.append(" |");
            for (int i = 0; i <= dim(); i++) {
                Object Di = op(((Integer) idcs.get(i)).intValue(), D);
                String s = (Di == null) ? "-" : Di.toString();
                buf.append(" ");
                buf.append(rjust(s, elmSize));
            }
            buf.append(" |");
            for (int i = 0; i < dim(); i++) {
                buf.append(" ");
                int i1 = ((Integer) idcs.get(i)).intValue();
                int i2 = ((Integer) idcs.get(i+1)).intValue();
                int v = v(i1, i2, D);
                String s = (v == 0) ? "-" : String.valueOf(v);
                buf.append(rjust(s, vSize));
            }
            buf.append("\n");
        }
        return buf.toString();
    }
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#hasStandardIndexSet()
     */
    public boolean hasStandardIndexSet() {
    	List idcs = new IndexList(this);
    	for (int i = 0; i < idcs.size(); ++i) {
    		if (((Integer) idcs.get(i)).intValue() != i) {
    			return false;
    		}
    	}
    	return true;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#isComplete()
     */
    public boolean isComplete() {
        final List idcs = new IndexList(this);
        for (final Iterator elms = elements(); elms.hasNext();) {
            final Object D = elms.next();
            for (int i = 0; i < idcs.size()-1; ++i) {
                final int ii = ((Integer) idcs.get(i)).intValue();
                if (!definesOp(ii, D)) {
                    return false;
                }
                for (int j = i+1; j < idcs.size(); ++j) {
                    final int jj = ((Integer) idcs.get(j)).intValue();
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
    public int r(int i, int j, Object D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }

        Object Di = D;
        int k = 0;
        while (true) {
            if (op(i, Di) != null) {
                Di = op(i, Di);
            }
            if (op(j, Di) != null) {
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
    public int m(int i, int j, Object D) {
        return v(i, j, D) * r(i, j, D);
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#numberOfOrbits(java.util.Collection)
     */
    public int numberOfOrbits(List indices) {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
    		throw new UnsupportedOperationException("symbol must be finite");
    	}
    	Traversal trav = new Traversal(this, indices, elements());
    	int count = 0;
    	while (trav.hasNext()) {
    		DSPair e = (DSPair) trav.next();
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
    public Iterator orbitReps(List indices) {
		return new FilteredIterator(new Traversal(this, indices, elements())) {
			public Object filter(Object x) {
				DSPair e = (DSPair) x;
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
    public Iterator orbit(List indices, Object seed) {
    	return new FilteredIterator(new Traversal(this, indices, seed)) {
    		public Object filter(Object x) {
    			return ((DSPair) x).getElement();
    		}
    	};
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#partialOrientation(java.util.List, java.util.Iterator)
     */
    public Map partialOrientation(List indices, Iterator seeds) {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
    		throw new UnsupportedOperationException("symbol must be finite");
    	}
        Traversal trav = new Traversal(this, indices, seeds);
        HashMap or = new HashMap();
        while (trav.hasNext()) {
            DSPair e = (DSPair) trav.next();
            int i = e.getIndex();
            Object D = e.getElement();
            if (i < 0) {
                or.put(D, new Integer(1));
            } else {
                Object Di = op(i, D);
                int x = ((Integer) or.get(Di)).intValue();
                or.put(D, new Integer(-x));
            }
        }
        return or;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#partialOrientation()
     */
    public Map partialOrientation() {
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
    private int orbitOrientation(List indices, Object seed) {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        final Map or = partialOrientation(indices, Iterators.singleton(seed));
        final Iterator elms = orbit(indices, seed);
        boolean weaklyOriented = true;
        boolean loopless = true;
        while (elms.hasNext()) {
            final Object D = elms.next();
            for (int k = 0; k < indices.size(); ++k) {
                final int i = ((Integer) indices.get(k)).intValue();
                final Object Di = op(i, D);
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
    public boolean orbitIsOriented(List indices, Object seed) {
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
    	List idcs = new IndexList(this);
        Iterator reps = this.orbitReps(idcs);
        while (reps.hasNext()) {
            if (!orbitIsOriented(idcs, reps.next())) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isLoopless(java.util.List, java.lang.Object)
     */
    public boolean orbitIsLoopless(List indices, Object seed) {
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
        List idcs = new IndexList(this);
        Iterator reps = this.orbitReps(idcs);
        while (reps.hasNext()) {
            if (!orbitIsLoopless(idcs, reps.next())) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#isWeaklyOriented(java.util.List,
     *      java.lang.Object)
     */
    public boolean orbitIsWeaklyOriented(List indices, Object seed) {
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
        List idcs = new IndexList(this);
        Iterator reps = this.orbitReps(idcs);
        while (reps.hasNext()) {
            if (!orbitIsWeaklyOriented(idcs, reps.next())) {
                return false;
            }
        }
        return true;
    }

    // --- Caches for invariant and map from original to canonical element names
    private NiftyList _invariant = null;
    private Map original2canonical;
    
    /* (non-Javadoc)
     * @see javaDSym.DelaneySymbol#invariant()
     */
    public NiftyList invariant() {
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
            final List invariants = new ArrayList();
            final IndexList idcs = new IndexList(this);
            for (final Iterator iter = this.orbitReps(idcs); iter.hasNext();) {
                final DelaneySymbol sub = new Subsymbol(this, idcs, iter.next()).flat();
                invariants.add(sub.invariant());
            }
            Collections.sort(invariants);
            final List result = new LinkedList();
            for (final Iterator iter = invariants.iterator(); iter.hasNext();) {
                result.addAll((List) iter.next());
            }
            this._invariant = new NiftyList(result);
            return this._invariant;
        }
        
        /* --- Preparations. */
        int[] best = null;
        int[] current = new int[(size() + 1) * (4 * dim() + 3)];
        int bestk = 0;
        List idcs = new IndexList(this);
        Map bestMap = null;

        /* --- Map indices. */
        HashMap i2pos = new HashMap();
        for (int i = 0; i <= dim(); ++i) {
            i2pos.put(idcs.get(i), new Integer(i));
        }
        
        /* --- Try each element in turn as the seed for a traversal. */
        Iterator elms = elements();
        while (elms.hasNext()) {
            Object seed = elms.next();

            /* --- Initialize the traversal. */
            Traversal trav = new Traversal(this, idcs, seed, true);
            
            /* --- Elements will be numbered in the order they appear. */
            HashMap old2new = new HashMap();
            int nextE = 1;

            /* --- Follow the traversal and create a protocol. */
            int k = 0;
            while (trav.hasNext()) {
            	/* --- Retrieve the next edge. */
                DSPair e = (DSPair) trav.next();
                Object D = e.getElement();
                int i = e.getIndex();
                
                /* --- Determine a running number E for the target element D. */
                int E;
                boolean elementIsNew;
                Integer tmp = (Integer) old2new.get(D);
                if (tmp == null) {
                	/* --- Element D is encountered for the first time. */
                    elementIsNew = true;
                    E = nextE++;
                    old2new.put(D, new Integer(E));
                } else {
                	/* --- Element D already has a number. */
                    elementIsNew = false;
                    E = tmp.intValue();
                }
                
                /* --- Add the mapped edge index to the protocol. */
                int ip = i;
                if (i >= 0) {
                    ip = ((Integer) i2pos.get(new Integer(i))).intValue();
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
					int Ei = ((Integer) old2new.get(op(i, D))).intValue();
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
                        int j0 = ((Integer) idcs.get(m)).intValue();
                        int j1 = ((Integer) idcs.get(m + 1)).intValue();
                        int v = definesV(j0, j1, D) ? v(j0, j1, D) : 0;
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
        final ArrayList result = new ArrayList();
        for (int i = 0; i < bestk; ++i) {
            result.add(new Integer(best[i]));
        }
        
        /* --- Cache and return it. */
        this._invariant = new NiftyList(result);
        return this._invariant;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals()
     */
    public boolean equals(Object other) {
    	if (other instanceof DelaneySymbol) {
    	    final DelaneySymbol ds = (DelaneySymbol) other;
    	    if (ds.dim() == this.dim() && ds.size() == this.size()) {
    	        return this.invariant().equals(ds.invariant());
    	    }
    	}
    	return false;
    }
    
    /* (non-Javadoc)
     * @see int java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object arg) {
        if (!(arg instanceof DelaneySymbol)) {
            throw new IllegalArgumentException("argument must be a DelaneySymbol");
        }
        final DelaneySymbol other = (DelaneySymbol) arg;
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
    public DelaneySymbol canonical() {
    	int op[][] = new int[dim() + 1][size() + 1];
    	int v[][] = new int[dim()][size() + 1];
    	final List invariant = invariant();
        int offset = 0;
    	
    	int maxD = 0;
    	int k = 0;
    	while (k < invariant.size()) {
    		int i = ((Integer) invariant.get(k++)).intValue();
            if (i < 0) {
                offset = maxD;
            }
    		int D = ((Integer) invariant.get(k++)).intValue() + offset;
    		int Di = 0;
    		if (i >= 0) {
    			Di = D;
    			D = ((Integer) invariant.get(k++)).intValue() + offset;
    			op[i][D] = Di;
    			op[i][Di] = D;
            }
    		if (D > maxD) {
    			for (i = 0; i < dim(); ++i) {
    	    		v[i][D] = ((Integer) invariant.get(k++)).intValue();
    	    		maxD = D;
    			}
    		}
    	}
    	return new DSymbol(op, v);
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#getMapToCanonical()
     */
    public Map getMapToCanonical() {
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
        TypedPartition P = new TypedPartition(this);

        Object D0 = null;
        for (final Iterator elms = elements(); elms.hasNext();) {
            final Object D = elms.next();
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
    public DelaneySymbol minimal() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }
        
        /* --- Determine classes of elements to be mapped to a common one. */
    	TypedPartition P = new TypedPartition(this);
    	P.uniteAll();
    	
    	/* --- Map elements of old to elements of new symbol. */
    	HashMap old2new = new HashMap();
    	Object new2old[] = new Object[size() + 1];
    	int k = 0;
    	Iterator elms = elements();
    	while (elms.hasNext()) {
    		Object D = elms.next();
    		Object E = P.find(D);
    		if (!old2new.containsKey(E)) {
    			++k;
    			old2new.put(E, new Integer(k));
    			new2old[k] = E;
    		}
    		old2new.put(D, old2new.get(E));
    	}
    	
    	/** --- Specify operations for new symbol. */
    	int newSize = k;
    	int op[][] = new int[dim() + 1][newSize + 1];
    	int v[][] = new int[dim()][newSize + 1];
    	List idcs = new IndexList(this);
   	
    	for (int D = 1; D <= newSize; ++D) {
    		Object E = new2old[D];
    		for (int i = 0; i <= dim(); ++i) {
    		    int ii = ((Integer) idcs.get(i)).intValue();
    			op[i][D] = ((Integer) old2new.get(op(ii, E))).intValue();
    			if (i < dim()) {
    				v[i][D] = 1;
    			}
    		}
    	}
    	
    	/** --- Specify branching limits for new symbol. */
    	DSymbol tmp = new DSymbol(op, v);
    	
    	for (int i = 0; i < dim(); ++i) {
    		List idcsI = new IndexList(i, i+1);
    		Iterator reps = tmp.orbitReps(idcsI);
    		while (reps.hasNext()) {
    			Object D = reps.next();
        		Object E = new2old[((Integer) D).intValue()];
    		    int ii = ((Integer) idcs.get(i)).intValue();
    		    int ii1 = ((Integer) idcs.get(i+1)).intValue();
        		int m = this.m(ii, ii1, E);
        		int r = tmp.r(i, i+1, D);
        		int b = m / r;
        		Iterator orb = tmp.orbit(idcsI, D);
        		while (orb.hasNext()) {
        			int C = ((Integer) orb.next()).intValue();
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
    public DelaneySymbol flat() {
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

		Iterator reps = orbitReps(new IndexList(i, j));
		while (reps.hasNext()) {
			Object D = reps.next();
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
    	Iterator idcs = indices();
    	int i = ((Integer) idcs.next()).intValue();
    	int j = ((Integer) idcs.next()).intValue();
    	int k = ((Integer) idcs.next()).intValue();
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
            final List degrees = new ArrayList();
            for (int i = 0; i < 2; ++i) {
                for (int j = i+1; j <= 2; ++j) {
                    final Iterator reps = dso.orbitReps(new IndexList(i, j));
                    while (reps.hasNext()) {
                        final Object D = reps.next();
                        final int v =dso.v(i, j, D);
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
        final Rational size = (Rational) new Fraction(4, 1).dividedBy(curvature2D());
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
        	List idcs = new IndexList(this);
    		idcs.remove(k);
    		Iterator reps = orbitReps(idcs);
    		while (reps.hasNext()) {
    			DelaneySymbol sub = new Subsymbol(this, idcs, reps.next());
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
		final List cones = new ArrayList();
		final List corners = new ArrayList();
		final int x[] = new int[3];
		final Iterator indices = indices();
		x[0] = ((Integer) indices.next()).intValue();
		x[1] = ((Integer) indices.next()).intValue();
		x[2] = ((Integer) indices.next()).intValue();
		
		for (int k = 0; k < 3; ++k) {
			final int i = (k + 1) % 3;
			final int j = (k + 2) % 3;
			final List idcs = new IndexList(x[i], x[j]);
			for (Iterator reps = orbitReps(idcs); reps.hasNext(); ) {
				final Object D = reps.next();
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
        for (Iterator iter2 = cones.iterator(); iter2.hasNext();) {
        	buf.append(iter2.next());
        }
        if (!isLoopless()) {
        	buf.append('*');
        }
        for (Iterator iter2 = corners.iterator(); iter2.hasNext();) {
        	buf.append(iter2.next());
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
    public DelaneySymbol orientedCover() {
        try {
            size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("symbol must be finite");
        }

        if (isOriented()) {
            return this;
        } else {
            final int n = size();
            final int d = dim();
            final int op[][] = new int[d + 1][2 * n + 1];
            final int v[][] = new int[d][2 * n + 1];
            final DSymbol ds = (DSymbol) this.flat();
            final Map ori = ds.partialOrientation();

            for (int i = 0; i <= d; ++i) {
                for (int k = 1; k <= n; ++k) {
                    final Object D = new Integer(k);
                    if (!ds.definesOp(i, D)) {
                        continue;
                    }
                    final Object Di = ds.op(i, D);
                    final int ki = ((Integer) Di).intValue();
                    if (ori.get(D).equals(ori.get(Di))) {
                        op[i][k] = ki + n;
                        op[i][ki] = k + n;
                        op[i][k + n] = ki;
                        op[i][ki + n] = k;
                    } else {
                        op[i][k] = ki;
                        op[i][ki] = k;
                        op[i][k + n] = ki + n;
                        op[i][ki + n] = k + n;
                    }
                    if (i < d) {
                        v[i][k] = v[i][k + n] = ds.v(i, i+1, D);
                    }
                }
            }
            return new DSymbol(op, v);
        }
    }
}

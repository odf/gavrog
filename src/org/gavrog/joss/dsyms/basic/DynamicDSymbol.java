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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;


/**
 * A Delaney symbol implementation that can grow, shrink and be modified. The
 * elements of such a symbol are {@link java.lang.Integer}s starting at 1. The
 * numbers stay consecutive as long as no elements are deleted. In any case, the
 * sequence of elements produced by {@link #elements()} is strictly growing.
 */
public class DynamicDSymbol extends DelaneySymbol<Integer> {
    final private int dim;
    private int lastId = 0;
    final Map<Integer, int[]> op;
    final Map<Integer, int[]> v;

    /**
     * Constructs an empty instance of a given dimension.
     * @param dim the dimension of the symbol.
     */
    public DynamicDSymbol(final int dim) {
        this.dim = dim;
        // --- use linked hash maps to preserve the element order
        this.op = new LinkedHashMap<Integer, int[]>();
        this.v = new LinkedHashMap<Integer, int[]>();
    }
    
    /**
     * Constructs an instance modelled after a given {@link DSymbol}, while possibly
     * changing the dimension.
     * 
     * @param dim the dimension of the symbol.
     * @param source the model {@link DSymbol}.
     */
    public DynamicDSymbol(final int dim, final DSymbol source) {
        this(dim);
        append(source);
    }
    
    /**
     * Constructs an instance modelled after a given {@link DSymbol}.
     * 
     * @param source the model {@link DSymbol}.
     */
    public DynamicDSymbol(final DSymbol source) {
        this(source.dim(), source);
    }
    
    /**
     * Constructs an instance as specified by a string.
     * 
     * @param spec the specifications for the new symbol.
     */
    public DynamicDSymbol(final String spec) {
        this(new DSymbol(spec));
    }
    
    // --- implementation of the inherited abstract methods
    
    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#dim()
     */
    public int dim() {
        return this.dim;
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#size()
     */
    public int size() {
        return this.op.size();
    }

    /**
     * This produces the elements in the order in which they were first created.
     */
    public IteratorAdapter<Integer> elements() {
        return new FilteredIterator<Integer, Integer>(
                    this.op.keySet().iterator())
        {
            @Override
            public Integer filter(final Integer D) {
                return D;
            }
        };
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#hasElement(java.lang.Object)
     */
    public boolean hasElement(final Integer D) {
        return this.op.containsKey(D);
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#indices()
     */
    public IteratorAdapter<Integer> indices() {
        return Iterators.range(0, dim() + 1);
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#hasIndex(int)
     */
    public boolean hasIndex(final int i) {
        return i >= 0 && i <= dim();
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#definesOp(int, java.lang.Object)
     */
    public boolean definesOp(final int i, final Integer D) {
		return hasElement(D) && hasIndex(i) && this.op.get(D)[i] != 0;
	}

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#op(int, java.lang.Object)
     */
    public Integer op(final int i, final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        return this.op.get(D)[i];
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#definesV(int, int, java.lang.Object)
     */
    public boolean definesV(final int i, final int j, final Integer D) {
		return hasElement(D)
               && hasIndex(i)
               && hasIndex(j)
               && (Math.abs(i - j) != 1 || this.v.get(D)[Math.min(i, j)] != 0);
    }

    /* (non-Javadoc)
     * @see javaDSym.symbols.DelaneySymbol#v(int, int, java.lang.Object)
     */
    public int v(final int i, final int j, final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }
        final int val;
        if (j == i+1) {
            val = ((int[]) this.v.get(D))[i];
        } else if (j == i-1) {
            val = ((int[]) this.v.get(D))[j];
        } else if (i != j && op(i, D).equals(op(j, D))) {
            val = 2;
        } else {
            val = 1;
        }
        return normalizedV(val);
    }
    
    // --- modification methods
    
    /**
     * Removes a neighbor specification.
     * 
     * @param i index.
     * @param D symbol element.
     */
    public void undefineOp(final int i, final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (definesOp(i, D)) {
            this.op.get(op(i, D))[i] = 0;
        }
        this.op.get(D)[i] = 0;
    }
    
    /**
     * Removes a branching number specification. To assure a consistent state
     * after the change, the specification is removed for all elements of the
     * relevant orbit.
     * 
     * @param i the first index.
     * @param j the second index.
     * @param D the symbol element.
     */
    public void undefineV(final int i, final int j, final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }
        if (Math.abs(i-j) != 1) {
            final String s = "implied value cannot be undefined";
            throw new IllegalArgumentException(s);
        }
        
        final int k = Math.min(i, j);
        for (final int E: orbit(new IndexList(k, k+1), D)) {
            this.v.get(E)[k] = 0;
        }
    }
    
    /**
     * Changes a neighbor specification.
     * 
     * @param i the index.
     * @param D the element.
     * @param E the new i-neighbor of D.
     */
    public void redefineOp(final int i, final Integer D, final Integer E) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasElement(E)) {
            throw new IllegalArgumentException("not an element: " + E);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (i > 0 && v(i - 1, i, D) != v(i - 1, i, E)) {
            throw new IllegalArgumentException("branching numbers differ");
        }
        if (i < dim() && v(i, i + 1, D) != v(i, i + 1, E)) {
            throw new IllegalArgumentException("branching numbers differ");
        }
        
        // TODO check that no special orbit is made too large
        
        if (definesOp(i, D)) {
            undefineOp(i, D);
        }
        if (definesOp(i, E)) {
            undefineOp(i, E);
        }
        this.op.get(D)[i] = E;	
        this.op.get(E)[i] = D;
    }
    
    /**
     * Changes a branching number specification. To assure a consistent state
     * after the change, the specification is changed for all elements of the
     * relevant orbit.
     * 
     * @param i the first index.
     * @param j the second index.
     * @param D the element.
     * @param v the new branching value.
     */
    public void redefineV(final int i, final int j, final Integer D, final int v) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }
        if (Math.abs(i-j) != 1) {
            final String s = "implied value cannot be redefined";
            throw new IllegalArgumentException(s);
        }
        if (v <= 0) {
            throw new IllegalArgumentException("invalid branching value " + v);
        }
        
        final int k = Math.min(i, j);
        for (final int E: orbit(new IndexList(k, k+1), D)) {
            this.v.get(E)[k] = v;
        }
    }
    
    // --- growing and shrinking the symbol
    
    /**
     * Adds a new element with undefined op()- and v()-values to this symbol.
     * 
     * @return the new element.
     */
    public Integer addElement() {
        final int x = ++this.lastId;
        this.op.put(x, new int[dim() + 1]);
        this.v.put(x, new int[dim()]);
        return x;
    }
    
    /**
     * Removes an element from this symbol.
     * @param D the element to remove.
     */
    public void removeElement(final Integer D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        for (int i = 0; i <= dim(); ++i) {
            undefineOp(i, D);
        }
        this.op.remove(D);
        this.v.remove(D);
    }
    
    /**
     * Grows this symbol by a specified number of elements. The op() and v()
     * values for these new elements will all be undefined.
     * 
     * @param n how many elements to add.
     * @return the list of new elements.
     */
    public List<Integer> grow(final int n) {
        final List<Integer> newElements = new ArrayList<Integer>(n);
        for (int i = 0; i < n; ++i) {
            newElements.add(addElement());
        }
        return newElements;
    }
    
    /**
     * Removes a certain portion of this symbol, closing up the gap behind it. A
     * connector index is used to find the new neighbors for any elements that
     * lost theirs. The set of elements to remove must be invariant under the
     * connector index. Furthermore, in the current implementation, none of
     * these elements may have any undefined or nontrivial branching number.
     * 
     * @param disposable the elements to remove.
     * @param connector the index used for reconnecting the remains.
     */
    public void collapse(final Collection<Integer> disposable,
            final int connector) {
        // TODO make this work for more general branching situations

        if (!hasIndex(connector)) {
            throw new IllegalArgumentException("illegal index " + connector);
        }
        for (final int D: disposable) {
            if (!hasElement(D)) {
                throw new IllegalArgumentException("illegal element " + D);
            }
            for (int i = 0; i < dim(); ++i) {
                if (!definesV(i, i+1, D) || v(i, i+1, D) != 1) {
                    final String s = "nontrivial branching at " + D;
                    throw new UnsupportedOperationException(s);
                }
            }
            if (!disposable.contains(op(connector, D))) {
                final String s = "elements not invariant under connnector";
                throw new IllegalArgumentException(s);
            }
        }
        
        for (final int i: indices()) {
            if (i == connector) {
                continue;
            }
            for (final int D: disposable) {
                if (!definesOp(i, D)) {
                    continue;
                }
                final int E = op(i, D);
                if (!disposable.contains(E)) {
                    int E1 = D;
                    while (disposable.contains(E1)) {
                        E1 = op(i, op(connector, E1));
                    }
                    redefineOp(i, E, E1);
                }
            }
        }
        for (final int D: disposable) {
            removeElement(D);
        }
    }

    /**
     * Constructs the dual of this symbol.
     * 
     * @return the dual.
     */
    public DynamicDSymbol dual() {
        // --- initialize the new symbol
        final DynamicDSymbol ds = new DynamicDSymbol(dim);
        final List<Integer> elms = ds.grow(size());
        
        // --- map old to new elements
        final Map<Integer, Integer> old2new = new HashMap<Integer, Integer>();
        int count = 0;
        for (final int D: elements()) {
            old2new.put(D, elms.get(count));
            ++count;
        }

        // --- set neighbor relations for dual
        for (final int D: elements()) {
            for (int i = 0; i <= dim(); ++i) {
                if (definesOp(i, D)) {
                    final int E = op(i, D);
                    ds.redefineOp(dim() - i, old2new.get(D), old2new.get(E));
                }
            }
        }
        
        // --- set branching numbers (must be done after neighbor relations)
        for (final int D: elements()) {
            for (int i = 0; i < dim(); ++i) {
                if (definesV(i, i+1, D)) {
                    final int v = v(i, i+1, D);
                    ds.redefineV(dim() - i, dim() - (i+1), old2new.get(D), v);
                }
            }
        }

        return ds;
    }
    
    /**
     * Append the contents of another symbol
     * @param source the symbol to append.
     */
    public List<Integer> append(final DSymbol source) {
        final List<Integer> elms = grow(source.size());
        // --- cannot mix the setting up of neigbors and branching numbers here
        for (final int sD: source.elements()) {
            final int tD = elms.get(sD - 1);
            for (int i = 0; i <= dim(); ++i) {
                if (source.definesOp(i, sD)) {
                    final int sE = source.op(i, sD);
                    final int tE = elms.get(sE - 1);
                    redefineOp(i, tD, tE);
                }
            }
        }
        for (final int sD: source.elements()) {
            final int tD = elms.get(sD - 1);
            for (int i = 0; i < dim(); ++i) {
                if (source.definesV(i, i+1, sD)) {
                    redefineV(i, i+1, tD, source.v(i, i+1, sD));
                }
            }
        }
        
        return elms;
    }
    
    /**
     * Remove all elements.
     */
    public void clear() {
        this.op.clear();
        this.v.clear();
        this.lastId = 0;
    }
    
    /**
     * Modifies the numbering of elements to be consecutive without changing
     * their relative order.
     * 
     * Restrictions: the symbol must be consistent.
     */
    public void renumber() {
        final DSymbol ds = new DSymbol(this);
        this.clear();
        this.append(ds);
    }
}

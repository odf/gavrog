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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.NiftyList;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

/**
 * Augments a tile in all possible ways by splitting edges (introducing
 * vertices of degree two) so that the symbol for the resulting tile has a
 * specified size.
 */
class SplitEdges2d extends IteratorAdapter<DSymbol> {
    final static private IndexList idcsEdge2d = new IndexList(0, 2);
    final private DSymbol base;
    final private int targetSize;
    final int orbRep[];
    final int orbSize[];
    final int orbAdded[];
    int currentSize;
    final Set<NiftyList<Integer>> results =
            new HashSet<NiftyList<Integer>>();

    /**
     * Constructs an instance.
     * @param base the symbol representing the input tile
     * @param size the target size for the augmented symbol
     */
    public SplitEdges2d(final DSymbol base, final int size) {
        // --- store parameters
        this.base = base;
        this.targetSize = size;
        this.currentSize = base.size();
        
        // --- collect (0,2)-orbits
        final List<List<Integer>> orbits = new ArrayList<List<Integer>>();
        for (final int D: base.orbitReps(idcsEdge2d)) {
			final List<Integer> orbit =
			        Iterators.asList(base.orbit(idcsEdge2d, D));
            orbits.add(orbit);
        }

        // --- sort by decreasing size
        Collections.sort(orbits, new Comparator<List<?>>() {
            public int compare(final List<?> l0, final List<?> l1) {
                return l1.size() - l0.size();
            }
        });
        
        // --- create arrays
        final int n = orbits.size();
        this.orbRep = new int[n];
        this.orbSize = new int[n];
        this.orbAdded = new int[n];
        
        // --- fill arrays
        for (int i = 0; i < n; ++i) {
            final List<Integer> orb = orbits.get(i);
            this.orbRep[i] = orb.get(0);
            this.orbSize[i] = orb.size();
            this.orbAdded[i] = 0;
        }
        
        // --- a little trick to make findNext() code simpler
        if (this.currentSize == this.targetSize) {
            this.orbAdded[n-1] = -this.orbSize[n-1];
            this.currentSize -= this.orbSize[n-1];
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.box.collections.IteratorAdapter#findNext()
     */
    protected DSymbol findNext() throws NoSuchElementException {
        while (true) {
            final int target = this.targetSize;
            int i = this.orbRep.length - 1;
            while (i >= 0 && this.currentSize + this.orbSize[i] > target) {
                this.currentSize -= this.orbAdded[i];
                this.orbAdded[i] = 0;
                --i;
            }
            if (i < 0) {
                throw new NoSuchElementException("at end");
            }
            this.orbAdded[i] += this.orbSize[i];
            this.currentSize += this.orbSize[i];

            if (this.currentSize == this.targetSize) {
                final DSymbol ds = augmented();
                final NiftyList<Integer> invariant = ds.invariant();
                if (!this.results.contains(invariant)) {
                    this.results.add(invariant);
                    return ds;
                }
            }
        }
    }

    /**
     * Constructs the augmented symbol based on the current values in
     * <code>orbAdded</code>.
     * @return the augmented symbol.
     */
    private DSymbol augmented() {
        final DynamicDSymbol ds = new DynamicDSymbol(this.base);
        for (int i = 0; i < this.orbRep.length; ++i) {
            final int size = this.orbSize[i];
            final int added = this.orbAdded[i];
            if (added == 0) {
                continue;
            }

            // --- holds orbit and added elements
            final int D[] = new int[size + added];
            // --- encodes original 0 operation
            final int op0[];
            // --- encodes original 2 operation
            final int op2[];

            // --- fill "op" arrays and start "D" array
            D[0] = this.orbRep[i];
            switch (size) {
            case 1:
                op0 = new int[] { 0 };
                op2 = new int[] { 0 };
                break;
            case 2:
                if (ds.op(0, D[0]).equals(D[0])) {
                    D[1] = ds.op(2, D[0]);
                    op0 = new int[] { 0, 1 };
                } else {
                    D[1] = ds.op(0, D[0]);
                    op0 = new int[] { 1, 0 };
                }
                if (ds.op(2, D[0]).equals(D[0])) {
                    op2 = new int[] { 0, 1 };
                } else {
                    op2 = new int[] { 1, 0 };
                }
                break;
            case 4:
                D[1] = ds.op(0, D[0]);
                D[2] = ds.op(2, D[1]);
                D[3] = ds.op(0, D[2]);
                op0 = new int[] { 1, 0, 3, 2 };
                op2 = new int[] { 3, 2, 1, 0 };
                break;
            default:
                throw new RuntimeException("this should not happen");
            }
            
            // --- remember v01 values and remove 0 edges
            final int v[] = new int[size];
            for (int k = 0; k < size; ++k) {
                final int E = D[k];
                v[k] = ds.v(0, 1, E);
                ds.undefineOp(0, E);
            }
            
            // --- add new elements for augmentation to array
            final List<Integer> newElements = ds.grow(added);
            for (int k = 0; k < added; ++k) {
                D[size + k] = newElements.get(k);
            }
            
            // --- set some v values
            int n = added / size;
            for (int k = 1; k <= n; ++k) {
                for (int m = 0; m < size; ++m) {
                    ds.redefineV(0, 1, D[k*size + m], v[m]);
                }
            }
            
            // --- connect the elements
            int idx = 0;
            for (int k = 0; k <= n; ++k) {
                for (int m = 0; m < size; ++m)  {
                    final int E = D[k*size + m];
                    final int E2 = D[k*size + op2[m]];
                    ds.redefineOp(2, E, E2);
                    final int Ei;
                    if (k < n) {
                        Ei = D[(k+1)*size + m];
                    } else {
                        Ei = D[k*size + op0[m]];
                    }
                    ds.redefineOp(idx, E, Ei);
                }
                idx = 1 - idx;
            }

            // --- set more v values
            for (int k = size; k < D.length; ++k) {
                final int E = D[k];
                ds.redefineV(1, 2, E, 2 / ds.r(1, 2, E));
            }
        }
        return new DSymbol(ds);
    }
}
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;

/**
 * Generates all feasible 1-dimensional symbols of a given size.
 */
public class Faces extends IteratorAdapter<DSymbol> {
	final private int minFace;
    final private int maxV;
    final Iterator<DSymbol> sets;
    int v;
    DynamicDSymbol currentSet = null;
    
    public Faces(final int size, final int minFace, final int maxV) {
    	this.minFace = minFace;
    	this.maxV = maxV;
        if (size % 2 == 1) {
            final DynamicDSymbol ds = new DynamicDSymbol(1);
            final List<Integer> elms = ds.grow(size);
            for (int i = 1; i < size; i += 2) {
                ds.redefineOp(0, elms.get(i-1), elms.get(i));
                ds.redefineOp(1, elms.get(i), elms.get(i+1));
            }
            ds.redefineOp(1, elms.get(0), elms.get(0));
            ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
            this.sets = Iterators.singleton(new DSymbol(ds));
        } else {
            final List<DSymbol> tmp = new LinkedList<DSymbol>();
            final DynamicDSymbol ds = new DynamicDSymbol(1);
            final List<Integer> elms = ds.grow(size);
            for (int i = 1; i < size; i += 2) {
                ds.redefineOp(0, elms.get(i-1), elms.get(i));
                ds.redefineOp(1, elms.get(i), elms.get((i+1) % size));
            }
            tmp.add(new DSymbol(ds));

            ds.redefineOp(1, elms.get(0), elms.get(0));
            ds.redefineOp(1, elms.get(size-1), elms.get(size-1));
            tmp.add(new DSymbol(ds));

            for (int i = 1; i < size; i += 2) {
                ds.redefineOp(1, elms.get(i-1), elms.get(i));
                ds.redefineOp(0, elms.get(i), elms.get((i+1) % size));
            }
            ds.redefineOp(0, elms.get(0), elms.get(0));
            ds.redefineOp(0, elms.get(size-1), elms.get(size-1));
            tmp.add(new DSymbol(ds));

            this.sets = tmp.iterator();
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.box.collections.IteratorAdapter#findNext()
     */
    protected DSymbol findNext() throws NoSuchElementException {
        while (true) {
            if (this.currentSet != null && this.v <= this.maxV) {
                final DynamicDSymbol ds = this.currentSet;
                final int D = ds.elements().next();
                ds.redefineV(0, 1, D, this.v);
                ++this.v;
                if (ds.m(0, 1, D) < this.minFace) {
                	continue;
                }
                return new DSymbol(ds);
            } else if (this.sets.hasNext()) {
                final DSymbol ds = this.sets.next();
                this.currentSet = new DynamicDSymbol(ds);
                this.v = 1;
            } else {
                throw new NoSuchElementException("at end");
            }
        }
    }
}
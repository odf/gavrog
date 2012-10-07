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

package org.gavrog.joss.dsyms.derived;

import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.Boundary.Face;


/**
 * An iterator which generates the fundamental edges of a
 * {@link org.gavrog.joss.dsyms.basic.DelaneySymbol} corresponding to its standard
 * {@link org.gavrog.joss.dsyms.basic.Traversal}. The fundamental edges are the interior
 * edges of one possible abstract fundamental domain for the symbol.
 */
public class FundamentalEdges<T> extends IteratorAdapter<DSPair<T>> {
	final private DelaneySymbol<T> ds;
	final private Boundary<T> boundary;
	final private Traversal<T> traversal;
	final private LinkedList<Face<T>> Q;
	
	/**
	 * Constructs an instance with a default spanning tree.
	 * 
	 * @param ds the Delaney symbol to process.
	 */
	public FundamentalEdges(final DelaneySymbol<T> ds) {
	    this(ds, new Traversal<T>(ds));
	}
	
	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol to process.
	 * @param trav a traversal representing a spanning tree of <code>ds</code>.
	 */
	public FundamentalEdges(
			final DelaneySymbol<T> ds,
			final Traversal<T> trav)
	{
	    if (ds == null) {
	        throw new NullPointerException("null argument");
	    }
	    this.ds = ds;
	    this.traversal = trav;
	    this.boundary = new Boundary<T>(this.ds);
	    this.Q = new LinkedList<Face<T>>();
	}
	
	/**
     * This methods finds the next result.
     */
	protected DSPair<T> findNext() {
        while (Q.size() > 0) {
            final Face<T> f = Q.removeFirst();
            final T D = f.getElement();
            final int i = f.getFirstIndex();
            final int j = f.getSecondIndex();
            if (!boundary.isOnBoundary(i, D)) {
                continue;
            }
            if (boundary.glueCountAtRidge(i, D, j) == 2 * ds.m(i, j, D)) {
                boundary.glueAndEnqueue(i, D, Q);
                return new DSPair<T>(i, D);
            }
        }
        while (traversal.hasNext()) {
            final DSPair<T> e = traversal.next();
            final T D = e.getElement();
            final int i = e.getIndex();
            if (i < 0) {
                continue;
            }
            if (!boundary.isOnBoundary(i, D)) {
                throw new RuntimeException("this should not happen");
            }
            boundary.glueAndEnqueue(i, D, Q);
            return new DSPair<T>(i, D);
        }
        throw new NoSuchElementException("at end");
    }
}

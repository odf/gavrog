/*
   Copyright 2005 Olaf Delgado-Friedrichs

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
 * 
 * @author Olaf Delgado
 * @version $Id: FundamentalEdges.java,v 1.4 2007/04/18 20:19:08 odf Exp $
 */
public class FundamentalEdges extends IteratorAdapter {
	final private DelaneySymbol ds;
	final private Boundary boundary;
	final private Traversal traversal;
	final private LinkedList Q;
	
	/**
	 * Constructs an instance with a default spanning tree.
	 * 
	 * @param ds the Delaney symbol to process.
	 */
	public FundamentalEdges(final DelaneySymbol ds) {
	    this(ds, new Traversal(ds));
	}
	
	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol to process.
	 * @param trav a traversal representing a spanning tree of <code>ds</code>.
	 */
	public FundamentalEdges(final DelaneySymbol ds, final Traversal trav) {
	    if (ds == null) {
	        throw new NullPointerException("null argument");
	    }
	    this.ds = ds;
	    this.traversal = trav;
	    this.boundary = new Boundary(this.ds);
	    this.Q = new LinkedList();
	}
	
	/**
     * This methods finds the next result.
     */
	protected Object findNext() {
        while (Q.size() > 0) {
            final Face f = (Face) Q.removeFirst();
            final Object D = f.getElement();
            final int i = f.getFirstIndex();
            final int j = f.getSecondIndex();
            if (!boundary.isOnBoundary(i, D)) {
                continue;
            }
            if (boundary.glueCountAtRidge(i, D, j) == 2 * ds.m(i, j, D)) {
                boundary.glueAndEnqueue(i, D, Q);
                return new DSPair(i, D);
            }
        }
        while (traversal.hasNext()) {
            final DSPair e = (DSPair) traversal.next();
            final Object D = e.getElement();
            final int i = e.getIndex();
            if (i < 0) {
                continue;
            }
            if (!boundary.isOnBoundary(i, D)) {
                throw new RuntimeException("this should not happen");
            }
            boundary.glueAndEnqueue(i, D, Q);
            return new DSPair(i, D);
        }
        throw new NoSuchElementException("at end");
    }
}

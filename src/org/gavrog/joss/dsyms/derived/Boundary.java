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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;


/**
 * Represents the boundary complex of a topological realization of a Delaney
 * symbol with facet identifications only performed partially.
 */
public class Boundary<T> {
    private DelaneySymbol<T> ds;
    private HashSet<Face<T>> onBoundary;
    private HashMap<Face<T>, Face<T>> neighbor;
    private HashMap<Face<T>, Integer> count;
    
    static class Face<T> {
        int i;
        int j;
        T D;
        
        public Face(final int i, final T D, final int j) {
            this.i = i;
            this.D = D;
            this.j = j;
        }
        
        public Face(final int i, final T D) {
            this(i, D, -1);
        }
        
        public Face(final Face<T> k, final int j) {
            this(k.i, k.D, j);
        }
        
        public int getFirstIndex() {
            return i;
        }
        
        public T getElement() {
            return D;
        }
        
        public int getSecondIndex() {
            return j;
        }
        
        public int hashCode() {
            return (i * 37 + D.hashCode()) * 37 + j; 
        }
        
        public boolean equals(final Object other) {
            if (other instanceof Face) {
                @SuppressWarnings("unchecked")
				final Face<T> k = (Face<T>) other;
                return this.D.equals(k.D) && this.i == k.i && this.j == k.j;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Constructs a boundary complex with no identifications.
     * @param ds the underlying Delaney symbol.
     */
    public Boundary(final DelaneySymbol<T> ds) {
        this.ds = ds;
        this.onBoundary = new HashSet<Face<T>>();
        this.neighbor = new HashMap<Face<T>, Face<T>>();
        this.count = new HashMap<Face<T>, Integer>();
        
        for (final int i: ds.indices()) {
            for (final T D: ds.elements()) {
                onBoundary.add(new Face<T>(i, D));
                for (final int j: ds.indices()) {
                    count.put(new Face<T>(i, D, j), 1);
                    neighbor.put(new Face<T>(i, D, j), new Face<T>(j, D));
                }
            }
        }
    }
    
    /**
     * Tests whether a facet is on the boundary.
     * @param i facet index.
     * @param D element to which the facet belongs.
     * @return true if face i,D is on the boundary.
     */
    public boolean isOnBoundary(final int i, final T D) {
        return onBoundary.contains(new Face<T>(i, D));
    }
    
    /**
     * Determines the neighbor of a facet across a boundary ridge.
     * @param i facet index.
     * @param D element to which the facet belongs.
     * @param j second index to specify the ridge.
     * @return the neighbor facet across the ridge.
     */
    public Face<T> neighbor(final int i, final T D, final int j) {
        if (!isOnBoundary(i, D)) {
            String text = "(" + i + "," + D + ") not on boundary.";
            throw new IllegalArgumentException(text);
        }
        return neighbor.get(new Face<T>(i, D, j));
    }
    
    /**
     * Determines the number of elements glued together at a boundary ridge.
     * @param i facet index.
     * @param D element to which the facet belongs.
     * @param j second index to specify the ridge.
     * @return the number of elements glued together along that ridge.
     */
    public int glueCountAtRidge(final int i, final T D, final int j) {
        if (!isOnBoundary(i, D)) {
            String text = "(" + i + "," + D + ") must be on boundary.";
            throw new UnsupportedOperationException(text);
        }
        return count.get(new Face<T>(i, D, j));
    }
    
    /**
     * Perform a specific identification.
     * @param e the edge at which to identify.
     */
    public void glue(final DSPair<T> e) {
        glue(e.getIndex(), e.getElement());
    }
    
    /**
     * Perform a specific identification.
     * @param i the index of the facet to be identified.
     * @param D the element to be identified with its i-neighbor.
     */
    public void glue(final int i, final T D) {
        final T E = ds.op(i, D);
        final Face<T> iD = new Face<T>(i, D);
        final Face<T> iE = new Face<T>(i, E);
        if (!onBoundary.contains(iD)) {
            String text = "(" + i + "," + D + ") not on boundary.";
            throw new IllegalArgumentException(text);
        }

        for (final int j: ds.indices()) {
            if (j != i) {
                final Face<T> iDjN = neighbor.get(new Face<T>(i, D, j));
                final Face<T> iEjN = neighbor.get(new Face<T>(i, E, j));
               
                int n = glueCountAtRidge(i, D, j) + glueCountAtRidge(i, E, j);
                
                final Face<T> iDjNk =
                		new Face<T>(iDjN, i + j - iDjN.getFirstIndex());
                neighbor.put(iDjNk, iEjN);
                count.put(iDjNk, n);

                final Face<T> iEjNk =
                		new Face<T>(iEjN, i + j - iEjN.getFirstIndex());
                neighbor.put(iEjNk, iDjN);
                count.put(iEjNk, n);
            }
        }
        onBoundary.remove(iD);
        onBoundary.remove(iE);
    }
    
	/**
	 * Identifies a facet with its counterpart and queues up the associated
	 * ridges.
	 * @param i the facet index.
	 * @param D the containing element.
	 * @param queue the queue to store ridges in.
	 */
	public void glueAndEnqueue(
			final int i,
			final T D,
			final LinkedList<Face<T>> queue)
	{
		for (final int j: ds.indices()) {
            if (j != i) {
                final Face<T> f = neighbor(i, D, j);
                final T E = f.getElement();
                final int k = f.getFirstIndex();
                final int l = i + j - k;
                queue.addLast(new Face<T>(k, E, l));
            }
        }
        glue(i, D);
	}
}

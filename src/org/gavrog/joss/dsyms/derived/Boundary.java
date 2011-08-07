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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSPair;


/**
 * Represents the boundary complex of a topological realization of a Delaney
 * symbol with facet identifications only performed partially.
 * @author Olaf Delgado
 * @version $Id: Boundary.java,v 1.3 2007/04/18 20:19:08 odf Exp $
 */
public class Boundary {
    private DelaneySymbol ds;
    private HashSet onBoundary;
    private HashMap neighbor;
    private HashMap count;
    
    static class Face {
        int i;
        int j;
        Object D;
        
        public Face(int i, Object D, int j) {
            this.i = i;
            this.D = D;
            this.j = j;
        }
        
        public Face(int i, Object D) {
            this(i, D, -1);
        }
        
        public Face(Face k, int j) {
            this(k.i, k.D, j);
        }
        
        public int getFirstIndex() {
            return i;
        }
        
        public Object getElement() {
            return D;
        }
        
        public int getSecondIndex() {
            return j;
        }
        
        public int hashCode() {
            return (i * 37 + D.hashCode()) * 37 + j; 
        }
        
        public boolean equals(Object other) {
            if (other instanceof Face) {
                Face k = (Face) other;
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
    public Boundary(DelaneySymbol ds) {
        this.ds = ds;
        this.onBoundary = new HashSet();
        this.neighbor = new HashMap();
        this.count = new HashMap();
        
        Iterator idcs1 = ds.indices();
        while (idcs1.hasNext()) {
            int i = ((Integer) idcs1.next()).intValue();
            Iterator elms = ds.elements();
            while (elms.hasNext()) {
                Object D = elms.next();
                onBoundary.add(new Face(i, D));
                Iterator idcs2 = ds.indices();
                while (idcs2.hasNext()) {
                    int j = ((Integer) idcs2.next()).intValue();
                    count.put(new Face(i, D, j), new Integer(1));
                    neighbor.put(new Face(i, D, j), new Face(j, D));
                }
            }
        }
    }
    
    /**
     * Tests wether a facet is on the boundary.
     * @param i facet index.
     * @param D element to which the facet belongs.
     * @return true if face i,D is on the boundary.
     */
    public boolean isOnBoundary(int i, Object D) {
        return onBoundary.contains(new Face(i, D));
    }
    
    /**
     * Determines the neighbor of a facet across a boundary ridge.
     * @param i facet index.
     * @param D element to which the facet belongs.
     * @param j second index to specify the ridge.
     * @return the neighbor facet across the ridge.
     */
    public Face neighbor(int i, Object D, int j) {
        if (!isOnBoundary(i, D)) {
            String text = "(" + i + "," + D + ") not on boundary.";
            throw new IllegalArgumentException(text);
        }
        return (Face) neighbor.get(new Face(i, D, j));
    }
    
    /**
     * Determines the number of elements glued together at a boundary ridge.
     * @param i facet index.
     * @param D element to which the facet belongs.
     * @param j second index to specify the ridge.
     * @return the number of elements glued together along that ridge.
     */
    public int glueCountAtRidge(int i, Object D, int j) {
        if (!isOnBoundary(i, D)) {
            String text = "(" + i + "," + D + ") must be on boundary.";
            throw new UnsupportedOperationException(text);
        }
        return ((Integer) count.get(new Face(i, D, j))).intValue();
    }
    
    /**
     * Perform a specific identification.
     * @param e the edge at which to identify.
     */
    public void glue(DSPair e) {
        glue(e.getIndex(), e.getElement());
    }
    
    /**
     * Perform a specific identification.
     * @param i the index of the facet to be identified.
     * @param D the element to be identified with its i-neighbor.
     */
    public void glue(int i, Object D) {
        Object E = ds.op(i, D);
        Face iD = new Face(i, D);
        Face iE = new Face(i, E);
        if (!onBoundary.contains(iD)) {
            String text = "(" + i + "," + D + ") not on boundary.";
            throw new IllegalArgumentException(text);
        }

        Iterator idcs = ds.indices();
        while (idcs.hasNext()) {
            int j = ((Integer) idcs.next()).intValue();
            if (j != i) {
                Face iDjN = (Face) neighbor.get(new Face(i, D, j));
                Face iEjN = (Face) neighbor.get(new Face(i, E, j));
                
                int n = glueCountAtRidge(i, D, j) + glueCountAtRidge(i, E, j);
                
                Face iDjNk = new Face(iDjN, i + j - iDjN.getFirstIndex());
                neighbor.put(iDjNk, iEjN);
                count.put(iDjNk, new Integer(n));

                Face iEjNk = new Face(iEjN, i + j - iEjN.getFirstIndex());
                neighbor.put(iEjNk, iDjN);
                count.put(iEjNk, new Integer(n));
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
	public void glueAndEnqueue(final int i, final Object D, LinkedList queue) {
        final Iterator idcs = ds.indices();
        while (idcs.hasNext()) {
            final int j = ((Integer) idcs.next()).intValue();
            if (j != i) {
                final Face f = neighbor(i, D, j);
                final Object E = f.getElement();
                final int k = f.getFirstIndex();
                final int l = i + j - k;
                queue.addLast(new Face(k, E, l));
            }
        }
        glue(i, D);
	}
}

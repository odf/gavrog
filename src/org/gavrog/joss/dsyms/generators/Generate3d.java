/*
   Copyright 2007 Olaf Delgado-Friedrichs

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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.SmallActionsIterator;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;


/**
 * Generates all minimal euclidean Delaney symbols up to a given size.
 * 
 * @author Olaf Delgado
 * @version $Id: Generate3d.java,v 1.5 2007/04/23 21:33:56 odf Exp $
 */
public class Generate3d extends IteratorAdapter {
    /*
     * A subclass of DefineBranching which forbids certain degeneracies.
     */
    private static class DefineProperBranching extends DefineBranching3d {
        public DefineProperBranching(final DelaneySymbol ds, final boolean deg2ok) {
            super(ds, deg2ok);
        }

        protected List getExtraDeductions(final DelaneySymbol ds, final Move move) {
            if (move.index == 1) {
                final Integer D = new Integer(move.element);
                final int m = ds.m(1, 2, D);
                if (m == 1) {
                    // --- no dangling vertices
                    return null;
                } else if (m == 2) {
                    // --- no trivial (two-faced) tiles
                    Object E = D;
                    do {
                        E = ds.op(1, ds.op(0, E));
                    } while (!E.equals(D) && ds.m(1, 2, E) == 2);
                    if (E.equals(D)) {
                        // --- all vertices in these face have degree 2 -> trivial tile
                        return null;
                    }
                    // --- no trivial (global degree 2) vertex 
                    E = D;
                    do {
                        E = ds.op(2, ds.op(3, E));
                    } while (!E.equals(D) && ds.m(1, 2, E) == 2);
                    if (E.equals(D)) {
                        // --- all vertices in these face have degree 2 -> trivial tile
                        return null;
                    }
                }
            }
            return new ArrayList();
        }
    }

    final private static List edgeIndices = new IndexList(0, 2, 3);
	final private Iterator actions;
	private Iterator current;
	final private FundamentalGroup G;
	final boolean allowEdgesOfDegreeTwo;
	final boolean edgeTransitive;

    public Generate3d(final int size) {
    	this(size, false, false);
    }
    
    public Generate3d(final int size, final boolean allowEdgesOfDegreeTwo,
    		final boolean edgeTransitive) {
        this.G = new FundamentalGroup(new DSymbol("1 3:1,1,1,1:0,0,0"));
        final FpGroup pG = G.getPresentation();
        this.actions = new SmallActionsIterator(pG, size, false);
        this.current = Iterators.empty();
        this.allowEdgesOfDegreeTwo = allowEdgesOfDegreeTwo;
        this.edgeTransitive = edgeTransitive;
    }
    
    protected Object findNext() throws NoSuchElementException {
		while (true) {
			if (current.hasNext()) {
				final DSymbol ds = (DSymbol) current.next();
				if (ds.isMinimal() && !new EuclidicityTester(ds).isBad()) {
					return ds;
				}
			} else if (actions.hasNext()) {
				final GroupAction action = (GroupAction) actions.next();
				final DSymbol set = new DSCover(G, action);
				if (this.edgeTransitive && set.numberOfOrbits(edgeIndices) > 1) {
					continue;
				}
				if (Utils.mayBecomeLocallyEuclidean3D(set)) {
					current = new DefineProperBranching(set, this.allowEdgesOfDegreeTwo);
				}
			} else {
				throw new NoSuchElementException("at end");
			}
		}
	}
    
    public static void main(final String[] args) {
    	int i;
    	boolean allowEdgesOfDegreeTwo = false;
    	boolean edgeTransitive = false;
    	for (i = 0; i < args.length; ++i) {
    		if (args[i].equalsIgnoreCase("-x")) {
    			allowEdgesOfDegreeTwo = !allowEdgesOfDegreeTwo;
    		} else if (args[i].equalsIgnoreCase("-e")) {
    			edgeTransitive = !edgeTransitive;
    		} else {
    			break;
    		}
    	}
        final int maxSize = args.length > i ? Integer.parseInt(args[i]) : 6;
        final Iterator symbols = new Generate3d(maxSize, allowEdgesOfDegreeTwo,
        		edgeTransitive);

        final long start = System.currentTimeMillis();
        final int count = Iterators.print(System.out, symbols, "\n");
        final long stop = System.currentTimeMillis();
        System.out.println("\nGenerated " + count + " symbols.");
        System.out.println("Execution time was " + (stop - start) / 1000.0 + " seconds.");
    }
}

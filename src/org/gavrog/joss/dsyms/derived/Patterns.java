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

import java.util.Iterator;
import java.util.List;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;


/**
 * Contains static method to recognize tilings or certain patterns in a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: Patterns.java,v 1.2 2007/04/26 20:21:58 odf Exp $
 */
public class Patterns {
    
    /**
     * Tests if a given symbol is a double ring (aka a prism) with the given ring size.
     * 
     * @param ds the input symbol.
     * @param size the ring size to look for.
     * @return a boolean indicating the results of the test.
     */
    public static boolean isDoubleRing(final DelaneySymbol ds, final int size) {
        final DSymbol test;
        if (size == 4) {
            test = new DSymbol("1:1,1,1:4,3");
        } else {
            test = new DSymbol("3:1 2 3,1 3,2 3:" + size + " 4,3");
        }
        return ds.minimal().equals(test);
    }
    
    /**
     * Tests if a given symbol contains a stacked pair of double rings.
     * 
     * @param ds the input symbol.
     * @return a boolean indicating the result of the test.
     */
    public static boolean containsDoubleRingStack(final DSymbol ds) {
        if (ds.dim() < 3) {
            return false;
        }
        final List iFace = new IndexList(0, 1, 3);
        final List iTile = new IndexList(0, 1, 2);
        for (final Iterator reps = ds.orbitReps(iFace); reps.hasNext();) {
            final Object D = reps.next();
            final Object E = ds.op(3, D);
            if (ds.m(0, 1, ds.op(2, D)) == 4 && ds.m(0, 1, ds.op(2, E)) == 4) {
                if (isDoubleRing(new Subsymbol(ds, iTile, D), ds.m(0, 1, D))
                        && isDoubleRing(new Subsymbol(ds, iTile, E), ds.m(0, 1, E))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Tests if a given symbol contains a patch of double rings. A patch is a
     * connected configuration of at least three double rings linked in
     * alignment to a central fourth one.
     * 
     * @param ds the input symbol.
     * @return a boolean indicating the result of the test.
     */
    public static boolean containsDoubleRingPatch(final DSymbol ds) {
        if (ds.dim() < 3) {
            return false;
        }
        final List iHalfFace = new IndexList(0, 1);
        final List iTile = new IndexList(0, 1, 2);
        for (final Iterator reps = ds.orbitReps(iHalfFace); reps.hasNext();) {
            final Object D = reps.next();
            if (ds.m(0, 1, ds.op(2, D)) != 4) {
                continue;
            }
            if (!isDoubleRing(new Subsymbol(ds, iTile, D), ds.m(0, 1, D))) {
                continue;
            }
            int count = 0;
            final int v = ds.v(0, 1, D);
            final boolean oriented = ds.orbitIsOriented(iHalfFace, D);
            Object E = D;
            do {
                final Object F = ds.op(2, ds.op(3, ds.op(2, E)));
                if (isDoubleRing(new Subsymbol(ds, iTile, F), ds.m(0, 1, F))) {
                    if (!oriented && !ds.op(0, E).equals(E)) {
                        count += 2 * v;
                    } else {
                        count += v;
                    }
                    if (count >= 3) {
                        return true;
                    }
                }
                E = ds.op(1, ds.op(0, E));
            } while (!E.equals(D));
        }
        return false;
    }
}

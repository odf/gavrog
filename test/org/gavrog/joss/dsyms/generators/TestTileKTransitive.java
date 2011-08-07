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

package org.gavrog.joss.dsyms.generators;

import java.util.Iterator;
import java.util.List;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.generators.TileKTransitive;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestTileKTransitive.java,v 1.2 2007/04/26 20:21:59 odf Exp $
 */
public class TestTileKTransitive extends TestCase {
    public void testTetra2() {
        doTest(new DSymbol("1:1,1,1:3,3"), 2, 100, false);
    }
    
    public void doTest(final DSymbol ds, final int k, final int limit, final boolean print) {
        final DSymbol minimal = new DSymbol(ds.minimal());
        final List idcs = new IndexList(0, 1, 2);
        
        final Iterator iter = new TileKTransitive(ds, 2, false);
        int count = 0;
        while (iter.hasNext()) {
            final DSymbol out = (DSymbol) iter.next();
            assertTrue(out.isConnected());
            int countTiles = 0;
            for (final Iterator reps = out.orbitReps(idcs); reps.hasNext();) {
                ++countTiles;
                assertEquals(minimal, new Subsymbol(out, idcs, reps.next()).minimal());
            }
            assertEquals(k, countTiles);
            if (print) {
                System.out.println(out);
            }
            ++count;
            
            if (limit > 0 && count > limit) {
                break;
            }
        }
    }
}

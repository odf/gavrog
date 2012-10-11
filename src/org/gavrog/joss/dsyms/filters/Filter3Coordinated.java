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

package org.gavrog.joss.dsyms.filters;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.generators.InputIterator;


/**
 * Extracts the tilings that carry 3-coordinated nets from a file. Also looks at
 * the duals of the tilings in the input file. Currently, no attempt is made to
 * avoid duplicates, unless a tiling is self-dual.
 */
public class Filter3Coordinated {

    public static void main(String[] args) {
        final String filename = args[0];

        int inCount = 0;
        int outCount = 0;

        for (final DSymbol ds: new InputIterator(filename)) {
            final DSymbol dual = ds.dual();
            ++inCount;
            if (is3coordinated(ds)) {
                ++outCount;
                System.out.println(ds.canonical());
            }
            if (!ds.equals(dual) && is3coordinated(dual)) {
                ++outCount;
                System.out.println(dual.canonical());
            }
        }

        System.err.println("Read " + inCount + " symbols. Found " + outCount
                           + " 3-coordinated symbols, including duals.");
    }

    /**
     * Checks if a symbol carries a 3-coordinated net.
     * @param ds the input symbol.
     * @return true or false according to the result of the test.
     */
    private static boolean is3coordinated(final DSymbol ds) {
        final IndexList idcs = new IndexList(1, 2, 3);
        for (int D: ds.orbitReps(idcs)) {
            final DSymbol sub =
                    new DSymbol(new Subsymbol<Integer>(ds, idcs, D));
            final DSymbol cov = new DSymbol(Covers.finiteUniversalCover(sub));
            final int nrEdges = cov.numberOfOrbits(new IndexList(1, 2));
            if (nrEdges != 3) {
                return false;
            }
        }
        return true;
    }
}

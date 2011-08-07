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

package org.gavrog.joss.dsyms.filters;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.tilings.Tiling;


/**
 * Extracts tilings the graphs of which have collisions.
 * 
 * @author Olaf Delgado
 * @version $Id: FilterUnstable.java,v 1.4 2007/04/18 23:06:22 odf Exp $
 */
public class FilterUnstable {

    public static void main(String[] args) {
        final String filename = args[0];

        int inCount = 0;
        int outCount = 0;

        for (final InputIterator input = new InputIterator(filename); input.hasNext();) {
            final DSymbol ds = (DSymbol) input.next();
            ++inCount;
            if (isUnstable(ds)) {
                ++outCount;
                System.out.println(ds);
            }
        }

        System.err.println("Read " + inCount + " symbols, " + outCount
				+ " of which had unstable graphs.");
    }

    private static boolean isUnstable(final DSymbol ds) {
        final DelaneySymbol cov = Covers.pseudoToroidalCover3D(ds.minimal());
        try {
            return !new Tiling(cov).getSkeleton().isLocallyStable();
        } catch (final Exception ex) {
            System.out.println("??? " + ds);
            return false;
        }
    }
}

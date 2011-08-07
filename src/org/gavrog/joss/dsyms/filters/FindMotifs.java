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
import org.gavrog.joss.dsyms.derived.Patterns;
import org.gavrog.joss.dsyms.generators.InputIterator;


/**
 * @author Olaf Delgado
 * @version $Id: FindMotifs.java,v 1.1 2006/08/29 03:47:21 odf Exp $
 */
public class FindMotifs {

    public static void main(String[] args) {
        final String filename = args[0];
        int count = 0;

        for (final InputIterator iter = new InputIterator(filename); iter.hasNext();) {
            final DSymbol ds = (DSymbol) iter.next();
            final boolean hasStacks = Patterns.containsDoubleRingStack(ds);
            final boolean hasPatches = Patterns.containsDoubleRingPatch(ds);
            ++count;
            
            if (hasStacks || hasPatches) {
                System.out.print("Structure " + count + " has double ring ");
                if (hasStacks && hasPatches) {
                    System.out.print("stacks and patches");
                } else if (hasStacks) {
                    System.out.print("stacks");
                } else if (hasPatches) {
                    System.out.print("patches");
                }
                System.out.println(".");
            }
        }
        System.out.flush();
    }
}

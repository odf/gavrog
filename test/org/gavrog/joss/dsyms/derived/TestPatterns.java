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

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.Patterns;

import junit.framework.TestCase;

/**
 * Unit tests for class Patterns.
 * @author Olaf Delgado
 * @version $Id: TestPatterns.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestPatterns extends TestCase {
    public void testIsDoubleRing() {
        final DSymbol ds = new DSymbol("3:1 2 3,2 3,1 3:4 5,3");
        assertTrue(Patterns.isDoubleRing(ds, 5));
        assertFalse(Patterns.isDoubleRing(ds, 4));
        assertTrue(Patterns.isDoubleRing(new DSymbol("1:1,1,1:4,3"), 4));
        assertFalse(Patterns.isDoubleRing(new DSymbol("1:1,1,1:3,4"), 4));
        assertTrue(Patterns.isDoubleRing(new DSymbol("3:1 2 3,2 3,1 3:4 4,3"), 4));
    }
    
    public void testContainsDoubleRingStack() {
        assertTrue(Patterns.containsDoubleRingStack(new DSymbol("1 3:1,1,1,1:4,3,4")));
        assertTrue(Patterns.containsDoubleRingStack(new DSymbol("1 3:1,1,1,1:4,3,3")));
        assertFalse(Patterns.containsDoubleRingStack(new DSymbol("1 3:1,1,1,1:3,3,4")));
        final DSymbol ds = new DSymbol("3 3:1 2 3,2 3,1 3,1 2 3:4 7,3,3 4");
        assertTrue(Patterns.containsDoubleRingStack(ds));
        final DSymbol big = new DSymbol("48 3:"
                + "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 25 26 27 "
                + "28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 48 47 46,"
                + "22 21 16 15 10 9 24 23 14 13 20 19 46 45 40 39 34 33 48 47 38 37 44 43,"
                + "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35 42 39 41 48 45 47,"
                + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48:"
                + "4 4 4 4 4 4 4 4 8 8 24 4 4 4 4,3 3 3 3 3 3 3 3,3 3 3 3 3 3 3 3");
        assertTrue(Patterns.containsDoubleRingStack(big));
    }
    
    public void testContainsDoubleRingPatch() {
        assertTrue(Patterns.containsDoubleRingPatch(new DSymbol("1 3:1,1,1,1:4,3,4")));
        assertTrue(Patterns.containsDoubleRingPatch(new DSymbol("1 3:1,1,1,1:4,3,3")));
        assertFalse(Patterns.containsDoubleRingPatch(new DSymbol("1 3:1,1,1,1:3,3,4")));
        final DSymbol ds = new DSymbol("3 3:1 2 3,2 3,1 3,1 2 3:4 6,3,3 4");
        assertTrue(Patterns.containsDoubleRingPatch(ds));
        final DSymbol big = new DSymbol("36 3:"
                        + "1 2 3 4 5 6 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36,"
                        + "8 7 10 9 6 12 34 33 28 27 22 21 36 35 26 25 32 31,"
                        + "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35,"
                        + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36:"
                        + "4 4 4 4 4 18 4 4 8 8 4 4,3 3 3 3 3 3,3 3 3 3 3 3");
        assertTrue(Patterns.containsDoubleRingPatch(big));
    }
}

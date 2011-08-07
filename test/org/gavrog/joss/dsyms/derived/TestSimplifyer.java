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
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.Simplifyer;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestSimplifyer.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestSimplifyer extends TestCase {
    final private DSymbol good1 = new DSymbol("48 3:"
            + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48,"
            + "8 3 5 7 16 11 13 15 24 19 21 23 32 27 29 31 40 35 37 39 48 43 45 47,"
            + "9 10 17 18 25 26 33 34 24 23 41 42 36 35 32 31 47 48 40 39 45 46 43 44,"
            + "42 41 48 47 46 45 44 43 26 25 32 31 30 29 28 27 34 33 40 39 38 37 36 35:"
            + "4 4 4 4 4 4,3 3 3 3 3 3 3 3,4 4 4 4 4 4");
    final private DSymbol good2 = new DSymbol("64 3:"
            + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48 50"
            + " 52 54 56 58 60 62 64,"
            + "6 3 5 12 9 11 18 15 17 26 21 23 25 32 29 31 38 35 37 46 41 43 45 52 49"
            + " 51 58 55 57 64 61 63,"
            + "7 8 13 14 19 20 27 28 22 21 33 34 39 40 47 48 53 54 59 60 42 41 46 45"
            + " 56 55 61 62 58 57 63 64,"
            + "59 60 61 62 63 64 33 34 35 36 37 38 47 48 49 50 51 52 39 40 41 42 43 44"
            + " 45 46 54 53 58 57 56 55:"
            + "3 3 3 4 3 3 4 3 3 3,3 5 5 5 3 3 3 5,4 4 3 3 3 3 3 3 3 3");

    public void testSimplifyer1() {
        testSimplifyer(new DSymbol("2 3:1 2,1 2,1 2,2:3 3,3 4,4"), true);
    }
    
    public void testSimplifyer2() {
        testSimplifyer(new DSymbol("1 3:1,1,1,1:4,3,4"), true);
    }
    
    public void testSimplifyer3() {
        testSimplifyer(new DSymbol("20 3:"
                + "2 4 6 8 10 12 14 16 18 20, 10 3 5 7 9 20 13 15 17 19,"
                + "4 3 11 12 16 15 19 20 17 18,16 15 14 13 12 11 20 19 18 17:"
                + "5 5,3 6 3 3,4 4 4"), false);
    }
    
    public void testSimplifyer(final DelaneySymbol ds, final boolean good) {
        final DelaneySymbol cover = Covers.pseudoToroidalCover3D(ds);
        final DelaneySymbol simpler = new Simplifyer(cover).getSimplifiedSymbol();
        Assert.assertEquals(good, simpler.equals(good1) || simpler.equals(good2));
    }
}

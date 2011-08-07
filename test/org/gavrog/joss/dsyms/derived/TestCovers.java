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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;

/**
 * Unit test for class Covers.
 * 
 * @author Olaf Delgado
 * @version $Id: TestCovers.java,v 1.3 2007/04/23 22:45:23 odf Exp $
 */
public class TestCovers extends TestCase {
    private List z2;
    private List z3;
    
    protected void setUp() throws Exception {
        super.setUp();
        z2 = new LinkedList();
        z2.add(Whole.ZERO);
        z2.add(Whole.ZERO);
        z3 = new LinkedList();
        z3.add(Whole.ZERO);
        z3.add(Whole.ZERO);
        z3.add(Whole.ZERO);
        super.setUp();
    }
    
    protected void tearDown() throws Exception {
        z2 = null;
        z3 = null;
        super.tearDown();
    }
    
    public void testFiniteUniversalCover1() {
        testFiniteUniversalCover(new DSymbol("1:1,1,1:3,5"), 120);
    }
    
    public void testFiniteUniversalCover2() {
        testFiniteUniversalCover(new DSymbol("1 3:1,1,1,1:3,3,3"), 120);
    }
    
    public void testFiniteUniversalCover3() {
        testFiniteUniversalCover(new DSymbol("1 4:1,1,1,1,1:3,3,3,3"), 720);
    }
    
    public void testAllCovers() {
        final DSymbol ds = new DSymbol("1:1,1,1:3,3");
        final Set covers = new HashSet(Iterators.asList(Covers.allCovers(ds)));
        assertEquals(11, covers.size());
        assertTrue(covers.contains(ds));
        assertTrue(covers.contains(new DSymbol("2:2,2,2:3,3")));
        assertTrue(covers.contains(new DSymbol("3:1 3,2 3,1 3:3,3")));
        assertTrue(covers.contains(new DSymbol("4:2 3 4,1 3 4,1 2 4:3 3,3 3")));
        assertTrue(covers.contains(new DSymbol("6:2 4 6,6 3 5,2 4 6:3,3")));
        assertTrue(covers.contains(new DSymbol("6:2 4 6,6 3 5,2 5 6:3,3")));
        assertTrue(covers.contains(new DSymbol("6:1 3 5 6,2 3 4 6,1 4 5 6:3 3,3 3")));
        assertTrue(covers.contains(new DSymbol("8:2 4 6 8,6 3 5 8,4 3 7 8:3 3,3 3")));
        assertTrue(covers.contains(new DSymbol(
                "12:2 4 6 8 12 11,6 3 5 9 10 12,7 8 4 10 11 12:3 3 3,3 3")));
        assertTrue(covers.contains(new DSymbol(
                "12:2 4 6 8 9 11 12,6 3 5 9 8 10 12,7 8 3 4 10 11 12:3 3 3,3 3 3")));
        assertTrue(covers.contains(Covers.finiteUniversalCover(ds)));
    }
    
    public void testToroidalCover2D_1() {
        testToroidalCover2D(new DSymbol("1:1,1,1:3,6"), 12);
    }
    
    public void testToroidalCover2D_2() {
        testToroidalCover2D(new DSymbol("1:1,1,1:3,5"), 0);
    }
    
    public void testToroidalCover2D_3() {
        testToroidalCover2D(new DSymbol("3:1 2 3,2 3,1 3:8 4,3"), 24);
    }
    
    public void testPseudoToroidalCover3D_1() {
        testPseudoToroidalCover3D(new DSymbol("1 3:1,1,1,1:4,3,4"), 48);
    }
    
    public void testPseudoToroidalCover3D_2() {
        testPseudoToroidalCover3D(new DSymbol("1 3:1,1,1,1:4,3,3"), 0);
    }
    
    public void testPseudoToroidalCover3D_3() {
        testPseudoToroidalCover3D(new DSymbol("3 3:1 2 3,1 3,2 3,1 2 3:6 4,3,4 3"), 72);
    }
    
    private void testFiniteUniversalCover(final DelaneySymbol ds, final int n) {
        final DSCover cov = Covers.finiteUniversalCover(ds);
        assertEquals(n, cov.size());
        if (ds.dim() == 2 && ds.isSpherical2D()) {
            Assert.assertTrue(new Whole(4).equals(cov.curvature2D()));
        }
        assertEquals(ds, cov.minimal());
        final FpGroup G = new FundamentalGroup(cov).getPresentation();
        assertEquals(0, G.getGenerators().size());
        assertEquals(0, G.getRelators().size());
        assertEquals(new LinkedList(), G.abelianInvariants());
        assertEquals(ds.elements().next(), cov.image(cov.elements().next()));
    }
    
    private void testToroidalCover2D(final DelaneySymbol ds, final int n) {
        if (n == 0) {
            try {
                Covers.toroidalCover2D(ds);
                Assert.fail("should throw an UnsupportedOperationException");
            } catch (UnsupportedOperationException success) {
            }
        } else {
            final DSCover cov = Covers.toroidalCover2D(ds);
            assertEquals(n, cov.size());
            assertTrue(cov.curvature2D().isZero());
            assertEquals(ds, cov.minimal());
            final FundamentalGroup G = new FundamentalGroup(cov);
            assertEquals(0, G.getAxes().size());
            assertEquals(z2, G.getPresentation().abelianInvariants());
            assertEquals(ds.elements().next(), cov.image(cov.elements().next()));
        }
    }
    
    private void testPseudoToroidalCover3D(final DelaneySymbol ds, final int n) {
        final DSCover cov = Covers.pseudoToroidalCover3D(ds);
        if (n == 0) {
            assertNull(cov);
        } else {
            assertEquals(n, cov.size());
            assertEquals(ds, cov.minimal());
            final FundamentalGroup G = new FundamentalGroup(cov);
            assertEquals(0, G.getAxes().size());
            assertEquals(z3, G.getPresentation().abelianInvariants());
            assertEquals(ds.elements().next(), cov.image(cov.elements().next()));
        }
    }
}

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

package org.gavrog.joss.pgraphs.basic;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.Archive;
import org.gavrog.joss.pgraphs.io.NetParser;

import de.jreality.math.CubicBSpline.Periodic;

/**
 * Tests class PeriodicGraph.
 * 
 * @author Olaf Delgado
 * @version $Id: TestPeriodicGraph.java,v 1.42 2007/05/12 01:32:27 odf Exp $
 */
public class TestPeriodicGraph extends TestCase {
    private PeriodicGraph G, dia, cds;

    private INode v1, v2;
    private PeriodicGraph.CoverNode w1, w2;

    private IEdge e1, e2, e3, e4;
    private PeriodicGraph.CoverEdge f1, f2, f3, f4;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        G = new PeriodicGraph(3);
        v1 = G.newNode();
        v2 = G.newNode();
        e1 = G.newEdge(v1, v2);
        e2 = G.newEdge(v2, v2, new int[] { 1, 0, 0 });
        e3 = G.newEdge(v2, v1, new int[] { 0, -1, 0 });
        e4 = G.newEdge(v1, v1, new int[] { 0, 0, 1 });

        w1 = G.new CoverNode(v1, new Vector(1, 2, 3));
        w2 = G.new CoverNode(v2, new Vector(1, 0, 2));
        f1 = G.new CoverEdge(e1);
        f2 = G.new CoverEdge(e2, new Vector(0, 1, 0));
        f3 = G.new CoverEdge(e3, new Vector(1, 0, 1));
        f4 = G.new CoverEdge(e4, new Vector(0, -1, 40));

        dia = diamond();
        cds = CdSO4();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        v1 = v2 = null;
        e1 = e2 = e3 = e4 = null;
        G = dia = cds = null;
    }

    private PeriodicGraph diamond() {
        final PeriodicGraph dia = new PeriodicGraph(3);
        final INode v1 = dia.newNode();
        final INode v2 = dia.newNode();
        dia.newEdge(v1, v2, new int[] {0,0,0});
        dia.newEdge(v1, v2, new int[] {-1,0,0});
        dia.newEdge(v1, v2, new int[] {0,-1,0});
        dia.newEdge(v1, v2, new int[] {0,0,-1});
        return dia;
    }
    
    private PeriodicGraph doubleHexGrid() {
        final PeriodicGraph H = new PeriodicGraph(2);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        final INode v3 = H.newNode();
        final INode v4 = H.newNode();
        H.newEdge(v1, v2, new int[] {0,0});
        H.newEdge(v1, v2, new int[] {1,0});
        H.newEdge(v1, v2, new int[] {0,1});
        H.newEdge(v3, v4, new int[] {0,0});
        H.newEdge(v3, v4, new int[] {1,0});
        H.newEdge(v3, v4, new int[] {0,1});
        H.newEdge(v1, v3, new int[] {-1,-1});
        return H;
    }
    
    private PeriodicGraph hexGrid() {
        final PeriodicGraph H = new PeriodicGraph(2);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        H.newEdge(v1, v2, new int[] {0,0});
        H.newEdge(v1, v2, new int[] {1,0});
        H.newEdge(v1, v2, new int[] {0,1});
        return H;
    }
    
    private PeriodicGraph CdSO4() {
        final PeriodicGraph cds = new PeriodicGraph(3);
        final INode w1 = cds.newNode();
        final INode w2 = cds.newNode();
        final INode w3 = cds.newNode();
        final INode w4 = cds.newNode();
        cds.newEdge(w1, w3, new int[] {-1, 0, 0});
        cds.newEdge(w1, w3, new int[] { 0, 0, 0});
        cds.newEdge(w1, w4, new int[] { 0, 0, 0});
        cds.newEdge(w1, w4, new int[] { 0, 1, 1});
        cds.newEdge(w2, w3, new int[] { 0,-1, 0});
        cds.newEdge(w2, w3, new int[] { 0, 0,-1});
        cds.newEdge(w2, w4, new int[] { 0, 0, 0});
        cds.newEdge(w2, w4, new int[] { 1, 0, 0});
        return cds;
    }
    
    public void testCopyConstructor() {
    	final PeriodicGraph copy = new PeriodicGraph(cds);
    	assertEquals(cds, copy);
    }
    
    public void testNewEdge() {
        final IEdge e1 = G.newEdge(v1, v2, new int[] { 1, 1, 1 });
        assertEquals(new Vector(1, 1, 1), G.getShift(e1));
        final String s1 = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(1,2,[1,1,1])"
                          + "(2,2,[-1,0,0])";
        assertEquals(s1, G.toString());
        G.delete(e1);
        final INode v3 = G.newNode();
        final IEdge e2 = G.newEdge(v1, v3);
        assertEquals(new Vector(0, 0, 0), G.getShift(e2));
        final String s2 = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(1,3,[0,0,0])"
                          + "(2,2,[-1,0,0])";
        assertEquals(s2, G.toString());
        try { // duplicate edge should be vetoed
            G.newEdge(v1, v2);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try { // loop with trivial shift should be vetoed
            G.newEdge(v3, v3);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try { // bad shift dimension should be vetoed
            G.newEdge(v3, v3, new int[] { 1, 2, 3, 4 });
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        G.newEdge(v3, v3, new int[] { 1, 2, 3 });
    }

    public void testDelete() {
        G.delete(e3);
        try {
            G.getShift(e3);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        assertEquals("(1,1,[0,0,-1])(1,2,[0,0,0])(2,2,[-1,0,0])", G.toString());
    }

    public void testShiftNode() {
        assertTrue(G.isBarycentric(G.barycentricPlacement()));
        G.shiftNode(v2, new Vector(1, 1, 0));
        final String s = "(1,1,[0,0,-1])(1,2,[-1,-1,0])(1,2,[-1,0,0])(2,2,[-1,0,0])";
        assertEquals(s, G.toString());
        assertTrue(G.isBarycentric(G.barycentricPlacement()));
    }
    
    public void testGetShift() {
        assertEquals(new Vector(0, 0, 0), G.getShift(e1));
        assertEquals(new Vector(1, 0, 0), G.getShift(e2));
        assertEquals(new Vector(0, -1, 0), G.getShift(e3));
        assertEquals(new Vector(0, 0, 1), G.getShift(e4));
        assertEquals(new Vector(0, 0, 0), G.getShift(e1.reverse()));
        assertEquals(new Vector(-1, 0, 0), G.getShift(e2.reverse()));
        assertEquals(new Vector(0, 1, 0), G.getShift(e3.reverse()));
        assertEquals(new Vector(0, 0, -1), G.getShift(e4.reverse()));
    }

    public void testGetEdge() {
        final IEdge test1 = G.getEdge(v1, v2, new Vector(0, 1, 0));
        assertEquals(e3.reverse(), test1);
        assertEquals(G.getShift(e3.reverse()), G.getShift(test1));
        assertEquals(e3, G.getEdge(v2, v1, new Vector(0, -1, 0)));
        assertNull(G.getEdge(v2, v1, new Vector(1, -1, 0)));
        assertEquals(e2, G.getEdge(v2, v2, new Vector(1, 0, 0)));

        final IEdge test2 = G.getEdge(v2, v2, new Vector(-1, 0, 0));
        assertEquals(e2.reverse(), test2);
        assertEquals(G.getShift(e2.reverse()), G.getShift(test2));
    }

    public void testCoverNodeDegree() {
        assertEquals(4, w1.degree());
        assertEquals(4, w2.degree());
    }
    
    public void testCoverEdgeSource() {
        assertEquals(G.new CoverNode(v1), f1.source());
        assertEquals(G.new CoverNode(v2, new Vector(0, 1, 0)), f2.source());
        assertEquals(G.new CoverNode(v2, new Vector(1, 0, 1)), f3.source());
        assertEquals(G.new CoverNode(v1, new Vector(0, -1, 40)), f4.source());
    }
    
    public void testCoverEdgeTarget() {
        assertEquals(G.new CoverNode(v2), f1.target());
        assertEquals(G.new CoverNode(v2, new Vector(1, 1, 0)), f2.target());
        assertEquals(G.new CoverNode(v1, new Vector(1, -1, 1)), f3.target());
        assertEquals(G.new CoverNode(v1, new Vector(0, -1, 41)), f4.target());
    }
    
    public void testCoverEdgeOpposite() {
        final PeriodicGraph.CoverNode w1 = G.new CoverNode(v1);
        final PeriodicGraph.CoverNode w2 = G.new CoverNode(v2);
        final PeriodicGraph.CoverNode w3 = G.new CoverNode(v2, new Vector(0, 1, 0));
        final PeriodicGraph.CoverNode w4 = G.new CoverNode(v2, new Vector(1, 1, 0));
        assertEquals(w2, f1.opposite(w1));
        assertEquals(w1, f1.opposite(w2));
        try {
            e1.opposite(w3);
            fail("should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        assertEquals(w3, f2.opposite(w4));
        assertEquals(w4, f2.opposite(w3));
    }
    
    public void testCoverEdgeReverse() {
        final IEdge r1 = f1.reverse();
        assertEquals(f1.target(), r1.source());
        assertEquals(f1.source(), r1.target());
        assertEquals(f1.unoriented(), r1.unoriented());
        assertFalse(f1.oriented().equals(r1.unoriented()));
        assertFalse(f1.equals(r1));
    }
    
    public void testCoverElementOwner() {
        assertEquals(G, w1.owner());
        assertEquals(G, f1.owner());
    }
    
    public void testCoverElementIncidences() {
        List incidences;
        
        // --- nodes:
        incidences = Iterators.asList(w2.incidences());
        assertEquals(4, incidences.size());
        assertTrue(incidences.contains(G.new CoverEdge(e1.reverse(), new Vector(1, 0, 2))));
        assertTrue(incidences.contains(G.new CoverEdge(e3, new Vector(1, 0, 2))));
        assertTrue(incidences.contains(G.new CoverEdge(e2, new Vector(1, 0, 2))));
        assertTrue(incidences.contains(G.new CoverEdge(e2.reverse(), new Vector(1, 0, 2))));
        
        // --- edges:
        incidences = Iterators.asList(f3.incidences());
        assertEquals(2, incidences.size());
        assertTrue(incidences.contains(G.new CoverNode(v1, new Vector(1, -1, 1))));
        assertTrue(incidences.contains(G.new CoverNode(v2, new Vector(1, 0, 1))));
    }
    
    public void testHashCodes() {
        final INode<Long> v = v1;
        final INode w = (INode) G.getElement(v.id());
        assertNotSame(v, w);
        assertEquals(v, w);
        assertEquals(v.hashCode(), w.hashCode());
        final IEdge<Long> e = e1;
        final IEdge f = (IEdge) G.getElement(e.id());
        assertNotSame(e, f);
        assertEquals(e, f);
        assertEquals(e.hashCode(), f.hashCode());
    }
    
    public void testToString() {
        final String s = "(1,1,[0,0,-1])(1,2,[0,0,0])(1,2,[0,1,0])(2,2,[-1,0,0])";
        assertEquals(s, G.toString());
    }
    
    public void testCoordinationSequence() {
        final INode start = (INode) dia.nodes().next();
        final Iterator cs = dia.coordinationSequence(start);
        assertEquals(new Integer(1), cs.next());
        assertEquals(new Integer(4), cs.next());
        assertEquals(new Integer(12), cs.next());
        assertEquals(new Integer(24), cs.next());
        assertEquals(new Integer(42), cs.next());
        assertEquals(new Integer(64), cs.next());
        assertEquals(new Integer(92), cs.next());
        assertEquals(new Integer(124), cs.next());
        assertEquals(new Integer(162), cs.next());
        assertEquals(new Integer(204), cs.next());
        assertEquals(new Integer(252), cs.next());
    }
    
    public void testShortestCycleAtAngle() {
    	Iterator<INode<Long>> diaNodes = dia.nodes();
    	final INode a = (INode) diaNodes.next();
    	final INode b = (INode) diaNodes.next();
    	final PeriodicGraph.CoverNode u = dia.new CoverNode(b);
    	final PeriodicGraph.CoverNode v = dia.new CoverNode(a);
    	final PeriodicGraph.CoverNode w =
    			dia.new CoverNode(b, new Vector(-1, 0, 0));
    	List<PeriodicGraph.CoverNode> cycle = dia.shortestCycleAtAngle(u, v, w);
    	
    	assertEquals(6, cycle.size());
    	assertEquals(v, cycle.get(0));
    	assertTrue(cycle.get(1).equals(u) || cycle.get(1).equals(w));
    	if (cycle.get(1).equals(u))
    		assertEquals(w, cycle.get(5));
    	else
    		assertEquals(u, cycle.get(5));
    }
    
    private INode firstNode(PeriodicGraph g) {
        return (INode) g.nodes().next();
    }
    
    public void testPointSymbol() {
        PeriodicGraph hex = hexGrid();
        PeriodicGraph hexd = doubleHexGrid();
        
        assertEquals("6^6", dia.pointSymbol(firstNode(dia)));
        assertEquals("6^5.8", cds.pointSymbol(firstNode(cds)));
        assertEquals("6^3", hex.pointSymbol(firstNode(hex)));
        assertEquals("6^6", hexd.pointSymbol(firstNode(hexd)));
    }
    
    public void testIsConnected() {
        final PeriodicGraph H = new PeriodicGraph(3);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        assertFalse(H.isConnected());
        H.newEdge(v1, v2, new int[] {1,0,0});
        H.newEdge(v1, v2, new int[] {0,1,0});
        H.newEdge(v1, v2, new int[] {0,0,1});
        assertFalse(H.isConnected());
        H.newEdge(v1, v2, new int[] {3,0,0});
        assertFalse(H.isConnected());
        final IEdge e = H.newEdge(v1, v2, new int[] {2,0,0});
        assertTrue(H.isConnected());
        H.delete(e);
        H.newEdge(v1, v2, new int[] {0,0,0});
        assertTrue(H.isConnected());
    }
    
    public void testConnectedComponents() {
    	final PeriodicGraph P = new PeriodicGraph(0);
    	P.newNode();
        final PeriodicGraph H = new PeriodicGraph(3);
        final INode v1 = H.newNode();
        final INode v2 = H.newNode();
        List components = H.connectedComponents();
        assertEquals(2, components.size());
        PeriodicGraph.Component c = (PeriodicGraph.Component) components.get(0);
        assertEquals(0, c.getDimension());
        assertEquals(Whole.ZERO, c.getMultiplicity());
        assertEquals(P, c.getGraph());
        
        H.newEdge(v1, v2, new int[] {1,0,0});
        H.newEdge(v1, v2, new int[] {0,1,0});
        H.newEdge(v1, v2, new int[] {0,0,1});
        components = H.connectedComponents();
        assertEquals(1, components.size());
        c = (PeriodicGraph.Component) components.get(0);
        assertEquals(2, c.getDimension());
        assertEquals(Whole.ZERO, c.getMultiplicity());
        assertEquals(hexGrid(), c.getGraph());
        
        H.newEdge(v1, v2, new int[] {2,1,0});
        components = H.connectedComponents();
        assertEquals(1, components.size());
        c = (PeriodicGraph.Component) components.get(0);
        assertEquals(3, c.getDimension());
        assertEquals(new Whole(2), c.getMultiplicity());
        assertEquals(diamond(), c.getGraph());
    }
    
    public void testBarycentricPositions() {
        assertTrue(G.isBarycentric(G.barycentricPlacement()));
        assertTrue(dia.isBarycentric(dia.barycentricPlacement()));
        assertTrue(cds.isBarycentric(cds.barycentricPlacement()));
    }
    
    public void testStabilityAndLadderness() {
        assertTrue(G.isStable());
        assertTrue(dia.isStable());
        assertTrue(cds.isStable());
        assertTrue(G.isLocallyStable());
        assertTrue(dia.isLocallyStable());
        assertTrue(cds.isLocallyStable());
        assertFalse(G.isLadder());
        assertFalse(dia.isLadder());
        assertFalse(cds.isLadder());
        
        final PeriodicGraph H = doubleHexGrid();
        assertFalse(H.isStable());
        assertTrue(H.isLocallyStable());
        assertTrue(H.isLadder());
        
        final PeriodicGraph H2 = new PeriodicGraph(2);
        final INode w1 = H2.newNode();
        final INode w2 = H2.newNode();
        final INode w3 = H2.newNode();
        H2.newEdge(w1, w1, new int[] {1,0});
        H2.newEdge(w1, w2, new int[] {0,0});
        final IEdge e1 = H2.newEdge(w1, w2, new int[] {0,1});
        H2.newEdge(w1, w3, new int[] {0,0});
        final IEdge e2 = H2.newEdge(w1, w3, new int[] {0,1});
        final IEdge e3 = H2.newEdge(w2, w3, new int[] {0,0});
        assertFalse(H2.isStable());
        assertFalse(H2.isLocallyStable());
        assertFalse(H2.isLadder());
        
        H2.delete(e1);
        H2.delete(e2);
        H2.delete(e3);
        H2.newEdge(w1, w2, new int[] {1,1});
        H2.newEdge(w1, w3, new int[] {-1,1});
        assertFalse(H2.isStable());
        assertTrue(H2.isLocallyStable());
        assertFalse(H2.isLadder());
        
        final PeriodicGraph H3 = new PeriodicGraph(3);
        final INode u1 = H3.newNode();
        final INode u2 = H3.newNode();
        H3.newEdge(u1, u1, new int[] {1,0,0});
        H3.newEdge(u1, u1, new int[] {0,1,0});
        H3.newEdge(u2, u2, new int[] {0,0,1});
        H3.newEdge(u1, u2, new int[] {0,0,0});
        assertFalse(H3.isStable());
        assertTrue(H3.isLocallyStable());
        assertFalse(H3.isLadder());
    }
    
    public void testTranslationalEquivalenceClasses() {
        assertFalse(dia.translationalEquivalenceClasses().hasNext());
        assertFalse(G.translationalEquivalenceClasses().hasNext());
        
        List classes;
        
        classes = Iterators.asList(cds.translationalEquivalenceClasses());
        assertEquals(2, classes.size());
        
        // --- do this twice to see if caching works
        classes = Iterators.asList(doubleHexGrid().translationalEquivalenceClasses());
        assertEquals(2, classes.size());
        classes = Iterators.asList(doubleHexGrid().translationalEquivalenceClasses());
        assertEquals(2, classes.size());
        
        final PeriodicGraph H = new PeriodicGraph(3);
        final INode w1 = H.newNode();
        final INode w2 = H.newNode();
        H.newEdge(w1, w1, new int[] {1,0,0});
        H.newEdge(w1, w1, new int[] {0,1,0});
        H.newEdge(w2, w2, new int[] {1,0,0});
        H.newEdge(w2, w2, new int[] {0,1,0});
        H.newEdge(w1, w2, new int[] {0,0,0});
        H.newEdge(w1, w2, new int[] {0,0,1});
        new Morphism(w1, w2, Matrix.one(3));
        classes = Iterators.asList(H.translationalEquivalenceClasses());
        assertEquals(1, classes.size());
    }
    
    public void testMinimalImage() {
    	for (int i = 0; i < 3; ++i) {
			final PeriodicGraph cds1 = cds.minimalImage();
			assertEquals(3, cds1.getDimension());
			assertEquals(2, cds1.numberOfNodes());
			assertEquals(4, cds1.numberOfEdges());
			assertTrue(cds1.isConnected());
			assertTrue(cds1.isStable());
			final Iterator nodes = cds1.nodes();
			final INode w1 = (INode) nodes.next();
			final INode w2 = (INode) nodes.next();
			final List loops1 = Iterators.asList(cds1.directedEdges(w1, w1));
			final List loops2 = Iterators.asList(cds1.directedEdges(w2, w2));
			assertEquals(1, loops1.size());
			assertEquals(1, loops2.size());
		}
        
        try {
            doubleHexGrid().minimalImage();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
        
        assertSame(dia, dia.minimalImage());
    }
    
    public void testCharacteristicBases() {
        testCharacteristicBases(dia, 48);
        testCharacteristicBases(G, 16);
        testCharacteristicBases(cds.minimalImage(), 16);
        testCharacteristicBases(doubleHexGrid(), 24);
    }
    
    public void testCharacteristicBases(final PeriodicGraph G, final int expectedNr) {
        final List bases = G.characteristicBases();
        assertEquals(expectedNr, bases.size());

        final int d = G.getDimension();
        final Map pos = G.barycentricPlacement();
        
        final Set seen = new HashSet();
        for (final Iterator iter = bases.iterator(); iter.hasNext();) {
            final List basis = (List) iter.next();
            final List key = new ArrayList();
            final Matrix M = new Matrix(d, d);
            for (int i = 0; i < basis.size(); ++i) {
                final IEdge e = (IEdge) basis.get(i);
                final INode v = e.source();
                final INode w = e.target();
                final Point pv = (Point) pos.get(v);
                final Point pw = (Point) pos.get(w);
                final Vector s = G.getShift(e);
                M.setRow(i, ((Vector) pw.minus(pv).plus(s)).getCoordinates());
                key.add(new Pair(e, s));
            }
            assertEquals(d, M.rank());
            assertFalse(seen.contains(key));
            seen.add(key);
            assertTrue(seen.contains(key));
        }
    }
    
    public void testSymmetries() {
        testSymmetries(dia, 48);
        testSymmetries(G, 16);
        testSymmetries(cds.minimalImage(), 16);
        testSymmetries(doubleHexGrid(), 12);
    }
    
    public void testSymmetries(final PeriodicGraph G, final int expectedNr) {
        final Set symmetries = G.symmetries();
        assertEquals(expectedNr, symmetries.size());
    }
    
    public void testSymmetricBasis() {
        testSymmetricBasis(G);
        testSymmetricBasis(dia);
        testSymmetricBasis(doubleHexGrid());
    }
    
    public void testSymmetricBasis(final PeriodicGraph G) {
        final Real eps = new FloatingPoint(1e-12);
        final int d = G.getDimension();
        final Matrix I = Matrix.one(d+1);
        final CoordinateChange c = new CoordinateChange(G.symmetricBasis());
        for (final Iterator syms = G.symmetries().iterator(); syms.hasNext();) {
            final Operator op = ((Morphism) syms.next()).getLinearOperator();
            final Matrix A = ((Operator) op.times(c)).getCoordinates();
            final Matrix D = (Matrix) A.times(A.transposed());
            assertTrue(D.minus(I).norm().isLessThan(eps));
        }
    }
    
    public void testNodeOrbits() {
        final List nodeOrbits = Iterators.asList(G.nodeOrbits());
        assertEquals(1, nodeOrbits.size());
        assertEquals(2, ((Set) nodeOrbits.get(0)).size());
    }
    
    public void testNodeStabilizer() {
        assertEquals(8, G.nodeStabilizer(v1).size());
        assertEquals(8, G.nodeStabilizer(v2).size());
    }
    
    public void testEdgeOrbits() {
        final List edgeOrbits = Iterators.asList(G.edgeOrbits());
        assertEquals(2, edgeOrbits.size());
        assertEquals(2, ((Set) edgeOrbits.get(0)).size());
        assertEquals(2, ((Set) edgeOrbits.get(1)).size());
    }
    
    public void testCanonical() {
        assertEquals("(1,2,[0,0,0])(1,2,[0,0,1])(1,2,[0,1,0])(1,2,[1,0,0])",
                dia.canonical().toString());
        assertEquals(G.canonical().toString(), cds.minimalImage().canonical().toString());
        assertFalse(G.canonical().toString().equals(dia.canonical().toString()));
        assertEquals(makeTestGraph(2).canonical().toString(), makeTestGraph(1)
                .canonical().toString());
    }
    
    public void testHex() {
        verifyKey("3 1 1 -1 -1 0 1 1 -1 0 0 1 1 0 -1 0 1 1 0 0 -1");
    }
    
    public void testInvariant() {
        assertEquals(G.invariant(), cds.minimalImage().invariant());
        assertFalse(G.invariant().equals(dia.invariant()));
        assertEquals(makeTestGraph(2).invariant(), makeTestGraph(1).invariant());
        
        assertEquals("3 1 2 0 0 0 1 2 0 0 1 1 2 0 1 0 1 2 1 0 0", dia.invariant()
                .toString());
        assertEquals("3 1 1 -1 0 0 1 2 0 0 0 1 2 0 1 0 2 2 0 0 -1", cds.minimalImage()
                .invariant().toString());
        verifyKey("3 1 2 0 0 0 1 2 0 0 1 1 2 0 1 0 1 2 1 0 0");
        verifyKey("3 1 1 -1 0 0 1 2 0 0 0 1 2 0 1 0 2 2 0 0 -1");
        verifyKey("2 1 2 0 0 1 2 0 1 1 2 1 0");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 3 0 1 0 2 4 1 0 0 3 4 0 0 1");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 5 0 0 0 2 6 0 0 0 3 4 0 0 0 "
                + "3 7 0 0 0 4 8 0 0 0 5 6 0 0 0 5 9 0 0 0 6 10 0 0 0 7 10 1 0 0 "
                + "7 11 0 0 0 8 9 0 1 0 8 12 0 0 0 9 12 0 -1 0 10 11 -1 0 0 11 12 0 0 1");
        verifyKey("3 1 2 0 0 0 1 2 0 1 0 1 3 0 0 0 1 3 1 0 0 2 3 0 0 1 2 3 1 -1 -1");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 5 0 0 0 2 6 0 0 0 3 7 0 0 0 "
                + "3 8 0 0 0 4 8 0 0 0 4 9 0 0 0 5 10 0 0 0 5 11 0 0 0 6 10 0 0 0 "
                + "6 12 0 0 0 7 11 0 1 0 7 12 1 0 0 8 10 0 0 1 9 11 -1 0 1 9 12 0 -1 1");
        verifyKey("3 1 2 0 0 0 1 3 0 0 0 1 4 0 0 0 2 5 0 0 0 3 6 0 0 0 4 7 0 0 0 "
                + "5 8 0 0 0 5 9 0 0 0 6 9 1 0 0 6 10 0 0 0 7 8 0 0 1 7 10 0 1 0");
        verifyKey("3 1 1 -1 0 0 1 2 0 0 0 1 2 0 0 1 1 3 0 0 0 1 3 0 1 0 1 4 0 0 0 "
                + "2 2 -1 0 0 2 3 0 0 0 2 4 -1 -1 -1 2 4 0 0 -1 3 3 -1 0 0 3 4 -1 -1 -1 "
                + "3 4 0 -1 0 4 4 -1 0 0");
    }
    
    private PeriodicGraph makeTestGraph(final int type) {
        final Vector x = new Vector(1, 0);
        final Vector y = new Vector(0, 1);
        final PeriodicGraph G = new PeriodicGraph(2);
        final INode v1 = G.newNode();
        final INode v2 = G.newNode();
        final INode v3 = G.newNode();
        final INode v4 = G.newNode();
        G.newEdge(v1, v2);
        G.newEdge(v1, v3);
        G.newEdge(v1, v4);
        G.newEdge(v2, v3, x);
        if (type == 1) {
            G.newEdge(v2, v4, x);
        } else {
            G.newEdge(v2, v4, y);
        }
        G.newEdge(v3, v4, y);
        return G;
    }
    
    private void verifyKey(final String key) {
        final List numbers = new ArrayList();
        final String fields[] = key.split("\\s+");
        for (int i = 0; i < fields.length; ++i) {
            numbers.add(new Integer(fields[i]));
        }
        final int d = ((Integer) numbers.get(0)).intValue();
        final int n = (numbers.size() - 1) / (d + 2);
        final PeriodicGraph G = new PeriodicGraph(d);
        final List nodes = new ArrayList();
        nodes.add(null);
        for (int i = 0; i < n; ++i) {
            final int offset = 1 + i * (d + 2);
            final int s = ((Integer) numbers.get(offset)).intValue();
            final int t = ((Integer) numbers.get(offset + 1)).intValue();
            if (s == nodes.size()) {
                nodes.add(G.newNode());
            }
            if (t == nodes.size()) {
                nodes.add(G.newNode());
            }
            if (s >= nodes.size() || t >= nodes.size()) {
                throw new RuntimeException("something's wrong here");
            }
            final int[] shift = new int[d];
            for (int j = 0; j < d; ++j) {
                final Integer x = (Integer) numbers.get(offset + 2 + j);
                shift[j] = x.intValue();
            }
            G.newEdge((INode) nodes.get(s), (INode) nodes.get(t), shift);
        }
        assertEquals(key, G.getSystreKey());
    }
    
    // CAVEAT: the following test takes hours. Run only under special circumstances.
    
    public void xtestInvariantsRCSR() {
        final Package pkg = Archive.class.getPackage();
        final String packagePath = pkg.getName().replaceAll("\\.", "/");
        final String archivePath = packagePath + "/rcsr.arc";

        final Archive rcsr = new Archive("1.0");
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(archivePath);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        rcsr.addAll(reader);
        
        final List keys = new ArrayList();
        keys.addAll(rcsr.keySet());
        Collections.sort(keys, new Comparator() {
            public int compare(final Object arg0, final Object arg1) {
                final String s0 = (String) arg0;
                final String s1 = (String) arg1;
                final int d = s0.length() - s1.length();
                if (d != 0) {
                    return d;
                } else {
                    return s0.compareTo(s1);
                }
            }
        });
        
        //int i = 0;
        for (final Iterator iter = keys.iterator(); iter.hasNext();) {
            //System.err.println(++i);
            final String key = (String) iter.next();
            verifyKey(key);
        }
    }
    
    public void testEquals() {
        assertEquals(G, G);
        assertEquals(dia, dia);
        assertEquals(G, cds.minimalImage());
        assertFalse(dia.equals(G));
        assertEquals(makeTestGraph(2), makeTestGraph(1));
    }
    
    public void testHashCode() {
        assertEquals(G.hashCode(), G.hashCode());
        assertEquals(dia.hashCode(), dia.hashCode());
        assertEquals(G.hashCode(), cds.minimalImage().hashCode());
        assertFalse(dia.hashCode() == G.hashCode());
    }
    
    public void testConventionalCellCover() {
        testConventionalCellCover(dia);
        testConventionalCellCover(cds.minimalImage());
        testConventionalCellCover(NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "  ID    sqc15\n"
                + "  EDGES\n"
                + "  1 1 1 0 1\n"
                + "  1 1 0 1 0\n"
                + "  1 1 1 1 0\n"
                + "  1 1 1 0 0\n"
                + "  1 1 0 0 1\n"
                + "  1 1 0 1 1\n"
                + "END\n"
                ));
        testConventionalCellCover(NetParser.stringToNet(""
                + "PERIODIC_GRAPH\n"
                + "   ID    sqc1135\n"
                + "   EDGES\n"
                + "     1   3 -1  -1  -1\n"
                + "     1   1  0   0   1\n"
                + "     1   2  0   0   0\n"
                + "     1   3 -1   0   0\n"
                + "     3   3  1   1   1\n"
                + "     2   3  0   0   0\n"
                + "     1   1  1   1   1\n"
                + "     1   2  0   1   0\n"
                + "     3   3  0   0   1\n"
                + "     2   3  0  -1   0\n"
                + "     1   4  0   0  -1\n"
                + "     3   4  0   0   0\n"
                + "     3   4  1   0   0\n"
                + "     1   4 -1   0  -1\n"
                + "END\n"
        ));
    }
    
    public void testConventionalCellCover(final PeriodicGraph G) {
        final Cover cov = G.conventionalCellCover();
        final Map pos = cov.barycentricPlacement();
        final Map posG = G.barycentricPlacement();
        assertTrue(cov.isBarycentric(pos));
        assertEquals(G, cov.minimalImage());
        for (final Iterator iter = cov.nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final Point p = (Point) pos.get(v);
            assertEquals(p, p.modZ());
            assertEquals(p, cov.liftedPosition(v, posG));
        }
    }
}

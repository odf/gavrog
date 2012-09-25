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

import java.util.Iterator;
import java.util.List;

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.IGraphElement;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.UndirectedGraph;

import junit.framework.TestCase;

/**
 * Tests class UndirectedGraph.
 * @author Olaf Delgado
 * @version $Id: TestUndirectedGraph.java,v 1.3 2006/04/04 22:59:27 odf Exp $
 */
public class TestUndirectedGraph extends TestCase {
    private UndirectedGraph G;
    private INode v1, v2, v3, v4, v5;
    private IEdge e1, e2, e3, e4;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        G = new UndirectedGraph();
        v1 = G.newNode();
        v2 = G.newNode();
        v3 = G.newNode();
        v4 = G.newNode();
        v5 = G.newNode();
        e1 = G.newEdge(v1, v2);
        e2 = G.newEdge(v1, v3);
        e3 = G.newEdge(v1, v4);
        e4 = G.newEdge(v2, v3);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        G = null;
        v1 = v2 = v3 = v4 = v5 = null;
        e1 = e2 = e3 = e4 = null;
        super.tearDown();
    }

    public void testNodes() {
        final List nodes = Iterators.asList(G.nodes());
        assertEquals(5, nodes.size());
        assertTrue(nodes.contains(v1));
        assertTrue(nodes.contains(v2));
        assertTrue(nodes.contains(v3));
        assertTrue(nodes.contains(v4));
        assertTrue(nodes.contains(v5));
    }

    public void testEdges() {
        final List edges = Iterators.asList(G.edges());
        assertEquals(4, edges.size());
        assertTrue(edges.contains(e1));
        assertTrue(edges.contains(e2));
        assertTrue(edges.contains(e3));
        assertTrue(edges.contains(e4));
    }

    public void testGetElements() {
        INode v = G.getNode(v1.id());
        assertTrue(v instanceof INode);
        assertEquals(v1.id(), v.id());
        assertEquals(v1, v);
        IEdge e = G.getEdge(e1.id());
        assertTrue(e instanceof IEdge);
        assertEquals(e1.id(), e.id());
        assertEquals(e1, e);
    }

    public void testHasElements() {
        assertTrue(G.hasNode(v1));
        assertTrue(G.hasEdge(e1));
        assertTrue(G.hasEdge(G.getEdge(e1.id())));
        assertFalse(G.hasEdge(null));
        final UndirectedGraph H = new UndirectedGraph();
        final INode v = H.newNode();
        assertFalse(G.hasNode(v));
        assertFalse(H.hasNode(v1));
    }

    public void testOrientedEdge() {
        final IEdge e1o = e1.oriented();
        assertTrue(e1.equals(e1.reverse()));
        assertFalse(e1o.equals(e1o.reverse()));
    }
    
    public void testConnectingEdges() {
        final Iterator edges = G.connectingEdges(v2, v1);
        assertTrue(edges.hasNext());
        final IEdge e = (IEdge) edges.next();
        assertFalse(edges.hasNext());
        assertEquals(v2, e.source());
        assertEquals(v1, e.target());
    }

    public void testDirectedEdges() {
        final Iterator edges = G.connectingEdges(v2, v4);
        assertFalse(edges.hasNext());
    }

    public void testNewNode() {
        G.newNode();
        G.newNode();
        assertEquals("(1,2)(1,3)(1,4)(2,3)(5)(6)(7)", G.toString());
    }

    public void testNewEdge() {
        G.newEdge(v2, v1);
        assertEquals("(1,2)(1,2)(1,3)(1,4)(2,3)(5)", G.toString());
        final List edges = Iterators.asList(G.connectingEdges(v1, v2));
        assertEquals(2, edges.size());
    }

    public void testDelete() {
        G.delete(e3);
        G.delete(v4);
        assertEquals("(1,2)(1,3)(2,3)(5)", G.toString());
        try {
            G.delete(v1);
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        assertEquals("(1,2)(1,3)(1,4)(2,3)(5)", G.toString());
    }
    
    public void testLoop() {
        final IEdge loop = G.newEdge(v1, v1);
        assertEquals("(1,1)(1,2)(1,3)(1,4)(2,3)(5)", G.toString());
        assertEquals(1, Iterators.size(G.directedEdges(v1, v1)));
        assertEquals(v1, loop.source());
        assertEquals(v1, loop.target());
        assertEquals(v1, loop.reverse().source());
        assertEquals(v1, loop.reverse().target());
        assertEquals(v1, loop.opposite(v1));

        List incidences;
        incidences = Iterators.asList(v1.incidences());
        assertEquals(4, incidences.size());
        assertTrue(incidences.contains(loop));
        incidences = Iterators.asList(loop.incidences());
        assertEquals(1, incidences.size());
        assertTrue(incidences.contains(v1));

        G.delete(loop);
        assertEquals("(1,2)(1,3)(1,4)(2,3)(5)", G.toString());
    }
    
    public void testNodeDegree() {
        assertEquals(3, v1.degree());
        assertEquals(2, v2.degree());
        assertEquals(2, v3.degree());
        assertEquals(1, v4.degree());
        assertEquals(0, v5.degree());
        G.newEdge(v2, v1);
        assertEquals(4, v1.degree());
        G.newEdge(v5, v5);
        assertEquals(2, v5.degree());
    }
    
    public void testEdgeSource() {
        assertEquals(v1, e1.source());
        assertEquals(v1, e2.source());
        assertEquals(v1, e3.source());
        assertEquals(v2, e4.source());
        assertEquals(v2, e1.reverse().source());
        assertEquals(v3, e2.reverse().source());
        assertEquals(v4, e3.reverse().source());
        assertEquals(v3, e4.reverse().source());
    }
    
    public void testEdgeTarget() {
        assertEquals(v2, e1.target());
        assertEquals(v3, e2.target());
        assertEquals(v4, e3.target());
        assertEquals(v3, e4.target());
        assertEquals(v1, e1.reverse().target());
        assertEquals(v1, e2.reverse().target());
        assertEquals(v1, e3.reverse().target());
        assertEquals(v2, e4.reverse().target());
    }
    
    public void testEdgeOpposite() {
        assertEquals(v2, e1.opposite(v1));
        assertEquals(v1, e1.opposite(v2));
        try {
            e1.opposite(v3);
            fail("should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        assertEquals(v3, e2.opposite(v1));
        assertEquals(v1, e2.opposite(v3));
    }
    
    public void testEdgeReverse() {
        final IEdge r1 = e1.reverse();
        assertEquals(v2, r1.source());
        assertEquals(v1, r1.target());
        assertEquals(e1, r1);
    }
    
    public void testElementOwner() {
        assertEquals(G, v1.owner());
        assertEquals(G, G.getNode(v1.id()).owner());
        assertEquals(G, e1.owner());
    }
    
    public void testElementId() {
        assertEquals(v1.id(), G.getNode(v1.id()).id());
        assertEquals(e1.reverse().id(), e1.id());
    }
    
    public void testElementIncidences() {
        List incidences;
        
        // --- nodes:
        incidences = Iterators.asList(v2.incidences());
        assertEquals(2, incidences.size());
        assertTrue(incidences.contains(e1));
        assertTrue(incidences.contains(e1.reverse()));
        assertTrue(incidences.contains(e4));
        incidences = Iterators.asList(v5.incidences());
        assertEquals(0, incidences.size());
        
        // --- edges:
        incidences = Iterators.asList(e3.incidences());
        assertEquals(2, incidences.size());
        assertTrue(incidences.contains(v1));
        assertTrue(incidences.contains(v4));
    }
}

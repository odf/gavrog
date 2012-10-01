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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Vector;

/**
 * @author Olaf Delgado
 * @version $Id: TestMorphism.java,v 1.6 2007/04/23 20:57:06 odf Exp $
 */
public class TestMorphism extends TestCase {
    private PeriodicGraph cds, dia, x, y;
    private INode v1, v2, w1, w2, x1, x2, y1;
    private IEdge e1, e2, e3, e4;
    private Operator inversion, rot_xyz, rot_y, stretch_x;
    private Morphism autoDia1, autoDia2, autoCds1, nonInjective;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        cds = new PeriodicGraph(3);
        v1 = cds.newNode();
        v2 = cds.newNode();
        e1 = cds.newEdge(v1, v2);
        e2 = cds.newEdge(v2, v2, new int[] { 1, 0, 0 });
        e3 = cds.newEdge(v2, v1, new int[] { 0, -1, 0 });
        e4 = cds.newEdge(v1, v1, new int[] { 0, 0, 1 });

        dia = new PeriodicGraph(3);
        w1 = dia.newNode();
        w2 = dia.newNode();
        dia.newEdge(w1, w2, new int[] {0,0,0});
        dia.newEdge(w1, w2, new int[] {-1,0,0});
        dia.newEdge(w1, w2, new int[] {0,-1,0});
        dia.newEdge(w1, w2, new int[] {0,0,-1});
        

        x = new PeriodicGraph(2);
        x1 = x.newNode();
        x2 = x.newNode();
        x.newEdge(x1, x2, new int[] {0,0});
        x.newEdge(x1, x2, new int[] {-1,0});
        x.newEdge(x1, x1, new int[] {0,1});
        x.newEdge(x2, x2, new int[] {0,1});
        y = new PeriodicGraph(2);
        y1 = y.newNode();
        y.newEdge(y1, y1, new int[] {1,0});
        y.newEdge(y1, y1, new int[] {0,1});

        inversion = new Operator("-x,-y,-z");
        rot_xyz = new Operator("z,x,y");
        rot_y = new Operator("-z,y,x");
        stretch_x = new Operator("2x,y");

        autoDia1 = new Morphism(w1, w2, inversion);
        autoDia2 = new Morphism(w1, w1, rot_xyz);
        autoCds1 = new Morphism(v1, v2, rot_y);
        nonInjective = new Morphism(x1, y1, stretch_x);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for void DSMorphism(INode, INode, Matrix)
     */
    public void testMorphismINodeINodeMatrix() {
        try {
            new Morphism(w1, w1, inversion);
            fail("should throw a NoSuchMorphismException");
        } catch (Morphism.NoSuchMorphismException success) {
        }
        
        try {
            new Morphism(w1, w2, rot_xyz);
            fail("should throw a NoSuchMorphismException");
        } catch (Morphism.NoSuchMorphismException success) {
        }
    }

    /*
     * Class under test for void DSMorphism(DSMorphism)
     */
    public void testMorphismMorphism() {
        final Morphism map = new Morphism(autoDia1);
        assertTrue(map.isIsomorphism());
        assertEquals(10, map.size());
        assertEquals(w1, map.get(map.getASource(w1)));
    }

    public void testInverse() {
        final Morphism map = autoDia1.inverse();
        assertTrue(map.isIsomorphism());
        assertEquals(10, map.size());
        for (final Iterator nodes = dia.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertEquals(v, map.get(autoDia1.get(v)));
            assertEquals(v, autoDia1.get(map.get(v)));
        }
        for (final Iterator edges = dia.edges(); edges.hasNext();) {
            final IEdge e = ((IEdge) edges.next()).oriented();
            final IEdge r = e.reverse();
            assertEquals(e, map.get(autoDia1.get(e)));
            assertEquals(e, autoDia1.get(map.get(e)));
            assertEquals(r, map.get(autoDia1.get(r)));
            assertEquals(r, autoDia1.get(map.get(r)));
        }
        
        try {
            nonInjective.inverse();
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        
        assertEquals(autoDia1, map);
        assertEquals(autoDia1.hashCode(), map.hashCode());
        assertFalse(autoDia2.equals(autoDia2.inverse()));
    }

    public void testTimes() {
        final Morphism map = autoDia1.times(autoDia2);
        assertTrue(map.isIsomorphism());
        assertEquals(10, map.size());

        for (final Iterator nodes = dia.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertEquals(autoDia2.get(autoDia1.get(v)), map.get(v));
            assertEquals(autoDia1.getASource(autoDia2.getASource(v)), map.getASource(v));
        }
        for (final Iterator edges = dia.edges(); edges.hasNext();) {
            final IEdge e = ((IEdge) edges.next()).oriented();
            final IEdge r = e.reverse();
            assertEquals(autoDia2.get(autoDia1.get(e)), map.get(e));
            assertEquals(autoDia1.getASource(autoDia2.getASource(e)), map.getASource(e));
            assertEquals(autoDia2.get(autoDia1.get(r)), map.get(r));
            assertEquals(autoDia1.getASource(autoDia2.getASource(r)), map.getASource(r));
        }
    }

    public void testIsIsomorphism() {
        assertTrue(autoCds1.isIsomorphism());
        assertTrue(autoDia1.isIsomorphism());
        assertTrue(autoDia2.isIsomorphism());
        assertFalse(nonInjective.isIsomorphism());
    }

    public void testGetASource() {
        for (final Iterator nodes = dia.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertEquals(v, autoDia1.get(autoDia1.getASource(v)));
            assertEquals(v, autoDia2.get(autoDia2.getASource(v)));
        }
        for (final Iterator edges = dia.edges(); edges.hasNext();) {
            final IEdge e = ((IEdge) edges.next()).oriented();
            final IEdge r = e.reverse();
            assertEquals(e, autoDia1.get(autoDia1.getASource(e)));
            assertEquals(e, autoDia2.get(autoDia2.getASource(e)));
            assertEquals(r, autoDia1.get(autoDia1.getASource(r)));
            assertEquals(r, autoDia2.get(autoDia2.getASource(r)));
        }

        for (final Iterator nodes = cds.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertEquals(v, autoCds1.get(autoCds1.getASource(v)));
        }
        for (final Iterator edges = cds.edges(); edges.hasNext();) {
            final IEdge e = ((IEdge) edges.next()).oriented();
            final IEdge r = e.reverse();
            assertEquals(e, autoCds1.get(autoCds1.getASource(e)));
            assertEquals(r, autoCds1.get(autoCds1.getASource(r)));
        }

        for (final Iterator nodes = y.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertEquals(v, nonInjective.get(nonInjective.getASource(v)));
        }
        for (final Iterator edges = y.edges(); edges.hasNext();) {
            final IEdge e = ((IEdge) edges.next()).oriented();
            final IEdge r = e.reverse();
            assertEquals(e, nonInjective.get(nonInjective.getASource(e)));
            assertEquals(r, nonInjective.get(nonInjective.getASource(r)));
        }
    }

    public void testGetLinearOperator() {
        assertEquals(inversion, autoDia1.getLinearOperator());
        assertEquals(rot_xyz, autoDia2.getLinearOperator());
        assertEquals(rot_y, autoCds1.getLinearOperator());
    }
    
    public void testGetTranslation() {
        final Vector t1 = (Vector) new Vector(1, 1, 1).dividedBy(4);
        final Vector t2 = new Vector(0, 0, 0);
        final Vector t3 = (Vector) new Vector(0, 1, 0).dividedBy(2);
        assertEquals(t1, autoDia1.getTranslation());
        assertEquals(t2, autoDia2.getTranslation());
        assertEquals(t3, autoCds1.getTranslation());
    }
    
    public void testGetAffineOperator() {
        assertEquals(new Operator("1/4-x,1/4-y,1/4-z"), autoDia1.getAffineOperator());
        assertEquals(rot_xyz, autoDia2.getAffineOperator());
        assertEquals(new Operator("-z,y+1/2,x"), autoCds1.getAffineOperator());
    }
    
    public void testSize() {
        // size is #nodes + 2 * #edges
        assertEquals(10, autoDia1.size());
        assertEquals(10, autoDia2.size());
        assertEquals(10, autoCds1.size());
        assertEquals(10, nonInjective.size());
    }

    public void testContainsKey() {
        for (final Iterator nodes = dia.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertTrue(autoDia1.containsKey(v));
            assertTrue(autoDia2.containsKey(v));
        }
        for (final Iterator edges = dia.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final IEdge o = e.oriented();
            final IEdge r = o.reverse();
            assertTrue(autoDia1.containsKey(o));
            assertTrue(autoDia2.containsKey(o));
            assertTrue(autoDia1.containsKey(r));
            assertTrue(autoDia2.containsKey(r));
            assertFalse(autoDia1.containsKey(e));
            assertFalse(autoDia2.containsKey(e));
        }
    }

    public void testContainsValue() {
        for (final Iterator nodes = dia.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            assertTrue(autoDia1.containsValue(v));
            assertTrue(autoDia2.containsValue(v));
        }
        for (final Iterator edges = dia.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final IEdge o = e.oriented();
            final IEdge r = o.reverse();
            assertTrue(autoDia1.containsValue(o));
            assertTrue(autoDia2.containsValue(o));
            assertTrue(autoDia1.containsValue(r));
            assertTrue(autoDia2.containsValue(r));
            assertFalse(autoDia1.containsValue(e));
            assertFalse(autoDia2.containsValue(e));
        }
    }

    public void testGet() {
        assertEquals(w2, autoDia1.get(w1));
        assertEquals(w1, autoDia1.get(w2));

        assertEquals(w1, autoDia2.get(w1));
        assertEquals(w2, autoDia2.get(w2));

        assertEquals(v2, autoCds1.get(v1));
        assertEquals(v1, autoCds1.get(v2));
        final IEdge e1o = e1.oriented();
        final IEdge e2o = e2.oriented();
        final IEdge e3o = e3.oriented();
        final IEdge e4o = e4.oriented();
        assertEquals(e3o, autoCds1.get(e1o));
        assertEquals(e4o, autoCds1.get(e2o));
        assertEquals(e4o.reverse(), autoCds1.get(e2o.reverse()));
        assertEquals(e1o, autoCds1.get(e3o));
        assertEquals(e2o.reverse(), autoCds1.get(e4o));
        assertEquals(e2o, autoCds1.get(e4o.reverse()));
    }
}

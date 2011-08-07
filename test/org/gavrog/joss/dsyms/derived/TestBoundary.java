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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.derived.Boundary.Face;

/**
 * Unit test for class Boundary.
 * @author Olaf Delgado
 * @version $Id: TestBoundary.java,v 1.2 2007/04/18 20:19:07 odf Exp $
 */
public class TestBoundary extends TestCase {
    private DelaneySymbol ds;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        ds = new DSymbol("6:1 3 4 6,2 4 5 6,1 5 6 4:8 4,3 3");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        ds = null;
        super.tearDown();
    }

    public void testBoundary() {
        final Boundary bound = new Boundary(ds);
        final Object D1 = new Integer(1);
        final Object D2 = new Integer(2);
        final Object D3 = new Integer(3);
        final Object D5 = new Integer(5);
        final Object D6 = new Integer(6);
        
        Assert.assertTrue(bound.isOnBoundary(1, D1));
        Assert.assertEquals(new Face(0, D1), bound.neighbor(1, D1, 0));
        Assert.assertEquals(1, bound.glueCountAtRidge(0, D1, 1));
        bound.glue(1, D1);
        Assert.assertFalse(bound.isOnBoundary(1, D1));
        Assert.assertEquals(new Face(0, D2), bound.neighbor(0, D1, 1));
        Assert.assertEquals(new Face(2, D2), bound.neighbor(2, D1, 1));
        Assert.assertEquals(2, bound.glueCountAtRidge(0, D1, 1));
        try {
            bound.neighbor(1, D1, 0);
            Assert.fail("should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try {
            bound.glueCountAtRidge(1, D1, 0);
            Assert.fail("should have thrown an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
        
        // close a cycle
        bound.glue(0, D2);
        bound.glue(2, D2);
        bound.glue(2, D3);
        bound.glue(0, D5);
        Assert.assertEquals(new Face(1, D5), bound.neighbor(2, D1, 1));
        
        // test glueAndEnqueue()
        final LinkedList Q = new LinkedList();
        bound.glueAndEnqueue(1, D3, Q);
        Assert.assertFalse(bound.isOnBoundary(1, D3));
        final Set ridges = new HashSet();
        for (Iterator iter = Q.iterator(); iter.hasNext();) {
            ridges.add(iter.next());
        }
        Assert.assertEquals(2, ridges.size());
        Assert.assertTrue(ridges.contains(new Face(0, D1, 1)));
        Assert.assertTrue(ridges.contains(new Face(1, D6, 2)));
    }
}

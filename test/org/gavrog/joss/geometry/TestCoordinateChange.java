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

package org.gavrog.joss.geometry;

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;

/**
 * Unit tests for {@link org.gavrog.joss.geometry.CoordinateChange}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestCoordinateChange.java,v 1.5 2007/03/02 21:21:19 odf Exp $
 */
public class TestCoordinateChange extends TestCase {
    final Matrix M = new Matrix(new int[][] { { 0, 2, 0 }, { 0, 0, 1 }, { 1, 0, 0 } });
    final Point p = new Point(new int[] { -1, 1, 1 });
    final CoordinateChange T = new CoordinateChange(M, p);

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testHashCode() {
        final Matrix A = new Matrix(new int[][] { { 0, 2, 0 }, { 0, 0, 1 }, { 1, 0, 0 } });
        final Point q = new Point(new int[] { -1, 1, 1 });
        assertEquals(T.hashCode(), new CoordinateChange(A, q).hashCode());
    }

    public void testIsExact() {
        assertTrue(T.isExact());
        final Point q = new Point(new double[] { -1, 1, 1 });
        assertFalse(new CoordinateChange(M, q).isExact());
    }

    public void testZero() {
        try {
            T.zero();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testOne() {
        assertEquals(T, T.times(T.one()));
    }

    public void testNegative() {
        try {
            T.negative();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testInverse() {
        assertEquals(T.one(), T.times(T.inverse()));
    }

    public void testPlus() {
        try {
            T.plus(T);
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testTimes() {
        final Point o = Point.origin(3);
        assertEquals(o, p.times(T));
        final Vector v = new Vector(new int[] { 0, 2, 0 });
        final Vector w = new Vector(new int[] { 1, 0, 0 });
        assertEquals(w, v.times(T));
        final Operator op1 = new Operator("-x, y, z");
        final Operator op2 = new Operator("x, y, 2-z");
        assertEquals(op2, op1.times(T));
        final Matrix A = new Matrix(new int[][] { { 1, 0, 0 }, { 0, 2, 0 }, { 0, 0, 1 } });
        final Point q = new Point(new int[] { 1, 0, 0 });
        final Matrix B = new Matrix(new int[][] { { 0, 2, 0 }, { 0, 0, 2 }, { 1, 0, 0 } });
        final Point r = new Point(new int[] { -1, 3, 1 });
        assertEquals(new CoordinateChange(B, r), T.times(new CoordinateChange(A, q)));
    }

    public void testCompareTo() {
        final int[][] A = new int[][] { { 0, 2, 0 }, { 0, 0, 1 }, { 1, 0, 0 } };
        final Point q = new Point(new int[] { -1, 1, 1 });
        assertEquals(0, T.compareTo(new CoordinateChange(new Matrix(A), q)));
        final Point r = new Point(new int[] { -1, 1, 2 });
        assertTrue(T.compareTo(new CoordinateChange(new Matrix(A), r)) < 0);
    }

    public void testFloor() {
        try {
            T.floor();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testToString() {
        final String s = "CoordinateChange(Matrix([[0,2,0],[0,0,1],[1,0,0]]),Point(-1,1,1))";
        assertEquals(s, T.toString());
    }

    public void testGetDimension() {
        assertEquals(3, T.getDimension());
    }

    public void testGetBasis() {
        final int A[][] = new int[][] { { 0, 2, 0 }, { 0, 0, 1 }, { 1, 0, 0 } };
        assertEquals(new Matrix(A), T.getBasis());
    }

    public void testGetOrigin() {
        final Point p = new Point(new int[] { -1, 1, 1 });
        assertEquals(p, T.getOrigin());
    }

    public void testConstructorFromOperator() {
        final CoordinateChange S = new CoordinateChange(new Operator("z-1,2x+1,y+1"));
        assertEquals(T.inverse(), S);
    }
    
    public void testConstructorWithoutOriginChange() {
    	final Point z = new Point(new int[] { 0, 0, 0 });
    	assertEquals(new CoordinateChange(M), new CoordinateChange(M, z));
    }
    
    public void testPartialBasis() {
    	final int A[][] = new int[][] { { 1, 1, 1 }, { -1, 1, 1 } };
    	final CoordinateChange C = new CoordinateChange(new Matrix(A));
    	final Point p = new Point(new int[] { 1, 1, 1 });
    	final Point pC = new Point(new int[] { 1, 0, 0 });
    	final Point q = new Point(new int[] { -1, 1, 1 });
    	final Point qC = new Point(new int[] { 0, 1, 0 });
    	assertEquals(pC, p.times(C));
    	assertEquals(qC, q.times(C));
    	assertEquals(C.one(), C.times(C.inverse()));
    	assertEquals(C.one(), C.inverse().times(C));
    }
}

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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * Unit tests for the Vector class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestVector.java,v 1.15 2006/03/19 05:16:27 odf Exp $
 */
public class TestVector extends TestCase {
    final Vector v = new Vector(1, 2, 3);
    final Vector w = new Vector(new double[] {1, 2, 4});
    final Matrix M = new Matrix(new int[][] {
            { 0, 1, 0, 0 },
            { 0, 0, 1, 0 },
            { 1, 0, 0, 0 },
            { 1, 3, 2, 2 },
            });

    public void testHashCode() {
        final Vector a = new Vector(1, 2, 3);
        final Vector b = new Vector(1, 2, 4);
        assertEquals(v.hashCode(), a.hashCode());
        assertFalse(v.hashCode() == b.hashCode());
    }

    public void testIsExact() {
        assertTrue(v.isExact());
        assertFalse(w.isExact());
    }

    public void testIsIntegral() {
        final Vector u = new Vector(2, 4, 8);
        assertTrue(v.isIntegral());
        assertFalse(w.isIntegral());
        assertFalse(((Vector) v.dividedBy(2)).isIntegral());
        assertTrue(((Vector) u.dividedBy(2)).isIntegral());
    }

    public void testZero() {
        final Vector z = new Vector(0, 0, 0);
        assertEquals(z, v.zero());
    }

    public void testUnit() {
        assertEquals(new Vector(0, 1, 0), Vector.unit(3, 1));
        assertEquals(new Vector(1, 0, 0), Vector.unit(3, 0));
    }

    public void testOne() {
        try {
            v.one();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testNegative() {
        final Vector n = new Vector(-1, -2, -3);
        assertEquals(n, v.negative());
    }

    public void testInverse() {
        try {
            v.inverse();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testPlus() {
        final Vector s = new Vector(new double[] {2, 4, 7});
        final Point p = new Point(new double[] {1, 2, 4});
        final Point q = new Point(new double[] {2, 4, 7});
        assertEquals(s, v.plus(w));
        assertEquals(q, v.plus(p));
    }

    public void testTimes() {
        final Vector a = new Vector(new double[] {1.5, 0.5, 1});
        assertEquals(a, v.times(new Operator(M)));
    }

    public void testCompareTo() {
        final Vector a = new Vector(1, 2, 3);
        assertTrue(v.compareTo(w) < 0);
        assertTrue(w.compareTo(v) > 0);
        assertTrue(v.compareTo(a) == 0);
    }

    public void testFloor() {
        try {
            v.floor();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testToString() {
        final String s = "Vector(1,2,3)";
        assertEquals(s, v.toString());
    }

    public void testVectorMatrix() {
        final Vector a = new Vector(new Matrix(new int[][] {{1, 2, 3}}));
        assertEquals(v, a);
    }

    public void testVectorIArithmeticArray() {
        final Vector a = new Vector(new IArithmetic[] { new Whole(1), new Whole(2),
                new Whole(3) });
        assertEquals(v, a);
    }

    public void testVectorIntArray() {
        final Vector a = new Vector(new int[] { 1, 2, 3 });
        assertEquals(v, a);
    }

    public void testVectorDoubleArray() {
        final Vector a = new Vector(new double[] { 1, 2, 4 });
        assertEquals(w, a);
    }

    public void testVectorVector() {
        final Vector a = new Vector(v);
        assertEquals(v, a);
    }

    public void testGetDimension() {
        assertEquals(3, v.getDimension());
    }

    public void testGet() {
        assertEquals(new Whole(2), v.get(1));
    }

    public void testGetCoordinates() {
        final Matrix A = new Matrix(new int[][] {{1, 2, 3}});
        assertEquals(A, v.getCoordinates());
    }
    
    public void testAsDoubleArray() {
    	final double[] c = v.asDoubleArray();
    	assertEquals(3, c.length);
    	assertEquals(1.0, c[0]);
    	assertEquals(2.0, c[1]);
    	assertEquals(3.0, c[2]);
    }
    
    public void testStaticZero() {
        final Vector z = new Vector(Matrix.zero(1, 5));
        assertEquals(z, Vector.zero(5));
    }
    
    public void testDot() {
        assertEquals(new FloatingPoint(17), Vector.dot(v, w));
        final Matrix form = new Matrix(new int[][] {{-1,0,0},{0,-1,0},{0,0,1}});
        assertEquals(new FloatingPoint(7), Vector.dot(v, w, form));
    }

    public void testRowVectors() {
        final Matrix A = new Matrix(new int[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 },
                { 9, 10, 11, 12 }, { 13, 14, 15, 16 } });
        final Vector r[] = new Vector[] { new Vector(new int[] { 1, 2, 3, 4 }),
                new Vector(new int[] { 5, 6, 7, 8 }),
                new Vector(new int[] { 9, 10, 11, 12 }),
                new Vector(new int[] { 13, 14, 15, 16 }) };
        final Vector rows[] = Vector.rowVectors(A);
        assertEquals(r[0], rows[0]);
        assertEquals(r[1], rows[1]);
        assertEquals(r[2], rows[2]);
        assertEquals(r[3], rows[3]);
    }
    
    public void testIsCollinearTo() {
        final Vector u = new Vector(new int[] { -2, -4, -6 });
        assertTrue(u.isCollinearTo(v));
        assertFalse(u.isCollinearTo(w));
        assertFalse(v.isCollinearTo(w));
    }
    
    public void testCrossProduct3D() {
        assertEquals(new Vector(new int[] { 2, -1, 0 }), Vector.crossProduct3D(v, w));
        assertEquals(new Vector(new int[] { -2, 1, 0 }), Vector.crossProduct3D(w, v));
    }
    
    public void testVolume3D() {
        final Vector u = new Vector(new int[] { 1, 1, 1 });
        assertEquals(new Whole(1), Vector.volume3D(u, v, w));
    }
    
    public void testFromMatrix() {
        final Matrix A = new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        final Vector rows[] = Vector.fromMatrix(A);
        assertEquals(new Vector(new int[] { 1, 2, 3 }), rows[0]);
        assertEquals(new Vector(new int[] { 4, 5, 6 }), rows[1]);
        assertEquals(new Vector(new int[] { 7, 8, 9 }), rows[2]);
    }
    
    public void testToMatrix() {
        final Matrix A = new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } });
        final Vector rows[] = new Vector[] {
                new Vector(1, 2, 3),
                new Vector(4, 5, 6),
                new Vector(7, 8, 9),
        };
        assertEquals(A, Vector.toMatrix(rows));
        final List<Vector> list = new ArrayList<Vector>();
        list.add(new Vector(1, 2, 3));
        list.add(new Vector(4, 5,6));
        list.add(new Vector(7, 8, 9));
        assertEquals(A, Vector.toMatrix(list));
    }
}

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

import org.gavrog.box.simple.DataFormatException;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * Unit tests for the Operator class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestOperator.java,v 1.13 2005/11/13 06:22:56 odf Exp $
 */
public class TestOperator extends TestCase {
    final int M[][] = new int[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}};
    final Operator op1 = new Operator(M);

    public void testHashCode() {
        final int A[][] = new int[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}};
        final int B[][] = new int[][] {{0, 1, 0}, { 1, 0, 0}, {1, 0, 1}};
        final Operator opA = new Operator(A);
        final Operator opB = new Operator(B);
        assertEquals(op1.hashCode(), opA.hashCode());
        assertFalse(op1.hashCode() == opB.hashCode());
    }

    public void testIsExact() {
        final double A[][] = new double[][] {{0, 1, 0}, {1, 0, 0}, {1, 0, 1}};
        final Operator opA = new Operator(A);
        assertTrue(op1.isExact());
        assertFalse(opA.isExact());
    }

    public void testZero() {
        try {
            op1.zero();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testOne() {
        assertEquals(op1, op1.times(op1.one()));
    }

    public void testNegative() {
        try {
            op1.negative();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testInverse() {
        assertEquals(op1.one(), op1.times(op1.inverse()));
    }

    public void testIdentity() {
        assertEquals(new Operator("x,y,z"), Operator.identity(3));
    }
    
    public void testPlus() {
        try {
            op1.plus(op1);
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testTimes() {
        final int A[][] = new int[][] {{-1, 0, 0}, {0, -1, 0}, {1, 1, 1}};
        final Operator opA = new Operator(A);
        assertEquals(opA, op1.times(op1));
        assertEquals(new Operator("3-x,-y"), opA.times(new Vector(2, -1)));
    }

    public void testCompareTo() {
        final int A[][] = new int[][] {{0, 1, 0}, {-1, 0, 0}, {2, 0, 1}};
        final int B[][] = new int[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}};
        final Operator opA = new Operator(A);
        final Operator opB = new Operator(B);
        assertTrue(op1.compareTo(opA) < 0);
        assertTrue(opA.compareTo(op1) > 0);
        assertTrue(opB.compareTo(op1) == 0);
    }

    public void testFloor() {
        try {
            op1.floor();
            fail("should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    public void testModZ() {
        final double A[][] = new double[][] {{0, 1.1, 0}, {-1, 0, 0}, {1.25, 0, 1}};
        final double B[][] = new double[][] {{0, 1.1, 0}, {-1, 0, 0}, { .25, 0, 1}};
        final Operator opA = new Operator(A);
        final Operator opB = new Operator(B);
        //CAVEAT: this test only works if the numbers used can be represented precisely
        assertEquals(opB, opA.modZ());
    }
    
    public void testFloorZ() {
        final double[][] A = new double[][] {{0, 1.1, 0 }, {-1, 0, 0}, {-1.25, 0, 1}};
        final Operator opA = new Operator(A);
        final Vector v = new Vector(-2, 0);
        assertEquals(v, opA.floorZ());
    }

    public void testToString() {
        final String s = "Operator([[0,1,0],[-1,0,0],[1,0,1]])";
        assertEquals(s, op1.toString());
    }

    public void testOperatorMatrix() {
        final Matrix A = new Matrix(new int[][] {{0, 2, 0}, {-2, 0, 0}, {2, 0, 2}});
        final Operator opA = new Operator(A);
        assertEquals(op1, opA);
    }

    public void testOperatorIArithmeticArrayArray() {
        final IArithmetic A[][] = new IArithmetic[][] {
                {Whole.ZERO, Whole.ONE, Whole.ZERO},
                {new Whole(-1), Whole.ZERO, Whole.ZERO},
                {Whole.ONE, Whole.ZERO, Whole.ONE}
        };
        final Operator opA = new Operator(A);
        assertEquals(op1, opA);
    }

    public void testOperatorIntArrayArray() {
        final int A[][] = new int[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}};
        final Operator opA = new Operator(A);
        assertEquals(op1, opA);
    }

    public void testOperatorDoubleArrayArray() {
        final double A[][] = new double[][] {{0, 1, 0}, {-1, 0, 0}, {1, 0, 1}};
        final Operator opA = new Operator(A);
        assertEquals(op1, opA);
    }

    public void testOperatorString() {
        final Operator opA = new Operator("1-y,x");
        assertEquals(op1, opA);
    }

    public void testOperatorVector() {
        final Vector s = new Vector(1, -2, 3);
        assertEquals(new Operator("x+1,y-2,z+3"), new Operator(s));
    }
    
    public void testGetDimension() {
        assertEquals(2, op1.getDimension());
    }

    public void testGet() {
        assertEquals(new Whole(-1), op1.get(1, 0));
    }

    public void testGetCoordinates() {
        assertEquals(new Matrix(M), op1.getCoordinates());
    }

    public void testLinearPart() {
        final int[][] A = new int[][] {{0, 1, 0}, {-1, 0, 0}, {0, 0, 1}};
        final Operator opA = new Operator(A);
        assertEquals(opA, op1.linearPart());
    }

    public void testLinearPartAsMatrix() {
        final Matrix A = new Matrix(new int[][] {{0, 1}, {-1, 0}});
        assertEquals(A, op1.linearPartAsMatrix());
    }

    public void testTranslationalPart() {
        final Vector v = new Vector(new int[] {1, 0});
        assertEquals(v, op1.translationalPart());
    }

    public void testApplyTo() {
        final Point x = new Point(new int[] {1, 0});
        final Point xy = new Point(new int[] {1, 1});
        final Point y = new Point(new int[] {0, 1});
        final Point z = new Point(new int[] {0, 0});
        assertEquals(xy, op1.applyTo(x));
        assertEquals(y, op1.applyTo(xy));
        assertEquals(z, op1.applyTo(y));
        assertEquals(x, op1.applyTo(z));
    }

    public void testParseOperator() {
        String s;
        Matrix M;
        
        s = "x-4y+7*z-10, +5/3y-8z+11-2x, +3*x+ 9z-6y - 12";
        M = new Matrix(new int[][] {
                {  1, -2,   3, 0},
                { -4,  5,  -6, 0},
                {  7, -8,   9, 0},
                {-10, 11, -12, 1}}).mutableClone();
        M.set(1, 1, new Fraction(5, 3));
        assertEquals(M, Operator.parse(s));
        assertFalse(Operator.parse(s).isMutable());
        
        assertEquals(Matrix.one(4), Operator.parse("x,y,z"));
        
        try {
            Operator.parse("1,2,3,4");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            Operator.parse("a,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            Operator.parse("1,2/,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
        
        try {
            Operator.parse("x+3x,2,3");
            fail("should throw an DataFormatException");
        } catch (DataFormatException success) {
        }
    }
    
    public void testLinearAxis() {
        final Operator op1 = new Operator("-y,x");
        final Operator op2 = new Operator("y,x");
        final Operator op3 = new Operator("z,x,y");
        final Operator op4 = new Operator("y,z,x");
        final Operator op5 = new Operator("x,y");
        final Operator op6 = new Operator("x,y,z");
        final Operator op7 = new Operator("y,-x");
        final Operator op8 = new Operator("-y,-z,-x");
        final Operator op9 = new Operator("-y,x-y,z");
        
        assertNull(op1.linearAxis());
        assertTrue(op2.linearAxis().isCollinearTo(new Vector(new int[] {1, 1})));
        assertTrue(op3.linearAxis().isCollinearTo(new Vector(new int[] {1, 1, 1})));
        assertTrue(op4.linearAxis().isCollinearTo(new Vector(new int[] {1, 1, 1})));
        assertNull(op5.linearAxis());
        assertNull(op6.linearAxis());
        assertNull(op7.linearAxis());
        assertTrue(op8.linearAxis().isCollinearTo(new Vector(new int[] {1, 1, 1})));
        assertTrue(op9.linearAxis().isCollinearTo(new Vector(new int[] {0, 0, 1})));
    }
    
    public void testOrthogonalProjection() {
        final Matrix I = Matrix.one(3);
        assertEquals(new Operator("x, y, 0"), Operator.orthogonalProjection(new Matrix(
                new int[][] { { 1, 0, 0 }, { 0, 1, 0 } }), I));
        final Operator op1 = new Operator(
                "1/3x+1/3y+1/3z, 1/3x+1/3y+1/3z, 1/3x+1/3y+1/3z");
        final Operator op2 = Operator.orthogonalProjection(new Matrix(new int[][] { { 1,
                1, 1 } }), I);
        final Matrix D = (Matrix) op1.getCoordinates().minus(op2.getCoordinates());
        assertTrue(D.norm().isLessThan(new FloatingPoint(1e-10)));
    }
}

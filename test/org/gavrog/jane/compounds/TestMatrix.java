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

package org.gavrog.jane.compounds;

import junit.framework.TestCase;

import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

/**
 * @author Olaf Delgado
 * @version $Id: TestMatrix.java,v 1.6 2006/03/04 20:45:36 odf Exp $
 */
public class TestMatrix extends TestCase {
    private Matrix A;
    private Matrix B;
    private Matrix C;
    private Matrix R;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new Matrix(new int[][] { 
                {  1,  2,  3,  4 },
                {  5,  6,  7,  8 },
                {  9, 10, 11, 12 },
                { 13, 14, 15, 16 } }).mutableClone();
        B = new Matrix(new int[][] {
                { 1, 2, 3 },
                { 4, 5, 6 },
                { 7, 8, 9 } }).mutableClone();
        C = new Matrix(new int[][] {{-1}, {-2}, {-3}, {-4}});
        R = new Matrix(new int[][] {{-1, -2, -3, -4}});
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAsDoubleArray() {
        final double a[][] = new double[][] { 
                {  1,  2,  3,  4 },
                {  5,  6,  7,  8 },
                {  9, 10, 11, 12 },
                { 13, 14, 15, 16 } };
        final double b[][] = A.asDoubleArray();
        assertEquals(a.length, b.length);
        assertEquals(a[0].length, b[0].length);
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < a[i].length; ++j) {
                assertTrue(a[i][j] == b[i][j]);
            }
        }
    }
    
    public void testInverse() {
        final Matrix A = new Matrix(3, 3);
        A.set(0, 0, new FloatingPoint(0.8164965809277261));
        A.set(0, 1, new FloatingPoint(0.0));
        A.set(0, 2, new FloatingPoint(0.0));
        A.set(1, 0, new FloatingPoint(-0.47140452079103173));
        A.set(1, 1, new FloatingPoint(0.9428090415820632));
        A.set(1, 2, new FloatingPoint(0.0));
        A.set(2, 0, new FloatingPoint(-0.3333333333333335));
        A.set(2, 1, new FloatingPoint(-0.3333333333333332));
        A.set(2, 2, new FloatingPoint(1.0));

        final Real eps = new FloatingPoint(1e-12);
        final Matrix D = (Matrix) Matrix.one(3).minus(A.times(A.inverse()));
        assertTrue(D.norm().isLessOrEqual(eps));
    }

    public void testTriangulate() {
        testTriangulate(new Matrix(new int[][] {{13, 21}, {34, 55}}));
        testTriangulate(new Matrix(new int[][] {{-13, 21}, {34, 55}}));
        testTriangulate(new Matrix(new int[][] {{34, 55}, {13, 21}}));
        testTriangulate(new Matrix(new int[][] {{34, 55}, {-13, 21}}));
    }
    
    private void testTriangulate(final Matrix A) {
        final IArithmetic det = A.get(0, 0).times(A.get(1, 1)).minus(
                A.get(1, 0).times(A.get(0, 1)));
        Matrix B, M;
        int sign;

        M = A.mutableClone();
        B = ((Matrix) A.one()).mutableClone();
        sign = Matrix.triangulate(M, B, false, true);
        assertEquals(M, B.times(A));
        if (A.numberOfRows() == 2 && A.numberOfColumns() == 2) {
            assertEquals(det, M.get(0, 0).times(M.get(1, 1)).times(new Whole(sign)));
        }

        M = A.mutableClone();
        B = ((Matrix) A.one()).mutableClone();
        sign = Matrix.triangulate(M, B, true, true);
        for (int i = 0; i < M.numberOfColumns(); ++i) {
            for (int j = 0; j < M.numberOfRows(); ++j) {
                assertTrue(M.get(i, j) instanceof Whole);
            }
        }
        for (int i = 0; i < B.numberOfColumns(); ++i) {
            for (int j = 0; j < B.numberOfRows(); ++j) {
                assertTrue(B.get(i, j) instanceof Whole);
            }
        }
        assertEquals(M, B.times(A));
        if (A.numberOfRows() == 2 && A.numberOfColumns() == 2) {
            assertEquals(det, M.get(0, 0).times(M.get(1, 1)).times(new Whole(sign)));
        }
    }

    public void testRank() {
        assertEquals(2, new Matrix(new int[][] {{1,2,3}, {4,5,6}, {7,8,9}}).rank());
        assertEquals(3, new Matrix(new int[][] {{1,2,3}, {4,5,6}, {8,8,9}}).rank());
    }

    public void testDeterminant() {
        assertEquals(Whole.ZERO, new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 },
                { 7, 8, 9 } }).determinant());
        assertEquals(new Whole(-3), new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 },
                { 8, 8, 9 } }).determinant());
    }
    
    public void testSolve() {
        final Matrix A1 = new Matrix(new int[][] {{1,2,3},{4,5,6},{7,8,9}});
        final Matrix A2 = new Matrix(new int[][] {{1,2,3},{4,5,6},{8,8,9}});
        final Matrix b1 = new Matrix(new int[][] {{6},{15},{24}});
        final Matrix b2 = new Matrix(new int[][] {{6},{15},{25}});
        testSolve(A1, b1, true);
        testSolve(A1, b2, false);
        testSolve(A2, b1, true);
        testSolve(A2, b2, true);
    }
    
    private void testSolve(final Matrix A, final Matrix b, final boolean hasSolution) {
        final Matrix A_saved = (Matrix) A.clone();
        final Matrix b_saved = (Matrix) b.clone();
        final Matrix x = Matrix.solve(A, b);
        assertEquals(A, A_saved);
        assertEquals(b, b_saved);
        if (hasSolution) {
            assertNotNull(x);
            assertEquals(b, A.times(x));
        } else {
            assertNull(x);
        }
    }
    
    public void testGetSubMatrix() {
        final Matrix T = new Matrix(new int [][] {{10, 11, 12}, {14, 15, 16}});
        assertEquals(T, A.getSubMatrix(2, 1, 2, 3));
    }
    
    public void testGetMinor() {
        final Matrix T = new Matrix(new int[][] { { 1, 3, 4 }, { 5, 7, 8 },
                { 13, 15, 16 } });
        assertEquals(T, A.getMinor(2, 1));
    }
    
    public void testSetSubMatrix() {
        final Matrix T = new Matrix(new int [][] {
                {  1,  2,  3,  4 },
                {  1,  2,  3,  8 },
                {  4,  5,  6, 12 },
                {  7,  8,  9, 16 } });
        A.setSubMatrix(1, 0, B);
        assertEquals(T, A);
    }
    
    public void testGetColumn() {
        final Matrix T = new Matrix(new int [][] {{3}, {7}, {11}, {15}});
        assertEquals(T, A.getColumn(2));
    }
    
    public void testSetColumn() {
        final Matrix T = new Matrix(new int [][] {
                {  1,  2, -1,  4 },
                {  5,  6, -2,  8 },
                {  9, 10, -3, 12 },
                { 13, 14, -4, 16 } });
        A.setColumn(2, C);
        assertEquals(T, A);
        try {
            A.setRow(2, C);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
    
    public void testGetRow() {
        final Matrix T = new Matrix(new int [][] {{13, 14, 15, 16}});
        assertEquals(T, A.getRow(3));
    }
    
    public void testGetRows() {
        final Matrix R0 = new Matrix(new int [][] {{ 1,  2,  3,  4}});
        final Matrix R1 = new Matrix(new int [][] {{ 5,  6,  7,  8}});
        final Matrix R2 = new Matrix(new int [][] {{ 9, 10, 11, 12}});
        final Matrix R3 = new Matrix(new int [][] {{13, 14, 15, 16}});
        final Matrix rows[] = A.getRows();
        assertEquals(R0, rows[0]);
        assertEquals(R1, rows[1]);
        assertEquals(R2, rows[2]);
        assertEquals(R3, rows[3]);
    }
    
    public void testSetRow() {
        final Matrix T = new Matrix(new int [][] {
                {  1,  2,  3,  4 },
                { -1, -2, -3, -4 },
                {  9, 10, 11, 12 },
                { 13, 14, 15, 16 } });
        A.setRow(1, R);
        assertEquals(T, A);
        assertEquals(T, A);
        try {
            A.setColumn(2, R);
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
}

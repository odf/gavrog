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
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

/**
 * Unit tests for class LinearAlgebra.
 * 
 * @author Olaf Delgado
 * @version $Id: TestLinearAlgebra.java,v 1.4 2005/11/06 05:04:41 odf Exp $
 */
public class TestLinearAlgebra extends TestCase {
    // TODO add tests for matrices with floating point entries.
    private final Matrix I = Matrix.one(3);

    private final Matrix M = new Matrix(new int[][] { { 4, 1, 3 }, { 1, 5, 2 },
            { 3, 2, 6 } });

    private final Matrix A = new Matrix(new int[][] { { 1, 2, 3 }, { 4, 5, 6 },
            { 7, 8, 8 } });

    private final Real eps = new FloatingPoint(1e-12);

    public void testRowOrthonormalized() {
        final Matrix B = LinearAlgebra.rowOrthonormalized(A, M);
        final Matrix D = (Matrix) I.minus(B.times(M).times(B.transposed()));
        assertTrue(D.norm().isLessOrEqual(eps));
        assertFalse(B.determinant().isZero());

        for (int i = 1; i <= 3; ++i) {
            // --- assert equal spans for initial row sets of both matrices
            final Matrix T = new Matrix(2 * i, 3);
            T.setSubMatrix(0, 0, A.getSubMatrix(0, 0, i, 3));
            T.setSubMatrix(i, 0, B.getSubMatrix(0, 0, i, 3));
            assertEquals(i, T.rank());
        }
    }

    public void testOrthonormalRowBasis() {
        final Matrix B = LinearAlgebra.orthonormalRowBasis(M);
        final Matrix D = (Matrix) I.minus(B.times(M).times(B.transposed()));
        assertTrue(D.norm().isLessOrEqual(eps));
        assertFalse(B.determinant().isZero());
    }

    private static boolean isDiagonal(final Matrix M) {
        final int n = M.numberOfRows();
        final int m = M.numberOfColumns();

        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                if (i != j && !M.get(i, j).isZero()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isUnimodular(final Matrix M) {
        return M.determinant().abs().equals(Whole.ONE);
    }

    private static boolean isIntegral(final Matrix M) {
        final int n = M.numberOfRows();
        final int m = M.numberOfColumns();

        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                if (!(M.get(i, j) instanceof Whole)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void testSmithNormalForm() {
        testSmithNormalForm(A, true);
        testSmithNormalForm(A, false);
        final Matrix L = new Matrix(new int[][] { { -2, 0, 0, -2, 0, 0 },
                { 0, -2, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, -2 } });
        testSmithNormalForm(L, true);
        testSmithNormalForm(L, false);
    }

    private void testSmithNormalForm(final Matrix A, final boolean integral) {
        final Matrix snf[] = LinearAlgebra.smithNormalForm(A, integral);
        final Matrix P = snf[0];
        final Matrix D = snf[1];
        final Matrix Q = snf[2];

        assertEquals(D, P.times(A).times(Q));
        assertTrue(isDiagonal(D));
        assertTrue(isUnimodular(P));
        assertTrue(isUnimodular(Q));
        if (integral) {
            assertTrue(isIntegral(P));
            assertTrue(isIntegral(Q));
        }
    }

    public void testNullSpace() {
        testColumnNullSpace(A, true);
        testColumnNullSpace(A, false);
        final Matrix M = new Matrix(new int[][] { { 1, 0, 0, 0, 0, -1 },
                { 0, 1, 0, 0, 0, 0 }, { 0, 0, 1, 0, 0, 0 }, { 0, 0, 0, 1, 0, -1 },
                { 0, 0, 0, 0, 1, 0 } });
        testColumnNullSpace(M, true);
        testColumnNullSpace(M, false);
    }

    private void testColumnNullSpace(final Matrix A, final boolean integral) {
        final int n = A.numberOfRows();
        final int m = A.numberOfColumns();
        final Matrix B = LinearAlgebra.columnNullSpace(A, integral);
        final int k = B.numberOfColumns();
        assertEquals(m, A.rank() + k);
        assertEquals(k, B.rank());
        assertEquals(Matrix.zero(n, k), A.times(B));
    }

    public void testSolutionInColumns() {
        testSolutionInColumns(A, I, true, true);
        testSolutionInColumns(A, I, false, true);

        final Matrix M = new Matrix(new int[][] { { 1, 0 }, { 1, 0 } });
        final Matrix b = new Matrix(new int[][] { { 0 }, { 1 } });
        testSolutionInColumns(M, b, true, true);
        testSolutionInColumns(M, b, false, false);
        testSolutionInColumns(M, (Matrix) b.dividedBy(2), true, false);
        
        final Matrix L = new Matrix(
                new int[][] { { -2, 0, 0 }, { 0, -2, 0 }, { 0, 0, 0 } });
        final Matrix r = new Matrix(new int[][] { { 0 }, { -1 }, { 1 } });
        testSolutionInColumns(L, r, true, true);
        testSolutionInColumns(L, r, false, false);
        testSolutionInColumns(L, (Matrix) r.dividedBy(2), true, false);
    }

    private void testSolutionInColumns(final Matrix A, final Matrix b,
            final boolean modZ, final boolean exists) {
        final Matrix x = LinearAlgebra.solutionInColumns(A, b, modZ);
        if (exists) {
            if (!modZ) {
                assertEquals(b, A.times(x));
            } else {
                assertTrue(isIntegral((Matrix) b.minus(A.times(x))));
            }
        } else {
            assertNull(x);
        }
    }

    public void testSolutionInRows() {
        testSolutionInRows(A, I, true, true);
        testSolutionInRows(A, I, false, true);

        final Matrix M = new Matrix(new int[][] { { 1, 1 }, { 0, 0 } });
        final Matrix b = new Matrix(new int[][] { { 0, 1 } });
        testSolutionInRows(M, b, true, true);
        testSolutionInRows(M, b, false, false);
        testSolutionInRows(M, (Matrix) b.dividedBy(2), true, false);
        
        final Matrix L = new Matrix(new int[][] { { -2, 0, 0, -2, 0, 0 },
                { 0, -2, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, -2 } });
        final Matrix r = new Matrix(new int[][] { { 0, -1, 1, -1, 1, 0 } });
        testSolutionInRows(L, r, true, true);
        //testSolutionInRows(L, r, false, false);
        testSolutionInRows(L, (Matrix) r.dividedBy(2), true, false);
    }

    private void testSolutionInRows(final Matrix A, final Matrix b,
            final boolean modZ, final boolean exists) {
        final Matrix x = LinearAlgebra.solutionInRows(A, b, modZ);
        if (exists) {
            if (!modZ) {
                assertEquals(b, x.times(A));
            } else {
                assertTrue(isIntegral((Matrix) b.minus(x.times(A))));
            }
        } else {
            assertNull(x);
        }
    }
}

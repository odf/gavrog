/*
   Copyright 2020 Olaf Delgado-Friedrichs

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

import java.lang.ArithmeticException;
import java.util.Arrays;

import org.gavrog.box.simple.TaskController;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Whole;


public class ModularSolver {

    public static long modularInverse(final long a, final long m) {
        long t = 0;
        long t1 = 1;
        long r = m;
        long r1 = a;
        long tmp;

        while (r1 != 0) {
            final long q = r / r1;

            tmp = t1;
            t1 = t - q * t1;
            t = tmp;

            tmp = r1;
            r1 = r - q * r1;
            r = tmp;
        }

        if (r != 1)
            throw new ArithmeticException(a + " has no inverse modulo " + m);

        if (t < 0)
            return t + m;
        else
            return t;
    }


    public static long[][]
        modularRowEchelonForm(final long[][] M, final long m)
    {
        // --- used for answering external cancel request
        final TaskController controller = TaskController.getInstance();

        final int nrows = M.length;
        final int ncols = M[0].length;

        final long A[][] = new long[nrows][ncols];
        for (int i = 0; i < nrows; ++i) {
            for (int j = 0; j < ncols; ++j) {
                final long a = M[i][j] % m;
                if (a < 0)
                    A[i][j] = a + m;
                else
                    A[i][j] = a;
            }
        }

        int row = 0;

        for (int col = 0; col < ncols; ++col) {
            // --- throw if the task controller received a cancel request
            controller.bailOutIfCancelled();

            int r = row;
            while (r < nrows && A[r][col] == 0)
                ++r;

            if (r >= nrows)
                continue;

            if (r != row) {
                final long[] tmp = A[r];
                A[r] = A[row];
                A[row] = tmp;
            }

            final long f = modularInverse(A[row][col], m);
            for (int j = col; j < ncols; ++j)
                A[row][j] = (A[row][j] * f) % m;

            for (int i = 0; i < nrows; ++i) {
                if (i == row || A[i][col] == 0)
                    continue;

                final long g = A[i][col];
                for (int j = col; j < ncols; ++j) {
                    if (A[row][j] != 0)
                        A[i][j] = (m - (A[row][j] * g) % m + A[i][j]) % m;
                }
            }

            ++row;
        }

        return A;
    }


    public static long[][]
        modularMatrixInverse(final long[][] M, final long m)
    {
        final int n = M.length;

        if (M[0].length != n)
            throw new ArithmeticException("matrix must be quadratic");

        final long A[][] = new long[n][n+n];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                A[i][j] = M[i][j];
            }
            A[i][n + i] = 1;
        }

        final long E[][] = modularRowEchelonForm(A, m);

        for (int i = 0; i < n; ++i)
            if (E[i][i] != 1)
                throw new ArithmeticException("matrix is not invertible");

        final long R[][] = new long[n][n];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                R[i][j] = E[i][j + n];
            }
        }

        return R;
    }


    public static void
        modularMatrixProduct(final long[][] A, final long[][] B,
                             final long[][] R, final long m)
    {
        final TaskController controller = TaskController.getInstance();

        final int nrowsA = A.length;
        final int ncolsA = A[0].length;
        final int nrowsB = B.length;
        final int ncolsB = B[0].length;
        final int nrowsR = R.length;
        final int ncolsR = R[0].length;

        if (nrowsR != nrowsA || ncolsR != ncolsB || ncolsA != nrowsB)
            throw new ArithmeticException("matrix shapes do not match");

        for (int i = 0; i < nrowsA; ++i) {
            controller.bailOutIfCancelled();

            for (int j = 0; j < ncolsB; ++j) {
                long x = 0;
                for (int k = 0; k < ncolsA; ++k)
                    x = ((A[i][k] * B[k][j]) % m + x) % m;

                R[i][j] = x;
            }
        }
    }


    public static long[][]
        modularMatrixProduct(final long[][] A, final long[][] B, final long m)
    {
        final long R[][] = new long[A.length][B[0].length];
        modularMatrixProduct(A, B, R, m);
        return R;
    }


    public static void
        integerMatrixProduct(final long[][] A, final long[][] B,
                             final Whole[][] R)
    {
        final TaskController controller = TaskController.getInstance();

        final int nrowsA = A.length;
        final int ncolsA = A[0].length;
        final int nrowsB = B.length;
        final int ncolsB = B[0].length;
        final int nrowsR = R.length;
        final int ncolsR = R[0].length;

        if (nrowsR != nrowsA || ncolsR != ncolsB || ncolsA != nrowsB)
            throw new ArithmeticException("matrix shapes do not match");

        for (int i = 0; i < nrowsA; ++i) {
            controller.bailOutIfCancelled();

            for (int j = 0; j < ncolsB; ++j) {
                Whole x = Whole.ZERO;
                for (int k = 0; k < ncolsA; ++k)
                    x = (Whole) x.plus((new Whole(A[i][k])).times(B[k][j]));

                R[i][j] = x;
            }
        }
    }


    public static Whole[][]
        integerMatrixProduct(final long[][] A, final long[][] B)
    {
        final Whole R[][] = new Whole[A.length][B[0].length];
        integerMatrixProduct(A, B, R);
        return R;
    }


    private static double columnNorm(final long[][] A, final int j) {
        double sum = 0.0;
        for (int i = 0; i < A.length; ++i)
            sum += A[i][j] * A[i][j];

        return Math.sqrt(sum);
    }


    private static int
        pAdicStepsNeeded(final long[][] A, final long[][] b, final long p)
    {
        final int n = A.length;

        if (A[0].length != n)
            throw new ArithmeticException("matrix must be quadratic");

        final double[] logNorms = new double[n];
        for (int i = 0; i < n; ++i)
            logNorms[i] = Math.log(columnNorm(A, i));

        Arrays.sort(logNorms);

        for (int i = 0; i < b[0].length; ++i)
            logNorms[0] = Math.max(logNorms[0], Math.log(columnNorm(b, i)));

        double logDelta = 0.0;
        for (int i = 0; i < n; ++i)
            logDelta += logNorms[i];

        final double phi = (1 + Math.sqrt(5)) / 2;

        return (int) Math.ceil(2 * (logDelta + Math.log(phi)) / Math.log(p));
    }


    private static Rational toRational(final Whole s, final Whole h) {
        Whole u0 = h;
        Whole u1 = s;
        Whole v0 = Whole.ZERO;
        Whole v1 = Whole.ONE;
        int sign = 1;

        while (u1.times(u1).compareTo(h) > 0) {
            final Whole q = (Whole) u0.div(u1);
            final Whole r = (Whole) u0.minus(q.times(u1));
            final Whole t = (Whole) v0.plus(q.times(v1));

            u0 = u1;
            u1 = r;
            v0 = v1;
            v1 = t;

            sign = -sign;
        }

        return (Rational) u1.times(sign).dividedBy(v1);
    }


    public static Matrix
        solve(final long[][] A, final long[][] b, final long p)
    {
        final TaskController controller = TaskController.getInstance();

        final int n = A.length;
        final int m = b[0].length;

        if (A[0].length != n)
            throw new ArithmeticException("matrix must be quadratic");

        if (b.length != n)
            throw new ArithmeticException("numbers of rows must be equal");

        final long[][] C = modularMatrixInverse(A, p);
        final int nrSteps = pAdicStepsNeeded(A, b, p);

        Whole pi = Whole.ONE;

        final long[][] bi = new long[n][m];
        for (int i = 0; i < n; ++i)
            for (int j = 0; j < m; ++j)
                bi[i][j] = b[i][j];

        final long[][] xi = new long[n][m];
        final Whole[][] Axi = new Whole[n][m];

        final Whole[][] si = new Whole[n][m];
        for (int i = 0; i < n; ++i)
            for (int j = 0; j < m; ++j)
                si[i][j] = Whole.ZERO;

        for (int k = 0; k < nrSteps; ++k) {
            modularMatrixProduct(C, bi, xi, p);

            for (int i = 0; i < n; ++i)
                for (int j = 0; j < m; ++j)
                    si[i][j] = (Whole) si[i][j].plus(pi.times(xi[i][j]));

            pi = (Whole) pi.times(p);

            if (k < nrSteps - 1) {
                integerMatrixProduct(A, xi, Axi);

                for (int i = 0; i < n; ++i) {
                    for (int j = 0; j < m; ++j) {
                        final Whole t = new Whole(bi[i][j]);
                        final Whole d = (Whole) t.minus(Axi[i][j]);
                        bi[i][j] = ((Whole) d.div(p)).longValue();
                    }
                }
            }
        }

        final Rational[][] R = new Rational[n][m];
        for (int i = 0; i < n; ++i) {
            controller.bailOutIfCancelled();

            for (int j = 0; j < m; ++j)
                R[i][j] = toRational(si[i][j], pi);
        }

        return new Matrix(R);
    }


    public static Matrix solve(final long[][] A, final long[][] b)
    {
        return solve(A, b, 0x7fffffff);
    }
}

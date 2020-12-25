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
import java.math.BigInteger;
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


    public static long[][]
        modularMatrixProduct(final long[][] A, final long[][] B, final long m)
    {
        final int nrowsA = A.length;
        final int ncolsA = A[0].length;
        final int nrowsB = B.length;
        final int ncolsB = B[0].length;

        if (ncolsA != nrowsB)
            throw new ArithmeticException("matrix shapes do not match");

        final long R[][] = new long[nrowsA][ncolsB];
        for (int i = 0; i < nrowsA; ++i) {
            for (int j = 0; j < ncolsB; ++j) {
                long x = 0;
                for (int k = 0; k < ncolsA; ++k)
                    x = ((A[i][k] * B[k][j]) % m + x) % m;

                R[i][j] = x;
            }
        }

        return R;
    }


    public static BigInteger[][]
        integerMatrixProduct(final long[][] A, final long[][] B)
    {
        final int nrowsA = A.length;
        final int ncolsA = A[0].length;
        final int nrowsB = B.length;
        final int ncolsB = B[0].length;

        if (ncolsA != nrowsB)
            throw new ArithmeticException("matrix shapes do not match");

        final BigInteger R[][] = new BigInteger[nrowsA][ncolsB];
        for (int i = 0; i < nrowsA; ++i) {
            for (int j = 0; j < ncolsB; ++j) {
                BigInteger x = BigInteger.ZERO;
                for (int k = 0; k < ncolsA; ++k) {
                    final BigInteger a = BigInteger.valueOf(A[i][k]);
                    final BigInteger b = BigInteger.valueOf(B[k][j]);

                    x = x.add(a.multiply(b));
                }

                R[i][j] = x;
            }
        }

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


    private static Rational
        toRational(final BigInteger s, final BigInteger h)
    {
        BigInteger u0 = h;
        BigInteger u1 = s;
        BigInteger v0 = BigInteger.ZERO;
        BigInteger v1 = BigInteger.ONE;
        BigInteger sign = BigInteger.ONE;

        while (u1.multiply(u1).compareTo(h) > 0) {
            final BigInteger[] quotRem = u0.divideAndRemainder(u1);

            u0 = u1;
            u1 = quotRem[1];

            final BigInteger t = v0;
            v0 = v1;
            v1 = t.add(quotRem[0].multiply(v1));

            sign = sign.negate();
        }

        final Whole numerator = new Whole(u1.multiply(sign));
        final Whole denominator = new Whole(v1);

        return (Rational) numerator.dividedBy(denominator);
    }


    public static Matrix
        solve(final long[][] A, final long[][] b, final long p)
    {
        final int n = A.length;
        final int m = b[0].length;
        final BigInteger P = BigInteger.valueOf(p);

        if (A[0].length != n)
            throw new ArithmeticException("matrix must be quadratic");

        if (b.length != n)
            throw new ArithmeticException("numbers of rows must be equal");

        final long[][] C = modularMatrixInverse(A, p);
        final int nrSteps = pAdicStepsNeeded(A, b, p);

        BigInteger pi = BigInteger.ONE;
        long[][] bi = b;

        BigInteger[][] si = new BigInteger[b.length][b[0].length];
        for (int i = 0; i < n; ++i)
            for (int j = 0; j < m; ++j)
                si[i][j] = BigInteger.ZERO;

        for (int k = 0; k < nrSteps; ++k) {
            final long[][] xi = modularMatrixProduct(C, bi, p);

            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < m; ++j) {
                    final BigInteger t = BigInteger.valueOf(xi[i][j]);
                    si[i][j] = si[i][j].add(pi.multiply(t));
                }
            }

            pi = pi.multiply(P);

            if (k < nrSteps - 1) {
                final BigInteger[][] Axi = integerMatrixProduct(A, xi);

                for (int i = 0; i < n; ++i) {
                    for (int j = 0; j < m; ++j) {
                        final BigInteger t = BigInteger.valueOf(bi[i][j]);
                        bi[i][j] = t.subtract(Axi[i][j]).divide(P).longValue();
                    }
                }
            }
        }

        final Rational[][] R = new Rational[n][m];
        for (int i = 0; i < n; ++i)
            for (int j = 0; j < m; ++j)
                R[i][j] = toRational(si[i][j], pi);

        return new Matrix(R);
    }


    public static Matrix solve(final long[][] A, final long[][] b)
    {
        return solve(A, b, 0x7fffffff);
    }
}

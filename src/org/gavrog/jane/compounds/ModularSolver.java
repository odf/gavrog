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

import org.gavrog.box.simple.TaskController;
import org.gavrog.jane.numbers.Fraction;
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


    public static long[][] modularRowEchelonForm(final long[][] M, long m) {
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


    public static long[][] modularMatrixInverse(final long[][] M, long m) {
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
}

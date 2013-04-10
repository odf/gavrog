/*
   Copyright 2013 Olaf Delgado-Friedrichs

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

import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

/**
 * Contains static methods implementing some of the classical algorithms from
 * linear algebra.
 * 
 * @author Olaf Delgado
 * @version $Id: LinearAlgebra.java,v 1.8 2006/03/08 05:53:06 odf Exp $
 */
public class LinearAlgebra {
    final static Real EPS = new FloatingPoint(1e-12);
    
    /**
     * Orthonormalizes the rows of a matrix B with respect to a given quadratic
     * form. In other words, computes a Matrix A such that A*M*A^t = I, for
     * given M and such that for any applicable k, the vector spaces spanned by
     * the first k rows of A and of B will be identical. This may fail if M is
     * not positive definite or A does not have full rank.
     * 
     * @param B the input matrix.
     * @param M a matrix representing a quadratic form.
     * @return an orthonormal basis as a matrix or null.
     */
    public static Matrix rowOrthonormalized(final Matrix B, final Matrix M) {
        // --- preparations
        final int n = M.numberOfRows();
        final int m = B.numberOfRows();
        
        // --- test arguments
        if (!M.equals(M.transposed())) {
            throw new IllegalArgumentException("second argument must be symmetric");
        }
        if (B.numberOfColumns() != n) {
            final String msg = "wrong number of columns for first argument";
            throw new IllegalArgumentException(msg);
        }
        if (m > n) {
            final String msg = "too many rows in first argument";
            throw new IllegalArgumentException(msg);
        }
        
        // --- create an array of the row vectors of B
        final Matrix e[] = new Matrix[m];
        for (int i = 0; i < m; ++i) {
            e[i] = B.getRow(i);
        }
        
        // --- perform a Gram-Schmidt orthonormalization on the vectors
        for (int i = 0; i < m; ++i) {
            Matrix v = e[i];
            for (int j = 0; j < i; ++j) {
                final Matrix dot = (Matrix) e[i].times(M).times(e[j].transposed());
                v = (Matrix) v.minus(e[j].times(dot.get(0, 0)));
            }
            final Matrix dot = (Matrix) v.times(M).times(v.transposed());
            final double d = ((Real) dot.get(0, 0)).doubleValue();
            if (d <= 0) {
                return null;
            }
            final double norm = Math.sqrt(d);
            e[i] = ((Matrix) v.dividedBy(norm));
        }
        
        // --- bake the orthonormal vectors into one matrix
        final Matrix A = new Matrix(m, n);
        for (int i = 0; i < m; ++i) {
            A.setRow(i, e[i]);
        }
        return A;
    }
    
    /**
     * Returns an orthonormal basis with respect to a given quadratic form. In
     * other words, computes a Matrix A such that A*M*A^t = I, for given M.
     * 
     * @param M a matrix representing a quadratic form.
     * @return a matrix with the orthonormal basis vectors as its rows.
     */
    public static Matrix orthonormalRowBasis(final Matrix M) {
        return rowOrthonormalized(Matrix.one(M.numberOfRows()), M);
    }

    /**
     * Constructs the orthogonal projection of a real vector space onto a
     * subspace.
     *
     * @param spanning a matrix the rows of which span the subspace.
     * @param G the gram matrix defining the metric.
     * @return the projection matrix.
     */
    public static Matrix orthogonalProjection(
            final Matrix spanning, final Matrix G) {
        // --- extract the dimensions of the total space and the subspace
        final int d = spanning.numberOfColumns();
        final int k = spanning.rank();

        // --- triangulate to extract a basis
        Matrix basisSub = spanning.mutableClone();
        Matrix.triangulate(basisSub, null, true, false);
        basisSub = basisSub.getSubMatrix(0, 0, k, d);

        // --- extend to a full basis
        final Matrix basisComplement =
                LinearAlgebra.columnNullSpace(basisSub, true).transposed();
        final Matrix basisFull = new Matrix(d, d);
        basisFull.setSubMatrix(0, 0, basisSub);
        basisFull.setSubMatrix(k, 0, basisComplement);

        // --- orthonormalize
        final Matrix basisOrtho =
                LinearAlgebra.rowOrthonormalized(basisFull, G);

        // --- the projection expressed in the new orthonormal basis
        final Matrix A = Matrix.one(d).mutableClone();
        for (int i = k; i < d; ++i) {
            A.set(i, i, Whole.ZERO);
        }

        // --- return the projection transformed to the standard basis
        return (Matrix) A.times(basisOrtho.inverse());
    }
    
    /**
     * Checks if the given matrix has non-zero entries only on the diagonal.
     * 
     * @param M the matrix to test
     * @return true if M is a diagonal matrix
     */
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
    
    /**
     * Returns an array of 3 matrices P, D, Q, where D is a diagonal matrix and P and Q
     * are unimodular matrices such that P*M*Q = D. Nonzero entries in D are consecutive
     * along the diagonal and, if present, start at the first position. If the parameter
     * integral is set, both P and Q will have integer entries only, otherwise, Q will be
     * the identity.
     * 
     * @param M the matrix to transform into a Smith normal form.
     * @param integral if true, stay within integer (Whole) numbers.
     * @return the array of matrices P, D, Q.
     */
    public static Matrix[] smithNormalForm(final Matrix M, final boolean integral) {
        
        final int n = M.numberOfRows();
        final int m = M.numberOfColumns();
        
        Matrix D = M.mutableClone();
        final Matrix P = Matrix.one(n).mutableClone();
        final Matrix Q = Matrix.one(m).mutableClone();
        
            do {
                Matrix.triangulate(D, P, integral, true);
                D = D.transposed().mutableClone();
                Matrix.triangulate(D, Q, integral, true);
                D = D.transposed().mutableClone();
            } while (!isDiagonal(D) && integral);
        
        return new Matrix[] {P, D, Q.transposed()};
    }
    
    /**
     * Returns a matrix A the columns of which form a basis of the column or
     * right null space of the input matrix M. The column or right null space of
     * M is defined as the space of all column vectors v with M*v=0.
     * 
     * @param M the input matrix.
     * @param integral if true, stay within integer (Whole) numbers.
     * @return a matrix representing the null space of M.
     */
    public static Matrix columnNullSpace(final Matrix M, final boolean integral) {
        final Matrix[] snf = smithNormalForm(M, integral);
        final Matrix D = snf[1];
        final Matrix Q = snf[2];
        final int m = Q.numberOfRows();
        final int r = D.rank();
        final Matrix A = new Matrix(m, m-r);
        A.setSubMatrix(0, 0, Matrix.zero(r, m-r));
        A.setSubMatrix(r, 0, Matrix.one(m-r));
        
        return (Matrix) Q.times(A);
    }
    
    /**
     * Returns a matrix A the rows of which form a basis of the row or
     * left null space of the input matrix M. The row or left null space of
     * M is defined as the space of all row vectors v with v*M=0.
     * 
     * @param M the input matrix.
     * @param integral if true, stay within integer (Whole) numbers.
     * @return a matrix representing the null space of M.
     */
    public static Matrix rowNullSpace(final Matrix M, final boolean integral) {
        return columnNullSpace(M.transposed(), integral).transposed();
    }
    
    /**
     * Computes an exact solution of the system A*x = b, where A and b are
     * given. If the parameters modZ is set, all entries in A and b must be
     * rational.
     * 
     * @param A left side input matrix.
     * @param b right side input matrix.
     * @param modZ if true, solve modulo the integers.
     * @return the solution matrix or null, if none exists.
     */
    public static Matrix solutionInColumns(final Matrix A, final Matrix b,
            final boolean modZ) {

        // --- check the arguments
        if (A == null || b == null) {
            throw new IllegalArgumentException("null argument");
        }
        if (A.numberOfRows() != b.numberOfRows()) {
            throw new IllegalArgumentException("matrix shapes don't match");
        }
        
        // --- get the matrix dimensions
        final int n = A.numberOfRows();
        final int m = A.numberOfColumns();
        final int k = b.numberOfColumns();
        
        // --- compute the Smith normal form
        final Matrix snf[] = smithNormalForm(A, modZ);
        final Matrix P = snf[0];
        final Matrix D = snf[1];
        final Matrix Q = snf[2];
        
        // --- construct a solution
        final Matrix v = (Matrix) P.times(b);
        final Matrix y = new Matrix(m, k);
        
        for (int i = 0; i < n; ++i) {
            final IArithmetic d = (i < m) ? D.get(i, i) : Whole.ZERO;
            for (int j = 0; j < k; ++j) {
                final IArithmetic r = v.get(i, j);
                final IArithmetic s;
                if (d.isZero()) {
                    if ((modZ && (r instanceof Whole)) || r.isZero()
                        || (r.isExact() == false && r.isLessOrEqual(EPS))) {
                        s = Whole.ZERO;
                    } else {
                        return null;
                    }
                } else {
                    s = r.dividedBy(d);
                }
                if (i < m) {
                    y.set(i, j, s);
                }
            }
        }
        
        // --- if successful, return the solution
        return (Matrix) Q.times(y);
    }
    
    /**
     * Computes an exact solution of the system x*A = b, where A and b are
     * given. If the parameters modZ is set, all entries in A and b must be
     * rational.
     * 
     * @param A left side input matrix.
     * @param b right side input matrix.
     * @param modZ if true, solve modulo the integers.
     * @return the solution matrix or null, if none exists.
     */
    public static Matrix solutionInRows(final Matrix A, final Matrix b,
            final boolean modZ) {
        final Matrix T = solutionInColumns(A.transposed(), b.transposed(), modZ);
        if (T == null) {
            return null;
        } else {
            return T.transposed();
        }
    }

    /**
     * Computes the dot product of two row vectors.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @param M the quadratic form determining the metric.
     * @return the value of the dot product.
     */
    public static IArithmetic dotRows(final Matrix v, final Matrix w, final Matrix M) {
        if (v.numberOfRows() != 1 || w.numberOfRows() != 1) {
            final String msg = "first two arguments must be row vectors";
            throw new IllegalArgumentException(msg);
        }
        final Matrix t = (Matrix) v.times(M).times(w.transposed());
        return t.get(0, 0);
    }
}

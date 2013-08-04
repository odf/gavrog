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

import org.gavrog.box.simple.TaskController;
import org.gavrog.jane.numbers.ArithmeticBase;
import org.gavrog.jane.numbers.Complex;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

public class Matrix extends ArithmeticBase {
    final private static boolean DEBUG = false;

    final private IArithmetic[] data;
    final private int nrows;
    final private int ncols;
    private boolean mutable;

    public Matrix(int height, int width) {
        this.nrows = height;
        this.ncols = width;
        this.data = new IArithmetic[height * width];
        this.mutable = true;
    }

    public Matrix(final Matrix M) {
        this.nrows = M.nrows;
        this.ncols = M.ncols;
        if (M.mutable) {
            this.data = (IArithmetic[]) M.data.clone();
        } else {
            this.data = M.data;
        }
        this.mutable = M.mutable;
    }
    
    public Matrix(IArithmetic[][] A) {
        this.nrows = A.length;
        this.ncols = A[0].length;
        this.data = new IArithmetic[nrows * ncols];
        this.mutable = false;

        int k = 0;
        for (int i = 0; i < nrows; ++i) {
            for (int j = 0; j < ncols; ++j) {
                this.data[k] = A[i][j];
                ++k;
            }
        }
    }

    public Matrix(int[][] A) {
        this.nrows = A.length;
        this.ncols = A[0].length;
        this.data = new IArithmetic[nrows * ncols];
        this.mutable = false;

        int k = 0;
        for (int i = 0; i < nrows; ++i) {
            for (int j = 0; j < ncols; ++j) {
                this.data[k] = new Whole(A[i][j]);
                ++k;
            }
        }
    }

    public Matrix(long[][] A) {
        this.nrows = A.length;
        this.ncols = A[0].length;
        this.data = new IArithmetic[nrows * ncols];
        this.mutable = false;

        int k = 0;
        for (int i = 0; i < nrows; ++i) {
            for (int j = 0; j < ncols; ++j) {
                this.data[k] = new Whole(A[i][j]);
                ++k;
            }
        }
    }

    public Matrix(double[][] A) {
        this.nrows = A.length;
        this.ncols = A[0].length;
        this.data = new IArithmetic[nrows * ncols];
        this.mutable = false;

        int k = 0;
        for (int i = 0; i < nrows; ++i) {
            for (int j = 0; j < ncols; ++j) {
                this.data[k] = new FloatingPoint(A[i][j]);
                ++k;
            }
        }
    }

    public int[] getShape() {
        int[] shape = { this.nrows, this.ncols };
        return shape;
    }

    public int numberOfRows() {
        return this.nrows;
    }
    
    public int numberOfColumns() {
        return this.ncols;
    }
    
    public double[][] asDoubleArray() {
        final double[][] result = new double[numberOfRows()][numberOfColumns()];
        for (int i  = 0; i < numberOfRows(); ++i) {
            for (int j = 0; j < numberOfColumns(); ++j) {
                result[i][j] = ((Real) get(i, j)).doubleValue();
            }
        }
        return result;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof Matrix) {
            Matrix other = (Matrix) obj;

            if (this.nrows != other.nrows || this.ncols != other.ncols) {
                return false;
            }

            for (int i = 0; i < this.nrows; ++i) {
                for (int j = 0; j < this.ncols; ++j) {
                	if (!this.get(i,j).equals(other.get(i,j))) {
                		return false;
                	}
                }
            }

            return true;
        } else {
            return false;
        }
    }
    
    public void makeImmutable() {
        this.mutable = false;
    }

    public boolean isMutable() {
        return this.mutable;
    }

    public IArithmetic get(int i, int j) {
        if (i < 0 || i >= this.nrows) {
            throw new ArrayIndexOutOfBoundsException("first index = " + i);
        } else if (j < 0 || j >= this.ncols) {
            throw new ArrayIndexOutOfBoundsException("second index = " + j);
        } else {
            return this.data[this.ncols * i + j];
        }
    }

    public void set(int i, int j, IArithmetic val) {
        if (! this.mutable) {
            throw new IllegalArgumentException("Matrix is immutable");
        } else if (i < 0 || i >= this.nrows) {
            throw new ArrayIndexOutOfBoundsException("first index = " + i);
        } else if (j < 0 || j >= this.ncols) {
            throw new ArrayIndexOutOfBoundsException("second index = " + j);
        } else {
            this.data[this.ncols * i + j] = val;
        }
    }

    public Matrix getSubMatrix(final int i, final int j, final int n, final int m) {
        final Matrix res = new Matrix(n, m);
        for (int r = 0; r < n; ++r) {
            for (int s = 0; s < m; ++s) {
                res.set(r, s, this.get(i+r, j+s));
            }
        }
        return res;
    }
    
    /**
     * Yields the result of deleting a specific row and column from this matrix.
     * 
     * @param i the index of the row to delete.
     * @param j the index of the column to delete.
     * @return the result of the deletions.
     */
    public Matrix getMinor(final int i, final int j) {
        final int n = this.nrows;
        final int m = this.ncols;
        final Matrix res = new Matrix(n - 1, m - 1);
        int k = 0;
        for (int r = 0; r < n; ++r) {
            if (r != i) {
                int l = 0;
                for (int c = 0; c < m; ++c) {
                    if (c != j) {
                        res.set(k, l, this.get(r, c));
                        ++l;
                    }
                }
                ++k;
            }
        }
        return res;
    }
    
    public void setSubMatrix(final int i, final int j, final Matrix A) {
        for (int r = 0; r < A.numberOfRows(); ++r) {
            for (int s = 0; s < A.numberOfColumns(); ++s) {
                this.set(i+r, j+s, A.get(r, s));
            }
        }
    }
    
    public Matrix getRow(final int i) {
        final Matrix row = new Matrix(1, ncols);
        for (int j = 0; j < ncols; ++j) {
            row.set(0, j, this.get(i, j));
        }
        row.makeImmutable();
        return row;
    }
    
    public Matrix[] getRows() {
        final Matrix rows[] = new Matrix[nrows];
        for (int i = 0; i < nrows; ++i) {
            rows[i] = getRow(i);
        }
        return rows;
    }
    
    public void setRow(final int i, final Matrix row) {
        if (row.numberOfRows() != 1 || row.numberOfColumns() != ncols) {
            throw new IllegalArgumentException("bad shape for row");
        }
        for (int j = 0; j < ncols; ++j) {
            set(i, j, row.get(0, j));
        }
    }
    
    public Matrix getColumn(final int j) {
        final Matrix column = new Matrix(nrows, 1);
        for (int i = 0; i < nrows; ++i) {
            column.set(i, 0, this.get(i, j));
        }
        column.makeImmutable();
        return column;
    }
    
    public void setColumn(final int j, final Matrix column) {
        if (column.numberOfRows() != nrows || column.numberOfColumns() != 1) {
            throw new IllegalArgumentException("bad shape for column");
        }
        for (int i = 0; i < nrows; ++i) {
            set(i, j, column.get(i, 0));
        }
    }
    
    public IArithmetic zero() {
        Matrix res = new Matrix(this.nrows, this.ncols);
            
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res.set(i, j, this.get(i, j).zero());
            }
        }

        res.makeImmutable();
        return res;
    }

    public IArithmetic one() {
        Matrix res = new Matrix(this.ncols, this.ncols);
        
        for (int i = 0; i < this.ncols; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                if (i == j) {
                    if (i < this.nrows) {
                        res.set(i, j, this.get(i, j).one());
                    } else {
                        res.set(i, j, Whole.ONE);
                    }
                } else {
                    if (i < this.nrows) {
                        res.set(i, j, this.get(i, j).zero());
                    } else {
                        res.set(i, j, Whole.ZERO);
                    }
                }
            }
        }

        res.makeImmutable();
        return res;
    }

    public static Matrix zero(final int n, final int m) {
        final Matrix res = new Matrix(n, m);
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < m; ++j) {
                res.set(i, j, Whole.ZERO);
            }
        }
        return res;
    }
    
    public static Matrix one(final int n) {
        final Matrix res = zero(n, n);
        for (int i = 0; i < n; ++i) {
            res.set(i, i, Whole.ONE);
        }
        return res;
    }
    
    public boolean isScalar() {
        return false;
    }

    public boolean isExact() {
        for (int i = 0; i < nrows; ++i) {
            for (int j = 0; j < ncols; ++j) {
                if (! this.get(i, j).isExact()) {
                    return false;
                }
            }
        }
        return true;
    }

    public Matrix transposed() {
        Matrix res = new Matrix(this.ncols, this.nrows);

        for (int i = 0; i < res.nrows; ++i) {
            for (int j = 0; j < res.ncols; ++j) {
                res.set(i, j, this.get(j, i));
            }
        }

        res.makeImmutable();
        return res;
    }

    public Matrix symmetric() {
        final int n = this.nrows;
        if (n != this.ncols) {
            throw new IllegalArgumentException("must be a square matrix");
        }
        final Whole two = new Whole(2);
        Matrix res = new Matrix(n, n);
        for (int i = 0; i < n; ++i) {
            res.set(i, i, this.get(i, i));
            for (int j = 0; j < i; ++j) {
                final IArithmetic x = this.get(i, j).plus(this.get(j, i)).dividedBy(two);
                res.set(i, j, x);
                res.set(j, i, x);
            }
        }
        res.makeImmutable();
        return res;
    }
    
    public Object clone() {
        Matrix res = new Matrix(this.nrows, this.ncols);
            
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res.set(i, j, this.get(i, j));
            }
        }

        res.makeImmutable();
        return res;
    }

    public Matrix mutableClone() {
        Matrix res = new Matrix(this.nrows, this.ncols);
            
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res.set(i, j, this.get(i, j));
            }
        }

        return res;
    }

    public Matrix dot(final Matrix other) {
        return (Matrix) this.times(other.transposed());
    }
    
    public Matrix scaled(IArithmetic factor) {
        Matrix res = new Matrix(this.nrows, this.ncols);
            
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res.set(i, j, this.get(i, j).times(factor));
            }
        }

        res.makeImmutable();
        return res;
    }

    public Matrix rscaled(IArithmetic factor) {
        Matrix res = new Matrix(this.nrows, this.ncols);
            
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res.set(i, j, factor.times(this.get(i, j)));
            }
        }

        res.makeImmutable();
        return res;
    }

    public IArithmetic negative() {
        Matrix res = new Matrix(this.nrows, this.ncols);
            
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res.set(i, j, this.get(i, j).negative());
            }
        }

        res.makeImmutable();
        return res;
    }
    
    /**
     * Checks if all entries of the given matrix are whole numbers and the determinant is
     * one.
     * 
     * @param M a matrix
     * @return true if M has only integer entries
     */
    public boolean isUnimodularIntegerMatrix() {
        for (int i = 0; i < numberOfRows(); ++i) {
            for (int j = 0; j < numberOfColumns(); ++j) {
                if (!(get(i, j) instanceof Whole)) {
                    return false;
                }
            }
        }
        return determinant().norm().isOne();
    }
    

    /**
     * Converts a matrix into upper triangular form using row operations.
     * @param A the matrix to triangulate.
     * @param B if present, takes the same sequence of row operations.
     * @param useTruncatedDivision if true, stay within integer (Whole) numbers.
     * @param clearAboveDiagonal if true, clear above diagonal as far as possible.
     * 
     * @return a number indicating the sign change in the determinant.
     */
    public static int triangulate(final Matrix A, final Matrix B,
            boolean useTruncatedDivision, final boolean clearAboveDiagonal) {

    	// --- used for answering external cancel request
    	final TaskController controller = TaskController.getInstance();
    	
        // --- verify arguments
        if (!A.isMutable()) {
            throw new IllegalArgumentException("first argument must be mutable");
        }
        if (B != null) {
            if (!B.isMutable()) {
                throw new IllegalArgumentException("second argument must be mutable");
            }
            if (B.nrows != A.nrows) {
                throw new IllegalArgumentException("shapes don't match");
            }
        }

        // --- "global" variables
        int sign = 1;
        int row = 0;
        int col = 0;

        // --- try to annihilate one entry at a time
        while (row < A.nrows && col < A.ncols) {
        	controller.bailOutIfCancelled();
            if (DEBUG) {
                System.err.println(A);
            }
            // --- find the entry of smallest norm in the current column
            IArithmetic pivot = null;
            int pivot_row = -1;

            for (int i = row; i < A.nrows; ++i) {
                IArithmetic val = A.get(i, col).norm();
                if (val instanceof FloatingPoint) {
                    // --- avoid near-zero pivots
                    // TODO use a relative rather than absolute threshhold
                    if (((Real) val).doubleValue() < 1e-12) {
                        val = new FloatingPoint(0.0);
                    }
                }
                if (!val.isZero() && (pivot == null || val.isLessThan(pivot))) {
                    pivot = val;
                    pivot_row = i;
                }
            }

            // --- if the current column is "clean", move on to the next one
            if (pivot == null) {
                col = col + 1;
                continue;
            }

            // --- move the pivot to the current row
            if (pivot_row != row) {
                for (int j = col; j < A.ncols; ++j) {
                    final IArithmetic tmp = A.get(row, j);
                    A.set(row, j, A.get(pivot_row, j));
                    A.set(pivot_row, j, tmp);
                }
                if (B != null) {
                    for (int j = 0; j < B.ncols; ++j) {
                        final IArithmetic tmp = B.get(row, j);
                        B.set(row, j, B.get(pivot_row, j));
                        B.set(pivot_row, j, tmp);
                    }
                }
                sign = -sign;
            }

            // --- make the pivot positive
            if (A.get(row, col).isNegative()) {
                for (int j = col; j < A.ncols; ++j) {
                    A.set(row, j, A.get(row, j).negative());
                }
                if (B != null) {
                    for (int j = 0; j < B.ncols; ++j) {
                        B.set(row, j, B.get(row, j).negative());
                    }
                }
                sign = -sign;
            }
            
            // --- attempt to clear the current column below the diagonal
            boolean cleared = true;

            for (int i = row+1; i < A.nrows; ++i) {
            	if (! A.get(i, col).isZero()) {
                    cleared = false;
                    final IArithmetic f;
                    if (useTruncatedDivision) {
                        f = ((Real) A.get(i, col)).div(A.get(row, col));
                    } else {
                        f = A.get(i, col).dividedBy(A.get(row, col));
                    }
                    for (int j = col; j < A.ncols; ++j) {
                        final IArithmetic a = A.get(row, j);
                        final IArithmetic b = A.get(i, j);
                        A.set(i, j, b.minus(f.times(a)));
                    }
                    if (!useTruncatedDivision) {
                        A.set(i, col, Whole.ZERO);
                    }
                    if (B != null) {
                        for (int j = 0; j < B.ncols; ++j) {
                            final IArithmetic a = B.get(row, j);
                            final IArithmetic b = B.get(i, j);
                            B.set(i, j, b.minus(f.times(a)));
                        }
                    }
                }
            }

            // --- if clearing was successful, move on
            if (cleared) {
                row = row + 1;
                col = col + 1;
            }
        }

        // --- if requested, try to clear above the diagonal
        if (clearAboveDiagonal) {
            col = 0;
            for (row = 0; row < A.nrows; ++row) {
                while (col < A.ncols && A.get(row, col).isZero()) {
                    col = col + 1;
                }
                if (col >= A.ncols) {
                    break;
                }
                for (int i = 0; i < row; ++i) {
                    if (A.get(i, col).isZero()) {
                        continue;
                    }
                    final IArithmetic f;
                    if (useTruncatedDivision) {
                        f = ((Real) A.get(i, col)).div(A.get(row, col));
                    } else {
                        f = A.get(i, col).dividedBy(A.get(row, col));
                    }
                    for (int j = col; j < A.ncols; ++j) {
                        final IArithmetic a = A.get(row, j);
                        final IArithmetic b = A.get(i, j);
                        A.set(i, j, b.minus(f.times(a)));
                    }
                    if (!useTruncatedDivision) {
                        A.set(i, col, Whole.ZERO);
                    }
                    if (B != null) {
                        for (int j = 0; j < B.ncols; ++j) {
                            final IArithmetic a = B.get(row, j);
                            final IArithmetic b = B.get(i, j);
                            B.set(i, j, b.minus(f.times(a)));
                        }
                    }
                }
            }
        }

        // --- whe're done
        return sign;
    }

    /**
     * Computes the rank of this matrix.
     * @return the rank.
     */
    public int rank() {
        final Matrix A = this.mutableClone();
        triangulate(A, null, false, false);
        int row = 0;
        for (int col = 0; col < numberOfColumns(); ++col) {
            if (row < numberOfRows() && !A.get(row, col).isZero()) {
                ++row;
            }
        }
        return row;
    }
    
    /**
     * Computes the determinant of this matrix.
     * @return the determinant.
     */
    public IArithmetic determinant() {
        if (numberOfRows() != numberOfColumns()) {
            throw new IllegalArgumentException("only defined for square matrices");
        }
        if (numberOfRows() == 0 || numberOfColumns() == 0) {
            return Whole.ZERO;
        }
        final Matrix A = this.mutableClone(); 
        final int sign = triangulate(A, null, false, false);
        IArithmetic result = new Whole(sign);
        final int k = Math.min(numberOfRows(), numberOfColumns());
        for (int i = 0; i < k; ++i) {
            result = result.times(A.get(i, i));
        }
        return result;
    }
    
    /**
     * Computes an exact solution to the system A*x=b, if one exists. If the
     * system has no solution, <code>null</code> is returned. If more than one
     * solution exists, an arbitrary one is picked. The complete solution space
     * is obtained by adding the result to the null space of the matrix A.
     * 
     * @param A left side of the equation.
     * @param b right side of the equation.
     * @return a solution, if one exists, else <code>null</code>.
     */
    public static Matrix solve(final Matrix A, final Matrix b) {
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
        
        // --- triangulate the left side
        final Matrix A1 = A.mutableClone();
        final Matrix b1 = b.mutableClone();
        triangulate(A1, b1, false, false);
        
        // --- determine a solution
        final Matrix x = new Matrix(m, k);
        final int top = Math.min(n, m);
        
        for (int j = 0; j < k; ++j) {
            for (int i = top-1; i >= 0; --i) {
                IArithmetic sum = null;
                for (int i1 = top-1; i1 > i; --i1) {
                    final IArithmetic product = A1.get(i, i1).times(x.get(i1, j));
                    if (sum == null) {
                        sum = product;
                    } else {
                        sum = sum.plus(product);
                    }
                }
                final IArithmetic rightSide;
                if (sum == null) {
                    rightSide = b1.get(i, j);
                } else {
                    rightSide = b1.get(i, j).minus(sum);
                }
                if (rightSide.isZero()) {
                    x.set(i, j, rightSide);
                } else if (A1.get(i, i).isZero()) {
                    return null;
                } else {
                    x.set(i, j, rightSide.dividedBy(A1.get(i, i)));
                }
            }
        }
        
        return x;
    }
    
    /* (non-Javadoc)
     * @see jumeric.IArithmetic#inverse()
     */
    public IArithmetic inverse() {
        if (numberOfRows() == numberOfColumns()) {
            final Matrix M = solve(this, (Matrix) this.one());
            if (M == null) {
                throw new ArithmeticException("matrix has no inverse");
            } else {
                return M;
            }
        } else {
            throw new IllegalArgumentException("mmust be a square matrix");
        }
    }

    public IArithmetic plus(Object obj) {
        if (obj instanceof Matrix) {
            Matrix other = (Matrix) obj;

            if (this.nrows != other.nrows || this.ncols != other.ncols) {
                throw new IllegalArgumentException();
            }

            Matrix res = new Matrix(this.nrows, this.ncols);
            
            for (int i = 0; i < this.nrows; ++i) {
                for (int j = 0; j < this.ncols; ++j) {
                    res.set(i, j, this.get(i, j).plus(other.get(i, j)));
                }
            }

            res.makeImmutable();
            return res;
        } else if (obj instanceof IArithmetic) {
            return ((IArithmetic) obj).rplus(this);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public IArithmetic rplus(IArithmetic other) {
        if (other instanceof Matrix) {
            return ((Matrix) other).plus(this);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public IArithmetic minus(Object obj) {
        if (obj instanceof Matrix) {
            Matrix other = (Matrix) obj;

            if (this.nrows != other.nrows || this.ncols != other.ncols) {
                throw new IllegalArgumentException("shapes don't match");
            }

            Matrix res = new Matrix(this.nrows, this.ncols);
            IArithmetic val;
            
            for (int i = 0; i < this.nrows; ++i) {
                for (int j = 0; j < this.ncols; ++j) {
                    val = this.get(i, j).minus(other.get(i, j));
                    res.set(i, j, val);
                }
            }

            res.makeImmutable();
            return res;
        } else if (obj instanceof IArithmetic) {
            return ((IArithmetic) obj).rminus(this);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public IArithmetic rminus(IArithmetic other) {
        if (other instanceof Matrix) {
            return ((Matrix) other).minus(this);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public IArithmetic times(Object obj) {
        if (obj instanceof Matrix) {
            Matrix other = (Matrix) obj;

            if (this.ncols != other.nrows) {
                throw new IllegalArgumentException("shapes don't match");
            }

            Matrix res = new Matrix(this.nrows, other.ncols);
            IArithmetic val;

            for (int i = 0; i < this.nrows; ++i) {
                for (int j = 0; j < other.ncols; ++j) {
                    val = this.get(i, 0).times(other.get(0, j));
                    for (int k = 1; k < this.ncols; ++k) {
                        val = val.plus(this.get(i, k).times(other.get(k, j)));
                    }
                    res.set(i, j, val);
                }
            }

            res.makeImmutable();
            return res;
        } else if (obj instanceof IArithmetic) {
            return this.scaled((IArithmetic) obj);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public IArithmetic rtimes(IArithmetic other) {
        if (other instanceof Matrix) {
            return ((Matrix) other).times(this);
        } else if (other instanceof Complex) {
            return this.rscaled(other);
        } else {
            throw new UnsupportedOperationException("operation not supported");
        }
    }

    public IArithmetic norm() {
        IArithmetic res = this.get(0, 0).zero();
        for (int i = 0; i < this.nrows; ++i) {
            for (int j = 0; j < this.ncols; ++j) {
                res = res.plus(this.get(i, j).norm());
            }
        }

        return res;
    }

    public String toString() {
        StringBuffer tmp = new StringBuffer(1000);
        tmp.append("Matrix([");
        for (int i = 0; i < nrows; ++i) {
            if (i > 0) {
                tmp.append(",");
            }
            tmp.append("[");
            for (int j = 0; j < ncols; ++j) {
                if (j > 0) {
                    tmp.append(",");
                }
                if (get(i,j) != null) {
                    tmp.append(get(i,j).toString());
                }
            }
            tmp.append("]");
        }
        tmp.append("])");
        return tmp.toString();
    }

    public int hashCode() {
        int code = 31 * nrows + ncols;
        for (int i = 0; i < nrows * ncols; ++i) {
            code = 31 * code + data[i].hashCode();
        }
        return code;
    }

	public int compareTo(Object other) {
	    throw new ArithmeticException("matrices are not ordered");
	}

    public IArithmetic floor() {
        throw new ArithmeticException("matrices are not ordered");
    }

	/* Convenience methods for use with Jython. */

    public IArithmetic __getitem__(int[] idx) {
        return get(idx[0], idx[1]);
    }

    public void __setitem__(int[] idx, IArithmetic val) {
        set(idx[0], idx[1], val);
    }

    public void __setitem__(int[] idx, long val) {
        set(idx[0], idx[1], new Whole(val));
    }

    public void __setitem__(int[] idx, double val) {
        set(idx[0], idx[1], new FloatingPoint(val));
    }
}

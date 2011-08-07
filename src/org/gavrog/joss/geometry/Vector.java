/*
Copyright 2008 Olaf Delgado-Friedrichs

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

import java.util.List;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.ArithmeticBase;
import org.gavrog.jane.numbers.Complex;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Rational;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;

/**
 * A d-dimensional vector represented by a row vector. To make interaction with
 * other geometry types easier, a zero coordinate is added internally.
 * 
 * @author Olaf Delgado
 * @version $Id: Vector.java,v 1.32 2008/02/27 08:14:27 odf Exp $
 */
public class Vector extends ArithmeticBase implements IArithmetic {
    final Matrix coords;
    final int dimension;

    /**
     * Creates a new vector from a row matrix containing its cartesian
     * coordinates. The dimension d of the vector created will correspond to the
     * number of columns in the given matrix. An extra coordinate of value 0
     * will be added internally.
     * 
     * @param M contains the coordinates for the vector.
     */
    public Vector(final Matrix M) {
        if (M.numberOfRows() != 1) {
            throw new IllegalArgumentException("matrix must have exactly 1 row");
        }
        final int d = M.numberOfColumns();
        this.dimension = d;
        this.coords = new Matrix(1, d + 1);
        this.coords.setSubMatrix(0, 0, M);
        this.coords.set(0, d, Whole.ZERO);
    }

    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final IArithmetic[] coordinates) {
        this(new Matrix(new IArithmetic[][] {coordinates}));
    }
    
    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final int[] coordinates) {
        this(new Matrix(new int[][] {coordinates}));
    }
    
    /**
     * Creates a new vector from its cartesian coordinates represented as an
     * array. The dimension d of the vector created will correspond to the
     * number of an entries in the array. An extra coordinate of value 0 will be
     * added internally.
     * 
     * @param coordinates the coordinates for the vector.
     */
    public Vector(final double[] coordinates) {
        this(new Matrix(new double[][] {coordinates}));
    }
    
    /**
     * Creates a new 2-dimensional vector.
     * 
     * @param a the first coordinate.
     * @param b the second coordinate.
     */
    public Vector(final int a, final int b) {
        this(new int[] { a, b });
    }
    
    /**
     * Creates a new 3-dimensional vector.
     * 
     * @param a the first coordinate.
     * @param b the second coordinate.
     * @param c the third coordinate.
     */
    public Vector(final int a, final int b, final int c) {
        this(new int[] { a, b, c });
    }
    
    /**
     * Creates a new 2-dimensional vector.
     * 
     * @param a the first coordinate.
     * @param b the second coordinate.
     */
    public Vector(final IArithmetic a, final IArithmetic b) {
        this(new IArithmetic[] { a, b });
    }
    
    /**
     * Creates a new 3-dimensional vector.
     * 
     * @param a the first coordinate.
     * @param b the second coordinate.
     * @param c the third coordinate.
     */
    public Vector(final IArithmetic a, final IArithmetic b, final IArithmetic c) {
        this(new IArithmetic[] { a, b, c });
    }
    
    /**
     * Creates a new vector as a copy of a given one.
     * @param v the model vector.
     */
    public Vector(final Vector v) {
        this.dimension = v.dimension;
        this.coords = v.coords;
    }
    
    /**
     * This constructor applies a matrix to a vector. It is used to apply a
     * general projective operator to a vector.
     * 
     * @param v the original vector.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     */
    Vector(final Vector v, final Matrix M) {
        this(image(v, M));
    }

    /**
     * Returns the coordinates as an array of double values.
     * @return the coordinate array.
     */
    public double[] asDoubleArray() {
        final double[] result = new double[getDimension()];
        for (int i  = 0; i < getDimension(); ++i) {
            result[i] = ((Real) get(i)).doubleValue();
        }
        return result;
    }
    
    /**
     * Applies an operator to a vector.
     * 
     * @param v the original vector.
     * @param M the operator to apply as a (d+1)x(d+1) matrix.
     * @return the resulting vector coordinates.
     */
    private static Matrix image(final Vector v, final Matrix M) {
        final int d = v.getDimension();
        if (d != M.numberOfRows() - 1 || d != M.numberOfColumns() - 1) {
            throw new IllegalArgumentException("dimensions don't match");
        }
        final Matrix img = (Matrix) v.coords.times(M);
        if (!img.get(0, d).isZero()) {
            throw new UnsupportedOperationException(
                    "the operation did not produce a vector");
        }
        return img.getSubMatrix(0, 0, 1, d);
    }
    
    /**
     * Returns a zero vector of a certain dimension.
     * 
     * @param dimension the requested dimension of the vector.
     * @return a zero vector of the requested dimension.
     */
    public static Vector zero(final int dimension) {
        return new Vector(Matrix.zero(1, dimension));
    }
    
    /**
     * Returns a unit vector, that is, a vector with a 1 at exactly one position and 0
     * everywhere else.
     * 
     * @param dimension the dimension of the vector.
     * @param axis the index of the non-zero entry.
     * @return the new vector.
     */
    public static Vector unit(final int dimension, final int axis) {
        final int a[] = new int[dimension];
        a[axis] = 1;
        return new Vector(a);
    }
    
    /**
     * Returns a unit vector in the same direction as the given one.
     * 
     * @param source the input vector.
     * @return a vector of unit length in the same direction.
     */
    public static Vector unit(final Vector source) {
        final IArithmetic x = dot(source, source);
        if (x instanceof Real) {
            return (Vector) source.dividedBy(((Real) x).sqrt());
        } else {
            throw new IllegalArgumentException("cannot take square root");
        }
    }
    
    /**
     * Computes the product of two vectors with respect to a given quadratic form.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @param form a matrix representing the quadratic form.
     * @return the value of the product.
     */
    public static IArithmetic dot(final Vector v, final Vector w, final Matrix form) {
        final int d = v.getDimension();
        if (w.getDimension() != d || form.numberOfRows() != d
                || form.numberOfColumns() != d) {
            throw new IllegalArgumentException("dimensions do not match");
        }
        if (!form.equals(form.transposed())) {
            throw new IllegalArgumentException("matrix must be symmetric, but was "
                                               + form);
        }
        final Matrix vc = v.getCoordinates();
        final Matrix wc = w.getCoordinates();
        return ((Matrix) vc.times(form).times(wc.transposed())).get(0, 0);
    }
    
    /**
     * Computes the standard dot product of two vectors.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @return the value of the product.
     */
    public static IArithmetic dot(final Vector v, final Vector w) {
        if (v.getDimension() != w.getDimension()) {
            throw new IllegalArgumentException("dimensions do not match");
        }
        final Matrix vc = v.getCoordinates();
        final Matrix wc = w.getCoordinates();
        return ((Matrix) vc.times(wc.transposed())).get(0, 0);
    }
    
    /**
     * Computes the cross product of two 3-dimensional vectors.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @return the cross product.
     */
    public static Vector crossProduct3D(final Vector v, final Vector w) {
        if (v.getDimension() != 3 || w.getDimension() != 3) {
            throw new IllegalArgumentException("both vectors must be 3-dimensional");
        }
        final IArithmetic v0 = v.get(0);
        final IArithmetic v1 = v.get(1);
        final IArithmetic v2 = v.get(2);
        final IArithmetic w0 = w.get(0);
        final IArithmetic w1 = w.get(1);
        final IArithmetic w2 = w.get(2);
        final IArithmetic x0 = v1.times(w2).minus(v2.times(w1));
        final IArithmetic x1 = v2.times(w0).minus(v0.times(w2));
        final IArithmetic x2 = v0.times(w1).minus(v1.times(w0));
        return new Vector(new IArithmetic[] { x0, x1, x2 });
    }
    
    /**
     * Computes the oriented volume of the parallelohedron spanned by three
     * 3-dimensional vectors with respect to the usual metric.
     * 
     * @param u the first vector.
     * @param v the second vector.
     * @param w the third vector.
     * @return the volume.
     */
    public static IArithmetic volume3D(final Vector u, final Vector v, final Vector w) {
        return dot(u, crossProduct3D(v, w));
    }
    
    /**
     * Computes the oriented area of the parallelogram spanned by two
     * 2-dimensional vectors with respect to the usual metric.
     * 
     * @param v the first vector.
     * @param w the second vector.
     * @return the area.
     */
    public static IArithmetic area2D(final Vector v, final Vector w) {
        if (v.getDimension() != 2 || w.getDimension() != 2) {
            throw new IllegalArgumentException("both vectors must be 2-dimensional");
        }
        final IArithmetic v0 = v.get(0);
        final IArithmetic v1 = v.get(1);
        final IArithmetic w0 = w.get(0);
        final IArithmetic w1 = w.get(1);
        return v0.times(w1).minus(v1.times(w0));
    }
    
    /**
     * Tests if this vector is collinear to another. Both vectors must have the
     * same dimension. The zero vector is considered non-collinear to any other,
     * even another zero vector.
     * 
     * @param v the other vector.
     * @return true if the two vectors are collinear.
     */
    public boolean isCollinearTo(final Vector v) {
        final int d = v.getDimension();
        if (getDimension() != d) {
            throw new UnsupportedOperationException("dimensions must be equal");
        }
        for (int i = 0; i < d; ++i) {
            if (!this.get(i).isZero()) {
                final IArithmetic q = v.get(i).dividedBy(this.get(i));
                return v.equals(this.times(q));
            }
        }
        return false;
    }
    
    /**
     * Tests if this vector is orthogonal to another. Both vectors must have the
     * same dimension. The zero vector is considered orthogonal to any other
     * vector.
     * 
     * @param v the other vector.
     * @return true if the two vectors are orthogonal.
     */
    public boolean isOrthogonalTo(final Vector v) {
        return dot(this, v).isZero();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.joss.geometry.IPoint#getDimension()
     */
    public int getDimension() {
        return this.dimension;
    }
    
    /**
     * Retrieves a cartesian coordinate value for this vector.
     * 
     * @param i the index of the coordinate to retrieve.
     * @return the coordinate value.
     */
    public IArithmetic get(final int i) {
        if (i < 0 || i > getDimension()) {
            throw new IllegalArgumentException("index out of range");
        }
        return this.coords.get(0, i);
    }
    
    /**
     * Syntactic sugar for jython. Makes vector[i] retrieve the i-th coordinate.
     * @param i the index of a coordinate to retrieve.
     * @return the coordinate value.
     */
    public IArithmetic __getitem__(final int i) {
        return get(i);
    }
    
    /**
     * Retrieves all cartesian coordinate values for this vector.
     * 
     * @return the coordinates as a row matrix.
     */
    public Matrix getCoordinates() {
        return this.coords.getSubMatrix(0, 0, 1, getDimension());
    }

    /**
     * Reduces the coordinate values of a vector modulo one. All entries must be of type
     * Real.
     * 
     * @return a copy of the input vector with each entry reduced modulo one.
     */
    public Vector modZ() {
        final Real res[] = new Rational[getDimension()];
        for (int i = 0; i < getDimension(); ++i) {
            res[i] = (Real) ((Real) get(i)).mod(1);
        }
        return new Vector(res);
    }
    
    /**
     * Checks if all vector entries are integers.
     * 
     * @return true if vector consists completely of integers.
     */
    public boolean isIntegral() {
        for (int i = 0; i < getDimension(); ++i) {
            if (! (get(i) instanceof Whole)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Compares two vectors lexicographically.
     * 
     * @param other the vector to compare with.
     */
    public int compareTo(final Object other) {
        if (other instanceof Vector) {
            final Vector v = (Vector) other;
            if (getDimension() != v.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            for (int i = 0; i < getDimension(); ++i) {
                final int d = get(i).compareTo(v.get(i));
                if (d != 0) {
                    return d;
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("can only compare two vectors");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#floor()
     */
    public IArithmetic floor() {
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getCoordinates().hashCode();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#inverse()
     */
    public IArithmetic inverse() {
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#isExact()
     */
    public boolean isExact() {
        return coords.isExact();
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#negative()
     */
    public IArithmetic negative() {
        return new Vector((Matrix) getCoordinates().negative());
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#one()
     */
    public IArithmetic one() {
        throw new UnsupportedOperationException("not defined on vectors");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        if (other instanceof Vector) {
            final Vector v = (Vector) other;
            return new Vector((Matrix) this.getCoordinates().plus(v.getCoordinates()));
        } else if (other instanceof Point){
            final Point p = (Point) other;
            return new Point((Matrix) this.getCoordinates().plus(p.getCoordinates()));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof Operator) {
            return new Vector(this, ((Operator) other).getCoordinates());
        } else if (other instanceof Complex) {
            return new Vector((Matrix) getCoordinates().times(other));
        } else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rtimes(this);
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#times(java.lang.Object)
     */
    public IArithmetic rtimes(final IArithmetic other) {
        if (other instanceof Complex) {
            return new Vector((Matrix) getCoordinates().times(other));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuffer tmp = new StringBuffer(1000);
        tmp.append("Vector(");
        for (int i = 0; i < getDimension(); ++i) {
            if (i > 0) {
                tmp.append(",");
            }
            if (get(i) != null) {
                tmp.append(get(i).toString());
            }
        }
        tmp.append(")");
        return tmp.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gavrog.jane.numbers.IArithmetic#zero()
     */
    public IArithmetic zero() {
        return new Vector((Matrix) getCoordinates().zero());
    }
    
    /**
     * Constructs an array of vectors from the rows of the given matrix.
     * 
     * @param M the input matrix.
     * @return the rows of M as vectors.
     */
    public static Vector[] rowVectors(final Matrix M) {
        return fromMatrix(M);
    }
    
    /**
     * Splits a matrix into its row vectors.
     * 
     * @param M the input matrix.
     * @return the array of row vectors from the input matrix.
     */
    public static Vector[] fromMatrix(final Matrix M) {
        final int n = M.numberOfRows();
        final Vector[] rows = new Vector[n];
        for (int i = 0; i < n; ++i) {
            rows[i] = new Vector(M.getRow(i));
        }
        return rows;
    }
    
    /**
     * Makes vectors of common dimension into rows of a matrix.
     * 
     * @param rows an array of vectors.
     * @return the matrix with the given vectors as its rows.
     */
    public static Matrix toMatrix(final Vector[] rows) {
        final int n = rows.length;
        final int m = rows[0].dimension;
        final Matrix M = new Matrix(n, m);
        for (int i = 0; i < n; ++i) {
            M.setRow(i, rows[i].getCoordinates());
        }
        M.makeImmutable();
        return M;
    }
    
    /**
     * Makes vectors of common dimension into rows of a matrix.
     * 
     * @param rows a list of vectors.
     * @return the matrix with the given vectors as its rows.
     */
    public static Matrix toMatrix(final List rows) {
        final int n = rows.size();
        if (n == 0) {
            return new Matrix(0, 0);
        }
        final int m = ((Vector) rows.get(0)).dimension;
        final Matrix M = new Matrix(n, m);
        for (int i = 0; i < n; ++i) {
            M.setRow(i, ((Vector) rows.get(i)).getCoordinates());
        }
        M.makeImmutable();
        return M;
    }
}

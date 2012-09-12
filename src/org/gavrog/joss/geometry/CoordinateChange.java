/*
Copyright 2012 Olaf Delgado-Friedrichs

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
import java.util.Collection;
import java.util.List;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.ArithmeticBase;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Whole;

/**
 * An coordinate change operator that can act on points, vectors and operators in order to
 * transform their representation into one fitting for a different choice of basis and
 * or/origin.
 * 
 * @author Olaf Delgado
 * @version $Id: CoordinateChange.java,v 1.7 2007/03/02 06:48:28 odf Exp $
 */
public class CoordinateChange extends ArithmeticBase implements IArithmetic {
    final Matrix left;
    final Matrix right;
    final int dimension;
    
    /**
     * Creates a new instance. The input consists of a matrix, the rows of which
     * represent the new basis vectors, and a point specifying the new origin.
     * 
     * @param basis the d x d matrix representing the new basis.
     * @param origin the new origin.
     */
    public CoordinateChange(final Matrix basis, final Point origin) {
    	final Matrix B = extendedToBasis(basis);
        final int d = B.numberOfRows();
        this.left = new Matrix(d+1, d+1);
        this.left.setSubMatrix(0, 0, B);
        this.left.setSubMatrix(d, 0, origin.getCoordinates());
        this.left.setSubMatrix(0, d, Matrix.zero(d, 1));
        this.left.set(d, d, Whole.ONE);
        this.right = (Matrix) this.left.inverse();
        this.dimension = d;
    }

    /**
     * Creates a new instance. The input is a matrix the rows of which
     * represent the new basis vectors. The origin is not changed.
     * 
     * @param basis the d x d matrix representing the new basis.
     */
    public CoordinateChange(final Matrix basis) {
    	this(basis, new Point(Matrix.zero(1, basis.numberOfColumns())));
    }

    /**
     * Creates an instance from an explicit coordinate change operator.
     * 
     * @param transform maps points and vectors to new coordinate system.
     */
    public CoordinateChange(final Operator transform) {
        //TODO check the argument
        this.right = transform.getCoordinates();
        this.left = (Matrix) this.right.inverse();
        this.dimension = transform.getDimension();
    }
    
    /**
     * Quick and dirty constructor for internal use.
     * 
     * @param left the coordination matrix for the new instance.
     * @param right the inverse of the coordination matrix.
     */
    private CoordinateChange(final Matrix left, final Matrix right) {
        final int d = left.numberOfRows() - 1;
        this.left = left;
        this.right = right;
        this.dimension = d;
    }
    
    /**
     * Checks if the non-zero rows in the given matrix are linearly independent
     * and if so, extend them to a basis of the total space.
     * @param basis a list of row vectors given as a matrix.
     * @return a matrix containing a basis of row vectors.
     */
    private Matrix extendedToBasis(final Matrix basis) {
    	final int d = basis.numberOfColumns();
    	final int m = basis.numberOfRows();
    	int r = basis.rank();
    	if (!basis.getSubMatrix(r, 0, m-r, d).isZero()) {
    		final String msg = "matrix does not represent a subspace basis";
    		throw new IllegalArgumentException(msg);
    	}
    	final Matrix B;
		if (r < d) {
			B = new Matrix(d, d);
			B.setSubMatrix(0, 0, basis.getSubMatrix(0, 0, r, d));
			B.setSubMatrix(r, 0, Matrix.zero(d - r, d));
			final Matrix I = Matrix.one(d);
			for (int k = 0; k < d; ++k) {
				B.setRow(r, I.getRow(k));
				if (B.rank() > r) {
					++r;
					if (r >= d) {
						break;
					}
				}
			}
		} else {
			B = basis;
		}
    	return B;
    }
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see org.gavrog.jane.numbers.ArithmeticBase#isExact()
	 */
    public boolean isExact() {
        return this.left.isExact();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#zero()
     */
    public IArithmetic zero() {
        throw new UnsupportedOperationException("operation not defined");
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#one()
     */
    public IArithmetic one() {
        final int d = this.dimension;
        return new CoordinateChange(Matrix.one(d));
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#negative()
     */
    public IArithmetic negative() {
        throw new UnsupportedOperationException("operation not defined");
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#inverse()
     */
    public IArithmetic inverse() {
        return new CoordinateChange(this.right, this.left);
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#plus(java.lang.Object)
     */
    public IArithmetic plus(final Object other) {
        throw new UnsupportedOperationException("operation not defined");
    }

    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#times(java.lang.Object)
     */
    public IArithmetic times(final Object other) {
        if (other instanceof CoordinateChange) {
            final CoordinateChange bt = (CoordinateChange) other;
            return new CoordinateChange((Matrix) bt.left.times(this.left),
                    (Matrix) this.right.times(bt.right));
        } else if (other instanceof IArithmetic) {
            return ((IArithmetic) other).rtimes(this);
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.IArithmetic#rtimes(org.gavrog.jane.numbers.IArithmetic)
     */
    public IArithmetic rtimes(final IArithmetic other) {
        if (other instanceof Point) {
            return new Point((Point) other, this.right);
        } else if (other instanceof Vector) {
            return new Vector((Vector) other, this.right);
        } else if (other instanceof Operator) {
            final Operator op = (Operator) other;
            return new Operator((Matrix) this.left.times(op.getCoordinates()).times(
                    this.right));
        } else {
            throw new UnsupportedOperationException("operation not defined");
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#compareTo(java.lang.Object)
     */
    public int compareTo(final Object other) {
        if (other instanceof CoordinateChange) {
            final CoordinateChange ob = (CoordinateChange) other;
            final int dim = getDimension();
            if (dim != ob.getDimension()) {
                throw new IllegalArgumentException("dimensions must be equal");
            }
            final Matrix A = this.left;
            final Matrix B = ob.left;
            for (int i = 0; i < dim+1; ++i) {
                for (int j = 0; j < dim+1; ++j) {
                    final int d = A.get(i, j).compareTo(B.get(i, j));
                    if (d != 0) {
                        return d;
                    }
                }
            }
            return 0;
        } else {
            throw new IllegalArgumentException("illegal argument type");
        }
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#floor()
     */
    public IArithmetic floor() {
        throw new UnsupportedOperationException();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#toString()
     */
    public String toString() {
        final StringBuffer buf = new StringBuffer(60);
        buf.append("CoordinateChange(");
        buf.append(getBasis().toString());
        buf.append(",");
        buf.append(getOrigin().toString());
        buf.append(")");
        return buf.toString();
    }
    
    /* (non-Javadoc)
     * @see org.gavrog.jane.numbers.ArithmeticBase#hashCode()
     */
    public int hashCode() {
        return left.hashCode();
    }
    
    /**
     * Returns the dimension of the underlying space.
     * 
     * @return the dimension.
     */
    public int getDimension() {
        return this.dimension;
    }
    
    /**
     * Returns the new basis in terms of the old one.
     * 
     * @return the new basis.
     */
    public Matrix getBasis() {
        final int d = getDimension();
        return this.left.getSubMatrix(0, 0, d, d);
    }
    
    /**
     * Returns the new origin in terms of the old origin and basis.
     * 
     * @return the new origin.
     */
    public Point getOrigin() {
        final int d = getDimension();
        return new Point(this.left.getSubMatrix(d, 0, 1, d));
    }
    
    /**
     * Retrieve the corresponding operator.
     */
    public Operator getOperator() {
        return new Operator(this.right);
    }
    
    /**
     * Performs a basis change on a Collection of geometric objects.
     * 
     * @param gens the objects to convert to the new basis.
     * @return the list of converted objects.
     */
    public List<Operator> applyTo(final Collection<Operator> gens) {
        final List<Operator> tmp = new ArrayList<Operator>();
        for (Operator gen: gens) {
            tmp.add((Operator) gen.times(this));
        }
        return tmp;
    }
}

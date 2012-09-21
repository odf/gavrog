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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;

/**
 * Various methods related to lattices and lattice bases.
 */
public class Lattices {

	/**
	 * Determines if the given vectors form a basis.
	 * @param v an array of vectors.
	 * @return true if the vectors form a basis.
	 */
	public static boolean isBasis(final Vector[] v) {
	    return !Vector.toMatrix(v).determinant().isZero();
	}

	/**
	 * Performs a gauss elimination on a pair of independent vectors.
	 * 
	 * @param v the input vectors.
	 * @param M the quadratic form determining the metric.
	 * @return reduced vectors spanning the same lattice.
	 */
	public static Vector[] gaussReduced(Vector[] v, Matrix M) {
	    if (v.length != 2) {
	        final String msg = "first argument must contain 2 vectors";
	        throw new IllegalArgumentException(msg);
	    }
	    if (!M.equals(M.transposed())) {
	        final String msg = "second argument must be a symmetric 2x2 matrix";
	        throw new IllegalArgumentException(msg);
	    }
	    
	    final Real eps = new FloatingPoint(1e-12);
	    IArithmetic sl[] = new IArithmetic[] { Vector.dot(v[0], v[0], M),
				Vector.dot(v[1], v[1], M) };
	    while (true) {
	        final int i = sl[0].isLessThan(sl[1]) ? 0 : 1;
	        final int j = 1 - i;
	        final IArithmetic t = Vector.dot(v[i], v[j], M).dividedBy(sl[i])
					.round();
	        v[j] = (Vector) v[j].minus(t.times(v[i]));
	        sl[j] = Vector.dot(v[j], v[j], M);
	        if (sl[j].isGreaterOrEqual(sl[i].minus(eps))) {
	            break;
	        }
	    }
	
	    if (Vector.dot(v[0], v[1], M).isPositive()) {
	        v[1] = (Vector) v[1].negative();
	    }
	    return v;
	}

	/**
	 * Performs a single step of the Selling reduction algorithm.
	 * 
	 * @param v the augmented list of basis vectors.
	 * @param M the quadratic form determining the metric.
	 * @return true if there was a change.
	 */
	private static boolean sellingStep(final Vector v[], final Matrix M) {
	    final Real eps = new FloatingPoint(1e-12);
	    for (int i = 0; i < 3; ++i) {
	        for (int j = i+1; j < 4; ++j) {
	            if (Vector.dot(v[i], v[j], M).isGreaterThan(eps)) {
	                for (int k = 0; k < 4; ++k) {
	                    if (k != i && k != j) {
	                        v[k] = (Vector) v[k].plus(v[i]);
	                    }
	                }
	                v[i] = (Vector) v[i].negative();
	                return true;
	            }
	        }
	    }
	    
	    return false;
	}

	/**
	 * Performs a Selling reduction on a set of 3 independent vectors.
	 * 
	 * @param v the input vectors.
	 * @param M the quadratic form determining the metric.
	 * @return reduced vectors spanning the same lattice.
	 */
	public static Vector[] sellingReduced(final Vector[] v, final Matrix M) {
	    if (v.length != 3) {
	        final String msg = "first argument must contain 3 vectors";
	        throw new IllegalArgumentException(msg);
	    }
	    if (!M.equals(M.transposed())) {
	        final String msg = "second argument must be a symmetric matrix";
	        throw new IllegalArgumentException(msg);
	    }
	    
	    final Vector[] w = new Vector[] { v[0], v[1], v[2],
	            (Vector) v[0].plus(v[1]).plus(v[2]).negative() };
	    
	    while (sellingStep(w, M)) {
	    }
	    
	    return new Vector[] { w[0], w[1], w[2] };
	}

	/**
	 * Computes the Dirichlet domain for a given vector lattice and returns the
	 * set of normal vectors for the pairs of parallel planes that bound it.
	 * 
	 * @param b vectors forming a lattice basis.
	 * @param M the quadratic form determining the metric.
	 * @return the normal vectors to the faces of the Dirichlet domain.
	 */
	public static Vector[] dirichletVectors(final Vector[] b, final Matrix M) {
	    final int dim = b.length;
	    if (!M.equals(M.transposed())) {
	        final String msg = "second argument must be symmetric, but was " + M;
	        throw new IllegalArgumentException(msg);
	    }

	    final Vector t[];
	    switch (dim) {
	    case 0:
	        return new Vector[] {};
	    case 1:
	        return new Vector[] { b[0] };
	    case 2:
	        t = gaussReduced(b, M);
	        return new Vector[] { t[0], t[1], (Vector) t[0].plus(t[1]) };
	    case 3:
	        t = sellingReduced(b, M);
	        return new Vector[] { t[0], t[1], t[2], (Vector) t[0].plus(t[1]),
	                (Vector) t[0].plus(t[2]), (Vector) t[1].plus(t[2]),
	                (Vector) t[0].plus(t[1]).plus(t[2]) };
	    default:
	        throw new UnsupportedOperationException("only dimensions up to 3 work");
	    }
	}

	/**
     * Returns a lattice basis of shortest Dirichlet vectors.
     * 
     * @param v original basis.
     * @param M the quadratic form determining the metric.
     * @return the reduced basis.
     */
	public static Vector[] reducedLatticeBasis(final Vector[] v, final Matrix M) {
	    final Vector tmp[] = dirichletVectors(v, M);
	    Arrays.sort(tmp, new Comparator<Vector>() {
	        public int compare(final Vector v1, final Vector v2) {
	            final int d = Vector.dot(v1, v1, M)
	            		.compareTo(Vector.dot(v2, v2, M));
	            if (d == 0) {
	                return v2.abs().compareTo(v1.abs());
	            } else {
	                return d;
	            }
	        }
	    });
	    
	    final int d = v[0].getDimension();
	    final Vector w[] = new Vector[d];
	    final Matrix A = Matrix.zero(d, d).mutableClone();
	    int k = 0;
	    for (int i = 0; i < d; ++i) {
	        while (k < tmp.length) {
	            w[i] = tmp[k];
	            if (w[i].isNegative()) {
	                w[i] = (Vector) w[i].negative();
	            }
	            if (i > 0 && Vector.dot(w[0], w[i], M).isPositive()) {
	                w[i] = (Vector) w[i].negative();
	            }
	            A.setRow(i, w[i].getCoordinates());
	            if (fuzzy_rank(A, 1e-8) > i) {
	                break;
	            }
	            ++k;
	        }
	    }
	    return w;
	}

    /**
     * Computes the rank of this matrix.
     * @return the rank.
     */
    private static int fuzzy_rank(final Matrix M, final double eps) {
        final Matrix A = M.mutableClone();
        Matrix.triangulate(A, null, false, false);
        int row = 0;
        for (int col = 0; col < A.numberOfColumns(); ++col) {
            if (row < A.numberOfRows()) {
            	final IArithmetic x = A.get(row, col);
            	final boolean zero;
            	if (x.isExact()) {
            		zero = x.isZero();
            	} else {
            		zero = ((Real) x.abs()).doubleValue() < Math.abs(eps);
            	}
            	if (!zero) {
            		++row;
            	}
            }
        }
        return row;
    }
    
	/**
	 * Returns the vector(s) by which a point has to be shifted in order to
	 * obtain a translationally equivalent point within the Dirichlet cell
	 * around the origin. All calculations are with respect to the unit lattice
	 * and an specific metric, which is passed as one of the arguments. An
	 * integral scaling factor can be specified, in which case both the unit
	 * lattice and its Dirchlet domain are taken to be scaled by that factor.
	 * 
	 * If the shifted point is close to a boundary of the Dirichlet domain, more
	 * multiple shift vectors may be returned, namely all those that would place
	 * the original point close enough to the domain.
	 * 
	 * @param pos the original point position.
	 * @param dirichletVectors normals to the parallel face pairs of the
	 *            Dirichlet cell.
	 * @param metric the underlying metric.
	 * @param factor a scaling factor.
	 * @return the shift vector needed to move the point inside.
	 */
	public static Vector[] dirichletShifts(final Point pos,
			final Vector dirichletVectors[], final Matrix metric, final int factor) {
	
	    final int dim = pos.getDimension();
	    final Real half = new Fraction(1, 2);
	    final Real minusHalf = new Fraction(-1, 2);
	    final double eps = 1e-8;
	    final double delta = 1e-2;
	    final Vector posAsVector = (Vector) pos.minus(Point.origin(dim));
	    Vector shift = Vector.zero(dim);
	    
	    // --- compute the first shift
	    while (true) {
	        boolean changed = false;
	        for (int i = 0; i < dirichletVectors.length; ++i) {
	            final Vector v = (Vector) dirichletVectors[i].times(factor);
	            final IArithmetic c = Vector.dot(v, v, metric);
	            final Vector p = (Vector) posAsVector.plus(shift);
	            final IArithmetic q = Vector.dot(p, v, metric).dividedBy(c);
	            if (q.isGreaterThan(half.plus(eps))) {
	                shift = (Vector) shift.minus(v.times(q.plus(half).floor()));
	                changed = true;
	            } else if (q.isLessOrEqual(minusHalf)) {
	                shift = (Vector) shift.minus(v.times(q.plus(half).minus(eps).floor()));
	                changed = true;
	            }
	        }
	        if (!changed) {
	            break;
	        }
	    }
	    
	    // --- compute further shifts
	    final Vector p = (Vector) posAsVector.plus(shift);
	    final Set<Vector> shifts = new HashSet<Vector>();
	    shifts.add(shift);
	    
	    for (int i = 0; i < dirichletVectors.length; ++i) {
	        final Vector v = (Vector) dirichletVectors[i].times(factor);
	        final IArithmetic c = Vector.dot(v, v, metric);
	        final IArithmetic q = Vector.dot(p, v, metric).dividedBy(c);
	        if (q.isGreaterThan(half.minus(delta))) {
	            shifts.add((Vector) shift.minus(v));
	        } else if (q.isLessThan(minusHalf.plus(delta))) {
	            shifts.add((Vector) shift.plus(v));
	        }
	    }
	
	    // --- convert results
	    final Vector results[] = new Vector[shifts.size()];
	    shifts.toArray(results);
	    return results;
	}
}

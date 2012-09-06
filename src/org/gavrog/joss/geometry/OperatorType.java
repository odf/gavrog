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

package org.gavrog.joss.geometry;

import org.gavrog.jane.compounds.Matrix;

/**
 * Encodes and determines the characteristica of the linear part of an operator
 * in a 2- or 3-dimensional crystallographic space group. When comparing two
 * objects, only the fields <code>dimension</code>,<code>order</code>,
 * <code>clockwise</code> and <code>orientationPreserving</code> are used.
 * 
 * @author Olaf Delgado
 * @version $Id: OperatorType.java,v 1.7 2005/09/20 05:25:10 odf Exp $
 */
public class OperatorType {
    final private int dimension;
    final private boolean orientationPreserving;
    final private int order;
    final private boolean clockwise;
    
    /**
     * Creates an instances with given characteristics.
     * 
     * @param d the dimension.
     * @param ori is it orientation-preserving?
     * @param n the order.
     * @param cw is the rotational component clockwise?
     */
    public OperatorType(final int d, final boolean ori, final int n, final boolean cw) {
        this.dimension = d;
        this.orientationPreserving = ori;
        this.order = n;
        this.clockwise = cw;
    }
    
    public String toString()
    {
        return "OperatorType(" + dimension + ", " + orientationPreserving
                + ", " + order + ", " + clockwise + ")";
    }
    
    /**
     * Creates a new instance by analysing an actual operator.
     * 
     * @param op the operator to analyze.
     */
    public OperatorType(final Operator op) {
        final int d = this.dimension = op.getDimension();
        final Vector axis = op.linearAxis();
        Matrix M = op.getCoordinates().getSubMatrix(0, 0, d, d);

        this.orientationPreserving = M.determinant().isNonNegative();
        if (d == 3 && !this.orientationPreserving) {
            M = (Matrix) M.negative();
        }
        this.order = matrixOrder(M, 6);
        
        switch (d) {
        case 1:
            this.clockwise = true;
            break;
        case 2:
            if (!this.isOrientationPreserving()) {
                this.clockwise = false;
            } else {
                if (this.order == 0 || this.order > 2) {
                    final Matrix v = new Matrix(new int[][] { { 1, 0 } });
                    final Matrix A = new Matrix(2, 2);
                    A.setRow(0, v);
                    A.setRow(1, (Matrix) v.times(M));
                    this.clockwise = A.determinant().isPositive();
                } else {
                    this.clockwise = true;
                }
            }
            break;
        case 3:
            if ((this.order == 0 || this.order > 2) && axis != null) {
                final Matrix a = axis.getCoordinates();
                final Matrix v;
                if (a.get(0, 1).isZero() && a.get(0, 2).isZero()) {
                    v = new Matrix(new int[][] { { 0, 1, 0 } });
                } else {
                    v = new Matrix(new int[][] { { 1, 0, 0 } });
                }
                final Matrix A = new Matrix(3, 3);
                A.setRow(0, axis.getCoordinates());
                A.setRow(1, v);
                A.setRow(2, (Matrix) v.times(M));
                this.clockwise = A.determinant().isPositive();
            } else {
                this.clockwise = true;
            }
            break;
        default:
            final String msg = "operator dimension is " + d + ", must be <=3";
            throw new UnsupportedOperationException(msg);
        }
    }
    
    /**
     * Determines the order of a matrix.
     * 
     * @param M the matrix.
     * @param max the maximum order considered.
     * @return the order of the matrix or 0, if larger than the maximum.
     */
    private static int matrixOrder(final Matrix M, final int max) {
        Matrix A = M;
        for (int i = 1; i <= max; ++i) {
            if (A.isOne()) {
                return i;
            }
            A = (Matrix) A.times(M);
        }
        return 0;
    }
    
    /**
     * @return the dimension of the operator.
     */
    public int getDimension() {
        return dimension;
    }
    
    /**
     * @return true if rotation is clockwise.
     */
    public boolean isClockwise() {
        return clockwise;
    }
    
    /**
     * @return the rotation order.
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * @return true if operator is orientation preserving.
     */
    public boolean isOrientationPreserving() {
        return orientationPreserving;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object other) {
        if (other instanceof OperatorType) {
            final OperatorType type = (OperatorType) other;
            return this.dimension == type.dimension && this.clockwise == type.clockwise
                   && this.order == type.order
                   && this.orientationPreserving == type.orientationPreserving;
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        int res = dimension * 37 + order;
        res = res * 37 + (clockwise ? 37 : 0) + (orientationPreserving ? 1 : 0);
        return res;
    }
}
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

import junit.framework.TestCase;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Whole;

/**
 * Unit tests for the Lattices class.
 * 
 * @author Olaf Delgado
 * @version $Id: TestLattices.java,v 1.1 2006/03/19 05:16:27 odf Exp $
 */
public class TestLattices extends TestCase {

	public void testIsBasis() {
	    final Vector a[] = { new Vector(new int[] { 1, 2, 3 }),
	            new Vector(new int[] { 4, 5, 6 }), new Vector(new int[] { 7, 8, 9 }) };
	    assertFalse(Lattices.isBasis(a));
	    final Vector b[] = { new Vector(new int[] { 1, 2 }),
	            new Vector(new int[] { 4, 5 }) };
	    assertTrue(Lattices.isBasis(b));
	    final Vector c[] = { new Vector(new int[] { 1, 2, 3 }),
	            new Vector(new int[] { 4, 5, 6 }), new Vector(new int[] { 7, 8, 8 }) };
	    assertTrue(Lattices.isBasis(c));
	}

	public void testGaussReduced() {
	    final Matrix G = new Matrix(new int[][] { { 4, 1 }, { 1, 5 } });
	    final Vector b[] = { new Vector(new int[] { 1, 2 }),
	            new Vector(new int[] { 4, 5 }) };
	    final Vector v[] = Lattices.gaussReduced(b, G);
	    assertTrue(Lattices.isBasis(v));
	    final Vector w[] = new Vector[] { v[0], v[1],
	            (Vector) v[0].negative().minus(v[1]) };
	    for (int i = 0; i < 2; ++i) {
	        for (int j = i + 1; j < 3; ++j) {
	            assertFalse(Vector.dot(w[i], w[j], G).isPositive());
	        }
	    }
	    final Matrix A = (Matrix) Vector.toMatrix(v).dividedBy(Vector.toMatrix(b));
	    assertTrue(A.determinant().abs().isOne());
	    for (int i = 0; i < 2; ++i) {
	        for (int j = 0; j < 2; ++j) {
	            assertTrue(A.get(i, j) instanceof Whole);
	        }
	    }
	}

	public void testSellingReduced() {
	    final Matrix G = new Matrix(new int[][] { { 4, 1, 3 }, { 1, 5, 2 }, { 3, 2, 6 } });
	    final Vector b[] = { new Vector(new int[] { 1, 2, 3 }),
	            new Vector(new int[] { 4, 5, 6 }), new Vector(new int[] { 7, 8, 8 }) };
	    final Vector v[] = Lattices.sellingReduced(b, G);
	    assertTrue(Lattices.isBasis(v));
	    final Vector w[] = new Vector[] { v[0], v[1], v[2],
	            (Vector) v[0].negative().minus(v[1]).minus(v[2]) };
	    for (int i = 0; i < 3; ++i) {
	        for (int j = i + 1; j < 4; ++j) {
	            assertFalse(Vector.dot(w[i], w[j], G).isPositive());
	        }
	    }
	    final Matrix A = (Matrix) Vector.toMatrix(v).dividedBy(Vector.toMatrix(b));
	    assertTrue(A.determinant().abs().isOne());
	    for (int i = 0; i < 3; ++i) {
	        for (int j = 0; j < 3; ++j) {
	            assertTrue(A.get(i, j) instanceof Whole);
	        }
	    }
	}

	public void testReducedBasis() {
	    final Matrix G = new Matrix(new int[][] { { 4, 1, 3 }, { 1, 5, 2 }, { 3, 2, 6 } });
	    final Vector b[] = { new Vector(new int[] { 1, 2, 3 }),
	            new Vector(new int[] { 4, 5, 6 }), new Vector(new int[] { 7, 8, 8 }) };
	    final Vector v[] = Lattices.reducedLatticeBasis(b, G);
	    assertTrue(Lattices.isBasis(v));
	    final Matrix A = (Matrix) Vector.toMatrix(v).dividedBy(Vector.toMatrix(b));
	    assertTrue(A.determinant().isOne());
	    for (int i = 0; i < 3; ++i) {
	        for (int j = 0; j < 3; ++j) {
	            assertTrue(A.get(i, j) instanceof Whole);
	        }
	    }
	}

}

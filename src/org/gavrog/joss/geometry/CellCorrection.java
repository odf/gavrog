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

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Whole;

/**
 * @author Olaf Delgado
 * @version $Id: CellCorrection.java,v 1.3 2008/01/08 04:40:05 odf Exp $
 */
public class CellCorrection {
	final private CoordinateChange coordinateChange;
	final private String groupName;
    
    public CellCorrection(final SpaceGroupFinder finder, final Matrix gram) {
		// --- extract some basic info
		final int dim = gram.numberOfRows();
		String name = finder.getGroupName();
		final CrystalSystem system = finder.getCrystalSystem();

		// --- check dimension
		if (dim != 3) {
			throw new UnsupportedOperationException("dimension must be 3");
		}

		// --- deal with trivial case quickly
		if (system != CrystalSystem.MONOCLINIC
				&& system != CrystalSystem.TRICLINIC) {
			this.coordinateChange = new CoordinateChange(Operator.identity(dim));
		} else {

			// --- the cell vectors in the original coordinate system
			final CoordinateChange fromStd = (CoordinateChange) finder
					.getToStd().inverse();
			final Vector a = (Vector) Vector.unit(3, 0).times(fromStd);
			final Vector b = (Vector) Vector.unit(3, 1).times(fromStd);
			final Vector c = (Vector) Vector.unit(3, 2).times(fromStd);

			// --- old and new cell vectors
			final Vector from[] = new Vector[] { a, b, c };
			final Vector to[];

			if (system == CrystalSystem.TRICLINIC) {
				to = Lattices.reducedLatticeBasis(from, gram);
			} else { // Monoclinic case
				// --- find the smallest vectors orthogonal to b
				final Vector old[] = new Vector[] { a, c };
				final Vector nu[] = Lattices.reducedLatticeBasis(old, gram);
				to = new Vector[] { nu[0], b, nu[1] };
				if (Vector.dot(to[0], to[2], gram).isPositive()) {
					to[2] = (Vector) to[2].negative();
				}
				
				// --- find symbols for transformed glide and centering vectors
				final CoordinateChange F = new CoordinateChange(Vector
						.toMatrix(from));
				final CoordinateChange T = new CoordinateChange(Vector
						.toMatrix(to));
				final CoordinateChange C = (CoordinateChange) F.inverse()
						.times(T);

				final Vector g = ((Vector) new Vector(0, 0, 1).dividedBy(
						new Whole(2)).times(C)).modZ();
				final char glide = g.get(0).isZero() ? 'c'
						: g.get(2).isZero() ? 'a' : 'n';
				final Vector s = ((Vector) new Vector(1, 0, 0).dividedBy(
						new Whole(2)).times(C)).modZ();
				final char centering = s.get(0).isZero() ? 'A' : s.get(2)
						.isZero() ? 'C' : 'I';

				// --- determine the type of group we're dealing with
				final boolean hasGlide = name.contains("c");
				final boolean hasCentering = name.contains("C");
				
				// --- now check and, if necessary, adjust the new setting
				boolean swap = false;
				
				if (hasCentering && hasGlide) {
					// --- the combinations 'In', 'Ca' and 'Ac' are impossible
					switch (centering) {
					case 'I': // 'Ia' or 'Ic', change latter to former
						name = name.replace('C', 'I').replace('c', 'a');
						swap = (glide == 'c');
						break;
					case 'A':
						if (glide == 'n') { // 'An', okay
							name = name.replace('C', 'A').replace('c', 'n');
						} else { // 'Aa', change to 'Cc'
							swap = true;
						}
						break;
					case 'C':
						if (glide == 'n') { // 'Cn', change to 'An'
							name = name.replace('C', 'A').replace('c', 'n');
							swap = true;
						} // else we have 'Cc' and there's nothing to do
						break;
					}
				} else if (hasGlide) {
					switch (glide) {
					case 'n':
						name = name.replace('c', 'n');
						break;
					case 'a':
						swap = true;
						break;
					}
				} else if (hasCentering) {
					switch (centering) {
					case 'I':
						name = name.replace('C', 'I');
						break;
					case 'A':
						swap = true;
						break;
					}
				}

				if (swap) {
					final Vector tmp = to[0];
					to[0] = to[2];
					to[2] = tmp;
					to[1] = (Vector) to[1].negative();
				}
				
				// --- make sure we have a right-handed set of vectors
				if (Vector.volume3D(to[0], to[1], to[2]).isNegative()) {
					to[1] = (Vector) to[1].negative();
				}
			}

			final CoordinateChange F = new CoordinateChange(Vector
					.toMatrix(from));
			final CoordinateChange T = new CoordinateChange(Vector.toMatrix(to));
			this.coordinateChange = (CoordinateChange) F.inverse().times(T);
		}
		this.groupName = name;
	}

	public CoordinateChange getCoordinateChange() {
		return this.coordinateChange;
	}

	public String getGroupName() {
		return this.groupName;
	}
}

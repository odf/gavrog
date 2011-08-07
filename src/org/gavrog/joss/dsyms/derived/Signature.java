/*
   Copyright 2007 Olaf Delgado-Friedrichs

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


package org.gavrog.joss.dsyms.derived;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.HashMapWithDefault;
import org.gavrog.box.collections.NiftyList;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;

/**
 * @author Olaf Delgado
 * @version $Id: Signature.java,v 1.3 2007/07/27 02:24:13 odf Exp $
 */
public class Signature {
	private static int gcd(int a, int b) {
		a = Math.abs(a);
		b = Math.abs(b);
		int t;
		if (a < b) {
			t = b;
			b = a;
			a = t;
		}
		while (b > 0) {
			t = b;
			b = a % b;
			a = t;
		}
		return a;
	}
	
	private static List faceSizes(final DelaneySymbol ds) {
		final List idcs = new IndexList(0, 1);
		final List sizes = new LinkedList();
		for (final Iterator reps = ds.orbitReps(idcs); reps.hasNext();) {
			sizes.add(new Integer(ds.m(0, 1, reps.next())));
		}
		Collections.sort(sizes);
		return new NiftyList(sizes);
	}
	
	private static String faceSizeToTileSig(final List sig) {
		final int max = ((Integer) sig.get(sig.size() - 1)).intValue() + 1;
		final int mult[] = new int[max];
		for (final Iterator iter = sig.iterator(); iter.hasNext();) {
			++mult[((Integer) iter.next()).intValue()];
		}
		
		final StringBuffer buf = new StringBuffer(100);
		boolean first = true;
		for (int i = 0; i < max; ++i) {
			final int k = mult[i];
			if (k > 0) {
				if (first) {
					first = false;
				} else {
					buf.append('.');
				}
				buf.append(i);
				if (k > 1) {
					buf.append('^');
					buf.append(k);
				}
			}
		}
		
		return buf.toString();
	}

	public static String ofTiling(final DelaneySymbol base) {
		if (base.dim() == 3) {
			return ofTiling3d(base);
		} else if (base.dim() == 2) {
			if (base.isSpherical2D()) {
				return ofTiling2dSpherical(base);
			} else if (base.curvature2D().isZero()){
				return ofTiling2dEuclidean(base);
			}
		}
		throw new UnsupportedOperationException("unsupported kind of tiling");
	}
	
	private static String ofTiling2dSpherical(final DelaneySymbol base) {
		assert base.dim() == 2 : "must be two-dimensional";
		final DSymbol ds = Covers.finiteUniversalCover(new DSymbol(base));
		return faceSizeToTileSig(faceSizes(ds));
	}
	
	private static String ofTiling2dEuclidean(final DelaneySymbol base) {
		assert base.dim() == 2 : "must be two-dimensional";
		final DSymbol ds = Covers.toroidalCover2D(new DSymbol(base));
		return faceSizeToTileSig(faceSizes(ds));
	}
	
	private static String ofTiling3d(final DelaneySymbol base) {
		final DSymbol ds = Covers.pseudoToroidalCover3D(base);
		final Map countSigs = new HashMapWithDefault() {
			public Object makeDefault() {
				return new Integer(0);
			}
		};
		final List idcs = new IndexList(0, 1, 2);
		for (final Iterator reps = ds.orbitReps(idcs); reps.hasNext();) {
			final List s = new NiftyList(faceSizes(new Subsymbol(ds, idcs,
					reps.next())));
			final int n = ((Integer) countSigs.get(s)).intValue();
			countSigs.put(s, new Integer(n + 1));
		}
		final List keys[] = new List[countSigs.keySet().size()];
		countSigs.keySet().toArray(keys);
		Arrays.sort(keys);
		int t = 0;
		for (int i = 0; i < keys.length; ++i) {
			t = gcd(t, ((Integer) countSigs.get(keys[i])).intValue());
		}
		final StringBuffer buf = new StringBuffer(100);
		for (int i = 0; i < keys.length; ++i) {
			final List s = keys[i];
			final int k = ((Integer) countSigs.get(s)).intValue() / t;
			if (i > 0) {
				buf.append(" + ");
			}
			if (k > 1) {
				buf.append(k);
			}
			buf.append('[');
			buf.append(faceSizeToTileSig(s));
			buf.append(']');
		}
		
		return buf.toString();
	}
}

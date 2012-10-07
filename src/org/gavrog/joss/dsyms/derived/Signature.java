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


package org.gavrog.joss.dsyms.derived;

import java.util.ArrayList;
import java.util.Collections;
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
	
	private static <T> List<Integer> faceSizes(final DelaneySymbol<T> ds) {
		final List<Integer> idcs = new IndexList(0, 1);
		final List<Integer> sizes = new LinkedList<Integer>();
		for (final T D: ds.orbitReps(idcs)) {
			sizes.add(new Integer(ds.m(0, 1, D)));
		}
		Collections.sort(sizes);
		return new NiftyList<Integer>(sizes);
	}
	
	private static String faceSizeToTileSig(final List<Integer> sig) {
		final int max = sig.get(sig.size() - 1) + 1;
		final int mult[] = new int[max];
		for (final int m: sig) {
			++mult[m];
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

	public static <T> String ofTiling(final DelaneySymbol<T> base) {
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
	
	private static <T> String ofTiling2dSpherical(final DelaneySymbol<T> base) {
		assert base.dim() == 2 : "must be two-dimensional";
		final DSymbol ds = Covers.finiteUniversalCover(new DSymbol(base));
		return faceSizeToTileSig(faceSizes(ds));
	}
	
	private static <T> String ofTiling2dEuclidean(final DelaneySymbol<T> base) {
		assert base.dim() == 2 : "must be two-dimensional";
		final DSymbol ds = Covers.toroidalCover2D(new DSymbol(base));
		return faceSizeToTileSig(faceSizes(ds));
	}
	
	private static <T> String ofTiling3d(final DelaneySymbol<T> base) {
		final DSymbol ds = Covers.pseudoToroidalCover3D(base);
		final Map<List<Integer>, Integer> countSigs =
				new HashMapWithDefault<List<Integer>, Integer>() {
			private static final long serialVersionUID = -7426228956696237260L;
			public Integer makeDefault() {
				return 0;
			}
		};
		final List<Integer> idcs = new IndexList(0, 1, 2);
		for (final int D: ds.orbitReps(idcs)) {
			final List<Integer> s = new NiftyList<Integer>(
					faceSizes(new Subsymbol<Integer>(ds, idcs, D)));
			final int n = countSigs.get(s);
			countSigs.put(s, new Integer(n + 1));
		}
		final List<List<Integer>> keys =
				new ArrayList<List<Integer>>(countSigs.keySet());
		Collections.sort(keys, NiftyList.<Integer>lexicographicComparator());
		int t = 0;
		for (final List<Integer> key: keys) {
			t = gcd(t, countSigs.get(key));
		}
		final StringBuffer buf = new StringBuffer(100);
		for (int i = 0; i < keys.size(); ++i) {
			final List<Integer> s = keys.get(i);
			final int k = countSigs.get(s) / t;
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

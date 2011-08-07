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


package org.gavrog.joss.graphics;

/**
 * @author Olaf Delgado
 * @version $Id: Vec.java,v 1.1 2007/05/19 06:39:44 odf Exp $
 */
public class Vec {
	public static double[] copy(double[] dst, double[] src) {
		if (dst == null) {
			dst = new double[src.length];
		}
		final int n = Math.min(src.length, dst.length);
		for (int i = 0; i < n; ++i) {
			dst[i] = src[i];
		}
		return dst;
	}
	
	public static double[] plus(double[] dst, double[] a, double[] b) {
		if (dst == null) {
			dst = new double[a.length];
		}
		final int n = Math.min(dst.length, Math.min(a.length, b.length));
		for (int i = 0; i < n; ++i) {
			dst[i] = a[i] + b[i];
		}
		return dst;
	}
	
	public static double[] minus(double[] dst, double[] a, double[] b) {
		if (dst == null) {
			dst = new double[a.length];
		}
		final int n = Math.min(dst.length, Math.min(a.length, b.length));
		for (int i = 0; i < n; ++i) {
			dst[i] = a[i] - b[i];
		}
		return dst;
	}

	public static double[] times(double[] dst, double f, double[] src) {
		if (dst == null) {
			dst = new double[src.length];
		}
		final int n = Math.min(src.length, dst.length);
		for (int i = 0; i < n; ++i) {
			dst[i] = f * src[i];
		}
		return dst;
	}

	public static double[] linearCombination(double[] dst, double fa,
			double[] a, double fb, double[] b) {
		if (dst == null) {
			dst = new double[a.length];
		}
		final int n = Math.min(dst.length, Math.min(a.length, b.length));
		for (int i = 0; i < n; ++i) {
			dst[i] = fa * a[i] + fb * b[i];
		}
		return dst;
	}

	public static double innerProduct(double[] a, double[] b) {
		if (a.length != b.length) {
			throw new IllegalArgumentException("vectors must have same length");
		}
		final int n = a.length;
		double result = 0.0;
		for (int i = 0; i < n; ++i) {
			result += a[i] * b[i];
		}
		return result;
	}

	public static double[] crossProduct(double[] dst, double[] a, double[] b) {
		if (a.length < 3 || b.length < 3 || dst != null && dst.length < 3) {
			throw new IllegalArgumentException("vectors too short");
		}
		if (dst == null) {
			dst = new double[3];
		}
		dst[0] = a[1] * b[2] - a[2] * b[1];
		dst[1] = a[2] * b[0] - a[0] * b[2];
		dst[2] = a[0] * b[1] - a[1] * b[0];
		return dst;
	}
	
	public static double norm(double[] a) {
		return Math.sqrt(innerProduct(a, a));
	}
	
	public static double[] normalized(double dst[], double[] a) {
		final double f = norm(a);
		if (f == 0.0) {
			return copy(dst, a);
		} else {
			return times(dst, 1.0 / f, a);
		}
	}

	public static double[] projection(double[] dst, double[] src,
			double[] fixed) {
		final double[] n = normalized(null, fixed);
		return times(dst, innerProduct(src, n), n);
	}
	
	public static double[] complementProjection(double[] dst, double[] src,
			double[] fixed) {
		return minus(dst, src, projection(null, src, fixed));
	}
	
	public static double angle(double[] a, double[] b) {
		final double aabb = innerProduct(a, a)* innerProduct(b, b);
		if (aabb == 0) {
			return 0;
		} else {
			final double f = innerProduct(a, b) / Math.sqrt(Math.abs(aabb));
			return Math.acos(Math.min(1.0, Math.max(-1.0, f)));
		}
	}
}

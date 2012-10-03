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

package org.gavrog.box.collections;

import java.util.Comparator;

/**
 * A class for hashable pairs of objects.
 */
public class Pair<F, S> {
	private F first;
	private S second;
	
	public Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}
	
	public F getFirst() {
		return first;
	}
	
	public S getSecond() {
		return second;
	}
	
	private boolean equal(final Object a, final Object b) {
	    return ((a == null) ? (b == null) : a.equals(b));
	}
	
	public boolean equals(Pair<?, ?> other) {
	    return equal(first, other.first) && equal(second, other.second);
	}
	
    public boolean equals(Object other) {
        if (other instanceof Pair<?, ?>)
            return equals((Pair<?, ?>) other);
        else
            return false;
    }
    
	private <T> int hash(final T x) {
	    return x == null ? 0 : x.hashCode();
	}
	
	public int hashCode() {
	    return hash(first) * 37 + hash(second);
	}
	
	public String toString() {
		return "("  + first + ", " + second + ")";
	}
	
	public static <F extends Comparable<? super F>,
	               S extends Comparable<? super S>>
	Comparator<Pair<F, S>> defaultComparator()
	{
        return new Comparator<Pair<F,S>>() {
            public int compare(final Pair<F, S> p1, final Pair<F, S> p2) {
                final int d = p1.getFirst().compareTo(p2.getFirst());
                if (d == 0)
                    return p1.getSecond().compareTo(p2.getSecond());
                else
                    return d;
            }
        };
	}
    
    public static <F extends Comparable<? super F>>
    Comparator<Pair<F, ?>> firstItemComparator()
    {
        return new Comparator<Pair<F, ?>>() {
            public int compare(final Pair<F, ?> p1, final Pair<F, ?> p2) {
                return p1.getFirst().compareTo(p2.getFirst());
            }
        };
    }
}

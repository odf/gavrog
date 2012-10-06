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

package org.gavrog.joss.dsyms.basic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Partition;


/**
 * A partition of a Delaney symbol's elements into equivalence classes of
 * constant "type".
 * 
 * The type of a Delaney symbol element is a map which assigns to each word in
 * the free Coxeter group associated to the index set of the symbol the
 * collection of m-values attained by the element which is reached by applying
 * that word to the original element.
 * 
 * In other words, two elements are of the same type if they map to a common
 * element in the "universal" or "minimal" image of the symbol. The universal
 * image is the smallest symbol to which there is a Delaney morphism (i.e., a
 * map which respects both indexed neighbor operations and m-values) from the
 * original symbol.
 */
public class TypedPartition<T> {
	private DelaneySymbol<T> ds;
	private List<Integer> indices;
	private Partition<T> P = new Partition<T>();
	private HashMap<T, int[]> M = new HashMap<T, int[]>();
	
	public TypedPartition(DelaneySymbol<T> ds) {
		this.ds = ds;
		this.indices = new IndexList(ds);
	}
	
	private int[] preliminaryType(final T D) {
		if (!M.containsKey(D)) {
			int tmp[] = new int[ds.dim()];
			M.put(D, tmp);
			for (int k = 0; k < ds.dim(); ++k) {
				tmp[k] = -1;
			}
		}
		return (int[]) M.get(D);
	}
	
	private int[] type(T D) {
		int result[] = preliminaryType(D);
		
		for (int k = 0; k < ds.dim(); ++k) {
			if (result[k] < 0) {
				int i = indices.get(k);
				int j = indices.get(k + 1);
				int m = ds.m(i, j, D);
				result[k] = m;
				for (final T E: ds.orbit(new IndexList(i, j), D)) {
					preliminaryType(E)[k] = m;
				}
			}
		}
		
		return result;
	}
	
	public boolean haveEqualTypes(T D, T E) {
		int typeD[] = type(D);
		int typeE[] = type(E);
		
		for (int i = 0; i < typeD.length; ++i) {
			if (typeD[i] != typeE[i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean unite(T D, T E) {
		D = P.find(D);
		E = P.find(E);

		if (D == E) {
			return true;
		} else if (! haveEqualTypes(D, E)) {
			return false;
		} else {
			Partition<T> newP = (Partition<T>) P.clone();
			LinkedList<T> stack = new LinkedList<T>();
			newP.unite(D, E);
			stack.addLast(D);
			stack.addLast(E);
			
			while (stack.size() > 0) {
				E = stack.removeLast();
				D = stack.removeLast();
				
				for (final int i: ds.indices()) {
					T Di = newP.find(ds.op(i, D));
					T Ei = newP.find(ds.op(i, E));
					if (Di == Ei) {
						continue;
					} else if (! haveEqualTypes(Di, Ei)) {
						return false;
					} else {
						newP.unite(Di, Ei);
						stack.addLast(Di);
						stack.addLast(Ei);
					}
				}
			}
			P = newP;
			
			return true;
		}
	}
	
	public void uniteAll() {
		Iterator<T> elms = ds.elements();
		T D0 = null;
		while (elms.hasNext()) {
			T D = elms.next();
			if (D0 == null) {
				D0 = D;
			} else {
				unite(D0, D);
			}
		}
	}
	
	public T find(T D) {
		return P.find(D);
	}
	
	public boolean areEquivalent(T D, T E) {
		return P.areEquivalent(D, E);
	}
	
	public Map<T, T> representativeMap() {
		return P.representativeMap();
	}
}

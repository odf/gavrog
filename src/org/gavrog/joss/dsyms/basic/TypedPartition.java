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
 * 
 * @author Olaf Delgado
 * @version $Id: TypedPartition.java,v 1.2 2005/07/18 23:32:57 odf Exp $
 */
public class TypedPartition {
	private DelaneySymbol ds;
	private List indices;
	private Partition P = new Partition();
	private HashMap M = new HashMap();
	
	public TypedPartition(DelaneySymbol ds) {
		this.ds = ds;
		this.indices = new IndexList(ds);
	}
	
	private int[] preliminaryType(Object D) {
		if (!M.containsKey(D)) {
			int tmp[] = new int[ds.dim()];
			M.put(D, tmp);
			for (int k = 0; k < ds.dim(); ++k) {
				tmp[k] = -1;
			}
		}
		return (int[]) M.get(D);
	}
	
	private int[] type(Object D) {
		int result[] = preliminaryType(D);
		
		for (int k = 0; k < ds.dim(); ++k) {
			if (result[k] < 0) {
				int i = ((Integer) indices.get(k)).intValue();
				int j = ((Integer) indices.get(k + 1)).intValue();
				int m = ds.m(i, j, D);
				result[k] = m;
				Iterator orb = ds.orbit(new IndexList(i, j), D);
				while (orb.hasNext()) {
					Object E = orb.next();
					preliminaryType(E)[k] = m;
				}
			}
		}
		
		return result;
	}
	
	public boolean haveEqualTypes(Object D, Object E) {
		int typeD[] = type(D);
		int typeE[] = type(E);
		
		for (int i = 0; i < typeD.length; ++i) {
			if (typeD[i] != typeE[i]) {
				return false;
			}
		}
		return true;
	}
	
	public boolean unite(Object D, Object E) {
		D = P.find(D);
		E = P.find(E);

		if (D == E) {
			return true;
		} else if (! haveEqualTypes(D, E)) {
			return false;
		} else {
			Partition newP = (Partition) P.clone();
			LinkedList stack = new LinkedList();
			newP.unite(D, E);
			stack.addLast(D);
			stack.addLast(E);
			
			while (stack.size() > 0) {
				E = stack.removeLast();
				D = stack.removeLast();
				
				Iterator idcs = ds.indices();
				while (idcs.hasNext()) {
					int i = ((Integer) idcs.next()).intValue();
					Object Di = newP.find(ds.op(i, D));
					Object Ei = newP.find(ds.op(i, E));
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
		Iterator elms = ds.elements();
		Object D0 = null;
		while (elms.hasNext()) {
			Object D = elms.next();
			if (D0 == null) {
				D0 = D;
			} else {
				unite(D0, D);
			}
		}
	}
	
	public Object find(Object D) {
		return P.find(D);
	}
	
	public boolean areEquivalent(Object D, Object E) {
		return P.areEquivalent(D, E);
	}
	
	public Map representativeMap() {
		return P.representativeMap();
	}
}

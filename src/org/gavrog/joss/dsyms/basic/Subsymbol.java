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

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;


/**
 * Represents subsymbols of Delaney symbols.
 * @author Olaf Delgado
 * @version $Id: Subsymbol.java,v 1.4 2007/04/22 06:31:43 odf Exp $
 */
public class Subsymbol<T> extends DelaneySymbol<T> {
	private DelaneySymbol ds;
	private TreeSet indices;
	private TreeSet elements;
	private int dim;
	private int size;
	
	public Subsymbol(DelaneySymbol ds, List indices, Object seed) {
		this.ds = ds;
		this.indices = new TreeSet();
		this.indices.addAll(indices);
		this.dim = this.indices.size() - 1;
		this.elements = new TreeSet();
		Iterator elms = new Traversal(ds, indices, Iterators.singleton(seed));
		while (elms.hasNext()) {
			this.elements.add(((DSPair) elms.next()).getElement());
		}
		this.size = this.elements.size();
	}
	
	public int dim() {
		return this.dim;
	}

	public int size() {
		return this.size;
	}

	public IteratorAdapter<T> elements() {
		return elements.iterator();
	}

	public boolean hasElement(Object D) {
		return elements.contains(D);
	}

	public IteratorAdapter<Integer> indices() {
		return indices.iterator();
	}

	public boolean hasIndex(int i) {
		return indices.contains(new Integer(i));
	}

	public boolean definesOp(int i, Object D) {
		return ds.definesOp(i, D);
	}
	
	public Object op(int i, Object D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        return ds.op(i, D);
	}

	public boolean definesV(int i, int j, Object D) {
		return ds.definesV(i, j, D);
	}
	
	public int v(int i, int j, Object D) {
        if (!hasElement(D)) {
            throw new IllegalArgumentException("not an element: " + D);
        }
        if (!hasIndex(i)) {
            throw new IllegalArgumentException("invalid index: " + i);
        }
        if (!hasIndex(j)) {
            throw new IllegalArgumentException("invalid index: " + j);
        }
        return normalizedV(ds.v(i, j, D));
	}
	
	public String toString() {
	    StringBuffer buf = new StringBuffer(100);
	    buf.append("Subsymbol(");
	    buf.append(ds);
	    buf.append(", (");
	    Iterator iter = indices();
	    boolean first = true;
	    while (iter.hasNext()) {
	        if (first) {
	            first = false;
	        } else {
	            buf.append(", ");
	        }
	        buf.append(iter.next());
	    }
	    buf.append("), ");
	    buf.append(elements.first());
	    buf.append(")");
	    return buf.toString();
	}
}

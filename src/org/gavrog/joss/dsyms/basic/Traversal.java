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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;


/**
 * Implements a traversal through a collection of connected components of a
 * Delaney symbol. By default, every element (node) is visited exactly once.
 * There is also an option to have every edge visited exactly once.
 */
public class Traversal<T> extends IteratorAdapter<DSPair<T>> {

    private DelaneySymbol<T> ds;
    private List<Integer> indices;
    private Iterator<T> seeds;
    private boolean visitAllEdges;

    private List<LinkedList<T>> buffer;
    private HashMap<T, Integer> elm2num;
    private int nextNum;

    /**
     * Initializes a new partial traversal.
     * @param ds the Delaney symbol to traverse.
     * @param indices follow only edges with these indices.
     * @param seeds the starting points for the traversal.
     * @param allEdges if true, traversal visits all edges.
     */
    public Traversal(
    		final DelaneySymbol<T> ds,
    		final List<Integer> indices,
    		final Iterator<T> seeds,
            final boolean allEdges)
    {
        this.ds = ds;
        this.indices = indices;
        this.seeds = seeds;
        this.visitAllEdges = allEdges;

        this.buffer = new ArrayList<LinkedList<T>>();
        for (int i = 0; i < indices.size(); ++i) {
            buffer.add(new LinkedList<T>());
        }
        this.elm2num = new HashMap<T, Integer>();
        this.nextNum = 0;
    }

    /**
     * Initializes a new partial traversal that visits each element (node)
     * exactly once.
     * @param ds the Delaney symbol to traverse.
     * @param indices follow only edges with these indices.
     * @param seeds the starting points for the traversal.
     */
    public Traversal(
    		final DelaneySymbol<T> ds,
    		final List<Integer> indices,
    		final Iterator<T> seeds)
    {
        this(ds, indices, seeds, false);
    }
    
    /**
     * Initializes a new partial traversal that visits each element (node)
     * exactly once.
     * @param ds the Delaney symbol to traverse.
     * @param indices follow only edges with these indices.
     * @param seed the single starting point for the traversal.
     */
    public Traversal(
    		final DelaneySymbol<T> ds,
    		final List<Integer> indices,
    		final T seed)
    {
        this(ds, indices, Iterators.singleton(seed), false);
    }
    
    /**
     * Initializes a new partial traversal.
     * @param ds the Delaney symbol to traverse.
     * @param indices follow only edges with these indices.
     * @param seed the single starting point for the traversal.
     * @param allEdges if true, traversal visits all edges.
     */
    public Traversal(
    		final DelaneySymbol<T> ds,
    		final List<Integer> indices,
    		final T seed,
            final boolean allEdges)
    {
        this(ds, indices, Iterators.singleton(seed), allEdges);
    }

    /**
     * Initializes a new complete traversal.
     * @param ds the Delaney symbol to traverse.
     * @param allEdges if true, traversal visits all edges.
     */
    public Traversal(final DelaneySymbol<T> ds, final boolean allEdges) {
        this(ds, new IndexList(ds), ds.elements(), allEdges);
    }
    
    /**
     * Initializes a new complete traversal that visits each element (node)
     * exactly once.
     * @param ds the Delaney symbol to traverse.
     */
    public Traversal(final DelaneySymbol<T> ds) {
        this(ds, new IndexList(ds), ds.elements(), false);
    }
    
    /**
     * This methods finds the next edge of the traversal.
     */
    protected DSPair<T> findNext() {
		for (int k = 0; k < indices.size(); ++k) {
			while (buffer.get(k).size() > 0) {
				final int i = indices.get(k);
				final T D;
				if (k < 2) {
					D = buffer.get(k).removeLast();
				} else {
					D = buffer.get(k).removeFirst();
				}
				if (ds.hasElement(D)) {
					if (!elm2num.containsKey(D)) {
						elm2num.put(D, nextNum++);

						for (int m = 0; m < indices.size(); ++m) {
							int j = indices.get(m);
							if (j != i) {
								buffer.get(m).addLast(ds.op(j, D));
							}
						}
						return new DSPair<T>(i, D);
					} else if (visitAllEdges) {
						int E = elm2num.get(D);
						int Ei = elm2num.get(ds.op(i, D));
						if (Ei <= E) {
							return new DSPair<T>(i, D);
						}
					}
				}
			}
		}

		while (seeds.hasNext()) {
			T D = seeds.next();
			if (D != null && !elm2num.containsKey(D)) {
				elm2num.put(D, nextNum++);
				for (int k = 0; k < indices.size(); ++k) {
					int i = indices.get(k);
					buffer.get(k).addLast(ds.op(i, D));
				}
				return new DSPair<T>(-1, D);
			}
		}
		throw new NoSuchElementException("at end");
	}
}

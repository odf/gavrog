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

package org.gavrog.jane.fpgroups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;


/**
 * Represents and computes a stabilizer subgroup in a finite group action.
 */
public class Stabilizer<E, D> {
    // --- set to true to enable logging
    final private static boolean LOGGING = false;

    // --- the input data
	final private GroupAction<E, D> action;
	final private D basepoint;
	final private int maxLabelLength;

	// --- convenience fields
	final private FpGroup<E> group;

	// --- temporary data
	private Map<FreeWord<E>, Set<FreeWord<E>>> relatorsByStartGen;

	// --- results of the stabilizer computation
	private List<FreeWord<E>> generators = null;
	private FpGroup<String> presentation = null;
	private Map<Pair<D, FreeWord<E>>, FreeWord<String>> edgeLabelling = null;

	/**
	 * Constructs a stabilizer instance.
	 * 
	 * @param action the underlying action.
	 * @param basepoint the point to be stabilize.
	 */
	public Stabilizer(final GroupAction<E, D> action, final D basepoint) {
	    this(action, basepoint, 10);
	}
	
	/**
     * Constructs a stabilizer instance. The parameter
     * <code>maxLabelLength</code> is used to adjust the characteristics of the
     * group presentation produced. A smaller value tends to produce more
     * generators but shorter relations, while a larger value produces fewer
     * generators but longer relations.
     * 
     * @param action the underlying action.
     * @param basepoint the point to be stabilize.
     * @param maxLabelLength the maximal word size on a Cayley graph edge.
     */
	public Stabilizer(final GroupAction<E, D> action, final D basepoint,
            final int maxLabelLength) {
	    
		// --- check the arguments
		if (action == null) {
			throw new NullPointerException("action cannot be null");
		}
		if (!action.isDefinedOn(basepoint)) {
			throw new IllegalArgumentException("illegal basepoint for action");
		}

		// --- copy the parameters
		this.action = action;
		this.basepoint = basepoint;
		this.maxLabelLength = maxLabelLength;
		this.group = action.getGroup();
	}

	/**
	 * Computes all the derived data.
	 */
	private void compute() {
		// --- see if computation has already been done
		if (this.generators != null) {
			return;
		}
		if (LOGGING) {
			System.out.println("Starting compute()...");
		}
		
		// --- collect group generators and their inverses
		final List<FreeWord<E>> groupGens = new LinkedList<FreeWord<E>>();
		for (final FreeWord<E> g: this.group.getGenerators()) {
			groupGens.add(g);
			groupGens.add(g.inverse());
		}

		// --- prepare collections of cyclically permuted relators
		preprocessRelators();

		// --- initialize
		this.generators = new ArrayList<FreeWord<E>>();
		this.edgeLabelling =
				new HashMap<Pair<D, FreeWord<E>>, FreeWord<String>>();

		final Alphabet<String> A = new PrefixAlphabet("s_");
		final FreeWord<String> id = FreeWord.parsedWord(A, "*");
		final Map<D, FreeWord<E>> point2word = new HashMap<D, FreeWord<E>>();
		final LinkedList<D> queue = new LinkedList<D>();
		queue.addLast(this.basepoint);
		point2word.put(this.basepoint, this.group.getIdentity());

		// --- construct and close relations on a spanning tree
		while (queue.size() > 0) {
			final D x = queue.removeFirst();
			final FreeWord<E> w = point2word.get(x);
			for (final FreeWord<E> g: groupGens) {
				final D y = this.action.apply(x, g);
				if (!point2word.containsKey(y)) {
					this.edgeLabelling.put(new Pair<D, FreeWord<E>>(x, g), id);
					this.edgeLabelling.put(
							new Pair<D, FreeWord<E>>(y, g.inverse()), id);
					queue.addLast(y);
					point2word.put(y, w.times(g));
					if (LOGGING) {
						System.out.println("  added " + y + " to tree");
					}
					closeRelations(y, g.inverse());
				}
			}
		}

		// --- find generators for the stabilizer subgroup
		for (Iterator<D> points = this.action.domain(); points.hasNext();) {
			final D x = points.next();
			final FreeWord<E> wx = point2word.get(x);
			for (final FreeWord<E> g: groupGens) {
				final Pair<D, FreeWord<E>> edge =
						new Pair<D, FreeWord<E>>(x, g);
				if (!this.edgeLabelling.containsKey(edge)) {
					final D y = this.action.apply(x, g);
					final FreeWord<E> wy = point2word.get(y);
					final Pair<D, FreeWord<E>> invEdge =
							new Pair<D, FreeWord<E>>(y, g.inverse());
					final int n = this.generators.size() + 1;
					final FreeWord<String> s = new FreeWord<String>(A, n);
					this.edgeLabelling.put(edge, s);
					this.edgeLabelling.put(invEdge, s.inverse());
					final FreeWord<E> newgen = wx.times(g).times(wy.inverse());
					this.generators.add(newgen);
					if (LOGGING) {
						System.out.println("  New generator " + newgen);
					}
					closeRelations(x, g);
					closeRelations(y, g.inverse());
				}
			}
		}

		// --- convert edge labels to a finite alphabet
		final int nrGens = this.generators.size();
		final FiniteAlphabet<String> B = FiniteAlphabet.fromPrefix("s_", nrGens);
		final Set<Pair<D, FreeWord<E>>> edges = this.edgeLabelling.keySet();
		for (final Pair<D, FreeWord<E>> edge: edges) {
			final FreeWord<String> w = this.edgeLabelling.get(edge);
			this.edgeLabelling.put(edge, new FreeWord<String>(B, w));
		}

		// --- collect relators for subgroup
		final List<FreeWord<E>> oldRelators = this.group.getRelators();
		final List<FreeWord<String>> newRelators =
				new LinkedList<FreeWord<String>>();
		for (Iterator<D> points = this.action.domain(); points.hasNext();) {
			final D x = points.next();
			for (final FreeWord<E> r: oldRelators)
				newRelators.add(traceWord(x, r));
		}
		
		// --- write the presentation
		this.presentation = new FpGroup<String>(B, newRelators);
	}

	/**
	 * Cyclically permute the relators of the original group and their inverses
	 * and sort the results into bins determined by their start generators.
	 */
	private void preprocessRelators() {
		this.relatorsByStartGen = new HashMap<FreeWord<E>, Set<FreeWord<E>>>();
		final List<FreeWord<E>> relators = this.group.getRelators();
		for (FreeWord<E> rel: relators) {
			for (int exp = -1; exp <= 1; exp += 2) {
				final FreeWord<E> w = rel.raisedTo(exp);
				final int n = w.length();
				for (int i = 0; i < n; ++i) {
					final FreeWord<E> v = w.subword(i, n).times(w.subword(0, i));
					final FreeWord<E> g = v.subword(0, 1);
					if (!this.relatorsByStartGen.containsKey(g)) {
						this.relatorsByStartGen.put(g,
								new HashSet<FreeWord<E>>());
					}
					this.relatorsByStartGen.get(g).add(v);
				}
			}
		}
	}

	/**
     * Recusively checks for and closes cycles in the action graph which
     * correspond to relations in the original group and which are completely
     * labelled except at one edge. Only cycles starting at the given edge are
     * considered. A cycle is not closed if the word to be assigned to the yet
     * unlabelled edge would be longer than {@link #maxLabelLength}.
     * 
     * @param start point at which to start looking for relation cycles.
     * @param startgen group generator to determine the start edge.
     */
	private void closeRelations(final D start, final FreeWord<E> startgen) {
		// --- initialize the edge queue with the given edge
		final LinkedList<Pair<D, FreeWord<E>>> queue =
				new LinkedList<Pair<D, FreeWord<E>>>();
		queue.addLast(new Pair<D, FreeWord<E>>(start, startgen));
		final Set<Pair<D, FreeWord<E>>> seen =
				new HashSet<Pair<D, FreeWord<E>>>();

		// --- close relations until the queue is empty
		while (queue.size() > 0) {
			final Pair<D, FreeWord<E>> edge = queue.removeFirst();
			if (seen.contains(edge)) {
				continue;
			}
            seen.add(edge);
			final D x = edge.getFirst();
			final FreeWord<E> g = edge.getSecond();

			// --- look at all relations starting with this generator
			final Set<FreeWord<E>> rels = this.relatorsByStartGen.get(g);
            if (rels == null) {
                continue;
            }
			for (final FreeWord<E> r: rels) {
				final int n = r.length();

				// --- trace relation r of length n starting at x
				D y = x;
				Pair<D, FreeWord<E>> cut = null;
				int cutIndex = 0;
				for (int i = 0; i < n; ++i) {
					// --- the next generator
					final FreeWord<E> h = r.subword(i, i+1);
					// --- the next edge
					final Pair<D, FreeWord<E>> next =
							new Pair<D, FreeWord<E>>(y, h);
					if (!this.edgeLabelling.containsKey(next)) {
						// --- found a cut
						if (cut != null) {
							// --- second cut: clear first and stop tracing
							cut = null;
							break;
						} else {
							// --- first cut: save it
							cut = next;
							cutIndex = i;
						}
					}
					// --- move to next vertex in graph
					y = this.action.apply(y, h);
				}
				
				// --- if a single cut was found, process it here
				if (cut != null) {
					// --- get point and generator of the cut
					y = cut.getFirst();
					final FreeWord<E> h = cut.getSecond();

					// --- construct the reverse of the cut edge
					final D z = this.action.apply(y, h);
					final Pair<D, FreeWord<E>> reverseEdge =
							new Pair<D, FreeWord<E>>(z, h.inverse());

					// --- trace what's assigned to the rest of the loop
					final FreeWord<E> r1 = r.subword(cutIndex, n);
					final FreeWord<E> r2 = r.subword(0, cutIndex);
					final FreeWord<E> w = r1.times(r2).inverse();
					final FreeWord<String> trace = traceWord(y, w);
					
					if (trace != null && trace.length() <= this.maxLabelLength) {
                        // --- label the cut edge and its reverse
                        this.edgeLabelling.put(cut, trace);
                        this.edgeLabelling.put(reverseEdge, trace.inverse());

                        // --- queue up the cut edge
                        queue.addLast(cut);
                        if (LOGGING) {
                            System.out.println("    closed relation " + w
                                               + " at " + cut + " yielding "
                                               + trace);
                        }
                    }
				}
			}
		}
	}

	/**
	 * Traces a word in the action graph and constructs the product of the words
	 * associated to the traversed edges.
	 * 
	 * @param x the starting point.
	 * @param r the word to trace
	 * @return the resulting word.
	 */
	private FreeWord<String> traceWord(D x, FreeWord<E> r) {
		FreeWord<String> res = null;
		D y = x;
		for (int i = 0; i < r.length(); ++i) {
			final FreeWord<E> g = r.subword(i, i+1);
			final Pair<D, FreeWord<E>> edge = new Pair<D, FreeWord<E>>(y, g);
			final FreeWord<String> w = this.edgeLabelling.get(edge);
			if (res == null) {
				res = w;
			} else if (w != null){
				res = res.times(w);
			}
			y = this.action.apply(y, g);
		}
		return res;
	}

	/**
	 * @return Returns the action.
	 */
	public GroupAction<E, D> getAction() {
		return action;
	}

	
	/**
	 * @return Returns the basepoint.
	 */
	public D getBasepoint() {
		return basepoint;
	}
	/**
	 * @return Returns the edge labelling.
	 */
	public Map<Pair<D, FreeWord<E>>, FreeWord<String>> getEdgeLabelling() {
		compute();
		return edgeLabelling;
	}

	/**
	 * @return Returns the generators.
	 */
	public List<FreeWord<E>> getGenerators() {
		compute();
		return generators;
	}

	/**
	 * @return Returns the presentation.
	 */
	public FpGroup<String> getPresentation() {
		compute();
		return presentation;
	}
}

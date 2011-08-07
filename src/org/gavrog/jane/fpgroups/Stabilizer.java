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
 * 
 * @author Olaf Delgado
 * @version $Id: Stabilizer.java,v 1.2 2005/07/18 23:33:29 odf Exp $
 */
public class Stabilizer {
    // --- set to true to enable logging
    final private static boolean LOGGING = false;

    // --- the input data
	final private GroupAction action;
	final private Object basepoint;
	final private int maxLabelLength;

	// --- convenience fields
	final private FpGroup group;

	// --- temporary data
	private Map relatorsByStartGen;

	// --- results of the stabilizer computation
	private List generators = null;
	private FpGroup presentation = null;
	private Map edgeLabelling = null;

	/**
	 * Constructs a stabilizer instance.
	 * 
	 * @param action the underlying action.
	 * @param basepoint the point to be stabilize.
	 */
	public Stabilizer(final GroupAction action, final Object basepoint) {
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
	public Stabilizer(final GroupAction action, final Object basepoint,
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
		final List groupGens = new LinkedList();
		for (final Iterator iter = this.group.getGenerators().iterator(); iter
				.hasNext();) {
			FreeWord g = (FreeWord) iter.next();
			groupGens.add(g);
			groupGens.add(g.inverse());
		}

		// --- prepare collections of cyclically permuted relators
		preprocessRelators();

		// --- initialize
		this.generators = new ArrayList();
		this.edgeLabelling = new HashMap();

		final Alphabet A = new PrefixAlphabet("s_");
		final FreeWord id = new FreeWord(A, "*");
		final Map point2word = new HashMap();
		final LinkedList queue = new LinkedList();
		queue.addLast(this.basepoint);
		point2word.put(this.basepoint, this.group.getIdentity());

		// --- construct and close relations on a spanning tree
		while (queue.size() > 0) {
			final Object x = queue.removeFirst();
			final FreeWord w = (FreeWord) point2word.get(x);
			for (final Iterator gens = groupGens.iterator(); gens.hasNext();) {
				final FreeWord g = (FreeWord) gens.next();
				final Object y = this.action.apply(x, g);
				if (!point2word.containsKey(y)) {
					this.edgeLabelling.put(new Pair(x, g), id);
					this.edgeLabelling.put(new Pair(y, g.inverse()), id);
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
		for (Iterator points = this.action.domain(); points.hasNext();) {
			final Object x = points.next();
			final FreeWord wx = (FreeWord) point2word.get(x);
			for (Iterator gens = groupGens.iterator(); gens.hasNext();) {
				final FreeWord g = (FreeWord) gens.next();
				final Pair edge = new Pair(x, g);
				if (!this.edgeLabelling.containsKey(edge)) {
					final Object y = this.action.apply(x, g);
					final FreeWord wy = (FreeWord) point2word.get(y);
					final Pair invEdge = new Pair(y, g.inverse());
					final int n = this.generators.size() + 1;
					final FreeWord s = new FreeWord(A, n);
					this.edgeLabelling.put(edge, s);
					this.edgeLabelling.put(invEdge, s.inverse());
					final FreeWord newgen = wx.times(g).times(wy.inverse());
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
		final FiniteAlphabet B = new FiniteAlphabet("s_", nrGens);
		final Set edges = this.edgeLabelling.keySet();
		for (Iterator iter = edges.iterator(); iter.hasNext();) {
			final Object edge = iter.next();
			final FreeWord w = (FreeWord) this.edgeLabelling.get(edge);
			this.edgeLabelling.put(edge, new FreeWord(B, w));
		}

		// --- collect relators for subgroup
		final List oldRelators = this.group.getRelators();
		final List newRelators = new LinkedList();
		for (Iterator points = this.action.domain(); points.hasNext();) {
			final Object x = points.next();
			for (Iterator rels = oldRelators.iterator(); rels.hasNext();) {
				final FreeWord r = (FreeWord) rels.next();
				newRelators.add(traceWord(x, r));
			}
		}
		
		// --- write the presentation
		this.presentation = new FpGroup(B, newRelators);
	}

	/**
	 * Cyclically permute the relators of the original group and their inverses
	 * and sort the results into bins determined by their start generators.
	 */
	private void preprocessRelators() {
		this.relatorsByStartGen = new HashMap();
		final List relators = this.group.getRelators();
		for (Iterator iter = relators.iterator(); iter.hasNext();) {
			final FreeWord rel = (FreeWord) iter.next();
			for (int exp = -1; exp <= 1; exp += 2) {
				final FreeWord w = rel.raisedTo(exp);
				final int n = w.length();
				for (int i = 0; i < n; ++i) {
					final FreeWord v = w.subword(i, n).times(w.subword(0, i));
					final FreeWord g = v.subword(0, 1);
					if (!this.relatorsByStartGen.containsKey(g)) {
						this.relatorsByStartGen.put(g, new HashSet());
					}
					((Set) this.relatorsByStartGen.get(g)).add(v);
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
	private void closeRelations(final Object start, final FreeWord startgen) {
		// --- initialize the edge queue with the given edge
		final LinkedList queue = new LinkedList();
		queue.addLast(new Pair(start, startgen));
		final Set seen = new HashSet();

		// --- close relations until the queue is empty
		while (queue.size() > 0) {
			final Pair edge = (Pair) queue.removeFirst();
			if (seen.contains(edge)) {
				continue;
			}
            seen.add(edge);
			final Object x = edge.getFirst();
			final FreeWord g = (FreeWord) edge.getSecond();

			// --- look at all relations starting with this generator
			final Set rels = (Set) this.relatorsByStartGen.get(g);
            if (rels == null) {
                continue;
            }
			for (Iterator iter = rels.iterator(); iter.hasNext();) {
				final FreeWord r = (FreeWord) iter.next();
				final int n = r.length();

				// --- trace relation r of length n starting at x
				Object y = x;
				Pair cut = null;
				int cutIndex = 0;
				for (int i = 0; i < n; ++i) {
					// --- the next generator
					final FreeWord h = r.subword(i, i+1);
					// --- the next edge
					final Pair next = new Pair(y, h);
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
					final FreeWord h = (FreeWord) cut.getSecond();

					// --- construct the reverse of the cut edge
					final Object z = this.action.apply(y, h);
					final Pair reverseEdge = new Pair(z, h.inverse());

					// --- trace what's assigned to the rest of the loop
					final FreeWord r1 = r.subword(cutIndex, n);
					final FreeWord r2 = r.subword(0, cutIndex);
					final FreeWord w = r1.times(r2).inverse();
					final FreeWord trace = traceWord(y, w);
					
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
	private FreeWord traceWord(Object x, FreeWord r) {
		FreeWord res = null;
		Object y = x;
		for (int i = 0; i < r.length(); ++i) {
			final FreeWord g = r.subword(i, i+1);
			final Pair edge = new Pair(y, g);
			final FreeWord w = (FreeWord) this.edgeLabelling.get(edge);
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
	public GroupAction getAction() {
		return action;
	}

	
	/**
	 * @return Returns the basepoint.
	 */
	public Object getBasepoint() {
		return basepoint;
	}
	/**
	 * @return Returns the edge labelling.
	 */
	public Map getEdgeLabelling() {
		compute();
		return edgeLabelling;
	}

	/**
	 * @return Returns the generators.
	 */
	public List getGenerators() {
		compute();
		return generators;
	}

	/**
	 * @return Returns the presentation.
	 */
	public FpGroup getPresentation() {
		compute();
		return presentation;
	}
}

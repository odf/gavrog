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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.fpgroups.Alphabet;
import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.PrefixAlphabet;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Boundary.Face;


/**
 * The fundamental group of a {@link org.gavrog.joss.dsyms.basic.DelaneySymbol}.
 */
public class FundamentalGroup<T> {

    final private DelaneySymbol<T> symbol;

    final private Map<DSPair<T>, FreeWord<String>> edgeToWord;

    final private Map<FreeWord<String>, DSPair<T>> generatorToEdge;

    final private Set<Pair<FreeWord<String>, Integer>> axes;
    
    final private FpGroup<String> presentation;

    /**
     * Constructs a FundementalGroup instance for a given symbol.
     * 
     * @param ds the symbol.
     */
    public FundamentalGroup(final DelaneySymbol<T> ds) {
        // --- initialize
        final Boundary<T> boundary = new Boundary<T>(ds);
        final Alphabet<String> A = new PrefixAlphabet("g_");
        final FreeWord<String> IdWord = new FreeWord<String>(A);
        
        this.symbol = ds;
        this.edgeToWord = new HashMap<DSPair<T>, FreeWord<String>>();
        this.generatorToEdge = new HashMap<FreeWord<String>, DSPair<T>>();

        // --- glue fundamental ("inner") edges
        for (final DSPair<T> e: new FundamentalEdges<T>(ds)) {
            boundary.glue(e);
            edgeToWord.put(e, IdWord);
        }

        // --- find generators for the fundamental group
        int nrGens = 0;
        for (final T D0: ds.elements()) {
            for (final int i0: ds.indices()) {
                if (boundary.isOnBoundary(i0, D0)) {
                    final DSPair<T> e0 = new DSPair<T>(i0, D0);
                    final LinkedList<Face<T>> Q = new LinkedList<Face<T>>();
                    final FreeWord<String> gen =
                    		new FreeWord<String>(A, ++nrGens);
                    boundary.glueAndEnqueue(i0, D0, Q);
                    edgeToWord.put(e0, gen);
                    generatorToEdge.put(gen, e0);

                    while (Q.size() > 0) {
                        final Face<T> f = Q.removeFirst();
                        final T D = f.getElement();
                        final int i = f.getFirstIndex();
                        final int j = f.getSecondIndex();
                        if (!boundary.isOnBoundary(i, D)) {
                            continue;
                        }
                        if (boundary.glueCountAtRidge(i, D, j) ==
                        		2 * ds.m(i, j, D))
                        {
                            final DSPair<T> e = new DSPair<T>(i, D);
                            final FreeWord<String> w =
                            		traceWord(ds, f, edgeToWord);
                            edgeToWord.put(e, w.inverse());
                            boundary.glueAndEnqueue(i, D, Q);
                        }
                    }
                }
            }
        }

        // --- complete edgeToWord and convert to a finite alphabet
        final FiniteAlphabet<String> B =
        		FiniteAlphabet.fromPrefix("g_", nrGens);
        final Set<DSPair<T>> edges =
        		new HashSet<DSPair<T>>(edgeToWord.keySet());
        for (final DSPair<T> e: edges) {
            final FreeWord<String> w = edgeToWord.get(e);
            edgeToWord.put(e, new FreeWord<String>(B, w));
            if (!e.equals(e.reverse(ds))) {
                edgeToWord.put(e.reverse(ds),
                		new FreeWord<String>(B, w.inverse()));
            }
        }

        // --- convert genToEdge to the same finite alphabet
        final Set<FreeWord<String>> oldGens =
        		new HashSet<FreeWord<String>>(generatorToEdge.keySet());
        for (final FreeWord<String> gen: oldGens) {
            final DSPair<T> e = generatorToEdge.get(gen);
            generatorToEdge.remove(gen);
            generatorToEdge.put(new FreeWord<String>(B, gen), e);
        }

        // --- collect relators for the fundamental group
        final List<FreeWord<String>> relators =
        		new LinkedList<FreeWord<String>>();
        this.axes = new HashSet<Pair<FreeWord<String>, Integer>>();
        for (final int i: ds.indices()) {
            for (final int j: ds.indices()) {
                if (j <= i) {
                    continue;
                }
                final List<Integer> idx = new IndexList(i, j);
                for (final T D: ds.orbitReps(idx)) {
                    final Face<T> f = new Face<T>(i, D, j);
                    final FreeWord<String> w = traceWord(ds, f, edgeToWord);
                    if (w != null) {
                        final int v = ds.v(i, j, D);
                        final FreeWord<String> wRep =
                        		FpGroup.relatorRepresentative(w);
                        relators.add(wRep.raisedTo(v));
                        if (v > 1) {
                            axes.add(new Pair<FreeWord<String>, Integer>(
                            		wRep, v));
                        }
                    }
                }
            }
        }
        final Set<FreeWord<String>> gens =
        		new HashSet<FreeWord<String>>(generatorToEdge.keySet());
        for (final FreeWord<String> gen: gens) {
            final DSPair<T> e = generatorToEdge.get(gen);
            final int i = e.getIndex();
            final T D = e.getElement();
            if (ds.op(i, D).equals(D)) {
                relators.add(gen.raisedTo(2));
            }
        }

        // --- construct the presentation
        this.presentation = new FpGroup<String>(B, relators);
    }

    /**
     * Traces the word associated to a 2-dimensional orbit in a Delaney symbol.
     * 
     * @param ds the symbol.
     * @param f an oriented ridge representing the orbit.
     * @param edgeToWord a mapping of symbol edges to words.
     * @return the product of all words read along the orbit.
     */
    protected static <T> FreeWord<String> traceWord(
    		final DelaneySymbol<T> ds,
    		final Face<T> f,
    		final Map<DSPair<T>, FreeWord<String>> edgeToWord)
    {
        FreeWord<String> w = null;
        final int i = f.getFirstIndex();
        final int j = f.getSecondIndex();
        final T D = f.getElement();

        T E = D;
        int k = i;
        while (true) {
            final T Ek = ds.op(k, E);
            final FreeWord<String> u = edgeToWord.get(new DSPair<T>(k, E));
            final FreeWord<String> uinv = edgeToWord.get(new DSPair<T>(k, Ek));
            final FreeWord<String> factor;
            if (u != null) {
                factor = u;
            } else if (uinv != null) {
                factor = uinv.inverse();
            } else {
                factor = null;
            }
            if (w == null) {
                w = factor;
            } else if (factor != null) {
                w = w.times(factor);
            }
            E = ds.op(k, E);
            k = i + j - k;
            if (E.equals(D) && k == i) {
                break;
            }
        }
        return w;
    }

    /**
     * Returns the value of edgeToWord.
     * 
     * @return a reference to edgeToWord, which is an unmodifiable map.
     */
    public Map<DSPair<T>, FreeWord<String>> getEdgeToWord() {
        return this.edgeToWord;
    }

    /**
     * Returns the value of generatorToEdge.
     * 
     * @return a reference to generatorToEdge, which is an unmodifiable map.
     */
    public Map<FreeWord<String>, DSPair<T>> getGeneratorToEdge() {
        return this.generatorToEdge;
    }

    /**
     * Returns the value of symbol.
     * 
     * @return the current value of symbol.
     */
    public DelaneySymbol<T> getSymbol() {
        return this.symbol;
    }

    /**
     * Returns the value of axes.
     * 
     * @return the set of axes, which is read-only..
     */
    public Set<Pair<FreeWord<String>, Integer>> getAxes() {
        return this.axes;
    }
    
    /**
     * @return the presentation.
     */
    public FpGroup<String> getPresentation() {
        return presentation;
    }
}

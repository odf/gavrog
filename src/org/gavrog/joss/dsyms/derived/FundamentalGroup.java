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

package org.gavrog.joss.dsyms.derived;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Boundary.Face;


/**
 * The fundamental group of a {@link org.gavrog.joss.dsyms.basic.DelaneySymbol}.
 * 
 * @author Olaf Delgado
 * @version $Id: FundamentalGroup.java,v 1.5 2007/04/26 20:21:58 odf Exp $
 */
public class FundamentalGroup {

    final private DelaneySymbol symbol;

    final private Map edgeToWord;

    final private Map generatorToEdge;

    final private Set axes;
    
    final private FpGroup presentation;

    /**
     * Constructs a FundementalGroup instance for a given symbol.
     * 
     * @param ds the symbol.
     */
    public FundamentalGroup(final DelaneySymbol ds) {
        // --- initialize
        final Boundary boundary = new Boundary(ds);
        final Alphabet A = new PrefixAlphabet("g_");
        final FreeWord IdWord = new FreeWord(A);
        
        this.symbol = ds;
        this.edgeToWord = new HashMap();
        this.generatorToEdge = new HashMap();

        // --- glue fundamental ("inner") edges
        final FundamentalEdges fund = new FundamentalEdges(ds);
        while (fund.hasNext()) {
            final DSPair e = (DSPair) fund.next();
            boundary.glue(e);
            edgeToWord.put(e, IdWord);
        }

        // --- find generators for the fundamental group
        int nrGens = 0;
        for (final Iterator elms = ds.elements(); elms.hasNext();) {
            final Object D0 = elms.next();
            for (final Iterator idcs = ds.indices(); idcs.hasNext();) {
                final int i0 = ((Integer) idcs.next()).intValue();
                if (boundary.isOnBoundary(i0, D0)) {
                    final DSPair e0 = new DSPair(i0, D0);
                    final LinkedList Q = new LinkedList();
                    final FreeWord gen = new FreeWord(A, ++nrGens);
                    boundary.glueAndEnqueue(i0, D0, Q);
                    edgeToWord.put(e0, gen);
                    generatorToEdge.put(gen, e0);

                    while (Q.size() > 0) {
                        final Face f = (Face) Q.removeFirst();
                        final Object D = f.getElement();
                        final int i = f.getFirstIndex();
                        final int j = f.getSecondIndex();
                        if (!boundary.isOnBoundary(i, D)) {
                            continue;
                        }
                        if (boundary.glueCountAtRidge(i, D, j) == 2 * ds.m(i,
                                j, D)) {
                            final DSPair e = new DSPair(i, D);
                            final FreeWord w = traceWord(ds, f, edgeToWord);
                            edgeToWord.put(e, w.inverse());
                            boundary.glueAndEnqueue(i, D, Q);
                        }
                    }
                }
            }
        }

        // --- complete edgeToWord and convert to a finite alphabet
        final FiniteAlphabet B = new FiniteAlphabet("g_", nrGens);
        final Set edges = new HashSet(edgeToWord.keySet());
        final Iterator edgeIter = edges.iterator();
        while (edgeIter.hasNext()) {
            final DSPair e = (DSPair) edgeIter.next();
            final FreeWord w = (FreeWord) edgeToWord.get(e);
            edgeToWord.put(e, new FreeWord(B, w));
            if (!e.equals(e.reverse(ds))) {
                edgeToWord.put(e.reverse(ds), new FreeWord(B, w.inverse()));
            }
        }

        // --- convert genToEdge to the same finite alphabet
        final Set oldGens = new HashSet(generatorToEdge.keySet());
        for (final Iterator iter = oldGens.iterator(); iter.hasNext();) {
            final FreeWord gen = (FreeWord) iter.next();
            final DSPair e = (DSPair) generatorToEdge.get(gen);
            generatorToEdge.remove(gen);
            generatorToEdge.put(new FreeWord(B, gen), e);
        }

        // --- collect relators for the fundamental group
        final List relators = new LinkedList();
        this.axes = new HashSet();
        for (final Iterator idcs = ds.indices(); idcs.hasNext();) {
            final int i = ((Integer) idcs.next()).intValue();
            for (final Iterator idcs1 = ds.indices(); idcs1.hasNext();) {
                final int j = ((Integer) idcs1.next()).intValue();
                if (j <= i) {
                    continue;
                }
                final List idx = new IndexList(i, j);
                final Iterator reps = ds.orbitReps(idx);
                while (reps.hasNext()) {
                    final Object D = reps.next();
                    final Face f = new Face(i, D, j);
                    final FreeWord w = traceWord(ds, f, edgeToWord);
                    if (w != null) {
                        final int v = ds.v(i, j, D);
                        final FreeWord wRep = FpGroup.relatorRepresentative(w);
                        relators.add(wRep.raisedTo(v));
                        if (v > 1) {
                            axes.add(new Pair(wRep, new Integer(v)));
                        }
                    }
                }
            }
        }
        final Set gens = new HashSet(generatorToEdge.keySet());
        for (final Iterator gensIter = gens.iterator(); gensIter.hasNext();) {
            final FreeWord gen = (FreeWord) gensIter.next();
            final DSPair e = (DSPair) generatorToEdge.get(gen);
            final int i = e.getIndex();
            final Object D = e.getElement();
            if (ds.op(i, D).equals(D)) {
                relators.add(gen.raisedTo(2));
            }
        }

        // --- construct the presentation
        this.presentation = new FpGroup(B, relators);
    }

    /**
     * Traces the word associated to a 2-dimensional orbit in a Delaney symbol.
     * 
     * @param ds the symbol.
     * @param f an oriented ridge representing the orbit.
     * @param edgeToWord a mapping of symbol edges to words.
     * @return the product of all words read along the orbit.
     */
    protected static FreeWord traceWord(DelaneySymbol ds, Face f, Map edgeToWord) {
        FreeWord w = null;
        final int i = f.getFirstIndex();
        final int j = f.getSecondIndex();
        final Object D = f.getElement();

        Object E = D;
        int k = i;
        while (true) {
            final Object Ek = ds.op(k, E);
            final FreeWord u = (FreeWord) edgeToWord.get(new DSPair(k, E));
            final FreeWord uinv = (FreeWord) edgeToWord.get(new DSPair(k, Ek));
            final FreeWord factor;
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
    public Map getEdgeToWord() {
        return this.edgeToWord;
    }

    /**
     * Returns the value of generatorToEdge.
     * 
     * @return a reference to generatorToEdge, which is an unmodifiable map.
     */
    public Map getGeneratorToEdge() {
        return this.generatorToEdge;
    }

    /**
     * Returns the value of symbol.
     * 
     * @return the current value of symbol.
     */
    public DelaneySymbol getSymbol() {
        return this.symbol;
    }

    /**
     * Returns the value of axes.
     * 
     * @return the set of axes, which is read-only..
     */
    public Set getAxes() {
        return this.axes;
    }
    
    /**
     * @return the presentation.
     */
    public FpGroup getPresentation() {
        return presentation;
    }
}

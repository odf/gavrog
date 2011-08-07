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

package org.gavrog.joss.dsyms.basic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.GroupActions;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;

/**
 * @author Olaf Delgado
 * @version $Id: DSCover.java,v 1.4 2007/04/26 20:21:56 odf Exp $
 */
public class DSCover extends DSymbol {
    final private DSMorphism coverMorphism;
    final private DelaneySymbol image;
    
    /**
     * Internally used by the private constructor.
     */
    private static class Descriptor {
        final private int op[][];
        final private int v[][];
        final private DelaneySymbol image;
        final private Object imageOf1;

        public Descriptor(final int[][] op, final int[][] v,
                final DelaneySymbol image, final Object imageOf1) {
            this.op = op;
            this.v = v;
            this.image = image;
            this.imageOf1 = imageOf1;
        }
    }
    
    /**
     * Constructs an instance.
     * @param descriptor a descriptor for the new instance.
     */
    private DSCover(final Descriptor d) {
        super(d.op, d.v);
        this.image = d.image;
        this.coverMorphism = new DSMorphism(this, this.image, new Integer(1),
                d.imageOf1);
    }

    /**
     * Constructs an instance given explicit data.
     * @param cover the covering symbol.
     * @param image the image (base) symbol.
     * @param imageOf1 the image of the cover element 1.
     */
    public DSCover(final DSymbol cover, final DelaneySymbol image,
            final Object imageOf1) {
        super(cover);
        this.image = image;
        this.coverMorphism = new DSMorphism(this, this.image, new Integer(1),
                imageOf1);
    }
    
    /**
     * Constructs an instance induced by an action of the fundamental group of
     * the image symbol. See {@link #make(FundamentalGroup, GroupAction)} below
     * for details.
     * 
     * @param G a fundamental group of a symbol.
     * @param action an action of the fundamental group.
     */
    public DSCover(final FundamentalGroup G, final GroupAction action) {
        this(make(G, action));
    }

    /**
     * Constructs a cover of a Delaney symbol induced by an action of its
     * fundamental group. The fundamental group of the new symbol will be an
     * element stabilizer w.r.t. the action. The covering morphism can be
     * reconstructed as the on which maps the first element of the cover onto
     * the first element of the base symbol.
     * 
     * @param G a fundamental group of a symbol.
     * @param action an action of the fundamental group.
     * 
     * @return a descriptor for the new symbol to pass to the constructor.
     */
    private static Descriptor make(final FundamentalGroup G,
            final GroupAction action) {

        if (action.getGroup() != G.getPresentation()) {
            final String s = "the action must be by the same group";
            throw new IllegalArgumentException(s);
        }
        
        if (!(G.getSymbol() instanceof DSymbol)) {
            // TODO fix this
            final String s = "only DSymbols are supported as base symbols";
            throw new UnsupportedOperationException(s);
        }
        
        // --- preliminaries
        final DelaneySymbol ds = G.getSymbol();
        final Map edge2word = G.getEdgeToWord();
        final GroupAction flatAction = GroupActions.flat(action);
        
        final int dim = ds.dim();
        final int nOld = ds.size();
        final int nLayers = action.size();
        final int nNew = nOld * nLayers;

        final int op[][] = new int[dim + 1][nNew + 1];
        final int v[][] = new int[dim][nNew + 1];
    
        // --- generate the neighbor definitions
        for (int i = 0; i <= dim; ++i) {
            for (int D = 1; D <= nOld; ++D) {
                final int Di = ((Integer) ds.op(i, new Integer(D))).intValue();
                final FreeWord g = (FreeWord) edge2word.get(new DSPair(i,
                        new Integer(D)));
                for (int k = 0; k < nLayers; ++k) {
                    op[i][D + nOld * k] = Di
                            + nOld
                            * ((Integer) flatAction.apply(new Integer(k), g))
                                    .intValue();
                }
            }
        }

        // --- make a temporary symbol in which to compute r-values
        final DSymbol tmp = new DSymbol(op, v);

        // --- generate the v-values for the final symbol
        for (int i = 0; i < dim; ++i) {
            final List idcs = new IndexList(i, i + 1);
            for (final Iterator reps = tmp.orbitReps(idcs); reps
                    .hasNext();) {
                final Integer D = (Integer) reps.next();
                final Integer E = new Integer((D.intValue() - 1) % nOld + 1);
                final int m = ds.m(i, i + 1, E);
                final int r = tmp.r(i, i + 1, D);
                final int b = m / r;
                for (final Iterator orb = tmp.orbit(idcs, D); orb.hasNext();) {
                    v[i][((Integer) orb.next()).intValue()] = b;
                }
            }
        }
        
        return new Descriptor(op, v, ds, ds.elements().next());
    }
    
    /**
     * @return the coverMorphism
     */
    public DSMorphism getCoverMorphism() {
        return this.coverMorphism;
    }

    /**
     * @return the image
     */
    public DelaneySymbol getImage() {
        return this.image;
    }
    
    /**
     * Retrieves the image of a particular cover element.
     * @param D the cover element.
     * @return the image of the given element.
     */
    public Object image(final Object D) {
        return getCoverMorphism().get(D);
    }
}

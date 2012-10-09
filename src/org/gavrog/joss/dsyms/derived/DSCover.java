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

import java.util.List;
import java.util.Map;

import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.GroupActions;
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

/**
 */
public class DSCover<T> extends DSymbol {
    final private DSMorphism<Integer, T> coverMorphism;
    final private DelaneySymbol<T> image;
    
    /**
     * Internally used by the private constructor.
     */
    private static class Descriptor<T> {
        final private int op[][];
        final private int v[][];
        final private DelaneySymbol<T> image;
        final private T imageOf1;

        public Descriptor(final int[][] op, final int[][] v,
                final DelaneySymbol<T> image, final T imageOf1) {
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
    private DSCover(final Descriptor<T> d) {
        super(d.op, d.v);
        this.image = d.image;
        this.coverMorphism =
        		new DSMorphism<Integer, T>(this, this.image, 1, d.imageOf1);
    }

    /**
     * Constructs an instance given explicit data.
     * @param cover the covering symbol.
     * @param image the image (base) symbol.
     * @param imageOf1 the image of the cover element 1.
     */
    public DSCover(
    		final DSymbol cover,
    		final DelaneySymbol<T> image,
            final T imageOf1)
    {
        super(cover);
        this.image = image;
        this.coverMorphism =
        		new DSMorphism<Integer, T>(this, this.image, 1, imageOf1);
    }
    
    /**
     * Constructs an instance induced by an action of the fundamental group of
     * the image symbol. See {@link #make(FundamentalGroup, GroupAction)} below
     * for details.
     * 
     * @param G a fundamental group of a symbol.
     * @param action an action of the fundamental group.
     */
    public DSCover(
            final FundamentalGroup<T> G,
    		final GroupAction<String, ?> action)
    {
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
    private static <T> Descriptor<T> make(
    		final FundamentalGroup<T> G,
            final GroupAction<String, ?> action) {

        if (action.getGroup() != G.getPresentation()) {
            final String s = "the action must be by the same group";
            throw new IllegalArgumentException(s);
        }
        
        // --- preliminaries
        final DelaneySymbol<T> symbol = G.getSymbol();
        final DelaneySymbol<Integer> ds = symbol.flat();
        final Map<DSPair<T>, FreeWord<String>> edge2word = G.getEdgeToWord();
        final GroupAction<String, Integer> flatAction =
        		GroupActions.flat(action);
        
        final int dim = ds.dim();
        final int nOld = ds.size();
        final int nLayers = action.size();
        final int nNew = nOld * nLayers;

        final int op[][] = new int[dim + 1][nNew + 1];
        final int v[][] = new int[dim][nNew + 1];
    
        // --- generate the neighbor definitions
        for (int i = 0; i <= dim; ++i) {
            for (int D = 1; D <= nOld; ++D) {
                final int Di = ds.op(i, D);
                final FreeWord<String> g =
                		edge2word.get(new DSPair<Integer>(i, D));
                for (int k = 0; k < nLayers; ++k) {
                    op[i][D + nOld * k] = Di + nOld * flatAction.apply(k, g);
                }
            }
        }

        // --- make a temporary symbol in which to compute r-values
        final DSymbol tmp = new DSymbol(op, v);

        // --- generate the v-values for the final symbol
        for (int i = 0; i < dim; ++i) {
            final List<Integer> idcs = new IndexList(i, i + 1);
            for (final int D: tmp.orbitReps(idcs)) {
                final int E = (D - 1) % nOld + 1;
                final int m = ds.m(i, i + 1, E);
                final int r = tmp.r(i, i + 1, D);
                final int b = m / r;
                for (final int C: tmp.orbit(idcs, D)) {
                    v[i][C] = b;
                }
            }
        }
        
        return new Descriptor<T>(op, v, symbol, symbol.elements().next());
    }
    
    /**
     * @return the coverMorphism
     */
    public DSMorphism<Integer, T> getCoverMorphism() {
        return this.coverMorphism;
    }

    /**
     * @return the image
     */
    public DelaneySymbol<T> getImage() {
        return this.image;
    }
    
    /**
     * Retrieves the image of a particular cover element.
     * @param D the cover element.
     * @return the image of the given element.
     */
    public T image(final Integer D) {
        return getCoverMorphism().get(D);
    }
}

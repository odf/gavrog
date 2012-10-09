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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;
import org.gavrog.jane.fpgroups.Coset;
import org.gavrog.jane.fpgroups.CosetAction;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.GroupActions;
import org.gavrog.jane.fpgroups.SmallActionsIterator;
import org.gavrog.jane.fpgroups.Stabilizer;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;


/**
 * Utility methods for constructing Delaney symbol covers.
 */
public class Covers {
    /**
     * Returns the universal cover of a Delaney symbol.
     * 
     * This method only works if both the symbol and its fundamental group are
     * finite and if the group's Caley graph, i.e., its coset table action with
     * respect to the trivial subgroup, can be computed within the space
     * restrictions as specified in {@link CosetAction#DEFAULT_SiZE_LIMIT}.
     * 
     * @param ds a Delaney symbol.
     * @return the universal cover of ds.
     */
    public static <T> DSCover<T> finiteUniversalCover(
            final DelaneySymbol<T> ds)
    {
        if (ds.dim() == 2 && !ds.curvature2D().isPositive()) {
            // --- we can check for a finite cover in 2d
            throw new UnsupportedOperationException("result would be infinite");
        }
        final FundamentalGroup<T> fg = new FundamentalGroup<T>(ds);
        final GroupAction<String, Coset<String, T>> T =
                new CosetAction<String, T>(fg.getPresentation());
        final int n = T.size();
        if (ds.dim() == 2 && ds.isSpherical2D()) {
            // --- another quick check for a special case
            final int sz = ds.sphericalGroupSize2D();
            assert sz == n : "group size is " + n + ", but should be " + sz;
        }
        return new DSCover<T>(fg, T);
    }
    
    /**
     * Returns an iterator over all covers of a given Delaney symbol up to
     * isomorphism.
     * 
     * This method only works if both the symbol and its fundamental group are
     * finite and if the group's Caley graph, i.e., its coset table action with
     * respect to the trivial subgroup, can be computed within the space
     * restrictions as specified in {@link CosetAction#DEFAULT_SiZE_LIMIT}.
     * 
     * @param ds a Delaney symbol.
     * @return an iterator over the covers of ds.
     */
    public static <T> Iterator<DSCover<T>> allCovers(
            final DelaneySymbol<T> ds)
    {
        if (ds.dim() == 2 && !ds.curvature2D().isPositive()) {
            // --- we can check for a finite cover in 2d
            throw new UnsupportedOperationException("result would be infinite");
        }
        final FundamentalGroup<T> F = new FundamentalGroup<T>(ds);
        final FpGroup<String> G = F.getPresentation();
        final int n = new CosetAction<String, T>(G).size();
        if (ds.dim() == 2 && ds.isSpherical2D()) {
            // --- another quick check for a special case
            final int sz = ds.sphericalGroupSize2D();
            assert sz == n : "group size is " + n + ", but should be " + sz;
        }
        final Iterator<GroupAction<String, Integer>> actions =
                new SmallActionsIterator<String>(G, n, false);
        
        return new IteratorAdapter<DSCover<T>>() {
            protected DSCover<T> findNext() throws NoSuchElementException {
                return new DSCover<T>(F, actions.next());
            }
        };
    }
    
    /**
     * Returns the smallest unbranched cover of a 2-dimensional euclidean (flat)
     * Delaney symbol.
     * 
     * @param ds the original symbol.
     * @return the toroidal cover.
     */
    public static <T> DSCover<T> toroidalCover2D(final DelaneySymbol<T> ds) {
        if (ds.dim() != 2) {
            final String s = "symbol must be 2-dimensional";
            throw new UnsupportedOperationException(s);
        }
        if (!ds.curvature2D().isZero()) {
            final String s = "symbol must be euclidean";
            throw new UnsupportedOperationException(s);
        }
        
        final List<Integer> idcs = new IndexList(ds);
        int deg = 1;
		for (int k = 0; k < ds.dim(); ++k) {
			final int i = idcs.get(k);
			for (int l = k + 1; l <= ds.dim(); ++l) {
				final int j = idcs.get(l);
				for (final T D: ds.orbitReps(new IndexList(i, j))) {
					deg = Math.max(deg, ds.v(i, j, D));
				}
			}
		}
        
        final DelaneySymbol<Integer> dso = ds.orientedCover();
        final FundamentalGroup<Integer> G = new FundamentalGroup<Integer>(dso);
        final FpGroup<String> pres = G.getPresentation();
        final Iterator<GroupAction<String, Integer>> actions =
                new SmallActionsIterator<String>(pres, deg, true);
        while (actions.hasNext()) {
            final GroupAction<String, Integer> action = actions.next();
            if (annihilatesAxes(action, G.getAxes())) {
                final DSymbol cov = new DSCover<Integer>(G, action);
                return new DSCover<T>(cov, ds, ds.elements().next());
            }
        }
        
        throw new RuntimeException("serious problem here: missed the cover");
    }

    /**
     * Returns a smallest pseudo-toroidal cover of a 3-dimensional Delaney
     * symbol, if one exists. A pseudo-toroidal cover is one with the branching
     * numbers v(i, j, D) all equal to 1 and a fundamental group which has
     * abelian invariants [0,0,0].
     * 
     * CAVEAT: the cover is returned as a flat symbol. The covering morphism
     * can be reconstructed as the on which maps the first element of the cover
     * onto the first element of the base symbol.
     * 
     * @param ds a 3-dimensional Delaney symbol.
     * @return the pseudo-toroidal cover or null.
     */
    public static <T> DSCover<T> pseudoToroidalCover3D(
            final DelaneySymbol<T> ds)
    {
        if (ds.dim() != 3) {
            final String s = "symbol must be 3-dimensional";
            throw new UnsupportedOperationException(s);
        }
        if (!ds.isLocallyEuclidean3D()) {
            final String s = "symbol must be locally euclidean";
            throw new UnsupportedOperationException(s);
        }

        final List<GroupAction<String, Integer>> z1 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z2 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z2a =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z2b =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z3 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z3a =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z4 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> v4 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> s3 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> s3a =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> z6 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> d4 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> d6 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> a4 =
                new LinkedList<GroupAction<String, Integer>>();
        final List<GroupAction<String, Integer>> s4 =
                new LinkedList<GroupAction<String, Integer>>();
        
        final List<List<GroupAction<String, Integer>>> lists =
            new LinkedList<List<GroupAction<String, Integer>>>();
        lists.add(z1);
        lists.add(z2);
        lists.add(z3);
        lists.add(z4);
        lists.add(v4);
        lists.add(s3);
        lists.add(z6);
        lists.add(d4);
        lists.add(d6);
        lists.add(a4);
        lists.add(s4);
        
        final DelaneySymbol<Integer> dso = ds.orientedCover();
        final FundamentalGroup<Integer> G = new FundamentalGroup<Integer>(dso);
        
        final Set<Pair<FreeWord<String>, Integer>> allAxes = G.getAxes();
        final Set<Pair<FreeWord<String>, Integer>> o2Axes =
                new HashSet<Pair<FreeWord<String>, Integer>>();
        final Set<Pair<FreeWord<String>, Integer>> o3Axes =
                new HashSet<Pair<FreeWord<String>, Integer>>();
        for (final Pair<FreeWord<String>, Integer> axis: allAxes) {
            final int order = axis.getSecond();
            if (order == 2) {
                o2Axes.add(axis);
            } else if (order == 3) {
                o3Axes.add(axis);
            } else if (order == 5 || order > 6) {
                return null;
            }
        }
        
        final FpGroup<String> pres = G.getPresentation();
        final Iterator<GroupAction<String, Integer>> actions =
                new SmallActionsIterator<String>(pres, 4, false);

        while (actions.hasNext()) {
            final GroupAction<String, Integer> core =
                    GroupActions.flat(
                            GroupActions.orbit(
                                    GroupActions.cover(actions.next())));
            final int index = core.size();
            
            if (index == 2) {
                if (annihilatesAxes(core, o2Axes)) {
                    z2a.add(core);
                }
                z2b.add(core);
            } else if (index == 3 && annihilatesAxes(core, o3Axes)) {
                z3a.add(core);
            } else if (index == 6 && annihilatesAxes(core, o3Axes)) {
                s3a.add(core);
            }
            
            if (annihilatesAxes(core, allAxes)) {
                if (index == 1) {
                    z1.add(core);
                } else if (index == 2) {
                    z2.add(core);
                } else if (index == 3) {
                    z3.add(core);
                } else if (index == 4) {
                    if (hasNonInvolutiveGenerator(core)) {
                        z4.add(core);
                    } else {
                        v4.add(core);
                    }
                } else if (index == 6) {
                    s3.add(core);
                } else if (index == 8) {
                    d4.add(core);
                } else if (index == 12) {
                    a4.add(core);
                } else if (index == 24) {
                    s4.add(core);
                }
            }
        }
            
        for (final GroupAction<String, Integer> a: z3a) {
            for (final GroupAction<String, Integer> b: z2a) {
                final GroupAction<String, Integer> prod =
                        GroupActions.flat(
                                GroupActions.orbit(
                                        GroupActions.product(a, b)));
                if (prod.size() == 6 && annihilatesAxes(prod, allAxes)) {
                    z6.add(prod);
                }
            }
        }
        
        for (final GroupAction<String, Integer> a: s3a) {
            for (final GroupAction<String, Integer> b: z2b) {
                final GroupAction<String, Integer> prod =
                        GroupActions.flat(
                                GroupActions.orbit(
                                        GroupActions.product(a, b)));
                if (prod.size() == 12 && annihilatesAxes(prod, allAxes)) {
                    d6.add(prod);
                }
            }
        }
        
        final List<Whole> expected = new LinkedList<Whole>();
        expected.add(Whole.ZERO);
        expected.add(Whole.ZERO);
        expected.add(Whole.ZERO);
        
        for (final List<GroupAction<String, Integer>> list: lists) {
            for (final GroupAction<String, Integer> action: list) {
                final int base = action.domain().next();
                final Stabilizer<String, Integer> stab =
                        new Stabilizer<String, Integer>(action, base);
                if (stab.getPresentation().abelianInvariants()
                        .equals(expected))
                {
                    final DSymbol cov = new DSCover<Integer>(G, action);
                    return new DSCover<T>(cov, ds, ds.elements().next());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Tests if any of the group generators applied twice differs from the
     * identity in the given action.
     * 
     * @param action the group action.
     * @return true if a noninvolutive generator exists.
     */
    private static boolean hasNonInvolutiveGenerator(
            final GroupAction<String, Integer> action)
    {
        final List<FreeWord<String>> generators =
                action.getGroup().getGenerators();
        for (final Iterator<Integer> domain = action.domain();
                domain.hasNext();)
        {
            final int x = domain.next();
            for (final FreeWord<String> g: generators) {
                if (!action.apply(x, g.raisedTo(2)).equals(x)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method, checking whether a group action annihilates all the axes
     * given. An axis is a pair consisting of a group element and a number. It
     * is annihilated if the order of that element in the action is a multiple
     * of that number. This means in effect that the corresponding edge in the
     * Delaney symbol cover generated by the action has branching number 1.
     * 
     * In fact, the order of the element must always divide the given number,
     * so we check for that in order to catch any possible nasty bugs.
     * 
     * The group action is supposed to be such that its stabilizer is be a
     * normal subgroup.
     * 
     * @param action the group action.
     * @param axes the set of axes.
     * 
     * @return true if the given axis is annihilated.
     */
    private static boolean annihilatesAxes(
            final GroupAction<String, Integer> action,
            final Set<Pair<FreeWord<String>, Integer>> axes)
    {
        final int x0 = action.domain().next();
        for (final Pair<FreeWord<String>, Integer> pair: axes) {
            final FreeWord<String> w = pair.getFirst();
            final int expected = pair.getSecond();
            
            int x = x0;
            int order = 0;
            do {
                x = action.apply(x, w);
                ++order;
            } while (x != x0);
            if (expected % order != 0) {
                throw new RuntimeException("this should never happen");
            }
            if (order != expected) {
                return false;
            }
        }
        return true;
    }
}

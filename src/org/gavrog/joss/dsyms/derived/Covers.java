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

package org.gavrog.joss.dsyms.derived;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;
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
 * 
 * @author Olaf Delgado
 * @version $Id: Covers.java,v 1.13 2007/11/28 03:31:53 odf Exp $
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
    public static DSCover finiteUniversalCover(final DelaneySymbol ds) {
        if (ds.dim() == 2 && !ds.curvature2D().isPositive()) {
            // --- we can check for a finite cover in 2d
            throw new UnsupportedOperationException("result would be infinite");
        }
        final FundamentalGroup fg = new FundamentalGroup(ds);
        final GroupAction T = new CosetAction(fg.getPresentation());
        final int n = T.size();
        if (ds.dim() == 2 && ds.isSpherical2D()) {
            // --- another quick check for a special case
            final int sz = ds.sphericalGroupSize2D();
            assert sz == n : "group size is " + n + ", but should be " + sz;
        }
        return new DSCover(fg, T);
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
    public static Iterator allCovers(final DelaneySymbol ds) {
        if (ds.dim() == 2 && !ds.curvature2D().isPositive()) {
            // --- we can check for a finite cover in 2d
            throw new UnsupportedOperationException("result would be infinite");
        }
        final FundamentalGroup F = new FundamentalGroup(ds);
        final FpGroup G = F.getPresentation();
        final int n = new CosetAction(G).size();
        if (ds.dim() == 2 && ds.isSpherical2D()) {
            // --- another quick check for a special case
            final int sz = ds.sphericalGroupSize2D();
            assert sz == n : "group size is " + n + ", but should be " + sz;
        }
        final Iterator actions = new SmallActionsIterator(G, n, false);
        
        return new IteratorAdapter() {
            protected Object findNext() throws NoSuchElementException {
                return new DSCover(F, (GroupAction) actions.next());
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
    public static DSCover toroidalCover2D(final DelaneySymbol ds) {
        if (ds.dim() != 2) {
            final String s = "symbol must be 2-dimensional";
            throw new UnsupportedOperationException(s);
        }
        if (!ds.curvature2D().isZero()) {
            final String s = "symbol must be euclidean";
            throw new UnsupportedOperationException(s);
        }
        
        final List idcs = new IndexList(ds);
        int deg = 1;
		for (int k = 0; k < ds.dim(); ++k) {
			final int i = ((Integer) idcs.get(k)).intValue();
			for (int l = k + 1; l <= ds.dim(); ++l) {
				final int j = ((Integer) idcs.get(l)).intValue();
				final Iterator reps = ds.orbitReps(new IndexList(i, j));
				while (reps.hasNext()) {
					deg = Math.max(deg, ds.v(i, j, reps.next()));
				}
			}
		}
        
        final DelaneySymbol dso = ds.orientedCover();
        final FundamentalGroup G = new FundamentalGroup(dso);
        final Iterator actions = new SmallActionsIterator(G.getPresentation(),
                deg, true);
        while (actions.hasNext()) {
            final GroupAction action = (GroupAction) actions.next();
            if (annihilatesAxes(action, G.getAxes())) {
                final DSymbol cov = new DSCover(G, action);
                return new DSCover(cov, ds, ds.elements().next());
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
    public static DSCover pseudoToroidalCover3D(final DelaneySymbol ds) {
        if (ds.dim() != 3) {
            final String s = "symbol must be 3-dimensional";
            throw new UnsupportedOperationException(s);
        }
        if (!ds.isLocallyEuclidean3D()) {
            final String s = "symbol must be locally euclidean";
            throw new UnsupportedOperationException(s);
        }

        final List z1 = new LinkedList();
        final List z2 = new LinkedList();
        final List z2a = new LinkedList();
        final List z2b = new LinkedList();
        final List z3 = new LinkedList();
        final List z3a = new LinkedList();
        final List z4 = new LinkedList();
        final List v4 = new LinkedList();
        final List s3 = new LinkedList();
        final List s3a = new LinkedList();
        final List z6 = new LinkedList();
        final List d4 = new LinkedList();
        final List d6 = new LinkedList();
        final List a4 = new LinkedList();
        final List s4 = new LinkedList();
        
        final List lists[] =
            new List[] { z1, z2, z3, z4, v4, s3, z6, d4, d6, a4, s4 };
        
        final DelaneySymbol dso = ds.orientedCover();
        final FundamentalGroup G = new FundamentalGroup(dso);
        
        final Set allAxes = G.getAxes();
        final Set o2Axes = new HashSet();
        final Set o3Axes = new HashSet();
        for (final Iterator iter = allAxes.iterator(); iter.hasNext();) {
            final Pair axis = (Pair) iter.next();
            final int order = ((Integer) axis.getSecond()).intValue();
            if (order == 2) {
                o2Axes.add(axis);
            } else if (order == 3) {
                o3Axes.add(axis);
            } else if (order == 5 || order > 6) {
                return null;
            }
        }
        
        final FpGroup pres = G.getPresentation();
        final Iterator actions = new SmallActionsIterator(pres, 4, false);
        int count = 0;

        while (actions.hasNext()) {
            ++count;
            final GroupAction action = (GroupAction) actions.next();
            final GroupAction core = GroupActions.flat(GroupActions
                    .orbit(GroupActions.cover(action)));
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
            
        for (final Iterator it1 = z3a.iterator(); it1.hasNext();) {
            final GroupAction a = (GroupAction) it1.next();
            for (final Iterator it2 = z2a.iterator(); it2.hasNext();) {
                final GroupAction b = (GroupAction) it2.next();
                final GroupAction prod = GroupActions.flat(GroupActions
                        .orbit(GroupActions.product(a, b)));
                if (prod.size() == 6 && annihilatesAxes(prod, allAxes)) {
                    z6.add(prod);
                }
            }
        }
        
        for (final Iterator it1 = s3a.iterator(); it1.hasNext();) {
            final GroupAction a = (GroupAction) it1.next();
            for (final Iterator it2 = z2b.iterator(); it2.hasNext();) {
                final GroupAction b = (GroupAction) it2.next();
                final GroupAction prod =
                    GroupActions.orbit(GroupActions.product(a, b));
                if (prod.size() == 12 && annihilatesAxes(prod, allAxes)) {
                    d6.add(prod);
                }
            }
        }
        
        final List expected = new LinkedList();
        expected.add(Whole.ZERO);
        expected.add(Whole.ZERO);
        expected.add(Whole.ZERO);
        
        for (int i = 0; i < lists.length; ++i) {
            final List list = lists[i];
            for (final Iterator iter = list.iterator(); iter.hasNext();) {
                final GroupAction action = (GroupAction) iter.next();
                final Object base = action.domain().next();
                final Stabilizer stab = new Stabilizer(action, base);
                if (stab.getPresentation().abelianInvariants().equals(expected)) {
                    final DSymbol cov = new DSCover(G, action);
                    return new DSCover(cov, ds, ds.elements().next());
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
    private static boolean hasNonInvolutiveGenerator(GroupAction action) {
        final List generators = action.getGroup().getGenerators();
        for (final Iterator domain = action.domain(); domain.hasNext();) {
            final Object x = domain.next();
            for (final Iterator gens = generators.iterator(); gens.hasNext();) {
                final FreeWord g = (FreeWord) gens.next();
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
    private static boolean annihilatesAxes(final GroupAction action,
            final Set axes) {
        
        final Object x0 = action.domain().next();
        for (final Iterator iter = axes.iterator(); iter.hasNext();) {
            final Pair pair = (Pair) iter.next();
            final FreeWord w = (FreeWord) pair.getFirst();
            final int expected = ((Integer) pair.getSecond()).intValue();
            
            Object x = x0;
            int order = 0;
            do {
                x = action.apply(x, w);
                ++order;
            } while (!x.equals(x0));
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

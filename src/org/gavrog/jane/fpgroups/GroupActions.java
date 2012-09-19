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

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;


/**
 * This class contains some utility methods for group actions.
 */
final public class GroupActions {
    /**
     * No instances for this class.
     */
    private GroupActions() {}
    
    /**
     * Returns the product of two actions of a group. The product action is
     * defined as (a,b)g = (ag,bg).
     * 
     * @param a the first action.
     * @param b the second action.
     * @return the product action.
     */
    public static <E, A, B> GroupAction<E, Pair<A, B>> product(
            final GroupAction<E, A> a,
            final GroupAction<E, B> b) {
        if (!a.getGroup().equals(b.getGroup())) {
            throw new IllegalArgumentException("actions of different groups");
        }
        
        return new GroupAction<E, Pair<A, B>>() {
            public FpGroup<E> getGroup() {
                return a.getGroup();
            }

            public Iterator<Pair<A, B>> domain() {
                return Iterators.cantorProduct(a.domain(), b.domain());
            }

            public int size() {
                return a.size() * b.size();
            }

            public Pair<A, B> apply(final Pair<A, B> x, final FreeWord<E> w) {
                return new Pair<A, B>(a.apply(x.getFirst(), w),
                                      b.apply(x.getSecond(), w));
            }

            public boolean isDefinedOn(final Pair<A, B> x) {
                    return a.isDefinedOn(x.getFirst())
                            && b.isDefinedOn(x.getSecond());
            }
        };
    }
    
    /**
     * Returns the induced action on the set of permutations of the given
     * action's domain. For example, if the domain consists of elements a, b,
     * c and d, then (a, b, c, d)g = (ag, bg, cg, dg) and so on.
     * 
     * @param action the given action.
     * @return the cover or permutation action.
     */
    public static <E, D> GroupAction<E, List<D>> cover(
            final GroupAction<E, D> action) {
        final int n;
        
        try {
            n = action.size();
        } catch (UnsupportedOperationException ex) {
            final String s = "base action has no defined size";
            throw new UnsupportedOperationException(s);
        }

        return new GroupAction<E, List<D>>() {
            public FpGroup<E> getGroup() {
                return action.getGroup();
            }

            public Iterator<List<D>> domain() {
                @SuppressWarnings("unchecked")
                final D dom[] = (D[]) new Object[action.size()];
                final Iterator<D> iter = action.domain();
                for (int i = 0; i < n; ++i) {
                    dom[i] = iter.next();
                }
                return Iterators.permutations(dom);
            }

            public int size() {
                int r = 1;
                for (int i = 2; i <= n; ++i)
                    r *= i;
                return r;
            }

            public List<D> apply(final List<D> a, final FreeWord<E> w) {
                if (!isDefinedOn(a))
                    return null;

                final List<D> result = new ArrayList<D>();
                for (final D x: a)
                    result.add(action.apply(x, w));
                return result;
            }

            public boolean isDefinedOn(final List<D> a) {
                if (a.size() != n)
                    return false;
                final Set<D> seen = new HashSet<D>();
                for (final D x: a) {
                    if (!action.isDefinedOn(x) || seen.contains(x))
                        return false;
                    seen.add(x);
                }
                return true;
            }
        };
    }
    
    /**
     * Constructs the restriction of a group action to the orbit of an element.
     * Thus this method always returns a transitive group action.
     * 
     * @param base the base element for the orbit.
     * @param action the underlying group action.
     * @return the underlying group action restricted to the orbit of the base.
     */
    public static <E, D> GroupAction<E, D> orbit(final D base,
            final GroupAction<E, D> action) {
        try {
            action.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("action must be finite");
        }
        
        final List<FreeWord<E>> gens = action.getGroup().getGenerators();
        final LinkedList<D> queue = new LinkedList<D>();
        final Set<D> domain = new HashSet<D>();
        queue.addLast(base);
        domain.add(base);

        while (queue.size() > 0) {
            final D next = queue.removeFirst();
            for (final FreeWord<E> g: gens) {
                for (int i = -1; i <= 1; i += 2) {
                    final FreeWord<E> w = g.raisedTo(i);
                    final D neighbor = action.apply(next, w);
                    if (!domain.contains(neighbor)) {
                        domain.add(neighbor);
                        queue.addLast(neighbor);
                    }
                }
            }
        }
        
        return new GroupAction<E, D>() {
            public FpGroup<E> getGroup() {
                return action.getGroup();
            }

            public Iterator<D> domain() {
                return domain.iterator();
            }

            public int size() {
                return domain.size();
            }

            public D apply(final D x, final FreeWord<E> w) {
                if (!isDefinedOn(x)) {
                    return null;
                } else {
                    return action.apply(x, w);
                }
            }

            public boolean isDefinedOn(final D x) {
                return action.isDefinedOn(x) && domain.contains(x);
            }  
        };
    }
    
    /**
     * Constructs the restriction of a group action to the orbit of the first
     * element in its domain.
     * 
     * @param action the underlying group action.
     * @return the underlying group action restricted to its first orbit.
     */
    public static <E, D> GroupAction<E, D> orbit(
            final GroupAction<E, D> action) {
        final D base = action.domain().next();
        return orbit(base, action);
    }
    
    /**
     * Constructs a flat (tabular) group action isomorphic to a given one.
     * 
     * @param action the original action.
     * @return the flat action.
     */
    public static <E, D> GroupAction<E, Integer> flat(
            final GroupAction<E, D> action) {
        final int n;
        try {
            n = action.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("action must be finite");
        }
        
        final Map<D, Integer> old2new = new HashMap<D, Integer>();
        final List<D> new2old = new ArrayList<D>();
        for (final Iterator<D> iter = action.domain(); iter.hasNext();) {
            final D x = iter.next();
            old2new.put(x, new2old.size());
            new2old.add(x);
        }
        
        final List<FreeWord<E>> gens = action.getGroup().getGenerators();
        final Map<FreeWord<E>, int[]> generatorActions =
                new HashMap<FreeWord<E>, int[]>();
        
        for (final FreeWord<E> g: gens) {
            for (int exp = -1; exp <= 1; exp += 2) {
                final FreeWord<E> w = g.raisedTo(exp);
                final int map[] = new int[n];
                for (int i = 0; i < n; ++i) {
                    map[i] = old2new.get(action.apply(new2old.get(i), w));
                }
                generatorActions.put(w, map);
            }
        }
        
        return new GroupAction<E, Integer>() {
            public FpGroup<E> getGroup() {
                return action.getGroup();
            }

            public Iterator<Integer> domain() {
                return Iterators.range(0, n);
            }

            public int size() {
                return n;
            }

            public Integer apply(final Integer x, final FreeWord<E> w) {
                if (!isDefinedOn(x)) {
                    return null;
                } else {
                    int y = x;
                    for (int i = 0; i < w.length(); ++i) {
                        final FreeWord<E> g = w.subword(i, i + 1);
                        y = ((int[]) generatorActions.get(g))[y];
                    }
                    return y;
                }
            }

            public boolean isDefinedOn(final Integer x) {
                return x >= 0 && x < n;
            }
        };
    }
}

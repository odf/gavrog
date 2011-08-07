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

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;


/**
 * This class contains some utility methods for group actions.
 * 
 * @author Olaf Delgado
 * @version $Id: GroupActions.java,v 1.2 2005/07/18 23:33:29 odf Exp $
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
    public static GroupAction product(final GroupAction a, final GroupAction b) {
        if (!a.getGroup().equals(b.getGroup())) {
            throw new IllegalArgumentException("actions of different groups");
        }
        
        return new GroupAction() {
            public FpGroup getGroup() {
                return a.getGroup();
            }

            public Iterator domain() {
                return Iterators.cantorProduct(a.domain(), b.domain());
            }

            public int size() {
                return a.size() * b.size();
            }

            public Object apply(Object x, FreeWord w) {
                if (x instanceof Pair) {
                    final Pair pair = (Pair) x;
                    return new Pair(a.apply(pair.getFirst(), w),
                            b.apply(pair.getSecond(), w));
                } else {
                    throw new IllegalArgumentException("Pair expected");
                }
            }

            public boolean isDefinedOn(Object x) {
                if (x instanceof Pair) {
                    final Pair pair = (Pair) x;
                    return a.isDefinedOn(pair.getFirst())
                           && b.isDefinedOn(pair.getSecond());
                } else {
                    return false;
                }
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
    public static GroupAction cover(final GroupAction action) {
        final int n;
        
        try {
            n = action.size();
        } catch (UnsupportedOperationException ex) {
            final String s = "base action has no defined size";
            throw new UnsupportedOperationException(s);
        }

        return new GroupAction() {
            public FpGroup getGroup() {
                return action.getGroup();
            }

            public Iterator domain() {
                final Object dom[] = new Object[action.size()];
                final Iterator iter = action.domain();
                for (int i = 0; i < n; ++i) {
                    dom[i] = iter.next();
                }
                return Iterators.permutations(dom);
            }

            public int size() {
                int r = 1;
                for (int i = 2; i <= n; ++i) {
                    r *= i;
                }
                return r;
            }

            public Object apply(Object x, FreeWord w) {
                if (!isDefinedOn(x)) {
                    return null;
                } else {
                    final List a = (List) x;
                    final List result = new ArrayList();
                    for (int i = 0; i < n; ++i) {
                        result.add(action.apply(a.get(i), w));
                    }
                    return result;
                }
            }

            public boolean isDefinedOn(Object x) {
                if (!(x instanceof List)) {
                    return false;
                }
                final List a = (List) x;
                if (a.size() != n) {
                    return false;
                }
                final Set seen = new HashSet();
                for (int i = 0; i < a.size(); ++i) {
                    final Object y = a.get(i);
                    if (!action.isDefinedOn(y) || seen.contains(y)) {
                        return false;
                    }
                    seen.add(y);
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
    public static GroupAction orbit(final Object base, final GroupAction action) {
        try {
            action.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("action must be finite");
        }
        final List gens = action.getGroup().getGenerators();
        final LinkedList queue = new LinkedList();
        final Set domain = new HashSet();
        queue.addLast(base);
        domain.add(base);

        while (queue.size() > 0) {
            final Object next = queue.removeFirst();
            for (final Iterator iter = gens.iterator(); iter.hasNext();) {
                final FreeWord g = (FreeWord) iter.next();
                for (int i = -1; i <= 1; i += 2) {
                    final FreeWord w = g.raisedTo(i);
                    final Object neighbor = action.apply(next, w);
                    if (!domain.contains(neighbor)) {
                        domain.add(neighbor);
                        queue.addLast(neighbor);
                    }
                }
            }
        }
        
        return new GroupAction() {
            public FpGroup getGroup() {
                return action.getGroup();
            }

            public Iterator domain() {
                return domain.iterator();
            }

            public int size() {
                return domain.size();
            }

            public Object apply(Object x, FreeWord w) {
                if (!isDefinedOn(x)) {
                    return null;
                } else {
                    return action.apply(x, w);
                }
            }

            public boolean isDefinedOn(Object x) {
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
    public static GroupAction orbit(final GroupAction action) {
        final Object base = action.domain().next();
        return orbit(base, action);
    }
    
    /**
     * Constructs a flat (tabular) group action isomorphic to a given one.
     * 
     * @param action the original action.
     * @return the flat action.
     */
    public static GroupAction flat(final GroupAction action) {
        final int n;
        try {
            n = action.size();
        } catch (UnsupportedOperationException ex) {
            throw new UnsupportedOperationException("action must be finite");
        }
        
        final Map old2new = new HashMap();
        final Object new2old[] = new Object[n];
        int count = 0;
        for (final Iterator iter = action.domain(); iter.hasNext();) {
            final Object x = iter.next();
            old2new.put(x, new Integer(count));
            new2old[count] = x;
            ++count;
        }
        
        final List gens = action.getGroup().getGenerators();
        final Map generatorActions = new HashMap();
        
        for (final Iterator iter = gens.iterator(); iter.hasNext();) {
            final FreeWord g = (FreeWord) iter.next();
            for (int exp = -1; exp <= 1; exp += 2) {
                final FreeWord w = g.raisedTo(exp);
                final int map[] = new int[n];
                for (int i = 0; i < n; ++i) {
                    final Object img = old2new.get(action.apply(new2old[i], w));
                    map[i] = ((Integer) img).intValue();
                }
                generatorActions.put(w, map);
            }
        }
        
        return new GroupAction() {

            public FpGroup getGroup() {
                return action.getGroup();
            }

            public Iterator domain() {
                return Iterators.range(0, n);
            }

            public int size() {
                return n;
            }

            public Object apply(final Object x, final FreeWord w) {
                if (!isDefinedOn(x)) {
                    return null;
                } else {
                    int y = ((Integer) x).intValue();
                    for (int i = 0; i < w.length(); ++i) {
                        final FreeWord g = w.subword(i, i + 1);
                        y = ((int[]) generatorActions.get(g))[y];
                    }
                    return new Integer(y);
                }
            }

            public boolean isDefinedOn(Object x) {
                if (x instanceof Integer) {
                    final int i = ((Integer) x).intValue();
                    return i >= 0 && i < n;
                } else {
                    return false;
                }
            }
        };
    }
}

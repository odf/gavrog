/*
   Copyright 2006 Olaf Delgado-Friedrichs

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;

/**
 * @author Olaf Delgado
 * @version $Id: OrbifoldGraph.java,v 1.18 2007/04/26 20:21:58 odf Exp $
 */
public class OrbifoldGraph {
    final private String[] stabilizers;
    final private List edges;
	final private List[] neighbors;

    public OrbifoldGraph(final DelaneySymbol input) {
        final DSymbol ds = new DSymbol(input);
        
        // --- check argument
        if (ds.dim() != 3) {
            final String msg = "symbol must be 3-dimensional";
            throw new UnsupportedOperationException(msg);
        }
        if (!ds.isLocallyEuclidean3D()) {
            final String msg = "symbol must be locally euclidean";
            throw new UnsupportedOperationException(msg);
        }
        
        // --- initialize
        final int d = ds.dim();
        final Map edges = new HashMap();
        final Map orb2type = new HashMap();
        final Map orb2rep = new HashMap();
        final Map orb2elms = new HashMap();
        
        // --- process 0-dimensional orbits (chamber faces)
        for (int i = 0; i <= d; ++i) {
            for (final Iterator elms = ds.elements(); elms.hasNext();) {
                final Object D = elms.next();
                if (ds.op(i, D).equals(D)) {
                    final Pair orb = new Pair(new IndexList(i), D);
                    orb2type.put(orb, "1*");
                    edges.put(orb, new ArrayList());
                }
            }
        }
        
        // --- add 1-dimensional orbits (chamber edges)
        
        for (int i = 0; i < d; ++i) {
            final List ili = new IndexList(i);
            for (int j = i+1; j <= d; ++j) {
                final List ilj = new IndexList(j);
                final List idcs = new IndexList(i, j);
                for (final Iterator reps = ds.orbitReps(idcs); reps
                        .hasNext();) {
                    final Object D = reps.next();
                    
                    // --- find the 0-dim orbits of type "*" in this orbit
                    final List cuts = new ArrayList();
                    for (final Iterator iter = ds.orbit(idcs, D); iter
                            .hasNext();) {
                        final Object E = iter.next();
                        final Pair ci = new Pair(ili, E);
                        final Pair cj = new Pair(ilj, E);
                        if (orb2type.containsKey(ci)) {
                            cuts.add(ci);
                        }
                        if (orb2type.containsKey(cj)) {
                            cuts.add(cj);
                        }
                    }
                    
                    // --- determine the stabilizer type of this orbit
                    final String type;
                    final int v = ds.v(i, j, D);
                    if (cuts.size() > 0) {
                    	if (v > 1) {
                    		type = "*" + v + v;
                    	} else {
                    		type = "1*";
                    	}
                    } else {
                    	if (v > 1) {
                    		type = "" + v + v;
                    	} else {
                    		type = "";
                    	}
                    }
                    
                    final Pair orb = new Pair(idcs, D);
                    orb2elms.put(orb, Iterators.asList(ds.orbit(idcs, D)));
                    
                    // --- let this orbit be represented by element D
                    for (final Iterator iter = ds.orbit(idcs, D); iter
                            .hasNext();) {
                        orb2rep.put(new Pair(idcs, iter.next()), orb);
                    }
                                        
                    // --- store and link this orbit if stabilizer not trivial
                    if (type.length() > 0) {
                        orb2type.put(orb, type);
                        
                        if (cuts.size() > 0) {
                            final Pair ca = (Pair) cuts.get(0);
                            final Pair cb = (Pair) cuts.get(1);
                            ((List) edges.get(ca)).add(orb);
                            ((List) edges.get(cb)).add(orb);
                            edges.put(orb, cuts);
                        } else {
                            edges.put(orb, new ArrayList());
                        }
                    }
                }
            }
        }

        // --- add 2-dimensional orbits (chamber vertices)
        for (int i = 0; i <= 3; ++i) {
            final List idcs = new IndexList(ds);
            idcs.remove(new Integer(i));
            for (final Iterator reps = ds.orbitReps(idcs); reps
                    .hasNext();) {
                final List sub = Iterators.asList(ds.orbit(idcs, reps.next()));
                final List cones = new ArrayList();
                final List corners = new ArrayList();
                final List neighbors = new ArrayList();
                
                // --- collect all relevant 1-dim orbits this one contains
                for (int j = 0; j <= 2; ++j) {
                    final int n = ((Integer) idcs.get((j + 1) % 3)).intValue();
                    final int m = ((Integer) idcs.get((j + 2) % 3)).intValue();
                    final List ilnm = new IndexList(Math.min(n, m), Math.max(n,
                            m));
                    final Set seen = new HashSet();
                    for (final Iterator elms = sub.iterator(); elms.hasNext();) {
                        final Object D = elms.next();
                        if (!seen.contains(D)) {
                            final Pair orb0 = new Pair(ilnm, D);
                            final Pair orb = (Pair) orb2rep.get(orb0);    
                            final String t = (String) orb2type.get(orb);
                            seen.addAll((List) orb2elms.get(orb));
                            if (t != null) {
                                if (t.charAt(0) == '*') {
                                    corners.add(t.substring(1, 2));
                                } else if (t.charAt(0) != '1') {
                                    cones.add(t.substring(0, 1));
                                }
                                neighbors.add(orb);
                                seen.addAll((List) orb2elms.get(orb));
                            }
                        }
                    }
                }
                
                // --- do some sorting
                Collections.sort(cones);
                Collections.reverse(cones);
                Collections.sort(corners);
                Collections.reverse(corners);
                
                // --- assemble the string specifying the stabilizer type
                final Object D = sub.get(0);
                
                final StringBuffer buf = new StringBuffer(20);
                for (final Iterator iter = cones.iterator(); iter.hasNext();) {
                	buf.append(iter.next());
                }
                if (!ds.orbitIsLoopless(idcs, D)) {
                	buf.append('*');
                    for (final Iterator iter = corners.iterator(); iter
							.hasNext();) {
						buf.append(iter.next());
					}
                }
                if (!ds.orbitIsWeaklyOriented(idcs, D)) {
                	buf.append('x');
                }
                String type = buf.toString();
                if (type.equals("*") || type.equals("x")) {
                	type = "1" + type;
                }
                
                if (type.length() == 0) {
                	continue;
                }
                
                // --- store and link new orbit
                final Pair orb = new Pair(idcs, D);
                orb2type.put(orb, type);
                edges.put(orb, neighbors);
                for (final Iterator iter = neighbors.iterator(); iter.hasNext();) {
                	final Object n = iter.next();
                	((List) edges.get(n)).add(orb);
                }
            }
        }

        // --- sort orbits by types
        final List orbs = new ArrayList();
        orbs.addAll(orb2type.keySet());
        Collections.sort(orbs, new Comparator() {
            public int compare(final Object arg0, final Object arg1) {
                final String type0 = (String) orb2type.get(arg0);
                final String type1 = (String) orb2type.get(arg1);
                return type0.compareTo(type1);
            }
        });
        
        // --- determine equivalence classes of orbits
        final Partition p = new Partition();
        for (int i = 0; i < orbs.size(); ++i) {
            final Object orb = orbs.get(i);
            p.unite(orb, orb); //make sure the partition sees this orbit
            final String type = (String) orb2type.get(orb);
            final List neighbors = (List) edges.get(orb);
            for (final Iterator iter = neighbors.iterator(); iter.hasNext();) {
                final Object n = iter.next();
                if (type.equals(orb2type.get(n))) {
                    p.unite(orb, n);
                }
            }
        }
        final Map reps = p.representativeMap();
        
        // --- reduce equivalence classes to single nodes
        final Map orb2class = new HashMap();
        final Map class2nr = new HashMap();
        int nrOfClasses = 0;
        for (int i = 0; i < orbs.size(); ++i) {
            final Object orb = orbs.get(i);
            final Object cl = reps.get(orb);
            if (!class2nr.containsKey(cl)) {
                class2nr.put(cl, new Integer(nrOfClasses));
                ++nrOfClasses;
            }
            orb2class.put(orb, class2nr.get(cl));
        }
        
        // --- collect stabilizer types and adjacency matrix for new nodes
        final String class2type[] = new String[nrOfClasses];
        final boolean adj[][] = new boolean[nrOfClasses][nrOfClasses];
        for (final Iterator iter = orb2class.keySet().iterator(); iter
                .hasNext();) {
            final Object orb = iter.next();
            final int cl = ((Integer) orb2class.get(orb)).intValue();
            class2type[cl] = (String) orb2type.get(orb);
            final List neighbors = (List) edges.get(orb);
            for (final Iterator it2 = neighbors.iterator(); it2.hasNext();) {
                final int v = ((Integer) orb2class.get(it2.next())).intValue();
                adj[cl][v] = true;
                adj[v][cl] = true;
            }
        }
        
        // --- remove redundant edges
        for (int i = 0; i < nrOfClasses; ++i) {
        	if (class2type[i].equals("1*")) {
        		for (int j = 0; j < nrOfClasses; ++j) {
        			final String t = class2type[j];
        			if (t.length() == 4 || t.charAt(0) != '*') {
        				adj[i][j] = false;
        				adj[j][i] = false;
        			}
        		}
        	}
        }
        
        // --- turn adjacency matrix into edge list
        final List edgeList = new ArrayList();
        final List neighbors[] = new List[nrOfClasses];
        for (int i = 0; i < nrOfClasses; ++i) {
        	neighbors[i] = new ArrayList();
        }
        for (int i = 0; i < nrOfClasses; ++i) {
            for (int j = i+1; j < nrOfClasses; ++j) {
                if (adj[i][j]) {
                    edgeList.add(new int[] {i, j});
                    neighbors[i].add(new Integer(j));
                    neighbors[j].add(new Integer(i));
                }
            }
        }
        
        // --- store results
        this.stabilizers = class2type;
        this.edges = edgeList;
        this.neighbors = neighbors;
    }

    public String[] getStabilizers() {
        return this.stabilizers;
    }
    
    public List getEdges() {
        return this.edges;
    }

    public List[] getNeighbors() {
		return this.neighbors;
	}

    public static void main(final String args[]) {
    	final String code = ""
    		+ "36 3:1 2 3 5 6 8 9 11 12 13 14 15 16 17 18 19 20 21 23 24 25 26 "
    		+ "27 28 29 30 31 32 33 34 35 36,"
    		+ "2 5 12 8 13 11 27 33 16 18 20 23 26 25 30 36 32 35,"
    		+ "3 6 7 8 31 11 19 20 16 18 33 24 23 26 32 34 30 36,"
    		+ "18 17 26 23 22 9 10 11 21 27 36 25 24 35 34 32 33 31:"
    		+ "4 4 4 4 8 4 4 4 4 4 6 8 6 4,3 3 3 3 3 3,3 4 4 4 3";
    	final DSymbol ds = new DSymbol(code);
        final OrbifoldGraph og = new OrbifoldGraph(ds);
        final String stabs[] = og.getStabilizers();
        System.out.println(stabs.length + " nodes");
        System.out.println(Arrays.asList(stabs));
        System.out.println(og.getEdges().size() + " edges");
        for (final Iterator iter = og.getEdges().iterator(); iter.hasNext();) {
            final int e[] = (int[]) iter.next();
            System.out.println(e[0] + "(" + stabs[e[0]] + ") <-> " + e[1] + "("
                    + stabs[e[1]] + ")");
        }
        System.out.println(Arrays.asList(og.getNeighbors()));
    }
}

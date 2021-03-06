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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
*/
public class OrbifoldGraph {
    private final class Orbit extends Pair<IndexList, Integer>
    {
        public Orbit(final IndexList idcs, final int elm)
        {
            super(idcs, elm);
        }
    }
    
    final private String[] stabilizers;
    final private List<int[]> edges;
	final private List<List<Integer>> neighbors;

    public <T> OrbifoldGraph(final DelaneySymbol<T> input) {
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
        final Map<Orbit, List<Orbit>> edges = new HashMap<Orbit, List<Orbit>>();
        final Map<Orbit, String> orb2type = new HashMap<Orbit, String>();
        final Map<Orbit, Orbit> orb2rep = new HashMap<Orbit, Orbit>();
        final Map<Orbit, List<Integer>> orb2elms =
                new HashMap<Orbit, List<Integer>>();
        
        // --- process 0-dimensional orbits (chamber faces)
        for (int i = 0; i <= d; ++i) {
            for (final int D: ds.elements()) {
                if (ds.op(i, D).equals(D)) {
                    final Orbit orb = new Orbit(new IndexList(i), D);
                    orb2type.put(orb, "1*");
                    edges.put(orb, new ArrayList<Orbit>());
                }
            }
        }
        
        // --- add 1-dimensional orbits (chamber edges)
        
        for (int i = 0; i < d; ++i) {
            final IndexList ili = new IndexList(i);
            for (int j = i+1; j <= d; ++j) {
                final IndexList ilj = new IndexList(j);
                final IndexList idcs = new IndexList(i, j);
                for (final int D: ds.orbitReps(idcs)) {
                    // --- find the 0-dim orbits of type "*" in this orbit
                    final List<Orbit> cuts = new ArrayList<Orbit>();
                    for (final int E: ds.orbit(idcs, D)) {
                        final Orbit ci = new Orbit(ili, E);
                        final Orbit cj = new Orbit(ilj, E);
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
                    
                    final Orbit orb = new Orbit(idcs, D);
                    orb2elms.put(orb, Iterators.asList(ds.orbit(idcs, D)));
                    
                    // --- let this orbit be represented by element D
                    for (final int E: ds.orbit(idcs, D)) {
                        orb2rep.put(new Orbit(idcs, E), orb);
                    }
                                        
                    // --- store and link this orbit if stabilizer not trivial
                    if (type.length() > 0) {
                        orb2type.put(orb, type);
                        
                        if (cuts.size() > 0) {
                            final Orbit ca = cuts.get(0);
                            final Orbit cb = cuts.get(1);
                            edges.get(ca).add(orb);
                            edges.get(cb).add(orb);
                            edges.put(orb, cuts);
                        } else {
                            edges.put(orb, new ArrayList<Orbit>());
                        }
                    }
                }
            }
        }

        // --- add 2-dimensional orbits (chamber vertices)
        for (int i = 0; i <= 3; ++i) {
            final IndexList idcs = new IndexList(ds);
            idcs.remove(new Integer(i));
            for (final int E: ds.orbitReps(idcs)) {
                final List<Integer> sub = Iterators.asList(ds.orbit(idcs, E));
                final List<String> cones = new ArrayList<String>();
                final List<String> corners = new ArrayList<String>();
                final List<Orbit> neighbors = new ArrayList<Orbit>();
                
                // --- collect all relevant 1-dim orbits this one contains
                for (int j = 0; j <= 2; ++j) {
                    final int n = idcs.get((j + 1) % 3);
                    final int m = idcs.get((j + 2) % 3);
                    final IndexList ilnm =
                            new IndexList(Math.min(n, m), Math.max(n, m));
                    final Set<Integer> seen = new HashSet<Integer>();
                    for (final int D: sub) {
                        if (!seen.contains(D)) {
                            final Orbit orb = orb2rep.get(new Orbit(ilnm, D));
                            final String t = orb2type.get(orb);
                            seen.addAll(orb2elms.get(orb));
                            if (t != null) {
                                if (t.charAt(0) == '*') {
                                    corners.add(t.substring(1, 2));
                                } else if (t.charAt(0) != '1') {
                                    cones.add(t.substring(0, 1));
                                }
                                neighbors.add(orb);
                                seen.addAll(orb2elms.get(orb));
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
                final int D = sub.get(0);
                
                final StringBuffer buf = new StringBuffer(20);
                for (final String s: cones) {
                	buf.append(s);
                }
                if (!ds.orbitIsLoopless(idcs, D)) {
                	buf.append('*');
                    for (final String s: corners) {
						buf.append(s);
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
                final Orbit orb = new Orbit(idcs, D);
                orb2type.put(orb, type);
                edges.put(orb, neighbors);
                for (final Orbit n: neighbors) {
                	edges.get(n).add(orb);
                }
            }
        }

        // --- sort orbits by types
        final List<Orbit> orbs = new ArrayList<Orbit>();
        orbs.addAll(orb2type.keySet());
        Collections.sort(orbs, new Comparator<Orbit>() {
            public int compare(final Orbit arg0, final Orbit arg1) {
                final String type0 = orb2type.get(arg0);
                final String type1 = orb2type.get(arg1);
                return type0.compareTo(type1);
            }
        });
        
        // --- determine equivalence classes of orbits
        final Partition<Orbit> p = new Partition<Orbit>();
        for (final Orbit orb: orbs) {
            p.unite(orb, orb); //make sure the partition sees this orbit
            final String type = orb2type.get(orb);
            for (final Orbit n: edges.get(orb)) {
                if (type.equals(orb2type.get(n))) {
                    p.unite(orb, n);
                }
            }
        }
        final Map<Orbit, Orbit> reps = p.representativeMap();
        
        // --- reduce equivalence classes to single nodes
        final Map<Orbit, Integer> orb2class = new HashMap<Orbit, Integer>();
        final Map<Orbit, Integer> class2nr = new HashMap<Orbit, Integer>();
        int nrOfClasses = 0;
        for (final Orbit orb: orbs) {
            final Orbit cl = reps.get(orb);
            if (!class2nr.containsKey(cl)) {
                class2nr.put(cl, nrOfClasses);
                ++nrOfClasses;
            }
            orb2class.put(orb, class2nr.get(cl));
        }
        
        // --- collect stabilizer types and adjacency matrix for new nodes
        final String class2type[] = new String[nrOfClasses];
        final boolean adj[][] = new boolean[nrOfClasses][nrOfClasses];
        for (final Orbit orb: orb2class.keySet()) {
            final int cl = orb2class.get(orb);
            class2type[cl] = orb2type.get(orb);
            for (final Orbit n: edges.get(orb)) {
                final int v = orb2class.get(n);
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
        final List<int[]> edgeList = new ArrayList<int[]>();
        final List<List<Integer>> neighbors = new LinkedList<List<Integer>>();
        for (int i = 0; i < nrOfClasses; ++i) {
        	neighbors.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < nrOfClasses; ++i) {
            for (int j = i+1; j < nrOfClasses; ++j) {
                if (adj[i][j]) {
                    edgeList.add(new int[] {i, j});
                    neighbors.get(i).add(j);
                    neighbors.get(j).add(i);
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
    
    public List<int[]> getEdges() {
        return this.edges;
    }

    public List<List<Integer>> getNeighbors() {
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
        for (final int e[]: og.getEdges()) {
            System.out.println(e[0] + "(" + stabs[e[0]] + ") <-> " + e[1] + "("
                    + stabs[e[1]] + ")");
        }
        for (final List<Integer> list: og.getNeighbors())
        {
            System.out.println(list);
        }
    }
}

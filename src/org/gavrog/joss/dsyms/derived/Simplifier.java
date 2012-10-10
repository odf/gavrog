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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Traversal;


/**
 * Takes a {@link DSymbol}and produces a "simpler" (typically smaller) symbol
 * with the same topological realization or one that is derived from the
 * topological realization of the input symbol by a sequence of handle
 * reductions. Currently, this is only implemented for 3-dimensional, oriented
 * symbols with trivial branching. Handle reductions are currently not reported,
 * but can be detected by comparing the abelian invariants of the fundamental
 * groups.
 */
public class Simplifier {
    private static int LOGGING_LEVEL = 0;
    
    // --- The input symbol.
    final DSymbol originalSymbol;
    // --- The current symbol.
    DynamicDSymbol ds;
    // --- The final result.
    DSymbol simplifiedSymbol = null;
    
    /**
     * Constructs an instance.
     * 
     * @param symbol the input symbol.
     */
    public <T> Simplifier(final DelaneySymbol<T> inputSymbol) {
        if (LOGGING_LEVEL > 0) {
            System.out.println("Simplifyer: constructor called"
                    + " on symbol of size " + inputSymbol.size());
        }
        if (inputSymbol == null) {
            throw new IllegalArgumentException("null pointer received");
        }
        if (inputSymbol.dim() != 3) {
            final String s = "symbol must be 3-dimensional";
            throw new UnsupportedOperationException(s);
        }

        final DSymbol symbol = new DSymbol(inputSymbol);

        for (final int i: symbol.indices()) {
            for (final int D: symbol.elements()) {
                if (!symbol.definesOp(i, D)) {
                    final String s = "symbol must define all neighbors";
                    throw new UnsupportedOperationException(s);
                }
            }
        }
        if (!symbol.isOriented()) {
            throw new UnsupportedOperationException("symbol must be oriented");
        }

        for (int i = 0; i < symbol.dim() - 1; ++i) {
            for (int j = i + 2; j <= symbol.dim(); ++j) {
                for (final int D: symbol.orbitReps(new IndexList(i, j))) {
                    final int E1 = symbol.op(j, symbol.op(i, D));
                    final int E2 = symbol.op(i, symbol.op(j, D));
                    if (E1 != E2) {
                        final String s = "symbol has oversized (" + i + "," + j
                                         + ") orbit";
                        throw new IllegalArgumentException(s);
                    }
                }
            }
        }
        for (int i = 0; i < symbol.dim(); ++i) {
            for (final int D: symbol.orbitReps(new IndexList(i, i + 1))) {
                if (symbol.v(i, i + 1, D) != 1) {
                    final String s = "symbol must be trivially branched";
                    throw new UnsupportedOperationException(s);
                }
            }
        }
        
        this.originalSymbol = (DSymbol) symbol;
    }
    
    /**
     * Retrieves the simplified version of the input symbol.
     * 
     * @return a simpler symbol.
     */
    public DelaneySymbol<Integer> getSimplifiedSymbol() {
        if (simplifiedSymbol == null) {
            compute();
        }
        return simplifiedSymbol;
    }

    /**
     * Does all the work.
     */
    private void compute() {
        if (LOGGING_LEVEL > 0) {
            System.out.println("Simplifyer: starting");
        }
        ds = new DynamicDSymbol(originalSymbol);
        
        if (countOrbits(1, 2, 3) < countOrbits(0, 1, 2)) {
            if (LOGGING_LEVEL > 0) {
                log("dualizing");
            }
            ds = ds.dual();
        }

        final List<Integer> disposable = new LinkedList<Integer>();
        IndexList idcs;
        boolean dualized = false;
        boolean modified = true;
        
        while (modified) {
            modified = false;
            
            // --- merge volumes and remove intruding faces
            if (countOrbits(0, 1, 2) > 1 || hasSize2Orbit(2, 3)) {
                disposable.clear();
                for (final DSPair<Integer> e:
                    new FundamentalEdges<Integer>(ds))
                {
                    final int i = e.getIndex();
                    if (i == 3) {
                        final int D = e.getElement();
                        disposable.add(D);
                        disposable.add(ds.op(3, D));
                    }
                }
                if (disposable.size() > 0) {
                    if (LOGGING_LEVEL > 0) {
                        log("merging volumes and removing intruding faces");
                    }
                    ds.collapse(disposable, 3);
                    modified = true;
                }
            }
            
            // --- remove edges of degree 2
            disposable.clear();
            idcs = new IndexList(2, 3);
            for (final int D: ds.orbitReps(idcs)) {
                if (ds.r(2, 3, D) == 2) {
                    disposable.add(D);
                    disposable.add(ds.op(2, D));
                    disposable.add(ds.op(3, D));
                    disposable.add(ds.op(3, ds.op(2, D)));
                }
            }
            if (disposable.size() > 0) {
                if (LOGGING_LEVEL > 0) {
                    log("removing degree 2 edges");
                }
                ds.collapse(disposable, 2);
                modified = true;
            }
            
            // --- contract edges
            if (countOrbits(1, 2, 3) > 1 || hasSize2Orbit(0, 1)) {
                disposable.clear();
                idcs = new IndexList(ds);
                Collections.reverse(idcs);
                final Traversal<Integer> trav =
                        new Traversal<Integer>(ds, idcs, ds.elements());
                for (final DSPair<Integer> e:
                    new FundamentalEdges<Integer>(ds, trav))
                {
                    final int i = e.getIndex();
                    if (i == 0) {
                        final int D = e.getElement();
                        disposable.add(D);
                        disposable.add(ds.op(0, D));
                    }
                }
                if (disposable.size() > 0) {
                    if (LOGGING_LEVEL > 0) {
                        log("contracting edges");
                    }
                    ds.collapse(disposable, 0);
                    modified = true;
                }
            }
            
            // --- remove faces of degree 2
            disposable.clear();
            idcs = new IndexList(0, 1);
            for (final int D: ds.orbitReps(idcs)) {
                if (ds.r(0, 1, D) == 2) {
                    disposable.add(D);
                    disposable.add(ds.op(0, D));
                    disposable.add(ds.op(1, D));
                    disposable.add(ds.op(1, ds.op(0, D)));
                }
            }
            if (disposable.size() > 0) {
                if (LOGGING_LEVEL > 0) {
                    log("removing degree 2 faces");
                }
                ds.collapse(disposable, 1);
                modified = true;
            }
            
            // --- remove vertex of local (i.e., 2d) degree 1
            if (!modified) {
                for (final int C: ds.orbitReps(new IndexList(1, 2))) {
                    if (ds.m(1, 2, C) == 1) {
                        if (LOGGING_LEVEL > 0) {
                            log("removing local degree one vertex");
                        }

                        final int D  = ds.op(0, ds.op(1, C));
                        final int E  = ds.op(1, ds.op(0, C));
                        final int F  = ds.op(3, D);
                        final int G  = ds.op(3, E);

                        final int D1 = ds.op(1, D);
                        final int E1 = ds.op(1, E);
                        final int F1 = ds.op(1, F);
                        final int G1 = ds.op(1, G);

                        ds.redefineOp(1, D, E1);
                        ds.redefineOp(1, E, D1);
                        ds.redefineOp(1, F, G1);
                        ds.redefineOp(1, G, F1);

                        disposable.clear();
                        Iterators.addAll(disposable,
                                ds.orbit(new IndexList(0, 1, 3), C));
                        ds.collapse(disposable, 3);

                        modified = true;
                        break;
                    }
                }
            }
            
            // --- remove vertex of local (i.e., 2d) degree 2
            if (!modified) {
                idcs = new IndexList(1, 2);
                for (final int D: ds.orbitReps(idcs)) {
                    if (ds.r(1, 2, D) == 2) {
                        int E = ds.op(3, ds.op(2, D));
                        if (D == E
                        		|| D == ds.op(1, ds.op(0, E))
                                || D == ds.op(0, ds.op(1, E))) {
                            continue;
                        }
                        if (LOGGING_LEVEL > 0) {
                            log("removing vertex of local degree 2");
                        }

                        E = ds.op(2, ds.op(1, D));
                        if (ds.r(0, 1, D) > 3) {
                            cutFace3D(ds.op(0, D), ds.op(0, ds.op(1, D)));
                        }
                        if (ds.r(0, 1, E) > 3) {
                            cutFace3D(ds.op(0, E), ds.op(0, ds.op(1, E)));
                        }
                        squeezeTile3D(ds.op(1, ds.op(0, D)),
                                ds.op(1, ds.op(0, E)));
                        
                        disposable.clear();
                        Iterators.addAll(disposable,
                                ds.orbit(new IndexList(0, 1, 3), D));
                        ds.collapse(disposable, 3);
                        modified = true;
                        break;
                    }
                }
            }
            
            // --- remove non-disk face
            if (!modified) {
                // --- assign to each chamber its face and vertex
                final Map<Integer, Integer> face2rep =
                		new HashMap<Integer, Integer>();
                final Map<Integer, Integer> vert2rep =
                		new HashMap<Integer, Integer>();
                
                final IndexList idcsFace = new IndexList(0, 1);
                for (final int D: ds.orbitReps(idcsFace)) {
                    final Iterator<Integer> orb = ds.orbit(idcsFace, D);
                    while (orb.hasNext()) {
                        face2rep.put(orb.next(), D);
                    }
                }
                
                final IndexList idcsVert = new IndexList(1, 2);
                for (final int D: ds.orbitReps(idcsVert)) {
                    final Iterator<Integer> orb = ds.orbit(idcsVert, D);
                    while (orb.hasNext()) {
                        vert2rep.put(orb.next(), D);
                    }
                }
                
                // --- maps face,vertex pairs to their first seen chamber
                final Map<Pair<Integer, Integer>, Integer> sig2chamber =
                		new HashMap<Pair<Integer, Integer>, Integer>();
                
                // --- loop through positive representatives for 1-orbits
                final Map<Integer, Integer> sign = ds.partialOrientation();
                for (final int D: ds.elements()) {
                    if (sign.get(D) < 0) {
                        continue;
                    }
                    final Pair<Integer, Integer> signature =
                        new Pair<Integer, Integer>(
                                face2rep.get(D), vert2rep.get(D));
                    final Integer E = sig2chamber.get(signature);
                    
                    if (E == null) {
                        sig2chamber.put(signature, D);
                    } else {
                        // --- we have seen the same signature twice here
                        if (LOGGING_LEVEL > 0) {
                            log("removing non-cellular face");
                        }

                        final int F  = ds.op(3, D);
                        final int G  = ds.op(3, E);

                        final int D1 = ds.op(1, D);
                        final int E1 = ds.op(1, E);
                        final int F1 = ds.op(1, F);
                        final int G1 = ds.op(1, G);

                        ds.redefineOp(1, D, E1);
                        ds.redefineOp(1, E, D1);
                        ds.redefineOp(1, F, G1);
                        ds.redefineOp(1, G, F1);

                        modified = true;
                        break;
                    }
                }
            }
            
            // --- remove disconnected face intersection
            if (!modified) {
                final int cut[] = firstLiftableTwoCut();
                if (cut != null) {
                    if (LOGGING_LEVEL > 0) {
                        log("removing disconnected face intersection");
                    }
                    final int D[] = new int[2];
                    for (int i = 0; i < 2; ++i) {
                        final int A = cut[2*i];
                        final int B = cut[2*i+1];
                        if (ds.op(0, A).equals(B)) {
                            D[i] = ds.op(2, A);
                        } else {
                            if (!ds.op(1, ds.op(0, ds.op(1, A))).equals(B)) {
                                cutFace3D(A, B);
                            }
                            D[i] = ds.op(1, A);
                        }
                    }
                    squeezeTile3D(D[0], D[1]);
                    modified = true;
                }
            }
                    
            // --- dualize and try further simplifications
            if (modified) {
                dualized = false;
            } else if (!dualized) {
                if (LOGGING_LEVEL > 0) {
                    log("dualizing");
                }
                ds = ds.dual();
                dualized = modified = true;
            }
        }
        
        if (LOGGING_LEVEL > 0) {
            log("exporting");
        }
        this.simplifiedSymbol = new DSymbol(ds);
        
        if (LOGGING_LEVEL > 0) {
            System.out.println("Simplifyer: finished");
            if (LOGGING_LEVEL >= 2) {
                System.out.println("  final result: " + simplifiedSymbol);
            }
        }
    }
    
    /**
     * Creates a log entry.
     * 
     * @param action describes the next action.
     */
    private void log(String action) {
        if (LOGGING_LEVEL >= 2) {
            int kfComplexity = 0;
            for (final int D: ds.orbitReps(new IndexList(0, 1, 3))) {
                kfComplexity += ds.r(0, 1, D) - 2;
            }
            System.out.println("  Symbol of size " + ds.size()
                               + " and kf-complexity " + kfComplexity);
            System.out.println(" with    " 
                    + countComponents() + " components, "
                    + countOrbits(0, 1, 2) + " tiles, "
                    + countOrbits(0, 1, 3) + " faces, "
                    + countOrbits(0, 2, 3) + " edges, "
                    + countOrbits(1, 2, 3) + " vertices.");
        }
        if (LOGGING_LEVEL >= 3) {
            System.out.println("    " + new DSymbol(ds));
        }
        if (LOGGING_LEVEL >= 1) {
            System.out.println("    " + action + "...");
        }
    }

    /**
     * Returns the number of orbits for a specific triplet of indices.
     * 
     * @param i the first index.
     * @param j the second index.
     * @param k the third index.
     * @return the number of (i,j,k)-orbits.
     */
    private int countOrbits(final int i, final int j, final int k) {
        return Iterators.size(ds.orbitReps(new IndexList(i, j, k)));
    }

    /**
     * Returns the number of connected components.
     * @return the number of connected components of the current symbol.
     */
    private int countComponents() {
        return Iterators.size(ds.orbitReps(new IndexList(ds)));
    }
    
    /**
     * Returns true if current symbol has a 2-element orbit with the given index
     * pair.
     * 
     * @param i the first index.
     * @param j the second index.
     * @return true if a 2-element orbit exists.
     */
    private boolean hasSize2Orbit(final int i, final int j) {
        for (final int D: ds.orbitReps(new IndexList(i, j))) {
            if (ds.op(i, D).equals(ds.op(j, D))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Cuts a face and inserts a new edge between vertices specified by two
     * symbol elements. These elements must be in a common (0,1)-orbit.
     * 
     * @param D1 the element specifying the first vertex.
     * @param D2 the element specifying the second vertex.
     */
    private void cutFace3D(final int D1, int D2) {
        // --- check the arguments and modify D1, if necessary
        
        if (D1 == D2 || D1 == ds.op(1, D2)) {
            throw new IllegalArgumentException("vertices not distinct");
        }
        
        int E = D1;
        while (true) {
            E = ds.op(0, E);
            if (E == D2) {
                break;
            }
            E = ds.op(1, E);
            if (E == D2) {
                D2 = ds.op(1, D2);
                break;
            }
            if (E == D1) {
                throw new IllegalArgumentException("vertices not in a common face");
            }
        }
        
        // --- create new symbol elements
        final Integer[] nu = ds.grow(8).toArray(new Integer[0]);
        
        // --- establish trivial branching for the new elements
        for (int k = 0; k < 8; ++k) {
            final int D = nu[k];
            for (int i = 0; i <= 2; ++i) {
                ds.redefineV(i, i + 1, D, 1);
            }
        }

        // --- connect the new elements with each other
        ds.redefineOp(0, nu[0], nu[1]);
        ds.redefineOp(0, nu[2], nu[3]);
        ds.redefineOp(0, nu[4], nu[5]);
        ds.redefineOp(0, nu[6], nu[7]);
        
        ds.redefineOp(3, nu[1], nu[2]);
        ds.redefineOp(3, nu[3], nu[0]);
        ds.redefineOp(3, nu[5], nu[6]);
        ds.redefineOp(3, nu[7], nu[4]);
        
        ds.redefineOp(2, nu[0], nu[4]);
        ds.redefineOp(2, nu[1], nu[5]);
        ds.redefineOp(2, nu[2], nu[6]);
        ds.redefineOp(2, nu[3], nu[7]);
        
        // --- connect the new elements with old ones
        final int old[] = new int[] {
                D1, D2,
                ds.op(3, D2), ds.op(3, D1),
                ds.op(1, D1), ds.op(1, D2),
                ds.op(3, ds.op(1, D2)), ds.op(3, ds.op(1, D1))
        };
        for (int i = 0; i < 8; ++i) {
            ds.redefineOp(1, nu[i], old[i]);
        }
    }
    
    /**
     * Takes two edges between the same vertices in the same tile and turns them
     * into a single edge, thereby effectively squeezing the tile until it is
     * split into two parts.
     * 
     * @param D specifies the first edge.
     * @param E specifies the second edge.
     */
    private void squeezeTile3D(final int D, final int E) {
        // TODO add a parameter check here
        final int d = ds.op(0, E);
        final int e = ds.op(0, D);
        final int d2 = ds.op(2, d);
        final int e2 = ds.op(2, e);
        final int E2 = ds.op(2, E);
        final int D2 = ds.op(2, D);
        
        ds.redefineOp(2, d, D);
        ds.redefineOp(2, e, E);
        ds.redefineOp(2, d2, D2);
        ds.redefineOp(2, e2, E2);
    }
    
    /**
     * Determines if the symbol contains a pair of faces of the same tile with a
     * disconnected intersection. Such a situation is only reported if it is
     * "liftable" in the sense that the simplification algorithm is able to deal
     * with it. This is the case unless the faces in question are mapped to each
     * other by the sigma-3 operation in a certain manner.
     * 
     * @return a quartet of chambers specifying the first liftable disconnected
     *         intersection found, if any.
     */
    private int[] firstLiftableTwoCut() {
        final IndexList idcsFace = new IndexList(0, 1);
        final IndexList idcsVertex = new IndexList(1, 2);
        final int n = ds.numberOfOrbits(idcsFace);
        
        // --- collect the faces of the symbol
        final List<Set<Integer>> faces = new ArrayList<Set<Integer>>();
        final Iterator<Integer> faceReps = ds.orbitReps(idcsFace);
        for (int i = 0; i < n; ++i) {
            final int D = faceReps.next();
            final Set<Integer> f = new HashSet<Integer>();
            final Iterator<Integer> iter = ds.orbit(idcsFace, D);
            while (iter.hasNext()) {
                f.add(iter.next());
            }
            faces.add(f);
        }
        
        // --- loop through the faces
        for (int i = 0; i < n-1; ++i) {
            // --- the current first face
            final Set<Integer> f1 = faces.get(i);
            
            // --- Mark the chambers at all vertices incident to this face,
            //     but only those not adjacent to it.
            final Set<Integer> marked = new HashSet<Integer>();
            for (final int D: f1) {
                for (final int E: ds.orbit(idcsVertex, D)) {
                    if (!f1.contains(ds.op(2, E))) {
                        marked.add(E);
                    }
                }
            }
            
            // --- loop through the faces with higher index
            for (int j = i+1; j < n; ++j) {
                // --- the current second face
                final Set<Integer> f2 = faces.get(j);
                
                // --- find marked chambers in this face
                final Set<Integer> intersection = new HashSet<Integer>();
                for (final int D: f2) {
                    if (marked.contains(D)) {
                        intersection.add(D);
                    }
                }
                
                // --- see if we have found a 2-cut
                if (intersection.size() < 4) {
                    continue;
                }
                // TODO make sure we don't actually have a 1-cut
                           
                // --- determine the characterizing chambers
                int D1 = 0;
                final Iterator<Integer> iter = intersection.iterator();
                while (iter.hasNext()) {
                    D1 = iter.next();
                    if (!intersection.contains(ds.op(0, D1))) {
                        break;
                    }
                }
                int D2 = ds.op(0, D1);
                while (!intersection.contains(D2)) {
                    D2 = ds.op(0, ds.op(1, D2));
                }
                int D3 = ds.op(2, D2);
                while (!f1.contains(D3)) {
                    D3 = ds.op(2, ds.op(1, D3));
                }
                int D4 = ds.op(2, D1);
                while (!f1.contains(D4)) {
                    D4 = ds.op(2, ds.op(1, D4));
                }
                
                // --- see if this 2-cut is liftable
                int E = D4;
                int count = 0;
                while (!ds.op(0, E).equals(D3)) {
                    E = ds.op(1, ds.op(0, E));
                    final int E3 = ds.op(3, E);
                    if (E3 == D1 || E3 == ds.op(1, D2)) {
                        ++count;
                    }
                }
                if (count == 1) {
                    // --- nope, not liftable
                    continue;
                }

                // --- return the chamber quartet for the cut
                return new int[] { D1, D2, D3, D4 };
            }
        }
        
        // --- nothing found
        return null;
    }
}

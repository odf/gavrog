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

package org.gavrog.joss.pgraphs.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;

/**
 * Represents a cover of a periodic graph.
 */
public class Cover extends PeriodicGraph {
    final private Morphism coverMorphism;
    final private PeriodicGraph image;
    
    /**
     * Constructs an instance.
     * 
     * @param image the image graph.
     * @param cell the unit cell for the cover, in terms of the original unit cell.
     */
    public Cover(final PeriodicGraph image, final Vector cell[]) {
        super(image.getDimension());
        
        // --- check the arguments
        if (cell == null) {
            throw new IllegalArgumentException("No cell given.");
        }
        final int dim = image.getDimension();
        if (cell.length != dim) {
            throw new IllegalArgumentException("Expected exactly " + dim
                    + " cell vectors.");
        }
        for (int i = 0; i < cell.length; ++i) {
            if (!cell[i].isIntegral()) {
                throw new IllegalArgumentException("Cell vectors must be integral.");
            }
            if (cell[i].getDimension() != dim) {
                throw new IllegalArgumentException(
                        "Cell vectors must have same dimension as base graph.");
            }
        }
        
        // --- compute transformation to new coordinates 
        final Matrix M = Vector.toMatrix(cell);
        if (M.rank() < dim) {
            throw new IllegalArgumentException("Cell vectors must form a basis.");
        }
        final CoordinateChange C = new CoordinateChange(M);
        
        // --- find translation representatives modulo the new unit lattice
        final Set<Vector> translations = new HashSet<Vector>();
        final int d = getDimension();
        for (int i = 0; i < d; ++i) {
            final Vector e = Vector.unit(d, i);
            final Vector b = ((Vector) e.times(C)).modZ();
            if (!translations.contains(b)) {
                translations.add(b);
            }
        }
        final LinkedList<Vector> Q = new LinkedList<Vector>();
        Q.addAll(translations);
        final Set<Vector> gens = new HashSet<Vector>();
        gens.addAll(translations);
        while (Q.size() > 0) {
            final Vector s = (Vector) Q.removeFirst();
            for (final Vector t: gens) {
                final Vector sum = ((Vector) s.plus(t)).modZ();
                if (!translations.contains(sum)) {
                    translations.add(sum);
                    Q.addFirst(sum);
                }
            }
        }

        // --- find node and edge representatives in the new coordinate system
        final Map<INode, Point> pos = image.barycentricPlacement();
        final List<Pair<INode, Point>> transformedNodes =
                new ArrayList<Pair<INode, Point>>();
        for (final INode v: image.nodes()) {
            transformedNodes.add(
                    new Pair<INode, Point>(v, (Point) pos.get(v).times(C)));
        }
        final Map<IEdge, Pair<Point, Point>> transformedEdges =
                new HashMap<IEdge, Pair<Point, Point>>();
        for (final IEdge e: image.edges()) {
            final Point p = pos.get(e.source());
            final Point q = pos.get(e.target());
            final Point src = (Point) p.times(C);
            final Point dst = (Point) q.plus(image.getShift(e)).times(C);
            transformedEdges.put(e, new Pair<Point, Point>(src, dst));
        }

        // --- extend the system of representatives to the new unit cell
        final List<Pair<INode, Point>> coverNodes =
                new ArrayList<Pair<INode, Point>>();
        
        for (final Pair<INode, Point> vp: transformedNodes) {
            final INode v = vp.getFirst();
            final Point p = vp.getSecond();
            for (final Vector s: translations) {
                final Point pv = (Point) p.plus(s);
                coverNodes.add(new Pair<INode, Point>(v, pv.modZ()));
            }
        }
        
        final List<Pair<IEdge, Pair<Point, Point>>> coverEdges =
                new ArrayList<Pair<IEdge, Pair<Point, Point>>>();
        for (final IEdge e: transformedEdges.keySet()) {
            final Pair<Point, Point> pq = transformedEdges.get(e);
            final Point p = pq.getFirst();
            final Point q = pq.getSecond();
            for (final Vector v: translations) {
                final Point pv = (Point) p.plus(v);
                final Point qv = (Point) q.plus(v);
                final Point rv = pv.modZ();
                final Point sv = (Point) qv.minus(pv).plus(rv);
                coverEdges.add(new Pair<IEdge, Pair<Point, Point>>(
                        e, new Pair<Point, Point>(rv, sv)));
            }
        }

        // --- extract a representation of the new graph
        final Map<Pair<INode, Point>, INode> pos2node =
                new HashMap<Pair<INode, Point>, INode>();
        final Map<INode, Point> node2pos = new HashMap<INode, Point>();
        for (final Pair<INode, Point> vp: coverNodes) {
            final INode w = newNode();
            pos2node.put(vp, w);
            node2pos.put(w, vp.getSecond());
        }
        for (final Pair<IEdge, Pair<Point, Point>> epq: coverEdges) {
            final IEdge e = epq.getFirst();
            final Pair<Point, Point> pq = epq.getSecond();
            final Point p = pq.getFirst();
            final Point q = pq.getSecond();
            final Point r = q.modZ();
            final Vector s = (Vector) q.minus(r);
            final INode v = pos2node.get(new Pair<INode, Point>(e.source(), p));
            final INode w = pos2node.get(new Pair<INode, Point>(e.target(), r));
            newEdge(v, w, s);
        }
        
        // --- we do not need to recompute the cover's barycentric placement
        cache.put(BARYCENTRIC_PLACEMENT, node2pos);
        
        // --- compute the cover morphism
        this.coverMorphism = new Morphism(
                this.nodes().next(),
                image.nodes().next(),
                ((CoordinateChange) C.inverse()).getOperator());
        
        // --- store some additional data
        this.image = image;
    }
    
    /**
     * @return the cover morphism.
     */
    public Morphism getCoverMorphism() {
        return this.coverMorphism;
    }
    
    /**
     * @return the image.
     */
    public PeriodicGraph getImage() {
        return this.image;
    }
    
    /**
     * Get the image of a node under the cover morphism.
     * 
     * @param v the original node.
     * @return the image.
     */
    public INode image(final INode v) {
        return getCoverMorphism().getImage(v);
    }
    
    /**
     * Get the image of an edge under the cover morphism.
     * 
     * CAVEAT: this maps only oriented edges.
     * 
     * @param e the original edge.
     * @return the image.
     */
    public IEdge image(final IEdge e) {
        return getCoverMorphism().getImage(e);
    }
    
    /**
     * Computes the position of a node with respect to the cover's coordinate
     * system given the position of it's image under the cover morphism in the
     * image's coordinate system.
     * 
     * @param v the node.
     * @param p the position of its image.
     * @return the corresponding position of v.
     */
    public Point liftedPosition(final INode v, final Point p) {
        final PeriodicGraph image = this.getImage();
        final Operator A = getCoverMorphism().getAffineOperator();
        final Point b = this.barycentricPlacement().get(v);
        final Point bA = (Point) b.times(A);
        final Vector d =
                (Vector) bA.minus(image.barycentricPlacement().get(image(v)));
        return (Point) p.plus(d).times(A.inverse());
    }
    
    /**
     * Computes the position of a node with respect to the cover's coordinate
     * system given the position of it's image under the cover morphism in the
     * image's coordinate system.
     * 
     * @param v the node.
     * @param pos a map assigning positions to image nodes.
     * @return the corresponding position of v.
     */
    public Point liftedPosition(final INode v, final Map<INode, Point> pos) {
        return liftedPosition(v, pos.get(image(v)));
    }
    
    /**
     * Returns the orbits of the set of nodes under the full combinatorial
     * symmetry group.
     * 
     * @return an iterator over the set of orbits.
     */
    public Iterator<Set<INode>> nodeOrbits() {
    	// --- determine node orbit representatives of the image graph
    	final PeriodicGraph img = getImage();
        final Partition<INode> P = new Partition<INode>();
        for (final Morphism a: img.symmetries()) {
            for (final INode v: img.nodes()) {
                P.unite(v, a.getImage(v));
            }
        }
        final Map<INode, INode> imageToRep = P.representativeMap();
        
        // --- determine preimage sets for each node orbit of the image graph
    	final SortedMap<INode, Set<INode>> preImages =
    	        new TreeMap<INode, Set<INode>>();
    	for (final INode v: nodes()) {
    		final INode w = imageToRep.get(image(v));
    		if (!preImages.containsKey(w)) {
    			preImages.put(w, new TreeSet<INode>());
    		}
    		preImages.get(w).add(v);
    	}
    	
        return preImages.values().iterator();
    }
    
    /**
     * Returns the orbits of the set of edges under the full combinatorial
     * symmetry group.
     * 
     * @return an iterator over the set of orbits.
     */
    public Iterator<Set<IEdge>> edgeOrbits() {
    	// --- determine edge orbit representatives of the image graph
    	final PeriodicGraph img = getImage();
        final Partition<IEdge> P = new Partition<IEdge>();
        for (final Morphism a: img.symmetries()) {
            for (final IEdge e: img.edges()) {
                final IEdge ae = a.getImage(e.oriented()).unoriented();
                P.unite(e, ae);
            }
        }
        final Map<IEdge, IEdge> imageToRep = P.representativeMap();

        // --- determine preimage sets for each edge orbit of the image graph
    	final Map<IEdge, Set<IEdge>> preImages =
    	        new HashMap<IEdge, Set<IEdge>>();
    	for (final IEdge e: edges()) {
    		final IEdge e1 = image(e.oriented()).unoriented();
    		final IEdge f = imageToRep.get(e1);
    		if (!preImages.containsKey(f)) {
    			preImages.put(f, new HashSet<IEdge>());
    		}
    		preImages.get(f).add(e);
    	}
    	
        return preImages.values().iterator();
    }
}

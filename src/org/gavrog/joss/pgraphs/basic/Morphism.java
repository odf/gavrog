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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;


/**
 * This class implements morphisms between periodic graphs, also called a
 * periodic morphisms. A periodic morphism is a graph morphism, i.e., an
 * incidence-preserving map of nodes and directed edges, which respects the
 * periodicity. On the representation level, it appears as a morphism between
 * two representation graphs, with the additional property that the relation
 * between edge-labels can, up to reassignment of node-class representatives, be
 * described by a linear map.
 * 
 * CAVEATS:
 * - only surjective morphisms are supported.
 * - only directed edges as returned by UndirectedGraph.orientedEdge() are mapped
 */
public class Morphism {
    final private Map<IGraphElement, IGraphElement> src2img;
    final private Map<IGraphElement, IGraphElement> img2src;
    final private Operator operator;
    final private boolean injective;
    final private PeriodicGraph sourceGraph;
    final private PeriodicGraph imageGraph;

    /**
     * Exception to be thrown if no morphism exists with a given specification.
     */
    public class NoSuchMorphismException extends RuntimeException {
        private static final long serialVersionUID = 5112586471768016998L;

        public NoSuchMorphismException(final String msg) {
            super(msg);
        }
    }
    
    /**
     * Creates a surjective morphism between connected, locally stable periodic
     * graphs, as specified by a pair of vertices v1 and v2 and a linear
     * transformation matrix M. The morphism is, if it exists, uniquely defined
     * by the property that it maps v1 to v2 and that the linear part of the
     * induced affine transformation of barycentric node positions is given by
     * M.
     * 
     * @param v1 a node of the source graph.
     * @param v2 the target node corresponding to v1.
     * @param M the linear part of the induced coordinate transformation.
     */
    public Morphism(final INode v1, final INode v2, final Operator M) {
        // --- retrieve some essential information
        final PeriodicGraph G1 = (PeriodicGraph) v1.owner();
        final PeriodicGraph G2 = (PeriodicGraph) v2.owner();
        final int d = G1.getDimension();
        
        // --- check the graphs
        if (!G1.isLocallyStable()) {
            throw new UnsupportedOperationException("first graph is not locally stable");
        }
        if (!G2.isLocallyStable()) {
            throw new UnsupportedOperationException("second graph is not locally stable");
        }
        if (!G1.isConnected()) {
            throw new UnsupportedOperationException("first graph is not connected");
        }
        if (!G2.isConnected()) {
            throw new UnsupportedOperationException("second graph is not connected");
        }
        if (G2.getDimension() != d) {
            throw new UnsupportedOperationException("graphs have different dimensions");
        }
        
        // --- check the matrix
        if (M != null) {
            if (M.getDimension() != d) {
                throw new IllegalArgumentException("bad transformation matrix");
            }
        }
        
        // --- initialize the map and queue
        final Map<IGraphElement, IGraphElement> src2img =
                new HashMap<IGraphElement, IGraphElement>();
        final Map<IGraphElement, IGraphElement> img2src =
                new HashMap<IGraphElement, IGraphElement>();
        boolean injective = true;
        final LinkedList<INode> queue = new LinkedList<INode>();
        
        src2img.put(v1, v2);
        queue.addLast(v1);
        
        // --- make a breadth-first traversal over both graphs in parallel
        while (queue.size() > 0) {
            final INode w1 = queue.removeFirst();
            final INode w2 = (INode) src2img.get(w1);
            final Map<Vector, IEdge> n1 = neighborVectors(w1);
            final Map<Vector, IEdge> n2 = neighborVectors(w2);
            
//            System.out.println("n1 = " + n1);
//            System.out.println("n2 = " + n2);
//            System.out.println("");
            
            for (final Vector dist: n1.keySet()) {
                final IEdge e1 = n1.get(dist);
                final IEdge e2 = n2.get(dist.times(M));
                if (e2 == null) {
                    throw new NoSuchMorphismException("no such morphism");
                } else if (e2.equals(src2img.get(e1))) {
                    continue;
                } else if (src2img.containsKey(e1)) {
                    throw new NoSuchMorphismException("no such morphism");
                } else {
                    if (img2src.containsKey(e2)) {
                        injective = false;
                    }
                    src2img.put(e1, e2);
                    img2src.put(e2, e1);
                    final INode u1 = e1.target();
                    final INode u2 = e2.target();
                    
                    if (img2src.containsKey(u2)) {
                        if (!img2src.get(u2).equals(u1)) {
                            injective = false;
                        }
                    } else {
                        img2src.put(u2, u1);
                    }
                    if (!src2img.containsKey(u1)) {
						src2img.put(u1, u2);
						queue.addLast(u1);
					} else if (!src2img.get(u1).equals(u2)) {
	                    throw new NoSuchMorphismException("no such morphism");					    
					}
                }
            }
        }
        
        // --- test for surjectivity
        for (final INode v: G2.nodes()) {
            if (!img2src.containsKey(v)) {
                throw new NoSuchMorphismException("no preimage for " + v);
            }
        }
        for (final IEdge ee: G2.edges()) {
            final IEdge e = ee.oriented();
            if (!img2src.containsKey(e)) {
                throw new NoSuchMorphismException("no preimage for " + e);
            }
            if (!img2src.containsKey(e.reverse())) {
                throw new NoSuchMorphismException("no preimage for " + e.reverse());
            }
        }
        
        // --- store the results
        this.src2img = src2img;
        this.img2src = img2src;
        this.operator = M;
        this.injective = injective;
        this.sourceGraph = G1;
        this.imageGraph = G2;
    }
    
    /**
     * Constructs a map from the distance vectors between a given node and its
     * neighbors to the representatives of corresponding connecting edges.
     * 
     * @param v a node.
     * @return a map from distance vectors to edge representatives.
     */
    private Map<Vector, IEdge> neighborVectors(final INode v) {
        final PeriodicGraph G = (PeriodicGraph) v.owner();
        final Map<Vector, IEdge> result = new TreeMap<Vector, IEdge>();
        
        for (final IEdge ee: v.incidences()) {
            final IEdge e = ee.oriented();
            final Vector dist = G.differenceVector(e);
            result.put(dist, e);
            if (v.equals(e.target())) {
                result.put((Vector) dist.negative(), e.reverse());
            }
        }
        return result;
    }
    
    /**
     * Constructs an instance.
     * 
     * @param v1 a node of the source graph.
     * @param v2 the target node corresponding to v1.
     * @param M the linear part of the induced coordinate transformation.
     */
    public Morphism(final INode v1, final INode v2, final Matrix M) {
        this(v1, v2, Operator.fromLinear(M));
    }
    
    /**
     * Creates an instance with given values for its fields.
     * 
     * @param src2img the map for the nodes and edges.
     * @param img2src the (partial) inverse map.
     * @param operator the associated coordinate transformation.
     * @param injective is the morphism injective?
     */
    public Morphism(
            final Map<IGraphElement, IGraphElement> src2img,
            final Map<IGraphElement, IGraphElement> img2src,
            final Operator operator,
            final boolean injective)
    {
        this.src2img = src2img;
        this.img2src = img2src;
        this.operator = operator;
        this.injective = injective;
        final IGraphElement x = src2img.keySet().iterator().next();
        final IGraphElement y = img2src.keySet().iterator().next();
        this.sourceGraph = (PeriodicGraph) x.owner();
        this.imageGraph = (PeriodicGraph) y.owner();
    }
    
    /**
     * Creates an instance after a given one.
     * @param model the model morphism.
     */
    public Morphism(final Morphism model) {
        this(model.src2img, model.img2src, model.operator, model.injective);
    }
    
    /**
     * Returns the inverse of a given morphism.
     * @return the morphism to invert.
     */
    public Morphism inverse() {
        if (!injective) {
            throw new IllegalArgumentException("not invertible");
        } else {
            return new Morphism(img2src, src2img, (Operator) operator.inverse(), true);
        }
    }
    
    /**
     * Checks if this morphism is bijective.
     * @return true if this morphism is bijective.
     */
    public boolean isIsomorphism() {
        return this.injective;
    }
    
    /**
     * Returns an object that is mapped onto the given one.
     * 
     * @param x the image.
     * @return a source for that image.
     */
    public Object getASource(final Object x) {
        return img2src.get(x);
    }
    
    /**
     * Computes the composition of this morphism with another one.
     * 
     * @param other the morphism to compose with.
     * @return the composed morphism.
     */
    public Morphism times(final Morphism other) {
        final Map<IGraphElement, IGraphElement> newS2I =
                new HashMap<IGraphElement, IGraphElement>();
        for (final IGraphElement x: this.src2img.keySet()) {
            final IGraphElement y = other.src2img.get(this.src2img.get(x));
            if (y == null) {
                throw new IllegalArgumentException("morphisms do not compose");
            } else {
                newS2I.put(x, y);
            }
        }
        final Map<IGraphElement, IGraphElement> newI2S =
                new HashMap<IGraphElement, IGraphElement>();
        for (final IGraphElement y: other.img2src.keySet()) {
            final IGraphElement x = this.img2src.get(other.img2src.get(y));
            if (x == null) {
                throw new IllegalArgumentException("morphisms do not compose");
            } else {
                newI2S.put(y, x);
            }
        }
        return new Morphism(newS2I, newI2S,
                (Operator) this.operator.times(other.operator),
                this.injective && other.injective);
    }
    
    /**
     * Returns the linear part of the affine transformation of barycentric
     * placements associated to this morphism.
     * 
     * @return the linear part of the associated affine transformation.
     */
    public Operator getLinearOperator() {
        return operator;
    }
    
    /**
     * Returns the translation part of the affine transformation of barycentric
     * placements associated to this morphism.
     * 
     * @return the translation part of the associated affine transformation.
     */
    public Vector getTranslation() {
        final IGraphElement x = src2img.keySet().iterator().next();
        final INode v;
        if (x instanceof INode) {
            v = (INode) x;
        } else {
            v = ((IEdge) x).source();
        }
        final INode w = (INode) src2img.get(v);
        final Map<INode, Point> pos1 =
                ((PeriodicGraph) v.owner()).barycentricPlacement();
        final Map<INode, Point> pos2 =
                ((PeriodicGraph) w.owner()).barycentricPlacement();
        final Point posv = pos1.get(v);
        final Point posw = pos2.get(w);
        final Vector s = (Vector) posw.minus(posv.times(getLinearOperator()));
        final int d = s.getDimension();
        final Real result[] = new Real[d];
        for (int i = 0; i < d; ++i) {
            result[i] = (Real) ((Real) s.get(i)).mod(1);
        }
        return new Vector(result);
    }
    
    /**
     * Returns the affine transformation of barycentric placements associated to
     * this morphism.
     * 
     * @return the associated affine transformation.
     */
    public Operator getAffineOperator() {
        return (Operator) getLinearOperator().times(getTranslation());
    }
    
    public int size() {
        return src2img.size();
    }


    public boolean containsKey(Object arg0) {
        return src2img.containsKey(arg0);
    }

    public boolean containsValue(Object arg0) {
        return src2img.containsValue(arg0);
    }

    public Object get(Object arg0) {
        return src2img.get(arg0);
    }

    public INode getImage(INode arg0) {
        return (INode) get(arg0);
    }

    public IEdge getImage(IEdge arg0) {
        return (IEdge) get(arg0);
    }

    public boolean equals(Object other) {
        if (other instanceof Morphism) {
            return this.src2img.equals(((Morphism) other).src2img);
        } else {
            return false;
        }
    }
    
    public String toString() {
    	final StringBuffer buf = new StringBuffer(500);
    	buf.append("(\n");
    	for (final IGraphElement key: this.src2img.keySet()) {
    		buf.append("  ");
    		buf.append(key);
    		buf.append("==>");
    		buf.append(get(key));
    		buf.append(",\n");
    	}
    	buf.append(")\n");
    	return buf.toString();
    }
    
    public int hashCode() {
        return this.src2img.hashCode();
    }

	public PeriodicGraph getImageGraph() {
		return imageGraph;
	}

	public PeriodicGraph getSourceGraph() {
		return sourceGraph;
	}
}

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import org.gavrog.box.collections.Cache;
import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.NiftyList;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.collections.Partition;
import org.gavrog.box.simple.Tag;
import org.gavrog.box.simple.TaskController;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;


/**
 * Implements a representation of a periodic graph.
 * 
 * @author Olaf Delgado
 * @version $Id: PeriodicGraph.java,v 1.73 2007/05/30 23:19:53 odf Exp $
 */

public class PeriodicGraph extends UndirectedGraph {

    // --- enables or disables debugging
    final protected static boolean DEBUG = false;
    
    // --- the cache keys
    final protected static Object CONNECTED_COMPONENTS = new Tag();
    final protected static Object BARYCENTRIC_PLACEMENT = new Tag();
    final protected static Object IS_LOCALLY_STABLE = new Tag();
    final protected static Object IS_LADDER = new Tag();
    final protected static Object CHARACTERISTIC_BASES = new Tag();
    final protected static Object SYMMETRIES = new Tag();
    final protected static Object INVARIANT = new Tag();
    final protected static Object CONVENTIONAL_CELL = new Tag();
    final protected static Object TRANSLATIONAL_EQUIVALENCE_CLASSES = new Tag();
    final protected static Object MINIMAL_IMAGE_MAP = new Tag();

    private static final Object TRANSLATIONAL_EQUIVALENCES = null;

    // --- cache for this instance
    final protected Cache cache = new Cache();

    // --- the Systre key version used
    final public String invariantVersion = "1.0";

    // --- other fixes fields
    final protected int dimension;
    final protected Map edgeIdToShift = new HashMap();

    /**
     * Constructs an instance.
     * 
     * @param dimension the dimension of periodicity of this graph.
     */
    public PeriodicGraph(final int dimension) {
        super();
        this.dimension = dimension;
    }

    /**
     * Constructs a copy of a given graph.
     * 
     * @param src the graph to copy.
     */
    public PeriodicGraph(final PeriodicGraph src) {
        this(src.getDimension());
        final Map old2new = new HashMap();
        for (final Iterator nodes = src.nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            old2new.put(v, newNode());
        }
        for (final Iterator edges = src.edges(); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            final INode v = (INode) old2new.get(e.source());
            final INode w = (INode) old2new.get(e.target());
            newEdge(v, w, src.getShift(e));
        }
    }
    
    /**
     * Implements a node of the covering graph - as opposed to the representing
     * orbit graph. Each node is given as a pair consisting of a node of the
     * orbit graph and an integral shift vector.
     */
    public class CoverNode implements INode {
        private INode v;
        private Vector shift;
        private int degree = -1;

        /**
         * Creates a new node.
         * 
         * @param v the orbit graph node.
         * @param shift the shift vector.
         */
        public CoverNode(final INode v, final Vector shift) {
            if (!PeriodicGraph.this.hasElement(v)) {
                throw new IllegalArgumentException("no such node");
            }
            if (!shift.isIntegral()) {
                throw new IllegalArgumentException("vector must be integral");
            }
            
            this.v = v;
            this.shift = shift;
        }
        
        /**
         * Creates a new node with a zero shift.
         * 
         * @param v the orbit graph node.
         */
        public CoverNode(final INode v) {
            this(v, Vector.zero(PeriodicGraph.this.getDimension()));
        }
        
        /**
         * Retrieves the associated node in the orbit graph.
         * 
         * @return the orbit node for this node.
         */
        public INode getOrbitNode() {
            return this.v;
        }
        
        /**
         * Retrieves the associated shift vector 
         * 
         * @return the shift for this node.
         */
        public Vector getShift() {
            return this.shift;
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.INode#degree()
         */
        public int degree() {
            if (this.degree < 0) {
                incidences();
            }
            return this.degree;
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IGraphElement#owner()
         */
        public IGraph owner() {
            return PeriodicGraph.this;
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IGraphElement#incidences()
         */
        public Iterator incidences() {
            final List result = new ArrayList();
            for (final Iterator iter = v.incidences(); iter.hasNext();) {
                final IEdge e = ((IEdge) iter.next());
                result.add(new CoverEdge(e, this.shift));
                if (e.source().equals(e.target())) {
                    result.add(new CoverEdge(e.reverse(), this.shift));
                }
            }
            this.degree = result.size();
            return result.iterator();
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IGraphElement#id()
         */
        public Object id() {
            return new Pair(this.v.id(), this.shift);
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object other) {
            if (other instanceof CoverNode) {
                final CoverNode x = (CoverNode) other;
                return this.owner().id().equals(x.owner().id())
                       && this.id().equals(x.id());
            } else {
                return false;
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.v.hashCode() * 37 + this.shift.hashCode();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "(" + this.v.id() + ", " + this.shift + ")";
        }
    }
    
    /**
     * Implements a edge of the covering graph - as opposed to the representing
     * orbit graph. Each edge is given as a pair consisting of an edge of the
     * orbit graph and an integral shift vector.
     */
    public class CoverEdge implements IEdge {
        private IEdge e;
        private Vector shift;
        private boolean compareAsOriented;

        /**
         * Creates a new edge.
         * 
         * @param e the orbit graph edge.
         * @param shift the shift vector.
         * @param compareAsOriented if true, reverse edges are considered different.
         */
        public CoverEdge(final IEdge e, final Vector shift, final boolean compareAsOriented) {
            if (!PeriodicGraph.this.hasElement(e)) {
                throw new IllegalArgumentException("no such edge");
            }
            if (!shift.isIntegral()) {
                throw new IllegalArgumentException("vector must be integral");
            }
            
            this.e = e.oriented();
            this.shift = shift;
            this.compareAsOriented = compareAsOriented;
        }
        
        /**
         * Creates a new edge with a zero shift.
         * 
         * @param e the orbit graph edge.
         */
        public CoverEdge(final IEdge e) {
            this(e, Vector.zero(PeriodicGraph.this.getDimension()));
        }
        
        /**
         * Creates a new edge.
         * 
         * @param e the orbit graph edge.
         * @param shift the shift vector.
         */
        public CoverEdge(final IEdge e, final Vector shift) {
            this(e, shift, true);
        }
        
        /**
         * Retrieves the associated edge in the orbit graph.
         * 
         * @return the orbit edge for this edge.
         */
        public IEdge getOrbitNode() {
            return this.e;
        }
        
        /**
         * Retrieves the associated shift vector 
         * 
         * @return the shift for this node.
         */
        public Vector getShift() {
            return this.shift;
        }
        
        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IEdge#source()
         */
        public INode source() {
            return new CoverNode(this.e.source(), this.shift);
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IEdge#target()
         */
        public INode target() {
            final Vector s = PeriodicGraph.this.getShift(this.e);
            return new CoverNode(this.e.target(), (Vector) this.shift.plus(s));
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IEdge#opposite(org.gavrog.joss.pgraphs.basic.INode)
         */
        public INode opposite(INode oneEnd) {
            if (source().equals(oneEnd)) {
                return target();
            } else if (target().equals(oneEnd)) {
                return source();
            } else {
                throw new IllegalArgumentException("edge has no such vertex");
            }
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IEdge#reverse()
         */
        public IEdge reverse() throws UnsupportedOperationException {
            final Vector s = PeriodicGraph.this.getShift(this.e);
            return new CoverEdge(this.e.reverse(), (Vector) this.shift.plus(s),
                    this.compareAsOriented);
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IEdge#oriented()
         */
        public IEdge oriented() {
            return new CoverEdge(this.e, this.shift, true);
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IEdge#unoriented()
         */
        public IEdge unoriented() {
            return new CoverEdge(this.e, this.shift, false);
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IGraphElement#owner()
         */
        public IGraph owner() {
            return PeriodicGraph.this;
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IGraphElement#incidences()
         */
        public Iterator incidences() {
            final List tmp = new LinkedList();
            tmp.add(source());
            if (!source().equals(target())) {
                tmp.add(target());
            }
            return tmp.iterator();
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.IGraphElement#id()
         */
        public Object id() {
            return new Pair(this.e.id(), this.shift);
        }
        
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object other) {
            if (other instanceof CoverEdge) {
                final CoverEdge x = (CoverEdge) other;
                if (!this.owner().id().equals(x.owner().id())) {
                    return false;
                } else if (this.compareAsOriented != x.compareAsOriented) {
                    return false;
                } else if (this.source().equals(x.source())
                        && this.target().equals(x.target())) {
                    return true;
                } else if (!this.compareAsOriented && this.source().equals(x.target())
                        && this.target().equals(x.source())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.source().hashCode() * 37 + this.target().hashCode();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "(" + this.e + ", " + this.shift + ")";
        }
    }
    
    /**
     * Represents a translational lattice of connected components of this
     * periodic graph.
     */
    public class Component {
        final private PeriodicGraph graph;
        final private Matrix basis;
        final private Whole multiplicity;
    
        /**
         * Constructs an instance.
         * @param graph the component as a connected periodic graph
         * @param basis describes the embedding into the containing graph
         */
        public Component(final PeriodicGraph graph, final Matrix basis) {
            this.graph = graph;
            this.basis = basis;
            if (graph.getDimension() == PeriodicGraph.this.getDimension()) {
                if (graph.getDimension() == 0) {
                    this.multiplicity = Whole.ONE;
                } else {
                    this.multiplicity = (Whole) basis.determinant();
                }
            } else {
                this.multiplicity = Whole.ZERO;
            }
        }
    
        /**
         * @return the basis
         */
        public Matrix getBasis() {
            return this.basis;
        }
    
        /**
         * @return the dimension
         */
        public int getDimension() {
            return this.graph.getDimension();
        }
    
        /**
         * @return the graph
         */
        public PeriodicGraph getGraph() {
            return this.graph;
        }
    
        /**
         * @return the multiplicity
         */
        public Whole getMultiplicity() {
            return this.multiplicity;
        }
    }

    /**
     * @return Returns the dimension.
     */
    public int getDimension() {
        return dimension;
    }
    
    /**
     * Retrieves the shift vector associated to an edge.
     * @param e an edge of the graph.
     * @return the shift vector for that edge.
     */
    public Vector getShift(final IEdge e) {
        if (hasElement(e)) {
            final Vector s = (Vector) edgeIdToShift.get(e.id());
            if (((UndirectedGraph.Edge) e).isReverse) {
                return (Vector) s.negative();
            } else {
                return s;
            }
        } else {
            throw new IllegalArgumentException("no such edge");
        }
    }

    /**
     * Retrieve an edge with a given source, target and shift.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector.
     * @return the unique edge with this data, or null, if none exists.
     */
    public IEdge getEdge(final INode source, final INode target, final Vector shift) {
        for (Iterator edges = directedEdges(source, target); edges.hasNext();) {
            final IEdge e = (IEdge) edges.next();
            if (getShift(e).equals(shift)) {
                return e;
            } else if (source.equals(target) && getShift(e).equals(shift.negative())) {
                return e.reverse();
            }
        }
        return null;
    }
    
    /**
     * Creates a new edge with a zero shift vector.
     * @param source the source node.
     * @param target the target node.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target) {
        return newEdge(source, target, Vector.zero(getDimension()));
    }

    /**
     * Creates a new edge.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector associated to the new edge.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target, final int[] shift) {
        return newEdge(source, target, new Vector(shift));
    }

    /**
     * Creates a new edge.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector associated to the new edge.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target,
            final Vector shift) {
        return newEdge(source, target, shift, true);
    }

    /**
     * Creates a new edge.
     * @param source the source node.
     * @param target the target node.
     * @param shift the shift vector associated to the new edge.
     * @param checkSimple if true, trivial loops and duplicates are forbidden.
     * @return the newly created edge.
     */
    public IEdge newEdge(final INode source, final INode target,
            final Vector shift, final boolean checkSimple) {
        if (shift.getDimension() != this.dimension) {
            throw new IllegalArgumentException("bad shape for shift");
        }
        if (checkSimple) {
            if (getEdge(source, target, shift) != null) {
                throw new IllegalArgumentException("duplicate edge");
            }
            if (source.equals(target) && shift.equals(shift.zero())) {
                throw new IllegalArgumentException("trivial loop");
            }
        }
        cache.clear();
        final IEdge e = super.newEdge(source, target);
        edgeIdToShift.put(e.id(), new Vector(shift));
        return e;
    }

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newNode()
     */
    public INode newNode() {
        cache.clear();
        return super.newNode();
    }
    
    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#delete(javaPGraphs.IGraphElement)
     */
    public void delete(final IGraphElement element) {
        cache.clear();
        if (element instanceof IEdge) {
            edgeIdToShift.remove(element.id());
        }
        super.delete(element);
    }

    /**
     * Modifies the shift vectors assigned to the edges of this graph to reflect
     * a conceptual replacement of one node representative by another copy.
     * 
     * @param node the node to replace.
     * @param amount the vector towards the new representative.
     */
    public void shiftNode(final INode node, final Vector amount) {
        if (!amount.isIntegral()) {
            throw new IllegalArgumentException("argument vector must be integral");
        }
        if (amount.getDimension() != getDimension()) {
            throw new IllegalArgumentException("argument vector has dimension "
                                               + amount.getDimension()
                                               + ", but should have " + getDimension());
        }
        for (final Iterator iter = node.incidences(); iter.hasNext();) {
            final IEdge e = (IEdge) iter.next();
            if (e.source().equals(e.target())) {
                continue;
            }
            
            if (((UndirectedGraph.Edge) e).isReverse) {
                final Object id = e.reverse().id();
                edgeIdToShift.put(id, ((Vector) edgeIdToShift.get(id)).minus(amount));
            } else {
                final Object id = e.id();
                edgeIdToShift.put(id, ((Vector) edgeIdToShift.get(id)).plus(amount));
            }
        }
            
        // --- adjust barycentric placement, if any
        try {
            final Map placement = (Map) cache.get(BARYCENTRIC_PLACEMENT);
            final Map tmp = new HashMap();
            tmp.putAll(placement);
            tmp.put(node, ((Point) tmp.get(node)).plus(amount));
            cache.put(BARYCENTRIC_PLACEMENT, Collections.unmodifiableMap(tmp));
        } catch (Cache.NotFoundException ex) {
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.UndirectedGraph#compareEdges(javaPGraphs.IEdge,
     *           javaPGraphs.IEdge)
     */
    protected int compareEdges(IEdge e1, IEdge e2) {
        final int d = super.compareEdges(e1, e2);
        if (d != 0) {
            return d;
        } else {
            return getShift(e1).compareTo(getShift(e2));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.UndirectedGraph#normalizedEdge(javaPGraphs.IEdge)
     */
    protected IEdge normalizedEdge(final IEdge e) {
        int d = compareIds(e.source(), e.target());
        if (d == 0) {
            d = getShift(e).sign();
        }
        if (d > 0) {
            return e.reverse();
        } else {
            return e;
        }
    }

    /* (non-Javadoc)
     * @see javaPGraphs.UndirectedGraph#formatEdgeInfo(javaPGraphs.IEdge)
     */
    protected String formatEdgeInfo(final IEdge e) {
        final StringBuffer buf = new StringBuffer(100);
        buf.append("[");
        final Vector s = getShift(e);
        for (int i = 0; i < this.dimension; ++i) {
            if (i > 0) {
                buf.append(",");
            }
            buf.append(s.get(i));
        }
        buf.append("]");
        return buf.toString();
    }
    
    /**
     * Constructs an iterator that generates the numbers of nodes in consecutive
     * shells around a start vertex. This count is for the actual (infinite)
     * periodic graph, not the representing finite multigraph.
     * 
     * @param start the start vertex.
     * @return an iterator for the coordination sequence.
     */
    public Iterator coordinationSequence(final INode start) {
        final Set previousShell = new HashSet();
        final Set currentShell = new HashSet();
        
        return new IteratorAdapter() {
            protected Object findNext() throws NoSuchElementException {
                if (currentShell.size() == 0) {
                    currentShell.add(new CoverNode(start));
                } else {
                    final Set nextShell = new HashSet();
                    for (final Iterator nodes = currentShell.iterator(); nodes.hasNext();) {
                        final CoverNode v = (CoverNode) nodes.next();
                        for (final Iterator edges = v.incidences(); edges.hasNext();) {
                            final CoverEdge e = (CoverEdge) edges.next();
                            final CoverNode w = (CoverNode) e.target();
                            if (!previousShell.contains(w)
                                    && !currentShell.contains(w)
                                    && !nextShell.contains(w)) {
                                nextShell.add(w);
                            }
                        }
                    }
                    previousShell.clear();
                    previousShell.addAll(currentShell);
                    currentShell.clear();
                    currentShell.addAll(nextShell);
                }
                return new Integer(currentShell.size());
            }
        };
    }
    
    /**
     * Return the shortest cycle at a given angle.
     * 
     * @param u the first leg of the angle.
     * @param v the apex of the angle.
     * @param w the second leg of the angle.
     * @param limit Maximal number of nodes to explore in total.
     * @return The cycle as a list of CoverNodes.
     */
    public List<CoverNode> shortestCycleAtAngle(
    		final CoverNode u, final CoverNode v, final CoverNode w,
    		final int limit)
    {
        if (u == v || u == w || v == w)
            throw new IllegalArgumentException("Must be pairwise distinct.");

        final Map<CoverNode, CoverNode> back =
                new HashMap<CoverNode, CoverNode>();
        final List<CoverNode> queue = new LinkedList<CoverNode>();
        
        back.put(v, v);
        back.put(w, v);
        queue.add(w);
        
        while (!queue.isEmpty() && back.size() < limit) {
            final CoverNode s = queue.remove(0);
            for (final Iterator<CoverEdge> edges = s.incidences();
                    edges.hasNext();) {
                final CoverEdge e = edges.next();
                final CoverNode t = (CoverNode) e.target();
                if (t.equals(u)) {
                    final List<CoverNode> res = new ArrayList<CoverNode>();
                    res.add(v);
                    res.add(u);
                    CoverNode x = s;
                    while (x != v) {
                        res.add(x);
                        x = back.get(x);
                    }
                    return res;
                } else if (!back.containsKey(t)) {
                    back.put(t, s);
                    queue.add(t);
                }
            }
        }
        
    	return null;
    }

    /**
     * Return the shortest cycle at a given angle.
     * 
     * @param u the first leg of the angle.
     * @param v the apex of the angle.
     * @param w the second leg of the angle.
     * @return The cycle as a list of CoverNodes.
     */
    public List<CoverNode> shortestCycleAtAngle(
            final CoverNode u, final CoverNode v, final CoverNode w)
    {
        return shortestCycleAtAngle(u, v, w, 1000000);
    }
    
    /**
     * Returns the point symbol at a given node, as used by Wells.
     * 
     * For an n-coordinated node there are n(n-1)/2 angles, given by the node
     * itself together with a pair of its neighbors. The point symbol is then
     * of the form A^a.B^b…,indicating that there are a angles with shortest
     * cycles of length A, b angles with shortest cycles of length B,
     * etc., where A < B, < … and a + b +.. = n(n-1)/2.
     * 
     * @param node the node.
     * @param limit the maximum number of nodes to be explored at each angle.
     * @return
     */
    public String pointSymbol(final INode node, final int limit) {
        final CoverNode v = new CoverNode(node);
        final SortedMap<Integer, Integer> counts =
                new TreeMap<Integer, Integer>();
        final List<CoverEdge> edges = new ArrayList<CoverEdge>();
        Iterators.addAll(edges, v.incidences());
        
        for (int i = 0; i < edges.size(); ++i) {
            final CoverNode u = (CoverNode) edges.get(i).target();
            for (int j = i + 1; j < edges.size(); ++j) {
                final CoverNode w = (CoverNode) edges.get(j).target();
                final int n = shortestCycleAtAngle(u, v, w, limit).size();
                if (counts.containsKey(n)) {
                    counts.put(n, counts.get(n) + 1);
                } else {
                    counts.put(n, 1);
                }
            }
        }
        StringBuffer tmp = new StringBuffer();
        for (int n: counts.keySet()) {
            int m = counts.get(n);
            if (tmp.length() > 0)
                tmp.append('.');
            tmp.append(n);
            if (m > 1) {
                tmp.append('^');
                tmp.append(m);
            }
        }
        
        return tmp.toString();
    }
    
    /**
     * Returns the point symbol at a given node.
     * 
     * @param node the node.
     * @return
     */
    public String pointSymbol(final INode v) {
        return pointSymbol(v, 1000000);
    }
    
    /**
     * Determines the connected components of the periodic graph.
     * @return the list of components.
     */
    public List connectedComponents() {
        try {
            return (List) this.cache.get(CONNECTED_COMPONENTS);
        } catch (Cache.NotFoundException ex) {
        }
        
        final int dim = getDimension();
        final Set seen = new HashSet();
        final Map adjustment = new HashMap();
        final List componentsTmp = new ArrayList();
        
        for (final Iterator nodes = nodes(); nodes.hasNext();) {
            final INode start = (INode) nodes.next();
            if (seen.contains(start)) {
                continue;
            }
            final List translations = new ArrayList();
            final LinkedList queue = new LinkedList();
            final List componentNodes = new ArrayList();
            queue.addLast(start);
            seen.add(start);
            componentNodes.add(start);
            adjustment.put(start, Vector.zero(dim));
            
            while (queue.size() > 0) {
                final INode v = (INode) queue.removeFirst();
                final Vector av = (Vector) adjustment.get(v);
                for (final Iterator edges = v.incidences(); edges.hasNext();) {
                    final IEdge e = (IEdge) edges.next();
                    final INode w = e.target();
                    final Vector s = getShift(e);
                    if (!seen.contains(w)) {
                        queue.addLast(w);
                        seen.add(w);
                        componentNodes.add(w);
                        adjustment.put(w, av.minus(s));
                    } else {
                        final Vector aw = (Vector) adjustment.get(w);
                        translations.add(s.plus(aw).minus(av));
                    }
                }
            }
            componentsTmp.add(new Pair(componentNodes, translations));
        }
        
        final List components = new ArrayList();
        for (final Iterator iter = componentsTmp.iterator(); iter.hasNext();) {
            final Pair entry = (Pair) iter.next();
            final List thisNodes = (List) entry.getFirst();
            final List thisTrans = (List) entry.getSecond();
            final Matrix A = Vector.toMatrix(thisTrans).mutableClone();
            Matrix.triangulate(A, null, true, true);
            final int thisDim = A.rank();
            final Matrix thisBasis = A.getSubMatrix(0, 0, thisDim, dim);
            final CoordinateChange C = new CoordinateChange(thisBasis);
            
            final PeriodicGraph thisGraph = new PeriodicGraph(thisDim);
            final Map old2new = new HashMap();
            for (final Iterator nodes = thisNodes.iterator(); nodes.hasNext();) {
                final INode v = (INode) nodes.next();
                old2new.put(v, thisGraph.newNode());
            }
            for (final Iterator edges = edges(); edges.hasNext();) {
                final IEdge e = (IEdge) edges.next();
                final INode v = (INode) old2new.get(e.source());
                final INode w = (INode) old2new.get(e.target());
                if (v == null || w == null) {
                    continue;
                }
                final Vector s = getShift(e);
                final Vector av = (Vector) adjustment.get(e.source());
                final Vector aw = (Vector) adjustment.get(e.target());
                final Vector t = (Vector) (Vector) s.plus(aw).minus(av).times(C);
                final Matrix c = t.getCoordinates().getSubMatrix(0, 0, 1,
                        thisDim);
                thisGraph.newEdge(v, w, new Vector(c));
            }
            components.add(new Component(thisGraph, thisBasis));
        }
        
        return (List) this.cache.put(CONNECTED_COMPONENTS, components);
    }
    
    /**
     * Tests if the covering graph (not just the representing multigraph) is
     * connected.
     * 
     * @return true if the periodic graph is connected.
     */
    public boolean isConnected() {
        final List components = connectedComponents();
        final int n = components.size();
        return n == 0
                || (n == 1 && ((Component) components.get(0)).getMultiplicity()
                        .equals(Whole.ONE));
    }
    
    /**
     * Computes a barycentric placement for the nodes. Nodes are in barycentric
     * positions if each node is in the center of gravity of its neighbors. In
     * other words, each coordinate for its position is the average of the
     * corresponding coordinates for its neighbors. The barycentric positions
     * are, of course, with respect to the periodic graph. In particular, shifts
     * are taken into account. The returned map, however, contains only the
     * positions for the node representatives.
     * 
     * The barycentric placement of connected graph are unique up to affine
     * transformations, i.e., general basis and origin changes. This method
     * computes coordinates expressed in terms of the basis used for edge shift
     * vectors in this graph. Moreover, the first vertex, as produced by the
     * iterator returned by nodes(), is placed at the origin.
     * 
     * The graph in question must, for now, be connected as for multiple
     * components the barycentric placement is no longer unique.
     * 
     * @return a map giving barycentric positions for the node representatives.
     */
    public Map barycentricPlacement() {
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        
        // --- see if placement has already been computed
        try {
            return (Map) this.cache.get(BARYCENTRIC_PLACEMENT);
        } catch (Cache.NotFoundException ex) {
        }
        
        // --- assign an integer index to each node representative
        final Map nodeToIndex = new HashMap();
        final List indexToNode = new ArrayList();
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            nodeToIndex.put(v, new Integer(indexToNode.size()));
            indexToNode.add(v);
        }
        
        // --- set up a system of equations
        final int n = indexToNode.size(); // the number of nodes
        final int[][] M = new int[n+1][n];
        final Matrix t = Matrix.zero(n+1, this.dimension);
        
        for (int i = 0; i < n; ++i) {
            final INode v = (INode) indexToNode.get(i);
            for (final Iterator iter = v.incidences(); iter.hasNext();) {
                final IEdge e = (IEdge) iter.next();
                final INode w = e.target();
                if (v.equals(w)) {
                    // loops cancel out with their reverses, so we must consider
                    // each loop twice (once in each direction) or not at all
                    continue;
                }
                final Vector s = getShift(e);
                final int j = ((Integer) nodeToIndex.get(w)).intValue();
                --M[i][j];
                ++M[i][i];
                t.setRow(i, (Matrix) t.getRow(i).plus(s.getCoordinates()));
            }
        }
        M[n][0] = 1;
        
        // --- solve the system
        final Matrix P = Matrix.solve(new Matrix(M), t);

        // --- extract the positions found
        final Map tmp = new HashMap();
        for (int i = 0; i < n; ++i) {
            tmp.put(indexToNode.get(i), new Point(P.getRow(i)));
        }
        final Map result = Collections.unmodifiableMap(tmp);
        
        // --- cache and return the result
        return (Map) this.cache.put(BARYCENTRIC_PLACEMENT, result);
    }
    
    /**
     * Checks whether a given node placement is barycentric.
     * 
     * @param pos a node placement.
     * @return true if the placement is barycentric.
     */
    public boolean isBarycentric(final Map pos) {
        for (final Iterator nodes = nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            final Point p = (Point) pos.get(v);
            Vector t = Vector.zero(getDimension());
            for (final Iterator iter = v.incidences(); iter.hasNext();) {
                final IEdge e = (IEdge) iter.next();
                final INode w = e.target();
                if (w.equals(v)) {
                    continue; // loops cancel out with their reverses
                }
                final Vector s = getShift(e);
                final Point q = (Point) pos.get(w);
                t = (Vector) t.plus(q).plus(s).minus(p);
            }
            if (!t.isZero()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if this graph is stable, meaning that no two nodes have the same
     * positions in a barycentric placement.
     * 
     * @return true if the graph is stable.
     */
    public boolean isStable() {
        final Map positions = barycentricPlacement();
        final Set seen = new HashSet();
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final Point p = (Point) positions.get(iter.next());
            final Real x[] = new Real[p.getDimension()];
            for (int i = 0; i < p.getDimension(); ++i) {
                x[i] = (Real) p.get(i).mod(Whole.ONE);
            }
            final Point p0 = new Point(x);
            if (seen.contains(p0)) {
                return false;
            } else {
                seen.add(p0);
            }
        }
        return true;
    }
    
    /**
     * Checks if this graph is locally stable, meaning that no two nodes with a
     * common neighbor have the same positions in a barycentric placement.
     * 
     * @return true if the graph is locally stable.
     */
    public boolean isLocallyStable() {
        try {
            return this.cache.getBoolean(IS_LOCALLY_STABLE);
        } catch (Cache.NotFoundException ex) {
            final Map positions = barycentricPlacement();
            for (final Iterator iter = nodes(); iter.hasNext();) {
                final INode v = (INode) iter.next();
                final Set positionsSeen = new HashSet();
                for (final Iterator incident = v.incidences(); incident
                        .hasNext();) {
                    final IEdge e = (IEdge) incident.next();
                    final Vector s = getShift(e);
                    final Point p0 = (Point) positions.get(e.target());
                    final Point p = (Point) p0.plus(s);
                    if (positionsSeen.contains(p)) {
                        return this.cache.put(IS_LOCALLY_STABLE, false);
                    } else {
                        positionsSeen.add(p);
                    }
                }
            }
            return this.cache.put(IS_LOCALLY_STABLE, true);
        }
    }
    
    /**
     * Checks if the graph is a ladder, meaning a locally stable graph with a
     * non-trivial automorphism which geometrically maps to the identity via
     * barycentric positions.
     * 
     * @return true is the graph is a ladder.
     */
    public boolean isLadder() {
        try {
            return this.cache.getBoolean(IS_LADDER);
        } catch (Cache.NotFoundException ex) {
        }
        
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (isStable() || !isLocallyStable()) {
            return cache.put(IS_LADDER, false);
        }
        
        // --- find equivalence classes w.r.t. ladder translations
        final Operator I = Operator.identity(getDimension());
        final Iterator iter = nodes();
        final INode start = (INode) iter.next();
        final Map pos = barycentricPlacement();
        final Point pos0 = (Point) pos.get(start);
        
        while (iter.hasNext()) {
            final INode v = (INode) iter.next();
            final Point posv = (Point) pos.get(v);
            if (((Vector) posv.minus(pos0)).modZ().isZero()) {
                try {
                    new Morphism(start, v, I);
                    return cache.put(IS_LADDER, true);
                } catch (Morphism.NoSuchMorphismException ex) {
                }
            }
        }
        return cache.put(IS_LADDER, false);
    }
    
    /**
     * Finds all additional topological translations in this periodic graph,
     * which must be connected and locally stable and constructs the equivalence
     * classes of nodes defined by these. A topological translation in this case
     * is defined as a graph automorphism which commutes with all the given
     * translations for this periodic graph and leaves no node fixed. This
     * includes "translations" of finite orders, as they may occur in
     * ladder-like structures. A topological translation of finite order must
     * correspond to an identity transformation in the barycentric placement, so
     * it can only occur in non-stable graphs.
     * 
     * An iterator is returned which contains the equivalence classes as sets if
     * additional translations are found. In the special case that none are
     * found, the iterator, however, covers the empty set rather than the set of
     * all single node sets.
     * 
     * @return an iterator over the set of equivalence classes.
     */
    public Iterator translationalEquivalenceClasses() {
        return translationalEquivalences().classes();
    }
    
    /**
     * Computes a partition object encoding the translational equivalence
     * classes (see above).
     * 
     * @return the partition into translational equivalence classes.
     */
    public Partition translationalEquivalences() {
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        try {
            return (Partition) this.cache.get(TRANSLATIONAL_EQUIVALENCES);
        } catch (Cache.NotFoundException ex) {
            final Operator I = Operator.identity(getDimension());
            final Partition P = new Partition();
            final Iterator iter = nodes();
            final INode start = (INode) iter.next();

            while (iter.hasNext()) {
                final INode v = (INode) iter.next();
                if (!P.areEquivalent(start, v)) {
                    final Morphism iso;
                    try {
                        iso = new Morphism(start, v, I);
                    } catch (Morphism.NoSuchMorphismException ex1) {
                        continue;
                    }
                    for (final Iterator it = nodes(); it.hasNext();) {
                        final INode w = (INode) it.next();
                        P.unite(w, iso.get(w));
                    }
                }
            }
            return (Partition) this.cache.put(TRANSLATIONAL_EQUIVALENCES, P);
        }
    }
    
    /**
     * Checks if this graph is minimal. A periodic graph is minimal if its
     * translation group can not be extended.
     * 
     * @return true if the graph if minimal.
     */
    public boolean isMinimal() {
        return !translationalEquivalenceClasses().hasNext();
    }
    
    /**
     * Computes a minimal image of the representation graph. This corresponds to
     * a maximal extension of the translation group of the periodic graph
     * consisting of topological translations of infinite order.
     * 
     * Currently, this only works for graphs with no nontrivial translations of
     * finite order.
     * 
     * @return a morphism from the original graph to its minimal image.
     */
    public Morphism minimalImageMap() {
        try {
            return (Morphism) this.cache.get(MINIMAL_IMAGE_MAP);
        } catch (Cache.NotFoundException ex) {
        }
        
        // --- some preparations
        final int d = getDimension();
        final Map pos = barycentricPlacement();
        
        // --- find translationally equivalent node classes
        final List classes = Iterators.asList(translationalEquivalenceClasses());
        if (classes.size() == 0) {
            // --- no extra translations, graph is minimal
            final INode v0 = (INode) nodes().next();
            final Morphism result = new Morphism(v0, v0, Operator.identity(d));
            return (Morphism) cache.put(MINIMAL_IMAGE_MAP, result);
        }
        
        // --- collect the translation vectors
        final List vectors = new ArrayList();
        {
            final Iterator iter = ((Set) classes.get(0)).iterator();
            final INode v = (INode) iter.next();
            final Point pv = (Point) pos.get(v);

            while (iter.hasNext()) {
                final INode w = (INode) iter.next();
                final Point pw = (Point) pos.get(w);
                final Vector t = ((Vector) pw.minus(pv)).modZ();
                if (t.isZero()) {
                    final String s = "found translation of finite order";
                    throw new UnsupportedOperationException(s);
                } else {
                    vectors.add(t);
                }
            }
        }
        
        // --- init new graph and map old nodes to new nodes and vice versa
        final PeriodicGraph G = new PeriodicGraph(d);
        final Map old2new = new HashMap();
        final Map new2old = new HashMap();
        for (final Iterator iter = classes.iterator(); iter.hasNext();) {
            final Set cl = (Set) iter.next();
            final INode vNew = G.newNode();
            for (final Iterator inClass = cl.iterator(); inClass.hasNext();) {
                final INode vOld = (INode) inClass.next();
                old2new.put(vOld, vNew);
                if (!new2old.containsKey(vNew)) {
                    new2old.put(vNew, vOld);
                }
            }
        }
        
        // --- determine a basis for the extended translation group
        final Matrix M = new Matrix(vectors.size() + d, d);
        M.setSubMatrix(0, 0, Vector.toMatrix(vectors));
        M.setSubMatrix(vectors.size(), 0, Matrix.one(d));
        Matrix.triangulate(M, null, true, false);
        if (M.rank() != d) {
            throw new RuntimeException("internal error - please contact author");
        }
        
        // --- compute the basis change matrix
        final Matrix A = M.getSubMatrix(0, 0, d, d);
        final CoordinateChange basisChange = new CoordinateChange(A);
        
        // --- now add the edges for the new graph
        for (final Iterator iter = edges(); iter.hasNext();) {
            // --- extract the data for the next edge
            final IEdge e = (IEdge) iter.next();
            final INode v = e.source();
            final INode w = e.target();
            final Vector s = getShift(e);
            
            // --- construct the corresponding edge in the new graph
            final INode vNew = (INode) old2new.get(v);
            final INode wNew = (INode) old2new.get(w);
            final INode vRep = (INode) new2old.get(vNew);
            final INode wRep = (INode) new2old.get(wNew);
            final Vector vShift = (Vector) ((Point) pos.get(v)).minus(pos.get(vRep));
            final Vector wShift = (Vector) ((Point) pos.get(w)).minus(pos.get(wRep));
            final Vector sNew = (Vector) wShift.minus(vShift).plus(s).times(basisChange);
            
            // --- insert this edge if it is not already there
            if (G.getEdge(vNew, wNew, sNew) == null) {
                G.newEdge(vNew, wNew, sNew);
            }
        }
        
        // --- return the new graph
        final INode v0 = (INode) nodes().next();
        final INode w0 = (INode) old2new.get(v0);
        final Morphism result = new Morphism(v0, w0, basisChange.getOperator());
        return (Morphism) cache.put(MINIMAL_IMAGE_MAP, result);
    }
    
    /**
     * Computes a minimal image of the representation graph. This corresponds to
     * a maximal extension of the translation group of the periodic graph
     * consisting of topological translations of infinite order.
     * 
     * Currently, this only works for graphs with no nontrivial translations of
     * finite order.
     * 
     * @return the minimal image.
     */
    public PeriodicGraph minimalImage() {
        return minimalImageMap().getImageGraph();
    }
    
    /**
     * Derives the set of characteristic bases of this periodic graph. Each
     * basis is represented by an ordered list of d directed edges, where d is
     * the dimension of periodicity of the graph, such that the difference
     * vectors between the source and target of each edge in a barycentric
     * placement are linearly independent. The list of bases is characteristic in
     * the sense that an isomorphism between periodic graphs will induce a
     * bijection between their associated sets of bases.
     * 
     * As this method depends on barycentric placement, the graph must, for now,
     * be connected.
     * 
     * CAVEAT: Please note that the actual set of bases depends on some
     * arbitrary choices in the algorithm. Thus, in order to compare graphs
     * using their characteristic bases or any structure derived using
     * characteristic bases, the same version of the algorithm has to be used on
     * both graphs. This applies, in particular, to canonical forms.
     * 
     * @return the set of characteristic bases, represented by edge lists.
     */
    public List characteristicBases() {
        try {
            return (List) this.cache.get(CHARACTERISTIC_BASES);
        } catch (Cache.NotFoundException ex) {
        }
        
        final List result = new LinkedList();
        final Map pos = barycentricPlacement();
        final int d = getDimension();

        // --- look for edge lists with a common source
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            final List edges = allIncidences(v);
            for (final Iterator good = goodCombinations(edges, pos); good.hasNext();) {
                result.add(good.next());
            }
        }

        if (result.size() == 0) {
            // --- no results, now look for edge lists that form chains
            for (final Iterator iter = nodes(); iter.hasNext();) {
                // --- get the next start node for the chain
                final INode v0 = (INode) iter.next();
                
                // --- initialize objects used in the subsequent search
                final LinkedList iterators = new LinkedList();
                final LinkedList edges = new LinkedList();
                final Matrix M = Matrix.zero(d, d).mutableClone();
                iterators.addLast(allIncidences(v0).iterator());
                edges.addLast(null);
                
                // --- do a depth first search for usable chains
                while (iterators.size() > 0) {
                    final int k = iterators.size();
                    final Iterator current = (Iterator) iterators.getLast();
                    if (current.hasNext()) {
                        // -- get next edge and data related to it
                        final IEdge e = (IEdge) current.next();
                        final INode v = e.source();
                        final INode w = e.target();
                        final Point pv = (Point) pos.get(v);
                        final Point pw = (Point) pos.get(w);
                        final Vector diff = (Vector) pw.minus(pv).plus(getShift(e));
                        
                        // --- see if so far edge vectors are independent
                        M.setRow(k-1, diff.getCoordinates());
                        if (M.rank() == k) {
                            // --- they are
                            edges.removeLast();
                            edges.addLast(e.oriented());
                            if (k == d) {
                                // --- found a result here
                                result.add(edges.clone());
                            } else {
                                // --- have to extend the chain
                                iterators.addLast(allIncidences(w).iterator());
                                edges.addLast(null);
                            }
                        }
                    } else {
                        // --- backtracking
                        iterators.removeLast();
                        edges.removeLast();
                        M.setRow(k-1, Matrix.zero(1, d));
                    }
                }
            }
        }
        
        if (result.size() == 0) {
            // --- still nothing, so use general edge lists
            final List edges = allDirectedEdges();
            for (final Iterator good = goodCombinations(edges, pos); good.hasNext();) {
                result.add(good.next());
            }
        }
        
        return (List) cache.put(CHARACTERISTIC_BASES, Collections
                .unmodifiableList(result));
    }

    /**
     * Returns all edges incident to the given node, with loops in the representation
     * graph listed once in each direction.
     * 
     * @param v the common source node.
     * @return the list of edges found.
     */
    public List allIncidences(final INode v) {
        final List result = new ArrayList();
        for (final Iterator iter = v.incidences(); iter.hasNext();) {
            final IEdge e = ((IEdge) iter.next()).oriented();
            result.add(e);
            if (e.source().equals(e.target())) {
                result.add(e.reverse());
            }
        }
        return result;
    }

    /**
     * Returns representatives for all directed edges present in this graph. Thus, each
     * undirected edge is produced twice, namely once in each direction.
     * 
     * @return the list of edges found.
     */
    private List allDirectedEdges() {
        final List result = new ArrayList();
        for (final Iterator iter = edges(); iter.hasNext();) {
            final IEdge e = ((IEdge) iter.next()).oriented();
            result.add(e);
            result.add(e.reverse());
        }
        return result;
    }

    /**
     * Constructs an iterator over all ordered sublists of size d of the given
     * edge list, for which the associated vectors form a linearly independent
     * set. Here, d is the dimension of periodicity of this graph. The vector
     * associated to an edge is the difference between the position of its
     * source and target.
     * 
     * @param edges the edge list to pick from.
     * @param pos associates d-dimensional coordinates to nodes.
     * @return an iterator over all good edge combinations.
     */
    public Iterator goodCombinations(final List edges, final Map pos) {
        final int d = getDimension();
        final int n = edges.size();
        if (n < d || d == 0) {
            return Iterators.empty();
        } else if (d == 1) {
            return new FilteredIterator(edges.iterator()) {
                @Override
                public Object filter(Object x) {
                    List a = new ArrayList();
                    a.add(x);
                    return a;
                }
            };
        }

        return new IteratorAdapter() {
            private Iterator perms = Iterators.empty();
            private final int a[] = new int[d];

            protected Object findNext() throws NoSuchElementException {
                while (!perms.hasNext()) {
                    int k;
                    if (d > 1 && a[1] == 0) {
                        for (int i = 0; i < d; ++i) {
                            a[i] = i;
                        }
                        k = 0;
                    } else {
                        k = d - 1;
                        while (k >= 0 && n - a[k] <= d - k) {
                            --k;
                        }
                        if (k < 0) {
                            throw new NoSuchElementException("at end");
                        } else {
                            ++a[k];
                            for (int i = k + 1; i < d; ++i) {
                                a[i] = a[k] + i - k;
                            }
                        }
                    }
                    
                    final Matrix M = new Matrix(d, d);
                    for (int i = 0; i < d; ++i) {
                        final IEdge e = (IEdge) edges.get(a[i]);
                        final Point pv = (Point) pos.get(e.source());
                        final Point pw = (Point) pos.get(e.target());
                        final Vector t = (Vector) pw.minus(pv).plus(getShift(e));
                        M.setRow(i, t.getCoordinates());                        
                    }
                    if (M.rank() == d) {
                        final Object picks[] = new Object[d];
                        for (int i = 0; i < d; ++i) {
                            picks[i] = edges.get(a[i]);
                        }
                        perms = Iterators.permutations(picks);
                    }
                }
                return perms.next();
            }
        };
    }
    
    /**
     * Determines the periodic automorphisms of this periodic graph. A periodic
     * automorphism is one that reflects the periodicity of the graph. It can be
     * represented as an automorphism of the representation graph that induces a
     * linear transformation on the edge shift vectors which is expressed by a
     * unimodular integer matrix.
     * 
     * @return the set of automorphisms, each expressed as a map between nodes
     */
    public Set symmetries() {
        try {
            return (Set) this.cache.get(SYMMETRIES);
        } catch (Cache.NotFoundException ex) {
        }
        
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        final TaskController taskController = TaskController.getInstance();
        
        final List generators = new LinkedList();
        final int d = getDimension();
        final List bases = characteristicBases();
        
        final List basis0 = (List) bases.get(0);
        final INode v0 = ((IEdge) basis0.get(0)).source();
        final Matrix B0 = differenceMatrix(basis0);
        
        for (int i = 0; i < bases.size(); ++i) {
            taskController.bailOutIfCancelled();
            
            final List b = (List) bases.get(i);
            final INode v = ((IEdge) b.get(0)).source();
            final Matrix B = differenceMatrix(b);
            final Matrix M = new Matrix(d+1, d+1);
            M.setSubMatrix(0, 0, Matrix.solve(B0, B));
            M.set(d, d, Whole.ONE);
            M.setSubMatrix(d, 0, Matrix.zero(1, d));
            M.setSubMatrix(0, d, Matrix.zero(d, 1));
            if (isUnimodularIntegerMatrix(M)) {
                final Morphism iso;
                try {
                    iso = new Morphism(v0, v, new Operator(M));
                } catch (Morphism.NoSuchMorphismException ex) {
                    continue;
                }
                generators.add(iso);
            }
        }
        
        final Set seen = new HashSet();
        final LinkedList queue = new LinkedList();
        final Morphism identity = new Morphism(v0, v0, Operator.identity(d));
        seen.add(identity);
        queue.addLast(identity);
        while (queue.size() > 0) {
            final Morphism phi = (Morphism) queue.removeFirst();
            for (final Iterator gens = generators.iterator(); gens.hasNext();) {
                final Morphism psi = (Morphism) gens.next();
                final Morphism product = phi.times(psi);
                if (!seen.contains(product)) {
                    seen.add(product);
                    queue.addLast(product);
                }
            }
        }
        
        return (Set) cache.put(SYMMETRIES, Collections.unmodifiableSet(seen));
    }

    /**
     * Determines the affine operators associated to the periodic automorphisms
     * of this periodic graph.
     * 
     * @return the list of operators.
     */
    public List symmetryOperators() {
        final List res = new ArrayList();
        for (final Iterator iter = symmetries().iterator(); iter.hasNext();) {
            res.add(((Morphism) iter.next()).getAffineOperator());
        }
        return res;
    }
    
    /**
     * Returns a space group object defined by the symmetry operators of this graph.
     * @return the space group object.
     */
    public SpaceGroup getSpaceGroup() {
        return new SpaceGroup(getDimension(), symmetryOperators());
    }
    
    /**
     * Extracts the difference vector for an edge.
     * 
     * @param e an edge.
     * @return the difference vector
     */
    public Vector differenceVector(final IEdge e) {
        final Map pos = barycentricPlacement();
        final Point pv = (Point) pos.get(e.source());
        final Point pw = (Point) pos.get(e.target());
        return (Vector) pw.minus(pv).plus(getShift(e));
    }
    
    /**
     * Extracts the difference vectors from a list of edges and turns them into
     * the rows of a matrix.
     * 
     * @param edges a list of edges.
     * @return a matrix composed of difference vectors
     */
    private Matrix differenceMatrix(final List edges) {
        final int n = edges.size();
        
        final Matrix M = new Matrix(n, getDimension());
        for (int i = 0; i < n; ++i) {
            M.setRow(i, differenceVector((IEdge) edges.get(i)).getCoordinates());
        }
        return M;
    }
    
    /**
     * Checks if all entries of the given matrix are whole numbers and the determinant is
     * one.
     * 
     * @param M a matrix
     * @return true if M has only integer entries
     */
    private boolean isUnimodularIntegerMatrix(final Matrix M) {
        for (int i = 0; i < M.numberOfRows(); ++i) {
            for (int j = 0; j < M.numberOfColumns(); ++j) {
                if (!(M.get(i, j) instanceof Whole)) {
                    return false;
                }
            }
        }
        return M.determinant().norm().isOne();
    }
    
    /**
     * Computes a basis for the translation lattice of this graph which turns
     * all the affine transformations associated to periodic symmetries into
     * isometries.
     * 
     * @return the new basis as a matrix.
     */
    public Matrix symmetricBasis() {
        // -- preparations
        final int d = getDimension();
        final Set syms = symmetries();
        
        // --- compute a symmetry-invariant quadratic form
        Matrix M = Matrix.zero(d, d);
        for (final Iterator iter = syms.iterator(); iter.hasNext();) {
            final Matrix A = ((Morphism) iter.next()).getLinearOperator().getCoordinates()
                    .getSubMatrix(0, 0, d, d);
            M = (Matrix) M.plus(A.times(A.transposed()));
        }
        M = (Matrix) M.times(new Fraction(1, syms.size()));
        
        // --- compute and return an orthonormal basis for the new form
        return LinearAlgebra.orthonormalRowBasis(M);
    }

    /**
     * Returns the orbits of the set of nodes under the full combinatorial
     * symmetry group.
     * 
     * @return an iterator over the set of orbits.
     */
//    public Iterator nodeOrbits() {
//        final Partition P = new Partition();
//        for (final Iterator syms = symmetries().iterator(); syms.hasNext();) {
//            final Morphism a = (Morphism) syms.next();
//            for (final Iterator nodes = nodes(); nodes.hasNext();) {
//                final INode v = (INode) nodes.next();
//                P.unite(v, a.get(v));
//            }
//        }
//        return P.classes();
//    }
    public Iterator<Set<INode>> nodeOrbits() {
        final Set<INode> seen = new HashSet<INode>();
        final List<Set<INode>> orbits = new ArrayList<Set<INode>>();

        for (final Iterator nodes = nodes(); nodes.hasNext();) {
            final INode v = (INode) nodes.next();
            if (!seen.contains(v)) {
                final Set<INode> orbit = new HashSet<INode>();
                orbit.add(v);
                seen.add(v);
                for (final Iterator syms = symmetries().iterator();
                        syms.hasNext();)
                {
                    final Morphism a = (Morphism) syms.next();
                    final INode w = (INode) a.get(v);
                    orbit.add(w);
                    seen.add(w);
                }
                orbits.add(orbit);
            }
        }
        return orbits.iterator();
    }
    
    /**
     * Computes the stabilizer of a node in the symmetry group.
     * 
     * @param v a node of the representation graph.
     * @return the list of symmetries stabilizing the node up to translations.
     */
    public List nodeStabilizer(final INode v) {
        final List res = new ArrayList();
        for (final Iterator syms = symmetries().iterator(); syms.hasNext();) {
            final Morphism a = (Morphism) syms.next();
            if (a.get(v).equals(v)) {
                res.add(a);
            }
        }
        return res;
    }
    
    /**
     * Returns the orbits of the set of edges under the full combinatorial
     * symmetry group.
     * 
     * @return an iterator over the set of orbits.
     */
    public Iterator edgeOrbits() {
        final Partition P = new Partition();
        for (final Iterator syms = symmetries().iterator(); syms.hasNext();) {
            final Morphism a = (Morphism) syms.next();
            for (final Iterator edges = edges(); edges.hasNext();) {
                final IEdge e = (IEdge) edges.next();
                final IEdge ae = ((IEdge) a.get(e.oriented())).unoriented();
                P.unite(e, ae);
            }
        }
        return P.classes();
    }
    
    /**
     * Computes a invariant for this periodic graph. An invariant is an object,
     * in this case a list, that is unique for an isomorphism class of periodic
     * graphs. In other words, the invariant does not depend on the original
     * representation of a graph, but only on the "essential structure" of the
     * graph itself. The invariants computed here have the additional advantage
     * that nonisomorphic graphs necessarily have different invariants. Even
     * further, they can be used to construct a canonical representation for a
     * given isomorphism class.
     * 
     * @return the invariant.
     */
    public NiftyList invariant() {
        if (DEBUG) {
            System.out.println("\nComputing invariant for " + this);
        }
        try {
            return (NiftyList) this.cache.get(INVARIANT);
        } catch (Cache.NotFoundException ex) {
        }
        
        // --- check prerequisites
        if (!isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        final TaskController taskController = TaskController.getInstance();
        
        final int d = getDimension();
        final int m = numberOfEdges();
        final List bases = characteristicBases();
        final Point zero = Point.origin(d);

        class EdgeCmd implements Comparable {
            public int source;
            public int target;
            public Vector shift;
            
            public EdgeCmd(final int v, final int w, final Vector s) {
                this.source = v;
                this.target = w;
                this.shift = s;
            }
            
            public int compareTo(final Object other) {
                if (other instanceof EdgeCmd) {
                    final EdgeCmd e = (EdgeCmd) other;
                    if (e.source != this.source) {
                        return this.source - e.source;
                    } else if (e.target != this.target) {
                        return this.target - e.target;
                    } else {
                        return ((Vector) this.shift.minus(e.shift)).sign();
                    }
                } else {
                    throw new IllegalArgumentException();
                }
            }
            
            private String shiftAsString() {
                final StringBuffer buf = new StringBuffer(10);
                buf.append("[");
                final Vector s = this.shift;
                for (int i = 0; i < s.getDimension(); ++i) {
                    if (i > 0) {
                        buf.append(",");
                    }
                    buf.append(s.get(i));
                }
                buf.append("]");
                return buf.toString();
            }
        
            public String toString() {
                return "(" + source + "," + target + "," + shiftAsString() + ")";
            }
        }
        
        final EdgeCmd bestScript[] = new EdgeCmd[m];
        Matrix bestBasis = null;
        INode bestStart = null;
        if (DEBUG) {
            System.out.println("  Found " + bases.size() + " bases\n");
        }
        
        for (int i = 0; i < bases.size(); ++i) {
            taskController.bailOutIfCancelled();
            
            final List b = (List) bases.get(i);
            if (DEBUG) {
                System.out.println("  Checking basis " + b);
            }
            final INode v0 = ((IEdge) b.get(0)).source();
            final Matrix B = differenceMatrix(b);
            final Matrix B_1 = (Matrix) B.inverse();
            
            final LinkedList Q = new LinkedList();
            Q.addLast(new Pair(v0, zero));
            final Map old2new = new HashMap();
            old2new.put(v0, new Integer(1));
            final Map newPos = new HashMap();
            newPos.put(v0, zero);

            int nextVertex = 2;
            int edgesSoFar = 0;
            boolean equal = (bestBasis != null);
            CoordinateChange basisAdjustment = null;
            final Matrix essentialShifts = Matrix.zero(d, d).mutableClone();
            int r = 0;
            
            class Break extends Throwable {
            }
            
            try {
                while (Q.size() > 0) {
                    final Pair entry = (Pair) Q.removeFirst();
                    final INode v = (INode) entry.getFirst();
                    final int vn = ((Integer) old2new.get(v)).intValue();
                    final Point p = (Point) entry.getSecond();
                    
                    // --- collect neighbors and sort by mapped difference vectors
                    final List incident = allIncidences(v);
                    final Matrix M = (Matrix) differenceMatrix(incident).times(B_1);
                    final Map edgeToRow = new HashMap();
                    for (int k = 0; k < incident.size(); ++k) {
                        edgeToRow.put(incident.get(k), new Vector(M.getRow(k)));
                    }
                    Collections.sort(incident, new Comparator() {
                        public int compare(final Object arg0, final Object arg1) {
                            final Vector a = (Vector) edgeToRow.get(arg0);
                            final Vector b = (Vector) edgeToRow.get(arg1);
                            return a.compareTo(b);
                        }
                    });
                    
                    // --- loop over neighbors
                    for (final Iterator it = incident.iterator(); it.hasNext();) {
                        final IEdge e = (IEdge) it.next();
                        final INode w = e.target();
                        final Point s = (Point) p.plus(edgeToRow.get(e));
                        final int wn;
                        Vector shift;
                        
                        if (!old2new.containsKey(w)) {
                            // --- edge connects to new vertex class
                            Q.addLast(new Pair(w, s));
                            wn = nextVertex++;
                            old2new.put(w, new Integer(wn));
                            newPos.put(w, s);
                            shift = Vector.zero(d);
                        } else {
                            wn = ((Integer) old2new.get(w)).intValue();
                            if (wn < vn) {
                                // --- wrong direction
                                continue;
                            }
                            // --- compute shift vector for new edge
                            shift = (Vector) s.minus(newPos.get(w));
                            if (basisAdjustment != null) {
                                // --- convert to a precomputed basis of shifts
                                shift = (Vector) shift.times(basisAdjustment);
                            } else {
                                // --- see if new vector contributes to a basis of shifts
                                essentialShifts.setRow(r, shift.getCoordinates());
                                if (essentialShifts.rank() > r) {
                                    // --- yes, it does
                                    shift = Vector.unit(d, r);
                                    ++r;
                                    if (r == d) {
                                        basisAdjustment = new CoordinateChange(
                                                essentialShifts);
                                    }
                                } else {
                                    // --- no, so express as sum of former shifts
                                    essentialShifts.setRow(r, Matrix.zero(1, d));
                                    shift = new Vector(LinearAlgebra.solutionInRows(
                                            essentialShifts, shift.getCoordinates(),
                                            false));
                                }
                            }
                        }
                        if (vn < wn || (vn == wn && shift.sign() < 0)) {
                            // --- compare with the best result to date
                            final EdgeCmd newEdge = new EdgeCmd(vn, wn, shift);
                            if (DEBUG) {
                                System.out.print("    New edge " + newEdge);
                            }
                            
                            if (equal) {
                                if (DEBUG) {
                                    System.out.print(" - compared to " + bestScript[edgesSoFar] + " at " + edgesSoFar + " -");
                                }
                                final int cmp = newEdge.compareTo(bestScript[edgesSoFar]);
                                if (cmp < 0) {
                                    if (DEBUG) {
                                        System.out.print(" is a winner.");
                                    }
                                    equal = false;
                                } else if (cmp > 0) {
                                    if (DEBUG) {
                                        System.out.println(" is a loser.");
                                    }
                                    throw new Break();
                                } else {
                                    if (DEBUG) {
                                        System.out.print(" is equal.");
                                    }
                                }
                            }
                            if (DEBUG) {
                                System.out.println("");
                            }
                            if (!equal) {
                                if (DEBUG) {
                                    System.out.println("    Writing " + newEdge + " at position " + edgesSoFar);
                                }
                                bestScript[edgesSoFar] = newEdge;
                            }
                            edgesSoFar++;
                        }
                    }
                }
            } catch (Break done) {
                continue;
            }
            bestBasis = (Matrix) basisAdjustment.getBasis().times(differenceMatrix(b));
            bestStart = ((IEdge) b.get(0)).source();
        }
        
        // --- collect the shift vectors and extract a basis
        
        final Matrix A = Matrix.zero(m, d).mutableClone();
        for (int i = 0; i < m; ++i) {
            A.setRow(i, bestScript[i].shift.getCoordinates());
        }
        Matrix.triangulate(A, null, true, false);
        final Matrix B = A.getSubMatrix(0, 0, d, d);

        final CoordinateChange basisChange = new CoordinateChange(B);
        
        // --- apply the basis change to the best script
        for (int i = 0; i < m; ++i) {
            final EdgeCmd cmd = bestScript[i];
            Vector shift = (Vector) cmd.shift.times(basisChange);
            for (int j = 0; j < d; ++j) {
                if (!(shift.get(j) instanceof Whole)) {
                    throw new RuntimeException("internal error - please contact author");
                }
            }
            if (cmd.source == cmd.target && shift.sign() > 0) {
                shift = (Vector) shift.negative();
            }
            bestScript[i] = new EdgeCmd(cmd.source, cmd.target, shift);
        }

        // --- sort the converted script
        Arrays.sort(bestScript);
        
        // --- construct the canonical form and the invariant
        final PeriodicGraph canonical = new PeriodicGraph(d);
        final List invariant = new LinkedList();
        invariant.add(new Integer(this.getDimension()));
        
        final int n = numberOfNodes();
        final INode nodes[] = new INode[n+1];
        for (int i = 1; i <= n; ++i) {
            nodes[i] = canonical.newNode();
        }
        for (int i = 0; i < m; ++i) {
            final EdgeCmd cmd = bestScript[i];
            canonical.newEdge(nodes[cmd.source], nodes[cmd.target], cmd.shift);
            invariant.add(new Integer(cmd.source));
            invariant.add(new Integer(cmd.target));
            for (int j = 0; j < d; ++j) {
                invariant.add(new Integer(((Whole) cmd.shift.get(j)).intValue()));
            }
        }
        
        // --- consistency test
        if (bestBasis != null) {
            final Matrix B1 = (Matrix) bestBasis.inverse().times(
                    basisChange.getBasis().inverse());
            try {
                new Morphism(bestStart, nodes[1], B1);
            } catch (Morphism.NoSuchMorphismException ex) {
                throw new RuntimeException(
                        "internal error - please contact author");
            }
        }

        // --- cache the results
        return (NiftyList) cache.put(INVARIANT, new NiftyList(invariant));
    }
    
    /**
     * Returns the Systre key for this graph.
     * 
     * @return the Systre key as a simple string.
     */
    public String getSystreKey() {
        final List inv = invariant();
        final StringBuffer buffer = new StringBuffer(50);
        for (int i = 0; i < inv.size(); ++i) {
            if (i > 0) {
                buffer.append(" ");
            }
            buffer.append(inv.get(i));
        }
        return buffer.toString();
    }
    
    /**
     * Computes a canonical form for this periodic graph. A canonical form is a
     * representation for a given graph that is unique for its isomorphism
     * class. In other words, the canonical form does not depend on the original
     * representation of a graph, but only on the "essential structure" of the
     * graph itself.
     * 
     * @return the canonical form.
     */
    public PeriodicGraph canonical() {
        return fromInvariantString(getSystreKey());
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals()
     */
    public boolean equals(final Object other) {
        if (other instanceof PeriodicGraph) {
            final PeriodicGraph G = (PeriodicGraph) other;
            return (G.getDimension() == this.getDimension()
                    && G.numberOfNodes() == this.numberOfNodes()
                    && G.numberOfEdges() == this.numberOfEdges()
                    && this.invariant().equals(G.invariant()));
        } else {
            return false;
        }
    }
    
    /* (non-Javadoc)
     * @see int java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object arg) {
        if (!(arg instanceof PeriodicGraph)) {
            throw new IllegalArgumentException("argument must be a PeriodicGraph");
        }
        final PeriodicGraph other = (PeriodicGraph) arg;
        if (this.getDimension() != other.getDimension()) {
            return this.getDimension() - other.getDimension();
        } else if (this.numberOfNodes() != other.numberOfNodes()){
            return this.numberOfNodes() - other.numberOfNodes();
        } else if (this.numberOfEdges() != other.numberOfEdges()) {
            return this.numberOfEdges() - other.numberOfEdges();
        } else {
            return this.invariant().compareTo(other.invariant());
        }
    }
    
    /**
     * Takes the textual representation of an invariant and reconstructs the associated
     * periodic graph.
     * 
     * @param key the invariant as text.
     * @return the associated periodic graph.
     */
    public static PeriodicGraph fromInvariantString(final String key) {
        final List numbers = new ArrayList();
        final String fields[] = key.split("\\s+");
        for (int i = 0; i < fields.length; ++i) {
            numbers.add(new Integer(fields[i]));
        }
        final int d = ((Integer) numbers.get(0)).intValue();
        final int n = (numbers.size() - 1) / (d + 2);
        final PeriodicGraph G = new PeriodicGraph(d);
        final List nodes = new ArrayList();
        nodes.add(null);
        for (int i = 0; i < n; ++i) {
            final int offset = 1 + i * (d + 2);
            final int s = ((Integer) numbers.get(offset)).intValue();
            final int t = ((Integer) numbers.get(offset + 1)).intValue();
            if (s == nodes.size()) {
                nodes.add(G.newNode());
            }
            if (t == nodes.size()) {
                nodes.add(G.newNode());
            }
            if (s >= nodes.size() || t >= nodes.size()) {
                throw new RuntimeException("something's wrong here");
            }
            final int[] shift = new int[d];
            for (int j = 0; j < d; ++j) {
                final Integer x = (Integer) numbers.get(offset + 2 + j);
                shift[j] = x.intValue();
            }
            G.newEdge((INode) nodes.get(s), (INode) nodes.get(t), shift);
        }
        return G;
    }
    
    /**
     * Computes a cover of this graph as it would sit in a conventional
     * crystallographic unit cell for its symmetry group.
     * 
     * @return the covering periodic graph.
     */
    public Cover conventionalCellCover() {
        // --- see if we can do this
        if (!isMinimal()) {
            throw new UnsupportedOperationException("graph not minimal");
        }

        try {
            return (Cover) this.cache.get(CONVENTIONAL_CELL);
        } catch (Cache.NotFoundException ex) {
            // --- construct a SpaceGroupFinder object for the symmetry group
            final SpaceGroupFinder finder = new SpaceGroupFinder(
                    getSpaceGroup());

            // --- determine a coordinate mapping into a conventional cell
            final CoordinateChange C = finder.getToStd();

            // --- express the new unit cell in terms of the old one
            final int dim = getDimension();
            final CoordinateChange Cinv = (CoordinateChange) C.inverse();
            final Vector basis[] = new Vector[dim];
            for (int i = 0; i < dim; ++i) {
                basis[i] = (Vector) Vector.unit(dim, i).times(Cinv);
            }

            // --- construct, cache and return the cover
            final Cover cover = new Cover(this, basis);
            return (Cover) cache.put(CONVENTIONAL_CELL, cover);
        }
    }
        
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return this.invariant().hashCode();
    }
}

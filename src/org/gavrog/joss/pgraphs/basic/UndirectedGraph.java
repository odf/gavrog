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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.Pair;


/**
 * Implements an undirected (multi-) graph. The "multi-" means that edge from a
 * node to itself ("loops") and multiple edges between the same pair of nodes
 * are allowed. The "undirected" means that. although for representation
 * purposes, edges still have a "source" and a "target" node, their roles are
 * interchangeable and, in effect, each edge is treated as equivalent to its
 * reverse.
 */
public abstract class UndirectedGraph<T extends Comparable<? super T>>
implements IGraph<T> {
    private static long nextGraphId = 1;
    private final long id;

    private Map<T, Class<?>> idToType = new HashMap<T, Class<?>>();

    private Map<T, Set<T>> nodeIdToIncidentEdgesIds =
            new LinkedHashMap<T, Set<T>>();

    private Map<T, Integer> nodeIdToDegree = new HashMap<T, Integer>();

    private Map<T, T> edgeIdToSourceNodeId = new HashMap<T, T>();

    private Map<T, T> edgeIdToTargetNodeId = new HashMap<T, T>();

    /**
     * Constructs an empty graph.
     */
    public UndirectedGraph() {
        this.id = new Long(nextGraphId++);
    }
    
    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#id()
     */
    public Object id() {
        return new Pair<Class<?>, Long>(getClass(), this.id);
    }
    
    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#numberOfNodes()
     */
    public int numberOfNodes() {
        return this.nodeIdToIncidentEdgesIds.size();
    }

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#numberOfEdges()
     */
    public int numberOfEdges() {
        return this.edgeIdToSourceNodeId.size();
    }
    
    /**
     * Implements node objects for this graph.
     */
    protected class Node implements INode<T>, Comparable<INode<T>> {
        private final T id;

        /**
         * Constructs a new node object.
         * 
         * @param id the id of this node.
         */
        public Node(final T id) {
            this.id = id;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.INode#degree()
         */
        public int degree() {
            return ((Integer) nodeIdToDegree.get(id)).intValue();
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#owner()
         */
        public IGraph<T> owner() {
            return UndirectedGraph.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#incidences()
         */
        public Iterator<IGraphElement<T>> incidences() {
            final Set<T> ids = nodeIdToIncidentEdgesIds.get(this.id);
            return new FilteredIterator<IGraphElement<T>, T>(ids.iterator()) {
                public IGraphElement<T> filter(final T x) {
                    if (edgeIdToSourceNodeId.get(x).equals(id())) {
                        return new Edge(x, false);
                    } else if (edgeIdToTargetNodeId.get(x).equals(id())) {
                        return new Edge(x, true);
                    } else {
                        throw new RuntimeException("inconsistency in graph");
                    }
                }
            };
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#id()
         */
        public T id() {
            return this.id;
        }

        public boolean equals(final INode<?> other)
        {
            return this.owner().id().equals(other.owner().id())
                    && id().equals(other.id());
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object other) {
            if (other instanceof INode<?>)
                return equals((INode<?>) other);
            return false;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.owner().id().hashCode() * 37 + id.hashCode();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "Node " + id;
        }

        public int compareTo(final INode<T> arg0) {
            return this.id().compareTo(arg0.id());
        }
    }

    /**
     * Implements edge objects for this graph.
     */
    protected class Edge implements IEdge<T> {
        private final T id;
        private boolean compareAsOriented;

        protected final boolean isReverse;

        /**
         * Constructs a new edge object.
         * 
         * @param id the id of this edge.
         * @param isReverse the direction of the edge.
         * @param compareAsOriented if true, use isReverse in equals() and hashCode()
         */
        public Edge(final T id, final boolean isReverse,
                final boolean compareAsOriented) {
            this.id = id;
            this.isReverse = isReverse;
            this.compareAsOriented = compareAsOriented;
        }

        /**
         * Constructs a new edge object.
         * 
         * @param id the id of this edge.
         * @param isReverse the direction of the edge.
         */
        public Edge(final T id, final boolean isReverse) {
            this(id, isReverse, false);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IEdge#source()
         */
        public INode<T> source() {
            if (this.isReverse) {
                return new Node(edgeIdToTargetNodeId.get(this.id()));
            } else {
                return new Node(edgeIdToSourceNodeId.get(this.id()));
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IEdge#target()
         */
        public INode<T> target() {
            if (this.isReverse) {
                return new Node(edgeIdToSourceNodeId.get(this.id()));
            } else {
                return new Node(edgeIdToTargetNodeId.get(this.id()));
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IEdge#opposite(javaPGraphs.INode)
         */
        public INode<T> opposite(final INode<T> oneEnd) {
            if (source().equals(oneEnd)) {
                return target();
            } else if (target().equals(oneEnd)) {
                return source();
            } else {
                throw new IllegalArgumentException("edge has no such vertex");
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IEdge#reverse()
         */
        public IEdge<T> reverse() throws UnsupportedOperationException {
            return new Edge(this.id, !this.isReverse, this.compareAsOriented);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#owner()
         */
        public IGraph<T> owner() {
            return UndirectedGraph.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#incidences()
         */
        public Iterator<IGraphElement<T>> incidences() {
            final List<IGraphElement<T>> tmp =
                    new LinkedList<IGraphElement<T>>();
            tmp.add(source());
            if (!source().equals(target())) {
                tmp.add(target());
            }
            return tmp.iterator();
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#id()
         */
        public T id() {
            return this.id;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Edge e) {
            if (!this.owner().id().equals(e.owner().id())
                    || !this.id.equals(e.id())) {
                return false;
            } else if (this.compareAsOriented != e.compareAsOriented) {
                return false;
            } else if (this.compareAsOriented) {
                return this.isReverse == e.isReverse;
            } else {
                return true;
            }
        }

        @SuppressWarnings("unchecked")
        public boolean equals(final Object other) {
            if (other instanceof UndirectedGraph.Edge)
                return equals((Edge) other);
            else
                return false;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            final int code = this.owner().id().hashCode() * 37 + id.hashCode();
            if (this.compareAsOriented) {
                return code * 37 + (this.isReverse ? 1 : 0);
            } else {
                return code;
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
//            if (this.compareAsOriented) {
//                return "Edge " + Math.abs(((Long) id).longValue()) + (this.isReverse ? "-" : "+");
//            } else {
//                return "Edge " + Math.abs(((Long) id).longValue());
//            }
            final StringBuffer buf = new StringBuffer(20);
            buf.append("(");
            buf.append(this.source().id());
            buf.append(",");
            buf.append(this.target().id());
            final String extra = formatEdgeInfo(this);
            if (extra != null && extra.length() > 0) {
                buf.append(",");
                buf.append(extra);
            }
            buf.append(")");
            return  buf.toString();
        }

        /* (non-Javadoc)
         * @see javaPGraphs.IEdge#orientedEdge()
         */
        public IEdge<T> oriented() {
            return new Edge(id(), this.isReverse, true);
        }

        /* (non-Javadoc)
         * @see javaPGraphs.IEdge#unorientedEdge()
         */
        public IEdge<T> unoriented() {
            return new Edge(id(), this.isReverse, false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#nodes()
     */
    public Iterator<INode<T>> nodes() {
        return new FilteredIterator<INode<T>, T>(
                this.nodeIdToIncidentEdgesIds.keySet().iterator()) {
            public INode<T> filter(final T x) {
                return new Node(x);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#edges()
     */
    public Iterator<IEdge<T>> edges() {
        return new FilteredIterator<IEdge<T>, T>(
                this.edgeIdToSourceNodeId.keySet().iterator()) {
            public IEdge<T> filter(final T x) {
                return new Edge(x, false);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#getElement(java.lang.Object)
     */
    public IGraphElement<T> getElement(final T id) {
        if (id == null) {
            return null;
        }
        final Class<?> type = this.idToType.get(id);
        if (Node.class.equals(type)) {
            return new Node(id);
        } else if (Edge.class.equals(type)) {
            return new Edge(id, false);
        } else {
            return null;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#hasElement(javaPGraphs.IGraphElement)
     */
    public boolean hasElement(final IGraphElement<T> element) {
        if (element == null || element.owner() != this) {
            return false;
        }
        final Class<?> type = this.idToType.get(element.id());
        if (Node.class.equals(type)) {
            return element instanceof UndirectedGraph.Node;
        } else if (Edge.class.equals(type)) {
            return element instanceof UndirectedGraph.Edge;
        } else {
            return false;
        }
    }

    public boolean hasElement(final T id) {
    	return getElement(id) != null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#connectingEdges(javaPGraphs.INode,
     *      javaPGraphs.INode)
     */
    public Iterator<IEdge<T>> connectingEdges(final INode<T> node1,
                                              final INode<T> node2) {
        return directedEdges(node1, node2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#directedEdges(javaPGraphs.INode,
     *      javaPGraphs.INode)
     */
    public Iterator<IEdge<T>> directedEdges(final INode<T> source,
                                            final INode<T> target) {
        if (!hasElement(source)) {
            throw new IllegalArgumentException("source node not in graph");
        }
        if (!hasElement(target)) {
            throw new IllegalArgumentException("source node not in graph");
        }
        final T sourceId = source.id();
        final T targetId = target.id();
        final Set<T> ids = nodeIdToIncidentEdgesIds.get(sourceId);
        return new FilteredIterator<IEdge<T>, T>(ids.iterator()) {
            public IEdge<T> filter(final T x) {
                final T s = edgeIdToSourceNodeId.get(x);
                final T t = edgeIdToTargetNodeId.get(x);
                if (s.equals(sourceId) && t.equals(targetId)) {
                    return new Edge(x, false);
                } else if (s.equals(targetId) && t.equals(sourceId)) {
                    return new Edge(x, true);
                } else {
                    return null;
                }
            }
        };
    }

    protected abstract T nextNodeId();
    protected abstract T nextEdgeId();

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newNode()
     */
    public INode<T> newNode() {
        final T id = nextNodeId();
        this.idToType.put(id, Node.class);
        this.nodeIdToIncidentEdgesIds.put(id, new LinkedHashSet<T>());
        this.nodeIdToDegree.put(id, new Integer(0));
        return new Node(id);
    }

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newEdge(javaPGraphs.INode, javaPGraphs.INode)
     */
    public IEdge<T> newEdge(final INode<T> source, final INode<T> target) {
        final T id = nextEdgeId();
        if (!hasElement(source)) {
            throw new IllegalArgumentException("source node does not exist");
        }
        if (!hasElement(target)) {
            throw new IllegalArgumentException("target node does not exist");
        }
        final T sId = source.id();
        final T tId = target.id();
        this.idToType.put(id, Edge.class);
        this.edgeIdToSourceNodeId.put(id, sId);
        this.edgeIdToTargetNodeId.put(id, tId);
        this.nodeIdToIncidentEdgesIds.get(sId).add(id);
        this.nodeIdToIncidentEdgesIds.get(tId).add(id);
        this.nodeIdToDegree.put(sId, this.nodeIdToDegree.get(sId) + 1);
        this.nodeIdToDegree.put(tId, this.nodeIdToDegree.get(tId) + 1);
        return new Edge(id, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#delete(javaPGraphs.IGraphElement)
     */
    public void delete(final IGraphElement<T> element) {
        if (!hasElement(element)) {
            throw new IllegalArgumentException("no such element");
        }
        final T id = element.id();
        if (element instanceof UndirectedGraph.Edge) {
            final Edge e = (Edge) element;
            final T sId = e.source().id();
            final T tId = e.target().id();
            this.nodeIdToIncidentEdgesIds.get(sId).remove(id);
            this.nodeIdToIncidentEdgesIds.get(tId).remove(id);
            this.edgeIdToSourceNodeId.remove(id);
            this.edgeIdToTargetNodeId.remove(id);
            this.nodeIdToDegree.put(sId, this.nodeIdToDegree.get(sId) - 1);
            this.nodeIdToDegree.put(tId, this.nodeIdToDegree.get(tId) - 1);
            this.idToType.remove(id);
        } else if (element instanceof UndirectedGraph.Node) {
            final Node v = (Node) element;
            if (v.degree() > 0) {
                throw new UnsupportedOperationException("node must be isolated");
            }
            this.nodeIdToIncidentEdgesIds.remove(id);
            this.nodeIdToDegree.remove(id);
            this.idToType.remove(id);
        }
    }

    /**
     * Compares two graph elements by their ids.
     * 
     * @param x the first element.
     * @param y the second element.
     * @return an integer indicating the sorting order.
     */
    protected int compareIds(final IGraphElement<T> x,
                             final IGraphElement<T> y) {
        return x.id().compareTo(y.id());
    }

    /**
     * Returns the given edge in its canonical orientation. The default
     * implementation orients the edge such that the source vertex id is not
     * larger then the target vertex id. Helper method for toString().
     * 
     * @param e the edge to normalize.
     * @return the normalized edge.
     */
    protected IEdge<T> normalizedEdge(final IEdge<T> e) {
        if (compareIds(e.source(), e.target()) <= 0) {
            return e;
        } else {
            return e.reverse();
        }
    }

    /**
     * Compares edges for sorting, returning, as usual, an integer indicating
     * the sorting order. For details, see {@link Comparator}. The default
     * implementation first compares source ids, then target ids, then edge ids.
     * Helper method for toString().
     * 
     * @param e1 the first edge.
     * @param e2 the second edge.
     * @return an integer indicating the sorting order.
     */
    protected int compareEdges(final IEdge<T> e1, final IEdge<T> e2) {
        int d;
        d = compareIds(e1.source(), e2.source());
        if (d != 0) {
            return d;
        }
        d = compareIds(e1.target(), e2.target());
        if (d != 0) {
            return d;
        }
        return -compareIds(e1, e2);
    }

    /**
     * Formats any additional information that may be associated to an edge. The
     * default implementation returns null.
     * 
     * @param e the edge in question.
     * @return a string containing information on this edge.
     */
    protected String formatEdgeInfo(final IEdge<T> e) {
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final List<IEdge<T>> edgeList = new ArrayList<IEdge<T>>();
        for (final Iterator<IEdge<T>> iter = edges(); iter.hasNext();) {
            edgeList.add(normalizedEdge(iter.next()));
        }
        Collections.sort(edgeList, new Comparator<IEdge<T>>() {
            public int compare(final IEdge<T> arg0, final IEdge<T> arg1) {
                return compareEdges(arg0, arg1);
            }
        });
        final List<INode<T>> isolatedNodeList = new ArrayList<INode<T>>();
        for (final Iterator<INode<T>> iter = nodes(); iter.hasNext();) {
            final INode<T> v = iter.next();
            if (v.degree() == 0) {
                isolatedNodeList.add(v);
            }
        }
        Collections.sort(isolatedNodeList, new Comparator<INode<T>>() {
            public int compare(final INode<T> arg0, final INode<T> arg1) {
                return compareIds(arg0, arg1);
            }
        });

        final StringBuffer buf = new StringBuffer(100);
        for (final IEdge<T> e: edgeList) {
            buf.append("(");
            buf.append(e.source().id());
            buf.append(",");
            buf.append(e.target().id());
            final String extra = formatEdgeInfo(e);
            if (extra != null && extra.length() > 0) {
                buf.append(",");
                buf.append(extra);
            }
            buf.append(")");
        }
        for (final INode<T> v: isolatedNodeList) {
            buf.append("(");
            buf.append(v.id());
            buf.append(")");
        }
        return buf.toString();
    }
}

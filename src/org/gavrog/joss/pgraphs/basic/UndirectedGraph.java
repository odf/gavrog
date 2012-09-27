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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.IteratorAdapter;
import org.gavrog.box.collections.Pair;


/**
 * Implements an undirected (multi-) graph. The "multi-" means that edge from a
 * node to itself ("loops") and multiple edges between the same pair of nodes
 * are allowed. The "undirected" means that. although for representation
 * purposes, edges still have a "source" and a "target" node, their roles are
 * interchangeable and, in effect, each edge is treated as equivalent to its
 * reverse.
 */
public class UndirectedGraph implements IGraph {
    private static long nextGraphId = 1;
    
    private final Long id;
    private long nextNodeId = 1;
    private long nextEdgeId = -1;

    private Map<Long, Class<? extends IGraphElement>> idToType =
            new HashMap<Long, Class<? extends IGraphElement>>();

    private Map<Long, Set<Long>> nodeIdToIncidentEdgesIds =
            new LinkedHashMap<Long, Set<Long>>();

    private Map<Long, Integer> nodeIdToDegree = new HashMap<Long, Integer>();

    private Map<Long, Long> edgeIdToSourceNodeId = new HashMap<Long, Long>();

    private Map<Long, Long> edgeIdToTargetNodeId = new HashMap<Long, Long>();

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
        return new Pair<Class<? extends UndirectedGraph>, Long>(
                getClass(), this.id);
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
    protected class Node implements INode, Comparable<INode> {
        private final long id;

        /**
         * Constructs a new node object.
         * 
         * @param id the id of this node.
         */
        public Node(final long id) {
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
        public IGraph owner() {
            return UndirectedGraph.this;
        }

        /* (non-Javadoc)
         * @see org.gavrog.joss.pgraphs.basic.INode#incidences()
         */
        public IteratorAdapter<IEdge> incidences() {
            final Set<Long> ids = nodeIdToIncidentEdgesIds.get(this.id);
            return new FilteredIterator<IEdge, Long>(ids.iterator()) {
                public IEdge filter(final Long x) {
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
        public long id() {
            return this.id;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object other) {
            if (other instanceof Node) {
                final Node v = (Node) other;
                return this.owner().id().equals(v.owner().id())
                        && this.id == v.id();
            } else {
                return false;
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return this.owner().id().hashCode() * 37 + (int) id;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "Node " + id;
        }

        public int compareTo(final INode arg0) {
            return (int) this.id() - (int) arg0.id();
        }
    }

    /**
     * Implements edge objects for this graph.
     */
    /**
     * @author olaf
     *
     */
    protected class Edge implements IEdge {
        private final long id;
        private boolean compareAsOriented;

        protected final boolean isReverse;

        /**
         * Constructs a new edge object.
         * 
         * @param id the id of this edge.
         * @param isReverse the direction of the edge.
         * @param compareAsOriented if true, use isReverse in equals() and hashCode()
         */
        public Edge(final long id, final boolean isReverse,
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
        public Edge(final long id, final boolean isReverse) {
            this(id, isReverse, false);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IEdge#source()
         */
        public INode source() {
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
        public INode target() {
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
        public INode opposite(final INode oneEnd) {
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
        public IEdge reverse() throws UnsupportedOperationException {
            return new Edge(this.id, !this.isReverse, this.compareAsOriented);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#owner()
         */
        public IGraph owner() {
            return UndirectedGraph.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#id()
         */
        public long id() {
            return this.id;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(final Object other) {
            if (other instanceof Edge) {
                final Edge e = (Edge) other;
                if (!this.owner().id().equals(e.owner().id())
                        || this.id != e.id) {
                    return false;
                } else if (this.compareAsOriented != e.compareAsOriented) {
                    return false;
                } else if (this.compareAsOriented) {
                    return this.isReverse == e.isReverse;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            final int code = this.owner().id().hashCode() * 37 + (int) id;
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
        public IEdge oriented() {
            return new Edge(id(), this.isReverse, true);
        }

        /* (non-Javadoc)
         * @see javaPGraphs.IEdge#unorientedEdge()
         */
        public IEdge unoriented() {
            return new Edge(id(), this.isReverse, false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#nodes()
     */
    public IteratorAdapter<INode> nodes() {
        return new FilteredIterator<INode, Long>(
                this.nodeIdToIncidentEdgesIds.keySet().iterator()) {
            public INode filter(final Long x) {
                return new Node(x);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#edges()
     */
    public IteratorAdapter<IEdge> edges() {
        return new FilteredIterator<IEdge, Long>(
                this.edgeIdToSourceNodeId.keySet().iterator()) {
            public IEdge filter(final Long x) {
                return new Edge(x, false);
            }
        };
    }


    public INode getNode(long id) {
        return new Node(id);
    }

    public IEdge getEdge(long id) {
        return new Edge(id, false);
    }
    

    public boolean hasNode(INode node) {
        return node != null && node.owner() == this
                && nodeIdToDegree.get(node.id()) != null;
    }

    public boolean hasEdge(IEdge edge) {
        return edge != null && edge.owner() == this
                && edgeIdToSourceNodeId.get(edge.id()) != null;
    }

    public IteratorAdapter<IEdge> connectingEdges(final INode source,
                                                  final INode target) {
        if (!hasNode(source)) {
            throw new IllegalArgumentException("source node not in graph");
        }
        if (!hasNode(target)) {
            throw new IllegalArgumentException("source node not in graph");
        }
        final long sourceId = source.id();
        final long targetId = target.id();
        final Set<Long> ids = nodeIdToIncidentEdgesIds.get(sourceId);
        return new FilteredIterator<IEdge, Long>(ids.iterator()) {
            public Edge filter(final Long x) {
                final Object s = edgeIdToSourceNodeId.get(x);
                final Object t = edgeIdToTargetNodeId.get(x);
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
    
    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newNode()
     */
    public INode newNode() {
        final Long id = new Long(nextNodeId++);
        this.idToType.put(id, Node.class);
        this.nodeIdToIncidentEdgesIds.put(id, new LinkedHashSet<Long>());
        this.nodeIdToDegree.put(id, new Integer(0));
        return new Node(id);
    }

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newEdge(javaPGraphs.INode, javaPGraphs.INode)
     */
    public IEdge newEdge(final INode source, final INode target) {
        final Long id = new Long(nextEdgeId--);
        if (!hasNode(source)) {
            throw new IllegalArgumentException("source node does not exist");
        }
        if (!hasNode(target)) {
            throw new IllegalArgumentException("target node does not exist");
        }
        final long sId = source.id();
        final long tId = target.id();
        this.idToType.put(id, Edge.class);
        this.edgeIdToSourceNodeId.put(id, sId);
        this.edgeIdToTargetNodeId.put(id, tId);
        this.nodeIdToIncidentEdgesIds.get(sId).add(id);
        this.nodeIdToIncidentEdgesIds.get(tId).add(id);
        this.nodeIdToDegree.put(sId, this.nodeIdToDegree.get(sId) + 1);
        this.nodeIdToDegree.put(tId, this.nodeIdToDegree.get(tId) + 1);
        return new Edge(id, false);
    }


    public void delete(INode v) {
        if (v.degree() > 0) {
            throw new UnsupportedOperationException("node must be isolated");
        }
        this.nodeIdToIncidentEdgesIds.remove(v.id());
        this.nodeIdToDegree.remove(v.id());
        this.idToType.remove(v.id());
    }

    public void delete(IEdge e) {
        final long eId = e.id();
        final long sId = e.source().id();
        final long tId = e.target().id();
        this.nodeIdToIncidentEdgesIds.get(sId).remove(eId);
        this.nodeIdToIncidentEdgesIds.get(tId).remove(eId);
        this.edgeIdToSourceNodeId.remove(eId);
        this.edgeIdToTargetNodeId.remove(eId);
        this.nodeIdToDegree.put(sId, this.nodeIdToDegree.get(sId) - 1);
        this.nodeIdToDegree.put(tId, this.nodeIdToDegree.get(tId) - 1);
        this.idToType.remove(eId);
    }

    /**
     * Compares two graph elements by their ids.
     * 
     * @param x the first element.
     * @param y the second element.
     * @return an integer indicating the sorting order.
     */
    protected int compareIds(final IGraphElement x, final IGraphElement y) {
        return ((Long) x.id()).compareTo((Long) y.id());
    }

    /**
     * Returns the given edge in its canonical orientation. The default
     * implementation orients the edge such that the source vertex id is not
     * larger then the target vertex id. Helper method for toString().
     * 
     * @param e the edge to normalize.
     * @return the normalized edge.
     */
    protected IEdge normalizedEdge(final IEdge e) {
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
    protected int compareEdges(final IEdge e1, final IEdge e2) {
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
    protected String formatEdgeInfo(final IEdge e) {
        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final List<IEdge> edgeList = new ArrayList<IEdge>();
        for (final IEdge e: edges()) {
            edgeList.add(normalizedEdge(e));
        }
        Collections.sort(edgeList, new Comparator<IEdge>() {
            public int compare(final IEdge arg0, final IEdge arg1) {
                return compareEdges(arg0, arg1);
            }
        });
        final List<Node> isolatedNodeList = new ArrayList<Node>();
        for (final INode v: nodes()) {
            if (v.degree() == 0) {
                isolatedNodeList.add((Node) v);
            }
        }
        Collections.sort(isolatedNodeList);

        final StringBuffer buf = new StringBuffer(100);
        for (final IEdge e: edgeList) {
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
        for (final INode v: isolatedNodeList) {
            buf.append("(");
            buf.append(v.id());
            buf.append(")");
        }
        return buf.toString();
    }
}

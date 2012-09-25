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
 * 
 * @author Olaf Delgado
 * @version $Id: UndirectedGraph.java,v 1.6 2008/02/29 03:42:23 odf Exp $
 */
public class UndirectedGraph implements IGraph {
    private static long nextGraphId = 1;
    
    private final Object id;
    private long nextNodeId = 1;
    private long nextEdgeId = -1;

    private Map idToType = new HashMap();

    private Map nodeIdToIncidentEdgesIds = new LinkedHashMap();

    private Map nodeIdToDegree = new HashMap();

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
        return new Pair(getClass(), this.id);
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
    protected class Node implements INode, Comparable {
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

        /*
         * (non-Javadoc)
         * 
         * @see javaPGraphs.IGraphElement#incidences()
         */
        public Iterator incidences() {
            final Set ids = (Set) nodeIdToIncidentEdgesIds.get(this.id);
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

        public int compareTo(Object arg0) {
            return ((Long) this.id()).intValue()
                    - ((Long) ((Node) arg0).id()).intValue();
        }
    }

    /**
     * Implements edge objects for this graph.
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
         * @see javaPGraphs.IGraphElement#incidences()
         */
        public Iterator incidences() {
            final List tmp = new LinkedList();
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
    public Iterator<INode> nodes() {
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
    public Iterator edges() {
        return new FilteredIterator<IEdge, Long>(
                this.edgeIdToSourceNodeId.keySet().iterator()) {
            public IEdge filter(final Long x) {
                return new Edge(x, false);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#getElement(java.lang.Object)
     */
    public IGraphElement getElement(final Long id) {
        if (id == null) {
            return null;
        }
        final Class type = (Class) this.idToType.get(id);
        if (Node.class.equals(type)) {
            return new Node(id);
        } else if (Edge.class.equals(type)) {
            return new Edge(id, false);
        } else {
            return null;
        }
    }

    public IGraphElement getElement(final long id) {
    	return getElement(new Long(id));
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#hasElement(javaPGraphs.IGraphElement)
     */
    public boolean hasElement(final IGraphElement element) {
        if (element == null || element.owner() != this) {
            return false;
        }
        final Class type = (Class) this.idToType.get(element.id());
        if (Node.class.equals(type)) {
            return element instanceof Node;
        } else if (Edge.class.equals(type)) {
            return element instanceof Edge;
        } else {
            return false;
        }
    }

    public boolean hasElement(final long id) {
    	return getElement(id) != null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#connectingEdges(javaPGraphs.INode,
     *      javaPGraphs.INode)
     */
    public Iterator connectingEdges(final INode node1, final INode node2) {
        return directedEdges(node1, node2);
    }

    public Iterator connectingEdges(final long i, final long j) {
    	return directedEdges(i, j);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#directedEdges(javaPGraphs.INode,
     *      javaPGraphs.INode)
     */
    public Iterator directedEdges(final INode source, final INode target) {
        if (!hasElement(source)) {
            throw new IllegalArgumentException("source node not in graph");
        }
        if (!hasElement(target)) {
            throw new IllegalArgumentException("source node not in graph");
        }
        final Object sourceId = source.id();
        final Object targetId = target.id();
        final Set ids = (Set) nodeIdToIncidentEdgesIds.get(sourceId);
        return new FilteredIterator<Edge, Long>(ids.iterator()) {
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

    public Iterator directedEdges(final long i, final long j) {
    	return directedEdges((INode) getElement(i), (INode) getElement(j));
    }
    
    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newNode()
     */
    public INode newNode() {
        final Long id = new Long(nextNodeId++);
        this.idToType.put(id, Node.class);
        this.nodeIdToIncidentEdgesIds.put(id, new LinkedHashSet());
        this.nodeIdToDegree.put(id, new Integer(0));
        return new Node(id);
    }

    /* (non-Javadoc)
     * @see javaPGraphs.IGraph#newEdge(javaPGraphs.INode, javaPGraphs.INode)
     */
    public IEdge newEdge(final INode source, final INode target) {
        final Long id = new Long(nextEdgeId--);
        if (!hasElement(source)) {
            throw new IllegalArgumentException("source node does not exist");
        }
        if (!hasElement(target)) {
            throw new IllegalArgumentException("target node does not exist");
        }
        final long sId = source.id();
        final long tId = target.id();
        this.idToType.put(id, Edge.class);
        this.edgeIdToSourceNodeId.put(id, sId);
        this.edgeIdToTargetNodeId.put(id, tId);
        ((Set) this.nodeIdToIncidentEdgesIds.get(sId)).add(id);
        ((Set) this.nodeIdToIncidentEdgesIds.get(tId)).add(id);
        this.nodeIdToDegree.put(sId, new Integer(((Integer) this.nodeIdToDegree.get(sId))
                .intValue() + 1));
        this.nodeIdToDegree.put(tId, new Integer(((Integer) this.nodeIdToDegree.get(tId))
                .intValue() + 1));
        return new Edge(id, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javaPGraphs.IGraph#delete(javaPGraphs.IGraphElement)
     */
    public void delete(final IGraphElement element) {
        if (!hasElement(element)) {
            throw new IllegalArgumentException("no such element");
        }
        final Object id = element.id();
        if (element instanceof Edge) {
            final Edge e = (Edge) element;
            final Object sId = e.source().id();
            final Object tId = e.target().id();
            ((Set) this.nodeIdToIncidentEdgesIds.get(sId)).remove(id);
            ((Set) this.nodeIdToIncidentEdgesIds.get(tId)).remove(id);
            this.edgeIdToSourceNodeId.remove(id);
            this.edgeIdToTargetNodeId.remove(id);
            this.nodeIdToDegree.put(sId, new Integer(((Integer) this.nodeIdToDegree
                    .get(sId)).intValue() - 1));
            this.nodeIdToDegree.put(tId, new Integer(((Integer) this.nodeIdToDegree
                    .get(tId)).intValue() - 1));
            this.idToType.remove(id);
        } else if (element instanceof Node) {
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
        final List edgeList = new ArrayList();
        for (final Iterator iter = edges(); iter.hasNext();) {
            edgeList.add(normalizedEdge((IEdge) iter.next()));
        }
        Collections.sort(edgeList, new Comparator() {
            public int compare(final Object arg0, final Object arg1) {
                return compareEdges((IEdge) arg0, (IEdge) arg1);
            }
        });
        final List isolatedNodeList = new ArrayList();
        for (final Iterator iter = nodes(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            if (v.degree() == 0) {
                isolatedNodeList.add(v);
            }
        }
        Collections.sort(isolatedNodeList, new Comparator() {
            public int compare(final Object arg0, final Object arg1) {
                return compareIds((INode) arg0, (INode) arg1);
            }
        });

        final StringBuffer buf = new StringBuffer(100);
        for (final Iterator iter = edgeList.iterator(); iter.hasNext();) {
            final IEdge e = (IEdge) iter.next();
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
        for (final Iterator iter = isolatedNodeList.iterator(); iter.hasNext();) {
            final INode v = (INode) iter.next();
            buf.append("(");
            buf.append(v.id());
            buf.append(")");
        }
        return buf.toString();
    }
}

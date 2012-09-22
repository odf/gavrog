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

import java.util.Iterator;

/**
 * Common interface for graph classes (not necessarily simple).
 */
public interface IGraph<T> {
    /**
     * Retrieves the identifier for this graph. Every graph object must have a
     * unique identifier.
     * 
     * @return the identifier for this graph.
     */
    public Object id();
    
    /**
     * Returns the number of nodes of this graph. Optional method, as the number
     * might be unknown or infinite.
     * 
     * @return the number of nodes.
     */
    public int numberOfNodes();
    
    /**
     * Returns the number of edges of this graph. Optional method, as the number
     * might be unknown or infinite.
     * 
     * @return the number of edges.
     */
    public int numberOfEdges();
    
    /**
     * Retrieves all the nodes of this graph in the order they were added.
     * 
     * @return an iterator over the node set.
     */
    public Iterator<INode<T>> nodes();

    /**
     * Retrieves all the edges of this graph in the order they were added.
     * 
     * @return an iterator over the edge set.
     */
    public Iterator<IEdge<T>> edges();

    /**
     * Retrieves an element of this graph with the given identifier.
     * 
     * @param id the element identifier.
     * @return the specified element or null, if none exists.
     */
    public IGraphElement<T> getElement(final T id);

    /**
     * Checks if this graph contains a certain element (node or edge).
     * 
     * @param element the element to search.
     * @return true if the given element is contained in this graph.
     */
    public boolean hasElement(final IGraphElement<T> element);

    /**
     * Retrieves all the connecting edges between a given pair of nodes, without
     * regard for their direction.
     * 
     * @param node1 the first node.
     * @param node2 the second node.
     * @return an iterator over the set of connections.
     */
    public Iterator<IEdge<T>> connectingEdges(
    		final INode<T> node1, final INode<T> node2);

    /**
     * Retrieves the directed edges from a given source to a given target. In an
     * undirected graph, this is equivalent to
     * {@link #connectingEdges(INode, INode)}.
     * 
     * @param source the source node.
     * @param target the target node.
     * @return an iterator over the set of edges.
     */
    public Iterator<IEdge<T>> directedEdges(
    		final INode<T> source, final INode<T> target);

    /**
     * Creates a new node.
     * 
     * @return the newly created node.
     */
    public INode<T> newNode();

    /**
     * Creates a new edge between the given nodes.
     * 
     * @param source the source node.
     * @param target the target node.
     * @return the newly created edge.
     */
    public IEdge<T> newEdge(final INode<T> source, final INode<T> target);

    /**
     * Removes an element from the graph.
     * @param element the element to remove.
     */
    public void delete(final IGraphElement<T> element);
}

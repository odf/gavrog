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


/**
 * Interface for the representation of edges in a graph. Here, edges are always
 * directed, i.e., they have defined source and target nodes. In an undirected
 * graph, this means that the reverse edge is by definition always present.
 */
public interface IEdge extends IGraphElement {
    /**
     * Retrieves the source node of this edge.
     * 
     * @return the source node.
     */
    public INode source();

    /**
     * Retrieves the target node of this edge.
     * 
     * @return the target node.
     */
    public INode target();

    /**
     * If an end of this edge is given, retrieves the opposite end, otherwise
     * throws an exception.
     * 
     * @param oneEnd one end of this edge.
     * @return the opposite end.
     */
    public INode opposite(final INode oneEnd);

    /**
     * Retrieves the reverse of this edge, if present.
     * 
     * @return the reverse edge.
     * @throws UnsupportedOperationException if the reverse does not exist.
     */
    public IEdge reverse() throws UnsupportedOperationException;

    /**
     * Returns an oriented version of this edge. The only difference is that
     * the new edge will be regarded as different from its reverse.
     * 
     * @return an oriented version of this edge.
     */
    public IEdge oriented();

    /**
     * Returns an unoriented version of this edge. The only difference is that
     * the new edge will not be regarded as different from its reverse.
     * 
     * @return an unoriented version of this edge.
     */
    public IEdge unoriented();
}

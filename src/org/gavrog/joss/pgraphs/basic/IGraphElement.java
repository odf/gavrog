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
 * A common interface for all the nodes and edges of a graph.
 */
public interface IGraphElement {
    /**
     * Retrieves the graph that owns this element.
     * 
     * @return the graph this element is in.
     */
    public IGraph owner();

    /**
     * Retrieves the identifier for this element. The graph implementation must
     * make sure that every element of a given graph has a unique identifier.
     * 
     * @return the identifier for this element.
     */
    public long id();
}

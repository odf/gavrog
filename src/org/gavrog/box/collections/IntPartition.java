/*
   Copyright 2005 Olaf Delgado-Friedrichs

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

package org.gavrog.box.collections;

import java.util.Map;

/**
 * A proxy for class Partitition which only works with int entries.
 * @author Olaf Delgado
 * @version $Id: IntPartition.java,v 1.1 2005/07/18 23:32:58 odf Exp $
 */
public class IntPartition {

    final private Partition partition;
    
    private IntPartition(final Partition P) {
        this.partition = P;
    }
    
    public IntPartition() {
        this(new Partition());
    }
    
    public IntPartition(final IntPartition P) {
        this((Partition) P.partition.clone());
    }
    
    public boolean areEquivalent(int a, int b) {
        return this.partition.areEquivalent(new Integer(a), new Integer(b));
    }

    public int find(int a) {
        return ((Integer) this.partition.find(new Integer(a))).intValue();
    }

    public void unite(int a, int b) {
        this.partition.unite(new Integer(a), new Integer(b));
    }
    
	public Map representativeMap() {
	    return this.partition.representativeMap();
	}
}

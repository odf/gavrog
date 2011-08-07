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

/**
 * A proxy for Pair which forms pairs of ints.
 * @author Olaf Delgado
 * @version $Id: IntPair.java,v 1.1 2005/07/18 23:32:58 odf Exp $
 */
public class IntPair {
    final private Pair pair;
    
    public IntPair(int first, int second) {
        this.pair = new Pair(new Integer(first), new Integer(second));
    }

    public int getFirst() {
        return ((Integer) this.pair.getFirst()).intValue();
    }

    public int getSecond() {
        return ((Integer) this.pair.getSecond()).intValue();
    }
    
    public boolean equals(Object other) {
        return this.pair.equals(other);
    }
    
    public int hashCode() {
        return this.pair.hashCode();
    }
    
    public String toString() {
        return this.pair.toString();
    }
}

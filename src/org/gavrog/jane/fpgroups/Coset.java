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

package org.gavrog.jane.fpgroups;

/**
 * Represents cosets.
 */
public class Coset<E, D> {
    final private CosetAction<E, D> action;
    final private int index;
    final private FreeWord<E> representative;
    
    /**
     * Constructs a Coset instance for a given table row number.
     * @param action the coset action this is a coset of.
     * @param index the row number corresponding to the coset.
     */
    Coset(final CosetAction<E, D> action, final int index) {
        if (index < 1 || index > action.size()) {
            throw new IllegalArgumentException("no such coset index");
        }

        this.action = action;
        this.index = index;
        this.representative = this.action.cosetRepresentatives[index];
    }
    
    /**
     * Returns the coset action object for this coset.
     * @return the coset action.
     */
    public CosetAction<E, D> getAction() {
        return this.action;
    }
    
    /**
     * Returns the index.
     * @return this coset's index.
     */
    int getIndex() {
        return this.index;
    }
    
    /**
     * Returns the representative.
     * @return this coset's representative.
     */
    public FreeWord<E> getRepresentative() {
        return this.representative;
    }
    
    public boolean equals(final Coset<?, ?> other) {
            return this.getAction() == other.getAction()
                    && this.getIndex() == other.getIndex();
    }
    
    public boolean equals(Object other) {
        if (other instanceof Coset<?, ?>) {
            return this.equals((Coset<?, ?>) other);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return getAction().hashCode() * 37 + getIndex();
    }
    
    public String toString() {
        return getRepresentative().toString();
    }
}
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

import java.util.Iterator;

/**
 * Interface for group action (from the right).
 */
public interface GroupAction<E, D> {
    /**
     * Retrieves the group for which this action is defined.
     * @return the acting group.
     */
    public FpGroup<E> getGroup();
    
    /**
     * Returns an iterator over the set on which the group acts (its domain).
     * @return an iterator.
     */
    public Iterator<D> domain();
    
    /**
     * Returns the size of the domain, if known and finite, or else throws a
     * {@link UnsupportedOperationException}.
     * 
     * @return the size of the domain.
     */
    public int size();
    
    /**
     * Applies a group element to an object. Actions are from the right, so
     * <code>apply(apply(x, w1), w2)</code> returns the same as
     * <code>apply(x, w1.times(w2))</code>. If the given word is not in the
     * group for which this action is defined or if the given Object is not in
     * the set on which the group acts, <code>null</code> is returned.
     * 
     * @param x an object.
     * @param w a word.
     * @return the result of applying <code>w</code> to <code>x</code> or
     *             <code>null</code>, if undefined.
     */
    public D apply(D x, FreeWord<E> w);
    
    /**
     * Checks if a given object is in the domain of this group action.
     * @param x the object to test.
     * @return true if action if defined on x.
     */
    public boolean isDefinedOn(D x);
}

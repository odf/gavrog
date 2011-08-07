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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * An abstract base class for iterators with single element cacheing. Derived
 * classes need only implement the method <code>findNext</code>.
 * @author Olaf Delgado
 * @version $Id: IteratorAdapter.java,v 1.3 2007/06/14 12:07:02 odf Exp $
 */
public abstract class IteratorAdapter implements Iterator {
    private LinkedList cache = new LinkedList();

    /**
     * Returns the next available element or throws an exception.
     * @return the next element.
     * @throws NoSuchElementException if no more elements are available.
     */
    protected abstract Object findNext() throws NoSuchElementException;
    
    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if (cache.size() == 0) {
            try {
                cache.addLast(findNext());
            } catch (NoSuchElementException ex) {
                return false;
            }
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if (cache.size() == 0) {
            return findNext();
        } else {
            return cache.removeFirst();
        }
    }
    
    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException("not supported");
    }
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator iterator() {
        return this;
    }
}

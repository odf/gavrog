/*
   Copyright 2008 Olaf Delgado-Friedrichs

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


package org.gavrog.joss.algorithms;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import buoy.event.EventSource;

/**
 * @author Olaf Delgado
 * @version $Id:$
 */
public abstract class ResumableGenerator<T> extends EventSource implements
		Iterator<T>, Iterable<T> {

	// -- the methods a concrete subtype to implement
	public abstract String getCheckpoint();

	public abstract void setResumePoint(final String spec);

	protected abstract T findNext();
	
	// -- cache for results generated in calls to hasNext()
    private LinkedList<T> cache = new LinkedList<T>();
    
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
    public T next() {
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
    public Iterator<T> iterator() {
        return this;
    }
}

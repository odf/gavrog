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

package org.gavrog.box.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides a framework for modify an iterator on the fly. This is done by
 * passing the iterator in question to the constructor of a derived class
 * (possibly anonymous) which implements the {@link #filter(Object)} method.
 */
public abstract class FilteredIterator<E, S> extends IteratorAdapter<E> {

	Iterator<? extends S> original;
	
	/**
	 * Constructs a FilteredIterator instance.
	 * @param original
	 */
	public FilteredIterator(Iterator<? extends S> original) {
		this.original = original;
	}

	/**
	 * The filter method to be provided by derived classes. Returns a modified
	 * object or <code>null</code> if the object should be skipped.
     * 
	 * @param x the object to inspect.
	 * @return true if the object should be passed through.
	 */
	public abstract E filter(S item);
	
    /**
     * This methods finds and caches the next result of the traversal, unless
     * there is already a result cached.
     */
	protected E findNext() throws NoSuchElementException {
        while (original.hasNext()) {
            S x = original.next();
            E y = filter(x);
            if (y != null) {
                return y;
            }
        }
        throw new NoSuchElementException("at end");
    }
}

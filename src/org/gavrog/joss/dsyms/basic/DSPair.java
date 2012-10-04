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

package org.gavrog.joss.dsyms.basic;

import org.gavrog.box.collections.Pair;

/**
 * This class represents edges in a Delaney symbol.
 */
public class DSPair<T> extends Pair<Integer, T> {

	public DSPair(int i, T D) {
		super(new Integer(i), D);
    }
    
    public int getIndex() {
        return getFirst();
    }
    
    public T getElement() {
        return getSecond();
    }
    
    public DSPair<T> reverse(final DelaneySymbol<T> ds) {
        final int i = getIndex();
        final T D = getElement();
        return new DSPair<T>(i, ds.op(i, D));
    }
}


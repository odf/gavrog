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

package org.gavrog.jane.fpgroups;

import java.util.Collection;
import java.util.Iterator;

/**
 * A convenient base class for group actions on finite sets. Derived classes
 * should only need to override {@link #applyGenerator(Object, int, int)}.
 * 
 * @author Olaf Delgado
 * @version $Id: FiniteGroupAction.java,v 1.1.1.1 2005/07/15 21:58:38 odf Exp $
 */
public abstract class FiniteGroupAction implements GroupAction {
    private FpGroup group;
    private Collection domain;

    /**
     * Applies a generator or inverse generator the an element of the domain.
     * 
     * @param x the element.
     * @param letter the index of the generator to apply.
     * @param sign if negative, apply the inverse.
     * @return the result.
     */
    protected abstract Object applyGenerator(final Object x, final int letter,
            final int sign);
    
    /**
     * Constructs an instance.
     * 
     * @param group the acting group.
     * @param domain the domain on which the group acts.
     */
    public FiniteGroupAction(final FpGroup group, final Collection domain) {
        this.group = group;
        this.domain = domain;
    }
    
    /* (non-Javadoc)
     * @see javaDSym.fpgroups.GroupAction#getGroup()
     */
    public FpGroup getGroup() {
        return this.group;
    }

    /* (non-Javadoc)
     * @see javaDSym.fpgroups.GroupAction#domain()
     */
    public Iterator domain() {
        return this.domain.iterator();
    }

    /* (non-Javadoc)
     * @see javaDSym.fpgroups.GroupAction#size()
     */
    public int size() {
        return this.domain.size();
    }

    /* (non-Javadoc)
     * @see javaDSym.fpgroups.GroupAction#apply(java.lang.Object, javaDSym.fpgroups.FreeWord)
     */
    public Object apply(Object x, FreeWord w) {
        if (isDefinedOn(x)) {
            Object z = x;
            for (int i = 0; i < w.length(); ++i) {
                z = applyGenerator(z, w.getLetter(i), w.getSign(i));
            }
            return z;
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see javaDSym.fpgroups.GroupAction#isDefinedOn(java.lang.Object)
     */
    public boolean isDefinedOn(Object x) {
        return this.domain.contains(x);
    }
}

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

package org.gavrog.joss.dsyms.basic;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Convenience class to represent lists of Delaney symbol indices.
 * @author Olaf Delgado
 * @version $Id: IndexList.java,v 1.2 2007/04/19 23:07:42 odf Exp $
 */
public class IndexList extends ArrayList<Integer> {

    public IndexList(final DelaneySymbol ds) {
        this(ds.indices());
    }

    public IndexList(final Iterator<Integer> iter) {
        while (iter.hasNext()) {
            add(iter.next());
        }
    }

    public IndexList(final int i) {
        add(new Integer(i));
    }

    public IndexList(final int i, final int j) {
        add(new Integer(i));
        add(new Integer(j));
    }

    public IndexList(final int i, final int j, final int k) {
        add(new Integer(i));
        add(new Integer(j));
        add(new Integer(k));
    }

    public static IndexList except(final DelaneySymbol ds, final int i) {
        final IndexList res = new IndexList(ds);
        res.remove(new Integer(i));
        return res;
    }

    public static IndexList except(final DelaneySymbol ds, final int i,
            final int j) {
        final IndexList res = new IndexList(ds);
        res.remove(new Integer(i));
        res.remove(new Integer(j));
        return res;
    }
}

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

/*
 * Created on Feb 21, 2005 by olaf.
 */
package org.gavrog.joss.dsyms.basic;

import org.gavrog.joss.dsyms.basic.IndexList;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestIndexList.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestIndexList extends TestCase {
    public void testVarargsConstructor() {
        final IndexList listA = new IndexList(1,2,3);
        assertEquals(3, listA.size());
        assertEquals(new Integer(3), listA.get(2));
    }
}

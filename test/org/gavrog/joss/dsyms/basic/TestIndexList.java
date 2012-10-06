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

/*
 * Created on Feb 21, 2005 by olaf.
 */
package org.gavrog.joss.dsyms.basic;

import org.gavrog.joss.dsyms.basic.IndexList;

import junit.framework.TestCase;

/**
 */
public class TestIndexList extends TestCase {
	private DSymbol ds;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		ds = new DSymbol("6:1 3 4 6,2 4 5 6,1 5 6 4:4 4,3 3");
	}

	public void testVarargsConstructor() {
        final IndexList listA = new IndexList(1,2,3);
        assertEquals(3, listA.size());
        assertEquals(new Integer(3), listA.get(2));
    }
	
	public void testExcept() {
		final IndexList listA = IndexList.except(ds, 0);
		assertEquals(2, listA.size());
		assertEquals(new Integer(1), listA.get(0));
		assertEquals(new Integer(2), listA.get(1));
		final IndexList listB = IndexList.except(ds, 0, 2);
		assertEquals(1, listB.size());
		assertEquals(new Integer(1), listB.get(0));
	}
}

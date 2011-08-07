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

import org.gavrog.jane.fpgroups.FiniteAlphabet;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class Alphabet.
 * @author Olaf Delgado
 * @version $Id: TestFiniteAlphabet.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestFiniteAlphabet extends TestCase {
	private FiniteAlphabet A;
	private FiniteAlphabet sameAsA;
	private FiniteAlphabet otherThanA;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		A = new FiniteAlphabet("s", 5);
		sameAsA = new FiniteAlphabet(new String[] { "s1", "s2", "s3", "s4",
                "s5" });
		otherThanA = new FiniteAlphabet(new String[] { "s1", "s2", "s3", "s4",
                "s_5" });
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		A = null;
		sameAsA = null;
		otherThanA = null;
		super.tearDown();
	}

	public void testConstructors() {
		Object names[] = { "a", "b", "c", new Integer(3) };
		FiniteAlphabet B = new FiniteAlphabet(names);
		Assert.assertTrue(B.size() == 4);
		Assert.assertEquals(B.letterToName(1), "a");
		Assert.assertEquals(B.letterToName(3), "c");
		Assert.assertEquals(B.letterToName(4), new Integer(3));
	}
	
	public void testSize() {
		Assert.assertTrue(A.size() == 5);
	}
	
	public void testGet() {
		Assert.assertEquals(A.letterToName(1), "s1");
		Assert.assertEquals(A.letterToName(5), "s5");
		try {
			A.letterToName(0);
			fail("should throw an IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException success) {
		}
		try {
			A.letterToName(6);
			fail("should throw an IndexOutOfBoundsException");
		} catch (IndexOutOfBoundsException success) {
		}
	}
	
	public void testToString() {
		Assert.assertEquals(
				"Alphabet({\"s1\", \"s2\", \"s3\", \"s4\", \"s5\"})",
				A.toString());
	}
	
	public void testEquals() {
	    Assert.assertEquals(A, sameAsA);
	    Assert.assertNotSame(A, otherThanA);
	}
	
	public void testHashCode() {
	    Assert.assertTrue(A.hashCode() == sameAsA.hashCode());
	    Assert.assertFalse(A.hashCode() == otherThanA.hashCode());
	}
}

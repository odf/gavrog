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
import org.gavrog.jane.fpgroups.FreeWord;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class FreeWord.
 * @author Olaf Delgado
 * @version $Id: TestFreeWord.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestFreeWord extends TestCase {
	private FiniteAlphabet A;
	private FreeWord w1;
	private FreeWord w2;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		A = new FiniteAlphabet(new String[] { "a", "b", "c" });
		w1 = new FreeWord(A, new int[] { 2, -3, 2, -2, -1, -2 });
		w2 = new FreeWord(A, "b*a*c*b*a");
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		w1 = null;
		A = null;
		super.tearDown();
	}

	public void testSmallWords() {
		Assert.assertEquals("*", new FreeWord(A).toString());
		Assert.assertEquals("a", new FreeWord(A, 1).toString());
		Assert.assertEquals("b", new FreeWord(A, "b").toString());
	}
	
	public void testParser() {
	    Assert.assertEquals("a*b*c*b*c*b*b*c*b*c*b",
	            new FreeWord(A, "a * (c*( c ^ -1*b^-1 )^3 ) ^-2").toString());
	    Assert.assertEquals(new FreeWord(A, "[a,c*b]"),
	            new FreeWord(A, "a*c * b*a^-1*b^-1* c ^ -1"));
	}
	
	public void testCompare() {
	    Assert.assertTrue(w1.compareTo(w2) > 0);
	    Assert.assertTrue(w1.compareTo(w1) == 0);
	    Assert.assertTrue(w1.compareTo(new FreeWord(A, "b*(b*a*c)^-1*a")) < 0);
	}
	
	public void testSize() {
	    Assert.assertEquals(4, w1.size());
	    Assert.assertEquals(5, w2.size());
	}
	
	public void testGetLetter() {
		Assert.assertEquals("b", w1.getLetterName(0));
		Assert.assertEquals("c", w1.getLetterName(1));
		Assert.assertEquals("a", w1.getLetterName(2));
	}
	
	public void testGetSign() {
		Assert.assertTrue(w1.getSign(3) == -1);
	}
	
	public void testSubword() {
		Assert.assertEquals("a^-1*b^-1", w1.subword(2, 4).toString());
		try {
			w1.subword(-1, 4);
			fail("should raise a IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		try {
			w1.subword(4, 4);
			fail("should raise a IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		try {
			w1.subword(2, 1);
			fail("should raise a IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		try {
			w1.subword(2, 5);
			fail("should raise a IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
	}
	
	public void testTimes() {
		Assert.assertEquals("b*b*a", w1.times(w2).toString());
		Assert.assertEquals("*", w1.times(w1.inverse()).toString());
	}
	
	public void testRaisedTo() {
	    Assert.assertEquals("b*c^-1*a^-1*b^-1", w1.raisedTo(1).toString());
	    Assert.assertEquals("*", w2.raisedTo(0).toString());
	    Assert.assertEquals("*", new FreeWord(A).raisedTo(-5).toString());
	    Assert.assertEquals("a^-1*b^-1*c^-1*a^-1*b^-1",
	            	w2.raisedTo(-1).toString());
	    Assert.assertEquals("b*c^-1*a^-1*c^-1*a^-1*c^-1*a^-1*b^-1",
	            w1.raisedTo(3).toString());
	    Assert.assertEquals("b*a*c*a*c*a*c*a*c*a*c*b^-1",
	            w1.raisedTo(-5).toString());
	}
	
	public void testToString() {
		Assert.assertEquals("b*c^-1*a^-1*b^-1", w1.toString());
	}
}

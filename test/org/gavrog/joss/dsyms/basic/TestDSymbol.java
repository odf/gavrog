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

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class DSymbol.
 * @author Olaf Delgado
 * @version $Id: TestDSymbol.java,v 1.3 2005/10/20 00:01:44 odf Exp $
 */
public class TestDSymbol extends TestCase {
	private DSymbol ds1;
	private DSymbol ds2;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		ds1 = new DSymbol("6:1 3 4 6,2 4 5 6,1 5 6 4:4 4,3 3");
		ds2 = new DSymbol("6:1 3 0 6,2 4 5 6,1 5 6 4:8 6,0 3");
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		ds1 = null;
		ds2 = null;
		super.tearDown();
	}

	public void testDim() {
		Assert.assertTrue(ds1.dim() == 2);
	}

	public void testSize() {
		Assert.assertTrue(ds1.size() == 6);
	}

	public void testElements() {
		Assert.assertTrue(Iterators.equal(ds1.elements(), Iterators.range(1, 7)));
	}

	public void testIsElement() {
		Assert.assertTrue(ds1.hasElement(new Integer(3)));
		Assert.assertFalse(ds1.hasElement(new Integer(7)));
		Assert.assertFalse(ds1.hasElement(new Integer(0)));
	}

	public void testIndices() {
		Assert.assertTrue(Iterators.equal(ds1.indices(), Iterators.range(0, 3)));
	}

	public void testIsIndex() {
		Assert.assertTrue(ds1.hasIndex(0));
		Assert.assertTrue(ds1.hasIndex(2));
		Assert.assertFalse(ds1.hasIndex(-1));
		Assert.assertFalse(ds1.hasIndex(3));
	}

	public void testOpDefined() {
		Assert.assertTrue(ds2.definesOp(1, new Integer(1)));
		Assert.assertFalse(ds2.definesOp(0, new Integer(4)));
		Assert.assertFalse(ds2.definesOp(0, new Integer(7)));
		Assert.assertFalse(ds2.definesOp(-1, new Integer(1)));
	}

	public void testOp() {
		Assert.assertEquals(ds1.op(1, new Integer(1)), new Integer(2));
		Assert.assertEquals(ds2.op(0, new Integer(4)), null);
		try {
			ds1.op(0, new Integer(7));
			fail("Should raise an IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		try {
			ds1.op(3, new Integer(4));
			fail("Should raise an IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
	}

	public void testVDefined() {
		Assert.assertTrue(ds2.definesV(0, 1, new Integer(2)));
		Assert.assertTrue(ds2.definesV(0, 2, new Integer(2)));
		Assert.assertFalse(ds2.definesV(1, 2, new Integer(2)));
		Assert.assertFalse(ds2.definesV(0, 1, new Integer(7)));
		Assert.assertFalse(ds2.definesV(1, 3, new Integer(1)));
	}

	public void testV() {
		Assert.assertTrue(ds1.v(1, 2, new Integer(1)) == 1);
		Assert.assertTrue(ds1.v(0, 2, new Integer(1)) == 2);
		Assert.assertTrue(ds2.v(1, 2, new Integer(2)) == 0);
		try {
			ds1.v(0, 1, new Integer(7));
			fail("Should raise an IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		try {
			ds1.v(0, 3, new Integer(4));
			fail("Should raise an IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
	}

	/*
	 * Class under test for void DSymbol(String)
	 */
	public void testConstructors() {
		final int op[][] = {{0, 1, 2}, {0, 1, 2}, {0, 2, 1}};
		final int v[][] = {{0, 6, 3}, {0, 2, 2}};
		final String s = "2:1 2, 1 2, 2:6 3,4";
		Assert.assertEquals(
				new DSymbol(op, v).toString(),
				new DSymbol(s).toString());
        final String s1 = "<1.1:2:1 2, 1 2, 2:6 3,4>";
        Assert.assertEquals(
                new DSymbol(op, v).toString(),
                new DSymbol(s1).toString());
		final String code = "8 3:2 8 7 6,3 4 5 6 8,1 2 8 5 7,2 4 6 8:3 4,5 3,4 3";
		final DSymbol tmp = new DSymbol(code);
		final DelaneySymbol sub = new Subsymbol(tmp, new IndexList(1, 2),
                new Integer(1));
		final String out = ""
			   + "   D |  op0  op1 |  v01\n"
			   + "-----+-----------+------\n"
			   + "   1 |    2    1 |    1\n"
			   + "   2 |    1    5 |    1\n"
			   + "   3 |    3    4 |    1\n"
			   + "   4 |    5    3 |    1\n"
			   + "   5 |    4    2 |    1\n"
			   ;
		Assert.assertEquals(out, new DSymbol(sub).tabularDisplay());
        Assert.assertEquals(out, new DSymbol(new DSymbol(sub)).tabularDisplay());
	}

	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
		Assert.assertEquals(ds1.toString(),
				"<1.1:6:1 3 4 6,2 4 5 6,1 5 6 4:4 4,3 3>");
		Assert.assertEquals(ds2.toString(),
				"<1.1:6:1 3 0 6,2 4 5 6,1 5 6 4:8 6,0 3>");
	}

	/*
	 * Class under test for Object clone()
	 */
	public void testClone() {
		Assert.assertEquals(ds1.toString(), ds1.clone().toString());
		Assert.assertEquals(
				ds2.toString(),
				ds2.clone().toString());
	}

	public void testDual() {
		Assert.assertEquals(ds1.dual().toString(),
				"<1.1:6:1 5 6 4,2 4 5 6,1 3 4 6:3 3,4 4>");
		Assert.assertEquals(ds2.dual().toString(),
				"<1.1:6:1 5 6 4,2 4 5 6,1 3 0 6:0 3,8 6>");
	}
}

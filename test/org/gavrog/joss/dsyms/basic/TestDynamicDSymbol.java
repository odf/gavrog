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

import java.util.LinkedList;
import java.util.List;

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class DynamicDSymbol.
 * 
 * @author Olaf Delgado
 * @version $Id: TestDynamicDSymbol.java,v 1.2 2005/07/18 23:32:58 odf Exp $
 */
public class TestDynamicDSymbol extends TestCase {
    final static private Integer one = new Integer(1);
    final static private Integer two = new Integer(2);
    final static private Integer three = new Integer(3);
    final static private Integer four = new Integer(4);
    final static private Integer five = new Integer(5);
    final static private Integer six = new Integer(6);
    final static private Integer seven = new Integer(7);
    
	private DynamicDSymbol ds1;
	private DynamicDSymbol ds2;

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		ds1 = new DynamicDSymbol(new DSymbol("6:1 3 4 6,2 4 5 6,1 5 6 4:4 4,3 3"));
		ds2 = new DynamicDSymbol("6:1 3 0 6,2 4 5 6,1 5 6 4:8 6,0 3");
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

	public void testHasElement() {
		Assert.assertTrue(ds1.hasElement(new Integer(3)));
		Assert.assertFalse(ds1.hasElement(new Integer(7)));
		Assert.assertFalse(ds1.hasElement(new Integer(0)));
	}

	public void testIndices() {
		Assert.assertTrue(Iterators.equal(ds1.indices(), Iterators.range(0, 3)));
	}

	public void testHasIndex() {
		Assert.assertTrue(ds1.hasIndex(0));
		Assert.assertTrue(ds1.hasIndex(2));
		Assert.assertFalse(ds1.hasIndex(-1));
		Assert.assertFalse(ds1.hasIndex(3));
	}

	public void testDefinesOp() {
		Assert.assertTrue(ds2.definesOp(1, one));
		Assert.assertFalse(ds2.definesOp(0, four));
		Assert.assertFalse(ds2.definesOp(0, seven));
		Assert.assertFalse(ds2.definesOp(-1, one));
	}

	public void testOp() {
		Assert.assertEquals(ds1.op(1, one), two);
		Assert.assertEquals(ds2.op(0, four), new Integer(0));
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

	public void testDefinesV() {
		Assert.assertTrue(ds2.definesV(0, 1, two));
		Assert.assertTrue(ds2.definesV(0, 2, two));
		Assert.assertFalse(ds2.definesV(1, 2, two));
		Assert.assertFalse(ds2.definesV(0, 1, seven));
		Assert.assertFalse(ds2.definesV(1, 3, one));
	}

	public void testV() {
		Assert.assertTrue(ds1.v(1, 2, one) == 1);
		Assert.assertTrue(ds1.v(0, 2, one) == 2);
		Assert.assertTrue(ds2.v(1, 2, two) == 0);
		try {
			ds1.v(0, 1, seven);
			fail("Should raise an IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		try {
			ds1.v(0, 3, four);
			fail("Should raise an IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
	}
	
	public void testUndefineOp() {
	    Assert.assertTrue(ds1.definesOp(0, two));
	    Assert.assertTrue(ds1.definesOp(0, three));
	    ds1.undefineOp(0, two);
	    Assert.assertEquals(new Integer(0), ds1.op(0, two));
	    Assert.assertTrue(ds1.definesOp(0, one));
	    Assert.assertTrue(ds1.definesOp(1, two));
	    Assert.assertFalse(ds1.definesOp(0, two));
	    Assert.assertFalse(ds1.definesOp(0, three));
	    
	    Assert.assertFalse(ds1.definesOp(0, three));
	    ds2.undefineOp(0, three);
	    Assert.assertFalse(ds1.definesOp(0, three));
	}
	
	public void testRedefineOp() {
	    ds1.redefineOp(0, one, two);
	    Assert.assertEquals(two, ds1.op(0, one));
	    Assert.assertEquals(one, ds1.op(0, two));
	    Assert.assertEquals(new Integer(0), ds1.op(0, three));
	}
	
	public void testUndefineV() {
	    ds1.undefineV(0, 1, one);
	    Assert.assertFalse(ds1.definesV(0, 1, one));
	    Assert.assertFalse(ds1.definesV(0, 1, two));
	    Assert.assertFalse(ds1.definesV(0, 1, three));
	    Assert.assertFalse(ds1.definesV(0, 1, four));
	    Assert.assertEquals(0, ds1.m(0, 1, one));
	    Assert.assertEquals(4, ds1.m(0, 1, five));
	    Assert.assertEquals(4, ds1.m(0, 1, six));
	}
	
	public void testRedefineV() {
	    ds1.redefineV(0, 1, one, 3);
	    Assert.assertEquals(12, ds1.m(0, 1, one));
	    Assert.assertEquals(12, ds1.m(0, 1, two));
	    Assert.assertEquals(12, ds1.m(0, 1, three));
	    Assert.assertEquals(12, ds1.m(0, 1, four));
	    Assert.assertEquals(4, ds1.m(0, 1, five));
	    Assert.assertEquals(4, ds1.m(0, 1, six));
	}
	
	public void testAddElement() {
	    final Object x = ds1.addElement();
	    Assert.assertEquals(x, seven);

	    final String out = ""
			   + "   D |  op0  op1  op2 |  v01  v12\n"
			   + "-----+----------------+-----------\n"
			   + "   1 |    1    2    1 |    1    1\n"
			   + "   2 |    3    1    5 |    1    1\n"
			   + "   3 |    2    4    6 |    1    1\n"
			   + "   4 |    4    3    4 |    1    1\n"
			   + "   5 |    6    5    2 |    2    1\n"
			   + "   6 |    5    6    3 |    2    1\n"
			   + "   7 |    -    -    - |    -    -\n"
			   ;
		Assert.assertEquals(out, ds1.tabularDisplay());
	}
	
	public void testRemoveElement() {
	    Assert.assertTrue(ds1.hasElement(three));
	    ds1.removeElement(three);
	    Assert.assertFalse(ds1.hasElement(three));
	    
	    final String out = ""
			   + "   D |  op0  op1  op2 |  v01  v12\n"
			   + "-----+----------------+-----------\n"
			   + "   1 |    1    2    1 |    1    1\n"
			   + "   2 |    -    1    5 |    1    1\n"
			   + "   4 |    4    -    4 |    1    1\n"
			   + "   5 |    6    5    2 |    2    1\n"
			   + "   6 |    5    6    - |    2    1\n"
			   ;
		Assert.assertEquals(out, ds1.tabularDisplay());
	}
	
	public void testCollapse() {
	    final List disposable = new LinkedList();
	    disposable.add(two);
	    disposable.add(five);
	    try {
	        ds1.collapse(disposable, 2);
	        Assert.fail("should have thrown an UnsupportedOperationException");
	    } catch (UnsupportedOperationException success) {
	    }
	    ds1.redefineV(0, 1, five, 1);
        ds1.collapse(disposable, 2);
        
		final String out = ""
			   + "   D |  op0  op1  op2 |  v01  v12\n"
			   + "-----+----------------+-----------\n"
			   + "   1 |    1    1    1 |    1    1\n"
			   + "   3 |    6    4    6 |    1    1\n"
			   + "   4 |    4    3    4 |    1    1\n"
			   + "   6 |    3    6    3 |    1    1\n"
			   ;
		Assert.assertEquals(out, ds1.tabularDisplay());
	}
	
	public void testDual() {
	    final String out = ""
			   + "   D |  op0  op1  op2 |  v01  v12\n"
			   + "-----+----------------+-----------\n"
			   + "   1 |    1    2    1 |    1    1\n"
			   + "   2 |    4    1    - |    1    1\n"
			   + "   3 |    3    -    3 |    1    1\n"
			   + "   4 |    2    4    5 |    1    2\n"
			   + "   5 |    -    5    4 |    1    2\n"
			   ;
	    ds1.removeElement(three);
		Assert.assertEquals(out, ds1.dual().tabularDisplay());
	}
    
    public void testAppend() {
        final DynamicDSymbol ds = new DynamicDSymbol(2);
        ds.append(new DSymbol("4:2 4,3 2 4,2 4:4,4"));
        ds.append(new DSymbol("4:2 4,4 3,2 4:4,4"));
        final String out = ""
               + "   D |  op0  op1  op2 |  v01  v12\n"
               + "-----+----------------+-----------\n"
               + "   1 |    2    3    2 |    1    1\n"
               + "   2 |    1    2    1 |    1    1\n"
               + "   3 |    4    1    4 |    1    1\n"
               + "   4 |    3    4    3 |    1    1\n"
               + "   5 |    6    8    6 |    2    2\n"
               + "   6 |    5    7    5 |    2    2\n"
               + "   7 |    8    6    8 |    2    2\n"
               + "   8 |    7    5    7 |    2    2\n"
               ;
        assertEquals(out, ds.tabularDisplay());
    }
    
    public void testRenumbert() {
        ds1.removeElement(three);
        ds1.renumber();
        
        final String out = ""
               + "   D |  op0  op1  op2 |  v01  v12\n"
               + "-----+----------------+-----------\n"
               + "   1 |    1    2    1 |    1    1\n"
               + "   2 |    -    1    4 |    1    1\n"
               + "   3 |    3    -    3 |    1    1\n"
               + "   4 |    5    4    2 |    2    1\n"
               + "   5 |    4    5    - |    2    1\n"
               ;
        Assert.assertEquals(out, ds1.tabularDisplay());
    }
}

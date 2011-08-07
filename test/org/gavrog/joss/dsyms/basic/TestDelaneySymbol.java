/*
   Copyright 2007 Olaf Delgado-Friedrichs

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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.NonSphericalException;
import org.gavrog.joss.dsyms.basic.Subsymbol;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Test methods implemented in AbstractDelaneySymbol.
 * @author Olaf Delgado
 * @version $Id: TestDelaneySymbol.java,v 1.2 2007/04/26 20:21:58 odf Exp $
 */
public class TestDelaneySymbol extends TestCase {
	private String code;
	private DelaneySymbol ds1, ds2, ds3, ds4;
	private DelaneySymbol sub;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		code = "<1.1:8 3:2 8 7 6,3 4 5 6 8,1 2 8 5 7,2 4 6 8:3 4,5 3,4 3>";
		ds1 = new DSymbol(code);
		ds2 = new DSymbol("8:2 4 6 8,8 3 5 7,6 5 8 7:4,4");
		ds3 = new DSymbol("8:2 4 6 8,8 3 5 7,6 5 7 8:4,4");
		ds4 = new DSymbol("<1.1:1:1,1,1:4,3>");
		sub = new Subsymbol(ds1, new IndexList(1, 2), new Integer(1));
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		code = null;
		ds1 = null;
		sub = null;
		super.tearDown();
	}


	public void testTabularDisplay() {
		String out1 = ""
			   + "   D |  op0  op1  op2  op3 |  v01  v12  v23\n"
			   + "-----+---------------------+----------------\n"
			   + "   1 |    2    3    1    2 |    1    1    2\n"
			   + "   2 |    1    4    2    1 |    1    1    2\n"
			   + "   3 |    8    1    8    4 |    1    1    1\n"
			   + "   4 |    7    2    5    3 |    1    1    1\n"
			   + "   5 |    6    5    4    6 |    2    1    1\n"
			   + "   6 |    5    6    7    5 |    2    1    1\n"
			   + "   7 |    4    8    6    8 |    1    1    1\n"
			   + "   8 |    3    7    3    7 |    1    1    1\n"
			   ;
		Assert.assertEquals(out1, ds1.tabularDisplay());
		String out2 = ""
			   + "   D |  op1  op2 |  v12\n"
			   + "-----+-----------+------\n"
			   + "   1 |    3    1 |    1\n"
			   + "   3 |    1    8 |    1\n"
			   + "   6 |    6    7 |    1\n"
			   + "   7 |    8    6 |    1\n"
			   + "   8 |    7    3 |    1\n"
			   ;
		Assert.assertEquals(out2, sub.tabularDisplay());
	}

	public void testHasStandardIndexSet() {
		Assert.assertTrue(ds1.hasStandardIndexSet());
		Assert.assertFalse(sub.hasStandardIndexSet());
	}
    
    public void testIsComplete() {
        assertTrue(ds1.isComplete());
        assertTrue(sub.isComplete());
        assertFalse(new DSymbol("2:1 0,2,2:4,4").isComplete());
        assertFalse(new DSymbol("2:1 2,2,2:4,0").isComplete());
    }

	public void testR() {
		Assert.assertEquals(3, ds1.r(0, 1, new Integer(1)));
		Assert.assertEquals(1, ds1.r(0, 3, new Integer(1)));
	}

	public void testM() {
		Assert.assertEquals(3, ds1.m(0, 1, new Integer(1)));
		Assert.assertEquals(2, ds1.m(0, 2, new Integer(1)));
	}

	public void testNumberOfOrbits() {
		Assert.assertEquals(2, ds1.numberOfOrbits(new IndexList(0, 1)));
		Assert.assertEquals(2, ds1.numberOfOrbits(new IndexList(1, 2)));
	}

	public void testIsConnected() {
		Assert.assertTrue(ds1.isConnected());
		Assert.assertFalse(new DSymbol("2:1 2,1 2,1 2:3 4, 5 6").isConnected());
		Assert.assertTrue(new DSymbol("0").isConnected());
	}

	public void testOrbitRepresentatives() {
		Iterator iter = ds1.orbitReps(new IndexList(0, 1));
		HashSet reps = new HashSet();
		while (iter.hasNext()) {
			Object x = iter.next();
			Assert.assertFalse(reps.contains(x));
			reps.add(x);
		}
		Assert.assertEquals(2, reps.size());
		Assert.assertTrue(reps.contains(new Integer(1)));
		Assert.assertTrue(reps.contains(new Integer(5)));
	}

	public void testElementsOfOrbit() {
		Object seed = new Integer(2);
		Iterator iter = ds1.orbit(new IndexList(0, 2), seed);
		HashSet reps = new HashSet();
		while (iter.hasNext()) {
			Object x = iter.next();
			Assert.assertFalse(reps.contains(x));
			reps.add(x);
		}
		Assert.assertEquals(2, reps.size());
		Assert.assertTrue(reps.contains(new Integer(1)));
		Assert.assertTrue(reps.contains(new Integer(2)));
	}

	public void testIsOriented() {
		Assert.assertFalse(ds1.isOriented());
		Assert.assertTrue(ds2.isOriented());
		Assert.assertFalse(ds3.isOriented());
	}

    public void testIsLoopless() {
        Assert.assertFalse(ds1.isLoopless());
        Assert.assertTrue(ds2.isLoopless());
        Assert.assertTrue(ds3.isLoopless());
    }

    public void testIsWeaklyOriented() {
        Assert.assertTrue(ds1.isWeaklyOriented());
        Assert.assertTrue(ds2.isWeaklyOriented());
        Assert.assertFalse(ds3.isWeaklyOriented());
    }

	public void testCanonical() {
		String out =
			"<1.1:8 3:2 4 6 8,6 3 5 7 8,2 7 8 5 6,4 3 6 8:3 4,5 3,3 4>";
		Assert.assertEquals(out, ds1.canonical().toString());
        // --- test a non-connected symbol
        final DSymbol ds2 = new DSymbol("8:6 8 5 7,7 3 6 5 8,6 8 5 7:4 4,4 4");
        out = "<1.1:8:2 4 6 8,3 2 4 8 7,2 4 6 8:4 4,4 4>";
        Assert.assertEquals(out, ds2.canonical().toString());
        assertTrue(ds1.compareTo(ds2) > 0);
	}

    public void testGetMapToCanonical() {
        final Map map = ds1.getMapToCanonical();
        Assert.assertEquals(new Integer(6), map.get(new Integer(1)));
        Assert.assertEquals(new Integer(5), map.get(new Integer(2)));
        Assert.assertEquals(new Integer(1), map.get(new Integer(3)));
        Assert.assertEquals(new Integer(4), map.get(new Integer(4)));
        Assert.assertEquals(new Integer(8), map.get(new Integer(5)));
        Assert.assertEquals(new Integer(7), map.get(new Integer(6)));
        Assert.assertEquals(new Integer(3), map.get(new Integer(7)));
        Assert.assertEquals(new Integer(2), map.get(new Integer(8)));
    }
    
    public void testIsMinimal() {
        assertTrue(ds1.isMinimal());
        assertFalse(ds2.isMinimal());
        assertFalse(ds3.isMinimal());
    }
    
	public void testMinimal() {
		DSymbol tiny = new DSymbol("<1.1:1:1,1,1:4,4>");
		Assert.assertEquals(ds1, ds1.minimal());
		Assert.assertEquals(tiny, ds2.minimal());
		Assert.assertEquals(tiny, ds3.minimal());
	}

    public void testFlat() {
        final String out = ""
               + "   D |  op0  op1 |  v01\n"
               + "-----+-----------+------\n"
               + "   1 |    2    1 |    1\n"
               + "   2 |    1    5 |    1\n"
               + "   3 |    3    4 |    1\n"
               + "   4 |    5    3 |    1\n"
               + "   5 |    4    2 |    1\n"
               ;
        Assert.assertEquals(out, sub.flat().tabularDisplay());
    }
    
	public void testCurvature2D() {
		try {
			ds1.curvature2D();
			fail("should raise a UnsupportedOperationException");
		} catch (UnsupportedOperationException success) {
		}
		Assert.assertTrue(ds2.curvature2D().equals(new Whole(0)));
		Assert.assertTrue(ds4.curvature2D().equals(new Fraction(1, 12)));
	}

	public void testSphericalGroupSize2D() {
		Assert.assertEquals(48, ds4.sphericalGroupSize2D());
		try {
			ds2.sphericalGroupSize2D();
			fail("should raise a NonSphericalException");
		} catch (NonSphericalException success) {
		}
	}

	public void testIsSpherical2D() {
		try {
			ds1.isSpherical2D();
			fail("should raise a IllegalArgumentException");
		} catch (IllegalArgumentException success) {
		}
		Assert.assertFalse(ds3.isSpherical2D());
		Assert.assertTrue(ds4.isSpherical2D());
        assertFalse(new DSymbol("1:1,1,1:4,1").isSpherical2D());
        assertFalse(new DSymbol("1:1,1,1:6,1").isSpherical2D());
        assertTrue(new DSymbol("1:1,1,1:6,2").isSpherical2D());
	}

	public void testIsLocallyEuclidean3D() {
		Assert.assertTrue(ds1.isLocallyEuclidean3D());
		try {
			ds2.isLocallyEuclidean3D();
			fail("should raise a UnsupportedOperationException");
		} catch (UnsupportedOperationException success) {
		}
	}

	public void testOrientedCover() {
		String in = ""
			+ "16:2 4 6 8 10 12 14 16, 8 3 5 7 16 11 13 15, 6 5 15 16 11 12 14 13"
			+ ":4 4,4 4";
		DSymbol expected = new DSymbol(in);
		DelaneySymbol found = ds3.orientedCover();
		Assert.assertEquals(expected, found);
		Assert.assertEquals(found, found.orientedCover());
	}
}

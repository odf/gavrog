/*
   Copyright 2008 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.dsyms.generators;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.gavrog.box.collections.Iterators;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.Covers;

/**
 * @author Olaf Delgado
 * @version $Id: TestCombineTiles.java,v 1.2 2006/11/03 21:24:12 odf Exp $
 */
public class TestCombineTiles extends TestCase {
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    //TODO write a test for the new implementation
//    public void testComputeSignatures() {
//        final DSymbol ds = new DSymbol(
//                "15:2 4 5 7 8 10 12 14 15,4 3 6 8 9 11 12 13 15,"
//                + "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0:4 4 8 3,0 0 0 0 0 0 0 0 0");
//        final Map sigs = CombineTiles.elementSignatures(ds);
//        assertEquals(makeSig(0, 2, 2, 0), sigs.get(new Integer(1)));
//        assertEquals(makeSig(0, 2, 2, 0), sigs.get(new Integer(2)));
//        assertEquals(makeSig(0, 2, 2, 0), sigs.get(new Integer(3)));
//        assertEquals(makeSig(0, 2, 2, 0), sigs.get(new Integer(4)));
//        assertEquals(makeSig(1, 4, 1, 0), sigs.get(new Integer(5)));
//        assertEquals(makeSig(1, 4, 1, 1), sigs.get(new Integer(6)));
//        assertEquals(makeSig(1, 4, 1, 1), sigs.get(new Integer(7)));
//        assertEquals(makeSig(1, 4, 1, 0), sigs.get(new Integer(8)));
//        assertEquals(makeSig(2, 4, 2, 0), sigs.get(new Integer(9)));
//        assertEquals(makeSig(2, 4, 2, 1), sigs.get(new Integer(10)));
//        assertEquals(makeSig(2, 4, 2, 1), sigs.get(new Integer(11)));
//        assertEquals(makeSig(2, 4, 2, 0), sigs.get(new Integer(12)));
//        assertEquals(makeSig(3, 3, 1, 2), sigs.get(new Integer(13)));
//        assertEquals(makeSig(3, 3, 1, 1), sigs.get(new Integer(14)));
//        assertEquals(makeSig(3, 3, 1, 0), sigs.get(new Integer(15)));
//    }

    public void testcomponentMultiplicities() {
        final DSymbol ds = new DSymbol(
                "12:2 4 6 8 10 12,3 2 4 8 7 11 10 12,2 4 6 8 10 12:4 4 4,4 4 4");
        final Map counts = CombineTiles.componentMultiplicities(ds);
        assertEquals(new Integer(2), counts.get(new DSymbol("4:2 4,3 2 4,2 4:4,4")));
        assertEquals(new Integer(1), counts.get(new DSymbol("4:2 4,4 3,2 4:4,4")));
    }
    
    public void testFirstRepresentatives() {
        List list;
        CombineTiles.firstRepresentatives(new DSymbol("0::"));
        list = CombineTiles.firstRepresentatives(new DSymbol("1:1,1,1:3,3"));
        assertEquals(1, list.size());
        assertTrue(list.contains(new Integer(1)));
        list = CombineTiles.firstRepresentatives(new DSymbol("4:2 4,3 2 4,2 4:4,4"));
        assertEquals(2, list.size());
        assertTrue(list.contains(new Integer(1)));
        assertTrue(list.contains(new Integer(2)));
        list = CombineTiles.firstRepresentatives(new DSymbol("4:2 4,4 3,2 4:4,4"));
        assertEquals(1, list.size());
        assertTrue(list.contains(new Integer(1)));
    }
    
    public void testSubCanonicalForms() {
        final DSymbol ds = new DSymbol("4:2 4,3 2 4,2 4:4,4");
        final List forms = CombineTiles.subCanonicalForms(ds);
        assertEquals(2, forms.size());
        final List<String> strings = new LinkedList<String>();
        strings.add(forms.get(0).toString());
        strings.add(forms.get(1).toString());
        assertTrue(strings.contains("<1.1:4:2 4,3 2 4,2 4:4,4>"));
        assertTrue(strings.contains("<1.1:4:2 4,1 3 4,2 4:4,4>"));
    }
    
    public void testIterator1() {
        final DSymbol ds = new DSymbol("3 2:1 2 3,1 3,2 3:6 4,3");
        final CombineTiles iter = new CombineTiles(ds);
        assertEquals(new DSymbol("3 3:1 2 3,1 3,2 3,1 2 3:6 4,3,0 0"), iter.next());
        assertEquals(new DSymbol("3 3:1 2 3,1 3,2 3,1 3:6 4,3,0"), iter.next());
        assertFalse(iter.hasNext());
    }
     
    public void testIterator2() {
        final DSymbol ds = new DSymbol("6:2 4 6,6 3 5,2 4 6:3,3");
        final Iterator iter = new CombineTiles(ds);
        assertEquals(new DSymbol("6 3:2 4 6,6 3 5,2 4 6,1 2 3 4 5 6:3,3,0 0 0"), iter
                .next());
        assertEquals(new DSymbol("6 3:2 4 6,6 3 5,2 4 6,2 6 5:3,3,0 0"), iter.next());
        assertFalse(iter.hasNext());
    }
    
    public void testIterator3() {
        final DSymbol ds = new DSymbol("2:1 2,1 2,1 2:3 3,3 4");
        final Iterator iter = new CombineTiles(ds);
        assertEquals(new DSymbol("2 3:1 2,1 2,1 2,2:3 3,3 4,0"), iter.next());
        assertFalse(iter.hasNext());
    }
    
    public void testIterator4() {
        final DSymbol ds = new DSymbol("4:1 2 3 4,1 2 4,1 3 4:3 3 4,3 3");
        final Iterator iter = new CombineTiles(ds);
        assertEquals(new DSymbol("4 3:1 2 3 4,2 3 4,1 3 4,1 2 4:4 3 3,3 3,0 0"), iter.next());
        assertEquals(new DSymbol("4 3:1 2 3 4,2 3 4,1 3 4,2 4:4 3 3,3 3,0"), iter.next());
        assertFalse(iter.hasNext());
    }
    
    public void testResume1() {
    	final DSymbol ds = new DSymbol("6 1:2 4 6,6 3 5:3");
        final CombineTiles iter = new CombineTiles(ds);
        iter.setResumePoint("2-3");
        assertEquals(new DSymbol("6:2 4 6,6 3 5,2 5 6:3,0"), iter
                .next());
        assertEquals(new DSymbol("6:2 4 6,6 3 5,2 6 5:3,0 0"), iter.next());
        assertFalse(iter.hasNext());
    }
    
    public void testResume2() {
    	final DSymbol ds = new DSymbol("6 1:2 4 6,6 3 5:3");
        final CombineTiles iter = new CombineTiles(ds);
        iter.setResumePoint("2-5");
        assertEquals(new DSymbol("6:2 4 6,6 3 5,2 5 6:3,0"), iter
                .next());
        assertEquals(new DSymbol("6:2 4 6,6 3 5,2 6 5:3,0 0"), iter.next());
        assertFalse(iter.hasNext());
    }
    
    public void testResume3() {
    	final DSymbol ds = new DSymbol("6 1:2 4 6,6 3 5:3");
        final CombineTiles iter = new CombineTiles(ds);
        iter.setResumePoint("3");
        assertFalse(iter.hasNext());
    }
    
    public void testTetra() {
        final DSymbol base = new DSymbol("1:1,1,1:3,3");
        final DSymbol tetra = new DSymbol(Covers.finiteUniversalCover(base));
        doTest(tetra, 34, 29, false);
        doTest(base, 67, 49, true);
    }
    
    public void xtestCube() {
        final DSymbol base = new DSymbol("1:1,1,1:4,3");
        final DSymbol cube = new DSymbol(Covers.finiteUniversalCover(base));
        doTest(cube, 1360, 1224, false);
    }
    
    public void doTest(final DSymbol ds, final int xTotal, final int xGood,
            final boolean multi) {
        int total = 0;
        int good = 0;
        
        final Iterator bases = multi ? Covers.allCovers(ds) : Iterators.singleton(ds);
        
        while (bases.hasNext()) {
            final DSymbol base = (DSymbol) bases.next();

            for (final Iterator exts = new CombineTiles(base); exts.hasNext();) {
                final DSymbol out = (DSymbol) exts.next();
                if (xTotal < 0) {
                    System.out.println(ds);
                }
                out.setVDefaultToOne(true);
                ++total;
                // TODO this may not be correct
                if (out.isLocallyEuclidean3D()) {
                    ++good;
                }
            }
        }
        
        if (xTotal >= 0) {
            assertEquals(xTotal, total);
        } else {
            System.out.println("Found " + total + " symbols altogether.");
        }
        if (xGood >= 0) {
            assertEquals(xGood, good);
        } else {
            System.out.println("Found " + good + " good symbols.");
        }
    }
}

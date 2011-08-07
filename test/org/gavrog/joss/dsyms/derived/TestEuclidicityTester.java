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

package org.gavrog.joss.dsyms.derived;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.gavrog.box.collections.Pair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.generators.InputIterator;

/**
 * @author Olaf Delgado
 * @version $Id: TestEuclidicityTester.java,v 1.4 2007/04/18 22:42:39 odf Exp $
 */
public class TestEuclidicityTester extends TestCase {

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

    public void test01() {
        final EuclidicityTester tester =
            new EuclidicityTester(new DSymbol("2 3:1 2,1 2,1 2,2:3 3,3 4,4"));
        Assert.assertTrue(tester.isGood());
    }
    
    public void test02() {
        final EuclidicityTester tester =
            new EuclidicityTester(new DSymbol("1 3:1,1,1,1:4,3,4"));
        Assert.assertTrue(tester.isGood());
    }
    
    public void test03() {
        final EuclidicityTester tester =
            new EuclidicityTester(new DSymbol("20 3:"
                + "2 4 6 8 10 12 14 16 18 20, 10 3 5 7 9 20 13 15 17 19,"
                + "4 3 11 12 16 15 19 20 17 18,16 15 14 13 12 11 20 19 18 17:"
                + "5 5,3 6 3 3,4 4 4"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("orbifold invariants do not match", tester.getCause());
    }
    
    public void test04() {
        final EuclidicityTester tester = new EuclidicityTester(
                new DSymbol("48 3:"
                                + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48,"
                                + "8 3 5 7 16 11 13 15 24 19 21 23 32 27 29 31 40 35 37 39 48 43 45 47,"
                                + "9 10 17 18 25 26 33 34 24 23 41 42 36 35 32 31 47 48 40 39 45 46 43 44,"
                                + "2 8 7 6 10 16 15 14 18 24 23 22 33 34 35 36 37 38 39 40 42 48 47 46:"
                                + "4 4 4 4 4 4,3 3 3 3 3 3 3 3,4 6 4 4 12 6"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("orbifold invariants do not match", tester.getCause());
    }

    public void test05() {
        final EuclidicityTester tester = new EuclidicityTester(
                new DSymbol("24 3:2 4 6 8 10 11 12 14 15 16 18 19 20 22 23 24,"
                                + "8 3 5 7 12 11 16 15 20 19 24 23,9 10 13 14 17 18 21 22 16 23 20 24,"
                                + "6 5 4 8 10 12 18 17 20 19 22 24:4 4 4 4 4,3 3 3 3,6 8 6 3"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("orbifold invariants do not match", tester.getCause());
    }
    
    public void test06() {
        final EuclidicityTester tester = new EuclidicityTester(
                new DSymbol("48 3:"
                        + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48,"
                        + "8 3 5 7 16 11 13 15 24 19 21 23 32 27 29 31 40 35 37 39 48 43 45 47,"
                        + "9 10 17 18 25 26 33 34 24 23 41 42 36 35 32 31 47 48 40 39 45 46 43 44,"
                        + "2 8 7 6 13 14 15 16 18 24 23 22 34 33 40 39 38 37 36 35 46 45 44 48:"
                        + "4 4 4 4 4 4,3 3 3 3 3 3 3 3,8 6 6 4 3 3"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("orbifold invariants do not match", tester.getCause());
    }
    
    public void test07() {
        final EuclidicityTester tester = new EuclidicityTester(new DSymbol("24 3:"
                + "1 3 4 6 8 10 12 14 16 18 20 22 24,2 3 5 6 12 9 11 18 15 17 24 21 23,"
                + "4 7 8 12 11 9 10 19 20 16 21 22 24,1 2 3 4 5 6 13 14 15 16 17 18 19 20 21 22 23 24:"
                + "3 3 3 3 3,3 3 3 3 3,6 4 4 12 12 4 4"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("orbifold invariants do not match", tester.getCause());
    }
    
    public void test08() {
        final EuclidicityTester tester = new EuclidicityTester(
                new DSymbol("60 3:"
                        + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48 50 "
                        + "52 54 56 58 60,"
                        + "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35 42 39 41 48 45 47 54 "
                        + "51 53 60 57 59,"
                        + "2 7 8 11 12 10 19 20 25 26 31 32 30 29 34 33 36 35 43 44 49 50 55 56 54 "
                        + "53 58 57 60 59,"
                        + "13 14 15 16 17 18 37 38 39 40 41 42 22 21 24 32 31 36 35 34 33 56 55 60 "
                        + "59 58 57 49 50 51 52 53 54:"
                        + "3 3 3 3 3 3 3 3 3 3,3 3 3 3 3 3 3 3 3 3,7 6 6 6 3 3 8 8 3 3"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("cover is a non-trivial connected sum", tester.getCause());
    }
    
    public void test09() {
        final EuclidicityTester tester = new EuclidicityTester(
                new DSymbol("60 3:" +
                        "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 " +
                        "44 46 48 50 52 54 56 58 60," +
                        "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35 42 39 41 " +
                        "48 45 47 54 51 53 60 57 59," +
                        "2 7 8 11 12 10 19 20 25 26 31 32 30 29 34 33 36 35 43 44 49 " +
                        "50 55 56 54 53 58 57 60 59," +
                        "13 14 15 16 17 18 37 38 39 40 41 42 22 21 24 32 31 36 35 34 " +
                        "33 56 55 60 59 58 57 50 54 53:" +
                        "3 3 3 3 3 3 3 3 3 3,3 3 3 3 3 3 3 3 3 3,7 6 6 6 3 3 8 8 3 3"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("cover is a non-trivial connected sum", tester.getCause());
    }
    
    public void test10() {
        final EuclidicityTester tester = new EuclidicityTester(
                new DSymbol("72 3:" +
                        "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 " +
                        "44 46 48 50 52 54 56 58 60 62 64 66 68 70 72," +
                        "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 33 35 42 39 41 " +
                        "48 45 47 54 51 53 60 57 59 66 63 65 72 69 71," +
                        "7 8 13 14 19 20 18 17 22 21 24 23 31 32 37 38 43 44 42 41 46 " +
                        "45 48 47 55 56 61 62 67 68 66 65 70 69 72 71," +
                        "2 6 5 25 26 27 28 29 30 49 50 51 52 53 54 63 64 65 66 61 62 " +
                        "34 33 36 44 43 48 47 46 45 71 72 67 68 69 70:" +
                        "3 3 3 3 3 3 3 3 3 3 3 3,3 3 3 3 3 3 3 3 3 3 3 3,8 4 4 6 6 12 3 3 6"));
        Assert.assertTrue(tester.isBad());
        Assert.assertEquals("cover is a non-trivial connected sum", tester.getCause());
    }
    
    public void testSmallList() {
        final int[] good = new int[] { 11, 65, 69, 71, 78, 89, 125 };
        testMany("TestResources/ftmax.ds", 1, 1000, good, new int[] {});
    }
    
    public void testMany(final String filename, final int start, final int stop,
            int[] expectedGood, int[] expectedAmbiguous) {
        
        final InputStream in = ClassLoader.getSystemResourceAsStream(filename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        final InputIterator input = new InputIterator(reader);
        
        final List good = new LinkedList();
        final List ambiguous = new LinkedList();
        final boolean trace = (expectedGood == null && expectedAmbiguous == null);
        
        int count = 0;
        
        while (input.hasNext() && count < stop) {
            ++count;
            if (count < start) {
                continue;
            }
            
            final DelaneySymbol ds = (DSymbol) input.next();
            
            final EuclidicityTester tester = new EuclidicityTester(ds);
            if (trace) {
                final String result = tester.isGood() ? "good" : tester.isBad() ? "bad"
                        : "ambiguous";
                System.out.println("Symbol " + count + " is " + result + ": "
                        + tester.getCause());
            }
            if (tester.isGood()) {
                good.add(new Integer(count));
            } else if (!tester.isBad()) {
                ambiguous.add(new Pair(new Integer(count), tester.getOutcome()));
            }
        }
        
        if (trace) {
            System.out.print(good.size() + " good symbols:");
            for (final Iterator iter = good.iterator(); iter.hasNext();) {
                System.out.print(" " + iter.next());
            }
            System.out.println();

            System.out.println(ambiguous.size() + " ambiguous symbols:");
            for (final Iterator iter = ambiguous.iterator(); iter.hasNext();) {
                final Pair pair = (Pair) iter.next();
                System.out.println("  " + pair.getFirst() + " - " + pair.getSecond());
                // + ((DelaneySymbol) pair.getSecond()).tabularDisplay());
            }
            System.out.println();
        }
        
        if (expectedGood != null) {
            final List expected = new ArrayList();
            for (int i = 0; i < expectedGood.length; ++i) {
                expected.add(new Integer(expectedGood[i]));
            }
            Collections.sort(expected);
            assertEquals(good, expected);
        }
        
        if (expectedAmbiguous != null) {
            final List expected = new ArrayList();
            for (int i = 0; i < expectedAmbiguous.length; ++i) {
                expected.add(new Integer(expectedAmbiguous[i]));
            }
            Collections.sort(expected);
            assertEquals(ambiguous, expected);
        }
    }
}

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

package org.gavrog.joss.dsyms.derived;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.fpgroups.Alphabet;
import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;


import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class FundamentalGroup.
 * @author Olaf Delgado
 * @version $Id: TestFundamentalGroup.java,v 1.3 2007/04/18 04:17:47 odf Exp $
 */
public class TestFundamentalGroup extends TestCase {
    
    final private static String spec1 = "1 3:1,1,1,1:4,3,4";
    final private static String spec2 = "48 3:"
            + "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48,"
            + "8 3 5 7 16 11 13 15 24 19 21 23 32 27 29 31 40 35 37 39 48 43 45 47,"
            + "9 10 17 18 25 26 33 34 24 23 41 42 36 35 32 31 47 48 40 39 45 46 43 44,"
            + "42 41 48 47 46 45 44 43 26 25 32 31 30 29 28 27 34 33 40 39 38 37 36 35:"
            + "4 4 4 4 4 4,3 3 3 3 3 3 3 3,4 4 4 4 4 4";
    
    private DelaneySymbol ds1;
    private DelaneySymbol ds2;
    
    private FundamentalGroup fg1;
    private FundamentalGroup fg2;
    
    private Alphabet al1;
    private Alphabet al2;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        ds1 = new DSymbol(spec1);
        fg1 = new FundamentalGroup(ds1);
        al1 = fg1.getPresentation().getAlphabet();
        
        ds2 = new DSymbol(spec2);
        fg2 = new FundamentalGroup(ds2);
        al2 = fg2.getPresentation().getAlphabet();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        ds1 = null; al1 = null;
        ds2 = null; al2 = null;
        super.tearDown();
    }

    public void testGetAlphabet() {
        Assert.assertEquals(FiniteAlphabet.fromPrefix("g_", 4), al1);
        Assert.assertEquals(FiniteAlphabet.fromPrefix("g_", 3), al2);
    }
    
    public void testGetRelators() {
        final List rels1 = fg1.getPresentation().getRelators();
        Assert.assertEquals(10, rels1.size());
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "g_1^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "g_2^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "g_3^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "g_4^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "(g_1*g_2)^4")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "(g_1*g_3)^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "(g_1*g_4)^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "(g_2*g_3)^3")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "(g_2*g_4)^2")));
        Assert.assertTrue(rels1.contains(FreeWord.parsedWord(al1, "(g_3*g_4)^4")));
        
        final List rels2 = fg2.getPresentation().getRelators();
        Assert.assertTrue(rels2.size() == 3);
        Assert.assertTrue(rels2.contains(FreeWord.parsedWord(al2, "g_1*g_2*g_1^-1*g_2^-1")));
        Assert.assertTrue(rels2.contains(FreeWord.parsedWord(al2, "g_1*g_3*g_1^-1*g_3^-1")));
        Assert.assertTrue(rels2.contains(FreeWord.parsedWord(al2, "g_2*g_3*g_2^-1*g_3^-1")));
    }
    
    public void testGetSymbol() {
        Assert.assertEquals(ds1, fg1.getSymbol());
        Assert.assertEquals(ds2, fg2.getSymbol());
    }
    
    public void testGetEdgeToWord() {
        final Object D1 = new Integer(1);
        final Object D9 = new Integer(9);
        final DSPair e0 = new DSPair(0, D1);
        final DSPair e1 = new DSPair(1, D1);
        final DSPair e2 = new DSPair(2, D1);
        final DSPair e3 = new DSPair(3, D1);
        
        final Map e2w1 = fg1.getEdgeToWord();
        Assert.assertEquals(FreeWord.parsedWord(al1, "g_1"), e2w1.get(e0));
        Assert.assertEquals(FreeWord.parsedWord(al1, "g_2"), e2w1.get(e1));
        Assert.assertEquals(FreeWord.parsedWord(al1, "g_3"), e2w1.get(e2));
        Assert.assertEquals(FreeWord.parsedWord(al1, "g_4"), e2w1.get(e3));
        
        final Map e2w2 = fg2.getEdgeToWord();
        final FreeWord al2g1 = FreeWord.parsedWord(al2, "g_1");
        Assert.assertEquals(FreeWord.parsedWord(al2, ""), e2w2.get(e0));
        Assert.assertEquals(al2g1, e2w2.get(e3));
        Assert.assertEquals(al2g1.inverse(), e2w2.get(e3.reverse(ds2)));
        Assert.assertEquals(FreeWord.parsedWord(al2, "g_2"), e2w2.get(new DSPair(3, D9)));
    }
    
    public void testGetGeneratorToEdge() {
        final Object D1 = new Integer(1);
        final DSPair e0 = new DSPair(0, D1);
        final DSPair e1 = new DSPair(1, D1);
        final DSPair e2 = new DSPair(2, D1);
        final DSPair e3 = new DSPair(3, D1);
        
        final Map g2e1 = fg1.getGeneratorToEdge();
        Assert.assertEquals(e0, g2e1.get(FreeWord.parsedWord(al1, "g_1")));
        Assert.assertEquals(e1, g2e1.get(FreeWord.parsedWord(al1, "g_2")));
        Assert.assertEquals(e2, g2e1.get(FreeWord.parsedWord(al1, "g_3")));
        Assert.assertEquals(e3, g2e1.get(FreeWord.parsedWord(al1, "g_4")));
        
        final Map e2w2 = fg2.getEdgeToWord();
        final Map g2e2 = fg2.getGeneratorToEdge();
        final FreeWord g1 = FreeWord.parsedWord(al2, "g_1");
        final FreeWord g2 = FreeWord.parsedWord(al2, "g_2");
        final FreeWord g3 = FreeWord.parsedWord(al2, "g_3");
        Assert.assertEquals(g1, e2w2.get(g2e2.get(g1)));
        Assert.assertEquals(g2, e2w2.get(g2e2.get(g2)));
        Assert.assertEquals(g3, e2w2.get(g2e2.get(g3)));
    }
    
    public void testGetAxes() {
        final Integer two = new Integer(2);
        final Integer three = new Integer(3);
        final Integer four = new Integer(4);
        
        final Set ax1 = fg1.getAxes();
        Assert.assertEquals(6, ax1.size());
        Assert.assertTrue(ax1.contains(new Pair(FreeWord.parsedWord(al1, "g_1*g_2"),
                four)));
        Assert.assertTrue(ax1.contains(new Pair(FreeWord.parsedWord(al1, "g_1*g_3"),
                two)));
        Assert.assertTrue(ax1.contains(new Pair(FreeWord.parsedWord(al1, "g_1*g_4"),
                two)));
        Assert.assertTrue(ax1.contains(new Pair(FreeWord.parsedWord(al1, "g_2*g_3"),
                three)));
        Assert.assertTrue(ax1.contains(new Pair(FreeWord.parsedWord(al1, "g_2*g_4"),
                two)));
        Assert.assertTrue(ax1.contains(new Pair(FreeWord.parsedWord(al1, "g_3*g_4"),
                four)));
        
        Assert.assertEquals(0, fg2.getAxes().size());
    }
}

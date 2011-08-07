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

import java.util.LinkedList;
import java.util.List;

import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.numbers.Whole;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class FpGroup.
 * @author Olaf Delgado
 * @version $Id: TestFpGroup.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestFpGroup extends TestCase {
    private FiniteAlphabet A;
    private FpGroup G;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new FiniteAlphabet(new String[] {"a", "b", "c"});
        G = new FpGroup(A, new String[] {
                "a*b^-1*a^-1*b",
                "a^-1*c^-1*a*c",
                "b*c*b^-1*c^-1" });
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testToString() {
        Assert.assertEquals("FpGroup(Alphabet({\"a\", \"b\", \"c\"}), "
                + "{a*b*a^-1*b^-1, a*c*a^-1*c^-1, b*c*b^-1*c^-1})",
                G.toString());
    }
    
    public void testGetAlphabet() {
        Assert.assertEquals(A, G.getAlphabet());
    }
    
    public void testGetIdentity() {
        Assert.assertEquals(new FreeWord(A, "*"), G.getIdentity());
    }
    
    public void testGetGenerators() {
        final List gens = G.getGenerators();
        Assert.assertTrue(gens.size() == 3);
        Assert.assertTrue(gens.contains(new FreeWord(A, "a")));
        Assert.assertTrue(gens.contains(new FreeWord(A, "b")));
        Assert.assertTrue(gens.contains(new FreeWord(A, "c")));
    }
    
    public void testGetRelators() {
        final List gens = G.getRelators();
        Assert.assertTrue(gens.size() == 3);
        Assert.assertTrue(gens.contains(new FreeWord(A, "a*b*a^-1*b^-1")));
        Assert.assertTrue(gens.contains(new FreeWord(A, "a*c*a^-1*c^-1")));
        Assert.assertTrue(gens.contains(new FreeWord(A, "a*c*a^-1*c^-1")));
    }
    
    public void testTrivialGroup() {
        final FiniteAlphabet B = new FiniteAlphabet(new String[] {});
        final FpGroup I = new FpGroup(B, new String[] {});
        Assert.assertEquals(0, I.getGenerators().size());
        Assert.assertEquals(0, I.getRelators().size());
        Assert.assertNotNull(I.toString());
        Assert.assertNotNull(I.getIdentity());
        Assert.assertEquals(new LinkedList(), I.abelianInvariants());
    }
    
    public void testGcdex() {
        testGcdex(5, 12, 1);
        testGcdex(111, -740, 37);
        testGcdex(-6100, 9870, 10);
    }
    
    public void testGcdex(final int a, final int b, final int expected) {
        final Whole m = new Whole(a);
        final Whole n = new Whole(b);
        final Whole x = new Whole(expected);
        final Whole result[] = FpGroup.gcdex(m, n);
        Assert.assertEquals(x, result[0]);
        Assert.assertEquals(x, result[1].times(m).plus(result[2].times(n)));
        Assert.assertEquals(Whole.ZERO, result[3].times(m).plus(
                result[4].times(n)));
    }
    
    private static List makeList(final int entries[]) {
        final List res = new LinkedList();
        for (int i = 0; i < entries.length; ++i) {
            res.add(new Whole(entries[i]));
        }
        return res;
    }
    
    public void testAbelianInvariants() {
        final List expected = makeList(new int[] { 0, 0, 0 });
        final List invars = G.abelianInvariants();
        Assert.assertEquals(expected, invars);
        final FpGroup H = new FpGroup(A, new String[] {
                "a^3*b*c^4*a^-1*c^-1*b",
                "c^7*b^-2*a*b^5*c^-1*a^2*c^-2",
                "(a*b*c)^2*c*b^-1" });
        Assert.assertEquals(new LinkedList(), H.abelianInvariants());
    }
}

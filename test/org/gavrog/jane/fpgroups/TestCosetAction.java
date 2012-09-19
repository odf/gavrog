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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.gavrog.jane.fpgroups.CosetAction;
import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class CosetTable.
 * 
 * @author Olaf Delgado
 * @version $Id: TestCosetAction.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestCosetAction extends TestCase {

    private FiniteAlphabet A;
    private FpGroup G;
    private FpGroup H;
    private FpGroup T;
    private CosetAction ActionG;
    private CosetAction ActionG5;
    private CosetAction ActionH;
    private CosetAction ActionT4;

    private Coset C_0;
    private Coset C_a;
    private Coset C_aa;
    private Coset C_b;
    private Coset C_ab;
    private Coset C_aab;

    private FreeWord a;
    private FreeWord a_1;
    private FreeWord b;
    private FreeWord b_1;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new FiniteAlphabet(new String[] { "a", "b" });
        G = new FpGroup(A, new String[] { "a^3", "b^2", "(a*b)^5" });
        H = new FpGroup(A, new String[] { "a^3", "b^2", "a*b*a^-1*b^-1" });
        T = new FpGroup(A, new String[] { "a*b*a^-1*b^-1" });

        List sgens;
        
        sgens = new LinkedList();
        sgens.add(FreeWord.parsedWord(A, "a"));
        sgens.add(FreeWord.parsedWord(A, "b^3"));
        ActionT4 = new CosetAction(T, sgens, 10);
        
        ActionG = new CosetAction(G);
        ActionH = new CosetAction(H);

        sgens = new LinkedList();
        sgens.add(FreeWord.parsedWord(A, "a*b"));
        ActionG5 = new CosetAction(G, sgens);
        
        a = FreeWord.parsedWord(A, "a");
        a_1 = FreeWord.parsedWord(A, "a^-1");
        b = FreeWord.parsedWord(A, "b");
        b_1 = FreeWord.parsedWord(A, "b^-1");

        C_0 = ActionH.getTrivialCoset();
        C_a = ActionH.getCoset(FreeWord.parsedWord(A, "a"));
        C_aa = ActionH.getCoset(FreeWord.parsedWord(A, "a*a"));
        C_b = ActionH.getCoset(FreeWord.parsedWord(A, "b"));
        C_ab = ActionH.getCoset(FreeWord.parsedWord(A, "a*b"));
        C_aab = ActionH.getCoset(FreeWord.parsedWord(A, "a*a*b"));
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        A = null;
        G = null;
        H = null;
        T = null;
        ActionG = null;
        ActionG5 = null;
        ActionH = null;
        ActionT4 = null;
        C_0 = null;
        C_a = null;
        C_aa = null;
        C_b = null;
        C_ab = null;
        C_aab = null;

        a = null;
        a_1 = null;
        b = null;
        b_1 = null;
        super.tearDown();
    }

    public void testGetSubgroupGenerators() {
        List empty = new ArrayList();
        Assert.assertEquals(empty, ActionH.getSubgroupGenerators());
        Assert.assertEquals(empty, ActionG.getSubgroupGenerators());
    }

    public void testGetSizeLimit() {
        Assert.assertEquals(CosetAction.DEFAULT_SiZE_LIMIT, ActionG.getSizeLimit());
        Assert.assertEquals(CosetAction.DEFAULT_SiZE_LIMIT, ActionH.getSizeLimit());
    }

    public void testGetNumberOfCosets() {
        Assert.assertEquals(60, ActionG.size());
        Assert.assertEquals(3, ActionT4.size());
        Assert.assertEquals(6, ActionH.size());
        Assert.assertEquals(12, ActionG5.size());
    }

    public void testGetTrivialCoset() {
        final Coset c1 = ActionG.getTrivialCoset();
        final Coset c2 = ActionG.getCoset("*");
        Assert.assertSame(ActionG, c1.getAction());
        Assert.assertSame(ActionG, c2.getAction());
        Assert.assertEquals(FreeWord.parsedWord(A, "*"), c1.getRepresentative());
        Assert.assertEquals(FreeWord.parsedWord(A, "*"), c2.getRepresentative());
        Assert.assertEquals("*", c1.toString());
        Assert.assertEquals("*", c2.toString());
        Assert.assertEquals(c1, c2);
        Assert.assertEquals(c1.hashCode(), c2.hashCode());
    }

    public void testGetCoset() {
        final Coset ab1 = ActionG.getCoset("a*b");
        final Coset ab2 = ActionG.getCoset("a^4*b^-1");
        Assert.assertSame(ActionG, ab1.getAction());
        Assert.assertSame(ActionG, ab2.getAction());
        Assert.assertEquals(FreeWord.parsedWord(A, "a*b"), ab1.getRepresentative());
        Assert.assertEquals(FreeWord.parsedWord(A, "a*b"), ab2.getRepresentative());
        Assert.assertEquals("a*b", ab1.toString());
        Assert.assertEquals("a*b", ab2.toString());
        Assert.assertEquals(ab1, ab2);
        Assert.assertEquals(ab1.hashCode(), ab2.hashCode());
    }
    
    public void testGetGroup() {
        Assert.assertSame(G, ActionG.getGroup());
        Assert.assertSame(H, ActionH.getGroup());
    }

    public void testGetSet() {
        final List set = new LinkedList();
        for (final Iterator iter = ActionH.domain(); iter.hasNext();) {
            set.add(iter.next());
        }
        Assert.assertEquals(6, set.size());
        Assert.assertTrue(set.contains(C_0));
        Assert.assertTrue(set.contains(C_a));
        Assert.assertTrue(set.contains(C_aa));
        Assert.assertTrue(set.contains(C_b));
        Assert.assertTrue(set.contains(C_ab));
        Assert.assertTrue(set.contains(C_aab));
    }
    
    public void testApply() {
        Assert.assertEquals(C_a, ActionH.apply(C_0, a));
        Assert.assertEquals(C_aa, ActionH.apply(C_a, a));
        Assert.assertEquals(C_0, ActionH.apply(C_aa, a));
        Assert.assertEquals(C_ab, ActionH.apply(C_b, a));
        Assert.assertEquals(C_aab, ActionH.apply(C_ab, a));
        Assert.assertEquals(C_b, ActionH.apply(C_aab, a));

        Assert.assertEquals(C_aa, ActionH.apply(C_0, a_1));
        Assert.assertEquals(C_0, ActionH.apply(C_a, a_1));
        Assert.assertEquals(C_a, ActionH.apply(C_aa, a_1));
        Assert.assertEquals(C_aab, ActionH.apply(C_b, a_1));
        Assert.assertEquals(C_b, ActionH.apply(C_ab, a_1));
        Assert.assertEquals(C_ab, ActionH.apply(C_aab, a_1));

        Assert.assertEquals(C_b, ActionH.apply(C_0, b));
        Assert.assertEquals(C_ab, ActionH.apply(C_a, b));
        Assert.assertEquals(C_aab, ActionH.apply(C_aa, b));
        Assert.assertEquals(C_0, ActionH.apply(C_b, b));
        Assert.assertEquals(C_a, ActionH.apply(C_ab, b));
        Assert.assertEquals(C_aa, ActionH.apply(C_aab, b));

        Assert.assertEquals(C_b, ActionH.apply(C_0, b_1));
        Assert.assertEquals(C_ab, ActionH.apply(C_a, b_1));
        Assert.assertEquals(C_aab, ActionH.apply(C_aa, b_1));
        Assert.assertEquals(C_0, ActionH.apply(C_b, b_1));
        Assert.assertEquals(C_a, ActionH.apply(C_ab, b_1));
        Assert.assertEquals(C_aa, ActionH.apply(C_aab, b_1));

        Assert.assertEquals(C_ab, ActionH.apply(C_0, FreeWord.parsedWord(A, "a^2*b^-1*a^-1")));
    }
}

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FiniteGroupAction;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.GroupAction;
import org.gavrog.jane.fpgroups.GroupActions;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class GroupActions.
 * 
 * @author Olaf Delgado
 * @version $Id: TestGroupActions.java,v 1.2 2005/07/18 23:33:29 odf Exp $
 */
public class TestGroupActions extends TestCase {
    private FiniteAlphabet A;
    private FpGroup G;
    private List domain;
    private GroupAction action1;
    private GroupAction action2;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new FiniteAlphabet(new String[] { "a" });
        G = new FpGroup(A);
        domain = new LinkedList();
        for (int i = 0; i < 5; ++i) {
            domain.add(new Integer(i));
        }
        action1 = new FiniteGroupAction(G, domain) {
            protected Object applyGenerator(final Object x, final int gen,
                    final int sign) {
                final int i = ((Integer) x).intValue();
                return new Integer((i + sign * gen + 5) % 5);
            }
        };
        action2 = new FiniteGroupAction(G, domain) {
            protected Object applyGenerator(final Object x, final int gen,
                    final int sign) {
                final int i = ((Integer) x).intValue();
                if (sign > 0) {
                    return new Integer((i * 2) % 5);
                } else {
                    return new Integer((i * 3) % 5);
                }
            }
        };
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        A = null;
        G = null;
        domain = null;
        action1 = null;
        action2 = null;
    }

    private Pair pair(final int a, final int b) {
        return new Pair(new Integer(a), new Integer(b));
    }
    
    public void testProduct() {
        final GroupAction product = GroupActions.product(action1, action2);
        Assert.assertEquals(G, product.getGroup());
        Assert.assertEquals(25, product.size());
        Assert.assertTrue(Iterators.equal(Iterators.cantorProduct(domain
                .iterator(), domain.iterator()), product.domain()));
        Assert.assertTrue(product.isDefinedOn(pair(1,4)));
        Assert.assertFalse(product.isDefinedOn(pair(1,5)));
        Assert.assertEquals(pair(3, 4), product.apply(pair(1, 1), FreeWord.parsedWord(A, "a^2")));
        Assert.assertEquals(pair(0, 0), product.apply(pair(0, 0), FreeWord.parsedWord(A, "a^5")));
    }

    public void testFlat() {
        final GroupAction flat = GroupActions.flat(GroupActions.product(
                action1, action2));
        Assert.assertEquals(G, flat.getGroup());
        Assert.assertEquals(25, flat.size());
        Assert.assertTrue(Iterators.equal(Iterators.range(0, 25), flat.domain()));
        Assert.assertTrue(flat.isDefinedOn(new Integer(15)));
        Assert.assertFalse(flat.isDefinedOn(new Integer(25)));
        Assert.assertEquals(new Integer(0), flat.apply(new Integer(0),
                FreeWord.parsedWord(A, "a^5")));
        Assert.assertEquals(new Integer(22), flat.apply(new Integer(4),
                FreeWord.parsedWord(A, "a^2")));
    }

    private List perm(final int a, final int b, final int c, final int d,
            final int e) {
        final List res = new ArrayList();
        res.add(new Integer(a));
        res.add(new Integer(b));
        res.add(new Integer(c));
        res.add(new Integer(d));
        res.add(new Integer(e));
        return res;
    }
    
    public void testCover() {
        final GroupAction cover = GroupActions.cover(action2);
        Assert.assertEquals(G, cover.getGroup());
        Assert.assertEquals(120, cover.size());
        final Iterator dom = cover.domain();
        Assert.assertEquals(perm(0, 1, 2, 3, 4), dom.next());
        Assert.assertEquals(perm(0, 1, 2, 4, 3), dom.next());
        Assert.assertEquals(perm(0, 1, 3, 2, 4), dom.next());
        Assert.assertEquals(perm(0, 1, 3, 4, 2), dom.next());
        Assert.assertTrue(cover.isDefinedOn(perm(0,1,2,3,4)));
        Assert.assertFalse(cover.isDefinedOn(perm(1,2,3,4,5)));
        Assert.assertEquals(perm(0, 4, 3, 2, 1), cover.apply(
                perm(0, 1, 2, 3, 4), FreeWord.parsedWord(A, "a^2")));
        Assert.assertEquals(perm(0, 2, 4, 1, 3), cover.apply(
                perm(0, 1, 2, 3, 4), FreeWord.parsedWord(A, "a^5")));
    }
    
    public void testOrbit() {
        final GroupAction orbit0 = GroupActions.orbit(new Integer(0), action2);
        Assert.assertEquals(1, orbit0.size());
        final GroupAction orbit1 = GroupActions.orbit(new Integer(1), action2);
        Assert.assertEquals(4, orbit1.size());
        final Set seen = new HashSet();
        final Iterator iter = orbit1.domain();
        while (iter.hasNext()) {
            final Object x = iter.next();
            final int i = ((Integer) x).intValue();
            Assert.assertTrue(orbit1.isDefinedOn(x));
            Assert.assertTrue(1 <= i && i <= 4);
            Assert.assertFalse(seen.contains(x));
            seen.add(x);
            Assert.assertTrue(seen.contains(x));
        }
    }
}

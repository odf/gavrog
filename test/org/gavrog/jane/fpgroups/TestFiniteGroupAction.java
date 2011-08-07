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

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FiniteGroupAction;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.jane.fpgroups.GroupAction;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class FiniteGroupAction.
 * 
 * @author Olaf Delgado
 * @version $Id: TestFiniteGroupAction.java,v 1.2 2005/07/18 23:33:29 odf Exp $
 */
public class TestFiniteGroupAction extends TestCase {
    private FiniteAlphabet A;
    private FpGroup G;
    private List domain;
    private GroupAction action;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new FiniteAlphabet(new String[] { "a", "b" });
        G = new FpGroup(A);
        domain = new LinkedList();
        for (int i = 0; i < 6; ++i) {
            domain.add(new Integer(i));
        }
        action = new FiniteGroupAction(G, domain) {
            protected Object applyGenerator(final Object x, final int gen,
                    final int sign) {
                final int i = ((Integer) x).intValue();
                return new Integer((i + sign * gen + 6) % 6);
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
        action = null;
    }

    public void testGetGroup() {
        Assert.assertEquals(G, action.getGroup());
    }
    
    public void testSize() {
        Assert.assertEquals(domain.size(), action.size());
    }
    
    public void testIsDefinedOn() {
        for (int i = 0; i < 6; ++i) {
            Assert.assertTrue(action.isDefinedOn(new Integer(i)));
        }
        Assert.assertFalse(action.isDefinedOn(new Integer(7)));
        Assert.assertFalse(action.isDefinedOn("hello"));
    }
    
    public void testDomain() {
        Assert.assertTrue(Iterators.equal(domain.iterator(), action.domain()));
    }
    
    public void testApply() {
        final FreeWord w1 = new FreeWord(A, "a*b*a^-1*b^-1");
        final FreeWord w2 = new FreeWord(A, "a*b*a*b");
        final FreeWord w3 = new FreeWord(A, "a^3*b^2");
        for (int i = 0; i < 6; ++i) {
            final Integer I = new Integer(i);
            final Integer J = new Integer((i + 1) % 6);
            Assert.assertEquals(I, action.apply(I, w1));
            Assert.assertEquals(I, action.apply(I, w2));
            Assert.assertEquals(J, action.apply(I, w3));
        }
        Assert.assertNull(action.apply(new Integer(6), w1));
    }
}

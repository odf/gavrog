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

import java.util.Iterator;

import org.gavrog.jane.fpgroups.FiniteAlphabet;
import org.gavrog.jane.fpgroups.FpGroup;
import org.gavrog.jane.fpgroups.SmallActionsIterator;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for SmallActionsIterator.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSmallActionsIterator.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestSmallActionsIterator extends TestCase {
    private FiniteAlphabet A;
    private FpGroup G;
    private FpGroup T;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        A = new FiniteAlphabet(new String[] { "a", "b", "c" });
        G = new FpGroup(A, new String[] { "[a,b]", "[a,c]", "[b,c]" });
        T = new FpGroup(A, new String[] { "a^2", "b^2", "c^2", "(a*b)^4",
                "(a*c)^2", "(b*c)^4" });
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        A = null;
        G = null;
        T = null;
    }

    public void test1() {
        test(G, 4, false, 56);
        test(G, 4, true, 56);
    }
    
    public void test2() {
        test(T, 3, false, 8);
        test(T, 3, true, 8);
    }
    
    public void test3() {
        test(T, 4, false, 27);
        test(T, 4, true, 15);
    }
    
    public void test(final FpGroup G, final int index, boolean normalOnly,
            final int expected) {
        final Iterator actions = new SmallActionsIterator(G, index, normalOnly);
        int count = 0;
        while (actions.hasNext()) {
            ++count;
            actions.next();
        }
        Assert.assertEquals(expected, count);
    }
}

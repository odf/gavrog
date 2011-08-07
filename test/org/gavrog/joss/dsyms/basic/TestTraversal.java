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

import java.util.Collections;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Traversal;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestTraversal.java,v 1.2 2007/04/18 04:17:48 odf Exp $
 */
public class TestTraversal extends TestCase {
    final static private Integer one = new Integer(1);
    final static private Integer two = new Integer(2);
    final static private Integer three = new Integer(3);
    final static private Integer four = new Integer(4);
    final static private Integer five = new Integer(5);
    final static private Integer six = new Integer(6);

    private DelaneySymbol ds;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        ds = new DSymbol("6:2 4 6,6 3 5,2 4 6:3,3");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        ds = null;
        super.tearDown();
    }

    public void testTraversal1() {
        final Traversal trav = new Traversal(ds);
        Assert.assertEquals(new DSPair(-1, one), trav.next());
        Assert.assertEquals(new DSPair(0, two), trav.next());
        Assert.assertEquals(new DSPair(1, three), trav.next());
        Assert.assertEquals(new DSPair(0, four), trav.next());
        Assert.assertEquals(new DSPair(1, five), trav.next());
        Assert.assertEquals(new DSPair(0, six), trav.next());
    }

    public void testTraversal2() {
        final IndexList idcs = new IndexList(ds);
        Collections.reverse(idcs);
        final Traversal trav = new Traversal(ds, idcs, ds.elements(), false);
        Assert.assertEquals(new DSPair(-1, one), trav.next());
        Assert.assertEquals(new DSPair(2, two), trav.next());
        Assert.assertEquals(new DSPair(1, three), trav.next());
        Assert.assertEquals(new DSPair(2, four), trav.next());
        Assert.assertEquals(new DSPair(1, five), trav.next());
        Assert.assertEquals(new DSPair(2, six), trav.next());
    }
}

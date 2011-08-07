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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.FundamentalEdges;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class FundamentalEdges.
 * @author Olaf Delgado
 * @version $Id: TestFundamentalEdges.java,v 1.2 2007/04/18 04:17:47 odf Exp $
 */
public class TestFundamentalEdges extends TestCase {
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
        ds = new DSymbol("6:1 3 4 6,2 4 5 6,1 5 6 4:8 4,3 3");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        ds = null;
        super.tearDown();
    }

    public void testFundamentalEdges1() {
        testFundamentalEdges(new FundamentalEdges(ds));
    }
    
    public void testFundamentalEdges2() {
        final IndexList idcs = new IndexList(ds);
        Collections.reverse(idcs);
        final Traversal trav = new Traversal(ds, idcs, ds.elements(), false);
        testFundamentalEdges(new FundamentalEdges(ds, trav));
    }
    
    public void testFundamentalEdges(final FundamentalEdges iter) {
        final Set edges = new HashSet();
        while (iter.hasNext()) {
            edges.add(iter.next());
        }
        Assert.assertTrue(edges.size() == 6);
        Assert.assertTrue(edges.contains(new DSPair(1, one))
                || edges.contains(new DSPair(1, two)));
        Assert.assertTrue(edges.contains(new DSPair(1, three))
                || edges.contains(new DSPair(1, four)));
        Assert.assertTrue(edges.contains(new DSPair(0, two))
                || edges.contains(new DSPair(0, three)));
        Assert.assertTrue(edges.contains(new DSPair(0, five))
                || edges.contains(new DSPair(0, six)));
        Assert.assertTrue(edges.contains(new DSPair(2, two))
                || edges.contains(new DSPair(2, five)));
        Assert.assertTrue(edges.contains(new DSPair(2, three))
                || edges.contains(new DSPair(2, six)));
    }
}

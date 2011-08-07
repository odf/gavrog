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

package org.gavrog.box.collections;

import org.gavrog.box.collections.Pair;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class javaDSym.util.Pair
 * @author Olaf Delgado
 * @version $Id: TestPair.java,v 1.2 2005/07/22 19:51:47 odf Exp $
 */
public class TestPair extends TestCase {
    private Pair p1;
    private Pair equals_p1;
    private Pair p2;
    private Pair p3;
    private Pair with_null;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        p1 = new Pair("babel", new Integer(17));
        equals_p1 = new Pair("babel", new Integer(17));
        p2 = new Pair("ocean's", new Integer(11));
        p3 = new Pair("babel", new Integer(9));
        with_null = new Pair("babel", null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        p1 = null;
        equals_p1 = null;
        p2 = null;
        p3 = null;
        with_null = null;
        super.tearDown();
    }

    public void testHashCode() {
        Assert.assertTrue(p1.hashCode() == equals_p1.hashCode());
        with_null.hashCode();
    }

    public void testPair() {
        Assert.assertEquals(p1.getFirst(), "babel");
        Assert.assertEquals(p1.getSecond(), new Integer(17));
    }

    /*
     * Class under test for boolean equals(Object)
     */
    public void testEqualsObject() {
        Assert.assertTrue(p1.equals(equals_p1));
        Assert.assertTrue(with_null.equals(new Pair("babel", null)));
        Assert.assertFalse(p1.equals(p2));
        Assert.assertFalse(p1.equals(with_null));
    }

    /*
     * Class under test for String toString()
     */
    public void testToString() {
        Assert.assertEquals(p1.toString(), "(babel, 17)");
        with_null.toString();
    }
    
    public void testCompareTo() {
        assertTrue(p1.compareTo(equals_p1) == 0);
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p1.compareTo(p3) > 0);
        assertTrue(p2.compareTo(equals_p1) > 0);
    }
}

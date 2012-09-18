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

package org.gavrog.box.collections;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Partition;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class Partition.
 * @author Olaf Delgado
 * @version $Id: TestPartition.java,v 1.1 2005/07/22 19:44:34 odf Exp $
 */
public class TestPartition extends TestCase {
    private Partition<String> P;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        P = new Partition<String>();
        P.unite("apfel", "birne");
        P.unite("gurke", "zucchini");
        P.unite("kirsche", "birne");
        P.unite("apfel", "pfirsich");
        P.unite("zucchini", "paprika");
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        P = null;
    }

    /*
     * Class under test for Object clone()
     */
    public void testClone() {
        Partition<String> Q = (Partition<String>) P.clone();
        Assert.assertEquals(P.representativeMap(), Q.representativeMap());
    }

    public void testFind() {
        String s1 = "auto";
        String s2 = "kirsche";
        Assert.assertSame(s1, P.find(s1));
        Assert.assertSame(P.find(s2), P.find(P.find(s2)));
    }

    public void testAreEquivalent() {
        Assert.assertTrue(P.areEquivalent("kirsche", "pfirsich"));
        Assert.assertTrue(P.areEquivalent("gurke", "paprika"));
        Assert.assertFalse(P.areEquivalent("gurke", "kirsche"));
        Assert.assertFalse(P.areEquivalent("gurke", "auto"));
    }

    public void testRepresentativeMap() {
        String entries[] = {"apfel", "birne", "gurke", "zucchini", "kirsche",
                "pfirsich", "paprika"};
        Map<String, String> M = P.representativeMap();
        for (int i = 0; i < entries.length; ++i) {
            String key = entries[i];
            Assert.assertTrue(M.containsKey(key));
            Assert.assertSame(M.get(key), P.find(key));
        }
        Assert.assertFalse(M.containsKey("auto"));
    }
    
    public void testNullEntry() {
        P.unite("apfel", null);
        Assert.assertEquals(P.find(null), P.find(P.find(null)));
        Assert.assertTrue(P.areEquivalent("apfel", null));
        Assert.assertFalse(P.areEquivalent("gurke", null));
    }
    
    public void testClasses() {
        final List<Set<String>> classes = Iterators.asList(P.classes());
        assertEquals(2, classes.size());
        final Set<String> class1;
        final Set<String> class2;
        if ((classes.get(0)).contains("apfel")) {
            class1 = classes.get(0);
            class2 = classes.get(1);
        } else {
            class1 = classes.get(1);
            class2 = classes.get(0);
        }
        assertEquals(4, class1.size());
        assertTrue(class1.contains("apfel"));
        assertTrue(class1.contains("birne"));
        assertTrue(class1.contains("kirsche"));
        assertTrue(class1.contains("pfirsich"));
        assertEquals(3, class2.size());
        assertTrue(class2.contains("gurke"));
        assertTrue(class2.contains("zucchini"));
        assertTrue(class2.contains("paprika"));
    }
}

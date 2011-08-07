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

import org.gavrog.jane.fpgroups.Alphabet;
import org.gavrog.jane.fpgroups.PrefixAlphabet;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class PrefixAlphabet.
 * @author Olaf Delgado
 * @version $Id: TestPrefixAlphabet.java,v 1.1.1.1 2005/07/15 21:58:40 odf Exp $
 */
public class TestPrefixAlphabet extends TestCase {
    public void testGeneral() {
        Alphabet A = new PrefixAlphabet("g_");
        Assert.assertEquals("g_3", A.letterToName(3));
        Assert.assertEquals(5, A.nameToLetter("g_5"));
        Assert.assertEquals(null, A.letterToName(0));
        Assert.assertEquals(null, A.letterToName(-1));
        try {
            A.nameToLetter("g3");
            Assert.fail("should raise an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try {
            A.nameToLetter("g_0");
            Assert.fail("should raise an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try {
            A.nameToLetter("g_-1");
            Assert.fail("should raise an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }
}

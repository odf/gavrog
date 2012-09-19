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

import java.util.NoSuchElementException;

import org.gavrog.box.collections.FilteredIterator;
import org.gavrog.box.collections.Iterators;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit test for class FilteredIterator, also using class IntegerInterator.
 */
public class TestFilteredIterator extends TestCase {
    FilteredIterator<Float, Integer> iter;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        iter = new FilteredIterator<Float, Integer>(Iterators.range(0, 7)) {
            public Float filter(Integer x) {
                if (x % 2 != 0)
                    return x.floatValue();
                else
                    return null;
            }
        };
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        iter = null;
        super.tearDown();
    }

    public void testFilteredIterator() {
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(iter.next(), 1.0f);
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(iter.next(), 3.0f);
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(iter.next(), 5.0f);
        Assert.assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Should raise a NoSuchElementException");
        } catch (NoSuchElementException success) {
        }
    }

    public void testRemove() {
        try {
            iter.remove();
            fail("Should raise an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }
}

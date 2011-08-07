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

package org.gavrog.joss.geometry;

import junit.framework.TestCase;

/**
 * @author Olaf Delgado
 * @version $Id: TestOperatorType.java,v 1.6 2005/09/20 05:25:10 odf Exp $
 */
public class TestOperatorType extends TestCase {
    final OperatorType ot0 = new OperatorType(3, true, 3, true);
    final OperatorType ot1 = new OperatorType(new Operator("-y,x"));
    final OperatorType ot2 = new OperatorType(new Operator("y,x"));
    final OperatorType ot3 = new OperatorType(new Operator("z,x,y"));
    final OperatorType ot4 = new OperatorType(new Operator("y,z,x"));
    final OperatorType ot5 = new OperatorType(new Operator("x,y"));
    final OperatorType ot6 = new OperatorType(new Operator("x,y,z"));
    final OperatorType ot7 = new OperatorType(new Operator("y,-x"));
    final OperatorType ot8 = new OperatorType(new Operator("-y,-z,-x"));
    final OperatorType ot9 = new OperatorType(new Operator("-y,x-y,z"));

    public void testGetDimension() {
        assertEquals(3, ot0.getDimension());
        assertEquals(2, ot1.getDimension());
        assertEquals(2, ot2.getDimension());
        assertEquals(3, ot3.getDimension());
        assertEquals(3, ot4.getDimension());
        assertEquals(2, ot5.getDimension());
        assertEquals(3, ot6.getDimension());
        assertEquals(2, ot7.getDimension());
        assertEquals(3, ot8.getDimension());
        assertEquals(3, ot9.getDimension());
    }
    
    public void testIsClockwise() {
        assertTrue(ot0.isClockwise());
        assertTrue(ot1.isClockwise());
        assertFalse(ot2.isClockwise());
        assertTrue(ot3.isClockwise());
        assertFalse(ot4.isClockwise());
        assertTrue(ot5.isClockwise());
        assertTrue(ot6.isClockwise());
        assertFalse(ot7.isClockwise());
        assertFalse(ot8.isClockwise());
        assertTrue(ot9.isClockwise());
    }

    public void testGetOrder() {
        assertEquals(3, ot0.getOrder());
        assertEquals(4, ot1.getOrder());
        assertEquals(2, ot2.getOrder());
        assertEquals(3, ot3.getOrder());
        assertEquals(3, ot4.getOrder());
        assertEquals(1, ot5.getOrder());
        assertEquals(1, ot6.getOrder());
        assertEquals(4, ot7.getOrder());
        assertEquals(3, ot8.getOrder());
        assertEquals(3, ot9.getOrder());
    }

    public void testIsOrientationPreserving() {
        assertTrue(ot0.isOrientationPreserving());
        assertTrue(ot1.isOrientationPreserving());
        assertFalse(ot2.isOrientationPreserving());
        assertTrue(ot3.isOrientationPreserving());
        assertTrue(ot4.isOrientationPreserving());
        assertTrue(ot5.isOrientationPreserving());
        assertTrue(ot6.isOrientationPreserving());
        assertTrue(ot7.isOrientationPreserving());
        assertFalse(ot8.isOrientationPreserving());
        assertTrue(ot9.isOrientationPreserving());
    }
    
    public void testEquals() {
        assertFalse(ot1.equals(ot2));
        assertTrue(ot3.equals(ot0));
        assertFalse(ot3.equals(ot1));
        assertFalse(ot3.equals(ot4));
        assertFalse(ot3.equals(ot8));
        assertTrue(ot3.equals(ot9));
        assertTrue(ot0.equals(ot9));
    }
    
    public void testHashCode() {
        assertEquals(ot0.hashCode(), ot3.hashCode());
        assertEquals(ot0.hashCode(), ot9.hashCode());
    }
}

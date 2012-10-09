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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.derived.Covers;

import junit.framework.TestCase;

/**
 * Unit test for class DSMorphism.
 * 
 * @author Olaf Delgado
 * @version $Id: TestMorphism.java,v 1.1 2007/04/23 20:57:07 odf Exp $
 */
public class TestMorphism extends TestCase {
    private DSymbol ds;
    private DelaneySymbol renumbered;
    private DelaneySymbol unconnected;
    private DelaneySymbol cover;
    private DSMorphism map;
    private DSMorphism iso;
    
    private Object getImage(final Object x) {
        return new Integer((((Integer) x).intValue() - 1) % ds.size() + 1);
    }
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        ds = new DSymbol("3:1 2 3,1 3,2 3:6 4,3");
        renumbered = new DSymbol("3:1 2 3,3 2,2 3:4 6,3");
        unconnected = new DSymbol("4:1 2 3 4,1 3 4,2 3 4:6 4 3,3 3");
        assertEquals(ds, renumbered);
        cover = Covers.finiteUniversalCover(ds);
        map = new DSMorphism(cover, ds);
        iso = new DSMorphism(ds, renumbered, new Integer(1), new Integer(2));
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        iso = null;
        map = null;
        cover = null;
        unconnected = null;
        renumbered = null;
        ds = null;
        super.tearDown();
    }

    /*
     * Class under test for void DSMorphism(DelaneySymbol, DelaneySymbol, Object, Object)
     */
    public void testMorphismDelaneySymbolDelaneySymbolObjectObject() {
        final DSMorphism map = new DSMorphism(cover, ds, new Integer(ds.size() + 1), new Integer(1));
        assertNotNull(map);
        for (final Iterator iter = cover.elements(); iter.hasNext();) {
            final Object x = iter.next();
            assertEquals(map.get(x), getImage(x));
        }
        try {
            new DSMorphism(cover, ds, new Integer(2), new Integer(1));
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try {
            new DSMorphism(ds, cover, new Integer(2), null);
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }

    /*
     * Class under test for void DSMorphism(DelaneySymbol, DelaneySymbol)
     */
    public void testMorphismDelaneySymbolDelaneySymbol() {
        final DSMorphism map = new DSMorphism(cover, ds);
        assertNotNull(map);
        for (final Iterator iter = cover.elements(); iter.hasNext();) {
            final Object x = iter.next();
            assertEquals(map.get(x), getImage(x));
        }
        try {
            new DSMorphism(ds, cover);
            fail("Should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        new DSMorphism(ds, unconnected);
        try {
            new DSMorphism(unconnected, ds);
            fail("Should throw an UnsupportedOperationException");
        } catch (UnsupportedOperationException success) {
        }
    }

    /*
     * Class under test for void DSMorphism(DSMorphism)
     */
    public void testMorphismMorphism() {
        final DSMorphism map = new DSMorphism(this.map);
        assertNotNull(map);
        for (final Iterator iter = cover.elements(); iter.hasNext();) {
            final Object x = iter.next();
            assertEquals(map.get(x), getImage(x));
        }
    }

    public void testInverse() {
        final DSMorphism inv = iso.inverse();
        for (final Iterator iter = ds.elements(); iter.hasNext();) {
            final Object x = iter.next();
            assertEquals(x, inv.get(iso.get(x)));
        }
        try {
            map.inverse();
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
        try {
            new DSMorphism(ds, unconnected).inverse();
            fail("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException success) {
        }
    }

    public void testIsIsomorphism() {
        assertTrue(iso.isIsomorphism());
        assertFalse(map.isIsomorphism());
        assertFalse(new DSMorphism(ds, unconnected).isIsomorphism());
    }

    public void testGetASource() {
        for (final Iterator iter = ds.elements(); iter.hasNext();) {
            final Object x = iter.next();
            assertEquals(x, getImage(map.getASource(x)));
        }
    }

    public void testSize() {
        assertEquals(map.size(), cover.size());
    }

    public void testGet() {
        for (final Iterator iter = cover.elements(); iter.hasNext();) {
            final Object x = iter.next();
            assertEquals(map.get(x), getImage(x));
        }
    }

}

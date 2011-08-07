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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Unit test for {@link org.gavrog.joss.geometry.SpaceGroupFinder}.
 * 
 * @author Olaf Delgado
 * @version $Id: TestSpaceGroupFinder.java,v 1.21 2006/09/12 23:02:24 odf Exp $
 */
public class TestSpaceGroupFinder extends TestCase {
    public void setUp() {
    }
    
    public void tearDown() {
    }
    
    public void testGetCrystalSystem() {
        assertEquals(CrystalSystem.MONOCLINIC, new SpaceGroupFinder(new SpaceGroup(3,
                "A121")).getCrystalSystem());
        assertEquals(CrystalSystem.MONOCLINIC, new SpaceGroupFinder(new SpaceGroup(3,
                "B112")).getCrystalSystem());
        assertEquals(CrystalSystem.MONOCLINIC, new SpaceGroupFinder(new SpaceGroup(3,
                "C121")).getCrystalSystem());
        assertEquals(CrystalSystem.MONOCLINIC, new SpaceGroupFinder(new SpaceGroup(3,
                "I121")).getCrystalSystem());

        assertEquals(CrystalSystem.ORTHORHOMBIC, new SpaceGroupFinder(new SpaceGroup(3,
                "A222")).getCrystalSystem());
        assertEquals(CrystalSystem.ORTHORHOMBIC, new SpaceGroupFinder(new SpaceGroup(3,
                "B222")).getCrystalSystem());
        assertEquals(CrystalSystem.ORTHORHOMBIC, new SpaceGroupFinder(new SpaceGroup(3,
                "C222")).getCrystalSystem());
        assertEquals(CrystalSystem.ORTHORHOMBIC, new SpaceGroupFinder(new SpaceGroup(3,
                "F222")).getCrystalSystem());
        assertEquals(CrystalSystem.ORTHORHOMBIC, new SpaceGroupFinder(new SpaceGroup(3,
                "I222")).getCrystalSystem());

        assertEquals(CrystalSystem.TETRAGONAL, new SpaceGroupFinder(new SpaceGroup(3,
                "P4")).getCrystalSystem());
        assertEquals(CrystalSystem.TETRAGONAL, new SpaceGroupFinder(new SpaceGroup(3,
                "I-4")).getCrystalSystem());

        assertEquals(CrystalSystem.TRIGONAL, new SpaceGroupFinder(
                new SpaceGroup(3, "P-3")).getCrystalSystem());
        assertEquals(CrystalSystem.TRIGONAL, new SpaceGroupFinder(
                new SpaceGroup(3, "R-3")).getCrystalSystem());

        assertEquals(CrystalSystem.HEXAGONAL_3D, new SpaceGroupFinder(
                new SpaceGroup(3, "P6")).getCrystalSystem());
        assertEquals(CrystalSystem.HEXAGONAL_3D, new SpaceGroupFinder(new SpaceGroup(3,
                "P-62c")).getCrystalSystem());

        assertEquals(CrystalSystem.CUBIC, new SpaceGroupFinder(new SpaceGroup(3, "P23"))
                .getCrystalSystem());
        assertEquals(CrystalSystem.CUBIC, new SpaceGroupFinder(new SpaceGroup(3, "F23"))
                .getCrystalSystem());
        assertEquals(CrystalSystem.CUBIC, new SpaceGroupFinder(new SpaceGroup(3, "I23"))
                .getCrystalSystem());
    }

    public void testSettings() {
        final StringBuffer failed = new StringBuffer(100);
        int countFailed = 0;
        String canonicalName = null;
        Set canonicalOps = null;
        for (final Iterator iter = SpaceGroupCatalogue.allKnownSettings(3); iter.hasNext();) {
            final String name = (String) iter.next();
            final List ops = SpaceGroupCatalogue.operators(3, name);
            final CoordinateChange trans = SpaceGroupCatalogue.transform(3, name);
            if (trans.isOne()) {
                canonicalName = name.split(":")[0];
                canonicalOps = new SpaceGroup(3, ops).primitiveOperators();
            }
            final SpaceGroupFinder finder = new SpaceGroupFinder(new SpaceGroup(3, ops));
            if (!canonicalName.equals(finder.getGroupName())) {
                final String text = name + " ==> " + finder.getGroupName()
                        + " (should be " + canonicalName + ")\n";
                //System.err.print(text);
                failed.append(text);
                ++countFailed;
            } else {
                final CoordinateChange c = finder.getToStd();
                final List transformedOps = c.applyTo(ops);
                final Set probes = new SpaceGroup(3, transformedOps).primitiveOperators();
                if (!probes.equals(canonicalOps)) {
                    final String text = name
                            + ": transformation to standard setting not correct\n";
                    //System.err.print(text);
                    failed.append(text);
                    ++countFailed;
                } else {
                    //System.err.println(name + ": OK!");
                }
            }
        }
        if (countFailed > 0) {
            failed.append(countFailed + " groups were not recognized.\n");
        }
        assertEquals("", failed.toString());
    }
    
    public void test_dme() {
        final List ops = new ArrayList();
        ops.add(new Operator("-x+1,-x+y-z,-z+1"));
        ops.add(new Operator("-x+1,-y+1,-z+1"));
        final SpaceGroup G = new SpaceGroup(3, ops, true, false);
        final SpaceGroupFinder finder = new SpaceGroupFinder(G);
        assertEquals("C12/m1", finder.getGroupName());
    }
}

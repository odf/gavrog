/*
Copyright 2006 Olaf Delgado-Friedrichs

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

import org.gavrog.box.simple.NamedConstant;


/**
 * Represents a 3-dimensional crystal system. Currently, this is just a wrapper for the
 * strings representing the names of the systems.
 * 
 * @author Olaf Delgado
 * @version $Id: CrystalSystem.java,v 1.3 2006/09/12 23:01:35 odf Exp $
 */
public class CrystalSystem extends NamedConstant {
    final public static CrystalSystem ZERO_D = new CrystalSystem("0d");
    final public static CrystalSystem ONE_D = new CrystalSystem("1d");
    
	final public static CrystalSystem OBLIQUE = new CrystalSystem("Oblique");
	final public static CrystalSystem RECTANGULAR = new CrystalSystem("Rectangular");
	final public static CrystalSystem SQUARE = new CrystalSystem("Square");
    final public static CrystalSystem HEXAGONAL_2D = new CrystalSystem("Hexagonal");
	
    final public static CrystalSystem CUBIC = new CrystalSystem("Cubic");
    final public static CrystalSystem ORTHORHOMBIC = new CrystalSystem("Orthorhombic");
    final public static CrystalSystem HEXAGONAL_3D = new CrystalSystem("Hexagonal");
    final public static CrystalSystem TETRAGONAL = new CrystalSystem("Tetragonal");
    final public static CrystalSystem TRIGONAL = new CrystalSystem("Trigonal");
    final public static CrystalSystem MONOCLINIC = new CrystalSystem("Monoclinic");
    final public static CrystalSystem TRICLINIC = new CrystalSystem("Triclinic");
    
    private CrystalSystem(final String name) {
        super(name);
    }
}
/*
Copyright 2008 Olaf Delgado-Friedrichs

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.Strings;

/**
 * This class handles catalogues of known space groups. For the time being, everything
 * here is static and the input files are hardwired.
 * 
 * @author Olaf Delgado
 * @version $Id: SpaceGroupCatalogue.java,v 1.19 2006/09/13 21:55:43 odf Exp $
 */
public class SpaceGroupCatalogue {
	private static boolean preferSecondOrigin = true;
	private static boolean preferHexagonal = true;
	
    /**
     * Making the constructor private prevents instantiation (I hope).
     */
    private SpaceGroupCatalogue() {
    }
    
    /**
     * This class is used to represent a table of space group settings of a
     * given dimension.
     */
    private static class Table {
        final public int dimension;
        final public Map<String, List<Operator>> nameToOps =
        	new HashMap<String, List<Operator>>();
        final public Map<String, CoordinateChange> nameToTransform =
        	new HashMap<String, CoordinateChange>();
        final public List<String> namesInOrder = new ArrayList<String>();
        
        public Table(final int dimension) {
            this.dimension = dimension;
        }
    }
    
    private static Table groupTables[] = new Table[5];
    private static Map<String, String> aliases = new HashMap<String, String>();
    
    /**
     * Represents lookup information for groups, as used by {@link SpaceGroupFinder}.
     */
    static class Lookup {
        final public String name;
        final public CrystalSystem system;
        final public char centering;
        final public CoordinateChange fromStd;
        
        public Lookup(final String name, final CrystalSystem system,
                final char centering, final CoordinateChange fromStd) {
            this.name = name;
            this.system = system;
            this.centering = centering;
            this.fromStd = fromStd;
        }
    }
    
    private static Map<String, Lookup> lookup = new HashMap<String, Lookup>();
    
    /**
     * Represents the result of a table lookup.
     */
    private static class Entry {
    	final public String key;
    	final public List ops;
    	final public CoordinateChange transform;
    	
    	public Entry(final String key, final List ops, final CoordinateChange transform) {
    		this.key = key;
    		this.ops = ops;
    		this.transform = transform;
    	}
    }
    
    /**
     * Parses space group settings from a file and stores them statically. Each setting is
     * identified by a name and the transformation used to derive it from the canonical
     * setting of the group, both given in the first input line. The following lines list
     * the operators for the group.
     * 
     * CAVEAT: currently, due to the way the constructors are implemented, a full list of
     * operators must be given. Just a set of generators is not sufficient.
     * 
     * TODO make this accept generator lists
     * 
     * @param filename
     */
    private static void parseGroups(final String filename) {
        final InputStream inStream = ClassLoader.getSystemResourceAsStream(filename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
    
        Table table = null;
        String currentName = null;
        
        while (true) {
            final String line;
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            if (line == null) {
                break;
            }
            if (line.length() == 0 || line.trim().charAt(0) == '#') {
                continue;
            }
            final int i = line.indexOf(' ');
            if (i > 0) {
                final String fields[] = line.trim().split("\\s+");
                if (fields[0].equalsIgnoreCase("alias")) {
                    aliases.put(fields[1], fields[2]);
                } else if (fields[0].equalsIgnoreCase("lookup")) {
                    final String name = fields[1];
                    final char centering = fields[3].charAt(0);
                    final CoordinateChange fromStd = new CoordinateChange(new Operator(fields[4]));
                    final int d = fromStd.getDimension();
                    final CrystalSystem system;
                    if (fields[2].equals("oblique")) {
                        system = CrystalSystem.OBLIQUE;
                    } else if (fields[2].equals("rectangular")) {
                        system = CrystalSystem.RECTANGULAR;
                    } else if (fields[2].equals("square")) {
                        system = CrystalSystem.SQUARE;
                    } else if (fields[2].equals("monoclinic")) {
                        system = CrystalSystem.MONOCLINIC;
                    } else if (fields[2].equals("triclinic")) {
                        system = CrystalSystem.TRICLINIC;
                    } else if (fields[2].equals("orthorhombic")) {
                        system = CrystalSystem.ORTHORHOMBIC;
                    } else if (fields[2].equals("trigonal")) {
                        system = CrystalSystem.TRIGONAL;
                    } else if (fields[2].equals("tetragonal")) {
                        system = CrystalSystem.TETRAGONAL;
                    } else if (fields[2].equals("hexagonal")) {
                    	if (d == 2) {
                    		system = CrystalSystem.HEXAGONAL_2D;
                    	} else {
                    		system = CrystalSystem.HEXAGONAL_3D;
                    	}
                    } else if (fields[2].equals("cubic")) {
                        system = CrystalSystem.CUBIC;
                    } else {
                        throw new RuntimeException(fields[2] + " system unknown");
                    }
                    lookup.put(name, new Lookup(name, system, centering, fromStd));
                } else {
                    currentName = fields[0];
                    final Operator T = new Operator(line.substring(i + 1));
                    final int d = T.getDimension();
                    if (groupTables[d] == null) {
                        groupTables[d] = new Table(d);
                    }
                    table = groupTables[d];
                    table.nameToOps.put(currentName, new LinkedList<Operator>());
                    table.nameToTransform.put(currentName, new CoordinateChange(T));
                    table.namesInOrder.add(currentName);
                }
            } else if (currentName != null) {
                final Operator op = new Operator(line).modZ();
                table.nameToOps.get(currentName).add(op);
            } else {
                throw new DataFormatException("error in space group table file");
            }
        }
	}

    /**
	 * The name of the file to read space group settings from.
	 */
    final private static String tablePath = "org/gavrog/joss/geometry/sgtable.data";
    
    /**
     * Retrieves an iterator of all known names for group settings for a given
     * dimension. Names are returned in the order they appear in in the data
     * file. This order should be such that all settings for a given group
     * appear consecutively.
     * 
     * CAVEAT: a group may have multiple settings, so this method may return
     * more than one name for each individual group.
     * 
     * @param dimension the common dimension of the space groups.
     * @return an iterator over the names of space group settings.
     */
    public static Iterator allKnownSettings(final int dimension) {
        if (groupTables[3] == null) {
            load();
        }
    
        return groupTables[dimension].namesInOrder.iterator();
    }

    /**
     * Strips any extensions from the given space group name and translates it into the
     * standard form found in the catalogue (as, e.g. "C2/c" becomes "C12/c1").
     * @param name a space group name.
     * @return the normalized name.
     */
    public static String normalizedName(final String name) {
        final String base = name.split(":")[0];
        if (aliases.containsKey(base)) {
            return (String) aliases.get(base);
        } else {
            return base;
        }
    }
    
    /**
	 * Retrieves information about a given space group setting as identified by
	 * its name. The return value contains the name under which the setting was
	 * found (including suffices likes ":1" etc), the operator list and the
	 * transformation used to obtain that setting from the canonical one.
	 * 
	 * @param dim the dimension of the group.
	 * @param name the name of the group setting to retrieve.
	 * @return the data for the given space group setting.
	 */
    private static Entry retrieve(int dim, final String name) {
        if (groupTables[3] == null) {
            load();
        }
        final Table table = groupTables[dim];

        final String parts[] = name.split(":");
        final String base = normalizedName(name);
        final String ext = parts.length > 1 ? Strings.capitalized(parts[1]) : "";
        
        final String candidates[];
        if (base.charAt(0) == 'R') {
            if (ext.equals("R")) {
                candidates = new String[] { base + ":R" };
            } else if (ext.equals("H")) {
                candidates = new String[] { base + ":H" };
            } else if (getPreferHexagonal()){
                candidates = new String[] { base + ":H", base + ":R" };
            } else {
                candidates = new String[] { base + ":R", base + ":H" };
            }
        } else if (ext.equals("1")) {
            candidates = new String[] { base + ":1", base };
        } else if (ext.equals("2")) {
            candidates = new String[] { base + ":2", base };
        } else if (getPreferSecondOrigin()){
            candidates = new String[] { base, base + ":2", base + ":1" };
        } else {
            candidates = new String[] { base, base + ":1", base + ":2" };
        }
        
        for (int i = 0; i < candidates.length; ++i) {
            final String key = candidates[i];
            if (table.nameToOps.containsKey(key)) {
                return new Entry(key, (List) table.nameToOps.get(key),
                        (CoordinateChange) table.nameToTransform.get(key));
            }
        }
        
        return null;
    }

    /**
	 * Retrieves the name under which a space group setting is listed.
	 * 
	 * @param dim the dimension of the group.
	 * @param name the name of the group setting.
	 * @return the listed.
	 */
    public static String listedName(final int dim, final String name) {
    	final Entry result = retrieve(dim, name);
    	if (result == null) {
    		return null;
    	} else {
    		return result.key;
    	}
    }

    /**
	 * Retrieves the list of operators for a given space group setting.
	 * 
	 * @param dim the dimension of the group.
	 * @param name the name of the group setting.
	 * @return the list of operators.
	 */
    public static List operators(final int dim, final String name) {
    	final Entry result = retrieve(dim, name);
    	if (result == null) {
    		return null;
    	} else {
    		return result.ops;
    	}
    }

    /**
     * Retrieves a transformation to obtain a space group setting from the canonical setting
     * for that group.
     * 
     * @param dim the dimension of the group.
     * @param name the name of the group setting.
     * @return the transformation operator.
     */
    public static CoordinateChange transform(final int dim, final String name) {
    	final Entry result = retrieve(dim, name);
    	if (result == null) {
    		return null;
    	} else {
    		return result.transform;
    	}
    }

    /**
     * Load the catalogue from the specification file.
     */
    public static void load() {
    	parseGroups(tablePath);
    }
    
    /**
     * Retrieves the lookup information stored.
     * 
     * @return an iterator over the values in the lookup table.
     */
    public static Iterator lookupInfo() {
        if (groupTables[3] == null) {
            load();
        }
        return lookup.values().iterator();
    }

	public static boolean getPreferHexagonal() {
		return preferHexagonal;
	}

	public static void setPreferHexagonal(boolean preferHexagonal) {
		SpaceGroupCatalogue.preferHexagonal = preferHexagonal;
	}

	public static boolean getPreferSecondOrigin() {
		return preferSecondOrigin;
	}

	public static void setPreferSecondOrigin(boolean preferSecondOrigin) {
		SpaceGroupCatalogue.preferSecondOrigin = preferSecondOrigin;
	}
}

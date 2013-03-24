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

package org.gavrog.joss.pgraphs.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.NiftyList;
import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.DataFormatException;
import org.gavrog.box.simple.NamedConstant;
import org.gavrog.box.simple.TaskController;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Lattices;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;


/**
 * Contains methods to parse a net specification in Systre format (file
 * extension "cgd").
 */
public class NetParser extends GenericParser {
    // --- used to enable or disable a log of the parsing process
    private final static boolean DEBUG = false;
    
    // --- define some key constants for data associated to nodes
    public static class InfoType extends NamedConstant {
        private InfoType(final String name) {
            super(name);
        }
    }
    public static InfoType CONNECTIVITY = new InfoType("Connectivity");
    public static InfoType COORDINATION_SEQUENCE =
            new InfoType("Coordination-Sequence");
    public static InfoType POSITION = new InfoType("Position");
    
    /**
     * Helper class - encapsulates the preliminary specification of a node.
     */
    private static class NodeDescriptor {
        public final Object name;     // the node's name
        public final int connectivity; // the node's connectivity
        public final IArithmetic site;     // the position or site of the node
        public boolean isEdgeCenter; // is this really an edge center?
        
        public NodeDescriptor(final Object name, final int connectivity,
                final IArithmetic site, final boolean isEdgeCenter) {
            this.name = name;
            this.connectivity = connectivity;
            this.site = site;
            this.isEdgeCenter = isEdgeCenter;
        }
        
        public String toString() {
            if (isEdgeCenter) {
                return "EdgeCenter(" + name + ", " + connectivity + ", " + site + ")";
            } else {
                return "Node(" + name + ", " + connectivity + ", " + site + ")";
            }
        }
    }
    
    /**
     * Helper class - encapsulates the preliminary specification of an edge.
     */
    private static class EdgeDescriptor {
        public final Object source; // the edge's source node representative
        public final Object target;   // the edge's target node representative
        public final Operator shift;  // shift to be applied to the target representative
        
        public EdgeDescriptor(final Object source, final Object target,
                final Operator shift) {
            this.source = source;
            this.target = target;
            this.shift = shift;
        }
        
        public String toString() {
            return "Edge(" + source + ", " + target + ", " + shift + ")";
        }
    }

    /**
     * Used to pass parsed face list data on to the next processing steps.
     */
    public static class FaceListDescriptor {
        public final List<Object> faceLists;
        public final Map<Integer, Point> indexToPosition;
        
        public FaceListDescriptor(
                final List<Object> faceLists,
                final Map<Integer, Point> indexToPosition)
        {
            this.faceLists = faceLists;
            this.indexToPosition = indexToPosition;
        }
        
        public String toString() {
            return "FaceListDescriptor(" + faceLists + ", " + indexToPosition
                    + ")";
        }
    }
    
    // The last block that was processed.
    private Block lastBlock;
    
    /**
     * Constructs an instance.
     * 
     * @param input the input stream.
     */
    public NetParser(final BufferedReader input) {
        super(input);
        this.synonyms = makeSynonyms();
        this.defaultKey = "edge";
    }
    
    /**
     * Constructs an instance.
     * 
     * @param input the input stream.
     */
    public NetParser(final Reader input) {
        this(new BufferedReader(input));
    }
    
    /**
     * Constructs an instance.
     * 
     * @param filename the name of a file read from.
     * @throws FileNotFoundException if no file of that name exists.
     */
    public NetParser(final String filename) throws FileNotFoundException {
        this(new BufferedReader(new FileReader(filename)));
    }
    
    /**
     * Sets up a keyword map to be used by {@link GenericParser#parseDataBlock()}.
     * 
     * @return the mapping of keywords.
     */
    private static Map<String, String> makeSynonyms() {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("vertex", "node");
        result.put("vertices", "node");
        result.put("vertexes", "node");
        result.put("atom", "node");
        result.put("atoms", "node");
        result.put("nodes", "node");
        result.put("bond", "edge");
        result.put("bonds", "edge");
        result.put("edges", "edge");
        result.put("faces", "face");
        result.put("ring", "face");
        result.put("rings", "face");
        result.put("tiles", "tile");
        result.put("body", "tile");
        result.put("bodies", "tile");
        result.put("spacegroup", "group");
        result.put("space_group", "group");
        result.put("id", "name");
        result.put("edge_centers", "edge_center");
        result.put("edge_centre", "edge_center");
        result.put("edge_centres", "edge_center");
        result.put("edgecenter", "edge_center");
        result.put("edgecenters", "edge_center");
        result.put("edgecentre", "edge_center");
        result.put("edgecentres", "edge_center");
        result.put("coordination_sequences", "coordination_sequence");
        result.put("coordinationsequence", "coordination_sequence");
        result.put("coordinationsequences", "coordination_sequence");
        result.put("cs", "coordination_sequence");
        return Collections.unmodifiableMap(result);
    }
    
    /**
     * Utility method - takes a string and directly returns the net specified by it.
     * 
     * @param s the specification string.
     * @return the net constructed from the input string.
     */
    public static PeriodicGraph stringToNet(final String s) {
        return new NetParser(new StringReader(s)).parseNet();
    }
    
    /**
     * Parses the input stream as specified in the constructor and returns the
     * net specified by it.
     * 
     * @return the periodic net constructed from the input.
     */
    public Net parseNet() {
        return parseNet(parseDataBlock());
    }
    
    /**
     * Parses a pre-parsed data block and returns the net specified by it.
     * 
     * @param block the data block to parse.
     * @return the periodic net constructed from the input.
     */
    public Net parseNet(final GenericParser.Block block) {
        final Entry entries[] = block.getEntries();
        if (entries == null) {
            return null;
        }
        final String type = block.getType().toLowerCase();
        this.lastBlock = block;
        Net result = null;
        try {
        if (type.equals("periodic_graph")) {
            result = parsePeriodicGraph(entries);
        } else if (type.equals("crystal")) {
            result = parseCrystal(entries);
        } else if (type.equals("net")) {
            result = parseSymmetricNet(entries);
        } else {
            throw new DataFormatException("type " + type + " not supported");
        }
        } catch (DataFormatException ex) {
        	result = new Net(0, getName(), getSpaceGroup());
        	result.logError(ex);
        }
        
        return result;
    }
    
    /**
     * Retrieves the name of the net last read, if any.
     * 
     * @return everything present under the "name" or "id" key.
     */
    private String getName() {
        return lastBlock.getEntriesAsString("name");
    }
    
    /**
     * Retrieves the spacegroup given for the net last read, if any.
     * 
     * @return everything present under the "name" of "id" key.
     */
    private String getSpaceGroup() {
        final String group = lastBlock.getEntriesAsString("group");
        if (group == null) {
            return "P1";
        } else {
            return group;
        }
    }
    
    /**
     * Generates a warning message indicating .
     * @param entry
     * @return
     */
    private String keywordWarning(final Entry entry) {
    	return "Unknown keyword '" + entry.originalKey
    			+ "' at line " + entry.lineNumber;
    }
    
    /**
     * Parses a specification for a raw periodic net. In this format, each line
     * specifies a translational equivalence class of edges of the net, given by
     * the names for the translational equivalence classes of the source and
     * target nodes and the additional lattice translation to be applied to the
     * target with respect to the lattice translation given for the source.
     * 
     * Example:
     * 
     * <pre>
     * PERIODIC_GRAPH # the diamond net
     *   1 2  0 0 0
     *   1 2  1 0 0
     *   1 2  0 1 0
     *   1 2  0 0 1
     * END
     * </pre>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private Net parsePeriodicGraph(final Entry block[]) {
        Net G = null;
        final Map<Object, INode> nameToNode = new HashMap<Object, INode>();
        final List<String> warnings = new ArrayList<String>();
        
        for (int i = 0; i < block.length; ++i) {
            if (block[i].key.equals("edge")) {
                final List<Object> row = block[i].values;
                final int d = row.size() - 2;
                if (d < 1) {
                    final String msg = "not enough fields at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                } else if (G == null) {
                    G = new Net(d, getName(), getSpaceGroup());
                } else if (d != G.getDimension()) {
                    final String msg = "inconsistent shift dimensions at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                INode v = (INode) nameToNode.get(row.get(0));
                if (v == null) {
                    v = G.newNode("" + row.get(0));
                    nameToNode.put(row.get(0), v);
                }
                INode w = (INode) nameToNode.get(row.get(1));
                if (w == null) {
                    w = G.newNode("" + row.get(1));
                    nameToNode.put(row.get(1), w);
                }
                final int s[] = new int[d];
                for (int k = 0; k < d; ++k) {
                    s[k] = ((Whole) row.get(k+2)).intValue();
                }
                G.newEdge(v, w, s);
            } else if (!block[i].key.equals("name")){
            	warnings.add(keywordWarning(block[i]));
            }
        }
        if (G == null)
            throw new DataFormatException("Empty graph");
        	
        for (Iterator<String> iter = warnings.iterator(); iter.hasNext();)
        	G.addWarning(iter.next());
        return G;
    }

    
    /**
     * Constructs a space group with the given name.
     * 
     * @param name the name of the group (as according to the International Tables).
     * @return the group constructed.
     */
    private static SpaceGroup parseSpaceGroupName(final String name) {
        final int dim;
        if (Character.isLowerCase(name.charAt(0))) {
            if (name.charAt(0) == 'o')
                dim = 1;
            else
                dim = 2;
        } else {
            dim = 3;
        }
        final Collection<Operator> ops =
                SpaceGroupCatalogue.operators(dim, name);
        if (ops == null) {
            return null;
        } else {
            return new SpaceGroup(dim, ops, false, false);
        }
    }
    
    
    /**
     * Parses a periodic net given in terms of a crystallographic group. Edges
     * are specified in a similar way as in parsePeriodicGraph(), but instead
     * of just lattice translation, any operator from the symmetry group may
     * be used.
     * 
     * Group operators are in symbolic form, as in "y,x,z+1/2". For nodes not
     * in general position, i.e., with a non-trivial stabilizer, their
     * respective special positions must be given in symbolic form, as e.g. in
     * "x,y,x+1/2". Symbolic specifications for both operators and special
     * positions are handled by {@link Operator#parse(String)}.
     * 
     * Example:
     * 
     * <pre>
     * 
     *  NET # the diamond net
     *    Group Fd-3m
     *    Node 1 3/8,3/8,3/8
     *    Edge 1 1 1-x,1-y,1-z
     *  END
     *  
     * </pre>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private Net parseSymmetricNet(final Entry[] block) {
        String groupName = null;
        int dimension = 0;
        SpaceGroup group = null;
        List<Operator> ops = new ArrayList<Operator>();
        List<NodeDescriptor> nodeDescriptors = new LinkedList<NodeDescriptor>();
        List<EdgeDescriptor> edgeDescriptors = new LinkedList<EdgeDescriptor>();
        final Map<Object, NodeDescriptor> nodeNameToDesc =
                new HashMap<Object, NodeDescriptor>();
        final List<String> warnings = new ArrayList<String>();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List<Object> row = block[i].values;
            final String key = block[i].key;
            if (key.equals("group")) {
                if (groupName == null) {
                    if (row.size() < 1) {
                        final String msg = "Missing argument at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                    groupName = (String) row.get(0);
                    group = parseSpaceGroupName(groupName);
                    if (group == null) {
                        final String msg = "Space group \"" + groupName
                                + "\" not recognized at line ";
                        throw new DataFormatException(msg + block[i].lineNumber);
                    }
                    dimension = group.getDimension();
                    ops.addAll(group.getOperators());
                } else {
                    final String msg = "Group specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
            } else if (key.equals("node")) {
                if (row.size() < 1) {
                    final String msg = "Missing argument at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Object name = row.get(0);
                if (nodeNameToDesc.containsKey(name)) {
                    final String msg = "Node specified twice at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Operator position = parseSiteOrOperator(row, 1);
                final NodeDescriptor node = new NodeDescriptor(name, -1, position, false);
                nodeDescriptors.add(node);
                nodeNameToDesc.put(name, node);
            } else if (key.equals("edge")) {
                if (row.size() < 2) {
                    final String msg = "Not enough arguments at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final Object sourceName = row.get(0);
                final Object targetName = row.get(1);
                final Operator shift = parseSiteOrOperator(row, 2);
                if (!ops.contains(shift.modZ())) {
                    final String msg = "Operator not in given group at line ";
                    throw new DataFormatException(msg + block[i].lineNumber);
                }
                final EdgeDescriptor edge = new EdgeDescriptor(sourceName, targetName,
                        shift);
                edgeDescriptors.add(edge);
            } else if (!key.equals("name")){
                warnings.add(keywordWarning(block[i]));
            }
        }
        
        // --- convert to primitive setting
        final Set<Operator> primitiveOps = group.primitiveOperators();
        final Operator to = group.transformationToPrimitive();
        final Operator from = (Operator) to.inverse();
        
        ops.clear();
        for (final Operator op: primitiveOps) {
            ops.add(((Operator) from.times(op).times(to)).modZ());
        }
        
        final List<NodeDescriptor> nodeDescsTmp =
                new LinkedList<NodeDescriptor>();
        for (final NodeDescriptor desc: nodeDescriptors) {
            nodeDescsTmp.add(new NodeDescriptor(desc.name, desc.connectivity,
                    desc.site.times(to), desc.isEdgeCenter));
        }
        nodeDescriptors.clear();
        nodeDescriptors.addAll(nodeDescsTmp);
        
        final List<EdgeDescriptor> edgeDescsTmp =
                new LinkedList<EdgeDescriptor>();
        for (final EdgeDescriptor desc: edgeDescriptors) {
            edgeDescsTmp.add(new EdgeDescriptor(desc.source, desc.target,
                    (Operator) from.times(desc.shift).times(to)));
        }
        edgeDescriptors.clear();
        edgeDescriptors.addAll(edgeDescsTmp);
        
        // TODO provide better error handling in the following
        
        // --- apply group operators to generate all nodes
        final Net G = new Net(dimension, getName(), getSpaceGroup());
        final Map<Pair<Object, Operator>, INode> addressToNode =
                new HashMap<Pair<Object, Operator>, INode>();
        final Map<Pair<Object, Operator>, Vector> addressToShift =
                new HashMap<Pair<Object, Operator>, Vector>();
        
        for (final NodeDescriptor node: nodeDescriptors) {
            final Object name = node.name;
            final Operator site = (Operator) node.site;
            final Map<Operator, INode> siteToNode =
                    new HashMap<Operator, INode>();
            for (final Operator op: ops) {
                final Operator image = (Operator) site.times(op);
                final Operator imageModZ = image.modZ();
                final INode v;
                final Pair<Object, Operator> address =
                        new Pair<Object, Operator>(name, op);
                if (siteToNode.containsKey(imageModZ)) {
                    v = siteToNode.get(imageModZ);
                } else {
                    v = G.newNode("" + name);
                    siteToNode.put(imageModZ, v);
                }
                addressToNode.put(address, v);
                addressToShift.put(address, image.floorZ());
            }
        }
        
        // --- apply group operators to generate all edges
        for (final EdgeDescriptor edge: edgeDescriptors) {
            final Object sourceName = edge.source;
            final Object targetName = edge.target;
            final Operator shift = edge.shift;
            for (final Operator srcOp: ops) {
                final Operator trgOp = (Operator) shift.times(srcOp);
                final Pair<Object, Operator> sourceAddress =
                        new Pair<Object, Operator>(sourceName, srcOp.modZ());
                final Pair<Object, Operator> targetAddress =
                        new Pair<Object, Operator>(targetName, trgOp.modZ());
                final Vector edgeShift =
                        (Vector) trgOp.floorZ().minus(srcOp.floorZ());
                
                final INode v = addressToNode.get(sourceAddress);
                final INode w = addressToNode.get(targetAddress);
                final Vector shiftv = addressToShift.get(sourceAddress);
                final Vector shiftw = addressToShift.get(targetAddress);
                final Vector totalShift =
                        (Vector) edgeShift.plus(shiftw.minus(shiftv));
                if (G.getEdge(v, w, totalShift) == null) {
                    G.newEdge(v, w, totalShift);
                }
            }
        }
        
        if (DEBUG) {
            System.err.println("generated " + G);
        }

        for (Iterator<String> iter = warnings.iterator(); iter.hasNext();)
        	G.addWarning(iter.next());
        return G;
    }

    /**
     * Utility method to parse an operator or site (same format) from a string
     * specification which is broken up into fields. The specified fields are
     * concatenated, using blanks as field separators, and the result is passed
     * to the {@link Operator#Operator(String)} constructor.
     * 
     * @param fields a list of fields.
     * @param startIndex the field index to start parsing at.
     * @return the result as an {@link Operator}.
     */
    private static Operator parseSiteOrOperator(
            final List<Object> fields, final int startIndex)
    {
        if (fields.size() <= startIndex) {
            return Operator.identity(3);
        } else {
            final StringBuffer buf = new StringBuffer(40);
            for (int i = startIndex; i < fields.size(); ++i) {
                buf.append(' ');
                buf.append(fields.get(i));
            }
            return new Operator(buf.toString());
        }
    }
    
    /**
     * Parses a crystal descriptor and constructs the corresponding atom-bond
     * network.
     * 
     * Example:
     * 
     * <pre>
     * CRYSTAL
     *   GROUP Fd-3m
     *   CELL         2.3094 2.3094 2.3094  90.0 90.0 90.0
     *   ATOM  1  4   5/8 5/8 5/8
     * END
     * </pre>
     * 
     * @param block the pre-parsed input.
     * @return the periodic graph constructed from the input.
     */
    private Net parseCrystal(final Entry[] block) {
        // TODO make this work for general dimensions
        final Set<String> seen = new HashSet<String>();
        
        String groupName = null;
        int dim = 3;
        SpaceGroup group = null;
        List<Operator> ops = new ArrayList<Operator>();
        Matrix cellGram = null;
        
        double precision = 0.001;
        double minEdgeLength = 0.1;
        
        final List<NodeDescriptor> nodeDescriptors =
                new LinkedList<NodeDescriptor>();
        final Map<Object, NodeDescriptor> nameToDesc =
                new HashMap<Object, NodeDescriptor>();
        final List<EdgeDescriptor> edgeDescriptors =
                new LinkedList<EdgeDescriptor>();
        final List<List<Object>> coordinationSeqs =
                new LinkedList<List<Object>>();
        final List<String> warnings = new ArrayList<String>();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List<Object> row = block[i].values;
            final String key = block[i].key;
            final int lineNr = block[i].lineNumber;
            if (key.equals("group")) {
                if (seen.contains(key)) {
                    final String msg = "Group specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                if (row.size() < 1) {
                    final String msg = "Missing argument at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                groupName = (String) row.get(0);
                group = parseSpaceGroupName(groupName);
                if (group == null) {
                    final String msg = "Space group \"" + groupName
                            + "\" not recognized at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                dim = group.getDimension();
                groupName = SpaceGroupCatalogue.listedName(dim, groupName);
                ops.addAll(group.getOperators());
            } else if (key.equals("cell")) {
                if (seen.contains(key)) {
                    final String msg = "Cell specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final int m = dim + dim * (dim-1) / 2;
                if (row.size() != m) {
                    final String msg = "Expected " + m + " arguments at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                for (int j = 0; j < m; ++j) {
                    if (!(row.get(i) instanceof Real)) {
                        final String msg = "Arguments must be real numbers at line ";
                        throw new DataFormatException(msg + lineNr);
                    }
                }
                cellGram = gramMatrix(dim, row);
            } else if (key.equals("node") || key.equals("edge_center")) {
                if (row.size() != dim + 2) {
                    final String msg = "Expected " + (dim + 2) + " arguments at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final Object name = row.get(0);
                if (nameToDesc.containsKey(name)) {
                    final String msg = "Node specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final Object conn = row.get(1);
                if (!(conn instanceof Whole && ((Whole) conn).isNonNegative())) {
                    final String msg = "Connectivity must be a non-negative integer at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final IArithmetic pos[] = new IArithmetic[dim];
                for (int j = 0; j < dim; ++j) {
                    pos[j] = (IArithmetic) row.get(j + 2);
                }
                final int c = Math.max(0, ((Whole) conn).intValue());
                final boolean isCenter;
                if (key.equals("node")) {
                    isCenter = false;
                } else {
                    if (c != 2) {
                        final String msg = "Edge center connectivity must be 2";
                        throw new DataFormatException(msg + lineNr);
                    }
                    isCenter = true;
                }
                final NodeDescriptor node =
                        new NodeDescriptor(name, c, new Point(pos), isCenter);
                nodeDescriptors.add(node);
                nameToDesc.put(name, node);
            } else if (key.equals("edge")) {
                final Object source;
                final Object target;
                if (row.size() == 2 * dim) {
                    // --- two node positions
                    final double a[] = new double[dim];
                    for (int k = 0; k < dim; ++k) {
                        a[k] = ((Real) row.get(k)).doubleValue();
                    }
                    source = new Point(a);
                    final double b[] = new double[dim];
                    for (int k = 0; k < dim; ++k) {
                        b[k] = ((Real) row.get(k + dim)).doubleValue();
                    }
                    target = new Point(b);
                } else if (row.size() == dim + 1) {
                    // --- a node name and a neighbor position
                    source = row.get(0);
                    final double a[] = new double[dim];
                    for (int k = 0; k < dim; ++k) {
                        a[k] = ((Real) row.get(k + 1)).doubleValue();
                    }
                    target = new Point(a);
                } else if (row.size() == 2) {
                    // --- two node names given
                    source = row.get(0);
                    target = row.get(1);
                } else  {
                    final String msg = "Expected 2, " + (dim + 1) + " or " + 2 * dim
                            + " arguments at line";
                    throw new DataFormatException(msg + lineNr);
                }
                final EdgeDescriptor edge =
                        new EdgeDescriptor(source, target, null);
                edgeDescriptors.add(edge);
            } else if (key.equals("coordination_sequence")) {
            	coordinationSeqs.add(row);
            } else if (!key.equals("name")){
                warnings.add(keywordWarning(block[i]));
            }
            seen.add(key);
        }
        
        // --- assign coordination sequences to node names
        final Map<Object, List<Object>> name2cs =
                new HashMap<Object, List<Object>>();
        for (int i = 0; i < coordinationSeqs.size(); ++i) {
			name2cs.put(nodeDescriptors.get(i).name, coordinationSeqs.get(i));
		}
        
        // --- use reasonable default for missing data
        if (group == null) {
            warnings.add("Space group missing; assuming P1");
            groupName = "P1";
            group = parseSpaceGroupName(groupName);
            dim = group.getDimension();
            ops.addAll(group.getOperators());
        }
        if (cellGram == null) {
            warnings.add("Unit cell parameters missing; using defaults");
            cellGram = defaultGramMatrix(groupName, dim);
        }
        
        // --- output some of the basic data
        if (DEBUG) {
            System.err.println();
            System.err.println("Group name: " + groupName);
            System.err.println("  operators:");
            for (final Operator op: ops) {
                System.err.println("    " + op);
            }
            System.err.println();

            System.err.println("Cell gram matrix = " + cellGram);
            System.err.println();
            
            System.err.println("Nodes:");
            for (final NodeDescriptor desc: nodeDescriptors) {
                System.err.println("  " + desc);
            }
            
            System.err.println("Edges:");
            for (final EdgeDescriptor desc: edgeDescriptors) {
                System.err.println("  " + desc);
            }
        }
        
        // --- warn about illegal cell parameters
        if (gramMatrixError(dim, group, cellGram) > 0.01)
                warnings.add("Unit cell parameters illegal for this group");
        
        // --- get info for converting to a primitive setting
        final Matrix primitiveCell = group.primitiveCell();
        final Operator to = group.transformationToPrimitive();
        final Operator from = (Operator) to.inverse();
        if (DEBUG) {
            System.err.println();
            System.err.println("Primitive cell: " + primitiveCell);
        }
        
        // --- extract and convert operators
        final Set<Operator> primitiveOps = group.primitiveOperators();
        ops.clear();
        for (final Operator op: primitiveOps) {
            ops.add(((Operator) from.times(op).times(to)).modZ());
        }
        
        // --- convert node descriptors
        final List<NodeDescriptor> nodeDescsTmp =
                new LinkedList<NodeDescriptor>();
        for (final NodeDescriptor desc: nodeDescriptors) {
            final NodeDescriptor newDesc = new NodeDescriptor(desc.name,
                    desc.connectivity, desc.site.times(to), desc.isEdgeCenter);
            nodeDescsTmp.add(newDesc);
            nameToDesc.put(desc.name, newDesc);
        }
        nodeDescriptors.clear();
        nodeDescriptors.addAll(nodeDescsTmp);
        
        // --- convert edge descriptors
        final List<EdgeDescriptor> edgeDescsTmp =
                new LinkedList<EdgeDescriptor>();
        for (final EdgeDescriptor desc: edgeDescriptors) {
            final Object source;
            if (desc.source instanceof Point) {
                source = ((Point) desc.source).times(to);
            } else {
                source = desc.source;
            }
            final Object target;
            if (desc.target instanceof Point) {
                target = ((Point) desc.target).times(to);
            } else {
                target = desc.target;
            }
            edgeDescsTmp.add(new EdgeDescriptor(source, target, desc.shift));
        }
        edgeDescriptors.clear();
        edgeDescriptors.addAll(edgeDescsTmp);
        
        // --- convert gram matrix
        if (cellGram != null) {
            cellGram = ((Matrix) primitiveCell.times(cellGram).times(
                    primitiveCell.transposed())).symmetric();
        }
        
        // --- apply group operators to generate all nodes
        final List<Pair<NodeDescriptor, Operator>> allNodes = applyOps(ops,
                nodeDescriptors, precision);
        
        if (DEBUG) {
            System.err.println();
            System.err.println("Generated " + allNodes.size()
                    + " nodes in unit cell.");
        }
        
        // --- Create a net with the computed nodes
        final Net G = new Net(dim, getName(), getSpaceGroup());
        final Map<INode, Pair<NodeDescriptor, Operator>>
            nodeToDescriptorAddress =
            new HashMap<INode, Pair<NodeDescriptor, Operator>>();
        
        for (final Pair<NodeDescriptor, Operator> adr: allNodes) {
            final NodeDescriptor desc = adr.getFirst();
            final INode v = G.newNode("" + desc.name);
            G.setNodeInfo(v, CONNECTIVITY, new Integer(desc.connectivity));
            G.setNodeInfo(v, COORDINATION_SEQUENCE, name2cs.get(desc.name));
            nodeToDescriptorAddress.put(v, adr);
        }

        // --- Map nodes to positions
        final Map<INode, Point> nodeToPosition = new HashMap<INode, Point>();

        for (final INode v: nodeToDescriptorAddress.keySet())
        {
            final Pair<NodeDescriptor, Operator> adr =
                    nodeToDescriptorAddress.get(v);
            nodeToPosition.put(v,
                    (Point) adr.getFirst().site.times(adr.getSecond()));
        }
        
        // --- Handle explicit edges
        addExplicitEdges(G, edgeDescriptors, ops, nameToDesc, nodeToPosition,
                from, precision);
        
        if (DEBUG) {
            System.err.println("Graph after adding explicit edges: " + G);
            System.err.println("  Graph is " + (G.isConnected() ? "" : "not ") +
            		"connected.");
        }
        
        // --- Compute implicit edges
        if (edgeDescriptors.size() == 0)
            computeImplicitEdges(G, cellGram, nodeToDescriptorAddress,
                    nodeToPosition, from, minEdgeLength);
        
        // TODO check to see if all nodes have the right number of neighbors
        
        // --- remove nodes that are really meant to be edge centers
        removeEdgeCenters(G, nodeToDescriptorAddress);
        
        if (DEBUG) {
            System.err.println("--------------------");
        }

        // Store the original positions for all nodes.
        for (final INode v: G.nodes()) {
            G.setNodeInfo(v, POSITION, nodeToPosition.get(v));
        }

        // Return the result.
        for (Iterator<String> iter = warnings.iterator(); iter.hasNext();)
        	G.addWarning(iter.next());
        return G;
    }

    /**
     * @param groupName
     * @param dim
     * @return
     */
    private static Matrix defaultGramMatrix(final String groupName,
                                            final int dim) {
        if (dim == 1) {
            return new Matrix(new double[][] { { 1.0 } });
        } else if (dim == 2) {
        	final char c = groupName.charAt(1);
        	if (c == '3' || c == '6') {
        		return new Matrix(new double[][] {
        				{  1.0, -0.5 },
        				{ -0.5,  1.0 }
        		});
        	} else {
        		return new Matrix(new double[][] {
        				{  1.0,  0.0 },
        				{  0.0,  1.0 }
        		});
        	}
        } else if (dim == 3) {
        	final char c;
        	if (groupName.charAt(1) == '-') {
        		c = groupName.charAt(2);
        	} else {
        		c = groupName.charAt(1);
        	}
        	if (c == '3' || c == '6') {
        		return new Matrix(new double[][] {
        				{  1.0, -0.5,  0.0 },
        				{ -0.5,  1.0,  0.0 },
        				{  0.0,  0.0,  1.0 },
        		});
        	} else {
        		return new Matrix(new double[][] {
        				{  1.0,  0.0,  0.0 },
        				{  0.0,  1.0,  0.0 },
        				{  0.0,  0.0,  1.0 },
        		});
        	}
        } else
            throw new RuntimeException("illegal dimension " + dim);
    }

    /**
     * @param G
     * @param cellGram
     * @param nodeToDescriptorAddress
     * @param nodeToPosition
     * @param from
     * @param minEdgeLength
     * @param edgeDescriptors
     */
    private void computeImplicitEdges(
            final Net G,
            final Matrix cellGram,
            final Map<INode, Pair<NodeDescriptor, Operator>>
                nodeToDescriptorAddress,
            final Map<INode, Point> nodeToPosition,
            final Operator from,
            final double minEdgeLength) {

        // --- construct a Dirichlet domain for the translation group
        final Vector basis[] = Vector.rowVectors(Matrix.one(G.getDimension()));
        if (DEBUG) {
            System.err.println("Computing Dirichlet vectors...");
        }
        final Vector dirichletVectors[] = Lattices.dirichletVectors(basis,
                cellGram);
        if (DEBUG) {
            for (int i = 0; i < dirichletVectors.length; ++i) {
                System.err.println("  " + dirichletVectors[i]);
            }
        }

        // --- shift generated nodes into the Dirichlet domain
        for (final INode v: nodeToPosition.keySet()) {
            final Point p = nodeToPosition.get(v);
            // --- shift into Dirichlet domain
            if (DEBUG) {
                System.err.println("Shifting " + p + " / " + p.times(from)
                        + " into Dirichlet domain...");
            }
            final Vector shift = Lattices.dirichletShifts(p, dirichletVectors,
                    cellGram, 1)[0];
            if (DEBUG) {
                System.err.println("  shift is " + shift);
            }
            nodeToPosition.put(v, (Point) p.plus(shift));
            G.shiftNode(v, shift);
            if (DEBUG) {
                System.err.println("  shifting done");
            }
        }

        // --- compute nodes in two times extended Dirichlet domain
        final Vector zero = Vector.zero(G.getDimension());

        final List<Pair<INode, Vector>> extended =
                new ArrayList<Pair<INode, Vector>>();
        final Map<Pair<INode, Vector>, Point> addressToPosition =
                new HashMap<Pair<INode, Vector>, Point>();
        for (final INode v: G.nodes()) {
            TaskController.getInstance().bailOutIfCancelled();
            final Point pv = nodeToPosition.get(v);
            if (DEBUG) {
                System.err.println();
                System.err.println("Extending " + v + " at " + pv);
            }
            extended.add(new Pair<INode, Vector>(v, zero));
            addressToPosition.put(new Pair<INode, Vector>(v, zero), pv);
            for (int i = 0; i < dirichletVectors.length; ++i) {
                final Vector vec = dirichletVectors[i];
                if (DEBUG) {
                    System.err.println("  shifting by " + vec);
                }
                final Point p = (Point) pv.plus(vec);
                final Vector shifts[] = Lattices.dirichletShifts(p,
                        dirichletVectors, cellGram, 2);
                if (DEBUG) {
                    System.err.println("    induced " + shifts.length
                            + " further shifts");
                }
                for (int k = 0; k < shifts.length; ++k) {
                    final Vector shift = shifts[k];
                    if (DEBUG) {
                        System.err.println("      added with shift " + shift);
                    }
                    final Pair<INode, Vector> adr = new Pair<INode, Vector>(v,
                            (Vector) vec.plus(shift));
                    extended.add(adr);
                    addressToPosition.put(adr, (Point) p.plus(shift));
                }
            }
        }

        if (DEBUG) {
            System.err.println();
            System.err.println("Generated " + extended.size()
                    + " nodes in extended Dirichlet domain.");
            System.err.println();
        }

        // --- compute potential edges
        final List<Pair<IArithmetic, Pair<INode, Integer>>> edges =
                new ArrayList<Pair<IArithmetic, Pair<INode, Integer>>>();
        for (final INode v: G.nodes()) {
            TaskController.getInstance().bailOutIfCancelled();
            final NodeDescriptor descV =
                    nodeToDescriptorAddress.get(v).getFirst();
            final Pair<INode, Vector> adr0 = new Pair<INode, Vector>(v, zero);
            final Point pv = nodeToPosition.get(v);
            final List<Pair<IArithmetic, Integer>> distances =
                    new ArrayList<Pair<IArithmetic, Integer>>();
            for (int i = 0; i < extended.size(); ++i) {
                TaskController.getInstance().bailOutIfCancelled();
                final Pair<INode, Vector> adr = extended.get(i);
                if (adr.equals(adr0)) {
                    continue;
                }
                final NodeDescriptor descW =
                        nodeToDescriptorAddress.get(adr.getFirst()).getFirst();
                if (descV.isEdgeCenter && descW.isEdgeCenter) {
                    continue;
                }

                final Point pos = addressToPosition.get(adr);
                final Vector diff0 = (Vector) pos.minus(pv);
                final Matrix diff = diff0.getCoordinates();
                final IArithmetic dist = LinearAlgebra.dotRows(diff, diff,
                        cellGram);
                distances.add(new Pair<IArithmetic, Integer>(dist, i));
            }
            Collections.sort(distances,
                    Pair.<IArithmetic, Integer>defaultComparator());

            for (int i = 0; i < descV.connectivity; ++i) {
                final Pair<IArithmetic, Integer> entry = distances.get(i);
                final IArithmetic dist = entry.getFirst();
                final Integer k = entry.getSecond();
                edges.add(new Pair<IArithmetic, Pair<INode, Integer>>(dist,
                        new Pair<INode, Integer>(v, k)));
            }
        }

        // --- sort potential edges by length
        Collections.sort(edges, Pair.<IArithmetic>firstItemComparator());

        // --- add eges shortest to longest until all nodes are saturated
        for (final Pair<IArithmetic, Pair<INode, Integer>> edge: edges) {
            final double dist = ((Real) edge.getFirst()).doubleValue();
            final Pair<INode, Integer> ends = edge.getSecond();
            final INode v = ends.getFirst();
            final Pair<INode, Vector> adr = extended.get(ends.getSecond());
            final INode w = adr.getFirst();
            final Vector s = adr.getSecond();

            final NodeDescriptor descV =
                    nodeToDescriptorAddress.get(v).getFirst();
            final NodeDescriptor descW =
                    nodeToDescriptorAddress.get(w).getFirst();

            if (v.degree() >= descV.connectivity
                    && w.degree() >= descW.connectivity) {
                continue;
            }
            if (dist < minEdgeLength) {
                final String msg = "Found points closer than minimal edge length of ";
                throw new DataFormatException(msg + minEdgeLength);
            }
            if (G.getEdge(v, w, s) == null) {
                if (DEBUG) {
                    System.err.println("Adding edge from " + v + " to (" + w
                            + ", " + s + ") of length " + dist);
                }

                G.newEdge(v, w, s);
            }
            if (v.degree() > descV.connectivity) {
                final String msg = "Found " + v.degree()
                        + " neighbors for node " + descV.name + " (should be "
                        + descV.connectivity + ")";
                throw new DataFormatException(msg);
            }
            if (w.degree() > descW.connectivity) {
                final String msg = "Found " + w.degree()
                        + " neighbors for node " + descW.name + " (should be "
                        + descW.connectivity + ")";
                throw new DataFormatException(msg);
            }
        }
    }

    /**
     * @param G
     * @param nodeToAdr
     */
    private void removeEdgeCenters(
            final Net G,
            final Map<INode, Pair<NodeDescriptor, Operator>> nodeToAdr) {
        final Set<INode> bogus = new HashSet<INode>();
        for (final INode v: G.nodes()) {
            if (nodeToAdr.get(v).getFirst().isEdgeCenter) {
                bogus.add(v);
            }
        }
        for (final INode v: bogus) {
            final List<IEdge> inc = G.allIncidences(v);
            if (inc.size() != 2) {
                throw new DataFormatException(
                        "Edge center has connectivity != 2");
            }
            final IEdge e1 = inc.get(0);
            final INode w1 = e1.opposite(v);
            final IEdge e2 = inc.get(1);
            final INode w2 = e2.opposite(v);
            final Vector shift = (Vector) G.getShift(e2).minus(G.getShift(e1));
            if (G.getEdge(w1, w2, shift) != null) {
                throw new DataFormatException("duplicate edge");
            } else if (w1.equals(w2) && shift.equals(shift.zero())) {
                throw new DataFormatException("trivial loop");
            }
            G.newEdge(w1, w2, shift);
            G.delete(e1);
            G.delete(e2);
            G.delete(v);
        }
    }

    /**
     * @param G
     * @param edgeDescriptors
     * @param ops
     * @param nameToDesc
     * @param nodeToPosition
     * @param from
     * @param precision
     */
    private void addExplicitEdges(final Net G,
            final List<EdgeDescriptor> edgeDescriptors,
            final List<Operator> ops,
            final Map<Object, NodeDescriptor> nameToDesc,
            final Map<INode, Point> nodeToPosition,
            final Operator from,
            final double precision) {
        for (final EdgeDescriptor desc: edgeDescriptors) {
            if (DEBUG) {
                System.err.println();
                System.err.println("Adding edge " + desc);
            }
            final Point sourcePos;
            if (desc.source instanceof Point) {
                sourcePos = (Point) desc.source;
            } else {
                sourcePos = (Point) nameToDesc.get(desc.source).site;
            }
            final Point targetPos;
            if (desc.target instanceof Point) {
                targetPos = (Point) desc.target;
            } else {
                targetPos = (Point) nameToDesc.get(desc.target).site;
            }
            
            // --- loop through the operators to generate all images
            for (final Operator oper: ops) {
                // --- get the next coset representative
                final Operator op = oper.modZ();
                if (DEBUG) {
                    System.err.println("  applying " + op);
                }
                final Point p = (Point) sourcePos.times(op);
                final Point q = (Point) targetPos.times(op);
                final Pair<INode, Vector> pAdr =
                        lookup(p, nodeToPosition, precision);
                final Pair<INode, Vector> qAdr =
                        lookup(q, nodeToPosition, precision);
                if (pAdr == null) {
                    throw new DataFormatException("no point at "
                            + p.times(from));
                }
                if (qAdr == null) {
                    throw new DataFormatException("no point at "
                            + q.times(from));
                }
                final INode v = pAdr.getFirst();
                final INode w = qAdr.getFirst();
                final Vector vShift = pAdr.getSecond();
                final Vector wShift = qAdr.getSecond();
                final Vector s = (Vector) wShift.minus(vShift);
                if (G.getEdge(v, w, s) == null) {
                    G.newEdge(v, w, s);
                }
            }
        }
    }

    /**
     * @param ops
     * @param nodeDescriptors
     * @param precision
     * @return
     */
    private List<Pair<NodeDescriptor, Operator>> applyOps(
            final List<Operator> ops,
            final List<NodeDescriptor> nodeDescriptors,
            final double precision) {
        final List<Pair<NodeDescriptor, Operator>> allNodes =
                new LinkedList<Pair<NodeDescriptor, Operator>>();

        for (final NodeDescriptor desc: nodeDescriptors) {
            if (DEBUG) {
                System.err.println();
                System.err.println("Mapping node " + desc);
            }
            final Point site = (Point) desc.site;
            final Set<Operator> stabilizer =
                    pointStabilizer(site, ops, precision);
            if (DEBUG) {
                System.err.println("  stabilizer has size " + stabilizer.size());
            }
            // --- loop through the cosets of the stabilizer
            final Set<Operator> opsSeen = new HashSet<Operator>();
            for (final Operator oper: ops) {
                // --- get the next coset representative
                final Operator op = oper.modZ();
                if (!opsSeen.contains(op)) {
                    if (DEBUG) {
                        System.err.println("  applying " + op);
                    }
                    allNodes.add(new Pair<NodeDescriptor, Operator>(desc, op));
                    // --- mark operators that should not be used anymore
                    for (final Operator op1: stabilizer) {
                        final Operator a = ((Operator) op1.times(op)).modZ();
                        opsSeen.add(a);
                        if (DEBUG) {
                            System.err.println("  marking operator " + a + " as used");
                        }
                    }
                }
            }
        }
        return allNodes;
    }

    static private double gramMatrixError(
            int dim, SpaceGroup group, Matrix cellGram)
    {
        final double g[] = new double[dim * (dim + 1) / 2];
        int k = 0;
        for (int i = 0; i < dim; ++i) {
            for (int j = i; j < dim; ++j) {
                g[k] = ((Real) cellGram.get(i, j)).doubleValue();
                ++k;
            }
        }
        final Matrix S = group.configurationSpaceForGramMatrix();
        final Matrix A = new Matrix(new double[][] { g });
        final Matrix M = LinearAlgebra.solutionInRows(S, A, false);
        if (M == null)
            return 1;
        final Matrix D = (Matrix) ((Matrix) M.times(S)).minus(A);
        return ((Real) D.norm()).doubleValue();
    }
    
    public static class Face implements Comparable<Face> {
    	final private int size;
    	final private int vertices[];
    	final private Vector shifts[];
    	
    	public Face(final int points[], final Vector shifts[]) {
    		if (points.length != shifts.length) {
    			throw new RuntimeException("lengths do not match");
    		}
    		this.vertices = (int[]) points.clone();
    		this.shifts = (Vector[]) shifts.clone();
    		this.size = shifts.length;
    	}
    	
		public int vertex(final int i) {
			return this.vertices[i];
		}
		public Vector shift(final int i) {
			return this.shifts[i];
		}
		public int size() {
			return this.size;
		}
		
		public int hashCode() {
			int code = 0;
			for (int i = 0; i < size(); ++i) {
				code = (code * 37 + vertex(i)) * 127 + shift(i).hashCode();
			}
			return code;
		}
		
		public int compareTo(final Face f) {
		    int d = 0;
		    for (int i = 0; i < size(); ++i) {
		        d = vertex(i) - f.vertex(i);
		        if (d != 0) {
		            return d;
		        }
		        d = shift(i).compareTo(f.shift(i));
		        if (d != 0) {
		            return d;
		        }
		    }
		    return 0;
		}
		
		public boolean equals(final Object other) {
		    if (other instanceof Face)
		        return compareTo((Face) other) == 0;
		    else
		        return false;
		}
		
		public String toString() {
			final  StringBuffer buf = new StringBuffer(100);
			for (int i = 0; i < size(); ++i) {
				if (i > 0) {
					buf.append('-');
				}
				buf.append('(');
				buf.append(vertex(i));
				buf.append(',');
				buf.append(shift(i).toString().replaceFirst("Vector", ""));
				buf.append(')');
			}
			return buf.toString();
		}
    }
    
    /**
     * Parses a list of rings.
     * 
     * @param block the pre-parsed input.
     * @return the ring list in symbolic form.
     */
    private static FaceListDescriptor parseFaceList(final Entry[] block) {
        final Set<String> seen = new HashSet<String>();
        
        String groupName = null;
        int dim = 3;
        SpaceGroup group = null;
        List<Operator> ops = new ArrayList<Operator>();
        Matrix cellGram = null;
        
        double precision = 0.001;
        boolean useTiles = false;
        final List<List<Point[]>> faceLists = new ArrayList<List<Point[]>>();
        List<Point[]> faces = new ArrayList<Point[]>();
        IArithmetic faceData[] = null;
        int faceDataIndex = 0;

        final List<String> warnings = new ArrayList<String>();
        
        // --- collect data from the input
        for (int i = 0; i < block.length; ++i) {
            final List<Object> row = block[i].values;
            final String key = block[i].key;
            final int lineNr = block[i].lineNumber;
            if (key.equals("group")) {
                if (seen.contains(key)) {
                    final String msg = "Group specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                if (row.size() < 1) {
                    final String msg = "Missing argument at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                groupName = (String) row.get(0);
                group = parseSpaceGroupName(groupName);
                if (group == null) {
                    final String msg = "Space group \"" + groupName
                            + "\" not recognized at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                dim = group.getDimension();
                groupName = SpaceGroupCatalogue.listedName(dim, groupName);
                ops.addAll(group.getOperators());
            } else if (key.equals("cell")) {
                if (seen.contains(key)) {
                    final String msg = "Cell specified twice at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                final int m = dim + dim * (dim-1) / 2;
                if (row.size() != m) {
                    final String msg = "Expected " + m + " arguments at line ";
                    throw new DataFormatException(msg + lineNr);
                }
                for (int j = 0; j < m; ++j) {
                    if (!(row.get(i) instanceof Real)) {
                        final String msg = "Arguments must be real numbers at line ";
                        throw new DataFormatException(msg + lineNr);
                    }
                }
                cellGram = gramMatrix(dim, row);
            } else if (key.equals("face")) {
                for (int j = 0; j < row.size(); ++j) {
                    final Object item = row.get(j);
                    if (faceData == null) {
                        if (item instanceof Whole) {
                            final int n = ((Whole) item).intValue();
                            faceData = new IArithmetic[n * dim];
                            faceDataIndex = 0;
                        } else {
                            String msg = "face size expected at line ";
                            throw new DataFormatException(msg + lineNr);
                        }
                    } else {
                        if (item instanceof IArithmetic) {
                            faceData[faceDataIndex++] = (IArithmetic) item;
                            if (faceDataIndex == faceData.length) {
                                final int n = faceData.length / dim;
                                final Point f[] = new Point[n];
                                int p = 0;
                                for (int nu = 0; nu < n; ++nu) {
                                    IArithmetic pos[] = new IArithmetic[dim];
                                    for (int k = 0; k < dim; ++k) {
                                        pos[k] = faceData[p++];
                                    }
                                    f[nu] = new Point(pos);
                                }
                                faces.add(f);
                                faceData = null;
                            }
                        } else {
                            String msg = "coordinate expected at line ";
                            throw new DataFormatException(msg + lineNr);
                        }
                    }
                }
            } else if (key.equals("tile")) {
                useTiles = true;
                if (faces.size() > 0) {
                    faceLists.add(faces);
                }
                faces = new ArrayList<Point[]>();
            } else {
                // store additional entrys here
            }
            seen.add(key);
        }
        if (faces.size() > 0) {
            faceLists.add(faces);
        }
        
        // --- use reasonable default for missing data
        if (group == null) {
            warnings.add("Space group missing; assuming P1");
            groupName = "P1";
            group = parseSpaceGroupName(groupName);
            dim = group.getDimension();
            ops.addAll(group.getOperators());
        }
        if (cellGram == null) {
            warnings.add("Unit cell parameters missing; using defaults");
            cellGram = defaultGramMatrix(groupName, dim);
        }
        
        // --- output some of the basic data
        if (DEBUG) {
            System.err.println();
            System.err.println("Group name: " + groupName);
            System.err.println("  operators:");
            for (final Operator op: ops) {
                System.err.println("    " + op);
            }
            System.err.println();

            System.err.println("Cell gram matrix = " + cellGram);
            System.err.println();
            
            if (useTiles) {
                System.err.println("Tiles:");
            } else {
                System.err.println("Faces:");
            }
            for (final List<Point[]> list: faceLists) {
                for (final Point f[]: list) {
                    System.err.print("   ");
                    for (int i = 0; i < f.length; ++i) {
                        System.err.print(" " + f[i]);
                    }
                    System.err.println();
                }
                System.err.println();
            }
        }
        
        // --- warn about illegal cell parameters
        if (gramMatrixError(dim, group, cellGram) > 0.01)
                warnings.add("Unit cell parameters illegal for this group");
        
        // --- get info for converting to a primitive setting
        final Matrix primitiveCell = group.primitiveCell();
        final Operator to = group.transformationToPrimitive();
        final Operator from = (Operator) to.inverse();
        if (DEBUG) {
            System.err.println();
            System.err.println("Primitive cell: " + primitiveCell);
        }
        
        // --- extract and convert operators
        final Set<Operator> primitiveOps = group.primitiveOperators();
        ops.clear();
        for (final Operator op: primitiveOps) {
            ops.add(((Operator) from.times(op).times(to)).modZ());
        }
        
        // --- convert face lists
        for (final List<Point[]> list: faceLists) {
            for (int i = 0; i < list.size(); ++i) {
                final Point faceOld[] = list.get(i);
                final Point faceNew[] = new Point[faceOld.length];
                for (int k = 0; k < faceOld.length; ++k) {
                    faceNew[k] = (Point) faceOld[k].times(to);
                }
                list.set(i, faceNew);
            }
        }
        
        // --- convert gram matrix
        if (cellGram != null) {
            cellGram = ((Matrix) primitiveCell.times(cellGram).times(
                    primitiveCell.transposed())).symmetric();
        }
        
        // --- apply group operators to generate all corner points
        final Map<Integer, Point> indexToPos = new HashMap<Integer, Point>();
        
        for (final List<Point[]> list: faceLists) {
            for (final Point face[]: list) {
                for (int i = 0; i < face.length; ++i) {
                    final Point site = face[i];
                    if (lookup(site, indexToPos, precision) != null) {
                        if (DEBUG) {
                            System.err.println();
                            System.err.println("Ignoring point " + site);
                        }
                        continue;
                    }
                    if (DEBUG) {
                        System.err.println();
                        System.err.println("Mapping point " + site);
                    }
                    final Set<Operator> stabilizer =
                            pointStabilizer(site, ops, precision);
                    if (DEBUG) {
                        System.err.println("  stabilizer has size "
                                + stabilizer.size());
                    }
                    // --- loop through the cosets of the stabilizer
                    final Set<Operator> opsSeen = new HashSet<Operator>();
                    for (final Operator oper: ops) {
                        // --- get the next coset representative
                        final Operator op = oper.modZ();
                        if (!opsSeen.contains(op)) {
                            if (DEBUG) {
                                System.err.println("  applying " + op);
                            }
                            // --- compute mapped node position
                            final Point p = (Point) site.times(op);
                            indexToPos.put(indexToPos.size(), p);

                            // --- mark operators that should not be used anymore
                            for (final Operator op2: stabilizer) {
                                final Operator a = (Operator) op2.times(op);
                                final Operator aModZ = a.modZ();
                                opsSeen.add(aModZ);
                                if (DEBUG) {
                                    System.err.println("  marking operator "
                                            + aModZ + " as used");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (DEBUG) {
			System.err.println();
			System.err.println("Generated " + indexToPos.size()
					+ " nodes in primitive cell.\n");
		}
        
        final Set<Object> notNew = new HashSet<Object>();
        final List<Object> result = new ArrayList<Object>();
        for (final List<Point[]> list: faceLists) {
            for (final Operator oper: ops) {
                final Operator op = oper.modZ();
                final List<Pair<Face, Vector>> mappedFaces =
                        new ArrayList<Pair<Face, Vector>>();
                
                for (final Point f[]: list) {
                    final int n = f.length;
                    final int points[] = new int[n];
                    final Vector shifts[] = new Vector[n];
                    for (int i = 0; i < n; ++i) {
                        final Pair<Integer, Vector> p =
                                lookup((Point) f[i].times(op),
                                        indexToPos, precision);
                        points[i] = p.getFirst();
                        shifts[i] = p.getSecond();
                    }
                    final Face fMapped = new Face(points, shifts);
                    if (DEBUG) {
                        System.err.println("Mapped face: " + fMapped);
                    }
                    final Pair<Face, Vector> normalized =
                            normalizedFace(fMapped);
                    if (DEBUG) {
                        System.err.println("  normalized: " + normalized);
                    }
                    if (useTiles) {
                        mappedFaces.add(normalized);
                    } else {
                        final Face fNormal = normalized.getFirst();
                        if (notNew.contains(fNormal)) {
                            if (DEBUG) {
                                System.err.println("  rejected!");
                            }
                        } else {
                            if (DEBUG) {
                                System.err.println("  accepted!");
                            }
                            result.add(fNormal);
                            notNew.add(fNormal);
                        }
                    }
                }
                if (useTiles) {
                    if (DEBUG) {
                        System.err.println("Mapped tile: " + mappedFaces);
                    }
                    final List<Pair<Face, Vector>> tNormal =
                            normalizedTile(mappedFaces);
                    if (DEBUG) {
                        System.err.println("  normalized: " + tNormal);
                    }
                    if (notNew.contains(tNormal)) {
                        if (DEBUG) {
                            System.err.println("  rejected!");
                        }
                    } else {
                        if (DEBUG) {
                            System.err.println("  accepted!");
                        }
                        result.add(tNormal);
                        notNew.add(tNormal);
                    }
                }
            }
        }
        
        // --- return the result here
        if (DEBUG) {
        	System.err.println("\nAccepted " + result.size()
                       + (useTiles ? " tiles:" : " faces:"));
        	for (final Object entry: result) {
        		System.err.println("  " + entry);
        	}
        }
        return new FaceListDescriptor(result, indexToPos);
    }
    
    public static FaceListDescriptor parseFaceList(final Block block) {
    	return parseFaceList(block.getEntries());
    }
    
    /**
	 * @param face
	 * @return the normalized form of the given face.
	 */
	public static Pair<Face, Vector> normalizedFace(final Face face) {
		final int n = face.size();
		Face trial;
		Face best = null;
        Vector bestShift = null;
		for (int i = 0; i < n; ++i) {
			final Vector s = face.shift(i);
			int points[] = new int[n];
			Vector shifts[] = new Vector[n];
			for (int k = 0; k < n; ++k) {
				final int index = (i + k) % n;
				final int v = face.vertex(index);
				final Vector t = face.shift(index);
				points[k] = v;
				shifts[k] = (Vector) t.minus(s);
			}
			trial = new Face(points, shifts);
			for (int r = 0; r <= 1; ++r) {
				if (best == null || best.compareTo(trial) > 0) {
					best = trial;
                    bestShift = s;
				}
				for (int k = 1; k  < (n + 1) / 2; ++k) {
					final int t = points[k];
					points[k] = points[n - k];
					points[n - k] = t;
					final Vector tmp = shifts[k];
					shifts[k] = shifts[n - k];
					shifts[n - k] = tmp;
				}
				trial = new Face(points, shifts);
			}
		}

		return new Pair<Face, Vector>(best, bestShift);
	}

    private static List<Pair<Face, Vector>> normalizedTile(
            final List<Pair<Face, Vector>> tile) {

        final Comparator<Pair<Face, Vector>> pairComparator =
                Pair.<Face, Vector>defaultComparator();

        final Comparator<List<Pair<Face, Vector>>> listComparator =
                NiftyList.<Pair<Face, Vector>>
                    lexicographicComparator(pairComparator);
        
        List<Pair<Face, Vector>> best = null;
        for (int i = 0; i < tile.size(); ++i) {
            final Vector shift = tile.get(i).getSecond();
            final List<Pair<Face, Vector>> current =
                    new ArrayList<Pair<Face, Vector>>();
            for (final Pair<Face, Vector> pair: tile) {
                final Face face = pair.getFirst();
                final Vector t = pair.getSecond();
                current.add(
                        new Pair<Face, Vector>(face, (Vector) t.minus(shift)));
            }
            Collections.sort(current, pairComparator);
            
            if (best == null || listComparator.compare(best, current) < 0) {
                best = current;
            }
        }
        
        return best;
    }
    
	/**
     * Finds the node and shift associated to a point position.
     * @param pos the position to look up.
     * @param nodeToPos maps nodes to positions.
     * @param precision how close must points be to considered equal.
     * 
     * @return the (node, shift) pair found or else null.
     */
    private static <K> Pair<K, Vector> lookup(
            final Point pos,
            final Map<K, Point> keyToPos,
            final double precision)
    {
        final int d = pos.getDimension();
        for (final K v: keyToPos.keySet()) {
            final Point p = keyToPos.get(v);
            if (distModZ(pos, p) <= precision) {
                final Vector diff = (Vector) pos.minus(p);
                final int s[] = new int[d];
                for (int i = 0; i < d; ++i) {
                    final double x = ((Real) diff.get(i)).doubleValue();
                    s[i] = (int) Math.round(x);
                }
                return new Pair<K, Vector>(v, new Vector(s));
            }
        }
        return null;
    }

    /**
     * Constructs a gram matrix for the edge vectors of a unit cell which is specified by
     * its cell parameters as according to crystallographic conventions.
     * 
     * @param dim the dimension of the cell.
     * @param cellParameters the list of cell parameters.
     * @return the gram matrix for the vectors.
     */
    private static Matrix gramMatrix(int dim,
            final List<Object> cellParameters) {
        if (dim == 2) {
            final Real a = (Real) cellParameters.get(0);
            final Real b = (Real) cellParameters.get(1);
            final Real angle = (Real) cellParameters.get(2);
            final Real x = (Real) cosine(angle).times(a).times(b);
            
            return new Matrix(new IArithmetic[][] { { a.raisedTo(2), x },
                    { x, b.raisedTo(2) } });
        } else if (dim == 3) {
            final Real a = (Real) cellParameters.get(0);
            final Real b = (Real) cellParameters.get(1);
            final Real c = (Real) cellParameters.get(2);
            final Real alpha = (Real) cellParameters.get(3);
            final Real beta = (Real) cellParameters.get(4);
            final Real gamma = (Real) cellParameters.get(5);
            
            final Real alphaG = (Real) cosine(alpha).times(b).times(c);
            final Real betaG = (Real) cosine(beta).times(a).times(c);
            final Real gammaG = (Real) cosine(gamma).times(a).times(b);

            return new Matrix(
                    new IArithmetic[][] { { a.raisedTo(2), gammaG, betaG },
                            { gammaG, b.raisedTo(2), alphaG },
                            { betaG, alphaG, c.raisedTo(2) }, });
        } else {
            throw new DataFormatException("supporting only dimensions 2 and 3");
        }
    }
    
    /**
     * Computes the cosine of an angle given in degrees, using the {@link Real} type for
     * the argument and return value.
     * 
     * @param arg the angle in degrees.
     * @return the value of the cosine.
     */
    private static Real cosine(final Real arg) {
        final double f = Math.PI / 180.0;
        return new FloatingPoint(Math.cos(arg.doubleValue() * f));
    }
    
    /**
     * Computes the stabilizer of a site modulo lattice translations.The infinity norm
     * (largest absolute value of a matrix entry) is used to determine the distances
     * between points.
     * 
     * Currently only tested for point sites.
     * 
     * @param site the site.
     * @param ops operators forming the symmetry group.
     * @param precision points this close are considered equal.
     * @return the set of operators forming the stabilizer
     */
    private static Set<Operator> pointStabilizer(
            final Point site, final List<Operator> ops, final double precision)
    {
        final Set<Operator> stabilizer = new HashSet<Operator>();
        
        for (final Operator op: ops) {
            final double dist = distModZ(site, (Point) site.times(op));
            if (dist <= precision) { // using "<=" allows for precision 0
                stabilizer.add(op.modZ());
            }
        }
        
        // --- check if stabilizer forms a group
        if (!formGroup(stabilizer)) {
            throw new RuntimeException("precision problem in stabilizer computation");
        }

        return stabilizer;
    }
    
    /**
     * Measures the distance between two sites in terms of the infinity norm of
     * the representing matrices. The distance is computed modulo Z^d, where Z
     * is the dimension of the sites, thus, sites are interpreted as residing in
     * the d-dimensional torus.
     * 
     * Currently only implemented for point sites.
     * 
     * @param site1 first point site.
     * @param site2 second point site.
     * @return the distance.
     */
    private static double distModZ(final Point site1, final Point site2) {
        final int dim = site1.getDimension();
        final Vector diff = (Vector) site1.minus(site2);
        double maxD = 0.0;
        for (int j = 0; j < dim; ++j) {
            final double d = ((Real) diff.get(j).mod(Whole.ONE)).doubleValue();
            maxD = Math.max(maxD, Math.min(d, 1.0 - d));
        }
        return maxD;
    }
    
    /**
     * Determines if the given operators form a group modulo Z^d.
     * @param operators a collection of operators.
     * @return true if the operators form a group.
     */
    final static boolean formGroup(final Collection<Operator> operators) {
        for (final Operator A: operators) {
            for (final Operator B: operators) {
                final Operator AB_ = ((Operator) A.times(B.inverse())).modZ();
                if (!operators.contains(AB_)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static void main(final String args[]) {
    	final String s = ""
    		+ "TILING\n"
    		+ "  GROUP P432\n"
    		+ "  FACE 4 0 0 0 1 0 0 1 1 0 0 1 0\n"
    		+ "END\n";
    	final NetParser parser = new NetParser(new StringReader(s));
        final Block data = parser.parseDataBlock();
        System.out.println(parseFaceList(data));
    }
}

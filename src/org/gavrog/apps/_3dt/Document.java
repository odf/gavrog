/**
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


package org.gavrog.apps._3dt;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.gavrog.box.collections.Cache;
import org.gavrog.box.collections.Iterators;
import org.gavrog.box.simple.NamedConstant;
import org.gavrog.box.simple.Tag;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.Signature;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroupCatalogue;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.embed.Embedder;
import org.gavrog.joss.pgraphs.io.GenericParser;
import org.gavrog.joss.pgraphs.io.Net;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.tilings.FaceList;
import org.gavrog.joss.tilings.Tiling;

import de.jreality.scene.Transformation;

/**
 * @author Olaf Delgado
 * @version $Id: Document.java,v 1.44 2008/05/29 06:22:34 odf Exp $
 */
public class Document extends DisplayList {
    // --- the cache keys
	final protected static Object TILES = new Tag();
    final protected static Object CELL_TO_WORLD = new Tag();
    final protected static Object CELL_TO_EMBEDDER = new Tag();
    final protected static Object EMBEDDER = new Tag();
    final protected static Object EMBEDDER_OUTPUT = new Tag();
    final protected static Object FINDER = new Tag();
    final protected static Object SIGNATURE = new Tag();
    final protected static Object SPACEGROUP = new Tag();
    final protected static Object TILING = new Tag();
    final protected static Object WORLD_TO_CELL = new Tag();
    final protected static Object CENTERING_VECTORS = new Tag();
    
    // --- cache for this instance
    final protected Cache cache = new Cache();

    // --- possible document types
    final static public class Type extends NamedConstant {
        public Type(final String name) { super(name); }
    }
    final static public Object TILING_3D = new Type("3d Tiling");
    final static public Object TILING_2D = new Type("2d Tiling");
    final static public Object NET       = new Type("Net");
    
    // --- The type of this instance and its source data
    private Object type;
    final private String name;
    private DSymbol symbol = null;
    private DSymbol effective_symbol = null;
    private DSCover given_cover = null;
    private GenericParser.Block data = null;
    
    // --- The tile and face colors set for this instance
    //TODO this should probably be moved to the DisplayList class.
    private Color[] tileClassColor = null;
    private Map<Tiling.Facet, Color> facetClassColor =
    	new HashMap<Tiling.Facet, Color>();
    private Set<Tiling.Facet> hiddenFacetClasses = new HashSet<Tiling.Facet>();
    
    // --- embedding options
    private int equalEdgePriority = 3;
    private int embedderStepLimit = 10000;
    private boolean useBarycentricPositions = false;

    // --- cell choice options
    private boolean usePrimitiveCell = false;
    
    // --- saved user options
    private Properties  properties = new Properties();
    
    // --- The last remembered viewing transformation
    private Transformation transformation = null;
    
    // --- random number generator
	private final static Random random = new Random();
	
	// --- convert a 2d symbol to 3d by extrusion
	private DSymbol extrusion(final DelaneySymbol ds) {
		if (ds.dim() != 2) {
			throw new UnsupportedOperationException("dimension must be 2");
		}
		final int s = ds.size();
		
		final DynamicDSymbol tmp = new DynamicDSymbol(3);
		final List elms_new = tmp.grow(s * 3);
		final List elms_old = Iterators.asList(ds.elements());
		
		for (int i = 0; i < ds.size(); ++i) {
			final Object Da = elms_new.get(i);
			final Object Db = elms_new.get(i + s);
			final Object Dc = elms_new.get(i + s + s);
			
			final Object D  = elms_old.get(i);
			final int i0 = elms_old.indexOf(ds.op(0, D));
			final int i1 = elms_old.indexOf(ds.op(1, D));
			final int i2 = elms_old.indexOf(ds.op(2, D));
			
			tmp.redefineOp(0, Da, elms_new.get(i0));
			tmp.redefineOp(1, Da, elms_new.get(i1));
			tmp.redefineOp(2, Da, Db);
			tmp.redefineOp(3, Da, Da);
			
			tmp.redefineOp(0, Db, elms_new.get(i0 + s));
			tmp.redefineOp(1, Db, Dc);
			tmp.redefineOp(2, Db, Da);
			tmp.redefineOp(3, Db, elms_new.get(i2 + s));
			
			tmp.redefineOp(0, Dc, Dc);
			tmp.redefineOp(1, Dc, Db);
			tmp.redefineOp(2, Dc, elms_new.get(i1 + s + s));
			tmp.redefineOp(3, Dc, elms_new.get(i2 + s + s));
		}
		
		for (int i = 0; i < ds.size(); ++i) {
			final Object Da = elms_new.get(i);
			final Object Db = elms_new.get(i + s);
			final Object Dc = elms_new.get(i + s + s);
			
			final Object D  = elms_old.get(i);
			tmp.redefineV(0, 1, Da, ds.v(0, 1, D));
			if (D.equals(ds.op(0, D))) {
				tmp.redefineV(0, 1, Db, 2);
			} else {
				tmp.redefineV(0, 1, Db, 1);
			}
			tmp.redefineV(1, 2, Da, 1);
			if (D.equals(ds.op(2, D))) {
				tmp.redefineV(2, 3, Da, 2);
			} else {
				tmp.redefineV(2, 3, Da, 1);
			}
			tmp.redefineV(2, 3, Dc, ds.v(1, 2, D));
		}
			
		return new DSymbol(tmp);
	}
	
	// --- construct the 2d Delaney symbol for a given periodic net
	private DSymbol symbolForNet(final Net net) {
		// -- check if the argument is supported
		if (net.getDimension() != 2) {
			throw new UnsupportedOperationException(
					"Only nets of dimension 2 are supported");
		}
		
		// -- helper class for sorting neighbors by angle
		class Neighbor implements Comparable {
			final private IEdge edge;
			final private double angle;
			
			public Neighbor(final IEdge e, final double a) {
				this.edge = e;
				this.angle = a;
			}
			
			public int compareTo(final Object arg) {
	            if (arg instanceof Neighbor) {
	                final Neighbor other = (Neighbor) arg;
	                if (this.angle < other.angle) {
	                    return -1;
	                } else if (this.angle > other.angle) {
	                    return 1;
	                } else {
	                    return 0;
	                }
	            } else {
	                throw new IllegalArgumentException("Neighbor expected");
	            }
			}
		}
		
		// -- initialize the Delaney symbol; map oriented net edges to chambers
		final Map<IEdge, Object> edge2chamber = new HashMap<IEdge, Object>();
		final DynamicDSymbol ds = new DynamicDSymbol(2);
		for (final Iterator iter = net.edges(); iter.hasNext();) {
			final IEdge e = ((IEdge) iter.next()).oriented();
			final List elms = ds.grow(4);
			edge2chamber.put(e, elms.get(0));
			edge2chamber.put(e.reverse(), elms.get(2));
			ds.redefineOp(2, elms.get(0), elms.get(1));
			ds.redefineOp(2, elms.get(2), elms.get(3));
			ds.redefineOp(0, elms.get(0), elms.get(3));
			ds.redefineOp(0, elms.get(1), elms.get(2));
		}
		
		// -- connect the edge orbits of the Delaney symbol
		final Map pos = net.barycentricPlacement();
		for (final Iterator iter = net.nodes(); iter.hasNext();) {
			final INode v = (INode) iter.next();
			final List<IEdge> incidences = (List<IEdge>) net.allIncidences(v);
			if (!net.goodCombinations(incidences, pos).hasNext()) {
				throw new UnsupportedOperationException(
					"Only convex tilings are currently supported");
			}
			
			final Point p = (Point) pos.get(v);
			final List<Neighbor> neighbors = new ArrayList<Neighbor>();
			for (final IEdge e: incidences) {
				final Vector d =
					(Vector) net.getShift(e).plus(pos.get(e.target())).minus(p);
				final double[] a = d.getCoordinates().asDoubleArray()[0];
				neighbors.add(new Neighbor(e, Math.atan2(a[1], a[0])));
			}
			Collections.sort(neighbors);
			neighbors.add(neighbors.get(0));
			for (int i = 0; i < neighbors.size() - 1; ++i) {
				final IEdge e = neighbors.get(i).edge;
				final IEdge f = neighbors.get(i + 1).edge;
				ds.redefineOp(1,
						ds.op(2, edge2chamber.get(e)), edge2chamber.get(f));
			}
		}
		
		// -- set branching to one everywhere
		for (final Iterator iter = ds.elements(); iter.hasNext();) {
			final Object D = iter.next();
			ds.redefineV(0, 1, D, 1);
			ds.redefineV(1, 2, D, 1);
		}
		
		// -- return the result
		return new DSymbol(ds);
	}
	
    /**
     * Constructs a tiling instance.
     * @param ds the Delaney symbol for the tiling.
     * @param name the name of this instance.
     */
    public Document(final DSymbol ds, final String name) {
    	this(ds, name, null);
	}
	
    /**
     * Constructs a tiling instance.
     * @param ds the Delaney symbol for the tiling.
     * @param name the name of this instance.
     * @param cov a pre-given (pseudo-) toroidal cover.
     */
    public Document(final DSymbol ds, final String name, final DSCover cov) {
        if (ds.dim() == 2) {
        	this.symbol = ds;
            this.effective_symbol = extrusion(ds);
            this.type = TILING_2D;
        } else if (ds.dim() == 3) {
            this.symbol = ds;
            this.effective_symbol = ds;
            this.type = TILING_3D;
        } else {
        	final String msg = "only dimensions 2 and 3 supported";
            throw new UnsupportedOperationException(msg);
        }
        this.name = name;
        this.given_cover = cov;
    }
    
    public Document(final GenericParser.Block block, final String defaultName) {
    	final String type = block.getType().toLowerCase();
    	if (type.equals("tiling")) {
        	this.type = TILING_3D;
    	} else {
    		this.type = NET;
    	}
    	final String name = block.getEntriesAsString("name");
    	if (name == null || name.length() == 0) {
    		this.name = defaultName;
    	} else {
    		this.name = name;
    	}
    	this.data = block;
    }
    
    public void clearCache() {
        this.cache.clear();
    }

	public String getName() {
		return this.name;
	}
	
    public Object getType() {
        return this.type;
    }
    
    public DSymbol getSymbol() {
    	if (this.symbol == null) {
    		if (this.data != null) {
    			if (this.type == TILING_3D) {
    				this.symbol = new FaceList(this.data).getSymbol();
    			} else {
    				final Net net =
    					new NetParser((BufferedReader) null).parseNet(this.data);
    				if (net.getDimension() != 2) {
    					throw new UnsupportedOperationException(
    							"Only nets of dimension 2 are supported.");
    				}
    				this.symbol = symbolForNet(net);
    				this.effective_symbol = extrusion(this.symbol);
    				this.type = TILING_2D;
    			}
    		}
    	}
        return this.symbol;
    }
    
    private DSymbol getEffectiveSymbol() {
    	if (this.effective_symbol == null) {
    		if (this.data != null) {
                this.effective_symbol = getSymbol();
    		}
    	}
        return this.effective_symbol;
    }
    
    public Tiling getTiling() {
        try {
            return (Tiling) cache.get(TILING);
        } catch (Cache.NotFoundException ex) {
            return (Tiling) cache.put(TILING, new Tiling(getEffectiveSymbol(),
					given_cover));
        }
    }

    public Tiling.Skeleton getNet() {
        return getTiling().getSkeleton();
    }
    
	@SuppressWarnings("unchecked")
	public List<Tiling.Tile> getTiles() {
		try {
			return (List) cache.get(TILES);
		} catch (Cache.NotFoundException ex) {
			return (List) cache.put(TILES, getTiling()
					.getTiles());
		}
	}
    
	public Tiling.Tile getTile(final int k) {
		return (Tiling.Tile) getTiles().get(k);
	}
    
    private SpaceGroupFinder getFinder() {
		try {
			return (SpaceGroupFinder) cache.get(FINDER);
		} catch (Cache.NotFoundException ex) {
			return (SpaceGroupFinder) cache.put(FINDER, new SpaceGroupFinder(
					getTiling().getSpaceGroup()));
		}
	}
    
    private Embedder getEmbedder() {
        try {
            return (Embedder) cache.get(EMBEDDER);
        } catch (Cache.NotFoundException ex) {
            return (Embedder) cache.put(EMBEDDER, new Embedder(getNet(), null));
        }
    }

    public void initializeEmbedder() {
        getEmbedder();
    }
    
    public void invalidateEmbedding() {
        cache.remove(EMBEDDER_OUTPUT);
    }
    
    private class EmbedderOutput {
        final private Map positions;
        final private CoordinateChange change;
        
        private EmbedderOutput(final Map pos, final CoordinateChange change) {
            this.positions = pos;
            this.change = change;
        }
    }
    
    private EmbedderOutput getEmbedderOutput() {
        try {
            return (EmbedderOutput) cache.get(EMBEDDER_OUTPUT);
        } catch (Cache.NotFoundException ex) {
            final Embedder embedder = getEmbedder();
            embedder.reset();
            embedder.setPasses(getEqualEdgePriority());
            if (embedder.getGraph().isStable() || getUseBarycentricPositions()) {
                embedder.setRelaxPositions(false);
                embedder.go(500);
            }
            if (!getUseBarycentricPositions()) {
                embedder.setRelaxPositions(true);
                embedder.go(getEmbedderStepLimit());
            }
            embedder.normalize();
            final Matrix G = embedder.getGramMatrix();
            if (!G.equals(G.transposed())) {
              throw new RuntimeException("asymmetric Gram matrix:\n" + G);
            }
            final CoordinateChange change =
              new CoordinateChange(LinearAlgebra.orthonormalRowBasis(G));
            final Map pos = getTiling().cornerPositions(embedder.getPositions());
            
            return (EmbedderOutput) cache.put(EMBEDDER_OUTPUT,
                    new EmbedderOutput(pos, change));
        }
    }
    
    private Map getPositions() {
        return getEmbedderOutput().positions;
    }
    
    public CoordinateChange getEmbedderToWorld() {
    	return getEmbedderOutput().change;
    }
    
    public double[] cornerPosition(final int i, final Object D) {
        final Point p0 = (Point) getPositions().get(new DSPair(i, D));
        final Point p = (Point) p0.times(getEmbedderToWorld());
        return p.getCoordinates().asDoubleArray()[0];
    }
    
    public double volume() {
    	double vol = 0.0;
    	for (final Iterator it = getTiling().getCover().elements(); it
				.hasNext();) {
    		final Object D = it.next();
    		final double p[][] = new double[4][];
    		for (int i = 0; i < 4; ++i)  p[i] = cornerPosition(i, D);
    		for (int i = 1; i < 4; ++i)
    			for (int j = 0; j < 3; ++j)
    				p[i][j] -= p[0][j];
    		final double or = getTiling().coverOrientation(D);
    		vol -= or * p[1][0] * p[2][1] * p[3][2];
    		vol -= or * p[1][1] * p[2][2] * p[3][0];
    		vol -= or * p[1][2] * p[2][0] * p[3][1];
    		vol += or * p[1][2] * p[2][1] * p[3][0];
    		vol += or * p[1][1] * p[2][0] * p[3][2];
    		vol += or * p[1][0] * p[2][2] * p[3][1];
		}
    	return vol / 6.0;
    }
    
    public Point nodePoint(final INode v) {
    	final Object D = getNet().chamberAtNode(v);
        final Point p = (Point) getPositions().get(new DSPair(0, D));
        return (Point) p.times(getEmbedderToWorld());
    }
    
    public Point edgeSourcePoint(final IEdge e) {
    	final Object D = getNet().chamberAtNode(e.source());
        final Point p = (Point) getPositions().get(new DSPair(0, D));
        return (Point) p.times(getEmbedderToWorld());
    }
    
    public Point edgeTargetPoint(final IEdge e) {
    	final Object D = getNet().chamberAtNode(e.target());
    	final Vector s = getNet().getShift(e);
        final Point q0 =
        	(Point) ((Point) getPositions().get(new DSPair(0, D))).plus(s);
        return (Point) q0.times(getEmbedderToWorld());
    }
    
    public List<Vector> centerIntoUnitCell(final Tiling.Tile t) {
    	final int dim = getEffectiveSymbol().dim();
    	final DSPair c = new DSPair(dim, t.getChamber());
    	return pointIntoUnitCell((Point) getPositions().get(c));
    }
    
    public List<Vector> centerIntoUnitCell(final IEdge e) {
    	final Tiling.Skeleton net = getNet();
    	final Object C = net.chamberAtNode(e.source());
    	final Object D = net.chamberAtNode(e.target());
    	final Vector s = net.getShift(e);
        final Point p = (Point) getPositions().get(new DSPair(0, C));
        final Point q =
        	(Point) ((Point) getPositions().get(new DSPair(0, D))).plus(s);
    	return pointIntoUnitCell(
    			(Point) p.plus(((Vector) q.minus(p)).times(0.5)));
    }
    
    public List<Vector> centerIntoUnitCell(final INode v) {
    	final Object D = getNet().chamberAtNode(v);
    	return pointIntoUnitCell((Point) getPositions().get(new DSPair(0, D)));
    }
    
    private Vector shifted(final Point p0, final Vector s,
        final CoordinateChange c)
    {
        final int dim = p0.getDimension();
        final Point p = (Point) p0.plus(s).times(c);
        final Real a[] = new Real[dim];
        for (int i = 0; i < dim; ++i) {
            a[i] = (Real) p.get(i).plus(0.001).mod(Whole.ONE);
        }
        final Vector v = (Vector)
            new Point(a).minus(p).times(c.inverse());
        final Whole b[] = new Whole[dim];
        for (int i = 0; i < dim; ++i) {
            b[i] = (Whole) v.get(i).round();
        }
        return (Vector) new Vector(b).plus(s);
    }
    
    public List<Vector> pointIntoUnitCell(final Point p) {
    	final int dim = p.getDimension();
    	final CoordinateChange toStd;
    	if (getUsePrimitiveCell()) {
    	  toStd = new CoordinateChange(Operator.identity(dim));
    	} else {
    	  toStd = getFinder().getToStd();
    	}
    	
    	final List<Vector> result = new ArrayList<Vector>();
    	for (final Vector s : getCenteringVectors()) {
    	    Vector v = shifted(p, s, toStd);
    	    if (getUsePrimitiveCell()) {
  	            v = (Vector) v.plus(originShiftForPrimitive());
    	    }
    	    result.add(v);
		}
    	return result;
    }
    
    public Color[] getPalette() {
    	if (this.tileClassColor == null) {
	    	int n = getEffectiveSymbol().numberOfOrbits(new IndexList(0, 1, 2));
	        this.tileClassColor = new Color[n];
	        fillPalette(this.tileClassColor);
    	}
    	return this.tileClassColor;
    }
    
    private void fillPalette(final Color[] palette) {
    	final int n = palette.length;
    	final int map[] = randomPermutation(n);
        final float offset = random.nextFloat();
        final float s = 0.6f;
        final float b = 1.0f;
        for (int i = 0; i < n; ++i) {
            final float h = (i / (float) n + offset) % 1.0f;
            palette[map[i]] = Color.getHSBColor(h, s, b);
        }
    }
    
    private int[] randomPermutation(final int n) {
    	final int result[] = new int[n];
    	final List<Integer> free = new ArrayList<Integer>();
    	for (int i = 0; i < n; ++i) {
    		free.add(i);
    	}
    	for (int i = 0; i < n; ++i) {
    		final int j = random.nextInt(n - i);
    		result[i] = free.remove(j);
    	}
    	return result;
    }
    
    public Color getEfffectiveColor(final Item item) {
		Color c = color(item);
		if (c == null && item.isFacet()) {
			c = getFacetClassColor(item.getFacet());
			if (c == null) c = getDefaultTileColor(item.getFacet().getTile());
		}
		if (c == null && item.isTile()) c = getDefaultTileColor(item.getTile());
		return c;
    }
    
    public Color getTileClassColor(final int i) {
    	return getPalette()[i];
    }
    
    public Color getDefaultTileColor(final Tiling.Tile t) {
    	return getTileClassColor(t.getKind());
    }
    
    public Color getDefaultTileColor(final int i) {
    	return getDefaultTileColor(getTile(i));
    }
    
    public void setTileClassColor(final int i, final Color c) {
    	getPalette()[i] = c;
    }

    public Color getFacetClassColor(final Tiling.Facet f) {
    	return this.facetClassColor.get(f);
    }

    public Collection<Tiling.Facet> getColoredFacetClasses() {
    	return Collections.unmodifiableSet(this.facetClassColor.keySet());
    }
    
    public void setFacetClassColor(final Tiling.Facet f, final Color c) {
    	this.facetClassColor.put(f, c);
    }

    public void removeFacetClassColor(final Tiling.Facet f) {
    	this.facetClassColor.remove(f);
    }
    
    public boolean isHiddenFacetClass(final Tiling.Facet f) {
    	return this.hiddenFacetClasses.contains(f);
    }
    
    public Collection<Tiling.Facet> getHiddenFacetClasses() {
    	return Collections.unmodifiableSet(this.hiddenFacetClasses);
    }
    
    public void hideFacetClass(final Tiling.Facet f) {
    	this.hiddenFacetClasses.add(f);
    }
    
    public void showFacetClass(final Tiling.Facet f) {
    	this.hiddenFacetClasses.remove(f);
    }
    
    public void randomlyRecolorTiles() {
    	fillPalette(getPalette());
    }
    
    public String getSignature() {
        try {
            return (String) cache.get(SIGNATURE);
        } catch (Cache.NotFoundException ex) {
        	final int dim = getSymbol().dim();
        	final String sig;
        	if (dim == 2) {
        		 sig = Signature.ofTiling(getSymbol());
        	} else {
        		sig = Signature.ofTiling(getTiling().getCover());
        	}
            return (String) cache.put(SIGNATURE, sig);
        }
    }
    
    public String getGroupName() {
    	try {
    		return (String) cache.get(SPACEGROUP);
    	} catch (Cache.NotFoundException ex) {
    		final int dim = getSymbol().dim();
    		final SpaceGroupFinder finder;
    		if (dim == 2) {
    			finder = new SpaceGroupFinder(new Tiling(getSymbol())
						.getSpaceGroup());
    		} else {
    			finder = getFinder();
    		}
    		return (String) cache.put(SPACEGROUP, finder.getGroupName());
    	}
    }
    
    public CoordinateChange getCellToEmbedder() {
        try {
            return (CoordinateChange) cache.get(CELL_TO_EMBEDDER);
        } catch (Cache.NotFoundException ex) {
            final CoordinateChange cc = getFinder().getToStd();
            return (CoordinateChange) cache.put(CELL_TO_EMBEDDER, cc.inverse());
        }
    }
  
    public CoordinateChange getCellToWorld() {
		try {
			return (CoordinateChange) cache.get(CELL_TO_WORLD);
		} catch (Cache.NotFoundException ex) {
		    return (CoordinateChange) cache.put(
		        CELL_TO_WORLD, getCellToEmbedder().times(getEmbedderToWorld()));
		}
	}
    
    public CoordinateChange getWorldToCell() {
    	try {
    		return (CoordinateChange) cache.get(WORLD_TO_CELL);
    	} catch (Cache.NotFoundException ex) {
    		return (CoordinateChange) cache.put(
    		    WORLD_TO_CELL, getCellToWorld().inverse());
    	}
    }
    
    public double[][] getUnitCellVectors() {
    	final int dim = getEffectiveSymbol().dim();
        final CoordinateChange toStd = getFinder().getToStd();
		final double result[][] = new double[dim][];
		for (int i = 0; i < dim; ++i) {
			final Vector v;
			if (getUsePrimitiveCell()) {
              v = (Vector)
                Vector.unit(dim, i).times(toStd).times(getCellToWorld());
			} else {
			  v = (Vector) Vector.unit(dim, i).times(getCellToWorld());
			}
			result[i] = v.getCoordinates().asDoubleArray()[0];
		}
		return result;
	}

    public Vector[] getUnitCellVectorsInEmbedderCoordinates() {
    	final int dim = getEffectiveSymbol().dim();
        final CoordinateChange toStd = getFinder().getToStd();
		final Vector result[] = new Vector[dim];
		for (int i = 0; i < dim; ++i) {
          final Vector v;
          if (getUsePrimitiveCell()) {
              v = (Vector) Vector.unit(dim, i).times(toStd);
          } else {
              v = (Vector) Vector.unit(dim, i);
          }
          result[i] = (Vector) v.times(getCellToEmbedder());
		}
		return result;
	}

    @SuppressWarnings("unchecked")
    private List<Vector> getCenteringVectors() {
        try {
            return (List<Vector>) cache.get(CENTERING_VECTORS);
        } catch (Cache.NotFoundException ex) {
            final List<Vector> result = new ArrayList<Vector>();
            final CoordinateChange fromStd = getFinder().getFromStd();
            final int dim = getEffectiveSymbol().dim();
            if (getUsePrimitiveCell()) {
                result.add(Vector.zero(dim));
            } else {
                for (final Operator op : (List<Operator>) SpaceGroupCatalogue
                        .operators(dim, getFinder().getExtendedGroupName())) {
                    if (op.linearPart().isOne()) {
                        result.add((Vector) op.translationalPart().times(fromStd));
                    }
                }
            }
            return (List<Vector>) cache.put(CENTERING_VECTORS, result);
        }
    }
    
    private Vector originShiftForPrimitive() {
        final int dim = getEffectiveSymbol().dim();
        Point p = Point.origin(dim);
        for (final Vector v: getUnitCellVectorsInEmbedderCoordinates()) {
            p = (Point) p.plus(v);
        }
        p = (Point) p.dividedBy(2);
        return shifted(p, Vector.zero(dim), getFinder().getToStd());
    }
    
	public double[] getOrigin() {
		final int dim = getEffectiveSymbol().dim();
		final CoordinateChange toStd = getFinder().getToStd();
		final CoordinateChange id = new CoordinateChange(Operator.identity(dim));
		final Point o;
		if (getUsePrimitiveCell()) {
		  final Point p = (Point) Point.origin(dim).times(toStd.inverse());
          o = (Point) p.plus(shifted(p, Vector.zero(dim), id))
                       .plus(originShiftForPrimitive())
                       .times(toStd).times(getCellToWorld());
		} else {
		  o = (Point) Point.origin(dim).times(getCellToWorld());
		}
		return o.getCoordinates().asDoubleArray()[0];
	}
    
	private static void add(final StringBuffer buf, final String key,
			final Object val) {
    	final Class cl = val == null ? null : val.getClass();
		final boolean quote = !(cl == Integer.class || cl == Boolean.class);
    	buf.append(key);
    	buf.append(": ");
    	if (quote) buf.append('"');
    	buf.append(val);
    	if (quote) buf.append('"');
    	buf.append('\n');
    }
    
    public String info() {
    	final DSymbol ds = getSymbol();
    	final StringBuffer buf = new StringBuffer(500);
    	buf.append("---\n");
    	if (getName() == null) {
    		add(buf, "name", "unnamed");
    	} else {
    		add(buf, "name", getName().split("\\W+")[0]);
    	}
    	add(buf, "full_name", getName());
    	add(buf, "dsymbol", getSymbol().canonical().toString());
    	add(buf, "symbol_size", ds.size());
    	add(buf, "dimension", ds.dim());
    	add(buf, "transitivity", getTransitivity());
    	add(buf, "minimal", ds.isMinimal());
		add(buf, "self_dual", ds.equals(ds.dual()));
		add(buf, "signature", getSignature());
		add(buf, "spacegroup", getGroupName());
    	return buf.toString();
    }
    
	public String getTransitivity() {
		final StringBuffer buf = new StringBuffer(10);
		final DelaneySymbol ds = getSymbol();
		for (int i = 0; i <= ds.dim(); ++i) {
			buf.append(showNumber(ds.numberOfOrbits(IndexList.except(ds, i))));
		}
		return buf.toString();
	}

	private static String showNumber(final int n) {
		if (n >= 0 && n < 10) {
			return String.valueOf(n);
		} else {
			return "(" + n + ")";
		}
	}

	public static List<Document> load(final String path)
			throws FileNotFoundException {
		final String ext = path.substring(path.lastIndexOf('.') + 1)
				.toLowerCase();
		return load(new FileReader(path), ext);
	}

	public static List<Document> load(final Reader input, final String ext) {
		final BufferedReader reader = new BufferedReader(input);
		final List<Document> result = new ArrayList<Document>();

		if (ext.equals("cgd") || ext.equals("pgr")) {
			final GenericParser parser = new NetParser(reader);
			while (!parser.atEnd()) {
				final GenericParser.Block data = parser.parseDataBlock();
				result.add(new Document(data, "#" + (result.size() + 1)));
			}
		} else if (ext.equals("ds") || ext.equals("tgs")){
			final StringBuffer buffer = new StringBuffer(200);
			String name = null;
			while (true) {
				String line;
				try {
					line = reader.readLine();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				if (line.charAt(0) == '#') {
					if (line.charAt(1) == '@') {
						line = line.substring(2).trim();
						if (line.startsWith("name ")) {
							name = line.substring(5);
						}
					}
				} else {
					int i = line.indexOf('#');
					if (i >= 0) {
						line = line.substring(0, i);
					}
					buffer.append(' ');
					buffer.append(line);
					if (buffer.toString().trim().endsWith(">")) {
						final DSymbol ds = new DSymbol(buffer.toString().trim());
						buffer.delete(0, buffer.length());
						if (name == null) {
							name = "#" + (result.size() + 1);
						}
						result.add(new Document(ds, name));
						name = null;
					}
				}
			}
		} else if (ext.equals("gsl")) {
			try {
				final ObjectInputStream ostream = DocumentXStream.instance()
						.createObjectInputStream(reader);
				while (true) {
					final Document doc = (Document) ostream.readObject();
					if (doc != null) {
						result.add(doc);
					}
				}
			} catch (EOFException ex) {
				; // End of stream reached
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException(ex);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		return result;
	}
	
	public String toXML() {
		//TODO hack!
		return "<object-stream>\n" + DocumentXStream.instance().toXML(this)
				+ "\n</object-stream>\n";
	}
	
	public static void main(final String args[]) {
		final String path = args[0];
		try {
			final List<Document> syms = load(path);
			for (final Document doc: syms) {
            	if (doc.getName() != null) {
            		System.out.println("#@ name " + doc.getName());
            	}
				System.out.println(doc.getSymbol().canonical());
			}
		} catch (final FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}
	
    // --- getters and setters for options
    public int getEmbedderStepLimit() {
        return this.embedderStepLimit;
    }

    public void setEmbedderStepLimit(int embedderStepLimit) {
        if (embedderStepLimit != this.embedderStepLimit) {
            invalidateEmbedding();
            this.embedderStepLimit = embedderStepLimit;
        }
    }

    public int getEqualEdgePriority() {
        return this.equalEdgePriority;
    }

    public void setEqualEdgePriority(int equalEdgePriority) {
        if (equalEdgePriority != this.equalEdgePriority) {
            invalidateEmbedding();
            this.equalEdgePriority = equalEdgePriority;
        }
    }

    public boolean getUseBarycentricPositions() {
        return this.useBarycentricPositions;
    }

    public void setUseBarycentricPositions(boolean useBarycentricPositions) {
        if (useBarycentricPositions != this.useBarycentricPositions) {
            invalidateEmbedding();
            this.useBarycentricPositions = useBarycentricPositions;
        }
    }

    public boolean getUsePrimitiveCell() {
        return this.usePrimitiveCell; 
    }
  
    public void setUsePrimitiveCell(final boolean value) {
        if (value != this.usePrimitiveCell) {
          cache.remove(CENTERING_VECTORS);
          this.usePrimitiveCell = value;
        }
    }
  
    public Properties getProperties() {
    	return (Properties) this.properties.clone();
	}

	public void setProperties(final Properties properties) {
		this.properties.clear();
		this.properties.putAll(properties);
	}

	public Transformation getTransformation() {
		return this.transformation;
	}

	public void setTransformation(final Transformation transformation) {
		this.transformation = transformation;
	}
}

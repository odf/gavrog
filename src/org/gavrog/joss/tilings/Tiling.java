/*
   Copyright 2009 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.tilings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Cache;
import org.gavrog.box.collections.CacheMissException;
import org.gavrog.box.simple.Tag;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.fpgroups.FreeWord;
import org.gavrog.joss.dsyms.basic.DSCover;
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.FundamentalGroup;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.IGraphElement;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * An instance of this class represents a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: Tiling.java,v 1.42 2007/07/27 06:07:15 odf Exp $
 */
public class Tiling {
    // --- the cache keys
    final protected static Object TRANSLATION_GROUP = new Tag();
    final protected static Object TRANSLATION_VECTORS = new Tag();
    final protected static Object EDGE_TRANSLATIONS = new Tag();
    final protected static Object CORNER_SHIFTS = new Tag();
    final protected static Object SKELETON = new Tag();
    final protected static Object DUAL_SKELETON = new Tag();
    final protected static Object BARYCENTRIC_POS_BY_VERTEX = new Tag();
    final protected static Object SPACEGROUP = new Tag();
    final protected static Object TILES = new Tag();
	final protected static Object SYMMETRIES = new Tag();
	final protected static Object COVER_ORIENTATION = new Tag();
    
    // --- cache for this instance
    final protected Cache cache = new Cache();

    // --- the symbol this tiling is based on and its (pseudo-) toroidal cover
    final protected DelaneySymbol ds;
    final protected DSCover cov;

	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol for the tiling.
	 */
	public Tiling(final DelaneySymbol ds) {
		this(ds, null);
	}
	
	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol for the tiling.
	 * @param cover a pre-computed (pseudo-)toroidal cover of the tiling.
	 */
	public Tiling(final DelaneySymbol ds, final DSCover cover) {
        // --- check basic properties
        if (!ds.isComplete()) {
            throw new IllegalArgumentException("symbol must be complete");
        }
        if (!ds.isConnected()) {
            throw new IllegalArgumentException("symbol must be connected");
        }
        
        // --- remember the input symbol
        this.ds = ds;
        
		// --- shortcut
		final int dim = ds.dim();

		// --- compute a torus cover
		DSCover cov;
		if (dim == 2) {
			cov = Covers.toroidalCover2D(ds);
		} else if (dim == 3) {
			cov = Covers.pseudoToroidalCover3D(ds);
		} else {
            final String msg = "symbol must be 2- or 3-dimensional";
			throw new UnsupportedOperationException(msg);
		}

		if (cov == null) {
			throw new IllegalArgumentException("symbol is not euclidean");
		}
		
		if (cover != null) {
			// --- if a cover is given, check if it's legal
			final Object D = ds.elements().next();
			final Object E1 = cov.getCoverMorphism().getASource(D);
			final Object E2 = cover.getCoverMorphism().getASource(D);
			assert new DSMorphism(cov, cover, E1, E2).isIsomorphism();
			new DSMorphism(cover, ds, E2, D);
			this.cov = cover;
		} else {
			// --- otherwise use the computed cover
			this.cov = cov;
		}
	}

    /**
     * @return the original symbol.
     */
    public DelaneySymbol getSymbol() {
        return this.ds;
    }
    
    /**
     * @return the toroidal or pseudo-toroidal cover.
     */
    public DSCover getCover() {
        return this.cov;
    }
    
    /**
     * @return a map assigning orientations to cover chambers.
     */
    public Map getCoverOrientation() {
    	try {
    		return (Map) this.cache.get(COVER_ORIENTATION);
    	} catch (CacheMissException ex) {
    		final DSCover cover = getCover();
    		final Map ori = cover.partialOrientation();
    		return (Map) this.cache.put(COVER_ORIENTATION, ori);
    	}
    }
    
    /**
     * @param D a chamber of the (pseudo-) toroidal cover.
     * @return the orientation of cover chamber D.
     */
    public int coverOrientation(final Object D) {
    	return ((Integer) this.getCoverOrientation().get(D)).intValue();
    }
    
    /**
     * @return the fundamental group of the toroidal or pseudo-toroidal cover.
     */
    public FundamentalGroup getTranslationGroup() {
        try {
            return (FundamentalGroup) this.cache.get(TRANSLATION_GROUP);
        } catch (CacheMissException ex) {
            final FundamentalGroup fg = new FundamentalGroup(getCover());
            return (FundamentalGroup) this.cache.put(TRANSLATION_GROUP, fg);
        }
    }
    
    /**
     * @return the generators of the translation group as vectors.
     */
    private Vector[] getTranslationVectors() {
        try {
            return (Vector[]) this.cache.get(TRANSLATION_VECTORS);
        } catch (CacheMissException ex) {
            final Matrix N = LinearAlgebra.columnNullSpace(
                    getTranslationGroup().getPresentation().relatorMatrix(),
                    true);
            if (N.numberOfColumns() != getCover().dim()) {
                final String msg = "could not compute translations";
                throw new RuntimeException(msg);
            }
            final Vector[] result = Vector.fromMatrix(N);
            return (Vector[]) this.cache.put(TRANSLATION_VECTORS, result);
        }
    }
    
    /**
     * @return a mapping of cover-edges to their associated translations
     */
    public Map getEdgeTranslations() {
        try {
            return (Map) this.cache.get(EDGE_TRANSLATIONS);
        } catch (CacheMissException ex) {
            final int dim = getCover().dim();
            final Vector[] t = getTranslationVectors();
            final Map e2w = (Map) getTranslationGroup().getEdgeToWord();
            final Map<Object, Vector> e2t = new HashMap<Object, Vector>();
            for (Iterator edges = e2w.keySet().iterator(); edges.hasNext();) {
                final Object e = edges.next();
                final FreeWord w = (FreeWord) e2w.get(e);
                Vector s = Vector.zero(dim);
                for (int i = 0; i < w.length(); ++i) {
                    final int k = w.getLetter(i) - 1;
                    final int sign = w.getSign(i);
                    if (sign > 0) {
                        s = (Vector) s.plus(t[k]);
                    } else {
                        s = (Vector) s.minus(t[k]);
                    }
                }
                e2t.put(e, s);
            }
            return (Map) this.cache.put(EDGE_TRANSLATIONS, Collections
                    .unmodifiableMap(e2t));
        }
    }
    
    /**
     * Determines the translation associated to an edge in the toroidal or
     * pseudo-toroidal cover.
     * 
     * @param i the index of the edge.
     * @param D the source element of the edge.
     * @return the translation vector associated to the edge.
     */
    public Vector edgeTranslation(final int i, final Object D) {
        return (Vector) getEdgeTranslations().get(new DSPair(i, D));
    }

    /**
     * @return shifts to obtain chamber corner positions from node positions.
     */
    public Map getCornerShifts() {
        try {
            return (Map) this.cache.get(CORNER_SHIFTS);
        } catch (CacheMissException ex) {
            final int dim = getCover().dim();
            final HashMap<DSPair, Vector> c2s = new HashMap<DSPair, Vector>();
            for (int i = 0; i <= dim; ++i) {
                final List idcs = IndexList.except(getCover(), i);
                final Traversal trav = new Traversal(getCover(), idcs,
                        getCover().elements());
                while (trav.hasNext()) {
                    final DSPair e = (DSPair) trav.next();
                    final int k = e.getIndex();
                    final Object D = e.getElement();
                    if (k < 0) {
                        c2s.put(new DSPair(i, D), Vector.zero(dim));
                    } else {
                        final Object Dk = getCover().op(k, D);
                        final Vector v = (Vector) c2s.get(new DSPair(i, Dk));
                        c2s.put(new DSPair(i, D),
                        		(Vector) v.minus(edgeTranslation(k, Dk)));
                    }
                }

            }
            return (Map) cache.put(CORNER_SHIFTS, Collections
                    .unmodifiableMap(c2s));
        }
    }
    
    /**
     * Returns the necessary shift to obtain the position of a chamber corner
     * from the node position associated to it in the barycentric skeleton.
     * 
     * @param i index of the corner.
     * @param D the chamber the corner belongs to.
     * @return shifts for this corner.
     */
    public Vector cornerShift(final int i, final Object D) {
    	return (Vector) getCornerShifts().get(new DSPair(i, D));
    }
    
    /**
     * Class to represent a skeleton graph for this tiling.
     */
    public class Skeleton extends PeriodicGraph {
        final private Map<INode, Object> node2chamber =
        	new HashMap<INode, Object>();
		final private Map<Object, INode> chamber2node =
			new HashMap<Object, INode>();
        final private Map<IEdge, Object> edge2chamber =
        	new HashMap<IEdge, Object>();
        final private Map<Object, IEdge> chamber2edge =
        	new HashMap<Object, IEdge>();
        final private List nodeIdcs;
        final private List halfEdgeIdcs;
        final private boolean dual;
        
        /**
         * Constructs an instance.
         * @param dual if true, constructs a dual skeleton.
         * @param dimension
         */
        private Skeleton(boolean dual) {
            super(getCover().dim());
            final DelaneySymbol cover = getCover();
            final int d = cover.dim();
            if (dual) {
            	nodeIdcs = IndexList.except(cover, d);
            	halfEdgeIdcs = IndexList.except(cover, d, d-1);
            } else {
            	nodeIdcs = IndexList.except(cover, 0);
            	halfEdgeIdcs = IndexList.except(cover, 0, 1);
            }
            this.dual = dual;
        }
        
        /**
         * Creates a new node associated to a chamber corner.
         * @param D the chamber the corner belongs to.
         * @return the newly created node.
         */
        private INode newNode(final Object D) {
            final DelaneySymbol cover = getCover();
            final INode v = super.newNode();
            this.node2chamber.put(v, D);
            for (final Iterator orb = cover.orbit(nodeIdcs, D); orb.hasNext();) {
                this.chamber2node.put(orb.next(), v);
            }
            return v;
        }

        /**
         * Creates a new edge associated to a chamber ridge.
         * @param v source node.
         * @param w target node.
         * @param s shift vector associated to this edge.
         * @param D chamber the ridge belongs to.
         * @return the newly created edge.
         */
        private IEdge newEdge(final INode v, final INode w, final Vector s,
                final Object D) {
            final DelaneySymbol cover = getCover();
            final IEdge e = super.newEdge(v, w, s, !this.dual);
            this.edge2chamber.put(e, D);
            for (final Iterator orb = cover.orbit(halfEdgeIdcs, D); orb.hasNext();) {
                this.chamber2edge.put(orb.next(), e);
            }
            final IEdge er = e.reverse();
            final Object Dr = dual ? cover.op(cover.dim(), D) : cover.op(0, D);
            this.edge2chamber.put(er, Dr);
            for (final Iterator orb = cover.orbit(halfEdgeIdcs, Dr); orb
                    .hasNext();) {
                this.chamber2edge.put(orb.next(), er);
            }
            return e;
        }
        
        /**
         * Determines the list of symmetries for this tiling.
         * 
         * @return the space group.
         */
        public Set symmetries() {
            try {
                return (Set) this.cache.get(SYMMETRIES);
            } catch (CacheMissException ex) {
                // --- get the toroidal cover of the base symbol
                final DSCover cover = getCover();

                // --- find a chamber with nonzero volume
                Object D0 = null;
                for (final Iterator elms = cover.elements(); elms.hasNext();) {
                    final Object D = elms.next();
                    if (!spanningMatrix(D).determinant().isZero()) {
                        D0 = D;
                        break;
                    }
                }
                if (D0 == null) {
                    throw new RuntimeException("all chambers have zero volume");
                }

                // --- compute affine maps from start chamber to its images
                final Set<Morphism> syms = new HashSet<Morphism>();
                final Object E = cover.image(D0);
                for (final Iterator elms = cover.elements(); elms.hasNext();) {
                    final Object D = elms.next();
                    if (cover.image(D).equals(E)) {
                        syms.add(derivedMorphism(D0, D));
                    }
                }

                // --- construct the group, cache and return it
                final Set result = Collections.unmodifiableSet(syms);
                return (Set) this.cache.put(SYMMETRIES, result);
            }
        }
        
        private Morphism derivedMorphism(final Object D, final Object E) {
            final DSCover cover = getCover();
            final DSMorphism map = new DSMorphism(cover, cover, D, E);
            final Operator op = Operator.fromLinear((Matrix) spanningMatrix(D)
                    .inverse().times(spanningMatrix(E)));
            final Map<IGraphElement, IGraphElement> src2img =
            	new HashMap<IGraphElement, IGraphElement>();
            final Map<IGraphElement, IGraphElement> img2src =
            	new HashMap<IGraphElement, IGraphElement>();
            for (final Iterator elms = cover.elements(); elms.hasNext();) {
                final Object src = elms.next();
                final Object img = map.get(src);
                final INode v = nodeForChamber(src);
                final INode w = nodeForChamber(img);
                src2img.put(v, w);
                img2src.put(w, v);
                // --- important to use oriented edges here
                IEdge e = edgeForChamber(src).oriented();
                IEdge f = edgeForChamber(img).oriented();
                src2img.put(e, f);
                img2src.put(f, e);
            }
            return new Morphism(src2img, img2src, op, true);
        }
        
        /**
         * Retrieves the chamber a node belongs to.
         * 
         * @param v the node.
         * @return a chamber associated to node v.
         */
        public Object chamberAtNode(final INode v) {
            return this.node2chamber.get(v);
        }
        
        /**
         * Retrieves the node associated to a chamber.
         * @param D the chamber.
         * @return the node associated to the chamber D.
         */
        public INode nodeForChamber(final Object D) {
            return (INode) this.chamber2node.get(D);
        }
        
        /**
         * Retrieves a chamber an edge touches.
         * @param e the edge.
         * @return a chamber associated to edge e.
         */
        public Object chamberAtEdge(final IEdge e) {
            return this.edge2chamber.get(e);
        }

		/**
		 * @return true if this is a dual skeleton
		 */
		public boolean isDual() {
			return this.dual;
		}
		
        /**
         * Retrieves the edge associated to a chamber.
         * @param D the chamber.
         * @return the edge associated to the chamber D.
         */
        public IEdge edgeForChamber(final Object D) {
            return (IEdge) this.chamber2edge.get(D);
        }
        
        // --- we override the following to make skeleta immutable from outside
        public void delete(IGraphElement element) {
            throw new UnsupportedOperationException();
        }

        public IEdge newEdge(INode source, INode target, int[] shift) {
            throw new UnsupportedOperationException();
        }

        public IEdge newEdge(INode source, INode target, Vector shift) {
            throw new UnsupportedOperationException();
        }

        public IEdge newEdge(INode source, INode target) {
            throw new UnsupportedOperationException();
        }

        public INode newNode() {
            throw new UnsupportedOperationException();
        }

        public void shiftNode(INode node, Vector amount) {
            throw new UnsupportedOperationException();
        }
    }
    
	/**
	 * @return the skeleton graph of the tiling.
	 */
	public Skeleton getSkeleton() {
        try {
            return (Skeleton) this.cache.get(SKELETON);
        } catch (CacheMissException ex) {
            return (Skeleton) this.cache.put(SKELETON, makeSkeleton(false));
        }
    }

	/**
	 * @return the skeleton graph of the tiling.
	 */
	public Skeleton getDualSkeleton() {
        try {
            return (Skeleton) this.cache.get(DUAL_SKELETON);
        } catch (CacheMissException ex) {
            return (Skeleton) this.cache.put(DUAL_SKELETON, makeSkeleton(true));
        }
    }

	/**
	 * Constructs the skeleton or dual skeleton of the tiling modulo
	 * translations.
	 * 
	 * @param dual if true, the dual skeleton is constructed.
	 * @return the resulting skeleton graph.
	 */
	private Skeleton makeSkeleton(final boolean dual) {
        final DelaneySymbol cover = getCover();
        final Skeleton G = new Skeleton(dual);
        final int d = cover.dim();
        final int idx0 = dual ? d : 0;
        final int idx1 = dual ? d-1 : 1;
        List idcs;

        // --- create nodes of the graph and map Delaney chambers to nodes
        idcs = IndexList.except(cover, idx0);
        for (final Iterator iter = cover.orbitReps(idcs); iter.hasNext();) {
            G.newNode(iter.next());
        }

        // --- create the edges
        idcs = IndexList.except(cover, idx1);
        for (final Iterator iter = cover.orbitReps(idcs); iter.hasNext();) {
            final Object D = iter.next();
            final Object E = cover.op(idx0, D);
            final INode v = G.nodeForChamber(D);
            final INode w = G.nodeForChamber(E);
            final Vector t = edgeTranslation(idx0, D);
            final Vector sD = cornerShift(idx0, D);
            final Vector sE = cornerShift(idx0, E);
            final Vector s = (Vector) t.plus(sE).minus(sD);
            G.newEdge(v, w, s, D);
        }
        
        return G;
	}
	
    /**
     * Computes positions for chamber corners by first placing corners for
     * vertices (index 0) barycentrically, then placing corners for edges etc.
     * int the centers of their bounding vertices.
     * 
     * @return a mapping from corners to positions
     */
    public Map getVertexBarycentricPositions() {
        try {
            return (Map) this.cache.get(BARYCENTRIC_POS_BY_VERTEX);
        } catch (CacheMissException ex) {
            final Map p = cornerPositions(getSkeleton().barycentricPlacement());
            return (Map) cache.put(BARYCENTRIC_POS_BY_VERTEX, p);
        }
    }
    
    /**
     * Computes positions for all chamber corners from skeleton node positions.
     * A corner position is taken as the center of gravity of all nodes incident
     * to the component of the tiling associated to that corner.
     * 
     * @param nodePositions maps skeleton nodes to positions.
     * @return a map containing the positions for all corners.
     */
    public Map cornerPositions(final Map nodePositions) {
        final DelaneySymbol cover = getCover();
        final Skeleton skel = getSkeleton();
        final Map<DSPair, Point> result = new HashMap<DSPair, Point>();

        for (final Iterator elms = cover.elements(); elms.hasNext();) {
            final Object D = elms.next();
            final Point p = (Point) nodePositions.get(skel.nodeForChamber(D));
            final Vector t = cornerShift(0, D);
            result.put(new DSPair(0, D), (Point) p.plus(t));
        }
        final int dim = cover.dim();
        List<Integer> idcs = new LinkedList<Integer>();
        for (int i = 1; i <= dim; ++i) {
            idcs.add(i-1);
            for (final Iterator reps = cover.orbitReps(idcs); reps.hasNext();) {
                final Object D = reps.next();
                Matrix s = Point.origin(dim).getCoordinates();
                int n = 0;
                for (Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                    final Object E = orb.next();
                    final Point p = (Point) result.get(new DSPair(0, E));
                    final Vector t = cornerShift(i, E);
                    final Point pt = (Point) p.minus(t);
                    s = (Matrix) s.plus(pt.getCoordinates());
                    ++n;
                }
                final Point p = new Point((Matrix) s.dividedBy(n));
                for (Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                    final Object E = orb.next();
                    final Vector t = cornerShift(i, E);
                    result.put(new DSPair(i, E), (Point) p.plus(t));
                }
            }
        }
        return result;
    }
    
    /**
	 * Returns the position for a corner as computed by
	 * {@link #getVertexBarycentricPositions()}.
	 * 
	 * @param i the index for the corner.
	 * @param D the chamber which the corner belongs to.
	 * @return the position of the corner.
	 */
    public Point vertexBarycentricPosition(final int i, final Object D) {
    	return (Point) getVertexBarycentricPositions().get(new DSPair(i, D));
    }
    
    /**
     * Determines the space group of this tiling.
     * 
     * @return the space group.
     */
    public SpaceGroup getSpaceGroup() {
		return getSkeleton().getSpaceGroup();
	}
    
    /**
	 * Computes a matrix of chamber edge vectors. The i-th row contains the
	 * vector from the 0-corner to the (i+1)-corner.
	 * 
	 * @param D a chamber.
	 * @return the matrix of edge vectors.
	 */
    private Matrix spanningMatrix(final Object D) {
        final int d = getCover().dim();
        final Point p = vertexBarycentricPosition(0, D);
        final Vector dif[] = new Vector[d];
        for (int i = 0; i < d; ++i) {
            dif[i] = (Vector) vertexBarycentricPosition(i + 1, D).minus(p);
        }
        return Vector.toMatrix(dif);
    }
    
    // --- maps cover chambers to tile numbers, tile kinds and facet numbers
    final private Map<Object, Integer> chamber2tile =
    	new HashMap<Object, Integer>();
    final private Map<Object, Integer> chamber2kind =
    	new HashMap<Object, Integer>();
    final private Map<Object, Integer> chamber2facet =
    	new HashMap<Object, Integer>();

    /**
     * Represents a facet (co-dimension 1 constituent) of this tiling.
     */
    public class Facet {
    	final private int tilingId = Tiling.this.hashCode();
        final private List<Object> chambers;
        final private int tile;
        final private int index;
    	
    	private Facet(final Object D, final int tile, final int index) {
    		final DelaneySymbol cover = getCover();
    		final int d = cover.dim();
            final Object E0 = coverOrientation(D) < 0 ? cover.op(0, D) : D;
            this.chambers = new LinkedList<Object>();
            if (d == 3) {
	            Object E = E0;
	            do {
	                this.chambers.add(E);
	                E = cover.op(1, cover.op(0, E));
	            } while (!E.equals(E0));
            } else if (d == 2) {
            	this.chambers.add(E0);
            	this.chambers.add(cover.op(0, E0));
            } else {
            	throw new UnsupportedOperationException("dimension must be 2 or 3");
            }
            for (final Object E: chambers) {
            	chamber2facet.put(E, index);
            	chamber2facet.put(cover.op(0, E), index);
            }
            this.tile = tile;
            this.index = index;
    	}

    	public int size() {
    		return this.chambers.size();
    	}
    	
        public Object chamber(final int i) {
            return this.chambers.get(i);
        }
        
		public IEdge edge(final int i) {
			return getSkeleton().edgeForChamber(chamber(i));
		}
		
		public Vector edgeShift(final int i) {
			return (Vector) cornerShift(0, chamber(i));
		}
		
		public Object getChamber() {
			return chamber(0);
		}

        public int getIndex() {
            return this.index;
        }

        public int getTileIndex() {
            return this.tile;
        }
        
        public Tile getTile() {
        	return getTiles().get(this.tile);
        }
        
        public Facet opposite() {
        	final DelaneySymbol cov = getCover();
        	final Object E = cov.op(cov.dim(), getChamber());
        	final Tile t = getTiles().get(chamber2tile.get(E));
        	return t.facet(chamber2facet.get(E));
        }
        
        public int hashCode() {
        	return (this.tilingId * 37 + this.tile) * 37 + this.index;
        }
        
        public boolean equals(final Object arg) {
        	final Facet other = (Facet) arg;
        	return other.tilingId == this.tilingId
        		&& other.tile == this.tile && other.index == this.index;
        }
    }
    
    /**
     * Represents a tile (top-dimensional constituent) of this tiling.
     */
    public class Tile {
    	final private int tilingId = Tiling.this.hashCode();
        final private int index;
        final private int kind;
        final private Facet facets[];
        final private int neighbors[];
        final private Vector neighborShifts[];

        private Tile(final INode v) {
        	final int d = getSymbol().dim();
        	final DSCover cover = getCover();
        	final Skeleton skel = getDualSkeleton();
            final Object D = skel.chamberAtNode(v);
        	final Integer k = chamber2tile.get(D);
            this.index = k.intValue();
            this.kind = chamber2kind.get(cover.image(D));

            final int deg = v.degree();
        	this.facets = new Facet[deg];
        	this.neighbors = new int[deg];
        	this.neighborShifts = new Vector[deg];
        	
            int i = 0;
            for (final Iterator conn = v.incidences(); conn.hasNext();) {
                final IEdge e = (IEdge) conn.next();
                Object Df = skel.chamberAtEdge(e);
                if (!chamber2tile.get(Df).equals(k)) {
                    Df = cover.op(d, Df);
                }
                final Vector t = edgeTranslation(d, Df);
                this.facets[i] = new Facet(Df, this.index, i);
                final Object Dn = skel.chamberAtNode(e.target());
                this.neighbors[i] = chamber2tile.get(Dn);
                this.neighborShifts[i] = t;
                ++i;
                if (e.source().equals(e.target())) {
                	Df = cover.op(d, Df);
                    this.facets[i] = new Facet(Df, this.index, i);
                    this.neighbors[i] = this.index;
                    this.neighborShifts[i] = (Vector) t.negative();
                    ++i;
                }
            }
        }
        
        public Object getChamber() {
            return facet(0).chamber(0);
        }

        public int getIndex() {
            return this.index;
        }

        public int getKind() {
            return this.kind;
        }

        public int size() {
            return this.facets.length;
        }
        
        public Facet facet(final int i) {
            return this.facets[i];
        }
        
        public Tile neighbor(final int i) {
            return getTiles().get(this.neighbors[i]);
        }
        
        public Vector neighborShift(final int i) {
            return this.neighborShifts[i];
        }
        
        public int hashCode() {
        	return this.tilingId * 37 + this.index;
        }
        
        public boolean equals(final Object arg) {
        	final Tile other = (Tile) arg;
        	return other.tilingId == this.tilingId && other.index == this.index;
        }
    }

    /**
     * @return the list of tiles for this tiling.
     */
    @SuppressWarnings("unchecked")
	public List<Tile> getTiles() {
        try {
            return (List<Tile>) this.cache.get(TILES);
        } catch (CacheMissException ex) {
        }
        
        final DelaneySymbol cover = getCover();
        final DelaneySymbol image = getSymbol();
        final List idcs = IndexList.except(cover, image.dim());
        
        // --- map image chambers to tile kinds
        int m = 0;
        for (final Iterator elms = image.elements(); elms.hasNext();) {
            final Object D = elms.next();
            if (!chamber2kind.containsKey(D)) {
                final Integer mm = new Integer(m++);
                for (final Iterator orb = image.orbit(idcs, D); orb.hasNext();) {
                    chamber2kind.put(orb.next(), mm);
                }
            }
        }
        
        // --- map chambers to tile indices and vice versa
        int n = 0;
        for (final Iterator elms = cover.elements(); elms.hasNext();) {
            final Object D = elms.next();
            if (!chamber2tile.containsKey(D)) {
                final Integer nn = new Integer(n++);
                for (final Iterator orb = cover.orbit(idcs, D); orb.hasNext();) {
                    chamber2tile.put(orb.next(), nn);
                }
            }
        }
        
        // --- construct the list of tiles with associated data
        final List<Tile> tiles = new ArrayList<Tile>();
        for (Iterator nodes = getDualSkeleton().nodes(); nodes.hasNext();) {
            tiles.add(new Tile((INode) nodes.next()));
        }
        
        // --- cache and return
        return (List<Tile>) this.cache.put(TILES, tiles);
    }
}

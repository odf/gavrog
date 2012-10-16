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

package org.gavrog.joss.tilings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.gavrog.joss.dsyms.basic.DSMorphism;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Traversal;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.derived.DSCover;
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
 */
public class Tiling {
    // --- the cache keys
    final protected static Tag TRANSLATION_GROUP = new Tag();
    final protected static Tag TRANSLATION_VECTORS = new Tag();
    final protected static Tag EDGE_TRANSLATIONS = new Tag();
    final protected static Tag CORNER_SHIFTS = new Tag();
    final protected static Tag SKELETON = new Tag();
    final protected static Tag DUAL_SKELETON = new Tag();
    final protected static Tag BARYCENTRIC_POS_BY_VERTEX = new Tag();
    final protected static Tag SPACEGROUP = new Tag();
    final protected static Tag TILES = new Tag();
	final protected static Tag SYMMETRIES = new Tag();
	final protected static Tag COVER_ORIENTATION = new Tag();
    
    // --- cache for this instance
    final protected Cache<Tag, Object> cache = new Cache<Tag, Object>();

    // --- the symbol this tiling is based on and its (pseudo-) toroidal cover
    final protected DelaneySymbol<Integer> ds;
    final protected DSCover<Integer> cov;

	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol for the tiling.
	 */
	public Tiling(final DelaneySymbol<Integer> ds) {
		this(ds, null);
	}
	
	/**
	 * Constructs an instance.
	 * 
	 * @param ds the Delaney symbol for the tiling.
	 * @param cover a pre-computed (pseudo-)toroidal cover of the tiling.
	 */
	public Tiling(
	        final DelaneySymbol<Integer> ds,
	        final DSCover<Integer> cover)
	{
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
		DSCover<Integer> cov;
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
			final int D = ds.elements().next();
			final int E1 = cov.getCoverMorphism().getASource(D);
			final int E2 = cover.getCoverMorphism().getASource(D);
			assert new DSMorphism<Integer, Integer>(cov, cover, E1, E2)
			    .isIsomorphism();
			new DSMorphism<Integer, Integer>(cover, ds, E2, D);
			this.cov = cover;
		} else {
			// --- otherwise use the computed cover
			this.cov = cov;
		}
	}

    /**
     * @return the original symbol.
     */
    public DelaneySymbol<Integer> getSymbol() {
        return this.ds;
    }
    
    /**
     * @return the toroidal or pseudo-toroidal cover.
     */
    public DSCover<Integer> getCover() {
        return this.cov;
    }
    
    /**
     * @return a map assigning orientations to cover chambers.
     */
    public Map<Integer, Integer> getCoverOrientation() {
        try {
    	    @SuppressWarnings("unchecked")
            final Map<Integer, Integer> result =
    	            (Map<Integer, Integer>) this.cache.get(COVER_ORIENTATION);
    	    return result;
    	} catch (CacheMissException ex) {
    		final Map<Integer, Integer> ori = getCover().partialOrientation();
    		this.cache.put(COVER_ORIENTATION, ori);
    		return ori;
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
    public FundamentalGroup<Integer> getTranslationGroup() {
        try {
            @SuppressWarnings("unchecked")
            final FundamentalGroup<Integer> result =
                (FundamentalGroup<Integer>)
                    this.cache.get(TRANSLATION_GROUP);
            return result;
        } catch (CacheMissException ex) {
            final FundamentalGroup<Integer> fg =
                    new FundamentalGroup<Integer>(getCover());
            this.cache.put(TRANSLATION_GROUP, fg);
            return fg;
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
            this.cache.put(TRANSLATION_VECTORS, result);
            return result;
        }
    }
    
    /**
     * @return a mapping of cover-edges to their associated translations
     */
    public Map<DSPair<Integer>, Vector> getEdgeTranslations() {
        try {
            @SuppressWarnings("unchecked")
            final Map<DSPair<Integer>, Vector> result =
                    (Map<DSPair<Integer>, Vector>)
                        this.cache.get(EDGE_TRANSLATIONS);
            return result;
        } catch (CacheMissException ex) {
            final int dim = getCover().dim();
            final Vector[] t = getTranslationVectors();
            final Map<DSPair<Integer>, FreeWord<String>> e2w =
                    getTranslationGroup().getEdgeToWord();
            final Map<DSPair<Integer>, Vector> e2t =
                    new HashMap<DSPair<Integer>, Vector>();
            for (final DSPair<Integer> e: e2w.keySet()) {
                final FreeWord<String> w = e2w.get(e);
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
            final Map<DSPair<Integer>, Vector> result =
                    Collections.unmodifiableMap(e2t);
            this.cache.put(EDGE_TRANSLATIONS, result);
            return result;
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
    public Vector edgeTranslation(final int i, final int D) {
        return getEdgeTranslations().get(new DSPair<Integer>(i, D));
    }

    /**
     * @return shifts to obtain chamber corner positions from node positions.
     */
    public Map<DSPair<Integer>, Vector> getCornerShifts() {
        try {
            @SuppressWarnings("unchecked")
            final Map<DSPair<Integer>, Vector> result =
                    (Map<DSPair<Integer>, Vector>)
                        this.cache.get(CORNER_SHIFTS);
            return result;
        } catch (CacheMissException ex) {
            final int dim = getCover().dim();
            final HashMap<DSPair<Integer>, Vector> c2s =
                    new HashMap<DSPair<Integer>, Vector>();
            for (int i = 0; i <= dim; ++i) {
                final IndexList idcs = IndexList.except(getCover(), i);
                for (final DSPair<Integer> e: 
                	new Traversal<Integer>(getCover(), idcs,
                			getCover().elements()))
                {
                    final int k = e.getIndex();
                    final int D = e.getElement();
                    if (k < 0) {
                        c2s.put(new DSPair<Integer>(i, D), Vector.zero(dim));
                    } else {
                        final int Dk = getCover().op(k, D);
                        final Vector v = c2s.get(new DSPair<Integer>(i, Dk));
                        c2s.put(new DSPair<Integer>(i, D),
                        		(Vector) v.minus(edgeTranslation(k, Dk)));
                    }
                }

            }
            final Map<DSPair<Integer>, Vector> result =
                    Collections.unmodifiableMap(c2s);
            cache.put(CORNER_SHIFTS, result);
            return result;
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
    public Vector cornerShift(final int i, final int D) {
    	return getCornerShifts().get(new DSPair<Integer>(i, D));
    }
    
    /**
     * Class to represent a skeleton graph for this tiling.
     */
    public class Skeleton extends PeriodicGraph {
        final private Map<INode, Integer> node2chamber =
        	new HashMap<INode, Integer>();
		final private Map<Integer, INode> chamber2node =
			new HashMap<Integer, INode>();
        final private Map<IEdge, Integer> edge2chamber =
        	new HashMap<IEdge, Integer>();
        final private Map<Integer, IEdge> chamber2edge =
        	new HashMap<Integer, IEdge>();
        final private IndexList nodeIdcs;
        final private IndexList halfEdgeIdcs;
        final private boolean dual;
        
        /**
         * Constructs an instance.
         * @param dual if true, constructs a dual skeleton.
         * @param dimension
         */
        private Skeleton(boolean dual) {
            super(getCover().dim());
            final DelaneySymbol<Integer> cover = getCover();
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
        private INode newNode(final int D) {
            final DelaneySymbol<Integer> cover = getCover();
            final INode v = super.newNode();
            this.node2chamber.put(v, D);
            for (final int E: cover.orbit(nodeIdcs, D)) {
                this.chamber2node.put(E, v);
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
        private IEdge newEdge(
                final INode v,
                final INode w,
                final Vector s,
                final int D)
        {
            final DSymbol cover = getCover();
            final IEdge e = super.newEdge(v, w, s, !this.dual);
            this.edge2chamber.put(e, D);
            for (final int E: cover.orbit(halfEdgeIdcs, D)) {
                this.chamber2edge.put(E, e);
            }
            final IEdge er = e.reverse();
            final int Dr = dual ? cover.op(cover.dim(), D) : cover.op(0, D);
            this.edge2chamber.put(er, Dr);
            for (final int E: cover.orbit(halfEdgeIdcs, Dr)) {
                this.chamber2edge.put(E, er);
            }
            return e;
        }
        
        /**
         * Determines the list of symmetries for this tiling.
         * 
         * @return the space group.
         */
        public Set<Morphism> symmetries() {
            try {
                @SuppressWarnings("unchecked")
                final Set<Morphism> result =
                        (Set<Morphism>) this.cache.get(SYMMETRIES);
                return result;
            } catch (CacheMissException ex) {
                // --- get the toroidal cover of the base symbol
                final DSCover<Integer> cover = getCover();

                // --- find a chamber with nonzero volume
                int D0 = 0;
                for (final int D: cover.elements()) {
                    if (!spanningMatrix(D).determinant().isZero()) {
                        D0 = D;
                        break;
                    }
                }
                if (D0 == 0) {
                    throw new RuntimeException("all chambers have zero volume");
                }

                // --- compute affine maps from start chamber to its images
                final Set<Morphism> syms = new HashSet<Morphism>();
                final int E = cover.image(D0);
                for (final int D: cover.elements()) {
                    if (cover.image(D) == E) {
                        syms.add(derivedMorphism(D0, D));
                    }
                }

                // --- construct the group, cache and return it
                final Set<Morphism> result = Collections.unmodifiableSet(syms);
                this.cache.put(SYMMETRIES, result);
                return result;
            }
        }
        
        private Morphism derivedMorphism(final int D, final int E) {
            final DSCover<Integer> cover = getCover();
            final DSMorphism<Integer, Integer> map =
                    new DSMorphism<Integer, Integer>(cover, cover, D, E);
            final Operator op = Operator.fromLinear((Matrix) spanningMatrix(D)
                    .inverse().times(spanningMatrix(E)));
            final Map<IGraphElement, IGraphElement> src2img =
            	new HashMap<IGraphElement, IGraphElement>();
            final Map<IGraphElement, IGraphElement> img2src =
            	new HashMap<IGraphElement, IGraphElement>();
            for (final int src: cover.elements()) {
                final int img = map.get(src);
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
        public int chamberAtNode(final INode v) {
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
        public int chamberAtEdge(final IEdge e) {
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
        final DSymbol cover = getCover();
        final Skeleton G = new Skeleton(dual);
        final int d = cover.dim();
        final int idx0 = dual ? d : 0;
        final int idx1 = dual ? d-1 : 1;
        IndexList idcs;

        // --- create nodes of the graph and map Delaney chambers to nodes
        idcs = IndexList.except(cover, idx0);
        for (final int D: cover.orbitReps(idcs)) {
            G.newNode(D);
        }

        // --- create the edges
        idcs = IndexList.except(cover, idx1);
        for (final int D: cover.orbitReps(idcs)) {
            final int E = cover.op(idx0, D);
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
    public Map<DSPair<Integer>, Point> getVertexBarycentricPositions() {
        try {
            @SuppressWarnings("unchecked")
            final Map<DSPair<Integer>, Point> result =
                    (Map<DSPair<Integer>, Point>)
                        this.cache.get(BARYCENTRIC_POS_BY_VERTEX);
            return result;
        } catch (CacheMissException ex) {
            final Map<DSPair<Integer>, Point> p =
                    cornerPositions(getSkeleton().barycentricPlacement());
            cache.put(BARYCENTRIC_POS_BY_VERTEX, p);
            return p;
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
    public Map<DSPair<Integer>, Point> cornerPositions(
            final Map<INode, Point> nodePositions)
    {
        final DelaneySymbol<Integer> cover = getCover();
        final Skeleton skel = getSkeleton();
        final Map<DSPair<Integer>, Point> result =
                new HashMap<DSPair<Integer>, Point>();

        for (final int D: cover.elements()) {
            final Point p = nodePositions.get(skel.nodeForChamber(D));
            final Vector t = cornerShift(0, D);
            result.put(new DSPair<Integer>(0, D), (Point) p.plus(t));
        }
        final int dim = cover.dim();
        List<Integer> idcs = new LinkedList<Integer>();
        for (int i = 1; i <= dim; ++i) {
            idcs.add(i-1);
            for (final int D: cover.orbitReps(idcs)) {
                Matrix s = Point.origin(dim).getCoordinates();
                int n = 0;
                for (final int E: cover.orbit(idcs, D)) {
                    final Point p = result.get(new DSPair<Integer>(0, E));
                    final Vector t = cornerShift(i, E);
                    final Point pt = (Point) p.minus(t);
                    s = (Matrix) s.plus(pt.getCoordinates());
                    ++n;
                }
                final Point p = new Point((Matrix) s.dividedBy(n));
                for (final int E: cover.orbit(idcs, D)) {
                    final Vector t = cornerShift(i, E);
                    result.put(new DSPair<Integer>(i, E), (Point) p.plus(t));
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
    public Point vertexBarycentricPosition(final int i, final int D) {
    	return getVertexBarycentricPositions().get(new DSPair<Integer>(i, D));
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
    private Matrix spanningMatrix(final int D) {
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
        final private List<Integer> chambers;
        final private int tile;
        final private int index;
    	
    	private Facet(final int D, final int tile, final int index) {
    		final DSymbol cover = getCover();
    		final int d = cover.dim();
            final int E0 = coverOrientation(D) < 0 ? cover.op(0, D) : D;
            this.chambers = new LinkedList<Integer>();
            if (d == 3) {
	            int E = E0;
	            do {
	                this.chambers.add(E);
	                E = cover.op(1, cover.op(0, E));
	            } while (E != E0);
            } else if (d == 2) {
            	this.chambers.add(E0);
            	this.chambers.add(cover.op(0, E0));
            } else {
            	throw new UnsupportedOperationException(
            	        "dimension must be 2 or 3");
            }
            for (final int E: chambers) {
            	chamber2facet.put(E, index);
            	chamber2facet.put(cover.op(0, E), index);
            }
            this.tile = tile;
            this.index = index;
    	}

    	public int size() {
    		return this.chambers.size();
    	}
    	
        public int chamber(final int i) {
            return this.chambers.get(i);
        }
        
		public IEdge edge(final int i) {
			return getSkeleton().edgeForChamber(chamber(i));
		}
		
		public Vector edgeShift(final int i) {
			return (Vector) cornerShift(0, chamber(i));
		}
		
		public int getChamber() {
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
        	final DelaneySymbol<Integer> cov = getCover();
        	final int E = cov.op(cov.dim(), getChamber());
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
        final private List<Facet> facets;
        final private int neighbors[];
        final private Vector neighborShifts[];

        private Tile(final INode v) {
        	final int d = getSymbol().dim();
        	final DSCover<Integer> cover = getCover();
        	final Skeleton skel = getDualSkeleton();
            final int D = skel.chamberAtNode(v);
            this.index = chamber2tile.get(D);
            this.kind = chamber2kind.get(cover.image(D));

            final int deg = v.degree();
        	this.facets = new ArrayList<Facet>();
        	this.neighbors = new int[deg];
        	this.neighborShifts = new Vector[deg];
        	
            int i = 0;
            for (final IEdge e: v.incidences()) {
                int Df = skel.chamberAtEdge(e);
                if (chamber2tile.get(Df) != this.index) {
                    Df = cover.op(d, Df);
                }
                final Vector t = edgeTranslation(d, Df);
                this.facets.add(new Facet(Df, this.index, i));
                final int Dn = skel.chamberAtNode(e.target());
                this.neighbors[i] = chamber2tile.get(Dn);
                this.neighborShifts[i] = t;
                ++i;
                if (e.source().equals(e.target())) {
                	Df = cover.op(d, Df);
                    this.facets.add(new Facet(Df, this.index, i));
                    this.neighbors[i] = this.index;
                    this.neighborShifts[i] = (Vector) t.negative();
                    ++i;
                }
            }
        }
        
        public int getChamber() {
            return facet(0).chamber(0);
        }

        public int getIndex() {
            return this.index;
        }

        public int getKind() {
            return this.kind;
        }

        public int size() {
            return this.facets.size();
        }
        
        public Facet facet(final int i) {
            return this.facets.get(i);
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
        
        final DelaneySymbol<Integer> cover = getCover();
        final DelaneySymbol<Integer> image = getSymbol();
        final IndexList idcs = IndexList.except(cover, image.dim());
        
        // --- map image chambers to tile kinds
        int m = 0;
        for (final int D: image.elements()) {
            if (!chamber2kind.containsKey(D)) {
                for (final int E: image.orbit(idcs, D)) {
                    chamber2kind.put(E, m);
                }
                ++m;
            }
        }
        
        // --- map chambers to tile indices and vice versa
        int n = 0;
        for (final int D: cover.elements()) {
            if (!chamber2tile.containsKey(D)) {
                for (final int E: cover.orbit(idcs, D)) {
                    chamber2tile.put(E, n);
                }
                ++n;
            }
        }
        
        // --- construct the list of tiles with associated data
        final List<Tile> tiles = new ArrayList<Tile>();
        for (final INode v: getDualSkeleton().nodes()) {
            tiles.add(new Tile(v));
        }
        
        // --- cache and return
        return (List<Tile>) this.cache.put(TILES, tiles);
    }
}

/*
   Copyright 2013 Olaf Delgado-Friedrichs

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.DSCover;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.io.GenericParser;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.NetParser.Face;

/**
 * Implements a periodic face set meant to define a tiling.
 * 
 * TODO Remove ? and Object as type parameters.
 */
public class FaceList {
	final private static boolean DEBUG = false;
	
	/**
	 * Hashable class for edges in the tiling to be constructed.
	 */
	private static class Edge implements Comparable<Edge> {
		final public int source;
		final public int target;
		final public Vector shift;
		
		public Edge(final int source, final int target, final Vector shift) {
			if (source < target || (source == target && shift.isNonNegative())) {
				this.source = source;
				this.target = target;
				this.shift = shift;
			} else {
				this.source = target;
				this.target = source;
				this.shift = (Vector) shift.negative();
			}
		}
		
		public int hashCode() {
			return ((source * 37) + target * 127) + shift.hashCode();
		}
		
		public boolean equals(final Object other) {
			final Edge e = (Edge) other;
			return source == e.source && target == e.target
					&& shift.equals(e.shift);
		}
		
        public int compareTo(final Edge other) {
        	if (this.source != other.source) {
        		return this.source - other.source;
        	}
        	if (this.target != other.target) {
        		return this.target - other.target;
        	}
        	return this.shift.compareTo(other.shift);
        }
        
		public String toString() {
			return "(" + source + "," + target + "," + shift + ")";
		}
	}
	
	private static class Incidence implements Comparable<Incidence> {
		final public int faceIndex;
		final public int edgeIndex;
		final public boolean reverse;
        final public double angle;
		
		public Incidence(
				final int faceIndex, final int edgeIndex, final boolean rev,
				final double angle) {
			this.faceIndex = faceIndex;
			this.edgeIndex = edgeIndex;
			this.reverse = rev;
            this.angle = angle;
		}
        
        public Incidence(
				final int faceIndex, final int edgeIndex, final boolean rev) {
            this(faceIndex, edgeIndex, rev, 0.0);
        }
        
        public Incidence(final Incidence source, final double angle) {
            this(source.faceIndex, source.edgeIndex, source.reverse, angle);
        }

		public int compareTo(final Incidence other) {
			if (this.angle < other.angle) {
				return -1;
			} else if (this.angle > other.angle) {
				return 1;
			}
			if (this.faceIndex != other.faceIndex) {
				return this.faceIndex - other.faceIndex;
			}
			if (this.edgeIndex != other.edgeIndex) {
				return this.edgeIndex - other.edgeIndex;
			}
			if (!this.reverse && other.reverse) {
				return -1;
			} else if (this.reverse && !other.reverse) {
				return 1;
			} else {
				return 0;
			}
		}
        
        public String toString() {
        	return "(" + faceIndex + "," + edgeIndex + "," + reverse + ","
					+ angle + ")";
        }
	}
	
	private static class Thing {
	    final public int D;
	    final public Vector inputShift;
	    final public Vector tilingShift;
	    
	    public Thing(final int D,
	                 final Vector inputShift,
	                 final Vector tilingShift)
	    {
	        this.D = D;
	        this.inputShift = inputShift;
	        this.tilingShift = tilingShift;
	    }
	    
	    public Thing(final int D) {
	        this(D, Vector.zero(3), Vector.zero(3));
        }

        public int hashCode()
	    {
	        return (D * 37 + inputShift.hashCode()) * 37
	                + tilingShift.hashCode();
	    }
	}
	
    final private List<Face> faces;
    final private List<List<Pair<Face, Vector>>> tiles;
    final private Map<Face, List<Pair<Integer, Vector>>> tilesAtFace;
    final private Map<Integer, Point> indexToPos;
    final private int dim;
    final private DSymbol ds;
    final private DSCover<Integer> cover;
    final private Map<Integer, Point> positions;
    
	public FaceList(
			final List<Object> input,
			final Map<Integer, Point> indexToPosition)
	{
		if (DEBUG) {
			System.err.println("\nStarting FaceList constructor");
		}
        if (input == null || input.size() < 1) {
            throw new IllegalArgumentException("no data given");
        }
        
        if (input.get(0) instanceof List) {
            this.tiles = new ArrayList<List<Pair<Face, Vector>>>();
            this.tilesAtFace = new HashMap<Face, List<Pair<Integer, Vector>>>();
            for (int i = 0; i < input.size(); ++i) {
                @SuppressWarnings("unchecked")
				final List<Pair<Face, Vector>> tile =
                		(List<Pair<Face, Vector>>) input.get(i);
                final List<Pair<Face, Vector>> newTile =
                		new ArrayList<Pair<Face, Vector>>();
                for (final Pair<Face, Vector> entry: tile) {
                    final Face face = entry.getFirst();
                    final Pair<Face, Vector> normal =
                    		NetParser.normalizedFace(face);
                    final Vector shift =
                    		(Vector) entry.getSecond().plus(normal.getSecond());
                    if (!this.tilesAtFace.containsKey(face)) {
                        this.tilesAtFace.put(face,
                        		new ArrayList<Pair<Integer, Vector>>());
                    }
                    this.tilesAtFace.get(face).add(
                    		new Pair<Integer, Vector>(i, shift));
                    newTile.add(new Pair<Face, Vector>(face, shift));
                }
                this.tiles.add(newTile);
            }
            // --- make sure each face is in exactly two tiles
            for (final List<Pair<Integer, Vector>> tlist:
            	this.tilesAtFace.values())
            {
            	final int n = tlist.size();
            	if (n != 2) {
            		throw new IllegalArgumentException("Face incident to " + n
							+ " tile" + (n == 1 ? "" : "s") + ".");
            	}
            }
            
            this.faces = new ArrayList<Face>();
            this.faces.addAll(this.tilesAtFace.keySet());
        } else {
            this.tiles = null;
            this.tilesAtFace = null;
            this.faces = new ArrayList<Face>();
            for (final Object x: input)
            	this.faces.add((Face) x);
        }
        
        final Face f0 = this.faces.get(0);
        if (f0.size() < 3) {
            throw new IllegalArgumentException("minimal face-size is 3");
        }
        this.dim = f0.shift(0).getDimension();
        if (this.dim != 3) {
            throw new UnsupportedOperationException("dimension must be 3");
        }
        
        this.indexToPos = indexToPosition;
        
        // --- initialize the intermediate symbol
        final Map<Face, List<Integer>> faceElements =
        		new HashMap<Face, List<Integer>>();
        final DynamicDSymbol ds = new DynamicDSymbol(this.dim);
        for (final Face f: this.faces) {
            final int n = f.size();
            final int _2n = 2 * n;
            final List<Integer> elms = ds.grow(4 * n);
            faceElements.put(f, elms);
            for (int i = 0; i < 4 * n; i += 2) {
                ds.redefineOp(0, elms.get(i), elms.get(i + 1));
            }
            for (int i = 1; i < _2n; i += 2) {
                final int i1 = (i + 1) % _2n;
                ds.redefineOp(1, elms.get(i), elms.get(i1));
                ds.redefineOp(1, elms.get(i + _2n), elms.get(i1 + _2n));
            }
            for (int i = 0; i < _2n; ++i) {
                ds.redefineOp(3, elms.get(i), elms.get(i + _2n));
            }
        }
        
        if (DEBUG) {
        	System.err.println("Symbol without 2-ops: " + new DSymbol(ds));
        }
        
        if (this.tiles == null) {
            set2opPlainMode(ds, faceElements);
        } else {
        	set2opTileMode(ds, faceElements);
        }
        
        if (DEBUG) {
        	System.err.println("Symbol with 2-ops: " + new DSymbol(ds));
        }
        
        // --- set all v values to 1
        for (int i = 0; i < dim; ++i) {
        	for (final int D: ds.elements()) {
        		if (!ds.definesV(i, i + 1, D)) {
					ds.redefineV(i, i + 1, D, 1);
				}
        	}
        }
        
        if (DEBUG) {
        	System.err.println("Completed symbol: " + new DSymbol(ds));
        }
        
        // --- do some checks
        assertCompleteness(ds);
        if (!ds.isConnected()) {
        	throw new RuntimeException("Built non-connected symbol.");
        } else if (!ds.isLocallyEuclidean3D()) {
        	throw new RuntimeException("Built non-manifold symbol.");
        } else if (!ds.isLocallyEuclidean3D()) {
        	throw new RuntimeException("Built non-manifold symbol.");
        }
        
        // --- freeze the symbol, make a tiling object, and extract the cover
        this.ds = new DSymbol(ds);
        final Tiling tiling = new Tiling(this.ds);
        this.cover = tiling.getCover();
        
        // --- map skeleton nodes for the tiling to appropriate positions
        final Tiling.Skeleton skel = tiling.getSkeleton();
        this.positions = new HashMap<Integer, Point>();
        final Matrix M = inputToTiling(faces, tiling, faceElements);

        for (final Face f: this.faces)
        {
            final int n = f.size();
            final List<Integer> chambers = faceElements.get(f);
            for (int i = 0; i < 4 * n; ++i)
            {
                final int k = (i % (2 * n) + 1) / 2 % n;
                final int D = chambers.get(i);
                assert(this.cover.image(D) == D);

                final INode v = skel.nodeForChamber(D);
                if (D == skel.chamberAtNode(v))
                {
                    final Point p0 = (Point) indexToPosition.get(f.vertex(k))
                            .plus(f.shift(k));
                    final Point p = (Point) M.times(p0);

                    final Vector t1 = tiling.cornerShift(0, D);
                    final Vector t2 = (i >= 2 * n) ?
                            tiling.edgeTranslation(3, D) : Vector.zero(3);
                    this.positions.put(
                            D, (Point) p.minus(t1).minus(t2));

                    System.err.println("D = " + D + ", " +
                                       "p = " + p + ", " +
                                       "t1 = " + t1 + ", " +
                                       "t2 = " + t2);
                }
            }
        }
	}
	
    private Matrix inputToTiling(
            final List<Face> faces,
            final Tiling tiling,
            final Map<Face, List<Integer>> faceElements) 
    {
        final Map<Integer, Vector> vertShifts = new HashMap<Integer, Vector>();
        final Map<Integer, Vector> edgeShifts = new HashMap<Integer, Vector>();

        for (final Face f: faces)
        {
            final int n = f.size();
            final List<Integer> chambers = faceElements.get(f);

            for (int i = 0; i < 2 * n; i += 2)
            {
                final int k = i / 2;
                final Vector t = (Vector) f.shift((k + 1) %n).minus(f.shift(k));
                final Vector minusT = (Vector) t.times(-1);
                
                edgeShifts.put(chambers.get(i), t);
                edgeShifts.put(chambers.get(i + 1), minusT);
                edgeShifts.put(chambers.get(2 * n + i), t);
                edgeShifts.put(chambers.get(2 * n + i + 1), minusT);
                
                vertShifts.put(chambers.get(i), f.shift(k));
                vertShifts.put(chambers.get((i + 2 * n - 1) % (2 * n)),
                        f.shift(k));
                vertShifts.put(chambers.get(i + 2 * n), f.shift(k));
                vertShifts.put(chambers.get((i + 2 * n - 1) % (2 * n) + 2 * n),
                        f.shift(k));
            }
        }

        final DSCover<Integer> cover = tiling.getCover();
        final Map<Integer, Integer> ori = cover.partialOrientation();
        
        final IndexList idcsF = new IndexList(0, 1, 3);
        final int D0 = cover.elements().next();
        final Set<Integer> orbD0 =
                new HashSet<Integer>(Iterators.asList(cover.orbit(idcsF, D0)));
        
        final LinkedList<Thing> queue = new LinkedList<Thing>();
        final Set<Integer> seen = new HashSet<Integer>();
        final List<Pair<Vector, Vector>> correspondences =
                new ArrayList<Pair<Vector,Vector>>();
        
        queue.addLast(new Thing(D0));
        for (final int x: cover.orbit(idcsF, D0))
            seen.add(x);
        
        while (!queue.isEmpty())
        {
            final Thing entry = queue.removeFirst();
            final int D = entry.D;
            final Vector s1 = entry.inputShift;
            final Vector s2 = entry.tilingShift;

            for (final int E: cover.orbit(idcsF, D))
            {
                if (ori.get(E) < 0)
                    continue;
                
                final int E2 = cover.op(2, E);
                
                final Vector a = (Vector) edgeShifts.get(E)
                    .plus(vertShifts.get(E))
                    .minus(vertShifts.get(D));
                final Vector t1 = (Vector) s1.plus(a);
                
                final Vector t2 = (Vector) s2
                        .minus(tiling.edgeTranslation(2, E))
                        .plus(tiling.cornerShift(2, E))
                        .minus(tiling.cornerShift(2, D));

                if (!seen.contains(E2))
                {
                    queue.addLast(new Thing(E2, t1, t2));
                    for (final int x: cover.orbit(idcsF, E2))
                        seen.add(x);
                }
                else if (orbD0.contains(E2))
                {
                    final Vector d = (Vector) t2
                            .plus(tiling.cornerShift(2, D0))
                            .minus(tiling.cornerShift(2, E2));
                    correspondences.add(new Pair<Vector, Vector>(t1, d));
                }
            }
        }
        System.err.println("correspondences:");
        for (final Pair<Vector, Vector> p: correspondences)
            System.err.println("  " + p);

        final int m = correspondences.size();
        for (int i = 0; i < m - 2; ++i)
        {
            final Vector a = correspondences.get(i).getFirst();
            for (int j = i + 1; j < m - 1; ++j)
            {
                final Vector b = correspondences.get(j).getFirst();
                for (int k = j + 1; k < m; ++k)
                {
                    final Vector c = correspondences.get(k).getFirst();
                    if (!Vector.volume3D(a, b, c).isZero())
                    {
                        final List<Vector> vs = new ArrayList<Vector>();
                        vs.add(a); vs.add(b); vs.add(c);
                        final Matrix A = Vector.toMatrix(vs);

                        final List<Vector> ws = new ArrayList<Vector>();
                        ws.add(correspondences.get(i).getSecond());
                        ws.add(correspondences.get(j).getSecond());
                        ws.add(correspondences.get(k).getSecond());
                        final Matrix B = Vector.toMatrix(ws);
                        
                        return ((Matrix) A.inverse().times(B)).transposed();
                    }
                }
            }
        }
        return null;
    }

    private FaceList(final Pair<List<Object>, Map<Integer, Point>> p) {
        this(p.getFirst(), p.getSecond());
    }
    
    public FaceList(final GenericParser.Block data) {
        this(NetParser.parseFaceList(data));
    }
    
    /**
     * Computes normals for the sectors of a face.
     * 
     * @param f the input face.
     * @param indexToPos maps symbolic corners to positions.
     * @return the array of sector normals.
     */
    private static Vector[] sectorNormals(
    		final Face f,
    		final Map<Integer, Point> indexToPos)
    {
        final int n = f.size();
        if (DEBUG) {
        	System.err.println("Computing normals for face " + f);
        }
        
        // --- compute corners and center of this face
        Matrix sum = null;
        final Point corners[] = new Point[n];
        for (int i = 0; i < n; ++i) {
            final int v = f.vertex(i);
            final Vector s = f.shift(i);
            final Point p = (Point) s.plus(indexToPos.get(v));
            corners[i] = p;
            if (sum == null) {
                sum = p.getCoordinates();
            } else {
                sum = (Matrix) sum.plus(p.getCoordinates());
            }
        }
        final Point center = new Point((Matrix) sum.dividedBy(n));

        // --- use that to compute the normals
        final Vector normals[] = new Vector[n];
        for (int i = 0; i < n; ++i) {
            final int i1 = (i + 1) % n;
            final Vector a = (Vector) corners[i].minus(center);
            final Vector b = (Vector) corners[i1].minus(corners[i]);
            normals[i] = Vector.unit(Vector.crossProduct3D(a, b));
            if (DEBUG) {
            	System.err.println("  " + normals[i]);
            }
        }

        return normals;
    }
    
	private static Map<?, List<Incidence>> collectEdges(
			final List<?> faces,
			final boolean useShifts)
	{
		final Map<Object, List<Incidence>> facesAtEdge =
				new HashMap<Object, List<Incidence>>();
		for (int i = 0; i < faces.size(); ++i) {
            final Face f;
            final Vector fShift;
            if (useShifts) {
                @SuppressWarnings("unchecked")
				final Pair<Face, Vector> entry =
                		(Pair<Face, Vector>) faces.get(i);
                f = entry.getFirst();
                fShift = entry.getSecond();
            } else {
                f = (Face) faces.get(i);
                fShift = null;
            }
			final int n = f.size();
			for (int j = 0; j < n; ++j) {
				final int j1 = (j + 1) % n;
				final int v = f.vertex(j);
				final int w = f.vertex(j1);
				final Vector s = (Vector) f.shift(j1).minus(f.shift(j));
				final Edge e = new Edge(v, w, s);
				final boolean rev = (e.source != v || !e.shift.equals(s));
                final Object key;
                if (useShifts) {
                    final Vector vShift = rev ? f.shift(j1) : f.shift(j);
                    key = new Pair<Edge, Vector>(e,
                    		(Vector) fShift.plus(vShift));
                } else {
                    key = e;
                }
				if (!facesAtEdge.containsKey(key)) {
					facesAtEdge.put(key, new ArrayList<Incidence>());
				}
				facesAtEdge.get(key).add(new Incidence(i, j, rev));
			}
		}
		
		if (DEBUG) {
			System.err.println("Edge to incident faces mapping:");
            final List<Object> edges = new ArrayList<Object>();
            edges.addAll(facesAtEdge.keySet());
            Collections.sort(edges, new Comparator<Object>() {
				public int compare(final Object arg0, final Object arg1) {
					if (useShifts) {
						@SuppressWarnings("unchecked")
						final Pair<Edge, Vector> p0 = (Pair<Edge, Vector>) arg0;
						@SuppressWarnings("unchecked")
						final Pair<Edge, Vector> p1 = (Pair<Edge, Vector>) arg1;
						if (p0.getFirst().equals(p1.getFirst()))
							return p0.getSecond().compareTo(p1.getSecond());
						else
							return p0.getFirst().compareTo(p1.getFirst());
					} else 
						return ((Edge) arg0).compareTo((Edge) arg1);
				}});
			for (final Object e: edges) {
				final List<Incidence> inc = facesAtEdge.get(e);
				System.err.println("  " + inc.size() + " at edge " + e + ":");
				for (int i = 0; i < inc.size(); ++i) {
					System.err.println("    " + inc.get(i));
				}
			}
		}
		return facesAtEdge;
	}

    private void set2opPlainMode(
    		final DynamicDSymbol ds,
    		final Map<Face, List<Integer>> faceElms)
    {
        // --- determine sector normals for each face
        final Map<Face, Vector[]> normals = new HashMap<Face, Vector[]>();
        for (final Face f: this.faces) {
            normals.put(f, sectorNormals(f, this.indexToPos));
        }
        
        // --- set 2 operator according to cyclic orders of faces around edges
        final Map<?, List<Incidence>> facesAtEdge =
        		collectEdges(this.faces, false);

        for (final Object obj: facesAtEdge.keySet()) {
        	final Edge e = (Edge) obj;
            final Point p = this.indexToPos.get(e.source);
            final Point q = this.indexToPos.get(e.target);
            final Vector a = Vector.unit((Vector) q.plus(e.shift).minus(p));
            
            // --- augment all incidences at this edge with their angles
            final List<Incidence> incidences = facesAtEdge.get(e);
            Vector n0 = null;
            for (int i = 0; i < incidences.size(); ++i) {
                final Incidence inc = incidences.get(i);
                Vector normal =
                		(normals.get(faces.get(inc.faceIndex)))[inc.edgeIndex];
                if (inc.reverse) {
                    normal = (Vector) normal.negative();
                }
                double angle;
                if (i == 0) {
                    n0 = normal;
                    angle = 0.0;
                } else {
                    double x = ((Real) Vector.dot(n0, normal)).doubleValue();
                    x = Math.max(Math.min(x, 1.0), -1.0);
                    angle = Math.acos(x);
                    if (Vector.volume3D(a, n0, normal).isNegative()) {
                        angle = 2 * Math.PI - angle;
                    }
                }
                incidences.set(i, new Incidence(inc, angle));
            }
            if (DEBUG) {
                System.err.println("Augmented incidences at edge " + e + ":");
                for (int i = 0; i < incidences.size(); ++i) {
                    System.err.println("    " + incidences.get(i));
                }
            }
            
            // --- sort by angle
            Collections.sort(incidences);
            
            // --- top off with a copy of the first incidences
            final Incidence inc = incidences.get(0);
            incidences.add(new Incidence(inc, inc.angle + 2 * Math.PI));
            if (DEBUG) {
                System.err.println("Sorted incidences at edge " + e + ":");
                for (int i = 0; i < incidences.size(); ++i) {
                    System.err.println("    " + incidences.get(i));
                }
            }
            
            // --- now set all the connections around this edge
            for (int i = 0; i < incidences.size() - 1; ++i) {
                final Incidence inc1 = incidences.get(i);
                final Incidence inc2 = incidences.get(i + 1);
                if (inc2.angle - inc1.angle < 1e-3) {
                    throw new RuntimeException("tiny dihedral angle");
                }
                final List<Integer> elms1 =
                		faceElms.get(faces.get(inc1.faceIndex));
				final List<Integer> elms2 =
						faceElms.get(faces.get(inc2.faceIndex));
                
                final int A, B, C, D;
                if (inc1.reverse) {
                    final int k =
                    	2 * (inc1.edgeIndex + faces.get(inc1.faceIndex).size());
                    A = elms1.get(k + 1);
                    B = elms1.get(k);
                } else {
                    final int k = 2 * inc1.edgeIndex;
                    A = elms1.get(k);
                    B = elms1.get(k + 1);
                }
                if (inc2.reverse) {
                    final int k = 2 * inc2.edgeIndex;
                    C = elms2.get(k + 1);
                    D = elms2.get(k);
                } else {
                    final int k =
                    	2 * (inc2.edgeIndex + faces.get(inc2.faceIndex).size());
                    C = elms2.get(k);
                    D = elms2.get(k + 1);
                }
                ds.redefineOp(2, A, C);
                ds.redefineOp(2, B, D);
            }
        }
    }
    
    private void set2opTileMode(
    		final DynamicDSymbol ds,
    		final Map<Face, List<Integer>> faceElms)
    {
        for (int i = 0; i < this.tiles.size(); ++i) {
			final List<Pair<Face, Vector>> tile = this.tiles.get(i);
			final Map<?, List<Incidence>> facesAtEdge =
					collectEdges(tile, true);
			for (final List<Incidence> flist: facesAtEdge.values()) {
				if (flist.size() != 2) {
					final String msg = flist.size() + " faces at an edge";
					throw new UnsupportedOperationException(msg);
				}
				final int D[] = new int[2];
                final int E[] = new int[2];
                boolean reverse = false;
				for (int k = 0; k <= 1; ++k) {
					final Incidence inc = flist.get(k);
                    final Pair<Face, Vector> p = tile.get(inc.faceIndex);
                    final Face face = p.getFirst();
                    final Vector shift = p.getSecond();
                    final List<Pair<Integer, Vector>> taf =
                    		this.tilesAtFace.get(face);
                    final int t =
                    		taf.indexOf(new Pair<Integer, Vector>(i, shift));
                    final int x = 2 * (t * face.size() + inc.edgeIndex);
                    D[k] = faceElms.get(face).get(x);
                    E[k] = faceElms.get(face).get(x + 1);
                    if (inc.reverse) {
                        reverse = !reverse;
                    }
				}
                if (reverse) {
                    ds.redefineOp(2, D[0], E[1]);
                    ds.redefineOp(2, D[1], E[0]);
                } else {
                    ds.redefineOp(2, D[0], D[1]);
                    ds.redefineOp(2, E[0], E[1]);
                }
			}
		}
    }
    
    private void assertCompleteness(final DelaneySymbol<Integer> ds)
    {
        final IndexList idcs = new IndexList(ds);
        for (final int D: ds.elements()) {
            for (int i = 0; i < idcs.size()-1; ++i) {
                final int ii = idcs.get(i);
                if (!ds.definesOp(ii, D)) {
                	throw new AssertionError(
                			"op(" + ii + ", " + D + ") undefined");
                }
                for (int j = i+1; j < idcs.size(); ++j) {
                    final int jj = idcs.get(j);
                    if (!ds.definesV(ii, jj, D)) {
                    	throw new AssertionError(
                    			"v(" + ii + ", " + jj + ", " + D +
                    			") undefined");
                    }
                }
            }
        }
    }

    public DSymbol getSymbol() {
        return ds;
    }

    public DSCover<Integer> getCover() {
        return cover;
    }

    public Map<Integer, Point> getPositions() {
        return positions;
    }
}

/*
   Copyright 2007 Olaf Delgado-Friedrichs

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.Pair;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.io.GenericParser;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.NetParser.Face;

/**
 * Implements a periodic face set meant to define a tiling.
 * 
 * @author Olaf Delgado
 * @version $Id: FaceList.java,v 1.14 2007/11/29 05:17:20 odf Exp $
 */
public class FaceList {
	final private static boolean DEBUG = false;
	
	/**
	 * Hashable class for edges in the tiling to be constructed.
	 */
	private static class Edge implements Comparable {
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
		
        public int compareTo(final Object arg) {
            if (arg instanceof Edge) {
                final Edge other = (Edge) arg;
                if (this.source != other.source) {
                    return this.source - other.source;
                }
                if (this.target != other.target) {
                    return this.target - other.target;
                }
                return this.shift.compareTo(other.shift);
            } else {
                throw new IllegalArgumentException("argument must be an Edge");
            }
        }
        
		public String toString() {
			return "(" + source + "," + target + "," + shift + ")";
		}
	}
	
	private static class Incidence implements Comparable {
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

        public int compareTo(final Object arg) {
            if (arg instanceof Incidence) {
                final Incidence other = (Incidence) arg;
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
            } else {
                throw new IllegalArgumentException("Incidence expected");
            }
        }
        
        public String toString() {
        	return "(" + faceIndex + "," + edgeIndex + "," + reverse + ","
					+ angle + ")";
        }
	}
	
    final private List faces;
    final private List tiles;
    final private Map tilesAtFace;
    final private Map indexToPos;
    final private int dim;
    final private DSymbol ds;
    
	public FaceList(final List input, final Map indexToPosition) {
		if (DEBUG) {
			System.err.println("\nStarting FaceList constructor");
		}
        if (input == null || input.size() < 1) {
            throw new IllegalArgumentException("no data given");
        }
        
        if (input.get(0) instanceof List) {
            this.tiles = new ArrayList();
            this.tilesAtFace = new HashMap();
            for (int i = 0; i < input.size(); ++i) {
                final List tile = (List) input.get(i);
                final List newTile = new ArrayList();
                for (int j = 0; j < tile.size(); ++j) {
                    final Pair entry = (Pair) tile.get(j);
                    final Face face = (Face) entry.getFirst();
                    final Pair normal = NetParser.normalizedFace(face);
                    final Vector shift = (Vector) ((Vector) entry.getSecond())
                            .plus(normal.getSecond());
                    if (!this.tilesAtFace.containsKey(face)) {
                        this.tilesAtFace.put(face, new ArrayList());
                    }
                    ((List) this.tilesAtFace.get(face)).add(new Pair(new Integer(i),
                            shift));
                    newTile.add(new Pair(face, shift));
                }
                this.tiles.add(newTile);
            }
            // --- make sure each face is in exactly two tiles
            for (Iterator iter = this.tilesAtFace.values().iterator();
            		iter.hasNext();) {
            	final List tlist = (List) iter.next();
            	final int n = tlist.size();
            	if (n != 2) {
            		throw new IllegalArgumentException("Face incident to " + n
							+ " tile" + (n == 1 ? "" : "s") + ".");
            	}
            }
            
            this.faces = new ArrayList();
            this.faces.addAll(this.tilesAtFace.keySet());
        } else {
            this.tiles = null;
            this.tilesAtFace = null;
            this.faces = new ArrayList();
            this.faces.addAll(input);
        }
        
        final Face f0 = (Face) this.faces.get(0);
        if (f0.size() < 3) {
            throw new IllegalArgumentException("minimal face-size is 3");
        }
        this.dim = f0.shift(0).getDimension();
        if (this.dim != 3) {
            throw new UnsupportedOperationException("dimension must be 3");
        }
        
        this.indexToPos = indexToPosition;
        
        // --- initialize the intermediate symbol
        final Map faceElements = new HashMap();
        final DynamicDSymbol ds = new DynamicDSymbol(this.dim);
        for (final Iterator iter = this.faces.iterator(); iter.hasNext();) {
            final Face f = (Face) iter.next();
            final int n = f.size();
            final int _2n = 2 * n;
            final List elms = ds.grow(4 * n);
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
        	for (final Iterator iter = ds.elements(); iter.hasNext();) {
        		final Object D = iter.next();
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
        
        // --- freeze the constructed symbol
        //TODO keep given symmetry and record original vertex positions
        this.ds = new DSymbol(ds.minimal());
	}
	
    private FaceList(final Pair p) {
        this((List) p.getFirst(), (Map) p.getSecond());
    }
    
    public FaceList(final GenericParser.Block data) {
        this(NetParser.parseFaceList(data));
    }
    
    public DSymbol getSymbol() {
        return this.ds;
    }
    
    /**
     * Computes normals for the sectors of a face.
     * 
     * @param f the input face.
     * @param indexToPos maps symbolic corners to positions.
     * @return the array of sector normals.
     */
    private static Vector[] sectorNormals(final Face f, final Map indexToPos) {
        final int n = f.size();
        if (DEBUG) {
        	System.err.println("Computing normals for face " + f);
        }
        
        // --- compute corners and center of this face
        Matrix sum = null;
        final Point corners[] = new Point[n];
        for (int i = 0; i < n; ++i) {
            final Integer v = new Integer(f.vertex(i));
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
    
	private static Map collectEdges(final List faces, final boolean useShifts) {
		final Map facesAtEdge = new HashMap();
		for (int i = 0; i < faces.size(); ++i) {
            final Face f;
            final Vector fShift;
            if (useShifts) {
                final Pair entry = (Pair) faces.get(i);
                f = (Face) entry.getFirst();
                fShift = (Vector) entry.getSecond();
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
                    key = new Pair(e, fShift.plus(vShift));
                } else {
                    key = e;
                }
				if (!facesAtEdge.containsKey(key)) {
					facesAtEdge.put(key, new ArrayList());
				}
				((List) facesAtEdge.get(key)).add(new Incidence(i, j, rev));
			}
		}
		
		if (DEBUG) {
			System.err.println("Edge to incident faces mapping:");
            final List edges = new ArrayList();
            edges.addAll(facesAtEdge.keySet());
            Collections.sort(edges);
			for (Iterator iter = edges.iterator(); iter.hasNext();) {
				final Object e = iter.next();
				final List inc = (List) facesAtEdge.get(e);
				System.err.println("  " + inc.size() + " at edge " + e + ":");
				for (int i = 0; i < inc.size(); ++i) {
					System.err.println("    " + inc.get(i));
				}
			}
		}
		return facesAtEdge;
	}

    private void set2opPlainMode(final DynamicDSymbol ds, final Map faceElms) {
        // --- determine sector normals for each face
        final Map normals = new HashMap();
        for (final Iterator iter = this.faces.iterator(); iter.hasNext();) {
            final Face f = (Face) iter.next();
            normals.put(f, sectorNormals(f, this.indexToPos));
        }
        
        // --- set 2 operator according to cyclic orders of faces around edges
        final Map facesAtEdge = collectEdges(this.faces, false);

        for (Iterator iter = facesAtEdge.keySet().iterator(); iter.hasNext();) {
            final Edge e = (Edge) iter.next();
            final Point p = (Point) this.indexToPos.get(new Integer(e.source));
            final Point q = (Point) this.indexToPos.get(new Integer(e.target));
            final Vector a = Vector.unit((Vector) q.plus(e.shift).minus(p));
            
            // --- augment all incidences at this edge with their angles
            final List incidences = (List) facesAtEdge.get(e);
            Vector n0 = null;
            for (int i = 0; i < incidences.size(); ++i) {
                final Incidence inc = (Incidence) incidences.get(i);
                Vector normal = ((Vector[]) normals.get(faces
						.get(inc.faceIndex)))[inc.edgeIndex];
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
            final Incidence inc = (Incidence) incidences.get(0);
            incidences.add(new Incidence(inc, inc.angle + 2 * Math.PI));
            if (DEBUG) {
                System.err.println("Sorted incidences at edge " + e + ":");
                for (int i = 0; i < incidences.size(); ++i) {
                    System.err.println("    " + incidences.get(i));
                }
            }
            
            // --- now set all the connections around this edge
            for (int i = 0; i < incidences.size() - 1; ++i) {
                final Incidence inc1 = (Incidence) incidences.get(i);
                final Incidence inc2 = (Incidence) incidences.get(i + 1);
                if (inc2.angle - inc1.angle < 1e-3) {
                    throw new RuntimeException("tiny dihedral angle");
                }
                final List elms1 = (List) faceElms.get(faces
						.get(inc1.faceIndex));
				final List elms2 = (List) faceElms.get(faces
						.get(inc2.faceIndex));
                
                final Object A, B, C, D;
                if (inc1.reverse) {
                    final int k = 2 * (inc1.edgeIndex + ((Face) faces
							.get(inc1.faceIndex)).size());
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
                    final int k = 2 * (inc2.edgeIndex + ((Face) faces
							.get(inc2.faceIndex)).size());
                    C = elms2.get(k);
                    D = elms2.get(k + 1);
                }
                ds.redefineOp(2, A, C);
                ds.redefineOp(2, B, D);
            }
        }
    }
    
    private void set2opTileMode(final DynamicDSymbol ds, final Map faceElms) {
        for (int i = 0; i < this.tiles.size(); ++i) {
			final List tile = (List) this.tiles.get(i);
			final Map facesAtEdge = collectEdges(tile, true);
			for (Iterator i2 = facesAtEdge.values().iterator(); i2.hasNext();) {
				final List flist = (List) i2.next();
				if (flist.size() != 2) {
					final String msg = flist.size() + " faces at an edge";
					throw new UnsupportedOperationException(msg);
				}
				final Object D[] = new Object[2];
                final Object E[] = new Object[2];
                boolean reverse = false;
				for (int k = 0; k <= 1; ++k) {
					final Incidence inc = (Incidence) flist.get(k);
                    final Pair p = (Pair) tile.get(inc.faceIndex);
                    final Face face = (Face) p.getFirst();
                    final Vector shift = (Vector) p.getSecond();
                    final List taf = (List) this.tilesAtFace.get(face);
                    final int t = taf.indexOf(new Pair(new Integer(i), shift));
                    final int x = 2 * (t * face.size() + inc.edgeIndex);
                    D[k] = ((List) faceElms.get(face)).get(x);
                    E[k] = ((List) faceElms.get(face)).get(x + 1);
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
    
    private void assertCompleteness(final DelaneySymbol ds) {
        final List idcs = new IndexList(ds);
        for (final Iterator elms = ds.elements(); elms.hasNext();) {
            final Object D = elms.next();
            for (int i = 0; i < idcs.size()-1; ++i) {
                final int ii = ((Integer) idcs.get(i)).intValue();
                if (!ds.definesOp(ii, D)) {
                	throw new AssertionError(
                			"op(" + ii + ", " + D + ") undefined");
                }
                for (int j = i+1; j < idcs.size(); ++j) {
                    final int jj = ((Integer) idcs.get(j)).intValue();
                    if (!ds.definesV(ii, jj, D)) {
                    	throw new AssertionError(
                    			"v(" + ii + ", " + jj + ", " + D +
                    			") undefined");
                    }
                }
            }
        }
    }
}

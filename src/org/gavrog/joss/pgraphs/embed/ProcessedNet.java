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

package org.gavrog.joss.pgraphs.embed;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Pair;
import org.gavrog.box.simple.Strings;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.IArithmetic;
import org.gavrog.jane.numbers.Real;
import org.gavrog.joss.geometry.CellCorrection;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Lattices;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroupFinder;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.Cover;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * Stores and prints a graph with its name, embedding and space group symmetry.
 */
public class ProcessedNet {
    private final static DecimalFormat fmtReal4 = new DecimalFormat("0.0000");
    private final static DecimalFormat fmtReal5 = new DecimalFormat("0.00000");
    
    final static boolean DEBUG = false;
    
    /*
     * Auxiliary type.
     */
    private class PlacedNode implements Comparable<PlacedNode> {
        final public INode v;
        final public Point p;
        
        public PlacedNode(final INode v, final Point p) {
            this.v = v;
            this.p = p;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(final PlacedNode other) {
            final Point p = this.p;
            final Point q = other.p;
            final int dim = p.getDimension();
            final Point o = Point.origin(dim);
            final Vector s = (Vector) p.minus(o);
            final Vector t = (Vector) q.minus(o);
            if (s.isNegative()) {
                if (t.isNonNegative()) {
                    return 1;
                }
            } else if (t.isNegative()) {
                return -1;
            }
            int diff = count_zeroes(t) - count_zeroes(s);
            if (diff != 0) {
                return diff;
            }
            diff = cmpCoords(Vector.dot(s, s), Vector.dot(t, t));
            if (diff != 0) {
                return diff;
            }
            for (int i = 0; i < dim; ++i) {
                diff = cmpCoords(p.get(i), q.get(i));
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }
        
		private int count_zeroes(final Vector s) {
			int n = 0;
			for (int i = 0; i < s.getDimension(); ++i) {
				if (((Real) s.get(i).abs()).doubleValue() < 1e-6) {
					++n;
				}
			}
			return n;
		}

		private int cmpCoords(final IArithmetic a, final IArithmetic b) {
            final double x = ((Real) a).doubleValue();
            final double y = ((Real) b).doubleValue();
            if (x < 0) {
                if (y > 0) {
                    return 1;
                }
            } else if (y < 0) {
                return -1;
            }
            final double d = Math.abs(x) - Math.abs(y);
            if (Math.abs(d) < 1e-6) {
                return 0;
            } else {
                return Double.compare(Math.abs(x), Math.abs(y));
            }
        }
    };

    private boolean verified = false;
    private final PeriodicGraph graph;
    private final String name;
    private final Map<INode, String> node2name;
    private final SpaceGroupFinder finder;
    private final Embedder embedder;

    public ProcessedNet(
			final PeriodicGraph G,
			final String name,
			final Map<INode, String> node2name,
			final SpaceGroupFinder finder,
			final Embedder embedder)
    {
        this.graph = G;
        this.name = name;
        this.node2name = node2name;
        this.finder = finder;
        this.embedder = embedder;
    }

    public void writeEmbedding(
            final Writer stream,
            final boolean cgdFormat,
			final boolean fullCell,
			final String posType)
    {
		final PrintWriter out = new PrintWriter(stream);
        
        // --- extract some data from the arguments
        if (DEBUG) {
        	System.out.println("\t\t@@@ In writeEmbedding(): preliminaries...");
        }
        
        final int d = graph.getDimension();
        final CoordinateChange toStd = finder.getToStd();
        final Matrix gram = embedder.getGramMatrix();
        
        // --- process unit cell parameters (possibly correcting settings)
        if (DEBUG) {
        	System.out.println("\t\t@@@ Computing cell parameters...");
        }
        
        CoordinateChange correction = processCellParameters(out,
				cgdFormat, fullCell);
        
        // --- compute orbit graph with respect to a conventional unit cell
        if (DEBUG) {
        	System.out.println("\t\t@@@ Computing full unit cell...");
        }
        
        final Cover cov = graph.conventionalCellCover();

        // --- lift relaxed node positions to the conventional unit cell
        if (DEBUG) {
        	System.out.println("\t\t@@@ Computing full list of node positions...");
        }
        
        final Map<INode, Point> pos = embedder.getPositions();
		final INode v0 = cov.nodes().next();
		final Vector shift = (Vector) pos.get(cov.image(v0)).times(
				toStd).minus(cov.liftedPosition(v0, pos));
		final Map<INode, Point> lifted = new HashMap<INode, Point>();
		for (final INode v: cov.nodes()) {
			lifted.put(v, (Point) cov.liftedPosition(v, pos).plus(shift)
			        .times(correction));
		}
        
        // --- if there's translational freedom, shift some node to a nice place
		final Vector tmp[] = graph.getSpaceGroup().shiftSpace();
		if (tmp.length > 0) {
			for (int i = 0; i < tmp.length; ++i) {
				tmp[i] = (Vector) tmp[i].times(toStd).times(correction);
			}
			final Matrix shiftSpace = Vector.toMatrix(tmp);
			final Operator proj = Operator.orthogonalProjection(shiftSpace,
					Matrix.one(d));
			final INode v = lifted.keySet().iterator().next();
			final Point p = lifted.get(v);
			final Vector s = (Vector) Point.origin(d).minus(p.times(proj));
			final CoordinateChange corrective_shift = new CoordinateChange(
					new Operator(s));
			for (final INode w: lifted.keySet()) {
				lifted.put(w, (Point) lifted.get(w).times(corrective_shift));
			}
			correction = (CoordinateChange) correction.times(corrective_shift);
		}
        
		// --- find the node representatives to print
        final boolean allNodes = fullCell;
        if (DEBUG) {
        	System.out.println("\t\t@@@ Computing node representatives...");
        }
        
        final Map<INode, Point> reps = nodeReps(cov, lifted, allNodes);
        
        // --- print the node positions
        if (!cgdFormat) {
			out.println("   " + posType	+ " positions:");
		}
        if (DEBUG) {
        	System.out.println("\t\t@@@ Printing node positions...");
        }
        int last = 0;
        for (final INode v: reps.keySet()) {
			final Point p = reps.get(v);
			final String name;
			if (allNodes) {
				name = "" + (++last);
			} else {
				name = Strings.parsable(this.node2name.get(cov.image(v)),
				        false);
			}

			// --- print them
			if (cgdFormat) {
				out.print("  NODE " + name + " "
						+ cov.new CoverNode(v).degree() + " ");
			} else {
				out.print("      Node " + name + ":   ");
			}
			for (int i = 0; i < d; ++i) {
				out.print(" "
				        + fmtReal5.format(((Real) p.get(i)).doubleValue()));
			}
			out.println();
		}
        
        // --- print the edges
        if (DEBUG) {
        	System.out.println("\t\t@@@ Printing edges...");
        }
        
        if (!cgdFormat) {
            out.println("   Edges:");
        }
        final List<Pair<PlacedNode, PlacedNode>> ereps =
                edgeReps(cov, reps, lifted, correction, fullCell);
        for (final Pair<PlacedNode, PlacedNode> pair: ereps) {
            final Point p = pair.getFirst().p;
            final Point q = pair.getSecond().p;

            // --- print its start and end positions
            if (cgdFormat) {
                out.print("  EDGE ");
            } else {
                out.print("     ");
            }
            for (int i = 0; i < d; ++i) {
                out.print(" " + fmtReal5.format(((Real) p.get(i)).doubleValue()));
            }
            if (cgdFormat) {
                out.print("  ");
            } else {
                out.print("  <-> ");
            }
            for (int i = 0; i < d; ++i) {
                out.print(" " + fmtReal5.format(((Real) q.get(i)).doubleValue()));
            }
            out.println();
        }
        
        // --- print the edge centers
        if (DEBUG) {
        	System.out.println("\t\t@@@ Printing edge centers...");
        }
        
        if (!cgdFormat) {
            out.println("   Edge centers:");
        }
        for (final Pair<PlacedNode, PlacedNode> pair: ereps) {
            final Point p = pair.getFirst().p;
            final Point q = pair.getSecond().p;

            if (cgdFormat) {
                out.print("# EDGE_CENTER ");
            } else {
                out.print("     ");
            }
            for (int i = 0; i < d; ++i) {
            	final double s = ((Real) p.get(i)).doubleValue();
            	final double t = ((Real) q.get(i)).doubleValue();
                out.print(" " + fmtReal5.format((s + t) / 2));
            }
            out.println();
        }
        
        // --- finish up
        if (cgdFormat) {
            out.println("END");
            out.println();
        } else {
            writeStatistics(out, gram, pos);
        }
        out.flush();
        if (DEBUG) {
        	System.out.println("\t\t@@@ In writeEmbedding(): done!");
        }
        
    }

	/**
	 * @param out
	 * @param gram
	 * @param pos
	 */
	private void writeStatistics(
	        final PrintWriter out,
	        final Matrix gram,
			final Map<INode, Point> pos) {
		// --- write edge statistics
		if (DEBUG) {
			System.out.println("\t\t@@@ Computing edge statistics...");
		}
		
		final String min = fmtReal5.format(embedder.minimalEdgeLength());
		final String max = fmtReal5.format(embedder.maximalEdgeLength());
		final String avg = fmtReal5.format(embedder.averageEdgeLength());
		out.println();
		out.println("   Edge statistics: minimum = " + min + ", maximum = "
				+ max + ", average = " + avg);
		
		// --- compute and write angle statistics
		if (DEBUG) {
			System.out.println("\t\t@@@ Computing angle statistics...");
		}
		
		double minAngle = Double.MAX_VALUE;
		double maxAngle = 0.0;
		double sumAngle = 0.0;
		int count = 0;
		
		for (final INode v: graph.nodes()) {
		    final Point p = pos.get(v);
		    final List<IEdge> incidences = graph.allIncidences(v);
		    final List<Vector> vectors = new ArrayList<Vector>();
		    for (final IEdge e: incidences) {
		        final INode w = e.target();
		        final Point q = (Point) pos.get(w);
		        vectors.add((Vector) q.plus(graph.getShift(e)).minus(p));
		    }
		    final int m = vectors.size();
		    for (int i = 0; i < m; ++i) {
		        final Vector s = vectors.get(i);
		        final double ls =
		                ((Real) Vector.dot(s, s, gram)).sqrt().doubleValue();
		        for (int j = i + 1; j < m; ++j) {
		            final Vector t = vectors.get(j);
		            final double lt = ((Real) Vector.dot(t, t, gram)).sqrt()
		                    .doubleValue();
		            final double dot =
		                    ((Real) Vector.dot(s, t, gram)).doubleValue();
		            final double arg =
		                    Math.max(-1, Math.min(1, dot / (ls * lt)));
		            final double angle = Math.acos(arg) / Math.PI * 180;
		            minAngle = Math.min(minAngle, angle);
		            maxAngle = Math.max(maxAngle, angle);
		            sumAngle += angle;
		            ++count;
		        }
		    }
		}
		out.println("   Angle statistics: minimum = " + fmtReal5.format(minAngle)
		        + ", maximum = " + fmtReal5.format(maxAngle) + ", average = "
		        + fmtReal5.format(sumAngle / count));
		out.flush();
		
		// --- write the shortest non-bonded distance
		if (DEBUG) {
			System.out.println("\t\t@@@ Computing shortest non-bonded distance...");
		}
		
		final double dist = smallestNonBondedDistance(graph, pos, gram);
		if (dist < 0) {
			out.println("   Shortest non-bonded distance not determined.");
		} else {
			out.println("   Shortest non-bonded distance = "
					+ fmtReal5.format(dist));
		}
		
		// --- write the degrees of freedom as found by the embedder
		if (embedder instanceof Embedder) {
			if (DEBUG) {
				System.out.println("\t\t@@@ Computing degrees of freedom...");
			}
			
		    out.println();
		    out.println("   Degrees of freedom: "
		            + ((Embedder) embedder).degreesOfFreedom());
		}
	}

	/**
	 * @param out
	 * @param cgdFormat
	 * @param fullCell
	 * @return
	 */
	private CoordinateChange processCellParameters(final PrintWriter out,
			final boolean cgdFormat, boolean fullCell) {
        final int d = graph.getDimension();
        final String extendedGroupName = finder.getExtendedGroupName();
        final CoordinateChange toStd = finder.getToStd();
        final CoordinateChange fromStd = (CoordinateChange) toStd.inverse();
        final boolean cellRelaxed = embedder.cellRelaxed();
        final Matrix gram = embedder.getGramMatrix();
        
		final CoordinateChange correction;
		if (d == 3) {
			// --- correct to a reduced cell for monoclinic and triclinic groups
        	final CellCorrection cc = new CellCorrection(finder, gram);
			correction = cc.getCoordinateChange();
			final String correctedGroupName;
			if (cc.getGroupName().equals(finder.getGroupName())) {
				correctedGroupName = extendedGroupName;
			} else {
				correctedGroupName = cc.getGroupName();
			}
			
			if (DEBUG) {
				System.out.println("\t\t@@@   cell correction = " + correction);
			}
			final CoordinateChange ctmp = (CoordinateChange) correction
					.inverse().times(fromStd);
			final Vector x = (Vector) Vector.unit(3, 0).times(ctmp);
			final Vector y = (Vector) Vector.unit(3, 1).times(ctmp);
			final Vector z = (Vector) Vector.unit(3, 2).times(ctmp);

			// --- compute the cell parameters
			if (DEBUG) {
				System.out.println("\t\t\t x = " + x);
				System.out.println("\t\t\t y = " + y);
				System.out.println("\t\t\t z = " + z);
				System.out.println("\t\t\t Gram = " + gram);
				System.out.println("\t\t\t\t determinant = " + gram.determinant());
			}
			final double a = Math.sqrt(((Real) Vector.dot(x, x, gram))
					.doubleValue());
			final double b = Math.sqrt(((Real) Vector.dot(y, y, gram))
					.doubleValue());
			final double c = Math.sqrt(((Real) Vector.dot(z, z, gram))
					.doubleValue());
			final double f = 180.0 / Math.PI;
			final double alpha = f * Math.acos(
					((Real) Vector.dot(y, z, gram)).doubleValue() / (b * c));
			final double beta = f * Math.acos(
					((Real) Vector.dot(x, z, gram)).doubleValue() / (a * c));
			final double gamma = f * Math.acos(
					((Real) Vector.dot(x, y, gram)).doubleValue() / (a * b));

			// --- print the cell parameters
			if (DEBUG) {
				System.out.println("\t\t@@@ Writing cell parameters...");
			}

	        // --- print a header if necessary
	        if (DEBUG) {
	        	System.out.println("\t\t@@@ Writing header...");
	        }
	        
	        if (cgdFormat) {
	            out.println("CRYSTAL");
	            out.println("  NAME " + Strings.parsable(name, false));
	            if (fullCell) {
	            		out.println("  GROUP P1");
	            } else {
	                out.println("  GROUP " + correctedGroupName);
	            }
	        } else if (correctedGroupName != extendedGroupName && ! fullCell) {
	        	out.println("   Group setting modified to " + correctedGroupName);
	        }
	        
	        // --- print the cell info
			if (cgdFormat) {
				out.println("  CELL " + fmtReal5.format(a) + " "
                        + fmtReal5.format(b) + " " + fmtReal5.format(c) + " "
                        + fmtReal4.format(alpha) + " " + fmtReal4.format(beta)
                        + " " + fmtReal4.format(gamma));
			} else {
				if (fullCell) {
					out.println("   Coordinates below are given for a full "
                            + "conventional cell.");
				}
				out.println("   " + (cellRelaxed ? "R" : "Unr")
                        + "elaxed cell parameters:");
                out.println("       a = " + fmtReal5.format(a) + ", b = "
                        + fmtReal5.format(b) + ", c = " + fmtReal5.format(c));
                out.println("       alpha = " + fmtReal4.format(alpha)
                        + ", beta = " + fmtReal4.format(beta) + ", gamma = "
                        + fmtReal4.format(gamma));
				out.println("   Cell volume: "
                        + fmtReal5.format(((Real) Vector.volume3D(x, y, z))
                                .doubleValue()
                                * Math.sqrt(((Real) gram.determinant())
                                        .doubleValue())));
			}
		} else if (d == 2){
			// --- the cell vectors in the embedder's coordinate system
			Vector x = (Vector) Vector.unit(2, 0).times(fromStd);
			Vector y = (Vector) Vector.unit(2, 1).times(fromStd);

			// --- correct to a reduced cell for monoclinic and triclinic groups
			correction = new CoordinateChange(Operator.identity(d));

			// --- compute the cell parameters
			final double a = Math.sqrt(((Real) Vector.dot(x, x, gram))
					.doubleValue());
			final double b = Math.sqrt(((Real) Vector.dot(y, y, gram))
					.doubleValue());
			final double f = 180.0 / Math.PI;
			final double gamma = Math.acos(((Real) Vector.dot(x, y, gram))
					.doubleValue() / (a * b)) * f;

	        // --- print a header if necessary
	        if (DEBUG) {
	        	System.out.println("\t\t@@@ Writing header...");
	        }
	        
	        if (cgdFormat) {
	            out.println("CRYSTAL");
	            out.println("  NAME " + Strings.parsable(name, false));
	            if (fullCell) {
	            	out.println("  GROUP p1");
	            } else {
	                out.println("  GROUP " + extendedGroupName);
	            }
	        }
	        
			// --- print the cell parameters
			if (DEBUG) {
				System.out.println("\t\t@@@ Writing cell parameters...");
			}

			if (cgdFormat) {
				out.println("  CELL " + fmtReal5.format(a) + " "
						+ fmtReal5.format(b) + " " + fmtReal4.format(gamma));
			} else {
				if (fullCell) {
					out.println("   Coordinates are for a full conventional cell.");
				}
				out.println("   " + (cellRelaxed ? "R" : "Unr")
						+ "elaxed cell parameters:");
				out.println("       a = " + fmtReal5.format(a) + ", b = "
						+ fmtReal5.format(b) + ", gamma = "
						+ fmtReal4.format(gamma));
			}
		} else if (d == 1) {
            correction = new CoordinateChange(Operator.identity(d));

            if (DEBUG) {
                System.out.println("\t\t@@@ Writing header...");
            }
            
            if (cgdFormat) {
                out.println("CRYSTAL");
                out.println("  NAME " + Strings.parsable(name, false));
                if (fullCell) {
                    out.println("  GROUP op1");
                } else {
                    out.println("  GROUP " + extendedGroupName);
                }
            }
		} else {
			throw new RuntimeException("dimension must be 2 or 3");
		}
		return correction;
	}

    private Map<INode, Point> nodeReps(
            final PeriodicGraph cov,
            final Map<INode, Point> lifted,
			final boolean allNodes)
	{
		final Map<INode, Point> reps = new LinkedHashMap<INode, Point>();
        for (final Iterator<Set<INode>> orbits = cov.nodeOrbits();
                orbits.hasNext();) {
            // --- grab the next node orbit
            final Set<INode> orbit = orbits.next();
            
            // --- find positions for all nodes
            final List<PlacedNode> tmp = new ArrayList<PlacedNode>();
            for (final INode v: orbit) {
                final Point p = lifted.get(v).modZ();
                tmp.add(new PlacedNode(v, p));
            }
            
            // --- sort by position
            Collections.sort(tmp);
            
            // --- extract the node (or nodes) to use
            if (allNodes) {
                for (int i = 0; i < tmp.size(); ++i) {
                    final PlacedNode pn = tmp.get(i);
                    reps.put(pn.v, pn.p);
                }
            } else {
                final PlacedNode pn = tmp.get(0);
                reps.put(pn.v, pn.p);
            }
        }
        
        return reps;
    }

    private List<Pair<PlacedNode, PlacedNode>> edgeReps(
            final PeriodicGraph cov,
            final Map<INode, Point> reps,
			final Map<INode, Point> lifted,
			final CoordinateChange correction,
			final boolean allEdges)
	{
        final List<Pair<PlacedNode, PlacedNode>> result =
                new LinkedList<Pair<PlacedNode, PlacedNode>>();
        
        for (final Iterator<Set<IEdge>> orbits = cov.edgeOrbits();
                orbits.hasNext();) {
            // --- grab the next edge orbit
            final Set<IEdge> orbit = orbits.next();
            
            // --- extract edges starting or ending in a node in reps
            final List<IEdge> edges = new ArrayList<IEdge>();
            for (final IEdge e: orbit) {
                if (reps.containsKey(e.source())) {
                    edges.add(e);
                }
                if (reps.containsKey(e.target())) {
                    edges.add(e.reverse());
                }
            }
            
            // --- find positions for all the end points
            final List<Pair<PlacedNode, PlacedNode>> candidates =
                    new ArrayList<Pair<PlacedNode, PlacedNode>>();
            for (final IEdge e: edges) {
                final INode v = e.source();
                final INode w = e.target();
                final Point p = lifted.get(v);
                final Point q = (Point) lifted.get(w)
                        .plus(cov.getShift(e).times(correction));
				final Point p0 = p.modZ();
				final Point q0 = (Point) q.minus(p.minus(p0));
				candidates.add(new Pair<PlacedNode, PlacedNode>(
				        new PlacedNode(v, p0), new PlacedNode(w, q0)));
            }
            
            // --- sort edges by end point positions
            Collections.sort(candidates,
                    Pair.<PlacedNode, PlacedNode>defaultComparator());
            
            // --- extract the edge (or edges) to use
            if (allEdges) {
                result.addAll(candidates);
            } else {
                result.add(candidates.get(0));
            }
        }
        return result;
    }

    /**
     * Does what it says.
     * 
     * @param G         a periodic graph.
     * @param embedder  an embedding for G.
     * @return the smallest distance between nodes that are not connected.
     */
    public static double smallestNonBondedDistance(
            final PeriodicGraph G,
            final Map<INode, Point> pos,
            final Matrix gram) {
        // --- get some data about the graph and embedding
    	final int dim = G.getDimension();
        
        // --- compute a Dirichlet domain for the translation lattice
        final Vector basis[] = Vector.rowVectors(Matrix.one(G.getDimension()));
		if (DEBUG) {
			System.out.println("\t\t\t@@@ Computing dirichlet vectors...");
		}
        final Vector dirichlet[] = Lattices.dirichletVectors(basis, gram);
		if (DEBUG) {
			System.out.println("\t\t\t@@@ Computing reduced basis...");
		}
        final Vector reduced[] = Lattices.reducedLatticeBasis(basis, gram);
        
        // --- find vectors that address neighborhood of dirichlet domain
		if (DEBUG) {
			System.out.println("\t\t\t@@@ Computing neighborhood vectors...");
		}
        final Matrix M = Vector.toMatrix(reduced);
        final List<Vector> cellNeighbors = new ArrayList<Vector>();
        final int f[] = new int[dim];
        for (int i = 0; i < dim; ++i) f[i] = -1;
        while (true) {
        	final Vector v = new Vector(f);
        	cellNeighbors.add(new Vector((Matrix) v.getCoordinates().times(M)));
        	int i = dim-1;
        	while (i >= 0 && f[i] == 1) --i;
        	if (i < 0) {
        		break;
        	} else {
        		++f[i++];
        		while (i < dim) f[i++] = -1;
        	}
        }

        // --- smallest distance seen so far
        double minDist = Double.MAX_VALUE;

        // --- loop over all pairs of points in the repeat unit
		if (DEBUG) {
			System.out.println("\t\t\t@@@ Looping over node pairs...");
		}
        for (final INode v: G.nodes()) {
            final Point p = pos.get(v);
            for (final INode w: G.nodes()) {
                final Point q = pos.get(w);
                
                // --- find a closest translate of w
                final Point d = (Point) q.minus(p).plus(Point.origin(dim));
                final Vector s =
                	Lattices.dirichletShifts(d, dirichlet, gram, 1)[0];
                
                // --- determine excluded translates and seeds
                final Set<Vector> excluded = new HashSet<Vector>();
                if (v.equals(w)) {
                	excluded.add(Vector.zero(dim));
                }
                final Set<Vector> seeds = new HashSet<Vector>();
                seeds.add(Vector.zero(dim));
                for (final IEdge e: G.allIncidences(v)) {
                	final INode u = e.target();
                	if (u.equals(w)) {
                		excluded.add(G.getShift(e));
                		seeds.add(G.getShift(e));
                	}
                }
                
                // --- find closest translate that isn't a neighbor of v
                for (final Vector seed: seeds) {
					for (final Vector n: cellNeighbors) {
						final Vector t = (Vector) n.plus(s).plus(seed);
						if (!excluded.contains(t)) {
							final Point r = (Point) q.plus(t);
							final Vector x = (Vector) r.minus(p);
							final double dist = ((Real) Vector.dot(x, x, gram))
									.doubleValue();
							if (dist < minDist) {
								minDist = dist;
							}
						}
					}
				}
            }
        }
        
        // --- return the result
        if (minDist == Double.MAX_VALUE) {
        	return -1.0;
        } else {
        	return Math.sqrt(minDist);
        }
    }
    
    /**
     * @return the graph.
     */
    public PeriodicGraph getGraph() {
        return this.graph;
    }
    
    /**
     * @return the name.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * @return the current value of verified.
     */
    public boolean getVerified() {
        return this.verified;
    }
    
    /**
     * @param verified The new value for verified.
     */
    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean hasEmbedding() {
        return this.embedder != null;
    }
}

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

package org.gavrog.joss.pgraphs.embed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Partition;
import org.gavrog.jane.algorithms.Amoeba;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.FloatingPoint;
import org.gavrog.jane.numbers.Fraction;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.SpaceGroup;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 * This class implements an embedding algorithm for periodic graphs.
 * 
 * @author Olaf Delgado
 * @version $Id: Embedder.java,v 1.9 2008/07/09 01:09:24 odf Exp $
 */
public class Embedder {
	final private static int EDGE = 1;
	final private static int ANGLE = 2;

	// --- temporary helper class
	private class Angle {
		final public INode v;
		final public INode w;
		final public Vector s;

		public Angle(final INode v, final INode w, final Vector s) {
			this.v = v;
			this.w = w;
			this.s = s;
		}

		public boolean equals(final Object other) {
			if (!(other instanceof Angle)) {
				return false;
			}
			final Angle a = (Angle) other;
			return (this.v.equals(a.v) && this.w.equals(a.w) && this.s
					.equals(a.s))
					|| (this.v.equals(a.w) && this.w.equals(a.v) && this.s
							.equals(a.s.negative()));
		}

		public int hashCode() {
			final int hv = this.v.hashCode();
			final int hw = this.w.hashCode();
			if (hv <= hw) {
				return (hv * 37 + hw) * 37 + this.s.hashCode();
			} else {
				return (hw * 37 + hv) * 37 + this.s.negative().hashCode();
			}
		}
	}

	// --- internal class that represents an edge orbit
	private class Edge {
		public final INode v;
		public final INode w;
		public final double shift[];
		public final int type;
		public final double weight;
		public double length;

		public Edge(
				final INode v, final INode w, final Vector s, int type,
				final double weight) {
			this.v = v;
			this.w = w;
			this.shift = s.asDoubleArray();
			this.type = type;
			this.weight = weight;
		}
	}

	// --- the graph to embed and its dimension
	final private PeriodicGraph graph;
	final private int dimGraph;

	// --- precomputed data used by the algorithm
	final private Map<INode, Operator> node2sym;
	final private Map<INode, Map<INode, Operator>> node2images;
	final private Map<INode, Integer> node2index;
	final private Map<INode, double[][]> node2mapping;
	final private int dimParSpace;
	final private Matrix gramSpace;
	final private int gramIndex[][];
	final private Edge edges[];

	// --- represent the current state of the algorithm
	double p[];
	double volumeWeight;
	double penaltyFactor;

	private boolean _positionsRelaxed = false;
	private boolean _cellRelaxed = false;

	// --- Options:
	private int passes = 3;
	private boolean optimizePositions = true;

	/**
	 * Constructs an instance.
	 * 
	 * @param graph the periodic graph to embed.
	 */
	public Embedder(final PeriodicGraph graph) {
		this.graph = graph;
		this.dimGraph = graph.getDimension();
		final int dim = this.dimGraph;

		this.node2sym = nodeSymmetrizations();

		this.node2images = new HashMap<INode, Map<INode, Operator>>();
		final Set<INode> seen = new HashSet<INode>();
		for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
			final INode v = (INode) nodes.next();
			if (!seen.contains(v)) {
				final Point bv = (Point) getGraph().barycentricPlacement().get(
						v);
				final Map<INode, Operator> img2sym =
					new HashMap<INode, Operator>();
				img2sym.put(v, new Operator(Matrix.one(dim + 1)));
				seen.add(v);
				for (final Iterator syms = this.graph.symmetries().iterator(); syms
						.hasNext();) {
					final Morphism a = (Morphism) syms.next();
					final INode va = (INode) a.get(v);
					if (!seen.contains(va)) {
						final Point bva = (Point) getGraph()
								.barycentricPlacement().get(va);
						final Operator opa = a.getAffineOperator();
						final Vector shift = (Vector) bva.minus(bv.times(opa));
						final Operator op = (Operator) opa.times(shift);
						img2sym.put(va, op);
						seen.add(va);
						if (bv.times(op).equals(bva) == false) {
							throw new RuntimeException("Bad operator for " + v
									+ " --> " + va + ": image is "
									+ bv.times(op) + ", but should be " + bva);
						}
					}
				}
				this.node2images.put(v, img2sym);
			}
		}

		// --- translations for Gram matrix parameters
		this.gramIndex = new int[dim][dim];
		int k = 0;
		for (int i = 0; i < dim; ++i) {
			this.gramIndex[i][i] = k++;
			for (int j = i + 1; j < dim; ++j) {
				this.gramIndex[i][j] = this.gramIndex[j][i] = k++;
			}
		}

		// --- set up translation between parameter space and gram matrix
		// entries
		final SpaceGroup group = graph.getSpaceGroup();
		this.gramSpace = group.configurationSpaceForGramMatrix();
		k = this.gramSpace.numberOfRows();

		// --- set up translating parameter space values into point coordinates
		this.node2index = new HashMap<INode, Integer>();
		this.node2mapping = new HashMap<INode, double[][]>();

		for (final Iterator nodeReps = nodeOrbitReps(); nodeReps.hasNext();) {
			final INode v = (INode) nodeReps.next();
			final Matrix N = normalizedPositionSpace(v);

			this.node2index.put(v, new Integer(k));
			this.node2mapping.put(v, N.asDoubleArray());
			final Map images = images(v);
			for (final Iterator iter = images.keySet().iterator(); iter
					.hasNext();) {
				final INode w = (INode) iter.next();
				if (w == v) {
					continue;
				}
				final Matrix M = ((Operator) images.get(w)).getCoordinates();
				this.node2index.put(w, new Integer(k));
				final Matrix NM = (Matrix) N.times(M);
				this.node2mapping.put(w, NM.asDoubleArray());
				if (NM.times(symmetrizer(w).getCoordinates()).equals(NM) == false) {
					throw new RuntimeException("bad parameter space for " + w
							+ ": " + NM + " (gets 'symmetrized' to "
							+ NM.times(symmetrizer(w)));
				}
			}
			k += N.numberOfRows() - 1;
		}
		this.dimParSpace = k;

		// --- the encoded list of graph edge orbits
		final List<Edge> edgeList = new ArrayList<Edge>();
		for (final Iterator iter = graph.edgeOrbits(); iter.hasNext();) {
			final Set orbit = (Set) iter.next();
			final IEdge e = (IEdge) orbit.iterator().next();
			edgeList.add(new Edge(e.source(), e.target(), graph.getShift(e),
					EDGE, orbit.size()));
		}

		// --- the encoded list of next nearest neighbor (angle) orbits
		for (final Iterator iter = angleOrbits(); iter.hasNext();) {
			final Set orbit = (Set) iter.next();
			final Angle a = (Angle) orbit.iterator().next();
			edgeList.add(new Edge(a.v, a.w, a.s, ANGLE, orbit.size()));
		}

		this.edges = new Edge[edgeList.size()];
		edgeList.toArray(this.edges);

		// --- initialize the parameter vector
		this.p = new double[this.dimParSpace];

		// --- set initial positions and cell parameters
		setPositions(null);
		setGramMatrix(null);
	}

	public int degreesOfFreedom() {
		return dimParSpace - graph.getSpaceGroup().shiftSpace().length;
	}

	private Matrix normalizedPositionSpace(final INode v) {
		// --- get the affine subspace that is stabilized by the nodes
		// stabilizer
		final Operator s = this.symmetrizer(v);
		final Matrix A = (Matrix) s.getCoordinates().minus(
				Matrix.one(this.dimGraph + 1));
		final Matrix M = LinearAlgebra.rowNullSpace(A, false);

		final Matrix N = normalizedPositionSpace(M, true);

		if (N.times(s.getCoordinates()).equals(N) == false) {
			throw new RuntimeException("bad parameter space for " + v + ": "
					+ N + " (gets 'symmetrized' to "
					+ N.times(s.getCoordinates()));
		}

		return N;
	}

	private Matrix normalizedPositionSpace(final Matrix M,
			final boolean movePivot) {
		// --- get the dimensions of the matrix
		final int n = M.numberOfRows();
		final int d = M.numberOfColumns();

		// --- make a local copy to work with
		final Matrix N = M.mutableClone();

		// --- not sure if this is of any use, but do it anyway
		Matrix.triangulate(N, null, false, true);

		if (movePivot) {
			// --- move pivot for last column into last row
			for (int i = 0; i < n - 1; ++i) {
				if (N.get(i, d - 1).isZero() == false) {
					final Matrix tmp = N.getRow(n - 1);
					N.setRow(n - 1, N.getRow(i));
					N.setRow(i, tmp);
					break;
				}
			}
		}

		// --- normalize the pivot
		N
				.setRow(n - 1, (Matrix) N.getRow(n - 1).dividedBy(
						N.get(n - 1, d - 1)));

		// --- make last column 0 in all other rows
		for (int i = 0; i < n - 1; ++i) {
			N.setRow(i, (Matrix) N.getRow(i).minus(
					N.getRow(n - 1).times(N.get(i, d - 1))));
		}

		// --- that's it
		return N;
	}

	/**
	 * Returns the orbits of the set of angles under the full combinatorial
	 * symmetry group.
	 * 
	 * @return an iterator over the set of orbits.
	 */
	private Iterator angleOrbits() {
		final Set<Angle> angles = new HashSet<Angle>();
		for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
			final INode v = (INode) nodes.next();
			final List incidences = this.graph.allIncidences(v);
			final int n = incidences.size();
			for (int i = 0; i < n - 1; ++i) {
				final IEdge e1 = (IEdge) incidences.get(i);
				final INode w1 = e1.target();
				for (int j = i + 1; j < n; ++j) {
					final IEdge e2 = (IEdge) incidences.get(j);
					final INode w2 = e2.target();
					final Vector s = (Vector) this.graph.getShift(e2).minus(
							this.graph.getShift(e1));
					angles.add(new Angle(w1, w2, s));
				}
			}
		}

		final Partition P = new Partition();
		final Map pos = getGraph().barycentricPlacement();
		for (final Iterator syms = getGraph().symmetries().iterator(); syms
				.hasNext();) {
			final Morphism phi = (Morphism) syms.next();
			final Operator A = phi.getAffineOperator();
			for (final Iterator iter = angles.iterator(); iter.hasNext();) {
				final Angle a = (Angle) iter.next();

				final INode vphi = (INode) phi.get(a.v);
				final Point pv = (Point) pos.get(a.v);
				final Vector dv = (Vector) pv.times(A).minus(pos.get(vphi));

				final INode wphi = (INode) phi.get(a.w);
				final Point pw = (Point) pos.get(a.w);
				final Vector dw = (Vector) pw.times(A).minus(pos.get(wphi));

				final Vector s = (Vector) a.s.times(A).plus(dw).minus(dv);

				P.unite(a, new Angle(vphi, wphi, s));
			}
		}
		return P.classes();
	}

	private void setPosition(final INode v, final Point pos,
			final double state[]) {
		final Matrix mapping = new Matrix((double[][]) this.node2mapping.get(v))
				.mutableClone();
		final int n = mapping.numberOfRows();

		if (n > 1) {
			final int d = this.dimGraph;
			final Matrix q = new Matrix(1, d + 1);
			q.setSubMatrix(0, 0, ((Point) pos.times(symmetrizer(v)))
					.getCoordinates());
			q.set(0, d, Whole.ONE);
			final Matrix r = mapping.getRow(n - 1);
			mapping.setRow(n - 1, (Matrix) r.minus(r));
			final Matrix s = LinearAlgebra.solutionInRows(mapping, (Matrix) q
					.minus(r), false);
			if (s == null) {
				throw new RuntimeException("Could not solve x * " + mapping
						+ " = " + q);
			}

			final int offset = ((Integer) this.node2index.get(v)).intValue();
			for (int i = 0; i < n - 1; ++i) {
				state[offset + i] = ((Real) s.get(0, i)).doubleValue();
			}
		}
		final Vector diff = (Vector) getPosition(v).minus(pos);
		if (((Real) Vector.dot(diff, diff)).sqrt().doubleValue() > 1e-12) {
			throw new RuntimeException("Position mismatch:" + v + " set to "
					+ pos + ", but turned up as " + getPosition(v));
		}
	}

	private double[] getPosition(final INode v, final double state[]) {
		final int d = this.dimGraph;
		final int offset = ((Integer) this.node2index.get(v)).intValue();
		final double mapping[][] = (double[][]) this.node2mapping.get(v);
		final int n = mapping.length;

		double loc[] = new double[d];
		for (int i = 0; i < d; ++i) {
			loc[i] = mapping[n - 1][i];
			for (int j = 0; j < n - 1; ++j) {
				loc[i] += state[offset + j] * mapping[j][i];
			}
		}
		return loc;
	}

	private double[] gramToArray(final Matrix gramMatrix) {
		final int d = this.dimGraph;
		final double p[] = new double[d * (d + 1) / 2];
		for (int i = 0; i < d; ++i) {
			for (int j = i; j < d; ++j) {
				final int k = this.gramIndex[i][j];
				p[k] = ((Real) gramMatrix.get(i, j)).doubleValue();
			}
		}
		return p;
	}

	private Matrix gramFromArray(final double p[]) {
		final int d = this.dimGraph;
		final Matrix gram = new Matrix(d, d);

		for (int i = 0; i < d; ++i) {
			for (int j = i; j < d; ++j) {
				final int k = this.gramIndex[i][j];
				final Real x = new FloatingPoint(p[k]);
				gram.set(i, j, x);
				gram.set(j, i, x);
			}
		}
		return gram;
	}

	private void setGramMatrix(final Matrix gramMatrix, final double p[]) {
		final double g[] = gramToArray(gramMatrix);
		final Matrix S = this.gramSpace;
		final int n = S.numberOfRows();
		final Matrix M = LinearAlgebra.solutionInRows(S, new Matrix(
				new double[][] { g }), false);
		for (int i = 0; i < n; ++i) {
			p[i] = ((Real) M.get(0, i)).doubleValue();
		}
	}

	private Matrix getGramMatrix(final double p[]) {
		final int d = this.dimGraph;
		final int m = d * (d + 1) / 2;

		// --- extract the data for the Gram matrix
		final double g[] = new double[m];
		final Matrix S = this.gramSpace;
		final int n = S.numberOfRows();
		for (int i = 0; i < m; ++i) {
			g[i] = 0.0;
			for (int j = 0; j < n; ++j) {
				g[i] += p[j] * ((Real) S.get(j, i)).doubleValue();
			}
		}
		final Matrix gram = gramFromArray(g);

		// --- adjust the gram matrix
		for (int i = 0; i < d; ++i) {
			if (gram.get(i, i).isNegative()) {
				gram.set(i, i, FloatingPoint.ZERO);
			}
		}
		for (int i = 0; i < d; ++i) {
			for (int j = i + 1; j < d; ++j) {
				final Real t = ((Real) gram.get(i, i).times(gram.get(j, j)))
						.sqrt();
				if (gram.get(i, j).isGreaterThan(t)) {
					gram.set(i, j, t);
					gram.set(j, i, t);
				}
			}
		}
		return gram;
	}

	// --- the following methods do the actual optimization

	private double energy(final double point[]) {
		// --- get some general data
		final int dim = this.dimGraph;
		final int n = getGraph().numberOfNodes();

		// --- extract and adjust the Gram matrix
		final Matrix gram = getGramMatrix(point);

		// --- make it an easy to read array
		final double g[] = gramToArray(gram);

		// --- use our original coordinates if only cell is relaxed
		final double p[];
		if (getRelaxPositions() == false) {
			p = this.p;
		} else {
			p = point;
		}

		// --- compute variance of squared edge lengths
		double edgeSum = 0.0;
		double edgeWeightSum = 0.0;
		double minEdge = Double.MAX_VALUE;

		for (int k = 0; k < this.edges.length; ++k) {
			final Edge e = this.edges[k];
			final double pv[] = getPosition(e.v, p);
			final double pw[] = getPosition(e.w, p);
			final double s[] = e.shift;
			final double diff[] = new double[dim];
			for (int i = 0; i < dim; ++i) {
				diff[i] = pw[i] + s[i] - pv[i];
			}
			double len = 0.0;
			for (int i = 0; i < dim; ++i) {
				len += diff[i] * diff[i] * g[this.gramIndex[i][i]];
				for (int j = i + 1; j < dim; ++j) {
					len += 2 * diff[i] * diff[j] * g[this.gramIndex[i][j]];
				}
			}
			len = Math.sqrt(len);
			e.length = len;
			if (e.type == EDGE) {
				edgeSum += len * e.weight;
				;
				edgeWeightSum += e.weight;
				minEdge = Math.min(len, minEdge);
			}
		}
		final double avg = edgeSum / edgeWeightSum;
		if (edgeWeightSum != getGraph().numberOfEdges()) {
			System.out.println("edgeWeightSum is " + edgeWeightSum
					+ ", but should be " + getGraph().numberOfEdges());
		}
		final double scaling = 1.01 / avg;

		double edgeVariance = 0.0;
		double edgePenalty = 0.0;
		double anglePenalty = 0.0;
		for (int k = 0; k < this.edges.length; ++k) {
			final Edge e = this.edges[k];
			final double len = e.length * scaling;
			final double penalty;
			if (len < 0.5) {
				final double x = Math.max(len, 1e-12);
				penalty = Math.exp(Math.tan((0.25 - x) * 2.0 * Math.PI))
						* e.weight;
			} else {
				penalty = 0.0;
			}
			if (e.type == EDGE) {
				final double t = (1 - len * len);
				edgeVariance += t * t * e.weight;
				edgePenalty += penalty;
			} else {
				anglePenalty += penalty;
			}
		}
		edgeVariance /= edgeWeightSum;
		if (edgeVariance < 0) {
			throw new RuntimeException("edge lengths variance got negative: "
					+ edgeVariance);
		}

		// --- compute volume per node
		final Matrix gramScaled = (Matrix) gram.times(scaling * scaling);
		final double cellVolume = Math.sqrt(((Real) gramScaled.determinant())
				.doubleValue());
		final double volumePenalty = Math.exp(1 / Math.max(cellVolume / n,
				1e-12)) - 1;

		// --- compute and return the total energy
		return this.volumeWeight * volumePenalty + edgeVariance +
			   this.penaltyFactor * (edgePenalty + anglePenalty);
	}

	private Map<INode, Operator> nodeSymmetrizations() {
		final Map<INode, Operator> result = new HashMap<INode, Operator>();
		for (final Iterator nodes = this.graph.nodes(); nodes.hasNext();) {
			final INode v = (INode) nodes.next();
			final List stab = this.graph.nodeStabilizer(v);
			final Point p = (Point) this.graph.barycentricPlacement().get(v);
			final int dim = p.getDimension();
			Matrix s = Matrix.zero(dim + 1, dim + 1);
			for (final Iterator syms = stab.iterator(); syms.hasNext();) {
				final Operator a = ((Morphism) syms.next()).getAffineOperator();
				final Vector d = (Vector) p.minus(p.times(a));
				final Operator ad = (Operator) a.times(d);
				s = (Matrix) s.plus(ad.getCoordinates());
			}

			final Operator op = new Operator(s);
			if (p.times(op).equals(p) == false) {
				throw new RuntimeException("Bad symmetrizer for " + v
						+ ": moves from " + p + " to " + p.times(op));
			}
			result.put(v, op);
		}

		return result;
	}

	private double length(final IEdge e) {
		final Point p = getPosition(e.source());
		final Point q = getPosition(e.target());
		final Vector s = this.graph.getShift(e);
		return length((Vector) q.plus(s).minus(p));
	}

	private double length(final Vector v) {
		final Real squareLength = (Real) Vector.dot(v, v, getGramMatrix());
		return Math.sqrt(squareLength.doubleValue());
	}

	private Iterator nodeOrbitReps() {
		return this.node2images.keySet().iterator();
	}

	private Map images(final INode v) {
		return (Map) this.node2images.get(v);
	}

	private Operator symmetrizer(final INode v) {
		return (Operator) this.node2sym.get(v);
	}

	private static Matrix defaultGramMatrix(final PeriodicGraph graph) {
		return resymmetrized(Matrix.one(graph.getDimension()), graph);
	}

	private static Matrix resymmetrized(final Matrix G,
			final PeriodicGraph graph) {
		// -- preparations
		final int d = graph.getDimension();
		final Set syms = graph.symmetries();

		// --- compute a symmetry-invariant quadratic form
		Matrix M = Matrix.zero(d, d);
		for (final Iterator iter = syms.iterator(); iter.hasNext();) {
			final Matrix A = ((Morphism) iter.next()).getLinearOperator()
					.getCoordinates().getSubMatrix(0, 0, d, d);
			M = (Matrix) M.plus(A.times(G).times(A.transposed()));
		}
		M = ((Matrix) M.times(new Fraction(1, syms.size()))).mutableClone();
		for (int i = 0; i < d; ++i) {
			for (int j = i + 1; j < d; ++j) {
				M.set(j, i, M.get(i, j));
			}
		}
		return M;
	}

	public void normalize() {
		final double avg = averageEdgeLength();
		if (avg < 1e-3) {
			throw new RuntimeException("degenerate unit cell while relaxing");
		}
		setGramMatrix((Matrix) getGramMatrix().dividedBy(avg * avg));
	}

	// --- the public interface for this class

	public PeriodicGraph getGraph() {
		return this.graph;
	}

	public int getPasses() {
		return this.passes;
	}

	public void setPasses(int relaxPasses) {
		this.passes = relaxPasses;
	}

	public void setRelaxPositions(boolean optimizePositions) {
		this.optimizePositions = optimizePositions;
	}

	public boolean getRelaxPositions() {
		return optimizePositions;
	}

	public void setGramMatrix(Matrix gramMatrix) {
		if (gramMatrix == null) {
			gramMatrix = defaultGramMatrix(this.graph);
		}
		setGramMatrix(gramMatrix, this.p);
	}

	public Matrix getGramMatrix() {
		return getGramMatrix(this.p);
	}

	public void setPosition(final INode v, final Point p) {
		setPosition(v, p, this.p);
	}

	public void setPositions(Map map) {
		if (map == null) {
			map = this.graph.barycentricPlacement();
		}
		for (final Iterator nodes = map.keySet().iterator(); nodes.hasNext();) {
			final INode v = (INode) nodes.next();
			setPosition(v, (Point) map.get(v));
		}
	}

	public Point getPosition(final INode v) {
		return new Point(getPosition(v, this.p));
	}

	public Map<INode, Point> getPositions() {
		final Map<INode, Point> pos = new HashMap<INode, Point>();
		for (final Iterator nodes = getGraph().nodes(); nodes.hasNext();) {
			final INode v = (INode) nodes.next();
			pos.put(v, getPosition(v));
		}
		return pos;
	}

	public int go(final int steps) {
		if (dimParSpace == 0) {
			this._positionsRelaxed = getRelaxPositions();
			this._cellRelaxed = true;
			return 0;
		}

		final Amoeba.Function energy = new Amoeba.Function() {
			public int dim() {
				if (getRelaxPositions()) {
					return dimParSpace;
				} else {
					return gramSpace.numberOfRows();
				}
			}

			public double evaluate(final double[] p) {
				return energy(p);
			}
		};

		// --- here's the relaxation procedure
		double p[] = this.p;
		final int nrPasses = Math.max(1, this.passes);

		for (int pass = 0; pass < nrPasses; ++pass) {
			this.volumeWeight = Math.pow(10, -pass);
			this.penaltyFactor = (pass == nrPasses - 1) ? 1 : 0;
			p = new Amoeba(energy, 1e-6, steps, 10, 1.0).go(p);
			for (int i = 0; i < p.length; ++i) {
				this.p[i] = p[i];
			}
		}
		this._positionsRelaxed = getRelaxPositions();
		this._cellRelaxed = true;
		return steps;
	}

	public void reset() {
		setPositions(null);
		setGramMatrix(null);
		this._positionsRelaxed = false;
		this._cellRelaxed = false;
	}

	public boolean positionsRelaxed() {
		return this._positionsRelaxed;
	}

	public boolean cellRelaxed() {
		return this._cellRelaxed;
	}

	public double maximalEdgeLength() {
		double maxLength = 0.0;
		for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
			maxLength = Math.max(maxLength, length((IEdge) edges.next()));
		}
		return maxLength;
	}

	public double minimalEdgeLength() {
		double minLength = Double.MAX_VALUE;
		for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
			minLength = Math.min(minLength, length((IEdge) edges.next()));
		}
		return minLength;
	}

	public double averageEdgeLength() {
		double sumLength = 0.0;
		int count = 0;
		for (final Iterator edges = this.graph.edges(); edges.hasNext();) {
			sumLength += length((IEdge) edges.next());
			++count;
		}
		return sumLength / count;
	}
}

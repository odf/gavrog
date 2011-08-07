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


package org.gavrog.joss.dsyms.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.gavrog.box.collections.Iterators;
import org.gavrog.jane.compounds.LinearAlgebra;
import org.gavrog.jane.compounds.Matrix;
import org.gavrog.jane.numbers.Real;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSPair;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.geometry.CoordinateChange;
import org.gavrog.joss.geometry.Lattices;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.embed.Embedder;
import org.gavrog.joss.tilings.Tiling;

/**
 * @author Olaf Delgado
 * @version $Id: EvolverExporter.java,v 1.8 2008/05/21 07:57:24 odf Exp $
 */
public class EvolverExporter {
    static {
        Locale.setDefault(Locale.US);
    }
	final private static NumberFormat fmt = new DecimalFormat("##0.000000000");
	
	final private Tiling til;
	final private Tiling.Skeleton net;
	final private Embedder embedder;
	final private CoordinateChange embedderToWorld;
	final private Map pos;
	final private double cell[][];
	final private CoordinateChange worldToCell;
	
	private String head = null;
	private String tail = null;
	private boolean skipPeriods = false;
	private boolean unitVolumes = false;
	
	public EvolverExporter(final Tiling til) {
		this.til = til;
		this.net = til.getSkeleton();
		
		// --- compute an embedding
		this.embedder = new Embedder(this.net);
        this.embedder.reset();
        this.embedder.setPasses(1);
        if (this.net.isStable()) {
            this.embedder.setRelaxPositions(false);
            this.embedder.go(500);
        }
        this.embedder.setRelaxPositions(true);
        this.embedder.go(10000);
        this.embedder.normalize();
        final Matrix gram = this.embedder.getGramMatrix();
        this.embedderToWorld = new CoordinateChange(LinearAlgebra
				.orthonormalRowBasis(gram));
        this.pos = this.til.cornerPositions(this.embedder.getPositions());
        
        // --- compute an appropriate unit cell
		final int dim = til.getSymbol().dim();
		final Matrix I = Matrix.one(dim);
		final Vector basis[] = new Vector[dim];
		for (int i = 0; i < dim; ++i) {
			basis[i] = (Vector) Vector.unit(dim, i).times(this.embedderToWorld);
		}
		final Vector tvecs[] = Lattices.reducedLatticeBasis(basis, I);
		this.cell = new double[dim][dim];
		for (int i = 0; i < dim; ++i) {
			for (int j = 0; j < dim; ++j) {
				cell[i][j] = ((Real) tvecs[i].get(j)).doubleValue();
			}
		}
		this.worldToCell = new CoordinateChange(Vector.toMatrix(tvecs));
	}
	
	private double[] vertexShift(final double p[]) {
		final int dim = p.length;
		final CoordinateChange C = this.worldToCell;
		final Point v = (Point) new Point(p).times(C);
		final Whole a[] = new Whole[dim];
		for (int i = 0; i < dim; ++i) {
			final Real x = (Real) v.get(i);
			a[i] = (Whole) x.plus(0.001).mod(Whole.ONE).minus(x).round();
		}
		final Vector w = (Vector) new Vector(a).times(C.inverse());
		return w.getCoordinates().asDoubleArray()[0];
	}
	
	private double volume(final double c[][]) {
		return    c[0][0] * c[1][1] * c[2][2] - c[0][2] * c[1][1] * c[2][0]
				+ c[0][1] * c[1][2] * c[2][0] - c[0][0] * c[1][2] * c[2][1]
				+ c[0][2] * c[1][0] * c[2][1] - c[0][1] * c[1][0] * c[2][2];
	}
	
	private double chamberVolume(final Object D) {
		final double c[] = cornerPosition(3, D);
		final double v[][] = new double[3][3];
		for (int i = 0; i < 3; ++i) {
			final double p[] = cornerPosition(i, D);
			for (int j = 0; j < 3; ++j) {
				v[i][j] = p[j] - c[j];
			}
		}
		return volume(v);
	}
	
    public double[] cornerPosition(final int i, final Object D) {
        final Point p0 = (Point) this.pos.get(new DSPair(i, D));
        final Point p = (Point) p0.times(this.embedderToWorld);
        return p.getCoordinates().asDoubleArray()[0];
    }
    
	public void writeTo(final Writer writer) throws IOException {
		final BufferedWriter outf = new BufferedWriter(writer);
	    final List tiles = this.til.getTiles();
	    final double vol = volume(this.cell) / tiles.size();
	    final double tvol;
	    if (getUnitVolumes()) {
	    	tvol = 1.0;
	    } else {
	    	tvol = 1.0 / tiles.size();
	    }
		final double scale = Math.pow(Math.abs(vol / tvol), -1.0 / 3.0);
		
		// --- write the initial unit cell vectors
	    outf.write("parameter p1x = " + fmt.format(cell[0][0] * scale) + '\n');
		outf.write("parameter p1y = " + fmt.format(cell[0][1] * scale) + '\n');
		outf.write("parameter p1z = " + fmt.format(cell[0][2] * scale) + '\n');
		outf.write("parameter p2x = " + fmt.format(cell[1][0] * scale) + '\n');
		outf.write("parameter p2y = " + fmt.format(cell[1][1] * scale) + '\n');
		outf.write("parameter p2z = " + fmt.format(cell[1][2] * scale) + '\n');
		outf.write("parameter p3x = " + fmt.format(cell[2][0] * scale) + '\n');
		outf.write("parameter p3y = " + fmt.format(cell[2][1] * scale) + '\n');
		outf.write("parameter p3z = " + fmt.format(cell[2][2] * scale) + '\n');
	    outf.write('\n');
	    
	    // --- optionally write a header include instruction
	    if (head != null) {
	    	outf.write("#include \"" + head + "\"\n");
		    outf.write('\n');
	    }
	    
	    // --- optionally set the periods using the parameters written above
	    if (!getSkipPeriods()) {
		    outf.write("torus_filled\n");
		    outf.write('\n');
		    outf.write("periods\n");
		    outf.write("p1x p1y p1z\n");
		    outf.write("p2x p2y p2z\n");
		    outf.write("p3x p3y p3z\n");
		    outf.write('\n');
	    }
	    
	    // --- write the vertices
	    outf.write("vertices\n");
	    final List nodes = new ArrayList();
	    nodes.add(null);
	    Iterators.addAll(nodes, this.net.nodes());
	    final double[][] shifts = new double[nodes.size()][];
	    final Map nodeNumbers = new HashMap();
	    int i = 0;
	    for (final Iterator iter = nodes.iterator(); iter.hasNext();) {
	    	final INode v = (INode) iter.next();
	    	if (v == null) {
	    		continue;
	    	}
	    	nodeNumbers.put(v, new Integer(++i));
	    	final Object D = this.net.chamberAtNode(v);
	    	final double p[] = cornerPosition(0, D);
	    	final double s[] = vertexShift(p);
	    	shifts[i] = s;
	    	
	    	outf.write(i + "  ");
	    	for (int k = 0; k < 3; ++k) {
	    		outf.write(" " + fmt.format((p[k] + s[k]) * scale));
	    	}
	    	outf.write('\n');
	    }
	    outf.write('\n');
	    
	    // --- write the edges
	    outf.write("edges\n");
	    final CoordinateChange e2w = this.embedderToWorld;
	    final CoordinateChange w2c = (CoordinateChange) this.worldToCell;
	    final List edges = new ArrayList();
	    edges.add(null);
	    Iterators.addAll(edges, this.net.edges());
	    final Map edgeNumbers = new HashMap();
	    i = 0;
	    for (final Iterator iter = edges.iterator(); iter.hasNext();) {
	    	final IEdge e = (IEdge) iter.next();
	    	if (e == null) {
	    		continue;
	    	}
	    	edgeNumbers.put(e, new Integer(++i));
	    	final int v = ((Integer) nodeNumbers.get(e.source())).intValue();
	    	final int w = ((Integer) nodeNumbers.get(e.target())).intValue();
	    	outf.write(i + "  " + v + ' ' + w + ' ');
	    	final Vector se = (Vector) this.net.getShift(e).times(e2w);
	    	final Vector sv = new Vector(shifts[v]);
	    	final Vector sw = new Vector(shifts[w]);
	    	final Vector s = (Vector) se.plus(sv).minus(sw).times(w2c);
	    	for (int k = 0; k < 3; ++k) {
	    		final Whole x = (Whole) s.get(k).round();
	    		if (x.isZero()) {
	    			outf.write(" *");
	    		} else if (x.isOne()) {
	    			outf.write(" +");
	    		} else if (x.negative().isOne()) {
	    			outf.write(" -");
	    		} else {
	    			outf.write(" " + x);
//	    			throw new RuntimeException("Illegal shift vector " + s);
	    		}
	    	}
	    	outf.write('\n');
	    }
	    outf.write('\n');
	    
	    // --- write the faces
	    outf.write("faces\n");
	    final DSymbol cover = this.til.getCover();
	    final Map ch2faceNr = new HashMap();
	    final Iterator iterF = cover.orbitReps(new IndexList(0, 1, 3));
	    i = 0;
	    while (iterF.hasNext()) {
	    	final Object entry = iterF.next();
	    	final Object D0;
	    	if (chamberVolume(entry) > 1e-3) {
	    		D0 = entry;
	    	} else if (chamberVolume(entry) < -1e-3){
	    		D0 = cover.op(0, entry);
	    	} else {
	    		throw new RuntimeException("degenerate chamber found");
	    	}
	    	outf.write(++i + " ");
	    	Object D = D0;
	    	while (true) {
	    		final IEdge e = this.net.edgeForChamber(D);
	    		final int k = ((Integer) edgeNumbers.get(e)).intValue();
	    		if (e.oriented().equals(((IEdge) edges.get(k)).oriented())) {
		    		outf.write(" " + k);
	    		} else {
		    		outf.write(" " + (-k));
	    		}
	    		ch2faceNr.put(D, new Integer(i));
	    		ch2faceNr.put(cover.op(3, D), new Integer(-i));
	    		D = cover.op(0, D);
	    		ch2faceNr.put(D, new Integer(i));
	    		ch2faceNr.put(cover.op(3, D), new Integer(-i));
	    		D = cover.op(1, D);
	    		if (D0.equals(D)) {
	    			break;
	    		}
	    	}
		    outf.write('\n');
	    }
	    outf.write('\n');
	    
	    // --- write the bodies
	    outf.write("bodies\n");
	    i = 0;
	    for (Iterator iter = this.til.getTiles().iterator(); iter.hasNext();) {
	    	final Tiling.Tile t = (Tiling.Tile) iter.next();
	    	outf.write(++i + " ");
	    	for (int k = 0; k < t.size(); ++k) {
	    		final Object D = t.facet(k).getChamber();
	    		final int n = ((Integer) ch2faceNr.get(D)).intValue();
	    		outf.write(" " + n);
	    	}
	    	outf.write("   volume " + fmt.format(tvol) + "\n");
	    }
	    outf.write('\n');
	    
	    // --- optionally write a tail include instruction
	    if (tail != null) {
	    	outf.write("#include \"" + tail + "\"\n");
		    outf.write('\n');
	    }
	    
	    // --- we're using a buffered writer, so flushing is crucial
	    outf.flush();
	}
	
	public String getHead() {
		return this.head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public String getTail() {
		return this.tail;
	}

	public void setTail(String tail) {
		this.tail = tail;
	}

	public boolean getSkipPeriods() {
		return this.skipPeriods;
	}

	public void setSkipPeriods(boolean skipPeriods) {
		this.skipPeriods = skipPeriods;
	}

	public void toggleSkipPeriods() {
		this.skipPeriods = !this.skipPeriods;
	}

	public boolean getUnitVolumes() {
		return this.unitVolumes;
	}

	public void setUnitVolumes(boolean unitVolumes) {
		this.unitVolumes = unitVolumes;
	}

	public void toggleUnitVolumes() {
		this.unitVolumes = !this.unitVolumes;
	}

	public static void main(final String args[]) {
		String head = null;
		String tail = null;
		boolean skipPeriods = false;
		boolean unitVolumes = false;
		
		int i = 0;
		while (i < args.length && args[i].startsWith("-")) {
			if (args[i].equals("-p")) {
				skipPeriods = !skipPeriods;
			} else if (args[i].equals("-u")) {
				unitVolumes = !unitVolumes;
			} else if (args[i].equals("-h")) {
				head = args[++i];
			} else if (args[i].equals("-t")) {
				tail = args[++i];
			} else {
				System.err.println("Unknown option '" + args[i] + "'");
			}
			++i;
		}
		
		final String name = args[i];
		final String base;
		if (name.endsWith(".ds")) {
			base = name.substring(0, name.length() - 3);
		} else {
			base = name;
		}
		
		final NumberFormat suffix = new DecimalFormat("-000.fe");
		int k = 0;
		for (Iterator it = new InputIterator(name); it.hasNext();) {
			final DSymbol ds = (DSymbol) it.next();
			final Tiling til = new Tiling(ds);
			
			final EvolverExporter exporter = new EvolverExporter(til);
			exporter.setHead(head);
			exporter.setTail(tail);
			exporter.setSkipPeriods(skipPeriods);
			exporter.setUnitVolumes(unitVolumes);
			final String outname = base + suffix.format(++k);
			try {
				final FileWriter out = new FileWriter(outname);
				exporter.writeTo(out);
				out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}

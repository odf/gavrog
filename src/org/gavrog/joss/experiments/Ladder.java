/*
   Copyright 2006 Olaf Delgado-Friedrichs

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

package org.gavrog.joss.experiments;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gavrog.box.collections.Partition;
import org.gavrog.joss.geometry.Operator;
import org.gavrog.joss.geometry.Point;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.Morphism;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.pgraphs.io.Net;
import org.gavrog.joss.pgraphs.io.NetParser;
import org.gavrog.joss.pgraphs.io.Output;

public class Ladder {
	private final PeriodicGraph graph;
	private final Partition rungPartition;
	private final List stileMaps;
	
    public Ladder(final PeriodicGraph G) {
        // --- check prerequisites
        if (!G.isConnected()) {
            throw new UnsupportedOperationException("graph must be connected");
        }
        if (!G.isLocallyStable()) {
            throw new UnsupportedOperationException("graph must be locally stable");
        }
        
        // --- remember the graph given
        this.graph = G;
        
        // --- find equivalence classes w.r.t. ladder translations
        final Operator I = Operator.identity(G.getDimension());
        final Partition P = new Partition();
        final Iterator iter = G.nodes();
        final INode start = (INode) iter.next();
        final Map pos = G.barycentricPlacement();
        final Point pos0 = (Point) pos.get(start);
        
        while (iter.hasNext()) {
			final INode v = (INode) iter.next();
			final Point posv = (Point) pos.get(v);
			if (!((Vector) posv.minus(pos0)).modZ().isZero()) {
				continue;
			}
			if (P.areEquivalent(start, v)) {
				continue;
			}
			final Morphism iso;
			try {
				iso = new Morphism(start, v, I);
			} catch (Morphism.NoSuchMorphismException ex) {
				continue;
			}
			boolean hasFixedPoints = false;
			for (final Iterator it = G.nodes(); it.hasNext();) {
				final INode w = (INode) it.next();
				final INode u = (INode) iso.get(w);
				if (w.equals(u)) {
					hasFixedPoints = true;
					break;
				}
			}
			if (hasFixedPoints) {
				continue;
			}
			for (final Iterator it = G.nodes(); it.hasNext();) {
				final INode w = (INode) it.next();
				P.unite(w, iso.get(w));
			}
		}
        this.rungPartition = P;
        
        // --- collect morphisms from the start node to each node in its rung
		final List maps = new LinkedList();
        for (final Iterator classes = P.classes(); classes.hasNext();) {
        	final Set A = (Set) classes.next();
        	if (A.contains(start)) {
        		for (final Iterator nodes = A.iterator(); nodes.hasNext();) {
        			final INode v = (INode) nodes.next();
        			maps.add(new Morphism(start, v, I));
        		}
    			break;
        	}
		}
		this.stileMaps = maps;
	}

    /**
	 * @return the graph
	 */
	public PeriodicGraph getGraph() {
		return graph;
	}

	/**
	 * @return the stileMaps
	 */
	public List getStileMaps() {
		return stileMaps;
	}

	/**
	 * @return the rungPartition
	 */
	public Partition getRungPartition() {
		return rungPartition;
	}

	public static void main(final String args[]) {
		try {
			final Reader r;
			final Writer w;
			if (args.length > 0) {
				r = new FileReader(args[0]);
			} else {
				r = new InputStreamReader(System.in);
			}
			if (args.length > 1) {
				w = new FileWriter(args[1]);
			} else {
				w = new OutputStreamWriter(System.out);
			}

			final NetParser parser = new NetParser(r);

			while (true) {
				final Net net = parser.parseNet();
				if (net == null) {
					return;
				} else {
					final PeriodicGraph G = net.canonical();
					final Ladder ladder = new Ladder(G);
					Output.writePGR(w, G, net.getName());
					w.write('\n');
					w.write(String.valueOf(ladder.getRungPartition()));
					w.write('\n');
					w.write(String.valueOf(ladder.getStileMaps()));
					w.write("\n\n");
					w.flush();
				}
			}
		} catch (final IOException ex) {
			System.err.print(ex);
			System.exit(1);
		}
	}
}

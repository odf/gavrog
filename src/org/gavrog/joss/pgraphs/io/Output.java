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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.gavrog.box.collections.NiftyList;
import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.geometry.Vector;
import org.gavrog.joss.pgraphs.basic.IEdge;
import org.gavrog.joss.pgraphs.basic.INode;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;

/**
 */
public class Output {
    public static void writePGR(
            final Writer out,
            final PeriodicGraph G,
            final String name) throws IOException {
    	
        out.write("PERIODIC_GRAPH\n");
        if (name != null) {
            out.write("  NAME " + name + "\n");
        }
        final Map<INode, Integer> node2idx = new HashMap<INode, Integer>();
        int i = 0;
        for (final INode v: G.nodes()) {
            node2idx.put(v, ++i);
        }
        final List<NiftyList<Integer>> tmp =
                new LinkedList<NiftyList<Integer>>();
        out.write("  EDGES\n");
        for (final IEdge e: G.edges()) {
            final Integer v = node2idx.get(e.source());
            final Integer w = node2idx.get(e.target());
            final Vector s = G.getShift(e);
            final List<Integer> list = new LinkedList<Integer>();
            final int d = v.compareTo(w);
            if (d > 0 || (d == 0 && s.isNegative())) {
				list.add(w);
				list.add(v);
				for (int k = 0; k < s.getDimension(); ++k) {
					list.add(((Whole) s.get(k).negative()).intValue());
				}
			} else {
				list.add(v);
				list.add(w);
				for (int k = 0; k < s.getDimension(); ++k) {
					list.add(((Whole) s.get(k)).intValue());
				}
			}
			tmp.add(new NiftyList<Integer>(list));
        }
        Collections.sort(tmp);
        for (final List<Integer> e: tmp) {
        	final StringBuffer line = new StringBuffer(20);
        	line.append("    ");
        	line.append(format(e.get(0), true));
        	line.append(' ');
        	line.append(format(e.get(1), true));
        	line.append("   ");
        	for (int k = 2; k < e.size(); ++k) {
        		line.append(' ');
        		line.append(format(e.get(k), false));
        	}
        	line.append('\n');
            out.write(line.toString());
        }
        out.write("END\n");
    }
    
    private static String format(final int n, final boolean isIndex) {
    	final StringBuffer tmp = new StringBuffer(5);
		if (isIndex) {
			if (n < 10) {
				tmp.append(' ');
			}
			if (n < 100) {
				tmp.append(' ');
			}
		} else {
			if (n >= 0) {
				tmp.append(' ');
			}
		}
		tmp.append(n);
    	return tmp.toString();
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
				final Net G = parser.parseNet();
				if (G == null) {
					return;
				} else {
					Output.writePGR(w, G.canonical(), G.getName());
					w.write('\n');
					w.flush();
				}
			}
		} catch (final IOException ex) {
			System.err.print(ex);
			System.exit(1);
		}
	}
}

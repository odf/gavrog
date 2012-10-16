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

package org.gavrog.joss.dsyms.filters;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import org.gavrog.box.collections.NiftyList;
import org.gavrog.box.simple.Misc;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.generators.InputIterator;
import org.gavrog.joss.pgraphs.basic.PeriodicGraph;
import org.gavrog.joss.tilings.Tiling;


/**
 * A tiling is proper if it has the same symmetry as its underlying net.
 */
public class FilterProper {

    public static void main(String[] args) {
		try {
			boolean unique = false;
			boolean canonical = false;
			boolean dualize = false;
			boolean verbose = false;
			
	        int i = 0;
	        while (i < args.length && args[i].startsWith("-")) {
	        	if (args[i].equalsIgnoreCase("-c")) {
	        		canonical = !canonical;
	        	} else if (args[i].equalsIgnoreCase("-u")){
	        		unique = !unique;
	        	} else if (args[i].equalsIgnoreCase("-d")){
	        		dualize = !dualize;
	        	} else if (args[i].equalsIgnoreCase("-v")) {
	        		verbose = !verbose;
	        	} else {
	        		System.err.println("Unknown option '" + args[i] + "'");
	        	}
	            ++i;
	        }
			
			final Reader in;
			final Writer out;
			if (args.length > i) {
				in = new FileReader(args[i]);
			} else {
				in = new InputStreamReader(System.in);
			}
			if (args.length > i+1) {
				out = new FileWriter(args[i+1]);
			} else {
				out = new OutputStreamWriter(System.out);
			}

			int inCount = 0;
			int outCount = 0;
			final Set<NiftyList<Integer>> seen =
			        new HashSet<NiftyList<Integer>>();

			for (final DSymbol symbol: new InputIterator(in)) {
                ++inCount;
			    final DSymbol ds = dualize ? symbol.dual() : symbol;
				try {
					final DSymbol min = new DSymbol(ds.minimal());
					final DSymbol cov = new DSymbol(Covers
							.pseudoToroidalCover3D(min));
					
					// IMPORTANT: use copy of skeleton to obtain full symmetry
					final Tiling t = new Tiling(cov);
					final PeriodicGraph gr =new PeriodicGraph(t.getSkeleton());
					
					if (!gr.isStable()) {
						if (verbose) {
							System.err.print("# --- Symbol " + inCount
									+ " is not stable.\n");
						}
						continue;
					}
					if (!gr.isMinimal()) {
						if (verbose) {
							System.err.print("# --- Symbol " + inCount
									+ " is not proper - "
									+ "graph has extra translations.\n");
						}
						continue;
					}
					if (unique && seen.contains(gr.invariant())) {
						if (verbose) {
							System.err.print("# --- Symbol " + inCount
									+ " is a duplicate.\n");
						}
						continue;
					}
					if (gr.symmetries().size() != cov.size() / min.size()) {
						if (verbose) {
							System.err.print("# --- Symbol " + inCount
									+ " is not proper - "
									+ gr.symmetries().size() + " vs. "
									+ cov.size() / min.size()
									+ " point ops.\n");
						}
						continue;
					}
					if (verbose) {
						System.err.print("# +++ Symbol " + inCount
								+ " is proper.\n");
					}
					++outCount;
					if (canonical) {
						out.write(ds.toString());
					} else {
						out.write(ds.canonical().flat().toString());
					}
					if (unique) {
						seen.add(gr.invariant());
					}
				} catch (final Exception ex) {
					out.write(Misc.stackTrace(ex, "# "));
					out.write("# in symbol " + ds);
				}
				out.write('\n');
				out.flush();
			}
	        out.write("### Read " + inCount + " and wrote " + outCount
	                + " symbols.\n");
	        out.flush();
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
    }
}

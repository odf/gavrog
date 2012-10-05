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

package org.gavrog.joss.dsyms.filters;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.basic.Subsymbol;
import org.gavrog.joss.dsyms.derived.Covers;
import org.gavrog.joss.dsyms.generators.InputIterator;


/**
 * Extracts the quasi-simple tilings from a file. A tiling is quasi-simple if
 * all the tiles of its dual can be derived from simplices by subdividing each
 * edge at most once.
 * 
 * @author Olaf Delgado
 * @version $Id: FilterQuasiSimple.java,v 1.1 2008/01/16 04:46:17 odf Exp $
 */
public class FilterQuasiSimple {
    public static void main(String[] args) {
		try {
			boolean reverse = false;
			boolean extended = false;
			boolean simple = false;

			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equalsIgnoreCase("-r")) {
					reverse = !reverse;
				} else if (args[i].equalsIgnoreCase("-e")) {
					extended = !extended;
				} else if (args[i].equalsIgnoreCase("-s")) {
					simple = !simple;
				} else {
					System.err.println("Unknown option '" + args[i] + "'");
				}
				++i;
			}

			final String inputPath;
			final Reader in;
			final Writer out;
			if (args.length > i) {
				inputPath = args[i];
				in = new FileReader(inputPath);
			} else {
				inputPath = null;
				in = new InputStreamReader(System.in);
			}
			if (args.length > i + 1) {
				out = new FileWriter(args[i + 1]);
			} else {
				out = new OutputStreamWriter(System.out);
			}
			
			if (inputPath != null) {
				out.write("# Input file: " + inputPath + "\n");
			}
			out.write("# Options:\n");
			out.write("#     Extended: " + extended + "\n");
			out.write("#     Reverse:  " + reverse + "\n");
			
			int inCount = 0;
			int outCount = 0;

			for (final InputIterator input = new InputIterator(in); input
					.hasNext();) {
				final DSymbol ds = (DSymbol) input.next();
				++inCount;
				if (isQuasiSimple(ds, extended, simple) != reverse) {
					++outCount;
					out.write(ds.toString());
					out.write('\n');
					out.flush();
				}
			}

			out.write("# Read " + inCount + " symbols. Extracted " + outCount
					+ " symbols.\n");
			out.flush();
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
    }

    /**
	 * Checks if a symbol encodes a quasi-simple tiling.
	 * 
	 * @param ds
	 *            the input symbol.
	 * @return true or false according to the result of the test.
	 */
    private static boolean isQuasiSimple(final DSymbol ds,
			final boolean extended, final boolean simple) {
		final List iVert = new IndexList(1, 2, 3);
		final List iFace = new IndexList(0, 1);
		for (final Iterator rVert = ds.orbitReps(iVert); rVert.hasNext();) {
			final Object D = rVert.next();
			final DSymbol sub = new DSymbol(new Subsymbol(ds, iVert, D)).dual();
			if (!sub.curvature2D().isPositive()) {
				return false;
			}
			final DSymbol cov = Covers.finiteUniversalCover(sub);
			for (final int E0: cov.orbitReps(iFace)) {
				int E = E0;
				int d = 0;
				do {
					final int m = cov.m(1, 2, E);
					E = cov.op(0, cov.op(1, E));
					if (m == 3) {
						++d;
					} else if (simple || m != 2
							|| (!extended && cov.m(1, 2, E) == 2)) {
						return false;
					}
				} while (E0 != E);
				if (d != 3) {
					return false;
				}
			}
		}
		return true;
	}
}

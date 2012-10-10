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


package org.gavrog.joss.dsyms.derived;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gavrog.jane.numbers.Whole;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.generators.InputIterator;

/**
 */
public class OrbifoldInvariant<T> {
	final private DelaneySymbol<T> ds;
	final private OrbifoldGraph ograph;
	final private List<Whole> abelian;

	public OrbifoldInvariant(final DelaneySymbol<T> ds) {
		this.ds = ds;
		this.ograph = new OrbifoldGraph(ds);
		this.abelian = new FundamentalGroup<T>(ds).getPresentation()
				.abelianInvariants();
	}
	
	public String toString() {
		final StringBuffer buf = new StringBuffer(200);
		final String stabs[] = this.ograph.getStabilizers();
		buf.append(stabs.length);
		buf.append('/');
		for (int i = 0; i < stabs.length; ++i) {
			buf.append(stabs[i]);
			buf.append('/');
		}
		if (this.ds.isOriented()) {
			buf.append("2/");
		} else if (this.ds.isWeaklyOriented()) {
			buf.append("1/");
		} else {
			buf.append("0/");
		}
		buf.append(this.ograph.getEdges().size());
		buf.append('/');
		buf.append(this.abelian.size());
		buf.append('/');
		for (final Whole n: this.abelian) {
			buf.append(n);
			buf.append('/');
		}
		return buf.toString();
	}
	
    public static void main(String[] args) {
		try {
	        int i = 0;
	        while (i < args.length && args[i].startsWith("-")) {
	        	System.err.println("Unknown option '" + args[i] + "'");
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

			final long before = System.currentTimeMillis();
			final Set<String> seen = new HashSet<String>();
			int inCount = 0;
			int dups = 0;
			
			for (final DSymbol ds: new InputIterator(in)) {
				++inCount;
				final String inv =
				        new OrbifoldInvariant<Integer>(ds).toString();
				if (seen.contains(inv)) {
					out.write("# Dup: ");
					++dups;
				} else {
					seen.add(inv);
				}
				out.write(inv);
				out.write('\n');
				out.flush();
			}
			final long after = System.currentTimeMillis();
	        out.write("### Processed " + inCount + " symbols in "
					+ (after - before) / 1000 + " seconds.\n");
	        out.write("### Found " + dups + " duplicates.\n");
	        out.flush();
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
    }
}

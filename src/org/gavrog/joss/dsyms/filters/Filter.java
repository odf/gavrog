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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.generators.InputIterator;

/**
 * Generic filter application that can sort, minimize, compute canonical forms,
 * remove duplicates.
 */
public class Filter {
    public static void main(String[] args) {
		try {
			boolean unique = false;
			boolean canonical = false;
			boolean minimize = false;
			boolean sort = false;
			
	        int i = 0;
	        while (i < args.length && args[i].startsWith("-")) {
	        	if (args[i].equalsIgnoreCase("-c")) {
	        		canonical = !canonical;
	        	} else if (args[i].equalsIgnoreCase("-m")){
	        		minimize = !minimize;
	        	} else if (args[i].equalsIgnoreCase("-s")){
	        		sort = !sort;
	        	} else if (args[i].equalsIgnoreCase("-u")){
	        		unique = !unique;
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
			final List<DelaneySymbol<Integer>> syms =
			        new ArrayList<DelaneySymbol<Integer>>();

			for (DSymbol symbol: new InputIterator(in)) {
			    DelaneySymbol<Integer> ds = symbol;
				++inCount;
				if (minimize) {
					ds = ds.minimal();
				}
				if (canonical) {
					ds = ds.canonical();
				}
				syms.add(ds);
			}
			if (unique) {
				final Set<DelaneySymbol<Integer>> set =
				        new HashSet<DelaneySymbol<Integer>>();
				set.addAll(syms);
				syms.clear();
				syms.addAll(set);
			}
			if (sort) {
				Collections.sort(syms, new Comparator<DelaneySymbol<Integer>>()
				{
					public int compare(
					        final DelaneySymbol<Integer> ds1,
					        final DelaneySymbol<Integer> ds2)
					{
						if (ds1.size() == ds2.size()) {
							return ds1.compareTo(ds2);
						} else {
							return ds1.size() - ds2.size();
						}
					}
				});
			}
			for (final DelaneySymbol<Integer> ds: syms) {
				out.write(ds.toString());
				++outCount;
				out.write("\n");
			}
			out.write("### Read " + inCount + " and wrote " + outCount
			        + " symbols.\n");
			out.flush();
		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}
}

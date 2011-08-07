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

package org.gavrog.joss.dsyms.generators;

import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gavrog.box.collections.Iterators;
import org.gavrog.box.simple.Stopwatch;
import org.gavrog.joss.algorithms.CheckpointEvent;
import org.gavrog.joss.dsyms.basic.DSymbol;
import org.gavrog.joss.dsyms.basic.DelaneySymbol;
import org.gavrog.joss.dsyms.basic.DynamicDSymbol;
import org.gavrog.joss.dsyms.basic.IndexList;
import org.gavrog.joss.dsyms.derived.EuclidicityTester;

import buoy.event.EventProcessor;


/**
 * Generates all tile-k-transitive tetrahedra tilings with edge degrees 5 and 6
 * only.
 * 
 * @author Olaf Delgado
 * @version $Id: FrankKasper.java,v 1.4 2008/03/15 05:59:46 odf Exp $
 */

public class FrankKasper extends TileKTransitive {
    public FrankKasper(final int k, final boolean verbose) {
        super(new DSymbol("1:1,1,1:3,3"), k, verbose);
    }

    protected Iterator defineBranching(final DelaneySymbol ds) {
        final DynamicDSymbol out = new DynamicDSymbol(new DSymbol(ds));
        final IndexList idx = new IndexList(2, 3);
        for (final Iterator reps = out.orbitReps(idx); reps.hasNext();) {
            final Object D = reps.next();
            final int r = out.r(2, 3, D);
            if (r == 5) {
                out.redefineV(2, 3, D, 1);
            } else if (r > 0 && 6 % r == 0) {
                out.redefineV(2, 3, D, 6 / r);
            } else {
                throw new RuntimeException(
                		"this should not happen: r = " + r + " at D = " + D);
            }
        }
        if (out.isLocallyEuclidean3D()) {
        	return Iterators.singleton(new DSymbol(out));
        } else {
        	return Iterators.empty();
        }
    }
    
    protected Iterator extendTo3d(final DSymbol ds) {
        return new CombineTiles(ds) {
            protected List<Move> getExtraDeductions(final DelaneySymbol ds,
					final Move move) {
                final List<Move> out = new ArrayList<Move>();
                final Object D = move.element;
                Object E = D;
                int r = 0;
                List<Object> cuts = new ArrayList<Object>();
                do {
                    E = ds.op(2, E);
                    if (ds.definesOp(3, E)) {
                        E = ds.op(3, E);
                    } else {
                        cuts.add(E);
                    }
                    ++r;
                } while (E != D);
                
                switch (cuts.size()) {
                case 0:
                    if (r == 4 || r > 6) {
                        return null;
                    }
                    break;
                case 1:
                    if (r > 6) {
                        return null;
                    } else if (r == 6) {
                        final Object A = cuts.get(0);
                        out.add(new Move(A, A, -1, -1, false, 0));
                    }
                    break;
                case 2:
                    if (r > 12) {
                        return null;
                    } else if (r == 12) {
                        final Object A = cuts.get(0);
                        final Object B = cuts.get(1);
                        out.add(new Move(A, B, -1, -1, false, 0));
                    }
                    break;
                default:
                    throw new RuntimeException("this should not happen");
                }
                
                return out;
            }
        };
    }

	public static void usage() {
		System.err.print(
			  "Usage: java [PATH] [OPTION]... k [FILE]\n"
			+ "\n"
			+ "where"
			+ "  PATH = org.gavrog.joss.dsyms.generators.FrankKasper\n"
			+ "  k:     number of tile orbits in each generated tiling"
			+ "\n"
			+ "Recognized options:\n"
			+ "  -e        skip euclidicity test\n"
			+ "  -i N      interval in seconds between writing checkpoints\n"
			+ "  -r A-B-C  resume generation at a checkpoint\n"
			+ "  -v        run in verbose mode\n"
			);
		System.exit(1);
	}
	
	public static void main(final String[] args) {
		try {
			boolean verbose = false;
			boolean check = true;
			int checkpointInterval = 3600;
			String resume = null;
			
			int i = 0;
			while (i < args.length && args[i].startsWith("-")) {
				if (args[i].equals("-v")) {
					verbose = !verbose;
				} else if (args[i].equals("-e")) {
					check = !check;
				} else if (args[i].equals("-i")) {
					checkpointInterval = Integer.parseInt(args[++i]);
				} else if (args[i].equals("-r")) {
					resume = args[++i];
				} else {
					usage();
				}
				++i;
			}

			if (args.length <= i) {
				usage();
			}
			
			final int k = Integer.parseInt(args[i]);
			final Writer output;
			if (args.length > i + 1) {
				output = new FileWriter(args[i + 1]);
			} else {
				output = new OutputStreamWriter(System.out);
			}

			int countGood = 0;
			int countAmbiguous = 0;

			final Stopwatch timer = new Stopwatch();
			final Stopwatch eTestTimer = new Stopwatch();
			timer.start();
			
			output.write("# Program FrankKasper with k = " + k + ".\n");
			output.write("# Options:\n");
			output.write("#     verbose mode:                    ");
			output.write((verbose ? "on" : "off") + "\n");
			output.write("#     euclidicity test:                ");
			output.write((check ? "on" : "off") + "\n");
			output.write("#     checkpoint interval:             ");
			output.write(checkpointInterval + "sec\n");
			if (resume != null) {
				output.write(String.format("# Resuming at %s.\n", resume));
			}
			output.write("\n");
			output.flush();
			
			final FrankKasper iter = new FrankKasper(k, verbose);
			final Stopwatch chkptTimer = new Stopwatch();
			final int interval = 1000 * checkpointInterval;
	        iter.addEventLink(CheckpointEvent.class, new EventProcessor() {
				@Override
				public void handleEvent(final Object event) {
					if (((CheckpointEvent) event).getMessage() != null
							|| chkptTimer.elapsed() > interval) {
						chkptTimer.reset();
						try {
							output.write(event + "\n");
							output.flush();
						} catch (Throwable ex) {
						}
					}
				}
			});
	        chkptTimer.start();
			if (resume != null) {
				iter.setResumePoint(resume);
			}

			for (final DSymbol ds : iter) {
				final DSymbol out = ds.dual();
				if (check) {
					eTestTimer.start();
					EuclidicityTester tester = new EuclidicityTester(out);
					final boolean bad = tester.isBad();
					final boolean ambiguous = tester.isAmbiguous();
					eTestTimer.stop();
					if (!bad) {
						if (ambiguous) {
							output.write("#@ name euclidicity dubious\n");
							++countAmbiguous;
						}
						output.write(out + "\n");
						++countGood;
					}
				} else {
					output.write(out + "\n");
				}
				output.flush();
			}
			timer.stop();

			output.write("\n");
			output.write("# Total execution time in user mode was "
					+ timer.format() + ".\n");
			if (check) {
				output.write("# Time for euclidicity tests was "
						+ eTestTimer.format() + ".\n");
			}
			output.write("\n");
			output.write("# " + iter.statistics() + "\n");
			if (check) {
				output.write("# Of those, " + countGood
						+ " were found euclidean.\n");
				if (countAmbiguous > 0) {
					output.write("# For " + countAmbiguous
							+ " symbols, euclidicity remains undetermined.\n");
				}
			}
			output.flush();
		} catch (final Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
}
